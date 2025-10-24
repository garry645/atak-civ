package com.atakmap.map.layer.feature;

import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.feature.control.DataSourceDataStoreControl;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.GeometryCollection;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.geometry.Polygon;
import com.atakmap.math.Rectangle;
import com.atakmap.util.WildCard;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.util.Disposable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Utils
{

    /**
     * Package Visible utility method used by AbstractFeatureDataStore3 and Utils to provide a
     * locking mechanism for bulk modification.
     *
     * @param dataStore      the datastore
     * @param bulkModify     true if the lock is for bulk modification.
     * @param allowInterrupt if the modification allows for an interrupt to occur which would
     *                       throw a DataStoreException. If false, no interrupt is allowed and
     *                       the lock is reacquired
     * @throws DataStoreException if there an issue using equiring a lock on the data store
     */
    static void internalAcquireModifyLock(FeatureDataStore2 dataStore, boolean bulkModify, boolean allowInterrupt) throws DataStoreException
    {
        while (true)
        {
            try
            {
                dataStore.acquireModifyLock(bulkModify);
            } catch (InterruptedException e)
            {
                if (allowInterrupt)
                    throw new DataStoreException("Interrupted while waiting to acquire modify lock", e);
                else
                    continue;
            }
            break;
        }
    }

    /**
     * Return the number of features in a datastore that match the query
     *
     * @param dataStore the datastore to use
     * @param params    the query to use
     * @throws DataStoreException
     */
    public static int queryFeaturesCount(FeatureDataStore2 dataStore, FeatureDataStore2.FeatureQueryParameters params) throws DataStoreException
    {
        FeatureCursor result = null;
        try
        {
            int retval = 0;

            // XXX - set ignore fields on params

            result = dataStore.queryFeatures(params);
            while (result.moveToNext())
                retval++;
            return retval;
        } finally
        {
            if (result != null)
                result.close();
        }
    }

    /**
     * Return the number of featuresets in a datastore that match the query
     *
     * @param dataStore the datastore to use
     * @param params    the query to use
     * @throws DataStoreException
     */
    public static int queryFeatureSetsCount(FeatureDataStore2 dataStore, FeatureDataStore2.FeatureSetQueryParameters params) throws DataStoreException
    {
        FeatureSetCursor result = null;
        try
        {
            int retval = 0;

            // XXX - set ignore fields on params

            result = dataStore.queryFeatureSets(params);
            while (result.moveToNext())
                retval++;
            return retval;
        } finally
        {
            if (result != null)
                result.close();
        }
    }

    /**
     * Bulk copy features represented by a feature cursor into the provided datastore.
     *
     * @param dataStore the datastore to use
     * @param features  the cursor to use when copying features over.
     * @throws DataStoreException
     */
    public static void insertFeatures(FeatureDataStore2 dataStore, FeatureCursor features) throws DataStoreException
    {
        internalAcquireModifyLock(dataStore, true, true);
        try
        {
            final FeatureDefinition2 def = Adapters.adapt(features);
            while (features.moveToNext())
                dataStore.insertFeature(features.getFsid(), features.getId(), def, features.getVersion());
        } finally
        {
            dataStore.releaseModifyLock();
        }
    }

    public static void insertFeatureSets(FeatureDataStore2 dataStore, FeatureSetCursor featureSets) throws DataStoreException
    {
        internalAcquireModifyLock(dataStore, true, true);
        try
        {
            while (featureSets.moveToNext())
                dataStore.insertFeatureSet(featureSets.get());
        } finally
        {
            dataStore.releaseModifyLock();
        }
    }

    /**
     * Given a query, delete the matching features
     *
     * @param dataStore the datastore to use
     * @param params    the query
     * @throws DataStoreException
     */
    public static void deleteFeatures(FeatureDataStore2 dataStore, FeatureDataStore2.FeatureQueryParameters params) throws DataStoreException
    {
        internalAcquireModifyLock(dataStore, true, true);
        try
        {
            FeatureCursor result = null;
            try
            {
                // XXX - set ignore fields on params

                result = dataStore.queryFeatures(params);
                while (result.moveToNext())
                    dataStore.deleteFeature(result.getId());
            } finally
            {
                if (result != null)
                    result.close();
            }
        } finally
        {
            dataStore.releaseModifyLock();
        }
    }

    /**
     * Given a query, delete the matching feature sets
     *
     * @param dataStore the datastore to use
     * @param params    the query
     * @throws DataStoreException
     */
    public static void deleteFeatureSets(FeatureDataStore2 dataStore, FeatureDataStore2.FeatureSetQueryParameters params) throws DataStoreException
    {
        internalAcquireModifyLock(dataStore, true, true);
        try
        {
            FeatureSetCursor result = null;
            try
            {
                // XXX - set ignore fields on params

                result = dataStore.queryFeatureSets(params);
                while (result.moveToNext())
                    dataStore.deleteFeatureSet(result.getId());
            } finally
            {
                if (result != null)
                    result.close();
            }
        } finally
        {
            dataStore.releaseModifyLock();
        }
    }

    /**
     * Given a query, set the matching features visibility
     *
     * @param dataStore the datastore to use
     * @param params    the query
     * @param visible   the visibility state to use for the feature visibility.
     * @throws DataStoreException
     */
    public static void setFeaturesVisible(FeatureDataStore2 dataStore, FeatureDataStore2.FeatureQueryParameters params, boolean visible) throws DataStoreException
    {
        internalAcquireModifyLock(dataStore, true, true);
        try
        {
            FeatureCursor result = null;
            try
            {
                // XXX - set ignore fields on params

                result = dataStore.queryFeatures(params);
                while (result.moveToNext())
                    dataStore.setFeatureVisible(result.getId(), visible);
            } finally
            {
                if (result != null)
                    result.close();
            }
        } finally
        {
            dataStore.releaseModifyLock();
        }
    }

    /**
     * Given a query, set the matching feature sets visibility
     *
     * @param dataStore the datastore to use
     * @param params    the query
     * @param visible   the visibility state to use for the feature set visibility.
     * @throws DataStoreException
     */
    public static void setFeatureSetsVisible(FeatureDataStore2 dataStore, FeatureDataStore2.FeatureSetQueryParameters params, boolean visible) throws DataStoreException
    {
        internalAcquireModifyLock(dataStore, true, true);
        try
        {
            FeatureSetCursor result = null;
            try
            {
                // XXX - set ignore fields on params

                result = dataStore.queryFeatureSets(params);
                while (result.moveToNext())
                    dataStore.setFeatureSetVisible(result.getId(), visible);
            } finally
            {
                if (result != null)
                    result.close();
            }
        } finally
        {
            dataStore.releaseModifyLock();
        }
    }

    /**
     * Method to simplify the retrieval of a Feature from a FeatureDataStore.
     *
     * @param dataStore the data store to use for the query.
     * @param fid       the feature identifier.
     * @return the Feature.
     * @throws DataStoreException a datastore exception if there was an issue retrieving the feature.
     */
    public static Feature getFeature(FeatureDataStore2 dataStore, long fid) throws DataStoreException
    {
        FeatureDataStore2.FeatureQueryParameters params = new FeatureDataStore2.FeatureQueryParameters();
        params.ids = Collections.<Long>singleton(fid);
        params.limit = 1;
        FeatureCursor result = null;
        try
        {
            result = dataStore.queryFeatures(params);
            if (!result.moveToNext())
                return null;
            return result.get();
        } finally
        {
            if (result != null)
                result.close();
        }
    }

    /**
     * Method to simplify the retrieval of a FeatureSet from a FeatureDataStore.
     *
     * @param dataStore the data store to use for the query.
     * @param fid       the featureset identifier.
     * @return the FeatureSet.
     * @throws DataStoreException a datastore exception if there was an issue retrieving the featureset.
     */
    public static FeatureSet getFeatureSet(FeatureDataStore2 dataStore, long fid) throws DataStoreException
    {
        FeatureDataStore2.FeatureSetQueryParameters params = new FeatureDataStore2.FeatureSetQueryParameters();
        params.ids = Collections.<Long>singleton(fid);
        params.limit = 1;
        FeatureSetCursor result = null;
        try
        {
            result = dataStore.queryFeatureSets(params);
            if (!result.moveToNext())
                return null;
            return result.get();
        } finally
        {
            if (result != null)
                result.close();
        }
    }

    /**
     * Method to simplify the deletion of all FeatureSets from the datastore.
     *
     * @param dataStore the datastore to delete from
     * @throws DataStoreException an exception if deletion of the datasets is not successful.
     */
    public static void deleteAllFeatureSets(FeatureDataStore2 dataStore) throws DataStoreException
    {
        FeatureDataStore2.FeatureSetQueryParameters params = new FeatureDataStore2.FeatureSetQueryParameters();
        FeatureSetCursor result = null;
        List<Long> ids = new ArrayList<>();
        internalAcquireModifyLock(dataStore, true, true);
        try
        {
            try
            {
                result = dataStore.queryFeatureSets(params);
                if (!result.moveToNext())
                    ids.add(result.get().id);
            } finally
            {
                if (result != null)
                    result.close();
            }
            for (long id : ids)
            {
                dataStore.deleteFeatureSet(id);
            }
        } finally
        {
            dataStore.releaseModifyLock();
        }

    }

    /**
     * Method to simplify the retrieval of the visibility of a featureset
     *
     * @param dataStore the datastore to use
     * @param fid       the featureset id;
     * @throws DataStoreException an exception if it could not query the feature datastore
     */
    public static boolean isFeatureSetVisible(FeatureDataStore2 dataStore, long fid) throws DataStoreException
    {
        FeatureDataStore2.FeatureSetQueryParameters params = new FeatureDataStore2.FeatureSetQueryParameters();
        params.ids = Collections.<Long>singleton(fid);
        params.visibleOnly = true;
        params.limit = 1;
        FeatureSetCursor result = null;
        try
        {
            result = dataStore.queryFeatureSets(params);
            if (!result.moveToNext())
                return false;
            return true;
        } finally
        {
            if (result != null)
                result.close();
        }
    }

    /**
     * Method to simplify the retrieval of the visibility of a feature
     *
     * @param dataStore the datastore to use
     * @param fid       the feature id;
     * @throws DataStoreException an exception if it could not query the feature datastore
     */
    public static boolean isFeatureVisible(FeatureDataStore2 dataStore, long fid) throws DataStoreException
    {
        FeatureDataStore2.FeatureQueryParameters params = new FeatureDataStore2.FeatureQueryParameters();
        params.ids = Collections.<Long>singleton(fid);
        params.visibleOnly = true;
        params.limit = 1;
        FeatureCursor result = null;
        try
        {
            result = dataStore.queryFeatures(params);
            if (!result.moveToNext())
                return false;
            return true;
        } finally
        {
            if (result != null)
                result.close();
        }
    }

    /**
     * Get the associated source file for this feature set (if applicable)
     * Only works if the feature data store implements
     * {@link DataSourceFeatureDataStore}
     * <p>
     * XXX - It would be very useful and productive if FeatureSet had a custom
     * metadata field which could provide stuff like this, but alas...
     *
     * @param db Feature data store v1
     * @param fs Feature set
     * @return File or null if N/A
     * @deprecated use {@link com.atakmap.map.layer.feature.control.DataSourceDataStoreControl}
     */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public static File getSourceFile(FeatureDataStore db, FeatureSet fs)
    {
        if (db instanceof DataSourceFeatureDataStore)
            return ((DataSourceFeatureDataStore) db).getFile(fs);
        return null;
    }

    /**
     * Get the associated source file for this feature set (if applicable)
     *
     * @param db Feature data store v2
     * @param fs Feature set
     * @return File or null if N/A
     * @deprecated use {@link com.atakmap.map.layer.feature.control.DataSourceDataStoreControl}
     */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public static File getSourceFile(FeatureDataStore2 db, FeatureSet fs)
    {
        DataSourceDataStoreControl dataSource = null;
        do {
            if(db instanceof DataSourceDataStoreControl) {
                dataSource = (DataSourceDataStoreControl) db;
                break;
            }
            if(db instanceof Controls) {
                dataSource = ((Controls)db).getControl(DataSourceDataStoreControl.class);
                break;
            }
        } while(false);
        return (dataSource != null) ? dataSource.getFile(fs) : null;
    }

    /**
     * Get the associated source file for this feature set (if applicable)
     *
     * @param db Feature data store v1 or v2
     * @param fs Feature set
     * @return File or null if N/A
     *
     * @deprecated use {@link com.atakmap.map.layer.feature.control.DataSourceDataStoreControl}
     */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public static File getSourceFile(Disposable db, FeatureSet fs)
    {
        if (db instanceof FeatureDataStore)
            return getSourceFile((FeatureDataStore) db, fs);
        else if (db instanceof FeatureDataStore2)
            return getSourceFile((FeatureDataStore2) db, fs);
        return null;
    }

    // Hit-Test utilities

    /**
     * <P>NOTE: hit-testing is only performed against rings for {@link Polygon} geometry types;
     * interior/fill testing is not supported
     * @param g             The geometry
     * @param point         The test point
     * @param radius        The hit-test radius, in meters
     * @param touchPoint    If non-{@code null} returns the closest point on the geometry
     * @return
     */

    public static boolean hitTest(Geometry g, GeoPoint point, double radius, GeoPoint touchPoint)
    {
        if (g instanceof Point)
        {
            Point p = (Point) g;
            if (touchPoint != null)
                touchPoint.set(p.getY(), p.getX());
            return (GeoCalculations.distanceTo(point, new GeoPoint(p.getY(), p.getX())) <= radius);
        } else if (g instanceof LineString)
        {
            if (!mbrIntersects(g.getEnvelope(), point, radius))
                return false;
            return testOrthoHit((LineString) g, point, radius, touchPoint);
        } else if (g instanceof Polygon)
        {
            if (!mbrIntersects(g.getEnvelope(), point, radius))
                return false;

            Polygon p = (Polygon) g;
            if (testOrthoHit(p.getExteriorRing(), point, radius, touchPoint))
                return true;
            for (LineString inner : p.getInteriorRings())
                if (testOrthoHit(inner, point, radius, touchPoint))
                    return true;
            return false;
        } else if (g instanceof GeometryCollection)
        {
            if (!mbrIntersects(g.getEnvelope(), point, radius))
                return false;

            for (Geometry child : ((GeometryCollection) g).getGeometries())
                if (hitTest(child, point, radius, touchPoint))
                    return true;
            return false;
        } else
        {
            throw new IllegalStateException();
        }
    }

    // XXX - next 3 modified from EditablePolyline, review for optimization

    private static boolean mbrIntersects(Envelope mbb, GeoPoint point, double radiusMeters)
    {
        final double x = point.getLongitude();
        final double y = point.getLatitude();

        if (Rectangle.contains(mbb.minX, mbb.minY, mbb.maxX, mbb.maxY, x, y))
            return true;

        // XXX - check distance from minimum bounding box is with the radius
        final double fromX;
        if (x < mbb.minX)
        {
            fromX = mbb.minX;
        } else if (x > mbb.maxX)
        {
            fromX = mbb.maxX;
        } else
        {
            fromX = x;
        }

        final double fromY;
        if (y < mbb.minY)
        {
            fromY = mbb.minY;
        } else if (y > mbb.maxY)
        {
            fromY = mbb.maxY;
        } else
        {
            fromY = y;
        }

        return (GeoCalculations.distanceTo(new GeoPoint(fromY, fromX), new GeoPoint(y, x)) < radiusMeters);
    }

    private static boolean testOrthoHit(LineString linestring, GeoPoint point, double radius, GeoPoint touchPoint)
    {

        boolean res = mbrIntersects(linestring.getEnvelope(), point, radius);
        if (!res)
        {
            //Log.d(TAG, "hit not contained in any geobounds");
            return false;
        }

        final int numPoints = linestring.getNumPoints();

        final double px = point.getLongitude();
        final double py = point.getLatitude();

        int detected_partition = -1;
        Envelope minibounds = new Envelope(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        double x0;
        double y0;
        double x1;
        double y1;
        for (int i = 0; i < numPoints - 1; ++i)
        {
            x0 = linestring.getX(i);
            y0 = linestring.getY(i);
            x1 = linestring.getX(i + 1);
            y1 = linestring.getY(i + 1);

            // construct the minimum bounding box for the segment
            minibounds.minX = Math.min(x0, x1);
            minibounds.minY = Math.min(y0, y1);
            minibounds.maxX = Math.max(x0, x1);
            minibounds.maxY = Math.max(y0, y1);

            if (mbrIntersects(minibounds, point, radius))
            {
                Point isect = (touchPoint != null) ? new Point(0, 0) : null;
                if (dist(x0, y0, x1, y1, px, py, isect) < radius)
                {
                    if (touchPoint != null && isect != null)
                        touchPoint.set(isect.getY(), isect.getX());
                    return true;
                }
            }
        }
        //Log.d(TAG, "hit not contained in any sub geobounds");
        return false;
    }

    private static double dist(double x1, double y1, double x2, double y2, double x3, double y3, Point linePt)
    { // x3,y3 is the point
        double px = x2 - x1;
        double py = y2 - y1;

        double something = px * px + py * py;

        double u = ((x3 - x1) * px + (y3 - y1) * py) / something;

        if (u > 1)
            u = 1;
        else if (u < 0)
            u = 0;

        double x = x1 + u * px;
        double y = y1 + u * py;

        if (linePt != null)
        {
            linePt.set(x, y);
        }

        return GeoCalculations.distanceTo(new GeoPoint(y, x), new GeoPoint(y3, x3));
    }

    /**
     * Test to see if a value matches a test string containing optional wildcard characters.
     *
     * @return true if the value matches the test, if the value is null, the method will return
     * false.
     */
    public static boolean matches(final String test, final String value, final char wildcard)
    {
        if (value == null)
            return false;

        if (test.indexOf(wildcard) < 0)
            return value.equals(test);

        return value.matches(WildCard.wildcardAsRegex(test, wildcard));
    }

    public static boolean matches(Collection<String> test, String value, char wildcard)
    {
        for (String arg : test)
            if (matches(arg, value, wildcard))
                return true;
        return false;
    }
}
