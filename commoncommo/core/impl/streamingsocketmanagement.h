#ifndef IMPL_STREAMINGSOCKETMANAGEMENT_H_
#define IMPL_STREAMINGSOCKETMANAGEMENT_H_


#include "commoutils.h"
#include "netinterface.h"
#include "commologger.h"
#include "commoresult.h"

#include "commotime.h"
#include "netsocket.h"
#include "threadedhandler.h"
#include "cotmessage.h"
#include "resolverqueue.h"
#include "internalutils.h"
#include "commothread.h"
#include "quicconnection.h"

#include "openssl/ssl.h"

#include <map>
#include <set>
#include <deque>

namespace atakmap {
namespace commoncommo {
namespace impl {


class StreamingMessageListener
{
public:
    // Parameters must be copied during event servicing if used after return
    virtual void streamingMessageReceived(std::string streamingEndpoint, const CoTMessage *message) = 0;

protected:
    StreamingMessageListener() {};
    virtual ~StreamingMessageListener() {};

private:
    COMMO_DISALLOW_COPY(StreamingMessageListener);

};



class StreamingSocketManagement : public ThreadedHandler, public ResolverListener
{
public:
    StreamingSocketManagement(CommoLogger *logger, ExtensionRegistry *extensions, const std::string &myuid);
    virtual ~StreamingSocketManagement();

    void setMonitor(bool enable);
    void setConnTimeout(float sec);

    StreamingNetInterface *addStreamingInterface(
            netinterfaceenums::StreamingTransport transport,
            const char *hostname, int port,
            const CoTMessageType *types,
            size_t nTypes,
            const uint8_t *clientCert, size_t clientCertLen,
            const uint8_t *caCert, size_t caCertLen,
            const char *certPassword,
            const char *caCertPassword,
            const char *username,
            const char *password,
            CommoResult *resultCode);
    CommoResult removeStreamingInterface(std::string *endpoint, StreamingNetInterface *iface);
    void reconnectAll();

    // Configures the given openssl context with ssl parameters associated with
    // the stream identifies by streamingEndpoint.  Throws if the endpoint
    // identifier does not correspond to an active endpoint or if said
    // endpoint is not SSL-based.
    void configSSLForConnection(std::string streamingEndpoint, SSL_CTX *ctx) COMMO_THROW (std::invalid_argument);
    bool isEndpointSSL(std::string streamingEndpoint) COMMO_THROW (std::invalid_argument);
    NetAddress *getAddressForEndpoint(std::string streamingEndpoint) COMMO_THROW (std::invalid_argument);

    // Msg should have destination contacts already set and endpoints
    // set for streaming.
    void sendMessage(std::string streamingEndpoint, const CoTMessage *msg) COMMO_THROW (std::invalid_argument);
    // Msg should have endpoints set for streaming
    // Only broadcast to those streams configured to support the message's type,
    // unless ignoreType is true, then send to all servers regardless
    void sendBroadcast(const CoTMessage *msg, bool ignoreType = false) COMMO_THROW (std::invalid_argument);

    void addStreamingMessageListener(StreamingMessageListener *listener);
    void removeStreamingMessageListener(StreamingMessageListener *listener);
    void addInterfaceStatusListener(InterfaceStatusListener *listener);
    void removeInterfaceStatusListener(InterfaceStatusListener *listener);
    
    // ResolverListener impl
    virtual bool resolutionComplete(ResolverQueue::Request *id,
                                    const std::string &hostAddr,
                                    NetAddress *result);
    virtual void resolutionAttemptFailed(ResolverQueue::Request *id,
                                    const std::string &hostAddr);

protected:
    // ThreadedHandler impl
    virtual void threadEntry(size_t threadNum);
    virtual void threadStopSignal(size_t threadNum);

private:
    enum { CONN_MGMT_THREADID, IO_THREADID, RX_QUEUE_THREADID };

    struct ConnectionContext;

    struct TxQueueItem {
        ConnectionContext *ctx;
        ExtensionRegistry *extensions;
        // For cot events, msg is non-null. For raw messages, it is null
        // and the data representation is fixed to the raw message content
        CoTMessage *msg;
        const uint8_t *data;
        size_t dataLen;
        size_t bytesSent;
        bool protoSwapRequest;
        TxQueueItem();
        // Takes ownership of msg. Will be delete'd on implosion.
        TxQueueItem(ConnectionContext *ctx, ExtensionRegistry *extensions, 
                    CoTMessage *msg,
                    int protoVersion = 0) COMMO_THROW (std::invalid_argument);
        TxQueueItem(ConnectionContext *ctx,
                    const std::string &rawMessage);
        // Copy is ok
        ~TxQueueItem();
        void implode();
        
        // Throws if error serializing to the format given or if
        // this item was raw message based
        void reserialize(bool useAnyExtension,
                         const ExtensionIdSet &enabledExtensions,
                         int protoVersion = 0) COMMO_THROW (std::invalid_argument);
    };

    struct RxQueueItem {
        ConnectionContext *ctx;
        CoTMessage *msg;
        RxQueueItem();
        // Takes ownership of msg. Will be delete'd on implosion.
        RxQueueItem(ConnectionContext *ctx, CoTMessage *msg);
        // Copy is ok
        ~RxQueueItem();
        void implode();
    };

    typedef std::map<std::string, ConnectionContext *> ContextMap;
    typedef std::set<ConnectionContext *> ContextSet;
    typedef std::map<ResolverQueue::Request *, ConnectionContext *> ResolverMap;
    typedef std::deque<RxQueueItem> RxQueue;
    typedef std::deque<TxQueueItem> TxQueue;
    typedef std::set<CoTMessageType> MessageTypeSet;

    struct SSLConfiguration {
        // The client's certificate
        X509 *cert;

        // The private key for the client cert
        EVP_PKEY *key;

        // Checker for the user-supplied trust store.  Generally only
        // using a single cert in practice.
        SSLCertChecker *certChecker;

        // The ca certs, in order
        STACK_OF(X509) *caCerts;
        
        SSLConfiguration(const uint8_t *clientCert,
                size_t clientCertLen,
                const uint8_t *caCert, size_t caCertLen,
                const char *certPassword,
                const char *caCertPassword)
                    COMMO_THROW (SSLArgException);
        ~SSLConfiguration();
    };

    struct ConnectionContext : public StreamingNetInterface {
        // Set equal to max udp message size because that's the practical
        // limit on UDP cot messages and is large enough for any reasonably
        // sized cot message.  This is used to prevent memory exhaustion
        // and DoS by any naughty remote clients
        static const size_t rxBufSize = maxUDPMessageSize;

        CommoLogger *logger;
        ExtensionRegistry *extensions;
        const std::string &myPingUid;
        // The endpoint identifier - identical to superclass id, except
        // in string form and not char array.
        // Contains host/ip, protocol, and port # - everything unique about
        // the connection.
        const std::string remoteEndpoint;
        // String form of the remote endpoint hostname - no port
        // or protocol info.
        // Set to empty string if the remote endpoint was specified
        // as an IP and thus is pre-resolved (remoteEndpointAddr is valid)
        std::string remoteEndpointHostname;
        // The remote port number
        uint16_t remotePort;
        // Endpoint address including port - NULL if Context has
        // remoteEndpointHostname that is not yet resolved/needs resolution
        // Protect with contextMutex
        NetAddress *remoteEndpointAddr;
        MessageTypeSet broadcastCoTTypes;
        netinterfaceenums::StreamingTransport transport;

        // Only valid in "needs resolution" state - this is the request
        // object in the resolver
        ResolverQueue::Request *resolverRequest;

        // In "needs resolution" state: time when connection will next be re-attempted (following resolution)
        // In "down" state: time when connection will next be re-attempted
        // In "up" state: next time to send a "ping"
        CommoTime retryTime;

        // Only used in "up" state. Time at which we last received valid
        // data
        CommoTime lastRxTime;
        
        // Valid when "up" only; this is empty otherwise. Protected by main
        // upMutex
        TxQueue txQueue;
        // Valid when "up"; indicates if this connection's tx queue is
        // protobuf (>0) or xml (0). Protected by main upMutex
        int txQueueProtoVersion;
        // Valid when "up"; indicates the server states it supports receipt of
        // any and all extensions.
        // Usage within the txQueue is pending on state of 
        // txQueueProtoVersion. Protected by main upMutex.
        bool supportsAllExtensions;
        // Valid when "up"; indicates the extensions the server claims to
        // support. Not populated if "supportsAnyExtension" for obvious
        // reasons. Usage within the txQueue is pending on state of 
        // txQueueProtoVersion. Protected by main upMutex.
        ExtensionIdSet txExtensions;
        // Valid when "up"; if true, the IO thread should terminate
        // the connection as soon as possible and attempt to reconnect.
        // Protected by main upMutex.
        bool resetRequested;
        
        // Next 2 valid only when "up" and only on io thread
        uint8_t rxBuf[rxBufSize];
        size_t rxBufStart;
        size_t rxBufOffset;

        // All proto* state vars valid only when "up" and only on io thread
        typedef enum {
            // Impl depends on ordering here!
            // Don't reorder without verifying impl!
            PROTO_XML_NEGOTIATE, // XML for now, negotiation may happen
            PROTO_WAITRESPONSE,  // Sent a request for proto, waiting on reply
            PROTO_XML_ONLY,  // XML only from here on out
            PROTO_HDR_MAGIC, // Expecting next byte to be header's magic #
            PROTO_HDR_LEN,   // Looking for length varint
            PROTO_DATA       // Reading in data
        } ProtoDecodeState;
        // Protobuf decoder state
        ProtoDecodeState protoState;
        unsigned int protoMagicSearchCount;
        // Valid only when protoState == PROTO_DATA; length of protobuf msg
        size_t protoLen;
        // Only ever true in PROTO_WAITRESPONSE - true if the request
        // was actually sent (not still in tx queue) and we are truly
        // awaiting reply now.
        bool protoBlockedForResponse;
        // Valid only for protoState is...
        //    PROTO_XML_NEGOTIATE - time when we no longer look
        //                          for proto version support message
        //    PROTO_WAITRESPONSE - time when we give up waiting for response
        CommoTime protoTimeout;

        // Must be holding relevant mutexes. Give clearIo as true to clean all
        // I/O related members to default state (as if freshly disconnected)
        virtual void reset(CommoTime nextConnTime, bool clearIo);
        virtual SSLConfiguration *getSSLConfig();

        // Connection in "needs resolution" state: Always NULL
        // Connection in "down" state: non-NULL if connection is in progress,
        //                             NULL if connection not in progress.
        // Connection in "up" state. Always non-null; used for i/o
        virtual Socket *getSocket() = 0;
        // Only invoked on connection thread.
        // Create socket and issue any needed connect call for the socket
        // Returns true if socket created AND connect call completes immediately
        // Returns false if socket created but connect is ongoing
        // throws if error occurs
        virtual bool connectionInitSocket() COMMO_THROW (SocketException) = 0;
        // Only invoked on connection thread, assumes that
        // connectionInitSocket() already called and did not error,
        // and that connection and/or post-connect handshake has yet
        // to complete.
        // Returns true if connection in progress in background, false
        // if connection is complete and now awaiting post-connect handshake
        // completion.  "connection" here is base socket connection.
        virtual bool connectionInProgress() { return true; }
        // Only invoked on connection thread.
        // Assumes underlying socket is fully connected.
        // Do any post-connect setup that needs to occur. Return true only when
        // post-connect handshaking is complete.
        // If throws, caller is assumed to be calling reset() or delete
        // to cleanup (implementation need not do cleanup
        // already done in reset)
        virtual bool connectionDoPost(bool *rebuildSelect)
                                COMMO_THROW (SocketException) { return true; }
        // Connection thread only, return true if post-connect needs
        // readability on socket. CANNOT RETURN TRUE IF BASE CONNECT IS NOT
        // YET COMPLETED
        virtual bool connectionPostWantsRead() { return false; }
        // Connection thread only, return true if post-connect needs
        // writability on socket. CANNOT RETURN TRUE IF BASE CONNECT IS NOT
        // YET COMPLETED
        virtual bool connectionPostWantsWrite() { return false; }
        // IO thread only
        // Return true if io needs writability on socket
        virtual bool ioWantsWrite();
        // Attempt to read up to 'len' bytes into data.
        // Returns actual number of bytes read
        // Can be 0 if no data is available.  Throws on any error.
        // Invalid to call if connection not "up"
        virtual size_t ioRead(uint8_t *data, size_t len) COMMO_THROW (SocketException) = 0;
        virtual bool ioReadReady(NetSelector *selector) = 0;
        virtual size_t ioWrite(const uint8_t *data, size_t len) COMMO_THROW (SocketException) = 0;
        // Write internally buffered/protocol management data as needed.
        virtual void ioWriteFlush() COMMO_THROW (SocketException) { }
        virtual bool ioWriteReady(NetSelector *selector) = 0;
        
        // true if this context type has expiration times and expects
        // handleExpirations to be called periodically.
        // this is protocol level expirations, not to be confused with
        // streaming-level timeouts/expirations.
        virtual bool hasExpiration() { return false; }
        // If protocol expiration time has lapsed, handle it. Return
        // milliseconds from now until next expiration time.
        virtual long handleExpirations() COMMO_THROW (SocketException) { throw SocketException(netinterfaceenums::ERR_INTERNAL, "Not implemented"); }
        // Push a ping message on to the txQueue - assumes caller
        // has necessary thread safety to modify tx queue
        void txPushPing();
        

        virtual ~ConnectionContext();

    protected:
        ConnectionContext(CommoLogger *logger,
                ExtensionRegistry *extensions,
                const std::string &myPingUid,
                const std::string &epString,
                const std::string &epAddrString,
                uint16_t port,
                NetAddress *ep,
                netinterfaceenums::StreamingTransport transport);
        // If username/password given, generate authMessage,
        // else leave authMessage as-is.
        // throws on error.
        void generateAuthDoc(std::string *authMessage,
                             const std::string &myuid,
                             const char *username,
                             const char *password)
                             COMMO_THROW (SSLArgException);
    private:
        COMMO_DISALLOW_COPY(ConnectionContext);

    };

    struct TcpConnectionContext : public ConnectionContext {
        // See getSocket() for NULL/non-NULL meanings in various states
        TcpSocket *socket;
        
        TcpConnectionContext(CommoLogger *logger,
                ExtensionRegistry *extensions,
                const std::string &myPingUid,
                const std::string &epString,
                const std::string &epAddrString,
                uint16_t port,
                NetAddress *ep);

        virtual bool connectionInitSocket() COMMO_THROW (SocketException);
        virtual size_t ioRead(uint8_t *data, size_t len) COMMO_THROW (SocketException);
        virtual bool ioReadReady(NetSelector *selector);
        virtual size_t ioWrite(const uint8_t *data, size_t len) COMMO_THROW (SocketException);
        virtual bool ioWriteReady(NetSelector *selector);
        virtual void reset(CommoTime nextConnTime, bool clearIo);
        virtual Socket *getSocket();

    protected:
        TcpConnectionContext(CommoLogger *logger,
                ExtensionRegistry *extensions,
                const std::string &myPingUid,
                const std::string &epString,
                const std::string &epAddrString,
                uint16_t port,
                NetAddress *ep,
                netinterfaceenums::StreamingTransport transport);
        virtual ~TcpConnectionContext();
    };

    struct SSLConnectionContext : public TcpConnectionContext {
        // Same as outer class member of same name
        // Shared across all SSLConnectionContext's in owning object
        SSL_CTX *sslCtx;

        // Initialized as needed
        SSL *ssl;

        typedef enum {
            WANT_NONE,
            WANT_READ,
            WANT_WRITE
        } SSLWantState;

        // The tx state of the ssl connection:
        // When the holding ConnectionContext is in a "connecting" state:
        //    WANT_NONE signifies no outstanding ssl connection
        //    WANT_READ/WANT_WRITE means an outstanding ssl connection
        //            needs read/write
        // When the holding ConnectionContext is in "up" state:
        //    WANT_NONE signifies no ongoing write operation
        //    WANT_READ/WANT_WRITE signifies that transmit operation
        //        is in progress, but needs data to read/write
        SSLWantState writeState;

        // The rx state of the ssl connection:
        // When the holding ConnectionContext is in a "connecting" state:
        //    WANT_NONE is the only used and legal value
        // When the holding ConnectionContext is in "up" state:
        //    WANT_NONE signifies no ongoing receive operation
        //    WANT_READ/WANT_WRITE signifies that receive operation
        //        is in progress, but needs data to read/write
        SSLWantState readState;
        
        SSLConfiguration *sslConfig;

        // Zero length if auth is not in use
        std::string authMessage;

        // Flag to track if fatal SSL-level error was seen. Controls
        // how teardown of this context is performed, particularly
        // if SSL_shutdown should be attempted
        bool fatallyErrored;

        SSLConnectionContext(CommoLogger *logger,
                ExtensionRegistry *extensions,
                const std::string &myPingUid,
                const std::string &epString,
                const std::string &epAddrString,
                uint16_t port,
                NetAddress *ep, const std::string &uid,
                SSL_CTX *sslCtx, const uint8_t *clientCert,
                size_t clientCertLen,
                const uint8_t *caCert, size_t caCertLen,
                const char *certPassword,
                const char *caCertPassword,
                const char *username, const char *password)
                    COMMO_THROW (SSLArgException);
        ~SSLConnectionContext();

        virtual bool connectionInProgress();
        virtual bool connectionDoPost(bool *rebuildSelect) COMMO_THROW (SocketException);
        virtual bool connectionPostWantsRead();
        virtual bool connectionPostWantsWrite();

        virtual bool ioWantsWrite();
        virtual size_t ioRead(uint8_t *data, size_t len) COMMO_THROW (SocketException);
        virtual bool ioReadReady(NetSelector *selector);
        virtual size_t ioWrite(const uint8_t *data, size_t len) COMMO_THROW (SocketException);
        virtual bool ioWriteReady(NetSelector *selector);
        virtual void reset(CommoTime nextConnTime, bool clearIo);
        virtual SSLConfiguration *getSSLConfig();

    };
    
    class StreamingQuicConnection : public QuicConnection {
    public:
        StreamingQuicConnection(CommoLogger *logger,
                                const NetAddress &localAddr,
                                const NetAddress &remoteEndpointAddr,
                                float connTimeoutSec,
                                SSLConfiguration *sslConfig)
                                COMMO_THROW(std::invalid_argument);
        virtual ~StreamingQuicConnection();
        
        virtual bool handleExpiration(UdpSocket *socket) COMMO_THROW (SocketException);
        bool isHandshakeComplete() COMMO_THROW (SocketException);
        // Write stream data. NULL/0 for flushing internal data, if any
        // Returns number of bytes of stream data written, 0 for flushes.
        size_t write(UdpSocket *socket, const uint8_t *data, size_t dataLen) COMMO_THROW(SocketException);
        // Read stream data into data up to dataLen bytes.
        // Internal buffer is emptied first; if space remains, new
        // packets are read from socket and passed through quic stack.
        // Stops when dataLen bytes are populated, or new packets are
        // available/should no longer be processed immediately. 
        // Leftovers when byte limit is hit are internally
        // buffered for later retrieval.
        // Returns # of bytes written, or 0 if all packets exhausted and
        // no data was found.
        // NULL/0 data/dataLen causes packets to be read until would block OR until
        // handshake completes, internally buffering any encountered
        // stream data.
        // Might also cause non-stream data to be written as well
        size_t read(UdpSocket *socket, const NetAddress &localAddr,
                    uint8_t *data, size_t dataLen) COMMO_THROW(SocketException);
        
        // true if readable data is buffered
        bool hasBufferedData();

    protected:
        virtual bool isTxSourceDone() const;
        virtual bool txSourceFill(uint8_t *buf, size_t *len, int *errCode);
        virtual bool receivedData(const uint8_t *data, size_t dataLen,
                                  bool finFlagged);
        virtual bool wantsBiDir();
        virtual void handshakeError(netinterfaceenums::NetInterfaceErrorCode errCode,
                                    const char *info);
        virtual bool handshakeComplete(const uint8_t *alpn,
                                       unsigned int alpnLen);

    private:
        netinterfaceenums::NetInterfaceErrorCode handshakeErr;
        const char *handshakeErrInfo;
        bool handshakeIsComplete;
        const uint8_t *curTxBuf;
        size_t curTxLen;
        size_t curTxWritten;
        uint8_t *curRxBuf;
        size_t curRxLen;
        size_t curRxRead;
        static const size_t rxStreamBufSize = STREAM_FLOW_CTRL_BYTES;
        uint8_t rxStreamBuf[rxStreamBufSize];
        size_t rxStreamBufLen;
        
    };

    struct QuicConnectionContext : public ConnectionContext {
        UdpSocket *socket;
        NetAddress *localAddr;
        StreamingQuicConnection *quicConn;

        SSLConfiguration *sslConfig;

        // Zero length if auth is not in use
        std::string authMessage;

        QuicConnectionContext(CommoLogger *logger,
                ExtensionRegistry *extensions,
                const std::string &myPingUid,
                const std::string &epString,
                const std::string &epAddrString,
                uint16_t port,
                NetAddress *ep, const std::string &uid,
                const uint8_t *clientCert,
                size_t clientCertLen,
                const uint8_t *caCert, size_t caCertLen,
                const char *certPassword,
                const char *caCertPassword,
                const char *username, const char *password)
                    COMMO_THROW (SSLArgException);
        ~QuicConnectionContext();

        virtual SSLConfiguration *getSSLConfig();
        virtual Socket *getSocket();

        virtual bool connectionInitSocket() COMMO_THROW (SocketException);
        virtual bool connectionInProgress();
        virtual bool connectionDoPost(bool *rebuildSelect) COMMO_THROW (SocketException);
        virtual bool connectionPostWantsRead();
        virtual bool connectionPostWantsWrite();

        virtual bool ioWantsWrite();
        virtual size_t ioRead(uint8_t *data, size_t len) COMMO_THROW (SocketException);
        virtual bool ioReadReady(NetSelector *selector);
        virtual size_t ioWrite(const uint8_t *data, size_t len) COMMO_THROW (SocketException);
        virtual void ioWriteFlush() COMMO_THROW (SocketException);
        virtual bool ioWriteReady(NetSelector *selector);
        virtual bool hasExpiration() { return true; }
        // If protocol expiration time has lapsed, handle it. Return
        // milliseconds from now until next expiration time.
        virtual long handleExpirations() COMMO_THROW (SocketException);
        virtual void reset(CommoTime nextConnTime, bool clearIo);

    };
    


    CommoLogger *logger;
    ExtensionRegistry *extensions;
    ResolverQueue *resolver;
    float connTimeoutSec;
    bool monitor;

    SSL_CTX *sslCtx;

    ContextMap contexts;
    thread::RWMutex contextMutex;

    bool ioNeedsRebuild;
    ContextSet upContexts;
    thread::Mutex upMutex;
    ContextSet downContexts;
    bool downNeedsRebuild;
    thread::Mutex downMutex;
    // protected by contextMutex write lock or read lock + downMutex
    ResolverMap resolutionContexts;
    
    std::string myuid;
    std::string myPingUid;

    RxQueue rxQueue;
    thread::Mutex rxQueueMutex;
    thread::CondVar rxQueueMonitor;

    std::set<InterfaceStatusListener *> ifaceListeners;
    thread::Mutex ifaceListenersMutex;

    std::set<StreamingMessageListener *> listeners;
    thread::Mutex listenersMutex;

    COMMO_DISALLOW_COPY(StreamingSocketManagement);
    void connectionThreadProcess();
    void ioThreadProcess();
    void recvQueueThreadProcess();
    void resolutionThreadProcess();

    void sendPing(ConnectionContext *ctx);
    void convertTxToProtoVersion(ConnectionContext *ctx, int protoVersion);
    bool checkProtoTimeout(ConnectionContext *ctx, const CommoTime &nowTime);
    bool protoNegotiation(ConnectionContext *ctx, CoTMessage *msg);
    bool dispatchRxMessage(ConnectionContext *ctx, size_t len, bool isXml);
    bool scanStreamData(ConnectionContext *ctx, size_t nNewBytes);

    std::string getEndpointString(const char *addr, int port,
                              netinterfaceenums::StreamingTransport transport);

    void fireInterfaceChange(ConnectionContext *ctx, bool up);
    void fireInterfaceErr(ConnectionContext *ctx,
                          netinterfaceenums::NetInterfaceErrorCode errCode);

};

}
}
}

#endif /* IMPL_STREAMINGSOCKETMANAGEMENT_H_ */
