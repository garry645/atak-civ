/*
 * Simplistic example illustrating use of commoncommo to connect to a
 * streaming cot (TAK) server to receive and broadcast CoT messages.
 *
 * Adjust TAK server parameters in getServerParams(...)
 * Build using (linux):
     g++ -I ../include \
         -I ../include/libxml2 \
         -o streamingcotio streamingcotio.cpp \
         -L ../lib -lcommoncommo -lprotobuf-lite -lxml2 \
         -lcurl -lmicrohttpd -liconv -lz -lssl -lcrypto
 *    It is possible you may need to append linker flags (-l lib) for other
 *    libraries depending on commo's build configuration for your platform
 * Run using ./streamingcotio your_uid_here your_callsign_here
 * UID and callsign should be reasonably unique to you
 */

// Commo headers we are using
#include <commo.h>
#include <commologger.h>
#include <cotmessageio.h>
#include <contactuid.h>
#include <commoresult.h>
#include <netinterface.h>

#include <string>
#include <vector>
#include <cstdio>
#include <cstring>

#include <libxml/tree.h>
#include <openssl/ssl.h>
#include <curl/curl.h>

#ifndef WIN32
#include <unistd.h>
#include <signal.h>
#endif


// Import commo public namespace
using namespace atakmap::commoncommo;


namespace
{

bool readFile(std::vector<uint8_t> *outBuf, const char *path)
{
    FILE *f = fopen(path, "rb");
    if (!f)
        return false;
    uint8_t buf[1024];
    bool done = false;
    
    while (!done) {
        size_t n = fread(buf, 1, 1024, f);
        if (n != 1024) {
            if (feof(f))
                done = true;
            else
                break;
        }
        outBuf->insert(outBuf->end(), buf, buf + n);
    }
    
    fclose(f);
    
    return done;
}

/*
 * Get parameters for the server connection.
 * Filling in host and port are required.
 * If you want to use SSL, also fill in the last 4 parameters.
 * The certs must be valid PKCS#12 data (obtained from TAK server admin)
 * and use the passwords in the last 2 parameters to decrypt
 * Return true if everything is ok, false to error out
 */
bool getServerParams(std::string *host, int *port, 
                     std::vector<uint8_t> *cert, 
                     std::vector<uint8_t> *caCert,
                     std::string *certPw,
                     std::string *caCertPw)
{
    // Set to IP or hostname of your server
    *host = "myserver.example.com";

    // Set port of your server - be sure to use correct one for streaming
    // messaging, with or without SSL as appropriate. See TAK server
    // install/administrative/configuration documentation.
    // 8088 is default tak server port for non-SSL, which is not typically
    // used. 8089 is default tak server port for SSL, which is much more
    // typical.  We are using the non-SSL port in this example as we
    // don't have certificates!
    *port = 8088;
    
    // To do for SSL connection -
    // Read in client certificate and populate *cert
    // Read in trust/CA store and populate *caCert
    // Populate *certPw and *caCertPw with passwords for each
    // For example:
    //if (!readFile(cert, "my_client_cert.p12"))
    //    return false;
    //if (!readFile(caCert, "my_truststore.p12"))
    //    return false;
    //*certPw = "somepassword";
    //*caCertPw = "someotherpassword";
    
    return true;
}
    


/*
 * Example implementation of a logger that outputs
 * to stdout.  Note that more sophisticated implementations may require
 * thread synchronization as the log method can be invoked by multiple threads
 * at once (see API docs)
 */
class StdoutLogger : public CommoLogger
{
public:
    StdoutLogger() {};
    virtual void log(Level level, Type type, const char *message, void *detail);
};

void StdoutLogger::log(Level level, Type type, const char *message, void *detail)
{
    printf("CommoLogMessage: %s\n", message);
}

/*
 * Example implementation of a CoTMessageListener that just outputs received
 * messages to stdout
 */
class StdoutCoTListener : public CoTMessageListener
{
public:
    virtual void cotMessageReceived(const char *cotMessage, const char *rxIfaceId);
};

void StdoutCoTListener::cotMessageReceived(const char *cotMessage, const char *rxIfaceId)
{
    printf("CoTMessage from %s - %s\n", rxIfaceId ? rxIfaceId : "(unknown)", cotMessage);
}


/*
 * Example implementation of a ContactPresenceListener that just
 * logs to stdout when new contacts are noticed
 */
class StdoutContactPresenceListener : public ContactPresenceListener
{
public:
    virtual void contactAdded(const ContactUID *c);
    virtual void contactRemoved(const ContactUID *c);
};

void StdoutContactPresenceListener::contactAdded(const ContactUID *c)
{
    std::string uid((const char * const)c->contactUID, c->contactUIDLen);
    printf("Contact added: %s\n", uid.c_str());
}

void StdoutContactPresenceListener::contactRemoved(const ContactUID *c)
{
    std::string uid((const char * const)c->contactUID, c->contactUIDLen);
    printf("Contact removed: %s\n", uid.c_str());
}


// This simply generates a mostly-fixed "SA" cot message suitable to
// broadcast our presence and fixed, fake location.
std::string getSAString(const char *ourUID, const char *ourCallsign)
{
    time_t now = time(NULL);
    struct tm t;
#ifdef WIN32
    gmtime_s(&t, &now);
#else
    gmtime_r(&now, &t);
#endif

    const size_t timebufsize = 256;
    char timebuf[timebufsize];
    char staletimebuf[timebufsize];
    
    strftime(timebuf, timebufsize, "%Y-%m-%dT%H:%M:%S.000Z", &t);
    
    // Stale in 2 minutes
    now += 120;
#ifdef WIN32
    gmtime_s(&t, &now);
#else
    gmtime_r(&now, &t);
#endif
    
    strftime(staletimebuf, timebufsize, "%Y-%m-%dT%H:%M:%S.000Z", &t);

    // The following is horribly inefficient, but suffices for this example
    std::string saString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><event version=\"2.0\" uid=\"";
    saString += ourUID;
    saString += "\" type=\"a-f-G-U-C\" how = \"h-e\" time=\"";
        saString += timebuf;
    saString += "\" start=\"";
    saString += timebuf;
    saString += "\" stale=\"";
    saString += staletimebuf;
    saString += "\">";

    saString += "<point lat=\"37.7459\" lon=\"-119.5332\" hae=\"9999999.0\" "
           "ce=\"9999999\" le=\"9999999\"/>"
           "<detail><contact endpoint=\"1.1.1.1:4242:tcp\""
           " callsign=\"";
    saString += ourCallsign;
    saString += "\"/>"
           "<uid Droid=\"Unk\"/>"
           "<__group name=\"Cyan\" role=\"Team Member\"/>"
           "<status battery=\"100\"/>"
           "<track speed=\"0\" course=\"20.12345\"/>"
           "<precisionlocation geopointsrc=\"User\" altsrc=\"???\"/>"
           "</detail>"
           "</event>";
    return saString;
}


}

int main(int argc, char *argv[])
{
    if (argc != 3) {
        fprintf(stderr, "Usage: %s <my_uid> <my_callsign>\n", argv[0]);
        return 1;
    }
    
    // Do platform-specific setups
#ifdef WIN32
    WSAData wsaData;
    WSAStartup(MAKEWORD(2, 2), &wsaData);
#else
    // Ignore sigpipe resulting from network I/O that writes to disconnected
    // remote tcp pipes
    struct sigaction action;
    struct sigaction oldaction;
    memset(&oldaction, 0, sizeof(struct sigaction));
    sigaction(SIGPIPE, NULL, &oldaction);
    action = oldaction;
    action.sa_handler = SIG_IGN;
    sigaction(SIGPIPE, &action, NULL);
#endif
    
    // Initialize dependent libraries

    // Initialize libxml2
    xmlInitParser();
    
    //SSL_library_init();
    OPENSSL_init_ssl(0, NULL);
    SSL_load_error_strings();

    // Initialize curl
    curl_global_init(CURL_GLOBAL_NOTHING);

    // end dependent library initialization    


    StdoutLogger myLogger;
    ContactUID myUID((const uint8_t *)argv[1], strlen(argv[1]));
    StdoutCoTListener myCotListener;
    StdoutContactPresenceListener myContactListener;

    // Create Commo instance;  args must remain valid until Commo is
    // destroyed
    Commo *commo = new Commo(&myLogger, &myUID, argv[2]); 
    
    // Register our CoTMessageListener so we can be informed of incoming
    // messages
    commo->addCoTMessageListener(&myCotListener);
    
    // Register our ContactPresenceListener so we can be informed of
    // new contacts that are discovered
    commo->addContactPresenceListener(&myContactListener);

    // Get SSL connection parameters for the streaming server connection
    std::vector<uint8_t> cert;
    std::vector<uint8_t> caCert;
    std::string certPw;
    std::string caCertPw;
    std::string host;
    int port;
    if (!getServerParams(&host, &port, &cert, &caCert, &certPw, &caCertPw)) {
        fprintf(stderr, "Failed to get server parameters - exiting\n");
        delete commo;
        return 3;
    }
    
    if (cert.empty() || caCert.empty()) {
        printf("SSL parameters not given, making an unencrypted connection\n");
    } else {
        printf("SSL parameters give, making an SSL connection\n");
    }
    
    // We will listen for all supported types of cot messages
    size_t numTypes = 2;
    CoTMessageType allTypes[numTypes] = {
        SITUATIONAL_AWARENESS,
        CHAT
    };
    CommoResult streamSetupErrcode;
    StreamingNetInterface *streamIface = 
        commo->addStreamingInterface(host.c_str(), port, allTypes, numTypes,
                                 cert.empty() ? NULL : cert.data(),
                                 cert.size(),
                                 caCert.empty() ? NULL : caCert.data(),
                                 caCert.size(),
                                 cert.empty() ? NULL : certPw.c_str(),
                                 caCert.empty() ? NULL : caCertPw.c_str(),
                                 // We are not doing authentication
                                 NULL, NULL,
                                 &streamSetupErrcode);
    if (streamIface == NULL) {
        printf("Streaming interface addition failed - result code = %d!\n", streamSetupErrcode);

        delete commo;
        return 2;
    }
    
    printf("Streaming interface added successfully!\n");

    // Commo will now try to connect to server in background
    // Once connected, we will be passively listening for cot and
    // new contacts in the background

    // From here, we choose to run until interrupted, all the while sending out
    // a simple "SA" CoT approximately every minute to all server
    // participants (broadcasting)
    while (true) {
        std::string sa = getSAString(argv[1], argv[2]);
        CommoResult r = commo->broadcastCoT(sa.c_str());
        if (r != COMMO_SUCCESS) {
            printf("CoT broadcast failed!  (%d)\n", r);
            break;
        }
    
#ifdef WIN32
        Sleep(1000);
#else
        usleep(1000 * 1000);
#endif
    }
     

    delete commo;

    return 0;
}
