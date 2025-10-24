#ifndef IMPL_QUICCONNECTION_H_
#define IMPL_QUICCONNECTION_H_

#include "netsocket.h"
#include "commologger.h"
#include "ngtcp2/ngtcp2.h"
#include "ngtcp2/ngtcp2_crypto.h"

namespace atakmap {
namespace commoncommo {
namespace impl
{
    // Length we use for our quic SCID. Must be less than or equal to NGTCP2_MAX_CIDLEN
    // and also more than or equal to NGTCP2_MIN_INITIAL_DCIDLEN
    #define QUIC_SERV_SCID_LEN 16
    #if QUIC_SERV_SCID_LEN < NGTCP2_MIN_INITIAL_DCIDLEN || QUIC_SERV_SCID_LEN > NGTCP2_MAX_CIDLEN
    # error "QUIC_SERV_SCID_LEN invalid"
    #endif


    enum QuicState
    {
        Prehandshake,
        Normal,
        Draining,
        Closing,
        Closed   // quic stack signalled it's all done and conn should
                 // be terminated;  during this state no new packets are processed
                 // and no new packets are generated; we persist only to handle
                 // sending outbound queued packets when sendable
    };

    struct QuicTxBuf
    {
        NetAddress *destAddr;

        uint8_t *buf;
        // Offset we need to send from still
        uint8_t *data;
        // *total* length from base of valid data
        size_t len;
        
        // Adopts all
        QuicTxBuf(NetAddress *destAddr, uint8_t *data, size_t len);
        ~QuicTxBuf();
        // Clones data buffer, adopts address
        static QuicTxBuf *clonedCreate(NetAddress *destAddr,
                                       uint8_t *data, size_t len);
    private:
        COMMO_DISALLOW_COPY(QuicTxBuf);
    };
    
    
    class QuicConnection
    {
    public:
        static const uint64_t STREAM_FLOW_CTRL_BYTES = 128 * 1024;
        static ngtcp2_tstamp gents();
        
        // Operate as server to incoming client.
        // allowedAlpns is *not* copied and must remain valid as long as this
        // connection is! It should be in openssl "wire format", and length
        // should not include any null terminator
        QuicConnection(CommoLogger *logger,
                       const uint8_t *allowedAlpns,
                       size_t allowedAlpnsLen,
                       const NetAddress &localAddr,
                       NetAddress *clientAddr,
                       uint32_t version,
                       const ngtcp2_cid &scidFromClient,
                       const ngtcp2_cid &dcidFromClient,
                       float connTimeoutSec,
                       X509 *cert,
                       EVP_PKEY *privKey,
                       STACK_OF(X509) *supportCerts) 
                   COMMO_THROW(std::invalid_argument);
        // Operate as client to remote server
        QuicConnection(CommoLogger *logger,
                       const uint8_t *alpn,
                       size_t alpnLen,
                       const NetAddress &localAddr,
                       const NetAddress &serverAddr,
                       float connTimeoutSec,
                       float idleTimeoutSec,
                       X509 *cert = NULL,
                       EVP_PKEY *privKey = NULL,
                       SSLCertChecker *certChecker = NULL)
                   COMMO_THROW(std::invalid_argument);
        virtual ~QuicConnection();
        
        ngtcp2_tstamp getExpTime() const { return expTime; }

        // true for all good, false if connection should be abandoned
        // throws if socket-level I/O error occurs
        // May cause new value for getExpTime()
        virtual bool handleExpiration(UdpSocket *socket) COMMO_THROW (SocketException);
        
        // transmit any queued packets and any outstanding update packets
        // true for all good, false if connection should be abandoned
        // throws if socket-level I/O error occurs
        // May cause new value for getExpTime()
        bool transmitPkts(UdpSocket *socket) COMMO_THROW (SocketException);
        
        // true for all good, false if connection should be abandoned
        // Adopts source addr
        // May cause new value for getExpTime()
        virtual bool processPkt(UdpSocket *socket,
                                ngtcp2_pkt_hd *hdr,
                                const NetAddress &localAddr,
                                NetAddress *sourceAddr,
                                const uint8_t *data, size_t len)
                                             COMMO_THROW (SocketException);

        // true if this connection expects to receive packets with given SCID
        bool hasSCID(const std::string &scid);

        // true if has buffered data waiting on socket queue to send out asap
        bool hasTxData() const { return !txBufs.empty(); }

        // true if all data to send has been sent *and* acknowledged by remote
        // host
        bool isTxStreamComplete() const { return !streamOpen && isTxSourceDone() && txAckCount == txLen; }
        
        // Number of bytes actually sent to remote (or queued to send to remote)
        size_t getBytesSent() const { return txSent; }
        
        bool isServerMode() const { return isServer; }

    protected:
        const NetAddress *getRemoteClientAddr() { return remoteClientAddr; }

        // Handles error/finished conditions on all public API calls by
        // masking false (error/finished) returns until the tx
        // buffer is empty so that we know all data has been sent before
        // final shutdown.  If arg is false, we enter Closed state
        // and return true until tx is complete.
        bool handleClosed(bool operationOk);
        
        // enter the closing state. sends a close packet and updates the
        // expiration time to reflect close period timeout.
        // false if connection should be terminated immediately, true to keep
        // going for now and come back at expiration time
        bool enterClosing(UdpSocket *socket, const ngtcp2_ccerr &err)
                                             COMMO_THROW (SocketException);


        // Return true if either no source to read from yet, or the source
        // has been exhausted.
        virtual bool isTxSourceDone() const { return true; }

        // Fill buf with up to len bytes from tx data source.
        // len should be updated with number of bytes written.
        // On entry, len will never be 0. On exit, len of 0
        // indicates source presently has no data (but could later if
        // isTxSourceDone() still returns true!).
        // If an error has occurred that should cause the connection to enter
        // the closing state, false should be returned and errCode set to
        // the application protocol level error code to issue during closing.
        // If no error has occurred, len must be set appropriately and true
        // returned.
        // This will only be invoked when isTxSourceDone() returns false
        virtual bool txSourceFill(uint8_t *buf, size_t *len, int *errCode) { *len = 0; return true; }

        // Invoked whenever data is *queued and/or sent successfully*.  This
        // includes stream tx data AND quic overhead data for the connection.
        // Implmentation should handle triggering shutdown of connection
        // if all data has been sent out and no further data is to come in.
        // NOTE: bytesSent is the number of bytes sent or queued for send on
        // the tx stream. It is NOT the number of bytes ack'd!
        // Implementation can either do nothing and return true
        // (in which case the connection remains operating),  or
        // invoke enterClosing(), returning the result of such (in which
        // case the connection is in the closing state or terminal depending
        // on the returned value).
        // return true if connection remains valid (but possibly in
        // closing state), false if it has become terminal and should
        // be abandoned
        // (see same for enterClosing())
        virtual bool txFinishHandling(UdpSocket *socket, size_t bytesSent)
            COMMO_THROW(SocketException) { return true; }

        // Called when new stream data is available;  implementations should
        // copy off any data it wishes to hold on to and update any internal
        // tracking/state. Return true if everything is ok, false
        // if an error should be indicated to the quic stack to cause it to
        // terminate the connection.
        // finFlagged indicates the data was flagged with "fin" indicator, which
        // means no more data will arrive on this stream, but it does NOT
        // indicate that all that data has been remotely ack'd! Stream close
        // callback will indicate this.
        virtual bool receivedData(const uint8_t *data, size_t dataLen,
                                  bool finFlagged) { return true; }
        // Called after a received packet is successfully processed
        // Return true for all ok, false if connection should be torn down
        // (same as enterClosing())
        virtual bool postProcessPkt(UdpSocket *socket) 
                COMMO_THROW(SocketException) { return true; }
        // Called when stream is not open and an outbound unidirectional stream 
        // becomes available for use. Return true to indicate one should be
        // opened and used, false to ignore it
        virtual bool wantsTxUni() { return false; }
        // Called when stream is not open and a bidirectional stream 
        // becomes available for use. Return true to indicate one should be
        // opened and used, false to ignore it.
        virtual bool wantsBiDir() { return false; }
        // Called when stream is not open and a stream has been opened by
        // the remote end.  Determines if this new stream is allowed to be
        // bidirectional (true) or if the write side should be closed, 
        // turning it into a unidirectional receiving stream (false).
        // Server side only.
        virtual bool isRemoteBiDirStreamAllowed() { return false; }
        // Called when handshake completes.  alpn is chosen ALPN and is
        // not necessarily null terminated; alpnLen indicates byte length.
        // alpn may be NULL, in which case no ALPN was explicitly negotiated.
        // Return true if alpn is acceptable, false if not and connection
        // should be terminated.
        // Not called when handshake completes with an error,
        // see handshakeError()
        virtual bool handshakeComplete(const uint8_t *alpn,
                                       unsigned int alpnLen) { return true; }
        // Called when handshake completes with an error condition.
        // info is a static string and persists beyond the call;
        // it is never NULL
        virtual void handshakeError(netinterfaceenums::NetInterfaceErrorCode errCode,
                                    const char *info) {}
        

        CommoLogger *logger;
        
    private:
        SSL_CTX *sslCtx;
        SSL *ssl;
        ngtcp2_crypto_conn_ref ngConnRef;

        ngtcp2_conn *ngConn;
        QuicState state;
        bool newBidirStream;
        bool streamOpen;
        int64_t streamId;
        
        // client only: if non-NULL, verify server using this
        SSLCertChecker *certChecker;

        // server only: our valid scids
        std::set<std::string> scids;
        
        // server only: allowed set of alpns and length, owned by client
        const uint8_t *allowedAlpns;
        size_t allowedAlpnsLen;

        // server only: address of the remote client
        NetAddress *remoteClientAddr;

        // true if created as a server, false if client
        bool isServer;
        
        // quic packets ready to send on wire, but buffered because
        // socket queue was full at send time. Oldest on front.
        std::deque<QuicTxBuf *> txBufs;
        
        // Stream data to send.
        // Used as circular buffer for data to be sent; we keep our copy
        // because data must be held/buffered until ack'd by the quic stack
        uint8_t *txBuffer;
        // Size of txBuffer
        size_t txBufferSize;
        // true if data to send is fully buffered in txData
        bool txSourceDone;
        // Next 3 are absolute byte counts from start of full tx stream
        // Absolute length of valid buffered data so far in txBuffer
        size_t txLen;
        // Absolute length of txBuffer "sent" (Actually the amount sent on wire
        // OR queued in quic packets on txBufs)
        size_t txSent;
        // Absolute length of txBuffer acknowledged by remote endpoint
        size_t txAckCount;
        
        
        // State == close or drain: this is the time at which close or drain
        // state can end and connection should be terminated.
        // In other states, this is the quic stack expiration callback time
        ngtcp2_tstamp expTime;
        
        // State == close: quic-formatted connection close packet that was
        // sent to close connection
        uint8_t closePkt[NGTCP2_MAX_UDP_PAYLOAD_SIZE];
        // Valid length of above
        size_t closePktLen;


        COMMO_DISALLOW_COPY(QuicConnection);

        void ngapiErrToCCErr(ngtcp2_ccerr *ccerr, ngtcp2_ssize apiErr);

        // write stream data as needed. no-op in close/drain states.
        // false if connection should be terminated immediately, true if ok
        bool writeStreams(UdpSocket *socket) COMMO_THROW (SocketException);
        // send a quic-formatted packet
        // True if packet sent, false if it was buffered due to blocked socket
        // Arguments EXCEPT addr are copied if needed after call; addr
        // is adopted.
        bool sendPkt(UdpSocket *socket, NetAddress *addr,
                     uint8_t *pkt, size_t len) COMMO_THROW (SocketException);
        void destroyInternals();
        static void commonParamInit(ngtcp2_settings *settings, ngtcp2_transport_params *params, float connTimeoutSec, float idleTimeoutSec);

        /////////
        // *Cb methods are quic stack (ngtcp2) callbacks; userData for 
        // all of these is the QuicConnection instance
        static int getNewConnectionIdCb(ngtcp2_conn *conn, ngtcp2_cid *cid,
                                        uint8_t *token, size_t cidLen,
                                        void *userData);
        static int removeConnectionIdCb(ngtcp2_conn *conn,
                                        const ngtcp2_cid *cid,
                                        void *userData);
        static int handshakeCompleteCb(ngtcp2_conn *conn, void *userData);
        static int recvStreamDataCb(ngtcp2_conn *conn, uint32_t flags,
                                    int64_t streamId, uint64_t offset,
                                    const uint8_t *data, size_t dataLen,
                                    void *userData, void *streamUserData);
        static int ackedStreamDataCb(ngtcp2_conn *conn, int64_t streamId, 
                                    uint64_t offset, uint64_t dataLen,
                                    void *userData, void *streamUserData);
        static int extendMaxUniStreamCb(ngtcp2_conn *conn, uint64_t maxStreams,
                                        void *userData);
        static int extendMaxBidirStreamCb(ngtcp2_conn *conn, 
                                          uint64_t maxStreams,
                                          void *userData);
        static int streamOpenCb(ngtcp2_conn *conn, int64_t streamId,
                                void *userData);
        static int streamCloseCb(ngtcp2_conn *conn, uint32_t flags, 
                                 int64_t streamId, uint64_t appErrCode,
                                 void *userData, void *streamUserData);
        static int selectAlpnSSLCb(SSL *ssl,
                                   const unsigned char **out,
                                   unsigned char *outLen,
                                   const unsigned char *in,
                                   unsigned int inLen,
                                   void *arg);
        static void quicStackLogCb(void *userData, const char *format, ...);
        static ngtcp2_conn *getConnFromRefCb(ngtcp2_crypto_conn_ref *connRef);
    };

}
}
}

#endif
