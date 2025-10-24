#ifndef IMPL_QUICMANAGEMENT_H_
#define IMPL_QUICMANAGEMENT_H_

#include "peermessagingbase.h"
#include "quicconnection.h"
#include "netsocket.h"
#include "cotmessage.h"
#include "commoresult.h"
#include "commologger.h"
#include "commotime.h"
#include "cryptoutil.h"
#include "commothread.h"
#include "ngtcp2/ngtcp2.h"
#include "ngtcp2/ngtcp2_crypto.h"

namespace atakmap {
namespace commoncommo {
namespace impl
{

class QuicManagement : public PeerMessagingBase
{
public:
    class FileTransferBuffer {
    public:
        FileTransferBuffer(size_t bufSize, thread::Mutex *mutex,
                                           thread::CondVar *cond);
        ~FileTransferBuffer();

        /////////////////////////////////////////////////////////
        // READ SIDE: all of these must be invoked holding mutex
        // from QuicManagement::getFileTransferSync()

        // 0 if nothing available; use isAtEOF() to determine if more
        // might still arrive. Returns actual num bytes read.
        size_t read(uint8_t *dest, size_t len);
        // true if has data to read, is at EOF, or write side is errored
        bool isReadable();
        // true if writer is done producing data *and* reader has
        // drained buffer. Also true if write side errored
        bool isAtEOF();
        // true if reader has ceased
        bool isReadDone();
        // sets reader indication it is done; once invoked,
        // reader can no longer access
        void setReadDone();
        // true if writer has ceased due to error
        bool isWriteErrored();

        // END READ SIDE
        /////////////////////////////////////////////////////////

        // Returns actual num bytes written, or 0 if no 
        // space and/or read is done
        // Write 0 bytes to signal EOF.
        // If something was written and the internal buffer went from
        // below low water mark to above, signals the condvar
        size_t write(const uint8_t *from, size_t len);
        // set writer error state; signals to condvar on set
        void setWriteErrored();

    private:
        size_t bufSize;
        // absolute offset of full data stream read so far
        size_t readOff;
        // absolute offset of full data stream written to buffer so far
        size_t writeOff;
        
        // circular buffer, size is bufSize
        uint8_t *buf;
        bool writeAtEOF;
        bool writeErrored;
        bool readDone;
        thread::Mutex *mutex;
        thread::CondVar *cond;
        
        COMMO_DISALLOW_COPY(FileTransferBuffer);
    };


    QuicManagement(CommoLogger *logger,
                   ContactUID *ourUid, ExtensionRegistry *extensions,
                   FileIOProviderTracker *ioProviderFactory,
                   CoTSendFailureListenerSet *failureListeners,
                   thread::Mutex *failureListenersMutex,
                   std::map<int, std::string> *mpTransferFiles,
                   thread::Mutex *mpTransferFilesMutex);
    virtual ~QuicManagement();

    QuicInboundNetInterface *addInboundInterface(int port, const uint8_t *certData,
                                                 size_t certLen, const char *certPassword,
                                                 CommoResult *result);
    CommoResult removeInboundInterface(QuicInboundNetInterface *iface);
    // true if any configured inbound interfaces
    bool hasInboundInterface();

    // Initiate file transfer using tak file protocol for file id given
    // on given host whose quic endpoint is on given port.
    // Return NULL if cannot create.
    FileTransferBuffer *initFileTransfer(const std::string &host, int port,
                                         const std::string &fileId);
    // Get shared file transfer synchronization objects;  these
    // are used to signal events from writes on all FileTransferBuffers
    // for readers
    void getFileTransferSync(thread::Mutex **mutex, thread::CondVar **cond);
    

private:
    class InboundContext;
    class ConnectionContext;
    struct IBPortComp
    {
        bool operator()(const InboundContext * const &a, const InboundContext * const &b) const
        {
            return a->port < b->port;
        }
    };
    typedef std::set<InboundContext *, IBPortComp> InboundCtxSet;
    typedef std::set<ConnectionContext *> ConnectionCtxSet;


    class InboundContext : public QuicInboundNetInterface
    {
    public:
        InboundContext(QuicManagement *owner, uint16_t localPort);
        ~InboundContext();

        void reset();
        CommoResult initSSLParams(const uint8_t *certData, size_t certLen,
                                  const char *certPassword);
        void initSocket() COMMO_THROW (SocketException);
        // When no legit expiration, returns UINT64_MAX
        ngtcp2_tstamp getExpiration() const;
        void ioRead() COMMO_THROW (SocketException);
        void ioWrite() COMMO_THROW (SocketException);
        void handleExpirations();
        bool wantsTx() const { return !txConnections.empty(); }
        UdpSocket *getSocket() { return socket; }

        uint16_t getLocalPort() { return localPort; }
        const CommoTime &getRetryTime() { return retryTime; }
        bool wasUpFired() { return upEventFired; }
        void setUpHasFired(bool fired) { upEventFired = fired; }
        
    private:
        QuicManagement *owner;
        X509 *cert;
        EVP_PKEY *privKey;
        STACK_OF(X509) *certChain;
        UdpSocket *socket;
        // when to retry init when socket is down
        CommoTime retryTime;
        // if socket is up, this is lowest quic stack expiration time of
        // all active connections
        ngtcp2_tstamp lowestExpTime;

        // all active connections        
        ConnectionCtxSet connections;
        // subset of above - all connections with queued data to transmit
        // and waiting for socket queue to free up
        ConnectionCtxSet txConnections;

        const uint16_t localPort;
        NetAddress *localAddr; // includes port

        // our local endpoint        
        std::string endpoint;
        
        bool upEventFired;

        COMMO_DISALLOW_COPY(InboundContext);
        void resetConnections();
        void recomputeLowestExpiration();
    };


    class QuicTxContext : public TxContext
    {
    public:
        // For file transfer
        QuicTxContext(QuicManagement *owner,
                  const std::string &host, uint16_t port,
                  const std::string &fileId);
        // For CoT messages
        QuicTxContext(QuicManagement *owner,
                  const std::string &host, uint16_t port,
                  const CoTMessage *msg, 
                  ContactUID *ourUid,
                  ExtensionRegistry *extensions,
                  const ExtensionIdSet &enabledExtensions,
                  int protoVersion) COMMO_THROW (std::invalid_argument);
        ~QuicTxContext();

        
        void init() COMMO_THROW (std::invalid_argument, SocketException);

        // Shuts down quic stack, socket, and most other internal state
        // leaving only any active FileTransferBuffer (for file transfers)
        void shutdownIO();
        
        // When no legit expiration, returns UINT64_MAX
        ngtcp2_tstamp getExpiration() const;
        
        // next 3:
        // throws for fatal errors, returns true if transmission still ongoing
        // false if transmission completed and connection closing has been
        // completed
        bool ioRead() COMMO_THROW (SocketException);
        bool ioWrite() COMMO_THROW (SocketException);
        bool handleExpiration() COMMO_THROW (SocketException);
        // true if has packets waiting to send due to full socket queue
        bool wantsTx() const;
        UdpSocket *getSocket() { return socket; }

        // If created for file transfer, this gets the FileTransferBuffer
        // to which received file data is written
        FileTransferBuffer *getFileTransferBuffer() { return fileTransferBuf; }


    private:
        QuicManagement *owner;
        UdpSocket *socket;
        ConnectionContext *qCtx;
        NetAddress *localAddr;
        
        // NULL for fixed cot tx, non-NULL for file transfers
        FileTransferBuffer *fileTransferBuf;

        COMMO_DISALLOW_COPY(QuicTxContext);

        bool handleFailure(bool operationOk) COMMO_THROW (SocketException);
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

    
    class ConnectionContext : public QuicConnection
    {
    public:
        // Operate as server to incoming client.
        // Auto-switches between cot and file transfer
        // depending on wire-negotiated application protocol (alpn)
        ConnectionContext(QuicManagement *owner,
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
        // Sending single message (cot) if fileTransfer is null,
        // else a file transfer using tak file transfer protocol if non-null.
        // If file transfer, file id to request transfer is in dataToSend
        ConnectionContext(QuicManagement *owner,
                      const NetAddress &localAddr,
                      const NetAddress &serverAddr,
                      float connTimeoutSec,
                      uint8_t *dataToSend,
                      size_t dataLen,
                      FileTransferBuffer *fileTransfer)
                      COMMO_THROW(std::invalid_argument);
        virtual ~ConnectionContext();
        
        // true if we have a *complete* received message (cot only, always
        // false for file transfers)
        bool hasRxData() const { return rxComplete && !isFileSender && rxLen > 0; }

        // release the complete message (cot) received to given args.
        // returns size of message.  Only valid for server side messaging
        // transfers
        size_t releaseRxData(uint8_t **data,
                             NetAddress **lastRemoteAddr);

    protected:
        virtual bool isTxSourceDone() const;
        virtual bool txSourceFill(uint8_t *buf, size_t *len, int *errCode);
        virtual bool txFinishHandling(UdpSocket *socket, size_t bytesSent)
            COMMO_THROW(SocketException);
        virtual bool receivedData(const uint8_t *data, size_t dataLen, bool finFlagged);
        virtual bool postProcessPkt(UdpSocket *socket) 
                COMMO_THROW(SocketException);
        virtual bool wantsTxUni();
        virtual bool wantsBiDir();
        virtual bool isRemoteBiDirStreamAllowed();
        virtual bool handshakeComplete(const uint8_t *alpn, unsigned int alpnLen);

        
    private:
        QuicManagement *owner;

        // server only: true if file transfer protocol negotiated
        bool isFileSender;
        // server only: isFileSender only. File send source once client
        //              tells us what it wants and file transfer begins.
        std::shared_ptr<FileIOProvider> fileSendFileProvider;
        // server only: isFileSender only. Open file handle once transfer
        //              begins
        FileHandle *fileSendFile;
        
        // client only: buffer file transfer receives are written to.
        //              if non-NULL, client is set to receive file transfer
        //              else it is used for message transfer (cot)
        FileTransferBuffer *fileRecvBuffer;

        // server, file transfer: buffer where received file id is stored
        // server, cot messaging: buffer where message is stored
        // client: not used
        uint8_t *rxData;
        // length of valid data in rxData
        size_t rxLen;
        // size of rxData buffer (total)
        size_t rxBufLen;
        // server: true after incoming stream data is completed
        //         (cot message or file id)
        // client: not used
        bool rxComplete;

        // Stream data to send.
        // Client, File transfer: Outbound file transfer id. not owned by us.
        // Client, CoT message: Outbound message. not owned by us.
        // Server: not used
        uint8_t *txMsg;
        // Size of txData buffer
        size_t txMsgLen;
        // How far into txMsg we've supplied to QuicConnection thus far
        size_t txMsgOffset;

        // true if data to send is fully buffered in txData
        bool txSourceDone;

        COMMO_DISALLOW_COPY(ConnectionContext);

        void growCapacity(size_t n);
    };


    typedef std::set<QuicTxContext *> TxCtxSet;

    // Next 2 protected by globalIOMutex
    InboundCtxSet inboundContexts;
    bool inboundNeedsRebuild;

    // Next 2 protected by txMutex
    TxCtxSet txContexts;          // *active* TxContexts. No resolution pending
                                  // and no zombies, those are held in 
                                  // superclass and zombieTxContexts, respectively
    bool txNeedsRebuild;
    thread::Mutex txMutex;

    TxCtxSet zombieTxContexts;    // Set of txContexts that are file transfers
                                  // and done with our IO, but read side
                                  // of transferbuffer is still using
                                  // protected with zombieMutex
    thread::Mutex zombieMutex;    // lock after txMutex if getting both

    // shared state with mission package manager of active outbound transfer
    // files on local filesystem.
    // These support outbound file transfer fulfillments (where we act
    // as server)
    std::map<int, std::string> *mpTransferFiles;
    thread::Mutex *mpTransferFilesMutex;
    FileIOProviderTracker *ioProviderFactory;
    
    // Mutex and condvar used across all outgoing (client) transfer requests
    thread::Mutex clientFileTransferMutex;
    thread::CondVar clientFileTransferCond;

protected:
    virtual void ioThreadProcess();
    virtual TxContext *createTxContext(const std::string &host, uint16_t port,
                  const CoTMessage *msg, 
                  ContactUID *ourUid,
                  ExtensionRegistry *extensions,
                  const ExtensionIdSet &enabledExtensions,
                  int protoVersion) COMMO_THROW (std::invalid_argument);
    virtual void txCtxReadyForIO(TxContext *txCtx);
    virtual void queueTxErr(TxContext *ctx, const std::string &reason);
    virtual void destroyTxCtx(TxContext *ctx);

private:
    COMMO_DISALLOW_COPY(QuicManagement);

    void queueTxErrAndKillCtx(QuicTxContext *ctx, const std::string &reason, 
                              bool removeFromCtxSet);
    void killTxCtx(QuicTxContext *ctx, bool removeFromCtxSet);
    void ioThreadResetInboundCtx(InboundContext *ctx, bool flagReset);

};



}
}
}


#endif
