#define __STDC_LIMIT_MACROS
#include "peermessagingbase.h"
#include "takmessage.h"
#include "internalutils.h"
#include "commothread.h"

#include <limits.h>
#include <string.h>

using namespace atakmap::commoncommo;
using namespace atakmap::commoncommo::impl;
using namespace atakmap::commoncommo::impl::thread;


namespace {
    const float DEFAULT_CONN_TIMEOUT_SEC = 20.0f;
}

const size_t PeerMessagingBase::RX_CLIENT_MAX_DATA_LEN = 500 * 1024;
const float PeerMessagingBase::INBOUND_RETRY_SECS = 60.0f;

PeerMessagingBase::PeerMessagingBase(CommoLogger *logger, 
                             ContactUID *ourUid,
                             ExtensionRegistry *extensions,
                             const char **threadNames,
                             CoTSendFailureListenerSet *sendFailureListeners,
                             thread::Mutex *sendFailureListenersMutex) :
        ThreadedHandler(3, threadNames),
        logger(logger),
        ourUid(ourUid),
        extensions(extensions),
        resolver(NULL),
        connTimeoutSec(DEFAULT_CONN_TIMEOUT_SEC),
        globalIOMutex(RWMutex::Policy_Fair),
        selector(),
        rxCrypto(NULL),
        resolverContexts(),
        resolverContextsMutex(),
        rxQueue(),
        rxQueueMutex(),
        rxQueueMonitor(),
        txErrQueue(),
        txErrQueueMutex(),
        txErrQueueMonitor(),
        listeners(),
        listenerMutex(),
        txErrListeners(sendFailureListeners),
        txErrListenerMutex(sendFailureListenersMutex),
        ifaceListeners(),
        ifaceListenerMutex()
{
    resolver = new ResolverQueue(logger, this, 5.0f, 1);
}

PeerMessagingBase::~PeerMessagingBase()
{
    while (!rxQueue.empty()) {
        RxQueueItem rxi = rxQueue.back();
        rxQueue.pop_back();
        rxi.implode();
    }
    
    ResolverReqMap::iterator resIter;
    for (resIter = resolverContexts.begin(); 
                resIter != resolverContexts.end(); ++resIter) {
        destroyTxCtx(resIter->second);
    }

    if (rxCrypto)
        delete rxCrypto;
}

void PeerMessagingBase::threadStopSignal(size_t threadNum)
{
    switch (threadNum) {
    case IO_THREADID:
        {
            break;
        }
    case TX_ERRQUEUE_THREADID:
        {
            Lock lock(txErrQueueMutex);
            txErrQueueMonitor.broadcast(lock);
        }
        break;
    case RX_QUEUE_THREADID:
        {
            Lock lock(rxQueueMutex);
            rxQueueMonitor.broadcast(lock);
            break;
        }
    }
}

void PeerMessagingBase::terminateResolver()
{
    delete resolver;
    resolver = NULL;
}

void PeerMessagingBase::setConnTimeout(float seconds)
{
    connTimeoutSec = seconds;
}

void PeerMessagingBase::sendMessage(const std::string &host, int port,
                                      const CoTMessage *msg,
                                      const ExtensionIdSet &enabledExtensions,
                                      int protoVersion)
                                               COMMO_THROW (std::invalid_argument)
{
    if (port > UINT16_MAX || port < 0)
        throw std::invalid_argument("Destination port out of range");
    uint16_t sport = (uint16_t)port;

    TxContext *ctx = createTxContext(host, sport, msg, ourUid, extensions,
                              enabledExtensions, protoVersion);

    queueNewTxContext(ctx);
}

void PeerMessagingBase::queueNewTxContext(TxContext *ctx)
{
    // See if host contains an IP string and does not need resolution
    NetAddress *destAddr = NetAddress::create(ctx->host.c_str(), ctx->destPort);

    if (destAddr) {
        ctx->destination = destAddr;
        txCtxReadyForIO(ctx);
    } else {
        // Needs name resolution
        Lock lock(resolverContextsMutex);
        ResolverReqMap resolverContexts;
        ResolverQueue::Request *r = resolver->queueForResolution(ctx->host);
        resolverContexts.insert(ResolverReqMap::value_type(r, ctx));
    }
}

bool PeerMessagingBase::resolutionComplete(
            ResolverQueue::Request *identifier,
            const std::string &hostAddr,
            NetAddress *result)
{
    // Find our context
    TxContext *ctx = NULL;
    {
        Lock lock(resolverContextsMutex);
        ResolverReqMap::iterator iter = resolverContexts.find(identifier);
        if (iter == resolverContexts.end())
            // should never happen
            return false;
        ctx = iter->second;
        resolverContexts.erase(iter);
    }

    if (!result) {
        queueTxErr(ctx, "Failed to resolve host");
        destroyTxCtx(ctx);
    } else {
        ctx->destination = result->deriveNewAddress(ctx->destPort);
        txCtxReadyForIO(ctx);
    }
    return false;
}


void PeerMessagingBase::addMessageReceiver(PeerMessageListener *receiver)
{
    Lock lock(listenerMutex);
    listeners.insert(receiver);
}

void PeerMessagingBase::removeMessageReceiver(PeerMessageListener *receiver)
{
    Lock lock(listenerMutex);
    listeners.erase(receiver);
}

void PeerMessagingBase::addInterfaceStatusListener(
        InterfaceStatusListener *listener)
{
    Lock lock(ifaceListenerMutex);
    ifaceListeners.insert(listener);
}

void PeerMessagingBase::removeInterfaceStatusListener(
        InterfaceStatusListener *listener)
{
    Lock lock(ifaceListenerMutex);
    ifaceListeners.erase(listener);
}

// Assumes holding of global IO mutex
void PeerMessagingBase::fireIfaceStatus(NetInterface *iface,
                                          bool up)
{
    std::set<InterfaceStatusListener *>::iterator iter;
    for (iter = ifaceListeners.begin(); iter != ifaceListeners.end(); ++iter) {
        InterfaceStatusListener *listener = *iter;
        if (up)
            listener->interfaceUp(iface);
        else
            listener->interfaceDown(iface);
    }
}

void PeerMessagingBase::threadEntry(
        size_t threadNum)
{
    switch (threadNum) {
    case IO_THREADID:
        ioThreadProcess();
        break;
    case RX_QUEUE_THREADID:
        recvQueueThreadProcess();
        break;
    case TX_ERRQUEUE_THREADID:
        txErrQueueThreadProcess();
        break;
    }
}

void PeerMessagingBase::recvQueueThreadProcess()
{
    while (!threadShouldStop(RX_QUEUE_THREADID)) {
        Lock qLock(rxQueueMutex);
        if (rxQueue.empty()) {
            rxQueueMonitor.wait(qLock);
            continue;
        }

        RxQueueItem qItem = rxQueue.back();
        rxQueue.pop_back();
        uint8_t *data = qItem.data;
        size_t dataLen = qItem.dataLen;
        bool decrypted = false;
        try {
            if (rxCrypto) {
                decrypted = rxCrypto->decrypt(&data, &dataLen);
                if (!decrypted)
                    throw std::invalid_argument("Unable to decrypt");
            }
            TakMessage takmsg(logger, data, dataLen, extensions, true, true);
            const CoTMessage *msg = takmsg.getCoTMessage();
            if (msg) {
                Lock listenerLock(listenerMutex);
                std::set<PeerMessageListener *>::iterator iter;
                for (iter = listeners.begin(); iter != listeners.end(); 
                                                                 ++iter) {
                    PeerMessageListener *l = *iter;
                    l->peerMessageReceived(qItem.sender, &qItem.endpoint, msg);
                }
            }
        } catch (std::invalid_argument &e) {
            // Drop this item
            CommoLogger::ParsingDetail detail{ data, dataLen, e.what() == NULL ? "" : e.what(), qItem.endpoint.c_str() };
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, CommoLogger::TYPE_PARSING, &detail, "Invalid CoT message received: %s", e.what() == NULL ? "unknown error" : e.what());
        }
        if (decrypted)
            delete[] data;
        qItem.implode();
    }
}


void PeerMessagingBase::txErrQueueThreadProcess()
{
    while (!threadShouldStop(TX_ERRQUEUE_THREADID)) {
        Lock qLock(txErrQueueMutex);
        if (txErrQueue.empty()) {
            txErrQueueMonitor.wait(qLock);
            continue;
        }

        TxErrQueueItem qItem = txErrQueue.back();
        txErrQueue.pop_back();
        {
            Lock listenerLock(*txErrListenerMutex);
            std::set<CoTSendFailureListener *>::iterator iter;
            for (iter = txErrListeners->begin(); iter != txErrListeners->end(); ++iter) {
                CoTSendFailureListener *l = *iter;
                l->sendCoTFailure(qItem.destinationHost.c_str(),
                                  qItem.destinationPort, qItem.errMsg.c_str());
            }
        }

    }
}

void PeerMessagingBase::setRxCryptoKeys(const uint8_t *authKey,
                                            const uint8_t *cryptoKey)
{
    Lock lock(rxQueueMutex);
    
    if (rxCrypto) {
        delete rxCrypto;
        rxCrypto = NULL;
    }
    if (authKey && cryptoKey)
        rxCrypto = new MeshNetCrypto(logger, cryptoKey, authKey);
}

// Queue an error callback
void PeerMessagingBase::queueTxErr(TxContext *ctx, const std::string &reason)
{
    Lock errLock(txErrQueueMutex);
    txErrQueue.push_front(TxErrQueueItem(ctx->host, ctx->destPort, reason.c_str()));
    txErrQueueMonitor.broadcast(errLock);
}

void PeerMessagingBase::destroyTxCtx(TxContext *ctx)
{
    delete ctx;
}

void PeerMessagingBase::queueRxItem(const RxQueueItem &rx)
{
    Lock lock(rxQueueMutex);
    rxQueue.push_front(rx);
    rxQueueMonitor.broadcast(lock);
}







PeerMessagingBase::RxQueueItem::RxQueueItem(
        NetAddress *sender, const std::string &endpoint,
        uint8_t *data,
        size_t nData) :   sender(sender),
                          endpoint(endpoint),
                          data(data),
                          dataLen(nData)
{
}

PeerMessagingBase::RxQueueItem::~RxQueueItem()
{
}

void PeerMessagingBase::RxQueueItem::implode()
{
    delete sender;
    delete[] data;
}


PeerMessagingBase::TxErrQueueItem::TxErrQueueItem(
        const std::string &host,
        int port,
        const std::string &errMsg) :
                destinationHost(host),
                destinationPort(port),
                errMsg(errMsg)
{
}

PeerMessagingBase::TxErrQueueItem::~TxErrQueueItem()
{
}


PeerMessagingBase::TxContext::TxContext(const std::string &host,
                                        uint16_t port,
                                        const uint8_t *data, 
                                        size_t dataLen) :
        destination(NULL),
        host(host),
        destPort(port),
        data(NULL),
        origData(NULL),
        dataLen(dataLen)
{
    origData = new uint8_t[dataLen];
    memcpy(origData, data, dataLen);
    this->data = origData;
}

PeerMessagingBase::TxContext::TxContext(const std::string &host,
                                          uint16_t port,
                                          MeshNetCrypto *crypto,
                                          const CoTMessage *msg,
                                          ContactUID *ourUid,
                                          ExtensionRegistry *extensions,
                                          const ExtensionIdSet &enabledExtensions,
                                          int protoVersion)
                                              COMMO_THROW (std::invalid_argument) :
        destination(NULL),
        host(host),
        destPort(port),
        data(NULL),
        origData(NULL),
        dataLen(0)
{
    CoTMessage msgCopy(*msg);
    msgCopy.setEndpoints(ENDPOINT_NONE, "", NULL);
    protoVersion = TakMessage::checkProtoVersion(protoVersion);
    if (protoVersion) {
        TakMessage takmsg(msg->getLogger(), &msgCopy, ourUid, extensions, true);
        dataLen = takmsg.serializeAsProtobuf(protoVersion, &origData,
                                             TakMessage::HEADER_TAKPROTO,
                                             true, true,
                                             extensions, false, 
                                             enabledExtensions);
    } else {
        dataLen = msgCopy.serialize(&origData);
    }
    if (crypto) {
        try {
            uint8_t *d = origData;
            crypto->encrypt(&origData, &dataLen);
            delete[] d;
        } catch (std::invalid_argument &e) {
            delete[] origData;
            origData = NULL;
            throw e;
        }
    }
    data = origData;
}

PeerMessagingBase::TxContext::~TxContext()
{
    delete destination;
    delete[] origData;
}


