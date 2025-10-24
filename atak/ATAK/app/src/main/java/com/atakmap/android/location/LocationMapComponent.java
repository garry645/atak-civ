
package com.atakmap.android.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings.Secure;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.atakmap.android.gui.HintDialogHelper;
import com.atakmap.android.icons.Icon2525cIconAdapter;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.location.framework.Location;
import com.atakmap.android.location.framework.LocationDerivation;
import com.atakmap.android.location.framework.LocationManager;
import com.atakmap.android.location.framework.LocationProvider;
import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.maps.Ellipse;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapMode;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.Marker.OnTrackChangedListener;
import com.atakmap.android.maps.MetaDataHolder2;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.PointMapItem.OnPointChangedListener;
import com.atakmap.android.navigation.views.NavView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.selfcoordoverlay.SelfCoordOverlayUpdater;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.IconUtilities;
import com.atakmap.android.util.NotificationUtil;
import com.atakmap.app.Permissions;
import com.atakmap.app.R;
import com.atakmap.comms.ReportingRate;
import com.atakmap.coremap.concurrent.NamedThreadFactory;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.assets.Icon;
import com.atakmap.coremap.maps.conversion.GeomagneticField;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.map.AtakMapController;
import com.atakmap.map.CameraController;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.util.zip.IoUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import gov.tak.api.annotation.NonNull;

/**
 * Provides a location Marker and various device information.
 * <p>
 * Provided <i>Global Map Data</i>:
 * <ul>
 * <li>{@link java.lang.String String} <b>deviceType</b> - from preferences (default: "a-f-G-U-C-I")
 * </li>
 * <li>{@link java.lang.String String} <b>deviceCallsign</b> - from preferences (default: "ATAK")</li>
 * <li>{@link java.lang.String String} <b>devicePhoneNumber</b> - requires
 * android.permission.READ_PHONE_STATE</li>
 * <li>{@link com.atakmap.coremap.maps.coords.GeoPoint GeoPoint} <b>fineLocation</b> - requires
 * android.permission.ACCESS_FINE_LOCATION and enabled in preferences</li>
 * <li>{@code double} <b>fineLocationBearing</b> - requires android.permission.ACCESS_FINE_LOCATION
 * and enabled in preferences</li>
 * <li>{@code double} <b>fineLocationSpeed</b> - requires android.permission.ACCESS_FINE_LOCATION
 * and enabled in preferences</li>
 * <li>{@code double} <b>deviceAzimuth</b> - modifications to deviceAzimuth outside of 
 * LocationMapComponent will not be used by LocationMapComponent since this is just for read only 
 * purposes.    This bundle variable mirrors the local field mapDataDeviceAzimuth.
 * </ul>
 * </p>
 * <p>
 * </p>
 *
 */

public class LocationMapComponent extends AbstractMapComponent implements
        SensorEventListener,
        SharedPreferences.OnSharedPreferenceChangeListener,
        LocationManager.LocationProviderListChangeListener {

    final static String UID_PREFIX = "ANDROID-";

    private final float[] Rbuf = new float[9];
    private final float[] values = new float[3];

    final private static int SELF_MARKER_WIDTH = 32;
    final private static int SELF_MARKER_HEIGHT = 43;

    private String _deviceCallsign;
    private String _deviceType;
    private String _deviceTeam;
    private String _deviceCustomRoleAbbreviation;
    private String _deviceCustomRole;
    private float[] _gravityMatrix;// = new float[9];
    private float[] _geoMagnetic;// = new float[3];
    private SensorManager _sensorMgr;
    private Sensor accelSensor = null;
    private Sensor magSensor = null;
    private double _orientationOffset = 0d;
    private Marker _locationMarker;
    private MapGroup _locationGroup;

    private static final int CLOCK_CHANGE_WAIT = 5000;

    private boolean useOnlyGPSBearing = false;

    private MapView _mapView;
    private Context context;
    private final AlphaBetaFilter _filteredHeading = new AlphaBetaFilter(
            _FILTER_ALPHA, _FILTER_BETA);
    private long _lastHeadingUpdate = 0;
    private long _lastMarkerRefresh = 0;

    private double _lastMeasuredHeading = 0;
    private boolean lastMeasurementInvalid = false;

    private Ellipse _accuracyEllipse;

    private final ScheduledExecutorService headingRefreshTimer = Executors
            .newScheduledThreadPool(1);
    private ScheduledFuture<?> headingRefreshTask;

    private static final ExecutorService locationUpdateWorker = Executors
            .newFixedThreadPool(1,
                    new NamedThreadFactory("publishprompt-refreshfeed"));

    private static final double MILLIS_IN_SECOND = 1000d;

    private static final double VALID_GPS_BEARING_SPEED = 0.44704; // 1 mph

    private static final double _FILTER_GRANULARITY = 1d / 30d;
    private static final double _FILTER_ALPHA = .1d; // .1 or .2 seems to work
    // well here
    private static final double _FILTER_BETA = 0.005d; // 0.005 or other very
    // low values (maybe
    // .01) seem to work
    // well here.

    // This is the rate that the self marker will visually update when no 
    // other actions are occurring.
    private static final int REFRESH_RATE = 1000;

    // This is the rate that the self location Marker will update its 
    // heading.
    private static final int HEADING_REFRESH_RATE = 150;

    // Just in case additional updates come in for the Markers track change
    // ignore ones that come faster.
    private static final int HEADING_REFRESH_MIN_ELAPSE_MS = 100;

    private static final long GPS_ROLLOVER_CORRECTION_VALUE = 1024L * 7 * 24
            * 60 * 60 * 1000L;

    private static final long GPS_ROLLOVER_DETECTION_VALUE = 1200000000000L; // Thu Jan 10 2008 21:20:00

    private SpeedComputer avgSpeed;

    private final static String LOCATION_INIT = "com.atakmap.android.location.LOCATION_INIT";
    private AtakPreferences locationPrefs;

    private static final String TAG = "LocationMapComponent";

    // value in microseconds, different phones define SENSOR_DELAY_GAME
    // differently, some as high as 100hz. We should only need 15hz.
    private static final int SENSOR_RATE = 60000;

    // private static long SENSOR_RATE = SensorManager.SENSOR_DELAY_GAME;
    private BroadcastReceiver selflocrec;
    private BroadcastReceiver snapselfrec;

    private boolean _gpserrorenabled = true;
    private boolean recvInitialTimestamp = false;

    // matches the default set by the xml
    private boolean useGPSTime = true;

    private Display display;

    private double mapDataDeviceAzimuth;

    private static final int SENSOR_RATE_LIMITER = 90; // The delay that you specify is only a suggested delay. The Android system and other applications can alter this delay.
    private long lastSensorAccelCall;
    private long lastSensorMagCall;

    private boolean componentsLoaded = false;
    private BroadcastReceiver _pluginLoadedRec;

    class SpeedComputer {
        // speed for switching from compass to GPS bearing
        private final static long VALID_TIME = 5 * 60000; // 5 * 1min

        private final static double GPS_BEARING_SPEED = 0.44704 * 5; // 1.0mph * 5 

        private final double[] vlist;
        int idx = 0;
        boolean reset;
        long lastHighSpeed = SystemClock.elapsedRealtime() - VALID_TIME;
        boolean using = false;

        SpeedComputer(final int size) {
            vlist = new double[size];
        }

        private void queue(double v) {
            if (idx <= 0)
                idx = vlist.length;
            --idx;

            // instead of synchronizing on the queue method since it really is only used to
            // compute average speed, go ahead an accept the possibility that queue might have
            // more than one thread queuing a speed therefore encountering a condition where idx
            // might be -1, vlist might contain old values or any other possible conditions.
            try {
                vlist[idx] = v;
            } catch (ArrayIndexOutOfBoundsException ignore) {
            }
        }

        /**
         * Add a speed value into the queue for consideration.
         * @param v the speed in meters per second
         */
        public void add(double v) {
            reset = false;
            queue(v);

            if (!Double.isNaN(v)) {
                if (getAverageSpeed() > GPS_BEARING_SPEED) {
                    lastHighSpeed = SystemClock.elapsedRealtime();
                } else {
                    if (v >= GPS_BEARING_SPEED) { // on the move
                        //Log.d(TAG, "on the move (external)" + instantSpeed +
                        //           " averagespeed = " + avgSpeed.getSpeed());

                        // do not call reset because that will unset the driving
                        // flag and cause the cam lock to screw up.
                        Arrays.fill(vlist, Double.NaN);
                        queue(v);
                        // dismiss the driving widget
                        setDrivingWidgetVisible(false);
                    }

                }
            }
        }

        public void reset() {
            if (reset)
                return;

            Arrays.fill(vlist, Double.NaN);

            lastHighSpeed = SystemClock.elapsedRealtime() - VALID_TIME;
            _locationMarker.setMetaBoolean("driving", false);
            setDrivingWidgetVisible(false);
            using = false;

            reset = true;
        }

        boolean useGPSBearing() {

            // if the system is configured only to respond to the GPS bearing then 
            // only use GPS bearing.
            if (useOnlyGPSBearing)
                return true;

            if (SystemClock.elapsedRealtime() - lastHighSpeed < 0) {
                lastHighSpeed = SystemClock.elapsedRealtime() - VALID_TIME;
                Log.d(TAG,
                        "non-monotonic elapsedRealtime encountered, correcting");
            }

            boolean use = (SystemClock.elapsedRealtime()
                    - lastHighSpeed) < VALID_TIME;
            //Log.d(TAG, "use=" + use + " using=" + using + "driving=" + _locationMarker.getMetaBoolean("driving", false));
            if (getMapMode() != MapMode.MAGNETIC_UP) {
                if (use != using) {
                    if (!use)
                        setDrivingWidgetVisible(false);

                    _locationMarker.setMetaBoolean("driving", use);

                    using = use;
                }
                if (use && getAverageSpeed() < GPS_BEARING_SPEED) {
                    setDrivingWidgetVisible(true);
                }
            }
            return use;
        }

        double getAverageSpeed() {

            if (reset)
                return 0.0;

            double avgV = 0.0;
            int count = 0;

            for (double aVlist : vlist) {
                if (!Double.isNaN(aVlist)) {
                    avgV = aVlist + avgV;
                    count++;
                }
            }
            if (count == 0)
                return 0.0;
            else
                return avgV / count;
        }
    }

    private int currAccuracy;

    private String acc2String(int accuracy) {
        if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
            return "high accuracy";
        else if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM)
            return "medium accuracy";
        else if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW)
            return "low accuracy";
        else
            return "invalid";

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (accuracy != currAccuracy)
            Log.d(TAG, "magnetic sensor accuracy changed: "
                    + acc2String(accuracy));
        currAccuracy = accuracy;

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        final int type = event.sensor.getType();
        long sensorCall = SystemClock.elapsedRealtime();

        try {
            if (type == Sensor.TYPE_ACCELEROMETER) {
                if ((sensorCall - lastSensorAccelCall) < SENSOR_RATE_LIMITER) {
                    return;
                }
                lastSensorAccelCall = sensorCall;

                if (_gravityMatrix == null) {
                    _gravityMatrix = new float[3];
                }
                System.arraycopy(event.values, 0, _gravityMatrix, 0,
                        event.values.length);
            } else if (type == Sensor.TYPE_MAGNETIC_FIELD) {
                if ((sensorCall - lastSensorMagCall) < SENSOR_RATE_LIMITER) {
                    return;
                }

                lastSensorMagCall = sensorCall;
                if (_geoMagnetic == null) {
                    _geoMagnetic = new float[3];
                }
                System.arraycopy(event.values, 0, _geoMagnetic, 0,
                        event.values.length);
            }

            if (_gravityMatrix != null && _geoMagnetic != null) {

                SensorManager.getRotationMatrix(Rbuf, null, _gravityMatrix,
                        _geoMagnetic);
                SensorManager.getOrientation(Rbuf, values);

                // the orientation offset remains constant through the lifespan of ATAK.
                // do not recheck as the inner call to display.getRotation() consumes a fair
                // amount of CPU resources.
                // _updateOrientationOffset();
                double magAzimuth = _orientationOffset
                        + (180d * values[0] / Math.PI);

                while (magAzimuth < 0) {
                    magAzimuth += 360d;
                }
                magAzimuth %= 360d;

                double deltaAzimuth = (magAzimuth - mapDataDeviceAzimuth);
                if (Math.abs(deltaAzimuth) > 3) {
                    mapDataDeviceAzimuth = magAzimuth;
                    _mapView.getMapData().setMetaDouble("deviceAzimuth",
                            mapDataDeviceAzimuth);
                    _locationMarker.setMetaDouble("deviceAzimuth",
                            mapDataDeviceAzimuth);

                    /*
                     * we only want the orientation set via the sensor when the system is not in
                     * trackup mode, or if it is in track up mode, then only set via the sensor
                     * when the speed of the device is below the GPS threshold. This means that
                     * there are a few different states that the system can be in in which we
                     * want to update the orientation. Also, if we have been unable to retrieve
                     * a position either from mocking or from the local GPS, we want to set the
                     * orientation via sensor. Finally, we will want to make sure that the
                     * orientation is set via sensor if we get a fix and then loose it.
                     */

                    if (!avgSpeed.useGPSBearing()
                            || getMapMode() == MapMode.MAGNETIC_UP) {

                        final double trueAzimuth = convertFromMagneticToTrue(
                                _locationMarker.getPoint(),
                                magAzimuth);

                        _updateHeading(trueAzimuth);

                        setLocationTrackHeading();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "error: ", e);
        }

    }

    // cached version of the callsign produced by a call to callsignGen(Context)
    private static String cachedCallsign = null;

    /**
     * Produce a callsign based on a file that is included with the distribution ATAK that is
     * selected using the an algorithm seeded with the best determined device uid.
     */
    public static String callsignGen(Context ctx) {

        if (cachedCallsign == null) {
            // just to avoid reading the whole file first...
            final int NCALLSIGNS = 1298;

            String str = _determineBestDeviceUID(ctx);

            // Log.v(TAG,
            // "Generated Hash Code: " + str.hashCode());

            // On several lab devices I have observed the hashCode
            // spit back from the string is a negative number.
            // For all of these devices, the callsign is set to
            // the first entry.

            int index = Math.abs(str.hashCode()) % NCALLSIGNS;

            try {
                String lang = LocaleUtil.getCurrent().getLanguage();
                String callsignFile = "callsigns-" + lang + ".txt";
                if (!FileSystemUtils.assetExists(ctx,
                        "callsigns-" + lang + ".txt")) {
                    callsignFile = "callsigns.txt";
                }

                Log.d(TAG, "loading " + callsignFile);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                ctx.getAssets().open(callsignFile)));

                String line;
                try {
                    line = reader.readLine();
                    int i = 0;
                    while (line != null && i++ < index) {
                        line = reader.readLine();
                    }
                } finally {
                    IoUtils.close(reader);
                }
                cachedCallsign = line;
            } catch (Exception e) {
                cachedCallsign = "ERROR";
                Log.e(TAG, "error: ", e);
            }
        }
        return cachedCallsign;
    }

    private void setDrivingWidgetVisible(final boolean visible) {
        _mapView.post(new Runnable() {
            @Override
            public void run() {
                if (visible) {
                    NavView.getInstance().setGPSLockAction(new Runnable() {
                        @Override
                        public void run() {
                            avgSpeed.reset();
                        }
                    });
                } else {
                    NavView.getInstance().setGPSLockAction(null);
                }
            }
        });
    }

    @Override
    public void onCreate(Context context, Intent intent, MapView view) {
        _mapView = view;
        this.context = context;

        LocationManager.getInstance().registerProvider(
                new MapDataLocationProvider(_mapView.getMapData()),
                LocationManager.HIGHEST_PRIORITY);
        LocationManager.getInstance().registerProvider(
                new InternalGPSLocationProvider(_mapView),
                LocationManager.LOWEST_PRIORITY);
        LocationManager.getInstance().registerProvider(
                new NetworkLocationProvider(_mapView),
                LocationManager.LOWEST_PRIORITY);

        mapDataDeviceAzimuth = _mapView.getMapData().getMetaDouble(
                "deviceAzimuth",
                0);

        WindowManager _winManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        if (_winManager != null)
            display = _winManager.getDefaultDisplay();

        avgSpeed = new SpeedComputer(30);

        // listen for preference changes
        locationPrefs = AtakPreferences.getInstance(context);
        ensureValidGroup();

        locationPrefs.registerListener(this);

        useGPSTime = locationPrefs.get("useGPSTime", useGPSTime);
        useOnlyGPSBearing = locationPrefs.get("useOnlyGPSBearing", false);

        createLocationMarker();
        _mapView.setSelfMarker(_locationMarker);

        // After user specifies a self location when there's no GPS, update the
        // overlay
        DocumentedIntentFilter selfLocationSpecifiedFilter = new DocumentedIntentFilter();
        selfLocationSpecifiedFilter
                .addAction("com.atakmap.android.map.SELF_LOCATION_SPECIFIED");
        AtakBroadcast.getInstance().registerReceiver(
                selflocrec = new BroadcastReceiver() {

                    @Override
                    public void onReceive(Context context, Intent intent) {
                        locationUpdateWorker
                                .execute(updateRefreshLocationMarker);
                        _reportGpsBack();
                        _reportNoGps();
                    }
                }, selfLocationSpecifiedFilter);

        DocumentedIntentFilter snapToSelfLocationSpecifiedFilter = new DocumentedIntentFilter();
        snapToSelfLocationSpecifiedFilter
                .addAction("com.atakmap.android.maps.SNAP_TO_SELF");
        AtakBroadcast.getInstance().registerReceiver(
                snapselfrec = new BroadcastReceiver() {

                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Marker placedSelf = ATAKUtilities.findSelf(_mapView);
                        if (placedSelf == null || placedSelf.getPoint() == null
                                || !placedSelf.getPoint().isValid()) {
                            Log.w(TAG,
                                    "Cannot snap to self w/out self location");
                            return;
                        }

                        String uid = intent.getStringExtra("uid");
                        if (FileSystemUtils.isEmpty(uid)) {
                            Log.w(TAG, "Cannot snap to self w/out UID");
                            return;
                        }

                        MapItem item = _mapView.getRootGroup().deepFindUID(uid);
                        if (!(item instanceof PointMapItem)) {
                            Log.w(TAG,
                                    "Cannot snap to self w/out point marker");
                            return;
                        }

                        Log.d(TAG, "Snapping to self location: " + uid);
                        ((PointMapItem) item).setPoint(placedSelf.getPoint());
                        item.refresh(_mapView.getMapEventDispatcher(), null,
                                this.getClass());
                        item.persist(_mapView.getMapEventDispatcher(), null,
                                this.getClass());

                        //now zoom map to self/new location
                        Intent zoomIntent = new Intent(
                                "com.atakmap.android.maps.FOCUS");
                        zoomIntent.putExtra("uid", uid);
                        zoomIntent.putExtra("useTightZoom", true);
                        AtakBroadcast.getInstance().sendBroadcast(zoomIntent);
                    }
                }, snapToSelfLocationSpecifiedFilter);

        MetaDataHolder2 mapData = view.getMapData();

        String deviceLine1Number = _fetchTelephonyLine1Number(context);
        if (deviceLine1Number != null) {
            mapData.setMetaString("devicePhoneNumber", deviceLine1Number);
        }

        _updateContactPreferences(mapData, locationPrefs.getSharedPrefs());

        // Set the locationCallsign to be something better than ATAK on a new
        // device. At the end of the
        // day a lot of our devices are set to ATAK as the callsign which makes
        // mass loading a pain.
        _deviceCallsign = locationPrefs.get("locationCallsign", "");
        if (_deviceCallsign.isEmpty()) {
            _deviceCallsign = callsignGen(context);
            Log.d(TAG, "making new callsign:" + _deviceCallsign);
            locationPrefs.set("locationCallsign", _deviceCallsign);
        }

        mapData.setMetaString("deviceCallsign", _deviceCallsign);

        // mimic legacy behavior
        for (LocationProvider lp : LocationManager.getInstance()
                .getLocationProviders())
            lp.setEnabled(!(lp instanceof NetworkLocationProvider));

        ignoreInternalGPS(locationPrefs.get("mockingOption", "WRGPS")
                .equals("IgnoreInternalGPS"));
        ignoreExternalGPS(locationPrefs.get("mockingOption", "WRGPS")
                .equals("LocalGPS"));
        _startOrientationGathering(context);

        _updateOrientationOffset();

        resetHeadingRefreshTimer();

        DeadReckoningManager.getInstance();

        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction(
                "com.atakmap.app.COMPONENTS_CREATED",
                "Once all of the components are created, launch the intent then remove.");
        AtakBroadcast.getInstance().registerReceiver(
                _pluginLoadedRec = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context,
                                          Intent intent) {
                        Log.d(TAG, "received notification that all components are loaded");
                        componentsLoaded = true;
                    }
                }, filter);


    }

    private final Runnable headingRefresh = new Runnable() {
        @Override
        public void run() {
            if (_locationMarker != null) {
                long startTime = SystemClock.elapsedRealtime();

                long deltaHeading = SystemClock.elapsedRealtime()
                        - _lastHeadingUpdate;

                // In case we stop getting compass updates, and the estimate
                // hasn't converged with the measured yet, schedule a filter
                // update
                // This can happen if the device stops rotating; the compass
                // will only send updates every half second then, which is quite
                // jerky.
                // TODO: Ensure this timer doesn't have performance
                // implications? Tests out ok on pretty heavily loaded devices
                // though. -ts
                if (deltaHeading > HEADING_REFRESH_RATE
                        && Math.abs(_lastMeasuredHeading
                                - _filteredHeading.getEstimate()) > 2) {

                    _updateHeading(_lastMeasuredHeading);

                    setLocationTrackHeading();
                }

                long deltaRefresh = SystemClock.elapsedRealtime()
                        - _lastMarkerRefresh;
                if (deltaRefresh > REFRESH_RATE) {
                    _lastMarkerRefresh = SystemClock.elapsedRealtime();

                    locationUpdateWorker.execute(updateRefreshLocationMarker);

                    long end = (SystemClock.elapsedRealtime() - startTime);
                    if (end > 100)
                        Log.d(TAG, "warning: refresh took longer than expected (" + 
                                   end + "ms)");
                }

            }

        }
    };

    private final Runnable updateRefreshLocationMarker = new Runnable() {
        @Override
        public void run() {
            _updateLocationMarker();
        }
    };

    private void ensureValidGroup() {
        final String locationTeam = locationPrefs.get("locationTeam", null);

        if (locationTeam == null)
            locationPrefs.set("locationTeam", "Cyan");
        else {
            // make sure that the value is in the valid group.
            String validGroup = ATAKUtilities.ensureValidGroup(locationTeam);
            if (!locationTeam.equals(validGroup)) {
                locationPrefs.set("locationTeam", validGroup);
            }
        }
    }

    /**
     * Determines the usable UID for the app.   This method is used to retrieve an
     * appropriate fingerprint.   As of 4.0, this will no longer attempt to use the IMEI as a
     * a possible identifier
     * @param context is the context for the app.
     * @return null if the fingerprint could not be identified based on the device.
     */
    synchronized public static String _determineDeviceUID(
            final Context context) {
        String suffix = null;
        String possibleSuffix = _fetchSerialNumber(context);
        //Log.v(TAG, "Checking (serialNumber): " + possibleSuffix);
        if (possibleSuffix != null) {
            suffix = possibleSuffix;
        }

        possibleSuffix = _fetchWifiMacAddress(context);
        if (possibleSuffix != null) {
            // Potentially broken UID generation on a Android 6 device
            // see bug https://atakmap.com/bugz/show_bug.cgi?id=5178
            if (!possibleSuffix.endsWith("00:00:00:00:00"))
                suffix = possibleSuffix;
        }
        return suffix;
    }

    /**
     * On the first run, determines the best possible device uid and persists it for the lifetime of
     * the saved preferences.   This method is also used by the AtakCertificateDatabase.setDeviceId and 
     * AtakAuthenticationDatabase.setDeviceId.   Any changes to this value will disrupt both 
     * device / tak server history as well as the loss of keys in the system.
     * <p>
     * UID will contain 2 parts:
     *  Hard coded prefix: ANDROID-
     *  Device specific suffix: preferred in this order
     *      WiFi MAC Address
     *      Telephony Device ID
     *      Serial Number
     *      Random UUID
     */
    synchronized public static String _determineBestDeviceUID(Context context) {

        String bestDeviceUID;
        AtakPreferences prefs = AtakPreferences.getInstance(context);

        if (prefs.contains("bestDeviceUID")) {
            // the devices best possible uid has been determined during a previous run.
            // the panasonic toughpad generates a different bestDeviceUID after each
            // power cycle.
            bestDeviceUID = prefs.get("bestDeviceUID", null);
            // check to see if it is not null and not the empty string

            if ((bestDeviceUID != null) && (!bestDeviceUID.trim().isEmpty())
                    && !bestDeviceUID.endsWith("00:00:00:00:00"))
                return bestDeviceUID;
        }

        String suffix = UUID.randomUUID().toString();

        String possibleSuffix = _fetchSerialNumber(context);
        Log.v(TAG, "Checking (serialNumber): " + possibleSuffix);
        if (possibleSuffix != null) {
            suffix = possibleSuffix;
        }

        possibleSuffix = _fetchTelephonyDeviceId(context);
        Log.v(TAG, "Checking (telephonyDeviceId): " + possibleSuffix);
        if (possibleSuffix != null) {
            suffix = possibleSuffix;
        }

        possibleSuffix = _fetchWifiMacAddress(context);
        Log.v(TAG, "Checking (WifiMacAddress): " + possibleSuffix);
        if (possibleSuffix != null) {
            // Potentially broken UID generation on a Android 6 device
            // see bug https://atakmap.com/bugz/show_bug.cgi?id=5178
            if (!possibleSuffix.endsWith("00:00:00:00:00"))
                suffix = possibleSuffix;
        }

        bestDeviceUID = UID_PREFIX + suffix;

        prefs.set("bestDeviceUID", bestDeviceUID);

        return bestDeviceUID;

    }

    synchronized private void _startOrientationGathering(Context context) {
        if (_sensorMgr == null) {
            _sensorMgr = (SensorManager) context
                    .getSystemService(Context.SENSOR_SERVICE);

            if (_sensorMgr == null) {
                HintDialogHelper
                        .showHint(
                                context,
                                context.getString(R.string.tool_text34),
                                context.getString(R.string.tool_text35),
                                "device.accelerometer.issue");
                return;
            }
            accelSensor = _sensorMgr
                    .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magSensor = _sensorMgr
                    .getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            boolean accPresent = accelSensor != null
                    && _sensorMgr.registerListener(
                            this, accelSensor,
                            SENSOR_RATE);
            boolean magPresent = magSensor != null
                    && _sensorMgr.registerListener(
                            this, magSensor,
                            SENSOR_RATE);

            if (!accPresent) {
                HintDialogHelper
                        .showHint(
                                context,
                                context.getString(R.string.tool_text34),
                                context.getString(R.string.tool_text35),
                                "device.accelerometer.issue");
            }

            if (!magPresent) {
                HintDialogHelper
                        .showHint(
                                context,
                                context.getString(R.string.tool_text36),
                                context.getString(R.string.tool_text37),
                                "device.compass.issue");

            }
        }
    }

    private void _updateOrientationOffset() {
        try {
            int rotation = (display != null) ? display.getRotation()
                    : Surface.ROTATION_0;
            switch (rotation) {
                case Surface.ROTATION_0:
                    _orientationOffset = 0d;
                    break;
                case Surface.ROTATION_90:
                    _orientationOffset = 90d;
                    break;
                case Surface.ROTATION_180:
                    _orientationOffset = 180d;
                    break;
                case Surface.ROTATION_270:
                    _orientationOffset = 270d;
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG,
                    "Error has occurred getting the window and rotation, setting 0",
                    e);
            _orientationOffset = 0d;
        }

        Log.d(TAG, "orientation changed requested: " + _orientationOffset);

    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {

        if (_pluginLoadedRec != null)
            AtakBroadcast.getInstance().unregisterReceiver(_pluginLoadedRec);

        if (headingRefreshTask != null) {
            headingRefreshTask.cancel(true);
            headingRefreshTimer.shutdown();
        }

        try {
            _sensorMgr.unregisterListener(this, accelSensor);
        } catch (Exception e) {
            Log.e(TAG, "error: ", e);
            // not sure if it is registered yet, but cannot check
        }

        try {
            _sensorMgr.unregisterListener(this, magSensor);
        } catch (Exception e) {
            Log.e(TAG, "error: ", e);
            // not sure if it is registered yet, but cannot check
        }
        try {
            _sensorMgr.unregisterListener(this);
        } catch (Exception e) {
            Log.e(TAG, "error: ", e);
            // not sure if it is registered yet, but cannot check
        }

        _sensorMgr = null;

        List<LocationProvider> locationProviderList = LocationManager
                .getInstance().getLocationProviders();
        for (LocationProvider lp : locationProviderList)
            lp.setEnabled(false);

        if (selflocrec != null) {
            AtakBroadcast.getInstance().unregisterReceiver(selflocrec);
        }
        if (snapselfrec != null) {
            AtakBroadcast.getInstance().unregisterReceiver(snapselfrec);
        }

        locationPrefs.unregisterListener(this);

        // cant unregister something that has not been registered
        try {
            AtakBroadcast.getInstance().unregisterReceiver(_receiver);
        } catch (Exception e) {
            // XXX: probably should not instatiate the _receiver unless it is ready to be
            // registered.
        }

        DeadReckoningManager.getInstance().dispose();
    }

    @Override
    public void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key) {

        if (key == null)
            return;

        switch (key) {
            case "useGPSTime":
                useGPSTime = sharedPreferences.getBoolean(key,
                        useGPSTime);
                Log.d(TAG,
                        "configuration changed for using GPS time using = "
                                + useGPSTime);
                CoordinatedTime.setCoordinatedTimeMillis(0);
                resetHeadingRefreshTimer();

                break;
            case "locationUnitType":
            case "locationCallsign":
                final String nlc = locationPrefs.get("locationCallsign", "");
                if (nlc.isEmpty()) {
                    locationPrefs.set("locationCallsign", callsignGen(context));
                }
            case "locationTeam":
                if (key.equals("locationCallsign"))
                    _mapView.getMapData().setMetaBoolean(
                            "mockLocationCallsignValid", false);
                _updateContactPreferences(_mapView.getMapData(),
                        sharedPreferences);
                break;
            case "locationUseWRCallsign":
            case "tadiljId":
            case "tadiljSelfPositionType":
                _updateContactPreferences(_mapView.getMapData(),
                        sharedPreferences);
                break;
            case "custom_gps_icon_setting":
            case "location_marker_scale_key":
                //refresh gps icon and set new icon based on pref
                break;
            case "mockingOption":
                ignoreInternalGPS(locationPrefs.get("mockingOption", "WRGPS")
                        .equals("IgnoreInternalGPS"));
                ignoreExternalGPS(locationPrefs.get("mockingOption", "WRGPS")
                        .equals("LocalGPS"));
                break;
            case "useOnlyGPSBearing":
                useOnlyGPSBearing = locationPrefs.get(key, false);
                break;
        }
        if (key.equals("locationTeam")) {
            ensureValidGroup();
            if (_locationMarker != null)
                _locationMarker.setMetaString("team",
                        locationPrefs.get("locationTeam", "Cyan"));
        }
        if (key.equals("location_marker_scale_key") ||
                key.equals("custom_gps_icon_setting") ||
                key.equals("locationTeam") ||
                key.equals("custom_color_selected") ||
                key.equals("custom_outline_color_selected")) {
            refreshGpsIcon();
        }

        //else { Log.d(TAG, "unhandled preference key changed: " + key); }
    }

    private void _updateContactPreferences(MetaDataHolder2 mapData,
            SharedPreferences prefs) {
        if (!prefs.getBoolean("locationUseWRCallsign", false)
                || !mapData.getMetaBoolean("mockLocationCallsignValid",
                        false)) {
            _deviceCallsign = prefs.getString("locationCallsign",
                    callsignGen(context));
            _mapView.setDeviceCallsign(_deviceCallsign);
            if (_locationMarker != null)
                _locationMarker.setMetaString("callsign", _deviceCallsign);
        }
        _deviceTeam = prefs.getString("locationTeam", "Cyan");
        _deviceType = prefs.getString("locationUnitType",
                _mapView.getContext().getString(R.string.default_cot_type));

        _deviceCustomRole = prefs.getString("customRole", null);
        _deviceCustomRoleAbbreviation = prefs
                .getString("customRoleAbbreviation", null);

        mapData.setMetaString("deviceTeam", _deviceTeam);
        mapData.setMetaString("deviceType", _deviceType);

        mapData.setMetaString("deviceCustomRoleAbbreviation",
                _deviceCustomRole);
        mapData.setMetaString("deviceCustomRole",
                _deviceCustomRoleAbbreviation);

        if (!prefs.getString("tadiljId", "").isEmpty()) {
            mapData.setMetaString("tadiljSelfPositionType",
                    prefs.getString("tadiljSelfPositionType", "J2.0"));
            mapData.setMetaString("tadiljId", prefs.getString("tadiljId", ""));
        } else if (mapData.hasMetaValue("tadiljId"))
            mapData.removeMetaData("tadiljId");
    }

    private void gpsDrift(final LocationProvider lp) {

        Location loc = lp.getLastReportedLocation();

        if (loc == null)
            return;

        // final gpsTimestamp to use
        long gpsTimestamp = loc.getLocationDerivedTime();

        // the timestamp is not supported
        if (gpsTimestamp < 0)
            return;

        if (gpsTimestamp < GPS_ROLLOVER_DETECTION_VALUE)
            gpsTimestamp = gpsTimestamp + GPS_ROLLOVER_CORRECTION_VALUE;

        if (useGPSTime) {
            CoordinatedTime.setCoordinatedTimeMillis(gpsTimestamp);
        } else {
            CoordinatedTime.setCoordinatedTimeMillis(0);
        }

        // Perform a one time notification if the drift between GPS time and the System time
        // is greater that 5 seconds.   This will serve two functions.
        // - notify the user the system time is probably wrong and should be corrected.
        // - remind the user that the system time is being corrected to gps time.

        if (!recvInitialTimestamp) {
            recvInitialTimestamp = true;
            if (Math.abs(CoordinatedTime.getCoordinatedTimeOffset()) > 10000) {

                // Announce to the rest of the system the date should be 
                // changed
                Intent i = new Intent("com.atakmap.utc_time_set");
                i.putExtra("millisec_epoch", gpsTimestamp);
                AtakBroadcast.getInstance().sendSystemBroadcast(i);

                // also send a local broadcast for CotMarkerRefresher
                AtakBroadcast.getInstance().sendBroadcast(i);

                // disable for 3.4 
                //NetworkManagerLite.setDateTime(gpsTimestamp);

                SimpleDateFormat dformatter = new SimpleDateFormat(
                        "dd MMMMM yyyy  HH:mm:ss", LocaleUtil.getCurrent());
                NotificationUtil
                        .getInstance()
                        .postNotification(
                                R.drawable.smallclock, NotificationUtil.WHITE,
                                context.getString(R.string.notification_text22),
                                context.getString(R.string.notification_text23)
                                        + dformatter.format(gpsTimestamp),
                                context.getString(R.string.notification_text24)
                                        +
                                        (int) Math.abs(CoordinatedTime
                                                .getCoordinatedTimeOffset()
                                                / 1000.0)
                                        + context
                                                .getString(
                                                        R.string.notification_text25)
                                        +
                                        dformatter.format(gpsTimestamp)
                                        + context
                                                .getString(
                                                        R.string.notification_text26)
                                        +
                                        dformatter.format(System
                                                .currentTimeMillis()));
                final Thread t = new Thread("resetHeadingRefreshTimer") {
                    public void run() {
                        try {
                            Thread.sleep(CLOCK_CHANGE_WAIT);
                        } catch (InterruptedException ignored) {
                        }
                        resetHeadingRefreshTimer();
                    }
                };
                t.start();
            }
        }
    }

    /**
     * Update the heading for the filter based on a measurement east of true
     * north. Will protect against NaN
     *
     * @param measurement east of true north.
     */
    private void _updateHeading(final double measurement) {
        if (Double.isNaN(measurement)) {
            lastMeasurementInvalid = true;
            //Log.d(TAG, "heading is invalid, ignore: " + measurement);
            return;
        } else if (lastMeasurementInvalid) {
            lastMeasurementInvalid = false;
            //Log.d(TAG, "good heading but last one was invalid, ignore: " + measurement);
            return;
        }

        if (_lastHeadingUpdate == 0) {
            _filteredHeading.reset(measurement);
        } else {
            long delta = SystemClock.elapsedRealtime() - _lastHeadingUpdate;
            if (delta > 0) {
                double deltaSeconds = delta / MILLIS_IN_SECOND;
                _filteredHeading.update(measurement, deltaSeconds,
                        _FILTER_GRANULARITY);
            }
        }
        _lastMeasuredHeading = measurement;
        _lastHeadingUpdate = SystemClock.elapsedRealtime();
    }

    private synchronized void createLocationMarker() {
        if (_locationGroup == null) {
            _locationGroup = _mapView.getRootGroup();
        }
        if (_locationMarker == null) {
            final String _deviceUID = _determineBestDeviceUID(context);
            _locationMarker = new Marker(GeoPoint.ZERO_POINT, _deviceUID);
            _locationMarker.setType("self");
            _locationMarker.setMetaString("team",
                    locationPrefs.get("locationTeam", "Cyan"));
            _locationMarker.setMetaBoolean("addToObjList", false);
            _locationMarker.setMetaBoolean("remoteDelete", false);
            _locationMarker
                    .addOnTrackChangedListener(_trackChangedListener);

            _locationMarker.setMetaString("callsign", _deviceCallsign);
            _locationMarker.setStyle(Marker.STYLE_ROTATE_HEADING_MASK);
            _locationMarker.setMetaInteger("color", Color.BLUE);

            _locationMarker.setMetaString("menu", "menus/self_menu.xml");
            // _locationMarker.setMovable(false); // empty is false
            _locationMarker.setZOrder(Double.NEGATIVE_INFINITY);

            DocumentedIntentFilter filter = new DocumentedIntentFilter();
            filter.addAction("com.atakmap.android.map.action.TOGGLE_GPS_ERROR");
            AtakBroadcast.getInstance().registerReceiver(_receiver, filter);

            if (_accuracyEllipse == null) {
                _accuracyEllipse = new Ellipse(UUID.randomUUID().toString());
                _accuracyEllipse
                        .setCenter(_locationMarker.getGeoPointMetaData());
                _accuracyEllipse.setFillColor(Color.argb(50, 187, 238, 255));
                _accuracyEllipse.setFillStyle(2);
                _accuracyEllipse.setStrokeColor(Color.BLUE);
                _accuracyEllipse.setStrokeWeight(4);
                _accuracyEllipse.setMetaString("shapeName", "GPS Error");
                _accuracyEllipse.setMetaBoolean("addToObjList", false);
                _accuracyEllipse.setClickable(false);
                _accuracyEllipse.setMetaBoolean("remoteDelete", false);

            }
            _locationGroup.addItem(_accuracyEllipse);
            enableGPSError(true);

            _locationMarker
                    .addOnPointChangedListener(new OnPointChangedListener() {
                        GeoPoint lastPoint = null;

                        @Override
                        public void onPointChanged(PointMapItem item) {
                            final GeoPointMetaData gp = item
                                    .getGeoPointMetaData();
                            if (!Double.isNaN(gp.get().getCE())) {
                                _locationMarker.removeMetaData(
                                        "disableErrorEllipseToggle");
                                _accuracyEllipse.setVisible(
                                        item.getVisible() && _gpserrorenabled);
                                if (lastPoint == null
                                        || lastPoint.distanceTo(gp.get()) > 0.25
                                        || Double.compare(gp.get().getCE(),
                                                lastPoint.getCE()) != 0) {
                                    _accuracyEllipse.setDimensions(gp,
                                            (int) gp.get().getCE(),
                                            (int) gp.get().getCE());
                                    lastPoint = gp.get();
                                }
                            } else {
                                _locationMarker.setMetaBoolean(
                                        "disableErrorEllipseToggle", true);
                                _accuracyEllipse.setDimensions(gp, 0, 0);
                                _accuracyEllipse.setVisible(false);
                            }

                        }
                    });

            _locationMarker.addOnVisibleChangedListener(
                    new MapItem.OnVisibleChangedListener() {
                        @Override
                        public void onVisibleChanged(MapItem item) {
                            if (_accuracyEllipse != null) {
                                _accuracyEllipse.setVisible(
                                        item.getVisible() && _gpserrorenabled);
                            }
                        }
                    });
        }
    }

    /* 
     * Updates the location Marker
     * Checks for custom color set by user in preferences
     * if not color is set return default icon used for application
     * if custom colored is selected recolor image and set in /atak/gps_icons/
     * depends on icon.
     * This is a pretty expensive method and should only be called when one of the 4 governing preferences 
     * are changed.
     *
     * @return Icon containing drawable/file path used for displaying GPS user self location
     */
    private void refreshGpsIcon() {
        if (_locationMarker != null) {

            if (!_locationMarker.getMetaBoolean("adapt_marker_icon", true))
                return;

            Icon.Builder builder = new Icon.Builder();

            final int size = Integer.parseInt(
                    locationPrefs.get("location_marker_scale_key", "-1"));
            final int userSelfColor = locationPrefs
                    .get("custom_gps_icon_setting", 0);
            final String userTeam = locationPrefs.get("locationTeam", "Cyan");
            final String userCustomColor = locationPrefs
                    .get("custom_color_selected", "#FFFFFF");
            final String userCustomStrokeColor = locationPrefs
                    .get("custom_outline_color_selected", "#FFFFFF");

            final Drawable selfMarker = context
                    .getDrawable(R.drawable.ic_self_tintable);
            final Drawable selfStroke = context
                    .getDrawable(R.drawable.ic_self_stroke_tintable);
            if (selfMarker != null && selfStroke != null) {
                switch (userSelfColor) {
                    case 0: //default
                        selfMarker.setTint(0xff44b2dd);
                        selfStroke.setTint(Color.WHITE);
                        break;
                    case 1: //use team color , image is created when user selects using team color
                        selfMarker.setTint(
                                Icon2525cIconAdapter.teamToColor(userTeam));
                        selfStroke.setTint(Color.WHITE);
                        break;
                    case 2:
                        selfMarker.setTint(Color.parseColor(userCustomColor));
                        selfStroke.setTint(
                                Color.parseColor(userCustomStrokeColor));
                        break;
                    default:
                        break;
                }
            }

            final LayerDrawable composite = new LayerDrawable(new Drawable[] {
                    selfMarker, selfStroke
            });
            final Bitmap marker = ATAKUtilities.getBitmap(composite);
            final String encoded = IconUtilities.encodeBitmap(marker);

            final int width;
            final int height;

            if (size == -1) {
                width = SELF_MARKER_WIDTH;
                height = SELF_MARKER_HEIGHT;
            } else {
                float factor = size / (float) SELF_MARKER_WIDTH;
                width = size;
                height = ((int) (SELF_MARKER_HEIGHT * factor));
            }

            builder.setImageUri(0, encoded);
            builder.setSize(width, height);
            _locationMarker.setIcon(builder.build());
        }
    }

    private void _updateLocationMarker() {

        if (_locationMarker == null) {
            Log.w(TAG, "Cannot update null location marker");
            return;
        }

        if (!componentsLoaded)
            return;

        final SelfCoordOverlayUpdater updater = SelfCoordOverlayUpdater
                .getInstance();

        LocationProvider lp = LocationManager.getInstance()
                .getPreferredLocationProvider();
        Location loc = null;
        if (lp != null && (loc = lp.getLastReportedLocation()) != null) {

            gpsDrift(lp);

            if (updater != null)
                updater.change();

            GeoPoint point = loc.getPoint();

            if (point != null && !point.isValid())
                point = null;

            LocationDerivation locationDerivation = loc.getDerivation();
            String altitudeSource = locationDerivation.getVerticalSource();
            String geoLocationSource = locationDerivation.getHorizontalSource();

            if (altitudeSource == null)
                altitudeSource = GeoPointMetaData.UNKNOWN;
            if (geoLocationSource == null)
                geoLocationSource = GeoPointMetaData.UNKNOWN;

            if (_locationMarker.getGroup() == null) {
                _updateOrientationOffset();
                Intent intent = new Intent();
                intent.setAction(LOCATION_INIT);
                AtakBroadcast.getInstance().sendBroadcast(intent);
                _locationGroup.addItem(_locationMarker);
                refreshGpsIcon();
            }

            if (point != null && !point.equals(_locationMarker.getPoint())) {

                if (locationPrefs.get("useTerrainElevationSelfMarker", false)) {
                    try {
                        final GeoPointMetaData elevGpm = ElevationManager
                                .getElevationMetadata(point);
                        final GeoPoint elev = elevGpm.get();

                        point = new GeoPoint(point.getLatitude(),
                                point.getLongitude(),
                                elev.getAltitude(),
                                elev.getAltitudeReference(),
                                point.getCE(),
                                elev.getLE());

                        altitudeSource = elevGpm.getAltitudeSource();
                    } catch (IllegalArgumentException ignored) {
                        // if an exception occurs due to incorrect usage of the elevation
                        // manager - just use the existing elevation provided.
                    }
                }

                GeoPointMetaData gpm = GeoPointMetaData.wrap(point,
                        geoLocationSource, altitudeSource);
                gpm.setMetaValue(LocationManager.LOCATION_PROVIDER_REFERENCE,
                        lp.getUniqueIdentifier());
                _locationMarker.setPoint(gpm);
            }

            /*
             * this data is shoved in here so the self coord display updates correctly
             */
            final double instantSpeed = loc.getSpeed();
            _locationMarker.setMetaDouble("Speed", instantSpeed);

            if (!Double.isNaN(instantSpeed)) {
                avgSpeed.add(instantSpeed);
            } else {
                // speed is invalid.
                avgSpeed.add(0.0);
            }
            //Log.d(TAG, "added new instant speed (prefix) " + instantSpeed + " averagespeed = " + avgSpeed.getAverageSpeed());
            _locationMarker.setMetaDouble("avgSpeed30",
                    avgSpeed.getAverageSpeed());

            if (avgSpeed.useGPSBearing()
                    && getMapMode() != MapMode.MAGNETIC_UP) {
                final double h = loc.getBearing();

                // no valid speed no bearing
                if (instantSpeed > VALID_GPS_BEARING_SPEED
                        || useOnlyGPSBearing) {
                    _updateHeading(h);

                    setLocationTrackHeading();
                }
            }

            if (point != null && !Double.isNaN(point.getCE())) {
                // Only update the accuracy, otherwise the marker will
                // move after the location estimate
                if (!_locationGroup.containsItem(_accuracyEllipse)) {
                    _locationGroup.addItem(_accuracyEllipse);
                }
            } else {
                _accuracyEllipse.removeFromGroup();
            }

        }
        if (updater != null)
            updater.change();

        /*
         * Hide / show the "NO GPS" overlay, turn on/off ability to move the marker. I think this
         * should work for both mock and fine now.
         */

        if (loc == null || !loc.isValid()) {
            _reportNoGps();
            _accuracyEllipse.removeFromGroup();
        } else
            _reportGpsBack();

        _trackChangedListener.onTrackChanged(_locationMarker);

    }

    private final BroadcastReceiver _receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            enableGPSError(!_gpserrorenabled);
        }

    };

    private void enableGPSError(boolean error) {
        _accuracyEllipse.setVisible(error);
        _locationMarker.setMetaBoolean("gpserrorenabled", error);
        _gpserrorenabled = error;
    }

    // TODO : clean up track up filtering code
    private final OnTrackChangedListener _trackChangedListener = new OnTrackChangedListener() {
        private float lastHeading = 0f;
        private long lastUpdateTime = 0;

        @Override
        public void onTrackChanged(Marker marker) {
            AtakMapController ctrl = _mapView.getMapController();
            final float heading = (float) marker.getTrackHeading();

            if (Double.isNaN(heading) || heading < 0f || heading > 360f) {
                //Log.d(TAG, "heading is invalid, ignore: " + heading);
                return;
            }

            long ctime = SystemClock.elapsedRealtime();

            if (lastUpdateTime == 0)
                lastUpdateTime = ctime;

            long timeSinceLast = ctime - lastUpdateTime;

            if (timeSinceLast < HEADING_REFRESH_MIN_ELAPSE_MS) {
                // Log.d(TAG,"time between:  --SKIPPED--  "+timeSinceLast+
                // "  /  "+currentTimeBetweenUpdatesMS);
                return;
            }

            lastUpdateTime = ctime;

            // Log.d(TAG,"time between:  "+timeSinceLast+
            // "  /  "+currentTimeBetweenUpdatesMS);

            float dHeading = heading - lastHeading;

            // Account for rotating through zero degrees
            if (dHeading > 180f)
                dHeading -= 360f;
            else if (dHeading < -180f)
                dHeading += 360f;

            float smoothed = dHeading;

            lastHeading += smoothed;

            if (lastHeading > 360f)
                lastHeading -= 360f;
            if (lastHeading < 0)
                lastHeading += 360f;

            MapMode orientationMethod = getMapMode();
            if (orientationMethod == MapMode.TRACK_UP
                    || orientationMethod == MapMode.MAGNETIC_UP) {

                if (_locationMarker.getMetaBoolean("camLocked", false)) {
                    PointF p = _mapView.forward(_locationMarker.getPoint());
                    CameraController.Interactive.rotateTo(
                            _mapView.getRenderer3(), lastHeading,
                            _locationMarker.getPoint(), p.x, p.y,
                            MapRenderer3.CameraCollision.AdjustCamera,
                            true);
                } else {
                    ctrl.rotateTo(lastHeading, true);
                }
            }
        }
    };

    private void _reportNoGps() {
        boolean reportAsap = _locationMarker.getMetaString("how", "h-e")
                .equals("m-g");

        _locationMarker.setMovable(true);
        _locationMarker.setMetaString("how", "h-e");
        _locationMarker.removeMetaData("Speed");

        if (reportAsap && _mapView != null && _mapView.getContext() != null) {
            Log.d(TAG,
                    "No GPS available, sending a system broadcast for now until CotService is augmented");
            AtakBroadcast.getInstance().sendBroadcast(
                    new Intent(ReportingRate.REPORT_LOCATION)
                            .putExtra("reason", "No GPS available"));
        }
    }

    private void _reportGpsBack() {
        boolean reportAsap = _locationMarker.getMetaString("how", "m-g")
                .equals("h-e");

        // empty is false, !empty is true
        _locationMarker.removeMetaData("movable");
        _locationMarker.setMetaString("how", "m-g");

        if (reportAsap && _mapView != null && _mapView.getContext() != null) {
            Log.d(TAG,
                    "GPS available, sending a system broadcast for now until CotService is augmented");
            AtakBroadcast.getInstance().sendBroadcast(
                    new Intent(ReportingRate.REPORT_LOCATION)
                            .putExtra("reason", "GPS now available"));
        }
    }

    /*
     * If we're at (0,0), we probably don't have GPS
     */
    private boolean _checkNoGps(GeoPoint point) {
        return point == null
                || (point.getLatitude() == 0d && point.getLongitude() == 0d);
    }

    /**
     * Get the current map mode/orientation
     * @return Map mode - one of {@link MapMode}
     */
    private MapMode getMapMode() {
        return NavView.getInstance().getMapMode();
    }

    /**
     * Retrieves a TAK generated serial number based on the device.   If the serial number
     * is not available, null will be returned.   Recent versions of Android rely on the ANDROID_ID
     * as the app accessible serial number
     * @param context the context to use
     * @return the serial number or null
     */
    public static String fetchSerialNumber(final Context context) {
        return _fetchSerialNumber(context);
    }

    /**
     * Retrives a TAK accessible telephony identifier based on the device.  If the telephony id is
     * not available, then return it will return null.
     * @param context the context to use
     * @return the telephony identifier
     */
    public static String fetchTelephonyDeviceId(final Context context) {
        return _fetchTelephonyDeviceId(context);
    }

    /**
     * Retrives a TAK accessible wifi mac address based on the device.  If the mac address is
     * not available, then return it will return null.
     * @param context the context to use
     * @return the telephony identifier
     */
    public static String fetchWifiMacAddress(final Context context) {
        return _fetchWifiMacAddress(context);
    }

    @SuppressLint({
            "PrivateApi", "HardwareIds"
    })
    private static String _fetchSerialNumber(final Context context) {
        String serialNumber = null;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class, String.class);

            serialNumber = (String) get.invoke(c, "sys.serialnumber",
                    Build.UNKNOWN);
            if (serialNumber != null
                    && serialNumber.equalsIgnoreCase(Build.UNKNOWN)) {
                serialNumber = (String) get.invoke(c, "ril.serialnumber",
                        Build.UNKNOWN);
            }
            if (serialNumber != null
                    && serialNumber.equalsIgnoreCase(Build.UNKNOWN)) {
                try {
                    if (Build.SERIAL != null
                            && !Build.UNKNOWN.equalsIgnoreCase(Build.SERIAL)) {
                        serialNumber = Build.SERIAL;
                    } else {
                        // attempt to get the Android_ID which does change on a factory reset but 
                        // does not change on application uninstall/reinstall.

                        // Please note  -
                        // On Android 8.0 (API level 26) and higher versions of the platform, a
                        // 64-bit number (expressed as a hexadecimal string), unique to each
                        // combination of app-signing key, user, and device. Values of ANDROID_ID
                        // are scoped by signing key and user. The value may change if a factory
                        // reset is performed on the device or if an APK signing key changes. For
                        // more information about how the platform handles ANDROID_ID in Android
                        // 8.0 (API level 26) and higher, see Android 8.0 Behavior Changes.
                        //
                        // In versions of the platform lower than Android 8.0 (API level 26), a
                        // 64-bit number (expressed as a hexadecimal string) that is randomly
                        // generated when the user first sets up the device and should remain
                        // constant for the lifetime of the user's device. On devices that have
                        // multiple users, each user appears as a completely separate device, so
                        // the ANDROID_ID value is unique to each user.

                        serialNumber = Secure.getString(
                                context.getContentResolver(),
                                Secure.ANDROID_ID);
                    }
                } catch (Exception e) {
                    return null;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "error obtaining the serial number via reflection", e);
        }

        // possibly serialNumber is getting set to unknown, all zeros or the emulator default / just return null instead
        if (serialNumber != null) {
            if (("9774d56d682e549c".equalsIgnoreCase(serialNumber)) ||
                    (Build.UNKNOWN.equalsIgnoreCase(serialNumber)) ||
                    ("000000000000000".equalsIgnoreCase(serialNumber)) ||
                    ("00".equalsIgnoreCase(serialNumber))) {
                return null;
            }
        }

        return serialNumber;
    }

    @SuppressLint({
            "HardwareIds", "MissingPermission"
    })
    private static String _fetchWifiMacAddress(Context context) {
        String wifiMacAddress = null;
        final WifiManager wifi = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        // a possible way to uniquely identify the phone 
        if (wifi != null && wifi.getConnectionInfo() != null) {
            wifiMacAddress = wifi.getConnectionInfo().getMacAddress();
        } else {
            Log.d(TAG, "unable to  obtain the wifi device id");
        }
        return wifiMacAddress;
    }

    @SuppressLint("HardwareIds")
    @SuppressWarnings({
            "MissingPermission"
    })
    private static String _fetchTelephonyDeviceId(Context context) {
        String telephonyDeviceId = null;
        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);

        // one way of making this uid truly globally unique -- another way is Wifi MAC address
        //
        if (tm != null) {
            try {
                // on newer devices this could throw a security exception
                // "android.permission.READ_PRIVILEGED_PHONE_STATE" 
                telephonyDeviceId = tm.getDeviceId();
            } catch (Exception ignored) {
            }
        } else {
            Log.d(TAG, "unable to obtain the telephony device id");
        }

        // possibly telephonyDeviceId is getting set to unknown / just return null instead
        // Note: in the case of the herelink controllers which run Android 7, the telephony
        // id is set to 862391030003883.
        if (telephonyDeviceId != null &&
                (telephonyDeviceId.equalsIgnoreCase(Build.UNKNOWN) ||
                        (telephonyDeviceId
                                .equalsIgnoreCase("862391030003883"))))
            return null;

        return telephonyDeviceId;
    }

    /**
     * Obtains the telephone number associated with the device.
     * @param context the context used to determine the telephone number.
     * @return null if no telephone number is found, otherwise the telephone number of the device.
     */
    @SuppressLint({
            "MissingPermission", "HardwareIds"
    })
    public static String _fetchTelephonyLine1Number(Context context) {
        String telephonyLineNumber = null;
        final TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null) {

            if (Permissions.checkPermission(context,
                    Manifest.permission.READ_PHONE_STATE)) {
                try {
                    telephonyLineNumber = tm.getLine1Number();
                } catch (SecurityException ignored) {
                    Log.e(TAG, "unable to get the line number - se1");
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && isValidTelephoneNumber(telephonyLineNumber)) {
                if (Permissions.checkPermission(context,
                        Manifest.permission.READ_PHONE_NUMBERS)) {
                    try {
                        telephonyLineNumber = tm.getLine1Number();
                    } catch (SecurityException ignored) {
                        Log.e(TAG, "unable to get the line number - se2");
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && telephonyLineNumber == null) {
                SubscriptionManager sm = (SubscriptionManager) context
                        .getSystemService(
                                Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                List<SubscriptionInfo> subscriptionInfoList = sm
                        .getActiveSubscriptionInfoList();

                if (subscriptionInfoList != null) {
                    for (SubscriptionInfo si : subscriptionInfoList) {
                        try {
                            telephonyLineNumber = sm
                                    .getPhoneNumber(si.getSubscriptionId());
                            if (isValidTelephoneNumber(telephonyLineNumber))
                                break;
                        } catch (SecurityException ignored) {
                            Log.e(TAG, "unable to get the line number - se3");
                        }
                    }
                } else {
                    Log.d(TAG,
                            "unable to get the line number - no subscriptionList");
                }
            }
        } else {
            Log.d(TAG, "unable to get the line number - no tm");
        }

        return telephonyLineNumber;
    }

    /**
     * Check empty or known invalid
     *
     * @param phone the phone number to check
     * @return true if the phone number is not empty and does not contain a series of zeros.
     */
    public static boolean isValidTelephoneNumber(final String phone) {
        return !FileSystemUtils.isEmpty(phone) && !phone.contains("0000000");
    }

    @Override
    public void onLocationProviderListChange() {

    }

    @Override
    public void onStart(Context context, MapView view) {
        _updateOrientationOffset();
    }

    @Override
    public void onResume(Context context, MapView view) {
        _updateOrientationOffset();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        _updateOrientationOffset();
    }

    private void ignoreInternalGPS(boolean b) {
        List<LocationProvider> locationProviderList = LocationManager
                .getInstance().getLocationProviders();
        for (LocationProvider lp : locationProviderList)
            if (lp instanceof InternalGPSLocationProvider)
                lp.setEnabled(!b);
    }

    private void ignoreExternalGPS(boolean b) {
        List<LocationProvider> locationProviderList = LocationManager
                .getInstance().getLocationProviders();
        for (LocationProvider lp : locationProviderList) {
            if (lp instanceof ManualPlacedLocationProvider)
                continue;
            if (lp instanceof NetworkLocationProvider)
                continue;

            if (!(lp instanceof InternalGPSLocationProvider))
                lp.setEnabled(!b);
        }
    }

    /**
     * On initialization or after a clock change the timer is not longer capable of working
     * properly.   We need to reset it.
     */
    private synchronized void resetHeadingRefreshTimer() {
        if (headingRefreshTask != null)
            headingRefreshTask.cancel(false);

        headingRefreshTask = headingRefreshTimer.scheduleWithFixedDelay(
                headingRefresh, 0, HEADING_REFRESH_RATE, TimeUnit.MILLISECONDS);
    }

    /**
     * Localized optimization to perform a faster conversion from magnetic to true
     * @param point the point to use for the conversion
     * @param magDegrees the magnetic direction is degrees
     * @return the true direction in degrees
     */
    private double convertFromMagneticToTrue(@NonNull GeoPoint point,
            double magDegrees) {

        final long time = CoordinatedTime.currentTimeMillis();

        final double latitude = point.getLatitude();
        final double longitude = point.getLongitude();

        if (Math.abs(lastTime - time) > TEMPORAL_TRIGGER_MAG_DEVIATION ||
                Math.abs(lastLatitude
                        - latitude) > GEOSPATIAL_TRIGGER_MAG_DEVIATION
                ||
                Math.abs(lastLongitude
                        - longitude) > GEOSPATIAL_TRIGGER_MAG_DEVIATION) {

            lastTime = time;
            lastLongitude = longitude;
            lastLatitude = latitude;

            GeomagneticField gmf = new GeomagneticField(
                    (float) latitude,
                    (float) longitude, 0f,
                    time);

            declination = gmf.getDeclination();

        }

        double truth = magDegrees + declination;
        if (truth >= 360d) {
            return truth - 360d;
        } else if (truth < 0d) {
            return truth + 360d;
        } else {
            return truth;
        }

    }

    private void setLocationTrackHeading() {
        final double estimate = _filteredHeading.getEstimate();
        final double locationTrackHeading = _locationMarker.getTrackHeading();
        final double sensorVariance = Build.MODEL.equals("MPU5") ? 1.50d
                : 0.75d;

        if (Double.isNaN(locationTrackHeading)
                || Math.abs(estimate - locationTrackHeading) > sensorVariance) {
            _locationMarker.setTrack(estimate, 0d);
        }
    }

    private volatile long lastTime = -1;
    private volatile double lastLatitude = 720;
    private volatile double lastLongitude = 720;
    private volatile double declination = -1;

    private static final double GEOSPATIAL_TRIGGER_MAG_DEVIATION = .01; // ~ 1.1km - 0.5km depending on latitude
    private static final double TEMPORAL_TRIGGER_MAG_DEVIATION = 60 * 60000; // 1 hour
}
