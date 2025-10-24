#define __STDC_LIMIT_MACROS
#include "tcpsocketmanagement.h"
#include "takmessage.h"
#include "internalutils.h"
#include "commothread.h"

#include <limits.h>
#include <string.h>

using namespace atakmap::commoncommo;
using namespace atakmap::commoncommo::impl;
using namespace atakmap::commoncommo::impl::thread;


namespace {
    const int LISTEN_BACKLOG = 15;

    const char *THREAD_NAMES[] = {
        "cmotcp.io", 
        "cmotcp.rxq",
        "cmotcp.txerr"
    };
}


TcpSocketManagement::TcpSocketManagement(CommoLogger *logger, 
                                 ContactUID *ourUid,
                                 ExtensionRegistry *extensions,
                                 CoTSendFailureListenerSet *failureListeners,
                                 thread::Mutex *failureListenersMutex) :
        PeerMessagingBase(logger, ourUid, extensions, THREAD_NAMES,
                          failureListeners, failureListenersMutex),
        txCrypto(NULL),
        inboundContexts(),
        inboundNeedsRebuild(false),
        clientContexts(),
        txContexts(),
        txNeedsRebuild(false),
        txMutex()
{
    startThreads();
}

TcpSocketManagement::~TcpSocketManagement()
{
    terminateResolver();
    stopThreads();

    // With the threads all joined, we can remove everything safely
    // Clean the queues first
    InboundCtxSet::iterator inbIter;
    for (inbIter = inboundContexts.begin(); 
                inbIter != inboundContexts.end(); ++inbIter) {
        delete *inbIter;
    }

    ClientCtxSet::iterator clIter;
    for (clIter = clientContexts.begin(); 
                clIter != clientContexts.end(); ++clIter) {
        delete *clIter;
    }

    TxCtxSet::iterator txIter;
    for (txIter = txContexts.begin(); 
                txIter != txContexts.end(); ++txIter) {
        destroyTxCtx(*txIter);
    }

    if (txCrypto)
        delete txCrypto;
}

TcpInboundNetInterface *TcpSocketManagement::addInboundInterface(int port)
{
    WriteLock lock(globalIOMutex);
    
    if (port > UINT16_MAX || port < 0)
        return NULL;
    uint16_t sport = (uint16_t)port;

    InboundContext *newCtx = new InboundContext(sport);
    if (inboundContexts.find(newCtx) != inboundContexts.end()) {
        delete newCtx;
        return NULL;
    }
    
    try {
        newCtx->initSocket();
        InternalUtils::logprintf(logger, CommoLogger::LEVEL_INFO, "Successfully created socket to listen on TCP port %d at initial addition", port);
    } catch (SocketException &) {
        InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "Failed to create socket to listen on TCP port %d during initial addition - will retry", port);
    }
    
    inboundContexts.insert(newCtx);
    inboundNeedsRebuild = true;
    return newCtx;
}

CommoResult TcpSocketManagement::removeInboundInterface(
                                       TcpInboundNetInterface *iface)
{
    WriteLock lock(globalIOMutex);
    
    InboundContext *ctx = (InboundContext *)iface;
    InboundCtxSet::iterator iter = inboundContexts.find(ctx);
    if (iter == inboundContexts.end())
        return COMMO_ILLEGAL_ARGUMENT;
    
    inboundContexts.erase(ctx);
    delete ctx;
    inboundNeedsRebuild = true;
    return COMMO_SUCCESS;
}

void TcpSocketManagement::setCryptoKeys(const uint8_t *authKey,
                                        const uint8_t *cryptoKey)
{
    setRxCryptoKeys(authKey, cryptoKey);
    {
        Lock txLock(txMutex);
        
        if (txCrypto) {
            delete txCrypto;
            txCrypto = NULL;
        }
        if (authKey && cryptoKey)
            txCrypto = new MeshNetCrypto(logger, cryptoKey, authKey);
    }
}

void TcpSocketManagement::ioThreadProcess()
{
    std::vector<Socket *> writeSocks;
    std::vector<Socket *> readSocks;
    InboundCtxSet inboundErroredCtxs;
    CommoTime lowErrorTime = CommoTime::ZERO_TIME;
    TxCtxSet curTxSet;

    while (!threadShouldStop(IO_THREADID)) {
        ReadLock lock(globalIOMutex);
        bool resetSelect = false;
        
        if (!inboundNeedsRebuild && 
                    !inboundErroredCtxs.empty() &&
                    CommoTime::now() > lowErrorTime)
            inboundNeedsRebuild = true;

        if (inboundNeedsRebuild) {
            CommoTime nowTime = CommoTime::now();
            resetSelect = true;
            inboundErroredCtxs.clear();
            readSocks.clear();
            InboundCtxSet::iterator inbIter;
            for (inbIter = inboundContexts.begin(); 
                         inbIter != inboundContexts.end(); ++inbIter) {
                InboundContext *ctx = *inbIter;
                if (!ctx->socket && nowTime > ctx->retryTime) {
                    try {
                        ctx->initSocket();
                        InternalUtils::logprintf(logger, CommoLogger::LEVEL_INFO, "Successfully created socket to listen on TCP port %d", (int)ctx->localPort);
                    } catch (SocketException &) {
                        InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "Failed to create socket to listen on TCP port %d - will retry", (int)ctx->localPort);
                        ioThreadResetInboundCtx(ctx, false);
                    }
                }
                
                if (!ctx->socket) {
                    if (inboundErroredCtxs.empty() || 
                            ctx->retryTime < lowErrorTime)
                        lowErrorTime = ctx->retryTime;
                    
                    inboundErroredCtxs.insert(ctx);
                } else {
                    readSocks.push_back(ctx->socket);
                    if (!ctx->upEventFired) {
                        fireIfaceStatus(ctx, true);
                        ctx->upEventFired = true;
                    }
                }
            }
            
            ClientCtxSet::iterator clientIter;
            for (clientIter = clientContexts.begin();
                         clientIter != clientContexts.end();
                         ++clientIter) {
                ClientContext *ctx = *clientIter;
                readSocks.push_back(ctx->socket);
            }
            
            inboundNeedsRebuild = false;
        }
        
        {
            Lock txLock(txMutex);
            if (txNeedsRebuild) {
                writeSocks.clear();
                curTxSet.clear();
                resetSelect = true;
                TxCtxSet::iterator txIter = txContexts.begin();
                while (txIter != txContexts.end()) {
                    TcpTxContext *ctx = *txIter;
                    TxCtxSet::iterator curIter = txIter;
                    txIter++;

                    if (!ctx->socket) {
                        try {
                            // Create socket, issue connect call
                            ctx->socket = new TcpSocket(
                                    ctx->destination->family, false);
                            if (!ctx->socket->connect(ctx->destination)) {
                                ctx->isConnecting = true;
                                ctx->timeout = CommoTime::now() + connTimeoutSec;
                            }
                            // ... else immediate connect success
                        } catch (SocketException &) {
                            queueTxErrAndKillCtx(ctx, "Failed to create transmission socket",
                                       false);
                            txContexts.erase(curIter);
                            continue;
                        }
                    }
                    writeSocks.push_back(ctx->socket);
                    curTxSet.insert(ctx);
                }
                
                txNeedsRebuild = false;
            }
        }
        
        bool killAllSockets = false;
        if (resetSelect) {
            try {
                selector.setSockets(&readSocks, &writeSocks);
            } catch (SocketException &) {
                killAllSockets = true;
                InternalUtils::logprintf(logger, 
                    CommoLogger::LEVEL_ERROR,
                    "tcp main io hit max socket capacity");
            }
        }


        try {
            if (!killAllSockets && !selector.doSelect(250)) {
                // Timeout
                CommoTime nowTime = CommoTime::now();
                TxCtxSet::iterator txIter;
                for (txIter = curTxSet.begin();
                                 txIter != curTxSet.end(); ++txIter) {
                    TcpTxContext *ctx = *txIter;
                    if (ctx->isConnecting && nowTime > ctx->timeout) {
                        Lock txLock(txMutex);
                        queueTxErrAndKillCtx(ctx, "Connection timed out", true);
                    }
                }
                continue;
            }

        } catch (SocketException &) {
            // odd. Force a rebuild
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "tcp main io select failed");
            killAllSockets = true;
        }



        // Look at our incoming clients *first*, before looking for
        // new clients as it could grow our client set to something NOT
        // in the select set
        ClientCtxSet::iterator clientIter = clientContexts.begin();
        while (clientIter != clientContexts.end()) {
            ClientContext *ctx = *clientIter;
            ClientCtxSet::iterator curIter = clientIter;
            clientIter++;

            if (killAllSockets) {
                delete ctx;
                clientContexts.erase(curIter);
                inboundNeedsRebuild = true;

            } else if (selector.getLastReadState(ctx->socket) == NetSelector::READABLE) {
                bool abort = false;
                try {
                    while (true) {
                        size_t n = ctx->bufLen - ctx->len;
                        if (!n) {
                            if (ctx->bufLen > RX_CLIENT_MAX_DATA_LEN) {
                                abort = true;
                                throw SocketException(netinterfaceenums::ERR_OTHER,
                                                      "Client sent excessive amount of data");
                            }
                            n = 5 * 1024;
                            ctx->growCapacity(n);
                        }
                        n = ctx->socket->read(ctx->data + ctx->len, n);
                        if (!n)
                            break;
                        ctx->len += n;
                    }
                } catch (SocketException &) {
                    // Something went awry or client disconnected.
                    // If we got any data, pass it on
                    if (!abort)
                        ioThreadQueueRx(ctx);
                    delete ctx;
                    clientContexts.erase(curIter);
                    inboundNeedsRebuild = true;
                }
            }
        }

        for (InboundCtxSet::iterator inbIter = inboundContexts.begin(); 
                     inbIter != inboundContexts.end(); ++inbIter) {
            InboundContext *ctx = *inbIter;
            bool ctxErr = killAllSockets;
            if (!ctxErr && ctx->socket && selector.getLastReadState(ctx->socket) == NetSelector::READABLE) {
                try {
                    while (true) {
                        NetAddress *clientAddr = NULL;
                        TcpSocket *clientSock = ctx->socket->accept(&clientAddr);
                        if (!clientSock)
                            break;

                        ClientContext *cctx = new ClientContext(clientSock,
                                                                clientAddr,
                                                                ctx->endpoint);
                        clientContexts.insert(cctx);
                        inboundNeedsRebuild = true;
                    }
                } catch (SocketException &) {
                    ctxErr = true;
                }
            }
            if (ctxErr)
                ioThreadResetInboundCtx(ctx, true);
        }


        TxCtxSet::iterator txIter;
        for (txIter = curTxSet.begin();
                         txIter != curTxSet.end(); ++txIter) {
            TcpTxContext *ctx = *txIter;
            if (killAllSockets) {
                Lock txLock(txMutex);
                queueTxErrAndKillCtx(ctx, "An unknown error occurred", true);

            } else {
                bool connDone = false;
                if (ctx->isConnecting) {
                    if (selector.getLastConnectState(ctx->socket) == NetSelector::WRITABLE) {
                        if (ctx->socket->isSocketErrored()) {
                            Lock txLock(txMutex);
                            queueTxErrAndKillCtx(ctx, "Connection to remote host failed", true);
                            ctx = NULL;
                        } else {
                            // connection just finished
                            ctx->isConnecting = false;
                            connDone = true;
                        }
                    } else if (CommoTime::now() > ctx->timeout) {
                        Lock txLock(txMutex);
                        queueTxErrAndKillCtx(ctx, "Connection timed out", true);
                        ctx = NULL;
                    }
                }
                
                if (ctx && !ctx->isConnecting && (connDone || selector.getLastWriteState(ctx->socket) == NetSelector::WRITABLE)) {
                    try {
                        while (ctx->dataLen > 0) {
                            size_t n = ctx->socket->write(ctx->data, ctx->dataLen);
                            if (n == 0)
                                // would block
                                break;
                            ctx->data += n;
                            ctx->dataLen -= n;
                        }
                        if (ctx->dataLen == 0) {
                            // all sent, all done
                            Lock txLock(txMutex);
                            killTxCtx(ctx, true);
                        }
                    } catch (SocketException &) {
                        Lock txLock(txMutex);
                        queueTxErrAndKillCtx(ctx, "I/O error transmitting data", true);
                    }
                }
            } 
        }
        
        if (killAllSockets)
            // Yield a short time to avoid spamming retries
            // after select system errors
            Thread::sleep(1000);
    }
}


PeerMessagingBase::TxContext *TcpSocketManagement::createTxContext(const std::string &host,
                  uint16_t port,
                  const CoTMessage *msg, 
                  ContactUID *ourUid,
                  ExtensionRegistry *extensions,
                  const ExtensionIdSet &enabledExtensions,
                  int protoVersion) COMMO_THROW (std::invalid_argument)
{
    Lock lock(txMutex);
    return new TcpTxContext(host, port, txCrypto, msg, ourUid, extensions,
                            enabledExtensions, protoVersion);
}

void TcpSocketManagement::txCtxReadyForIO(TxContext *txCtx)
{
    Lock lock(txMutex);
    txContexts.insert((TcpTxContext *)txCtx);
    txNeedsRebuild = true;
}

// Queue an error callback and kill the context
// Assumes holding of tx mutex
// if removeFromCtxSet, take from global ctxset and flag txrebuild
void TcpSocketManagement::queueTxErrAndKillCtx(TcpTxContext *ctx, const std::string &reason, bool removeFromCtxSet)
{
    queueTxErr(ctx, reason);
    killTxCtx(ctx, removeFromCtxSet);
}

// Kill off a tx context
// Assumes holding of tx mutex
// if removeFromCtxSet, take from global ctxset and flag txrebuild
void TcpSocketManagement::killTxCtx(TcpTxContext *ctx, bool removeFromCtxSet)
{
    if (removeFromCtxSet) {
        txContexts.erase(ctx);
        txNeedsRebuild = true;
    }
    destroyTxCtx(ctx);
}

// Close socket if non-null.
// Set retry time. flag inbound rebuild if flagReset true
// Assumes holding of globalIOMutex
void TcpSocketManagement::ioThreadResetInboundCtx(InboundContext *ctx,
                                                  bool flagReset)
{
    if (ctx->socket) {
        delete ctx->socket;
        ctx->socket = NULL;
    }
    if (ctx->upEventFired) {
        ctx->upEventFired = false;
        fireIfaceStatus(ctx, false);
    }
    ctx->retryTime = CommoTime::now() + INBOUND_RETRY_SECS;
    if (flagReset)
        inboundNeedsRebuild = true;
}

// Move any received data to rx queue; if nothing there, just
// no-op
void TcpSocketManagement::ioThreadQueueRx(ClientContext *ctx)
{
    if (ctx->len == 0)
        return;

    {
        RxQueueItem rx(ctx->clientAddr, ctx->endpoint, ctx->data, ctx->len);
        ctx->clearBuffers();
        queueRxItem(rx);
    }
}



TcpSocketManagement::InboundContext::InboundContext(uint16_t localPort) :
        TcpInboundNetInterface(localPort),
        socket(NULL),
        retryTime(CommoTime::ZERO_TIME),
        localPort(localPort),
        localAddr(NULL),
        endpoint(),
        upEventFired(false)
{
    localAddr = NetAddress::create(NA_TYPE_INET4, localPort);
    endpoint = "*:";
    endpoint += InternalUtils::intToString(localPort);
    endpoint += ":tcp";
}


TcpSocketManagement::InboundContext::~InboundContext()
{
    delete socket;
    delete localAddr;
}

void TcpSocketManagement::InboundContext::initSocket() COMMO_THROW (SocketException)
{
    try {
        socket = new TcpSocket(NA_TYPE_INET4, false);
        socket->bind(localAddr);
        socket->listen(LISTEN_BACKLOG);
    } catch (SocketException &e) {
        delete socket;
        socket = NULL;
        throw e;
    }
}


TcpSocketManagement::ClientContext::ClientContext(TcpSocket *socket,
                                                  NetAddress *clientAddr,
                                                  const std::string &endpoint) :
        clientAddr(clientAddr),
        endpoint(endpoint),
        socket(socket),
        data(NULL),
        len(0),
        bufLen(0)
{
}

TcpSocketManagement::ClientContext::~ClientContext()
{
    delete[] data;
    delete socket;
    delete clientAddr;
}

void TcpSocketManagement::ClientContext::growCapacity(size_t n)
{
    size_t newLen = bufLen + n;
    uint8_t *newBuf = new uint8_t[newLen];
    if (data) {
        memcpy(newBuf, data, len);
        delete[] data;
    }
    data = newBuf;
    bufLen = newLen;
}

// wipe buffer data so it isn't free'd
void TcpSocketManagement::ClientContext::clearBuffers()
{
    data = NULL;
    len = bufLen = 0;
    clientAddr = NULL;
}


TcpSocketManagement::TcpTxContext::TcpTxContext(const std::string &host,
                                          uint16_t port,
                                          MeshNetCrypto *crypto,
                                          const CoTMessage *msg,
                                          ContactUID *ourUid,
                                          ExtensionRegistry *extensions,
                                          const ExtensionIdSet &enabledExtensions,
                                          int protoVersion)
                                              COMMO_THROW (std::invalid_argument) :
        TxContext(host, port, crypto, msg, ourUid, extensions, enabledExtensions, protoVersion),
        socket(NULL),
        isConnecting(false),
        timeout(CommoTime::ZERO_TIME)
{
}

// close+delete socket if !NULL
TcpSocketManagement::TcpTxContext::~TcpTxContext()
{
    delete socket;
}

