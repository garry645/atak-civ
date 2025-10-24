#ifndef IMPL_PEERMESSAGINGBASE_H_
#define IMPL_PEERMESSAGINGBASE_H_

#include "netsocket.h"
#include "threadedhandler.h"
#include "cotmessage.h"
#include "commoresult.h"
#include "commologger.h"
#include "commotime.h"
#include "resolverqueue.h"
#include "cryptoutil.h"
#include "commothread.h"
#include <set>
#include <map>
#include <deque>

namespace atakmap {
namespace commoncommo {
namespace impl
{

class PeerMessageListener
{
public:
    // Parameters must be copied during event servicing if used after return
    virtual void peerMessageReceived(const NetAddress *sender, const std::string *endpointId, const CoTMessage *message) = 0;

protected:
    PeerMessageListener() {};
    virtual ~PeerMessageListener() {};

private:
    COMMO_DISALLOW_COPY(PeerMessageListener);
};


class PeerMessagingBase : public ThreadedHandler, public ResolverListener
{
public:
    static const size_t RX_CLIENT_MAX_DATA_LEN;
    static const float INBOUND_RETRY_SECS;

    typedef std::set<CoTSendFailureListener *> CoTSendFailureListenerSet;
    
    virtual ~PeerMessagingBase();


    void setConnTimeout(float seconds);
    // Uses protoVersion = 0 if supplied version not supported
    void sendMessage(const std::string &host, int port,
                     const CoTMessage *msg, 
                     const ExtensionIdSet &enabledExtensions,
                     int protoVersion) 
                     COMMO_THROW (std::invalid_argument);

    void addMessageReceiver(PeerMessageListener *receiver);
    void removeMessageReceiver(PeerMessageListener *receiver);
    CommoResult addCoTSendFailureListener(CoTSendFailureListener *listener);
    CommoResult removeCoTSendFailureListener(CoTSendFailureListener *listener);
    void addInterfaceStatusListener(InterfaceStatusListener *listener);
    void removeInterfaceStatusListener(InterfaceStatusListener *listener);
    
    // ResolverListener
    virtual bool resolutionComplete(ResolverQueue::Request *identifier,
                                    const std::string &hostAddr,
                                    NetAddress *result);

protected:
    PeerMessagingBase(CommoLogger *logger, ContactUID *ourUid,
                      ExtensionRegistry *extensions,
                      const char **threadNames,
                      CoTSendFailureListenerSet *failureListeners,
                      thread::Mutex *failureListenersMutex);

    struct TxContext
    {
        // Where's it going? Includes port
        // NULL if out for name resolution
        NetAddress *destination;
        std::string host;
        uint16_t destPort;

        // The actual data to send
        uint8_t *data;
        uint8_t *origData;
        size_t dataLen;

    protected:
        // Data is copied
        TxContext(const std::string &host, uint16_t port,
                  const uint8_t *data, 
                  size_t dataLen);
        TxContext(const std::string &host, uint16_t port,
                  MeshNetCrypto *crypto, const CoTMessage *msg, 
                  ContactUID *ourUid,
                  ExtensionRegistry *extensions,
                  const ExtensionIdSet &enabledExtensions,
                  int protoVersion) COMMO_THROW (std::invalid_argument);
    public:
        virtual ~TxContext();

    private:
        COMMO_DISALLOW_COPY(TxContext);
    };

    struct RxQueueItem
    {
        NetAddress *sender;
        std::string endpoint;
        uint8_t *data;
        size_t dataLen;

        RxQueueItem(NetAddress *sender, const std::string &endpoint,
                    uint8_t *data, size_t dataLen);
        // Copy is ok
        ~RxQueueItem();
        void implode();
    };
    
    

    enum { IO_THREADID, RX_QUEUE_THREADID, TX_ERRQUEUE_THREADID };

    // ThreadedHandler impl
    virtual void threadEntry(size_t threadNum);
    virtual void threadStopSignal(size_t threadNum);
    

    // Clean up resolver, stopping its processing thread
    void terminateResolver();


    virtual TxContext *createTxContext(const std::string &host, uint16_t port,
                  const CoTMessage *msg, 
                  ContactUID *ourUid,
                  ExtensionRegistry *extensions,
                  const ExtensionIdSet &enabledExtensions,
                  int protoVersion) COMMO_THROW (std::invalid_argument) = 0;
    void queueNewTxContext(TxContext *ctx);
    virtual void txCtxReadyForIO(TxContext *txCtx) = 0;
    virtual void queueTxErr(TxContext *ctx, const std::string &reason);
    virtual void destroyTxCtx(TxContext *ctx);

    void setRxCryptoKeys(const uint8_t *authKey,
                         const uint8_t *cryptoKey);
    void queueRxItem(const RxQueueItem &item);

    void fireIfaceStatus(NetInterface *iface, bool up);



private:
    struct TxErrQueueItem
    {
        const std::string destinationHost;
        const int destinationPort;
        const std::string errMsg;
        TxErrQueueItem(const std::string &host, 
                       int port,
                       const std::string &errMsg);
        // Copy ok
        ~TxErrQueueItem();
    };


    typedef std::map<ResolverQueue::Request *, TxContext *> ResolverReqMap;



protected:
    CommoLogger *logger;
    ContactUID *ourUid;
    ExtensionRegistry *extensions;
    ResolverQueue *resolver;
    float connTimeoutSec;

    // Protects all IO sockets across all contexts (aka the big select() mutex)
    // While this is a RW Mutex, the internal use of it is rather
    // non-traditional.  There is only ever one reader; this reader
    // holds the lock quite often while doing some long-running tasks, but
    // frequently and briefly relinquishes the lock to allow a writer to
    // interject and perform an exclusive operation.
    // Thus it is used basically like a regular Mutex but uses the reader
    // writer paradigm to allow prioritization to writers.  With a normal
    // Mutex, lock starvation of the "writing" calls was seen with the "read"
    // portion never giving up the mutex effectively (on some platforms,
    // notably Linux)
    thread::RWMutex globalIOMutex;

    NetSelector selector;
    
private:
    MeshNetCrypto *rxCrypto;   // lock on rxQueueMutex 

    ResolverReqMap resolverContexts;
    thread::Mutex resolverContextsMutex;


    // RX Queue - new items on front
    std::deque<RxQueueItem> rxQueue;
    thread::Mutex rxQueueMutex;
    thread::CondVar rxQueueMonitor;

    // TX Error Queue - new items on front
    std::deque<TxErrQueueItem> txErrQueue;
    thread::Mutex txErrQueueMutex;
    thread::CondVar txErrQueueMonitor;

    // Listeners
    std::set<PeerMessageListener *> listeners;
    thread::Mutex listenerMutex;

    // Error Listeners
    CoTSendFailureListenerSet *txErrListeners;
    thread::Mutex *txErrListenerMutex;

    // Interface status listeners
    std::set<InterfaceStatusListener *> ifaceListeners;
    thread::Mutex ifaceListenerMutex;

    COMMO_DISALLOW_COPY(PeerMessagingBase);

private:
    virtual void ioThreadProcess() = 0;
    void recvQueueThreadProcess();
    void txErrQueueThreadProcess();


};





}
}
}


#endif
