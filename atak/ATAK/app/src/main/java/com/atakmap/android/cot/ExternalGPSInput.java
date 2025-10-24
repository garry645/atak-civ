
package com.atakmap.android.cot;

import android.graphics.Color;
import android.os.SystemClock;

import com.atakmap.android.cot.detail.PrecisionLocationHandler;
import com.atakmap.android.gps.bluetooth.NMEAMessageHelper;
import com.atakmap.android.location.LocationMapComponent;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.comms.SocketFactory;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.util.zip.IoUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

import gnu.nmea.Packet;
import gnu.nmea.PacketGGA;
import gnu.nmea.PacketGST;
import gnu.nmea.PacketPTNL;
import gnu.nmea.PacketRMC;
import gnu.nmea.SentenceHandler;

/**
 * This thread will provide an entry point to receive and process externally supplied GPS data. The
 * format of this data is: The required input for this data is in CoT form and was based off the
 * early work to support the MPU.
 * <?xml version='1.0' standalone='yes'?> <event version="2.0"
 * uid="MPU 237" type="a-f-G-I-U-T" time="2013-12-18T17:27:35.94Z" start="2013-12-18T17:27:35.94Z"
 * stale="2013-12-18T17:28:05.94Z" how="m-g"> <point lat="27.885905" lon="-82.538630" hae="9" ce="0"
 * le=" 0"/> <detail><track course="0" speed="0.04" /></detail> </event>
 * Any further work to augment
 * this interface should be documented in this comment.
 */

public class ExternalGPSInput implements Runnable {

    public static final String TAG = "ExternalGPSInput";

    private final static int BUFFERSIZE = 1024 * 64;

    private static boolean _listening = false;
    private boolean closed = false;

    private final MapView _mapView;

    private final int _port;
    private DatagramSocket socket;

    // used for the NMEA over ethernet capability //
    private PacketRMC rmc = null;
    private PacketGGA gga = null;
    private PacketPTNL ptnl = null;
    private PacketGST gst = null;

    private static ExternalGPSInput _instance;

    ExternalGPSInput(int port, MapView mapView) {
        _port = port;
        _mapView = mapView;
        _instance = this;
    }

    public static ExternalGPSInput getInstance() {
        return _instance;
    }

    @Override
    public void run() {

        Log.w(TAG, "mocking UDP listening on port:" + _port);

        _listening = true;
        try {
            socket = SocketFactory.getSocketFactory()
                    .createDatagramSocket(_port);
            byte[] buffer = new byte[BUFFERSIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.setSoTimeout(10000);
            while (!socket.isClosed()) {
                try {
                    packet.setLength(packet.getData().length);
                    socket.receive(packet);

                    if (packet.getLength() > 0) {
                        String data = new String(buffer, 0, packet.getLength(),
                                FileSystemUtils.UTF8_CHARSET);

                        process(data);
                    } else {
                        Log.w(TAG, "Received an empty packet, ignoring.");
                    }
                } catch (SocketTimeoutException e) {
                    //Log.w(TAG, "UDP timed out.  Trying again....");
                    _mapView.getMapData()
                            .removeMetaData("mockLocationParentUID");
                    _mapView.getMapData()
                            .removeMetaData("mockLocationParentType");
                }
            }
        } catch (IOException e) {
            if (!closed)
                Log.e(TAG, "error: ", e);
        } finally {
            IoUtils.close(socket);
        }
        _listening = false;
        Log.w(TAG, "mocking UDP no longer listening on port: " + _port);
    }

    private void process(String data) {
        if (data.charAt(0) == '$') {
            // Found NMEA messages, will go ahead and process
            try {
                processNMEA(data);
            } catch (Exception ignored) {

            }
        } else {
            CotEvent event;
            try {
                event = CotEvent.parse(data);
                process(event);
            } catch (Exception e) {
                Log.e(TAG, "error: ", e);
            }
        }
    }

    private void processNMEA(final String data) {
        Packet p = null;
        String[] lines = data.split("\\r?\\n");
        for (String line : lines) {
            try {
                p = SentenceHandler.makePacket(line, false);
            } catch (Exception ignored) {
            }

            if (p == null) {
                Log.e(TAG, "unable to process NMEA string: " + line);
            }

            if (p instanceof PacketRMC) {
                rmc = (PacketRMC) p;
            } else if (p instanceof PacketGGA) {
                gga = (PacketGGA) p;
            } else if (p instanceof PacketPTNL) {
                ptnl = (PacketPTNL) p;
            } else if (p instanceof PacketGST) {
                gst = (PacketGST) p;
            }

            String msg = null;
            CotEvent event;
            if (ptnl != null) {
                if (ptnl.isActive()) {
                    msg = NMEAMessageHelper.createMessage(ptnl, gst, "NW");
                }
                ptnl = null;
            } else if ((rmc != null) && (gga != null)) {
                if (rmc.isActive()) {
                    msg = NMEAMessageHelper
                            .createMessage(rmc, gga, gst, "NW");

                }
                rmc = null;
                gga = null;
            }
            if (msg != null) {
                event = CotEvent.parse(msg);
                process(event);
            }
        }
    }

    /**
     * Process a CoT event that purports to be a GPS ownship position.
     * @param event the event
     */
    public void process(CotEvent event) {

        // currently this will only process if the ExternalGPSInput manager is in the run state
        if (!_listening)
            return;

        if (event == null ||
                ((event.getCotPoint().getLat() == 0.0 &&
                        event.getCotPoint().getLon() == 0.0) ||
                        event.getCotPoint().getLat() < -90 ||
                        event.getCotPoint().getLat() > 90 ||
                        event.getCotPoint().getLon() < -180 ||
                        event.getCotPoint().getLon() > 180)) {
            Log.w(TAG,
                    "External GPS source provided invalid position: "
                            + ((event != null) ? event.getCotPoint().toString()
                                    : "[null]"));
            _mapView.getMapData().removeMetaData(
                    "mockLocationParentUID");
            _mapView.getMapData().removeMetaData(
                    "mockLocationParentType");
        } else {
            if (event.getDetail() != null) {
                CotDetail track = event.getDetail()
                        .getFirstChildByName(0, "track");
                if (track != null) {
                    double speedD = Double.NaN;
                    String speed = track.getAttribute("speed");
                    if (speed != null)
                        speedD = Double.parseDouble(speed);
                    _mapView.getMapData().setMetaDouble(
                            "mockLocationSpeed", speedD);

                    double bearingD = Double.NaN;
                    String bearing = track
                            .getAttribute("course");
                    if (bearing != null)
                        bearingD = Double.parseDouble(bearing);
                    _mapView.getMapData()
                            .setMetaDouble("mockLocationBearing",
                                    bearingD);
                }
                CotDetail remarks = event.getDetail()
                        .getFirstChildByName(0,
                                "remarks");

                String source = "External";

                if (remarks != null) {
                    String sourceS = remarks.getInnerText();
                    if (sourceS != null)
                        source = sourceS;
                }

                CotDetail remarksColor = event.getDetail()
                        .getFirstChildByName(0,
                                "remarksColor");
                if (remarksColor != null) {
                    try {
                        String s = remarksColor.getInnerText();
                        if (s != null) {
                            // just attempt to parse the color which if not parsable will trigger
                            // an exception.  The variable c is not used.
                            int c = Color.parseColor(s);
                            _mapView.getMapData()
                                    .setMetaString("mockLocationSourceColor",
                                            s);
                        }
                    } catch (Exception e) {
                        Log.d(TAG,
                                "error occurred parsing remarksColor",
                                e);
                    }
                }

                _mapView.getMapData().setMetaString(
                        "mockLocationSource", source);

                CotDetail extendedGPSDetails = event
                        .getDetail().getFirstChildByName(0,
                                "extendedGpsDetails");
                if (extendedGPSDetails != null) {
                    try {
                        String s = extendedGPSDetails.getAttribute("time");
                        if (s != null) {
                            _mapView.getMapData()
                                    .setMetaLong("mockGPSTime",
                                            Long.parseLong(s));
                        }
                    } catch (Exception e) {
                        Log.d(TAG,
                                "error occurred parsing gpsTimeMillis",
                                e);
                    }
                    try {
                        String s = extendedGPSDetails
                                .getAttribute("fixQuality");
                        if (s != null) {
                            _mapView.getMapData()
                                    .setMetaInteger("mockFixQuality",
                                            Integer.parseInt(s));
                        }
                    } catch (Exception e) {
                        Log.d(TAG,
                                "error occurred parsing fixQuality",
                                e);
                    }
                    try {
                        String s = extendedGPSDetails
                                .getAttribute("numSatellites");
                        if (s != null) {
                            _mapView.getMapData()
                                    .setMetaInteger("mockNumSatellites",
                                            Integer.parseInt(s));
                        }
                    } catch (Exception e) {
                        Log.d(TAG,
                                "error occurred parsing numSatellites",
                                e);
                    }
                }
            }

            MapItem oldMarker = _mapView.getMapItem(event.getUID());
            if (oldMarker != null)
                oldMarker.setVisible(false);

            _mapView.getMapData()
                    .setMetaString("mockLocationParentUID",
                            event.getUID());
            _mapView.getMapData().setMetaString(
                    "mockLocationParentType",
                    event.getType());
            _mapView.getMapData().setMetaBoolean(
                    "mockLocationAvailable", true);
            _mapView.getMapData().setMetaString(
                    "locationSourcePrefix", "mock");

            GeoPointMetaData gpm = PrecisionLocationHandler
                    .getPrecisionLocation(event);
            _mapView.getMapData().setMetaString("mockLocation",
                    gpm.get().toStringRepresentation());
            _mapView.getMapData().setMetaString("mockLocationSrc",
                    gpm.getGeopointSource());
            _mapView.getMapData().setMetaString("mockLocationAltSrc",
                    gpm.getAltitudeSource());

            // XXY - need to figure out how to save other information describing the GeoPointSource //
            // 04 MARCH 2019 - for now just just augment the map bundle - see the LocationMapComponent
            // corresponding modification.

            // All location time is used for to determine when the last GPS pump occurred.
            // should be based on SystemClock which is not prone to error by setting the
            // System Date/Time.
            //
            _mapView.getMapData().setMetaLong("mockLocationTime",
                    SystemClock.elapsedRealtime());
            AtakPreferences locationPrefs = AtakPreferences
                    .getInstance(_mapView.getContext());
            if (locationPrefs.get(
                    "locationUseWRCallsign", false)) {
                String callsign = CotUtils.getCallsign(event);
                _mapView.setDeviceCallsign((callsign == null) ? event
                        .getUID()
                        : callsign);
                // mark the mock callsign as valid
                _mapView.getMapData().setMetaBoolean(
                        "mockLocationCallsignValid", true);
            } else {
                _mapView.setDeviceCallsign(locationPrefs
                        .get(
                                "locationCallsign",
                                LocationMapComponent
                                        .callsignGen(_mapView.getContext())));
                _mapView.getMapData()
                        .setMetaBoolean(
                                "mockLocationCallsignValid",
                                false);
            }

        }
    }

    void interruptSocket() {
        closed = true;
        if (socket != null)
            socket.close();

        _mapView.getMapData().removeMetaData("mockLocationParentUID");
        _mapView.getMapData().removeMetaData("mockLocationParentType");
        Log.w(TAG, "interrupted thread for listening on port: " + _port);
    }

}
