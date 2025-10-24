#define __STDC_LIMIT_MACROS
#include "quicmanagement.h"
#include "takmessage.h"
#include "internalutils.h"
#include "commothread.h"

#include "ngtcp2/ngtcp2_crypto_quictls.h"
#include "openssl/ssl.h"
#include "openssl/rand.h"

#include <limits.h>
#include <string.h>
#include <chrono>

using namespace atakmap::commoncommo;
using namespace atakmap::commoncommo::impl;
using namespace atakmap::commoncommo::impl::thread;

namespace {
    const char *THREAD_NAMES[] = {
        "cmoquic.io", 
        "cmoquic.rxq",
        "cmoquic.txerr"
    };
    
    const unsigned char ALPNS[] = {
        0x06, 't','a','k','c','o','t', 0x06, 't','a','k', 'f', 'd', 'p'
    };
    const unsigned char *TAKCOT_ALPN = ALPNS;
    const unsigned char *TAKFDP_ALPN = ALPNS + TAKCOT_ALPN[0] + 1;
    const uint64_t FILETRANSFER_LOWWATER =
            QuicConnection::STREAM_FLOW_CTRL_BYTES / 2;

    
    // takcot application protocol error codes
    enum {
        MSG_OK,
    };
    // takfdp application protocol error codes
    enum {
        FILEXFER_OK,
        FILEXFER_FILE_NOT_FOUND,
        FILEXFER_IO_ERR,
        FILEXFER_UNK_ERR,
    };
}
    


/***********************************************************************/
// QuicManagement ctor/dtor


QuicManagement::QuicManagement(CommoLogger *logger, 
                               ContactUID *ourUid,
                               ExtensionRegistry *extensions,
                               FileIOProviderTracker *ioProviderFactory,
                               CoTSendFailureListenerSet *failureListeners,
                               thread::Mutex *failureListenersMutex,
                               std::map<int, std::string> *mpTransferFiles,
                               thread::Mutex *mpTransferFilesMutex) :
        PeerMessagingBase(logger, ourUid, extensions, THREAD_NAMES,
                          failureListeners, failureListenersMutex),
        inboundContexts(),
        inboundNeedsRebuild(false),
        txContexts(),
        txNeedsRebuild(false),
        txMutex(),
        mpTransferFiles(mpTransferFiles),
        mpTransferFilesMutex(mpTransferFilesMutex),
        ioProviderFactory(ioProviderFactory)
{
    startThreads();
}

QuicManagement::~QuicManagement()
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

    TxCtxSet::iterator txIter;
    for (txIter = txContexts.begin(); 
                txIter != txContexts.end(); ++txIter) {
        delete *txIter;
    }
    
    for (txIter = zombieTxContexts.begin();
                txIter != zombieTxContexts.end(); ++txIter) {
        delete *txIter;
    }
}



/***********************************************************************/
// QuicManagement public api


QuicInboundNetInterface *QuicManagement::addInboundInterface(int port,
                              const uint8_t *certData,
                              size_t certLen, const char *certPassword,
                              CommoResult *result)
{
    if (port > UINT16_MAX || port < 0) {
        *result = COMMO_ILLEGAL_ARGUMENT;
        return NULL;
    }
    uint16_t sport = (uint16_t)port;

    InboundContext *newCtx = new InboundContext(this, sport);
    if (inboundContexts.find(newCtx) != inboundContexts.end()) {
        delete newCtx;
        if (result)
            *result = COMMO_ILLEGAL_ARGUMENT;
        return NULL;
    }
    CommoResult sslResult = newCtx->initSSLParams(certData, certLen, certPassword);
    if (sslResult != COMMO_SUCCESS) {
        delete newCtx;
        if (result)
            *result = sslResult;
        return NULL;
    }
    
    try {
        newCtx->initSocket();
        InternalUtils::logprintf(logger, CommoLogger::LEVEL_INFO, "Successfully created socket to listen on QUIC port %d at initial addition", port);
    } catch (SocketException &) {
        InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "Failed to create socket to listen on QUIC port %d during initial addition - will retry", port);
    }

    {    
        WriteLock lock(globalIOMutex);
        
        inboundContexts.insert(newCtx);
        inboundNeedsRebuild = true;
        if (result)
            *result = COMMO_SUCCESS;
        return newCtx;
    }
}

CommoResult QuicManagement::removeInboundInterface(
                                       QuicInboundNetInterface *iface)
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

bool QuicManagement::hasInboundInterface()
{
    ReadLock lock(globalIOMutex);
    return !inboundContexts.empty();
}

QuicManagement::FileTransferBuffer *QuicManagement::initFileTransfer(
                                     const std::string &host,
                                     int port,
                                     const std::string &fileId)
{
    if (port > UINT16_MAX || port < 0) {
        InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR,
                "invalid port %d given for quic file transfer init",
                port);
        return NULL;
    }
    if (fileId.size() == 0) {
        InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR,
                "invalid empty file id given for quic file transfer init");
        return NULL;
    }
    uint16_t sport = (uint16_t)port;

    QuicTxContext *ctx = new QuicTxContext(this, host, sport, fileId);

    queueNewTxContext(ctx);
    return ctx->getFileTransferBuffer();
}

void QuicManagement::getFileTransferSync(thread::Mutex **mutex,
                                         thread::CondVar **cond)
{
    *mutex = &clientFileTransferMutex;
    *cond = &clientFileTransferCond;
}


/***********************************************************************/
// QuicManagement impls of PeerMessagingBase methods



void QuicManagement::ioThreadProcess()
{
    std::vector<Socket *> inboundWriteSocks;
    std::vector<Socket *> inboundReadSocks;
    InboundCtxSet inboundWriteCtxs;
    bool inboundWriteNeedsRebuild = false;
    InboundCtxSet inboundErroredCtxs;
    CommoTime lowErrorTime = CommoTime::ZERO_TIME;

    std::vector<Socket *> outboundWriteSocks;
    std::vector<Socket *> outboundReadSocks;
    TxCtxSet curOutboundCtxs;
    TxCtxSet curOutboundWriteCtxs;
    bool outboundWriteNeedsRebuild = false;

    while (!threadShouldStop(IO_THREADID)) {
        // Clean up zombies first, before possibly creating new
        // zombies during this iteration that would be pointless
        // to iterate through as they'd probably not yet have had 
        // time to complete the read end of the pipe keeping them "alive"
        // and also because they aren't in any IO set and don't need
        // to hold IO lock
        {
            Lock zlock(zombieMutex);
            TxCtxSet::iterator zIter = zombieTxContexts.begin();
            while (zIter != zombieTxContexts.end()) {
                QuicTxContext *ctx = *zIter;
                TxCtxSet::iterator curIter = zIter;
                zIter++;
                if (ctx->getFileTransferBuffer()->isReadDone()) {
                    zombieTxContexts.erase(curIter);
                    // do a hard delete; we know it's ready
                    delete ctx;
                }
            }
        }

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
            inboundWriteCtxs.clear();
            inboundWriteNeedsRebuild = true;
            inboundReadSocks.clear();
            InboundCtxSet::iterator inbIter;
            for (inbIter = inboundContexts.begin(); 
                         inbIter != inboundContexts.end(); ++inbIter) {
                InboundContext *ctx = *inbIter;
                if (!ctx->getSocket() && nowTime > ctx->getRetryTime()) {
                    try {
                        ctx->initSocket();
                        InternalUtils::logprintf(logger, CommoLogger::LEVEL_INFO, "Successfully created socket to listen on QUIC port %d", (int)ctx->getLocalPort());
                    } catch (SocketException &) {
                        InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "Failed to create socket to listen on QUIC port %d - will retry", (int)ctx->getLocalPort());
                        ioThreadResetInboundCtx(ctx, false);
                    }
                }
                
                if (!ctx->getSocket()) {
                    if (inboundErroredCtxs.empty() || 
                            ctx->getRetryTime() < lowErrorTime)
                        lowErrorTime = ctx->getRetryTime();
                    
                    inboundErroredCtxs.insert(ctx);
                } else {
                    inboundReadSocks.push_back(ctx->getSocket());
                    if (ctx->wantsTx())
                        inboundWriteCtxs.insert(ctx);
                    if (!ctx->wasUpFired()) {
                        fireIfaceStatus(ctx, true);
                        ctx->setUpHasFired(true);
                    }
                }
            }
            
            inboundNeedsRebuild = false;
        }
        
        {
            Lock txLock(txMutex);
            if (txNeedsRebuild) {
                resetSelect = true;
                
                outboundReadSocks.clear();
                curOutboundCtxs.clear();
                curOutboundWriteCtxs.clear();
                outboundWriteNeedsRebuild = true;
                TxCtxSet::iterator txIter = txContexts.begin();
                while (txIter != txContexts.end()) {
                    QuicTxContext *ctx = *txIter;
                    TxCtxSet::iterator curIter = txIter;
                    txIter++;

                    if (!ctx->getSocket()) {
                        try {
                            // Create socket, issue connect call, init quic
                            ctx->init();
                        } catch (SocketException &ex) {
                            InternalUtils::logprintf(logger,
                                    CommoLogger::LEVEL_ERROR,
                                    "Quic Tx socket init failed %s",
                                    ex.what());
                            queueTxErrAndKillCtx(ctx,
                                    "Failed to create transmission socket",
                                    false);
                            txContexts.erase(curIter);
                            continue;
                        } catch (std::invalid_argument &ex) {
                            InternalUtils::logprintf(logger,
                                    CommoLogger::LEVEL_ERROR,
                                    "Quic Tx stack init failed %s",
                                    ex.what());
                            queueTxErrAndKillCtx(ctx,
                                    "Failed to create transmission stack",
                                    false);
                            txContexts.erase(curIter);
                            continue;
                        }
                    }
                    curOutboundCtxs.insert(ctx);
                    outboundReadSocks.push_back(ctx->getSocket());
                    if (ctx->wantsTx())
                        curOutboundWriteCtxs.insert(ctx);
                }
                
                txNeedsRebuild = false;
            }
        }

        // Handle inbound expirations
        ngtcp2_tstamp now = QuicConnection::gents();
        for (InboundCtxSet::iterator inbIter = inboundContexts.begin(); 
                     inbIter != inboundContexts.end(); ++inbIter) {
            InboundContext *ctx = *inbIter;
            if (ctx->getSocket() && now > ctx->getExpiration()) {
                 ctx->handleExpirations();
                 InboundCtxSet::iterator eiter = inboundWriteCtxs.find(ctx);
                 if (!ctx->wantsTx() && eiter != inboundWriteCtxs.end()) {
                     inboundWriteCtxs.erase(eiter);
                     inboundWriteNeedsRebuild = true;
                 } else if (ctx->wantsTx() && eiter == inboundWriteCtxs.end()) {
                     inboundWriteCtxs.insert(ctx);
                     inboundWriteNeedsRebuild = true;
                 }
            }
        }
        // Outbound expirations
        for (TxCtxSet::iterator outbIter = curOutboundCtxs.begin(); 
                     outbIter != curOutboundCtxs.end(); ++outbIter) {
            QuicTxContext *ctx = *outbIter;
            if (now > ctx->getExpiration()) {
                try {
                    if (!ctx->handleExpiration()) {
                        // all done and successfully sent
                        Lock txLock(txMutex);
                        killTxCtx(ctx, true);
                    }
                } catch (SocketException &) {
                    Lock txLock(txMutex);
                    queueTxErrAndKillCtx(ctx, "I/O error transmitting data",
                                         true);
                }
                // Update write side, but don't bother if we've changed overall
                // outbound set as it'll get rebuilt anyway
                if (!txNeedsRebuild) {
                    TxCtxSet::iterator eiter = curOutboundWriteCtxs.find(ctx);
                    if (!ctx->wantsTx() && eiter != curOutboundWriteCtxs.end()) {
                        curOutboundWriteCtxs.erase(eiter);
                        outboundWriteNeedsRebuild = true;
                    } else if (ctx->wantsTx() && eiter == curOutboundWriteCtxs.end()) {
                        curOutboundWriteCtxs.insert(ctx);
                        outboundWriteNeedsRebuild = true;
                    }
                }
            }
        }
        // If we lost any write contexts, we need to rebuild before proceeding
        // as our current sets now hold invalid contexts
        if (txNeedsRebuild)
            continue;

        if (inboundWriteNeedsRebuild) {
            resetSelect = true;
            inboundWriteSocks.clear();
            for (InboundCtxSet::iterator inbIter = inboundWriteCtxs.begin(); 
                         inbIter != inboundWriteCtxs.end(); ++inbIter) {
                InboundContext *ctx = *inbIter;
                // at this point inboundTxCtxs can contain only up contexts
                inboundWriteSocks.push_back(ctx->getSocket());
            }
            inboundWriteNeedsRebuild = false;
        }
        
        if (outboundWriteNeedsRebuild) {
            resetSelect = true;
            outboundWriteSocks.clear();
            for (TxCtxSet::iterator outbIter = curOutboundWriteCtxs.begin(); 
                         outbIter != curOutboundWriteCtxs.end(); ++outbIter) {
                QuicTxContext *ctx = *outbIter;
                outboundWriteSocks.push_back(ctx->getSocket());
            }
            outboundWriteNeedsRebuild = false;
        }
        
        bool killAllSockets = false;
        if (resetSelect) {
            std::vector<Socket *> writeSocks;
            std::vector<Socket *> readSocks;
            writeSocks.reserve(inboundWriteSocks.size() + outboundWriteSocks.size());
            readSocks.reserve(inboundReadSocks.size() + outboundReadSocks.size());
            writeSocks.insert(writeSocks.end(), inboundWriteSocks.begin(), inboundWriteSocks.end());
            readSocks.insert(readSocks.end(), inboundReadSocks.begin(), inboundReadSocks.end());
            writeSocks.insert(writeSocks.end(), outboundWriteSocks.begin(), outboundWriteSocks.end());
            readSocks.insert(readSocks.end(), outboundReadSocks.begin(), outboundReadSocks.end());
            try {
                selector.setSockets(&readSocks, &writeSocks);
            } catch (SocketException &) {
                killAllSockets = true;
                InternalUtils::logprintf(logger, 
                    CommoLogger::LEVEL_ERROR,
                    "quic main io hit max socket capacity");
            }
        }


        try {
            if (!killAllSockets && !selector.doSelect(250)) {
                // Timeout
                continue;
            }

        } catch (SocketException &) {
            // odd. Force a rebuild
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR,
                                     "quic main io select failed");
            killAllSockets = true;
        }


        for (InboundCtxSet::iterator inbIter = inboundContexts.begin(); 
                     inbIter != inboundContexts.end(); ++inbIter) {
            InboundContext *ctx = *inbIter;
            bool ctxErr = killAllSockets;
            if (!ctxErr && ctx->getSocket()) {
                InboundCtxSet::iterator txIter = inboundWriteCtxs.find(ctx);
                bool isTx = txIter != inboundWriteCtxs.end();
                bool ioPerformed = false;
                try {
                    if (selector.getLastReadState(ctx->getSocket()) == NetSelector::READABLE) {
                        ctx->ioRead();
                        ioPerformed = true;
                    }
                    if (isTx && selector.getLastWriteState(ctx->getSocket()) == NetSelector::WRITABLE) {
                        ctx->ioWrite();
                        ioPerformed = true;
                    }
                    // read or write can change tx status
                    if (ioPerformed) {
                        if (isTx && !ctx->wantsTx()) {
                            inboundWriteCtxs.erase(txIter);
                            inboundWriteNeedsRebuild = true;
                        } else if (!isTx && ctx->wantsTx()) {
                            inboundWriteCtxs.insert(ctx);
                            inboundWriteNeedsRebuild = true;
                        }
                    }
                } catch (SocketException &) {
                    ctxErr = true;
                }

            }
            if (ctxErr)
                ioThreadResetInboundCtx(ctx, true);
        }
        
        TxCtxSet::iterator outboundIter;
        for (outboundIter = curOutboundCtxs.begin();
                         outboundIter != curOutboundCtxs.end(); ++outboundIter) {
            QuicTxContext *ctx = *outboundIter;
            if (killAllSockets) {
                Lock txLock(txMutex);
                queueTxErrAndKillCtx(ctx, "An unknown error occurred", true);

            } else {
                bool ioPerformed = false;

                try {
                    bool done = false;
                    if (selector.getLastReadState(ctx->getSocket()) == NetSelector::READABLE) {
                        done = !ctx->ioRead();
                        ioPerformed = true;
                    }
                    TxCtxSet::iterator txIter = curOutboundWriteCtxs.find(ctx);
                    bool isTx = txIter != curOutboundWriteCtxs.end();
                    if (!done) {
                        if (isTx && selector.getLastWriteState(ctx->getSocket()) == NetSelector::WRITABLE) {
                            done = !ctx->ioWrite();
                            ioPerformed = true;
                        }
                    }

                    if (done) {
                        // all done and successfully sent
                        Lock txLock(txMutex);
                        killTxCtx(ctx, true);
                    } else if (ioPerformed && !txNeedsRebuild) {
                        if (isTx && !ctx->wantsTx()) {
                            curOutboundWriteCtxs.erase(txIter);
                            outboundWriteNeedsRebuild = true;
                        } else if (!isTx && ctx->wantsTx()) {
                            curOutboundWriteCtxs.insert(ctx);
                            outboundWriteNeedsRebuild = true;
                        }
                    }
                } catch (SocketException &) {
                    Lock txLock(txMutex);
                    queueTxErrAndKillCtx(ctx, "I/O error transmitting data", true);
                }
            } 
        }
        
        if (killAllSockets)
            // Yield a short time to avoid spamming retries
            // after select system errors
            Thread::sleep(1000);
    }
}


PeerMessagingBase::TxContext *QuicManagement::createTxContext(const std::string &host,
                  uint16_t port,
                  const CoTMessage *msg, 
                  ContactUID *ourUid,
                  ExtensionRegistry *extensions,
                  const ExtensionIdSet &enabledExtensions,
                  int protoVersion) COMMO_THROW (std::invalid_argument)
{
    return new QuicTxContext(this, host, port, msg, ourUid, extensions,
                             enabledExtensions, protoVersion);
}

void QuicManagement::txCtxReadyForIO(TxContext *txCtx)
{
    Lock lock(txMutex);
    txContexts.insert((QuicTxContext *)txCtx);
    txNeedsRebuild = true;
}

void QuicManagement::queueTxErr(TxContext *ctx, const std::string &reason)
{
    // Don't send tx errs for file transfers
    FileTransferBuffer *ftbuf = 
            ((QuicTxContext *)ctx)->getFileTransferBuffer();
    if (ftbuf) {
        ftbuf->setWriteErrored();
    } else {
        PeerMessagingBase::queueTxErr(ctx, reason);
    }
}

// Terminate a tx context
// This handles txContext's reclaimed during resolution (superclass)
// as well as our IO handling.  Unlike above, no mutex is guaranteed to be held
void QuicManagement::destroyTxCtx(TxContext *ctx)
{
    // We cannot destroy file transfer TxContext's if the read side
    // of the FTB is still active; instead put them on list to be destroyed
    // later. IO context (or destructor) will reclaim them when their time
    // comes
    QuicTxContext *qctx = (QuicTxContext *)ctx;
    FileTransferBuffer *ftbuf = qctx->getFileTransferBuffer();
    if (ftbuf && !ftbuf->isReadDone()) {
        Lock txLock(zombieMutex);
        qctx->shutdownIO();
        zombieTxContexts.insert(qctx);
    } else {
        delete ctx;
    }
}


/***********************************************************************/
// QuicManagement internal methods


// Queue an error callback and kill the context
// Assumes holding of tx mutex
// if removeFromCtxSet, take from global ctxset and flag txrebuild
void QuicManagement::queueTxErrAndKillCtx(QuicTxContext *ctx, const std::string &reason, bool removeFromCtxSet)
{
    queueTxErr(ctx, reason);
    killTxCtx(ctx, removeFromCtxSet);
}

// Kill off a tx context
// Assumes holding of tx mutex
// if removeFromCtxSet, take from global ctxset and flag txrebuild
void QuicManagement::killTxCtx(QuicTxContext *ctx, bool removeFromCtxSet)
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
void QuicManagement::ioThreadResetInboundCtx(InboundContext *ctx,
                                             bool flagReset)
{
    if (ctx->wasUpFired()) {
        ctx->setUpHasFired(false);
        fireIfaceStatus(ctx, false);
    }
    ctx->reset();
    if (flagReset)
        inboundNeedsRebuild = true;
}




/***********************************************************************/
// InboundContext


QuicManagement::InboundContext::InboundContext(QuicManagement *owner,
                                               uint16_t localPort) :
        QuicInboundNetInterface(localPort),
        owner(owner),
        cert(NULL),
        privKey(NULL),
        certChain(NULL),
        socket(NULL),
        retryTime(CommoTime::ZERO_TIME),
        lowestExpTime(UINT64_MAX),
        connections(),
        txConnections(),
        localPort(localPort),
        localAddr(NULL),
        endpoint(),
        upEventFired(false)
{
    localAddr = NetAddress::create(NA_TYPE_INET4, localPort);
    endpoint = "*:";
    endpoint += InternalUtils::intToString(localPort);
    endpoint += ":quic";
}

QuicManagement::InboundContext::~InboundContext()
{
    resetConnections();
    delete socket;
    delete localAddr;
    if (certChain)
        sk_X509_pop_free(certChain, X509_free);
    if (cert)
        X509_free(cert);
    if (privKey)
        EVP_PKEY_free(privKey);
}

void QuicManagement::InboundContext::reset()
{
    if (socket) {
        delete socket;
        socket = NULL;
    }
    resetConnections();
    retryTime = CommoTime::now() + INBOUND_RETRY_SECS;
    lowestExpTime = UINT64_MAX;
}

CommoResult QuicManagement::InboundContext::initSSLParams(
        const uint8_t *certData,
        size_t certLen,
        const char *certPassword)
{
    try {
        int nCaCerts;
        InternalUtils::readCert(certData, certLen,
                                certPassword,
                                &cert, 
                                &privKey, 
                                &certChain,
                                &nCaCerts);
    } catch (SSLArgException &ex) {
        return ex.errCode;
    }
    return COMMO_SUCCESS;
}

void QuicManagement::InboundContext::initSocket() COMMO_THROW (SocketException)
{
    socket = new UdpSocket(localAddr, false, false, false);
}

void QuicManagement::InboundContext::ioRead() COMMO_THROW (SocketException)
{
    const size_t buflen = 65536;
    uint8_t buf[buflen];
    bool newExpScan = false;
    int pktCount = 0;
    ConnectionCtxSet wSet;

    while (pktCount++ < 10) {
        NetAddress *sourceAddr = NULL;
        NetAddress *localAddr = NULL;
        ngtcp2_version_cid qversion;
        size_t len = buflen;

        try {
            socket->recvfrom(&localAddr, &sourceAddr, buf, &len);
        } catch (SocketWouldBlockException &) {
            break;
        }
        
        if (!localAddr) {
            // Cannot process without this
            delete sourceAddr;
            continue;
        }
            
        int r = ngtcp2_pkt_decode_version_cid(&qversion, buf, len,
                                              QUIC_SERV_SCID_LEN);
        if (r != 0) {
            InternalUtils::logprintf(owner->logger, CommoLogger::LEVEL_DEBUG,
                                     "Received quic version negotiation pkt; unsupported at this time! Ignoring packet");
            // We're not supporting version negotiation at this time
            delete sourceAddr;
            delete localAddr;
            continue;
        }
        
        // Look for existing connection by the packet's DCID
        std::string dcid((const char *)qversion.dcid, qversion.dcidlen);
        ConnectionContext *cctx = NULL;
        for (ConnectionCtxSet::iterator iter = connections.begin();
                         iter != connections.end(); ++iter) {
            ConnectionContext *ctx = *iter;
            if (ctx->hasSCID(dcid)) {
                cctx = ctx;
                break;
            }
        }

        ngtcp2_pkt_hd header;
        ngtcp2_pkt_hd *hdrIfNew = NULL;
        if (!cctx) {
            r = ngtcp2_accept(&header, buf, len);
            if (r != 0) {
                // invalid, drop it
                InternalUtils::logprintf(owner->logger, 
                                         CommoLogger::LEVEL_DEBUG,
                                         "quic inbound receives invalid packet for new connection");
                delete sourceAddr;
                delete localAddr;
                continue;
            }
            try {
                cctx = new ConnectionContext(
                    owner,
                    *localAddr,
                    sourceAddr,
                    header.version,
                    header.scid,
                    header.dcid,
                    owner->connTimeoutSec,
                    cert,
                    privKey,
                    certChain
                );
                connections.insert(cctx);
                hdrIfNew = &header;
            } catch (std::invalid_argument &ex) {
                InternalUtils::logprintf(owner->logger, 
                                         CommoLogger::LEVEL_DEBUG,
                                         "quic inbound receives invalid or packet for new connection (%s)",
                                         ex.what());
                delete sourceAddr;
                delete localAddr;
                continue;
            }
        }
        // If we make it here, any of the below actions could cause expiration
        // time update
        newExpScan = true;
        
        bool pRet;
        try {
            pRet = cctx->processPkt(socket, hdrIfNew, *localAddr, sourceAddr, buf, len);
        } catch (SocketException &) {
            delete localAddr;
            throw;
        }
        delete localAddr;
        if (cctx->hasRxData()) {
            uint8_t *d;
            NetAddress *addr;
            size_t dlen = cctx->releaseRxData(&d, &addr);
            
            RxQueueItem rx(addr, endpoint, d, dlen);
            owner->queueRxItem(rx);
        }
        
        if (pRet) {
            // may have data to send; do it after we finish this batch of
            // packets in case there is more to process so we can batch replies
            wSet.insert(cctx);
        } else {
            txConnections.erase(cctx);
            connections.erase(cctx);
            wSet.erase(cctx);
            delete cctx;
        }
    }
    for (ConnectionCtxSet::iterator iter = wSet.begin(); iter != wSet.end(); iter++) {
        ConnectionContext *cctx = *iter;
        if (cctx->transmitPkts(socket)) {
            if (cctx->hasTxData())
                txConnections.insert(cctx);
            else
                txConnections.erase(cctx);
        } else {
            txConnections.erase(cctx);
            connections.erase(cctx);
            delete cctx;
        }
    }
    if (newExpScan) {
        lowestExpTime = UINT64_MAX;
        for (ConnectionCtxSet::iterator iter = connections.begin(); iter != connections.end(); iter++) {
            ConnectionContext *cctx = *iter;
            ngtcp2_tstamp exp = cctx->getExpTime();
            if (exp < lowestExpTime)
                lowestExpTime = exp;
        }
    }
}

void QuicManagement::InboundContext::ioWrite() COMMO_THROW (SocketException)
{
    bool newExpScan = false;
    ConnectionCtxSet::iterator iter = txConnections.begin();
    while (iter != txConnections.end()) {
        ConnectionContext *cctx = *iter;
        ConnectionCtxSet::iterator eiter = iter;
        iter++;

        if (cctx->transmitPkts(socket)) {
            if (!cctx->hasTxData()) {
                newExpScan = true;
                txConnections.erase(eiter);
            } // else still has buffered data, come back later
        } else {
            newExpScan = true;
            txConnections.erase(eiter);
            connections.erase(cctx);
            delete cctx;
        }
    }
    if (newExpScan) {
        lowestExpTime = UINT64_MAX;
        for (iter = connections.begin(); iter != connections.end(); iter++) {
            ConnectionContext *cctx = *iter;
            ngtcp2_tstamp exp = cctx->getExpTime();
            if (exp < lowestExpTime)
                lowestExpTime = exp;
        }
    }
}

void QuicManagement::InboundContext::handleExpirations()
{
    ngtcp2_tstamp newLow = UINT64_MAX;
    ConnectionCtxSet::iterator iter = connections.begin();
    ngtcp2_tstamp now = QuicConnection::gents();
    while (iter != connections.end()) {
        ConnectionContext *cctx = *iter;
        ConnectionCtxSet::iterator eiter = iter;
        iter++;

        ngtcp2_tstamp ex = cctx->getExpTime();
        if (now > ex) {
            bool killCtx = true;
            try {
                killCtx = !cctx->handleExpiration(socket);
            } catch (SocketException &) {
                // ignore - will kill context below
            }
            if (killCtx) {
                txConnections.erase(cctx);
                connections.erase(eiter);
                delete cctx;
                continue;
            }
            // May have written and/or sent tx data
            if (cctx->hasTxData())
                txConnections.insert(cctx);
            else
                txConnections.erase(cctx);

            // Update to new expiration
            ex = cctx->getExpTime();
        }
        if (ex < newLow)
            newLow = ex;
    }
    lowestExpTime = newLow;
}


ngtcp2_tstamp QuicManagement::InboundContext::getExpiration() const
{
    return lowestExpTime;
}

void QuicManagement::InboundContext::resetConnections()
{
    for (ConnectionCtxSet::iterator c = connections.begin();
                    c != connections.end(); c++)
        delete *c;
    connections.clear();
    txConnections.clear();
}







/***********************************************************************/
// QuicTxContext


QuicManagement::QuicTxContext::QuicTxContext(QuicManagement *owner,
                                          const std::string &host,
                                          uint16_t port,
                                          const std::string &fileId) : 
        TxContext(host, port, (const uint8_t *)fileId.data(), fileId.size()),
        owner(owner),
        socket(NULL),
        qCtx(NULL),
        localAddr(NULL),
        fileTransferBuf(NULL)
{
    fileTransferBuf = new FileTransferBuffer(
                                4 * QuicConnection::STREAM_FLOW_CTRL_BYTES,
                                &owner->clientFileTransferMutex,
                                &owner->clientFileTransferCond);
}

QuicManagement::QuicTxContext::QuicTxContext(QuicManagement *owner,
                                          const std::string &host,
                                          uint16_t port,
                                          const CoTMessage *msg,
                                          ContactUID *ourUid,
                                          ExtensionRegistry *extensions,
                                          const ExtensionIdSet &enabledExtensions,
                                          int protoVersion)
                                              COMMO_THROW (std::invalid_argument) :
        TxContext(host, port, NULL, msg, ourUid, extensions, enabledExtensions, protoVersion),
        owner(owner),
        socket(NULL),
        qCtx(NULL),
        localAddr(NULL),
        fileTransferBuf(NULL)
{
}

QuicManagement::QuicTxContext::~QuicTxContext()
{
    shutdownIO();
    delete fileTransferBuf;
}

void QuicManagement::QuicTxContext::shutdownIO()
{
    delete socket;
    socket = NULL;
    delete localAddr;
    localAddr = NULL;
    delete qCtx;
    qCtx = NULL;
}

void QuicManagement::QuicTxContext::init() COMMO_THROW (std::invalid_argument, SocketException)
{
    socket = new UdpSocket(destination, false);
    localAddr = socket->getBoundAddr();
    qCtx = new ConnectionContext(owner, *localAddr, *destination,
                             owner->connTimeoutSec, data, dataLen,
                             fileTransferBuf);
    // Need to do a write to get connection starting
    if (!qCtx->transmitPkts(socket))
        throw std::invalid_argument("Starting initial quic handshake failed");
}

ngtcp2_tstamp QuicManagement::QuicTxContext::getExpiration() const
{
    return qCtx->getExpTime();
}

bool QuicManagement::QuicTxContext::ioRead() COMMO_THROW (SocketException)
{
    const size_t buflen = 65536;
    uint8_t buf[buflen];
    
    bool ok = true;
    int pktCount = 0;
    while (pktCount++ < 10 && ok) {
        NetAddress *sourceAddr = NULL;
        size_t len = buflen;

        try {
            socket->recvfrom(&sourceAddr, buf, &len);
        } catch (SocketWouldBlockException &) {
            break;
        }
        
        ok = qCtx->processPkt(socket, NULL, *localAddr, sourceAddr, buf, len);
    }
    ok = ok && qCtx->transmitPkts(socket);
    return handleFailure(ok);
}

bool QuicManagement::QuicTxContext::ioWrite() COMMO_THROW (SocketException)
{
    return handleFailure(qCtx->transmitPkts(socket));
}

bool QuicManagement::QuicTxContext::handleExpiration() COMMO_THROW (SocketException)
{
    return handleFailure(qCtx->handleExpiration(socket));
}

bool QuicManagement::QuicTxContext::handleFailure(bool operationOk) COMMO_THROW (SocketException)
{
    if (!operationOk) {
        // quic connection has ceased and tx is drained.
        // Indicate if data was fully acknowledged
        // (ret false) or not (exception)
        if (qCtx->isTxStreamComplete()) {
            InternalUtils::logprintf(owner->logger, CommoLogger::LEVEL_DEBUG, "quic client transmit completed, dropping connection");
            return false;
        }

        // ... Not exactly a socket or I/O low level error, but good enough for now
        InternalUtils::logprintf(owner->logger, CommoLogger::LEVEL_ERROR, "quic client connection error before transmit completed");
        throw SocketException(netinterfaceenums::ERR_OTHER, "I/O error transmitting data");
    }

    return true;
}

bool QuicManagement::QuicTxContext::wantsTx() const
{
    return qCtx->hasTxData();
}





/***********************************************************************/
// ConnectionContext

QuicManagement::ConnectionContext::ConnectionContext(QuicManagement *owner,
                                             const NetAddress &localAddr,
                                             NetAddress *clientAddr,
                                             uint32_t version,
                                             const ngtcp2_cid &scidFromClient,
                                             const ngtcp2_cid &dcidFromClient,
                                             float connTimeoutSec,
                                             X509 *cert,
                                             EVP_PKEY *privKey,
                                             STACK_OF(X509) *supportCerts)
                            COMMO_THROW(std::invalid_argument) :
        QuicConnection(owner->logger, ALPNS, sizeof(ALPNS),
                       localAddr, clientAddr, version, scidFromClient,
                       dcidFromClient, connTimeoutSec, cert, privKey,
                       supportCerts),
        owner(owner),
        isFileSender(false),
        fileSendFileProvider(NULL),
        fileSendFile(NULL),
        fileRecvBuffer(NULL),
        rxData(NULL),
        rxLen(0),
        rxBufLen(0),
        rxComplete(false),
        txMsg(NULL),
        txMsgLen(0),
        txMsgOffset(0),
        txSourceDone(true)
{
}

QuicManagement::ConnectionContext::ConnectionContext(QuicManagement *owner,
                                             const NetAddress &localAddr,
                                             const NetAddress &serverAddr,
                                             float connTimeoutSec,
                                             uint8_t *dataToSend,
                                             size_t dataLen,
                                             FileTransferBuffer *fileRecvBuffer)
                            COMMO_THROW(std::invalid_argument) :
        QuicConnection(owner->logger,
                       fileRecvBuffer ? TAKFDP_ALPN : TAKCOT_ALPN,
                       (fileRecvBuffer ? TAKFDP_ALPN[0] : TAKCOT_ALPN[0]) + 1,
                       localAddr, serverAddr, connTimeoutSec, 20),
        owner(owner),
        isFileSender(false),
        fileSendFileProvider(NULL),
        fileSendFile(NULL),
        fileRecvBuffer(fileRecvBuffer),
        rxData(NULL),
        rxLen(0),
        rxBufLen(0),
        rxComplete(false),
        txMsg(dataToSend),
        txMsgLen(dataLen),
        txMsgOffset(0),
        txSourceDone(false)
{
}

QuicManagement::ConnectionContext::~ConnectionContext()
{
    delete[] rxData;
    if (fileSendFile) {
        fileSendFileProvider->close(fileSendFile);
    }
}

bool QuicManagement::ConnectionContext::isTxSourceDone() const
{
    return txSourceDone;
}

bool QuicManagement::ConnectionContext::txSourceFill(
        uint8_t *buf, size_t *len, int *errCode)
{
    bool ret = true;
    if (isFileSender) {
InternalUtils::logprintf(owner->logger, CommoLogger::LEVEL_DEBUG,
        "quic %p txSourceFill %d",
        this, (int)*len);
        size_t nr = fileSendFileProvider->read(buf, 1, *len, fileSendFile);
        if (nr != *len) {
            if (fileSendFileProvider->eof(fileSendFile)) {
                txSourceDone = true;
            } else if (fileSendFileProvider->error(fileSendFile)) {
                *errCode = FILEXFER_IO_ERR;
                ret = false;
            } else {
                *errCode = FILEXFER_UNK_ERR;
                ret = false;
            }
            fileSendFileProvider->close(fileSendFile);
            fileSendFile = NULL;
        }
        *len = nr;
    } else {
        size_t nr = txMsgLen - txMsgOffset;
        if (!nr) {
            txSourceDone = true;
        } else {
            if (nr > *len)
                nr = *len;
            memcpy(buf, txMsg + txMsgOffset, nr);
            txMsgOffset += nr;
        }
        *len = nr;
        // ret stays true
    }
    return ret;
}

bool QuicManagement::ConnectionContext::txFinishHandling(
        UdpSocket *socket, size_t bytesSent) COMMO_THROW(SocketException)
{
    if (!fileRecvBuffer && bytesSent && isTxStreamComplete()) {
        // We've sent and received ack for all there is to send
        // and we're not expecting data to come in
        ngtcp2_ccerr err;
        int ec;
        if (isFileSender)
            ec = FILEXFER_OK;
        else
            ec = MSG_OK;
        ngtcp2_ccerr_set_application_error(&err, ec, NULL, 0);
        InternalUtils::logprintf(owner->logger, CommoLogger::LEVEL_DEBUG,
                "quic %p txFinishHandling causes close because no data left to send or receive",
                this);
        return enterClosing(socket, err);
    }
    return true;
}

bool QuicManagement::ConnectionContext::receivedData(const uint8_t *data,
                                               size_t dataLen, bool finFlagged)
{
    if (fileRecvBuffer) {
        size_t nw = fileRecvBuffer->write(data, dataLen);
        if (nw != dataLen) {
            InternalUtils::logprintf(owner->logger,
                                     CommoLogger::LEVEL_ERROR,
                                     "quic %p stream data received for file transfer, but write transfer returned mismatched length",
                                     this);
            return false;
        }
            
    } else {
        size_t rem = rxBufLen - rxLen;
        if (rem < dataLen) {
            if (rxLen + dataLen > RX_CLIENT_MAX_DATA_LEN) {
                InternalUtils::logprintf(owner->logger,
                                         CommoLogger::LEVEL_ERROR,
                                         "quic %p stream data received but exceeds max message length",
                                         this);
                return false;
            }

            size_t n = dataLen - rem;
            growCapacity(n);
            rem += n;
        }
        memcpy(rxData + rxLen, data, dataLen);
        rxLen += dataLen;
    }
    
    // While FIN does indicate all data came in,
    // note that it does *not* indicate we have ack'd it all,
    // so we can't, for example, close down connection immediately
    // as remote would initiate unneeded resends
    // Completion of acks for rx is indicated by stream closure (assuming
    // tx side of stream is closed
    if (finFlagged) {
        InternalUtils::logprintf(owner->logger,
                                 CommoLogger::LEVEL_DEBUG,
                                 "quic %p stream received FIN",
                                 this);
        if (fileRecvBuffer)
            fileRecvBuffer->write(NULL, 0);
        rxComplete = true;
    }

    return true;
}

bool QuicManagement::ConnectionContext::postProcessPkt(UdpSocket *socket)
                                                   COMMO_THROW(SocketException)
{
    if (rxComplete && isFileSender && !fileSendFileProvider) {
        // try to initiate transfer
        std::string s((const char *)rxData, rxLen);
        try {
            InternalUtils::logprintf(owner->logger, CommoLogger::LEVEL_DEBUG,
                                     "quic %p initiating file send for fid {%s}",
                                     this,
                                     s.c_str());
            // throws for bad int
            int fileId = InternalUtils::intFromString(s.c_str());
            // needs map, mapmutex, provider
            Lock xferLock(*owner->mpTransferFilesMutex);
            std::map<int, std::string>::iterator iter = owner->mpTransferFiles->find(fileId);
            if (iter == owner->mpTransferFiles->end())
                throw std::invalid_argument("file to send not found");
            fileSendFileProvider = owner->ioProviderFactory->getCurrentProvider();
            fileSendFile = fileSendFileProvider->open(iter->second.c_str(), "rb");
            if (!fileSendFile)
                throw std::invalid_argument("file to send could not be opened");
            txSourceDone = false;
            
        } catch (std::invalid_argument &ex) {
            InternalUtils::logprintf(owner->logger, CommoLogger::LEVEL_ERROR,
                                     "quic %p open of file send for fid {%s} failed (%s)",
                                     this, s.c_str(), ex.what());
            ngtcp2_ccerr err;
            ngtcp2_ccerr_set_application_error(&err, FILEXFER_FILE_NOT_FOUND,
                                               NULL, 0);
            return handleClosed(enterClosing(socket, err));
        }
    }

    return true;
}

bool QuicManagement::ConnectionContext::wantsTxUni()
{
    return !fileRecvBuffer && getBytesSent() < txMsgLen;
}

bool QuicManagement::ConnectionContext::wantsBiDir()
{
    return fileRecvBuffer && getBytesSent() < txMsgLen;
}

bool QuicManagement::ConnectionContext::isRemoteBiDirStreamAllowed()
{
    return isFileSender;
}

bool QuicManagement::ConnectionContext::handshakeComplete(
        const uint8_t *alpn,
        unsigned int alpnLen)
{
    if (isServerMode()) {
        if (alpnLen == TAKFDP_ALPN[0] && memcmp(alpn, TAKFDP_ALPN + 1, alpnLen) == 0) {
            // Toggle on as file transfer
            isFileSender = true;
        }
    }
    return true;
}

void QuicManagement::ConnectionContext::growCapacity(size_t n)
{
    size_t newLen = rxBufLen + n;
    uint8_t *newBuf = new uint8_t[newLen];
    if (rxData) {
        memcpy(newBuf, rxData, rxLen);
        delete[] rxData;
    }
    rxData = newBuf;
    rxBufLen = newLen;
}

size_t QuicManagement::ConnectionContext::releaseRxData(uint8_t **data,
                             NetAddress **lastRemoteAddr)
{
    size_t ret = rxLen;
    *data = rxData;
    *lastRemoteAddr = NetAddress::duplicateAddress(getRemoteClientAddr());

    rxData = NULL;
    rxLen = rxBufLen = 0;
    
    return ret;
}




/***********************************************************************/
// FileTransferBuffer helper class

QuicManagement::FileTransferBuffer::FileTransferBuffer(size_t bufSize,
                                           thread::Mutex *mutex,
                                           thread::CondVar *cond) :
            bufSize(bufSize),
            readOff(0),
            writeOff(0),
            buf(NULL),
            writeAtEOF(false),
            writeErrored(false),
            readDone(false),
            mutex(mutex),
            cond(cond)
{
    buf = new uint8_t[bufSize];
}

QuicManagement::FileTransferBuffer::~FileTransferBuffer()
{
    delete[] buf;
}

size_t QuicManagement::FileTransferBuffer::read(uint8_t *dest, size_t len)
{
    size_t n = writeOff - readOff;
    if (n > len)
        n = len;
    size_t srcOff = readOff % bufSize;
    size_t nToEnd = bufSize - srcOff;
    if (nToEnd < n) {
        memcpy(dest, buf + srcOff, nToEnd);
        memcpy(dest + nToEnd, buf, n - nToEnd);
    } else {
        memcpy(dest, buf + srcOff, n);
    }
    readOff += n;
    return n;
}

size_t QuicManagement::FileTransferBuffer::write(const uint8_t *from,
                                                 size_t len)
{
    Lock lock(*mutex);

    if (len == 0) {
        writeAtEOF = true;
        cond->broadcast(lock);
        return 0;
    } else {
        // see what space we have
        size_t used = writeOff - readOff;
        size_t n = bufSize - used;
        size_t check = SIZE_MAX - writeOff;
        // clamp to length requested
        if (n > len)
            n = len;
        // prevent overflow of our read/write offsets
        if (n > check)
            n = check;
        if (!n || readDone)
            // no space or reader doesn't care anymore
            return 0;

        size_t destOff = writeOff % bufSize;
        size_t nToEnd = bufSize - destOff;
        if (nToEnd < n) {
            // 2 parts
            memcpy(buf + destOff, from, nToEnd);
            memcpy(buf, from + nToEnd, n - nToEnd);
        } else {
            memcpy(buf + destOff, from, n);
        }
        writeOff += n;
        // Signal if we were starved below our "low water mark" of
        // half the stream flow control amount and are now going above it
        // to prevent extraneous sleep/wake cycles on readers
        if (used > FILETRANSFER_LOWWATER && used + n > FILETRANSFER_LOWWATER)
            cond->broadcast(lock);
        return n;
    }
}

bool QuicManagement::FileTransferBuffer::isAtEOF()
{
    size_t n = writeOff - readOff;
    return writeAtEOF && !n;
}

bool QuicManagement::FileTransferBuffer::isReadable()
{
    return writeErrored || writeAtEOF ||
           (writeOff - readOff) > FILETRANSFER_LOWWATER;
}

bool QuicManagement::FileTransferBuffer::isReadDone()
{
    return readDone;
}

void QuicManagement::FileTransferBuffer::setReadDone()
{
    readDone = true;
}

bool QuicManagement::FileTransferBuffer::isWriteErrored()
{
    return writeErrored;
}

void QuicManagement::FileTransferBuffer::setWriteErrored()
{
    Lock lock(*mutex);
    writeErrored = true;
    cond->broadcast(lock);
}




