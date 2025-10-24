
package com.atakmap.android.cot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import com.atakmap.android.contact.ContactStatusReceiver;
import com.atakmap.android.importexport.CotEventFactory;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.comms.CotDispatcher;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.assets.Icon;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import gov.tak.api.util.Disposable;

/**
 * Listens for self-produced CoT entry MapItems and installs listeners to 
 * maintain an send changes
 */
public class CotMarkerRefresher implements Disposable {

    public static final String TAG = "CotMarkerRefresher";

    private CotDispatcher external;
    private final Map<String, Marker> _markers = new ConcurrentHashMap<>();
    private final MapView _mapView;

    public CotMarkerRefresher(final MapView mapView) {
        _mapView = mapView;

        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction("com.atakmap.utc_time_set");
        AtakBroadcast.getInstance().registerReceiver(timeDriftDetected, filter);
    }

    /**
     * Sets the dispatcher used to send CoT external to the system.
     */
    public void setCotRemote(final CotDispatcher external) {
        this.external = external;
    }

    public void addMarker(final Marker marker) {
        if (marker.hasMetaValue("uid") && marker.hasMetaValue("type")) {
            String uid = marker.getUID();

            synchronized (_markers) {
                if (_markers.containsKey(uid)) {
                    //Log.d(TAG, "addMarker already contains: " + uid);
                    return;
                }

                _markers.put(uid, marker);
                _mapView.getMapEventDispatcher().addMapItemEventListener(
                        marker, _mapItemEventListener);
            }
        }
    }

    public Marker getMarker(final String uid) {
        return _markers.get(uid);
    }

    private static int makeTransparent(final int color,
            final float transparency) {
        return Color.argb(
                (int) (transparency * 255.0),
                Color.red(color),
                Color.green(color),
                Color.blue(color));
    }

    /**
     * Given a list of uids, set the markers to state out on the next pass
     * @param uids the list of uids
     */
    public void staleMarkers(final String[] uids) {
        for (String uid : uids) {
            Marker m = _markers.get(uid);
            if (m != null)
                m.setMetaBoolean("forceStale", true);
        }
    }

    /**
     * Force stale all markers with the specified meta string
     *
     * @param key   key of the data
     * @param value value of the data to match
     * @param  teamOnly true to only stale team members
     */
    void staleMarkers(String key, String value, boolean teamOnly) {
        //Log.d(TAG, "staleMarkers: " + key + "=" + value);
        for (Marker m : _markers.values()) {
            if (teamOnly && !m.hasMetaValue("team")) {
                continue;
            }

            String mValue = m.getMetaString(key, null);
            //Log.d(TAG, "marker: " + m.getUID() + " marker value=" + mValue);
            if (FileSystemUtils.isEquals(value, mValue)) {
                //Log.d(TAG, "Staling team member: " + m.getUID());
                m.setMetaBoolean("forceStale", true);
            }
        }
    }

    public void checkStaleTeams(final Context context,
            final SharedPreferences prefs) {
        boolean expireUnknowns = prefs.getBoolean("expireUnknowns", true);
        boolean expireEverything = prefs.getBoolean("expireEverything", true);

        int deleteStaleAfterMillis = 5 * 60 * 1000; //20 minute default
        try {
            deleteStaleAfterMillis = Integer.parseInt(prefs.getString(
                    "expireStaleItemsTime", "5")) * 60 * 1000;
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed to parse expireStaleItemsTime", e);
        }

        List<Marker> deleteList = new LinkedList<>();

        Collection<Marker> markers = _markers.values();
        final long now = new CoordinatedTime().getMilliseconds();
        for (Marker m : markers) {
            final boolean forceStale = m
                    .getMetaBoolean("forceStale", false);
            final boolean stale = m.getMetaBoolean("stale", false);
            //final long staleTime = m.getMetaLong("staleTime", -1);

            final boolean bTeamMember = m.hasMetaValue("team");

            long lastUpdate = m.getMetaLong("lastUpdateTime", 0L);
            if (m.hasMetaValue("autoStaleDuration")) {
                lastUpdate += m.getMetaLong("autoStaleDuration", 0L);
            } else {
                if (!bTeamMember) {
                    //preserving legacy behavior for non team members without autoStaleDuration
                    continue;
                } else {
                    // default to 10 sec
                    lastUpdate += 10000;
                }
            }

            if (!forceStale && stale && lastUpdate > now) {
                //Log.d(TAG, "Marker no longer stale " + m.getUID() + " now=" + now + ", stale since: " + staleTime);
                //marker was stale, but is no longer stale
                m.setMetaBoolean("stale", false);
                m.removeMetaData("staleTime");

                Icon icon = m.getIcon();
                if (icon != null) {
                    if (bTeamMember) {
                        int teamColor = m.getMetaInteger("teamColor",
                                Color.WHITE);
                        Icon newIcon = icon.buildUpon()
                                .setColor(0, teamColor)
                                .build();
                        m.setIcon(newIcon);
                    } else {
                        int iconColor = icon.getColor(1);
                        Icon newIcon = icon
                                .buildUpon()
                                .setColor(0,
                                        makeTransparent(iconColor, 1.0f))
                                .build();
                        m.setIcon(newIcon);
                    }
                }

                if (bTeamMember && context != null) {
                    Intent intent = new Intent();
                    intent.setAction(ContactStatusReceiver.ITEM_REFRESHED);
                    intent.putExtra("uid", m.getUID());
                    AtakBroadcast.getInstance().sendBroadcast(intent);
                }
            } else if (forceStale || stale || (lastUpdate <= now)) {
                //Log.d(TAG, "Marker is stale " + m.getUID() + " now=" + now + ", stale since: " + staleTime);
                //marker is stale
                if (!stale) {
                    //Log.d(TAG, "Marker initial stale " + m.getUID() + " now=" + now + ", stale since: " + staleTime);
                    //item is now stale for first time
                    m.setMetaBoolean("stale", true);
                    m.setMetaLong("staleTime", now);

                    final Icon icon = m.getIcon();
                    if (icon != null) {
                        if (bTeamMember) {
                            int teamColor = icon.getColor(0);
                            Icon newIcon = icon.buildUpon()
                                    .setColor(0, Color.GRAY).build();
                            m.setIcon(newIcon);
                            m.setMetaInteger("teamColor", teamColor);
                        } else {
                            int iconColor = icon.getColor(0);
                            Icon newIcon = icon.buildUpon()
                                    .setColor(1, iconColor)
                                    .setColor(0, Color.DKGRAY).build();
                            m.setIcon(newIcon);
                        }
                    }

                    if (forceStale) {
                        m.setMetaLong("autoStaleDuration", 0L);
                        m.removeMetaData("forceStale");
                        // See ATAK-8256
                        // No reason to clear lastUpdateTime if
                        // autoStaleDuration is set to 0
                        //m.removeMetaData("lastUpdateTime");
                    }

                    if (bTeamMember && context != null) {
                        Intent intent = new Intent();
                        intent.setAction(ContactStatusReceiver.ITEM_STALE);
                        intent.putExtra("uid", m.getUID());
                        intent.putExtra("ttl", 0);
                        AtakBroadcast.getInstance().sendBroadcast(intent);
                    }
                } //end first stale

                //see if it has been stale long enough to delete
                if (bTeamMember) {
                    if (expireEverything
                            && (now - m.getMetaLong("staleTime",
                                    0) >= deleteStaleAfterMillis)) {
                        //Log.d(TAG, "Deleting " + m.getUID()
                        //        + " stale for: " + (now - staleTime)
                        //        + " threshold=" + deleteStaleAfterMillis);
                        deleteList.add(m);
                    } else {
                        //Log.d(TAG, "Not yet time to delete " + m.getUID() + " stale for: " + (now - staleTime));
                    }
                } else {
                    final String type = m.getType();

                    if (type != null) {
                        if ((!type.startsWith("a-"))
                                ||
                                ((expireEverything ||
                                        (type.startsWith("a-u")
                                                && expireUnknowns))
                                        && (now - m.getMetaLong("staleTime",
                                                0) >= deleteStaleAfterMillis))) {
                            // If the type isn't an atom delete as soon as stale
                            // If it is an atom and has been stale long enough, then delete
                            //Log.d(TAG, "Deleting " + m.getUID()
                            //        + " stale for: " + (now - staleTime)
                            //        + " threshold="
                            //        + deleteStaleAfterMillis);
                            deleteList.add(m);
                        } else {
                            //Log.d(TAG, "Not yet time to delete " + m.getUID() + " stale for: " + (now - staleTime));
                            //Log.d(TAG, "Stale Item " + m.getTitle());
                        }
                    }
                } //end delete time check
            } //end stale
            else {
                //no change in stale status, no-op
            }
        } //end marker loop

        // remove items
        for (Marker m : deleteList)
            m.removeFromGroup();
    }

    private final MapEventDispatcher.OnMapEventListener _mapItemEventListener = new MapEventDispatcher.OnMapEventListener() {
        @Override
        public void onMapItemMapEvent(final MapItem item,
                final MapEvent event) {

            String eventType = event.getType();
            Bundle extras = event.getExtras();

            if (eventType.equals(MapEvent.ITEM_PERSIST)
                    && item instanceof Marker) {

                Marker marker = (Marker) item;

                if (extras != null && !extras.getBoolean("internal", true)) {
                    _dispatchCotFromMarker(marker, extras);
                }
            } else if (eventType.equals(MapEvent.ITEM_REFRESH)
                    && item instanceof Marker) {
                Marker marker = (Marker) item;
                PlacePointTool.updateCallsign(marker);

            } else if (eventType.equals(MapEvent.ITEM_REMOVED)
                    && item instanceof Marker) {

                _markers.remove(item.getUID());
                _mapView.getMapEventDispatcher()
                        .removeMapItemEventListener(item,
                                _mapItemEventListener);

            }
        }
    };

    private void _dispatchCotFromMarker(final Marker marker,
            final Bundle data) {
        if (data.getBoolean("dontSend", false))
            return;

        if (external != null) {
            try {
                CotEvent cot = CotEventFactory.createCotEvent(marker);
                final int staleInterval = data.getInt("staleInterval", -1);
                // if stale is specified
                // - remove any archive tags
                // - set the stale time on the event to be now + `staleInterval`
                if(staleInterval >= 0) {
                    final CotDetail details = cot.getDetail();
                    for(CotDetail archive : details.getChildrenByName("archive"))
                        details.removeChild(archive);
                    cot.setStale(cot.getTime().addSeconds(staleInterval));
                }
                external.dispatch(cot, data);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "failed to save marker", e);
            }
        }
    }

    @Override
    public void dispose() {
        AtakBroadcast.getInstance().unregisterReceiver(timeDriftDetected);
    }

    private final BroadcastReceiver timeDriftDetected = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "time drift detected based on GPS");
            for (Marker m : _markers.values()) {
                long lastUpdate = m.getMetaLong("lastUpdateTime", 0L);
                m.setMetaLong("lastUpdateTime", lastUpdate
                        - CoordinatedTime.getCoordinatedTimeOffset());
                //final long now = new CoordinatedTime().getMilliseconds();
                //final long nlastUpdate = m.getMetaLong("lastUpdateTime", 0L);
                //final boolean stale = m.getMetaBoolean("stale", false);
                //Log.d(TAG, "correcting the last seen time for: " + m.getUID() + " now: " + now + " last: " + nlastUpdate + " stale: " + stale);

            }

        }
    };

}
