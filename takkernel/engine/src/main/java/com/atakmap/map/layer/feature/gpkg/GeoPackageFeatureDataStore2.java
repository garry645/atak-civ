package com.atakmap.map.layer.feature.gpkg;

import android.net.Uri;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.database.CursorIface;
import com.atakmap.database.RowIteratorWrapper;
import com.atakmap.map.gpkg.GeoPackage;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureCursor;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.FeatureSetCursor;
import com.atakmap.map.layer.feature.Utils;
import com.atakmap.map.layer.feature.cursor.BruteForceLimitOffsetFeatureCursor;
import com.atakmap.map.layer.feature.cursor.MultiplexingFeatureCursor;
import com.atakmap.map.layer.feature.datastore.AbstractReadOnlyFeatureDataStore2;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.GeometryFactory;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.math.Rectangle;
import com.atakmap.spatial.GeometryTransformer;
import com.atakmap.util.Collections2;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class GeoPackageFeatureDataStore2 extends AbstractReadOnlyFeatureDataStore2
{

    private static final int MODIFY_PROHIBITED = 0;
//    private static final int VISIBILITY_SETTINGS_PROHIBITED = 0;

    private static final String TAG = "GeoPackageFeatureDataStore";
    private static final String PROVIDER = "gpkg";
    private static final String TYPE = "gpkg";

    private final GeoPackageSchemaHandler2.OnFeatureDefinitionsChangedListener changeListener = new GeoPackageSchemaHandler2.OnFeatureDefinitionsChangedListener()
    {
        @Override
        public void onFeatureDefinitionsChanged(GeoPackageSchemaHandler2 handler)
        {
            schemaRefreshes++;
            dispatchContentChanged();
        }
    };
    private final Map<String, LayerInfo> layersByName = new HashMap<>();
    private final Map<Long, LayerInfo> layersById = new HashMap<>();
    private int nextLayerId = 1;
    private final Map<Long, FeatureSetInfo> featureSetInfos = new HashMap<>();
    private int nextFSID = 1;
    private final String URI;

    protected GeoPackage geoPackage;
    protected GeoPackageSchemaHandler2 schemaHandler;
    protected boolean disposed;
    protected int fsIdBits;               // Number of feature set bits in id.
    protected long fIdMask;               // Bit mask for feature portion of id.
    protected int schemaRefreshes;

    public GeoPackageFeatureDataStore2(File dbFile)
    {
        super(MODIFY_PROHIBITED, VISIBILITY_SETTINGS_FEATURESET);
        URI = Uri.fromFile(dbFile).toString();
        initDataStore(dbFile);
    }

    private FeatureSet getFeatureSet(long featureSetID)
    {
        FeatureSet result = null;

        FeatureSetInfo info = featureSetInfos.get(Long.valueOf(featureSetID));
        if (info != null)
        {

            result = new FeatureSet(featureSetID,
                    PROVIDER, TYPE,
                    info.featureSetName,
                    info.minResolution,
                    info.maxResolution,
                    schemaHandler.getSchemaVersion());
        }

        return result;
    }

    public GeoPackageSchemaHandler2 getSchemaHandler()
    {
        return schemaHandler;
    }

    @Override
    public boolean hasTimeReference() {
        return false;
    }

    @Override
    public long getMinimumTimestamp() {
        return TIMESTAMP_NONE;
    }

    @Override
    public long getMaximumTimestamp() {
        return TIMESTAMP_NONE;
    }

    @Override
    public String getUri()
    {
        return URI;
    }

    @Override
    public boolean hasCache() {
        return false;
    }

    @Override
    public void clearCache() {

    }

    @Override
    public long getCacheSize() {
        return 0;
    }

    @Override
    public FeatureCursor queryFeatures(FeatureQueryParameters params)
    {
        Map<LayerInfo, FeatureQueryParameters> queryMap = splitQuery(params);
        boolean bruteForceLimit = params.limit != 0 && queryMap.size() > 1;

        if (bruteForceLimit)
        {
            for (FeatureQueryParameters fsParams : queryMap.values())
            {
                if (fsParams != null)
                {
                    fsParams.offset = fsParams.limit = 0;
                }
            }
        }

        List<FeatureCursor> cursors
                = new ArrayList<FeatureCursor>(queryMap.size());

        for (Map.Entry<LayerInfo, FeatureQueryParameters> entry
                : queryMap.entrySet())
        {
            cursors.add(queryFeatures(entry.getKey(), entry.getValue()));
        }

        FeatureCursor result
                = new MultiplexingFeatureCursor(cursors, (params.order != null) ?
                    new ArrayList<>(params.order) : null);

        return bruteForceLimit
                ? new BruteForceLimitOffsetFeatureCursor(result,
                params.offset,
                params.limit)
                : result;
    }

    @Override
    public int queryFeaturesCount(FeatureQueryParameters params) throws DataStoreException
    {
        Map<LayerInfo, FeatureQueryParameters> queryMap = splitQuery(params);
        boolean bruteForceLimit = params.limit != 0 && queryMap.size() > 1;

        if (bruteForceLimit)
        {
            return Utils.queryFeaturesCount(this, params);
        }

        int featuresCount = 0;
        for (Map.Entry<LayerInfo, FeatureQueryParameters> entry
                : queryMap.entrySet())
        {
            String layerName = entry.getKey().name;
            featuresCount += schemaHandler.queryFeaturesCount(layerName, entry.getValue());
        }

        return featuresCount;
    }

    @Override
    public FeatureSetCursor queryFeatureSets(FeatureSetQueryParameters params)
    {
        //
        // Sort results by FeatureSet name.
        //
        Map<FeatureSetInfo, Long> matchingSets
                = new TreeMap<FeatureSetInfo, Long>
                (new Comparator<FeatureSetInfo>()
                {
                    @Override
                    public int
                    compare(FeatureSetInfo lhs,
                            FeatureSetInfo rhs)
                    {
                        return lhs.featureSetName.compareToIgnoreCase(rhs.featureSetName);
                    }
                });

        for (FeatureSetInfo fsInfo : featureSetInfos.values())
        {
            if (matches(fsInfo, params))
            {
                matchingSets.put(fsInfo,
                        fsInfo.fsid);
            }
        }

        return new FeatureSetCursorImpl(new ArrayList<>(matchingSets.values()));
    }

    @Override
    public int queryFeatureSetsCount(FeatureSetQueryParameters params)
    {
        final int abslimit = (params != null) ? (params.offset + params.limit) : 0;
        final int offset = (params != null) ? params.offset : 0;
        int matchingSetCount = 0;

        for (FeatureSetInfo fsInfo : featureSetInfos.values())
        {
            if (matches(fsInfo, params))
            {
                ++matchingSetCount;
                if (matchingSetCount == abslimit)
                    break;
            }
        }

        return Math.max(matchingSetCount - offset, 0);
    }

    @Override
    public synchronized void dispose()
    {
        if (!disposed)
        {
            geoPackage.close();
            schemaHandler.removeOnFeatureDefinitionsChangedListener(changeListener);
            schemaHandler = null;
            featureSetInfos.clear();
            disposed = true;
        }
    }

    @Override
    protected boolean setFeatureVisibleImpl(long fid, boolean visible)
    {
        return false;
    }


    @Override
    protected boolean setFeatureSetVisibleImpl(long setId, boolean visible)
    {
        boolean changed = false;
        FeatureSetInfo fsInfo = featureSetInfos.get(Long.valueOf(setId));

        if (fsInfo != null)
        {
            changed = visible != fsInfo.visible;
            fsInfo.visible = visible;
        }

        return changed;
    }

    private class FeatureCursorImpl extends RowIteratorWrapper implements FeatureCursor
    {
        private GeoPackageSchemaHandler2 schemaHandler;
        private long fsID;
        private Geometry rawGeometry;
        private Style rawStyle;
        private AttributeSet attribs;
        private Feature feature;
        private final GeoPackageFeatureCursor filter;

        FeatureCursorImpl(GeoPackageFeatureCursor wrapped, GeoPackageSchemaHandler2 handler, long featureSetID)
        {
            super(wrapped);
            this.filter = wrapped;
            schemaHandler = handler;
            fsID = featureSetID;
        }

        @Override
        public Feature get()
        {
            if (feature == null)
            {
                feature = new Feature(fsID,
                        getId(),
                        getName(),
                        (Geometry) getRawGeometry(),
                        (Style) getRawStyle(),
                        getAttributes(),
                        FeatureDataStore2.TIMESTAMP_NONE,
                        getVersion());
            }

            return feature;
        }

        @Override
        public AttributeSet getAttributes()
        {
            if (attribs == null)
            {attribs = this.filter.getAttributes();}

            return attribs;
        }

        @Override
        public long getFsid()
        {
            return fsID;
        }

        @Override
        public int getGeomCoding()
        {
            return GEOM_ATAK_GEOMETRY;
        }

        @Override
        public long getId()
        {
            return fsID << (64 - fsIdBits)
                    | getFeatureID();
        }

        @Override
        public String getName()
        {
            return this.filter.getName();
        }

        @Override
        public Object getRawGeometry()
        {
            if (rawGeometry == null)
            {rawGeometry = this.filter.getGeometry();}

            return rawGeometry;
        }

        @Override
        public Object getRawStyle()
        {
            if (rawStyle == null)
            {rawStyle = this.filter.getStyle();}

            return rawStyle;
        }

        @Override
        public int getStyleCoding()
        {
            return STYLE_ATAK_STYLE;
        }

        @Override
        public long getVersion()
        {
            return this.filter.getVersion();
        }

        @Override
        public boolean moveToNext()
        {
            rawGeometry = null;
            rawStyle = null;
            attribs = null;
            feature = null;
            return super.moveToNext();
        }

        private long getFeatureID()
        {
            return this.filter.getID();
        }
    }

    private class FeatureSetCursorImpl implements FeatureSetCursor
    {
        private List<Long> featureSetIDs;
        private int i = -1;

        FeatureSetCursorImpl(List<Long> ids)
        {
            featureSetIDs = ids;
        }

        @Override
        public FeatureSet get()
        {
            return i < featureSetIDs.size()
                    ? GeoPackageFeatureDataStore2.this.getFeatureSet
                    (featureSetIDs.get(i).longValue())
                    : null;
        }

        @Override
        public long getId() {
            if(i < 0 || i >= featureSetIDs.size())
                return FEATURESET_ID_NONE;
            final FeatureSetInfo info = featureSetInfos.get(featureSetIDs.get(i));
            if(info == null)
                return FEATURESET_ID_NONE;
            return info.fsid;
        }

        @Override
        public String getType() {
            return TYPE;
        }

        @Override
        public String getProvider() {
            return PROVIDER;
        }

        @Override
        public String getName() {
            if(i < 0 || i >= featureSetIDs.size())
                return null;
            final FeatureSetInfo info = featureSetInfos.get(featureSetIDs.get(i));
            if(info == null)
                return null;
            return info.layerName;
        }

        @Override
        public double getMinResolution() {
            if(i < 0 || i >= featureSetIDs.size())
                return Double.NaN;
            final FeatureSetInfo info = featureSetInfos.get(featureSetIDs.get(i));
            if(info == null)
                return Double.NaN;
            return info.minResolution;
        }

        @Override
        public double getMaxResolution() {
            if(i < 0 || i >= featureSetIDs.size())
                return Double.NaN;
            final FeatureSetInfo info = featureSetInfos.get(featureSetIDs.get(i));
            if(info == null)
                return Double.NaN;
            return info.maxResolution;
        }

        @Override
        public void close()
        {
            featureSetIDs.clear();
            i = 0;
        }

        @Override
        public boolean isClosed()
        {
            return featureSetIDs.isEmpty() && i == 0;
        }

        @Override
        public boolean moveToNext()
        {
            return ++i < featureSetIDs.size();
        }
    }

    private static class FeatureSetInfo
    {
        final String layerName;
        final long fsid;
        final String featureSetName;
        final double minResolution;
        final double maxResolution;
        boolean visible = true;

        FeatureSetInfo(String layerName,
                       long fsid,
                       String featureSetName,
                       double minResolution,
                       double maxResolution)
        {
            this.layerName = layerName;
            this.fsid = fsid;
            this.featureSetName = featureSetName;
            this.minResolution = minResolution;
            this.maxResolution = maxResolution;
        }


    }

    //
    // Returns a copy of the supplied FeatureQueryParameters filtered for a
    // particular FeatureSet.  Returns null if the supplied
    // FeatureQueryParameters is null or the query against the FeatureSet needs
    // no restrictions.
    //
    private FeatureQueryParameters filterQuery(LayerInfo layer, FeatureQueryParameters params)
    {
        FeatureQueryParameters result = null;

        if (params != null)
        {
            FeatureQueryParameters fsParams
                    = new FeatureQueryParameters(params);

            if (params.ids != null)
            {
                //
                // Check and mask off FSID.
                //
                fsParams.ids = new HashSet<>();
                for (Long fID : params.ids)
                {
                    long longID = fID.longValue();

                    if (longID >>> (64 - fsIdBits) == layer.layerId)
                    {
                        fsParams.ids.add(longID & fIdMask);
                    }
                }
                result = fsParams;
            }
            if (params.visibleOnly)
            {
                Set<Long> visibleFSIDs = new HashSet<Long>();
                for (FeatureSetInfo fsInfo : layer.featureSets.values())
                {
                    if (fsInfo.visible)
                        visibleFSIDs.add(Long.valueOf(fsInfo.fsid));
                }
                if (visibleFSIDs.size() != layer.featureSets.size())
                {
                    if(fsParams.featureSetFilter == null)
                        fsParams.featureSetFilter = new FeatureSetQueryParameters();
                    if (fsParams.featureSetFilter.ids == null)
                        fsParams.featureSetFilter.ids = visibleFSIDs;
                    else
                        fsParams.featureSetFilter.ids.retainAll(visibleFSIDs);
                }
                params = fsParams;
            }
            if (params.featureSetFilter != null && params.featureSetFilter.names != null)
            {
                Set<String> absoluteNames = new HashSet<String>();
                for (String featureSetName : layer.featureSetNames)
                {
                    if (Utils.matches(params.featureSetFilter.names, featureSetName, '%'))
                        absoluteNames.add(featureSetName);
                }
                fsParams.featureSetFilter.names = absoluteNames;
                result = fsParams;
            }
            if (params.featureSetFilter != null && params.featureSetFilter.ids != null)
            {
                // intersect FSIDs with layer FSIDs
                fsParams.featureSetFilter.ids.retainAll(layer.featureSets.keySet());

                // convert FSIDs to feature set names
                Set<String> featureSetNames = new HashSet<String>();
                for (Long fsid : params.featureSetFilter.ids)
                {
                    final FeatureSetInfo fsInfo = layer.featureSets.get(fsid);
                    if (fsInfo != null)
                        featureSetNames.add(fsInfo.featureSetName);
                }
                fsParams.featureSetFilter.ids = null;

                // intersect FSID derived names with parameter names
                if (fsParams.featureSetFilter.names != null)
                    featureSetNames.retainAll(fsParams.featureSetFilter.names);

                // params will utilize names for schema handler
                fsParams.featureSetFilter.names = featureSetNames;
                result = fsParams;
            }

            if (params.featureSetFilter != null && params.featureSetFilter.providers != null)
            {
                fsParams.featureSetFilter.providers = null;
                result = fsParams;
            }
            if (params.featureSetFilter != null && params.featureSetFilter.types != null)
            {
                fsParams.featureSetFilter.types = null;
                result = fsParams;
            }
            if (result == null
                    && (params.visibleOnly
                    || params.geometryTypes != null
                    || params.ignoredFeatureProperties != 0
                    || (params.featureSetFilter != null && !Double.isNaN(params.featureSetFilter.maxResolution))
                    || (params.featureSetFilter != null && !Double.isNaN(params.featureSetFilter.minResolution))
                    || params.offset != 0
                    || params.spatialOps != null
                    || params.order != null
                    || params.spatialFilter != null))
            {
                result = fsParams;
            }
        }

        return result;
    }

    private String
    getPackagePath()
    {
        //
        // The path to the GeoPackage is the path to the main database.
        //
        String result = null;
        CursorIface cursor = null;

        try
        {
            cursor = geoPackage.getDatabase().query("PRAGMA database_list",
                    null);
            while (result == null && cursor.moveToNext())
            {
                if (cursor.getString(1).equals("main"))
                {
                    result = cursor.getString(2);
                }
            }
        } catch (Exception e)
        {
            Log.e(TAG, "Error getting GeoPackage path from database list");
        } finally
        {
            if (cursor != null)
            {
                cursor.close();
            }
        }

        return result;
    }

    private void initDataStore(File dbFile)
    {
        if (!GeoPackage.isGeoPackage(dbFile))
            throw new IllegalArgumentException("File " + dbFile
                    + " is not a GeoPackage");
        geoPackage = new GeoPackage(dbFile);
        schemaHandler = GeoPackageSchemaHandlerRegistry.getHandler2(geoPackage);
        schemaHandler.addOnFeatureDefinitionsChangedListener(changeListener);

        for (GeoPackage.ContentsRow content : geoPackage.getPackageContents())
        {
            if (content.data_type == GeoPackage.TableType.FEATURES)
            {
                final String layerName = content.identifier;

                if (schemaHandler.ignoreLayer(layerName))
                    continue;

                LayerInfo layer = new LayerInfo(nextLayerId++,
                        layerName,
                        schemaHandler.getMinResolution(layerName),
                        schemaHandler.getMaxResolution(layerName),
                        GeometryTransformer.transform(new Envelope(content.min_x, content.min_y, 0d, content.max_x, content.max_y, 0d), content.srs_id, 4326),
                        content.srs_id);
                layer.featureSetNames.addAll(schemaHandler.getLayerFeatureSets(layerName));
                for (String featureSetName : layer.featureSetNames)
                {
                    final FeatureSetInfo fs =
                            new FeatureSetInfo(layer.name,
                                    nextFSID++,
                                    featureSetName,
                                    layer.minResolution,
                                    layer.maxResolution);
                    fs.visible = schemaHandler.getDefaultFeatureSetVisibility(layer.name, featureSetName);
                    featureSetInfos.put(Long.valueOf(fs.fsid), fs);

                    layer.featureSets.put(Long.valueOf(fs.fsid), fs);
                }

                layersById.put(Long.valueOf(layer.layerId), layer);
                layersByName.put(layer.name, layer);
            }
        }

        if (featureSetInfos.isEmpty())
        {
            geoPackage.close();
            throw new IllegalArgumentException("GeoPackage " + dbFile
                    + " contains no feature sets");
        }
        fsIdBits = (int) Math.ceil(Math.log(featureSetInfos.size())
                / Math.log(2.0));
        fIdMask = -1L >>> fsIdBits;
    }

    public GeoPackage getGeoPackage()
    {
        return this.geoPackage;
    }

    //
    // Returns true if the supplied FeatureQueryParameters are consistent with a
    // particular FeatureSet.
    //
    private boolean matches(String layerName, FeatureQueryParameters params)
    {
        if (params == null)
        {
            return true;
        }

        // provider/type
        if(params.featureSetFilter != null) {
            if (params.featureSetFilter.providers != null
                    && !Utils.matches(params.featureSetFilter.providers, PROVIDER, '%')
                    || params.featureSetFilter.types != null
                    && !Utils.matches(params.featureSetFilter.types, TYPE, '%')) {
                return false;
            }
        }

        final LayerInfo info = layersByName.get(layerName);
        if (info == null)
            return false;

        // FSIDs
        if (params.featureSetFilter != null
                && params.featureSetFilter.ids != null
                && !Collections2.containsAny(info.featureSets.keySet(), params.featureSetFilter.ids))
        {
            return false;
        }
        // FIDs
        if (params.ids != null)
        {
            //
            // Find requests for features in this FeatureSet.
            //
            boolean matches = false;

            for (Long fID : params.ids)
            {
                if (fID.longValue() >>> (64 - fsIdBits) == info.layerId)
                {
                    matches = true;
                    break;
                }
            }
            if (!matches)
            {
                return false;
            }
        }

        // feature set names
        if (params.featureSetFilter != null && params.featureSetFilter.names != null)
        {
            boolean matches = false;
            for (String featureSetName : info.featureSetNames)
            {
                if (Utils.matches(params.featureSetFilter.names, featureSetName, '%'))
                {
                    matches = true;
                    break;
                }
            }
            if (!matches)
            {
                return false;
            }
        }
        // resolution thresholds
        if(params.featureSetFilter != null) {
            if ((!Double.isNaN(params.featureSetFilter.minResolution)
                    && !Double.isNaN(info.maxResolution)
                    && params.featureSetFilter.minResolution < info.maxResolution)
                    || (!Double.isNaN(params.featureSetFilter.maxResolution)
                    && !Double.isNaN(info.minResolution)
                    && params.featureSetFilter.maxResolution > info.minResolution)) {
                return false;
            }
        }
        // visibility
        if (params.visibleOnly && !isVisible(info))
        {
            return false;
        }
        // geometry types
        if (params.geometryTypes != null)
        {
            boolean matches = false;
            Class<? extends Geometry> layerGeometry
                    = schemaHandler.getGeometryType(layerName);

            if (layerGeometry != null)
            {
                for (Class<? extends Geometry> geoClass : params.geometryTypes)
                {
                    //
                    // A layer may declare a general type name (e.g., GEOMETRY
                    // or GEOMETRYCOLLECTION) in the gpkg_geometry_columns table.
                    // We can only rule out the FeatureSet if the geometry
                    // generalization hierarchies don't intersect.
                    //
                    if (geoClass.isAssignableFrom(layerGeometry)
                            || layerGeometry.isAssignableFrom(geoClass))
                    {
                        matches = true;
                        break;
                    }
                }
            }
            if (!matches)
            {
                return false;
            }
        }

        return true;
    }

    private static boolean isVisible(LayerInfo layer)
    {
        for (FeatureSetInfo info : layer.featureSets.values())
            if (info.visible)
                return true;
        return false;
    }

    //
    // Returns true if the supplied FeatureSetQueryParameters are consistent
    // with a particular FeatureSet.
    //
    private boolean
    matches(FeatureSetInfo fsInfo,
            FeatureSetQueryParameters params)
    {
        if (params == null)
        {
            return true;
        }

        if (params.providers != null && !params.providers.isEmpty()
                && !Utils.matches(params.providers, PROVIDER, '%')
                || params.types != null && !params.types.isEmpty()
                && !Utils.matches(params.types, TYPE, '%')
                || params.ids != null && !params.ids.isEmpty()
                && !params.ids.contains(Long.valueOf(fsInfo.fsid))
                || params.names != null && !params.names.isEmpty()
                && !Utils.matches(params.names, fsInfo.featureSetName, '%')
                || params.visibleOnly && !fsInfo.visible)
        {
            return false;
        }

        return true;
    }

    private FeatureCursor
    queryFeatures(LayerInfo layer,
                  FeatureQueryParameters params)
    {
        if(layer.srid != 4326 && params.spatialFilter != null) {
            if(params.spatialFilter instanceof Point) {
                final Point p = (Point)params.spatialFilter;
                if(!Rectangle.contains(layer.bounds_wgs84.minX, layer.bounds_wgs84.minY, layer.bounds_wgs84.maxX, layer.bounds_wgs84.maxY, p.getX(), p.getY()))
                    return FeatureCursor.EMPTY;
            } else {
                Envelope spatialFilter = params.spatialFilter.getEnvelope();
                if(!Rectangle.intersects(layer.bounds_wgs84.minX, layer.bounds_wgs84.minY, layer.bounds_wgs84.maxX, layer.bounds_wgs84.maxY, spatialFilter.minX, spatialFilter.minY, spatialFilter.maxX, spatialFilter.maxY))
                    return FeatureCursor.EMPTY;
                // intersect the spatial filter with the bounds -- the native SRS may only support
                // transformation for a region
                params = new FeatureQueryParameters(params);
                params.spatialFilter = GeometryFactory.fromEnvelope(
                    new Envelope(
                        Math.max(layer.bounds_wgs84.minX, spatialFilter.minX),
                        Math.max(layer.bounds_wgs84.minY, spatialFilter.minY),
                        0d,
                        Math.min(layer.bounds_wgs84.maxX, spatialFilter.maxX),
                        Math.min(layer.bounds_wgs84.maxY, spatialFilter.maxY),
                        0d));
            }
        }
        return new FeatureCursorImpl(schemaHandler.queryFeatures(layer.name,
                params),
                schemaHandler,
                layer.layerId);
    }

    //
    // Splits the supplied FeatureQueryParameters into FeatureSet-specific
    // parameters.
    //

    /**
     * Splits the specified query parameters into layer-specific parameters.
     * Any feature set filtering will be expanded into all matching feature set
     * names on per-layer basis.
     *
     * @param params The parameters
     * @return
     */
    private Map<LayerInfo, FeatureQueryParameters>
    splitQuery(FeatureQueryParameters params)
    {
        Map<LayerInfo, FeatureQueryParameters> result
                = new HashMap<LayerInfo, FeatureQueryParameters>();

        for (LayerInfo layer : layersById.values())
        {
            if (matches(layer.name, params))
            {
                result.put(layer, filterQuery(layer, params));
            }
        }

        return result;
    }
    
    static class LayerInfo
    {
        final String name;
        final int layerId;
        final Map<Long, FeatureSetInfo> featureSets;
        final Set<String> featureSetNames;
        final double minResolution;
        final double maxResolution;
        final Envelope bounds_wgs84;
        final int srid;

        LayerInfo(int id, String name, double minResolution, double maxResolution, Envelope bounds_wgs84, int srid)
        {
            this.layerId = id;
            this.name = name;
            this.featureSets = new HashMap<>();
            this.featureSetNames = new HashSet<>();
            this.minResolution = minResolution;
            this.maxResolution = maxResolution;
            this.bounds_wgs84 = bounds_wgs84;
            this.srid = srid;
        }
    }
}
