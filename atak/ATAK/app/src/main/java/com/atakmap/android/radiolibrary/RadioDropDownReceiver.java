
package com.atakmap.android.radiolibrary;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.gui.DragLinearLayout;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.video.ConnectionEntry;
import com.atakmap.android.video.StreamManagementUtils;
import com.atakmap.app.R;
import com.atakmap.comms.NetworkManagerLite;
import com.atakmap.comms.NetworkManagerLite.NetworkDevice;
import com.atakmap.coremap.log.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RadioDropDownReceiver extends DropDownReceiver {

    public static final String TAG = "RadioDropDownReceiver";

    private final HarrisSA hsv;
    private final LayoutInflater inflater;
    private final DragLinearLayout listView;
    private final View _layout;
    private final Context context;
    private final Rover rover;
    private View softKdu = null;

    private static final String RADIO_CONTROL_PRIORITY_PREFERENCE_PREFIX = "radiocontrol_entry.";

    private final AtakPreferences atakPreferences;

    // provides a mapping between the view and what is displayed on the screen
    // which is a view with a line.
    private final Map<String, View> viewMap = new HashMap<>();
    private final Map<View, String> uidMap = new HashMap<>();

    public RadioDropDownReceiver(MapView mapView) {
        super(mapView);
        context = mapView.getContext();
        inflater = LayoutInflater.from(getMapView().getContext());
        _layout = inflater.inflate(R.layout.radio_main, null);
        listView = _layout.findViewById(R.id.radioList);
        atakPreferences = AtakPreferences.getInstance(context);

        hsv = new HarrisSA(getMapView());
        registerControl("harris-sa", hsv.getView());

        if (exists("com.harris.rfcd.android.kdu")) {
            softKdu = inflater.inflate(R.layout.radio_item_harris_skdu, null);
            View b = softKdu.findViewById(R.id.harris_skdu);
            b.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean success;
                    try {
                        success = launchThirdParty(
                                "com.harris.rfcd.android.kdu",
                                "com.harris.rfcd.android.kdu.KDUActivity");
                    } catch (Exception e) {
                        success = false;
                    }
                    if (!success)
                        Toast.makeText(
                                context,
                                R.string.radio_could_not_start_configuration,
                                Toast.LENGTH_SHORT).show();

                }
            });
            registerControl("soft-kdu", softKdu);

        }

        rover = new Rover(getMapView());
        registerControl("rover-control", rover.getView());

        listView.setOnViewSwapListener(
                new DragLinearLayout.OnViewSwapListener() {
                    @Override
                    public void onSwap(View firstView, int firstPosition,
                            View secondView, int secondPosition) {

                        String firstUid = uidMap.get(((LinearLayout) firstView
                                .findViewById(R.id.radio_item)).getChildAt(0));
                        String secondUid = uidMap.get(((LinearLayout) secondView
                                .findViewById(R.id.radio_item)).getChildAt(0));
                        if (firstUid != null && secondUid != null &&
                                !firstUid.startsWith("ignore.")
                                && !secondUid.startsWith("ignore.")) {
                            atakPreferences.set(
                                    RADIO_CONTROL_PRIORITY_PREFERENCE_PREFIX
                                            + firstUid,
                                    secondPosition);
                            atakPreferences.set(
                                    RADIO_CONTROL_PRIORITY_PREFERENCE_PREFIX
                                            + secondUid,
                                    firstPosition);
                        }
                    }
                });

    }

    @Override
    public void onReceive(final Context context, Intent intent) {

        setRetain(true);

        if (isPortrait())
            showDropDown(_layout, FIVE_TWELFTHS_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    FIVE_TWELFTHS_HEIGHT);
        else
            showDropDown(_layout, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT);
    }

    /**
     * Allows for external addition of control view for a radio.
     * @param uid the unique identifier for a view
     * @param view the control as an Android View.
     */
    synchronized void registerControl(final String uid, final View view) {
        if (view == null)
            return;

        // if the view is already being displayed, unregister it before continuing
        if (this.viewMap.containsKey(uid))
            unregisterControl(uid);

        final LinearLayout item = (LinearLayout) inflater
                .inflate(R.layout.radio_item_with_line, null);
        ((LinearLayout) item.findViewById(R.id.radio_item)).addView(view);

        getMapView().post(new Runnable() {
            @Override
            public void run() {
                int idx = listView.getChildCount() - 1;

                final int priority = atakPreferences
                        .get(RADIO_CONTROL_PRIORITY_PREFERENCE_PREFIX + uid,
                                idx + 1);

                for (int i = 0; i < listView.getChildCount(); ++i) {
                    final View v = ((LinearLayout) listView.getChildAt(i)
                            .findViewById(R.id.radio_item)).getChildAt(0);

                    int curr_priority = atakPreferences
                            .get(RADIO_CONTROL_PRIORITY_PREFERENCE_PREFIX
                                    + uidMap.get(v), 0);
                    if (priority <= curr_priority) {
                        idx = i;
                        break;
                    } else {
                        idx = i + 1;
                    }
                }

                //Log.d(TAG, "loading: " + uid + " priority" + priority + " computedid " + idx);
                View handle = item.findViewById(R.id.radio_handle);
                listView.addDragView(item, handle, idx);
                viewMap.put(uid, item);
                uidMap.put(view, uid);
            }
        });
    }

    /**
     * Allows for external remove of control view for a radio.
     * @param uid the registered control as an Android View.
     */
    synchronized void unregisterControl(final String uid) {
        if (uid == null)
            return;

        final View line = viewMap.remove(uid);
        uidMap.remove(line);
        if (line instanceof LinearLayout) {
            final View v = ((LinearLayout) line.findViewById(R.id.radio_item))
                    .getChildAt(0);

            getMapView().post(new Runnable() {
                @Override
                public void run() {
                    listView.removeView(line);
                    ((LinearLayout) line).removeView(v);
                }
            });
        }
    }

    @Override
    public void disposeImpl() {
        unregisterControl("harris-sa");
        hsv.dispose();

        unregisterControl("rover-control");
        rover.dispose();

        unregisterControl("soft-kdu");
    }

    private boolean launchThirdParty(final String pkg, final String act) {

        if (!exists(pkg))
            return false;
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(pkg, act));
        //intent.setAction("");
        //intent.putExtra("uri", uri);
        context.startActivity(intent);
        return true;

    }

    private boolean exists(final String pkg) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(pkg, PackageManager.GET_META_DATA);
            Log.d(TAG, "found " + pkg + " on the device");
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "could not find " + pkg + " on the device");
            return false;
        }
    }

    /**
     * Broadcasts the video intent to watch a video.
     * @param external true will launch the external video player.
     */
    private void watchPDDLVideo(boolean external) {

        final String url = "udp://:49410";

        Log.d(TAG, "connecting to: " + url);
        ConnectionEntry ce = StreamManagementUtils
                .createConnectionEntryFromUrl("pddl", url);

        if (ce == null)
            return;

        NetworkDevice nd = getPDDLDevice();
        if (nd != null)
            ce.setPreferredInterfaceAddress(
                    NetworkManagerLite.getAddress(nd.getInterface()));

        Toast.makeText(context, R.string.radio_initiating_connection,
                Toast.LENGTH_SHORT).show();

        Intent i = new Intent("com.atakmap.maps.video.DISPLAY");
        i.putExtra("CONNECTION_ENTRY", ce);
        if (external) {
            i.putExtra("standalone", true);
        }
        i.putExtra("cancelClose", true);
        AtakBroadcast.getInstance().sendBroadcast(i);

    }

    /**
     * Search the network map and see if there is a specific interface
     * with the type set to POCKET_DDL
     */
    private NetworkDevice getPDDLDevice() {
        List<NetworkDevice> devices = NetworkManagerLite.getNetworkDevices();
        for (NetworkDevice nd : devices) {
            if (nd.isSupported(NetworkDevice.Type.POCKET_DDL)) {
                Log.d(TAG, "found PocketDDL entry in network.map file: " + nd);
                if (nd.getInterface() != null) {
                    Log.d(TAG, "interface is up, returning: " + nd);
                    return nd;
                }
            }
        }

        return null;

    }

}
