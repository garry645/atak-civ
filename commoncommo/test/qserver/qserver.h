#include "ngtcp2/ngtcp2.h"
#include "ngtcp2/ngtcp2_crypto.h"
#include "openssl/ssl.h"

#include <string>
#include <set>
#include <deque>

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
    sockaddr_storage destAddrStorage;
    sockaddr *destAddr;
    socklen_t destAddrLen;

    uint8_t *buf;
    // Offset we need to send from still
    uint8_t *data;
    // *total* length from base of valid data
    size_t len;
    
    QuicTxBuf(const sockaddr *destAddr, socklen_t addrLen, uint8_t *data, size_t len);
    ~QuicTxBuf();
};




class ServerConn {        
public:
    static const uint64_t STREAM_FLOW_CTRL_BYTES = 128 * 1024;
    // Operate as server to incoming client.
    ServerConn(const sockaddr *localAddr, socklen_t localAddrLen,
            sockaddr *clientAddr, socklen_t clientAddrLen,
            uint32_t version,
            const ngtcp2_cid &scidFromClient,
            const ngtcp2_cid &dcidFromClient,
            const char *certFile,
            const char *keyFile,
            const char *caFile,
            const char *xmlLogPath);
    virtual ~ServerConn();
    
    ngtcp2_tstamp getExpTime() const { return expTime; }

    // true for all good, false if connection should be abandoned
    bool handleExpiration(int fd);
    
    // transmit any queued packets and any outstanding update packets
    // true for all good, false if connection should be abandoned
    bool transmitPkts(int fd);
    
    // true for all good, false if connection should be abandoned
    virtual bool processPkt(int fd,
                            ngtcp2_pkt_hd *hdr,
                            const sockaddr *localAddr,
                            socklen_t localAddrLen,
                            const sockaddr *sourceAddr,
                            socklen_t sourceAddrLen,
                            const uint8_t *data, size_t len);

    // true if this connection expects to receive packets with given SCID
    bool hasSCID(const std::string &scid);

    // true if has buffered data waiting on socket queue to send out asap
    bool hasTxData() const { return !txBufs.empty(); }

private:
    bool txSourceFill(uint8_t *buf, size_t *len, int *errCode);
    bool handleClosed(bool operationOk);

    // enter the closing state. sends a close packet and updates the
    // expiration time to reflect close period timeout.
    // false if connection should be terminated immediately, true to keep
    // going for now and come back at expiration time
    bool enterClosing(int fd, const ngtcp2_ccerr &err);

private:
    std::string curSAMsg;
    size_t curSAOffset;
    ngtcp2_tstamp nextSATime;

    FILE *xmlLogFile;
    SSL_CTX *sslCtx;
    SSL *ssl;
    ngtcp2_crypto_conn_ref ngConnRef;

    ngtcp2_conn *ngConn;
    QuicState state;
    bool streamOpen;
    int64_t streamId;
    
    // our valid scids
    std::set<std::string> scids;
    
    // quic packets ready to send on wire, but buffered because
    // socket queue was full at send time. Oldest on front.
    std::deque<QuicTxBuf *> txBufs;
    
    // Stream data to send.
    // Used as circular buffer for data to be sent; we keep our copy
    // because data must be held/buffered until ack'd by the quic stack
    uint8_t *txBuffer;
    // Size of txBuffer
    size_t txBufferSize;
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
    void ngapiErrToCCErr(ngtcp2_ccerr *ccerr, ngtcp2_ssize apiErr);

    // write stream data as needed. no-op in close/drain states.
    // false if connection should be terminated immediately, true if ok
    bool writeStreams(int fd);
    // send a quic-formatted packet
    // True if packet sent, false if it was buffered due to blocked socket
    // Arguments EXCEPT addr are copied if needed after call
    bool sendPkt(int fd, const sockaddr *addr, socklen_t addrLen,
                 uint8_t *pkt, size_t len);
    void destroyInternals();
    
    void logData(const uint8_t *data, size_t dataLen);

    /////////
    // *Cb methods are quic stack (ngtcp2) callbacks; userData for 
    // all of these is the ServerConn instance
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
    static int streamOpenCb(ngtcp2_conn *conn, int64_t streamId,
                            void *userData);
    static int streamCloseCb(ngtcp2_conn *conn, uint32_t flags, 
                             int64_t streamId, uint64_t appErrCode,
                             void *userData, void *streamUserData);
    static void quicStackLogCb(void *userData, const char *format, ...);
    static ngtcp2_conn *getConnFromRefCb(ngtcp2_crypto_conn_ref *connRef);
};


