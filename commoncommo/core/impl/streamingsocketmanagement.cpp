#define __STDC_LIMIT_MACROS
#include "streamingsocketmanagement.h"
#include "takmessage.h"
#include "commothread.h"
#include "libxml/tree.h"

#include <sstream>
#include <limits.h>
#include <string.h>
#include <inttypes.h>

#include "openssl/pkcs12.h"
#include "openssl/err.h"

using namespace atakmap::commoncommo;
using namespace atakmap::commoncommo::netinterfaceenums;
using namespace atakmap::commoncommo::impl;
using namespace atakmap::commoncommo::impl::thread;


namespace {
    const float PROTO_TIMEOUT_SECONDS = 60.0f;
    const float DEFAULT_CONN_TIMEOUT_SECONDS = 20.0f;
    const float CONN_RETRY_SECONDS = 15.0f;
    const float RESOLVE_RETRY_SECONDS = 30.0f;
    // Send "ping" to server after this long of no rx activity
    const float RX_STALE_SECONDS = 15.0f;
    // Repeat "ping" to server this often as long as no rx activity goes on
    const float RX_STALE_PING_SECONDS = 4.5f;
    // Reset connection after this long of no rx activity
    const float RX_TIMEOUT_SECONDS = 25.0f;
    const char MESSAGE_END_TOKEN[] = "</event>";
    const size_t MESSAGE_END_TOKEN_LEN = sizeof(MESSAGE_END_TOKEN) - 1;
    const char *PING_UID_SUFFIX = "-ping";

    const unsigned char ALPN_STREAMING[] = {
        0x09, 't','a','k','s','t','r','e','a','m'
    };

    char *copyString(const std::string &s)
    {
        char *c = new char[s.length() + 1];
        const char *sc = s.c_str();
        strcpy(c, sc);
        return c;
    }
    
    const char *THREAD_NAMES[] = {
        "cmostrm.conn", 
        "cmostrm.io",
        "cmostrm.rxq",
    };
}

/*************************************************************************/
// StreamingSocketManagement constructor/destructor


StreamingSocketManagement::StreamingSocketManagement(CommoLogger *logger,
                                                ExtensionRegistry *extensions,
                                                const std::string &myuid) :
        ThreadedHandler(3, THREAD_NAMES), logger(logger),
        extensions(extensions),
        resolver(new ResolverQueue(logger, this, RESOLVE_RETRY_SECONDS, RESQ_INFINITE_TRIES)),
        connTimeoutSec(DEFAULT_CONN_TIMEOUT_SECONDS),
        monitor(true),
        sslCtx(NULL),
        contexts(), contextMutex(RWMutex::Policy_Fair),
        ioNeedsRebuild(false),
        upContexts(), upMutex(),
        downContexts(), downNeedsRebuild(false), downMutex(),
        resolutionContexts(),
        myuid(myuid),
        myPingUid(myuid + PING_UID_SUFFIX),
        rxQueue(), rxQueueMutex(), rxQueueMonitor(),
        ifaceListeners(), ifaceListenersMutex(),
        listeners(), listenersMutex()
{
    ERR_clear_error();
    sslCtx = SSL_CTX_new(SSLv23_client_method());
    if (sslCtx)
        // Be certain openssl doesn't do anything with its internal
        // verification as we will do our own (internal verify cannot be made
        // to work with in-memory certs)
        SSL_CTX_set_verify(sslCtx, SSL_VERIFY_NONE, NULL);
    else {
        unsigned long errCode = ERR_get_error();
        char ebuf[1024];
        ERR_error_string_n(errCode, ebuf, 1024);
        ebuf[1023] = '\0';
        InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "Cannot create SSL Context! SSL connections will not be available (details: %s)", ebuf);
    }
    startThreads();
}

StreamingSocketManagement::~StreamingSocketManagement()
{
    stopThreads();
    delete resolver;

    // With the threads all joined, we can remove everything safely
    // Clean the rx queue first
    while (!rxQueue.empty()) {
        RxQueueItem rxi = rxQueue.back();
        rxQueue.pop_back();
        rxi.implode();
    }

    // Now tear down all the contexts
    ContextMap::iterator iter;
    for (iter = contexts.begin(); iter != contexts.end(); ++iter)
        delete iter->second;

    if (sslCtx) {
        SSL_CTX_free(sslCtx);
        sslCtx = NULL;
    }
}


/*************************************************************************/
// StreamingSocketManagement - interface management

StreamingNetInterface *StreamingSocketManagement::addStreamingInterface(
        netinterfaceenums::StreamingTransport transport,
        const char *addr, int port, const CoTMessageType *types,
        size_t nTypes, const uint8_t *clientCert, size_t clientCertLen,
        const uint8_t *caCert, size_t caCertLen,
        const char *certPassword,
        const char *caCertPassword,
        const char *username, const char *password, CommoResult *resultCode)
{
    CommoResult rc = COMMO_ILLEGAL_ARGUMENT;
    StreamingNetInterface *ret = NULL;

    uint16_t sport = (uint16_t)port;
    NetAddress *endpoint = NULL;
    std::string epString;


    // First try to parse all the args to get essential connection info
    if (port > UINT16_MAX || port < 0) {
        rc = COMMO_ILLEGAL_ARGUMENT;
        goto done;
    }
    // Try parsing addr as a string-form of an IP address directly.
    // If this fails, endpoint will be null
    endpoint = NetAddress::create(addr, sport);

    // Get string that is unique based on identifying criteria of the connection
    epString = getEndpointString(addr, port, transport);

    {
        WriteLock lock(contextMutex);
        ContextMap::iterator iter = contexts.find(epString);
        if (iter != contexts.end()) {
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR,
                    "Streaming interface %s already exists", epString.c_str());
            delete endpoint;
            
            rc = COMMO_ILLEGAL_ARGUMENT;
            goto done;
        }

        ConnectionContext *ctx = NULL;
        switch (transport) {
        case TRANSPORT_SSL:
            if (!clientCert) {
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR,
                        "Streaming interface %s addition did not supply client certificate", epString.c_str());
                rc = COMMO_ILLEGAL_ARGUMENT;
                goto done;
            }
            try {
                ctx = new SSLConnectionContext(logger, extensions, myPingUid,
                                               epString, addr, sport,
                                               endpoint,
                                               myuid, 
                                               sslCtx,
                                               clientCert, clientCertLen,
                                               caCert, caCertLen,
                                               certPassword,
                                               caCertPassword,
                                               username, password);
            } catch (SSLArgException &e) {
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "Unable to create SSL connection configuration: %s", e.what());
                // Remove any lingering open ssl error codes
                ERR_clear_error();
                rc = e.errCode;
                goto done;
            }
            break;
        case TRANSPORT_TCP:
            ctx = new TcpConnectionContext(logger, extensions, myPingUid,
                                           epString, addr, sport,
                                           endpoint);
            break;
        case TRANSPORT_QUIC:
            if (!clientCert) {
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR,
                        "Streaming interface %s addition did not supply client certificate", epString.c_str());
                rc = COMMO_ILLEGAL_ARGUMENT;
                goto done;
            }
            try {
                ctx = new QuicConnectionContext(logger, extensions, myPingUid,
                                                epString, addr, sport,
                                                endpoint,
                                                myuid, 
                                                clientCert, clientCertLen,
                                                caCert, caCertLen,
                                                certPassword,
                                                caCertPassword,
                                                username, password);
            } catch (SSLArgException &e) {
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "Unable to create quic connection configuration: %s", e.what());
                rc = e.errCode;
                goto done;
            }

            break;
        default:
            rc = COMMO_ILLEGAL_ARGUMENT;
            goto done;
        }
        contexts.insert(ContextMap::value_type(epString, ctx));

        ctx->broadcastCoTTypes.insert(types, types + nTypes);

        if (endpoint) {
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                    "Streaming interface %s added with pre-resolved IP string", epString.c_str());

        } else {
            // Needs name resolution
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                    "Streaming interface %s added with hostname string %s - going to name resolution", epString.c_str(), addr);
        }
        // Give to connection thread to do resolution, if needed,
        // and handle connection
        {
            Lock downLock(downMutex);
            downContexts.insert(ctx);
        }
        ret = ctx;
        rc = COMMO_SUCCESS;
    }
    
done:
    if (resultCode)
        *resultCode = rc;

    return ret;
}

CommoResult StreamingSocketManagement::removeStreamingInterface(
        std::string *endpoint, StreamingNetInterface *iface)
{
    WriteLock lock(contextMutex);

    // Try to erase context from the master list
    ContextMap::iterator cmIter = contexts.begin();
    for (cmIter = contexts.begin(); cmIter != contexts.end(); ++cmIter) {
        if (cmIter->second == iface)
            break;
    }
    if (cmIter == contexts.end())
        return COMMO_ILLEGAL_ARGUMENT;
    
    *endpoint = std::string(iface->remoteEndpointId, iface->remoteEndpointLen);
    contexts.erase(cmIter);

    // Now we know it was one of ours
    ConnectionContext *ctx = (ConnectionContext *)iface;

    // Remove from up, down, or resolution list, whichever it is in
    {
        Lock uLock(upMutex);
        if (upContexts.erase(ctx) == 1)
            ioNeedsRebuild = true;
    }
    {
        Lock dLock(downMutex);
        if (downContexts.erase(ctx) == 1)
            downNeedsRebuild = true;
    }
    
    if (ctx->resolverRequest) {
        resolutionContexts.erase(ctx->resolverRequest);
        resolver->cancelResolution(ctx->resolverRequest);
        ctx->resolverRequest = NULL;
    }

    // Remove any reference in the rxQueue
    {
        Lock rxqlock(rxQueueMutex);
        RxQueue::iterator iter = rxQueue.begin();
        while (iter != rxQueue.end()) {
            if (iter->ctx == ctx) {
                RxQueue::iterator eraseMe = iter;
                iter++;
                eraseMe->implode();
                rxQueue.erase(eraseMe);
            } else {
                iter++;
            }
        }
    }

    // Clean up the context itself
    delete ctx;
    return COMMO_SUCCESS;
}

void StreamingSocketManagement::reconnectAll()
{
    ReadLock lock(contextMutex);
    Lock uLock(upMutex);
    
    for (ConnectionContext *ctx : upContexts)
        ctx->resetRequested = true;
}

void StreamingSocketManagement::addInterfaceStatusListener(
        InterfaceStatusListener* listener)
{
    Lock lock(ifaceListenersMutex);
    ifaceListeners.insert(listener);
}

void StreamingSocketManagement::removeInterfaceStatusListener(
        InterfaceStatusListener* listener)
{
    Lock lock(ifaceListenersMutex);
    ifaceListeners.erase(listener);
}

std::string StreamingSocketManagement::getEndpointString(
        const char *addr, int port, StreamingTransport transport)
{
    std::stringstream ss;
    switch (transport) {
    case TRANSPORT_QUIC:
        ss << "quic:";
        break;
    case TRANSPORT_SSL:
        ss << "ssl:";
        break;
    case TRANSPORT_TCP:
        ss << "tcp:";
        break;
    }
    ss << addr;
    ss << ":";
    ss << port;
    return ss.str();
}

void StreamingSocketManagement::fireInterfaceChange(ConnectionContext *ctx, bool up)
{
    Lock lock(ifaceListenersMutex);
    std::set<InterfaceStatusListener *>::iterator iter;
    for (iter = ifaceListeners.begin(); iter != ifaceListeners.end(); ++iter) {
        InterfaceStatusListener *listener = *iter;
        if (up)
            listener->interfaceUp(ctx);
        else
            listener->interfaceDown(ctx);
    }
}

void StreamingSocketManagement::fireInterfaceErr(ConnectionContext *ctx,
                              netinterfaceenums::NetInterfaceErrorCode errCode)
{
    Lock lock(ifaceListenersMutex);
    std::set<InterfaceStatusListener *>::iterator iter;
    for (iter = ifaceListeners.begin(); iter != ifaceListeners.end(); ++iter) {
        InterfaceStatusListener *listener = *iter;
        listener->interfaceError(ctx, errCode);
    }
}


/*************************************************************************/
// StreamingSocketManagement public api: misc 

void StreamingSocketManagement::setConnTimeout(float sec)
{
    connTimeoutSec = sec;
}


void StreamingSocketManagement::setMonitor(bool en)
{
    this->monitor = en;
}


/*************************************************************************/
// StreamingSocketManagement public api: SSL config access

void StreamingSocketManagement::configSSLForConnection(std::string streamingEndpoint, SSL_CTX *sslCtx) COMMO_THROW (std::invalid_argument)
{
    ReadLock lock(contextMutex);
    ContextMap::iterator iter = contexts.find(streamingEndpoint);
    if (iter == contexts.end())
        throw std::invalid_argument("Invalid stream endpoint - interface has been removed/disabled");

    ConnectionContext *ctx = iter->second;

    SSLConfiguration *sslConfig = ctx->getSSLConfig();
    if (!sslConfig)
        throw std::invalid_argument("Connection is not ssl based");
    

    SSL_CTX_use_certificate(sslCtx, sslConfig->cert);
    SSL_CTX_use_PrivateKey(sslCtx, sslConfig->key);
    // Disable ECDH ciphers as demo.atakserver.com fails when they are
    // enabled.  See:  https://bugs.launchpad.net/ubuntu/+source/openssl/+bug/1475228
    // for similar issues with other servers.
    SSL_CTX_set_cipher_list(sslCtx, "DEFAULT:!ECDH");

    // Can't use the cert store in sslConfig as it is used internally and they
    // don't share well in our current openssl version (set_cert_store does
    // not increment ref counts!)
    // Make a new one
    X509_STORE *store = X509_STORE_new();
    if (!store)
         throw std::invalid_argument("couldn't init cert store");

    int nCaCerts = sk_X509_num(sslConfig->caCerts);
    for (int i = 0; i < nCaCerts; ++i)
         X509_STORE_add_cert(store, sk_X509_value(sslConfig->caCerts, i));

    SSL_CTX_set_cert_store(sslCtx, store);
}

bool StreamingSocketManagement::isEndpointSSL(std::string streamingEndpoint) COMMO_THROW (std::invalid_argument)
{
    ReadLock lock(contextMutex);
    ContextMap::iterator iter = contexts.find(streamingEndpoint);
    if (iter == contexts.end())
        throw std::invalid_argument("Invalid stream endpoint - interface has been removed/disabled");

    ConnectionContext *ctx = iter->second;
    return ctx->getSSLConfig() != NULL;
}

NetAddress *StreamingSocketManagement::getAddressForEndpoint(std::string endpoint) COMMO_THROW (std::invalid_argument)
{
    // Though not writing the context, this gives us exclusivity against
    // remoteEndpointAddr changing
    WriteLock lock(contextMutex);
    ContextMap::iterator iter = contexts.find(endpoint);
    if (iter == contexts.end())
        throw std::invalid_argument("Invalid stream endpoint - interface has been removed/disabled");

    ConnectionContext *ctx = iter->second;
    if (ctx->remoteEndpointAddr) {
        return NetAddress::duplicateAddress(ctx->remoteEndpointAddr);
    } else
        throw std::invalid_argument("Invalid stream endpoint - interface has hostname not yet resolved!");
}

/*************************************************************************/
// StreamingSocketManagement public api: cot messaging

void StreamingSocketManagement::sendMessage(
        std::string streamingEndpoint, const CoTMessage *msg)
                COMMO_THROW (std::invalid_argument)
{
    ReadLock lock(contextMutex);
    ContextMap::iterator iter = contexts.find(streamingEndpoint);
    if (iter == contexts.end())
        throw std::invalid_argument("Invalid stream endpoint - interface has been removed/disabled");

    ConnectionContext *ctx = iter->second;

    {
        Lock upLock(upMutex);
        if (upContexts.find(ctx) == upContexts.end())
            throw std::invalid_argument("Specified streaming interface is down");

        CoTMessage *msgCopy = new CoTMessage(*msg);

        //std::string dbgStr((const char *)msgBytes, len);
        //InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "Stream CoT message to ep %s is: {%s}", streamingEndpoint.c_str(), dbgStr.c_str());
        try {
            ctx->txQueue.push_front(TxQueueItem(ctx, extensions,
                                                msgCopy,
                                                ctx->txQueueProtoVersion));
        } catch (std::invalid_argument &e) {
            delete msgCopy;
            throw e;
        }
    }
}

void StreamingSocketManagement::sendBroadcast(
        const CoTMessage *msg, bool ignoreType) COMMO_THROW (std::invalid_argument)
{
    CoTMessageType type = msg->getType();


    ReadLock lock(contextMutex);
    Lock upLock(upMutex);

    ContextSet::iterator iter;
    for (iter = upContexts.begin(); iter != upContexts.end(); ++iter) {
        ConnectionContext *ctx = *iter;
        if (ignoreType || ctx->broadcastCoTTypes.find(type) != 
                                              ctx->broadcastCoTTypes.end()) {
            CoTMessage *msgCopy = new CoTMessage(*msg);
            try {
                ctx->txQueue.push_front(TxQueueItem(ctx, extensions, 
                                                    msgCopy, 
                                                    ctx->txQueueProtoVersion));
            } catch (std::invalid_argument &e) {
                delete msgCopy;
                throw e;
            }
        }
    }
}

void StreamingSocketManagement::addStreamingMessageListener(
        StreamingMessageListener* listener)
{
    Lock lock(listenersMutex);
    listeners.insert(listener);
}

void StreamingSocketManagement::removeStreamingMessageListener(
        StreamingMessageListener* listener)
{
    Lock lock(listenersMutex);
    listeners.erase(listener);
}




/*************************************************************************/
// StreamingSocketManagement - ThreadedHandler impl

void StreamingSocketManagement::threadEntry(
        size_t threadNum)
{
    switch (threadNum) {
    case IO_THREADID:
        ioThreadProcess();
        break;
    case RX_QUEUE_THREADID:
        recvQueueThreadProcess();
        break;
    case CONN_MGMT_THREADID:
        connectionThreadProcess();
        break;
    }
}

void StreamingSocketManagement::threadStopSignal(
        size_t threadNum)
{
    switch (threadNum) {
    case IO_THREADID:
        break;
    case RX_QUEUE_THREADID:
    {
        Lock rxLock(rxQueueMutex);
        rxQueueMonitor.broadcast(rxLock);
        break;
    }
    case CONN_MGMT_THREADID:
        break;
    }
}



/*************************************************************************/
// StreamingSocketManagement - Connection management thread

void StreamingSocketManagement::connectionThreadProcess()
{
    ContextSet pendingContexts;
    ContextSet pendingReadContexts;
    ContextSet pendingWriteContexts;
    ContextSet pendingExpContexts;
    NetSelector selector;

    while (!threadShouldStop(CONN_MGMT_THREADID)) {
        // Hold this the entire iteration, preventing any of our
        // in-use contexts from being destroyed.
        ReadLock ctxLock(contextMutex);

        ContextSet newlyConnectedCtxs;
        bool selectFatalError = false;

        // Run through all down contexts.
        // For each, if no connection in progress and time has come to try
        // again, start the connection attempt.
        ContextSet::iterator ctxIter;
        {
            Lock lock(downMutex);
            for (ctxIter = downContexts.begin(); ctxIter != downContexts.end(); ++ctxIter) {
                ConnectionContext *ctx = *ctxIter;
                CommoTime nowTime = CommoTime::now();

                if (!ctx->getSocket()) {
                    if (!ctx->remoteEndpointAddr) {
                        // Need to resolve endpoint
                        if (!ctx->resolverRequest) {
                            InternalUtils::logprintf(logger, CommoLogger::LEVEL_INFO, "Initiating attempt to resolve tak server %s for connection %s", ctx->remoteEndpointHostname.c_str(), ctx->remoteEndpoint.c_str());
                            ctx->resolverRequest = 
                                    resolver->queueForResolution(ctx->remoteEndpointHostname);
                            resolutionContexts.insert(
                                    ResolverMap::value_type(ctx->resolverRequest, ctx));
                        } // else resolution is in progress, just skip
                        
                    } else if (nowTime > ctx->retryTime) {
                        try {
                            InternalUtils::logprintf(logger, CommoLogger::LEVEL_INFO, "Initiating streaming network connection for %s", ctx->remoteEndpoint.c_str());
                            if (ctx->connectionInitSocket()) {
                                // this connection succeeded immediately
                                // no need to rebuild just yet,
                                // just note as connected.
                                ctx->retryTime = nowTime + connTimeoutSec;
                                if (ctx->connectionDoPost(&downNeedsRebuild))
                                    newlyConnectedCtxs.insert(ctx);
                            } else {
                                // Flag for rebuilding our pending socket set
                                // since this is "new"
                                downNeedsRebuild = true;
                                // Set connection timeout
                                ctx->retryTime = nowTime + connTimeoutSec;
                            }
                        } catch (SocketException &e) {
                            InternalUtils::logprintf(logger, CommoLogger::LEVEL_VERBOSE, "streaming socket connection initiation failed (%s); will retry", e.what());
                            fireInterfaceErr(ctx, e.errCode);
                            ctx->reset(nowTime + CONN_RETRY_SECONDS, false);
                        }
                    }
                }
            }

            if (downNeedsRebuild) {
                std::vector<Socket *> pendingSockets;
                std::vector<Socket *> pendingConnSockets;
                std::vector<Socket *> pendingReadSockets;
                pendingContexts.clear();
                pendingWriteContexts.clear();
                pendingReadContexts.clear();
                pendingExpContexts.clear();

                for (ctxIter = downContexts.begin(); ctxIter != downContexts.end(); ++ctxIter) {
                    ConnectionContext *ctx = *ctxIter;
                    if (ctx->getSocket()) {
                        if (ctx->connectionInProgress()) {
                            pendingConnSockets.push_back(ctx->getSocket());
                            pendingWriteContexts.insert(ctx);
                        } else {
                            if (ctx->connectionPostWantsRead()) {
                                // Place on to read/write context list
                                pendingReadSockets.push_back(ctx->getSocket());
                                pendingReadContexts.insert(ctx);
                            }
                            if (ctx->connectionPostWantsWrite()) {
                                pendingSockets.push_back(ctx->getSocket());
                                pendingWriteContexts.insert(ctx);
                            }
                        }
                        pendingContexts.insert(ctx);
                        if (ctx->hasExpiration())
                            pendingExpContexts.insert(ctx);
                    }
                }
                try {
                    selector.setSockets(&pendingReadSockets, &pendingSockets, &pendingConnSockets);
                    downNeedsRebuild = false;
                } catch (SocketException &) {
                    InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "Socket limit reached managing streaming connections; closing all and restarting");
                    selectFatalError = true;
                }
            }
        }
        
        // If we have sockets with pending connections, select on them to see
        // if they complete or error. However, skip past this and process
        // any immediately connected sockets from above if we have such.
        if (newlyConnectedCtxs.empty()) {
            long selectTimeout = 500;

            for (ctxIter = pendingExpContexts.begin(); 
                    ctxIter != pendingExpContexts.end(); ++ctxIter) {
                ConnectionContext *ctx = *ctxIter;
                try {
                    ContextSet::iterator writeIter = pendingWriteContexts.find(ctx);
                    long ex = ctx->handleExpirations();
                    if (ex > 0 && ex < selectTimeout)
                        selectTimeout = ex;
                    bool wantsWrite = ctx->connectionPostWantsWrite();
                    if ((writeIter != pendingWriteContexts.end() &&
                                                                !wantsWrite) ||
                                (writeIter == pendingWriteContexts.end() &&
                                 wantsWrite)) {
                        downNeedsRebuild = true;
                    }
                } catch (SocketException &) {
                    InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "IO error encountered during a protocol expiration during connection handshake for streaming connection %s, will retry", ctx->remoteEndpoint.c_str());
                    fireInterfaceErr(ctx, netinterfaceenums::ERR_CONN_OTHER);
                    ctx->reset(CommoTime::now() + CONN_RETRY_SECONDS, false);
                    downNeedsRebuild = true;
                }
            }
            if (downNeedsRebuild)
                continue;
        
            try {
                if (!selectFatalError && !selector.doSelect(selectTimeout)) {
                    // Timed out
                    CommoTime nowTime = CommoTime::now();
                    for (ctxIter = pendingContexts.begin(); ctxIter != pendingContexts.end(); ++ctxIter) {
                        ConnectionContext *ctx = *ctxIter;
                        bool stillConnecting = ctx->connectionInProgress();
                        if (nowTime > ctx->retryTime) {
                            if (stillConnecting)
                                InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "Error trying to make network connection for %s - will retry (connection timed out)", ctx->remoteEndpoint.c_str());
                            else
                                InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "Error trying to make network connection for %s - will retry (SSL handshake following connection timed out)", ctx->remoteEndpoint.c_str());
                            fireInterfaceErr(ctx, netinterfaceenums::ERR_CONN_TIMEOUT);
                            ctx->reset(nowTime + CONN_RETRY_SECONDS, false);
                            downNeedsRebuild = true;
                        }
                    }
                    continue;
                }
            } catch (SocketException &) {
                // Weird to get an error here, unless your name is Solaris
                // which will apparently error on select() for sockets in error
                // state.  Anyway, if this happens, flag to restart all of them
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "Error from streaming connection select() - retrying connections");
                selectFatalError = true;
            }
            
            if (selectFatalError) {
                CommoTime rTime = CommoTime::now() + CONN_RETRY_SECONDS;
                for (ctxIter = pendingContexts.begin(); ctxIter != pendingContexts.end(); ++ctxIter) {
                    ConnectionContext *ctx = *ctxIter;
                    fireInterfaceErr(ctx, netinterfaceenums::ERR_CONN_OTHER);
                    ctx->reset(rTime, false);
                }
                downNeedsRebuild = true;
                continue;
            }

            // Something is ready; check for writes first as this
            // is most common case
            CommoTime nowTime = CommoTime::now();
            for (ctxIter = pendingWriteContexts.begin(); ctxIter != pendingWriteContexts.end(); ++ctxIter) {
                ConnectionContext *ctx = *ctxIter;
                bool stillConnecting = ctx->connectionInProgress();

                try {
                    if (selector.getLastConnectState(ctx->getSocket()) != NetSelector::WRITABLE) {
                        if (nowTime > ctx->retryTime) {
                            if (stillConnecting)
                                throw SocketException(netinterfaceenums::ERR_CONN_TIMEOUT,
                                                  "tcp connection timed out");
                            else
                                throw SocketException(netinterfaceenums::ERR_CONN_TIMEOUT,
                                                  "SSL handshake following connection timed out");
                        }
                        continue;
                    }

                    if (stillConnecting) {
                        netinterfaceenums::NetInterfaceErrorCode errCode;
                        if (ctx->getSocket()->isSocketErrored(&errCode)) {
                            throw SocketException(errCode,
                                                  "tcp socket connection error");
                        } else if (ctx->connectionDoPost(&downNeedsRebuild))
                            newlyConnectedCtxs.insert(ctx);
                    } else if (ctx->connectionPostWantsWrite()) {
                        // Is handshaking and wanted write
                        // that is now fulfilled.
                        if (ctx->connectionDoPost(&downNeedsRebuild))
                            newlyConnectedCtxs.insert(ctx);
                        // else it now wants more writing or reading; all handled
                        // by connectionDoPost().
                    }
                } catch (SocketException &e) {
                    InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "Error trying to make network connection for %s - will retry (%s)", ctx->remoteEndpoint.c_str(), e.what());
                    fireInterfaceErr(ctx, e.errCode);
                    ctx->reset(nowTime + CONN_RETRY_SECONDS, false);
                    downNeedsRebuild = true;
                }
            }

            for (ctxIter = pendingReadContexts.begin(); ctxIter != pendingReadContexts.end(); ++ctxIter) {
                ConnectionContext *ctx = *ctxIter;
                try {
                    if (selector.getLastReadState(ctx->getSocket()) != NetSelector::READABLE) {
                        if (nowTime > ctx->retryTime)
                            throw SocketException(netinterfaceenums::ERR_CONN_TIMEOUT,
                                                  "SSL handshake following connection timed out waiting on read");
                        continue;
                    }

                    // Only base connected, post-connect handshakes can trigger
                    // read pending.
                    // Just check the post-connect status.
                    if (ctx->connectionDoPost(&downNeedsRebuild))
                        newlyConnectedCtxs.insert(ctx);
                } catch (SocketException &e) {
                    InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "Error trying to make network connection for %s - will retry (%s)", ctx->remoteEndpoint.c_str(), e.what());
                    fireInterfaceErr(ctx, e.errCode);
                    ctx->reset(CommoTime::now() + CONN_RETRY_SECONDS, false);
                    downNeedsRebuild = true;
                }
            }

        }

        if (!newlyConnectedCtxs.empty()) {
            {
                Lock lock(downMutex);
                // Pull each from the down list
                for (ctxIter = newlyConnectedCtxs.begin(); ctxIter != newlyConnectedCtxs.end(); ++ctxIter)
                    downContexts.erase(*ctxIter);
            }

            for (ctxIter = newlyConnectedCtxs.begin(); ctxIter != newlyConnectedCtxs.end(); ++ctxIter)
                fireInterfaceChange(*ctxIter, true);

            {
                Lock lock(upMutex);
                CommoTime now = CommoTime::now();
                for (ctxIter = newlyConnectedCtxs.begin(); ctxIter != newlyConnectedCtxs.end(); ++ctxIter) {
                    InternalUtils::logprintf(logger, CommoLogger::LEVEL_INFO, "Stream connection to %s is up!", (*ctxIter)->remoteEndpoint.c_str());
                    (*ctxIter)->retryTime = now;
                    (*ctxIter)->lastRxTime = now;
                    (*ctxIter)->protoTimeout = now + PROTO_TIMEOUT_SECONDS;
                    (*ctxIter)->resetRequested = false;
                    upContexts.insert(*ctxIter);
                }
                ioNeedsRebuild = true;
            }
            downNeedsRebuild = true;
        }



    }
}



/*************************************************************************/
// StreamingSocketManagement - I/O thread

void StreamingSocketManagement::ioThreadProcess()
{
    // RX set is all "up" ifaces  << rebuild on iface changes
    // TX set is all "up" ifaces that are full wqueue << rebuilds on iface changes or tx status change
    // exp set is all "up" ifaces that have expiration handling << rebuild on iface changes
    ContextSet rxSet;
    std::vector<Socket *> rxSockets;
    ContextSet txSet;
    std::vector<Socket *> txSockets;
    ContextSet expSet;
    NetSelector selector;

    while (!threadShouldStop(IO_THREADID)) {
        ReadLock ctxLock(contextMutex);

        ContextSet errorSet;
        ContextSet::iterator ctxIter;
        ConnectionContext *ctx;

        // Set if txSet has changes and tx socket set needs rebuilding
        bool txNeedsRebuild = false;

        {
            Lock upLock(upMutex);

            // If list of contexts has externally changed, don't use existing
            // socket sets or txSet.  Just rebuild everything
            if (ioNeedsRebuild) {
                expSet.clear();
                txSet.clear();
                rxSet.clear();
                rxSockets.clear();
                txNeedsRebuild = true;
            }

            for (ctxIter = upContexts.begin(); ctxIter != upContexts.end(); ++ctxIter) {
                ctx = *ctxIter;
                if (ctx->resetRequested) {
                    InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "Forcing connection reset for %s due to external request", ctx->remoteEndpoint.c_str());
                    errorSet.insert(ctx);
                    continue;
                }
                bool inTxSelection = txSet.find(ctx) != txSet.end();

                bool doTx;
                doTx = ctx->ioWriteReady(inTxSelection ? &selector : NULL);

                try {
                    if (doTx) {
                        // Tx might have room
                        bool writeTried = false;
                        while (!ctx->txQueue.empty() && !ctx->protoBlockedForResponse) {
                            TxQueueItem &item = ctx->txQueue.back();
                            size_t r = item.dataLen - item.bytesSent;
                            while (r > 0) {
                                size_t w = ctx->ioWrite(item.data + item.bytesSent, r);
                                writeTried = true;
                                if (!w)
                                    break;
                                r -= w;
                                item.bytesSent += w;
                            }
                            if (r) {
                                // socket tx queue is full (non-SSL) or
                                // some input or output needed (SSL)
                                // before finishing this item.
                                // break out and leave queue item intact
                                break;
                            } else {
                                if (item.protoSwapRequest)
                                    // No more sending on this guy until
                                    // we get a response to this proto swap
                                    // request (in rx handling)
                                    ctx->protoBlockedForResponse = true;
                                item.implode();
                                ctx->txQueue.pop_back();
                            }
                        }
                        if (!writeTried)
                            ctx->ioWriteFlush();
                    }

                    if (!inTxSelection) {
                        if (ctx->ioWantsWrite()) {
                            txSet.insert(ctx);
                            txNeedsRebuild = true;
                        }
                    } else {
                        if (!ctx->ioWantsWrite()) {
                            // Sent everything - this guy is in the clear
                            txSet.erase(ctx);
                            txNeedsRebuild = true;
                        }
                    }
                } catch (SocketException &) {
                    // Socket error. Queue for going to down state.
                    fireInterfaceErr(ctx, netinterfaceenums::ERR_IO);
                    InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "IO error sending streaming data");
                    errorSet.insert(ctx);
                }

                if (ioNeedsRebuild) {
                    rxSockets.push_back(ctx->getSocket());
                    rxSet.insert(ctx);
                    if (ctx->hasExpiration())
                        expSet.insert(ctx);
                }
            }

            ioNeedsRebuild = false;
        }

        long selectTimeout = 100;
        if (errorSet.empty()) {
            for (ctxIter = expSet.begin(); 
                    ctxIter != expSet.end(); ++ctxIter) {
                ConnectionContext *ctx = *ctxIter;
                try {
                    long ex = ctx->handleExpirations();
                    if (ex > 0 && ex < selectTimeout)
                        selectTimeout = ex;

                    bool wantsWrite = ctx->ioWantsWrite();
                    ContextSet::iterator txIter = txSet.find(ctx);
                    if (txIter != txSet.end() && !wantsWrite) {
                        txSet.erase(txIter);
                        txNeedsRebuild = true;
                    } else if (txIter == txSet.end() && wantsWrite) {
                        txSet.insert(ctx);
                        txNeedsRebuild = true;
                    }

                } catch (SocketException &) {
                    InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "IO error encountered during a protocol expiration event for streaming connection %s", ctx->remoteEndpoint.c_str());
                    fireInterfaceErr(ctx, netinterfaceenums::ERR_IO);
                    errorSet.insert(ctx);
                }
            }
        }

        if (errorSet.empty() && txNeedsRebuild) {
            txSockets.clear();
            for (ctxIter = txSet.begin(); ctxIter != txSet.end(); ++ctxIter) {
                ctx = *ctxIter;
                txSockets.push_back(ctx->getSocket());
            }
            try {
                selector.setSockets(&rxSockets, &txSockets);
                txNeedsRebuild = false;
            } catch (SocketException &) {
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "Socket limit reached managing streaming io; closing all and restarting");
                for (ctxIter = rxSet.begin(); ctxIter != rxSet.end(); ++ctxIter) {
                    ctx = *ctxIter;
                    fireInterfaceErr(ctx, netinterfaceenums::ERR_OTHER);
                    errorSet.insert(ctx);
                }
            }
        }

        if (errorSet.empty()) {
            // If expirations caused need to rebuild tx, bail and force that
            if (txNeedsRebuild)
                continue;

            try {
                selector.doSelect(selectTimeout);
            } catch (SocketException &) {
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "Error from streaming io select()?");
                for (ctxIter = rxSet.begin(); ctxIter != rxSet.end(); ++ctxIter) {
                    ctx = *ctxIter;
                    fireInterfaceErr(ctx, netinterfaceenums::ERR_OTHER);
                    errorSet.insert(ctx);
                }
            }
        }

        // Only deal with RX. TX will be dealt with later.
        if (errorSet.empty()) {
            CommoTime now = CommoTime::now();
            for (ctxIter = rxSet.begin(); ctxIter != rxSet.end(); ++ctxIter) {
                ctx = *ctxIter;
                try {
                    bool foundSomething = false;
                    if (ctx->ioReadReady(&selector)) {
                        // Read until there is nothing available
                        while (true) {
                            size_t r = ctx->ioRead(ctx->rxBuf + ctx->rxBufOffset, ConnectionContext::rxBufSize - ctx->rxBufOffset);
                            if (!r)
                                break;
                            bool f = scanStreamData(ctx, r);
                            foundSomething = foundSomething || f;
                        }
                    }

                    if (foundSomething) {
                        ctx->lastRxTime = now;
                    } else if (monitor) {
                        float d = now.minus(ctx->lastRxTime);
                        if (d > RX_TIMEOUT_SECONDS) {
                            InternalUtils::logprintf(logger, 
                                    CommoLogger::LEVEL_ERROR,
                                    "No data received from %s in %d seconds; reconnecting", 
                                    ctx->remoteEndpoint.c_str(),
                                    (int)d);
                            fireInterfaceErr(ctx, netinterfaceenums::ERR_IO_RX_DATA_TIMEOUT);
                            errorSet.insert(ctx);
                        } else if (d > RX_STALE_SECONDS && now > ctx->retryTime) {
                            InternalUtils::logprintf(logger, 
                                    CommoLogger::LEVEL_DEBUG,
                                    "No data received from %s in %d seconds; sending ping", 
                                    ctx->remoteEndpoint.c_str(),
                                    (int)d);
                            sendPing(ctx);
                            ctx->retryTime = now + RX_STALE_PING_SECONDS;
                        }
                    }

                    if (!checkProtoTimeout(ctx, now)) {
                        InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "TakServer %s Proto Negotiate: timed out waiting for response from server", ctx->remoteEndpoint.c_str());
                        fireInterfaceErr(ctx, netinterfaceenums::ERR_OTHER);
                        errorSet.insert(ctx);
                    }

                } catch (SocketException &) {
                    InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "Error receiving data from %s", ctx->remoteEndpoint.c_str());
                    fireInterfaceErr(ctx, netinterfaceenums::ERR_IO);
                    errorSet.insert(ctx);
                }
            }
        }

        if (!errorSet.empty()) {
            {
                Lock lock(upMutex);
                // remove all in errorSet from up, killing sockets and
                // expunging txQueue
                for (ctxIter = errorSet.begin(); ctxIter != errorSet.end(); ctxIter++) {
                    ctx = *ctxIter;
                    ctx->reset(CommoTime::now() + CONN_RETRY_SECONDS, true);
                    upContexts.erase(ctx);
                }
            }

            for (ctxIter = errorSet.begin(); ctxIter != errorSet.end(); ctxIter++)
                fireInterfaceChange(*ctxIter, false);

            {
                // Move to down list
                Lock lock(downMutex);
                downContexts.insert(errorSet.begin(), errorSet.end());
            }
            // Make certain to reset everything!
            ioNeedsRebuild = true;
        }

    }
}

void StreamingSocketManagement::sendPing(ConnectionContext *ctx)
{
    Lock uplock(upMutex);
    ctx->txPushPing();
}

void StreamingSocketManagement::convertTxToProtoVersion(ConnectionContext *ctx,
                                                        int protoVersion)
{
    Lock upLock(upMutex);
    
    TxQueue::iterator iter;
    for (iter = ctx->txQueue.begin(); iter != ctx->txQueue.end(); )
    {
        try {
            iter->reserialize(ctx->supportsAllExtensions, 
                              ctx->txExtensions, protoVersion);
            iter++;
        } catch (std::invalid_argument &ex) {
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR,
                "Unexpected error reserializing message! %s", ex.what());
            TxQueue::iterator curIter = iter;
            iter++;
            
            ctx->txQueue.erase(curIter);
        }
    }
    ctx->txQueueProtoVersion = protoVersion;

}

bool StreamingSocketManagement::checkProtoTimeout(ConnectionContext *ctx,
                                                  const CommoTime &now)
{
    if (ctx->protoState > ConnectionContext::PROTO_WAITRESPONSE)
        return true;
    if (now > ctx->protoTimeout) {
        if (ctx->protoState == ConnectionContext::PROTO_WAITRESPONSE) {
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR,
                "TakServer %s Proto Negotiate: Timed out waiting for protocol negotiation response, reconnecting...", ctx->remoteEndpoint.c_str());
            return false;
        } else { // PROTO_XML_NEGOTIATE
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_INFO,
                "TakServer %s Proto Negotiate: Timed out waiting for protocol version support message - continuing as XML only", ctx->remoteEndpoint.c_str());
            ctx->protoState = ConnectionContext::PROTO_XML_ONLY;
        }
    }
    return true;
}

bool StreamingSocketManagement::protoNegotiation(ConnectionContext *ctx,
                                                 CoTMessage *msg)
{
    bool ret = false;
    TakControlType t = msg->getTakControlType();
    switch (t) {
      case TYPE_SUPPORT:
        if (ctx->protoState == ConnectionContext::PROTO_XML_NEGOTIATE) {
            std::set<int> vs;
            ExtensionIdSet es;
            bool allExtensionsSupported = false;
            msg->getTakControlSupportedVersions(&vs, &es, &allExtensionsSupported);
            std::string vstring;
            std::string estring;
            for (std::set<int>::iterator iter = vs.begin(); iter != vs.end(); ++iter) {
                vstring += InternalUtils::intToString(*iter);
                vstring += " ";
            }
            if (allExtensionsSupported) {
                estring = "<all>";
            } else {
                for (ExtensionIdSet::iterator iter = es.begin(); iter != es.end(); ++iter) {
                    estring += InternalUtils::intToString(*iter);
                    estring += " ";
                }
            }
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_INFO,
                "TakServer %s Proto Negotiate: Server supports protocol versions: %s", ctx->remoteEndpoint.c_str(), vstring.c_str());
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_INFO,
                "TakServer %s Proto Negotiate: Server supports protocol extensions: %s", ctx->remoteEndpoint.c_str(), estring.c_str());
            if (vs.count(1)) {
                // We support version 1 - tell server
                {
                    Lock uplock(upMutex);
                    // Update extension set;  while we won't be using these
                    // for sending data until/if we swap fully over to protobuf
                    // we need to stash it now for possible use later.
                    ctx->txExtensions = es;
                    ctx->supportsAllExtensions = allExtensionsSupported;
                    ExtensionIdSet ourExtensions;
                    extensions->getAllIds(ourExtensions);
                    CoTMessage *rmsg = new CoTMessage(logger, 
                                                      msg->getEventUid(), 1,
                                                      ourExtensions);
                    try {
                        TxQueueItem qi(ctx, extensions, rmsg);
                        qi.protoSwapRequest = true;
                        ctx->txQueue.push_front(qi);
                    } catch (std::invalid_argument &) {
                        delete rmsg;
                        InternalUtils::logprintf(logger,
                            CommoLogger::LEVEL_ERROR,
                            "TakServer %s Proto Negotiate: Unexpected error when serializing protocol swap request", ctx->remoteEndpoint.c_str());
                        break;
                    }
                }

                ctx->protoState = ConnectionContext::PROTO_WAITRESPONSE;
                ctx->protoTimeout = CommoTime::now() + PROTO_TIMEOUT_SECONDS;
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_INFO,
                    "TakServer %s Proto Negotiate: Requesting transition to protocol version 1", ctx->remoteEndpoint.c_str());
            }
        } else {
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_WARNING,
                "TakServer %s Proto Negotiate: TakControl message advertising version support received after already being received, ignoring", ctx->remoteEndpoint.c_str());
        }
        break;
      case TYPE_RESPONSE:
        if (ctx->protoState == ConnectionContext::PROTO_WAITRESPONSE) {
            if (msg->getTakControlResponseStatus()) {
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_INFO,
                    "TakServer %s Proto Negotiate: Protocol negotiation request accepted, swapping proto version", ctx->remoteEndpoint.c_str());
                ctx->protoState = ConnectionContext::PROTO_HDR_MAGIC;
                ret = true;
                convertTxToProtoVersion(ctx, 1);

            } else {
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_INFO,
                    "TakServer %s Proto Negotiate: protocol negotiation request denied, using xml only", ctx->remoteEndpoint.c_str());
                ctx->protoState = ConnectionContext::PROTO_XML_ONLY;
            }
            ctx->protoBlockedForResponse = false;
            
        } else {
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_WARNING,
                "TakServer %s Proto Negotiate: TakControl response message received when we didn't send a request - ignoring!", ctx->remoteEndpoint.c_str());
        }
        break;
      default:
        break;
    }

    return ret;
}

bool StreamingSocketManagement::dispatchRxMessage(ConnectionContext *ctx,
                                               size_t len, bool isXml)
{
    bool ret = false;

    // Convert the data to a CoTMessage
    try {
        TakMessage takmsg(logger, ctx->rxBuf + ctx->rxBufStart,
                             len, extensions, isXml, !isXml);
        CoTMessage *msg = takmsg.releaseCoTMessage();
        if (!msg)
            return ret;
        
        if (isXml && ctx->protoState != ConnectionContext::PROTO_XML_ONLY) {
            ret = protoNegotiation(ctx, msg);
        }
        
        // Discard if it is a pong or control message, else post it
        if (!msg->isPong() && msg->getTakControlType() == TakControlType::TYPE_NONE) {
            Lock qLock(rxQueueMutex);

            rxQueue.push_back(RxQueueItem(ctx, msg));
            rxQueueMonitor.broadcast(qLock);
        } else {
            delete msg;
        }
    } catch (std::invalid_argument &e) {
        std::string s((const char *)ctx->rxBuf + ctx->rxBufStart, len);
        CommoLogger::ParsingDetail detail{ ctx->rxBuf + ctx->rxBufStart, len, e.what() == NULL ? "" : e.what(), ctx->remoteEndpoint.c_str() };
        InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, CommoLogger::TYPE_PARSING, &detail, "Invalid CoT message received from stream: {%s} -- %s", s.c_str(), e.what() == NULL ? "" : e.what());
    }
    
    return ret;
}

bool StreamingSocketManagement::scanStreamData(
        ConnectionContext *ctx, size_t nNewBytes)
{
    bool foundSomething = false;
    size_t scanStart;
    bool protoIsXml = ctx->protoState <= ConnectionContext::PROTO_XML_ONLY;
    if (protoIsXml && (ctx->rxBufOffset - ctx->rxBufStart) > MESSAGE_END_TOKEN_LEN)
        scanStart = ctx->rxBufOffset - MESSAGE_END_TOKEN_LEN;
    else
        scanStart = ctx->rxBufStart;
    size_t scanEnd = ctx->rxBufOffset + nNewBytes;

    if (protoIsXml) {
        size_t tokenIdx = 0;
        for (size_t i = scanStart; i < scanEnd; ++i) {
            if (ctx->rxBuf[i] == MESSAGE_END_TOKEN[tokenIdx]) {
                if (++tokenIdx == MESSAGE_END_TOKEN_LEN) {
                    // msg complete - dispatch it and start new search
                    size_t msgLen = i - ctx->rxBufStart + 1;
                    bool protoSwap = dispatchRxMessage(ctx, msgLen, true);
                    ctx->rxBufStart = i + 1;
                    foundSomething = true;
                    if (protoSwap) {
                        // transition to protobuf immediately
                        // State already updated by dispatch
                        protoIsXml = false;
                        scanStart = ctx->rxBufStart;
                        break;
                    }

                    tokenIdx = 0;
                }
            } else
                tokenIdx = 0;
        }
    }

    if (!protoIsXml) {
        for (size_t i = scanStart; i < scanEnd; ) {
            switch (ctx->protoState) {
                case ConnectionContext::PROTO_HDR_MAGIC:
                    if (ctx->rxBuf[i] == 0xbf) {
                        ctx->protoState = ConnectionContext::PROTO_HDR_LEN;
                        ctx->rxBufStart = i + 1;
                        if (ctx->protoMagicSearchCount != 0) {
                            InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                                "Found magic byte 0xBF in server stream after skipping %u bytes", ctx->protoMagicSearchCount);
                            ctx->protoMagicSearchCount = 0;
                        }
                    } else {
                        if (ctx->protoMagicSearchCount == 0) {
                            InternalUtils::logprintf(logger, CommoLogger::LEVEL_WARNING,
                                "Found %X instead of magic byte 0xBF in server stream - skipping until next magic byte!", ((int)ctx->rxBuf[i]) & 0xFF);
                        }
                        ctx->protoMagicSearchCount++;
                    }

                    i++;
                    break;
                case ConnectionContext::PROTO_HDR_LEN:
                    if ((ctx->rxBuf[i] & 0x80) == 0) {
                        // End of varint
                        size_t n = scanEnd - ctx->rxBufStart;
                        try {
                            uint64_t vlen = InternalUtils::varintDecode(
                                    ctx->rxBuf + ctx->rxBufStart, &n);
                            // Second condition is just a sanity check
                            if (vlen > ConnectionContext::rxBufSize || (ctx->rxBufStart + n) != (i + 1)) {
                                ctx->protoState = ConnectionContext::PROTO_HDR_MAGIC;
                                ctx->rxBufStart = i + 1;
                                InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR,
                                    "Protobuf length varint encoding incorrect or too large (%" PRIu64 ") - skipping and rescanning for header", vlen);
                            } else {
                                //InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                                //    "Protobuf length %d in server stream", (unsigned int)vlen);
                                ctx->protoState = ConnectionContext::PROTO_DATA;
                                ctx->protoLen = (size_t)vlen;
                                ctx->rxBufStart = i + 1;
                            }
                        } catch (std::invalid_argument &) {
                            ctx->protoState = ConnectionContext::PROTO_HDR_MAGIC;
                            ctx->rxBufStart = i + 1;
                        }
                        
                    } // else still looking for end of varint
                    
                    // All paths consume 1 byte
                    i++;
                    break;
                case ConnectionContext::PROTO_DATA:
                    // See if we have enough data in buffer to complete
                    if (scanEnd - i >= ctx->protoLen) {
                        // msg complete - dispatch it and start new search
                        dispatchRxMessage(ctx, ctx->protoLen, false);

                        i += ctx->protoLen;
                        ctx->rxBufStart = i;
                        ctx->protoState = ConnectionContext::PROTO_HDR_MAGIC;
                        foundSomething = true;
                        
                    } else {
                        // Not enough data - shortcut to end of new data
                        i = scanEnd;
                    }
                    break;
                case ConnectionContext::PROTO_XML_NEGOTIATE:
                case ConnectionContext::PROTO_WAITRESPONSE:
                case ConnectionContext::PROTO_XML_ONLY:
                    // Nothing to do here - this doesn't happen
                    // Here to appease compiler
                    break;
            }
        }
    }
    ctx->rxBufOffset = scanEnd;

    if (ctx->rxBufOffset == ctx->rxBufSize) {
        // Full buffer - need to make more room
        if (ctx->rxBufStart == 0) {
            // Have to dump some data.
            ctx->rxBufOffset = ctx->rxBufSize / 2;
            memcpy(ctx->rxBuf, ctx->rxBuf + ctx->rxBufOffset, ctx->rxBufOffset);
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                                     "Message scanning buffer full - had to discard some streaming data!");

            // Start proto scanning over since we dumped data...
            if (!protoIsXml)
                ctx->protoState = ConnectionContext::PROTO_HDR_MAGIC;
        } else {
            // Shift down
            ctx->rxBufOffset -= ctx->rxBufStart;
            memmove(ctx->rxBuf, ctx->rxBuf + ctx->rxBufStart, ctx->rxBufOffset);
            ctx->rxBufStart = 0;
        }
    }
    return foundSomething;
}


/*************************************************************************/
// StreamingSocketManagement - Rx queue processing thread

void StreamingSocketManagement::recvQueueThreadProcess()
{
    while (!threadShouldStop(RX_QUEUE_THREADID)) {
        RxQueueItem qitem;
        std::string endpoint;
        {
            Lock rxLock(rxQueueMutex);
            if (rxQueue.empty()) {
                rxQueueMonitor.wait(rxLock);
                continue;
            }

            qitem = rxQueue.back();
            rxQueue.pop_back();

            // Copy out the context identifier while holding the queue lock
            endpoint = qitem.ctx->remoteEndpoint;
        }

        // Override any endpoint in the message with the streaming
        // endpoint indicator.  This is holdover from old ATAK
        // CoTService, but its nice because less chance for clients
        // to use the wrong thing if sending outside the bounds of
        // this library
        qitem.msg->setEndpoints(ENDPOINT_STREAMING, "", NULL);
        
        Lock lock(listenersMutex);
        std::set<StreamingMessageListener *>::iterator iter;
        for (iter = listeners.begin(); iter != listeners.end(); ++iter) {
            StreamingMessageListener *listener = *iter;
            listener->streamingMessageReceived(endpoint, qitem.msg);
        }
        qitem.implode();
    }
}


/*************************************************************************/
// StreamingSocketManagement - name resolution processing


void StreamingSocketManagement::resolutionAttemptFailed(
                             ResolverQueue::Request *id,
                             const std::string &hostAddr)
{
    // Acquire the lock and be certain our context is still valid.
    // If so, send out an error indication.
    {
        ReadLock ctxLock(contextMutex);
        ConnectionContext *resolveMeContext = NULL;

        {
            Lock lock(downMutex);
            ResolverMap::iterator iter = resolutionContexts.find(id);
            if (iter != resolutionContexts.end()) {
                // Still valid
                resolveMeContext = iter->second;
                
            } // else no longer valid, so just ignore it
        }

        if (resolveMeContext)
            fireInterfaceErr(resolveMeContext, netinterfaceenums::ERR_CONN_NAME_RES_FAILED);
    }
}


bool StreamingSocketManagement::resolutionComplete(
                                ResolverQueue::Request *request,
                                const std::string &hostAddr,
                                NetAddress *addr)
{
    if (addr) {
        InternalUtils::logprintf(logger, CommoLogger::LEVEL_INFO, "Success resolving tak server address: %s", hostAddr.c_str());
    } else {
        // Should never happen!
        InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "Resolver returned null on infinite retries - this is a bug!");
        return false;
    }
    
    // Acquire the lock and be certain our context is still valid.
    // If so, clean up resolver request and note the resolution result;
    // connection thread will pick it up from there
    // If not, just forget it.
    {
        ReadLock ctxLock(contextMutex);
        Lock lock(downMutex);
        ConnectionContext *resolveMeContext = NULL;

        ResolverMap::iterator iter = resolutionContexts.find(request);
        if (iter != resolutionContexts.end()) {
            // Still valid
            resolveMeContext = iter->second;
            // Pull it from the resolver list and note new address
            // Connection thread will take it from here
            resolutionContexts.erase(iter);
            resolveMeContext->resolverRequest = NULL;
            resolveMeContext->remoteEndpointAddr = addr->deriveNewAddress(resolveMeContext->remotePort);
        } // else no longer valid, so just ignore it
    }
    // We don't need the result, return false
    return false;
}



/*************************************************************************/
// Internal utility classes

StreamingSocketManagement::TxQueueItem::TxQueueItem() :
        ctx(NULL), extensions(NULL), msg(NULL), data(NULL), dataLen(0), 
        bytesSent(0), protoSwapRequest(false)
{
}

StreamingSocketManagement::TxQueueItem::TxQueueItem(
        ConnectionContext *ctx, ExtensionRegistry *extensions,
        CoTMessage *msg, int protoVersion)
            COMMO_THROW (std::invalid_argument) :
                ctx(ctx), extensions(extensions), 
                msg(msg), data(NULL), dataLen(0), bytesSent(0),
                protoSwapRequest(false)
{
    reserialize(ctx->supportsAllExtensions, ctx->txExtensions, 
                protoVersion);
}


StreamingSocketManagement::TxQueueItem::TxQueueItem(
        ConnectionContext *ctx,
        const std::string &rawMessage) :
            ctx(ctx), extensions(NULL), msg(NULL), data(NULL),
            dataLen(0), bytesSent(0),
            protoSwapRequest(false)
{
    dataLen = rawMessage.length();
    uint8_t *ndata = new uint8_t[dataLen];
    memcpy(ndata, rawMessage.c_str(), dataLen);
    data = ndata;
}

StreamingSocketManagement::TxQueueItem::~TxQueueItem()
{
}

void StreamingSocketManagement::TxQueueItem::implode()
{
    delete[] data;
    data = NULL;
    delete msg;
    msg = NULL;
}

void StreamingSocketManagement::TxQueueItem::reserialize(
        bool useAnyExtension,
        const ExtensionIdSet &enabledExtensions,
        int protoVersion) 
                COMMO_THROW (std::invalid_argument)
{
    if (!msg)
        throw std::invalid_argument("Cannot reserialize raw message");

    uint8_t *ndata;
    size_t len;
    if (!protoVersion)
        len = msg->serialize(&ndata);
    else {
        TakMessage tmsg(msg->getLogger(), msg, NULL, extensions);
        len = tmsg.serializeAsProtobuf(protoVersion, &ndata, 
                                       TakMessage::HEADER_LENGTH,
                                       true, false, extensions,
                                       useAnyExtension,
                                       enabledExtensions);
    }

    delete data;
    data = ndata;
    dataLen = len;
}


StreamingSocketManagement::RxQueueItem::RxQueueItem() :
        ctx(NULL), msg()
{
}

StreamingSocketManagement::RxQueueItem::RxQueueItem(
        ConnectionContext *ctx, CoTMessage *msg) :
                ctx(ctx), msg(msg)
{
}

StreamingSocketManagement::RxQueueItem::~RxQueueItem()
{
}

void StreamingSocketManagement::RxQueueItem::implode()
{
    delete msg;
    msg = NULL;
}

StreamingSocketManagement::ConnectionContext::ConnectionContext(
        CommoLogger *logger,
        ExtensionRegistry *extensions,
        const std::string &myPingUid,
        const std::string &epString, const std::string &epAddrString,
        unsigned short int port, NetAddress *ep, StreamingTransport transport) :
        StreamingNetInterface(copyString(epString), epString.length()),
        logger(logger),
        extensions(extensions),
        myPingUid(myPingUid),
        remoteEndpoint(epString),
        remoteEndpointHostname(ep ? "" : epAddrString),
        remotePort(port),
        remoteEndpointAddr(ep), broadcastCoTTypes(),
        transport(transport),
        resolverRequest(NULL), retryTime(CommoTime::ZERO_TIME),
        lastRxTime(CommoTime::ZERO_TIME),
        txQueue(), txQueueProtoVersion(0), txExtensions(),
        resetRequested(false),
        rxBufStart(0), rxBufOffset(0),
        protoState(PROTO_XML_NEGOTIATE),
        protoMagicSearchCount(0),
        protoLen(0),
        protoBlockedForResponse(false),
        protoTimeout(CommoTime::now() + PROTO_TIMEOUT_SECONDS)
{
}

StreamingSocketManagement::ConnectionContext::~ConnectionContext()
{
    while (!txQueue.empty()) {
        TxQueueItem txi = txQueue.back();
        txQueue.pop_back();
        txi.implode();
    }
    delete remoteEndpointAddr;
    delete[] remoteEndpointId;
}

bool StreamingSocketManagement::ConnectionContext::ioWantsWrite()
{
    return !txQueue.empty() && !protoBlockedForResponse;
}


StreamingSocketManagement::SSLConfiguration *
StreamingSocketManagement::ConnectionContext::getSSLConfig()
{
    return NULL;
}

void StreamingSocketManagement::ConnectionContext::reset(
    CommoTime nextConnTime, bool clearIo)
{
    retryTime = nextConnTime;
    // Clear any resolved address *if* we were configured
    // with an unresolved hostname
    if (!remoteEndpointHostname.empty()) {
        delete remoteEndpointAddr;
        remoteEndpointAddr = NULL;
    }
    if (clearIo) {
        while (!txQueue.empty()) {
            TxQueueItem &txi = txQueue.back();
            txi.implode();
            txQueue.pop_back();
        }
        txQueueProtoVersion = 0;
        supportsAllExtensions = false;
        txExtensions.clear();
        resetRequested = false;
        rxBufOffset = 0;
        rxBufStart = 0;
        protoState = ConnectionContext::PROTO_XML_NEGOTIATE;
        protoLen = 0;
        protoBlockedForResponse = false;
    }
}

void StreamingSocketManagement::ConnectionContext::generateAuthDoc(
                                           std::string *authMessage,
                                           const std::string &myuid,
                                           const char *username,
                                           const char *password)
                                           COMMO_THROW (SSLArgException)
{
    if (username && password && username[0] != '\0') {
        xmlDoc *doc = xmlNewDoc((const xmlChar *)"1.0");
        xmlNode *authElement = xmlNewNode(NULL, (const xmlChar *)"auth");
        xmlDocSetRootElement(doc, authElement);
        xmlNode *cotElement = xmlNewChild(authElement, NULL, 
                                          (const xmlChar *)"cot", NULL);
        xmlNewProp(cotElement, (const xmlChar *)"username",
                   (const xmlChar *)username);
        xmlNewProp(cotElement, (const xmlChar *)"password",
                   (const xmlChar *)password);
        xmlNewProp(cotElement, (const xmlChar *)"uid",
                   (const xmlChar *)myuid.c_str());
        
        xmlChar *outPtr = NULL;
        int outSize;
        xmlDocDumpFormatMemory(doc, &outPtr, &outSize, 0);
        if (outPtr == NULL) {
            xmlFreeDoc(doc);
            throw SSLArgException(COMMO_ILLEGAL_ARGUMENT, 
                    "unable to create auth document - unknown error");
        }
        if (outSize > 0 && outPtr[outSize - 1] == '\n')
            // Remove trailing newline - this is needed by at least streaming to TAK server
            outSize--;

        *authMessage = std::string((const char *)outPtr, outSize);
        xmlFree(outPtr);
        xmlFreeDoc(doc);
    }
}

void StreamingSocketManagement::ConnectionContext::txPushPing()
{
    CoTMessage *msg = new CoTMessage(logger, myPingUid);
    try {
InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "ping CoT message to server");
        txQueue.push_front(TxQueueItem(this, extensions,
                                            msg,
                                            txQueueProtoVersion));
    } catch (std::invalid_argument &) {
        delete msg;
    }
}




StreamingSocketManagement::TcpConnectionContext::TcpConnectionContext(
        CommoLogger *logger,
        ExtensionRegistry *extensions,
        const std::string &myPingUid,
        const std::string &epString,
        const std::string &epAddrString,
        uint16_t port,
        NetAddress *ep) : 
            ConnectionContext(logger, extensions, myPingUid,
                              epString, epAddrString,
                              port, ep, TRANSPORT_TCP),
            socket(NULL)
{
}

StreamingSocketManagement::TcpConnectionContext::TcpConnectionContext(
        CommoLogger *logger,
        ExtensionRegistry *extensions,
        const std::string &myPingUid,
        const std::string &epString,
        const std::string &epAddrString,
        uint16_t port,
        NetAddress *ep, StreamingTransport transport) : 
            ConnectionContext(logger, extensions, myPingUid, 
                              epString, epAddrString,
                              port, ep, transport),
            socket(NULL)
{
}

bool StreamingSocketManagement::TcpConnectionContext::connectionInitSocket()
        COMMO_THROW (SocketException)
{
    socket = new TcpSocket(remoteEndpointAddr->family, false);
    return socket->connect(remoteEndpointAddr);
}

bool StreamingSocketManagement::TcpConnectionContext::ioReadReady(
    NetSelector *selector)
{
    return selector->getLastReadState(socket) == NetSelector::READABLE;
}


size_t StreamingSocketManagement::TcpConnectionContext::ioRead(
    uint8_t *data, size_t len) COMMO_THROW (SocketException)
{
    return socket->read(data, len);
}

bool StreamingSocketManagement::TcpConnectionContext::ioWriteReady(
    NetSelector *selector)
{
    return !selector || 
           selector->getLastWriteState(socket) == NetSelector::WRITABLE;
}

size_t StreamingSocketManagement::TcpConnectionContext::ioWrite(
    const uint8_t *data, size_t len) COMMO_THROW (SocketException)
{
    return socket->write(data, len);
}

void StreamingSocketManagement::TcpConnectionContext::reset(
    CommoTime nextConnTime, bool clearIo)
{
    if (socket) {
        delete socket;
        socket = NULL;
    }
    ConnectionContext::reset(nextConnTime, clearIo);
}

Socket *StreamingSocketManagement::TcpConnectionContext::getSocket()
{
    return socket;
}

StreamingSocketManagement::TcpConnectionContext::~TcpConnectionContext()
{
    delete socket;
}




StreamingSocketManagement::SSLConnectionContext::SSLConnectionContext(
        CommoLogger *logger,
        ExtensionRegistry *extensions,
        const std::string &myPingUid,
        const std::string &epString,
        const std::string &epAddrString,
        uint16_t port,
        NetAddress *ep,
        const std::string &myuid,
        SSL_CTX* sslCtx, const uint8_t* clientCert, size_t clientCertLen,
        const uint8_t* caCertBuf, size_t caCertBufLen, const char* certPassword,
        const char *caCertPassword,
        const char *username, const char *password)
                COMMO_THROW (SSLArgException) :
                TcpConnectionContext(logger, extensions, myPingUid,
                                     epString, epAddrString,
                                     port, ep, TRANSPORT_SSL),
                sslCtx(sslCtx),
                ssl(NULL),
                writeState(WANT_NONE),
                readState(WANT_NONE),
                sslConfig(NULL),
                authMessage(),
                fatallyErrored(false)
{
    if (!sslCtx)
        throw SSLArgException(COMMO_ILLEGAL_ARGUMENT, "SSL is unavailable - did you initialize SSL libraries?");

    sslConfig = new SSLConfiguration(clientCert, clientCertLen,
                                     caCertBuf, caCertBufLen, certPassword,
                                     caCertPassword);

    if (username && password && username[0] != '\0') {
        xmlDoc *doc = xmlNewDoc((const xmlChar *)"1.0");
        xmlNode *authElement = xmlNewNode(NULL, (const xmlChar *)"auth");
        xmlDocSetRootElement(doc, authElement);
        xmlNode *cotElement = xmlNewChild(authElement, NULL, 
                                          (const xmlChar *)"cot", NULL);
        xmlNewProp(cotElement, (const xmlChar *)"username",
                   (const xmlChar *)username);
        xmlNewProp(cotElement, (const xmlChar *)"password",
                   (const xmlChar *)password);
        xmlNewProp(cotElement, (const xmlChar *)"uid",
                   (const xmlChar *)myuid.c_str());
        
        xmlChar *outPtr = NULL;
        int outSize;
        xmlDocDumpFormatMemory(doc, &outPtr, &outSize, 0);
        if (outPtr == NULL) {
            throw SSLArgException(COMMO_ILLEGAL_ARGUMENT, 
                    "unable to create auth document - unknown error");
        }
        if (outSize > 0 && outPtr[outSize - 1] == '\n')
            // Remove trailing newline - this is needed by at least streaming to TAK server
            outSize--;

        authMessage = std::string((const char *)outPtr, outSize);
        xmlFree(outPtr);
        xmlFreeDoc(doc);
    }

}

StreamingSocketManagement::SSLConnectionContext::~SSLConnectionContext()
{
    delete sslConfig;
    if (ssl) {
        if (!fatallyErrored)
            SSL_shutdown(ssl);
        SSL_free(ssl);
        ssl = NULL;
    }
}

bool StreamingSocketManagement::SSLConnectionContext::connectionInProgress()
{
    return writeState == SSLConnectionContext::WANT_NONE;
}

bool StreamingSocketManagement::SSLConnectionContext::connectionDoPost(bool *rebuildSelect)
    COMMO_THROW (SocketException)
{
    if (!ssl) {
        // Need to make ssl context and associate fd
        ssl = SSL_new(sslCtx);
        if (!ssl)
            throw SocketException(netinterfaceenums::ERR_INTERNAL,
                                  "ssl context creation failed");
        // Set certs and key into the context.
        // These functions use the pointers as-is, no copies
        SSL_use_certificate(ssl, sslConfig->cert);
        SSL_use_PrivateKey(ssl, sslConfig->key);

        if (SSL_set_fd(ssl, (int)socket->getFD()) != 1)
            throw SocketException(netinterfaceenums::ERR_INTERNAL,
                                  "associating fd with ssl context failed");
    }

    ERR_clear_error();
    int r = SSL_connect(ssl);
    if (r == 1) {
        // openssl's cert verification during connection cannot be made
        // to use an in-memory certificate (only files, and even then seems
        // only globally on SSL_CTX and not per-connection like we need).
        // So instead we verify ourselves here
        // NOTE: we *intentionally* do not check authenticity of peer
        // certificate by CN v. hostname comparison or the like (per
        // ATAK sources at time of writing)
        X509 *cert = SSL_get_peer_certificate(ssl);
        if (!cert)
            throw SocketException(netinterfaceenums::ERR_CONN_SSL_NO_PEER_CERT,
                                  "server did not provide a certificate");

        bool certOk = sslConfig->certChecker->checkCert(cert);
        X509_free(cert);

        if (!certOk) {
            int vResult = sslConfig->certChecker->getLastErrorCode();
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "Server cert verification failed: %d - check truststore for this connection", vResult);
            throw SocketException(netinterfaceenums::ERR_CONN_SSL_PEER_CERT_NOT_TRUSTED,
                                  "server certificate failed verification - check truststore");
        }

        // Clear writeState now that we are connected!
        writeState = SSLConnectionContext::WANT_NONE;

        // If there is an auth document, push it on to the tx queue to get sent
        if (!authMessage.empty()) {
            txQueue.push_front(TxQueueItem(this, authMessage));
        }

        return true;
    }
    if (r == 0)
        throw SocketException(netinterfaceenums::ERR_CONN_SSL_HANDSHAKE,
                              "fatal error performing ssl handshake");
    else {
        r = SSL_get_error(ssl, r);
        // if it changes, flag for connection rebuild
        if (r == SSL_ERROR_WANT_READ || r == SSL_ERROR_WANT_WRITE) {
            SSLConnectionContext::SSLWantState newState;
            switch (r) {
            case SSL_ERROR_WANT_READ:
                newState = SSLConnectionContext::WANT_READ;
                break;
            case SSL_ERROR_WANT_WRITE:
                newState = SSLConnectionContext::WANT_WRITE;
                break;
            }
            if (newState != writeState) {
                writeState = newState;
                // Flag for select rebuild
                *rebuildSelect = true;
            }
            return false;
        } else {
            if (r == SSL_ERROR_SSL) {
                char msg[1024];
                ERR_error_string_n(ERR_get_error(), msg, sizeof(msg));
                msg[1023] = '\0';
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "Error making SSL connection: %s", msg);
                fatallyErrored = true;
            } else if (r == SSL_ERROR_SYSCALL) {
                fatallyErrored = true;
            }
            throw SocketException(netinterfaceenums::ERR_CONN_SSL_HANDSHAKE,
                                  "unexpected error in ssl handshake");
        }
    }
}


bool StreamingSocketManagement::SSLConnectionContext::connectionPostWantsRead()
{
    return writeState == SSLConnectionContext::WANT_READ;
}

bool StreamingSocketManagement::SSLConnectionContext::connectionPostWantsWrite()
{
    return writeState == SSLConnectionContext::WANT_WRITE;
}

bool StreamingSocketManagement::SSLConnectionContext::ioWantsWrite()
{
    return writeState == SSLConnectionContext::WANT_WRITE ||
           readState == SSLConnectionContext::WANT_WRITE;
}

bool StreamingSocketManagement::SSLConnectionContext::ioReadReady(
    NetSelector *selector)
{
    return (readState == SSLConnectionContext::WANT_WRITE && 
            selector->getLastWriteState(socket) == NetSelector::WRITABLE) ||
           (readState != SSLConnectionContext::WANT_WRITE && 
            selector->getLastReadState(socket) == NetSelector::READABLE);
}

size_t StreamingSocketManagement::SSLConnectionContext::ioRead(
    uint8_t *data, size_t len) COMMO_THROW (SocketException)
{
    ERR_clear_error();
    int n = SSL_read(ssl, data, (int)len);
    SSLConnectionContext::SSLWantState newState = SSLConnectionContext::WANT_NONE;
    if (n <= 0) {
        int n0 = n;
        n = SSL_get_error(ssl, n);

        switch (n) {
        case SSL_ERROR_WANT_READ:
            newState = SSLConnectionContext::WANT_READ;
            break;
        case SSL_ERROR_WANT_WRITE:
            newState = SSLConnectionContext::WANT_WRITE;
            break;
        case SSL_ERROR_SSL:
        case SSL_ERROR_SYSCALL:
            fatallyErrored = true;
            // Intentionally fall-through
        default:
            InternalUtils::logprintf(logger, 
                    CommoLogger::LEVEL_ERROR,
                    "SSL Read fatal error - %d, %d", 
                    n0,
                    n);
            throw SocketException();
        }

        n = 0;
    }
    readState = newState;

    return n;
}

bool StreamingSocketManagement::SSLConnectionContext::ioWriteReady(
    NetSelector *selector)
{
    return writeState == SSLConnectionContext::WANT_NONE ||
           (selector && 
               ((writeState == SSLConnectionContext::WANT_WRITE &&
                 selector->getLastWriteState(socket) == NetSelector::WRITABLE) ||
                (writeState == SSLConnectionContext::WANT_READ &&
                 selector->getLastReadState(socket) == NetSelector::READABLE)
               )
           );
}

size_t StreamingSocketManagement::SSLConnectionContext::ioWrite(
    const uint8_t *data, size_t dataLen) COMMO_THROW (SocketException)
{
    ERR_clear_error();
    int n = SSL_write(ssl, data, (int)dataLen);
    SSLConnectionContext::SSLWantState newState = SSLConnectionContext::WANT_NONE;
    if (n <= 0) {
        int n0 = n;
        n = SSL_get_error(ssl, n);

        switch (n) {
        case SSL_ERROR_WANT_READ:
            newState = SSLConnectionContext::WANT_READ;
            break;
        case SSL_ERROR_WANT_WRITE:
            newState = SSLConnectionContext::WANT_WRITE;
            break;
        case SSL_ERROR_SSL:
        case SSL_ERROR_SYSCALL:
            fatallyErrored = true;
            // Intentionally fall-through
        default:
            InternalUtils::logprintf(logger, 
                    CommoLogger::LEVEL_ERROR,
                    "SSL Write fatal error - %d, %d", 
                    n0,
                    n);
            throw SocketException();
        }

        n = 0;
    }
    writeState = newState;

    return n;
}

void StreamingSocketManagement::SSLConnectionContext::reset(
    CommoTime nextConnTime, bool clearIo)
{
    writeState = SSLConnectionContext::WANT_NONE;
    readState = SSLConnectionContext::WANT_NONE;
    if (ssl) {
        if (!fatallyErrored)
            SSL_shutdown(ssl);
        SSL_free(ssl);
        ssl = NULL;
    }
    fatallyErrored = false;
    TcpConnectionContext::reset(nextConnTime, clearIo);
}

StreamingSocketManagement::SSLConfiguration *
StreamingSocketManagement::SSLConnectionContext::getSSLConfig()
{
    return sslConfig;
}







StreamingSocketManagement::QuicConnectionContext::QuicConnectionContext(
        CommoLogger *logger,
        ExtensionRegistry *extensions,
        const std::string &myPingUid,
        const std::string &epString,
        const std::string &epAddrString,
        uint16_t port,
        NetAddress *ep,
        const std::string &myuid,
        const uint8_t* clientCert, size_t clientCertLen,
        const uint8_t* caCertBuf, size_t caCertBufLen, const char* certPassword,
        const char *caCertPassword,
        const char *username, const char *password)
                COMMO_THROW (SSLArgException) :
                ConnectionContext(logger, extensions, myPingUid,
                                     epString, epAddrString,
                                     port, ep, TRANSPORT_QUIC),
                socket(NULL),
                quicConn(NULL),
                sslConfig(NULL),
                authMessage()
{
    sslConfig = new SSLConfiguration(clientCert, clientCertLen,
                                     caCertBuf, caCertBufLen, certPassword,
                                     caCertPassword);

    try {
        generateAuthDoc(&authMessage, myuid, username, password);
    } catch (SSLArgException &) {
        delete sslConfig;
        throw;
    }
}

StreamingSocketManagement::QuicConnectionContext::~QuicConnectionContext()
{
    delete sslConfig;
    delete quicConn;
    delete socket;
    delete localAddr;
}



Socket *StreamingSocketManagement::QuicConnectionContext::getSocket()
{
    return socket;
}

bool StreamingSocketManagement::QuicConnectionContext::connectionInitSocket()
        COMMO_THROW (SocketException)
{
    socket = new UdpSocket(remoteEndpointAddr, false);
    localAddr = socket->getBoundAddr();
    
    return true;
}

bool StreamingSocketManagement::QuicConnectionContext::connectionInProgress()
{
    return false;
}

bool StreamingSocketManagement::QuicConnectionContext::connectionDoPost(bool *rebuildSelect)
    COMMO_THROW (SocketException)
{
    if (!quicConn) {
        try {
            // Need to make quic context
            quicConn = new StreamingQuicConnection(logger,
                                                   *localAddr,
                                                   *remoteEndpointAddr,
                                                   // connection timeout is
                                                   // watched at upper
                                                   // layer, so use a big number
                                                   60 * 15,
                                                   sslConfig);
            *rebuildSelect = true;
        } catch (std::invalid_argument &) {
            throw SocketException(netinterfaceenums::ERR_INTERNAL,
                                  "quic context creation failed");
        }
    }

    bool hadTx = quicConn->hasTxData();

    try {
        // Write to kick off connection (in the case of new) or 
        // to push out any pending data before we read
        quicConn->write(socket, NULL, 0);

        // Read any new data. this might generate new tx data as well
        quicConn->read(socket, *localAddr, NULL, 0);
    } catch (SocketException &) {
        // exceptions from read/write don't indicate connect err codes
        throw SocketException(ERR_CONN_OTHER, "quic IO error during handshake");
    }

    // Check if handshake completed
    if (quicConn->isHandshakeComplete()) {
        // If there is an auth document, push it on to the tx queue to get sent
        if (!authMessage.empty()) {
            txQueue.push_front(TxQueueItem(this, authMessage));
        } else {
            // otherwise push on a ping message because tak server's quic
            // support has an issue where it won't send data to us until we
            // send something to it
            txPushPing();
        }

        return true;
    } else if (hadTx != quicConn->hasTxData()) {
        *rebuildSelect = true;
    }
    return false;
}


bool StreamingSocketManagement::QuicConnectionContext::connectionPostWantsRead()
{
    return true;
}

bool StreamingSocketManagement::QuicConnectionContext::connectionPostWantsWrite()
{
    return quicConn->hasTxData();
}

bool StreamingSocketManagement::QuicConnectionContext::ioReadReady(
    NetSelector *selector)
{
    return quicConn->hasBufferedData() ||
           selector->getLastReadState(socket) == NetSelector::READABLE;
}

size_t StreamingSocketManagement::QuicConnectionContext::ioRead(
    uint8_t *data, size_t len) COMMO_THROW (SocketException)
{
    return quicConn->read(socket, *localAddr, data, len);
}

bool StreamingSocketManagement::QuicConnectionContext::ioWantsWrite()
{
    return quicConn->hasTxData() || ConnectionContext::ioWantsWrite();
}

bool StreamingSocketManagement::QuicConnectionContext::ioWriteReady(
    NetSelector *selector)
{
    return !selector || 
           selector->getLastWriteState(socket) == NetSelector::WRITABLE;
}

size_t StreamingSocketManagement::QuicConnectionContext::ioWrite(
    const uint8_t *data, size_t dataLen) COMMO_THROW (SocketException)
{
    return quicConn->write(socket, data, dataLen);
}

void StreamingSocketManagement::QuicConnectionContext::ioWriteFlush()
                                         COMMO_THROW (SocketException)
{
    quicConn->write(socket, NULL, 0);
}

long StreamingSocketManagement::QuicConnectionContext::handleExpirations()
                                         COMMO_THROW (SocketException)
{
    ngtcp2_tstamp now = QuicConnection::gents();
    ngtcp2_tstamp exTime = quicConn->getExpTime();
    if (now >= exTime) {
        if (!quicConn->handleExpiration(socket))
            throw SocketException();
        exTime = quicConn->getExpTime();
    }
    ngtcp2_tstamp ret = exTime - now;
    ret /= 1000000;
    if (ret > LONG_MAX)
        return LONG_MAX;
    return (long)ret;
}

void StreamingSocketManagement::QuicConnectionContext::reset(
    CommoTime nextConnTime, bool clearIo)
{
    delete quicConn;
    quicConn = NULL;
    delete localAddr;
    localAddr = NULL;
    delete socket;
    socket = NULL;
    ConnectionContext::reset(nextConnTime, clearIo);
}

StreamingSocketManagement::SSLConfiguration *
StreamingSocketManagement::QuicConnectionContext::getSSLConfig()
{
    return sslConfig;
}


StreamingSocketManagement::StreamingQuicConnection::StreamingQuicConnection(
                            CommoLogger *logger,
                            const NetAddress &localAddr,
                            const NetAddress &remoteEndpointAddr,
                            float connTimeoutSec,
                            SSLConfiguration *sslConfig)
                            COMMO_THROW (std::invalid_argument) :
        QuicConnection(logger,
                       ALPN_STREAMING,
                       sizeof(ALPN_STREAMING), 
                       localAddr,
                       remoteEndpointAddr,
                       connTimeoutSec,
                       100,
                       sslConfig->cert,
                       sslConfig->key,
                       sslConfig->certChecker),
        handshakeErr(netinterfaceenums::ERR_OTHER),
        handshakeErrInfo(NULL),
        handshakeIsComplete(false),
        curTxBuf(NULL),
        curTxLen(0),
        curTxWritten(0),
        curRxBuf(NULL),
        curRxLen(0),
        curRxRead(0),
        rxStreamBuf(),
        rxStreamBufLen(0)
{
}

StreamingSocketManagement::StreamingQuicConnection::~StreamingQuicConnection()
{
}

bool StreamingSocketManagement::StreamingQuicConnection::handleExpiration(UdpSocket *socket) COMMO_THROW (SocketException)
{
    curTxBuf = NULL;
    return QuicConnection::handleExpiration(socket);
}

bool StreamingSocketManagement::StreamingQuicConnection::isHandshakeComplete()
                                                 COMMO_THROW(SocketException)
{
    if (handshakeErrInfo)
        throw SocketException(handshakeErr, handshakeErrInfo);
    return handshakeIsComplete;
}

size_t StreamingSocketManagement::StreamingQuicConnection::write(
                                      UdpSocket *sock,
                                      const uint8_t *data,
                                      size_t dataLen)
                                      COMMO_THROW(SocketException)
{
    curTxBuf = data;
    curTxLen = dataLen;
    curTxWritten = 0;
    if (!transmitPkts(sock)) {
        // XXX - better error
        throw SocketException(netinterfaceenums::ERR_INTERNAL,
                              "quic fatal error");
    }
    return curTxWritten;
}

size_t StreamingSocketManagement::StreamingQuicConnection::read(
                                      UdpSocket *socket,
                                      const NetAddress &localAddr,
                                      uint8_t *data,
                                      size_t dataLen)
                                      COMMO_THROW(SocketException)
{
    curRxBuf = data;
    curRxLen = dataLen;
    curRxRead = 0;
    curTxBuf = NULL;
    
    // Copy in from stream buf first
    if (data) {
        size_t n = rxStreamBufLen;
        bool usingAll = n > dataLen;
        if (usingAll)
            n = dataLen;
        memcpy(data, rxStreamBuf, n);
        // Shift buffer down. Could swap to circular buffer
        memmove(rxStreamBuf, rxStreamBuf + n, rxStreamBufLen - n);
        rxStreamBufLen -= n;
        if (usingAll)
            // Used all our read buffer, nothing left
            return n;
        curRxRead += n;
    }
    
    const size_t pktbufsize = 65536;
    uint8_t pktbuf[pktbufsize];
    
    bool ok = true;
    int pktCount = 0;
    while (pktCount++ < 10 && ok && 
            ((curRxBuf && curRxRead < dataLen) || !handshakeIsComplete)) {
        NetAddress *sourceAddr = NULL;
        size_t len = pktbufsize;

        try {
            socket->recvfrom(&sourceAddr, pktbuf, &len);
        } catch (SocketWouldBlockException &) {
            break;
        }

        ok = processPkt(socket, NULL, localAddr, sourceAddr, pktbuf, len);
    }
    ok = ok && transmitPkts(socket);
    if (!ok)
        // XXX - better error
        throw SocketException(netinterfaceenums::ERR_INTERNAL,
                              "quic fatal error");

    return curRxRead;
}
    
bool StreamingSocketManagement::StreamingQuicConnection::hasBufferedData()
{
    return rxStreamBufLen != 0;
}

bool StreamingSocketManagement::StreamingQuicConnection::isTxSourceDone() const
{
    // streaming connection has no end
    return false;
}

bool StreamingSocketManagement::StreamingQuicConnection::txSourceFill(
                                       uint8_t *buf,
                                       size_t *len,
                                       int *errCode)
{
    if (!curTxBuf) {
        *len = 0;
        return true;
    }
    size_t n = curTxLen - curTxWritten;
    if (*len > n) {
        *len = n;
    }
    
    memcpy(buf, curTxBuf, *len);
    curTxWritten += *len;
    return true;
}

bool StreamingSocketManagement::StreamingQuicConnection::receivedData(
                                       const uint8_t *data,
                                       size_t dataLen,
                                       bool finFlagged)
{
    size_t rxSpace = curRxLen - curRxRead;
    size_t n = dataLen;
    
    if (n < rxSpace) {
        memcpy(curRxBuf + curRxRead, data, n);
        curRxRead += n;
        return true;
    }
    
    // else we have not enough space, so copy what we can first
    memcpy(curRxBuf + curRxRead, data, rxSpace);
    curRxRead += rxSpace;
    data += rxSpace;
    dataLen -= rxSpace;
    // and then copy remainder to stream buf if we can
    if (dataLen > rxStreamBufSize - rxStreamBufLen)
        return false;
    memcpy(rxStreamBuf + rxStreamBufLen, data, dataLen);
    rxStreamBufLen += dataLen;
    return true;
}

bool StreamingSocketManagement::StreamingQuicConnection::wantsBiDir()
{
    return true;
}

void StreamingSocketManagement::StreamingQuicConnection::handshakeError(
                                          netinterfaceenums::NetInterfaceErrorCode errCode,
                                          const char *info)
{
    handshakeErr = errCode;
    handshakeErrInfo = info;
}

bool StreamingSocketManagement::StreamingQuicConnection::handshakeComplete(
                                          const uint8_t *alpn,
                                          unsigned int alpnLen)
{
    if (!alpn || alpnLen != ALPN_STREAMING[0] || 
                 memcmp(alpn, ALPN_STREAMING + 1, alpnLen) != 0) {
        if (alpn)
            handshakeErrInfo = "Invalid protocol identifier from server";
        else
            handshakeErrInfo = "Server did not negotiate a protocol identifier";
        handshakeErr = netinterfaceenums::ERR_CONN_SSL_HANDSHAKE;
        return false;
    }
    handshakeIsComplete = true;
    return true;
}



StreamingSocketManagement::SSLConfiguration::SSLConfiguration(
        const uint8_t *clientCert,
        size_t clientCertLen,
        const uint8_t *caCertBuf, size_t caCertBufLen,
        const char *certPassword,
        const char *caCertPassword)
                COMMO_THROW (SSLArgException) :
                cert(NULL),
                key(NULL),
                certChecker(),
                caCerts(NULL)
{
    // Attempt to parse cert info
    EVP_PKEY *privKey;
    X509 *cert;
    InternalUtils::readCert(clientCert, clientCertLen,
                            certPassword, &cert, &privKey);

    EVP_PKEY *caPrivKey = NULL;
    SSLCertChecker *certChecker = NULL;
    int nCaCerts = 0;
    
    try {
    InternalUtils::readCACerts(caCertBuf, caCertBufLen,
            caCertPassword, &caCerts, &nCaCerts);
    
    certChecker = new SSLCertChecker(caCerts, nCaCerts);

    } catch (SSLArgException &e) {
        if (certChecker)
            delete certChecker; 
        if (caCerts)
            sk_X509_pop_free(caCerts, X509_free);
        X509_free(cert);
        EVP_PKEY_free(caPrivKey);

        throw e;
    }

    this->certChecker = certChecker;
    this->cert = cert;
    this->key = privKey;

}

StreamingSocketManagement::SSLConfiguration::~SSLConfiguration()
{
    sk_X509_pop_free(caCerts, X509_free);
    delete certChecker;
    EVP_PKEY_free(key);
    X509_free(cert);
}


