
package com.atakmap.net;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.util.ATAKConstants;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.NotificationUtil;
import gov.tak.platform.lang.Parsers;
import com.atakmap.app.R;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.comms.CotService;
import com.atakmap.comms.NetConnectString;
import com.atakmap.comms.SslNetCotPort;
import com.atakmap.comms.TAKServer;
import com.atakmap.comms.app.CredentialsDialog;
import com.atakmap.comms.app.EnrollmentDialog;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

public class CertificateEnrollmentClient implements EnrollmentDialog.Callback {

    public enum CertificateEnrollmentStatus {
        SUCCESS,
        BAD_CREDENTIALS,
        QUICK_CONNECT_ERROR,
        ERROR
    }

    public interface CertificateEnrollmentCompleteCallback {
        void onCertificateEnrollmentComplete(
                CertificateEnrollmentStatus status);
    }

    private static CertificateEnrollmentClient instance = null;

    private CertificateEnrollmentClient() {
    }

    public synchronized static CertificateEnrollmentClient getInstance() {
        if (instance == null) {
            instance = new CertificateEnrollmentClient();
        }
        return instance;
    }

    private CertificateEnrollmentCompleteCallback certificateEnrollmentCompleteCallback = null;
    protected static final String TAG = "CertificateEnrollmentClient";
    private ProgressDialog progressDialog;
    private Context context;
    private boolean getProfile;

    private MapView view;
    private Context appCtx;

    public void enroll(final Context context, final String desc,
            final String connectString, final String cacheCreds,
            final Long expiration,
            CertificateEnrollmentCompleteCallback certificateEnrollmentCompleteCallback,
            final boolean getProfile) {
        enroll(context, desc, connectString, cacheCreds, expiration,
                certificateEnrollmentCompleteCallback, getProfile, false);
    }

    public void enroll(final Context context, final String desc,
            final String connectString, final String cacheCreds,
            final Long expiration,
            CertificateEnrollmentCompleteCallback certificateEnrollmentCompleteCallback,
            final boolean getProfile, final boolean isQuickConnect) {
        this.context = context;
        this.getProfile = getProfile;
        this.certificateEnrollmentCompleteCallback = certificateEnrollmentCompleteCallback;

        this.view = MapView.getMapView();
        if (view == null) {
            // Possibly caused by ATAK crashing while the preference screen is open
            Log.d(TAG, "mapview is null, cannot enroll");
            return;
        }

        this.appCtx = view.getContext();

        view.post(new Runnable() {
            @Override
            public void run() {
                progressDialog = new ProgressDialog(context);
                progressDialog.setTitle(
                        appCtx.getString(R.string.enroll_client_title));
                progressDialog.setIcon(
                        com.atakmap.android.util.ATAKConstants.getIconId());
                progressDialog.setMessage(appCtx.getString(
                        R.string.enroll_client_message));
            }
        });

        if (connectString != null) {
            NetConnectString ncs = NetConnectString.fromString(connectString);

            if (ncs == null) {
                Log.e(TAG, "could not enroll for a bad connectString: "
                        + connectString, new Exception());
                return;
            }

            // clear out any current client cert for this server
            AtakCertificateDatabase.deleteCertificateForServerAndPort(
                    AtakCertificateDatabaseIFace.TYPE_CLIENT_CERTIFICATE,
                    ncs.getHost(), ncs.getPort());

            AtakAuthenticationCredentials credentials = AtakAuthenticationDatabase
                    .getCredentials(
                            AtakAuthenticationCredentials.TYPE_COT_SERVICE,
                            ncs.getHost());

            final String username;
            final String password;

            if (credentials != null) {
                username = credentials.username;
                password = credentials.password;
            } else {
                username = null;
                password = null;
            }

            if (FileSystemUtils.isEmpty(username)
                    || FileSystemUtils.isEmpty(password)) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        CredentialsDialog.createCredentialDialog(
                                desc, connectString,
                                FileSystemUtils.isEmpty(username) ? ""
                                        : username,
                                FileSystemUtils.isEmpty(password) ? ""
                                        : password,
                                cacheCreds, expiration,
                                context,
                                new CredentialsCallback(isQuickConnect));
                    }
                });
            } else {
                CertificateConfigRequest request = new CertificateConfigRequest(
                        connectString, cacheCreds, desc, username, password,
                        expiration);
                request.setQuickConnect(isQuickConnect);
                verifyTrust(request);
            }
        } else {
            EnrollmentDialog.createEnrollmentDialog(null, null, null,
                    context, CertificateEnrollmentClient.this);
        }
    }


    private class CredentialsCallback implements CredentialsDialog.Callback {
        private boolean isQuickConnect;

        private CredentialsCallback(boolean isQuickConnect) {
            this.isQuickConnect = isQuickConnect;
        }

        public void onCredentialsEntered(String connectString,
                String cacheCreds,
                String description,
                String username, String password, Long expiration) {

            CommsMapComponent cmc = CommsMapComponent.getInstance();
            if (cmc != null) {
                CotService cs = cmc.getCotService();
                if (cs != null) {
                    cs.setCredentialsForStream(connectString, username,
                            password);

                    CertificateConfigRequest request = new CertificateConfigRequest(
                            connectString, cacheCreds, description, username,
                            password,
                            expiration);

                    request.setQuickConnect(isQuickConnect);
                    verifyTrust(request);
                }
            }
        }

        @Override
        public void onCredentialsCancelled(String connectString) {
            Log.d(TAG, "cancelled out of CredentialsDialog");
        }
    }

    private void verifyTrust(final CertificateConfigRequest request) {
        try {
            execute(request);

        } catch (Exception e) {
            Log.e(TAG, "Exception in post!", e);
        }
    }

    private class EnrollCompletionListener
            implements CommsMapComponent.EnrollmentListener {
        private final CertificateConfigRequest request;
        private final String clientPass;
        private final String caPass;
        private final boolean getProfile;

        private byte[] privKeyStore;

        public EnrollCompletionListener(CertificateConfigRequest request,
                String clientPass,
                String caPass,
                boolean getProfile) {
            this.request = request;
            this.clientPass = clientPass;
            this.caPass = caPass;
            this.getProfile = getProfile;
        }

        @Override
        public void onEnrollmentErrored(final ErrorState error) {
            view.post(new Runnable() {
                public void run() {
                    String message = null;
                    CertificateEnrollmentClient.CertificateEnrollmentStatus status = CertificateEnrollmentStatus.ERROR;
                    CertificateConfigRequest requestForAlert = request;

                    switch (error) {
                        case SERVER_NOT_TRUSTED: {
                            message = "The TAK Server's identity could not be verified";
                            showProgress(false);

                            if (request.getQuickConnect()) {
                                CommsMapComponent cmc = CommsMapComponent
                                        .getInstance();
                                if (cmc != null) {
                                    CotService cs = cmc.getCotService();
                                    if (cs != null)
                                        cs.removeStreaming(request
                                                .getConnectString(), false);
                                }
                                showAlertDialog(message,
                                        CertificateEnrollmentStatus.QUICK_CONNECT_ERROR,
                                        request);
                            } else {
                                final AlertDialog dialog = new AlertDialog.Builder(
                                        context)
                                                .setIcon(ATAKConstants
                                                        .getIconId())
                                                .setTitle(
                                                        R.string.server_auth_error)
                                                .setMessage(message)
                                                .setPositiveButton(R.string.ok,
                                                        null)
                                                .create();
                                try {
                                    dialog.show();
                                } catch (Exception ignored) {
                                }
                            }
                            // This case is self-handled; we are done
                            return;
                        }
                        case CONNECTION_FAILURE: {
                            Log.e(TAG,
                                    "CertificateEnrollmentRequest Failed - Connection Error");

                            message = appCtx
                                    .getString(R.string.enroll_client_failure);
                            if (request.getQuickConnect()) {
                                status = CertificateEnrollmentStatus.QUICK_CONNECT_ERROR;
                                CommsMapComponent cmc = CommsMapComponent
                                        .getInstance();
                                if (cmc != null) {
                                    CotService cs = cmc.getCotService();
                                    if (cs != null)
                                        cs.removeStreaming(
                                                request.getConnectString(),
                                                false);
                                }
                            }

                            requestForAlert = request;
                            break;
                        }
                        case AUTH_ERROR: {
                            Log.e(TAG,
                                    "CertificateEnrollmentRequest Failed - Auth Error");
                            message = appCtx
                                    .getString(R.string.invalid_credentials);
                            status = CertificateEnrollmentStatus.BAD_CREDENTIALS;
                            requestForAlert = request;
                            break;
                        }
                        case OTHER: {
                            Log.e(TAG,
                                    "CertificateEnrollmentRequest Failed - Other Error");
                            message = appCtx
                                    .getString(R.string.enroll_client_failure);

                            if (request.getQuickConnect()) {
                                status = CertificateEnrollmentStatus.QUICK_CONNECT_ERROR;
                                CommsMapComponent cmc = CommsMapComponent
                                        .getInstance();
                                if (cmc != null) {
                                    CotService cs = cmc.getCotService();
                                    if (cs != null)
                                        cs.removeStreaming(
                                                request.getConnectString(),
                                                false);
                                }
                            }
                            // requestForAlert intentionally remains null
                            break;
                        }
                    }

                    NotificationUtil.getInstance().postNotification(
                            NotificationUtil.GeneralIcon.NETWORK_ERROR.getID(),
                            NotificationUtil.RED,
                            appCtx.getString(R.string.connection_error),
                            appCtx.getString(R.string.enroll_client_failure),
                            message);

                    // reconnect streams
                    CommsMapComponent cmc = CommsMapComponent.getInstance();
                    if (cmc != null) {
                        CotService cs = cmc.getCotService();
                        if (cs != null)
                            cs.reconnectStreams();
                    }
                    showProgress(false);

                    showAlertDialog(message, status, requestForAlert);
                }
            });
        }

        @Override
        public void onEnrollmentCompleted(final byte[] clientCertStore,
                final byte[] enrollmentTrustStore) {
            String fatalError = null;
            if (FileSystemUtils.isEmpty(privKeyStore)) {
                Log.e(TAG, "generateKey failed!");
                fatalError = "Certificate config request failed";
            }

            if (request.getQuickConnect() && enrollmentTrustStore == null) {
                Log.e(TAG,
                        "no enrollment trust store and none given in enrollment setup!");
                fatalError = "Server did not return trust configuration";
            }

            if (fatalError != null) {
                showProgress(false);
                final String fFatalError = fatalError;
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        NotificationUtil.getInstance().postNotification(
                                NotificationUtil.GeneralIcon.NETWORK_ERROR
                                        .getID(),
                                NotificationUtil.RED,
                                context.getString(R.string.connection_error),
                                fFatalError,
                                fFatalError);

                        showAlertDialog(
                                appCtx.getString(
                                        R.string.enroll_client_success),
                                CertificateEnrollmentStatus.SUCCESS,
                                null);
                    }
                });
                return;
            }

            // Save private key
            AtakCertificateDatabase.saveCertificate(
                    AtakCertificateDatabaseAdapter.TYPE_PRIVATE_KEY,
                    privKeyStore);

            // store the client certificate password
            AtakAuthenticationDatabase.saveCredentials(
                    AtakAuthenticationCredentials.TYPE_clientPassword,
                    request.getServer(),
                    "",
                    clientPass,
                    false);

            // store the client certificate
            NetConnectString ncs = NetConnectString
                    .fromString(request.getConnectString());
            AtakCertificateDatabase.saveCertificateForServerAndPort(
                    AtakCertificateDatabaseIFace.TYPE_CLIENT_CERTIFICATE,
                    request.getServer(),
                    ncs.getPort(),
                    clientCertStore);

            // CA SAVING
            if (request.getQuickConnect()) {
                // store the CA certificate password
                AtakAuthenticationDatabase.saveCredentials(
                        AtakAuthenticationCredentials.TYPE_caPassword,
                        request.getServer(),
                        "",
                        caPass,
                        false);

                // store the CA certificate
                AtakCertificateDatabase.saveCertificateForServerAndPort(
                        AtakCertificateDatabaseIFace.TYPE_TRUST_STORE_CA,
                        request.getServer(),
                        ncs.getPort(),
                        enrollmentTrustStore);
            }

            // clear out any cached socket factories for this server
            CertificateManager.invalidate(request.getServer());

            if (getProfile) {
                CertificateEnrollmentClient.getInstance()
                        .executeDeviceProfileRequest(request);
            } else {
                //
                // reconnect streams only if we're not about to retrieve an enrollment profile.
                // the reconnect will trigger a query for a connection profile, so we'll delay
                // reconnect until after enrollment profile is processed
                //

                CommsMapComponent cmc = CommsMapComponent.getInstance();
                if (cmc != null) {
                    CotService cs = cmc.getCotService();
                    if (cs != null)
                        cs.reconnectStreams();
                }

                // if we're not retrieving a profile from TAK server, go ahead and
                // close the progress dialog and finish the enrollment
                showProgress(false);
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        showAlertDialog(
                                appCtx.getString(
                                        R.string.enroll_client_success),
                                CertificateEnrollmentStatus.SUCCESS,
                                null);
                    }
                });
            }
        }

        @Override
        public void onEnrollmentKeygenerated(byte[] key) {
            privKeyStore = key;
        }
    }

    private void execute(CertificateConfigRequest request) {
        if (request == null || !request.isValid()) {
            Log.w(TAG, "Invalid CertificateConfigRequest!");
            return;
        }

        // notify user
        Log.d(TAG,
                "CertificateConfigRequest created for: " + request);

        showProgress(true);

        String keystorePassword;
        AtakAuthenticationCredentials keystoreCreds = AtakAuthenticationDatabase
                .getCredentials(
                        AtakAuthenticationCredentials.TYPE_clientPassword,
                        request.getServer());

        if (keystoreCreds == null
                || FileSystemUtils.isEmpty(keystoreCreds.password)) {
            keystorePassword = ATAKUtilities.getRandomString(64);
        } else {
            keystorePassword = keystoreCreds.password;
        }

        AtakAuthenticationCredentials truststoreCreds = AtakAuthenticationDatabase
                .getCredentials(
                        AtakAuthenticationCredentials.TYPE_caPassword,
                        request.getServer());

        String truststorePassword;
        if (truststoreCreds == null
                || FileSystemUtils.isEmpty(truststoreCreds.password)) {
            truststorePassword = ATAKUtilities.getRandomString(64);
        } else {
            truststorePassword = truststoreCreds.password;
        }

        CommsMapComponent.EnrollmentListener listener = new EnrollCompletionListener(
                request, keystorePassword, truststorePassword, getProfile);
        try {
            // Prior implementation of this used TakHttpClient which ignores all system certs, so
            // to retain this behavior, only pull trust anchors from local trust manager
            X509Certificate[] trustedIssuers = null;

            if (!request.getQuickConnect()) {
                byte[] truststore = AtakCertificateDatabase
                        .getCertificateForServer(
                                AtakCertificateDatabaseIFace.TYPE_TRUST_STORE_CA,
                                request.getServer());
                if (truststore != null) {
                    AtakAuthenticationCredentials caCertCredentials = AtakAuthenticationDatabase
                            .getCredentials(
                                    AtakAuthenticationCredentials.TYPE_caPassword,
                                    request.getServer());
                    trustedIssuers = gov.tak.platform.engine.net.CertificateManager
                            .loadCertificate(
                                    truststore, caCertCredentials.password)
                            .toArray(new X509Certificate[0]);
                }
            } else {
                // No truststore for enrollment; use local trust manager with public CAs included.
                // This request will use hostname verification.
                trustedIssuers = CertificateManager.getInstance()
                        .getLocalTrustManager(false).getAcceptedIssuers();
            }
            // create a new trust store to hold the trust chain
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(null, null);
            int count = 0;
            if (trustedIssuers != null) {
                for (X509Certificate cert : trustedIssuers) {
                    String alias = "ca" + count;
                    trustStore.setCertificateEntry(alias, cert);
                    count++;
                }
            }
            Log.d(TAG, count + " trust anchors found for enrollment");
            String caCertsPass = ATAKUtilities.getRandomString(64);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            trustStore.store(baos, caCertsPass.toCharArray());
            byte[] caCerts = baos.toByteArray();
            CommsMapComponent.getInstance().enroll(listener,
                    request.getServer(),
                    SslNetCotPort.getServerApiPort(
                            SslNetCotPort.Type.CERT_ENROLLMENT),
                    request.getQuickConnect(),
                    request.getUsername(),
                    request.getPassword(),
                    false,
                    caCerts,
                    caCertsPass,
                    keystorePassword,
                    truststorePassword);
        } catch (Exception ex) {
            Log.e(TAG, "Enrollment failed to start", ex);
            listener.onEnrollmentErrored(
                    CommsMapComponent.EnrollmentListener.ErrorState.OTHER);
        }

    }

    private void executeDeviceProfileRequest(
            final CertificateConfigRequest request) {
        if (request == null || !request.isValid()) {
            Log.w(TAG, "Invalid CertificateConfigRequest!");
            showProgress(false);
            return;
        }

        executeDeviceProfileRequestImpl(request.getServer(),
                request.getConnectString(),
                request.getUsername(),
                request.getPassword(), !request.getQuickConnect());
    }

    private void executeDeviceProfileRequestImpl(
            final String server, final String serverConnectString,
            final String username, final String password,
            final boolean allowAllHostnames) {
        Log.d(TAG, "retrieving enrollment profile");
        if (!DeviceProfileClient.getInstance().getProfile(
                context,
                server, serverConnectString,
                username,
                password,
                allowAllHostnames,
                true, false, -1, new DeviceProfileCallback(context) {

                    @Override
                    public void onDeviceProfileRequestComplete(boolean status,
                            Bundle resultData) {
                        Log.d(TAG,
                                "onDeviceProfileRequestComplete finished successfully: "
                                        + server);

                        showProgress(false);
                        if (status) {
                            showAlertDialog(
                                    appCtx.getString(
                                            R.string.enroll_client_success),
                                    CertificateEnrollmentStatus.SUCCESS, null);
                        } else {
                            showAlertDialog(
                                    appCtx.getString(
                                            R.string.device_profile_failure),
                                    CertificateEnrollmentStatus.ERROR, null);
                        }
                    }
                })) {

            //if enrollment profile is disabled, then proceed
            Log.d(TAG, "getProfile not sent: " + server);
            view.post(new Runnable() {
                @Override
                public void run() {
                    CommsMapComponent cmc = CommsMapComponent.getInstance();
                    if (cmc != null) {
                        CotService cs = cmc.getCotService();
                        if (cs != null)
                            cs.reconnectStreams();
                    }
                    showProgress(false);
                    showAlertDialog(
                            appCtx.getString(R.string.enroll_client_success),
                            CertificateEnrollmentStatus.SUCCESS, null);
                }
            });
        } else {
            showProgress(false);
        }
    }

    private void showProgress(final boolean show) {
        view.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (progressDialog == null) {
                        Log.w(TAG, "progress not set");
                        return;
                    }

                    if (show) {
                        progressDialog.show();
                    } else {
                        progressDialog.dismiss();
                    }
                } catch (Exception ex) {
                    Log.e(TAG, ex.getMessage());
                }
            }
        });
    }

    private void showAlertDialog(final String message,
            final CertificateEnrollmentStatus status,
            final CertificateConfigRequest certificateConfigRequest) {
        String title = status == CertificateEnrollmentStatus.SUCCESS
                ? appCtx.getString(R.string.enroll_client_success_title)
                : appCtx.getString(R.string.enroll_client_failure_title);

        boolean isQuickConnectError = certificateConfigRequest != null
                && certificateConfigRequest.getQuickConnect()
                && status != CertificateEnrollmentStatus.SUCCESS;

        String positiveButtonText = isQuickConnectError
                ? appCtx.getString(R.string.retry)
                : appCtx.getString(R.string.ok);

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context)
                .setIcon(com.atakmap.android.util.ATAKConstants.getIconId())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                if (certificateEnrollmentCompleteCallback != null) {
                                    certificateEnrollmentCompleteCallback
                                            .onCertificateEnrollmentComplete(
                                                    status);
                                } else if (isQuickConnectError) {

                                    NetConnectString ncs = NetConnectString
                                            .fromString(
                                                    certificateConfigRequest
                                                            .getConnectString());

                                    EnrollmentDialog.createEnrollmentDialog(
                                            ncs.getHost() + ":" + ncs.getPort(),
                                            certificateConfigRequest
                                                    .getUsername(),
                                            certificateConfigRequest
                                                    .getPassword(),
                                            context,
                                            CertificateEnrollmentClient.this);
                                } else if (status == CertificateEnrollmentStatus.BAD_CREDENTIALS
                                        &&
                                        certificateConfigRequest != null) {
                                    view.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            CredentialsDialog
                                                    .createCredentialDialog(
                                                            certificateConfigRequest
                                                                    .getDescription(),
                                                            certificateConfigRequest
                                                                    .getConnectString(),
                                                            certificateConfigRequest
                                                                    .getUsername(),
                                                            certificateConfigRequest
                                                                    .getPassword(),
                                                            certificateConfigRequest
                                                                    .getCacheCreds(),
                                                            certificateConfigRequest
                                                                    .getExpiration(),
                                                            context,
                                                            new CredentialsCallback(
                                                                    certificateConfigRequest
                                                                            .getQuickConnect()));
                                        }
                                    });
                                }
                            }
                        });

        if (isQuickConnectError) {
            alertDialog.setNegativeButton(R.string.cancel, null);
        }

        try {
            alertDialog.show();
        } catch (Exception e) {
            // if enrollment does not complete on time and the preference activity has been closed, 
            // just continue on with the application and do not error out.
            Log.e(TAG,
                    "error occurred and the preference activity has been closed prior to the enrollment completing",
                    e);
        }
    }

    public void onEnrollmentOk(Context context, String address,
            String cacheCreds,
            String description,
            String username, String password, Long expiration) {
        Log.d(TAG, "in onEnrollmentOk");

        if (FileSystemUtils.isEmpty(address)) {
            Log.e(TAG, "cannot enroll with an empty address");
            return;
        }

        String host;
        int port = 8089;

        if (address.contains("://")) {
            address = address.substring(address.indexOf("://") + 3);
        }

        String[] split = address.split(":");
        host = split[0];

        if (FileSystemUtils.isEmpty(host)) {
            Log.e(TAG, "cannot enroll with an empty hostname");
            return;
        }

        if (split.length > 1) {
            port = Parsers.parseInt(split[1], port);
        }

        String protocol = "ssl";
        if (split.length > 2) {
            if ("quic".equalsIgnoreCase(split[2]))
                protocol = split[2];
        }

        // save the credentials
        AtakAuthenticationDatabase.saveCredentials(
                AtakAuthenticationCredentials.TYPE_COT_SERVICE,
                host, username, password, expiration);

        // build up a connectString using the default port
        String connectString = host + ":" + port + ":" + protocol;

        // create the TAKServer
        Bundle bundle = new Bundle();
        bundle.putString(TAKServer.CONNECT_STRING_KEY, connectString);
        bundle.putString(TAKServer.DESCRIPTION_KEY, description);
        bundle.putBoolean(TAKServer.ENROLL_FOR_CERT_KEY, true);
        // quick connect, which all users of this method assume, does not use pre-configured trust
        bundle.putBoolean(TAKServer.ENROLL_USE_TRUST_KEY, false);
        TAKServer takServer = new TAKServer(bundle);

        // add the streaming connection
        CommsMapComponent cmc = CommsMapComponent.getInstance();
        if (cmc != null) {
            CotService cs = cmc.getCotService();
            if (cs != null) {
                cs.addStreaming(takServer);
                // launch the enrollment process
                // all users of this method assume quick connect semantics
                enroll(context, description, connectString,
                        cacheCreds, expiration, null, true, true);

            }
        }
    }

    public void onEnrollmentCancel() {
        Log.d(TAG, "in onEnrollmentCancel");
    }
}
