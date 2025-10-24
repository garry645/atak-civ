
package com.atakmap.android.radiolibrary.rover;

import androidx.annotation.NonNull;

import com.atakmap.android.radiolibrary.rover.KLV.KeyLength;
import com.atakmap.android.radiolibrary.rover.KLV.LengthEncoding;
import com.atakmap.android.radiolibrary.rover.KLV_L3.FrameSize;
import com.atakmap.android.radiolibrary.rover.KLV_L3.KeyCategory;
import com.atakmap.android.radiolibrary.rover.KLV_L3.ModuleType;
import com.atakmap.android.radiolibrary.rover.KLV_L3.NetworkFramingType;
import com.atakmap.android.radiolibrary.rover.KLV_L3.VideoEncoder;
import com.atakmap.android.radiolibrary.rover.KLV_L3.WaveformType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.CRC32;

/**
 * Main class for describing a specific type of Radio within the L3 Com Rover 
 * series of receivers.   All of the receivers use the same ICD; however 
 * implementations of that ICD vary across units.   
 * Notable physical differences are with the number of transmitters/receivers.
 */
public abstract class Radio {
    /**
     * Allows for a registered receiver to get command output,
     * input, status, and preset information when changed.
     */
    public interface RadioListener {

        /**
         * Outgoing sending status for the Radio.   
         * This is only useful for describing the outgoing data from the 
         * end user device to the Rover.
         */
        void onMcoStatus(MessageManager.Status status);

        /**
         * Incoming status for the Radio. 
         * Monitors the receipt of incoming packets and when there are 
         * consecutive losses where packets are expected, then the status
         * moves from TIMEOUT to ERROR.
         */
        void onMciStatus(MessageManager.Status status);

        /**
         * Provides the heartbeat status for the device based on the solicited
         * response (automatically done with a static period after monitoring has
         * been started.
         */
        void onReceiverStatus(Radio.Receiver r);

        /**
         * Callback when the preset list is returned after a successful
         * command to solicit the presets.
         */
        void onReceivePresets(List<String> presets);

        /**
         * Provides log style messages from the library.
         */
        void log(String tag, String message);

    }

    /**
     * Receiver class, provides status information (heart beat) for the rover.
     */
    public static class Receiver {
        // properties
        public final int number;
        public int frequency;
        public int autoreconnect;
        public int dataRate;
        public int fec;
        public int searchMode;
        public int searchProgress;
        public int searchIterationsComplete;
        public int searchStateLock;
        public int rssi;
        public NetworkFramingType network_framing;
        public final int maxRssi = 100;
        public int linkEstablished;
        public int dataLinkUtilization;
        public WaveformType waveformCategory;
        public ModuleType moduleType;
        public int channel;
        public double version;
        public int build;

        public Receiver(final int number) {
            this.number = number;
        }

        @NonNull
        public String toString() {
            return "receiver: " + number + " frequency: " + frequency + " " +
                    "autoreconnect: " + autoreconnect + " " +
                    "dataRate: " + dataRate + " " +
                    "waveformcategory: " + waveformCategory + " " +
                    "channel: " + channel + " " +
                    "moduleType: " + moduleType + " " +
                    "fec: " + fec + " " +
                    "searchMode: " + searchMode + " " +
                    "searchProgress: " + searchProgress + " " +
                    "searchIterationsComplete: " + searchIterationsComplete
                    + " " +
                    "searchStateLock: " + searchStateLock + " " +
                    "rssi: " + rssi + " " +
                    "maxRssi: " + maxRssi + " " +
                    "linkEstablished: " + linkEstablished + " " +
                    "dataLinkUtilization: " + dataLinkUtilization + " " +
                    "version: " + version + " " +
                    "build: " + build;
        }
    }

    /** 
     * Structure that contains the short key, value command status values 
     * for the SIR series of radios.   This is an intermediary to clean
     * up calling the KLV class proper.
     */
    public static class ShortKLV {
        final int key;
        final LengthEncoding len;
        final byte[] val;

        /**
        * create a shortKLV klv with the value supplied (cmd)
        */
        public ShortKLV(final int key, final LengthEncoding len,
                final byte[] val) {
            this.key = key;
            this.len = len;
            this.val = val;
        }

        /**
        * create a shortKLV klv with the value supplied (cmd)
        */
        public ShortKLV(final int key, final LengthEncoding len,
                final int val) {
            this.key = key;
            this.len = len;
            if (len == KLV.LengthEncoding.OneByte) {
                this.val = KLV.int8ToByteArray((byte) val);
            } else if (len == KLV.LengthEncoding.TwoBytes) {
                this.val = KLV.int16ToByteArray((short) val);
            } else if (len == KLV.LengthEncoding.FourBytes) {
                this.val = KLV.int32ToByteArray(val);
            } else {
                this.val = null;
            }
        }

        /**
        * create a shortKLV klv wit no value supplied (sts)
        */
        public ShortKLV(final int key) {
            this.key = key;
            this.len = LengthEncoding.BER;
            this.val = null;
        }
    }

    // space between calling changing between frequency scanning and setting
    final static int ROVER_COMMAND_SPACING = 750;
    final static int STATUS_COMMAND_SPACING = 1750;
    final static int UNIT_SEND_TTL = 12;
    final static int UNIT_RECEIVE_TIMEOUT = 12000;
    final static int UNIT_RETRY_TIMEOUT = 5000;

    private boolean DEBUG = false;

    final static String TAG = "Radio";

    MessageManager messageManager = null;
    protected RadioListener mRadioListener = null;
    protected boolean mIsMonitoring;

    // properties
    public final CopyOnWriteArrayList<String> presets;

    public final String nickname;

    // properties - network
    protected final String mWebAdminAddress;

    protected final String mMciOutputIp;
    public final int mMciOutputPort;

    public final String mMciInputIp;
    public final int mMciInputPort;

    public final Receiver receiver;
    public final Receiver receiver2;

    private RoverStatusRunnable monitor;

    /* ********  C O N S T A N T S  -  U N I V E R S A L  S T R I N G     ******** */
    public final byte[] UNIVERSAL_SET = {
            (byte) 0x06, (byte) 0x0E, (byte) 0x2B, (byte) 0x34, (byte) 0x01,
            (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x0F, (byte) 0x4C,
            (byte) 0x33, (byte) 0x4D, (byte) 0x44, (byte) 0x4C, (byte) 0x01,
            (byte) 0x00
    };

    /* ********  P R O P E R T I E S  ******** */

    /**
     * Construction of a generic radio type.
     * @param nickname a user readable description of the radio.
     * @param radioAddress the ip address of the radio for use when launching the web configurator
     * @param mciOutputAddress the multicast input address, standard is 230.77.68.76   (command in)
     * @param mciOutputPort the multicast output address, standard is 19024 (command in)
     * @param mciInputAddress the multicast output address, standard is 230.77.68.76 (status out)
     * @param mciInputPort the multicast output address, standard is 19025 (status out)
     */
    protected Radio(final String nickname,
            final String radioAddress,
            final String mciInputAddress,
            final int mciInputPort,
            final String mciOutputAddress,
            final int mciOutputPort) {

        // init storage
        presets = new CopyOnWriteArrayList<>();

        receiver = new Receiver(1);
        receiver2 = new Receiver(2);

        this.nickname = nickname;
        this.mWebAdminAddress = radioAddress;

        this.mMciInputIp = mciInputAddress;
        this.mMciInputPort = mciInputPort;

        this.mMciOutputIp = mciOutputAddress;
        this.mMciOutputPort = mciOutputPort;

        messageManager = new MessageManager(mciInputAddress,
                mciInputPort,
                mciOutputAddress,
                mciOutputPort,
                UNIT_SEND_TTL,
                UNIT_RECEIVE_TIMEOUT,
                UNIT_RETRY_TIMEOUT);
        messageManager.setListener(this.dataListener);
    }

    // radio listener
    public void setRadioListener(RadioListener listener) {
        mRadioListener = listener;
    }

    public RadioListener getRadioListener(RadioListener listener) {
        return mRadioListener;
    }

    public void log(final String tag, final String message) {
        if (mRadioListener != null) {
            mRadioListener.log(tag, message);
        }
    }

    public void stop() {
        stopMonitoring();
        stopListening();
    }

    public void setDebug(boolean enabled) {
        DEBUG = enabled;
    }

    public boolean isMonitoring() {
        synchronized (this) {
            return mIsMonitoring;
        }
    }

    public void startMonitoring() {
        synchronized (this) {
            mIsMonitoring = true;

            if (monitor != null) {
                monitor.cancel();
            }
            monitor = new RoverStatusRunnable();
            Thread monitorThread = new Thread(monitor);
            monitorThread.start();
        }

    }

    public void stopMonitoring() {
        synchronized (this) {
            mIsMonitoring = false;
            if (monitor != null)
                monitor.cancel();
        }
    }

    /**
     * Start interacting with the radio.
     * @param iface describes the specific interface as defined by the interface name one of eth0,
     *            eth1, eth2, wlan0, ppp0, rmnet0, etc to use, or null for "ANY" interface and will
     *            result in the default interface being selected for multicast traffic. This
     *            parameter will do nothing for unicast traffic.
    
     */
    public void startListening(final String iface) {
        // start listening
        messageManager.setInterface(iface);
        messageManager.startListening();
    }

    public boolean isListening() {
        return messageManager.isListening();
    }

    public void stopListening() {
        // stop listening
        messageManager.stopListening();
    }

    protected byte[] getFrequentQuery() {
        // bulk command
        ArrayList<ShortKLV> command = new ArrayList<>();
        command.add(new ShortKLV(
                KLV_L3.KeyCategoryReceiver.FrequencyValue.value()));
        command.add(
                new ShortKLV(KLV_L3.KeyCategoryReceiver.AutoReconnect.value()));

        command.add(new ShortKLV(KLV_L3.KeyCategoryReceiver.Channel.value()));
        command.add(new ShortKLV(KLV_L3.KeyCategoryReceiver.Module.value()));
        command.add(new ShortKLV(
                KLV_L3.KeyCategoryReceiver.DDLModuleChannel.value()));
        command.add(new ShortKLV(
                KLV_L3.KeyCategoryReceiver.LinkDemultiplexing.value()));

        command.add(
                new ShortKLV(KLV_L3.KeyCategoryReceiver.SearchMode.value()));
        command.add(new ShortKLV(
                KLV_L3.KeyCategoryReceiver.SearchProgress.value()));
        command.add(new ShortKLV(
                KLV_L3.KeyCategoryReceiver.SearchIterationsComplete.value()));
        command.add(new ShortKLV(
                KLV_L3.KeyCategoryReceiver.SearchStateLock.value()));

        command.add(new ShortKLV(KLV_L3.KeyCategoryReceiver.RSSI.value()));
        command.add(new ShortKLV(
                KLV_L3.KeyCategoryReceiver.WaveformCategory.value()));
        command.add(new ShortKLV(
                KLV_L3.KeyCategoryReceiver.LinkEstablishedStatus.value()));
        command.add(new ShortKLV(
                KLV_L3.KeyCategoryReceiver.DataLinkUtilization.value()));
        command.add(new ShortKLV(KLV_L3.KeyCategoryReceiver.DataRate.value()));

        return constructKLV(KLV_L3.KeyCategory.Receiver1.value(), command);
    }

    public byte[] getDeviceVersion() {
        ArrayList<ShortKLV> command = new ArrayList<>();
        command.add(new ShortKLV(
                KLV_L3.KeyCategoryConfiguration.SoftwareVersionID.value()));
        return constructKLV(KLV_L3.KeyCategory.Configuration.value(), command);
    }

    public void getEncodingParams() {
        ArrayList<ShortKLV> command = new ArrayList<>();
        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.VideoEncoderEnable.value()));
        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.VideoEncoderType.value()));
        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.VideoEncoderResolution.value()));
        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.VideoEncoderBitrate.value()));
        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.MediaOutput1Port.value()));
        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.MediaOutput1IPAddress.value()));
        sendCommand(KLV_L3.KeyCategory.MediaEncoder1.value(), command);
    }

    public void enableCoT(final boolean state) {
        ArrayList<ShortKLV> command = new ArrayList<>();
        command.add(new ShortKLV(
                KLV_L3.TelemetryKLV.CoTTranslationOutputEnable.value(),
                KLV.LengthEncoding.BER,
                KLV.int8ToByteArray(state ? (byte) 1 : (byte) 0)));
        sendCommand(KLV_L3.KeyCategory.MetaData.value(), command);
    }

    // ---------------------------------------------------
    // Properties
    // --------------------------------------------------- 

    /**
     *  Given an array of ShortKLV and a Category, send a command.
     */

    synchronized public void sendCommand(int category,
            ArrayList<ShortKLV> commands) {
        sendCommand(constructKLV(category, commands));
    }

    /**
     * Send the command specified by the commandBytes over the interface addressed with
     * ifaceAddress.
     * if ifaceAddress is null, then the message is sent over the default interface
     */
    synchronized public void sendCommand(byte[] commandBytes) {
        if (DEBUG)
            log(TAG, "sending: " + Arrays.toString(bytesToHex(commandBytes)));

        // always delay before sending next command.
        try {
            Thread.sleep(500);
        } catch (Exception ignored) {
        }
        messageManager.send(commandBytes);
    }

    // lifted off the intertubes. -- for printing purposes only.
    private static String[] bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        String[] hexChars = new String[bytes.length];

        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j] = "0x" + hexArray[v >>> 4] + "" + hexArray[v & 0x0F];
        }
        return hexChars;
    }

    public void getTransmitterFrequency() {
        // get command bytes
        byte[] commandBytes = constructKLV(
                KLV_L3.KeyCategory.Transmitter2.value(),
                KLV_L3.KeyCategoryTransmitter.FrequencyValue.value(), null);

        // send
        sendCommand(commandBytes);
    }

    public void enableReceiver() {

        ArrayList<ShortKLV> command = new ArrayList<>();

        command.add(new ShortKLV(KLV_L3.KeyCategoryReceiver.Enable.value(),
                KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) 1)));

        sendCommand(KLV_L3.KeyCategory.Receiver1.value(), command);

    }

    public void enableTestVideo(boolean state) {
        ArrayList<ShortKLV> command = new ArrayList<>();

        command.add(new ShortKLV(KLV_L3.KeyCategorySystemTest.VideoTest.value(),
                KLV.LengthEncoding.BER,
                KLV.int8ToByteArray(state ? (byte) 1 : (byte) 0)));

        sendCommand(KLV_L3.KeyCategory.SystemTest.value(), command);
    }

    public void resetEncoder() {

        ArrayList<ShortKLV> command = new ArrayList<>();

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.VideoEncoderEnable.value(),
                KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) 0)));

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.VideoEncoderType.value(),
                KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) 0)));

        sendCommand(KLV_L3.KeyCategory.MediaEncoder1.value(), command);

    }

    public void setEncodingParam(final int bitrate, final FrameSize framesize,
            final VideoEncoder vet) {

        try {
            Thread.sleep(ROVER_COMMAND_SPACING);
        } catch (Exception ignored) {
        }

        resetEncoder();

        try {
            Thread.sleep(ROVER_COMMAND_SPACING);
        } catch (Exception ignored) {
        }

        ArrayList<ShortKLV> command = new ArrayList<>();

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.VideoInputStandard.value(),
                KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) 0)));

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.VideoInputConnector.value(),
                KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) 1)));

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.VideoTimestampSource.value(),
                KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) 0)));

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.VideoEncoderEnable.value(),
                KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) 1)));

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.VideoEncoderType.value(),
                KLV.LengthEncoding.BER,
                KLV.int8ToByteArray(vet.value())));

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.VideoEncoderResolution.value(),
                KLV.LengthEncoding.BER,
                KLV.int8ToByteArray(framesize.value())));

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.VideoEncoderIntraFrameInterval
                        .value(),
                KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) 30)));

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.VideoEncoderBitrate.value(),
                KLV.LengthEncoding.BER, KLV.int32ToByteArray(bitrate)));

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.VideoEncoderQuality.value(),
                KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) 100)));

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.VideoEncoderSkipFrames.value(),
                KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) 0)));

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.MediaOutput1IPAddress.value(),
                KLV.LengthEncoding.BER, new byte[] {
                        (byte) 239, (byte) 255, (byte) 0, (byte) 1
                }));

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryMediaEncoder.MediaOutput1Port.value(),
                KLV.LengthEncoding.BER, KLV.int16ToByteArray((short) 1841)));

        sendCommand(KLV_L3.KeyCategory.MediaEncoder1.value(), command);

    }

    public void setTransmitterFrequency(final int freq) {

        ArrayList<ShortKLV> command = new ArrayList<>();

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryTransmitter.FrequencyValue.value(),
                KLV.LengthEncoding.BER, KLV.int32ToByteArray(freq)));

        sendCommand(KLV_L3.KeyCategory.Transmitter2.value(), command);
    }

    /**
     * Issue a stop scanning command.
     */
    public void stopScanFrequency() {

        ArrayList<ShortKLV> command = new ArrayList<>();

        command.add(new ShortKLV(KLV_L3.KeyCategoryReceiver.SearchMode.value(),
                KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) 0)));

        sendCommand(KLV_L3.KeyCategory.Receiver1.value(), command);
    }

    public void disconnect() {

        ArrayList<ShortKLV> command = new ArrayList<>();

        command.add(
                new ShortKLV(KLV_L3.KeyCategoryReceiver.AutoReconnect.value(),
                        KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) 0)));

        sendCommand(KLV_L3.KeyCategory.Receiver1.value(), command);
    }

    public void setChannel(ModuleType m, int channel) {
        ArrayList<ShortKLV> command = new ArrayList<>();
        byte[] bval = new byte[4];
        bval[0] = (byte) (m.value() + 48);
        bval[1] = (byte) ((channel / 100) + 48);
        bval[2] = (byte) (((channel % 100) / 10) + 48);
        bval[3] = (byte) (((channel % 10)) + 48);

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryReceiver.DDLModuleChannel.value(),
                KLV.LengthEncoding.BER, bval));

        command.add(new ShortKLV(KLV_L3.KeyCategoryReceiver.Channel.value(),
                KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) channel)));
        command.add(new ShortKLV(KLV_L3.KeyCategoryReceiver.Module.value(),
                KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) m.value())));
        sendCommand(KLV_L3.KeyCategory.Receiver1.value(), command);
    }

    public void scanFrequency(final int startFrequency,
            final int endFrequency,
            final int stepSize, WaveformType type) {

        ArrayList<ShortKLV> command = new ArrayList<>();

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryReceiver.FrequencyValue.value(),
                KLV.LengthEncoding.BER, KLV.int32ToByteArray(startFrequency)));

        command.add(
                new ShortKLV(KLV_L3.KeyCategoryReceiver.AutoReconnect.value(),
                        KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) 1)));
        sendCommand(KLV_L3.KeyCategory.Receiver1.value(), command);

        if (type != WaveformType.NONE) {
            command.add(new ShortKLV(
                    KLV_L3.KeyCategoryReceiver.WaveformCategory.value(),
                    KLV.LengthEncoding.BER,
                    KLV.int8ToByteArray(type.value())));
        }

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryReceiver.SearchStartFrequency.value(),
                KLV.LengthEncoding.BER, KLV.int32ToByteArray(startFrequency)));

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryReceiver.SearchEndFrequency.value(),
                KLV.LengthEncoding.BER, KLV.int32ToByteArray(endFrequency)));

        command.add(new ShortKLV(
                KLV_L3.KeyCategoryReceiver.SearchStepSize.value(),
                KLV.LengthEncoding.BER, KLV.int32ToByteArray(stepSize)));

        command.add(
                new ShortKLV(KLV_L3.KeyCategoryReceiver.SearchDirection.value(),
                        KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) 1)));

        command.add(new ShortKLV(KLV_L3.KeyCategoryReceiver.SearchType.value(),
                KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) 2)));

        sendCommand(KLV_L3.KeyCategory.Receiver1.value(), command);

        command.clear();

        try {
            Thread.sleep(ROVER_COMMAND_SPACING);
        } catch (Exception ignored) {
        }

        command.add(new ShortKLV(KLV_L3.KeyCategoryReceiver.SearchMode.value(),
                KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) 3)));

        sendCommand(KLV_L3.KeyCategory.Receiver1.value(), command);
    }

    public void reaquire() {

        ArrayList<ShortKLV> command = new ArrayList<>();

        command.add(
                new ShortKLV(KLV_L3.KeyCategoryReceiver.AutoReconnect.value(),
                        KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) 2)));

        sendCommand(KLV_L3.KeyCategory.Receiver1.value(), command);

    }

    public void setReceiverFrequency(final int freq) {

        ArrayList<ShortKLV> command = new ArrayList<>();

        command.add(
                new ShortKLV(KLV_L3.KeyCategoryReceiver.AutoReconnect.value(),
                        KLV.LengthEncoding.BER, KLV.int8ToByteArray((byte) 1)));

        command.add(
                new ShortKLV(KLV_L3.KeyCategoryReceiver.FrequencyValue.value(),
                        KLV.LengthEncoding.BER, KLV.int32ToByteArray(freq)));

        sendCommand(KLV_L3.KeyCategory.Receiver1.value(), command);

        try {
            Thread.sleep(ROVER_COMMAND_SPACING);
        } catch (Exception ignored) {
        }

        reaquire();

    }

    private final MessageManager.DataListener dataListener = new MessageManager.DataListener() {

        @Override
        public void onReceiveData(byte[] data) {
            //if (DEBUG) log(TAG, "receiving: " + Arrays.toString(bytesToHex(data)));
            parseStatusKLV(data);
        }

        @Override
        public void onSendStatus(MessageManager.Status status) {
            if (mRadioListener != null) {
                mRadioListener.onMcoStatus(status);
            }
        }

        @Override
        public void onReceiveStatus(MessageManager.Status status) {
            if (mRadioListener != null) {
                mRadioListener.onMciStatus(status);
            }
        }

        @Override
        public void log(String tag, String message) {
            if (mRadioListener != null) {
                mRadioListener.log(tag, message);
            }
        }

    };

    // ---------------------------------------------------
    // Radio - Presets
    // ---------------------------------------------------

    public void getPresets(int offset, int count) {
        // bulk command
        ArrayList<ShortKLV> commandKeys = new ArrayList<>();

        // add presets for query
        int last = offset + count;
        for (int i = offset; i < last; i++) {
            commandKeys.add(new ShortKLV(
                    KLV_L3.KeyCategoryConfiguration.Configuration0.value()
                            + offset + i));
        }

        // get command bytes
        byte[] commandBytes = constructKLV(
                KLV_L3.KeyCategory.Configuration.value(), commandKeys);

        // send
        sendCommand(commandBytes);

    }

    public void getPresetName(int presetNum) {
        // command value
        int presetKey = (KLV_L3.KeyCategoryPresets.Preset0.value() + presetNum);

        // get command bytes
        byte[] commandBytes = constructKLV(KLV_L3.KeyCategory.Presets.value(),
                presetKey, null);

        // send
        sendCommand(commandBytes);
    }

    public void loadPreset(byte presetNum) {
        // command value
        byte[] commandValue = KLV.int8ToByteArray(presetNum);

        // get command bytes
        byte[] commandBytes = constructKLV(
                KLV_L3.KeyCategory.Configuration.value(),
                KLV_L3.KeyCategoryConfiguration.ConfigurationRestore.value(),
                commandValue);

        // send
        sendCommand(commandBytes);
    }

    // ---------------------------------------------------
    // Status Response - Presets (0x11)
    // ---------------------------------------------------

    protected void parseCategoryPresetsStatusKLV(List<KLV> categoryItems) {
        // return array
        List<String> al = new ArrayList<>();

        // clear existing
        presets.clear();

        // process items
        for (KLV categoryItem : categoryItems) {
            // get category sub item key
            int key = categoryItem.getShortKey();

            // get value
            if (categoryItem.getLength() > 1) {
                al.add(categoryItem.getValueAsString());
                presets.add(categoryItem.getValueAsString());
            }
        }

        // notify listener
        if (mRadioListener != null) {
            mRadioListener.onReceivePresets(al);
        }
    }

    /**
     * Based on a predetermined response message, parse the subcategory and provide information.
     */
    protected void parseCategoryStatus(int categoryKey,
            List<KLV> categoryItems) {

        if (categoryKey == KLV_L3.KeyCategory.Configuration.value()) {
            for (KLV categoryItem : categoryItems) {
                int key = categoryItem.getShortKey();
                if (key == KLV_L3.KeyCategoryConfiguration.SoftwareVersionID
                        .value()) {
                    parseDeviceVersion(categoryItem);
                    return;
                }
            }

            parseCategoryPresetsStatusKLV(categoryItems);
        } else if (categoryKey == KLV_L3.KeyCategory.Receiver1.value()) {
            parseReceiver(categoryItems);
        } else if (categoryKey == KLV_L3.KeyCategory.Receiver2.value()) {
            log(TAG, "receiver 2 data not supported");
        } else if (categoryKey == KLV_L3.KeyCategory.MediaEncoder1.value()) {
            parseMediaEncoder(categoryItems);
        } else {
            log(TAG, "Status key: " + categoryKey
                    + " response unsupported at this time");
        }

    }

    private void parseDeviceVersion(KLV categoryItem) {
        int ver = categoryItem.getValueAs32bitInt();

        int major = (0xFF000000 & ver) >> 24;
        int minor = (0x00FF0000 & ver) >> 16;
        int build = (0x0000FFFF & ver);
        try {
            receiver.version = Double.parseDouble(major + "." + minor);
        } catch (Exception ignored) {
        }
        receiver.build = build;
    }

    protected void parseMediaEncoder(List<KLV> categoryItems) {
        for (KLV categoryItem : categoryItems) {
            int key = categoryItem.getShortKey();
            if (key == KLV_L3.KeyCategoryMediaEncoder.VideoEncoderEnable
                    .value()) {
                if (DEBUG)
                    log(TAG, "media encoder: " + key + ":"
                            + categoryItem.getValueAs8bitUnsignedInt());
            } else if (key == KLV_L3.KeyCategoryMediaEncoder.VideoEncoderType
                    .value()) {
                if (DEBUG)
                    log(TAG, "media type: " + key + ":"
                            + categoryItem.getValueAs8bitUnsignedInt());
            } else if (key == KLV_L3.KeyCategoryMediaEncoder.VideoEncoderResolution
                    .value()) {
                if (DEBUG)
                    log(TAG, "media resolution: " + key + ":"
                            + categoryItem.getValueAs16bitUnsignedInt());
            } else if (key == KLV_L3.KeyCategoryMediaEncoder.VideoEncoderBitrate
                    .value()) {
                if (DEBUG)
                    log(TAG, "media bitrate: " + key + ":"
                            + categoryItem.getValueAs32bitInt());
            } else if (key == KLV_L3.KeyCategoryMediaEncoder.MediaOutput1Port
                    .value()) {
                if (DEBUG)
                    log(TAG, "media port: " + key + ":"
                            + categoryItem.getValueAs16bitUnsignedInt());
            } else if (key == KLV_L3.KeyCategoryMediaEncoder.MediaOutput1IPAddress
                    .value()) {
                if (DEBUG)
                    log(TAG, "media ip: " + key + ":" + Arrays.toString(
                            toUnsignedIntArray(categoryItem.getValue())));
            }
        }

    }

    static int[] toUnsignedIntArray(byte[] barray) {
        if (barray == null)
            return null;
        int[] ret = new int[barray.length];

        for (int i = 0; i < barray.length; ++i) {
            ret[i] = barray[i] & 0xFF;
        }
        return ret;
    }

    protected void parseReceiver(List<KLV> categoryItems) {
        // get receiver

        // process items
        for (KLV categoryItem : categoryItems) {
            // get category sub item key
            int key = categoryItem.getShortKey();

            // get value
            if (key == KLV_L3.KeyCategoryReceiver.FrequencyValue.value()) {
                // frequency
                receiver.frequency = categoryItem.getValueAs32bitInt();
            } else if (key == KLV_L3.KeyCategoryReceiver.DataRate.value()) {
                // data rate
                receiver.dataRate = categoryItem.getValueAs32bitInt();
            } else if (key == KLV_L3.KeyCategoryReceiver.SearchMode.value()) {
                receiver.searchMode = categoryItem.getValueAs8bitUnsignedInt();
            } else if (key == KLV_L3.KeyCategoryReceiver.SearchProgress
                    .value()) {
                receiver.searchProgress = categoryItem
                        .getValueAs8bitUnsignedInt();
            } else if (key == KLV_L3.KeyCategoryReceiver.SearchIterationsComplete
                    .value()) {
                receiver.searchIterationsComplete = categoryItem
                        .getValueAs8bitUnsignedInt();
            } else if (key == KLV_L3.KeyCategoryReceiver.SearchStateLock
                    .value()) {
                receiver.searchStateLock = categoryItem
                        .getValueAs8bitUnsignedInt();
            } else if (key == KLV_L3.KeyCategoryReceiver.Channel.value()) {
                receiver.channel = categoryItem.getValueAs8bitUnsignedInt();
            } else if (key == KLV_L3.KeyCategoryReceiver.Module.value()) {
                receiver.moduleType = ModuleType
                        .find(categoryItem.getValueAs8bitUnsignedInt());

            } else if (key == KLV_L3.KeyCategoryReceiver.LinkDemultiplexing
                    .value()) {
                receiver.network_framing = NetworkFramingType
                        .find(categoryItem.getValueAs8bitUnsignedInt());
            } else if (key == KLV_L3.KeyCategoryReceiver.RSSI.value()) {
                receiver.rssi = categoryItem.getValueAs8bitUnsignedInt();
            } else if (key == KLV_L3.KeyCategoryReceiver.LinkEstablishedStatus
                    .value()) {
                receiver.linkEstablished = categoryItem
                        .getValueAs8bitUnsignedInt();
            } else if (key == KLV_L3.KeyCategoryReceiver.DataLinkUtilization
                    .value()) {
                receiver.dataLinkUtilization = categoryItem
                        .getValueAs32bitInt();
            } else if (key == KLV_L3.KeyCategoryReceiver.AutoReconnect
                    .value()) {
                receiver.autoreconnect = categoryItem
                        .getValueAs8bitUnsignedInt();
            } else if (key == KLV_L3.KeyCategoryReceiver.WaveformCategory
                    .value()) {
                receiver.waveformCategory = WaveformType
                        .find(categoryItem.getValueAs8bitUnsignedInt());
            } else if (key == KLV_L3.KeyCategoryReceiver.DDLModuleChannel
                    .value()) {
                byte[] value = categoryItem.getValue();
                if (value.length == 4) {
                    // for the SIR 2.5, these values are returned as ASCII numbers
                    for (int i = 0; i < value.length; ++i)
                        value[i] = (byte) (value[i] - 48);

                    receiver.moduleType = ModuleType.find(value[0]);
                    receiver.channel = value[1] * 100 + value[2] * 10
                            + value[3];
                } else if (value.length == 5) {
                    // not sure exactly why the TRE is reporting a length of 5
                    // hit Scott Francom back
                    for (int i = 0; i < value.length; ++i)
                        value[i] = (byte) (value[i] - 48);
                    receiver.moduleType = ModuleType.find(value[0]);
                    receiver.channel = value[1] * 100 + value[2] * 10
                            + value[3];
                }

            } else {
                log(TAG, "received unhandled receiver key = " + key);
            }
        }

        // notify listener
        if (mRadioListener != null) {
            mRadioListener.onReceiverStatus(receiver);
        }
    }

    /**
     * Given a solicited status response, populate the appropriate callbacks.
     */
    private void parseStatusKLV(byte[] response) {
        // universal key
        byte[] universalKey = new byte[16];
        System.arraycopy(response, 0, universalKey, 0, 16);

        // create klv from response
        KLV klvResponse = new KLV(response, KeyLength.SixteenBytes,
                LengthEncoding.BER);

        // get sub klvs
        List<KLV> items = klvResponse.getSubKLVList(KeyLength.OneByte,
                LengthEncoding.BER);

        // process
        for (KLV item : items) {
            int key = item.getShortKey();

            if (key == KLV_L3.KEY_BASE_TYPE) {
            } else if (KeyCategory.isCategory(key)) {
                // get category sub klvs
                List<KLV> categoryItems = item.getSubKLVList(KeyLength.OneByte,
                        LengthEncoding.BER);

                // parse category status items
                parseCategoryStatus(key, categoryItems);
            }
        }
    }

    public KLV wrapKLV(KLV klvInner) {
        /**
        // create digest for md5
        MessageDigest digest = null;
        try {
            digest = java.security.MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "error occurred during digest creation", e);
            return null;
        }
        
        // get token MD5
        digest.update("L3tacrover".getBytes());
        byte messageDigest[] = digest.digest();
        
        // pad to 16 bytes
        byte[] tk = new byte[16];
        System.arraycopy(messageDigest, 0, tk, 16 - messageDigest.length, messageDigest.length);
        **/

        // create token klv
        KLV klvToken = new KLV(KLV_L3.KEY_BASE_TOKEN, KeyLength.OneByte,
                LengthEncoding.BER, new byte[] {
                        (byte) 0x81, (byte) 0x65, (byte) 0x75, (byte) 0x00,
                        (byte) 0xE8, (byte) 0x98, (byte) 0x93, (byte) 0x06,
                        (byte) 0xE6, (byte) 0x4E, (byte) 0xEC, (byte) 0x53,
                        (byte) 0x44, (byte) 0x8D, (byte) 0x3F, (byte) 0xF8
                });

        // universal key klv
        KLV universal = new KLV();
        universal.setKey(UNIVERSAL_SET);
        universal.setLengthEncoding(LengthEncoding.BER);

        // add time stamp klv
        // universal.addSubKLV(klvTimestamp);

        // add inner klv
        universal.addSubKLV(klvInner);

        // add token klv
        universal.addSubKLV(klvToken);

        // get current bytes and length
        byte[] currentBytes = universal.toBytes();
        int currentLength = currentBytes.length;

        // create byte array for checksum calculation 
        // (plus 2 bytes for checksum key and length bytes) and copy current bytes

        byte[] checksumCalc = new byte[currentLength + 2];
        System.arraycopy(currentBytes, 0, checksumCalc, 0, currentBytes.length);

        // add checksum key and length bytes
        checksumCalc[currentLength] = KLV_L3.KEY_BASE_CHECKSUM;
        checksumCalc[currentLength + 1] = 4;

        // adjust length value
        checksumCalc[16] += 6;

        // calculate checksum
        CRC32 crc = new CRC32();
        crc.update(checksumCalc);
        long check = crc.getValue();

        byte[] checksum = KLV.int32ToByteArray((int) check);

        // create checksum klv
        KLV klvChecksum = new KLV(KLV_L3.KEY_BASE_CHECKSUM,
                KeyLength.OneByte,
                LengthEncoding.BER, checksum);

        // add checksum klv
        universal.addSubKLV(klvChecksum);

        return universal;
    }

    /**
     * Builds the appropriate klv byte[] array to be sent to the SIR.
     * @param categoryKey is the one byte category key defined in the KLV_L3 class.
     * @param keys is the listing of keys.  Status requests (in the case where the length is BER and the 
     * value is null ) and command messages (where the length is BER and the value is non-null).
     */
    public byte[] constructKLV(int categoryKey, ArrayList<ShortKLV> keys) {
        // category
        KLV klvCategory = new KLV();
        klvCategory.setKeyLength(KeyLength.OneByte);
        klvCategory.setKey(categoryKey);
        klvCategory.setLengthEncoding(LengthEncoding.BER);

        // commands
        for (ShortKLV sklv : keys) {
            KLV klvCommand = new KLV(sklv.key, KeyLength.OneByte, sklv.len,
                    sklv.val);
            klvCategory.addSubKLV(klvCommand);
        }

        // wrap
        KLV klvFinal = wrapKLV(klvCategory);

        // send
        return klvFinal.toBytes();
    }

    public byte[] constructKLV(int categoryKey, int commandKey, byte[] value) {
        ShortKLV sklv = new ShortKLV(commandKey, LengthEncoding.BER, value);
        ArrayList<ShortKLV> keys = new ArrayList<>();
        keys.add(sklv);
        return constructKLV(categoryKey, keys);
    }

    public class RoverStatusRunnable implements Runnable {
        boolean cancelled = false;

        public void cancel() {
            cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void run() {
            byte[] command;

            while (!cancelled) {
                command = getFrequentQuery();
                sendCommand(command);
                try {
                    Thread.sleep(STATUS_COMMAND_SPACING);
                } catch (Exception ignored) {
                }
                command = getDeviceVersion();
                sendCommand(command);
                try {
                    Thread.sleep(STATUS_COMMAND_SPACING);
                } catch (Exception ignored) {
                }

            }
        }

    }

}
