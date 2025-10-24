
#ifndef IMPL_COTMESSAGE_H_
#define IMPL_COTMESSAGE_H_


#include "internalutils.h"
#include "cotmessageio.h"
#include "contactuid.h"
#include "commologger.h"
#include <stdexcept>
#include <vector>
#include <set>
#include <string>

namespace atakmap {
namespace commoncommo {

// Forward declare
namespace protobuf {
namespace v1 {
    class CotEvent;
}
}

namespace impl
{

struct CoTMessageImpl;


typedef enum {
    ENDPOINT_QUIC_USESRC,
    ENDPOINT_QUIC,
    ENDPOINT_UDP_USESRC,
    ENDPOINT_UDP,
    ENDPOINT_TCP_USESRC,
    ENDPOINT_TCP,
    ENDPOINT_STREAMING,
    ENDPOINT_NONE
} EndpointType;

typedef enum {
    // indices used by impl - do not change without reviewing impl
    TYPE_SUPPORT = 0,
    TYPE_REQUEST,
    TYPE_RESPONSE,
    TYPE_NONE
} TakControlType;

class CoTFileTransferRequest {
public:
    // All args are copied during construction
    CoTFileTransferRequest(const std::string &access,
                           const std::string &caveats,
                           const std::string &releasableTo,
                           const std::string &sha256hash,
                           const std::string &name,
                           const std::string &senderFilename,
                           const std::string &senderUrl,
                           const uint64_t sizeInBytes,
                           const std::string &senderCallsign,
                           const ContactUID *senderuid,
                           const std::string &ackuid,
                           const bool peerHosted,
                           const int httpsPort);
    CoTFileTransferRequest(const CoTFileTransferRequest &src);
    ~CoTFileTransferRequest();

    // Access attribute to/from the CoT event for this transfer.
    // Not checked for compliance with CoT spec - as-is
    const std::string access;

    // caveats attribute to/from the CoT event for this transfer.
    // Not checked for compliance with CoT spec - as-is
    const std::string caveats;

    // releasableTo attribute to/from the CoT event for this transfer.
    // Not checked for compliance with CoT spec - as-is
    const std::string releasableTo;

    // hash of the file to be transferred
    const std::string sha256hash;

    // name of the transfer
    const std::string name;

    // the sender's indication of the file name on its end
    const std::string senderFilename;

    // the url at which the file may be obtained
    const std::string senderUrl;

    // the size in bytes of the file
    const uint64_t sizeInBytes;

    // Callsign of the sending contact
    const std::string senderCallsign;

    // UID of the sending contact
    const ContactUID *senderuid;

    // uid to be used for any ack message
    // This may be the empty string, in which case no ack is requested
    const std::string ackuid;
    
    // true if the peerHosted attribute is present with value true to signify
    // the file being sent is hosted by the sender and not some external server
    bool peerHosted;
    
    // Value of the httpsPort attribute, if present with a valid unsigned
    // port value.
    // MP_LOCAL_PORT_DISABLE otherwise.
    int httpsPort;
};


class CoTMessage
{
private:
    static const std::string STREAMING_ENDPOINT;
    static const std::string TCP_USESRC_ENDPOINT;
    static const std::string QUIC_USESRC_ENDPOINT;
    static const std::string UDP_USESRC_ENDPOINT;

public:

    // "endpoints", in a TAK CoT message context, are simply
    // stringified network addresses and protocol of the sender.
    // This struct breaks the endpoint string into its components
    struct EndpointInfo {
        // throws if type is NONE or otherwise unparsable
        EndpointInfo(const std::string &epString) COMMO_THROW(std::invalid_argument);
        // throws if type is NONE
        EndpointInfo(const char *host, int port, EndpointType type) COMMO_THROW(std::illegal_argument);

        // Return the type of endpoint.  Will never be ENDPOINT_NONE
        EndpointType type;
        
        // Endpoint host includes only the host -
        // it does not include anything else
        // (port #'s, protocol info), just IP or host portion
        std::string host;

        // The port number encoded in the endpoint. If there was no
        // valid endpoint port, this will be -1. -1 also generally used 
        // for stream type endpoints
        int port;
    };
    typedef std::vector<EndpointInfo> EndpointList;

    // Init a new ping request CoTMessage
    CoTMessage(CommoLogger *logger, const std::string &uid);
    
    // Init a new TakControl/TakRequest message indicating
    // desire to use the specified version of TAK protocol
    // and advertising we support the given set of extensions
    CoTMessage(CommoLogger *logger, const std::string &uid,
               int version, const ExtensionIdSet &extensions);

    // Init a new file transfer request CoTMessage
    CoTMessage(CommoLogger *logger, 
               const std::string &uid, const CoTPointData &point,
               const CoTFileTransferRequest &fileTransferRequest);

    // Init a new file transfer ack CoTMessage for the specified transfer.
    // The message should have an indication of the failure if the
    // transfer failed, else it can contain some description
    CoTMessage(CommoLogger *logger, 
               const std::string &uid, const CoTPointData &point,
               const CoTFileTransferRequest &fileTransferRequest,
               const ContactUID *receiveruid,
               const bool failed,
               const std::string &message);

    // Init a new CoTMessage by deserializing xml from the supplied buffer
    // Throws if there is a formatting error in the supplied serialized form
    CoTMessage(CommoLogger *logger, const uint8_t *data, size_t len)
               COMMO_THROW (std::invalid_argument);
    // Init a new CoTMessage by deserializing protobuf data from the given
    // CotEvent. Throws if there is an error in one or more of the values
    CoTMessage(CommoLogger *logger, const protobuf::v1::CotEvent &event, ExtensionRegistry *extensions)
               COMMO_THROW (std::invalid_argument);
    CoTMessage(const CoTMessage &src) COMMO_THROW (std::invalid_argument);
    ~CoTMessage();

    // Re-init this CoTMessage by deserializing from the supplied buffer;
    // basically the same as the constructor of similar args except without
    // creation of a new CoTMessage object.
    // On success all old state is lost and rebuilt from the deserialized
    // data. On any failure the state is kept precisely the same as before
    // the call was made.
    // Throws if there is a formatting error in the supplied serialized form.
    void reinitFrom(const uint8_t *data, const size_t len) COMMO_THROW (std::invalid_argument);

    // Same as reinitFrom() but for protobuf data
    void reinitFromProtobuf(const protobuf::v1::CotEvent &event, ExtensionRegistry *extensions) COMMO_THROW (std::invalid_argument);

    // Serialize the CoTMessage to a new byte array.  The returned array must
    // be delete[]'d by the caller when finished. A null terminator is added for
    // convenience.  Returns the size of the serialized data without the
    // null terminator.
    // Throws invalid_argument for any errors.
    size_t serialize(uint8_t **buf, bool prettyFormat = false) const COMMO_THROW (std::invalid_argument);

    // Serialize the CoTMessage to the given protocol buffer object, which
    // is assumed to be in a default state at invocation.
    // Throws invalid_argument for any errors.
    // If an error is thrown, the event is likely to be in an indeterminate
    // state and should be discarded.
    void serializeAsProtobuf(protobuf::v1::CotEvent *event, ExtensionRegistry *extensions, bool useAnyExtension, const ExtensionIdSet &extensionIds) const COMMO_THROW (std::invalid_argument);

    // Gets the uid string from the event
    std::string getEventUid() const;

    // "endpoints", in a TAK CoT message context, are simply
    // stringified network addresses and protocol of the sender.
    // This obtains the endpoints of the message in broken out form.
    // The list is *not* cleared on entry.  If the message
    // has no legitimate endpoints, the list will be as-was on return.
    // Returns number of endpoints added.
    // See EndpointInfo for more detail.
    size_t getEndpoints(EndpointList *list) const;

    // This will write the unicast source endpoint using the specified
    // type of endpoint. For the endpoint string, specify only the IP
    // information (no port or transport).
    // The ipAddr is ignored for ENDPOINT_QUIC_USESRC, ENDPOINT_UDP_USESRC,
    // ENDPOINT_TCP_USESRC, ENDPOINT_STREAMING, and ENDPOINT_NONE types.
    // Use ENDPOINT_NONE to clear the endpoints of the message.
    // Use ENDPOINT_STREAMING to set for a TAK server endpoint.
    // Use ENDPOINT_TCP_USESRC to set for tcp replies via packet source address.
    // Use ENDPOINT_UDP_USESRC to set for tcp replies via packet source address.
    // Use ENDPOINT_QUIC_USESRC to set for quic replies via packet source address.
    // For the types which need ipAddr, if ipAddr is empty string
    // this behaves as though ENDPOINT_NONE were passed instead.
    // altEndpointTypes specifies the endpoint types to populate as alternate
    // endpoints; if this is NULL or empty, or the main endpoint type is NONE,
    // the message's alternate endpoints are cleared.
    // Has no effect at all if this CoTMessage has no contact node and no
    // file transfer ack request node.
    void setEndpoints(EndpointType type, const std::string &ipAddr,
                     const std::vector<EndpointType> *altEndpoints);

    // Gets the callsign of the sender of this message. May be the empty string.
    std::string getCallsign() const;

    // Gets the type of the message
    CoTMessageType getType() const;

    // Gets the type of control message represented by this message    
    TakControlType getTakControlType() const;
    
    // If this is a TYPE_SUPPORT message, populates two sets of info:
    // The TAK protocol versions and extension identifiers advertised
    // as being supported in the message. 'allExtensionsSupported'
    // is set to true if the message indicates support
    // for all extensions (wildcard support). Both supplied sets are cleared
    // of existing entries that are not present in this support message.
    // If not a TYPE_SUPPORT message, the supplied sets are cleared and
    // allExtensionsSupported is set to false
    void getTakControlSupportedVersions(std::set<int> *versions, 
                                        ExtensionIdSet *extensions,
                                        bool *allExtensionsSupported) const;
    
    // If this is a TYPE_RESPONSE message, returns the status of the response
    bool getTakControlResponseStatus() const;
    
    // True if this message is a "pong" message
    bool isPong() const;

    // Gets file transfer request details if this message is a request
    // for file transfer, else returns NULL.
    // The returned pointer is valid  until this CoTMessage is reinitialized
    // or destroyed
    const CoTFileTransferRequest *getFileTransferRequest() const;

    // Gets the uid of the sender of the file transfer that this CoTMessage
    // is acknowledging if this message is a file transfer (n)ack.
    // If this is not a file transfer acknowledgement, the returned
    // value will be the empty string.
    std::string getFileTransferAckSenderUid() const;

    // Gets the uid of the file transfer that this CoTMessage is acknowledging
    // if this message is a file transfer (n)ack. Use
    // getFileTransferSucceeded() to determine if this message is an
    // ack or a nack.
    // If this is not a file transfer acknowledgement, the returned
    // value will be the empty string.
    std::string getFileTransferAckUid() const;

    // Returns the file size stated in the acknowledgement of
    // a file transfer.  Returns 0 if this message is not an ack
    // of a file transfer (see getFileTransferAckSenderUid() or 
    // if the value in the ack is not valid
    uint64_t getFileTransferAckSize() const;

    // Returns true if this CoTMessage represents a file transfer
    // acknowledgement (getFileTransferAckUid() returns non-empty result)
    // and this acknowledgement indicates the transfer was successful.
    // Otherwise, false is returned to indicate the transfer failed (or
    // that this CoTMessage does not represent a file transfer acknowledgement)
    bool getFileTransferSucceeded() const;

    // Obtains the reason a file transfer has failed or an ancillary message
    // if a file transfer has succeeded, according to this
    // response from the receiver.  Only meaningful if this CoTMessage
    // represents a file transfer acknowledgement (getFileTransferAckUid()
    // returns a non-empty result).
    std::string getFileTransferReason() const;

    // Gets a ContactUID for the sender of this message.  The returned object
    // is only valid until this CoTMessage is reinitialized (reinitFrom) or
    // destroyed.
    // Returns NULL if this message is *not* a contact-info-containing
    // SA message.
    const ContactUID *getContactUID() const;

    // Add or replace peer to peer UID information
    // This will add a __dest detail element if not existing and set
    // the given uid as that element's uid attribute
    void setPeerDestUid(const std::string *destUid);

    // Add or replace TAK server destination information.
    // Any existing TAK server destination information is always removed.
    // empty vector will result in the message going to "all tak server"
    // recipients.
    // NULL will result in removing any TAK server destination information.
    void setTAKServerRecipients(const std::vector<std::string> *recipients);

    // Add or replace TAK server destination information with
    // a single mission recipient.
    void setTAKServerMissionRecipient(const std::string &mission);
    
    CommoLogger *getLogger() const;

private:
    CoTMessageImpl *internalState;
    CommoLogger *logger;


    std::string endpointAsString(bool alt) const;
    static std::string endpointForType(EndpointType type, 
                                       const std::string &ipStr);

};

}
}
}


#endif
