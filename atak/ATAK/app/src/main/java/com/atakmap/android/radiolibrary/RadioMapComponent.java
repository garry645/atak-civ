
package com.atakmap.android.radiolibrary;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapView;
import com.atakmap.app.R;
import com.atakmap.app.preferences.ToolsPreferenceFragment;

/**
 * Provides a generalized tab for Radio control, this can be extended 
 * for use by plugins offering additional radio controls.
 */
public class RadioMapComponent extends DropDownMapComponent {

    static public final String TAG = "RadioMapComponent";

    private RadioDropDownReceiver rddr;
    private static RadioMapComponent _instance;

    private WaveRelayControlLite wrcl;

    public static RadioMapComponent getInstance() {
        return _instance;
    }

    @Override
    public void onCreate(Context context, Intent intent, MapView view) {
        super.onCreate(context, intent, view);
        DocumentedIntentFilter radioControlFilter = new DocumentedIntentFilter();
        radioControlFilter.addAction("com.atakmap.radiocontrol.RADIO_CONTROL");

        registerDropDownReceiver(
                rddr = new RadioDropDownReceiver(view),
                radioControlFilter);

        ToolsPreferenceFragment.register(
                new ToolsPreferenceFragment.ToolPreference(
                        context.getString(R.string.isrv_control_prefs),
                        context.getString(R.string.adjust_ISRV_settings),
                        "isrvNetworkPreference",
                        context.getResources().getDrawable(
                                R.drawable.nav_radio),
                        new IsrvNetworkPreferenceFragment()));

        wrcl = WaveRelayControlLite.getInstance(view);

        _instance = this;
    }

    /**
     * Allows for external addition of control view for a radio.
     * @param uid used during the registration process
     * @param view the control as an Android View.
     *
     */
    public void registerControl(final String uid, final View view) {
        rddr.registerControl(uid, view);
    }

    /**
     * Allows for external remove of control view for a radio.
     * @param uid used during the registration process
     */
    public void unregisterControl(final String uid) {
        rddr.unregisterControl(uid);
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);
        if (wrcl != null)
            wrcl.dispose();

        _instance = null;
    }
}
