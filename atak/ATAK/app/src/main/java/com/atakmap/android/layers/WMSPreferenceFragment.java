
package com.atakmap.android.layers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.widget.Toast;

import com.atakmap.android.eud.EudApiClient;
import com.atakmap.android.eud.EudApiDropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.preference.AtakPreferenceFragment;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.preference.PreferenceSearchIndex;
import com.atakmap.app.R;
import com.atakmap.app.system.ResourceUtil;

import java.util.ArrayList;
import java.util.List;

/*
 */

public class WMSPreferenceFragment extends AtakPreferenceFragment implements
        Preference.OnPreferenceClickListener {
    Context context;
    AtakPreferences _prefs;

    public static List<PreferenceSearchIndex> index(Context context) {
        return index(context,
                WMSPreferenceFragment.class,
                R.string.wms_preferences_summary,
                R.drawable.ic_menu_settings);
    }

    public WMSPreferenceFragment() {
        super(R.xml.wms_preferences, R.string.wms_preferences_summary);

    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        new AlertDialog.Builder(context)
                .setTitle(R.string.redeploy)
                .setMessage(
                        R.string.sure_next_restart)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    DialogInterface dialog,
                                    int id) {

                                Intent intent = new Intent(
                                        EudApiDropDownReceiver.ACTION_SYNC_RESOURCES);
                                intent.putExtra(
                                        EudApiDropDownReceiver.EXTRA_RESOURCES_MASK,
                                        EudApiClient.RESOURCES_MAP_SOURCES);
                                intent.putExtra(
                                        EudApiDropDownReceiver.EXTRA_RESOURCES_FORCE,
                                        true);
                                AtakBroadcast.getInstance()
                                        .sendBroadcast(intent);
                            }
                        })
                .create().show();

        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(getResourceID());

        context = getActivity();
        _prefs = AtakPreferences.getInstance(context);

        Preference numberOfSpis = findPreference(
                "prefs_layer_grg_map_interaction");
        numberOfSpis.setTitle(
                ResourceUtil.getResource(R.string.civ_enable_grg_interact,
                        R.string.enable_grg_interact));
        numberOfSpis.setSummary(
                ResourceUtil.getResource(R.string.civ_enable_grg_interact_summ,
                        R.string.enable_grg_interact_summ));

        Preference redeploy = findPreference("redeploywms");
        redeploy
                .setOnPreferenceClickListener(this);

        Preference timeout = findPreference("wmsconnecttimeout");
        timeout.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        final ArrayList<String> opts = new ArrayList<>();
                        opts.add(String.valueOf(3));
                        opts.add(String.valueOf(5));
                        opts.add(String.valueOf(10));
                        opts.add(String.valueOf(15));
                        opts.add(String.valueOf(30));

                        final int sel = _prefs.get("wms_connect_timeout", 3000)
                                / 1000;
                        if (!opts.contains(String.valueOf(sel)))
                            opts.add(0, String.valueOf(sel));

                        new AlertDialog.Builder(context)
                                .setTitle("WMS Connect Timeout (Seconds)")
                                .setCancelable(true)
                                .setSingleChoiceItems(
                                        opts.toArray(new String[0]),
                                        opts.indexOf(String.valueOf(sel)),
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int id) {
                                                _prefs.set(
                                                        "wms_connect_timeout",
                                                        Integer.parseInt(
                                                                opts.get(
                                                                        id))
                                                                * 1000);

                                                Toast.makeText(
                                                        context,
                                                        R.string.next_restart,
                                                        Toast.LENGTH_SHORT)
                                                        .show();
                                            }
                                        })
                                .create().show();

                        return false;
                    }
                });

    }

    @Override
    public String getSubTitle() {
        return getSubTitle(getString(R.string.toolPreferences),
                getSummary());
    }

}
