
package com.atakmap.android.eud;

import android.content.Context;
import android.os.OperationCanceledException;
import android.os.SystemClock;
import android.view.View;
import android.widget.Toast;

import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.app.R;
import com.atakmap.coremap.concurrent.NamedThreadFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import gov.tak.api.engine.net.auth.OAuthAccessToken;
import gov.tak.api.engine.net.auth.OAuthTokenManager;
import gov.tak.api.util.Async;
import gov.tak.platform.client.eud.Device;
import gov.tak.platform.client.eud.EndPoints;
import gov.tak.platform.engine.net.auth.OAuthTokenEndpoint;

final class EudRegistrationWorkflow {
    final Context _context;
    final OAuthTokenManager _tokenManager;
    final EudApiClient _client;
    EudApiWorkflowUI _ui;
    Executor _mainContext;

    EudRegistrationWorkflow(Context context, OAuthTokenManager tokenManager,
            EudApiClient client) {
        _context = context;
        _tokenManager = tokenManager;
        _client = client;
    }

    void startWorkflow(EudApiWorkflowUI ui, Executor mainContext) {
        _ui = ui;
        _mainContext = mainContext;

        do {
            OAuthAccessToken token = _tokenManager
                    .getToken(EndPoints.AUTH_SERVER, EndPoints.client_id);
            if (token == null)
                break;
            // verify token
            final Executor asyncContext = Executors.newSingleThreadExecutor(
                    new NamedThreadFactory("verifyTokenThread"));

            Async<OAuthAccessToken, OAuthAccessToken> flow = new Async<>(
                    new Async.Function<OAuthAccessToken, OAuthAccessToken>() {
                        @Override
                        public OAuthAccessToken then(OAuthAccessToken arg)
                                throws Throwable {
                            if (arg != null) {
                                Toast.makeText(_context,
                                        R.string.eud_workflow_verifying_token,
                                        Toast.LENGTH_SHORT).show();
                            }
                            return arg;
                        }
                    }, _mainContext,
                    new Async.Error() {
                        @Override
                        public void error(Throwable t) {
                            // unexpected error, start the workflow
                            doRegistrationWorkflow();
                        }
                    }, _mainContext);

            flow
                    .then(
                            new Async.Function<OAuthAccessToken, OAuthAccessToken>() {
                                @Override
                                public OAuthAccessToken then(
                                        OAuthAccessToken arg) throws Throwable {
                                    // refresh the token
                                    arg.accessToken();
                                    // only pass through if valid
                                    return arg.isValid() ? arg : null;
                                }
                            }, asyncContext,
                            new Async.Error() {
                                @Override
                                public void error(Throwable t) {
                                    // there was an error validating the token, go through the flow
                                    doRegistrationWorkflow();
                                }
                            }, _mainContext)
                    .then(
                            new Async.Function<Boolean, OAuthAccessToken>() {
                                @Override
                                public Boolean then(OAuthAccessToken arg)
                                        throws Throwable {
                                    if (arg == null) {
                                        // the token is no longer valid, evict
                                        _tokenManager.removeToken(
                                                EndPoints.AUTH_SERVER,
                                                EndPoints.client_id);
                                        return Boolean.FALSE;
                                    } else {
                                        _mainContext.execute(new Runnable() {
                                            public void run() {
                                                Toast.makeText(_context,
                                                        R.string.eud_workflow_device_linked,
                                                        Toast.LENGTH_SHORT)
                                                        .show();
                                                _ui.dismiss();
                                            }
                                        });
                                        try {
                                            _client.syncResources(-1, false);
                                        } catch (Throwable t) {
                                            // XXX - can we tie this in to our async chain?
                                            _mainContext
                                                    .execute(new Runnable() {
                                                        public void run() {
                                                            Toast.makeText(
                                                                    _context,
                                                                    R.string.eud_workflow_failed_to_sync,
                                                                    Toast.LENGTH_LONG)
                                                                    .show();
                                                        }
                                                    });
                                        }
                                        return Boolean.TRUE;
                                    }
                                }
                            }, asyncContext)
                    .then(
                            new Async.Function<Void, Boolean>() {
                                @Override
                                public Void then(Boolean arg) throws Throwable {
                                    if (!arg) {
                                        // account is no longer linked, start workflow
                                        doRegistrationWorkflow();
                                    }
                                    return null;
                                }
                            }, _mainContext);

            flow.start(token);

            return;
        } while (false);

        doRegistrationWorkflow();
    }

    void doRegistrationWorkflow() {
        final Executor asyncContext = Executors.newSingleThreadExecutor(
                new NamedThreadFactory("registrationWorkflowThread"));

        Async<String, String> workflow = new Async(
                new Async.Function<String, String>() {
                    @Override
                    public String then(String url) throws Throwable {
                        _ui.setInstructionsText(_context
                                .getString(R.string.eud_workflow_connecting));
                        _ui.setRegistrationUrlText("");
                        _ui.setConnectingProgressBarVisibility(View.VISIBLE);
                        _ui.setEudActiveWorkflowViewVisible(View.GONE);
                        _ui.show();
                        return url;
                    }
                }, _mainContext,
                new Async.Error() {
                    @Override
                    public void error(Throwable ignored) {
                    }
                }, asyncContext);
        workflow
                .then(
                        new Async.Function<Device, String>() {
                            @Override
                            public Device then(String url) throws Throwable {
                                //  evict non-map source resources
                                _client.evictResources(
                                        ~EudApiClient.RESOURCES_MAP_SOURCES);
                                // sync map sources -- this will evict any non-anonymous
                                _client.syncResources(
                                        EudApiClient.RESOURCES_MAP_SOURCES,
                                        false);

                                final Device device = Device.register(url);
                                if (device == null)
                                    throw new RuntimeException();
                                return device;
                            }
                        }, asyncContext,
                        new Async.Error() {
                            @Override
                            public void error(Throwable t) {
                                Toast.makeText(_context,
                                        R.string.eud_workflow_registration_not_available,
                                        Toast.LENGTH_LONG).show();
                            }
                        }, _mainContext)
                // update UI with device code
                .then(
                        new Async.Function<Device, Device>() {
                            @Override
                            public Device then(Device device) {
                                _ui.setInstructionsText(
                                        R.string.eud_workflow_activate_instructions);
                                _ui.setRegistrationUrlText(
                                        R.string.eud_workflow_activate_url_text);

                                _ui.setConnectingProgressBarVisibility(
                                        View.GONE);
                                _ui.setEudActiveWorkflowViewVisible(
                                        View.VISIBLE);
                                _ui.setDeviceCodeText(device.user_code,
                                        device.verification_uri_complete);

                                if (device.expires_in % 60 == 0)
                                    _ui.setExpirationText(
                                            "This code will expire in "
                                                    + device.expires_in / 60
                                                    + " minutes.");
                                else
                                    _ui.setExpirationText(
                                            "This code will expire in "
                                                    + device.expires_in
                                                    + " seconds.");
                                _ui.setExpirationBarProgress(0);
                                _ui.show();
                                return device;
                            }
                        }, _mainContext)
                // poll for device authorization
                .then(
                        new Async.Function<OAuthAccessToken.Data, Device>() {
                            @Override
                            public OAuthAccessToken.Data then(Device device)
                                    throws Throwable {
                                long tokenStart = SystemClock.uptimeMillis();
                                long timeout = tokenStart
                                        + device.expires_in * 1000L;
                                long checkAt = tokenStart
                                        + device.interval * 1000L;
                                final OAuthTokenEndpoint endpoint = new OAuthTokenEndpoint(
                                        EndPoints.AUTH_SERVER,
                                        EndPoints.client_id);
                                while (true) {
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException ignored) {
                                    }
                                    if (_ui.isDismissed())
                                        throw new OperationCanceledException();
                                    long ct = SystemClock.uptimeMillis();
                                    final int progress = (int) ((double) (ct
                                            - tokenStart)
                                            / (double) (timeout - tokenStart)
                                            * 100d);
                                    _mainContext.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            _ui.setExpirationBarProgress(
                                                    progress);
                                        }
                                    });
                                    // not ready to check yet
                                    if (ct < checkAt)
                                        continue;
                                    checkAt = ct + (device.interval * 1000L);
                                    final OAuthAccessToken.Data t = endpoint
                                            .authorizeDevice(
                                                    device.device_code);
                                    if (t == null)
                                        continue;
                                    if (t.error != null) {
                                        if (SystemClock
                                                .uptimeMillis() > timeout) {
                                            // expired, obtain a new code
                                            device = Device
                                                    .register(EndPoints.DEVICE);
                                            if (device == null)
                                                throw new RuntimeException();

                                            tokenStart = SystemClock
                                                    .uptimeMillis();
                                            timeout = tokenStart
                                                    + device.expires_in * 1000L;
                                            checkAt = tokenStart
                                                    + device.interval * 1000L;

                                            final String code = device.user_code;
                                            final String verification_uri_complete = device.verification_uri_complete;

                                            _mainContext
                                                    .execute(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            _ui.setDeviceCodeText(
                                                                    code,
                                                                    verification_uri_complete);
                                                            _ui.setExpirationBarProgress(
                                                                    0);
                                                        }
                                                    });
                                        }
                                        continue;
                                    }
                                    return t;
                                }
                            }
                        }, asyncContext,
                        new Async.Error() {
                            @Override
                            public void error(Throwable t) {
                                if (!(t instanceof OperationCanceledException))
                                    Toast.makeText(_context,
                                            R.string.error_occurred,
                                            Toast.LENGTH_LONG).show();
                                _ui.dismiss();
                            }
                        }, _mainContext)
                // persist the token
                .then(
                        new Async.Function<Void, OAuthAccessToken.Data>() {
                            @Override
                            public Void then(OAuthAccessToken.Data device)
                                    throws Throwable {
                                _ui.setInstructionsText(
                                        R.string.eud_workflow_complete_instructions);
                                _ui.setRegistrationUrlText(
                                        R.string.eud_workflow_complete_url_text);
                                _ui.setEudActiveWorkflowViewVisible(View.GONE);

                                // persist token
                                _tokenManager.addToken(EndPoints.AUTH_SERVER,
                                        EndPoints.client_id, device);

                                // XXX - the preference here is not propagating!!!

                                // record that an initial link has been established
                                AtakPreferences.getInstance(_context)
                                        .set("eud_api_initial_link",
                                                true);
                                return null;
                            }
                        }, _mainContext)
                // persist the token
                .then(
                        new Async.Function<Void, Void>() {
                            @Override
                            public Void then(Void v) throws Throwable {
                                _client.syncResources(-1, false);
                                return null;
                            }
                        }, asyncContext,
                        new Async.Error() {
                            @Override
                            public void error(Throwable t) {
                                Toast.makeText(_context,
                                        R.string.eud_workflow_failed_to_sync,
                                        Toast.LENGTH_LONG).show();
                            }
                        }, _mainContext);

        // kick off the workflow
        workflow.start(EndPoints.DEVICE);
    }
}
