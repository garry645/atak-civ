
package com.atakmap.android.eud;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;

import java.util.concurrent.Executor;

import gov.tak.api.engine.net.auth.OAuthTokenManager;

public final class EudApiDropDownReceiver extends DropDownReceiver implements
        OnStateListener {

    public static final String TAG = "RegisterEudDropDownReceiver";

    public final static String SYNCED_RESOURCES_PATH = "linked-user-resources";

    public static final String ACTION_LINK_EUD = "com.atakmap.android.eud.LINK_EUD";
    public static final String ACTION_UNLINK_EUD = "com.atakmap.android.eud.UNLINK_EUD";
    public static final String ACTION_SYNC_RESOURCES = "com.atakmap.android.eud.SYNC_RESOURCES";

    public static final String EXTRA_RESOURCES_MASK = "resourcesMask";
    public static final String EXTRA_RESOURCES_FORCE = "resourcesForce";
    public static final String EXTRA_WORKFLOW_DIALOG = "workflowDialog";

    private final OAuthTokenManager _tokenManager;
    private final EudApiClient _client;
    private final SharedPreferences _prefs;

    /**************************** CONSTRUCTOR *****************************/

    EudApiDropDownReceiver(final MapView mapView,
            EudApiClient client,
            OAuthTokenManager tokenManager,
            SharedPreferences prefs) {
        super(mapView);

        _client = client;
        _tokenManager = tokenManager;
        _prefs = prefs;

    }

    /**************************** PUBLIC METHODS *****************************/

    public void disposeImpl() {
    }

    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(final Context context, Intent intent) {

        final String action = intent.getAction();
        if (action == null)
            return;

        switch (action) {
            case ACTION_LINK_EUD:
                final EudRegistrationWorkflow workflow = new EudRegistrationWorkflow(
                        getMapView().getContext(), _tokenManager, _client);
                workflow.startWorkflow(
                        intent.getBooleanExtra(EXTRA_WORKFLOW_DIALOG, false)
                                ? new EudApiWorkflowUI.Dialog(
                                        getMapView().getContext())
                                : new EudApiWorkflowUI.Dropdown(this),
                        new Executor() {
                            @Override
                            public void execute(Runnable runnable) {
                                getMapView().post(runnable);
                            }
                        });
                break;
            case ACTION_UNLINK_EUD: {
                _client.unlink();

                // when the EUD is unlinked, re-sync the anonymous map sources
                Intent syncResources = new Intent(ACTION_SYNC_RESOURCES);
                syncResources.putExtra(EXTRA_RESOURCES_MASK,
                        EudApiClient.RESOURCES_MAP_SOURCES);
                AtakBroadcast.getInstance().sendBroadcast(syncResources);
                break;
            }
            case ACTION_SYNC_RESOURCES:
                // spawn a background thread to begin sync'ing the plugins
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        try {
                            _client.syncResources(
                                    intent.getIntExtra(EXTRA_RESOURCES_MASK,
                                            -1),
                                    intent.getBooleanExtra(
                                            EXTRA_RESOURCES_FORCE,
                                            false));
                        } catch (Throwable ignored) {
                        }
                    }
                }, "eud-sync-resources");
                t.setPriority(Thread.NORM_PRIORITY);
                t.start();
                break;
            case "com.atakmap.app.COMPONENTS_CREATED": {
                Intent syncResources = new Intent(ACTION_SYNC_RESOURCES);
                syncResources.putExtra(EXTRA_RESOURCES_MASK,
                        EudApiClient.RESOURCES_PLUGINS);
                AtakBroadcast.getInstance().sendBroadcast(syncResources);
                break;
            }
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }
}
