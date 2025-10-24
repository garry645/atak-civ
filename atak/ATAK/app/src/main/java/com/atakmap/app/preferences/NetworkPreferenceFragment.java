
package com.atakmap.app.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferenceFragment;
import com.atakmap.android.preference.PreferenceSearchIndex;
import com.atakmap.android.util.ATAKConstants;
import com.atakmap.app.R;
import com.atakmap.comms.CommsProvider;
import com.atakmap.comms.CommsProviderFactory;
import com.atakmap.comms.CotServiceRemote;
import com.atakmap.comms.TAKServer;
import com.atakmap.comms.app.CotStreamListActivity;
import com.atakmap.coremap.log.Log;

public class NetworkPreferenceFragment extends AtakPreferenceFragment implements
        OnPreferenceClickListener {

    private final CotServiceRemote _remote;
    private Preference _myServers;

    public static java.util.List<PreferenceSearchIndex> index(Context context) {
        return index(context,
                NetworkPreferenceFragment.class,
                R.string.networkPreferences,
                R.drawable.ic_menu_network);
    }

    public NetworkPreferenceFragment() {
        super(R.xml.network_preferences, R.string.networkPreferences);

        // connect to the service
        _remote = new CotServiceRemote();
        //get connection state callbacks
        _remote.setOutputsChangedListener(_outputsChangedListener);
        _remote.connect(_connectionListener);
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        String key = pref.getKey();
        switch (key) {
            case "networkSettings":
                showScreen(new NetworkConnectionPreferenceFragment());
                break;
            case "tadiljSettings":
                showScreen(new TadilJPreferenceFragment());
                break;
            case "serverConnections":
                if (CotMapComponent.hasServer()) {
                    startActivity(new Intent(getActivity(),
                            CotStreamListActivity.class));
                } else {
                    showScreen(new PromptNetworkPreferenceFragment());
                }
                break;
        }

        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(getResourceID());

        Preference networkSettings = findPreference("networkSettings");
        networkSettings
                .setOnPreferenceClickListener(this);

        Preference tadiljSettings = findPreference("tadiljSettings");
        tadiljSettings.setOnPreferenceClickListener(this);

        Preference serverConnections = findPreference("serverConnections");
        serverConnections.setOnPreferenceClickListener(this);
        _myServers = serverConnections;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "destroy the preference activity");
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {

        final MapView mapView = MapView.getMapView();
        final CotMapComponent inst = CotMapComponent.getInstance();

        if (mapView == null || inst == null)
            return;

        final Context context = mapView.getContext();

        if (_myServers != null) {
            TAKServer[] servers = inst.getServers();
            if (servers == null || servers.length < 1) {
                _myServers.setIcon(R.drawable.ic_menu_network);
                _myServers.setTitle(R.string.no_servers_title);
                _myServers.setSummary(R.string.no_servers_summary);
            } else {
                if (!CommsProviderFactory.getProvider().hasFeature(
                        CommsProvider.CommsFeature.ENDPOINT_SUPPORT)) {
                    _myServers.setEnabled(false);
                } else {
                    _myServers.setEnabled(true);
                }
                _myServers.setTitle(R.string.my_servers);
                String summary = context
                        .getString(R.string.my_servers_summary)
                        + " (" + servers.length
                        + (servers.length == 1 ? " server " : " servers ")
                        + "configured)";
                _myServers.setSummary(summary);

                if (inst.isServerConnected()) {
                    _myServers.setIcon(ATAKConstants.getServerConnection(true));
                } else {
                    _myServers
                            .setIcon(ATAKConstants.getServerConnection(false));
                }
            }

        }
    }

    protected final CotServiceRemote.ConnectionListener _connectionListener = new CotServiceRemote.ConnectionListener() {

        @Override
        public void onCotServiceDisconnected() {

        }

        @Override
        public void onCotServiceConnected(Bundle fullServiceState) {
            Log.v(TAG, "onCotServiceConnected");
            refreshStatus();
        }
    };

    protected final CotServiceRemote.OutputsChangedListener _outputsChangedListener = new CotServiceRemote.OutputsChangedListener() {

        @Override
        public void onCotOutputRemoved(Bundle descBundle) {
            Log.v(TAG, "onCotOutputRemoved");
            refreshStatus();

        }

        @Override
        public void onCotOutputUpdated(Bundle descBundle) {
            Log.v(TAG, "onCotOutputUpdated");
            refreshStatus();
        }
    };

}
