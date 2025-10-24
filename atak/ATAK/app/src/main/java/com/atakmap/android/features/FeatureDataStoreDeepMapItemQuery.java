
package com.atakmap.android.features;

import android.graphics.Color;
import android.support.util.LruCache;
import android.util.Pair;

import com.atakmap.android.maps.DeepMapItemQuery;
import com.atakmap.android.maps.DefaultMetaDataHolder;
import com.atakmap.android.maps.FilterMetaDataHolder;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapTouchController;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.MetaDataHolder2;
import com.atakmap.android.maps.MetaShape;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.Polyline;
import com.atakmap.android.maps.Shape;
import com.atakmap.android.maps.hittest.DeepHitTestControlQuery;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.assets.Icon;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.coremap.maps.coords.MutableGeoBounds;
import com.atakmap.map.MapControl;
import com.atakmap.map.MapRenderer2;
import com.atakmap.map.hittest.ClassResultFilter;
import com.atakmap.map.hittest.HitTestControl;
import com.atakmap.map.hittest.HitTestQueryParameters;
import com.atakmap.map.hittest.HitTestResult;
import com.atakmap.map.layer.Layer2;
import com.atakmap.map.layer.feature.Adapters;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureCursor;
import com.atakmap.map.layer.feature.FeatureDataStore;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDataStore2.FeatureQueryParameters;
import com.atakmap.map.layer.feature.FeatureDataStore3;
import com.atakmap.map.layer.feature.FeatureLayer3;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.FeatureSetCursor;
import com.atakmap.map.layer.feature.Utils;
import com.atakmap.map.layer.feature.control.FeaturesLabelControl;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.GeometryCollection;
import com.atakmap.map.layer.feature.geometry.GeometryFactory;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.geometry.Polygon;
import com.atakmap.map.layer.feature.style.BasicFillStyle;
import com.atakmap.map.layer.feature.style.BasicPointStyle;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.IconPointStyle;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.map.opengl.GLRenderGlobals;
import com.atakmap.spatial.file.MvtSpatialDb;
import com.atakmap.util.Collections2;
import com.atakmap.util.Visitor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import gov.tak.api.annotation.DeprecatedApi;

public class FeatureDataStoreDeepMapItemQuery implements DeepMapItemQuery,
        DeepHitTestControlQuery {

    private final static String TAG = "FeatureDataStoreDeepMapItemQuery";

    private final static Set<String> SUPPORTED_METADATA = new HashSet<>();
    static {
        SUPPORTED_METADATA.add("uid");
        SUPPORTED_METADATA.add("featureId");
        SUPPORTED_METADATA.add("visibleOnly");
        SUPPORTED_METADATA.add("limit");
    }

    private final static Set<String> ATTRIBUTE_METADATA_KEYS = new HashSet<>();
    static {
        ATTRIBUTE_METADATA_KEYS.add("html");
        ATTRIBUTE_METADATA_KEYS.add("remarks");
        ATTRIBUTE_METADATA_KEYS.add("attachments");
        ATTRIBUTE_METADATA_KEYS.add("title");
    }

    private static final double DEFAULT_RENDER_SCALE = (1.0d / 250000.0d);
    private static final long DATE_CUTOFF = 100000000000L;

    //private final WeakValueMap<Long, Pair<MapItem, Feature>> items;
    private final LruCache<Long, Pair<MapItem, Feature>> items;

    protected long groupId;
    protected final FeatureDataStore2 spatialDb;
    protected final Layer2 layer;
    protected final String uidPrefix;
    protected final boolean noVisibilitySupport;

    public FeatureDataStoreDeepMapItemQuery(FeatureLayer3 layer) {
        this(layer, layer.getDataStore());
    }

    public FeatureDataStoreDeepMapItemQuery(Layer2 layer,
            FeatureDataStore2 db) {
        this.layer = layer;
        this.spatialDb = db;
        this.items = new LruCache<>(100);
        this.noVisibilitySupport = (spatialDb.getVisibilityFlags()
                & FeatureDataStore2.VISIBILITY_SETTINGS_FEATURE) == 0;

        this.uidPrefix = "spatialdb::" + this.spatialDb.getUri();
    }

    @Override
    public MapItem deepFindItem(Map<String, String> metadata) {
        if (!Collections2.containsAny(metadata.keySet(), SUPPORTED_METADATA))
            return null;

        List<MapItem> results = this.deepFindItemsImpl(metadata, 1);
        if (!results.isEmpty())
            return results.get(0);
        else
            return null;
    }

    @Override
    public List<MapItem> deepFindItems(Map<String, String> metadata) {
        if (metadata != null) {
            if (!Collections2
                    .containsAny(metadata.keySet(), SUPPORTED_METADATA))
                return Collections.emptyList();
        }

        return this.deepFindItemsImpl(metadata, 0);
    }

    protected List<MapItem> deepFindItemsImpl(Map<String, String> metadata,
            int limit) {
        Set<Long> fids = null;
        boolean visibleOnly = false;
        if (metadata != null) {
            final long fid = getFeatureId(metadata);
            if (fid != 0L)
                fids = Collections.singleton(fid);
            if (limit == 0 && metadata.containsKey("limit"))
                try {
                    final String str = metadata.get("limit");
                    if (str != null)
                        limit = Integer.parseInt(str);
                } catch (Exception ignored) {
                    // invalid limit passed in - keep it as 0
                }
            if (metadata.containsKey("visibleOnly")) {
                final String str = metadata.get("visibleOnly");
                visibleOnly = (str != null && str.equals("true"));
            } else if (fid == 0L)
                return Collections.emptyList();
        }

        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.groupId = this.groupId;
        queryBuilder.fids = fids;
        queryBuilder.limit = limit;
        queryBuilder.visibleOnly = visibleOnly;

        List<MapItem> retval = new LinkedList<>();
        this.deepFindItemsImpl(retval, queryBuilder);
        return retval;
    }

    @Override
    public MapItem deepFindClosestItem(GeoPoint location, double threshold,
            Map<String, String> metadata) {

        if (metadata != null) {
            if (!Collections2
                    .containsAny(metadata.keySet(), SUPPORTED_METADATA))
                return null;
        }

        Collection<MapItem> results = this.deepFindItemsImpl(location,
                threshold, metadata,
                true, 1);
        if (!results.isEmpty())
            return results.iterator().next();
        else
            return null;
    }

    @Override
    public Collection<MapItem> deepFindItems(GeoPoint location, double radius,
            Map<String, String> metadata) {

        if (metadata != null) {
            if (!Collections2
                    .containsAny(metadata.keySet(), SUPPORTED_METADATA))
                return Collections.emptySet();
        }

        return this.deepFindItemsImpl(location, radius, metadata,
                false, 0);
    }

    @Override
    public Collection<MapItem> deepFindItems(
            GeoBounds bounds, Map<String, String> metadata) {

        int limit = 0;
        Set<Long> fids = null;
        boolean visibleOnly = false;
        if (metadata != null) {
            if (!Collections2
                    .containsAny(metadata.keySet(), SUPPORTED_METADATA))
                return Collections.emptySet();

            final long fid = getFeatureId(metadata);
            if (fid != 0L)
                fids = Collections.singleton(fid);
            if (metadata.containsKey("limit")) {
                try {
                    final String str = metadata.get("limit");
                    if (str != null)
                        limit = Integer.parseInt(str);
                } catch (Exception e) {
                    // assume that an invalid limit is still 0
                }
            }
            if (metadata.containsKey("visibleOnly")) {
                final String str = metadata.get("visibleOnly");
                visibleOnly = (str != null && str.equals("true"));
            } else if (fid == 0L)
                return Collections.emptyList();
        }
        QueryBuilder qb = new QueryBuilder();
        qb.groupId = this.groupId;
        qb.fids = fids;
        qb.bounds = bounds;
        qb.orderByDistance = false;
        qb.visibleOnly = visibleOnly;
        qb.limit = limit;

        List<MapItem> retval = new LinkedList<>();
        this.deepFindItemsImpl(retval, qb);
        return retval;
    }

    private Collection<MapItem> deepFindItemsImpl(GeoPoint location,
            double radius,
            Map<String, String> metadata,
            boolean orderByDistance, int limit) {

        Set<Long> fids = null;
        boolean visibleOnly = false;
        if (metadata != null) {
            if (!Collections2
                    .containsAny(metadata.keySet(), SUPPORTED_METADATA))
                return Collections.emptySet();

            final long fid = getFeatureId(metadata);
            if (fid != 0L)
                fids = Collections.singleton(fid);
            if (limit == 0 && metadata.containsKey("limit"))
                try {
                    final String str = metadata.get("limit");
                    if (str != null)
                        limit = Integer.parseInt(str);
                } catch (Exception e) {
                    // assume in an error condition the limit is 0
                }
            if (metadata.containsKey("visibleOnly")) {
                final String str = metadata.get("visibleOnly");
                visibleOnly = (str != null && str.equals("true"));
            } else if (fid == 0L)
                return Collections.emptyList();
        }

        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.groupId = this.groupId;
        queryBuilder.fids = fids;
        queryBuilder.location = location;
        queryBuilder.radius = radius;
        queryBuilder.orderByDistance = orderByDistance;
        queryBuilder.limit = limit;
        queryBuilder.visibleOnly = visibleOnly;

        List<MapItem> retval = new LinkedList<>();
        this.deepFindItemsImpl(retval, queryBuilder);
        return retval;
    }

    protected MapItem featureToMapItem(Feature feature) {
        return featureToMapItem(feature, this.uidPrefix, this.layer,
                this.spatialDb);
    }

    public static MapItem featureToMapItem(Feature feature, String uidPrefix) {
        return featureToMapItem(feature, uidPrefix, null, null);
    }

    static String findNameAttribute(AttributeSet attrs) {
        final Locale locale = LocaleUtil.getCurrent();
        final String lang = locale.getDisplayLanguage().toLowerCase(locale);
        final String script = locale.getDisplayScript().toLowerCase(locale);
        final String[] keys = new String[] {
                "name",
                "name:" + lang,
                "name_" + lang,
                "name:" + script,
                "NAME",
                "Name",
        };
        for (String key : keys) {
            if (attrs.containsAttribute(key))
                return key;
        }
        return null;
    }

    static MapItem featureToMapItem(Feature feature, String uidPrefix,
            Layer2 layer, FeatureDataStore2 spatialDb) {
        MapItem item;

        final String featureName = feature.getName();
        final long id = feature.getId();
        final AttributeSet attribs = feature.getAttributes();
        final MetaDataHolder2 metadata = (attribs == null && spatialDb != null)
                ? new DeferredFeatureMetadata(spatialDb, id, featureName)
                : attributesToMetadata(attribs, new DefaultMetaDataHolder(),
                        featureName);
        final Geometry geom = feature.getGeometry();
        final Style style = feature.getStyle();
        final String title = (featureName != null && !featureName.isEmpty())
                ? featureName
                : (attribs != null)
                        ? metadata.getMetaString("title", "[Unnamed]")
                        : "[Unnamed]";

        final String uid = uidPrefix + "::" + id;
        final String type = "u-d-feature";

        if (geom instanceof Point) {
            final Point p = (Point) geom;

            GeoPointMetaData gpm = FeatureHierarchyUtils.getAltitude(p,
                    feature.getAltitudeMode(), false);

            Marker m = new Marker(MapItem.createSerialId(),
                    metadata,
                    uid);
            m.setPoint(gpm);

            if (title != null) {
                m.setTitle(title);

                // XXX - hack to support search
                m.setMetaString("callsign", title);
            }

            int iconColor = -1;
            String iconUri = null;
            float iconWidth = 0f;
            float iconHeight = 0f;

            IconPointStyle istyle = (style instanceof IconPointStyle)
                    ? (IconPointStyle) style
                    : null;
            BasicPointStyle bstyle = (style instanceof BasicPointStyle)
                    ? (BasicPointStyle) style
                    : null;

            if (style instanceof CompositeStyle) {
                istyle = (IconPointStyle) CompositeStyle
                        .find((CompositeStyle) style, IconPointStyle.class);
                bstyle = (BasicPointStyle) CompositeStyle
                        .find((CompositeStyle) style, BasicPointStyle.class);
            }
            if (bstyle != null) {
                iconColor = bstyle.getColor();
                iconWidth = bstyle.getSize();
                iconHeight = bstyle.getSize();
            } else if (istyle != null) {
                iconUri = istyle.getIconUri();
                iconWidth = istyle.getIconWidth();
                iconHeight = istyle.getIconHeight();
                iconColor = istyle.getColor();

                // XXX - alignment
            }

            if (iconUri != null) {
                Icon.Builder mib = new Icon.Builder()
                        .setImageUri(0, iconUri)
                        .setSize(
                                (int) Math.ceil(iconWidth
                                        * GLRenderGlobals.getRelativeScaling()),
                                (int) Math.ceil(iconHeight
                                        * GLRenderGlobals.getRelativeScaling()))
                        .setAnchor(Icon.ANCHOR_CENTER,
                                Icon.ANCHOR_CENTER)
                        .setColor(0, iconColor);
                m.setIcon(mib.build());
                m.setMetaDouble("minRenderScale", DEFAULT_RENDER_SCALE);
            } else
                m.setAlwaysShowText(true);

            m.addOnIconChangedListener(new Marker.OnIconChangedListener() {
                @Override
                public void onIconChanged(Marker marker) {
                    updateFeatureStyle(spatialDb, id, new IconPointStyle(
                            marker.getIconColor(),
                            marker.getIcon().getImageUri(Icon.STATE_DEFAULT)));
                }
            });

            item = m;
        } else if (geom instanceof LineString) {
            LineString line = (LineString) geom;

            GeoPointMetaData[] pts = new GeoPointMetaData[line.getNumPoints()];
            for (int i = 0; i < line.getNumPoints(); i++) {
                pts[i] = GeoPointMetaData
                        .wrap(new GeoPoint(line.getY(i), line.getX(i)));
            }

            Polyline poly;
            if (pts.length < 1) {
                Log.w(TAG,
                        "Empty LineString for " + feature.getName());
                return null;
            }

            poly = new Polyline(
                    MapItem.createSerialId(),
                    metadata,
                    uid);
            poly.setPoints(pts);// getCoordinates());
            poly.setTitle(title);
            applyStyle(poly, style);

            poly.addOnStyleChangedListener(new Shape.OnStyleChangedListener() {
                @Override
                public void onStyleChanged(Shape s) {
                    updateFeatureStyle(spatialDb, id, new BasicStrokeStyle(
                            s.getStrokeColor(), (float) s.getStrokeWeight()));
                }
            });
            poly.addOnStrokeColorChangedListener(
                    new Shape.OnStrokeColorChangedListener() {
                        @Override
                        public void onStrokeColorChanged(Shape s) {
                            updateFeatureStyle(spatialDb, id,
                                    new BasicStrokeStyle(s.getStrokeColor(),
                                            (float) s.getStrokeWeight()));
                        }
                    });
            poly.addOnStrokeWeightChangedListener(
                    new Shape.OnStrokeWeightChangedListener() {
                        @Override
                        public void onStrokeWeightChanged(Shape s) {
                            updateFeatureStyle(spatialDb, id,
                                    new BasicStrokeStyle(s.getStrokeColor(),
                                            (float) s.getStrokeWeight()));
                        }
                    });
            item = poly;

        } else if (geom instanceof Polygon) {
            LineString exteriorRing = ((Polygon) geom)
                    .getExteriorRing();
            if (exteriorRing == null) {
                Log.w(TAG,
                        "No exterior ring for " + feature.getName());
                return null;
            }

            GeoPointMetaData[] pts = new GeoPointMetaData[exteriorRing
                    .getNumPoints()];
            for (int i = 0; i < exteriorRing.getNumPoints(); i++) {
                pts[i] = GeoPointMetaData
                        .wrap(new GeoPoint(exteriorRing.getY(i),
                                exteriorRing.getX(i)));
            }

            Polyline poly;
            if (pts.length < 1) {
                Log.w(TAG,
                        "Empty LineString for " + feature.getName());
                return null;
            }

            poly = new Polyline(
                    MapItem.createSerialId(),
                    metadata,
                    uid);
            poly.setPoints(pts);// getCoordinates());
            if (title != null)
                poly.setTitle(title);

            poly.addStyleBits(Polyline.STYLE_CLOSED_MASK);
            applyStyle(poly, style);

            poly.addOnStrokeWeightChangedListener(
                    new Shape.OnStrokeWeightChangedListener() {
                        @Override
                        public void onStrokeWeightChanged(Shape p) {
                            updateFeatureStyle(spatialDb, id,
                                    new CompositeStyle(new Style[] {
                                            new BasicFillStyle(
                                                    p.getFillColor()),
                                            new BasicStrokeStyle(
                                                    p.getStrokeColor(),
                                                    (float) p
                                                            .getStrokeWeight()),
                            }));
                        }
                    });

            poly.addOnFillColorChangedListener(
                    new Shape.OnFillColorChangedListener() {
                        @Override
                        public void onFillColorChanged(Shape s) {
                            updateFeatureStyle(spatialDb, id,
                                    new CompositeStyle(new Style[] {
                                            new BasicFillStyle(
                                                    s.getFillColor()),
                                            new BasicStrokeStyle(
                                                    s.getStrokeColor(),
                                                    (float) s
                                                            .getStrokeWeight()),
                            }));
                        }
                    });

            item = poly;
        } else if (geom instanceof GeometryCollection) {
            final GeometryCollection c = (GeometryCollection) geom;

            MetaShape shape = new MetaShape(
                    MapItem.createSerialId(),
                    metadata,
                    uid) {

                @Override
                public GeoPoint[] getPoints() {
                    LinkedList<GeoPoint> pts = new LinkedList<>();
                    FeatureDataStoreDeepMapItemQuery.getPoints(c, pts);
                    return pts.toArray(new GeoPoint[0]);
                }

                @Override
                public GeoPointMetaData[] getMetaDataPoints() {
                    LinkedList<GeoPoint> pts = new LinkedList<>();
                    FeatureDataStoreDeepMapItemQuery.getPoints(c, pts);
                    return GeoPointMetaData.wrap(pts.toArray(new GeoPoint[0]));
                }

                @Override
                public GeoBounds getBounds(MutableGeoBounds bounds) {
                    return GeoBounds.createFromPoints(
                            this.getPoints());
                }
            };
            item = shape;
            shape.setTitle(title);
            shape.setMetaBoolean("collection", true);
            applyStyle(shape, style);
        } else {
            Log.w(TAG,
                    "Cannot create MapItem for Geometry Type: "
                            + geom.getClass().getSimpleName());
            return null;
        }

        item.setMetaLong("fid", id);
        item.setType(type);

        // ATAK-19810 Points radial menu is missing REDs/MSDs and 9-line icons
        // With the R8 obfuscator supplied with 8.12/AGP 8.8.2 when ofuscated
        // item instanceof PointMapItem returns false 
        // but 
        // item instanceof Marker returns true
        // note
        // in the unobfuscated build both return true
        //

        if (item instanceof PointMapItem || item instanceof Marker) {
            item.setRadialMenu("menus/feature_point_menu.xml");
            item.setMetaString("menu_point", ((PointMapItem) item)
                    .getPoint()
                    .toString());
        } else {
            item.setRadialMenu("menus/feature_menu.xml");
        }
        item.setTitle(title);
        item.setZOrder(item.getZOrder() + 1.0d);
        if (spatialDb == null || (spatialDb.getVisibilityFlags()
                & FeatureDataStore2.VISIBILITY_SETTINGS_FEATURE) == 0)
            item.setMetaBoolean("visibilityNotSupported", true);
        item.setMetaBoolean("drag", false);
        item.setMetaLong("atak.feature.version", feature.getVersion());

        final FeatureSet featureSet = (spatialDb != null)
                ? getFeatureSet(spatialDb, feature
                        .getFeatureSetId())
                : null;
        if (featureSet != null) {
            item.setMetaDouble("minMapGsd", featureSet.getMinResolution());
            item.setMetaString("featureSet", featureSet.getName());
            item.setMetaLong("fsid", featureSet.getId());
            File f = Utils.getSourceFile(spatialDb, featureSet);
            if (f != null)
                item.setMetaString("file", f.getAbsolutePath());
            item.setEditable(!featureSet.getType()
                    .equals(MvtSpatialDb.MVT_CONTENT_TYPE));
        }

        if (layer != null) {
            item.addOnMetadataChangedListener("labels_on",
                    new MapItem.OnMetadataChangedListener() {
                        @Override
                        public void onMetadataChanged(MapItem item,
                                String field) {
                            boolean labelsOn = item.hasMetaValue("labels_on");
                            if (item.hasMetaValue("fid")) {
                                long fid = item.getMetaLong("fid", 0);
                                MapView.getMapView().getRenderer3()
                                        .visitControl(
                                                layer,
                                                new Visitor<FeaturesLabelControl>() {
                                                    @Override
                                                    public void visit(
                                                            FeaturesLabelControl c) {
                                                        c.setShapeLabelVisible(
                                                                fid,
                                                                labelsOn);
                                                    }
                                                }, FeaturesLabelControl.class);
                            }
                        }
                    });
        }
        return item;
    }

    private static void updateFeatureStyle(FeatureDataStore2 spatialDb, long id,
            Style style) {
        if (spatialDb == null)
            return;
        try {
            spatialDb.updateFeature(id,
                    FeatureDataStore2.PROPERTY_FEATURE_STYLE, null,
                    null, style, null, 0);
        } catch (Exception e) {
            Log.e(TAG, "Update Feature Error", e);
        }
    }

    private static FeatureSet getFeatureSet(FeatureDataStore2 db, long fsid) {
        FeatureDataStore2.FeatureSetQueryParameters params = new FeatureDataStore2.FeatureSetQueryParameters();
        params.ids = Collections.singleton(fsid);
        params.limit = 1;

        FeatureSetCursor result = null;
        try {
            result = db.queryFeatureSets(params);
            if (!result.moveToNext())
                return null;
            return result.get();
        } catch (DataStoreException e) {
            Log.w(TAG, "Unexpected error querying for FeatureSet fsid=" + fsid,
                    e);
            return null;
        } finally {
            if (result != null)
                result.close();
        }
    }

    private void deepFindItemsImpl(Collection<MapItem> retval,
            QueryBuilder queryBuilder) {
        FeatureCursor result = null;
        Pair<MapItem, Feature> item;
        try {
            result = this.spatialDb.queryFeatures(queryBuilder
                    .buildQueryParams());
            while (result.moveToNext()) {
                final long id = result.getId();
                item = this.items.get(id);
                if (item != null
                        && item.first.getMetaLong("atak.feature.version",
                                FeatureDataStore2.FEATURE_VERSION_NONE) == result
                                        .getVersion()) {
                    retval.add(item.first);
                    continue;
                }

                Feature feature = result.get();
                item = Pair.create(featureToMapItem(feature), feature);
                if (item == null || item.first == null)
                    continue;

                // XXX - obtained from the FeatureSet !!!
                //                item.setMetaDouble("minMapGsd",
                //                        OSMUtils.mapnikTileResolution(result.getInt(4)));
                this.items.put(id, item);

                //add visibility listener e.g. toggle via radial menu
                // on second thought, it's better to leave the outline visible
                // so we can toggle visibility back on
                /*item.first
                        .addOnVisibleChangedListener(new MapItem.OnVisibleChangedListener() {
                            public void onVisibleChanged(MapItem item) {
                                FeatureDataStoreDeepMapItemQuery.this.spatialDb
                                        .setFeatureVisible(id,
                                                item.getVisible());
                            }
                        });*/
                retval.add(item.first);
            }
        } catch (DataStoreException e) {
            Log.w(TAG, "Unexpected error performing deep item query", e);
        } finally {
            if (result != null)
                result.close();
        }
    }

    private static String getAttributeAsString(AttributeSet attribs,
            String name, boolean raw) {
        Class<?> attribType = attribs.getAttributeType(name);
        if (attribType == null)
            return "";
        else if (attribType.equals(String.class))
            return attribs.getStringAttribute(name);
        else if (attribType.equals(Integer.TYPE)
                || attribType.equals(Integer.class))
            return String.valueOf(attribs.getIntAttribute(name));
        else if (attribType.equals(Double.TYPE)
                || attribType.equals(Double.class))
            return String.valueOf(attribs.getDoubleAttribute(name));
        else if (attribType.equals(Long.TYPE)
                || attribType.equals(Long.class)) {
            long val = attribs.getLongAttribute(name);
            if (raw)
                return String.valueOf(val);
            if ((name.toLowerCase(LocaleUtil.getCurrent()).contains("date") ||
                    name.toLowerCase(LocaleUtil.getCurrent()).contains("time"))
                    &&
                    val > DATE_CUTOFF) {
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy, HH:mm",
                        LocaleUtil.getCurrent());
                return sdf.format(new Date(val));
            } else
                return String.valueOf(val);
        } else
            return "";
    }

    @Override
    public SortedSet<MapItem> deepHitTest(MapView mapView,
            HitTestQueryParameters params,
            Map<Layer2, Collection<HitTestControl>> controls) {

        return deepHitTest(mapView, params, controls, layer,
                new HitTestVisitor2.ResultToMapItem<Long>() {
                    @Override
                    public void transform(Collection<Long> fids,
                            Collection<MapItem> results) {
                        QueryBuilder queryBuilder = new QueryBuilder();
                        queryBuilder.fids = fids;
                        deepFindItemsImpl(results, queryBuilder);
                    }
                }, Long.class);
    }

    protected static <T> SortedSet<MapItem> deepHitTest(MapView mapView,
            HitTestQueryParameters params,
            Map<Layer2, Collection<HitTestControl>> controls,
            Layer2 layer,
            HitTestVisitor2.ResultToMapItem<T> resultToMapItem,
            Class<T> controlResultType) {

        Collection<HitTestControl> ctrls = controls.get(layer);
        if (ctrls == null)
            return null;

        HitTestVisitor2<T> visitor = new HitTestVisitor2<>(
                mapView, params, resultToMapItem, controlResultType);

        for (HitTestControl c : ctrls)
            visitor.visit(c);

        if (visitor.error != null) {
            if (visitor.error instanceof RuntimeException)
                throw (RuntimeException) visitor.error;
            else
                throw new RuntimeException(visitor.error);
        }
        return visitor.result;
    }

    @Override
    public MapItem deepHitTest(int xpos, int ypos, GeoPoint point,
            MapView view) {
        SortedSet<MapItem> items = deepHitTestItemsImpl(xpos, ypos, point, view,
                1);
        return items != null ? items.first() : null;
    }

    @Override
    public SortedSet<MapItem> deepHitTestItems(int xpos, int ypos,
            GeoPoint point, MapView view) {
        return deepHitTestItemsImpl(xpos, ypos, point, view,
                MapTouchController.MAXITEMS);
    }

    /**
     * @deprecated Use {@link #deepHitTest(MapView, HitTestQueryParameters)}
     */
    @Deprecated
    @DeprecatedApi(since = "4.4", forRemoval = true, removeAt = "4.7")
    protected SortedSet<MapItem> deepHitTestItemsImpl(int xpos, int ypos,
            GeoPoint point, MapView view, int limit) {

        final HitTestQueryParameters params = new HitTestQueryParameters(
                view.getGLSurface(), xpos, ypos,
                MapRenderer2.DisplayOrigin.UpperLeft);
        params.geo.set(point);
        params.limit = limit;

        final SortedSet<MapItem>[] result = new SortedSet[1];
        view.getGLSurface().getGLMapView().visitControls(
                this.layer, new Visitor<Iterator<MapControl>>() {
                    @Override
                    public void visit(Iterator<MapControl> iter) {
                        Collection<HitTestControl> layerControls = new HashSet<>();
                        while (iter.hasNext()) {
                            final MapControl ctrl = iter.next();
                            if (ctrl instanceof HitTestControl)
                                layerControls.add((HitTestControl) ctrl);
                        }
                        if (!layerControls.isEmpty()) {
                            result[0] = deepHitTest(view, params,
                                    Collections.singletonMap(layer,
                                            layerControls));
                        }
                    }
                });

        return (result[0] != null) ? result[0]
                : new TreeSet<>(MapItem.ZORDER_HITTEST_COMPARATOR);
    }

    private long getFeatureId(Map<String, String> metadata) {
        if (metadata != null) {
            String featureId;
            if (metadata.containsKey("uid")) {
                final String uid = metadata.get("uid");
                if (uid != null && uid.startsWith(this.uidPrefix + "::")) {
                    featureId = uid.substring(uid.lastIndexOf(':') + 1);
                    if (featureId.matches("(\\-)?\\d+"))
                        return Long.parseLong(featureId);
                }
            } else if (metadata.containsKey("featureId")) {
                featureId = metadata.get("featureId");
                if (featureId != null && featureId.matches("(\\-)?\\d+")) {
                    return Long.parseLong(featureId);
                }
            }
        }

        return 0L;
    }

    /** @deprecated use {@link #getFeatureUID(FeatureDataStore2, long)} */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    protected static String getFeatureUID(FeatureDataStore spatialDb,
            long featureId) {
        return getFeatureUID(Adapters.adapt(spatialDb), featureId);
    }

    protected static String getFeatureUID(FeatureDataStore2 spatialDb,
            long featureId) {
        return "spatialdb::" + spatialDb.getUri() + "::" + featureId;
    }

    private static void applyStyle(Shape shape, Style style) {
        if (style == null) {
            shape.setStrokeColor(Color.WHITE);
            shape.setFillColor(0);
        } else if (style instanceof BasicStrokeStyle) {
            shape.setStrokeColor(((BasicStrokeStyle) style).getColor());
            shape.setStrokeWeight(((BasicStrokeStyle) style).getStrokeWidth());

            shape.addStyleBits(Polyline.STYLE_OUTLINE_STROKE_MASK);
        } else if (style instanceof BasicFillStyle) {
            shape.setFillColor(((BasicFillStyle) style).getColor());

            shape.addStyleBits(Shape.STYLE_FILLED_MASK);
        } else if (style instanceof CompositeStyle) {
            for (int i = 0; i < ((CompositeStyle) style).getNumStyles(); i++)
                applyStyle(shape, ((CompositeStyle) style).getStyle(i));
        }
    }

    /**************************************************************************/

    static private class QueryBuilder {
        Collection<Long> fids = null;
        long groupId = 0L;
        GeoPoint location = null;
        GeoBounds bounds = null;
        double radius = Double.NaN;
        boolean orderByDistance = false;
        boolean visibleOnly = false;
        int limit = 0;

        public FeatureDataStore2.FeatureQueryParameters buildQueryParams() {
            FeatureDataStore2.FeatureQueryParameters retval = new FeatureDataStore2.FeatureQueryParameters();
            retval.featureSetFilter = new FeatureDataStore2.FeatureSetQueryParameters();
            retval.ignoredFeatureProperties = FeatureDataStore2.PROPERTY_FEATURE_ATTRIBUTES;

            if (this.groupId != 0L) {
                retval.featureSetFilter.ids = Collections
                        .singleton(this.groupId);
            }

            if (this.fids != null) {
                retval.ids = new HashSet<>(fids);
            }

            if (this.location != null && !Double.isNaN(this.radius)) {
                LineString ring = new LineString(2);
                for (int i = 0; i < 360; i += 10) {
                    GeoPoint proj = GeoCalculations
                            .pointAtDistance(this.location, i, this.radius);
                    ring.addPoint(proj.getLongitude(), proj.getLatitude());
                }
                ring.addPoint(ring.getX(0), ring.getY(0));
                retval.spatialFilter = new Polygon(ring);
            } else if (this.bounds != null) {
                double east = this.bounds.getEast();
                double west = this.bounds.getWest();
                if (this.bounds.crossesIDL()) {
                    east = this.bounds.getWest() + 360;
                    west = this.bounds.getEast();
                }
                retval.spatialFilter = GeometryFactory.fromEnvelope(
                        new Envelope(west, this.bounds.getSouth(), 0d,
                                east, this.bounds.getNorth(), 0d));
            }

            if (this.orderByDistance && this.location != null) {
                retval.order = Collections.singleton(
                        new FeatureDataStore2.FeatureQueryParameters.Order.Distance(
                                this.location));
            }

            retval.visibleOnly = this.visibleOnly;

            if (this.limit != 0)
                retval.limit = this.limit;

            return retval;
        }
    }

    protected static class HitTestVisitor2<V>
            implements Visitor<HitTestControl> {

        public interface ResultToMapItem<HitTestResult> {
            void transform(Collection<HitTestResult> results,
                    Collection<MapItem> items);
        }

        private final MapView mapView;
        private final HitTestQueryParameters params;
        private final ResultToMapItem<V> resultToMapItem;
        private final Class<V> resultClazz;

        SortedSet<MapItem> result;
        Throwable error;

        public HitTestVisitor2(MapView mapView, HitTestQueryParameters params,
                ResultToMapItem<V> resultToMapItem, Class<V> resultClazz) {
            this.mapView = mapView;
            this.params = params;
            this.resultToMapItem = resultToMapItem;
            this.resultClazz = resultClazz;
            this.result = null;
        }

        @Override
        public void visit(HitTestControl c) {
            try {

                // Must accept map item results
                if (!this.params.acceptsResult(MapItem.class))
                    return;

                Set<V> controlResults = new HashSet<>();
                GLMapView glmv = mapView.getGLSurface().getGLMapView();

                HitTestQueryParameters params = new HitTestQueryParameters(
                        this.params);
                params.resultFilter = new ClassResultFilter(resultClazz);
                if (c instanceof HitTestControl) {
                    List<HitTestResult> results = new ArrayList<>();
                    ((HitTestControl) c).hitTest(glmv, params, results);
                    for (HitTestResult result : results) {
                        if (resultClazz
                                .isAssignableFrom(result.subject.getClass()))
                            controlResults.add((V) result.subject);
                    }
                }

                if (this.result == null)
                    this.result = new TreeSet<>(
                            MapItem.ZORDER_HITTEST_COMPARATOR);

                // XXX - need to invalidate on datastore changed as version may
                //       have been bumped
                /*                
                                Iterator<Long> iter = fids.iterator();
                                while (iter.hasNext()) {
                                    Long fid = iter.next();
                                    Pair<MapItem, Feature> entry = items.get(fid);
                                    if (entry != null && entry.first != null) {
                                        entry.first.setMetaString("menu_point",
                                                point.toString());
                                        this.result.add(entry.first);
                                        iter.remove();
                                    }
                                }
                */
                if (!controlResults.isEmpty()) {
                    Collection<MapItem> results = new LinkedList<>();
                    resultToMapItem.transform(controlResults, results);
                    for (MapItem item : results) {
                        item.setClickPoint(params.geo);
                        this.result.add(item);
                    }
                }
            } catch (Throwable t) {
                this.error = t;
            }
        }
    }

    /**************************************************************************/

    private static void getPoints(GeometryCollection geom,
            Collection<GeoPoint> points) {
        Collection<Geometry> children = geom.getGeometries();
        for (Geometry child : children) {
            if (child instanceof Point) {
                points.add(new GeoPoint(((Point) child).getY(),
                        ((Point) child).getX()));
            } else if (child instanceof LineString) {
                getPoints((LineString) child, points);
            } else if (child instanceof Polygon) {
                Polygon poly = (Polygon) child;
                if (poly.getExteriorRing() != null)
                    getPoints(poly.getExteriorRing(), points);
                for (LineString ring : poly.getInteriorRings())
                    getPoints(ring, points);
            } else if (child instanceof GeometryCollection) {
                getPoints((GeometryCollection) child, points);
            }
        }
    }

    private static void getPoints(LineString linestring,
            Collection<GeoPoint> points) {
        for (int i = 0; i < linestring.getNumPoints(); i++)
            points.add(new GeoPoint(linestring.getY(i), linestring.getX(i)));
    }

    static MetaDataHolder2 attributesToMetadata(AttributeSet attribs,
            MetaDataHolder2 metadata, String title) {
        StringBuilder remarks = new StringBuilder();
        StringBuilder html = new StringBuilder();

        final String nameKey = findNameAttribute(attribs);

        String htmlAttribute = null;
        if (attribs.containsAttribute("html"))
            htmlAttribute = attribs
                    .getStringAttribute("html");

        if (!FileSystemUtils.isEmpty(htmlAttribute)) {
            String str = htmlAttribute;
            if (str != null) {
                // XXX - a little hacky -- replace newlines with HTML line
                //       breaks if the content does not contain tags
                if (str.indexOf('<') < 0
                        && str.indexOf('>') < 0)
                    str = str.replace("\n", "<BR>");
            }
            html.append(str);
            remarks.append(html);
        } else {
            html.append(
                    "<html><body><table border=\"1\"><tr><th>Field Name</th><th>Field Value</th></tr>");
            for (String s : attribs.getAttributeNames()) {
                // only emit a "name*" attribute if there is no title or the
                // attribute is the primary name attribute
                if (s.startsWith("name") &&
                        ((title != null) ||
                                (nameKey != null && !nameKey.equals(s)))) {

                    continue;
                }
                remarks.append(s);
                remarks.append(": ");
                remarks.append(getAttributeAsString(
                        attribs, s, true));
                remarks.append("\n");

                html.append("<tr><td>");
                html.append(s);
                html.append("</td><td>");
                html.append(
                        getAttributeAsString(attribs, s,
                                false));
                html.append("</td></tr>");
            }
            html.append("</table></body></html>");
        }
        metadata.setMetaString("html", html.toString());
        metadata.setMetaString("remarks", remarks.toString());

        if (attribs.containsAttribute(".ATTACHMENT_URIS")) {
            try {
                String[] attachments = attribs
                        .getStringArrayAttribute(
                                ".ATTACHMENT_URIS");

                if (attachments.length > 0) {
                    metadata.setMetaStringArrayList(
                            "attachments",
                            new ArrayList<>(
                                    Arrays
                                            .asList(attachments)));
                }
            } catch (Exception e) {
                Log.w(TAG,
                        "Failed to get .ATTACHMENT_URIS attribute");
            }
        }

        if (title != null)
            metadata.setMetaString("title", title);
        else if (nameKey != null && !FileSystemUtils
                .isEmpty(attribs.getStringAttribute(nameKey)))
            metadata.setMetaString("title",
                    attribs.getStringAttribute(nameKey));
        return metadata;
    }

    private static class DeferredFeatureMetadata extends FilterMetaDataHolder {

        private final FeatureDataStore2 dataStore;
        private final long fid;
        private boolean metadataQueried;
        private final String title;

        public DeferredFeatureMetadata(FeatureDataStore2 dataStore, long fid,
                String title) {
            super(new DefaultMetaDataHolder());

            if (!FileSystemUtils.isEmpty(title))
                this.setMetaString("title", title);
            this.dataStore = dataStore;
            this.fid = fid;
            this.title = title;
            this.metadataQueried = false;
        }

        private void queryMetadataNoSync() {
            if (!this.metadataQueried) {
                FeatureQueryParameters params = new FeatureQueryParameters();
                params.ids = Collections.singleton(this.fid);
                params.ignoredFeatureProperties = FeatureDataStore2.PROPERTY_FEATURE_GEOMETRY
                        |
                        FeatureDataStore3.PROPERTY_FEATURE_ALTITUDE_MODE |
                        FeatureDataStore3.PROPERTY_FEATURE_EXTRUDE |
                        FeatureDataStore2.PROPERTY_FEATURE_NAME |
                        FeatureDataStore2.PROPERTY_FEATURE_STYLE;
                params.limit = 1;

                FeatureCursor result = null;
                try {
                    result = this.dataStore.queryFeatures(params);
                    if (result.moveToNext()) {
                        AttributeSet attribs = result.getAttributes();
                        if (attribs != null)
                            attributesToMetadata(attribs, this, title);
                    }
                } catch (DataStoreException e) {
                    Log.w(TAG,
                            "Unexpected exception querying for feature metadata (FID="
                                    + fid + ")",
                            e);
                } finally {
                    if (result != null)
                        result.close();
                }

                this.metadataQueried = true;
            }
        }

        private boolean needsQuery(String key) {
            return !this.hasMetaValue(key) &&
                    !this.metadataQueried &&
                    ATTRIBUTE_METADATA_KEYS.contains(key);
        }

        @Override
        public synchronized String getMetaString(String key,
                String fallbackValue) {
            if (needsQuery(key))
                this.queryMetadataNoSync();
            return super.getMetaString(key, fallbackValue);
        }

        @Override
        public synchronized int getMetaInteger(String key, int fallbackValue) {
            if (needsQuery(key))
                this.queryMetadataNoSync();
            return super.getMetaInteger(key, fallbackValue);
        }

        @Override
        public synchronized double getMetaDouble(String key,
                double fallbackValue) {
            if (needsQuery(key))
                this.queryMetadataNoSync();
            return super.getMetaDouble(key, fallbackValue);
        }

        @Override
        public synchronized long getMetaLong(String key, long fallbackValue) {
            if (needsQuery(key))
                this.queryMetadataNoSync();
            return super.getMetaLong(key, fallbackValue);
        }

        @Override
        public synchronized boolean getMetaBoolean(String key,
                boolean fallbackValue) {
            if (needsQuery(key))
                this.queryMetadataNoSync();
            return super.getMetaBoolean(key, fallbackValue);
        }

        @Override
        public synchronized ArrayList<String> getMetaStringArrayList(
                String key) {
            if (needsQuery(key))
                this.queryMetadataNoSync();
            return super.getMetaStringArrayList(key);
        }

        @Override
        public synchronized int[] getMetaIntArray(String key) {
            if (needsQuery(key))
                this.queryMetadataNoSync();
            return super.getMetaIntArray(key);
        }
    }
}
