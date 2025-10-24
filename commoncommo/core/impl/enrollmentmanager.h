#ifndef IMPL_ENROLLMENTMANAGER_H_
#define IMPL_ENROLLMENTMANAGER_H_


#include "enrollment.h"
#include "urlrequestmanager.h"
#include "commothread.h"
#include "cryptoutil.h"
#include <libxml/parser.h>
#include <set>
#include <vector>

namespace atakmap {
namespace commoncommo {
namespace impl
{

struct EnrollmentRequest
{
    int xferid;
    ContactUID *ourUid;
    CommoLogger *logger;
    CryptoUtil *crypto;

    // what step is this request processing
    EnrollmentStep step;
    // Completed, either successfully or fatal error
    bool completed;

    // configuration from requester
    std::string host;
    int port;
    bool verifyHost;
    STACK_OF(X509) *caCerts;
    int nCaCerts;
    std::string user;
    std::string password;
    bool useTokenAuth;
    // pw to use for output client cert
    std::string clientCertPassword;
    // pw to use for output trust
    std::string enrolledTrustPassword;
    // pw to use for output key
    std::string keyPassword;
    int keyLen;
    // empty string means don't send
    std::string clientVersionInfo;

    // invalid until KEYGEN step completed    
    std::string privKeyPem;
    // invalid until CSR step completed
    std::string csr;

    
    EnrollmentRequest(int xferid,
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
                      CryptoUtil *crypto) COMMO_THROW (SSLArgException, std::invalid_argument);
    ~EnrollmentRequest();
    
    // get url request for current state; throws if not valid for current state
    URLRequest *createURLRequest() COMMO_THROW(std::invalid_argument);
    
    // parse document result from server for current step and
    // update request internal state (step, outputs)
    // Returns new EnrollmentIOUpdate based on sourceUpdate and the completed
    // state; **may be NULL if no update should be fired
    // Assumes that the passed info represents results that are not in progress
    // For KEYGEN state, sourceUpdate not used and NULL/0 results indicates
    // failure
    EnrollmentIOUpdate *processStateUpdate(SimpleFileIOUpdate *sourceUpdate, 
                            const uint8_t *resultDoc, size_t resultDocSize);
                            
    // Parse CSR document, populate csrEntries map
    void parseCsrDoc(const uint8_t *doc, size_t docSize) COMMO_THROW(std::invalid_argument);
    // Return new p12 of CAs and new p12 of client cert with CAs embedded
    void parseSigningXML(uint8_t **p12ca, size_t *caSize, 
                         uint8_t **p12client, size_t *clientSize,
                         const uint8_t *doc, size_t docSize) COMMO_THROW(std::invalid_argument);
  private:
    const char *getStatusString();
};


class EnrollmentManager : public ThreadedHandler, public URLRequestIO
{
public:
    EnrollmentManager(CommoLogger *logger, URLRequestManager *urlManager,
                      CryptoUtil *crypto, ContactUID *ourUid, EnrollmentIO *io);
    ~EnrollmentManager();

    CommoResult enrollmentInit(int *enrollmentId,
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
                               const char *clientVersionInfo);
    CommoResult enrollmentStart(int enrollmentId);
    
    // URLRequestIO
    virtual void urlRequestUpdate(URLIOUpdate *update);

protected:
    enum { REQUESTS_THREADID };
    virtual void threadStopSignal(size_t threadNum);
    virtual void threadEntry(size_t threadNum);

private:
    CommoLogger *logger;
    URLRequestManager *urlManager;
    CryptoUtil *crypto;
    ContactUID *ourUid;
    EnrollmentIO *io;

    // Sync on for all request-containing data structures
    thread::Mutex requestsMutex;

    // Listing of all unfinished requests, regardless of state
    std::map<int, EnrollmentRequest *> allRequests;

    // Requests that are new, but not yet started    
    std::map<int, EnrollmentRequest *> newRequests;

    // Requests that have been started and need our attention
    std::deque<EnrollmentRequest *> needsAttention;
    thread::CondVar needsAttentionMonitor;
    
    int nextId;



    COMMO_DISALLOW_COPY(EnrollmentManager);

    void requestsThreadProcess();

};

}
}
}

#endif
