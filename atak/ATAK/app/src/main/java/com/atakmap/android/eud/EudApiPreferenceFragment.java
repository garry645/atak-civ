
package com.atakmap.android.eud;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

import com.atakmap.android.gui.PanPreference;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.preference.AtakPreferenceFragment;
import com.atakmap.app.R;

public final class EudApiPreferenceFragment extends AtakPreferenceFragment {

    static EudApiClient staticClient;
    public static final String TAG = "RegisterEudPreferenceFragment";

    /**
     * Only will be called after this has been instantiated with the 1-arg constructor.
     * Fragments must has a zero arg constructor.
     */
    public EudApiPreferenceFragment() {
        super(R.xml.takgov_eud_preferences, R.string.eud_preferences);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(getResourceID());

        PanPreference eudApiClientStatusPref = (PanPreference) findPreference(
                "eud_api_client_status");
        eudApiClientStatusPref
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference pref) {
                        final boolean isCurrentlyLinked = staticClient
                                .isLinked();
                        final DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                switch (which) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                        if (isCurrentlyLinked) {
                                            staticClient.unlink();
                                            refreshEudClientStatus();
                                            dialog.dismiss();

                                            // when the EUD is unlinked, re-sync the anonymous map sources
                                            Intent syncResources = new Intent(
                                                    EudApiDropDownReceiver.ACTION_SYNC_RESOURCES);
                                            syncResources.putExtra(
                                                    EudApiDropDownReceiver.EXTRA_RESOURCES_MASK,
                                                    EudApiClient.RESOURCES_MAP_SOURCES);
                                            AtakBroadcast.getInstance()
                                                    .sendBroadcast(
                                                            syncResources);
                                        } else {
                                            AtakBroadcast.getInstance()
                                                    .sendBroadcast(new Intent(
                                                            EudApiDropDownReceiver.ACTION_LINK_EUD));
                                            dialog.dismiss();
                                            getActivity().finish();
                                        }
                                        break;
                                    case DialogInterface.BUTTON_NEGATIVE:
                                        dialog.dismiss();
                                        break;
                                    default:
                                        break;
                                }
                            }
                        };
                        new AlertDialog.Builder(getActivity())
                                .setTitle(isCurrentlyLinked
                                        ? R.string.eud_prefs_unlink_dialog_title
                                        : R.string.eud_prefs_link_dialog_title)
                                .setMessage(isCurrentlyLinked
                                        ? R.string.eud_prefs_unlink_dialog_message
                                        : R.string.eud_prefs_link_dialog_message)
                                .setPositiveButton(isCurrentlyLinked
                                        ? R.string.eud_prefs_unlink_dialog_button
                                        : R.string.eud_prefs_link_dialog_button,
                                        clickListener)
                                .setNegativeButton(R.string.cancel,
                                        clickListener)
                                .setCancelable(true)
                                .create()
                                .show();

                        return true;
                    }
                });

        refreshEudClientStatus();
    }

    private void refreshEudClientStatus() {
        PanPreference eudApiClientStatusPref = (PanPreference) findPreference(
                "eud_api_client_status");
        eudApiClientStatusPref.setTitle(
                staticClient.isLinked() ? R.string.eud_prefs_title_status_linked
                        : R.string.eud_prefs_title_status_not_linked);
        eudApiClientStatusPref.setSummary(
                staticClient.isLinked() ? R.string.eud_prefs_desc_status_linked
                        : R.string.eud_prefs_desc_status_not_linked);
    }

    @Override
    public String getSubTitle() {
        return getSubTitle(getString(R.string.eud_preferences), getSummary());
    }
}
