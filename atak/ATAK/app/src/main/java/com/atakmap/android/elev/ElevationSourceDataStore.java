
package com.atakmap.android.elev;

import android.util.Pair;

import com.atakmap.android.layers.LayerZOrderControl;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.elevation.ElevationSourceManager;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.GeometryFactory;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.ImageDatasetDescriptor;
import com.atakmap.map.layer.raster.RasterDataStore;
import com.atakmap.map.layer.raster.RuntimeRasterDataStore;
import com.atakmap.map.layer.raster.mosaic.MosaicDatabase2;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.map.layer.raster.tilematrix.TileClient;
import com.atakmap.map.layer.raster.tilematrix.TileContainer;
import com.atakmap.map.layer.raster.tilematrix.TileMatrix;
import com.atakmap.math.MathUtils;
import com.atakmap.math.Rectangle;
import com.atakmap.util.Collections2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

final class ElevationSourceDataStore implements
        RasterDataStore,
        LayerZOrderControl,
        ElevationSourceManager.OnSourcesChangedListener {

    Set<Pair<ElevationSource, Collection<DatasetDescriptor>>> _sources = Collections
            .newSetFromMap(new ConcurrentHashMap<>());
    Set<OnDataStoreContentChangedListener> _listeners = Collections
            .newSetFromMap(new ConcurrentHashMap<>());
    Set<OnSelectionZOrderChangedListener> _zOrderListeners = Collections
            .newSetFromMap(new ConcurrentHashMap<>());

    Map<String, Integer> _zorder = new ConcurrentHashMap<>();

    ElevationSourceDataStore() {
        ElevationSourceManager.addOnSourcesChangedListener(this);
        Collection<ElevationSource> sources = new LinkedList<>();
        ElevationSourceManager.getSources(sources);
        for (ElevationSource source : sources)
            onSourceAttached(source);
    }

    @Override
    public DatasetDescriptorCursor queryDatasets() {
        return queryDatasets(null);
    }

    @Override
    public DatasetDescriptorCursor queryDatasets(
            DatasetQueryParameters params) {
        return new CursorImpl(params, _sources);
    }

    @Override
    public int queryDatasetsCount(DatasetQueryParameters params) {
        int count = 0;
        try (DatasetDescriptorCursor result = queryDatasets(params)) {
            while (result.moveToNext())
                count++;
        }
        return count;
    }

    @Override
    public Collection<String> getDatasetNames() {
        Collection<String> names = new HashSet<>();
        for (Pair<ElevationSource, Collection<DatasetDescriptor>> desc : _sources)
            names.add(desc.first.getName());
        return names;
    }

    @Override
    public Collection<String> getImageryTypes() {
        return getDatasetNames();
    }

    @Override
    public Collection<String> getDatasetTypes() {
        return getDatasetNames();
    }

    @Override
    public Collection<String> getProviders() {
        return Collections.singleton("elevation");
    }

    @Override
    public Geometry getCoverage(String dataset, String type) {
        // XXX -
        return GeometryFactory
                .fromEnvelope(new Envelope(-180d, -90d, 0d, 180d, 90d, 0d));
    }

    @Override
    public double getMinimumResolution(String dataset, String type) {
        // XXX -
        return OSMUtils.mapnikTileResolution(0);
    }

    @Override
    public double getMaximumResolution(String dataset, String type) {
        // XXX -
        return OSMUtils.mapnikTileResolution(20);
    }

    @Override
    public void refresh() {
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void addOnDataStoreContentChangedListener(
            OnDataStoreContentChangedListener l) {
        _listeners.add(l);
    }

    @Override
    public void removeOnDataStoreContentChangedListener(
            OnDataStoreContentChangedListener l) {
        _listeners.remove(l);
    }

    @Override
    public void dispose() {
        ElevationSourceManager.removeOnSourcesChangedListener(this);
    }

    @Override
    public void onSourceAttached(ElevationSource src) {
        for (Pair<ElevationSource, Collection<DatasetDescriptor>> entry : _sources)
            if (entry.first == src)
                return;

        String sourcePath = src.getName();
        final Controls ctrls = (src instanceof Controls) ? (Controls) src
                : null;
        final TileMatrix tiles = (ctrls != null)
                ? ctrls.getControl(TileMatrix.class)
                : null;
        Map<String, String> extras = new HashMap<>();
        if (tiles != null) {
            final Controls tilesCtrls = (tiles instanceof Controls)
                    ? (Controls) tiles
                    : null;
            final TileContainer cache = (tilesCtrls != null)
                    ? tilesCtrls.getControl(TileContainer.class)
                    : null;
            final File offlineCache = getFile(cache);
            if (offlineCache != null)
                extras.put("offlineCache", offlineCache.getAbsolutePath());
            extras.put("_levelCount",
                    String.valueOf(tiles.getZoomLevel().length));
            final File sourceFile = getFile(tiles);
            ;
            if (sourceFile != null)
                sourcePath = sourceFile.getAbsolutePath();
        }

        ArrayList<DatasetDescriptor> desc = new ArrayList<>(
                extras.containsKey("offlineCache") ? 2 : 1);
        desc.add(new ImageDatasetDescriptor(
                src.getName(),
                sourcePath,
                "elevation",
                src.getName(),
                src.getName(),
                0, 0,
                (tiles != null)
                        ? tiles.getZoomLevel()[tiles.getZoomLevel().length
                                - 1].resolution
                        : OSMUtils.mapnikTileResolution(20),
                (tiles != null) ? tiles.getZoomLevel().length : 21,
                new GeoPoint(90, -180),
                new GeoPoint(90, 180),
                new GeoPoint(-90, 180),
                new GeoPoint(-90, -180),
                4326,
                tiles instanceof TileClient,
                null,
                extras));
        if (extras.containsKey("offlineCache")) {
            desc.add(new ImageDatasetDescriptor(
                    src.getName(),
                    extras.get("offlineCache"),
                    "elevation",
                    src.getName(),
                    src.getName(),
                    0, 0,
                    (tiles != null)
                            ? tiles.getZoomLevel()[tiles.getZoomLevel().length
                                    - 1].resolution
                            : OSMUtils.mapnikTileResolution(20),
                    (tiles != null) ? tiles.getZoomLevel().length : 21,
                    new GeoPoint(90, -180),
                    new GeoPoint(90, 180),
                    new GeoPoint(-90, 180),
                    new GeoPoint(-90, -180),
                    4326,
                    false,
                    null,
                    Collections.emptyMap()));
        }
        for (DatasetDescriptor d : desc)
            d.setLocalData("elevationSource", src);

        final Pair<ElevationSource, Collection<DatasetDescriptor>> entry = Pair
                .create(src, desc);
        refreshZOrder(entry);
        _sources.add(entry);

        for (OnDataStoreContentChangedListener l : _listeners)
            l.onDataStoreContentChanged(this);
    }

    @Override
    public void onSourceDetached(ElevationSource src) {
        Iterator<Pair<ElevationSource, Collection<DatasetDescriptor>>> iter = _sources
                .iterator();
        while (iter.hasNext()) {
            if (iter.next().first == src) {
                iter.remove();
                if (!_zorder.isEmpty())
                    refreshZOrder(null);
                for (OnDataStoreContentChangedListener l : _listeners)
                    l.onDataStoreContentChanged(this);
                break;
            }
        }
    }

    void refreshZOrder(
            Pair<ElevationSource, Collection<DatasetDescriptor>> source) {
        ArrayList<Pair<ElevationSource, Collection<DatasetDescriptor>>> sources = new ArrayList<>(
                _sources);
        final Map<String, Integer> zorder = new HashMap<>(_zorder);
        final Map<String, SourceTraits> traits = new HashMap<>();
        for (Pair<ElevationSource, Collection<DatasetDescriptor>> entry : sources) {
            SourceTraits st = traits.get(entry.first.getName());
            if (st == null)
                traits.put(entry.first.getName(),
                        new SourceTraits(entry.first));
            else
                st.append(entry.first);
        }
        if (source != null) {
            if (traits.containsKey(source.first.getName()))
                traits.get(source.first.getName()).append(source.first);
            else
                traits.put(source.first.getName(),
                        new SourceTraits(source.first));
        }

        // insert if there is either full or no zorder coverage for all sources
        final boolean insert = !zorder.keySet().containsAll(traits.keySet())
                && Collections2.containsAny(zorder.keySet(), traits.keySet());
        if (!insert && source != null)
            sources.add(source);

        // sort sources
        Collections.sort(sources,
                new Comparator<Pair<ElevationSource, Collection<DatasetDescriptor>>>() {
                    @Override
                    public int compare(
                            Pair<ElevationSource, Collection<DatasetDescriptor>> a,
                            Pair<ElevationSource, Collection<DatasetDescriptor>> b) {
                        Integer za = zorder.get(a.first.getName());
                        Integer zb = zorder.get(b.first.getName());
                        if (za == null || zb == null) {
                            SourceTraits ta = traits.get(a.first.getName());
                            SourceTraits tb = traits.get(b.first.getName());
                            return ta.compareTo(tb);
                        } else if (za != null && zb != null
                                && za.compareTo(zb) != 0) {
                            return za.compareTo(zb);
                        }
                        final int strcmp = a.first.getName()
                                .compareToIgnoreCase(b.first.getName());
                        return (strcmp != 0) ? strcmp
                                : a.first.hashCode() - b.first.hashCode();
                    }
                });

        // insert the source into the sorted list
        if (insert && source != null) {
            final SourceTraits it = traits.get(source.first.getName());
            boolean inserted = false;
            for (int i = 0; i < sources.size(); i++) {
                final SourceTraits ct = traits
                        .get(sources.get(i).first.getName());
                // insert
                if (it.compareTo(ct) <= 0) {
                    sources.add(i, source);
                    inserted = true;
                    break;
                }
            }
            // append
            if (!inserted)
                sources.add(source);
        }

        _zorder.clear();
        int z = 0;
        for (Pair<ElevationSource, Collection<DatasetDescriptor>> entry : sources)
            if (!_zorder.containsKey(entry.first.getName()))
                _zorder.put(entry.first.getName(), z++);
    }

    static class SourceTraits implements Comparable<SourceTraits> {
        enum Category {
            DSM,
            Tiles,
            DTED,
            SRTM,
            Unknown,
        };

        boolean streaming;
        double maxResolution;
        Category category;
        String name;

        SourceTraits(ElevationSource source) {
            name = source.getName();
            category = Category.Unknown;
            if (source.getName().equals("DTED")) {
                category = Category.DTED;
                maxResolution = 30d;
            } else if (source.getName().equals("SRTM")) {
                category = Category.SRTM;
                maxResolution = 30d;
            } else if (source.getName().equals("Surface Models")) {
                category = Category.DSM;
            } else if (source instanceof Controls) {
                final Controls controls = (Controls) source;
                final TileMatrix tiles = controls.getControl(TileMatrix.class);
                final MosaicDatabase2 mosaicdb = controls
                        .getControl(MosaicDatabase2.class);
                if (tiles != null) {
                    streaming = (tiles instanceof TileClient);
                    category = Category.Tiles;
                    final TileMatrix.ZoomLevel[] zoomLevels = tiles
                            .getZoomLevel();
                    maxResolution = zoomLevels[zoomLevels.length
                            - 1].resolution;
                } else if (mosaicdb != null) {
                    if (mosaicdb.getType().equals("dsm"))
                        category = Category.DSM;
                }
            }
        }

        boolean append(ElevationSource source) {
            if (!source.getName().equals(name))
                return false;
            SourceTraits other = new SourceTraits(source);
            streaming |= other.streaming;
            if (Double.isNaN(maxResolution)
                    || (!Double.isNaN(other.maxResolution)
                            && other.maxResolution < maxResolution))
                maxResolution = other.maxResolution;
            return true;
        }

        @Override
        public int compareTo(SourceTraits other) {
            if ((category.ordinal() - other.category.ordinal()) != 0)
                return (category.ordinal() - other.category.ordinal());
            if (category == Category.Tiles && streaming != other.streaming)
                return streaming ? 1 : -1;
            else if (!Double.isNaN(maxResolution)
                    && !Double.isNaN(other.maxResolution))
                return Double.compare(maxResolution, other.maxResolution);
            else if (Double.isNaN(maxResolution)
                    && !Double.isNaN(other.maxResolution))
                return 1;
            else if (!Double.isNaN(maxResolution)
                    && Double.isNaN(other.maxResolution))
                return -1;
            return name.compareToIgnoreCase(other.name);
        }
    }

    @Override
    public int getPosition(String selection) {
        if (selection == null)
            return Integer.MAX_VALUE;
        Integer z = _zorder.get(selection);
        return (z != null) ? z.intValue() : Integer.MAX_VALUE;
    }

    @Override
    public void setPosition(String selection, int z) {
        if (!_zorder.containsKey(selection))
            return;
        ArrayList<String> zorder = new ArrayList<>(_zorder.keySet());
        Collections.sort(zorder, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                final int aidx = _zorder.get(a);
                final int bidx = _zorder.get(b);
                return aidx - bidx;
            }
        });
        z = MathUtils.clamp(z, 0, zorder.size() - 1);
        zorder.remove(_zorder.get(selection).intValue());
        zorder.add(z, selection);
        _zorder.clear();
        for (int i = 0; i < zorder.size(); i++)
            _zorder.put(zorder.get(i), i);

        for (OnSelectionZOrderChangedListener l : _zOrderListeners)
            l.onSelectionZOrderChanged(this);
    }

    @Override
    public void addOnSelectionZOrderChangedListener(
            OnSelectionZOrderChangedListener l) {
        _zOrderListeners.add(l);
    }

    @Override
    public void removeOnSelectionZOrderChangedListener(
            OnSelectionZOrderChangedListener l) {
        _zOrderListeners.remove(l);
    }

    static File getFile(TileMatrix tiles) {
        if (tiles == null)
            return null;
        if (!(tiles instanceof Controls))
            return null;
        final Controls ctrls = (Controls) tiles;
        return ctrls.getControl(File.class);
    }

    static boolean matches(DatasetQueryParameters filter,
            DatasetDescriptor desc) {
        if (filter == null)
            return true;
        if (filter.datasetTypes != null
                && !filter.datasetTypes.contains(desc.getDatasetType()))
            return false;
        if (filter.imageryTypes != null && !Collections2
                .containsAny(filter.imageryTypes, desc.getImageryTypes()))
            return false;
        if (filter.names != null && !filter.names.contains(desc.getName()))
            return false;
        if (filter.providers != null
                && !filter.providers.contains(desc.getProvider()))
            return false;
        if (filter.spatialFilter != null) {
            if (filter.spatialFilter instanceof DatasetQueryParameters.PointSpatialFilter) {
                final DatasetQueryParameters.PointSpatialFilter p = (DatasetQueryParameters.PointSpatialFilter) filter.spatialFilter;
                final Envelope e = desc.getCoverage(null).getEnvelope();
                if (!Rectangle.contains(e.minX, e.minY, e.maxX, e.maxY,
                        p.point.getLongitude(), p.point.getLatitude()))
                    return false;
            } else if (filter.spatialFilter instanceof RasterDataStore.DatasetQueryParameters.RegionSpatialFilter) {
                final DatasetQueryParameters.RegionSpatialFilter r = (DatasetQueryParameters.RegionSpatialFilter) filter.spatialFilter;
                final Envelope e = desc.getCoverage(null).getEnvelope();
                if (!Rectangle.intersects(e.minX, e.minY, e.maxX, e.maxY,
                        r.upperLeft.getLongitude(), r.lowerRight.getLatitude(),
                        r.lowerRight.getLongitude(), r.upperLeft.getLatitude()))
                    return false;
            } else {
                return false;
            }
        }
        if (!Double.isNaN(filter.maxGsd)
                && desc.getMaxResolution(null) > filter.maxGsd)
            return false;
        if (!Double.isNaN(filter.minGsd)
                && desc.getMinResolution(null) < filter.minGsd)
            return false;
        if (filter.remoteLocalFlag != null
                && (filter.remoteLocalFlag == DatasetQueryParameters.RemoteLocalFlag.REMOTE) != desc
                        .isRemote())
            return false;

        return true;
    }

    final static class CursorImpl implements DatasetDescriptorCursor {

        final Iterator<DatasetDescriptor> _iter;
        final DatasetQueryParameters _filter;
        DatasetDescriptor _row;

        CursorImpl(final DatasetQueryParameters filter,
                Collection<Pair<ElevationSource, Collection<DatasetDescriptor>>> descs) {
            ArrayList<DatasetDescriptor> rows = new ArrayList<>(descs.size());
            for (Pair<ElevationSource, Collection<DatasetDescriptor>> desc : descs)
                rows.addAll(desc.second);
            if (filter != null && filter.order != null) {
                Collections.sort(rows, new Comparator<DatasetDescriptor>() {
                    @Override
                    public int compare(DatasetDescriptor a,
                            DatasetDescriptor b) {
                        for (DatasetQueryParameters.Order order : filter.order) {
                            Comparator<DatasetDescriptor> impl = RuntimeRasterDataStore.SORT_IDENTITY;
                            if (order instanceof DatasetQueryParameters.GSD)
                                impl = RuntimeRasterDataStore.SORT_GSD;
                            else if (order instanceof DatasetQueryParameters.Name)
                                impl = RuntimeRasterDataStore.SORT_NAME;
                            else if (order instanceof DatasetQueryParameters.Provider)
                                impl = RuntimeRasterDataStore.SORT_PROVIDER;
                            else if (order instanceof DatasetQueryParameters.Type)
                                impl = RuntimeRasterDataStore.SORT_DATASET_TYPE;

                            final int c = impl.compare(a, b);
                            if (c != 0)
                                return c;
                        }
                        return RuntimeRasterDataStore.SORT_IDENTITY.compare(a,
                                b);
                    }
                });
            }

            _filter = filter;
            _iter = rows.iterator();
        }

        @Override
        public boolean moveToNext() {
            _row = null;
            while (_iter.hasNext()) {
                _row = _iter.next();
                if (!matches(_filter, _row)) {
                    _row = null;
                    continue;
                }
                return true;
            }
            return false;
        }

        @Override
        public void close() {
            while (moveToNext())
                ;
            _row = null;
        }

        @Override
        public boolean isClosed() {
            return (_row == null && !_iter.hasNext());
        }

        @Override
        public DatasetDescriptor get() {
            return _row;
        }
    }
}
