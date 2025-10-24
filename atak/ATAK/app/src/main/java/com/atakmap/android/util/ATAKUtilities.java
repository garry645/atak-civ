
package com.atakmap.android.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.atakmap.android.data.URIContentHandler;
import com.atakmap.android.data.URIContentManager;
import com.atakmap.android.editableShapes.EditablePolyline;
import com.atakmap.android.filesystem.ResourceFile;
import com.atakmap.android.hierarchy.filters.FOVFilter;
import com.atakmap.android.icons.Icon2525cTypeResolver;
import com.atakmap.android.icons.UserIcon;
import com.atakmap.android.image.ImageDropDownReceiver;
import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.importfiles.task.ImportFilesTask;
import com.atakmap.android.maps.AnchoredMapItem;
import com.atakmap.android.maps.ILocation;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.Shape;
import com.atakmap.android.maps.graphics.GLBitmapLoader;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.toolbars.BullseyeTool;
import com.atakmap.android.user.icon.Icon2525cPallet;
import com.atakmap.android.user.icon.SpotMapPalletFragment;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.conversion.GeomagneticField;
import com.atakmap.coremap.maps.coords.Ellipsoid;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.coremap.maps.coords.MutableUTMPoint;
import com.atakmap.coremap.maps.coords.UTMPoint;
import com.atakmap.coremap.maps.coords.Vector2D;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.map.MapRenderer2;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.MapSceneModel;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.opengl.GLRenderGlobals;
import com.atakmap.map.projection.MapProjectionDisplayModel;
import com.atakmap.math.MathUtils;
import com.atakmap.math.PointD;
import com.atakmap.spatial.SpatialCalculator;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.annotation.Nullable;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.importfiles.ImportResolver;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.util.AttributeSet;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.symbology.SymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilStd2525cSymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilStd2525dSymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilStd2525eSymbologyProvider;

/**
 * Home for utility functions that don't have a better home yet. Should consolidate functions like
 * findSelf that otherwise will be copy-pasted 20 times.
 */
public class ATAKUtilities {
    private final static char[] HEX_DIGITS = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c',
            'd', 'e', 'f'
    };

    private static final ElevationManager.QueryParameters DTED_FILTER = new ElevationManager.QueryParameters();
    static {
        DTED_FILTER.types = new HashSet<>(Arrays.asList(
                "DTED",
                "DTED0",
                "DTED1",
                "DTED2",
                "DTED3"));
    }

    private static final String TAG = "ATAKUtilities";

    // For decoding base-64 images
    private static final String BASE64 = "base64,";
    private static final String BASE64_2 = "base64";
    private static final String BASE64_PNG = "iVBORw0K";

    public static final int ICON_LOADING = 0;
    public static final int ICON_LOADED = 1;
    public static final int ICON_FAILED = -1;

    private static final ExecutorService _remoteIconExecutor = new ThreadPoolExecutor(
            3,
            3,
            1000,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());
    static {
        ((ThreadPoolExecutor) _remoteIconExecutor).allowCoreThreadTimeOut(true);
    }

    private static final Map<String, Integer> _remoteIconLoadStatus = new ConcurrentHashMap<>();

    private static ISymbologyProvider provider;
    private static ISymbologyProvider milstd2525c;
    private static ISymbologyProvider milstd2525d;
    private static ISymbologyProvider milstd2525e;

    /**
     * Returns the default symbology provider. This provider should be used for
     * flows where the symbology provider may not be implicitly derived based
     * on the symbol code or where the user otherwise is not able to select a
     * provider.
     * @return the default symbology provider at this time.
     */
    public static synchronized ISymbologyProvider getDefaultSymbologyProvider() {
        if(milstd2525c == null) {
            MilStd2525cSymbologyProvider
                    .init(MapView.getMapView().getContext());
            milstd2525c = new MilStd2525cSymbologyProvider();
            MilStd2525dSymbologyProvider
                    .init(MapView.getMapView().getContext());
            milstd2525d = new MilStd2525dSymbologyProvider();

            milstd2525e = new MilStd2525eSymbologyProvider();

            gov.tak.platform.symbology.milstd2525.MilStd2525.init(MapView.getMapView().getContext());
            SymbologyProvider.registerProvider(milstd2525c, 0);
            SymbologyProvider.registerProvider(milstd2525d, 0);
            SymbologyProvider.registerProvider(milstd2525e, 0);
        }
        if (provider == null) {
            AtakPreferences prefs = AtakPreferences.getInstance(MapView.getMapView().getContext());
            String providerName = prefs.get("symbologyProvider","2525C");

            for(ISymbologyProvider prov : SymbologyProvider.getProviders()) {
                if(prov.getName().equals(providerName)) {
                    provider = prov;
                }
            }
            if(provider == null) {
                provider = milstd2525c;
            }

        }
        return provider;
    }

    public static synchronized void setDefaultSymbologyProvider(ISymbologyProvider milsym) {
        Log.i(TAG, "setDefaultSymbologyProvider " + milsym);
        if (milsym == null) {
            MilStd2525cSymbologyProvider
                    .init(MapView.getMapView().getContext());
            provider = new MilStd2525cSymbologyProvider();
        } else {
            provider = milsym;
        }
    }

    /**
     * Uses logic to obtain the best display name for a specific MapItem in the system.
     * @param item The map item to get the display name from.
     * @return the display name.
     */
    public static String getDisplayName(final MapItem item) {
        if (item == null)
            return "";

        String title = item.getTitle();
        if (!FileSystemUtils.isEmpty(title))
            return title;

        if (item instanceof Marker) {
            // For markers that don't have a set title but an underlying callsign
            if (FileSystemUtils.isEmpty(title))
                title = item.getMetaString("callsign", "");

            // Shape markers without a title
            if (FileSystemUtils.isEmpty(title))
                title = item.getMetaString("shapeName", "");

            // Do not perform shape lookup here (see ATAK-10593)
            // If "shapeName" isn't specified then assume it's untitled
            // or address the issue separately
        }

        if (FileSystemUtils.isEmpty(title)) {
            // dont display gross ATAK UUID's, just other ones from
            // systems that just put a callsign as the unique indentifer
            MapView mv = MapView.getMapView();
            String uid = item.getUID();
            if (!isUUID(uid))
                title = uid;
            else if (mv != null)
                title = mv.getContext().getString(R.string.untitled_item);
        }
        return title;
    }

    /**
     * Determine if a string supplied is a valid UUID.
     * @param string the unknown string
     * @return true if it is a UUID.
     */
    public static boolean isUUID(String string) {
        try {
            UUID u = UUID.fromString(string);
            return (u != null);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Allow for application registration of decoders.
     */
    public interface BitmapDecoder {
        /**
         * Provided a URI, allow for an application level decoding of the uri into a bitmap.
         * @param uriStr the uri for the bitmap, it should be assumed that this bitmap is 
         * can be actively recycled by the caller. 
         * @return Bitmap a valid bitmap based on the uriStr or null if unable to process the uri.
         */
        Bitmap decodeUri(String uriStr);
    }

    private final static Map<String, BitmapDecoder> decoders = new ConcurrentHashMap<>();

    /**
     * Finds self marker even if it has not been placed on the map.
     * 
     * @param mapView the mapView to use
     * @return Self marker if it exists, otherwise null.
     */
    static public Marker findSelfUnplaced(MapView mapView) {
        return mapView.getSelfMarker();
    }

    /**
     * Finds self marker but only if it has been placed on the map
     * 
     * @param mapView the mapView to use
     * @return Self marker if it exists, otherwise null.
     */
    static public Marker findSelf(@Nullable MapView mapView) {
        if (mapView == null)
            return null;

        final Marker self = mapView.getSelfMarker();
        if (self != null && self.getGroup() != null)
            return self;
        else
            return null;
    }

    /**
     * Method that will return true if the map item provided is the self marker.
     *
     * @param mapView the mapView used
     * @param item the item passed in
     * @return true if the item is the self marker.
     */
    public static boolean isSelf(MapView mapView, PointMapItem item) {
        if (!(item instanceof Marker))
            return false;

        return isSelf(mapView, item.getUID());
    }

    /**
     * Method that will return true if the map item provided is the self marker.
     *
     * @param mapView the mapView used
     * @param uid the uid of the item to check
     * @return true if the item is the self marker.
     */
    public static boolean isSelf(MapView mapView, String uid) {
        Marker self = findSelfUnplaced(mapView);
        if (self == null)
            return false;

        return FileSystemUtils.isEquals(self.getUID(), uid);
    }

    /**
     * Scales map to fit item and, if includeself is true, the self marker, within a space width by
     * height around the focus point.
     * 
     * @param _mapView the mapview to use when scaling to fit around the item.
     * @param item the item to use
     * @param includeSelf if the scaleToFit should also make use of the self marker
     * @param widthPad the padding to be used for the width around the best fit.
     * @param heightPad the padding to be used for the height of the bet fit.
     */
    public static void scaleToFit(MapView _mapView, MapItem item,
            boolean includeSelf, int widthPad,
            int heightPad) {
        if (includeSelf) {
            scaleToFit(_mapView, new MapItem[] {
                    item, findSelf(_mapView)
            }, widthPad, heightPad);
        } else {
            scaleToFit(_mapView, new MapItem[] {
                    item
            }, widthPad, heightPad);
        }
    }

    /**
     * Scales map to fit items within a space width by height around the focus point.
     *
     * @param _mapView the mapview to use when scaling to fit around the items.
     * @param items the array of items to provide a best fit for
     * @param widthPad the padding to be used for the width around the best fit.
     * @param heightPad the padding to be used for the height of the bet fit.
     */
    public static void scaleToFit(MapView _mapView, MapItem[] items,
            int widthPad,
            int heightPad) {

        // Need to compute the max altitude for a proper perspective fit
        double maxAlt = -Double.MAX_VALUE;
        ArrayList<GeoPoint> pointList = new ArrayList<>(items.length);
        for (MapItem i : items) {
            if (i == null)
                continue;

            double alt = Double.NaN;

            // Rectangles, circles, polylines, etc.
            if (i instanceof Shape) {
                GeoPoint[] points = ((Shape) i).getPoints();
                Collections.addAll(pointList, points);
                if (i instanceof EditablePolyline) {
                    // Max altitude has been pre-computed
                    alt = ((EditablePolyline) i).getMaxAltitude().get()
                            .getAltitude();
                } else {
                    // Compute max point altitude
                    alt = -Double.MAX_VALUE;
                    for (GeoPoint p : points) {
                        if (p.isAltitudeValid())
                            alt = Math.max(alt, p.getAltitude());
                    }
                }

                // Include shape center/anchor altitude
                if (i instanceof AnchoredMapItem) {
                    PointMapItem pmi = ((AnchoredMapItem) i).getAnchorItem();
                    if (pmi != null) {
                        GeoPoint p = pmi.getPoint();
                        if (p.isAltitudeValid())
                            alt = Math.max(alt, p.getAltitude());
                    }
                }
            }

            // Anchored map items that aren't shapes - redirect to anchor
            else if (i instanceof AnchoredMapItem) {
                MapItem anchor = ((AnchoredMapItem) i).getAnchorItem();
                if (anchor != null)
                    i = anchor;
            }

            // Markers
            if (i instanceof PointMapItem) {
                GeoPoint p = ((PointMapItem) i).getPoint();
                pointList.add(p);
                alt = p.getAltitude();
            }

            // Include the height in the max altitude
            double height = i.getHeight();
            if (!Double.isNaN(alt) && !Double.isNaN(height))
                alt += height;

            // Add to max altitude if valid
            if (GeoPoint.isAltitudeValid(alt))
                maxAlt = Math.max(maxAlt, alt);
        }
        scaleToFit(_mapView, pointList.toArray(new GeoPoint[0]), maxAlt,
                widthPad, heightPad);
    }

    /**
     * Scales map to fit items within a space width by height around the focus point.
     *
     * @param _mapView the mapview to use when scaling to fit around the items.
     * @param target the target point to include
     * @param friendly the friendly point to include
     */
    public static void scaleToFit(MapView _mapView, PointMapItem target,
            PointMapItem friendly) {
        //not quite sure why this method does not just make use of one of the other myriad of methods.
        if (_mapView != null) {
            GeoPoint[] points = new GeoPoint[2];
            points[0] = target.getPoint();
            points[1] = friendly.getPoint();

            GeoPoint center = GeoCalculations.centerOfExtremes(points, 0,
                    points.length);
            if (center == null)
                return;

            MapSceneModel sm = _mapView.getRenderer3().getMapSceneModel(false,
                    MapRenderer2.DisplayOrigin.UpperLeft);
            sm.set(_mapView.getDisplayDpi(),
                    sm.width,
                    sm.height,
                    sm.mapProjection,
                    center,
                    sm.focusx,
                    sm.focusy,
                    sm.camera.azimuth,
                    0d,
                    sm.gsd,
                    true);

            PointF tvp = sm.forward(points[0], (PointF) null);
            PointF fvp = sm.forward(points[1], (PointF) null);

            double viewWidth = 2 * sm.width / (double) 3;
            double padding = viewWidth / 4;
            viewWidth -= padding;
            double viewHeight = sm.height - padding;
            double modelWidth = Math.abs(tvp.x - fvp.x);
            double modelHeight = Math.abs(tvp.y - fvp.y);

            double zoomFactor = viewWidth / modelWidth;
            if (zoomFactor * modelHeight > viewHeight) {
                zoomFactor = viewHeight / modelHeight;
            }

            _mapView.getMapController().dispatchOnPanRequested();
            _mapView.getRenderer3().lookAt(
                    center,
                    sm.gsd / zoomFactor,
                    sm.camera.azimuth,
                    90d + sm.camera.elevation,
                    true);
        }
    }

    /**
     * Scales map to fit item and, if includeself is true, the self marker, within a space width by
     * height around the focus point.
     *
     * @param mv the mapview to use when scaling to fit.
     * @param points the  array of geopoints to use
     * @param altitude Altitude to zoom to (perspective mode only; NaN to ignore)
     * @param widthPad the padding to be used for the width around the best fit.
     * @param heightPad the padding to be used for the height of the bet fit.
     * @return true if the scale process was successful.
     */
    public static boolean scaleToFit(MapView mv, GeoPoint[] points,
            double altitude, int widthPad, int heightPad) {
        if (points.length == 0) {
            Log.d(TAG, "Points are empty");
            return false;
        }

        // get the extremes in pixel-size so we can zoom to that size
        int[] e = GeoCalculations.findExtremes(points, 0, points.length,
                mv.isContinuousScrollEnabled());
        if (e.length < 4) {
            Log.d(TAG,
                    "cannot find the extremes for: " + Arrays.toString(points));
            return false;
        }

        GeoPoint north = points[e[1]], south = points[e[3]];
        GeoPoint west = points[e[0]], east = points[e[2]];
        double minLat = south.getLatitude(), maxLat = north.getLatitude();
        double minLng = west.getLongitude(), maxLng = east.getLongitude();
        boolean crossesIDL = mv.isContinuousScrollEnabled()
                && Math.abs(maxLng - minLng) > 180 && maxLng <= 180
                && minLng >= -180;
        if (crossesIDL)
            minLng = west.getLongitude() - 360;
        GeoBounds bounds = new GeoBounds(minLat, minLng, maxLat, maxLng);

        // Get highest altitude so everything is in frame
        if (!GeoPoint.isAltitudeValid(altitude)) {
            double maxAlt = -Double.MAX_VALUE;
            for (GeoPoint p : points) {
                if (p.isAltitudeValid())
                    maxAlt = Math.max(maxAlt, p.getAltitude());
            }
        }

        return scaleToFit(mv, bounds, altitude, widthPad, heightPad);
    }

    public static boolean scaleToFit(MapView mv, GeoPoint[] points,
            int width, int height) {
        return scaleToFit(mv, points, Double.NaN, width, height);
    }

    public static boolean scaleToFit(MapView mv, GeoBounds bounds,
            int widthPad, int heightPad) {
        return scaleToFit(mv, bounds, Double.NaN, widthPad, heightPad);
    }

    /**
     * Given a bounds and an altitude scale the current map view to fit with some padding.
     * @param mv the map view
     * @param bounds the bounds
     * @param altitude the altitude and if the altitude is unknown should be Double.NaN
     * @param widthPad the padding width of the bounds visually in pixels
     * @param heightPad the padding height of the bounds visually in pixels
     * @return true if it is successful
     */
    public static boolean scaleToFit(MapView mv, GeoBounds bounds,
            double altitude, int widthPad, int heightPad) {
        double minLat = bounds.getSouth();
        double maxLat = bounds.getNorth();
        double minLng = bounds.getWest();
        double maxLng = bounds.getEast();
        GeoPoint center = bounds.getCenter(null);
        double cLat = center.getLatitude();
        double cLng = center.getLongitude();

        MapSceneModel scaleToModel = mv.getRenderer3().getMapSceneModel(false,
                MapRenderer2.DisplayOrigin.UpperLeft);

        // IDL corrections
        if (bounds.crossesIDL()) {
            if (cLng < 0) {
                minLng = bounds.getEast() - 360;
                maxLng = bounds.getWest();
            } else {
                minLng = bounds.getEast();
                maxLng = bounds.getWest() + 360;
            }
        }
        double unwrap = 0;
        if (minLng < -180 || maxLng > 180)
            unwrap = mv.getLongitude() > 0 ? 360 : -360;

        final double localAlt = ElevationManager.getElevation(
                cLat, GeoCalculations.wrapLongitude(cLng), null);

        // currently there exists cases where the altitude constructed from a shape passed in
        // when invalid is not NaN.   Preserve the current bounds check since this is a public
        // interface and note in the documentation what is expected. 
        if (!GeoPoint.isAltitudeValid(altitude))
            altitude = localAlt;
        else if (!Double.isNaN(localAlt) && altitude < localAlt)
            altitude = localAlt;

        final GeoPoint panTo = new GeoPoint(
                cLat, GeoCalculations.wrapLongitude(cLng),
                Double.isNaN(altitude) ? 0d : altitude);

        // pan to the point -- whether or not the location is wrapped or
        // unwrapped, repositioning the map at the unwrapped location
        // should preserve any necessary IDL wrapping by the view
        scaleToModel.set(
                scaleToModel.dpi,
                scaleToModel.width, scaleToModel.height,
                scaleToModel.mapProjection,
                panTo,
                scaleToModel.focusx, scaleToModel.focusy,
                scaleToModel.camera.azimuth,
                90d + scaleToModel.camera.elevation,
                scaleToModel.gsd,
                true);

        // the perspective camera has additional effective zoom based on the
        // AGL -- we will need to adjust the scale factor to account for this.
        double zoomFactor = 1d;
        if (!scaleToModel.camera.perspective) {
            double spread = 0.0;
            if (Double.compare(minLat, maxLat) == 0
                    || Double.compare(minLng, maxLng) == 0)
                spread = 0.0001d;

            if (minLng < -180 || maxLng > 180)
                unwrap = mv.getLongitude() > 0 ? 360 : -360;

            PointF northWest = mv.forward(new GeoPoint(maxLat + spread,
                    minLng - spread), unwrap);
            PointF southEast = mv.forward(new GeoPoint(minLat - spread,
                    maxLng + spread), unwrap);

            double padding = widthPad / 4d;
            widthPad -= padding;
            heightPad -= padding;

            double modelWidth = Math.abs(northWest.x - southEast.x);
            double modelHeight = Math.abs(northWest.y - southEast.y);

            zoomFactor = widthPad / modelWidth;
            if (zoomFactor * modelHeight > heightPad)
                zoomFactor = heightPad / modelHeight;
        } else {
            // NOTE: we cannot rely on screen space ratio to compute zoom with
            // the perspective camera on the surface plane due to perspective
            // skew. Screenspace ratios may only be used to compute zoom for
            // planes that are tangent to the camera line of sight. We will
            // first compute the minimum view required to contain the entire
            // bounding sphere. Then the bounding box will be computed in
            // screenspace. A plane will be constructed at the `z` of the
            // screenspace bounding box closest to the camera. World space
            // vectors on that plane corresponding to the screen positions will
            // be computed to determine final screenspace to world space
            // ratios.

            // compute bounding sphere, set a reasonable minimum limit
            final double radius = MathUtils.max(
                    GeoCalculations.distanceTo(
                            new GeoPoint(maxLat, (minLng + maxLng) / 2d),
                            new GeoPoint(maxLat, (minLng + maxLng) / 2d)) / 2d,
                    GeoCalculations.distanceTo(
                            new GeoPoint((minLat + maxLat) / 2d, minLng),
                            new GeoPoint((minLat + maxLat) / 2d, maxLng)) / 2d,
                    GeoCalculations.distanceTo(new GeoPoint(maxLat, minLng),
                            new GeoPoint(minLat, maxLng)) / 2d,
                    10d // minimum of 10m radius for zoom purposes
            );

            double padding = widthPad / 4d;
            widthPad -= padding;
            heightPad -= padding;

            // compute the dimension, in pixels, to capture the object radius
            final double pixelsAtD = Math.min(heightPad, widthPad);
            // compute the local GSD of the object
            final double gsdAtD = radius / (pixelsAtD / 2d);

            // compute camera range to centroid at target GSD. Adjust by altitude to account for AGL relative scaling.
            final double camRange = MapSceneModel.range(gsdAtD,
                    scaleToModel.camera.fov, scaleToModel.height)
                    + panTo.getAltitude();
            // compute GSD based on AGL adjusted range
            final double gsd = MapSceneModel.gsd(camRange,
                    scaleToModel.camera.fov, scaleToModel.height);

            // create a model such that the major-radius adheres to the minor dimension
            scaleToModel.set(
                    scaleToModel.dpi,
                    scaleToModel.width, scaleToModel.height,
                    scaleToModel.mapProjection,
                    scaleToModel.mapProjection
                            .inverse(scaleToModel.camera.target, null),
                    scaleToModel.focusx, scaleToModel.focusy,
                    scaleToModel.camera.azimuth,
                    90d + scaleToModel.camera.elevation,
                    gsd,
                    true);

            // XXX - there are some issues with very close zoom on small
            //       objects and `panTo` locations with negative altitude. The
            //       code below is observed to generally improve experience
            //       with very small objects (e.g. vehicle models) and also
            //       mitigates the aforementioned issue with negative
            //       altitudes. If the below condition is removed, issues
            //       related to negative altitudes will need to be resolved as
            //       well

            // for larger objects, try to closely fit the AABB against the
            // padded dimensions. small objects will zoom in too far.
            if (radius > 10d) {
                // since the AABB of the content should now be in view, obtain the
                // screenspace coordinates for the extents to do a final zoom
                double spread = 0.0;
                if (Double.compare(minLat, maxLat) == 0
                        || Double.compare(minLng, maxLng) == 0)
                    spread = 0.0001d;

                // shift longitude for IDL for flat projection
                if (scaleToModel.mapProjection
                        .getSpatialReferenceID() == 4326) {
                    if (Math.abs(panTo.getLongitude() - minLng) > 180d)
                        minLng += (panTo.getLongitude() < 0d) ? -360d : 360d;
                    if (Math.abs(panTo.getLongitude() - maxLng) > 180d)
                        maxLng += (panTo.getLongitude() < 0d) ? -360d : 360d;
                }

                PointD northWest = new PointD(0d, 0d, 0d);
                scaleToModel.forward(
                        new GeoPoint(maxLat + spread, minLng - spread,
                                ElevationManager.getElevation(maxLat + spread,
                                        minLng - spread, null)),
                        northWest);
                PointD northEast = new PointD(0d, 0d, 0d);
                scaleToModel.forward(
                        new GeoPoint(maxLat + spread, minLng + spread,
                                ElevationManager.getElevation(maxLat + spread,
                                        minLng + spread, null)),
                        northEast);
                PointD southEast = new PointD(0d, 0d, 0d);
                scaleToModel.forward(
                        new GeoPoint(minLat - spread, minLng + spread,
                                ElevationManager.getElevation(maxLat - spread,
                                        minLng + spread, null)),
                        southEast);
                PointD southWest = new PointD(0d, 0d, 0d);
                scaleToModel.forward(
                        new GeoPoint(minLat - spread, maxLng - spread,
                                ElevationManager.getElevation(minLat - spread,
                                        maxLng - spread, null)),
                        southWest);

                // establish the plane that the camera will zoom relative to
                final double zoomPlaneZ = MathUtils.min(northWest.z,
                        northEast.z, southEast.z, southWest.z);

                PointD ss_ul = new PointD(
                        MathUtils.min(northWest.x, northEast.x, southEast.x,
                                southWest.x),
                        MathUtils.min(northWest.y, northEast.y, southEast.y,
                                southWest.y),
                        zoomPlaneZ);
                PointD ss_ll = new PointD(
                        MathUtils.min(northWest.x, northEast.x, southEast.x,
                                southWest.x),
                        MathUtils.max(northWest.y, northEast.y, southEast.y,
                                southWest.y),
                        zoomPlaneZ);
                PointD ss_lr = new PointD(
                        MathUtils.max(northWest.x, northEast.x, southEast.x,
                                southWest.x),
                        MathUtils.max(northWest.y, northEast.y, southEast.y,
                                southWest.y),
                        zoomPlaneZ);

                // transform screenspace corners into WCS
                PointD wcs_ul = new PointD(0d, 0d, 0d);
                PointD wcs_ll = new PointD(0d, 0d, 0d);
                PointD wcs_lr = new PointD(0d, 0d, 0d);
                PointD wcs_c = new PointD(0d, 0d, 0d);

                scaleToModel.inverse.transform(ss_ul, wcs_ul);
                scaleToModel.inverse.transform(ss_ll, wcs_ll);
                scaleToModel.inverse.transform(ss_lr, wcs_lr);

                // compute target point on plane
                scaleToModel.inverse.transform(new PointD(scaleToModel.focusx,
                        scaleToModel.focusy, zoomPlaneZ), wcs_c);

                // compute vertical and horizontal axes on zoom-to plane in WCS
                final double verticalAxis = wcs_distance(wcs_ul, wcs_ll,
                        scaleToModel.displayModel);
                final double horizontalAxis = wcs_distance(wcs_lr, wcs_ll,
                        scaleToModel.displayModel);

                // compute vertical and horizontal GSDs on zoom plane
                final double gsdVertical = verticalAxis / heightPad;
                final double gsdHorizontal = horizontalAxis / widthPad;
                final double vpixelsHgsd = verticalAxis / gsdHorizontal;
                final double hpixelsVgsd = horizontalAxis / gsdVertical;

                // select GSD that will most closely correspond to padded width or
                // height
                final double gsdAtZoomPlane;
                if (vpixelsHgsd > heightPad)
                    gsdAtZoomPlane = gsdVertical;
                else if (hpixelsVgsd > widthPad)
                    gsdAtZoomPlane = gsdHorizontal;
                else
                    gsdAtZoomPlane = Math.min(gsdVertical, gsdHorizontal);

                // compute the camera range from the zoom plane
                final double rangeAtZoomPlane = MapSceneModel.range(
                        gsdAtZoomPlane, scaleToModel.camera.fov,
                        scaleToModel.height);

                // compute camera location
                double dx = (scaleToModel.camera.location.x - wcs_c.x)
                        * scaleToModel.displayModel.projectionXToNominalMeters;
                double dy = (scaleToModel.camera.location.y - wcs_c.y)
                        * scaleToModel.displayModel.projectionYToNominalMeters;
                double dz = (scaleToModel.camera.location.z - wcs_c.z)
                        * scaleToModel.displayModel.projectionZToNominalMeters;
                final double m = MathUtils.distance(dx, dy, dz, 0d, 0d, 0d);
                dx /= m;
                dy /= m;
                dz /= m;

                PointD camera = new PointD(
                        wcs_c.x + (dx * rangeAtZoomPlane)
                                / scaleToModel.displayModel.projectionXToNominalMeters,
                        wcs_c.y + (dy * rangeAtZoomPlane)
                                / scaleToModel.displayModel.projectionYToNominalMeters,
                        wcs_c.z + (dz * rangeAtZoomPlane)
                                / scaleToModel.displayModel.projectionZToNominalMeters);

                final double rangeToTarget = wcs_distance(camera,
                        scaleToModel.camera.target, scaleToModel.displayModel);

                scaleToModel.set(
                        scaleToModel.dpi,
                        scaleToModel.width, scaleToModel.height,
                        scaleToModel.mapProjection,
                        panTo,
                        scaleToModel.focusx, scaleToModel.focusy,
                        scaleToModel.camera.azimuth,
                        90d + scaleToModel.camera.elevation,
                        MapSceneModel.gsd(rangeToTarget + panTo.getAltitude(),
                                scaleToModel.camera.fov, scaleToModel.height),
                        true);
            }
            // zoom is already adjusted
        }

        // Clamp tilt to max at new zoom level
        double maxTilt = mv.getMaxMapTilt(mv.getMapScale() * zoomFactor);
        if (mv.getMapTilt() > maxTilt) {
            scaleToModel.set(
                    scaleToModel.dpi,
                    scaleToModel.width, scaleToModel.height,
                    scaleToModel.mapProjection,
                    scaleToModel.mapProjection
                            .inverse(scaleToModel.camera.target, null),
                    scaleToModel.focusx, scaleToModel.focusy,
                    scaleToModel.camera.azimuth,
                    maxTilt,
                    scaleToModel.gsd,
                    true);
        }

        // Zoom to area
        scaleToModel.set(
                scaleToModel.dpi,
                scaleToModel.width, scaleToModel.height,
                scaleToModel.mapProjection,
                scaleToModel.mapProjection.inverse(scaleToModel.camera.target,
                        null),
                scaleToModel.focusx, scaleToModel.focusy,
                scaleToModel.camera.azimuth,
                90d + scaleToModel.camera.elevation,
                scaleToModel.gsd / zoomFactor,
                true);

        mv.getRenderer3().lookAt(
                scaleToModel.mapProjection.inverse(scaleToModel.camera.target,
                        null),
                scaleToModel.gsd,
                scaleToModel.camera.azimuth,
                90d + scaleToModel.camera.elevation,
                MapRenderer3.CameraCollision.Ignore,
                true);

        return true;
    }

    /**
     * Scales map to fit item.
     *
     * @param item the item to use
     */
    public static void scaleToFit(final MapItem item) {
        MapView mv = MapView.getMapView();
        if (mv != null)
            scaleToFit(mv, item, false, mv.getWidth(), mv.getHeight());
    }

    /**
     * Given a location, scale to fit the bounds provided by the location
     * @param loc the location
     */
    public static void scaleToFit(ILocation loc) {
        MapView mv = MapView.getMapView();
        if (mv != null) {
            GeoBounds bounds = loc.getBounds(null);
            if (bounds == null)
                return;
            bounds = new GeoBounds(bounds);
            bounds.setWrap180(mv.isContinuousScrollEnabled());
            scaleToFit(mv, bounds, mv.getWidth(), mv.getHeight());
        }
    }

    /**
     * Computes the maximum GSD that should be specified when focusing on a
     * point. This ensures an appropriate minimum camera offset. In general,
     * this method is only recommended for points that are elevated above the
     * terrain surface.
     *
     * <P>NOTE: this is NOT a recommended focus distance.
     *
     * @param location          The focus location
     * @param localElevation    The local elevation at the point location
     * @param vfov              The vertical FOV of the camera
     * @param heightPx          The height of the display viewport, in pixels
     *
     * @return  The maximum recommended GSD that should be associated when
     *          requesting the camera to focus on the specified point, when
     *          taking into consideration the local elevation.
     */
    public static double getMaximumFocusResolution(GeoPoint location,
            double localElevation, double vfov, int heightPx) {
        final double alt = location.getAltitude();
        // if the point is at or below the terrain surface, any GSD selected
        // will be acceptable
        if (Double.isNaN(alt))
            return 0d;
        if (location.getAltitudeReference() == GeoPoint.AltitudeReference.AGL
                && alt <= 0d)
            return 0d;
        if (alt <= localElevation)
            return 0d;

        final double minOffset = 25d;
        return MapSceneModel.gsd(alt + minOffset, vfov, heightPx);
    }

    public static double getMaximumFocusResolution(GeoPoint location) {
        final MapSceneModel sm = MapView.getMapView().getRenderer3()
                .getMapSceneModel(false, MapRenderer2.DisplayOrigin.UpperLeft);
        return getMaximumFocusResolution(location,
                ElevationManager.getElevation(location.getLatitude(),
                        location.getLongitude(), null),
                sm.camera.fov, sm.height);
    }

    /**
     * For a specific GeoPoint, derive the actual magnetic declination that should be used as 
     * double offset.
     * Note: Prior to August 7, 2017 Android devices utilized WMM 2010.  See the change id I36f26086b1e2f62f81974d81d90c9a9c315a3445 in the Google Android Source code     Peng Xu <pengxu@google.com> in response to bug 31216311.
     * https://android.googlesource.com/platform/frameworks/base/+/63bf36a2117ca0338d7d4fdd3c5612a9e6091c04
     */
    public static double getCurrentMagneticVariation(GeoPoint point) {
        Date d = CoordinatedTime.currentDate();

        // Use the GMF around the user to find the declination, according to the Jump master 
        double hae = EGM96.getHAE(point);

        // Default to zero so we don't get a NaN result
        if (!GeoPoint.isAltitudeValid(hae))
            hae = 0;

        GeomagneticField gmf = new GeomagneticField(
                (float) point.getLatitude(),
                (float) point.getLongitude(),
                (float) hae, d.getTime());
        return gmf.getDeclination();
    }

    /**
     * Obtains the grid convergence used for a specific line of bearing.
     *
     * @deprecated {@link gov.tak.api.engine.map.coords.GeoCalculations#computeGridConvergence(IGeoPoint, IGeoPoint)}
     * @param sPoint the starting point for the line of bearing.
     * @param ePoint the end point for the line of bearing.
     * @return the grid deviation (grid convergence) for the provided line of 
     * bearing.      The value is in angular degrees between [-180.0, 180.0)
     */
    @Deprecated()
    @DeprecatedApi(since = "4.7", forRemoval = true, removeAt = "5.0")
    public static double computeGridConvergence(GeoPoint sPoint,
            GeoPoint ePoint) {
        return gov.tak.api.engine.map.coords.GeoCalculations
                .computeGridConvergence(
                        MarshalManager.marshal(sPoint, GeoPoint.class,
                                gov.tak.api.engine.map.coords.GeoPoint.class),
                        MarshalManager.marshal(ePoint, GeoPoint.class,
                                gov.tak.api.engine.map.coords.GeoPoint.class));
    }

    /**
     * Obtains the grid convergence used for a specific line of bearing.
     *
     * @deprecated {@link gov.tak.api.engine.map.coords.GeoCalculations#computeGridConvergence(IGeoPoint, double, double)}
     * @param sPoint the starting point for the line of bearing.
     * @param angle the angle of the line of bearing.
     * @param distance the length of the line of bearing. 
     * @return the grid deviation (grid convergence) for the provided line of 
     * bearing.   It is important to note that grid convergence cannot be acheived
     * unless the line of bearing has some length.  This will determine which grid 
     * line is used to converge against.   The value is in angular degrees between [-180.0, 180.0)
     */
    @Deprecated()
    @DeprecatedApi(since = "4.7", forRemoval = true, removeAt = "5.0")
    public static double computeGridConvergence(final GeoPoint sPoint,
            final double angle, final double distance) {
        final GeoPoint ePoint = GeoCalculations.pointAtDistance(sPoint,
                angle, distance);
        return computeGridConvergence(sPoint, ePoint);
    }

    /**
     * Convert a bearing from Magnetic North to True North
     *
     * @deprecated {@link gov.tak.api.engine.map.coords.GeoCalculations#convertFromMagneticToTrue(IGeoPoint, double)}
     * @param point GeoPoint for the location of the Compass.
     * @param angle Bearing in degrees (Magnetic North)
     * @return Bearing in degrees (True North)
     */
    @Deprecated()
    @DeprecatedApi(since = "4.7", forRemoval = true, removeAt = "5.0")
    public static double convertFromMagneticToTrue(final GeoPoint point,
            final double angle) {
        return gov.tak.api.engine.map.coords.GeoCalculations
                .convertFromMagneticToTrue(
                        MarshalManager.marshal(point, GeoPoint.class,
                                gov.tak.api.engine.map.coords.GeoPoint.class),
                        angle);
    }

    /**
     * Convert a bearing from True North to Magnetic North
     *
     * @deprecated {@link gov.tak.api.engine.map.coords.GeoCalculations#convertFromTrueToMagnetic(IGeoPoint, double)}
     * @param point GeoPoint for the location of the Compass.
     * @param angle Bearing in degrees (True North)
     * @return Bearing in degrees (Magnetic North)
     */
    @Deprecated()
    @DeprecatedApi(since = "4.7", forRemoval = true, removeAt = "5.0")
    public static double convertFromTrueToMagnetic(final GeoPoint point,
            final double angle) {
        return gov.tak.api.engine.map.coords.GeoCalculations
                .convertFromTrueToMagnetic(
                        MarshalManager.marshal(point, GeoPoint.class,
                                gov.tak.api.engine.map.coords.GeoPoint.class),
                        angle);
    }

    /**
     * Check if a GeoPoint resides within a polygon represented by a GeoPoint Array The polygon does
     * not have to convex in order for this function to work, but the first and last GeoPoint must
     * be equivalent
     * 
     * @param point GeoPoint to be tested
     * @param polygon Array of GeoPoint that represents the polygon (does not have to be convex)
     * @return point resides in polygon?
     */
    public static boolean pointInsidePolygon(GeoPoint point,
            GeoPoint[] polygon) {
        Vector2D vPoint = FOVFilter.geo2Vector(point);
        Vector2D[] vPolygon = new Vector2D[polygon.length];
        for (int i = 0; i < polygon.length; i++)
            vPolygon[i] = FOVFilter.geo2Vector(polygon[i]);
        return Vector2D.polygonContainsPoint(vPoint, vPolygon);
    }

    public static boolean segmentInsidePolygon(GeoPoint point0,
            GeoPoint point1, GeoPoint[] polygon) {
        Vector2D vPoint0 = FOVFilter.geo2Vector(point0);
        Vector2D vPoint1 = FOVFilter.geo2Vector(point1);
        Vector2D[] vPolygon = new Vector2D[polygon.length];
        for (int i = 0; i < polygon.length; i++)
            vPolygon[i] = FOVFilter.geo2Vector(polygon[i]);
        return Vector2D.segmentIntersectsOrContainedByPolygon(vPoint0, vPoint1,
                vPolygon);
    }

    public static boolean segmentArrayIntersectsOrContainedByPolygon(
            GeoPoint[] segments,
            GeoPoint[] polygon) {
        Vector2D[] vSegments = new Vector2D[segments.length];
        for (int i = 0; i < segments.length; i++)
            vSegments[i] = FOVFilter.geo2Vector(segments[i]);
        Vector2D[] vPolygon = new Vector2D[polygon.length];
        for (int i = 0; i < polygon.length; i++)
            vPolygon[i] = FOVFilter.geo2Vector(polygon[i]);
        return Vector2D.segmentArrayIntersectsOrContainedByPolygon(vSegments,
                vPolygon);
    }

    public static ArrayList<GeoPoint> segmentArrayIntersectionsWithPolygon(
            GeoPoint[] segments,
            GeoPoint[] polygon) {
        Vector2D[] vSegments = new Vector2D[segments.length];
        UTMPoint z = UTMPoint.fromGeoPoint(polygon[0]);
        String zone = z.getZoneDescriptor();
        for (int i = 0; i < segments.length; i++)
            vSegments[i] = FOVFilter.geo2Vector(segments[i]);
        Vector2D[] vPolygon = new Vector2D[polygon.length];
        for (int i = 0; i < polygon.length; i++)
            vPolygon[i] = FOVFilter.geo2Vector(polygon[i]);
        ArrayList<Vector2D> vIntersections = Vector2D
                .segmentArrayIntersectionsWithPolygon(
                        vSegments, vPolygon);
        ArrayList<GeoPoint> intersections = new ArrayList<>();
        for (Vector2D v : vIntersections) {
            intersections.add(vector2Geo(v, zone));
        }
        return intersections;
    }

    public static double computeDistanceOfRayToSegment(GeoPoint p,
            double azimuth, GeoPoint seg1,
            GeoPoint seg0) {
        UTMPoint uP = MutableUTMPoint.fromLatLng(Ellipsoid.WGS_84,
                p.getLatitude(),
                p.getLongitude(), null);
        Vector2D vP = FOVFilter.geo2Vector(p);
        Vector2D vSeg1 = FOVFilter.geo2Vector(seg1);
        Vector2D vSeg0 = FOVFilter.geo2Vector(seg0);
        double adjusted;
        if (azimuth >= 0d && azimuth <= 180d) {
            adjusted = Math.toRadians(azimuth);
        } else {
            adjusted = Math.toRadians(azimuth - 360d);
        }
        Vector2D vDir = new Vector2D(Math.sin(adjusted), Math.cos(adjusted));
        Vector2D intersect = Vector2D.rayToSegmentIntersection(vP, vDir, vSeg1,
                vSeg0);
        if (intersect != null) {
            UTMPoint uIntersect = new UTMPoint(uP.getZoneDescriptor(),
                    intersect.x, intersect.y);
            double[] ll = uIntersect.toLatLng(null);
            GeoPoint gpIntersect = new GeoPoint(ll[0], ll[1]);
            return GeoCalculations.distanceTo(p, gpIntersect);
        }
        return Double.POSITIVE_INFINITY;
    }

    /**
     * Given a vector from the start of the zone and a zone - utilize UTM to generate a geopoint.
     * @param v the vector offset from the zone described by easting (x) and northing (y).
     * @param zone the zone descriptor
     * @return the geopoint.
     */
    public static GeoPoint vector2Geo(final Vector2D v, final String zone) {
        UTMPoint uP = new UTMPoint(zone, v.x, v.y);
        double[] ll = uP.toLatLng(null);
        return new GeoPoint(ll[0], ll[1], v.alt);
    }

    /**
     * Find the shape, if any, associated with this map item
     * @param mi Map item to search
     * @return The associated shape or the input map item if none found
     */
    public static MapItem findAssocShape(MapItem mi) {
        MapView mv = MapView.getMapView();
        if (mi == null || mv == null)
            return mi;

        // General > Associated > R&B Line > Bullseye
        String shapeUID = mi.getMetaString("shapeUID", null);
        if (FileSystemUtils.isEmpty(shapeUID))
            shapeUID = mi.getMetaString("assocSetUID", null);
        if (FileSystemUtils.isEmpty(shapeUID))
            shapeUID = mi.getMetaString("rabUUID", null);
        if (FileSystemUtils.isEmpty(shapeUID))
            shapeUID = mi.getMetaString("bullseyeUID", null);

        if (!FileSystemUtils.isEmpty(shapeUID)) {
            MapItem shape = mv.getRootGroup().deepFindUID(shapeUID);
            if (shape instanceof Shape)
                mi = shape;
        }
        return mi;
    }

    /**
     * Get the Icon URI based on marker icon or 'iconUri' meta string
     * Meant to be used when displaying the map item in UI (not in MapView)
     * @param mi Map item
     * @return Map item icon URI
     */
    public static String getIconUri(MapItem mi) {
        if (mi == null)
            return null;

        String uri = null;

        // Hack for bullseye items
        // TODO: Refactor bullseye to work like every other shape - no more marker-centrism
        if (mi.getType().equals(BullseyeTool.BULLSEYE_COT_TYPE))
            return getResourceUri(R.drawable.bullseye);

        // Look for Marker icon
        if (mi instanceof Marker) {
            Marker mkr = (Marker) mi;
            // Use label icon for label markers
            if (mkr.getMetaString(UserIcon.IconsetPath, "").equals(
                    SpotMapPalletFragment.LABEL_ONLY_ICONSETPATH))
                uri = getResourceUri(R.drawable.enter_location_label_icon);
            else if (mkr.getIcon() != null)
                uri = mkr.getIcon().getImageUri(mkr.getState());
        }

        // Last look for general icon URI
        if (FileSystemUtils.isEmpty(uri) && mi.hasMetaValue("iconUri"))
            uri = mi.getMetaString("iconUri", null);
        return uri;
    }

    /**
     * Convert resource ID to URI string
     * @param context Resource context
     * @param resId Resource ID
     * @return Resource URI
     */
    public static String getResourceUri(Context context, int resId) {
        return "android.resource://" + context.getPackageName() + "/" + resId;
    }

    /**
     * Provided a resource identifier, return the full resource URI describing 
     * the resource.
     */
    public static String getResourceUri(int resId) {
        MapView mv = MapView.getMapView();
        return mv != null ? getResourceUri(mv.getContext(), resId) : null;
    }

    /**
     * Get color from a map item icon
     * @param mi the map item to use for finding the currently set icon color.
     * @return the color, WHITE if no color is found.
     */
    public static int getIconColor(MapItem mi) {
        int color = Color.WHITE;
        if (mi != null) {
            if (mi.getType().equals(BullseyeTool.BULLSEYE_COT_TYPE)) {
                mi = findAssocShape(mi);
            }

            if (mi instanceof Marker)
                color = mi.getIconColor();
            else if (mi instanceof Shape)
                color = mi.getIconColor();
            else {
                try {
                    if (mi != null)
                        color = mi.getMetaInteger("color", Color.WHITE);
                } catch (Exception ignore) {
                }
            }
        }
        return (color & 0xFFFFFF) + 0xFF000000;
    }

    /**
     * Set the icon for the user.
     *  Use current marker's icon
     *  Use self icon if item is 'self'
     *  Use friendly default
     *
     * @param view the map view to use when searching or the user.
     * @param icon the view to populate with the found icon.
     * @param uid the uid of the user to be found.
     */
    public static void SetUserIcon(MapView view, ImageView icon,
            String uid) {
        SetUserIcon(view, icon, uid, R.drawable.friendly);
    }

    /**
     * Set the icon for the user
     *  Use current marker's icon
     *  Use self icon if item is 'self'
     *  Use specified default
     *
     * @param view the mapView to use to find the online users.
     * @param icon the view to set with the found default icon.
     * @param uid the uid of the user.
     * @param defaultResource if the user is not online or on the map, use the defaultResource to represent the user.
     */
    public static void SetUserIcon(final MapView view, final ImageView icon,
            String uid,
            int defaultResource) {
        MapItem item = view.getRootGroup().deepFindUID(uid);
        if (item instanceof PointMapItem) {
            PointMapItem pmi = (PointMapItem) item;
            ATAKUtilities.SetIcon(view.getContext(), icon, pmi);
        } else {
            icon.clearColorFilter();
            boolean bSelf = item != null && FileSystemUtils
                    .isEquals(item.getUID(), MapView.getDeviceUid());
            String iconUri = "android.resource://"
                    + view.getContext().getPackageName()
                    + "/"
                    + (bSelf ? R.drawable.ic_self : defaultResource);
            ATAKUtilities.SetIcon(view.getContext(), icon, iconUri,
                    Color.WHITE);
        }

        //TODO use ComMapServerListener/ServerContact to get last known icon? And color it grey if currently offline
    }

    public static void SetUserIcon(MapView context, ImageView icon,
            PointMapItem item) {
        SetUserIcon(context, icon, item, R.drawable.friendly);
    }

    /**
     * Set the icon for the user
     *  Use current marker's icon
     *  Use specified default
     *
     * @param context the context to use when setting the icon
     * @param icon the icon to use
     * @param item the item to set the icon for
     * @param defaultResource the default resource
     */
    public static void SetUserIcon(MapView context, ImageView icon,
            PointMapItem item, int defaultResource) {
        if (item != null) {
            ATAKUtilities.SetIcon(context.getContext(), icon, item);
        } else {
            icon.clearColorFilter();
            icon.setImageResource(defaultResource);
        }
    }

    /**
     * Set the icon based on the map item's icon drawable
     *
     * @param view  The ImageView to display the icon
     * @param mi    The Map Item to load the icon from
     */
    public static void setIcon(ImageView view, MapItem mi) {
        Drawable dr = mi.getIconDrawable();
        if (dr != null) {
            view.setImageDrawable(dr);
            view.setColorFilter(mi.getIconColor(), PorterDuff.Mode.MULTIPLY);
            view.setVisibility(View.VISIBLE);
        } else
            view.setVisibility(View.INVISIBLE);
    }

    /**
     * Set the icon based on the iconUriStr
     * Parses all the support uri formats
     *
     * @param context the context to use
     * @param view  The ImageView to display the icon
     * @param mi    The Map Item to load the icon from
     */
    public static void SetIcon(final Context context, final ImageView view,
            final MapItem mi) {
        setIcon(view, mi);
    }

    /**
     * Set the icon based on the iconUriStr, must be run on the ui thread 
     * since it directly manipulates the passed in ImageView
     * Parses all the support uri formats
     *
     * <P>This call will block until the bitmap has been loaded. Use
     * {@link #setIcon(Context, ImageView, String, int)} as a non-blocking
     * variant.
     *
     * @param context
     * @param icon  The ImageView to display the icon
     * @param iconUriStr    The URI to load the icon from
     * @param color The color to be applied to the icon
     * @return Bitmap if one was created
     */
    public static Bitmap SetIcon(final Context context, final ImageView icon,
            final String iconUriStr, final int color) {
        Bitmap[] bitmap = new Bitmap[1];
        setIcon(context, icon, iconUriStr, color, bitmap);
        return bitmap[0];
    }

    /**
     * Set the icon based on the iconUriStr, must be run on the ui thread
     * since it directly manipulates the passed in ImageView
     * Parses all the support uri formats
     *
     * <P>This call will return immediately. If the icon cannot be loaded
     * immediately, {@link #ICON_LOADING} will be returned. Otherwise one
     * of {@link #ICON_LOADED} or {@link #ICON_FAILED} will be returned,
     * if the icon was or was not successfully set.
     *
     * @param context
     * @param icon  The ImageView to display the icon
     * @param iconUriStr    The URI to load the icon from
     * @param color The color to be applied to the icon
     *
     * @return  One of {@link #ICON_LOADED}, {@link #ICON_LOADING} or
     *          {@link #ICON_FAILED}. If {@link #ICON_LOADING} is
     *          returned, future calls to this method may be successful.
     */
    public static int setIcon(final Context context, final ImageView icon,
            final String iconUriStr, final int color) {

        return setIcon(context, icon, iconUriStr, color, null);
    }

    static Bitmap bitmapLoaderLoadIcon(String iconUriStr) {
        GLBitmapLoader glBitmapLoader = GLRenderGlobals.get(MapView
                .getMapView().getRenderer3().getRenderContext())
                .getBitmapLoader();
        final Future<Bitmap> future = glBitmapLoader.loadBitmap(
                iconUriStr, new BitmapFactory.Options());

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException ie) {
            return null;
        }
    }

    /**
     * See general contract on {@link #setIcon(Context, ImageView, String, int)}
     *
     * @param rbitmap   If non-{@code null}, returns the
     *
     * @return  One of {@link #ICON_LOADED}, {@link #ICON_LOADING} or
     *          {@link #ICON_FAILED}. If {@link #ICON_LOADING} is
     *          returned, future calls to this method may be successful.
     */
    static int setIcon(final Context context, final ImageView icon,
            final String iconUriStr, final int color, Bitmap[] rbitmap) {
        if (icon == null) {
            return ICON_FAILED;
        }

        Bitmap ret = null;
        int iconStatus = ICON_FAILED;
        if (iconUriStr != null && !iconUriStr.trim().isEmpty()) {

            icon.setVisibility(View.VISIBLE);

            final Uri iconUri = Uri.parse(iconUriStr);

            String scheme = iconUri.getScheme();
            String path = getUriPath(iconUriStr);
            if (scheme == null)
                scheme = "";

            switch (scheme) {
                case "resource":
                    String properResourceUri = "android.resource://"
                            + context.getPackageName() + "/"
                            + path;
                    icon.setImageURI(Uri.parse(properResourceUri));
                    break;
                case "android.resource":
                    List<String> segs = iconUri.getPathSegments();
                    if (segs.isEmpty()) {
                        // support for:       android.resource://id_number
                        String[] split = path.split("/");
                        String properAndroidResourceUri = "android.resource://"
                                + context.getPackageName() + "/"
                                + FileSystemUtils.sanitizeWithSpacesAndSlashes(
                                        split[split.length - 1]);
                        icon.setImageURI(Uri.parse(properAndroidResourceUri));
                    } else {
                        icon.setImageURI(iconUri);
                    }
                    break;
                case "http":
                case "https":
                    icon.setColorFilter(color);
                    // NOTE: this method may only be invoked on UI thread per
                    // contract, so initial check+set will be thread-safe and
                    // avoid redundant loading of same icon
                    final Integer status = _remoteIconLoadStatus
                            .get(iconUriStr);
                    if (rbitmap != null) {
                        // blocking load bitmap
                        ret = bitmapLoaderLoadIcon(iconUriStr);
                        if (ret == null)
                            icon.setVisibility(View.INVISIBLE);
                        icon.setImageBitmap(ret);
                        // set the status
                        _remoteIconLoadStatus.put(iconUriStr,
                                (ret != null) ? 1 : -1);
                    } else if (status == null) {
                        _remoteIconLoadStatus.put(iconUriStr, 0);
                        _remoteIconExecutor.execute(new Runnable() {
                            public void run() {
                                final Bitmap bmp = bitmapLoaderLoadIcon(
                                        iconUriStr);
                                final int status = (bmp != null) ? 1 : -1;
                                _remoteIconLoadStatus.put(iconUriStr, status);
                            }
                        });
                        iconStatus = ICON_LOADING;
                    } else {
                        switch (status) {
                            case ICON_LOADED: // successfully loaded, proceed with blocking
                                ret = bitmapLoaderLoadIcon(iconUriStr);
                                if (ret == null)
                                    icon.setVisibility(View.INVISIBLE);
                                icon.setImageBitmap(ret);
                                break;
                            case ICON_LOADING: // loading, do nothing
                            case ICON_FAILED: // failed previous load
                            default: // illegal state
                                icon.setImageBitmap(null);
                                icon.setVisibility(View.INVISIBLE);
                                break;
                        }
                    }
                    break;
                case "zip":
                    icon.setColorFilter(color);
                    ret = bitmapLoaderLoadIcon(iconUriStr);
                    if (ret == null)
                        icon.setVisibility(View.INVISIBLE);
                    icon.setImageBitmap(ret);
                    break;
                case "base64":
                    String image = iconUriStr
                            .substring(
                                    (scheme != null) ? scheme.length() + 2 : 0);
                    if (image.startsWith("/")) {
                        image = image.substring(1);
                    }
                    try {
                        byte[] buf = Base64.decode(
                                image.getBytes(FileSystemUtils.UTF8_CHARSET),
                                Base64.URL_SAFE | Base64.NO_WRAP);
                        ret = BitmapFactory.decodeByteArray(buf, 0, buf.length);
                        icon.setImageBitmap(ret);
                    } catch (Exception e) {
                        icon.setVisibility(View.INVISIBLE);
                    }
                    break;
                default:
                    ret = getUriBitmap(iconUriStr);
                    if (ret != null)
                        setIconBitmap(icon, ret);
                    else
                        icon.setVisibility(View.INVISIBLE);
                    break;
            }
            icon.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        } else {
            //Log.w(TAG, "item: " + item.getTitle() + ", no iconUri");
            icon.setVisibility(View.INVISIBLE);
        }

        if (rbitmap != null)
            rbitmap[0] = ret;

        if (iconStatus != ICON_LOADING)
            iconStatus = (ret != null) ? ICON_LOADED : ICON_FAILED;
        return iconStatus;
    }

    /**
     * Generate a bitmap for this map item's icon
     * @param item Map item
     * @return Icon bitmap
     */
    public static Bitmap getIconBitmap(final MapItem item) {
        final String milsym = item.getMetaString("milsym", null);
        final String iconsetPath = item.getMetaString(UserIcon.IconsetPath,
                null);

        // If we should adapt the marker icon, then if iconsetPath is not set or if the iconset path
        // starts with a 2525 marker - then rely on the milsym if it is set.
        if (item.getMetaBoolean("adapt_marker_icon", true)
                && (iconsetPath == null || iconsetPath.startsWith(Icon2525cPallet.COT_MAPPING_2525))
                && milsym != null && !milsym.isEmpty()) {
            gov.tak.api.commons.graphics.Bitmap bmp = SymbologyProvider
                    .renderSinglePointIcon(milsym, null, null);
            Bitmap abmp = MarshalManager.marshal(bmp,
                    gov.tak.api.commons.graphics.Bitmap.class, Bitmap.class);
            if(abmp != null)
                return abmp;
        }

        return getUriBitmap(getIconUri(item));
    }

    /**
     * Convert a uriStr into a path keeping legacy behavior.   Does not account
     * for the hostname being included in the path.  We also do not support file uri
     * with two slashes "used to access files in a remote system."
     * @param uriStr the uri string { file, android, android.resource, base64 or
     *               no scheme at all}
     * @return the path of the uriStr
     */
    public static String getUriPath(String uriStr) {
        String path = uriStr;

        if (uriStr.startsWith("file://") && !uriStr.startsWith("file:///"))
            uriStr = uriStr.replace("file://", "file:///");
        else if (uriStr.startsWith("/"))
            uriStr = "file://" + uriStr;

        final Uri uri = Uri.parse(uriStr);
        final String scheme = uri.getScheme();

        if (scheme != null && !scheme.isEmpty()) {

            // properly decode file uri (with 3 slashes / not 2)
            if (scheme.equals("file")) {
                String retval = Uri.decode(uri.getPath());
                return retval;
            }

            // Old method
            path = uriStr.substring(scheme.length() + 1);
            // Takes care of cases where there's only one slash
            // i.e. asset:/icons/icon.png
            while (!path.isEmpty() && path.charAt(0) == '/')
                path = path.substring(1);
        }
        return path;
    }

    /**
     * Register a bitmap decoder with the system that will be called when the uri scheme is encountered.
     * @param scheme the uri scheme supported.
     * @param uriBitmapDecoder the decoder used to process the uri.
     */
    public static void registerBitmapDecoder(String scheme,
            BitmapDecoder uriBitmapDecoder) {
        decoders.put(scheme, uriBitmapDecoder);
    }

    /**
     * Unregister the uriBitmapDecoder for a specified scheme.
     * @param scheme the uri scheme for the bitmap decoder to remove.
     */
    public static void unregisterBitmapDecoder(final String scheme) {
        decoders.remove(scheme);
    }

    /**
     * Decode URI to its matching bitmap
     * @param uriStr URI string to decode
     * @return Icon bitmap the bitmap that can be recycled by the user at any time.
     */
    public static Bitmap getUriBitmap(String uriStr) {
        final MapView mapView = MapView.getMapView();
        if (mapView != null)
            return getUriBitmap(mapView.getContext(), uriStr);
        else
            return null;
    }

    /**
     * Decode URI to its matching bitmap
     * @param uriStr    URI string to decode
     * @param ctx       The application context
     * @return Icon bitmap the bitmap that can be recycled by the user at any time.
     */
    public static Bitmap getUriBitmap(Context ctx, String uriStr) {
        if (uriStr == null)
            return null;

        Uri iconUri = Uri.parse(uriStr);
        String scheme = iconUri.getScheme();
        String path = getUriPath(uriStr);
        if (scheme == null)
            scheme = "";
        if (scheme.equals("asset") || scheme.equals("root")) {
            if (path.startsWith(Icon2525cPallet.ASSET_PATH)) {
                try {
                    final String p = path
                            .replace(Icon2525cPallet.ASSET_PATH, "")
                            .replace(".png", "").toUpperCase(LocaleUtil.US);
                    gov.tak.api.commons.graphics.Bitmap bmp = SymbologyProvider
                            .renderSinglePointIcon(p,
                                    new AttributeSet(), null);
                    return MarshalManager.marshal(bmp,
                            gov.tak.api.commons.graphics.Bitmap.class,
                            Bitmap.class);
                } catch (Exception e) {
                    Log.e(TAG, "error occurred obtaining " + path
                            + " from the provider for ", e);
                    return null;
                }
            } else {
                try {
                    InputStream in = ctx.getAssets().open(path);
                    Bitmap b = BitmapFactory.decodeStream(in);
                    in.close();
                    return b;
                } catch (IOException e) {
                    return null;
                }
            }
        } else if ((scheme.equals("resource")
                || scheme.equals("android.resource"))
                && path.contains("/")) {
            try {
                String packageName = path.substring(0, path.indexOf("/"));
                Context resCtx = ctx;
                try {
                    resCtx = ctx.createPackageContext(packageName, 0);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Failed to find context for icon: "
                            + packageName);
                }
                int resId = Integer.parseInt(path
                        .substring(path.lastIndexOf("/") + 1));
                return BitmapFactory.decodeResource(
                        resCtx.getResources(), resId);
            } catch (NumberFormatException nfe) {
                Log.d(TAG,
                        "unable to extract ID from the provided Uri: "
                                + uriStr);
                return null;
            }
        } else if (scheme.equals("sqlite")) {
            return UserIcon.GetIconBitmap(uriStr, ctx);
        } else if (scheme.equals("zip") || scheme.equals("https")
                || scheme.equals("http")) {
            return bitmapLoaderLoadIcon(uriStr);
        } else if (uriStr.startsWith(BASE64)
                || uriStr.startsWith(BASE64_PNG)) {
            // Base-64 image
            if (uriStr.startsWith(BASE64))
                uriStr = uriStr.substring(uriStr.indexOf(BASE64)
                        + BASE64.length());
            try {
                byte[] b64 = Base64.decode(uriStr, Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(b64, 0, b64.length);
            } catch (Exception e) {
                Log.d(TAG, "Failed to decode base-64 PNG", e);
            }
            return null;
        } else if (uriStr.startsWith(BASE64_2)) {
            uriStr = uriStr.substring(uriStr.indexOf(BASE64_2)
                    + BASE64_2.length());
            try {
                byte[] b64 = Base64.decode(
                        uriStr.getBytes(FileSystemUtils.UTF8_CHARSET),
                        Base64.URL_SAFE | Base64.NO_WRAP);
                return BitmapFactory.decodeByteArray(b64, 0, b64.length);
            } catch (Exception e) {
                Log.d(TAG, "Failed to decode base-64 regular icon", e);
            }

            return null;

        } else {
            final BitmapDecoder decoder = decoders.get(scheme);
            if (decoder != null) {
                try {
                    Bitmap r = decoder.decodeUri(uriStr);
                    // if the bitmap return is null, then continue on.
                    if (r != null)
                        return r;
                } catch (Exception e) {
                    Log.d(TAG, "error decoding: " + uriStr + " by: " + decoder);
                }
            }
            try (FileInputStream fis = IOProviderFactory
                    .getInputStream(new File(FileSystemUtils
                            .validityScan(path)))) {
                return BitmapFactory.decodeStream(fis);
            } catch (IOException ioe) {
                return null;
            }
        }
    }

    /**
     * Convert list of strings to a JSON array
     * @param strings the list of string
     * @return a list of strings into a JSON string array.
     */
    public static String toJSON(List<String> strings) {
        JSONArray a = new JSONArray();
        for (int i = 0; i < strings.size(); i++) {
            a.put(strings.get(i));
        }
        return a.toString();
    }

    /**
     * Convert JSON string into a list of strings
     * @param json the original encoded json document.
     * @return the list of Strings
     */
    public static List<String> fromJSON(String json) {
        List<String> strings = new ArrayList<>();
        if (!FileSystemUtils.isEmpty(json)) {
            try {
                JSONArray a = new JSONArray(json);
                for (int i = 0; i < a.length(); i++) {
                    String s = a.optString(i);
                    if (FileSystemUtils.isEmpty(s)) {
                        Log.w(TAG, "Unable to parse string");
                        continue;
                    }
                    strings.add(s);
                }
            } catch (JSONException e) {
                Log.w(TAG, "Unable to get string", e);
            }
        }
        return strings;
    }

    /**
     * Turn a byte array into a hex string representation
     * @param arr the byte array
     * @return the corresponding string
     */
    public static String bytesToHex(byte[] arr) {
        StringBuilder retval = new StringBuilder();
        retval.ensureCapacity(arr.length * 2);
        for (final int v : arr) {
            retval.append(HEX_DIGITS[(v >> 4) & 0xF]);
            retval.append(HEX_DIGITS[(v & 0xF)]);
        }
        return retval.toString();
    }

    /**
     * Given a hex string, decode it into a byte array.
     * @param hex a hex string containing characters [0-F]
     * @return the corresponding byte array
     * @throws IllegalArgumentException if the string contains a non hex character
     */
    public static byte[] hexToBytes(String hex) {
        byte[] retval = new byte[(hex.length() + 1) / 2];
        for (int i = hex.length() - 1; i >= 0; i -= 2) {
            int v = 0;
            v |= decodeHex(hex.charAt(i));
            if (i > 0)
                v |= decodeHex(hex.charAt(i - 1)) << 4;
            retval[i / 2] = (byte) v;
        }
        return retval;
    }

    private static int decodeHex(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if ((c & ~0x20) >= 'A' && (c & ~0x20) <= 'F') {
            return ((char) (c & ~0x20) - 'A') + 10;
        } else {
            throw new IllegalArgumentException("Not a hex character: " + c);
        }
    }

    /**
     * Given a list of names, find the next non-duplicate name based on newName
     * @param newName New name - i.e. ItemName
     * @param names List of names - i.e. ItemName, ItemName (1), ItemName (2)
     * @param format Format where '#' is replaced by the dupe number - i.e. " (#)"
     * @return New non-duplicate name - i.e. ItemName (3)
     */
    public static String getNonDuplicateName(String newName,
            List<String> names, String format) {
        if (FileSystemUtils.isEmpty(format) || !format.contains("#")) {
            Log.w(TAG,
                    "getNonDuplicateName invalid format: missing \"#\" in string");
            format = " (#)";
        }
        String fStart = format.substring(0, format.indexOf("#"));
        String fEnd = format.substring(format.lastIndexOf("#") + 1);
        int maxNum = 1;
        SparseArray<String> dupes = new SparseArray<>();
        for (String name : names) {
            if (name.startsWith(newName)) {
                if (name.equals(newName))
                    // This is the first duplicate
                    dupes.put(1, name);
                else {
                    String diff = name.substring(newName.length());
                    if (diff.startsWith(fStart) && diff.endsWith(fEnd)) {
                        try {
                            // Get the dupe number in parentheses
                            int v = Integer.parseInt(diff.substring(
                                    fStart.length(),
                                    diff.length() - fEnd.length()));
                            dupes.put(v, name);
                            maxNum = Math.max(maxNum, v);
                        } catch (Exception ignore) {
                            Log.d(TAG,
                                    "parsing error typing to get a non-duplicative number");
                        }
                    }
                }
            }
        }
        for (int i = 1; i <= maxNum + 1; i++) {
            if (dupes.get(i) == null) {
                if (i > 1)
                    // Only include number if not the first duplicate
                    newName += fStart + i + fEnd;
                break;
            }
        }
        return newName;
    }

    /**
     * Given a file and a imageview, set the image from the file as part of the icon and then if
     * sucessful, return the bitmap constructed from the file.
     * @param icon the imageview used for the bitmap from the file.
     * @param iconFile the file containing the image.
     * @return the bitmap if the file is loaded correctly.
     */
    public static Bitmap setIconFromFile(ImageView icon, File iconFile) {
        Bitmap bitmap = null;
        if (IOProviderFactory.exists(iconFile)) {
            try (InputStream is = IOProviderFactory.getInputStream(iconFile)) {
                bitmap = BitmapFactory.decodeStream(is);
            } catch (IOException ioe) {
                return null;
            } catch (RuntimeException ignored) {
            }
        }
        setIconBitmap(icon, bitmap);
        return bitmap;
    }

    /**
     * Given a bitmap and a imageview, set the imageview from the bitmap.
     * @param icon the imageview used for the bitmap from the file.
     * @param bitmap the  the image.
     */
    private static void setIconBitmap(ImageView icon, Bitmap bitmap) {
        if (bitmap == null)
            icon.setVisibility(View.INVISIBLE);
        else
            icon.setImageBitmap(bitmap);
    }

    /**
     * Create a Bitmap from the specified drawable
     *
     * @param drawable the drawable to turn into a bitmap.
     * @return the bitmap representation of the drawable.
     */
    public static Bitmap getBitmap(Drawable drawable) {
        Bitmap result;
        if (drawable instanceof BitmapDrawable) {
            result = ((BitmapDrawable) drawable).getBitmap();
        } else {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            // Some drawables have no intrinsic width - e.g. solid colours.
            if (width <= 0) {
                width = 1;
            }
            if (height <= 0) {
                height = 1;
            }

            result = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        return result;
    }

    /**
     * Method to assist in the copying of text data to the clipboard supporting cut and paste.
     * @param label the label defining the data
     * @param text the actual data to be copied
     * @param bToast if true, displays a toast.
     * @since 3.8
     */
    public static void copyClipboard(final String label, final String text,
            final boolean bToast) {
        Context c = MapView.getMapView().getContext();
        ClipboardManager clipboard = (ClipboardManager) c
                .getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        try {
            if (clipboard != null)
                clipboard.setPrimaryClip(clip);

            if (bToast)
                Toast.makeText(c, R.string.copied_to_clipboard_generic,
                        Toast.LENGTH_SHORT)
                        .show();

        } catch (SecurityException se) {
            // simple fix for ATAK-16501
            Log.d(TAG, "unable to set primary clip", se);
            if (bToast)
                Toast.makeText(c, "error occurred during copy and paste",
                        Toast.LENGTH_SHORT)
                        .show();
        }

    }

    // Content type -> drawable icon based on import resolver
    private static final Map<String, Drawable> contentIcons = new HashMap<>();
    private static final Map<String, Drawable> fileMimeIcons = new HashMap<>();

    /**
     * Get an icon for a given file content type
     * @param contentType Content type
     * @return Icon or null if N/A
     */
    public static Drawable getContentIcon(String contentType) {
        if (contentType == null)
            return null;

        // Import resolver icons
        // To avoid calling this method excessively, cache results where possible
        synchronized (contentIcons) {
            // Cached icon
            Drawable icon = contentIcons.get(contentType);
            if (icon != null)
                return icon;

            Collection<ImportResolver> resolvers = null;

            if (contentIcons.isEmpty()) {
                // Build icons for default resolvers
                MapView mv = MapView.getMapView();
                if (mv != null)
                    resolvers = ImportFilesTask.GetKernelSorters(mv.getContext(), false);
            } else {
                // New resolver icons that may have been registered since
                ImportExportMapComponent iemc = ImportExportMapComponent
                        .getInstance();
                if (iemc != null)
                    resolvers = iemc.getKernelImporterResolvers();
            }

            if (resolvers == null || resolvers.isEmpty())
                return null;

            // Read icons from resolvers
            for (ImportResolver res : resolvers) {
                icon = MarshalManager.marshal(res.getIcon(), gov.tak.api.commons.graphics.Drawable.class, Drawable.class);
                if (icon == null)
                    continue;
                Pair<String, String> p = res.getContentMIME();
                if (p == null || p.first == null)
                    continue;
                contentIcons.put(p.first, icon);
            }

            ImportExportMapComponent.getInstance().cleanupResolvers(new ArrayList<>(resolvers));

            // Last attempt to read from cached icons
            return contentIcons.get(contentType);
        }
    }

    /**
     * Get an icon for a file
     * Note: If the file does not exist it's recommended to use
     * {@link #getContentIcon(String)} provided you have the content type
     * @param f File
     * @return File icon or null if N/A
     */
    public static Drawable getFileIcon(File f) {
        if (f == null)
            return null;

        MapView mv = MapView.getMapView();
        Context ctx = mv != null ? mv.getContext() : null;

        // Content handler icon
        URIContentHandler handler = URIContentManager.getInstance()
                .getHandler(f);
        if (handler != null)
            return handler.getIcon();

        if (ctx == null)
            return null;

        // Images
        if (ImageDropDownReceiver.ImageFileFilter.accept(null, f.getName()))
            return ctx.getDrawable(R.drawable.camera);

        ResourceFile.MIMEType mime = ResourceFile
                .getMIMETypeForFile(f.getName());
        if (mime == null)
            return null;

        Bitmap bmp = null;
        do {
            synchronized (fileMimeIcons) {
                if (bmp != null) // cache the loaded bitmap
                    fileMimeIcons.put(mime.ICON_URI,
                            new BitmapDrawable(ctx.getResources(),
                                    bmp));
                // retrieve bitmap from cache
                Drawable retval = fileMimeIcons.get(mime.ICON_URI);
                // if there's a cached bitmap, return it
                if (retval != null)
                    return retval;
            }
            // load bitmap
            bmp = getUriBitmap(mime.ICON_URI);
            if (bmp == null)
                return null;
        } while (true);
    }

    /**
     * Returns the appropriate starting directory for file dialogs. If the "defaultDirectory" shared
     * preference exists, it will use the value. Otherwise, the "lastDirectory" shared preference
     * value will be used.
     *
     * @param sharedPrefContext The context of the shared preferences to use
     * @return the "defaultDirectory" shared preference, if exists, it will use the value.
     * Otherwise, the "lastDirectory" shared preference value will be used.
     */
    public static String getStartDirectory(Context sharedPrefContext) {
        AtakPreferences Prefs = AtakPreferences.getInstance(sharedPrefContext);
        String defaultDirectory = Prefs.get("defaultDirectory", "");
        final String lastDirectory = Prefs.get("lastDirectory",
                Environment.getExternalStorageDirectory().getPath());
        if (defaultDirectory.isEmpty()
                || !IOProviderFactory.exists(new File(defaultDirectory))) {
            return lastDirectory;
        } else {
            return defaultDirectory;
        }
    }

    private static double wcs_distance(PointD a, PointD b,
            MapProjectionDisplayModel displayModel) {
        return MathUtils.distance(
                a.x * displayModel.projectionXToNominalMeters,
                a.y * displayModel.projectionYToNominalMeters,
                a.z * displayModel.projectionZToNominalMeters,
                b.x * displayModel.projectionXToNominalMeters,
                b.y * displayModel.projectionYToNominalMeters,
                b.z * displayModel.projectionZToNominalMeters);
    }

    /**
     * Get the estimated meters per pixel at a given point on the screen
     * @param x X coordinate on the screen
     * @param y Y coordinate on the screen
     * @return Meters or {@link Double#NaN} if could not be calculated
     */
    public static double getMetersPerPixel(float x, float y) {
        MapView mapView = MapView.getMapView();
        if (mapView == null)
            return Double.NaN;
        GeoPoint p1 = mapView.inverse(x, y).get();
        GeoPoint p2 = mapView.inverse(x + 1, y + 1).get();
        return p1.isValid() && p2.isValid() ? p1.distanceTo(p2) : Double.NaN;
    }

    /**
     * Get the estimated meters per pixel at a given point on the screen
     * @param point Point on the screen (x, y)
     * @return Meters or {@link Double#NaN} if could not be calculated
     */
    public static double getMetersPerPixel(PointF point) {
        return getMetersPerPixel(point.x, point.y);
    }

    /**
     * Get the estimated meters per pixel at a given point on the map
     * @param point Point on the map
     * @return Meters or {@link Double#NaN} if could not be calculated
     */
    public static double getMetersPerPixel(GeoPoint point) {
        MapView mapView = MapView.getMapView();
        if (mapView == null)
            return Double.NaN;
        return getMetersPerPixel(mapView.forward(point));
    }

    /**
     * Get the estimated meters per pixel at the default focus point
     * @return Meters or {@link Double#NaN} if could not be calculated
     */
    public static double getMetersPerPixel() {
        MapView mapView = MapView.getMapView();
        if (mapView == null)
            return Double.NaN;
        MapSceneModel mdl = mapView.getSceneModel();
        return getMetersPerPixel(mdl.focusx, mdl.focusy);
    }

    /**
     * Helper method for {@link SpatialCalculator#simplify(Collection, double, boolean)}
     * Automatically calculates degree threshold using the current DPI
     * @param calc Spatial calculator
     * @param points List of points
     * @return List of simplified points or null if the simplification process failed
     */
    public static List<GeoPoint> simplifyPoints(SpatialCalculator calc,
            Collection<GeoPoint> points) {
        double thresh = 0;
        MapView mapView = MapView.getMapView();
        if (mapView != null) {
            float dp = mapView.getResources().getDisplayMetrics().density;
            MapSceneModel scene = mapView.getSceneModel();
            GeoPoint p1 = mapView.inverse(scene.focusx, scene.focusy).get();
            GeoPoint p2 = mapView.inverse(scene.focusx, scene.focusy + dp)
                    .get();
            double threshX = Math.abs(p1.getLongitude() - p2.getLongitude());
            double threshY = Math.abs(p1.getLatitude() - p2.getLatitude());
            thresh = Math.max(threshX, threshY);
        }

        final Collection<GeoPoint> simplified = calc.simplify(points, thresh,
                true);
        // if simplification cannot be done calc.simplfy returns null which causes an exception
        if (simplified == null)
            return null;
        else
            return simplified instanceof List ? (List<GeoPoint>) simplified
                    : new ArrayList<>(simplified);
    }

    /**
     * Get a Secure Random string of the specified length, encoded in Base64
     *
     * @param length in range [1, 65536]
     * @return
     */
    public static String getRandomString(int length) {
        return new String(
                Base64.encode(getRandomBytes(length),
                        Base64.URL_SAFE | Base64.NO_WRAP),
                FileSystemUtils.UTF8_CHARSET);
    }

    /**
     * Get a Secure Random byte array of the specified length
     *
     * @param length in range [1, 65536]
     * @return
     */
    public static byte[] getRandomBytes(int length) {
        if (length < 1)
            length = 1;
        else if (length > 65536)
            length = 65536;

        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    /**
     * This ensures that the valid group colors are being used - otherwise
     * it will default to a high contrast non-user settable group "Pink".
     * @param groupName the group name as received in the CoT message
     * @return the normalized group name
     */
    public static String ensureValidGroup(final String groupName) {
        // a pink group does not exist and indicates an error with the group name
        String retval = "Pink";
        switch (groupName.toLowerCase(LocaleUtil.getCurrent())) {
            case "white":
                retval = "White";
                break;
            case "orange":
                retval = "Orange";
                break;
            case "maroon":
                retval = "Maroon";
                break;
            case "purple":
                retval = "Purple";
                break;
            case "dark blue":
                retval = "Dark Blue";
                break;
            case "teal":
                retval = "Teal";
                break;
            case "dark green":
                retval = "Dark Green";
                break;
            case "brown":
                retval = "Brown";
                break;
            case "cyan":
                retval = "Cyan";
                break;
            case "blue":
                retval = "Blue";
                break;
            case "green":
                retval = "Green";
                break;
            case "red":
                retval = "Red";
                break;
            case "magenta":
                retval = "Magenta";
                break;
            case "yellow":
            case "rad sensor":
                retval = "Yellow";
                break;
        }
        return retval;

    }

    /**
     * Given a map item, set the fields required for proper handling of the author information
     * @param mapItem the map item
     */
    public static void setAuthorInformation(final MapItem mapItem) {

        // On creation of a mapItem, record the producer UID and the
        // type at that moment - although the type could change in the future
        // it is best to know what the producer was when the item was produced.
        MapView mapView = MapView.getMapView();
        Marker self = mapView.getSelfMarker();
        mapItem.setMetaString("parent_uid", self.getUID());
        mapItem.setMetaString(
                "parent_type",
                mapView.getMapData()
                        .getMetaString("deviceType", "a-f-G"));
        mapItem.setMetaString("parent_callsign", mapView.getDeviceCallsign());
        mapItem.setMetaString("production_time",
                new CoordinatedTime().toString());
    }

    /**
     * Returns the best possible resolution of data or the best possible DTED ignoring
     * all other elevation sources based on the system preference.
     * @param lat the latitude to pull the elevation from
     * @param lon the longitude to pull the elevation from
     * @param p If it is null if only the raw elevation is desired as meters HAE.
     *          Double.NaN is returned if no elevation is found.
     */
    public static double getElevation(double lat, double lon,
            GeoPointMetaData p) {
        final AtakPreferences pref = AtakPreferences.getInstance(null);
        final boolean forceDtedElevationOnly = pref
                .get("force_dted_elevation_only", false);
        return ElevationManager.getElevation(lat, lon,
                forceDtedElevationOnly ? DTED_FILTER : null, p);
    }

    /**
     * Returns the best possible resolution of data or the best possible DTED ignoring
     * all other elevation sources based on the system preference.
     * @param lat the latitude to pull the elevation from
     * @param lon the longitude to pull the elevation from
     */
    public static GeoPointMetaData getElevationMetadata(double lat,
            double lon) {
        GeoPointMetaData gpm = new GeoPointMetaData();
        getElevation(lat, lon, gpm);
        return gpm;
    }

    /**
     * Perform a check on the device to determine if it is a tablet vs a phone
     * @return true if it is a tablet or false if it is a phone
     */
    public static boolean isTablet() {
        final DisplayMetrics metrics = new DisplayMetrics();

        MapView _mapView = MapView.getMapView();
        if (_mapView == null)
            return false;

        final Activity a = ((Activity) _mapView.getContext());
        WindowManager wm = a.getWindowManager();
        if (wm != null) {
            Display display = wm.getDefaultDisplay();
            if (display != null) {
                display.getMetrics(metrics);
                double yInches = metrics.heightPixels / metrics.ydpi;
                double xInches = metrics.widthPixels / metrics.xdpi;
                double diagonalInches = Math
                        .sqrt((xInches * xInches) + (yInches * yInches));
                return diagonalInches >= 6.5;
            }
        }
        return false;
    }

}
