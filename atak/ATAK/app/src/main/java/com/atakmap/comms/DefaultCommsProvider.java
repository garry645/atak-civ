
package com.atakmap.comms;

import android.content.Context;

import com.atakmap.commoncommo.CloudClient;
import com.atakmap.commoncommo.CloudIO;
import com.atakmap.commoncommo.CloudIOProtocol;
import com.atakmap.commoncommo.CoTDetailExtender;
import com.atakmap.commoncommo.CoTMessageListener;
import com.atakmap.commoncommo.CoTMessageType;
import com.atakmap.commoncommo.CoTSendFailureListener;
import com.atakmap.commoncommo.CoTSendMethod;
import com.atakmap.commoncommo.Commo;
import com.atakmap.commoncommo.CommoException;
import com.atakmap.commoncommo.CommoLogger;
import com.atakmap.commoncommo.Contact;
import com.atakmap.commoncommo.EnrollmentIO;
import com.atakmap.commoncommo.InterfaceStatusListener;
import com.atakmap.commoncommo.NetInterfaceAddressMode;
import com.atakmap.commoncommo.PhysicalNetInterface;
import com.atakmap.commoncommo.QuicInboundNetInterface;
import com.atakmap.commoncommo.StreamingNetInterface;
import com.atakmap.commoncommo.StreamingTransport;
import com.atakmap.commoncommo.TcpInboundNetInterface;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import gov.tak.api.annotation.DeprecatedApi;

public final class DefaultCommsProvider extends CommsProvider {

    private static final String TAG = "DefaultCommsProvider";

    private static final HashSet<CommsFeature> COMMS_FEATURES = new HashSet<>(
            Arrays.asList(
                    CommsFeature.MISSION_PACK_SETTINGS,
                    CommsFeature.ENDPOINT_SUPPORT,
                    CommsFeature.FILE_IO,
                    CommsFeature.INTERFACE_OPTIONS,
                    CommsFeature.CRYPTO,
                    CommsFeature.VPN));

    private static boolean commoNativeInitComplete = false;

    private Commo commo;
    private final com.atakmap.commoncommo.ContactPresenceListener contactPresenceForwarder;
    private final Set<ContactPresenceListener> contactPresenceListeners;
    private final Map<String, Contact> uidToCommoContact;

    public DefaultCommsProvider() {
        contactPresenceListeners = Collections
                .newSetFromMap(new ConcurrentHashMap<>());
        uidToCommoContact = new ConcurrentHashMap<>();
        contactPresenceForwarder = new ContactPresenceForwarder();
    }

    @Override
    public boolean isInitialized() {
        return commoNativeInitComplete && commo != null;
    }

    @Override
    public void init(Context context, CommoLogger logger, String uid,
            String callsign, NetInterfaceAddressMode mode)
            throws CommoException {

        if (!commoNativeInitComplete) {
            com.atakmap.coremap.loader.NativeLoader
                    .loadLibrary("commoncommojni");
            Commo.initThirdpartyNativeLibraries();
            commoNativeInitComplete = true;
            Log.d(TAG,
                    "initialized the common communication layer native libraries");
        }

        if (commoNativeInitComplete && commo == null) {
            commo = new Commo(logger, uid, callsign,
                    NetInterfaceAddressMode.NAME);
            commo.addContactPresenceListener(contactPresenceForwarder);
        }
    }

    /**
     * A human readable name describing this CommsProvider.
     *
     * @return the name
     */
    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public boolean hasFeature(CommsFeature feature) {
        return COMMS_FEATURES.contains(feature);
    }

    @Override
    public void shutdown() {
        if (commo != null)
            commo.shutdown();
        commo = null;
    }

    @Override
    public void addCoTMessageListener(CoTMessageListener coTMessageListener) {
        commo.addCoTMessageListener(coTMessageListener);
    }

    void addInterfaceStatusListener(
            InterfaceStatusListener interfaceStatusListener) {
        commo.addInterfaceStatusListener(interfaceStatusListener);
    }

    @Override
    public void addContactPresenceListener(
            ContactPresenceListener contactPresenceListener) {
        contactPresenceListeners.add(contactPresenceListener);
    }

    @Override
    public void addCoTSendFailureListener(
            CoTSendFailureListener coTSendFailureListener) {
        commo.addCoTSendFailureListener(coTSendFailureListener);
    }

    void setStreamMonitorEnabled(boolean enabled) {
        commo.setStreamMonitorEnabled(enabled);
    }

    void setMulticastLoopbackEnabled(boolean enabled) {
        commo.setMulticastLoopbackEnabled(enabled);
    }

    @Override
    public void setCryptoKeys(byte[] bytes, byte[] bytes1)
            throws CommoException {
        commo.setCryptoKeys(bytes, bytes1);
    }

    @Override
    public void setMissionPackageNumTries(int i) throws CommoException {
        commo.setMissionPackageNumTries(i);
    }

    @Override
    public void setMissionPackageConnTimeout(int i) throws CommoException {
        commo.setMissionPackageConnTimeout(i);
    }

    @Override
    public void setMissionPackageTransferTimeout(int i) throws CommoException {
        commo.setMissionPackageTransferTimeout(i);
    }

    @Override
    public void setMissionPackageHttpsPort(int i) throws CommoException {
        commo.setMissionPackageHttpsPort(i);
    }

    @Override
    public void setMissionPackageHttpPort(int i) throws CommoException {
        commo.setMissionPackageHttpPort(i);
    }

    void setEnableAddressReuse(boolean enable) {
        commo.setEnableAddressReuse(enable);
    }

    @Override
    public void setupMissionPackageIO(CommsMapComponent.MasterMPIO mpio)
            throws CommoException {
        commo.setupMissionPackageIO(mpio);
    }

    @Override
    public void enableSimpleFileIO(CommsMapComponent.MasterFileIO masterFileIO)
            throws CommoException {
        commo.enableSimpleFileIO(masterFileIO);
    }

    void enableEnrollment(EnrollmentIO enrollmentIO) throws CommoException {
        commo.enableEnrollment(enrollmentIO);
    }

    int enrollmentInit(String host, int port, boolean verifyHost, String user,
            String password, boolean passIsToken, byte[] caCert,
            String caCertPassword, String clientCertPassword,
            String enrolledTrustPassword, String clientCertPassword2,
            int size, String versionCode) throws CommoException {
        return commo.enrollmentInit(host, port, verifyHost, user,
                password, passIsToken, caCert,
                caCertPassword, clientCertPassword, enrolledTrustPassword,
                clientCertPassword2,
                size, versionCode);
    }

    void enrollmentStart(int id) throws CommoException {
        commo.enrollmentStart(id);
    }

    @Override
    public void setMagtabWorkaroundEnabled(boolean enabled) {
        commo.setMagtabWorkaroundEnabled(enabled);
    }

    @Override
    public void registerFileIOProvider(
            com.atakmap.commoncommo.FileIOProvider provider) {
        commo.registerFileIOProvider(provider);
    }

    @Override
    public void unregisterFileIOProvider(
            com.atakmap.commoncommo.FileIOProvider provider) {
        commo.unregisterFileIOProvider(provider);
    }

    @Override
    public void registerCoTDetailExtender(int id, String element,
            CoTDetailExtender extension) throws CommoException {
        commo.registerCoTDetailExtender(id, element, extension);
    }

    @Override
    public void deregisterCoTDetailExtender(int id) throws CommoException {
        commo.deregisterCoTDetailExtender(id);
    }

    void setDestUidInsertionEnabled(boolean en) {
        commo.setDestUidInsertionEnabled(en);
    }

    void setPreferStreamEndpoint(boolean prefer) {
        commo.setPreferStreamEndpoint(prefer);
    }

    void setTTL(int seconds) {
        commo.setTTL(seconds);
    }

    void setUdpNoDataTimeout(int seconds) {
        commo.setUdpNoDataTimeout(seconds);
    }

    void setTcpConnTimeout(int seconds) {
        commo.setTcpConnTimeout(seconds);
    }

    @Override
    public PhysicalNetInterface addInboundInterface(String name, int port,
            String[] mcastAdresses,
            boolean b) throws CommoException {
        return commo.addInboundInterface(name, port, mcastAdresses, b);
    }

    @Override
    public void removeInboundInterface(PhysicalNetInterface iface) {
        commo.removeInboundInterface(iface);
    }

    @Override
    public TcpInboundNetInterface addTcpInboundInterface(int port)
            throws CommoException {
        return commo.addTcpInboundInterface(port);
    }

    @Override
    public void removeTcpInboundInterface(TcpInboundNetInterface iface) {
        commo.removeTcpInboundInterface(iface);
    }

    public PhysicalNetInterface addBroadcastInterface(CoTMessageType[] types,
            String mcastAddress, int port) throws CommoException {
        return commo.addBroadcastInterface(types, mcastAddress, port);
    }

    @Override
    public PhysicalNetInterface addBroadcastInterface(String name,
            CoTMessageType[] types,
            String mcastAddress, int port) throws CommoException {
        return commo.addBroadcastInterface(name, types, mcastAddress, port);
    }

    @Override
    public void removeBroadcastInterface(PhysicalNetInterface iface) {
        commo.removeBroadcastInterface(iface);
    }

    StreamingNetInterface addStreamingInterface(StreamingTransport transport,
            String hostname, int port,
            CoTMessageType[] types, byte[] clientCert,
            byte[] caCert, String certPass,
            String caCertPass, String authUser,
            String authPass) throws CommoException {
        return commo.addStreamingInterface(transport, hostname, port, types,
                clientCert,
                caCert, certPass, caCertPass, authUser, authPass);
    }

    void removeStreamingInterface(StreamingNetInterface iface) {
        commo.removeStreamingInterface(iface);
    }

    // DEPRECATED
    // Supports only tcp endpoints for backwards compatibility
    void sendCoTTcpDirect(String ip, int port, String event)
            throws CommoException {
        commo.sendCoTTcpDirect(ip, port, event);
    }

    @Override
    public void broadcastCoT(String event, CoTSendMethod method)
            throws CommoException {
        commo.broadcastCoT(event, method);
    }

    @Override
    public void sendCoT(Collection<String> contactUids, String event,
            CoTSendMethod method) throws CommoException {
        Vector<Contact> contacts = new Vector<>(contactUids.size());
        for (String contactUid : contactUids) {
            Contact contact = uidToCommoContact.get(contactUid);
            if (contact != null)
                contacts.add(contact);
        }
        commo.sendCoT(contacts, event, method);
    }

    @Override
    public void sendCoTToServerMissionDest(String id, String mission,
            String event) throws CommoException {
        commo.sendCoTToServerMissionDest(id, mission, event);
    }

    void sendCoTServerControl(String id, String event) throws CommoException {
        commo.sendCoTServerControl(id, event);
    }

    void configKnownEndpointContact(String id, String name, String ipAddr,
            int port) throws CommoException {
        commo.configKnownEndpointContact(id, name, ipAddr, port);
    }

    @Override
    public String generateKeyCryptoString(String password, int keyLength) {
        return commo.generateKeyCryptoString(password, keyLength);
    }

    @Override
    public String generateKeystoreCryptoString(String certPem,
            List<String> caPem, String privateKeyPem,
            String password, String friendlyName) {
        return commo.generateKeystoreCryptoString(certPem,
                caPem, privateKeyPem, password, friendlyName);
    }

    @Override
    public String generateCSRCryptoString(Map<String, String> dnEntries,
            String privateKey, String password) {
        return commo.generateCSRCryptoString(dnEntries,
                privateKey, password);
    }

    CloudClient createCloudClient(CloudIO io, CloudIOProtocol proto,
            String host, int port, String basePath, String user,
            String password, byte[] caCerts, String s) throws Exception {
        return commo.createCloudClient(io, proto, host, port, basePath,
                user, password, caCerts, s);
    }

    void destroyCloudClient(CloudClient client) throws Exception {
        commo.destroyCloudClient(client);
    }

    @Override
    public void setMissionPackageViaServerEnabled(boolean enable) {
        commo.setMissionPackageViaServerEnabled(enable);
    }

    @Override
    public byte[] generateSelfSignedCert(String secret) throws CommoException {
        return commo.generateSelfSignedCert(secret);
    }

    @Override
    public void setMissionPackageLocalPort(int port) throws CommoException {
        commo.setMissionPackageLocalPort(port);
    }

    @Override
    public void setMissionPackageLocalHttpsParams(int port, byte[] cert,
            String secret) throws CommoException {
        commo.setMissionPackageLocalHttpsParams(port, cert, secret);
    }

    @Override
    public int sendMissionPackageInit(String streamingId, File file,
            String transferFilename) throws CommoException {
        return commo.sendMissionPackageInit(streamingId,
                file, transferFilename);
    }

    @Override
    @Deprecated
    @DeprecatedApi(forRemoval = true, removeAt = "5.7", since = "5.4")
    public int sendMissionPackageInit(Collection<String> contactUids, File file,
            String transferFilename, String transferName)
            throws CommoException {
        return sendMissionPackageInit(contactUids, file, transferFilename,
                transferName,
                null, null, null);
    }

    @Override
    public int sendMissionPackageInit(Collection<String> contactUids, File file,
            String transferFilename, String transferName, String access,
            String caveats, String releasableTo)
            throws CommoException {
        List<Contact> contactList = new ArrayList<>(contactUids.size());
        for (String contactUid : contactUids) {
            Contact contact = uidToCommoContact.get(contactUid);
            if (contact != null)
                contactList.add(contact);
        }
        return commo.sendMissionPackageInit(contactList, file,
                transferFilename, transferName, access, caveats, releasableTo);
    }

    @Override
    public void startMissionPackageSend(int id) throws CommoException {
        commo.startMissionPackageSend(id);
    }

    @Override
    public int simpleFileTransferInit(boolean forUpload, URI uri, byte[] caCert,
            String caCertPassword, String username, String password,
            File localFile) throws CommoException {
        return commo.simpleFileTransferInit(forUpload, uri, caCert,
                caCertPassword, username, password, localFile);
    }

    @Override
    public void simpleFileTransferStart(int id) throws CommoException {
        commo.simpleFileTransferStart(id);
    }

    QuicInboundNetInterface addQuicInboundInterface(int i, byte[] bytes,
            String s) throws CommoException {
        return commo.addQuicInboundInterface(i, bytes, s);
    }

    void removeQuicInboundInterface(QuicInboundNetInterface iface) {
        commo.removeQuicInboundInterface(iface);
    }

    final class ContactPresenceForwarder
            implements com.atakmap.commoncommo.ContactPresenceListener {
        public void contactAdded(Contact commoContact) {
            if (uidToCommoContact.put(commoContact.contactUID,
                    commoContact) == null) {
                for (ContactPresenceListener l : contactPresenceListeners)
                    l.contactAdded(commoContact.contactUID);
            }
        }

        @Override
        public void contactRemoved(Contact commoContact) {
            if (uidToCommoContact.remove(commoContact.contactUID) != null) {
                for (ContactPresenceListener l : contactPresenceListeners)
                    l.contactRemoved(commoContact.contactUID);
            }
        }
    }
}
