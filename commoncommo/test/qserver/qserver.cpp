#include "qserver.h"
#include "ngtcp2/ngtcp2_crypto_quictls.h"

#include "openssl/rand.h"

#include <string.h>
#include <chrono>
#include <stdexcept>
#include <fcntl.h>
#include <errno.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <netinet/in.h>
#include <net/if.h>
#include <net/if_arp.h>
#include <stdio.h>
#include <linux/if_packet.h>

namespace {
    void rand_cb_impl(uint8_t *dest, size_t destlen, const ngtcp2_rand_ctx *randctx) {
        RAND_bytes(dest, (int)destlen);
    }

    ngtcp2_tstamp gents() {
        return std::chrono::duration_cast<std::chrono::nanoseconds>(
                 std::chrono::steady_clock::now().time_since_epoch())
          .count();
    }

    void logvprintf(const char* format, va_list vargs)
    {
        vprintf(format, vargs);
        printf("\n");
    }

    void logprintf(const char* format, ...)
    {
        va_list args;
        va_start(args, format);

        logvprintf(format, args);

        va_end(args);
    }

    const unsigned char ALPNS[] = {
        0x09, 't','a','k','s','t','r','e','a','m',
        0x08, 'h','t','t','p','/','0','.','9'
    };
    
    int selectAlpnSSLCb(SSL *ssl,
                        const unsigned char **out,
                        unsigned char *outLen,
                        const unsigned char *in,
                        unsigned int inLen,
                        void *arg)
    {
        // games because the api decl for SSL_select_next_proto misuses const
        unsigned char *tmp;
        int rc = SSL_select_next_proto(&tmp, outLen, ALPNS,
                                       sizeof(ALPNS), in, inLen);
        *out = tmp;
        if (rc == OPENSSL_NPN_NEGOTIATED)
            return SSL_TLSEXT_ERR_OK;
        else
            return SSL_TLSEXT_ERR_ALERT_FATAL;
    }



    std::string genSA()
    {
        const char *myUID = "qserv-test-uid";
        const char *myCallsign = "qserv-test";
        time_t now = time(NULL);
        struct tm t;
        gmtime_r(&now, &t);
        const size_t timebufsize = 256;
        char timebuf[timebufsize];
        char timebuf2[timebufsize];
        strftime(timebuf, timebufsize, "%Y-%m-%dT%H:%M:%S.000Z", &t);
        now += 120;
        gmtime_r(&now, &t);
        strftime(timebuf2, timebufsize, "%Y-%m-%dT%H:%M:%S.000Z", &t);

        std::string saString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><event version=\"2.0\" uid=\"";
        saString += "2084306f-8896-42a6-80a5-7c1706f93620";
        saString += "\" type=\"b-m-p-c\" how=\"h-g-i-g-o\" time=\"";
        saString += timebuf;
        saString += "\" start=\"";
        saString += timebuf;
        saString += "\" stale=\"";
        saString += timebuf2;
        saString += "\">";

        saString += "<point lat=\"46.5261810013514\" lon=\"-87.3862509255614\" hae=\"9999999.0\" "
               "ce=\"9999999\" le=\"9999999\"/>"
               "<detail>        <contact callsign=\"";
        saString += myCallsign;
        saString += ".22.161714\"/>";
        saString += "<link type=\"b-m-p-c\" uid=\"";
        saString += myUID;
        saString += "\" relation=\"p-p\" production_time=\"";
        saString += timebuf;
        saString += "\" />"
               "<archive /><request notify=\"192.168.1.2:4242:udp\"/>";
        saString += 
               "</detail>";
         saString += "<handled />";
        saString += "</event>";
        return saString;
    }
}


    
ServerConn::ServerConn(const sockaddr *localAddr, socklen_t localAddrLen,
                       sockaddr *clientAddr, socklen_t clientAddrLen,
                       uint32_t version,
                       const ngtcp2_cid &scidFromClient,
                       const ngtcp2_cid &dcidFromClient,
                       const char *certFile,
                       const char *keyFile,
                       const char *caFile,
                       const char *xmlLogPath) :
        curSAMsg(),
        curSAOffset(0),
        nextSATime(0),
        xmlLogFile(NULL),
        sslCtx(NULL),
        ssl(NULL),
        ngConnRef{getConnFromRefCb, this},
        ngConn(NULL),
        state(Prehandshake),
        streamOpen(false),
        streamId(0),
        scids(),
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
    ngtcp2_settings settings;
    ngtcp2_transport_params params;

    ngtcp2_settings_default(&settings);
    settings.log_printf = quicStackLogCb;
    settings.initial_ts = gents();
    settings.handshake_timeout = (ngtcp2_duration)30 * 1000000000;

    ngtcp2_transport_params_default(&params);
    params.initial_max_stream_data_bidi_local = STREAM_FLOW_CTRL_BYTES;
    params.initial_max_stream_data_bidi_remote = STREAM_FLOW_CTRL_BYTES;
    params.initial_max_data = 1024 * 1024;
    params.max_idle_timeout = (ngtcp2_duration)30 * 1000000000;
    params.original_dcid = dcidFromClient;
    params.original_dcid_present = 1;
    params.initial_max_streams_uni = 0;
    params.initial_max_streams_bidi = 1;

        
    ngtcp2_cid scid;
    scid.datalen = 16;
    if (RAND_bytes(scid.data, (int)scid.datalen) != 1)
        throw std::invalid_argument("RNG failure");

    ngtcp2_path path;
    memset(&path, 0, sizeof(ngtcp2_path));
    ngtcp2_addr_init(&path.local, localAddr, localAddrLen);
    ngtcp2_addr_init(&path.remote, clientAddr, clientAddrLen);
        
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
    
    if (SSL_CTX_use_certificate_file(sslCtx, certFile, SSL_FILETYPE_PEM) != 1)
        throw std::invalid_argument("Could not configure TLS layer with provided cert");
    
    if (SSL_CTX_use_PrivateKey_file(sslCtx, keyFile, SSL_FILETYPE_PEM) != 1)
        throw std::invalid_argument("Could not configure TLS layer with provided key");
    
    if (SSL_CTX_load_verify_locations(sslCtx, caFile, NULL) != 1)
        throw std::invalid_argument("Could not configure TLS layer with provided CAs");
    
    if (SSL_CTX_check_private_key(sslCtx) != 1)
        throw std::invalid_argument("Priv key not valid for cert");
    
    SSL_CTX_set_verify(sslCtx, SSL_VERIFY_PEER, NULL);
    
    ssl = SSL_new(sslCtx);
    if (!ssl)
        throw std::invalid_argument("Could not create TLS session");
    
    SSL_set_app_data(ssl, &ngConnRef);
    SSL_set_accept_state(ssl);
    ngtcp2_conn_set_tls_native_handle(ngConn, ssl);

    txBufferSize = 2 * STREAM_FLOW_CTRL_BYTES;
    txBuffer = new uint8_t[txBufferSize];
    
    xmlLogFile = fopen(xmlLogPath, "wb");
}

ServerConn::~ServerConn()
{
    logprintf("quic %p ServerConn destroy", this);
    destroyInternals();
    for (std::deque<QuicTxBuf *>::iterator iter = txBufs.begin(); iter != txBufs.end(); ++iter)
        delete *iter;
}

void ServerConn::logData(const uint8_t *data, size_t len)
{
    fwrite(data, 1, len, xmlLogFile);
    fflush(xmlLogFile);
}

void ServerConn::destroyInternals()
{
    if (ngConn)
        ngtcp2_conn_del(ngConn);
    if (ssl)
        SSL_free(ssl);
    if (sslCtx)
        SSL_CTX_free(sslCtx);
    if (txBuffer)
        delete[] txBuffer;
    if (xmlLogFile) {
        fclose(xmlLogFile);
        xmlLogFile = NULL;
    }
}

bool ServerConn::writeStreams(int fd)
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
        while (true) {
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
                ngtcp2_ccerr err;
                ngtcp2_ccerr_set_application_error(&err, errCode,
                                                   NULL, 0);
                connOk = enterClosing(fd, err);
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
            flags = 0;
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
                logprintf("quic %p writevstream %d ret %d causes close",
                                         this, (int)sid, (int)wlen);
                ngtcp2_ccerr err;
                ngapiErrToCCErr(&err, wlen);
                connOk = enterClosing(fd, err);
                break;
            }
        } else {
            if (nDatav && nStreamBytes > 0) {
                txSent += nStreamBytes;
            }

            if (wlen > 0) {
                // Queue tx data
                try {
                    sockaddr *dstAddr = pathStore.path.remote.addr;
                    socklen_t dstAddrLen = pathStore.path.remote.addrlen;
                    if (!sendPkt(fd, dstAddr, dstAddrLen, buf, wlen))
                        // Cannot send any more right now
                        // packet was saved to send later
                        break;
                    bytesOut += wlen;

                } catch (std::invalid_argument &) {
                    // can't send this since the address is invalid.
                    // go straight to destroying connection
                    connOk = false;
                }
            } else {
                // else nothing to send or congestion limited on connection. 
                break;
            }
        }

    }

    if (connOk && state != Closing) {
        ngtcp2_conn_update_pkt_tx_time(ngConn, now);
        expTime = ngtcp2_conn_get_expiry(ngConn);
    }
    return connOk;
    
}

bool ServerConn::handleExpiration(int fd)
{
    bool ret = false;
    switch (state) {
    case Draining:
    case Closing:
        // Our drain/close time has come, signal to be done
        ret = false;
        logprintf("quic %p close/drain time arrived, terminating",
                this);
        break;
    default:
        {
            int rc = ngtcp2_conn_handle_expiry(ngConn, gents());
            switch (rc) {
            case 0:
                ret = transmitPkts(fd);
                break;
                
            case NGTCP2_ERR_IDLE_CLOSE:
                logprintf("quic %p handleExp returns IDLE_CLOSE", this);
            default:
                logprintf("quic %p handleExp returns error %d, terminating",
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

bool ServerConn::transmitPkts(int fd)
{
    while (!txBufs.empty()) {
        QuicTxBuf *txb = txBufs.front();
        // this call is sole thrower...
        
        int n = ::sendto(fd, txb->data, txb->len, 0, txb->destAddr, txb->destAddrLen);
        if (n < 0) {
            if (errno == EAGAIN || errno == EWOULDBLOCK)
                // Cannot send more now, don't generate more either
                // All is ok, just come back later when we can write/send again
                return true;
            else
                return false;
        }
        // ... if it succeeded, we can pop the data
        txBufs.pop_front();
        delete txb;
    }
    
    // We have sent all buffered data
    // See if quic stack has more for us
    return handleClosed(writeStreams(fd));
}

bool ServerConn::processPkt(int fd,
                            ngtcp2_pkt_hd *hdr, 
                            const sockaddr *localAddr,
                            socklen_t localAddrLen,
                            const sockaddr *sourceAddr,
                            socklen_t sourceAddrLen,
                            const uint8_t *data, size_t len)
{
    if (!len || state == Draining) {
        return true;
    } else if (state == Closing) {
        // new pkts when already in closing state -- ignore it and requeue
        // send of existing close pkt
        txBufs.push_back(new QuicTxBuf(sourceAddr, sourceAddrLen, closePkt, closePktLen));
        logprintf("quic %p pkt received when already closing - dropping and resending close pkt",
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
    ngtcp2_addr_init(&path.local, localAddr, localAddrLen);
    ngtcp2_addr_init(&path.remote, sourceAddr, sourceAddrLen);
    

    int r = ngtcp2_conn_read_pkt(ngConn, &path, NULL, data, len, gents());

    if (r != 0) {
        if (isInitial && r == NGTCP2_ERR_RETRY) {
            // send retry (only sensible on initial pkt)
            // For now, just bail/drop connection
            logprintf("quic %p read_pkt indicates retry; unsupported, dropping connection",
                                     this);
            return handleClosed(false);
            
        } else if (r == NGTCP2_ERR_DRAINING) {
            // go to drain
            const ngtcp2_ccerr *err = ngtcp2_conn_get_ccerr(ngConn);
            logprintf("quic %p read_pkt causes draining (type %d ec %d)",
                                     this, err->type, (int)err->error_code);
            state = Draining;
            expTime = gents() + ngtcp2_conn_get_pto(ngConn) * 3;
            
            return true;
        } else if (r == NGTCP2_ERR_DROP_CONN) {
            // Unrecoverable error; abandon connection
            logprintf("quic %p read_pkt indicates drop conn",
                                     this);
            return handleClosed(false);
        } else {
            ngtcp2_ccerr err;
            ngapiErrToCCErr(&err, r);
        
            // send close pkt and enter closing state
            bool ret = enterClosing(fd, err);
            logprintf("quic %p read_pkt other fatal error %d, dropping",
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
    
    return true;
}

void ServerConn::ngapiErrToCCErr(ngtcp2_ccerr *ccerr,
                                     ngtcp2_ssize apiErr)
{
    if (apiErr == NGTCP2_ERR_CRYPTO)
        ngtcp2_ccerr_set_tls_alert(ccerr, 
            ngtcp2_conn_get_tls_alert(ngConn), NULL, 0);
    else
        ngtcp2_ccerr_set_liberr(ccerr, (int)apiErr, NULL, 0);
}

bool ServerConn::hasSCID(const std::string &scid)
{
    return scids.find(scid) != scids.end();
}

bool ServerConn::handleClosed(bool operationOk)
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

bool ServerConn::txSourceFill(uint8_t *buf, size_t *len, int *errCode)
{
    size_t used = 0;
    
    while (used < *len) {
        size_t n = curSAMsg.length();
        if (curSAOffset >= n) {
            // cur message was sent - see if time for new one
            if (gents() > nextSATime) {
                curSAMsg = genSA();
                curSAOffset = 0;
                nextSATime = gents() + ((ngtcp2_tstamp)2 * 1000000000);
                continue;
            } else {
                break;
            }
        }

        n -= curSAOffset;
        if (n > (*len - used))
            n = *len - used;
        
        memcpy(buf, curSAMsg.data() + curSAOffset, n);
        curSAOffset += n;
        used += n;
    }
    *len = used;
    return true;
}


bool ServerConn::enterClosing(int fd,
                                  const ngtcp2_ccerr &err)
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
    
    // return doesn't matter; if socket is blocked, we'll just resend later 
    sendPkt(fd, cpath.path.remote.addr, cpath.path.remote.addrlen, closePkt, closePktLen);

    state = Closing;
    expTime = gents() + ngtcp2_conn_get_pto(ngConn) * 3;
    
    return true;
}

bool ServerConn::sendPkt(int fd,
                             const sockaddr *addr,
                             socklen_t addrLen,
                             uint8_t *pkt, size_t len)
{
    bool blocked = !txBufs.empty();
    if (!blocked) {
        // Cannot try to direct send if we already have buffered packets to send
        // from a prior 'would block'
        int n = ::sendto(fd, pkt, len, 0, addr, addrLen);
        if (n < 0) {
            if (errno == EAGAIN || errno == EWOULDBLOCK)
                blocked = true;
            else
                return false;
        }
    }
    if (blocked)
        txBufs.push_back(new QuicTxBuf(addr, addrLen, pkt, len));
    return !blocked;
}




int ServerConn::getNewConnectionIdCb(ngtcp2_conn *conn,
                                         ngtcp2_cid *cid,
                                         uint8_t *token,
                                         size_t cidLen,
                                         void *userData)
{
    ServerConn *cctx = (ServerConn *)userData;
    if (RAND_bytes(cid->data, (int)cidLen) != 1)
        return NGTCP2_ERR_CALLBACK_FAILURE;
    
    cid->datalen = cidLen;
    
    if (RAND_bytes(token, NGTCP2_STATELESS_RESET_TOKENLEN) != 1)
        return NGTCP2_ERR_CALLBACK_FAILURE;

    cctx->scids.insert(std::string((const char *)cid->data, cid->datalen));

    return 0;
}

int ServerConn::removeConnectionIdCb(ngtcp2_conn *conn,
                                         const ngtcp2_cid *cid,
                                         void *userData)
{
    ServerConn *cctx = (ServerConn *)userData;
    cctx->scids.erase(std::string((const char *)cid->data, cid->datalen));
    return 0;
}


int ServerConn::handshakeCompleteCb(ngtcp2_conn *conn,
                                        void *userData)
{
    ServerConn *cctx = (ServerConn *)userData;
    
    if (cctx->state == Prehandshake)
        cctx->state = Normal;
    logprintf("*** Handshake complete!");
    return 0;
}


int ServerConn::recvStreamDataCb(ngtcp2_conn *conn,
                                     uint32_t flags,
                                     int64_t streamId,
                                     uint64_t offset,
                                     const uint8_t *data,
                                     size_t dataLen,
                                     void *userData,
                                     void *streamUserData)
{
    ServerConn *cctx = (ServerConn *)userData;
    if ((flags & NGTCP2_STREAM_DATA_FLAG_0RTT) || cctx->state == Prehandshake)
        return 0;
    
    if (!cctx->streamOpen || streamId != cctx->streamId) {
        logprintf("quic %p stream %d data received for unopened stream",
                                 cctx, (int)streamId);
        return NGTCP2_ERR_CALLBACK_FAILURE;
    }

    cctx->logData(data, dataLen);
    
    ngtcp2_conn_extend_max_stream_offset(cctx->ngConn, streamId, dataLen);
    ngtcp2_conn_extend_max_offset(cctx->ngConn, dataLen);

    return 0;
}


int ServerConn::ackedStreamDataCb(ngtcp2_conn *conn,
                                      int64_t streamId, 
                                      uint64_t offset,
                                      uint64_t dataLen,
                                      void *userData,
                                      void *streamUserData)
{
    ServerConn *cctx = (ServerConn *)userData;

    if (!cctx->streamOpen || streamId != cctx->streamId)
        return NGTCP2_ERR_CALLBACK_FAILURE;

    // NOTE: tx data below offset is now ok to discard/dealloc
    cctx->txAckCount = offset + dataLen;
    return 0;    
}

int ServerConn::streamOpenCb(ngtcp2_conn *conn,
                                 int64_t streamId,
                                 void *userData)
{
    ServerConn *cctx = (ServerConn *)userData;
    if (cctx->streamOpen) {
        logprintf("quic %p new stream open %d, but stream already opened!",
            cctx, (int)streamId);
        return NGTCP2_ERR_CALLBACK_FAILURE;
    }
    logprintf("quic %p new stream opened %d", cctx, (int)streamId);
    
    if (!ngtcp2_is_bidi_stream(streamId)) {
        logprintf("quic %p new stream opened remotely %d; expecting bidirectional stream, but new stream is unidirectional",
            cctx,
            (int)streamId);
        return NGTCP2_ERR_CALLBACK_FAILURE;
    }
    
    cctx->streamOpen = true;
    cctx->streamId = streamId;
    return 0;
}

int ServerConn::streamCloseCb(ngtcp2_conn *conn,
                                  uint32_t flags, 
                                  int64_t streamId,
                                  uint64_t appErrCode,
                                  void *userData,
                                  void *streamUserData)
{
    ServerConn *cctx = (ServerConn *)userData;
    logprintf("quic %p stream close initiated %d",
        cctx,
        (int)streamId);
    if (!cctx->streamOpen || streamId != cctx->streamId) {
        logprintf("quic %p stream close for %d, but doesn't match our open stream id",
            cctx,
            (int)streamId);
        return NGTCP2_ERR_CALLBACK_FAILURE;
    }
    cctx->streamOpen = false;
    // NOTE: tx data with outstanding acks can be dropped at this point!
    return 0;
}

void ServerConn::quicStackLogCb(void *userData,
                                    const char *format, ...)
{
    va_list args;
    va_start(args, format);

    logvprintf(format, args);

    va_end(args);
}

ngtcp2_conn *ServerConn::getConnFromRefCb(ngtcp2_crypto_conn_ref *connRef)
{
    return ((ServerConn *)(connRef->user_data))->ngConn;
}



/***********************************************************************/
// QuicTxBuf


QuicTxBuf::QuicTxBuf(const sockaddr *destAddr, 
                     socklen_t destAddrLen,
                     uint8_t *data, size_t len) :
        destAddrStorage(),
        destAddr((sockaddr *)&destAddrStorage),
        destAddrLen(destAddrLen),
        data(new uint8_t[len]),
        len(len)
{
    memcpy(this->destAddr, destAddr, destAddrLen);
    memcpy(this->data, data, len);
}

QuicTxBuf::~QuicTxBuf()
{
    delete[] data;
}



int main(int argc, const char *argv[])
{
    if (argc < 5) {
        printf("ERROR: %s <cert> <key> <ca> <xmlLog>\n", argv[0]);
        return 10;
    }
    const char *certFile = argv[1];
    const char *keyFile = argv[2];
    const char *caFile = argv[3];
    const char *xmlLogPath = argv[4];

    int fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if (fd == -1) {
        printf("Failed to make socket\n");
        return 1;
    }
    struct sockaddr_in bound4;
    struct sockaddr *bound = (struct sockaddr *)&bound4;
    memset(&bound4, 0, sizeof(sockaddr_in));
    bound4.sin_family = AF_INET;
    bound4.sin_addr.s_addr = INADDR_ANY;
    bound4.sin_port = htons(9089);
    ::bind(fd, bound, sizeof(sockaddr_in));
    
    int flags = fcntl(fd, F_GETFL, 0);
    fcntl(fd, F_SETFL, flags | O_NONBLOCK);
    
    struct sockaddr_storage actualAddr;
    socklen_t actualAddrLen = sizeof(actualAddr);
    getsockname(fd, (struct sockaddr *)&actualAddr, &actualAddrLen);
    
    flags = 1;
    setsockopt(fd, IPPROTO_IP, IP_PKTINFO, &flags, sizeof(flags));
    
    bool wantsWrite = false;
    fd_set fdRead0, fdWrite0;
    FD_ZERO(&fdRead0);
    FD_SET(fd, &fdRead0);
    FD_ZERO(&fdWrite0);
    
    ServerConn *serverConn = NULL;

    while (true) {
        struct timeval tv;
        struct timeval *timeout = NULL;
    
        if (serverConn) {
            ngtcp2_tstamp now = gents();
            
            ngtcp2_tstamp exp = serverConn->getExpTime();
            if (now > exp) {
                logprintf("Calling handleexp");
                if (!serverConn->handleExpiration(fd)) {
                    logprintf("connection fatal error - expiration!");
                    delete serverConn;
                    serverConn = NULL;
                }
                continue;
            }
            memset(&tv, 0, sizeof(tv));
            tv.tv_sec =  (exp - now) / 1000000000;
            tv.tv_usec = ((exp - now) % 1000000000) / 1000;
            timeout = &tv;
        }
    
        fd_set fdRead, fdWrite;
        fdRead = fdRead0;
        fdWrite = fdWrite0;
        int sRet = select(fd + 1, &fdRead, &fdWrite, NULL, timeout);
        
        if (sRet == -1) {
            return 2;
        } else if (!sRet) {
            printf("Select timeout?\n");
            continue;
        }
        
        if (FD_ISSET(fd, &fdRead)) {
            ngtcp2_pkt_hd header;
            ngtcp2_pkt_hd *hdrIfNew = NULL;
            bool tryWrite = false;
            for (int pktCount = 0; pktCount < 10; pktCount++) {
                const size_t buflen = 65536;
                uint8_t buf[buflen];
                ngtcp2_version_cid qversion;

                struct sockaddr_storage localAddr;
                size_t localAddrLen = 0;
                struct sockaddr_storage remoteAddr;
                struct msghdr msg;
                iovec iov;
                iov.iov_base = buf;
                iov.iov_len = buflen;
                uint8_t pktinfmsgbuf[CMSG_SPACE(sizeof(struct in_pktinfo))];
                msg.msg_name = &remoteAddr;
                msg.msg_namelen = sizeof(remoteAddr);
                msg.msg_iov = &iov;
                msg.msg_iovlen = 1;
                msg.msg_control = pktinfmsgbuf;
                msg.msg_controllen = sizeof(pktinfmsgbuf);
                ssize_t n = ::recvmsg(fd, &msg, 0);
                if (n < 0 && (errno == EAGAIN || errno == EWOULDBLOCK))
                    break;
                else if (n < 0) {
                    logprintf("Fatal error reading socket");
                    return 3;
                }

                size_t remoteAddrLen = msg.msg_namelen;
                for (struct cmsghdr *cmhdr = CMSG_FIRSTHDR(&msg); cmhdr; cmhdr = CMSG_NXTHDR(&msg, cmhdr)) {
                    if (cmhdr->cmsg_level == IPPROTO_IP && cmhdr->cmsg_type == IP_PKTINFO) {
                        struct in_pktinfo pktinf;
                        memcpy(&pktinf, CMSG_DATA(cmhdr), sizeof(pktinf));
                        struct sockaddr_in *localAddr4 = (struct sockaddr_in *)&localAddr;
                        localAddr4->sin_family = AF_INET;
                        localAddr4->sin_addr = pktinf.ipi_addr;
                        localAddr4->sin_port = bound4.sin_port;
                        localAddrLen = sizeof(struct sockaddr_in);
                    }
                }
                if (!localAddrLen) {
                    logprintf("Unable to read local address from incoming pkt - skipping");
                    continue;
                }
                size_t len = (size_t)n;
                

                int r = ngtcp2_pkt_decode_version_cid(&qversion, buf, len, 16);
                if (r != 0) {
                    logprintf("Version negotiation not supported!");
                    continue;
                }
                std::string dcid((const char *)qversion.dcid, qversion.dcidlen);
                if (!serverConn) {
                    if (ngtcp2_accept(&header, buf, len) != 0) {
                        logprintf("pkt not accepted!");
                        continue;
                    }
                    
                    serverConn = new ServerConn((sockaddr *)&localAddr, localAddrLen,
                                                (sockaddr *)&remoteAddr, remoteAddrLen,
                                                header.version,
                                                header.scid,
                                                header.dcid,
                                                certFile,
                                                keyFile,
                                                caFile,
                                                xmlLogPath);
                    hdrIfNew = &header;
                } else if (!serverConn->hasSCID(dcid)) {
                    logprintf("pkt does not match existing conn!");
                    continue;
                }
                
                if (!serverConn->processPkt(fd, hdrIfNew, (sockaddr *)&localAddr, localAddrLen,
                                       (sockaddr *)&remoteAddr, remoteAddrLen, buf, len)) {
                    logprintf("connection fatal error - reading!");
                    tryWrite = false;
                    delete serverConn;
                    serverConn = NULL;
                    break;
                } else
                    tryWrite = true;
            }
            if (tryWrite) {
                if (!serverConn->transmitPkts(fd)) {
                    logprintf("connection fatal error - write after read!");
                    tryWrite = false;
                    delete serverConn;
                    serverConn = NULL;
                }
            }
        }
        if (serverConn && wantsWrite && FD_ISSET(fd, &fdWrite)) {
            if (!serverConn->transmitPkts(fd)) {
                logprintf("connection fatal error - write!");
                delete serverConn;
                serverConn = NULL;
            }
        }
        
        if (serverConn && serverConn->hasTxData()) {
            FD_SET(fd, &fdWrite0);
            wantsWrite = true;
        } else {
            FD_CLR(fd, &fdWrite0);
            wantsWrite = false;
        }
        
    }

    return 0;
}


