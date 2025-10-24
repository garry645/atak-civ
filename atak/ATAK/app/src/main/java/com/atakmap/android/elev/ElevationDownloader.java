
package com.atakmap.android.elev;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.Preference;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.atakmap.android.gui.HintDialogHelper;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.map.AtakMapView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;

import gov.tak.platform.commons.resources.AndroidResourceManager;

/**
 * This class isolates all changes for ATAK 4.3.0 so that this set of fixes can be deployed
 * without disturbing the rest of the mapping.
 */
class ElevationDownloader
        extends gov.tak.platform.client.dted.ElevationDownloader
        implements AtakMapView.OnMapMovedListener {

    private static final String TAG = "ElevationDownloader";

    // Preference keys
    private static final String PREF_WARNING = "dted.install.warning";
    private static final String PREF_DOWNLOAD_SERVER = "prefs_dted_stream_server";
    private static final String PREF_DTED_STREAM = "prefs_dted_stream";

    // Default server to download tiles from
    private static final String DEFAULT_DOWNLOAD_SERVER = "tak.gov";

    private static final String[] HEMISPHERE_NAMES = new String[] {
            "North East Hemisphere", "North West Hemisphere",
            "South East Hemisphere", "South West Hemisphere"
    };

    private static final String[] HEMISPHERE_FILES = new String[] {
            "dted_ne_hemi.zip", "dted_nw_hemi.zip", "dted_se_hemi.zip",
            "dted_sw_hemi.zip"
    };

    private static ElevationDownloader _instance;

    static ElevationDownloader getInstance() {
        return _instance;
    }

    private final MapView _mapView;
    private final Context _context;
    private final AtakPreferences _prefs;

    ElevationDownloader(MapView mapView) {
        super(new AndroidResourceManager(mapView.getContext()),
                ElevationMapComponent.getDtedWatcher());
        _mapView = mapView;
        _context = mapView.getContext();
        _prefs = AtakPreferences.getInstance(mapView.getContext());
        _mapView.addOnMapMovedListener(this);
        _instance = this;

        // trigger for an initial download
        onMapMoved(_mapView, false);
    }

    @Override
    public void dispose() {
        super.dispose();
        _mapView.removeOnMapMovedListener(this);
    }

    @Override
    public String getDownloadServer() {
        return _prefs.get(PREF_DOWNLOAD_SERVER, DEFAULT_DOWNLOAD_SERVER);
    }

    @Override
    public boolean isEnabled() {
        return _prefs.get(PREF_DTED_STREAM, true);
    }

    @Override
    protected GeoBounds[] getCurrentAreasOfInterest() {
        return new GeoBounds[] {
                _mapView.getBounds()
        };
    }

    /**
     * A dialog telling the user that elevation might not be available on this device and that is it
     * pretty important.  If we can get automatic downloading working in this class based on the current map position,
     * then we might not need to even have this method.
     */
    public void displayInformation(Context context) {
        HintDialogHelper.showHint(context,
                "Install Elevation Data",
                "Elevation Data is very important to the proper functioning of any mapping tool.  Click 'OK' to choose what elevation data you want. \n\nIf you want more, you can download it under Settings",
                PREF_WARNING,
                new HintDialogHelper.HintActions() {
                    @Override
                    public void postHint() {
                        chooseElevationToDownload(_context);
                    }

                    public void preHint() {
                    }

                });

    }

    /**
     * Support the ability to download elevation data based on the hemisphere.
     * @param context Map view or preference context
     */
    private void chooseElevationToDownload(final Context context) {
        final ListView listView = new ListView(context);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setAdapter(new ArrayAdapter<>(context,
                android.R.layout.simple_list_item_multiple_choice,
                HEMISPHERE_NAMES));
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(listView);
        builder.setCancelable(false);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listView.isItemChecked(0);
                        startDownload(context, listView);
                    }
                });

        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                builder.show();
            }
        });
    }

    private void startDownload(Context context, ListView listView) {

        final ProgressDialog progressDialog = new ProgressDialog(context);
        final DownloadThread dt = new DownloadThread(context, progressDialog,
                listView);

        progressDialog
                .setMessage(context.getString(R.string.downloading_message));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setButton(_context.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dt.cancel();
                    }
                });

        progressDialog.show();
        dt.start();
    }

    private class DownloadThread extends Thread {

        private final ProgressDialog dialog;
        private final ListView listView;
        private final Context context;

        private volatile boolean cancelled = false;

        DownloadThread(Context context, ProgressDialog dialog,
                ListView listView) {
            this.dialog = dialog;
            this.listView = listView;
            this.context = context;
        }

        /**
         * Stop the downloading process for the elevation data
         */
        public void cancel() {
            cancelled = true;
        }

        private void setProgress(final int progress, final int max,
                final String message) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (max == -1) {
                        setIndeterminate(dialog, true);
                    } else {
                        setIndeterminate(dialog, false);
                        dialog.setMax(100);
                        dialog.setProgress(
                                (int) Math
                                        .floor((progress / (float) max) * 100));
                    }
                    dialog.setMessage(message);
                }
            });
        }

        @Override
        public void run() {
            final String server = _prefs.get(PREF_DOWNLOAD_SERVER,
                    DEFAULT_DOWNLOAD_SERVER);

            final byte[] buffer = new byte[8096 * 2];

            int len, off;
            int count = 0;
            for (int i = 0; i < HEMISPHERE_FILES.length; ++i) {
                if (cancelled || !isRunning())
                    return;

                final String file = HEMISPHERE_FILES[i];
                final String name = HEMISPHERE_NAMES[i];

                if (!listView.isItemChecked(i))
                    continue;
                count++;
                final String srcPath = "https://" + server + "/elevation/DTED/"
                        + file;
                final File dstPathZip = FileSystemUtils.getItem("DTED/" + file);

                final String message = "Downloading: " + name + "(" + count
                        + " of " + listView.getCheckedItemCount() + ")";
                setProgress(0, 100, message);

                InputStream is = null;
                OutputStream os = null;
                try {
                    //Log.d(TAG, "copying from: " + srcPath + " to " + dstPathZip);
                    URL u = new URL(srcPath);
                    URLConnection conn = u.openConnection();
                    final int fileLength = conn.getContentLength();
                    setProgress(0, fileLength, message);
                    is = new BufferedInputStream(conn.getInputStream());
                    off = 0;
                    os = new FileOutputStream(dstPathZip.getAbsolutePath());
                    while ((len = is.read(buffer)) >= 0) {
                        os.write(buffer, 0, len);
                        off += len;
                        setProgress(off, fileLength, message);
                        if (cancelled)
                            return;
                    }
                    os.flush();
                    setProgress(-1, -1, "Decompressing: " + name
                            + "(" + count
                            + " of " + listView.getCheckedItemCount() + ")");
                    File pFile = dstPathZip.getParentFile();
                    if (pFile != null) {
                        FileSystemUtils.unzip(
                                new File(dstPathZip.getAbsolutePath()),
                                pFile, true);
                    }

                } catch (Exception e) {
                    Log.d(TAG, "error occurred: ", e);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (Exception ignored) {
                        }
                    }
                    if (os != null) {
                        try {
                            os.close();
                        } catch (Exception ignored) {
                        }
                    }
                    FileSystemUtils.delete(dstPathZip);
                }
            }

            // Dismiss progress dialog
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (dialog != null) {
                        try {
                            dialog.dismiss();
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
        }
    }

    private static void setIndeterminate(ProgressDialog dialog,
            boolean isIndeterminate) {
        dialog.setIndeterminate(isIndeterminate);
        if (isIndeterminate) {
            dialog.setProgressNumberFormat(null);
            dialog.setProgressPercentFormat(null);
        } else {
            dialog.setProgressNumberFormat("%1d/%2d");
            NumberFormat percentInstance = NumberFormat.getPercentInstance();
            percentInstance.setMaximumFractionDigits(0);
            dialog.setProgressPercentFormat(percentInstance);
        }
    }

    @Override
    public void onMapMoved(AtakMapView view, boolean animate) {
        scanCurrentAreas();
    }

    /**
     * Setup a preference button for downloading elevation data.   This localizes the changes for
     * ElevationOverlaysPreferenceFragment
     * @param prefActivity the activity used for the mechanism for downloading the datasets.
     * @param p the preference
     */
    void setupPreferenceDownloader(final Activity prefActivity,
            final Preference p) {
        p.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        chooseElevationToDownload(prefActivity);
                        return true;
                    }
                });
    }
}
