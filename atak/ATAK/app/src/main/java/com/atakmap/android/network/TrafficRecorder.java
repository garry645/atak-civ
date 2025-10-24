
package com.atakmap.android.network;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.gui.AlertDialogHelper;
import com.atakmap.android.math.MathUtils;
import com.atakmap.app.R;
import com.atakmap.comms.NetworkUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.util.Disposable;
import gov.tak.platform.util.LimitingThread;

public class TrafficRecorder implements Runnable {

    private static final int DEFAULT_BUFFER_LENGTH = 1024 * 64; // 64k ; max udp packet size

    // This is the maximum interval between issuing joins when no traffic is received.
    private static final int _IGMP_JOIN_INTERVAL = 20000; // 8k milliseconds

    private static final String TAG = "TrafficRecorder";
    private final String address;
    private final int port;
    private final NetworkInterface ni;
    private final File file;
    private final File index;
    private final Context context;

    private MulticastSocket socket;
    private volatile boolean cancelled = false;
    private AlertDialog ad = null;


    private TextView stats = null;


    /**
     * Create a new traffic recorder.
     * @param address the network address to record (multicast).
     * @param port  the network port to record from.
     * @param ni the network device to record from.
     * @param file the file to record to.
     * @param view the mapview to pull the context from
     * @deprecated
     */
    @DeprecatedApi(since= "5.6", removeAt = "5.9", forRemoval = true)
    public TrafficRecorder(final String address,
            final int port,
            final NetworkInterface ni,
            final File file,
            final View view) {
        this(address,port, ni, file, view.getContext());
    }

    /**
     * Create a new traffic recorder.
     * @param address the network address to record (multicast).
     * @param port  the network port to record from.
     * @param ni the network device to record from.
     * @param file the file to record to.
     */
    public TrafficRecorder(final String address,
                           final int port,
                           final NetworkInterface ni,
                           final File file,
                           final Context context) {
        this.address = address;
        this.port = port;
        this.ni = ni;
        this.file = file;
        this.index = new File(file.toString() + ".idx");
        this.context = context;
    }

    public void cancel() {
        cancelled = true;
        if (socket != null) {
            socket.close();
        }
    }

    @Override
    public void run() {
        create();

        DatagramPacket packet = new DatagramPacket(
                new byte[DEFAULT_BUFFER_LENGTH], DEFAULT_BUFFER_LENGTH);
        InetAddress sourceAddress = null;
        try {
            sourceAddress = InetAddress.getByAddress(NetworkUtils
                    .getByteAddress(address));
        } catch (Exception ignored) {
        }

        if (sourceAddress == null || ni == null) {
            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, R.string.traffic_recorder_interface_missing,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        try {
            socket = new MulticastSocket(port);
            socket.setSoTimeout(_IGMP_JOIN_INTERVAL);
        } catch (Exception e) {
            dismiss(context.getString(R.string.unable_to_listen_for_data_stopping), ad);
            return;
        }

        if (sourceAddress != null && sourceAddress.isMulticastAddress()) {
            InetSocketAddress sourceInetAddress = new InetSocketAddress(
                    address, port);
            try {
                if (ni != null)
                    socket.joinGroup(sourceInetAddress, ni);
                else
                    socket.joinGroup(sourceInetAddress.getAddress());
            } catch (Exception e) {
                dismiss(context.getString(R.string.error_occurred_subscribing_to_traffic), ad);
                return;
            }
        }

        long filelength = 0;
        long totalpackets = 0;
        long starttime = SystemClock.elapsedRealtime();
        UpdateStatistics us = new UpdateStatistics();
        us.updateStats(0, starttime, 0);

        try (FileOutputStream fos = IOProviderFactory.getOutputStream(file);
                OutputStream os = IOProviderFactory.getOutputStream(index);
                PrintWriter idxos = new PrintWriter(os)) {

            while (!cancelled) {
                socket.receive(packet);
                fos.write(packet.getData(), packet.getOffset(),
                        packet.getLength());
                int size = packet.getLength() - packet.getOffset();
                idxos.println(filelength + ","
                        + android.os.SystemClock.elapsedRealtime()
                        + "," + size);
                filelength += size;

                us.updateStats(filelength, starttime, totalpackets++);
            }
        } catch (FileNotFoundException fnfe) {
            dismiss(context.getString(R.string.unable_to_create_recording_file), ad);
            return;
        } catch (IOException ioe) {
            if (!cancelled)
                Log.d(TAG, "exception has occurred: ", ioe);
        }
        dismiss(null, ad);
        us.dispose();
    }

    private void create() {
        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
                context);
        View v = LayoutInflater.from(context).inflate(R.layout.traffic_recorder, null);
        TextView tv1 = v.findViewById(R.id.block1);
        tv1.setText(String.format(LocaleUtil.getCurrent(),
                context.getString(R.string.record_network_traffic_message), address, port, (ni == null)?"default":ni.getName()));
        stats = v.findViewById(R.id.block2);


        alertBuilder
                .setTitle(R.string.recording_network_traffic)
                .setView(v)
                .setPositiveButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();
                                cancel();
                            }
                        });
        ((Activity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ad = alertBuilder.create();
                ad.show();
                AlertDialogHelper.adjustWidth(ad, .90d);
            }
        });

    }


    private class UpdateStatistics implements Disposable {

        long totalBytes = 0;
        long startTime = 0;
        long totalPackets = 0;

        final LimitingThread lt;

        void updateStats(long totalBytes, long startTime, long totalPackets) {
            this.totalBytes = totalBytes;
            this.startTime = startTime;
            this.totalPackets = totalPackets;
            lt.exec();
        }


        UpdateStatistics() {
           lt = new LimitingThread("updatestats", new Runnable() {
                @Override
                public void run() {
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            long elapsedTime = SystemClock.elapsedRealtime() - startTime;
                            stats.setText(String.format(LocaleUtil.getCurrent(),
                                    "Packets Received: %d / Bytes Received %s / Elapsed Time %s",
                                    totalPackets,
                                    MathUtils.GetLengthString(totalBytes),
                                    MathUtils.GetTimeRemainingString(elapsedTime)));
                        }
                    });
                    // update every 2 seconds
                    try {
                        Thread.sleep(2000);
                    } catch (Exception ignored) {}
                }
            });
        }

        @Override
        public void dispose() {
            lt.dispose();
        }
    }



    private void dismiss(final String reason, final AlertDialog ad) {
        ((Activity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (reason != null)
                    Toast.makeText(context, reason,
                            Toast.LENGTH_SHORT)
                            .show();
                if (ad != null)
                    ad.dismiss();
            }
        });
    }

}
