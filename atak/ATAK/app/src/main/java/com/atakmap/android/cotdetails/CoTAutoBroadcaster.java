
package com.atakmap.android.cotdetails;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.importexport.CotEventFactory;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import gov.tak.api.util.Disposable;

import com.atakmap.android.cotdelete.CotDeleteEventMarshal;

/**
 *
 * This class holds a list of markers to auto broadcast to any users on the same network.
 * It uses a timer similar to the SPI tool.
 */
public class CoTAutoBroadcaster implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        MapEventDispatcher.MapEventDispatchListener, Disposable,
        PointMapItem.OnPointChangedListener {

    public static final String TAG = "CoTAutoBroadcaster";
    private static final String FILENAME = "autobroadcastmarkers.dat";
    private static final String PREFERENCE_BROADCAST_DELETE = "preference_broadcast_delete";

    private Timer _timer;
    private int _updateTimeout;
    private boolean _staleEnabled;
    private int _staleInterval;
    private final AtakPreferences _prefs;

    private final ConcurrentLinkedQueue<String> _markers = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<AutoBroadcastListener> listeners = new ConcurrentLinkedQueue<>();

    private final MapView _mapView;
    private static CoTAutoBroadcaster _instance;

    /**
     * Listen to changes to the markers currently being auto broadcasted.
     */
    public interface AutoBroadcastListener {
        /**
         * A marker has been added to the autobroadcast queue. 
         */
        void markerAdded(PointMapItem pmi);

        /**
         * A marker has been removed from the autobroadcast queue. 
         */
        void markerRemoved(PointMapItem pmi);
    }

    private CoTAutoBroadcaster(final MapView view) {
        _mapView = view;
        _prefs = AtakPreferences.getInstance(_mapView
                .getContext());
        _prefs.registerListener(this);
        _updateTimeout = Integer
                .parseInt(_prefs.get("hostileUpdateDelay", "10")); // default to 60 seconds
        _staleEnabled = _prefs.get("autosendStaleEnabled", false);
        _staleInterval = Integer
                .parseInt(_prefs.get("autosendStaleInterval", "30")); // default to 30 seconds
        loadMarkers();
        addMapListener();
        _instance = this;
        startTimer();

    }

    /**
     * Add a listener for changes to which markers are auto broadcasted.
     * @param abl the listener for the autobroadcast event.
     */
    public void addAutoBroadcastListener(AutoBroadcastListener abl) {
        synchronized (listeners) {
            listeners.add(abl);
        }
    }

    /**
     * Remove a listener for changes to which markers are auto broadcasted.
     * @param abl the listener for the auto broadcast event.
     */
    public void removeAutoBroadcastListener(AutoBroadcastListener abl) {
        synchronized (listeners) {
            listeners.remove(abl);
        }
    }

    synchronized public static CoTAutoBroadcaster getInstance() {
        if (_instance == null)
            _instance = new CoTAutoBroadcaster(MapView.getMapView());
        return _instance;
    }

    /**
     * Registers the appropriate map listeners used during one of the selection
     * states.   Care should be taken to call _removeMapListener() in order to
     * restore the previous state of the Map Interface.
     */

    private void addMapListener() {
        MapEventDispatcher dispatcher = _mapView.getMapEventDispatcher();
        dispatcher.addMapEventListener(MapEvent.ITEM_REMOVED, this);
    }

    /**
     * This  method will load the marker ID's
     * from a list stored in Databases
     */
    private void loadMarkers() {
        //load markers from list
        final File inputFile = FileSystemUtils.getItem("Databases/" + FILENAME);
        if (IOProviderFactory.exists(inputFile)) {
            try (InputStream is = IOProviderFactory.getInputStream(inputFile)) {
                byte[] temp = new byte[is.available()];
                int read = is.read(temp);
                String menuString = new String(temp, 0, read,
                        FileSystemUtils.UTF8_CHARSET);
                String[] lines = menuString.split("\n");
                for (String line : lines) {
                    // mark as autobroadcast
                    synchronized (_markers) {
                        _markers.add(line);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "error occurred reading the list of hostiles", e);
            }
        } else
            Log.d(TAG, "File not found: " + FILENAME);

    }

    /**
     * Save the marker ID's to a
     * file in Databases
     */
    private void saveMarkers() {
        final File outputFile = FileSystemUtils
                .getItem("Databases/" + FILENAME);

        if (IOProviderFactory.exists(outputFile))
            FileSystemUtils.delete(outputFile);

        StringBuilder builder = new StringBuilder();
        synchronized (_markers) {
            if (_markers.isEmpty()) {
                return;
            }
            for (String m : _markers) {
                if (m != null) {
                    builder.append(m);
                    builder.append("\n");
                }
            }
        }

        try (OutputStream os = IOProviderFactory.getOutputStream(outputFile)) {
            try (InputStream is = new ByteArrayInputStream(builder.toString()
                    .getBytes())) {
                FileSystemUtils.copy(is, os);
            }
        } catch (IOException e) {
            Log.e(TAG, "error occurred", e);
        }
    }

    /**
     * When the 'Broadcast' button
     * is toggled on, add the marker ID to
     * the marker list
     */
    public void addMarker(final Marker m) {

        if (_markers.contains(m.getUID())) {
            return;
        }

        _markers.add(m.getUID());
        m.addOnPointChangedListener(this);

        send(m.getUID(), _staleInterval + _updateTimeout);
        saveMarkers();

        for (AutoBroadcastListener abl : listeners) {
            try {
                abl.markerAdded(m);
            } catch (Exception e) {
                Log.e(TAG, "error", e);
            }
        }
    }

    /**
     * When the 'Broadcast' button is toggled off, remove the marker
     * from the marker list.
     * @param m the marker to stop autobroadcasting
     */
    public void removeMarker(final Marker m) {
        if (_markers.contains(m.getUID())) {
            m.removeOnPointChangedListener(this);
            _markers.remove(m.getUID());
            send(m, 0);
            saveMarkers();
        }

        for (AutoBroadcastListener abl : listeners) {
            try {
                abl.markerRemoved(m);
            } catch (Exception e) {
                Log.e(TAG, "error", e);
            }
        }
    }

    /**
     * Check to see if a marker has broadcast 'on'
     * @param m the marker
     * @return true if marker is in the broadcast list
     */
    public boolean isBroadcast(final Marker m) {
        return _markers.contains(m.getUID());
    }

    /**
     * Returns a list of all of the markers currently set to autobroadcast.
     * @return returns a copy of all of the markers currently used by the auto broadcaster
     */
    public List<String> getMarkers() {
        return new ArrayList<>(_markers);
    }

    /**
     * Broadcast the markers in the broadcast list
     */
    private void broadcastCoTs() {
        //send the CoT markers
        for (String m : _markers) {
            send(m, _staleInterval + _updateTimeout);
        }
    }

    private void send(final String m, int staleInterval) {
        MapItem marker = _mapView.getMapItem(m);
        if (marker instanceof Marker)
            send((Marker)marker, staleInterval);
    }
    private void send(final Marker marker, int staleInterval) {
        Bundle persistExtras = new Bundle();
        persistExtras.putBoolean("internal", false);
        persistExtras.putBoolean("autobroadcaster", true);
        if(_staleEnabled)
            persistExtras.putInt("staleInterval", staleInterval);
        else if(staleInterval == 0)
            return; // only send immediate staleout when stale is enabled

        marker.persist(_mapView.getMapEventDispatcher(),
                persistExtras,
                this.getClass());
    }

    synchronized private void startTimer() {
        stopTimer();

        // disabled
        if (_updateTimeout == 0)
            return;

        Log.d(TAG,
                "restarting the basic auto cot broadcaster: " + _updateTimeout);
        _timer = new Timer("autobroadcast-timer");
        _timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    broadcastCoTs();
                } catch (Exception e) {
                    Log.e(TAG, "error: ", e);
                }
            }
        }, 0, _updateTimeout * 1000L);
    }

    synchronized private void stopTimer() {
        if (_timer != null) {
            Log.d(TAG, "stopping the basic auto cot broadcaster");
            _timer.cancel();
            _timer.purge();
            _timer = null;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {

        if (key == null)
            return;

        switch(key) {
            case "hostileUpdateDelay":
                // default to 60 seconds
                _updateTimeout = Integer.parseInt(_prefs.get(key, "60"));
                stopTimer();
                startTimer();
                break;
            case "autosendStaleInterval":
                _staleInterval = Integer.parseInt(_prefs.get(key, "30"));
                break;
            case "autosendStaleEnabled":
                _staleEnabled = _prefs.get("autosendStaleEnabled", false);
                break;
        }
    }

    @Override
    public void onMapEvent(MapEvent event) {
        if (event.getType().equals(MapEvent.ITEM_REMOVED)) {
            MapItem item = event.getItem();
            if (item instanceof Marker) {
                Marker m = (Marker) item;
                synchronized (_markers) {
                    if (_markers.contains(m.getUID())) {
                        removeMarker(m);

                        // return if we're not going to broadcast the deletion
                        if (!_prefs.get(PREFERENCE_BROADCAST_DELETE, false)) {
                            return;
                        }

                        // create a copy of the event
                        CotEvent delete = CotEventFactory.createCotEvent(m);
                        delete.setType(CotDeleteEventMarshal.COT_TASK_DISPLAY_DELETE_TYPE);
                        CotDetail detail = delete.getDetail();
                        if (detail == null) {
                            detail = new CotDetail("detail");
                            delete.setDetail(detail);
                        }

                        // ensure link is set to self
                        CotDetail link = detail.getChild("link");
                        if (link == null) {
                            link = new CotDetail("link");
                            detail.addChild(link);
                        }
                        link.setAttribute("relation", "p-p");
                        link.setAttribute("type", m.getType());
                        link.setAttribute("uid", m.getUID());

                        // add the force delete detail
                        CotDetail forcedelete = new CotDetail("__forcedelete");
                        detail.addChild(forcedelete);

                        // broadcast the event now
                        CotMapComponent.getExternalDispatcher()
                                .dispatchToBroadcast(delete);
                    }
                }
            }
        }
    }

    @Override
    public void onPointChanged(PointMapItem m) {
        send(m.getUID(), _staleInterval + _updateTimeout);
    }

    @Override
    public void dispose() {
        stopTimer();
    }
}
