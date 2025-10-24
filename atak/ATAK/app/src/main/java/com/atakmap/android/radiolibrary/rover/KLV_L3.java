
package com.atakmap.android.radiolibrary.rover;

import androidx.annotation.NonNull;

public class KLV_L3 {

    /* ********  C O N S T A N T S  -  B A S E   K E Y S  ******** */

    public final static byte KEY_BASE_CHECKSUM = (byte) 0xC0;
    public final static byte KEY_BASE_TIMESTAMP = (byte) 0xC1;
    public final static byte KEY_BASE_TYPE = (byte) 0xC2;
    public final static byte KEY_BASE_SOURCE = (byte) 0xC3;
    public final static byte KEY_BASE_TOKEN = (byte) 0xC4;

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y   ******** */

    public enum KeyCategory {
        Configuration(0x10),
        Presets(0x11),
        EFCD(0x18),
        InterfaceIPNetwork(0x20),
        InterfaceAsyncSerial(0x21),
        InterfaceSyncSerial(0x22),
        Transmitter1(0x30),
        Transmitter2(0x31), // originally Transmitter
        Receiver1(0x38),
        Receiver2(0x39),
        MediaEncoder1(0x40),
        MediaDecoder1(0x48),
        NetworkRouting(0x50),
        NetworkMulticastRouting(0x51),
        NetworkDynamicRouting(0x52),
        NetworkMACFiltering(0x53),
        LegacyPacketMux(0x58),
        LegacyBit(0x60),
        PowerOnTest(0x61),
        ContinuousTest(0x64),
        InitiatedTest(0x67),
        AsyncSerialIngressForwarding(0x70),
        AsyncSerialEgressForwarding(0x71),
        SyncSerialInressForwarding(0x72),
        SyncSerialEgressForwarding(0x73),
        TCPIngressForwarding(0x74),
        COMSEC(0x78),
        CSICommand(0x80),
        CSIStatus(0x88),
        SystemTest(0x90),
        MetaData(0xA0),
        Navigation(0xA8);

        private final int value;

        /** 
         * Sets the Category key value
         */
        KeyCategory(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the category key
         */
        public int value() {
            return this.value;
        }

        public static boolean isCategory(int value) {
            for (KeyCategory wc : KeyCategory.values()) {
                if (wc.value == (byte) value) {
                    return true;
                }
            }
            return false;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  C O N F I G U R A T I O N  (0x10)   ******** */

    public enum KeyCategoryConfiguration {

        RestoreFactoryDefaults(0x10), // cmd
        SoftwareVersionID(0x21), // sts
        SystemPartNumber(0x24), // sts
        SystemDashNumber(0x25), // sts
        SystemRevisionNumber(0x26), // sts
        SystemName(0x30), // cmd, sts
        UnitSerialNumber(0x31), // sts
        Configuration0(0x40), // cmd, sts
        Configuration31(0x5F), // cmd, sts
        ConfigurationRestore(0x60), // cmd
        AirSurface(0x80), // sts
        NetworkingMode(0x81), // cmd, sts
        FullNetworkingModeType(0x82); // cmd, sts

        private final int value;

        /** 
         * Sets the Category Configuration cmd/sts key value
         */
        KeyCategoryConfiguration(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Configuration cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  P R E S E T S  (0x11)   ******** */

    public enum KeyCategoryPresets {
        RestorePreset(0x01), // cmd
        Preset0(0x10), // sts
        Preset95(0x6F); // sts

        private final int value;

        /** 
         * Sets the Category Presets cmd/sts key value
         */
        KeyCategoryPresets(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Presets cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  E F C D  (0x18)   ******** */

    public enum KeyCategoryEFCD {
        EFCD1Enable(0x01), // cmd, sts
        EFCDActiveLink(0x02), // cmd, sts
        RemoteNavigationSource(0x10), // cmd, sts
        RealTimeCoordinatesEnable(0x11), // cmd, sts    
        EFCFLCoordinatePNCodeChange(0x18), // cmd, sts
        EFCFLCoordinatedFrequencyChange(0x19), // cmd, sts
        EFCRLCoordinatedFrequencyChange(0x1A), // cmd, sts
        EFCRLModulationChangeRequest(0x1B), // cmd, sts
        EFCRLAttenuationChangeRequest(0x1C), // cmd, sts
        GroundCoordinate1Latitude(0x20), // cmd, sts
        GroundCoordinate1Longitude(0x21), // cmd, sts
        GroundCoordinate1Altitude(0x22), // cmd, sts
        GroundCoordinate2Latitude(0x24), // cmd, sts
        GroundCoordinate2Longitude(0x25), // cmd, sts
        GroundCoordinate2Altitude(0x26), // cmd, sts
        GroundCoordinate3Latitude(0x28), // cmd, sts
        GroundCoordinate3Longitude(0x29), // cmd, sts
        GroundCoordinate3Altitude(0x2A), // cmd, sts
        GroundCoordinate4Latitude(0x2C), // cmd, sts
        GroundCoordinate4Longitude(0x2D), // cmd, sts
        GroundCoordinate4Altitude(0x2E), // cmd, sts
        GroundCoordinate5Latitude(0x30), // cmd, sts
        GroundCoordinate5Longitude(0x31), // cmd, sts
        GroundCoordinate5Altitude(0x32), // cmd, sts
        GroundCoordinate6Latitude(0x34), // cmd, sts
        GroundCoordinate6Longitude(0x35), // cmd, sts
        GroundCoordinate6Altitude(0x36), // cmd, sts
        GroundStation1Latitude(0x40), // cmd, sts
        GroundStation1Longitude(0x41), // cmd, sts
        GroundStation1Altitude(0x42), // cmd, sts
        GroundStation2Latitude(0x44), // cmd, sts
        GroundStation2Longitude(0x45), // cmd, sts
        GroundStation2Altitude(0x46), // cmd, sts
        GroundStation3Latitude(0x48), // cmd, sts
        GroundStation3Longitude(0x49), // cmd, sts
        GroundStation3Altitude(0x4A), // cmd, sts
        GroundStation4Latitude(0x4C), // cmd, sts
        GroundStation4Longitude(0x4D), // cmd, sts
        GroundStation4Altitude(0x4E), // cmd, sts
        GroundStation5Latitude(0x50), // cmd, sts
        GroundStation5Longitude(0x51), // cmd, sts
        GroundStation5Altitude(0x52), // cmd, sts
        RealTimeGroundStation6Latitude(0x54), // cmd, sts
        RealTimeGroundStation6Longitude(0x55), // cmd, sts
        RealTimeGroundStation6Altitude(0x56), // cmd, sts
        PCEProcessorTemp(0x60), // sts
        PCERemoteFLPNCode(0x61), // sts
        PCEFLFreq(0x62), // sts
        PCETransmitPowerAmpTemp(0x63), // sts
        PCERLAttenuation(0x64), // sts
        PCERLFreq(0x65), // sts
        PCEFLViterbiErrorCount(0x66), // sts
        EFDDownDiscrete(0x67), // sts
        PCEFLPMECSync(0x68), // sts
        PCEFLEFCSync(0x69), // sts
        PCEAirAutoMode(0x6A), // sts
        PCEAirManualMode(0x6B), // sts
        PCEGroundManualMode(0x6C), // sts
        PCEActiveGroundStation(0x6D), // sts
        PCEFLRandomization(0x6E), // sts
        PCEFLPNState(0x6F), // sts
        PCEFLSpread(0x70), // sts
        EFCUp(0x71), // sts
        SCERLLinkQuality(0x72), // sts
        SCERLLinkLock(0x73), // sts
        SCERLSync(0x74), // sts
        SCERangeToPCE(0xA9), // sts
        SCECalibratedRangeToPCE(0xAA), // cmd, sts
        SCERangeState(0xAB), // sts
        SCERangeEFCEpoch(0xAC), // sts
        SCERangeDelayMinimum(0xAD), // sts
        SCERangeDelayUncalibrated(0xAE), // sts
        SCERangeDelayCalibrated(0xAF), // sts
        SCERangeDelayTotal(0xB0); // sts

        private final int value;

        /** 
         * Sets the Category Transmitter cmd/sts key value
         */
        KeyCategoryEFCD(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Transmitter cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  I P   N E T W O R K  (0x20)   ******** */

    public enum KeyCategoryIPNetwork {
        IPNetwork1IPVersion(0x01), // cmd, sts
        IPNetwork1IPAddress(0x02), // cmd, sts
        IPNetwork1Netmask(0x10), // cmd, sts
        IPNetwork1DHCP(0x11), // cmd, sts    
        RoutingLayerSelect(0x18), // cmd, sts
        IPNetwork1DynamicRouting(0x19), // cmd, sts
        IPNetwork2IPVersion(0x1A), // cmd, sts
        IPNetwork2EthernetIPAddress(0x1B), // cmd, sts
        IPNetwork2EthernetNetmask(0x1C), // cmd, sts
        IPNetwork2EthernetDHCP(0x20), // cmd, sts
        IPNetwork2EthernetTrafficForwardDisable(0x21), // cmd, sts
        IPNetwork2NetworkTrafficPriority(0x22), // cmd, sts
        IPNetwork3IPVersion(0x24), // cmd, sts
        IPNetworkEthernetIPAddress(0x25), // cmd, sts
        IPNetworkEthernetNetmask(0x26), // cmd, sts
        IPNetworkPriority0Bandwidth(0x28), // cmd, sts
        IPNetworkPriority1Bandwidth(0x29), // cmd, sts
        IPNetworkPriority2Bandwidth(0x2A), // cmd, sts
        IPNetworkPriority3Bandwidth(0x2C), // cmd, sts
        IPNetworkPriority4Bandwidth(0x2D), // cmd, sts
        IPNetworkPriority5Bandwidth(0x2E), // cmd, sts
        IPNetworkPriority6Bandwidth(0x30), // cmd, sts
        IPNetworkPriority7Bandwidth(0x31); // cmd, sts

        private final int value;

        /** 
         * Sets the Category Transmitter cmd/sts key value
         */
        KeyCategoryIPNetwork(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Transmitter cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  A S Y N C   S E R I A L  (0x21)   ******** */

    public enum KeyCategoryAsyncSerial {
        AsyncSerial2TxRedRS232GPSEnable(0x20), // cmd, sts
        AsyncSerial2TxRedRS232GPSBaudRate(0x22), // cmd, sts
        AsyncSerial3TxRedRS232Port1Enable(0x30), // cmd, sts
        AsyncSerial3TxRedRS232Port1BaudRate(0x32), // cmd, sts    
        AsyncSerial3InterfaceStandard(0x38), // cmd, sts
        TxRedRS422Serial5Port1Enable(0x50), // cmd, sts
        TxRedRS422Serial5Port1BaudRate(0x52), // cmd, sts
        TxBlackRS485AntennaPort1Enable(0x70), // cmd, sts
        TxBlackRS485AntennaPort1BaudRate(0x72), // cmd, sts
        RxBlackRS232GPSEnable(0xA0), // cmd, sts
        RxBlackRS232GPSBaudRate(0xA2), // cmd, sts
        AsyncSerial10InterfaceStandard(0xA8), // cmd, sts
        TxBlackRS485AntennaPort2Enable(0xB0), // cmd, sts
        TxBlackRS485AntennaPort2BaudRate(0xB2); // cmd, sts

        private final int value;

        /** 
         * Sets the Category Transmitter cmd/sts key value
         */
        KeyCategoryAsyncSerial(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Transmitter cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  T R A N S M I T T E R  (0x31)   ******** */

    public enum KeyCategoryTransmitter {
        Enable(0x01), // cmd, sts
        FrequencyValue(0x10), // cmd, sts
        DataRate(0x20), // cmd, sts
        WaveformCategory(0x21), // cmd, sts    
        LinkDemultiplexing(0x22), // cmd, sts
        SpreadSpectrum(0x23), // cmd, sts
        Modulation(0x24), // cmd, sts
        FEC(0x25), // cmd, sts
        Interleaver(0x26), // cmd, sts
        ChannelMux(0x27), // cmd, sts
        TelemetrySubchannelBaudRate(0x30), // cmd, sts
        TelemetrySubchannelEncoding(0x31), // cmd, sts
        TelemetrySubchannelFrequencyOffset(0x33), // cmd, sts
        WeightOnWheelsStatus(0x42), // sts
        TransmitPower(0x80), // cmd, sts
        RFPortSelect(0x81), // cmd, sts
        TransmitPowerOnPowerUp(0x82), // cmd, sts
        TransmitPAPowerOutMonitor(0x83), // sts
        TransmitEnableOnPowerUp(0x84), // cmd, sts
        TxBlanking(0x87), // cmd, sts
        ModemTemperature(0x90); // sts

        private final int value;

        /** 
         * Sets the Category Transmitter cmd/sts key value
         */
        KeyCategoryTransmitter(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Transmitter cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  R E C E I V E R   1 / 2  (0x38, 0x39)   ******** */

    public enum KeyCategoryReceiver {
        Enable(0x01), // cmd, sts
        FrequencyValue(0x10), // cmd, sts
        AutoReconnect(0x11), // cmd, sts
        Channel(0x15), // cmd, sts
        Module(0x16), // cmd, sts
        DataRate(0x20), // cmd, sts
        WaveformCategory(0x21), // cmd, sts
        LinkDemultiplexing(0x22), // cmd, sts
        SpreadSpectrum(0x23), // cmd, sts
        Modulation(0x24), // cmd, sts
        FEC(0x25), // cmd, sts
        Deinterleaver(0x26), // cmd, sts
        ChannelDemux(0x27), // cmd, sts
        TelemetrySubchannelBaudRate(0x30), // cmd, sts
        TelemetrySubchannelEncoding(0x31), // cmd, sts
        TelemetrySubchannelFrequencyDeviation(0x32), // cmd, sts
        TelemetrySubchannelFrequencyOffset(0x33), // cmd, sts
        InvertSpectrum(0x34), // cmd, sts
        LNAVoltage(0x40), // cmd, sts
        LNAVoltageAtStartup(0x41), // cmd, sts
        FrequencySearchMode(0x50), // cmd, sts
        FrequencySearchArea(0x51), // cmd, sts
        FrequencySearchPercentComplete(0x52), // sts
        FrequencySearchSubSecStatus(0x53), // sts
        SearchMode(0x60), // cmd, sts
        SearchStartFrequency(0x61), // cmd, sts
        SearchEndFrequency(0x62), // cmd, sts
        SearchStepSize(0x63), // cmd, sts
        SearchDirection(0x64), // cmd, sts
        SearchType(0x65), // cmd, sts
        SearchProgress(0x67), // sts
        SearchIterationsComplete(0x68), // sts
        SearchStateLock(0x6B), // sts
        RSSI(0x80), // sts
        LinkEstablishedStatus(0x81), // sts
        CDLLinkStatusDetails(0x83), // sts
        DataLinkUtilization(0x84), // sts
        RFPortSelect(0x85), // cmd, sts
        EqualizerEnable(0x86), // cmd, sts
        DDLModuleChannel(0xB0); // cmd, sts

        private final int value;

        /** 
         * Sets the Category Receiver 1 or 2 cmd/sts key value
         */
        KeyCategoryReceiver(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Receiver 1 or 2 cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  M E D I A   E N C O D E R  (0x40)   ******** */

    public enum KeyCategoryMediaEncoder {
        VideoInputStandard(0x01), // cmd, sts
        VideoInputConnector(0x02), // cmd, sts
        VideoMonochromeMode(0x03), // cmd, sts
        VideoCroppingMode(0x04), // cmd, sts
        VideoDeinterlacingMode(0x05), // cmd, sts
        VideoColorBarMode(0x06), // cmd, sts
        VideoBrightness(0x07), // cmd, sts
        VideoContrast(0x08), // cmd, sts
        VideoColorSaturation(0x09), // cmd, sts
        VideoTintHue(0x0A), // sts
        VideoInActivity(0x0B), // sts
        VideoInputSampleMode(0x0C), // cmd, sts
        VideoTimestampSource(0x0D), // cmd, sts
        VideoEncoderEnable(0x40), // cmd, sts
        VideoEncoderType(0x41), // cmd, sts
        VideoEncoderStandard(0x42), // cmd, sts
        VideoEncoderResolution(0x43), // cmd, sts
        VideoEncoderBitrate(0x44), // cmd, sts
        VideoEncoderFramerate(0x45), // cmd, sts
        VideoEncoderIntraFrameInterval(0x46), // cmd, sts
        VideoEncoderQuality(0x47), // cmd, sts
        VideoEncoderSkipFrames(0x48), // cmd, sts
        VideoQualitySettings(0x50), // cmd, sts
        MediaOutput1InterfaceType(0xC1), // cmd, sts
        MediaOutput1IPAddress(0xC3), // cmd, sts
        MediaOutput1Port(0xC4), // cmd, sts
        MediaOutput1Priority(0xC5), // cmd, sts
        MediaOutput2IPAddress(0xD3), // cmd, sts
        MediaOutput2Port(0xD4), // cmd, sts
        MediaOutput2Priority(0xD5); // cmd, sts

        private final int value;

        /** 
         * Sets the Category Media Encoder cmd/sts key value
         */
        KeyCategoryMediaEncoder(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Media Encoder cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  M E D I A   D E C O D E R  (0x48)   ******** */

    public enum KeyCategoryMediaDecoder {
        VideoOutputStandard(0x01), // cmd, sts
        VideoColorBarMode(0x03), // cmd, sts
        VideoBrightness(0x04), // cmd, sts
        VideoContrast(0x05), // cmd, sts
        VideoColorSaturation(0x06), // cmd, sts
        VideoTintHue(0x07), // cmd, sts
        VideoOutputActivity(0x08), // sts
        VideoOutputOSDEnable(0x10), // cmd, sts
        VideoDecoderEnable(0x40), // cmd, sts
        VideoDecoderType(0x41), // sts
        VideoDecoderResolution(0x43), // sts
        VideoDecoderBitrate(0x44), // sts
        VideoDecoderFramerate(0x45), // sts
        MediaInput1InterfaceType(0x81), // cmd, sts
        MediaInput1IPAddress(0x83), // cmd, sts
        MediaInput1Port(0x84), // cmd, sts
        MediaInput2IPAddress(0x93), // cmd, sts
        MediaInput2Port(0x94); // cmd, sts

        private final int value;

        /** 
         * Sets the Category Media Encoder cmd/sts key value
         */
        KeyCategoryMediaDecoder(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Media Encoder cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  N E T W O R K   R O U T I N G  (0x50)   ******** */

    public enum KeyCategoryNetworkRouting {
        RouteCount(0x01), // sts
        DefaultRoute(0x02), // sts
        IPv6DefaultRoute(0x03), // sts
        AddRemoveRouteIPAddress(0x04), // sts
        AddRemoveRouteNetmask(0x05), // sts
        AddRemoveRouteGateway(0x06), // sts
        AddRemoveRoute(0x08), // sts
        Route1IPAddress(0x10), // sts
        Route1Netmask(0x11), // sts
        Route1GatewayS(0x12), // sts
        Route60IPAddress(0xFC), // sts
        Route60Netmask(0xFD), // sts
        Route60Gateway(0xFE); // sts

        private final int value;

        /** 
         * Sets the Category Media Encoder cmd/sts key value
         */
        KeyCategoryNetworkRouting(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Media Encoder cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  N E T W O R K   M U L T I C A S T   R O U T I N G  (0x51)   ******** */

    public enum KeyCategoryNetworkMulticastRouting {
        RouteCount(0x01), // sts
        AddRemoveRouteIPAddress(0x04), // cmd, sts
        AddRemoveRouteIPNetworkInterfaces(0x06), // cmd, sts
        MulticastImportPort(0x07), // cmd, sts
        AddRemoveRoute(0x08), // cmd
        Route1IPAddress(0x10), // sts
        Route1IPNetworkInterfaces(0x12), // sts
        Route1MulticastInputPort(0x13), // sts
        Route60IPAddress(0xFC), // sts
        Route60IPNetworkInterfaces(0xFE), // sts
        Route60MulticastInputPort(0xFF); // sts

        private final int value;

        /** 
         * Sets the Category Media Encoder cmd/sts key value
         */
        KeyCategoryNetworkMulticastRouting(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Media Encoder cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  N E T W O R K   D Y N A M I C   R O U T I N G  (0x52)   ******** */

    public enum KeyCategoryNetworkDynamicRouting {
        DynamicRouteCount(0x01), // sts
        DynamicRoute1IPAddress(0x10), // sts
        DynamicRoute1Netmask(0x11), // sts
        DynamicRoute1Gateway(0x12), // sts
        DynamicRoute1Protocol(0x14), // sts
        DynamicRoute60IPAddress(0xFC), // sts
        DynamicRoute60Netmask(0xFD), // sts
        DynamicRoute60Gateway(0xFE), // sts
        DynamicRoute60Protocol(0xFF); // sts

        private final int value;

        /** 
         * Sets the Category Media Encoder cmd/sts key value
         */
        KeyCategoryNetworkDynamicRouting(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Media Encoder cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  L E G A C Y   P A C K E T   M U X  (0x58)   ******** */

    public enum KeyCategoryLegacyPacketMux {
        IPNetworkInterfaceID(0x01), // cmd, sts
        IPMulticastAddress(0x10); // cmd, sts

        private final int value;

        /** 
         * Sets the Category Media Encoder cmd/sts key value
         */
        KeyCategoryLegacyPacketMux(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Media Encoder cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  L E G A C Y   B U I L T   I N   T E S T  (0x60)   ******** */

    public enum KeyCategoryLegacyBuiltInTest {
        BITCommand(0x10), // sts
        FPGA1ComTest(0x30), // sts
        FPGA16ComTest(0x3F), // sts
        RF1MicrocontrollerComTest(0x40), // sts
        RF16MicrocontrollerComTest(0x4F), // sts
        ModemTempTest(0x50), // sts
        ModemRFLO1PLLLockedTest(0x60), // sts
        ModemRFLO2PLLLockedTest(0x61), // sts
        PATempTest(0x70), // sts
        PATxPowerTest(0x71), // sts
        RFEComTest(0x74); // sts

        private final int value;

        /** 
         * Sets the Category Media Encoder cmd/sts key value
         */
        KeyCategoryLegacyBuiltInTest(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Media Encoder cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  P O W E R   O N   B I T   S T A T U S  (0x61)   ******** */

    public enum KeyCategoryPowerOnBitStatus {
        PBITStatus(0x01), // sts
        MicroprocessorVolatileMemoryTest(0x20), // sts
        MicroprocessorNonVolatileMemoryTest(0x21), // sts
        FPGA1ComTest(0x30), // sts
        FPGA2ComTest(0x31), // sts
        VideoDecoderComTest(0x38), // sts
        VideoEncoderComTest(0x39), // sts
        RF1MicrocontrollerComTest(0x40), // sts
        RF2MicrocontrollerComTest(0x41), // sts
        RF3MicrocontrollerComTest(0x42), // sts
        RF4MicrocontrollerComTest(0x43), // sts
        RFCalibrationFileTest(0x55); // sts

        private final int value;

        /** 
         * Sets the Category Media Encoder cmd/sts key value
         */
        KeyCategoryPowerOnBitStatus(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Media Encoder cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  P O W E R   O N   B I T   S T A T U S  (0x64)   ******** */

    public enum KeyCategoryContinuousBitStatus {
        CBITStatus(0x01), // sts
        FPGA1ComTest(0x30), // sts
        FPGA2ComTest(0x31), // sts
        VideoDecoderComTest(0x38), // sts
        VideoEncoderComTest(0x39), // sts
        RF1MicrocontrollerComTest(0x40), // sts
        RF2MicrocontrollerComTest(0x41), // sts
        RF3MicrocontrollerComTest(0x42), // sts
        RF4MicrocontrollerComTest(0x43), // sts
        ModemTempTest(0x50), // sts
        RFCalibrationFileTest(0x55), // sts
        ModemRFTransmit1PLLLockedTest(0x60), // sts
        ModemRFReceive1PLLLockedTest(0x61), // sts
        ModemRFTransmit2PLLLockedTest(0x62), // sts
        ModemRFReceive2PLLLockedTest(0x63); // sts

        private final int value;

        /** 
         * Sets the Category Media Encoder cmd/sts key value
         */
        KeyCategoryContinuousBitStatus(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Media Encoder cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  I N I T I A T E D   B I T   S T A T U S  (0x67)   ******** */

    public enum KeyCategoryInitiatedBitStatus {
        IBITStatus(0x01), // sts
        InitiateBitCommand(0x02), // cmd
        MicroprocessorVolatileMemoryTest(0x20), // sts
        MicroprocessorNonVolatileMemoryTest(0x21), // sts
        FPGA1ComTest(0x30), // sts
        FPGA2ComTest(0x31), // sts
        VideoDecoderComTest(0x38), // sts
        VideoEncoderComTest(0x39), // sts
        RF1MicrocontrollerComTest(0x40), // sts
        RF2MicrocontrollerComTest(0x41), // sts
        RF3MicrocontrollerComTest(0x42), // sts
        RF4MicrocontrollerComTest(0x43), // sts
        ModemTempTest(0x50), // sts
        ModemTxPowerTest(0x51), // sts
        ModemDigitalLoopbackTest(0x54), // sts
        RFCalibrationFileTest(0x55), // sts
        ModemRFTransmit1PLLLockedTest(0x60), // sts
        ModemRFReceive1PLLLockedTest(0x61), // sts
        ModemRFTransmit2PLLLockedTest(0x62), // sts
        ModemRFReceive3PLLLockedTest(0x63); // sts

        private final int value;

        /** 
         * Sets the Category Media Encoder cmd/sts key value
         */
        KeyCategoryInitiatedBitStatus(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Media Encoder cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  A S Y N C   S E R I A L   I N G R E S S   F O R W A R D I N G  (0x70)   ******** */

    public enum KeyCategoryAsyncSerialIngressForwarding {
        AsyncSerial1InputIPAddress(0x13), // cmd, sts
        AsyncSerial1InputPort(0x14), // cmd, sts
        AsyncSerial1InputPriority(0x15), // sts
        AsyncSerial2InputIPAddress(0x23), // cmd, sts
        AsyncSerial2InputPort(0x24), // cmd, sts
        AsyncSerial2InputPriority(0x25), // sts
        AsyncSerial3nputIPAddress(0x33), // cmd, sts
        AsyncSerial3InputPort(0x34), // cmd, sts
        AsyncSerial3InputPriority(0x35), // sts
        AsyncSerial4InputIPAddress(0x43), // cmd, sts
        AsyncSerial4InputPort(0x44), // cmd, sts
        AsyncSerial4InputPriority(0x45), // sts
        AsyncSerial5InputPriority(0x55), // sts
        AsyncSerial6InputPriority(0x65), // sts
        AsyncSerial7InputPriority(0x75), // sts
        AsyncSerial8InputPriority(0x85), // sts
        AsyncSerial9InputPriority(0x95), // sts
        AsyncSerial10InputPriority(0xA5), // sts
        AsyncSerial11InputPriority(0xB5), // sts
        AsyncSerial12InputPriority(0xC5), // sts
        AsyncSerial13InputPriority(0xD5), // sts
        AsyncSerial14InputPriority(0xE5), // sts
        AsyncSerial15InputPriority(0xF5); // sts

        private final int value;

        /** 
         * Sets the Category Media Encoder cmd/sts key value
         */
        KeyCategoryAsyncSerialIngressForwarding(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Media Encoder cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  C O M S E C  (0x78)   ******** */

    public enum KeyCategoryComsec {
        TxEncryptionType(0x01), // cmd, sts
        Type1Installed(0x10), // sts
        CodePlugInstalled(0x11), // sts
        Type1Device(0x20), // sts
        Type1KeySelect(0x21), // cmd, sts
        Type1KeyAlgorithmSelect(0x22), // cmd, sts
        Type1ModeSelect(0x23), // cmd, sts
        Type1RunKeyingEnable(0x24), // cmd, sts
        Type1Bypass(0x30), // sts
        Type1Zeroize(0x40), // sts
        Type1KeyStatus(0x50), // sts
        Type1Operational(0x60), // sts
        DESActiveKey(0x80), // cmd, sts
        DESZeroize(0x82), // cmd
        DESKeysState(0x83), // sts
        DESTxOperational(0x84), // sts
        DESRxOperational(0x85), // sts
        DESKey1(0x90), // cmd
        DESKey2(0x91), // cmd
        DESKey3(0x92), // cmd
        DESKey4(0x93), // cmd
        DESKey5(0x94), // cmd
        DESKey6(0x95), // cmd
        DESKey7(0x96), // cmd
        DESKey8(0x97), // cmd
        DESKey9(0x98), // cmd
        DESKey10(0x99), // cmd
        DESKey11(0x9A), // cmd
        DESKey12(0x9B), // cmd
        DESKey13(0x9C), // cmd
        DESKey14(0x9D), // cmd
        DESKey15(0x9E), // cmd
        DESKey16(0x9F), // cmd
        DESKeyName1(0xD0), // cmd, sts
        DESKeyName2(0xD1), // cmd, sts
        DESKeyName3(0xD2), // cmd, sts
        DESKeyName4(0xD3), // cmd, sts
        DESKeyName5(0xD4), // cmd, sts
        DESKeyName6(0xD5), // cmd, sts
        DESKeyName7(0xD6), // cmd, sts
        DESKeyName8(0xD7), // cmd, sts
        DESKeyName9(0xD8), // cmd, sts
        DESKeyName10(0xD9), // cmd, sts
        DESKeyName11(0xDA), // cmd, sts
        DESKeyName12(0xDB), // cmd, sts
        DESKeyName13(0xDC), // cmd, sts
        DESKeyName14(0xDD), // cmd, sts
        DESKeyName15(0xDE), // cmd, sts
        DESKeyName16(0xDF), // cmd, sts
        AESActiveKey(0xA0), // cmd, sts
        AESZeroize(0xA2), // cmd
        AESKeyState(0xA3), // sts
        AESTxOperational(0xA4), // sts
        AESRxOperational(0xA5), // sts
        AESKey1(0xB0), // cmd
        AESKey2(0xB1), // cmd
        AESKey3(0xB2), // cmd
        AESKey4(0xB3), // cmd
        AESKey5(0xB4), // cmd
        AESKey6(0xB5), // cmd
        AESKey7(0xB6), // cmd
        AESKey8(0xB7), // cmd
        AESKey9(0xB8), // cmd
        AESKey10(0xB9), // cmd
        AESKey11(0xBA), // cmd
        AESKey12(0xBB), // cmd
        AESKey13(0xBC), // cmd
        AESKey14(0xBD), // cmd
        AESKey15(0xBE), // cmd
        AESKey16(0xBF), // cmd
        AESKeyName1(0xC0), // cmd, sts
        AESKeyName2(0xC1), // cmd, sts
        AESKeyName3(0xC2), // cmd, sts
        AESKeyName4(0xC3), // cmd, sts
        AESKeyName5(0xC4), // cmd, sts
        AESKeyName6(0xC5), // cmd, sts
        AESKeyName7(0xC6), // cmd, sts
        AESKeyName8(0xC7), // cmd, sts
        AESKeyName9(0xC8), // cmd, sts
        AESKeyName10(0xC9), // cmd, sts
        AESKeyName11(0xCA), // cmd, sts
        AESKeyName12(0xCB), // cmd, sts
        AESKeyName13(0xCC), // cmd, sts
        AESKeyName14(0xCD), // cmd, sts
        AESKeyName15(0xCE), // cmd, sts
        AESKeyName16(0xCF); // cmd, sts

        private final int value;

        /** 
         * Sets the Category Media Encoder cmd/sts key value
         */
        KeyCategoryComsec(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Media Encoder cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  C S I   C O M M A N D  (0x80)   ******** */

    public enum KeyCategoryCSICommand {
        Command1MulticastIPAddress(0x13), // cmd, sts
        Command2MulticastIPAddress(0x23); // cmd, sts

        private final int value;

        /** 
         * Sets the Category Media Encoder cmd/sts key value
         */
        KeyCategoryCSICommand(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Media Encoder cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  C S I   S T A T U S  (0x88)   ******** */

    public enum KeyCategoryCSIStatus {
        StatusMulticastIPAddress(0x23), // cmd, sts
        StatusPort(0x24);

        private final int value;

        /** 
         * Sets the Category Media Encoder cmd/sts key value
         */
        KeyCategoryCSIStatus(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Media Encoder cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  S Y S T E M   T E S T  (0x90)   ******** */

    public enum KeyCategorySystemTest {
        FineOscillatorAdjustTx2(0x3B), // cmd, sts
        FineOscillatorAdjustRx1(0x3C), // cmd, sts
        FineOscillatorAdjustRx2(0x3D), // cmd, sts
        ICarrierSuppressionAdjustTx2(0x3E), // cmd, sts
        QCarrierSuppressionAdjustTx2(0x3F), // cmd, sts
        SaveCalibrationData(0x40), // cmd
        ModulationOff(0x45), // cmd, sts
        MatchConfiguration(0x4A), // cmd, sts
        EnableLogging(0x4B), // cmd, sts
        AWGNEnable(0x50), // cmd, sts
        AWGNEbNo(0x51), // cmd, sts
        PRBSTxMode(0x80), // cmd, sts
        PRBSRxMode(0x81), // cmd, sts
        PRBSRxDetectEnable(0x82), // cmd, sts
        PRBSRxErrors(0x83), // sts
        PRBSRxCountingDone(0x84), // sts
        LEDLampTest(0x90), // sts
        VideoTest(0xd0); // cmd, sts

        private final int value;

        /** 
         * Sets the Category Media Encoder cmd/sts key value
         */
        KeyCategorySystemTest(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Media Encoder cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  M E T A D A T A  (0xA0)   ******** */

    public enum KeyCategoryMetadata {
        RequestMetadataPacketRoverIIIDictionary(0x01), // cmd
        MetadataInput1Enable(0x80), // cmd, sts
        MetadataInput1InterfaceType(0x81); // cmd, sts

        private final int value;

        /** 
         * Sets the Category Media Encoder cmd/sts key value
         */
        KeyCategoryMetadata(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Media Encoder cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    /* ********  E N U M  -  K E Y S  -  C A T E G O R Y  -  N A V I G A T I O N  (0xA8)   ******** */

    public enum KeyCategoryNavigation {
        LocalNavigationSource(0x01), // cmd, sts
        RemoteNavigationSource(0x02), // cmd, sts
        LocalLongitude(0x21), // cmd, sts
        LocalLatitude(0x22), // cmd, sts
        LocalAltitude(0x23), // cmd, sts
        LocalEastRate(0x24), // cmd, sts
        LocalNorthRatte(0x25), // cmd, sts
        LocalUpRate(0x26), // cmd, sts
        LocalRoll(0x27), // cmd, sts
        LocalPitch(0x28), // cmd, sts
        LocalHeading(0x29), // cmd, sts
        RemoteLongitude(0x41), // cmd, sts
        RemoteLatitude(0x42), // cmd, sts
        RemoteAltitude(0x43), // cmd, sts
        RemoteEastRate(0x44), // cmd, sts
        RemoteNorthRate(0x45), // cmd, sts
        RemoteUpRate(0x46), // cmd, sts
        RemoteRoll(0x47), // cmd, sts
        RemotePitch(0x48), // cmd, sts
        RemoteHeading(0x49), // cmd, sts
        NavRange(0x90), // sts
        NavAzimuth(0x91), // sts
        NavElevation(0x92); // sts

        private final int value;

        /** 
         * Sets the Category Media Encoder cmd/sts key value
         */
        KeyCategoryNavigation(int value) {
            this.value = value;
        }

        /** 
         * Returns the int value of the Category Media Encoder cmd/sts key
         */
        public int value() {
            return this.value;
        }

    }

    public enum NetworkFramingType {
        NONE(0x00, "NONE"),
        ANNEX_A(0x01, "Annex A"),
        ANNEX_B(0x02, "Annex B");
        private final byte value;
        private final String name;

        NetworkFramingType(final int value, final String name) {
            this.value = (byte) value;
            this.name = name;
        }

        public byte value() {
            return this.value;
        }

        @NonNull
        public String toString() {
            return this.name;
        }

        public static NetworkFramingType find(final String value) {
            for (NetworkFramingType m : NetworkFramingType.values()) {
                if (m.name.equalsIgnoreCase(value)) {
                    return m;
                }
            }
            return NONE;
        }

        public static NetworkFramingType find(int value) {
            for (NetworkFramingType m : NetworkFramingType.values()) {
                if (m.value == (byte) value) {
                    return m;
                }
            }
            return NONE;
        }

    }

    public enum ModuleType {
        NONE(0x00, "NONE"),
        M1(0x01, "M1"),
        M2(0x02, "M2"),
        M3(0x03, "M3"),
        M4(0x04, "M4");

        private final byte value;
        private final String name;

        ModuleType(final int value, final String name) {
            this.value = (byte) value;
            this.name = name;
        }

        public byte value() {
            return this.value;
        }

        @NonNull
        public String toString() {
            return this.name;
        }

        public static ModuleType find(final String value) {
            for (ModuleType m : ModuleType.values()) {
                if (m.name.equalsIgnoreCase(value)) {
                    return m;
                }
            }
            return NONE;
        }

        public static ModuleType find(int value) {
            for (ModuleType m : ModuleType.values()) {
                if (m.value == (byte) value) {
                    return m;
                }
            }
            return NONE;
        }
    }

    public enum WaveformType {
        CDL_TYPE_A(0x10, "CDL"),
        CDL_TYPE_B(0x11, "CDL ALTERNATIVE"),
        VNW(0x20, "VNW"),
        TACTICAL(0x30, "TACTICAL"),
        ANALOG(0x40, "ANALOG"),
        BECDL(0x70, "BECDL"),
        DDL(0x80, "DDL"),
        NONE(0x00, "NONE");

        private final byte value;
        private final String name;

        WaveformType(final int value, final String name) {
            this.value = (byte) value;
            this.name = name;
        }

        public byte value() {
            return this.value;
        }

        @NonNull
        public String toString() {
            return this.name;
        }

        public static WaveformType find(final String value) {
            for (WaveformType wc : WaveformType.values()) {
                if (wc.name.equalsIgnoreCase(value)) {
                    return wc;
                }
            }
            return NONE;
        }

        public static WaveformType find(int value) {
            for (WaveformType wc : WaveformType.values()) {
                if (wc.value == (byte) value) {
                    return wc;
                }
            }
            return NONE;
        }
    }

    public enum FrameSize {
        FULL(0x00, "FULL"),
        HALF(0x01, "HALF"),
        QUARTER(0x02, "QUARTER");

        private final byte value;
        private final String name;

        FrameSize(final int value, final String name) {
            this.value = (byte) value;
            this.name = name;
        }

        public byte value() {
            return this.value;
        }

        @NonNull
        public String toString() {
            return this.name;
        }

    }

    public enum TelemetryKLV {
        TelemetryOutputEnable(0xC0),
        TelemetryOutputInterfaceType(0xC1),
        TelemetryOutputIPAddress(0xC3),
        TelemetryOutputPort(0xC4),
        CoTTranslationOutputEnable(0xD0),
        CoTTranslationOutputIPAddress(0xD3),
        CoTTranslationOutputPort(0xD4);

        private final int value;

        TelemetryKLV(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    public enum VideoEncoder {
        MPEG2(0x00, "MPEG-2"),
        MPEG4(0x01, "MPEG-4"),
        H264(0x02, "H264"),
        MJPEG(0x03, "MJPEG");

        private final byte value;
        private final String name;

        VideoEncoder(final int value, final String name) {
            this.value = (byte) value;
            this.name = name;
        }

        public byte value() {
            return this.value;
        }

        @NonNull
        public String toString() {
            return this.name;
        }

    }

}
