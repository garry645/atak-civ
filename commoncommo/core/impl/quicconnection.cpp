#include "quicconnection.h"
#include "internalutils.h"

#include "ngtcp2/ngtcp2_crypto_quictls.h"
#include "openssl/ssl.h"
#include "openssl/rand.h"

#include <string.h>
#include <chrono>

using namespace atakmap::commoncommo;
using namespace atakmap::commoncommo::impl;


namespace {
    void rand_cb_impl(uint8_t *dest, size_t destlen, const ngtcp2_rand_ctx *randctx) {
        RAND_bytes(dest, (int)destlen);
    }

}


ngtcp2_tstamp QuicConnection::gents() {
    return std::chrono::duration_cast<std::chrono::nanoseconds>(
             std::chrono::steady_clock::now().time_since_epoch())
      .count();
}
    
QuicConnection::QuicConnection(CommoLogger *logger,
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
                            COMMO_THROW(std::invalid_argument) :
        logger(logger),
        sslCtx(NULL),
        ssl(NULL),
        ngConnRef{getConnFromRefCb, this},
        ngConn(NULL),
        state(Prehandshake),
        newBidirStream(false),
        streamOpen(false),
        streamId(0),
        certChecker(NULL),
        scids(),
        allowedAlpns(allowedAlpns),
        allowedAlpnsLen(allowedAlpnsLen),
        remoteClientAddr(NetAddress::duplicateAddress(clientAddr)),
        isServer(true),
        txBufs(),
        txBuffer(NULL),
        txBufferSize(0),
        txLen(0),
        txSent(0),
        txAckCount(0),
        expTime(UINT64_MAX),
        closePkt(),
        closePktLen(0)
{
    try {
        ngtcp2_settings settings;
        ngtcp2_transport_params params;

        commonParamInit(&settings, &params, connTimeoutSec, 20);
        params.original_dcid = dcidFromClient;
        params.original_dcid_present = 1;
        params.initial_max_streams_uni = 1;
        params.initial_max_streams_bidi = 1;
        
        ngtcp2_cid scid;
        scid.datalen = QUIC_SERV_SCID_LEN;
        if (RAND_bytes(scid.data, (int)scid.datalen) != 1)
            throw std::invalid_argument("RNG failure");

        ngtcp2_path path;
        memset(&path, 0, sizeof(ngtcp2_path));
        ngtcp2_addr_init(&path.local, localAddr.getSockAddr(), localAddr.getSockAddrLen());
        ngtcp2_addr_init(&path.remote, clientAddr->getSockAddr(), clientAddr->getSockAddrLen());
        
        ngtcp2_callbacks callbacks;
        memset(&callbacks, 0, sizeof(ngtcp2_callbacks));
        // required for server side
        callbacks.recv_client_initial = ngtcp2_crypto_recv_client_initial_cb;
        callbacks.recv_crypto_data = ngtcp2_crypto_recv_crypto_data_cb;
        callbacks.encrypt = ngtcp2_crypto_encrypt_cb;
        callbacks.decrypt = ngtcp2_crypto_decrypt_cb;
        callbacks.hp_mask = ngtcp2_crypto_hp_mask_cb;
        callbacks.rand = rand_cb_impl;
        callbacks.get_new_connection_id = getNewConnectionIdCb;
        callbacks.update_key = ngtcp2_crypto_update_key_cb;
        callbacks.delete_crypto_aead_ctx = ngtcp2_crypto_delete_crypto_aead_ctx_cb;
        callbacks.delete_crypto_cipher_ctx = ngtcp2_crypto_delete_crypto_cipher_ctx_cb;
        callbacks.get_path_challenge_data = ngtcp2_crypto_get_path_challenge_data_cb;
        callbacks.version_negotiation = ngtcp2_crypto_version_negotiation_cb;
        // end required
        callbacks.acked_stream_data_offset = ackedStreamDataCb;
        callbacks.handshake_completed = handshakeCompleteCb;
        callbacks.recv_stream_data = recvStreamDataCb;
        callbacks.stream_open = streamOpenCb;
        callbacks.stream_close = streamCloseCb;
        callbacks.remove_connection_id = removeConnectionIdCb;
        
        if (ngtcp2_conn_server_new(&ngConn,
                                   &scidFromClient,
                                   &scid,
                                   &path,
                                   version,
                                   &callbacks,
                                   &settings,
                                   &params,
                                   NULL,
                                   this) != 0)
            throw std::invalid_argument("Could not create ngtcp2 server");

        // Set up TLS/SSL layer
        sslCtx = SSL_CTX_new(TLS_server_method());
        if (!sslCtx)
            throw std::invalid_argument("Could create TLS layer");
        
        if (ngtcp2_crypto_quictls_configure_server_context(sslCtx) != 0)
            throw std::invalid_argument("ngtcp2 config of TLS layer failed");
        
        SSL_CTX_set_alpn_select_cb(sslCtx, selectAlpnSSLCb, this);
        
        if (SSL_CTX_use_cert_and_key(sslCtx, cert, privKey, supportCerts, 1) != 1)
            throw std::invalid_argument("Could not configure TLS layer with provided cert");
        
        if (SSL_CTX_check_private_key(sslCtx) != 1)
            throw std::invalid_argument("Priv key not valid for cert");
        
        ssl = SSL_new(sslCtx);
        if (!ssl)
            throw std::invalid_argument("Could not create TLS session");
        
        SSL_set_app_data(ssl, &ngConnRef);
        SSL_set_accept_state(ssl);
        ngtcp2_conn_set_tls_native_handle(ngConn, ssl);

        txBufferSize = 2 * STREAM_FLOW_CTRL_BYTES;
        txBuffer = new uint8_t[txBufferSize];


    } catch (std::invalid_argument &ex) {
        InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR,
                "quic QuicConnection create as server failed (%s)",
                ex.what());
        destroyInternals();
        throw;
    }
    InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
            "quic %p QuicConnection create as server", this);
}

QuicConnection::QuicConnection(CommoLogger *logger,
                               const uint8_t *alpn,
                               size_t alpnLen,
                               const NetAddress &localAddr,
                               const NetAddress &serverAddr,
                               float connTimeoutSec,
                               float idleTimeoutSec,
                               X509 *cert,
                               EVP_PKEY *privKey,
                               SSLCertChecker *certChecker)
                            COMMO_THROW(std::invalid_argument) :
        logger(logger),
        sslCtx(NULL),
        ssl(NULL),
        ngConnRef{getConnFromRefCb, this},
        ngConn(NULL),
        state(Prehandshake),
        newBidirStream(false),
        streamOpen(false),
        streamId(0),
        certChecker(certChecker),
        scids(),
        allowedAlpns(NULL),
        allowedAlpnsLen(0),
        remoteClientAddr(NULL),
        isServer(false),
        txBufs(),
        txBuffer(NULL),
        txBufferSize(0),
        txLen(0),
        txSent(0),
        txAckCount(0),
        expTime(UINT64_MAX),
        closePkt(),
        closePktLen(0)
{
    try {
        ngtcp2_settings settings;
        ngtcp2_transport_params params;

        commonParamInit(&settings, &params, connTimeoutSec, idleTimeoutSec);
        // We leave max_streams_* at 0 default as
        // they specify remote stream allowance and we (the client)
        // are the one making streams

        ngtcp2_cid scid;
        ngtcp2_cid dcid;
        scid.datalen = dcid.datalen = QUIC_SERV_SCID_LEN;
        if (RAND_bytes(scid.data, (int)scid.datalen) != 1 ||
                RAND_bytes(dcid.data, (int)dcid.datalen) != 1)
            throw std::invalid_argument("RNG failure");

        ngtcp2_path path;
        memset(&path, 0, sizeof(ngtcp2_path));
        ngtcp2_addr_init(&path.local, localAddr.getSockAddr(), localAddr.getSockAddrLen());
        ngtcp2_addr_init(&path.remote, serverAddr.getSockAddr(), serverAddr.getSockAddrLen());
        
        ngtcp2_callbacks callbacks;
        memset(&callbacks, 0, sizeof(ngtcp2_callbacks));
        // required for client side
        callbacks.client_initial = ngtcp2_crypto_client_initial_cb;
        callbacks.recv_crypto_data = ngtcp2_crypto_recv_crypto_data_cb;
        callbacks.encrypt = ngtcp2_crypto_encrypt_cb;
        callbacks.decrypt = ngtcp2_crypto_decrypt_cb;
        callbacks.hp_mask = ngtcp2_crypto_hp_mask_cb;
        callbacks.recv_retry = ngtcp2_crypto_recv_retry_cb;
        callbacks.rand = rand_cb_impl;
        callbacks.get_new_connection_id = getNewConnectionIdCb;
        callbacks.update_key = ngtcp2_crypto_update_key_cb;
        callbacks.delete_crypto_aead_ctx = ngtcp2_crypto_delete_crypto_aead_ctx_cb;
        callbacks.delete_crypto_cipher_ctx = ngtcp2_crypto_delete_crypto_cipher_ctx_cb;
        callbacks.get_path_challenge_data = ngtcp2_crypto_get_path_challenge_data_cb;
        callbacks.version_negotiation = ngtcp2_crypto_version_negotiation_cb;
        // end required
        callbacks.handshake_completed = handshakeCompleteCb;
        callbacks.stream_close = streamCloseCb;
        callbacks.recv_stream_data = recvStreamDataCb;
        callbacks.acked_stream_data_offset = ackedStreamDataCb;
        callbacks.extend_max_local_streams_uni = extendMaxUniStreamCb;
        callbacks.extend_max_local_streams_bidi = extendMaxBidirStreamCb;
        
        
        if (ngtcp2_conn_client_new(&ngConn,
                                   &dcid,
                                   &scid,
                                   &path,
                                   NGTCP2_PROTO_VER_V1,
                                   &callbacks,
                                   &settings,
                                   &params,
                                   NULL,
                                   this) != 0)
            throw std::invalid_argument("Could not create ngtcp2 client");

        // Set up TLS/SSL layer
        sslCtx = SSL_CTX_new(TLS_client_method());
        if (!sslCtx)
            throw std::invalid_argument("Could create TLS layer");

        if (certChecker)
            // We're doing our own verification in handshake completion cb
            // (see streamingsocketmanagement notes as to why)
            SSL_CTX_set_verify(sslCtx, SSL_VERIFY_NONE, NULL);
        
        if (ngtcp2_crypto_quictls_configure_client_context(sslCtx) != 0)
            throw std::invalid_argument("ngtcp2 config of TLS layer failed");
        
        ssl = SSL_new(sslCtx);
        if (!ssl)
            throw std::invalid_argument("Could not create TLS session");

        if (cert) {        
            SSL_use_certificate(ssl, cert);
            SSL_use_PrivateKey(ssl, privKey);
        }
        SSL_set_app_data(ssl, &ngConnRef);
        SSL_set_connect_state(ssl);
        SSL_set_alpn_protos(ssl, alpn, (unsigned int)alpnLen);
        
        ngtcp2_conn_set_tls_native_handle(ngConn, ssl);

        txBufferSize = 2 * STREAM_FLOW_CTRL_BYTES;
        txBuffer = new uint8_t[txBufferSize];
        
        // Set a 30 second keepalive if idle timeout is longer than that
        // For shorter idle timeout, consistent traffic is expected and so
        // keep alive is extraneous
        if (idleTimeoutSec >= 30)
            ngtcp2_conn_set_keep_alive_timeout(ngConn, 30 * (ngtcp2_duration)1000000000);

    } catch (std::invalid_argument &ex) {
        InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR,
                "quic QuicConnection create as client failed (%s)",
                ex.what());
        destroyInternals();
        throw;
    }
    InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "quic %p QuicConnection create as client", this);
}

QuicConnection::~QuicConnection()
{
    InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "quic %p QuicConnection destroy", this);
    destroyInternals();
    for (std::deque<QuicTxBuf *>::iterator iter = txBufs.begin(); iter != txBufs.end(); ++iter)
        delete *iter;
}

void QuicConnection::destroyInternals()
{
    if (ngConn)
        ngtcp2_conn_del(ngConn);
    if (ssl)
        SSL_free(ssl);
    if (sslCtx)
        SSL_CTX_free(sslCtx);
    delete remoteClientAddr;
    if (txBuffer)
        delete[] txBuffer;
}

bool QuicConnection::writeStreams(UdpSocket *socket)
                COMMO_THROW(SocketException)
{
    if (state == Draining || state == Closing || state == Closed)
        // Can't send anything, but not a terminal state either
        return true;
    
    ngtcp2_tstamp now = gents();
    bool connOk = true;
    bool streamBlocked = false;
    const size_t maxBytesOut = ngtcp2_conn_get_send_quantum(ngConn);
    size_t bytesOut = 0;
    
    while (connOk && bytesOut < maxBytesOut) {
        // If we have input, try to refill transfer buffer
        while (!isTxSourceDone()) {
            size_t check = SIZE_MAX - txLen;
            size_t n = txBufferSize - (txLen - txAckCount);
            // prevent overflow of txLen
            if (n > check)
                n = check;
            if (!n)
                // no space
                break;

            size_t sOff = txLen % txBufferSize;
            size_t nToRead = txBufferSize - sOff;
            if (nToRead > n)
                nToRead = n;

            int errCode;
            if (txSourceFill(txBuffer + sOff, &nToRead, &errCode)) {
                txLen += nToRead;
                if (!nToRead)
                    // No more data right now, come back later
                    break;
            } else {
                InternalUtils::logprintf(logger,
                                         CommoLogger::LEVEL_DEBUG,
                                         "quic %p writevstream txSourceFill failed, causing close",
                                         this);
                ngtcp2_ccerr err;
                ngtcp2_ccerr_set_application_error(&err, errCode,
                                                   NULL, 0);
                connOk = enterClosing(socket, err);
                break;
            }
        }
        if (state == Closing)
            // Errored above
            break;
        
        // Send for a stream if we have a stream and there is data to send, 
        // otherwise use -1/no data to give stack opportunity to send
        // control messages
        int64_t sid;
        ngtcp2_vec datav[2];
        size_t nDatav;
        uint32_t flags;
        bool sendingZeroStream = false;
        if (streamOpen && !streamBlocked && txSent < txLen) {
            sid = streamId;
            size_t nTotal = txLen - txSent;
            size_t txValidOff = txSent % txBufferSize;
            size_t n = txBufferSize - txValidOff;
            if (n >= nTotal) {
                // it's all one contiguous buffer at end of our ring
                n = nTotal;
                nDatav = 1;
            } else {
                // split over two pieces at end and start
                nDatav = 2;
                datav[1].base = txBuffer;
                datav[1].len = nTotal - n;
            }
            datav[0].base = txBuffer + txValidOff;
            datav[0].len = n;
            flags = isTxSourceDone() ? NGTCP2_WRITE_STREAM_FLAG_FIN : 0;
        } else if (newBidirStream && streamOpen && !streamBlocked && !txLen) {
            // send 0 stream bytes to cause a 0 length stream frame
            // to generate and send to remote causing us to inform remote
            // that we've opened a bidir stream.  Only need to do this if
            // we have not sent them anything; a stream frame with data will
            // also inform remote that we've opened the stream.
            sid = streamId;
            datav[0].len = 0;
            datav[0].base = NULL;
            nDatav = 1;
            flags = 0;
            sendingZeroStream = true;
        } else {
            sid = -1;
            datav[0].len = 0;
            datav[0].base = NULL;
            nDatav = 0;
            flags = 0;
        }
        
        ngtcp2_path_storage pathStore;
        ngtcp2_path_storage_zero(&pathStore);
        uint8_t buf[NGTCP2_MAX_UDP_PAYLOAD_SIZE];
        ngtcp2_ssize nStreamBytes;
        ngtcp2_ssize wlen = ngtcp2_conn_writev_stream(ngConn, 
                                  &pathStore.path, NULL, buf,
                                  sizeof(buf), &nStreamBytes,
                                  flags, sid, datav, nDatav, now);
        if (wlen < 0) {
            if (wlen == NGTCP2_ERR_STREAM_DATA_BLOCKED) {
                // Can't write more stream data now;  come back later.
                streamBlocked = true;
                continue;
                
            } else {
                // Queue close pkt, go to closing state and bail out
                InternalUtils::logprintf(logger,
                                         CommoLogger::LEVEL_DEBUG,
                                         "quic %p writevstream %d ret %d causes close",
                                         this, (int)sid, (int)wlen);
                ngtcp2_ccerr err;
                ngapiErrToCCErr(&err, wlen);
                connOk = enterClosing(socket, err);
                break;
            }
        } else {
            if (nDatav && nStreamBytes > 0) {
                txSent += nStreamBytes;
            }
            if (wlen > 0) {
                if (sendingZeroStream && nStreamBytes == 0) {
                    // ^^ nStreamBytes checking for 0 is important as
                    // if output packet filled with connection handling overhead
                    // our 0-length stream data didn't actually fit in and we
                    // need to try again
                    newBidirStream = false;
                }

                // Queue tx data
                try {
                    NetAddress *dstAddr = NetAddress::create(
                            pathStore.path.remote.addr);
                    if (!sendPkt(socket, dstAddr, buf, wlen))
                        // Cannot send any more right now
                        // packet was saved to send later
                        break;
                    bytesOut += wlen;

                } catch (std::invalid_argument &) {
                    // can't send this since the address is invalid.
                    // go straight to destroying connection
                    InternalUtils::logprintf(logger,
                                             CommoLogger::LEVEL_DEBUG,
                                             "quic %p writevstream %d invalid address causes close",
                                             this, (int)sid);
                    connOk = false;
                }
            } else {
                // else nothing to send or congestion limited on connection. 
                break;
            }
        }

    }

    if (connOk && state != Closing)
        // Note: allowed to change state
        connOk = txFinishHandling(socket, txLen);
    
    if (connOk && state != Closing) {
        ngtcp2_conn_update_pkt_tx_time(ngConn, now);
        expTime = ngtcp2_conn_get_expiry(ngConn);
    }
    return connOk;
    
}

bool QuicConnection::handleExpiration(UdpSocket *socket)
                COMMO_THROW (SocketException)
{
    bool ret = false;
    switch (state) {
    case Draining:
    case Closing:
        // Our drain/close time has come, signal to be done
        ret = false;
        InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                "quic %p close/drain time arrived, terminating",
                this);
        break;
    default:
        {
            int rc = ngtcp2_conn_handle_expiry(ngConn, gents());
            switch (rc) {
            case 0:
                ret = transmitPkts(socket);
                break;
                
            case NGTCP2_ERR_IDLE_CLOSE:
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                        "quic %p handleExp returns IDLE_CLOSE", this);
            default:
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                        "quic %p handleExp returns error %d, terminating",
                        this, rc);
                // signal us to be destroyed
                ret = false;
                break;
            }
        }
        break;
    case Closed:
        // nothing to do here
        break;
    }

    return handleClosed(ret);
}

bool QuicConnection::transmitPkts(UdpSocket *socket)
                COMMO_THROW (SocketException)
{
    try {
        while (!txBufs.empty()) {
            QuicTxBuf *txb = txBufs.front();
            // this call is sole thrower...
            socket->sendto(txb->destAddr, txb->data, txb->len);
            // ... if it succeeded, we can pop the data
            txBufs.pop_front();
            delete txb;
        }
    } catch (SocketWouldBlockException &) {
        // Cannot send more now, don't generate more either
        // All is ok, just come back later when we can write/send again
        return true;
    }
    
    // We have sent all buffered data
    // See if quic stack has more for us
    return handleClosed(writeStreams(socket));
}

bool QuicConnection::processPkt(UdpSocket *socket,
                                ngtcp2_pkt_hd *hdr, 
                                const NetAddress &localAddr,
                                NetAddress *sourceAddr,
                                const uint8_t *data, size_t len)
                                                   COMMO_THROW(SocketException)
{
    if (!len || state == Draining) {
        delete sourceAddr;
        return true;
    } else if (state == Closing) {
        // new pkts when already in closing state -- ignore it and requeue
        // send of existing close pkt
        txBufs.push_back(QuicTxBuf::clonedCreate(sourceAddr, closePkt, closePktLen));
        InternalUtils::logprintf(logger,
                CommoLogger::LEVEL_DEBUG,
                "quic %p pkt received when already closing - dropping and resending close pkt",
                this);
        return true;
    } else if (state == Closed) {
        // just draining out tx packets; ignore incoming packets
        // we didn't do anything to change tx buffer so no need
        // to filter through handleClosed
        return true;
    }

    bool isInitial = hdr != NULL;

    ngtcp2_path path;
    memset(&path, 0, sizeof(ngtcp2_path));
    ngtcp2_addr_init(&path.local, localAddr.getSockAddr(), localAddr.getSockAddrLen());
    ngtcp2_addr_init(&path.remote, sourceAddr->getSockAddr(), sourceAddr->getSockAddrLen());
    

    int r = ngtcp2_conn_read_pkt(ngConn, &path, NULL, data, len, gents());
    delete sourceAddr;

    if (r != 0) {
        if (isInitial && r == NGTCP2_ERR_RETRY) {
            // send retry (only sensible on initial pkt)
            // For now, just bail/drop connection
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                                     "quic %p read_pkt indicates retry; unsupported, dropping connection",
                                     this);
            return handleClosed(false);
            
        } else if (r == NGTCP2_ERR_DRAINING) {
            // go to drain
            const ngtcp2_ccerr *err = ngtcp2_conn_get_ccerr(ngConn);
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                                     "quic %p read_pkt causes draining (type %d ec %d)",
                                     this, err->type, (int)err->error_code);
            state = Draining;
            expTime = gents() + ngtcp2_conn_get_pto(ngConn) * 3;
            
            return true;
        } else if (r == NGTCP2_ERR_DROP_CONN) {
            // Unrecoverable error; abandon connection
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                                     "quic %p read_pkt indicates drop conn",
                                     this);
            return handleClosed(false);
        } else {
            ngtcp2_ccerr err;
            ngapiErrToCCErr(&err, r);
        
            // send close pkt and enter closing state
            bool ret = enterClosing(socket, err);
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                                     "quic %p read_pkt other fatal error %d, dropping",
                                     this, r);
            return handleClosed(ret);
        }
    }
    
    if (isInitial) {
        size_t n = ngtcp2_conn_get_scid(ngConn, NULL);
        ngtcp2_cid *ids = new ngtcp2_cid[n];
        ngtcp2_conn_get_scid(ngConn, ids);
        for (size_t i = 0; i < n; i++)
            scids.insert(std::string((const char *)ids->data, ids->datalen));
        delete[] ids;
        scids.insert(std::string((const char *)hdr->dcid.data, hdr->dcid.datalen));
    }
    
    return postProcessPkt(socket);
}

void QuicConnection::ngapiErrToCCErr(ngtcp2_ccerr *ccerr,
                                     ngtcp2_ssize apiErr)
{
    if (apiErr == NGTCP2_ERR_CRYPTO)
        ngtcp2_ccerr_set_tls_alert(ccerr, 
            ngtcp2_conn_get_tls_alert(ngConn), NULL, 0);
    else
        ngtcp2_ccerr_set_liberr(ccerr, (int)apiErr, NULL, 0);
}

bool QuicConnection::hasSCID(const std::string &scid)
{
    return scids.find(scid) != scids.end();
}

bool QuicConnection::handleClosed(bool operationOk)
{
    if (!operationOk || state == Closed) {
        state = Closed;
        expTime = UINT64_MAX;
        if (hasTxData())
            // Still need to drain out tx
            return true;
        
        return false;
    }
    return true;
}

bool QuicConnection::enterClosing(UdpSocket *socket,
                                  const ngtcp2_ccerr &err)
                COMMO_THROW(SocketException)
{
    ngtcp2_path_storage cpath;
    ngtcp2_path_storage_zero(&cpath);
    ngtcp2_ssize n = ngtcp2_conn_write_connection_close(ngConn, 
                                                &cpath.path, NULL, closePkt,
                                                NGTCP2_MAX_UDP_PAYLOAD_SIZE,
                                                &err, gents());
    if (n < 0)
        return false;
    closePktLen = (size_t)n;
    
    NetAddress *remoteAddr;
    try {
        remoteAddr = NetAddress::create(cpath.path.remote.addr);
    } catch (std::invalid_argument &) {
        // can't send this since the address is invalid.  Bail
        return false;
    }

    // return doesn't matter; if socket is blocked, we'll just resend later 
    sendPkt(socket, remoteAddr, closePkt, closePktLen);

    state = Closing;
    expTime = gents() + ngtcp2_conn_get_pto(ngConn) * 3;
    
    return true;
}

bool QuicConnection::sendPkt(UdpSocket *socket,
                             NetAddress *addr,
                             uint8_t *pkt, size_t len)
                COMMO_THROW(SocketException)
{
    bool blocked = !txBufs.empty();
    if (!blocked) {
        // Cannot try to direct send if we already have buffered packets to send
        // from a prior 'would block'
        try {
            socket->sendto(addr, pkt, len);
        } catch (SocketWouldBlockException &) {
            blocked = true;
        } catch (SocketException &) {
            delete addr;
            throw;
        }
    }
    if (blocked)
        txBufs.push_back(QuicTxBuf::clonedCreate(addr, pkt, len));
    else
        delete addr;
    return !blocked;
}




int QuicConnection::getNewConnectionIdCb(ngtcp2_conn *conn,
                                         ngtcp2_cid *cid,
                                         uint8_t *token,
                                         size_t cidLen,
                                         void *userData)
{
    QuicConnection *cctx = (QuicConnection *)userData;
    if (RAND_bytes(cid->data, (int)cidLen) != 1)
        return NGTCP2_ERR_CALLBACK_FAILURE;
    
    cid->datalen = cidLen;
    
    if (RAND_bytes(token, NGTCP2_STATELESS_RESET_TOKENLEN) != 1)
        return NGTCP2_ERR_CALLBACK_FAILURE;

    if (cctx->isServer)
        cctx->scids.insert(std::string((const char *)cid->data, cid->datalen));

    return 0;
}

int QuicConnection::removeConnectionIdCb(ngtcp2_conn *conn,
                                         const ngtcp2_cid *cid,
                                         void *userData)
{
    QuicConnection *cctx = (QuicConnection *)userData;
    cctx->scids.erase(std::string((const char *)cid->data, cid->datalen));
    return 0;
}


int QuicConnection::handshakeCompleteCb(ngtcp2_conn *conn,
                                        void *userData)
{
    QuicConnection *cctx = (QuicConnection *)userData;
    
    // Check cert of server, if so requested.
    // See comments on streamingsocketmanagement regarding verification
    // as to why it must be done "manually"
    if (!cctx->isServer && cctx->certChecker) {
        X509 *cert = SSL_get_peer_certificate(cctx->ssl);
        if (!cert) {
            InternalUtils::logprintf(cctx->logger, CommoLogger::LEVEL_DEBUG,
                                     "quic %p handshake complete, but remote did not provide certificate",
                                     cctx);
            cctx->handshakeError(netinterfaceenums::ERR_CONN_SSL_NO_PEER_CERT,
                                    "remote peer did not provide a certificate");
            return NGTCP2_ERR_CALLBACK_FAILURE;
        }

        bool certOk = cctx->certChecker->checkCert(cert);
        X509_free(cert);

        if (!certOk) {
            int vResult = cctx->certChecker->getLastErrorCode();
            InternalUtils::logprintf(cctx->logger, CommoLogger::LEVEL_DEBUG,
                                     "quic %p handshake complete, but cert does not match truststore %d",
                                     cctx, vResult);
            cctx->handshakeError(netinterfaceenums::ERR_CONN_SSL_PEER_CERT_NOT_TRUSTED,
                                 "remote peer certificate failed verification - check truststore");
            return NGTCP2_ERR_CALLBACK_FAILURE;
        }
    }
    
    const unsigned char *alpn = nullptr;
    unsigned int alpnlen;
    
    SSL_get0_alpn_selected(cctx->ssl, &alpn, &alpnlen);
    bool alpnOk;
    if (alpn) {
        std::string alpnStr((const char *)alpn, alpnlen);
        InternalUtils::logprintf(cctx->logger,
                                 CommoLogger::LEVEL_DEBUG,
                                 "quic %p handshake complete, selected ALPN is {%s}",
                                 cctx,
                                 alpnStr.c_str());
        alpnOk = cctx->handshakeComplete(alpn, alpnlen);
    } else {
        InternalUtils::logprintf(cctx->logger, CommoLogger::LEVEL_DEBUG,
                                 "quic %p handshake complete, but no ALPN negotiated; assuming takcot",
                                 cctx);
        alpnOk = cctx->handshakeComplete(NULL, 0);
    }
    if (!alpnOk)
        return NGTCP2_ERR_CALLBACK_FAILURE;
    if (cctx->state == Prehandshake)
        cctx->state = Normal;
    return 0;
}


int QuicConnection::recvStreamDataCb(ngtcp2_conn *conn,
                                     uint32_t flags,
                                     int64_t streamId,
                                     uint64_t offset,
                                     const uint8_t *data,
                                     size_t dataLen,
                                     void *userData,
                                     void *streamUserData)
{
    QuicConnection *cctx = (QuicConnection *)userData;
    if ((flags & NGTCP2_STREAM_DATA_FLAG_0RTT) || cctx->state == Prehandshake)
        return 0;
    
    if (!cctx->streamOpen || streamId != cctx->streamId) {
        InternalUtils::logprintf(cctx->logger, CommoLogger::LEVEL_ERROR,
                                 "quic %p stream %d data received for unopened stream",
                                 cctx, (int)streamId);
        return NGTCP2_ERR_CALLBACK_FAILURE;
    }
    
    if (!cctx->receivedData(data, dataLen, flags & NGTCP2_STREAM_DATA_FLAG_FIN))
        return NGTCP2_ERR_CALLBACK_FAILURE;
    
    ngtcp2_conn_extend_max_stream_offset(cctx->ngConn, streamId, dataLen);
    ngtcp2_conn_extend_max_offset(cctx->ngConn, dataLen);

    return 0;
}


int QuicConnection::ackedStreamDataCb(ngtcp2_conn *conn,
                                      int64_t streamId, 
                                      uint64_t offset,
                                      uint64_t dataLen,
                                      void *userData,
                                      void *streamUserData)
{
    QuicConnection *cctx = (QuicConnection *)userData;

    if (!cctx->streamOpen || streamId != cctx->streamId)
        return NGTCP2_ERR_CALLBACK_FAILURE;

    // NOTE: tx data below offset is now ok to discard/dealloc
    cctx->txAckCount = offset + dataLen;
    return 0;    
}

int QuicConnection::extendMaxUniStreamCb(ngtcp2_conn *conn,
                                         uint64_t maxStreams,
                                         void *userData)
{
    QuicConnection *cctx = (QuicConnection *)userData;
    if (cctx->state == Normal && !cctx->streamOpen && cctx->wantsTxUni()) {
        int64_t sid;
        if (ngtcp2_conn_open_uni_stream(cctx->ngConn, &sid, NULL) != 0) {
            InternalUtils::logprintf(cctx->logger,
                                     CommoLogger::LEVEL_DEBUG,
                                     "quic %p failed to open new unidirectional stream",
                                     cctx);
            return NGTCP2_ERR_CALLBACK_FAILURE;
        }
        InternalUtils::logprintf(cctx->logger,
                                 CommoLogger::LEVEL_DEBUG,
                                 "quic %p opened new unidirectional stream %d",
                                 cctx,
                                 (int)sid);
        cctx->streamOpen = true;
        cctx->streamId = sid;
    } else {
        InternalUtils::logprintf(cctx->logger,
                                 CommoLogger::LEVEL_DEBUG,
                                 "quic %p max unidirectional streams extended, but not needed, ignoring",
                                 cctx);
    }
    return 0;
}

int QuicConnection::extendMaxBidirStreamCb(ngtcp2_conn *conn,
                                           uint64_t maxStreams,
                                           void *userData)
{
    QuicConnection *cctx = (QuicConnection *)userData;
    if (cctx->state == Normal && !cctx->streamOpen && cctx->wantsBiDir()) {
        int64_t sid;
        if (ngtcp2_conn_open_bidi_stream(cctx->ngConn, &sid, NULL) != 0) {
            InternalUtils::logprintf(cctx->logger,
                                     CommoLogger::LEVEL_DEBUG,
                                     "quic %p failed to open new bidirectional stream",
                                     cctx);
            return NGTCP2_ERR_CALLBACK_FAILURE;
        }
        InternalUtils::logprintf(cctx->logger,
                                 CommoLogger::LEVEL_DEBUG,
                                 "quic %p opened new bidirectional stream %d",
                                 cctx,
                                 (int)sid);
        cctx->streamOpen = true;
        cctx->streamId = sid;
        cctx->newBidirStream = true;
    } else {
        InternalUtils::logprintf(cctx->logger,
                                 CommoLogger::LEVEL_DEBUG,
                                 "quic %p max bidirectional streams extended, but not needed, ignoring",
                                 cctx);
    }
    return 0;
}

int QuicConnection::streamOpenCb(ngtcp2_conn *conn,
                                 int64_t streamId,
                                 void *userData)
{
    QuicConnection *cctx = (QuicConnection *)userData;
    if (cctx->streamOpen) {
        InternalUtils::logprintf(cctx->logger, CommoLogger::LEVEL_DEBUG,
            "quic %p new stream open %d, but stream already opened!",
            cctx, (int)streamId);
        return NGTCP2_ERR_CALLBACK_FAILURE;
    }
    InternalUtils::logprintf(cctx->logger, CommoLogger::LEVEL_DEBUG,
        "quic %p new stream opened %d", cctx, (int)streamId);
    
    if (ngtcp2_is_bidi_stream(streamId) && !cctx->isRemoteBiDirStreamAllowed()) {
        InternalUtils::logprintf(cctx->logger, CommoLogger::LEVEL_DEBUG,
            "quic %p new stream opened remotely %d; expecting unidirectional stream, but new stream is bidirectional; closing write side",
            cctx,
            (int)streamId);
        ngtcp2_conn_shutdown_stream_write(cctx->ngConn, 0, streamId, 0);
    }
    
    cctx->streamOpen = true;
    cctx->streamId = streamId;
    return 0;
}

int QuicConnection::streamCloseCb(ngtcp2_conn *conn,
                                  uint32_t flags, 
                                  int64_t streamId,
                                  uint64_t appErrCode,
                                  void *userData,
                                  void *streamUserData)
{
    QuicConnection *cctx = (QuicConnection *)userData;
    InternalUtils::logprintf(cctx->logger, CommoLogger::LEVEL_DEBUG,
        "quic %p stream close initiated %d",
        cctx,
        (int)streamId);
    if (!cctx->streamOpen || streamId != cctx->streamId) {
        InternalUtils::logprintf(cctx->logger, CommoLogger::LEVEL_DEBUG,
            "quic %p stream close for %d, but doesn't match our open stream id",
            cctx,
            (int)streamId);
        return NGTCP2_ERR_CALLBACK_FAILURE;
    }
    cctx->streamOpen = false;
    // NOTE: tx data with outstanding acks can be dropped at this point!
    return 0;
}

int QuicConnection::selectAlpnSSLCb(SSL *ssl,
                                    const unsigned char **out,
                                    unsigned char *outLen,
                                    const unsigned char *in,
                                    unsigned int inLen,
                                    void *arg)
{
    QuicConnection *cctx = (QuicConnection *)arg;
    // games because the api decl for SSL_select_next_proto misuses const
    unsigned char *tmp;
    int rc = SSL_select_next_proto(&tmp, outLen, cctx->allowedAlpns,
                                   (unsigned int)cctx->allowedAlpnsLen, in, inLen);
    *out = tmp;
    if (rc == OPENSSL_NPN_NEGOTIATED)
        return SSL_TLSEXT_ERR_OK;
    else
        return SSL_TLSEXT_ERR_ALERT_FATAL;
}

void QuicConnection::quicStackLogCb(void *userData,
                                    const char *format, ...)
{
    QuicConnection *cctx = (QuicConnection *)userData;
    va_list args;
    va_start(args, format);

    InternalUtils::logvprintf(cctx->logger, CommoLogger::LEVEL_DEBUG, CommoLogger::Type::TYPE_GENERAL, NULL, format, args);

    va_end(args);
}

ngtcp2_conn *QuicConnection::getConnFromRefCb(ngtcp2_crypto_conn_ref *connRef)
{
    return ((QuicConnection *)(connRef->user_data))->ngConn;
}

void QuicConnection::commonParamInit(ngtcp2_settings *settings,
                                     ngtcp2_transport_params *params,
                                     float connTimeoutSec,
                                     float idleTimeoutSec)
{
        ngtcp2_settings_default(settings);
        //settings->log_printf = quicStackLogCb;
        settings->initial_ts = gents();
        settings->handshake_timeout = (ngtcp2_duration)connTimeoutSec * 1000000000;

        ngtcp2_transport_params_default(params);
        params->initial_max_stream_data_bidi_local = STREAM_FLOW_CTRL_BYTES;
        params->initial_max_stream_data_bidi_remote = STREAM_FLOW_CTRL_BYTES;
        params->initial_max_stream_data_uni = 128 * 1024;
        params->initial_max_data = 1024 * 1024;
        params->max_idle_timeout = (ngtcp2_duration)idleTimeoutSec * 1000000000;
}



/***********************************************************************/
// QuicTxBuf


QuicTxBuf::QuicTxBuf(NetAddress *destAddr, 
                     uint8_t *data, size_t len) :
        destAddr(destAddr),
        data(data),
        len(len)
{
}

QuicTxBuf::~QuicTxBuf()
{
    delete destAddr;
    delete[] data;
}

QuicTxBuf *QuicTxBuf::clonedCreate(
        NetAddress *destAddr, uint8_t *data, size_t len)
{
    uint8_t *newData = new uint8_t[len];
    memcpy(newData, data, len);
    return new QuicTxBuf(destAddr,
                         newData, len);
}






