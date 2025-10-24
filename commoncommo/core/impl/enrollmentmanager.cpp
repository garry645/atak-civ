#include "enrollmentmanager.h"
#include <string.h>
#include <sstream>

using namespace atakmap::commoncommo;
using namespace atakmap::commoncommo::impl;
using namespace atakmap::commoncommo::impl::thread;




/****************************************************************/
// Internal class/var decls

namespace {
    const char *THREAD_NAMES[] = {
        "cmoenroll"
    };

    std::string PEM_CERT_HEADER("-----BEGIN CERTIFICATE-----\n");
    std::string PEM_CERT_FOOTER("-----END CERTIFICATE-----\n");
    
    xmlNode *getFirstChildElementByName(xmlNode *node, const xmlChar *name)
    {
        xmlNode *ret = NULL;
        for (xmlNode *child = node->children; child; child = child->next) {
            if (xmlStrEqual(child->name, name)) {
                ret = child;
                break;
            }
        }
        return ret;
    }


    struct BufferedURLIOUpdate : public URLIOUpdate, public SimpleFileIOUpdate
    {
        BufferedURLIOUpdate(const int xferid,
                const SimpleFileIOStatus status,
                const char *additionalInfo,
                uint64_t bytesTransferred,
                uint64_t totalBytesToTransfer,
                EnrollmentRequest *masterRequest,
                uint8_t *resultDocument,
                size_t resultDocumentLen);
        virtual ~BufferedURLIOUpdate();
        
        virtual SimpleFileIOUpdate *getBaseUpdate();
        
        EnrollmentRequest *masterRequest;
        uint8_t *resultDocument;
        size_t resultDocumentLen;
        
    private:
        COMMO_DISALLOW_COPY(BufferedURLIOUpdate);
    };


    struct InternalEnrollmentUpdate : public EnrollmentIOUpdate
    {
        InternalEnrollmentUpdate(EnrollmentStep step,
                const int xferid,
                const SimpleFileIOStatus status,
                const char *additionalInfo,
                uint64_t bytesTransferred,
                uint64_t totalBytesToTransfer,
                uint8_t *privResult,
                size_t privResultLen,
                uint8_t *caResult,
                size_t caResultLen);
        virtual ~InternalEnrollmentUpdate();
        
    private:
        COMMO_DISALLOW_COPY(InternalEnrollmentUpdate);
    };


    struct EnrollmentURLRequest : public URLRequest
    {
        EnrollmentURLRequest(EnrollmentRequest *enrollRequest,
                             const std::string &url,
                             URLRequestType urlReqType);
        virtual ~EnrollmentURLRequest();
        
        virtual void curlExtraConfig(CURL *curlCtx)
                               COMMO_THROW (IOStatusException);
        virtual URLIOUpdate *createUpdate(
            const int xferid,
            SimpleFileIOStatus status,
            const char *additionalInfo,
            uint64_t bytesTransferred,
            uint64_t totalBytesToTransfer);
            
        virtual SimpleFileIOStatus statusForResponse(int response);

        virtual void downloadedData(uint8_t *data, size_t len);
        virtual size_t dataForUpload(uint8_t *data, size_t len);
        virtual uint64_t getUploadLen();

        std::vector<uint8_t> docBuffer;
        std::vector<uint8_t> uploadDocBuffer;

    private:
        EnrollmentRequest *enrollRequest;
        struct curl_slist *customHeaders;
        
        static const size_t MAX_DOCUMENT_SIZE = 10 * 1024 * 1024;
        bool maxSizeExceeded;
        size_t readOffset;

        COMMO_DISALLOW_COPY(EnrollmentURLRequest);
    };

}







/****************************************************************/
// Manager ctor/dtor

EnrollmentManager::EnrollmentManager(
        CommoLogger *logger, 
        URLRequestManager *urlManager, 
        CryptoUtil *crypto,
        ContactUID *ourUid,
        EnrollmentIO *io) :
            ThreadedHandler(1, THREAD_NAMES),
            logger(logger),
            urlManager(urlManager),
            crypto(crypto),
            ourUid(ourUid),
            io(io),
            requestsMutex(),
            allRequests(),
            newRequests(),
            needsAttention(),
            needsAttentionMonitor(),
            nextId(0)
{
    startThreads();
}


EnrollmentManager::~EnrollmentManager()
{
    stopThreads();
    urlManager->cancelRequestsForIO(this);
    
    for (auto entry : allRequests)
        delete entry.second;
    allRequests.clear();
    newRequests.clear();
    needsAttention.clear();
}

/****************************************************************/
// Public API

CommoResult EnrollmentManager::enrollmentInit(int *enrollmentId,
                                              const char *host,
                                              int port,
                                              bool verifyHost,
                                              const char *user,
                                              const char *password,
                                              bool useTokenAuth,
                                              const uint8_t *caCert,
                                              size_t caCertLen,
                                              const char *caCertPass,
                                              const char *clientCertPassword,
                                              const char *enrolledTrustPassword,
                                              const char *keyPass,
                                              int keyLen,
                                              const char *clientVersionInfo)
{
    Lock lock(requestsMutex);

    try {
        if (allRequests.find(nextId) != allRequests.end())
            throw std::invalid_argument("Too many concurrent requests");
        
        EnrollmentRequest *req = new EnrollmentRequest(
            nextId,
            host,
            port,
            verifyHost,
            user,
            password,
            useTokenAuth,
            caCert,
            caCertLen,
            caCertPass,
            clientCertPassword,
            enrolledTrustPassword,
            keyPass,
            keyLen,
            clientVersionInfo,
            ourUid,
            logger,
            crypto);
        
        allRequests[nextId] = req;
        newRequests[nextId] = req;
        *enrollmentId = nextId;

        InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
            "enrollment new - id %d host=%s port=%d auth=%s versiontoken=%s hostverification=%s", 
            nextId, host, port, useTokenAuth ? "token" : "basic", 
            clientVersionInfo ? clientVersionInfo : "(null)",
            verifyHost ? "on" : "off");

        nextId++;
    } catch (SSLArgException &ex) {
        InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "enrollmentInit SSL argument: %d (%s)", ex.errCode, ex.what());
        return ex.errCode;
    } catch (std::invalid_argument &ex) {
        InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "enrollmentInit invalid argument: %s", ex.what());
        return COMMO_ILLEGAL_ARGUMENT;
    }
        
    return COMMO_SUCCESS;
}


CommoResult EnrollmentManager::enrollmentStart(int enrollmentId)
{
    Lock lock(requestsMutex);

    auto iter = newRequests.find(enrollmentId);
    if (iter == newRequests.end())
        return COMMO_ILLEGAL_ARGUMENT;
    
    EnrollmentRequest *req = iter->second;
    newRequests.erase(iter);
    needsAttention.push_back(req);

    needsAttentionMonitor.broadcast(lock);
    return COMMO_SUCCESS;
}
    


/****************************************************************/
// URLRequestIO callback

void EnrollmentManager::urlRequestUpdate(URLIOUpdate *urlIOUpdate)
{
    BufferedURLIOUpdate *internalUpdate = (BufferedURLIOUpdate *)urlIOUpdate;
    EnrollmentRequest *req = internalUpdate->masterRequest;

    EnrollmentIOUpdate *up = NULL;
    if (internalUpdate->status == FILEIO_INPROGRESS) {
        // progress updates just pass through
        up = new InternalEnrollmentUpdate(req->step,
            req->xferid,
            internalUpdate->status,
            internalUpdate->additionalInfo,
            internalUpdate->bytesTransferred,
            internalUpdate->totalBytesToTransfer,
            NULL,
            0,
            NULL,
            0);
    } else {
        // Failure or success - in either event, let the original request
        // control our fate
        up = req->processStateUpdate(urlIOUpdate->getBaseUpdate(),
                                     internalUpdate->resultDocument,
                                     internalUpdate->resultDocumentLen);
        {
            Lock lock(requestsMutex);

            needsAttention.push_back(req);
            needsAttentionMonitor.broadcast(lock);
        }
    }

    if (up) {
        io->enrollmentUpdate(up);
        delete (InternalEnrollmentUpdate *)up;
    }
    
}



/****************************************************************/
// ThreadedHandler implementations

void EnrollmentManager::threadStopSignal(size_t threadNum)
{
    Lock lock(requestsMutex);
    needsAttentionMonitor.broadcast(lock);
}


void EnrollmentManager::threadEntry(size_t threadNum)
{
    if (threadNum == REQUESTS_THREADID)
        requestsThreadProcess();
}


/****************************************************************/
// Request thread methods

void EnrollmentManager::requestsThreadProcess()
{
    while (!threadShouldStop(REQUESTS_THREADID)) {
        EnrollmentRequest *r = NULL;
        {
            Lock lock(requestsMutex);
            
            if (needsAttention.empty()) {
                needsAttentionMonitor.wait(lock);
                continue;
            }
            r = needsAttention.front();
            needsAttention.pop_front();

            if (r->completed) {
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "enrollment id: %d destroy", r->xferid);
                allRequests.erase(r->xferid);
                delete r;
                continue;
            }
        }
        
        if (r->step == ENROLL_STEP_KEYGEN) {
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "enrollment id: %d generating key, len=%d", r->xferid, r->keyLen);
            char *key = crypto->generateKeyCryptoString(r->keyPassword.c_str(), 
                                                        r->keyLen);
            EnrollmentIOUpdate *up = r->processStateUpdate(NULL, (uint8_t *)key, key ? strlen(key) : 0);
            if (key)
                crypto->freeCryptoString(key);
            io->enrollmentUpdate(up);
            {
                // Re-insert as we need attention again
                Lock lock(requestsMutex);
                needsAttention.push_back(r);
            }
            delete (InternalEnrollmentUpdate *)up;
        } else {
            int xferId;
            urlManager->initRequest(&xferId, this,
                r->createURLRequest());
            urlManager->startTransfer(xferId);
        }
    }
}



/****************************************************************/
// EnrollmentRequest impl

EnrollmentRequest::EnrollmentRequest(
        int xferid,
        const char *host,
        int port,
        bool verifyHost,
        const char *user,
        const char *password,
        bool useTokenAuth,
        const uint8_t *caCert,
        size_t caCertLen,
        const char *caCertPassword,
        const char *clientCertPassword,
        const char *enrolledTrustPassword,
        const char *keyPass,
        int keyLen,
        const char *clientVersionInfo,
        ContactUID *ourUid,
        CommoLogger *logger,
        CryptoUtil *crypto)
        COMMO_THROW (SSLArgException, std::invalid_argument) :
            xferid(xferid),
            ourUid(ourUid),
            logger(logger),
            crypto(crypto),
            step(ENROLL_STEP_KEYGEN),
            completed(false),
            host(),
            port(port),
            verifyHost(verifyHost),
            caCerts(NULL),
            nCaCerts(0),
            user(),
            password(),
            useTokenAuth(useTokenAuth),
            clientCertPassword(),
            enrolledTrustPassword(),
            keyPassword(),
            keyLen(keyLen),
            clientVersionInfo(),
            privKeyPem(),
            csr()
{
    if (port <= 0 || port > 65535 || !user || !host || !keyPass || !password ||
            !clientCertPassword || !enrolledTrustPassword)
        throw std::invalid_argument("Port out of range or missing other required arg");
    
    this->host = host;
    this->user = user;
    this->password = password;
    this->keyPassword = keyPass;
    if (clientVersionInfo)
        this->clientVersionInfo = clientVersionInfo;
    this->clientCertPassword = clientCertPassword;
    this->enrolledTrustPassword = enrolledTrustPassword;
    
    if (caCert) {
        if (!caCertPassword)
            throw SSLArgException(COMMO_INVALID_CACERT_PASSWORD, 
                                  "certificate password not given");
        InternalUtils::readCACerts(caCert, caCertLen, caCertPassword,
                                   &caCerts, &nCaCerts);
    }
}

EnrollmentRequest::~EnrollmentRequest()
{
    if (caCerts)
        sk_X509_pop_free(caCerts, X509_free);
}

// get url for current step; throws if not valid for current step
URLRequest *EnrollmentRequest::createURLRequest() COMMO_THROW(std::invalid_argument)
{
    std::stringstream ss;
    ss << "https://" << host << ":" << port << "/Marti/api/tls/";
    URLRequest::URLRequestType urlType = URLRequest::BUFFER_DOWNLOAD;
    switch (step) {
        case ENROLL_STEP_KEYGEN:
            throw std::invalid_argument("no url for keygen");
            break;
        case ENROLL_STEP_CSR:
            ss << "config";
            break;
        case ENROLL_STEP_SIGN:
            urlType = URLRequest::POST_BUFFER_UPLOAD;
            ss << "signClient";
            ss << "/v2";
            ss << "?clientUid=";
            ss << std::string((const char *)ourUid->contactUID, ourUid->contactUIDLen);
            if (!clientVersionInfo.empty())
                ss << "&version=" << clientVersionInfo;
            break;
    }
    std::string url = ss.str();
    
    EnrollmentURLRequest *ret = new EnrollmentURLRequest(this, url, urlType);
    if (step == ENROLL_STEP_SIGN) {
        const uint8_t *ccsr = (const uint8_t *)csr.c_str();
        ret->uploadDocBuffer.insert(ret->uploadDocBuffer.end(), ccsr, ccsr + csr.length());
    }
    return ret;
}


EnrollmentIOUpdate *EnrollmentRequest::processStateUpdate(
                            SimpleFileIOUpdate *sourceUpdate, 
                            const uint8_t *resultDoc, size_t resultDocSize)
{
    InternalEnrollmentUpdate *ret = NULL;
    const char *startStatus = getStatusString();
    SimpleFileIOStatus causeStatus = FILEIO_OTHER_ERROR;
    
    switch (step) {
    case ENROLL_STEP_KEYGEN:
        {
            uint8_t *docCopy = NULL;
            if (resultDoc) {
                privKeyPem = std::string((const char *)resultDoc, resultDocSize);
                docCopy = new uint8_t[resultDocSize];
                memcpy(docCopy, resultDoc, resultDocSize);
                causeStatus = FILEIO_SUCCESS;
            } 
            ret = new InternalEnrollmentUpdate(
                        step,
                        xferid,
                        causeStatus,
                        docCopy ? NULL : "key generation failed",
                        0,
                        0,
                        docCopy,
                        docCopy ? resultDocSize : 0,
                        NULL,
                        0);
            if (docCopy) {
                step = ENROLL_STEP_CSR;
            } else {
                // fatal error will be fired
                completed = true;
            }
        }
        break;
    case ENROLL_STEP_CSR:
        causeStatus = sourceUpdate->status;
        if (sourceUpdate->status == FILEIO_SUCCESS) {
            try {
                parseCsrDoc(resultDoc, resultDocSize);
                ret = new InternalEnrollmentUpdate(
                            step,
                            xferid,
                            FILEIO_SUCCESS,
                            NULL,
                            sourceUpdate->bytesTransferred,
                            sourceUpdate->totalBytesToTransfer,
                            NULL,
                            0,
                            NULL,
                            0);
                step = ENROLL_STEP_SIGN;
            } catch (std::invalid_argument &ex) {
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, "enrollment id: %d parsing returned CSR failed (%s)", xferid, ex.what());
                ret = new InternalEnrollmentUpdate(
                            step,
                            xferid,
                            FILEIO_OTHER_ERROR,
                            ex.what(),
                            sourceUpdate->bytesTransferred,
                            sourceUpdate->totalBytesToTransfer,
                            NULL,
                            0,
                            NULL,
                            0);
                completed = true;
            }
        } else {
            // document fetch error - pass through info from source update
            ret = new InternalEnrollmentUpdate(
                        step,
                        xferid,
                        sourceUpdate->status,
                        sourceUpdate->additionalInfo,
                        sourceUpdate->bytesTransferred,
                        sourceUpdate->totalBytesToTransfer,
                        NULL,
                        0,
                        NULL,
                        0);
            completed = true;
        }
    
        break;
    case ENROLL_STEP_SIGN:
        causeStatus = sourceUpdate->status;
        if (sourceUpdate->status == FILEIO_SUCCESS) {
            try {
                uint8_t *signedCAs = NULL;
                uint8_t *signedClient = NULL;
                size_t signedCAsSize = 0;
                size_t signedClientSize = 0;
                parseSigningXML(&signedCAs, &signedCAsSize,
                                &signedClient, &signedClientSize,
                                resultDoc, resultDocSize);
                
                ret = new InternalEnrollmentUpdate(
                            step,
                            xferid,
                            FILEIO_SUCCESS,
                            NULL,
                            sourceUpdate->bytesTransferred,
                            sourceUpdate->totalBytesToTransfer,
                            signedClient,
                            signedClientSize,
                            signedCAs,
                            signedCAsSize);
                
                completed = true;
                
            } catch (std::invalid_argument &ex) {
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR,
                    "enrollment id: %d parsing signed certs (v2) failed (%s)",
                    xferid, ex.what());
                ret = new InternalEnrollmentUpdate(
                            step,
                            xferid,
                            FILEIO_OTHER_ERROR,
                            ex.what(),
                            sourceUpdate->bytesTransferred,
                            sourceUpdate->totalBytesToTransfer,
                            NULL,
                            0,
                            NULL,
                            0);
                completed = true;
            }
        } else {
            // failed to fetch document
            InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR,
                "enrollment id: %d signing (v2) failed (%s)",
                xferid, sourceUpdate->additionalInfo);
            ret = new InternalEnrollmentUpdate(
                        step,
                        xferid,
                        sourceUpdate->status,
                        sourceUpdate->additionalInfo,
                        sourceUpdate->bytesTransferred,
                        sourceUpdate->totalBytesToTransfer,
                        NULL,
                        0,
                        NULL,
                        0);
            completed = true;
        }
        break;
    }
    
    InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "enrollment id: %d state update %s -> %s, cause: %d (%s)", xferid, startStatus, getStatusString(), causeStatus, (sourceUpdate && sourceUpdate->additionalInfo) ? sourceUpdate->additionalInfo : "");

    return ret;
}


void EnrollmentRequest::parseCsrDoc(const uint8_t *docBuf, size_t docSize)
    COMMO_THROW(std::invalid_argument)
{
    std::vector<std::pair<std::string, std::string>> entries;
    // Parse docBuffer
    xmlDoc *doc = xmlReadMemory((const char *)docBuf,
                                (int)docSize, "mbuf:",
                                NULL, XML_PARSE_NONET);
    xmlNode *entriesNode = NULL;
    try {
        if (!doc)
           throw std::invalid_argument("Invalid xml document");
        
        xmlNode *rootElement = xmlDocGetRootElement(doc);
        if (!rootElement || !xmlStrEqual(rootElement->name, (const xmlChar *)"certificateConfig"))
           throw std::invalid_argument("Invalid root node for xml response");

        entriesNode = getFirstChildElementByName(rootElement, (const xmlChar *)"nameEntries");
        if (!entriesNode)
           throw std::invalid_argument("Invalid xml response structure - missing nameEntries");
        
    } catch (std::invalid_argument &) {
        if (doc)
            xmlFreeDoc(doc);
        throw;
    }

    // Before adding CSR config info, add our CN
    entries.push_back(std::pair<std::string, std::string>("CN", user));

    // Now walk all config entries and add to csr config
    for (xmlNode *child = entriesNode->children; child; child = child->next) {
        if (xmlStrEqual(child->name, (const xmlChar *)"nameEntry")) {
            xmlChar *namep = xmlGetProp(child, (const xmlChar *)"name");
            xmlChar *valp = xmlGetProp(child, (const xmlChar *)"value");
            if (namep && valp) {
                entries.push_back(std::pair<std::string, std::string>
                    ((char *)namep, (char *)valp)
                );
            }
            if (namep)
                xmlFree(namep);
            if (valp)
                xmlFree(valp);
        }
    }

    xmlFreeDoc(doc);

    // migrate to arrays
    size_t nentries = entries.size();
    const char **keys = new const char *[nentries];
    const char **vals = new const char *[nentries];
    for (size_t i = 0; i < nentries; ++i) {
        keys[i] = entries[i].first.c_str();
        vals[i] = entries[i].second.c_str();
    }
    
    // Generate actual CSR using our compiled config
    bool csrOk = crypto->generateCSR(csr, keys, vals, nentries, 
                                    privKeyPem.c_str(), keyPassword.c_str());

    // Free our entries
    delete[] keys;
    delete[] vals;
    
    if (!csrOk)
        throw std::invalid_argument("CSR generation failed using provided parameters");
    
    // Lastly, remove PEM start/end banners
    std::string pemHeader("-----BEGIN CERTIFICATE REQUEST-----\n");
    std::string pemFooter("-----END CERTIFICATE REQUEST-----\n");
    size_t n = csr.find(pemHeader);
    if (n != std::string::npos)
        csr.replace(n, pemHeader.length(), "");
    n = csr.find(pemFooter);
    if (n != std::string::npos)
        csr.replace(n, pemFooter.length(), "");
}


void EnrollmentRequest::parseSigningXML(uint8_t **p12caOut, size_t *caSizeOut, 
        uint8_t **p12clientOut, size_t *clientSizeOut,
        const uint8_t *docBuf, size_t docSize) COMMO_THROW(std::invalid_argument)
{
    EVP_PKEY *pkey = crypto->stringToPkey(privKeyPem.c_str(), keyPassword.c_str());
    if (!pkey)
        throw std::invalid_argument("Private key could not be read");
    STACK_OF(X509) *caStack = sk_X509_new_null();
    if (!caStack) {
        EVP_PKEY_free(pkey);
        throw std::invalid_argument("CA stack allocation failed");
    }
    X509 *clientCert = NULL;

    xmlDoc *doc = xmlReadMemory((const char *)docBuf,
                                (int)docSize, "mbuf:",
                                NULL, XML_PARSE_NONET);
    try {
        if (!doc)
           throw std::invalid_argument("Invalid xml document");
        
        xmlNode *rootElement = xmlDocGetRootElement(doc);
        if (!rootElement || !xmlStrEqual(rootElement->name, (const xmlChar *)"enrollment"))
           throw std::invalid_argument("Invalid root node for signing xml response");

         for (xmlNode *child = rootElement->children; child; child = child->next) {
            xmlChar *certPEMString = xmlNodeListGetString(doc, child->xmlChildrenNode, true);
            // TAK Server does not include PEM header/footer but openssl
            // requires it.  Also TAK server PEM string may or may not
            // end with a newline, and OpenSSL requires precisely:
            // \n{FOOTER HERE}\n
            std::string pem((char *)certPEMString);
            if (pem.find(PEM_CERT_HEADER) != 0)
                pem.insert(0, PEM_CERT_HEADER);
            if (pem.at(pem.length() - 1) != '\n')
                pem.append("\n");
            size_t footLoc = pem.length();
            if (footLoc >= PEM_CERT_FOOTER.length())
                footLoc -= PEM_CERT_FOOTER.length();
            else
                footLoc = 0;
            if (pem.find(PEM_CERT_FOOTER, footLoc) == std::string::npos)
                pem.append(PEM_CERT_FOOTER);
            X509 *cert = crypto->stringToCert(pem.c_str());
            xmlFree(certPEMString);
            if (!cert)
                throw std::invalid_argument("Invalid certificate returned by server");

            if (xmlStrEqual(child->name, (const xmlChar *)"signedCert")) {
                if (clientCert) {
                    X509_free(cert);
                    throw std::invalid_argument("Multiple signed certificates returned by server");
                }

                clientCert = cert;
            } else {
                // CA
                sk_X509_push(caStack, cert);
                std::string alias("enrollCaResult");
                alias += InternalUtils::intToString(sk_X509_num(caStack));
                X509_alias_set1(cert, (const unsigned char *)alias.c_str(), -1);
            }
        }
        
        if (!clientCert)
            throw std::invalid_argument("No signed certificate returned by server");

    } catch (std::invalid_argument &) {
        if (doc)
            xmlFreeDoc(doc);
        sk_X509_pop_free(caStack, X509_free);
        if (clientCert)
            X509_free(clientCert);
        EVP_PKEY_free(pkey);

        throw;
    }

    xmlFreeDoc(doc);
    
    uint8_t *p12ca = NULL;
    size_t caSize = 0;
    uint8_t *p12client = NULL;
    size_t clientSize = 0;

    // Now combine client cert with CAs and private key
    // to provide the client certificate store
    clientSize = crypto->generateKeystore(&p12client, clientCert, pkey,
                                               caStack,
                                               clientCertPassword.c_str(),
                                               "TAK Client Cert");
    
    // Combine CA list to produce CA store, but only if above succeeded
    bool hasCa = sk_X509_num(caStack) > 0;
    if (clientSize && hasCa)
        caSize = crypto->generateKeystore(&p12ca, NULL, NULL,
                                               caStack,
                                               enrolledTrustPassword.c_str(),
                                               "");

    // Free X509 objects now that we are done
    sk_X509_pop_free(caStack, X509_free);
    X509_free(clientCert);
    EVP_PKEY_free(pkey);
    
    if (!clientSize || (hasCa && !caSize))
        throw std::invalid_argument("Output certificate storage generation failed");

    *p12clientOut = p12client;
    *clientSizeOut = clientSize;
    *p12caOut = p12ca;
    *caSizeOut = caSize;
}


const char *EnrollmentRequest::getStatusString()
{
    if (completed) return "complete";

    switch (step) {
        case ENROLL_STEP_KEYGEN:
            return "keygen";
            break;

        case ENROLL_STEP_CSR:
            return "csrconfig";
            break;
        
        case ENROLL_STEP_SIGN:
            return "signv2";
            break;
        default:
            return "unknown";
    }
}



/****************************************************************/
// BufferedURLIOUpdate impl

// Copies additionalInfo, adopts byte arrays
BufferedURLIOUpdate::BufferedURLIOUpdate(
        const int xferid,
        const SimpleFileIOStatus status,
        const char *additionalInfo,
        uint64_t bytesTransferred,
        uint64_t totalBytesToTransfer,
        EnrollmentRequest *masterRequest,
        uint8_t *resultDocument,
        size_t resultDocumentLen) :
            URLIOUpdate(),
            SimpleFileIOUpdate(xferid, status,
                additionalInfo ? new char[strlen(additionalInfo) + 1] : NULL,
                bytesTransferred, totalBytesToTransfer),
            masterRequest(masterRequest),
            resultDocument(resultDocument),
            resultDocumentLen(resultDocumentLen)
{
    if (additionalInfo)
        strcpy(const_cast<char * const>(this->additionalInfo), additionalInfo);
}


BufferedURLIOUpdate::~BufferedURLIOUpdate()
{
    if (additionalInfo)
        delete[] additionalInfo;
    if (resultDocument)
        delete[] resultDocument;
}


SimpleFileIOUpdate *BufferedURLIOUpdate::getBaseUpdate()
{
    return this;
}



/****************************************************************/
// InternalEnrollmentUpdate impl

// Copies additionalInfo, adopts byte arrays
InternalEnrollmentUpdate::InternalEnrollmentUpdate(
        EnrollmentStep step,
        const int xferid,
        const SimpleFileIOStatus status,
        const char *additionalInfo,
        uint64_t bytesTransferred,
        uint64_t totalBytesToTransfer,
        uint8_t *privResult,
        size_t privResultLen,
        uint8_t *caResult,
        size_t caResultLen) :
            EnrollmentIOUpdate(step, xferid, status,
                additionalInfo ? new char[strlen(additionalInfo) + 1] : NULL,
                bytesTransferred, totalBytesToTransfer,
                privResult, privResultLen,
                caResult, caResultLen)
{
    if (additionalInfo)
        strcpy(const_cast<char * const>(this->additionalInfo), additionalInfo);
}


InternalEnrollmentUpdate::~InternalEnrollmentUpdate()
{
    if (additionalInfo)
        delete[] additionalInfo;
    if (privResult)
        delete[] privResult;
    if (caResult)
        delete[] caResult;
}




/****************************************************************/
// EnrollmentURLRequest impl


EnrollmentURLRequest::EnrollmentURLRequest(EnrollmentRequest *enrollRequest,
                                           const std::string &url,
                                           URLRequestType type) :
                                               URLRequest(
                                                   type,
                                                   url,
                                                   "",
                                                   true,
                                                   enrollRequest->caCerts,
                                                   enrollRequest->verifyHost),
                                               docBuffer(),
                                               uploadDocBuffer(),
                                               enrollRequest(enrollRequest),
                                               customHeaders(NULL),
                                               maxSizeExceeded(false),
                                               readOffset(0)
{
}


EnrollmentURLRequest::~EnrollmentURLRequest()
{
    if (customHeaders)
        curl_slist_free_all(customHeaders);
}
    
void EnrollmentURLRequest::curlExtraConfig(CURL *curlCtx)
        COMMO_THROW (IOStatusException)
{
    bool headers = false;
    if (enrollRequest->useTokenAuth) {
        // Set up for token auth
        std::string tokenHdr("Authorization: Bearer ");
        tokenHdr += enrollRequest->password.c_str();
        customHeaders = curl_slist_append(customHeaders, tokenHdr.c_str());
        headers = true;
    } else {
        // Basic auth
        CURL_CHECK(curl_easy_setopt(curlCtx, CURLOPT_USERNAME,
                                    enrollRequest->user.c_str()));
        CURL_CHECK(curl_easy_setopt(curlCtx, CURLOPT_PASSWORD,
                                    enrollRequest->password.c_str()));
    }
    // Set xml for non-legacy endpoint
    if (enrollRequest->step == ENROLL_STEP_SIGN) {
        customHeaders = curl_slist_append(customHeaders, "Accept: application/xml");
        customHeaders = curl_slist_append(customHeaders, "Content-Type: application/octet-stream");
        headers = true;
    }
    if (headers)
        CURL_CHECK(curl_easy_setopt(curlCtx, CURLOPT_HTTPHEADER, customHeaders));
}


URLIOUpdate *EnrollmentURLRequest::createUpdate(
        const int xferid,
        SimpleFileIOStatus status,
        const char *additionalInfo,
        uint64_t bytesTransferred,
        uint64_t totalBytesToTransfer)
{
    uint8_t *resultBuffer = NULL;
    size_t resultBufferLen = 0;

    if (status == FILEIO_SUCCESS) {
        resultBufferLen = docBuffer.size();
        resultBuffer = new uint8_t[resultBufferLen];
        memcpy(resultBuffer, docBuffer.data(), resultBufferLen);
    }

    BufferedURLIOUpdate *update = new BufferedURLIOUpdate(
                xferid,
                status,
                additionalInfo,
                bytesTransferred,
                totalBytesToTransfer,
                enrollRequest,
                resultBuffer,
                resultBufferLen);

    return update;
}


SimpleFileIOStatus EnrollmentURLRequest::statusForResponse(int response)
{
    SimpleFileIOStatus ret;
    switch (response) {
        case 401:
            ret = FILEIO_AUTH_ERROR;
            break;
        case 403:
            ret = FILEIO_ACCESS_DENIED;
            break;
        case 404:
        case 410:
            ret = FILEIO_URL_NO_RESOURCE;
            break;
        case 200:
            ret = FILEIO_SUCCESS;
            break;
        default:
            ret = FILEIO_OTHER_ERROR;
            break;
    }
    return ret;
}


void EnrollmentURLRequest::downloadedData(uint8_t *data, size_t len)
{
    if (docBuffer.size() + len < MAX_DOCUMENT_SIZE)
        docBuffer.insert(docBuffer.end(), data, data + len);
    else
        maxSizeExceeded = true;
}


size_t EnrollmentURLRequest::dataForUpload(uint8_t *data, size_t len)
{
    size_t n = uploadDocBuffer.size() - readOffset;
    if (n > len)
        n = len;

    memcpy(data, uploadDocBuffer.data() + readOffset, n);
    return n;
}


uint64_t EnrollmentURLRequest::getUploadLen()
{
    return uploadDocBuffer.size();
}
