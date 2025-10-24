
package com.atakmap.android.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;

import com.atakmap.coremap.log.Log;
import com.atakmap.util.zip.IoUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

abstract class BluetoothClientConnection extends BluetoothConnection {

    private static final String TAG = "BluetoothClientConnection";

    private final BluetoothAdapter adapter;
    protected BluetoothSocket socket = null;
    protected final String name;

    @SuppressLint({
            "MissingPermission"
    })
    BluetoothClientConnection(final BluetoothDevice device,
            final UUID uuid) {
        super(device, uuid);
        adapter = BluetoothAdapter.getDefaultAdapter();
        name = device.getName();
    }

    @Override
    protected boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    /**
     * onStart is responsible for cancelling any high bandwidth discovery and creating a secure or
     * insecure connection with the bluetooth device. First starting out with a secure connection,
     * and then upon failure, resorts to trying to connect insecurely.
     */
    @Override
    @SuppressLint({
            "MissingPermission"
    })
    protected void onStart(final BluetoothDevice device, final UUID uuid)
            throws IOException {

        adapter.cancelDiscovery();
        sleep(100);

        Log.d(TAG, "closing previous socket: " + socket);
        IoUtils.close(socket, TAG, "socket close exception");
        socket = null;

        if (device == null)
            return;

        if (!isRunning())
            return;

        try {
            socket = createRfcommSocketToServiceRecord(device, uuid);

            if (!isRunning() || socket != null)
                return;

            socket = createInsecureRfcommSocketToServiceRecord(device);

            if (!isRunning() || socket != null)
                return;

            socket = createRfCommSocket(device, uuid);
            if (!isRunning() || socket != null)
                return;

            socket = createInsecureRfcommSocketToServiceRecord(device, uuid);
            if (socket != null)
                return;

            //failed to connect, bail out
            throw new IOException(
                    "failed to bond to device: " + device.getName());
        } finally {
            if (!isRunning()) {
                IoUtils.close(socket, TAG,
                        "client connection stopped during discovery");
                socket = null;
            }
        }
    }

    @SuppressLint("MissingPermission")
    private static BluetoothSocket createInsecureRfcommSocketToServiceRecord(
            BluetoothDevice device, UUID uuid) {
        BluetoothSocket s = null;

        Log.d(TAG, "trying to bond insecurely with the device: " + device
                + " and the uuid: "
                + uuid);
        try {
            s = device.createInsecureRfcommSocketToServiceRecord(uuid);
            sleep(250);

            s.connect();
            if (!s.isConnected())
                throw new IOException("connect completed, but not connected");

            return s;
        } catch (IOException ioe) {
            Log.w(TAG, "error encountered trying to bond (insecurely)", ioe);
        }

        // reset
        IoUtils.close(s, TAG, "error closing insecure socket");
        return null;
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    private static BluetoothSocket createRfCommSocket(BluetoothDevice device,
            UUID uuid) {
        BluetoothSocket s = null;
        Log.d(TAG, "trying to bond using reflection: " + device
                + " and the uuid: " + uuid);
        Method m;
        try {
            m = device.getClass().getMethod("createRfcommSocket", int.class);
            s = (BluetoothSocket) m.invoke(device, 1);
            if (s == null)
                throw new IOException("could not create a socket");
            s.connect();
            if (!s.isConnected())
                throw new IOException("connect completed, but not connected");

            return s;
        } catch (SecurityException | NoSuchMethodException
                | IllegalArgumentException | IllegalAccessException
                | InvocationTargetException e) {
            Log.e(TAG, "wrap() failed", e);
        } catch (IOException ioe) {
            Log.w(TAG, "error encountered trying to bond (reflection)", ioe);
        }

        // reset
        IoUtils.close(s, TAG, "error closing reflection socket");
        return null;
    }

    @SuppressLint("MissingPermission")
    private static BluetoothSocket createInsecureRfcommSocketToServiceRecord(
            BluetoothDevice device) {
        BluetoothSocket s = null;
        try {
            ParcelUuid[] uuids = device.getUuids();
            if (uuids != null && uuids.length > 0) {
                final UUID supplied_uuid = device.getUuids()[0].getUuid();
                Log.d(TAG,
                        "trying to bond securely with the device (using provided UUID): "
                                + device
                                + " and the uuid: " + supplied_uuid);
                s = device
                        .createInsecureRfcommSocketToServiceRecord(
                                supplied_uuid);
                sleep(250);

                s.connect();
                if (!s.isConnected())
                    throw new IOException(
                            "connect completed, but not connected");

                return s;
            }
        } catch (NullPointerException | IOException ioe) {
            Log.w(TAG,
                    "error encountered trying to bond (securely with provided UUID)",
                    ioe);
        }

        // reset
        IoUtils.close(s, TAG, "error closing insecure socket");
        return null;
    }

    @SuppressLint("MissingPermission")
    private static BluetoothSocket createRfcommSocketToServiceRecord(
            BluetoothDevice device, UUID uuid) {
        BluetoothSocket s = null;
        Log.d(TAG, "trying to bond securely with the device: " + device
                + " and the uuid: " + uuid);
        try {
            s = device.createRfcommSocketToServiceRecord(uuid);
            sleep(250);

            s.connect();
            if (!s.isConnected())
                throw new IOException("connect completed, but not connected");

            return s;
        } catch (IOException ioe) {
            Log.w(TAG, "error encountered trying to bond (securely)", ioe);
        }

        //reset
        IoUtils.close(s, TAG, "error closing secure socket");
        return null;
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    protected void onStop() throws IOException {
        Log.d(TAG, "closing the bluetooth socket for: " + name);
        if (socket != null) {
            try {
                socket.close();
                Log.d(TAG, "closed the bluetooth socket for: " + name);
            } catch (IOException ioe) {
                Log.d(TAG, "error closing the socket for: " + name);
            }
            socket = null;
        }
    }
}
