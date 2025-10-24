
package com.atakmap.comms;

import android.content.Context;

import com.atakmap.commoncommo.CoTDetailExtender;
import com.atakmap.commoncommo.CoTMessageListener;
import com.atakmap.commoncommo.CoTMessageType;
import com.atakmap.commoncommo.CoTSendFailureListener;
import com.atakmap.commoncommo.CoTSendMethod;
import com.atakmap.commoncommo.CommoException;
import com.atakmap.commoncommo.CommoLogger;
import com.atakmap.commoncommo.MissionPackageTransferStatus;
import com.atakmap.commoncommo.NetInterfaceAddressMode;
import com.atakmap.commoncommo.PhysicalNetInterface;
import com.atakmap.commoncommo.TcpInboundNetInterface;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * CommsProvider is the abstract base class for all implementations of communication methods.
 * Classes extending this interface should indicate which features are supported. CommsFeatures include:
 * <ul>
 *     <li>NONE:
 *     <p>Does not have to be explicitly supported, all methods linked to this FEATURE
 *     are required to support the bare minimum functionality of TAK.</li>
 *     <li>MISSION_PACK_SETTINGS:
 *     <p>Settings for mission packages, and how they get transferred.</li>
 *     <li>ENDPOINT_SUPPORT:
 *     <p>Settings for connecting to a takserver, and functionality that requires a takserver or
 *     other endpoint connections. e.g. Mesh endpoint</li>
 *     <li>FILE_IO:
 *  *     <p>FileIO Provider management and settings.</li>
 *     <li>INTERFACE_OPTIONS:
 *  *     <p>Manage the network interfaces, and socket settings.</li>
 *     <li>CRYPTO:
 *     <p>Crypto accessible by the TAK application. A CommsProvider may implement custom crypto
 *     protections that may or may not be exposed to the rest of the application.</li>
 *     <li>VPN:
 *  *     <p>Required for supporting Tactical VPN by Kranze Technologies</li>
 *     <li>UNKNOWN_CONTACT:
 *     <p>Used by CommsMapComponent to determine if we are allowed to send to unknown contacts.
 *     CommsProviders implementing this feature  must support sending messages to UIDs that do
 *     not have an associated Contact card. An example of this is sending a GeoChat to
 *     "All Chat Rooms". There are no associated methods, but it does change the list of UIDs being
 *     sent to the CommsProvider when calling, for example, {@link CommsProvider#sendCoT}. The
 *     primary purpose of this feature is to support information-centric networking environments.
 *     <p>This feature is not supported by the DefaultCommsProvider as a part of ATAK.
 *     </li>
 * </ul>
 *<p>If a CommsFeature is not specified as supported, linked methods should not be called.
 * A call to an unimplemented method will result in a logged Warning, and null/error returns
 * where applicable. To avoid calling unimplemented functions, for a CommsFeature to be declared as
 * supported, all relevant methods should be implemented.
 *
 */
public abstract class CommsProvider {
    private static final String TAG = "CommsProvider";

    public enum CommsFeature {
        MISSION_PACK_SETTINGS,
        /** Reserved for future use */
        ENDPOINT_SUPPORT,
        FILE_IO,
        /** Reserved for future use */
        INTERFACE_OPTIONS,
        CRYPTO,
        VPN,
        UNKNOWN_CONTACT,
        MISSION_API,
        BROADCAST_DATAPACKAGE_CAPABLE,
        MESH_MODE_INPUT_SUPPORTED,
        MESH_MODE_OUTPUT_SUPPORTED
    }

    public interface ContactPresenceListener {
        void contactAdded(String contactUid);

        void contactRemoved(String contactUid);
    }

    /**
     * A bundle of information about an ongoing or just completed
     * inbound Mission Package transfer.
     */
    public final static class MissionPackageReceiveStatusUpdate {
        /**
         * The local file identifying which transfer the update pertains to.
         * Will match a File previously returned in a call to MissionPackageIO's
         * missionPackageReceiveInit()
         */
        public final File localFile;

        /**
         * The current status of the transfer.
         * Can be one of:
         * FINISHED_SUCCESS
         * FINISHED_FAILED
         * ATTEMPT_IN_PROGRESS
         * ATTEMPT_FAILED
         * <p>
         * The FINISHED_* status codes indicate this will be the final update
         * for this MP transfer.
         * The ATTEMPT_* codes indicate that another status report will be
         * forthcoming.
         */
        public final MissionPackageTransferStatus status;

        /**
         * Total number of bytes received <b>in this attempt</b>.
         */
        public final long totalBytesReceived;

        /**
         * Total number of bytes expected to be received to complete the transfer.
         * Note: this number is provided by the sender and may not be accurate!
         * If the sender does not provide the information, it will be reported as
         * zero (0).  Because of these limitations, totalBytesReceived
         * could exceed this number!
         * This number will remain the same in all status updates for a
         * given transfer.
         */
        public final long totalBytesExpected;

        /**
         * The current download attempt, numbered stating at 1, and less than or
         * equal to "maxAttempts". For ATTEMPT_FAILED updates, this is the
         * attempt number that failed. For FINISHED_* updates, this is the attempt
         * number that caused the final status update.
         */
        public final int attempt;

        /**
         * The total number of attempts that will be made to download the
         * Mission Package.  This will be constant for all updates for a given
         * transfer.
         */
        public final int maxAttempts;

        /**
         * a textual description of why the transfer failed.
         * This is only non-null when status is FINISHED_FAILED or ATTEMPT_FAILED
         */
        public final String errorDetail;

        public MissionPackageReceiveStatusUpdate(File file,
                MissionPackageTransferStatus status,
                long bytesReceived,
                long bytesExpected,
                int attempt,
                int maxAttempts,
                String errorDetail) {
            this.localFile = file;
            this.status = status;
            this.totalBytesReceived = bytesReceived;
            this.totalBytesExpected = bytesExpected;
            this.attempt = attempt;
            this.maxAttempts = maxAttempts;
            this.errorDetail = errorDetail;
        }

    }

    /**
     * A bundle of information about an ongoing outbound Mission Package transfer
     * to one or more Contacts, or a TAK server.
     */
    public final static class MissionPackageSendStatusUpdate {
        /**
         * identifier that uniquely identifies the transfer - matches
         * the id returned from Commo's sendMissionPackageInit()
         */
        public final int transferId;
        /**
         * Recipient to which this update pertains.
         * Will be null when sending to a TAK server.
         */
        public final String recipientUid;
        /**
         * The status of the transfer to this recipient. Can be one of:
         * FINISHED_SUCCESS,
         * FINISHED_TIMED_OUT,
         * FINISHED_FAILED,
         * FINISHED_DISABLED_LOCALLY,
         * SERVER_UPLOAD_PENDING,
         * SERVER_UPLOAD_IN_PROGRESS,
         * SERVER_UPLOAD_SUCCESS,
         * SERVER_UPLOAD_FAILED
         * For sends to one or more Contacts (not a TAK server), can also
         * be one of:
         * ATTEMPT_IN_PROGRESS,
         * FINISHED_CONTACT_GONE,
         * <p>
         * FINISHED_CONTACT_GONE indicates the contact disappeared between the
         * initiation of the transfer and when we could actually notify
         * the Contact that the file was ready for transfer.
         * FINISHED_DISABLED_LOCALLY indicates that a transfer had been started,
         * but the local mission package setup was changed in a way that was
         * incompatible with the started transfer (server transfer and
         * server transfers were disabled, peer to peer transfer and the local
         * web server port was changed or disabled).
         * <p>
         * Any FINISHED_* code should be considered the final update for this
         * transfer *to the specified recipient*. No further updates will
         * come for the recipient for this transfer.
         * <p>
         * If the transfer is to a server, or if a destination Contact is
         * reachable on a server, the SERVER_* status events may be seen to report
         * progress of making the MP available on the server.
         * SERVER_UPLOAD_PENDING will be seen shortly after the start of the
         * transfer if the upload is required; this will continue until
         * the upload actually begins.
         * SERVER_UPLOAD_IN_PROGRESS status events will report
         * start and progress of the upload.
         * The upload ends with either SERVER_UPLOAD_SUCCESS or
         * SERVER_UPLOAD_FAILED.
         * Successful uploads will shortly post a FINISHED_SUCCESS status
         * update for sends
         * to a server, or an ATTEMPT_IN_PROGRESS (see below) for a send to
         * a server-reachable Contact.
         * Failed uploads will shortly post a FINISHED_* status.
         * <p>
         * For transfers to contacts not reachable via TAK servers,
         * the first status report will generally be ATTEMPT_IN_PROGRESS which
         * indicates the Contact has been notified of the MP availability and
         * we are awaiting them to fetch it and send us an acknowledgement (or
         * failure report).
         */
        public final MissionPackageTransferStatus status;
        /**
         * For FINISHED_* events, this is the "reason" string returned by the
         * recipient; which gives more detail on the transfer results.
         * It is always non-null (but possibly empty) for FINISHED_* events
         * generated by a (n)ack response from the receiver.
         * For FINISHED_* events caused by local error conditions,
         * this will be null.
         * For SERVER_UPLOAD_SUCCESS, this will contain the URL of the
         * file on the server.
         * For SERVER_UPLOAD_FAILED, this will contain error details on why
         * the upload failed.
         * For other status codes, this will be null.
         */
        public final String additionalDetail;

        /**
         * For status == SERVER_UPLOAD_*,
         * indicates the total number of bytes uploaded so far.
         * For status == FINISHED_SUCCESS, on a send to a contact
         * this contains the # of bytes the remote recipient says it received
         * or 0 if unknown; on sends to a TAK server, contains the #
         * of bytes transferred (this will be 0 if the file was
         * already on the server).
         * Will be 0 for other status codes.
         */
        public final long totalBytesTransferred;

        public MissionPackageSendStatusUpdate(int transferId,
                String receiverUid,
                MissionPackageTransferStatus status,
                String detail,
                long bytesTransferred) {
            this.transferId = transferId;
            this.recipientUid = receiverUid;
            this.status = status;
            this.additionalDetail = detail;
            this.totalBytesTransferred = bytesTransferred;
        }
    }

    public abstract boolean isInitialized();

    public abstract void init(Context context, CommoLogger logger, String uid,
            String callsign, NetInterfaceAddressMode mode)
            throws CommoException;

    /**
     * A human readable name describing this CommsProvider.
     *
     * @return the name
     */
    public abstract String getName();

    /**
     * Checks if the provided CommsFeature is supported
     *
     * @param feature CommsFeature to be checked
     * @return true if the CommsFeature is supported
     */
    public abstract boolean hasFeature(CommsFeature feature);

    /**
     * Shuts down the active CommsProvider
     */
    public abstract void shutdown();

    /**************************************************
     * Feature: General/Other
     * <p>
     * These will always be called, up to the CommsProvider
     * to implement if applicable.
     **************************************************/

    /**
     * If {@code false}, the communications layer does not support user
     * initiated modifications to the callsign via traditional methods.
     *
     * <P>Feature: None (required to implement)
     */
    public boolean isCallsignConfigurable() {
        return true;
    }

    /** <P>Feature: None (required to implement) */
    public void addCoTMessageListener(CoTMessageListener coTMessageListener) {
        Log.w(getName(), "addCotMessageListener() not implemented");
    }

    /** <P>Feature: None (required to implement) */
    public void addCoTSendFailureListener(
            CoTSendFailureListener coTSendFailureListener) {
        Log.w(TAG, "addCoTSendFailureListener() not implemented");
    }

    /** <P>Feature: None (required to implement) */
    public void addContactPresenceListener(
            ContactPresenceListener contactPresenceListener) {
        Log.w(TAG, "addContactPresenceListener() not implemented");
    }

    /** <P>Feature: None (required to implement) */
    public void setupMissionPackageIO(CommsMapComponent.MasterMPIO mpio)
            throws CommoException {
        Log.w(TAG, "setupMissionPackageIO() not implemented");
    }

    /** <P>Feature: None (required to implement) */
    public void broadcastCoT(String event) throws CommoException {
        Log.w(TAG, "broadcastCoT() not implemented");
    }

    /** <P>Feature: None (required to implement) */
    public void broadcastCoT(String event, CoTSendMethod method)
            throws CommoException {
        Log.w(TAG, "broadcastCoT() not implemented");
    }

    /** <P>Feature: None (required to implement) */
    public void sendCoT(Collection<String> contactUids, String event,
            CoTSendMethod method) throws CommoException {
        Log.w(TAG, "broadcastCoT() not implemented");
    }

    /** <P>Feature: None (required to implement) */
    public void sendCoTToServerMissionDest(String id, String mission,
            String event) throws CommoException {
        Log.w(TAG, "sendCoTToServerMissionDest() not implemented");
    }

    /** <P>Feature: None (required to implement) */
    public int sendMissionPackageInit(String streamingId, File file,
            String transferFilename) throws CommoException {
        Log.w(TAG, "sendMissionPackageInit() not implemented");
        return -1;
    }

    /** <P>Feature: None (required to implement) */
    public int sendMissionPackageInit(Collection<String> contactUidList,
            File file, String transferFileName, String transferName,
            String access, String caveats,
            String releasableTo) throws CommoException {
        Log.w(TAG, "sendMissionPackageInit() not implemented");
        return -1;
    }

    /** <P>Feature: None (required to implement) */
    @Deprecated
    @DeprecatedApi(forRemoval = true, removeAt = "5.7", since = "5.4")
    public int sendMissionPackageInit(Collection<String> contactUidList,
            File file, String transferFileName, String transferName)
            throws CommoException {
        Log.w(TAG, "sendMissionPackageInit() not implemented");
        return -1;
    }

    /** <P>Feature: None (required to implement) */
    public void startMissionPackageSend(int id) throws CommoException {
        Log.w(TAG, "startMissionPackageSend() not implemented");
    }

    /** <P>Feature: None (required to implement) */
    public int simpleFileTransferInit(boolean forUpload, URI uri, byte[] caCert,
            String caCertPassword, String username, String password,
            File localFile) throws CommoException {
        Log.w(TAG, "simpleFileTransferInit() not implemented");
        return -1;
    }

    /** <P>Feature: None (required to implement) */
    public void simpleFileTransferStart(int id) throws CommoException {
        Log.w(TAG, "simpleFileTransferStart() not implemented");
    }

    /** <P>Feature: None (required to implement) */
    public void registerCoTDetailExtender(int id, String element,
            CoTDetailExtender extension) throws CommoException {
        Log.w(TAG, "registerCoTDetailExtender() not implemented");
    }

    /** <P>Feature: None (required to implement) */
    public void deregisterCoTDetailExtender(int id) throws CommoException {
        Log.w(TAG, "deregisterCoTDetailExtender() not implemented");
    }

    /**************************************************
     * Feature: MISSION_PACK_SETTINGS
     * <p>
     * Settings for mission packages, and how they get
     * transferred.
     **************************************************/

    /** <P>Feature: {@link CommsFeature#MISSION_PACK_SETTINGS} */
    public void setMissionPackageNumTries(int i) throws CommoException {
        Log.w(TAG, "setMissionPackageNumTries() not implemented");
    }

    /** <P>Feature: {@link CommsFeature#MISSION_PACK_SETTINGS} */
    public void setMissionPackageConnTimeout(int i) throws CommoException {
        Log.w(TAG, "setMissionPackageConnTimeout() not implemented");
    }

    /** <P>Feature: {@link CommsFeature#MISSION_PACK_SETTINGS} */
    public void setMissionPackageTransferTimeout(int i) throws CommoException {
        Log.w(TAG, "setMissionPackageTransferTimeout() not implemented");
    }

    /** <P>Feature: {@link CommsFeature#MISSION_PACK_SETTINGS} */
    public void setMissionPackageHttpsPort(int i) throws CommoException {
        Log.w(TAG, "setMissionPackageHttpsPort() not implemented");
    }

    /** <P>Feature: {@link CommsFeature#MISSION_PACK_SETTINGS} */
    public void setMissionPackageHttpPort(int i) throws CommoException {
        Log.w(TAG, "setMissionPackageHttpPort() not implemented");
    }

    /** <P>Feature: {@link CommsFeature#MISSION_PACK_SETTINGS} */
    public void setMissionPackageViaServerEnabled(boolean enable) {
        Log.w(TAG, "setMissionPackageViaServerEnabled() not implemented");
    }

    /** <P>Feature: {@link CommsFeature#MISSION_PACK_SETTINGS} */
    public void setMissionPackageLocalPort(int port) throws CommoException {
        Log.w(TAG, "setMissionPackageLocalPort() not implemented");
    }

    /** <P>Feature: {@link CommsFeature#MISSION_PACK_SETTINGS} */
    public void setMissionPackageLocalHttpsParams(int port, byte[] cert,
            String secret) throws CommoException {
        Log.w(TAG, "setMissionPackageLocalHttpsParams() not implemented");
    }

    /**************************************************
     * Feature: FILE_IO
     * <p>
     * FileIO Provider management and settings.
     **************************************************/

    /** <P>Feature: {@link CommsFeature#FILE_IO} */
    public void enableSimpleFileIO(CommsMapComponent.MasterFileIO masterFileIO)
            throws CommoException {
        Log.w(TAG, "enableSimpleFileIO() not implemented");
    }

    /** <P>Feature: {@link CommsFeature#FILE_IO} */
    public void registerFileIOProvider(
            com.atakmap.commoncommo.FileIOProvider provider) {
        Log.w(TAG, "registerFileIOProvider() not implemented");
    }

    /** <P>Feature: {@link CommsFeature#FILE_IO} */
    public void unregisterFileIOProvider(
            com.atakmap.commoncommo.FileIOProvider provider) {
        Log.w(TAG, "unregisterFileIOProvider() not implemented");
    }

    /**************************************************
     * Feature: CRYPTO
     * <p>
     * Crypto accessible by the TAK application. A
     * CommsProvider may implement custom crypto
     * protections that may or may not be exposed to
     * the rest of the application.
     **************************************************/

    /** <P>Feature: {@link CommsFeature#CRYPTO} */
    public String generateKeyCryptoString(String password, int keyLength) {
        Log.w(TAG, "generateKeyCryptoString() not implemented");
        return null;
    }

    /** <P>Feature: {@link CommsFeature#CRYPTO} */
    public String generateKeystoreCryptoString(String certPem,
            List<String> caPem, String privateKeyPem,
            String password, String friendlyName) {
        Log.w(TAG, "generateKeystoreCryptoString() not implemented");
        return null;
    }

    /** <P>Feature: {@link CommsFeature#CRYPTO} */
    public String generateCSRCryptoString(Map<String, String> dnEntries,
            String privateKey, String password) {
        Log.w(TAG, "generateCSRCryptoString() not implemented");
        return null;
    }

    /** <P>Feature: {@link CommsFeature#CRYPTO} */
    public byte[] generateSelfSignedCert(String secret) throws CommoException {
        Log.w(TAG, "generateSelfSignedCert() not implemented");
        return null;
    }

    /** <P>Feature: {@link CommsFeature#CRYPTO} */
    public void setCryptoKeys(byte[] bytes, byte[] bytes1)
            throws CommoException {
        Log.w(TAG, "setCryptoKeys() not implemented");
    }

    /**************************************************
     * Feature: VPN
     * <p>
     * Required for Tactical VPN by Kranze Technologies
     **************************************************/

    /** <P>Feature: {@link CommsFeature#VPN} */
    public void setMagtabWorkaroundEnabled(boolean enabled) {
        Log.w(TAG, "setMagtabWorkaroundEnabled() not implemented");
    }

    /**************************************************
     * Feature: UNKNOWN_CONTACT
     * <p>
     * Used by CommsMapComponent to determine if we are
     * allowed to send to unknown contacts.
     **************************************************/

    /**************************************************
     * Feature: MISSION_API
     * <p>
     * Used by Mission API to send requests using
     * alternative communications mechanisms
     **************************************************/

    /** <P>Feature: {@link CommsFeature#MISSION_API}
     *
     * @param requestType String indicating the request type to be sent via CommsProvider
     * @param json String containing the request information to be sent via CommsProvider
     * @return true if send was successful
     */
    public boolean sendMissionApiRequest(String requestType, String json) {
        Log.w(TAG, "sendMissionApiRequest() not implemented");
        return false;
    }

    @Deprecated
    @DeprecatedApi(forRemoval = true, removeAt = "5.9", since = "5.6")
    public byte[] getRequestResponse(String requestLine, byte[] requestData) {
        Log.w(TAG, "getRequestResponse() not implemented");
        return null;
    }

    @Deprecated
    @DeprecatedApi(forRemoval = true, removeAt = "5.9", since = "5.6")
    public int getResponseCode(String requestLine, byte[] requestData) {
        Log.w(TAG, "getResponseCode() not implemented");
        return 500;
    }

    public InputStream getRequestResponse(String requestLine, InputStream requestData) {
        Log.w(TAG, "getRequestResponse() not implemented");
        return null;
    }

    public int getResponseCode(String requestLine, InputStream requestData) {
        Log.w(TAG, "getResponseCode() not implemented");
        return 500;
    }

    /**************************************************
     * Feature: MESH_MODE_INPUT_SUPPORTED
     * <p>
     * Used by alternative comms to allow
     * ingesting traffic using mesh network mode inputs
     **************************************************/

    public void removeTcpInboundInterface(TcpInboundNetInterface iface) {
        Log.w(TAG, "removeTcpInboundInterface() not implemented");
    }

    public TcpInboundNetInterface addTcpInboundInterface(int port)
            throws CommoException {
        Log.w(TAG, "addTcpInboundInterface() not implemented");
        return null;
    }

    public void removeInboundInterface(PhysicalNetInterface iface) {
        Log.w(TAG, "removeInboundInterface() not implemented");
    }

    public PhysicalNetInterface addInboundInterface(String name, int port,
            String[] mcastAdresses, boolean b) throws CommoException {
        Log.w(TAG, "addInboundInterface() not implemented");
        return null;
    }

    /**************************************************
     * Feature: MESH_MODE_OUTPUT_SUPPORTED
     * <p>
     * Used by alternative comms to allow
     * sending traffic using mesh network mode outputs
     **************************************************/

    public void removeBroadcastInterface(PhysicalNetInterface iface) {
        Log.w(TAG, "removeBroadcastInterface() not implemented");
    }

    public PhysicalNetInterface addBroadcastInterface(CoTMessageType[] types,
            String mcastAddress, int port) throws CommoException {
        Log.w(TAG, "addBroadcastInterface() not implemented");
        return null;
    }

    public PhysicalNetInterface addBroadcastInterface(String name,
            CoTMessageType[] types, String mcastAddress, int port)
            throws CommoException {
        Log.w(TAG, "addBroadcastInterface() not implemented");
        return null;
    }
}
