
package com.atakmap.android.radiolibrary;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.DefaultIOProvider;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.util.List;

import gov.tak.api.util.Disposable;

public class ControllerPppd extends BroadcastReceiver
        implements Runnable, RadiosQueriedListener, Disposable {

    public static final String TAG = "ControllerPppd";
    private Thread t;
    private volatile boolean cancelled = false;
    private String cancelled_reason;
    private final Context context;
    private final File root;

    private String pppOptions = null;

    /**
     * Action for the system receiver
     */
    public static final String ACTION = "com.atakmap.pppd";

    /**
     * The Harris Radio Manager
     */
    private final HarrisSaRadioManager harrisSaRadioManager;

    public ControllerPppd(Context c,
            HarrisSaRadioManager harrisSaRadioManager) {
        context = c;
        // For use with the Android S6 com.atakmap.Samservices
        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction(ACTION);
        AtakBroadcast.getInstance().registerSystemReceiver(this, filter);

        this.harrisSaRadioManager = harrisSaRadioManager;

        root = new File(context.getFilesDir(), "ppp");
    }

    /**
     * Returns true if the controller is no longer running.
     * @return true if it is no longer running.
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * If the controller is no longer running due to a particular reason then allow for that
     * reason to be known.
     * @return a reason if the pppd controller is cancelled due to an error.
     */
    public String getCancelledReason() {
        return cancelled_reason;
    }

    /**
     * Start to initialize the pppd daemon with the appropriate parameters.
     */
    synchronized public void start() {
        cancelled = false;
        cancelled_reason = "";
        try {
            FileSystemUtils.copyFile(new File(root, "options"),
                    FileSystemUtils.getItem("tools/.options"),
                    new DefaultIOProvider());

            try {
                pppOptions = FileSystemUtils.copyStreamToString(
                        FileSystemUtils.getItem("tools/.options"));
            } catch (IOException ioe) {
                Log.d(TAG, "could not cache file");
                pppOptions = null;
            }
            harrisSaRadioManager.addListener(this);
            harrisSaRadioManager.queryRadios();
        } catch (Exception e) {
            Log.e(TAG, "error copying file over", e);
        }
    }

    @Override
    public void run() {
        while (!cancelled) {
            try {
                NetworkInterface nd = NetworkInterface.getByName("ppp0");
                if (nd == null || !nd.isUp()) {
                    if (harrisSaRadioManager.getSelectedRadio() != null) {
                        int lastport = harrisSaRadioManager.getSelectedRadio()
                                .getLastPort();
                        String s = "/dev/ttyACM" + lastport;
                        sendIntent(s);
                    } else {
                        final String genericSerial = getGenericSerial();
                        if (genericSerial != null) {
                            Log.d(TAG, "found a generic tty device - "
                                    + genericSerial);
                            sendIntent(genericSerial);
                        } else {
                            Log.d(TAG, "no connection to a Harris radio found");
                            cancelled_reason = context.getString(
                                    R.string.no_serial_connection_found);
                            stop();
                        }
                    }
                }
            } catch (Exception ie) {
                Log.d(TAG, "error determining if ppp0 is up", ie);
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {
            }
        }

    }

    private String getGenericSerial() {
        // find the first device node in a list of possible device nodes that could be used
        String[] altDevNodes = new String[] {
                "/dev/ttyACM0", "/dev/ttyACM1", "/dev/ttyACM2", "/dev/ttyACM3",
                "/dev/ttyUSB0", "/dev/ttyUSB1", "/dev/ttyUSB2", "/dev/ttyUSB3"
        };
        for (String dev : altDevNodes)
            if (new File(dev).exists())
                return dev;

        return null;
    }

    private void sendIntent(String s) {
        // For use with the Android S6 com.atakmap.Samservices
        Intent i = new Intent("com.atakmap.exec");
        i.putExtra("command", "pppd");
        i.putExtra(
                "args",
                s
                        + " file "
                        + FileSystemUtils
                                .getItem("tools/.options"));

        if (pppOptions != null) {
            i.putExtra("pppOptions", pppOptions);
        }

        i.putExtra("return", "com.atakmap.pppd");
        AtakBroadcast.getInstance().sendSystemBroadcast(i);

    }

    synchronized public void stop() {
        Log.d(TAG, "stopping the polling for Harris SA");
        harrisSaRadioManager.removeListener(this);

        cancelled = true;
        if (t != null) {
            t.interrupt();
            t = null;
        }
        if (!FileSystemUtils.getItem("tools/.options").delete()) {
            Log.d(TAG, "failed to delete .options");
        }

        Intent i = new Intent("com.atakmap.exec");
        i.putExtra("command", "pppd");
        i.putExtra("args", "stop");
        i.putExtra("return", "com.atakmap.pppd");
        AtakBroadcast.getInstance().sendSystemBroadcast(i);
    }

    @Override
    public void dispose() {
        try {
            AtakBroadcast.getInstance().unregisterSystemReceiver(this);
        } catch (Exception e) {
            // protect against double call to dispose
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null)
            return;

        if (action.equals(ACTION)) {
            Bundle b = intent.getExtras();
            Log.d(TAG, "result received: " + action + ", status: "
                    + ((b == null) ? "null"
                            : intent.getExtras().get("status")));
        }
    }

    /**
     * Fires when the radios are finished being queried
     *
     * @param radios The radios they have been queried
     */
    @Override
    public void radiosQueried(List<HarrisRadio> radios) {
        harrisSaRadioManager.removeListener(this);
        if (harrisSaRadioManager.pppConnectionExists()) {
            //If ppp connection already exists, then just start the thread.
            t = new Thread(this, "ControllerPppd");
            t.start();
        } else if (radios.size() == 1) {
            // get the first and only radio and use that
            harrisSaRadioManager.setSelectedRadio(radios.get(0));
            t = new Thread(this);
            t.start();
        } else if (radios.size() > 1) {
            //Prompt the user with multiple radios
            final AlertDialog.Builder builder = new AlertDialog.Builder(
                    context);

            //Load list of radios
            CharSequence[] radioDescriptions = new CharSequence[radios.size()];
            int i = 0;
            for (HarrisRadio radio : radios) {
                radioDescriptions[i++] = radio.toString();
            }

            //Create dialog
            builder.setTitle(R.string.select_harris_sa_radio)
                    .setCancelable(false)
                    .setSingleChoiceItems(radioDescriptions, 0, null)
                    .setPositiveButton(R.string.ok, (dialog, id) -> {
                        ListView lv = ((AlertDialog) dialog).getListView();
                        int itemPos = lv.getCheckedItemPosition();
                        harrisSaRadioManager
                                .setSelectedRadio(radios.get(itemPos));
                        t = new Thread(ControllerPppd.this);
                        cancelled = false;
                        t.start();
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
                        harrisSaRadioManager.setSelectedRadio(null);
                        ControllerPppd.this.stop();
                    });

            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    builder.show();
                }
            });
        } else if (getGenericSerial() != null) {
            harrisSaRadioManager.setSelectedRadio(null);
            t = new Thread(this);
            t.start();
        } else {
            cancelled_reason = context
                    .getString(R.string.no_serial_connection_found);
            stop();
        }
    }
}
