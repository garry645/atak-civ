
package com.atakmap.android.radiolibrary.rover;

import java.util.List;

import java.util.ArrayList;

public class Vortex extends Radio {

    boolean mRx1Query;

    /**
     * Construct a Vortex radio.
     */
    public Vortex(final String nickname,
            final String radioAddress,
            final String mciInputAddress,
            final int mciInputPort,
            final String mciOutputAddress,
            final int mciOutputPort) {

        super(nickname, radioAddress,
                mciInputAddress, mciInputPort,
                mciOutputAddress, mciOutputPort);

    }

    protected byte[] getFrequentQuery() {
        // bulk command
        ArrayList<ShortKLV> commandKeys = new ArrayList<>();
        commandKeys.add(new ShortKLV(
                KLV_L3.KeyCategoryReceiver.FrequencyValue.value()));
        commandKeys.add(new ShortKLV(KLV_L3.KeyCategoryReceiver.RSSI.value()));
        commandKeys.add(new ShortKLV(
                KLV_L3.KeyCategoryReceiver.LinkEstablishedStatus.value()));
        commandKeys.add(new ShortKLV(
                KLV_L3.KeyCategoryReceiver.DataLinkUtilization.value()));
        commandKeys
                .add(new ShortKLV(KLV_L3.KeyCategoryReceiver.DataRate.value()));

        // flip
        mRx1Query = !mRx1Query;

        // return
        if (mRx1Query) {
            return constructKLV(KLV_L3.KeyCategory.Receiver1.value(),
                    commandKeys);
        } else {
            return constructKLV(KLV_L3.KeyCategory.Receiver2.value(),
                    commandKeys);
        }
    }

    @Override
    public void setReceiverFrequency(int frequency) {
        // frequency
        byte[] commandValue = KLV.int32ToByteArray(frequency);

        // get command bytes
        byte[] commandBytes;
        commandBytes = constructKLV(KLV_L3.KeyCategory.Receiver1.value(),
                KLV_L3.KeyCategoryReceiver.FrequencyValue.value(),
                commandValue);

        // send
        sendCommand(commandBytes);
    }

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
                    (KLV_L3.KeyCategoryConfiguration.Configuration0.value()
                            + offset + i)));
        }

        // get command bytes
        byte[] commandBytes = constructKLV(
                KLV_L3.KeyCategory.Configuration.value(), commandKeys);

        // send
        sendCommand(commandBytes);

    }

    public void getPresetName(int presetNum) {
        // command value
        byte presetKey = (byte) (KLV_L3.KeyCategoryPresets.Preset0.value()
                + presetNum);

        // get command bytes
        byte[] commandBytes = constructKLV(KLV_L3.KeyCategory.Presets.value(),
                presetKey, null);

        // send
        sendCommand(commandBytes);
    }

    public void loadPreset(int presetNum) {
        // command value
        byte[] commandValue = KLV.int8ToByteArray((byte) presetNum);

        // get command bytes
        byte[] commandBytes = constructKLV(
                KLV_L3.KeyCategory.Configuration.value(),
                KLV_L3.KeyCategoryConfiguration.ConfigurationRestore.value(),
                commandValue);

        // send
        sendCommand(commandBytes);
    }

    // ---------------------------------------------------
    // Status Response
    // ---------------------------------------------------

    protected void parseCategoryStatusKLV(int categoryKey,
            List<KLV> categoryItems) {
        // parse for category
        if (categoryKey == KLV_L3.KeyCategory.Configuration.value()) {
            // presets
            parseCategoryPresetsStatusKLV(categoryItems);
        } else if (categoryKey == KLV_L3.KeyCategory.Receiver1.value()
                || categoryKey == KLV_L3.KeyCategory.Receiver2.value()) {
            // receiver
            parseCategoryStatus(categoryKey, categoryItems);
        }
    }
}
