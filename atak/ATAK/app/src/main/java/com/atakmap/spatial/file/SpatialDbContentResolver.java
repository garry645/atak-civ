
package com.atakmap.spatial.file;

import com.atakmap.android.data.FileContentResolver;
import com.atakmap.android.data.URIContentHandler;
import com.atakmap.android.data.URIQueryParameters;
import com.atakmap.android.features.FeatureDataStoreUtils;
import com.atakmap.android.hierarchy.filters.FOVFilter;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.atakmap.map.layer.feature.Adapters;
import com.atakmap.map.layer.feature.DataSourceFeatureDataStore;
import com.atakmap.map.layer.feature.FeatureCursor;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.FeatureSetCursor;
import com.atakmap.map.layer.feature.PersistentDataSourceFeatureDataStore3;
import com.atakmap.map.layer.feature.control.DataSourceDataStoreControl;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.GeometryFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class SpatialDbContentResolver extends FileContentResolver {

    private static final String TAG = "SpatialDbContentResolver";

    protected final MapView _mapView;
    protected final DataSourceFeatureDataStore __database;
    protected final FeatureDataStore2 _datastore;
    private final DataSourceDataStoreControl _datasource;

    protected SpatialDbContentResolver(final MapView mv,
            final DataSourceFeatureDataStore db, final Set<String> validExts) {
        this(mv, Adapters.adapt(db), db, validExts);
    }

    protected SpatialDbContentResolver(final MapView mv,
            final FeatureDataStore2 db, final Set<String> validExts) {
        this(mv, db, null, validExts);
    }

    private SpatialDbContentResolver(final MapView mv,
            final FeatureDataStore2 ds,
            final DataSourceFeatureDataStore db,
            final Set<String> validExts) {
        super(validExts);
        _mapView = mv;
        _datastore = ds;
        __database = db;
        _datasource = SpatialDbContentSource.getDataSource(ds);
    }

    /**
     * Perform a scan for existing files in the database and create handlers
     * Only to be called during initialization
     *
     * @param source Content source used for checking if a file matches
     */
    public void scan(SpatialDbContentSource source) {
        DataSourceDataStoreControl.FileCursor c = null;
        try {
            c = _datasource.queryFiles();
            if (c != null) {
                while (c.moveToNext()) {
                    final File f = c.getFile();
                    if (f != null) {
                        if (source.processAccept(f,
                                0) == SpatialDbContentSource.PROCESS_ACCEPT)
                            addHandler(f);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to scan files", e);
        } finally {
            if (c != null)
                c.close();
        }
    }

    /**
     * Add or update a content handler
     *
     * @param f Content file
     */
    public void addHandler(File f) {
        Envelope.Builder bounds = new Envelope.Builder();
        List<Long> ids = new ArrayList<>();
        FeatureSetCursor fsc = null;
        try {
            // Get all feature sets for this file
            final boolean bruteForce = !(_datastore instanceof PersistentDataSourceFeatureDataStore3);
            if (!bruteForce)
                fsc = ((PersistentDataSourceFeatureDataStore3) _datastore)
                        .queryFeatureSets(f);
            else
                fsc = _datastore.queryFeatureSets(null);
            while (fsc != null && fsc.moveToNext()) {
                FeatureSet fs = fsc.get();
                if (fs == null)
                    continue;
                final File fsFile = _datasource.getFile(fs);
                if (bruteForce && (fsFile == null || !fsFile.equals(f)))
                    continue;
                ids.add(fs.getId());

                // Add feature bounds
                FeatureDataStoreUtils.addToEnvelope(_datastore, fs, bounds);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to add handler for " + f, e);
        } finally {
            if (fsc != null)
                fsc.close();
        }

        // Add the new handler
        addHandler(createHandler(f, ids, bounds.build()));
    }

    /**
     * Create a new content handler
     *
     * @param f Content file
     * @param featureSetIds List of associated feature set IDs
     * @param bounds Complete bounds (null if N/A)
     * @return New content handler or null if failed
     */
    protected abstract SpatialDbContentHandler createHandler(File f,
            List<Long> featureSetIds, Envelope bounds);

    @Override
    public List<URIContentHandler> query(URIQueryParameters params) {

        // Basic query based on handler attributes
        List<URIContentHandler> handlers = super.query(params);

        // If there are no results or we're not doing a FOV query then just
        // return what we got
        if (handlers.isEmpty() || params.fov == null)
            return handlers;

        // FOV query needs to be narrowed down to the feature level since the
        // basic query only checks the overall bounds of each file
        List<URIContentHandler> ret = new ArrayList<>(handlers.size());
        FOVFilter.MapState ms = params.fov.getMapState();
        FeatureDataStore2.FeatureQueryParameters fp = new FeatureDataStore2.FeatureQueryParameters();
        fp.limit = 1;
        fp.visibleOnly = params.visibleOnly;
        fp.spatialFilter = GeometryFactory.fromEnvelope(new Envelope(
                ms.upperLeft.getLongitude(), ms.lowerRight.getLatitude(), 0d,
                ms.upperRight.getLongitude(), ms.upperLeft.getLatitude(), 0d));
        fp.featureSetFilter = new FeatureDataStore2.FeatureSetQueryParameters();
        for (URIContentHandler h : handlers) {
            SpatialDbContentHandler sh = (SpatialDbContentHandler) h;
            fp.featureSetFilter.ids = new HashSet<>(sh.getFeatureSetIds());
            FeatureCursor fc = null;
            try {
                fc = _datastore.queryFeatures(fp);
                if (fc.moveToNext())
                    ret.add(h); // At least one feature was found - file passes
            } catch (Exception e) {
                Log.e(TAG, "Failed to query features for " + sh.getTitle(), e);
            } finally {
                if (fc != null)
                    fc.close();
            }
        }

        return ret;
    }
}
