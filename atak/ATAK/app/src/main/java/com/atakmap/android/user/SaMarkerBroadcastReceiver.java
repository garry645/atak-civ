package com.atakmap.android.user;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.cot.detail.CotDetailManager;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import gov.tak.api.util.Disposable;


/**
 * This class is meant to handle incoming intents to modify the SA marker on the map via a CoT
 * message.
 */
public class SaMarkerBroadcastReceiver extends BroadcastReceiver implements Disposable {
    /**
     * Tag for logging
     */
    private static final String TAG = "SaMarkerBroadcastReceiver";

    // Intent actions
    public static final String SA_ADD_DETAIL = "com.atakmap.android.maps.SA_ADD_DETAIL";

    private final String dummyEvent = "<?xml version='1.0' encoding='UTF-8'?>" +
            "<event version='2.0' uid='dummyEvent' type='u-d-p' how='h-e' time='2014-10-29T02:40:00.677Z' " +
            "start='2014-10-29T02:40:00.677Z' stale='2014-10-30T02:40:00.677Z'>" +
            "<point ce='0' le='0' hae='0' lat='0' lon='0'/><detail>%s</detail></event>";

    private final ConcurrentHashMap<String, Long> validityMapping = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CotDetail> removeCotMapping = new ConcurrentHashMap<>();
    private final ReaperThread reaperThread = new ReaperThread();
    private final MapView mapView;

    public SaMarkerBroadcastReceiver(final MapView mapView) {
        reaperThread.start();
        this.mapView = mapView;
    }

    /**
     * Processes the incoming intent and adds it to the SA Marker's details.  The incoming intent
     * is defined as having the following extras:
     *
     * required:  "cotDetail"        defines the detail tag that should be added to the self marker SA
     *                               message
     * optional:  "removeCotDetail"  defines a detail to be used when the original CoTDetail is no
     *                               longer considered valid.   The largest use case for this would
     *                               be in the case of ICU, where a video URL is being provided.
     *                               At some point ICU crashes and is unable to send the cleanup
     *                               information to ATAK.   ATAK will remove the video detail when
     *                               the validity timeout is reached and automatically have the
     *                               removal detail required to make it so that other clients no
     *                               longer see a video url that no longer is valid.
     *
     * optional:  "returnIntent"     defines a return intent that contains the final status of
     *                               of adding the detail to the self marker SA message.
     *                               "status"     '0' : details were successfully processed
     *                               "status"     '1' : there were no details recognized to process
     *                               "status"     '2' : something went wrong trying to process the details
     *
     *                               "statusMsg" : the actual message associated with the error
     *                                            condition.
     * required:  "validityMillis"   defines the time as a long where the detail is considered valid
     *                               and once the time is expired, the detail is removed.  If a
     *                               "removeCotDetail" is defined, then that is broadcast and then
     *                               the detail is removed.
     *
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null) return;
        switch (action) {
            case SA_ADD_DETAIL:
                // The detail or details to add to the outgoing self SA message.
                String cotDetailString = intent.getStringExtra("cotDetail");

                String removeCotDetailString = intent.getStringExtra("removeCotDetail");

                // the return response to the sender indicating that the message was processed
                // either successfully or unsuccessfully. If ATAK is not running, the client will
                // not get a return message and should act accordingly. The extra would be a
                // "status" of either 0 or 1 where 0 is success and 1 or higher is a failure code
                String returnIntent = intent.getStringExtra("returnIntent");

                // the number of milliseconds from the current time that this detail or set of
                // details will be valid.
                long validityMillis = intent.getLongExtra("validityMillis", -1);

                if (cotDetailString != null) {
                    final String cotEventStr = String.format(LocaleUtil.US, dummyEvent, cotDetailString);
                    final CotEvent cotEvent = CotEvent.parse(cotEventStr);
                    final CotDetail cotDetail = cotEvent.getDetail();

                    // If there are no attributes or children for the detail, nothing to process
                    if (cotDetail.getChildren().isEmpty()) {
                        Intent i = new Intent(returnIntent);
                        i.putExtra("status", 1);
                        i.putExtra("statusMsg", "CoT detail contained no children");
                        AtakBroadcast.getInstance().sendSystemBroadcast(i);
                        return;
                    }

                    boolean success = false;
                    if (validityMillis > 0) {
                        for (CotDetail child : cotDetail.getChildren()) {
                            CotMapComponent.getInstance().addAdditionalDetail(
                                    child.getElementName(), child);
                            validityMapping.put(child.getElementName(),
                                    CoordinatedTime.currentTimeMillis() + validityMillis);
                        }
                        // specific cases for getting a metadata attribute to be added to the
                        // local SA marker without changing the current public interfaces.
                        // According to the implementation this should only process the details
                        // and not the main part of the message.
                        CotDetailManager.getInstance().processDetails(mapView.getSelfMarker(),
                                cotEvent);
                        success = true;
                    }

                    if (removeCotDetailString != null) {
                        final String removeCotEventStr = String.format(LocaleUtil.US, dummyEvent, cotDetailString);
                        final CotEvent removeCotEvent = CotEvent.parse(removeCotEventStr);
                        final CotDetail removeCotDetail = removeCotEvent.getDetail();
                        for (CotDetail child : removeCotDetail.getChildren()) {
                            removeCotMapping.put(child.getElementName(), child);
                        }
                        CotDetailManager.getInstance().processDetails(mapView.getSelfMarker(),
                                removeCotEvent);
                    }

                    if (success) {
                        Intent i = new Intent(returnIntent);
                        i.putExtra("status", 0);
                        AtakBroadcast.getInstance().sendSystemBroadcast(i);
                    } else {
                        // else something went wrong with processing the details
                        Intent i = new Intent(returnIntent);
                        i.putExtra("status", 2);
                        i.putExtra("statusMsg", "Some or all of the details could not be processed");
                        AtakBroadcast.getInstance().sendBroadcast(i);
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void dispose() {
        reaperThread.cancel();
    }

    private class ReaperThread extends Thread {
        private volatile boolean cancelled = false;

        void cancel() {
            cancelled = true;
            interrupt();
        }
        public void run() {
            while (!cancelled) {
                long currentTime = CoordinatedTime.currentTimeMillis();
                Iterator<String> iter = validityMapping.keySet().iterator();
                while (iter.hasNext()) {
                    String detailName = iter.next();
                    Long expireTime = validityMapping.get(detailName);
                    // If the detail has expired its validity period, remove it
                    if (expireTime == null || expireTime < currentTime) {
                        CotMapComponent.getInstance().removeAdditionalDetail(detailName);
                        validityMapping.remove(detailName);

                        CotDetail removeCotDetail = removeCotMapping.remove(detailName);
                        // in case a detail that effectively is used to remove metadata is supplied.
                        if (removeCotDetail != null) {
                            CotMapComponent.getInstance().addAdditionalDetail(removeCotDetail.getElementName(),
                                    removeCotDetail);
                        }
                    }
                }
                try { Thread.sleep(3000); } catch (InterruptedException ignored) { }
            }
        }
    }
}