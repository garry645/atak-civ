
package com.atakmap.app.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Base64;
import android.widget.EditText;
import android.widget.Toast;

import com.atakmap.android.gui.ImportFileBrowserDialog;
import com.atakmap.android.gui.PanEditTextPreference;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferenceFragment;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.preference.PreferenceSearchIndex;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.app.R;
import com.atakmap.comms.NetConnectString;
import com.atakmap.comms.app.CotInputsListActivity;
import com.atakmap.comms.app.CotOutputsListActivity;
import com.atakmap.comms.app.CotStreamListActivity;
import com.atakmap.comms.CommsProvider;
import com.atakmap.comms.CommsProviderFactory;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.net.AtakCertificateDatabase;
import com.atakmap.net.AtakCertificateDatabaseIFace;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;

public class NetworkConnectionPreferenceFragment
        extends AtakPreferenceFragment {

    public static final String TAG = "NetworkConnectionPreferenceFragment";

    public static final String CERTIFICATE_UPDATED = "com.atakmap.app.preferences.CERTIFICATE_UPDATED";

    public static java.util.List<PreferenceSearchIndex> index(Context context) {
        return index(context,
                NetworkConnectionPreferenceFragment.class,
                R.string.networkConnectionPreferences,
                R.drawable.ic_menu_network_connections);
    }

    public NetworkConnectionPreferenceFragment() {
        super(R.xml.network_connections_preferences,
                R.string.networkConnectionPreferences);
    }

    @Override
    public String getSubTitle() {
        return getSubTitle(getString(R.string.networkPreferences),
                getSummary());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(getResourceID());
        Preference manageInputs = findPreference(
                "manageInputsLink");
        manageInputs
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        startActivity(new Intent(getActivity(),
                                CotInputsListActivity.class));
                        return false;
                    }
                });

        if (!CommsProviderFactory.getProvider()
                .hasFeature(CommsProvider.CommsFeature.ENDPOINT_SUPPORT)
                && !CommsProviderFactory.getProvider().hasFeature(
                        CommsProvider.CommsFeature.MESH_MODE_INPUT_SUPPORTED))
            manageInputs.setEnabled(false);

        Preference manageOutputs = findPreference(
                "manageOutputsLink");
        manageOutputs
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        startActivity(new Intent(getActivity(),
                                CotOutputsListActivity.class));
                        return false;
                    }
                });

        if (!CommsProviderFactory.getProvider()
                .hasFeature(CommsProvider.CommsFeature.ENDPOINT_SUPPORT)
                && !CommsProviderFactory.getProvider().hasFeature(
                        CommsProvider.CommsFeature.MESH_MODE_OUTPUT_SUPPORTED))
            manageOutputs.setEnabled(false);

        Preference manageStreams = findPreference(
                "manageStreamingLink");
        manageStreams
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        startActivity(new Intent(getActivity(),
                                CotStreamListActivity.class));
                        return false;
                    }
                });
        if (!CommsProviderFactory.getProvider()
                .hasFeature(CommsProvider.CommsFeature.ENDPOINT_SUPPORT)) {
            manageStreams.setEnabled(false);
        }

        final Preference caLocation = findPreference("caLocation");
        caLocation
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        getCertFile(
                                getActivity(),
                                getString(R.string.preferences_text412),
                                AtakCertificateDatabaseIFace.TYPE_TRUST_STORE_CA,
                                false, null);
                        return false;
                    }
                });

        final Preference certLocation = findPreference(
                "certificateLocation");
        certLocation
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        getCertFile(
                                getActivity(),
                                getString(R.string.preferences_text413),
                                AtakCertificateDatabaseIFace.TYPE_CLIENT_CERTIFICATE,
                                false, null);
                        return false;
                    }
                });

        final Preference enableNonStreamingConnections = findPreference(
                "enableNonStreamingConnections");
        if (!CommsProviderFactory.getProvider()
                .hasFeature(CommsProvider.CommsFeature.ENDPOINT_SUPPORT)
                && !CommsProviderFactory.getProvider()
                        .hasFeature(
                                CommsProvider.CommsFeature.MESH_MODE_INPUT_SUPPORTED)
                && !CommsProviderFactory.getProvider().hasFeature(
                        CommsProvider.CommsFeature.MESH_MODE_OUTPUT_SUPPORTED)) {
            enableNonStreamingConnections.setEnabled(false);
        }

        final Preference nonStreamingEncryption = findPreference(
                "configureNonStreamingEncryption");
        nonStreamingEncryption
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {

                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
                                NetworkConnectionPreferenceFragment.this
                                        .getActivity());
                        alertBuilder
                                .setTitle(getString(R.string.mesh_encryption))
                                .setMessage(
                                        getString(
                                                R.string.mesh_encryption_message))
                                .setPositiveButton(
                                        getString(R.string.generate_key),
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int which) {
                                                try {
                                                    generateKey();
                                                } catch (Exception e) {
                                                    Toast.makeText(
                                                            NetworkConnectionPreferenceFragment.this
                                                                    .getActivity(),
                                                            "error generating key",
                                                            Toast.LENGTH_LONG)
                                                            .show();
                                                }
                                            }
                                        })
                                .setNegativeButton(getString(R.string.load_key),
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int which) {
                                                readKey();
                                            }
                                        })
                                .setNeutralButton(
                                        getString(R.string.forget_key),
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int which) {
                                                AtakPreferences prefs = AtakPreferences
                                                        .getInstance(
                                                                NetworkConnectionPreferenceFragment.this
                                                                        .getActivity());
                                                prefs.set("networkMeshKey", "");
                                            }
                                        });
                        alertBuilder.show();
                        return true;
                    }
                });

        ((PanEditTextPreference) findPreference("apiSecureServerPort"))
                .setValidIntegerRange(0, 65535);
        ((PanEditTextPreference) findPreference("apiUnsecureServerPort"))
                .setValidIntegerRange(0, 65535);
        ((PanEditTextPreference) findPreference("tcpConnectTimeout"))
                .checkValidInteger();
        ((PanEditTextPreference) findPreference("udpNoDataTimeout"))
                .checkValidInteger();
        ((PanEditTextPreference) findPreference("multicastTTL"))
                .checkValidInteger();
    }

    /**
     * Create a dialog for importing a certificate
     * @param context the context used for display (must be the activity context)
     * @param title the title to use for the dialog
     * @param type the type used for storage of the certificate
     * @param promptForPassword if it should also prompt for the password
     * @param connectString the connect string used in combination with the type for the certificate storage.
     */
    public static void getCertFile(final Context context, final String title,
            final String type, final boolean promptForPassword,
            final String connectString) {
        getCertFile(context, title, type, promptForPassword, connectString,
                new String[] {
                        "p12"
                });
    }

    /**
     * Create a dialog for importing a certificate
     * @param context the context used for display (must be the activity context)
     * @param title the title to use for the dialog
     * @param type the type used for storage of the certificate
     * @param promptForPassword if it should also prompt for the password
     * @param connectString the connect string used in combination with the type for the certificate storage.
     * @param fileExtensions the list of valid extensions for the certificate
     */
    public static void getCertFile(final Context context, final String title,
            final String type, final boolean promptForPassword,
            final String connectString, final String[] fileExtensions) {

        final File certDir = FileSystemUtils.getItem("cert");

        final String directory;

        if (certDir != null && IOProviderFactory.exists(certDir)
                && IOProviderFactory.isDirectory(certDir))
            directory = certDir.getAbsolutePath();
        else
            directory = Environment.getExternalStorageDirectory().getPath();

        ImportFileBrowserDialog.show(context.getString(R.string.select_space) +
                title + context.getString(R.string.to_import), directory,
                fileExtensions,
                new ImportFileBrowserDialog.DialogDismissed() {
                    @Override
                    public void onFileSelected(final File file) {
                        if (!FileSystemUtils.isFile(file))
                            return;

                        byte[] contents;
                        try {
                            contents = FileSystemUtils.read(file);
                        } catch (IOException ioe) {
                            Log.e(TAG, "Failed to read cert from: "
                                    + file.getAbsolutePath(), ioe);
                            return;
                        }

                        String server = null;
                        if (connectString == null
                                || connectString.isEmpty()) {
                            AtakCertificateDatabase.saveCertificate(type,
                                    contents);
                        } else {
                            NetConnectString ncs = NetConnectString
                                    .fromString(connectString);
                            server = ncs.getHost();
                            AtakCertificateDatabase
                                    .saveCertificateForServerAndPort(
                                            type, server, ncs.getPort(),
                                            contents);
                        }

                        Log.d(TAG,
                                "Adding " + title
                                        + " to certificate database from "
                                        + file.getAbsolutePath());

                        Intent certUpdated = new Intent(CERTIFICATE_UPDATED)
                                .putExtra("type", type)
                                .putExtra("promptForPassword",
                                        promptForPassword);

                        if (server != null) {
                            certUpdated.putExtra("host", server);
                        }

                        AtakBroadcast.getInstance().sendBroadcast(certUpdated);

                        Toast.makeText(
                                context,
                                context.getString(R.string.imported) + " "
                                        + title + ": "
                                        + file.getName(),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDialogClosed() {
                        //Do nothing
                    }
                }, context);
    }

    public void readKey() {
        ImportFileBrowserDialog.show("Select Mesh Encryption Key to Import",
                ATAKUtilities
                        .getStartDirectory(MapView.getMapView().getContext()),
                new String[] {
                        "pref"
                },
                new ImportFileBrowserDialog.DialogDismissed() {
                    @Override
                    public void onFileSelected(final File file) {
                        readKey(file);
                    }

                    @Override
                    public void onDialogClosed() {
                        //Do nothing
                    }
                }, NetworkConnectionPreferenceFragment.this.getActivity());
    }

    private void readKey(File file) {
        if (!FileSystemUtils.isFile(file))
            return;

        List<String> success = PreferenceControl
                .getInstance(
                        NetworkConnectionPreferenceFragment.this.getActivity())
                .loadSettings(file);

        if (!success.contains("networkMeshKey")) {
            Toast.makeText(
                    NetworkConnectionPreferenceFragment.this.getActivity(),
                    "Failed to load Mesh Encryption Key, file does not contain a key",
                    Toast.LENGTH_LONG)
                    .show();
        } else {
            Toast.makeText(
                    NetworkConnectionPreferenceFragment.this.getActivity(),
                    getString(R.string.mesh_key_loaded),
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    public void generateKey() {
        byte[] bytes = new byte[256];
        new SecureRandom().nextBytes(bytes);

        String s = new String(
                Base64.encode(bytes, Base64.URL_SAFE | Base64.NO_WRAP),
                FileSystemUtils.UTF8_CHARSET);

        final String key = "<?xml version='1.0' standalone='yes'?>\n" +
                "<preferences>\n" +
                "\t<preference version='1' name='com.atakmap.app_preferences'>\n"
                +
                "\t\t<entry key='networkMeshKey' class='class java.lang.String'>"
                +
                s.substring(12, 76) + "</entry>\n" +
                "\t</preference>\n" +
                "</preferences>\n";
        final EditText input = new EditText(this.getActivity());

        AlertDialog.Builder ad = new AlertDialog.Builder(this.getActivity());
        ad.setTitle(R.string.pref_save_title);
        ad.setView(input);
        ad.setNegativeButton(R.string.cancel, null);
        ad.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                            int whichButton) {
                        String inputText = input.getText().toString()
                                .trim();
                        String fname = FileSystemUtils
                                .sanitizeFilename(inputText);
                        if (inputText.equals(fname)) {
                            final File f = new File(
                                    FileSystemUtils
                                            .getItem(PreferenceControl.DIRNAME),
                                    fname + ".pref");

                            try {
                                FileSystemUtils.write(
                                        IOProviderFactory.getOutputStream(f),
                                        key);
                                Toast.makeText(
                                        NetworkConnectionPreferenceFragment.this
                                                .getActivity(),
                                        "created key file " + f.getName(),
                                        Toast.LENGTH_LONG)
                                        .show();
                                AlertDialog.Builder adl = new AlertDialog.Builder(
                                        NetworkConnectionPreferenceFragment.this
                                                .getActivity());
                                adl.setTitle("Load");
                                adl.setMessage(
                                        "The key has been saved as "
                                                + PreferenceControl.DIRNAME
                                                + "/" + f.getName()
                                                + " and can be shared with other TAK devices.   Would you like to load the key?");
                                adl.setPositiveButton(R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int whichButton) {
                                                readKey(f);
                                            }
                                        }).setNegativeButton(R.string.cancel,
                                                null);
                                adl.show();
                            } catch (IOException e) {
                                Log.e(TAG, "failed", e);
                                Toast.makeText(
                                        NetworkConnectionPreferenceFragment.this
                                                .getActivity(),
                                        "failed to save key",
                                        Toast.LENGTH_LONG)
                                        .show();
                            }
                        } else {
                            Toast.makeText(
                                    NetworkConnectionPreferenceFragment.this
                                            .getActivity(),
                                    getString(
                                            R.string.failed_save_bad_filename),
                                    Toast.LENGTH_LONG)
                                    .show();
                        }
                    }
                });

        ad.show();

    }
}
