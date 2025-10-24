#ifndef IMPL_TCPSOCKETMANAGEMENT_H_
#define IMPL_TCPSOCKETMANAGEMENT_H_

#include "peermessagingbase.h"
#include "netsocket.h"
#include "cotmessage.h"
#include "commoresult.h"
#include "commologger.h"
#include "commotime.h"
#include "resolverqueue.h"
#include "cryptoutil.h"
#include "commothread.h"
#include <set>

namespace atakmap {
namespace commoncommo {
namespace impl
{


class TcpSocketManagement : public PeerMessagingBase
{
public:
    TcpSocketManagement(CommoLogger *logger,
                        ContactUID *ourUid, ExtensionRegistry *extensions,
                        CoTSendFailureListenerSet *failureListeners,
                        thread::Mutex *failureListenersMutex);
    virtual ~TcpSocketManagement();



    TcpInboundNetInterface *addInboundInterface(int port);
    CommoResult removeInboundInterface(TcpInboundNetInterface *iface);

    void setCryptoKeys(const uint8_t *authKey, const uint8_t *cryptoKey);

private:
    struct InboundContext;
    struct ClientContext;
    struct IBPortComp
    {
        bool operator()(const InboundContext * const &a, const InboundContext * const &b) const
        {
            return a->port < b->port;
        }
    };
    typedef std::set<InboundContext *, IBPortComp> InboundCtxSet;
    typedef std::set<ClientContext *> ClientCtxSet;


    struct InboundContext : public TcpInboundNetInterface
    {
        TcpSocket *socket;
        CommoTime retryTime;

        const uint16_t localPort;
        NetAddress *localAddr; // includes port
        
        std::string endpoint;
        
        bool upEventFired;

        InboundContext(uint16_t localPort);
        ~InboundContext();
        
        void initSocket() COMMO_THROW (SocketException);

    private:
        COMMO_DISALLOW_COPY(InboundContext);
    };
    
    struct ClientContext
    {
        NetAddress *clientAddr;
        std::string endpoint;

        TcpSocket *socket;
        uint8_t *data;
        size_t len;
        size_t bufLen;
        
        ClientContext(TcpSocket *socket, NetAddress *clientAddr,
                      const std::string &endpoint);
        ~ClientContext();
        
        void growCapacity(size_t n);
        void clearBuffers();
    private:
        COMMO_DISALLOW_COPY(ClientContext);
    };


    struct TcpTxContext : public TxContext
    {
        // Socket
        TcpSocket *socket;
        
        bool isConnecting;
        // Time at which in-progress connect should time out
        CommoTime timeout;
        
        TcpTxContext(const std::string &host, uint16_t port,
                  MeshNetCrypto *crypto, const CoTMessage *msg, 
                  ContactUID *ourUid,
                  ExtensionRegistry *extensions,
                  const ExtensionIdSet &enabledExtensions,
                  int protoVersion) COMMO_THROW (std::invalid_argument);
        // close+delete socket if !NULL, delete destination
        // delete cotmsg
        virtual ~TcpTxContext();

    private:
        COMMO_DISALLOW_COPY(TcpTxContext);
    };


    typedef std::set<TcpTxContext *> TxCtxSet;


    MeshNetCrypto *txCrypto;   // lock on txMutex

    // Next 2 protected by globalIOMutex
    InboundCtxSet inboundContexts;
    bool inboundNeedsRebuild;

    ClientCtxSet clientContexts;  // access only on io thread

    TxCtxSet txContexts;
    bool txNeedsRebuild;
    thread::Mutex txMutex;
    

protected:
    virtual void ioThreadProcess();
    virtual TxContext *createTxContext(const std::string &host, uint16_t port,
                  const CoTMessage *msg, 
                  ContactUID *ourUid,
                  ExtensionRegistry *extensions,
                  const ExtensionIdSet &enabledExtensions,
                  int protoVersion) COMMO_THROW (std::invalid_argument);
    virtual void txCtxReadyForIO(TxContext *txCtx);

private:
    COMMO_DISALLOW_COPY(TcpSocketManagement);

    void queueTxErrAndKillCtx(TcpTxContext *ctx, const std::string &reason, 
                              bool removeFromCtxSet);
    void killTxCtx(TcpTxContext *ctx, bool removeFromCtxSet);
    void ioThreadResetInboundCtx(InboundContext *ctx, bool flagReset);
    void ioThreadQueueRx(ClientContext *ctx);

};

}
}
}


#endif
