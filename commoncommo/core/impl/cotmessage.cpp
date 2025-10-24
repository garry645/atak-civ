#define __STDC_LIMIT_MACROS
#include "cotmessage.h"
#include "commotime.h"
#include "internalutils.h"
#include "cryptoutil.h"
#include "missionpackage.h"

#include "protobuf.h"

#include "libxml/parser.h"
#include "libxml/tree.h"

#include <string.h>
#include <string>
#include <sstream>
#include <vector>

using namespace atakmap::commoncommo;
using namespace atakmap::commoncommo::impl;




namespace {

    static const std::string emptyString("");

#if 0
    void getChildElementsByName(std::vector<xmlNode *> *results, xmlNode *node, const xmlChar *name)
    {
        results->clear();
        for (xmlNode *child = node->children; child; child = child->next) {
            if (xmlStrEqual(child->name, name))
                results->push_back(child);
        }
    }
#endif

    void removeAllChildren(xmlNode *node)
    {
        xmlNode *next = node->children;
        while (next) {
            xmlNode *cur = next;
            next = next->next;
            xmlUnlinkNode(cur);
            xmlFreeNode(cur);
        }
    }
    
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

    void setEndpointsImpl(xmlNode *node, const std::string &epStr, const std::string &altEpStr)
    {
        xmlAttr *attr = xmlHasProp(node, (const xmlChar *)"endpoint");
        xmlAttr *altAttr = xmlHasProp(node, (const xmlChar *)"altendpoints");
        if (epStr.empty()) {
            if (attr)
                xmlRemoveProp(attr);
            if (altAttr)
                xmlRemoveProp(altAttr);
            return;
        }
        
        if (attr)
            xmlSetProp(node, (const xmlChar *)"endpoint", (const xmlChar *)epStr.c_str());
        else
            xmlNewProp(node, (const xmlChar *)"endpoint", (const xmlChar *)epStr.c_str());

        if (altEpStr.empty()) {
            if (altAttr)
                xmlRemoveProp(altAttr);
            return;
        }
        
        if (altAttr) {
            xmlSetProp(node, (const xmlChar *)"altendpoints", (const xmlChar *)altEpStr.c_str());
        } else {
            xmlNewProp(node, (const xmlChar *)"altendpoints", (const xmlChar *)altEpStr.c_str());
        }
    }

    std::string checkedGetProp(xmlNode *node, const char *name) COMMO_THROW (std::invalid_argument)
    {
        xmlChar *p = xmlGetProp(node, (const xmlChar *)name);
        if (!p) {
            std::string errmsg("Required property ");
            errmsg += name;
            errmsg += " missing on node ";
            errmsg += (const char *)node->name;
            throw std::invalid_argument(errmsg);
        }
        std::string ret = (char *)p;
        xmlFree(p);
        return ret;
    }
    
    std::string checkedRemoveProp(xmlNode *node, const char *name) COMMO_THROW (std::invalid_argument)
    {
        xmlAttr *attr = xmlHasProp(node, (const xmlChar *)name);
        xmlChar *p = xmlGetProp(node, (const xmlChar *)name);
        if (!p || !attr)
            throw std::invalid_argument("");
        std::string ret = (char *)p;
        xmlFree(p);
        xmlRemoveProp(attr);
        return ret;
    }
    
    const std::string POINT_NOVALUE_STRING = "9999999";
    std::string doubleToStringPointNoVal(double d)
    {
        if (d == COMMO_COT_POINT_NO_VALUE)
            return POINT_NOVALUE_STRING;
        else
            return InternalUtils::doubleToString(d);
    }
    
    uint64_t millisFromCotTime(const std::string &timeString)
                                           COMMO_THROW (std::invalid_argument)
    {
        CommoTime t = CommoTime::fromUTCString(timeString);
        
        return t.getPosixMillis();
    }

    // Indexed by TakControlType enum values
    const char *TAKCONTROL_TYPE_STRINGS[TakControlType::TYPE_NONE] = {
        "t-x-takp-v",
        "t-x-takp-q",
        "t-x-takp-r"
    };
    const char HOW_TAKCONTROL[] = "m-g";
    const float STALE_TIME_TAKCONTROL = 60.0f;

    const char TYPE_FILE_REQ[] = "b-f-t-r";
    const char HOW_FILE_REQ[] = "h-e";
    const float STALE_TIME_FILE_REQ = 10.0f;

    const char TYPE_FILE_ACK[] = "b-f-t-a";
    const char HOW_FILE_ACK[] = "m-g";
    const float STALE_TIME_FILE_ACK = 10.0f;

    const char TYPE_PONG[] = "t-x-c-t-r";
    const char TYPE_PING[] = "t-x-c-t";
    const char HOW_PING[] = "m-g";
    const float STALE_TIME_PING = 10.0f;
    
    const CoTPointData ZERO_POINT(0, 0, 0,
            COMMO_COT_POINT_NO_VALUE, COMMO_COT_POINT_NO_VALUE);
}


namespace atakmap {
namespace commoncommo {
namespace impl
{

struct CoTMessageImpl
{
    xmlDoc *doc;
    xmlNode *eventElement;
    xmlNode *detailsElement;
    xmlNode *contactElement;
    xmlNode *fileShareElement;
    xmlNode *fileShareAckElement;
    xmlNode *ackRequestElement;
    CoTMessageType type;
    xmlChar *uidBacking;
    ContactUID *uid;
    CoTFileTransferRequest *xferReq;

    std::string uidString;
    std::string howString;
    std::string typeString;
    uint64_t timeMillis;
    uint64_t startTimeMillis;
    uint64_t staleTimeMillis;
    double latitude;
    double longitude;
    double hae;
    double ce;
    double le;


    CoTMessageImpl(const std::string &uid,
                                   const std::string &type,
                                   const std::string &how,
                                   float staleSeconds,
                                   const std::string &access,
                                   const std::string &caveats,
                                   const std::string &releasableTo,
                                   const CoTPointData &point) :
                                        doc(NULL), eventElement(NULL),
                                        detailsElement(NULL),
                                        contactElement(NULL),
                                        fileShareElement(NULL),
                                        fileShareAckElement(NULL),
                                        ackRequestElement(NULL),
                                        type(SITUATIONAL_AWARENESS), uid(NULL),
                                        xferReq(NULL),
                                        uidString(),
                                        howString(),
                                        typeString(),
                                        timeMillis(0),
                                        startTimeMillis(0),
                                        staleTimeMillis(0),
                                        latitude(0),
                                        longitude(0),
                                        hae(0),
                                        ce(0),
                                        le(0)
    {
        createInit(uid, type, how, staleSeconds, access, caveats, 
                   releasableTo, point);
    }
    
    
    CoTMessageImpl(const std::string &uid,
                                   const std::string &type,
                                   const std::string &how,
                                   const CommoTime &time,
                                   const CommoTime &startTime,
                                   const CommoTime &staleTime,
                                   const std::string &access,
                                   const std::string &caveats,
                                   const std::string &releasableTo,
                                   const std::string &qos,
                                   const std::string &opex,
                                   const CoTPointData &point,
                                   xmlNode *details) :
                                        doc(NULL), eventElement(NULL),
                                        detailsElement(NULL),
                                        contactElement(NULL),
                                        fileShareElement(NULL),
                                        fileShareAckElement(NULL),
                                        ackRequestElement(NULL),
                                        type(SITUATIONAL_AWARENESS), uid(NULL),
                                        xferReq(NULL),
                                        uidString(),
                                        howString(),
                                        typeString(),
                                        timeMillis(0),
                                        startTimeMillis(0),
                                        staleTimeMillis(0),
                                        latitude(0),
                                        longitude(0),
                                        hae(0),
                                        ce(0),
                                        le(0)
    {
        createInit(uid, type, how, time, startTime, staleTime,
                   access, caveats, releasableTo, point, details);
        
        if (qos.size() > 0)
            xmlNewProp(eventElement, (const xmlChar *)"qos", (const xmlChar *)qos.c_str());
        if (opex.size() > 0)
            xmlNewProp(eventElement, (const xmlChar *)"opex", (const xmlChar *)opex.c_str());
    }
    
    
    CoTMessageImpl(const std::string &uid,
                                   const std::string &type,
                                   const std::string &how,
                                   float staleSeconds,
                                   const CoTPointData &point,
                                   const CoTFileTransferRequest &fileTransferRequest,
                                   const ContactUID *receiveruid,
                                   const bool failed,
                                   const std::string &message) :
                                        doc(NULL), eventElement(NULL),
                                        detailsElement(NULL),
                                        contactElement(NULL),
                                        fileShareElement(NULL),
                                        fileShareAckElement(NULL),
                                        ackRequestElement(NULL),
                                        type(SITUATIONAL_AWARENESS), uid(NULL),
                                        xferReq(NULL),
                                        uidString(),
                                        howString(),
                                        typeString(),
                                        timeMillis(0),
                                        startTimeMillis(0),
                                        staleTimeMillis(0),
                                        latitude(0),
                                        longitude(0),
                                        hae(0),
                                        ce(0),
                                        le(0)
    {
        createInit(uid, type, how, staleSeconds,
                   fileTransferRequest.access,
                   fileTransferRequest.caveats,
                   fileTransferRequest.releasableTo,
                   point);

        std::string sizeStr = InternalUtils::uint64ToString(fileTransferRequest.sizeInBytes);
        std::string receiveruidString((const char *)receiveruid->contactUID,
                                      receiveruid->contactUIDLen);

        fileShareAckElement = xmlNewChild(detailsElement,
                        NULL, (const xmlChar *)"ackresponse", NULL);
        xmlNewProp(fileShareAckElement, (const xmlChar *)"uid",
                (const xmlChar *)fileTransferRequest.ackuid.c_str());
        xmlNewProp(fileShareAckElement, (const xmlChar *)"senderUid",
                (const xmlChar *)receiveruidString.c_str());
        xmlNewProp(fileShareAckElement, (const xmlChar *)"success",
                (const xmlChar *)(!failed ? "true" : "false"));
        xmlNewProp(fileShareAckElement, (const xmlChar *)"tag",
                (const xmlChar *)fileTransferRequest.name.c_str());
        xmlNewProp(fileShareAckElement, (const xmlChar *)"reason",
                (const xmlChar *)message.c_str());
        xmlNewProp(fileShareAckElement, (const xmlChar *)"sha256",
                (const xmlChar *)fileTransferRequest.sha256hash.c_str());
        xmlNewProp(fileShareAckElement, (const xmlChar *)"sizeInBytes",
                (const xmlChar *)sizeStr.c_str());
    }

    CoTMessageImpl(const std::string &uid,
                                   const std::string &type,
                                   const std::string &how,
                                   float staleSeconds,
                                   const CoTPointData &point,
                                   CoTFileTransferRequest fileTransferRequest) :
                                        doc(NULL), eventElement(NULL),
                                        detailsElement(NULL),
                                        contactElement(NULL),
                                        fileShareElement(NULL),
                                        fileShareAckElement(NULL),
                                        ackRequestElement(NULL),
                                        type(SITUATIONAL_AWARENESS), uid(NULL),
                                        xferReq(NULL),
                                        uidString(),
                                        howString(),
                                        typeString(),
                                        timeMillis(0),
                                        startTimeMillis(0),
                                        staleTimeMillis(0),
                                        latitude(0),
                                        longitude(0),
                                        hae(0),
                                        ce(0),
                                        le(0)
    {
        createInit(uid, type, how, staleSeconds,
                   fileTransferRequest.access,
                   fileTransferRequest.caveats,
                   fileTransferRequest.releasableTo,
                   point);

        fileShareElement = xmlNewChild(detailsElement,
                        NULL, (const xmlChar *)"fileshare", NULL);
        xmlNewProp(fileShareElement, (const xmlChar *)"filename",
                (const xmlChar *)fileTransferRequest.senderFilename.c_str());
        xmlNewProp(fileShareElement, (const xmlChar *)"senderUrl",
                (const xmlChar *)fileTransferRequest.senderUrl.c_str());
        std::string sizeStr = InternalUtils::uint64ToString(fileTransferRequest.sizeInBytes);
        xmlNewProp(fileShareElement, (const xmlChar *)"sizeInBytes",
                (const xmlChar *)sizeStr.c_str());
        xmlNewProp(fileShareElement, (const xmlChar *)"sha256",
                (const xmlChar *)fileTransferRequest.sha256hash.c_str());
        std::string senderuidString((const char *)fileTransferRequest.senderuid->contactUID,
                                    fileTransferRequest.senderuid->contactUIDLen);
        xmlNewProp(fileShareElement, (const xmlChar *)"senderUid",
                (const xmlChar *)senderuidString.c_str());
        xmlNewProp(fileShareElement, (const xmlChar *)"senderCallsign",
                (const xmlChar *)fileTransferRequest.senderCallsign.c_str());
        xmlNewProp(fileShareElement, (const xmlChar *)"name",
                (const xmlChar *)fileTransferRequest.name.c_str());
        if (fileTransferRequest.peerHosted)
            xmlNewProp(fileShareElement, (const xmlChar *)"peerHosted",
                (const xmlChar *)"true");
        if (fileTransferRequest.httpsPort != MP_LOCAL_PORT_DISABLE) {
            std::string httpsPortStr = InternalUtils::uint64ToString(
                                            fileTransferRequest.httpsPort);
            xmlNewProp(fileShareElement, (const xmlChar *)"httpsPort",
                (const xmlChar *)httpsPortStr.c_str());
        }

        if (!fileTransferRequest.ackuid.empty()) {
            ackRequestElement = xmlNewChild(detailsElement, NULL, (const xmlChar *)"ackrequest", NULL);
            xmlNewProp(ackRequestElement, (const xmlChar *)"uid",
                    (const xmlChar *)fileTransferRequest.ackuid.c_str());
            xmlNewProp(ackRequestElement, (const xmlChar *)"ackrequested",
                    (const xmlChar *)"true");
            xmlNewProp(ackRequestElement, (const xmlChar *)"tag",
                    (const xmlChar *)fileTransferRequest.name.c_str());
        }

        xferReq = new CoTFileTransferRequest(fileTransferRequest);
    }

    CoTMessageImpl(const uint8_t* data, const size_t len) COMMO_THROW (std::invalid_argument) :
            doc(NULL), eventElement(NULL), detailsElement(NULL),
            contactElement(NULL), fileShareElement(NULL),
            fileShareAckElement(NULL),
            ackRequestElement(NULL),
            type(SITUATIONAL_AWARENESS), uid(NULL), xferReq(NULL),
            uidString(),
            howString(),
            typeString(),
            timeMillis(0),
            startTimeMillis(0),
            staleTimeMillis(0),
            latitude(0),
            longitude(0),
            hae(0),
            ce(0),
            le(0)
    {
        doc = xmlReadMemory((const char *)data, (int)len, "mbuf:", NULL, XML_PARSE_NONET);
        if (!doc)
            throw std::invalid_argument("basic XML structure invalid, parsing failed");

        init();
    }

    CoTMessageImpl(const CoTMessageImpl *src) COMMO_THROW (std::invalid_argument) :
            doc(NULL), eventElement(NULL), detailsElement(NULL),
            contactElement(NULL), fileShareElement(NULL),
            fileShareAckElement(NULL), ackRequestElement(NULL),
            type(SITUATIONAL_AWARENESS), uid(NULL), xferReq(NULL),
            uidString(src->uidString),
            howString(src->howString),
            typeString(src->typeString),
            timeMillis(src->timeMillis),
            startTimeMillis(src->startTimeMillis),
            staleTimeMillis(src->staleTimeMillis),
            latitude(src->latitude),
            longitude(src->longitude),
            hae(src->hae),
            ce(src->ce),
            le(src->le)
    {
        doc = xmlCopyDoc(src->doc, 1);
        if (!doc)
            throw std::invalid_argument("Unable to copy existing document tree");
        init();
    }

    ~CoTMessageImpl()
    {
        if (xferReq)
            delete xferReq;
        if (doc)
            xmlFreeDoc(doc);
        if (uid) {
            xmlFree(uidBacking);
            delete uid;
        }
    }
private:
    COMMO_DISALLOW_COPY(CoTMessageImpl);

    void init() COMMO_THROW (std::invalid_argument) {
        // Try to extract the base CoT elements
        eventElement = xmlDocGetRootElement(doc);
        if (!eventElement || !xmlStrEqual(eventElement->name, (const xmlChar *)"event")) {
            xmlFreeDoc(doc);
            doc = NULL;
            throw std::invalid_argument("Invalid root node for CoT message");
        }

        uidBacking = xmlGetProp(eventElement, (const xmlChar *)"uid");
        if (!uidBacking) {
            xmlFreeDoc(doc);
            doc = NULL;
            throw std::invalid_argument("Missing uid in CoT event");
        }
        size_t contactUIDLen = strlen((const char *)uidBacking);
        uid = new ContactUID(uidBacking, contactUIDLen);
        
        uidString = (const char *)uidBacking;
        try {
            howString = checkedGetProp(eventElement, "how");
            typeString = checkedGetProp(eventElement, "type");
            timeMillis = millisFromCotTime(checkedGetProp(eventElement, "time"));
            startTimeMillis = millisFromCotTime(checkedGetProp(eventElement, "start"));
            staleTimeMillis = millisFromCotTime(checkedGetProp(eventElement, "stale"));

            xmlNode *pointElement = getFirstChildElementByName(eventElement, (const xmlChar *)"point");
            if (!pointElement)
                throw std::invalid_argument("");
            latitude = InternalUtils::doubleFromString(
                    checkedGetProp(pointElement, "lat").c_str());
            longitude = InternalUtils::doubleFromString(
                    checkedGetProp(pointElement, "lon").c_str());
            hae = InternalUtils::doubleFromString(
                    checkedGetProp(pointElement, "hae").c_str());
            ce = InternalUtils::doubleFromString(
                    checkedGetProp(pointElement, "ce").c_str());
            le = InternalUtils::doubleFromString(
                    checkedGetProp(pointElement, "le").c_str());
        } catch (std::invalid_argument &ex) {
            xmlFreeDoc(doc);
            doc = NULL;
            std::string errmsg("Missing or invalid CoT event and/or point attributes (");
            errmsg += ex.what();
            errmsg += ")";
            throw std::invalid_argument(errmsg);
        }
        
        detailsElement = getFirstChildElementByName(eventElement, (const xmlChar *)"detail");
        if (!detailsElement) {
            detailsElement = xmlNewChild(eventElement, 
                                         NULL,
                                         (const xmlChar *)"detail",
                                         NULL);
        }
        
        detailsInit();

    }
    
    void detailsInit()
    {
        // Strip any <_flow-tags_ elements under <detail>
        // These are added in transit by TAK server to control routing.
        // If this exists in the message and matches the server's tag,
        // TAK server will drop the message thinking it was routed in a loop.
        // See WTK-1434
        for (xmlNode *child = detailsElement->children; child; ) {
            xmlNode *curChild = child;
            child = child->next;
            if (xmlStrEqual(curChild->name, (const xmlChar *)"_flow-tags_")) {
                xmlUnlinkNode(curChild);
                xmlFreeNode(curChild);
            }
        }

        // OK if this is NULL
        contactElement = getFirstChildElementByName(detailsElement, (const xmlChar *)"contact");

        // OK if either of these are NULL
        fileShareElement = getFirstChildElementByName(detailsElement, (const xmlChar *)"fileshare");
        fileShareAckElement = getFirstChildElementByName(detailsElement, (const xmlChar *)"ackresponse");

        // Detect the message type; if it has a chat element, it's chat.
        // Else it's some form of SA.
        xmlNode *chatNode = getFirstChildElementByName(detailsElement, (const xmlChar *)"__chat");
        type = chatNode ? CHAT : SITUATIONAL_AWARENESS;

        if (fileShareElement) {
            try {
                std::string sha256 = checkedGetProp(fileShareElement, "sha256");
                std::string name = checkedGetProp(fileShareElement, "name");
                std::string senderFilename = checkedGetProp(fileShareElement, "filename");
                std::string senderUrl= checkedGetProp(fileShareElement, "senderUrl");
                std::string sizeInBytesStr = checkedGetProp(fileShareElement, "sizeInBytes");
                uint64_t sizeInBytes = atoll(sizeInBytesStr.c_str());
                std::string senderUidStr = checkedGetProp(fileShareElement, "senderUid");
                ContactUID senderuid((const uint8_t *)senderUidStr.c_str(),
                                     senderUidStr.length());
                std::string senderCallsign = checkedGetProp(fileShareElement, "senderCallsign");

                bool peerHosted = false;
                xmlChar *xmlProp = xmlGetProp(fileShareElement, (const xmlChar *)"peerHosted");
                if (xmlProp) {
                    std::string peerHostedStr = (char *)xmlProp;
                    xmlFree(xmlProp);
                    peerHosted = peerHostedStr == "true";
                }
                int httpsPort = MP_LOCAL_PORT_DISABLE;
                xmlProp = xmlGetProp(fileShareElement, (const xmlChar *)"httpsPort");
                if (xmlProp) {
                    std::string httpsPortStr = (char *)xmlProp;
                    xmlFree(xmlProp);
                    httpsPort = InternalUtils::intFromString(httpsPortStr.c_str(), 1, 65535);
                }

                // Get security attributes to use in later ack/nack messages
                // We *do not* check these values, just use them as-is
                std::string access, caveats, relTo;
                xmlProp = xmlGetProp(eventElement, (const xmlChar *)"access");
                if (xmlProp) {
                    access = (char *)xmlProp;
                    xmlFree(xmlProp);
                }
                xmlProp = xmlGetProp(eventElement, (const xmlChar *)"caveat");
                if (xmlProp) {
                    caveats = (char *)xmlProp;
                    xmlFree(xmlProp);
                }
                xmlProp = xmlGetProp(eventElement, (const xmlChar *)"releasableTo");
                if (xmlProp) {
                    relTo = (char *)xmlProp;
                    xmlFree(xmlProp);
                }

                std::string ackuid;
                ackRequestElement = getFirstChildElementByName(detailsElement, (const xmlChar *)"ackrequest");
                if (ackRequestElement) {
                    std::string ackReq = checkedGetProp(ackRequestElement, "ackrequested");
                    if (ackReq == "true")
                        ackuid = checkedGetProp(ackRequestElement, "uid");
                }

                xferReq = new CoTFileTransferRequest(access, caveats, relTo,
                        sha256, name,
                        senderFilename, senderUrl, sizeInBytes,
                        senderCallsign, &senderuid, ackuid, peerHosted,
                        httpsPort);
            } catch (std::invalid_argument &) {
            }
        }
    }



    void createInit(const std::string &uid,
                                   const std::string &type,
                                   const std::string &how,
                                   float staleSeconds,
                                   const std::string &access,
                                   const std::string &caveats,
                                   const std::string &releasableTo,
                                   const CoTPointData &point)
    {
        CommoTime nowTime = CommoTime::now();
        CommoTime staleTime = nowTime;
        staleTime += staleSeconds;
        createInit(uid, type, how, nowTime, nowTime, staleTime, 
                   access, caveats, releasableTo, point, NULL);
    }

    void createInit(const std::string &uid,
                                   const std::string &type,
                                   const std::string &how,
                                   const CommoTime &time,
                                   const CommoTime &startTime,
                                   const CommoTime &staleTime,
                                   const std::string &access,
                                   const std::string &caveats,
                                   const std::string &releasableTo,
                                   const CoTPointData &point,
                                   xmlNode *details)
    {
        doc = xmlNewDoc((const xmlChar *)"1.0");
        eventElement = xmlNewNode(NULL, (const xmlChar *)"event");
        xmlDocSetRootElement(doc, eventElement);
        xmlNewProp(eventElement, (const xmlChar *)"version", (const xmlChar *)"2.0");
        xmlNewProp(eventElement, (const xmlChar *)"uid", (const xmlChar *)uid.c_str());
        xmlNewProp(eventElement, (const xmlChar *)"type", (const xmlChar *)type.c_str());

        if (access.size() > 0)
            xmlNewProp(eventElement, (const xmlChar *)"access", (const xmlChar *)access.c_str());
        else
            xmlNewProp(eventElement, (const xmlChar *)"access", (const xmlChar *)"Undefined");
        if (caveats.size() > 0)
            xmlNewProp(eventElement, (const xmlChar *)"caveat", (const xmlChar *)caveats.c_str());
        if (releasableTo.size() > 0)
            xmlNewProp(eventElement, (const xmlChar *)"releasableTo", (const xmlChar *)releasableTo.c_str());

        std::string timeString = time.toUTCString();
        timeMillis = time.getPosixMillis();
        
        std::string startTimeString = startTime.toUTCString();
        startTimeMillis = startTime.getPosixMillis();

        xmlNewProp(eventElement, (const xmlChar *)"time", (const xmlChar *)timeString.c_str());
        xmlNewProp(eventElement, (const xmlChar *)"start", (const xmlChar *)startTimeString.c_str());

        std::string staleTimeString = staleTime.toUTCString();
        staleTimeMillis = staleTime.getPosixMillis();
        xmlNewProp(eventElement, (const xmlChar *)"stale", (const xmlChar *)staleTimeString.c_str());
        xmlNewProp(eventElement, (const xmlChar *)"how", (const xmlChar *)how.c_str());

        xmlNode *pointNode = xmlNewChild(eventElement, NULL, (const xmlChar *)"point", NULL);
        std::string s = InternalUtils::doubleToString(point.lat);
        xmlNewProp(pointNode, (const xmlChar *)"lat", (const xmlChar *)s.c_str());
        s = InternalUtils::doubleToString(point.lon);
        xmlNewProp(pointNode, (const xmlChar *)"lon", (const xmlChar *)s.c_str());
        s = doubleToStringPointNoVal(point.hae);
        xmlNewProp(pointNode, (const xmlChar *)"hae", (const xmlChar *)s.c_str());
        s = doubleToStringPointNoVal(point.ce);
        xmlNewProp(pointNode, (const xmlChar *)"ce", (const xmlChar *)s.c_str());
        s = doubleToStringPointNoVal(point.le);
        xmlNewProp(pointNode, (const xmlChar *)"le", (const xmlChar *)s.c_str());

        uidBacking = xmlGetProp(eventElement, (const xmlChar *)"uid");
        size_t contactUIDLen = strlen((const char *)uidBacking);
        this->uid = new ContactUID(uidBacking, contactUIDLen);
        
        uidString = uid;
        howString = how;
        typeString = type;
        latitude = point.lat;
        longitude = point.lon;
        hae = point.hae;
        ce = point.ce;
        le = point.le;

        if (details == NULL) {
            detailsElement = xmlNewChild(eventElement, NULL, (const xmlChar *)"detail", NULL);
        } else {
            detailsElement = details;
            xmlAddChild(eventElement, details);
        }
        detailsInit();
    }
};

}
}
}

#if 0
bool CoTMessage::staticInit()
{
    xmlInitParser();
    return true;
}

void CoTMessage::staticCleanup()
{
    xmlCleanupParser();
}
#endif

const std::string CoTMessage::STREAMING_ENDPOINT("*:-1:stcp");
const std::string CoTMessage::TCP_USESRC_ENDPOINT("tcpsrcreply");
const std::string CoTMessage::UDP_USESRC_ENDPOINT("udpsrcreply");
const std::string CoTMessage::QUIC_USESRC_ENDPOINT("quicsrcreply");


CoTMessage::CoTMessage(CommoLogger *logger, 
                       const std::string &uid) : internalState(NULL),
                                                 logger(logger)
{
    internalState = new CoTMessageImpl(uid, TYPE_PING, HOW_PING,
                                       STALE_TIME_PING,
                                       emptyString,
                                       emptyString,
                                       emptyString,
                                       ZERO_POINT);
}

CoTMessage::CoTMessage(CommoLogger *logger, 
                       const std::string &uid,
                       int version,
                       const ExtensionIdSet &extensions) : internalState(NULL),
                                      logger(logger)
{
    internalState = new CoTMessageImpl(uid, 
                TAKCONTROL_TYPE_STRINGS[TakControlType::TYPE_REQUEST],
                HOW_TAKCONTROL,
                STALE_TIME_TAKCONTROL,
                emptyString,
                emptyString,
                emptyString,
                ZERO_POINT);
    xmlNode *n = xmlNewChild(internalState->detailsElement, NULL,
                             (const xmlChar *)"TakControl", NULL);
    n = xmlNewChild(n, NULL, (const xmlChar *)"TakRequest", NULL);
    std::string vs = InternalUtils::intToString(version);
    xmlNewProp(n, (const xmlChar *)"version",
               (const xmlChar *)vs.c_str());
    for (ExtensionIdSet::value_type id : extensions) {
        std::string ids = InternalUtils::uint64ToString(id);
        xmlNode *e = xmlNewChild(n, NULL, (const xmlChar *)"DetailExt", NULL);
        xmlNewProp(e, (const xmlChar *)"id", (const xmlChar *)ids.c_str());
    }
}

CoTMessage::CoTMessage(CommoLogger *logger, 
                       const std::string &uid, const CoTPointData &point,
                       const CoTFileTransferRequest &fileTransferRequest) :
                               internalState(NULL),
                               logger(logger)
{
    internalState = new CoTMessageImpl(uid, TYPE_FILE_REQ,
                                       HOW_FILE_REQ, STALE_TIME_FILE_REQ,
                                       point, fileTransferRequest);

}

CoTMessage::CoTMessage(CommoLogger *logger, 
                       const std::string &uid, const CoTPointData &point,
                       const CoTFileTransferRequest &fileTransferRequest,
                       const ContactUID *receiveruid,
                       const bool failed,
                       const std::string &message) :
                   internalState(NULL),
                   logger(logger)
{
    internalState = new CoTMessageImpl(uid, TYPE_FILE_ACK, HOW_FILE_ACK,
                                       STALE_TIME_FILE_ACK, point,
                                       fileTransferRequest, receiveruid,
                                       failed, message);
}

CoTMessage::CoTMessage(CommoLogger *logger, const uint8_t *data, size_t len)
            COMMO_THROW (std::invalid_argument) : internalState(NULL), logger(logger)
{
    reinitFrom(data, len);
}

CoTMessage::CoTMessage(CommoLogger *logger, const protobuf::v1::CotEvent &event,
                       ExtensionRegistry *extensions)
            COMMO_THROW (std::invalid_argument) : internalState(NULL), logger(logger)
{
    reinitFromProtobuf(event, extensions);
}

CoTMessage::CoTMessage(const CoTMessage& src) COMMO_THROW (std::invalid_argument) : 
            internalState(new CoTMessageImpl(src.internalState)),
            logger(src.logger)
{

}

CoTMessage::~CoTMessage()
{
    if (internalState)
        delete internalState;
}

void CoTMessage::reinitFrom(
        const uint8_t* data, const size_t len) COMMO_THROW (std::invalid_argument)
{
    CoTMessageImpl *newImpl = new CoTMessageImpl(data, len);

    // Wipe any old impl
    if (internalState)
        delete internalState;

    internalState = newImpl;
}

void CoTMessage::reinitFromProtobuf(
        const protobuf::v1::CotEvent &ev,
        ExtensionRegistry *extensions) COMMO_THROW (std::invalid_argument)
{
    const std::string uid = ev.uid();
    const std::string type = ev.type();
    const std::string how = ev.how();
    uint64_t timeMillis = ev.sendtime();
    uint64_t startTimeMillis = ev.starttime();
    uint64_t staleTimeMillis = ev.staletime();
    const std::string access = ev.access();
    const std::string caveat = ev.caveat();
    const std::string releasableTo = ev.releasableto();
    const std::string qos = ev.qos();
    const std::string opex = ev.opex();
    double lat = ev.lat();
    double lon = ev.lon();
    double hae = ev.hae();
    double ce = ev.ce();
    double le = ev.le();
    CoTPointData pt(lat, lon, hae, ce, le);
    
    xmlNode *detailsNode = NULL;
    if (ev.has_detail()) {
        const protobuf::v1::Detail detail = ev.detail();
        
        std::string xmlDetail = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><detail>" +
            detail.xmldetail() + "</detail>";
        size_t detailSize = xmlDetail.size();
        if (detailSize > INT_MAX)
            throw std::invalid_argument("Details string too long");
        int detailSizeInt = (int)detailSize;

        xmlDoc *detailsDoc = xmlReadMemory(xmlDetail.data(), 
                                   detailSizeInt, 
                                   "mbuf:", NULL, XML_PARSE_NONET);
        if (!detailsDoc)
            throw std::invalid_argument("Unable to parse details string");
        
        xmlNode *details = xmlDocGetRootElement(detailsDoc);
        if (!details) {
            xmlFreeDoc(detailsDoc);
            throw std::invalid_argument("Invalid detail xml string");
        }
        
        // Tried transferring the node as
        // xmlUnlinkNode(details);
        // But the node was still destroyed in the xmlFreeDoc call
        // (even if we did that after createInit()'s association of
        // the node to the new doc).  So copy instead of re-parent
        detailsNode = xmlCopyNode(details, 1);
        xmlFreeDoc(detailsDoc);


        // Restore from extensions, if any
        int numExt = detail.extensiondetails_size();
        for (int i = 0; i < numExt; i++) {
            thread::ReadLockPtr extLock;
            const protobuf::v1::Detail_ExtensionEncodedDetail &edetail =
                    detail.extensiondetails(i);
            CoTDetailExtender *extender = NULL;
            std::string name;
            const char *exml = NULL;
            xmlDoc *edoc = NULL;

            try {
                if (!extensions->getById(extLock, name, &extender, edetail.extensionid())) {
                    std::stringstream ss;
                    ss << "No extension handler for extension " << edetail.extensionid() << " in received message" ;
                    throw std::invalid_argument(ss.str());
                }
                const std::string &edata = edetail.data();
                CommoResult erc = extender->decode(&exml, (const uint8_t *)edata.data(), edata.length());
                if (erc != COMMO_SUCCESS) {
                    std::stringstream ss;
                    ss << "Extension handler for extension " << edetail.extensionid() << " failed to decode extension data";
                    throw std::invalid_argument(ss.str());
                }
                
                edoc = xmlReadMemory(exml, (int)strlen(exml), 
                                     "mbuf:", NULL, XML_PARSE_NONET);
                if (!edoc) {
                    std::stringstream ss;
                    ss << "Extension decoded result for extension " << edetail.extensionid() << " was not valid xml";
                    throw std::invalid_argument(ss.str());
                }
                
                xmlNode *edocRoot = xmlDocGetRootElement(edoc);
                if (!edocRoot || !xmlStrEqual(edocRoot->name, (const xmlChar *)name.c_str())) {
                    std::stringstream ss;
                    ss << "Extension decoded result for extension " << edetail.extensionid() << " produced xml with document root element name not matching its registration";
                    throw std::invalid_argument(ss.str());
                }
                    
                xmlNode *extRoot = xmlCopyNode(edocRoot, 1);
                if (!extRoot) {
                    std::stringstream ss;
                    ss << "Extension decoding encountered internal error";
                    throw std::invalid_argument(ss.str());
                }

                xmlAddChild(detailsNode, extRoot);

            } catch (std::invalid_argument &ex) {
                // Extension decode failure is not fatal
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                    "Extension decoding error (%s) - skipping\n", ex.what());

            }
            if (edoc)
                xmlFreeDoc(edoc);
            if (exml)
                extender->releaseDecodeResult(exml);
        }
        

        if (detail.has_contact() && !getFirstChildElementByName(detailsNode, (const xmlChar *)"contact")) {
            const protobuf::v1::Contact &c = detail.contact();
            xmlNode *n = xmlNewChild(detailsNode, NULL, (const xmlChar *)"contact", NULL);
            if (c.endpoint().length() > 0)
                xmlNewProp(n, (const xmlChar *)"endpoint", (const xmlChar *)c.endpoint().c_str());
            if (c.altendpoints().length() > 0)
                xmlNewProp(n, (const xmlChar *)"altendpoints", (const xmlChar *)c.altendpoints().c_str());
            xmlNewProp(n, (const xmlChar *)"callsign", (const xmlChar *)c.callsign().c_str());
        }
        if (detail.has_group() && !getFirstChildElementByName(detailsNode, (const xmlChar *)"__group")) {
            const protobuf::v1::Group &g = detail.group();
            xmlNode *n = xmlNewChild(detailsNode, NULL, (const xmlChar *)"__group", NULL);
            xmlNewProp(n, (const xmlChar *)"name", (const xmlChar *)g.name().c_str());
            xmlNewProp(n, (const xmlChar *)"role", (const xmlChar *)g.role().c_str());
        }
        if (detail.has_precisionlocation() && !getFirstChildElementByName(detailsNode, (const xmlChar *)"precisionlocation")) {
            const protobuf::v1::PrecisionLocation &pl = detail.precisionlocation();
            xmlNode *n = xmlNewChild(detailsNode, NULL, (const xmlChar *)"precisionlocation", NULL);
            xmlNewProp(n, (const xmlChar *)"geopointsrc", (const xmlChar *)pl.geopointsrc().c_str());
            xmlNewProp(n, (const xmlChar *)"altsrc", (const xmlChar *)pl.altsrc().c_str());
        }
        if (detail.has_status() && !getFirstChildElementByName(detailsNode, (const xmlChar *)"status")) {
            const protobuf::v1::Status &s = detail.status();
            xmlNode *n = xmlNewChild(detailsNode, NULL, (const xmlChar *)"status", NULL);
            ::google::protobuf::uint32 battery = s.battery();
            if (battery > (unsigned int)INT_MAX)
                battery = INT_MAX;
            std::string bstr = InternalUtils::intToString((int)battery);
            xmlNewProp(n, (const xmlChar *)"battery", (const xmlChar *)bstr.c_str());
        }
        if (detail.has_takv() && !getFirstChildElementByName(detailsNode, (const xmlChar *)"takv")) {
            const protobuf::v1::Takv &tv = detail.takv();
            xmlNode *n = xmlNewChild(detailsNode, NULL, (const xmlChar *)"takv", NULL);
            xmlNewProp(n, (const xmlChar *)"device", (const xmlChar *)tv.device().c_str());
            xmlNewProp(n, (const xmlChar *)"platform", (const xmlChar *)tv.platform().c_str());
            xmlNewProp(n, (const xmlChar *)"os", (const xmlChar *)tv.os().c_str());
            xmlNewProp(n, (const xmlChar *)"version", (const xmlChar *)tv.version().c_str());
        }
        if (detail.has_track() && !getFirstChildElementByName(detailsNode, (const xmlChar *)"track")) {
            const protobuf::v1::Track &t = detail.track();
            xmlNode *n = xmlNewChild(detailsNode, NULL, (const xmlChar *)"track", NULL);
            double s = t.speed();
            double c = t.course();
            std::string ss = InternalUtils::doubleToString(s);
            std::string cs = InternalUtils::doubleToString(c);
            xmlNewProp(n, (const xmlChar *)"speed", (const xmlChar *)ss.c_str());
            xmlNewProp(n, (const xmlChar *)"course", (const xmlChar *)cs.c_str());
        }
    }
    
    CoTMessageImpl *newImpl = new CoTMessageImpl(uid,
                                   type,
                                   how,
                                   CommoTime::fromPosixMillis(timeMillis),
                                   CommoTime::fromPosixMillis(startTimeMillis),
                                   CommoTime::fromPosixMillis(staleTimeMillis),
                                   access,
                                   caveat,
                                   releasableTo,
                                   qos,
                                   opex,
                                   pt,
                                   detailsNode);
    
    // Wipe any old impl
    if (internalState)
        delete internalState;

    internalState = newImpl;
}

size_t CoTMessage::serialize(uint8_t **buf, bool prettyFormat) const COMMO_THROW (std::invalid_argument)
{
    xmlChar *outPtr = NULL;
    int outSize;
    xmlDocDumpFormatMemory(internalState->doc, &outPtr, &outSize, prettyFormat ? 1 : 0);
    if (outPtr == NULL)
        throw std::invalid_argument("Unknown error serializing CoTMessage");
    if (!prettyFormat && outSize > 0 && outPtr[outSize - 1] == '\n')
        // Remove trailing newline - this is needed by at least streaming to TAK server
        outSize--;
    uint8_t *p = new uint8_t[outSize + 1];
    memcpy(p, outPtr, outSize);
    p[outSize] = '\0';
    xmlFree(outPtr);
    *buf = p;
    return outSize;
}

void CoTMessage::serializeAsProtobuf(
        protobuf::v1::CotEvent *ev,
        ExtensionRegistry *extensions,
        bool useAnyExtension,
        const ExtensionIdSet &extensionIds)
            const COMMO_THROW (std::invalid_argument)
{
    xmlDoc *doc = xmlCopyDoc(internalState->doc, 1);
    
    try {
        xmlNode *eventElement = xmlDocGetRootElement(doc);
        xmlNode *detailsElement = getFirstChildElementByName(eventElement, (const xmlChar *)"detail");
        
        ev->set_type(checkedGetProp(eventElement, "type"));
        
        try {
            // Optional attribute
            ev->set_access(checkedGetProp(eventElement, "access"));
        } catch (std::invalid_argument &) {
        }
        try {
            // Optional attribute
            ev->set_caveat(checkedGetProp(eventElement, "caveat"));
        } catch (std::invalid_argument &) {
        }
        try {
            // Optional attribute
            ev->set_releasableto(checkedGetProp(eventElement, "releasableTo"));
        } catch (std::invalid_argument &) {
        }
        try {
            // Optional attribute
            ev->set_qos(checkedGetProp(eventElement, "qos"));
        } catch (std::invalid_argument &) {
        }
        try {
            // Optional attribute
            ev->set_opex(checkedGetProp(eventElement, "opex"));
        } catch (std::invalid_argument &) {
        }


        ev->set_uid(checkedGetProp(eventElement, "uid"));
        ev->set_sendtime(internalState->timeMillis);
        ev->set_starttime(internalState->startTimeMillis);
        ev->set_staletime(internalState->staleTimeMillis);
        ev->set_how(checkedGetProp(eventElement, "how"));
        ev->set_lat(internalState->latitude);
        ev->set_lon(internalState->longitude);
        ev->set_hae(internalState->hae);
        ev->set_ce(internalState->ce);
        ev->set_le(internalState->le);

        if (detailsElement) {
            // Reparent doc at details node
            xmlUnlinkNode(detailsElement);
            xmlReplaceNode(eventElement, detailsElement);
            xmlFreeNode(eventElement);
            
            protobuf::v1::Detail *detail = ev->mutable_detail();
            bool hasSomeDetail = false;
            
            xmlNode *detailsCopy = xmlCopyNode(detailsElement, 1);
            
            xmlNode *n = getFirstChildElementByName(detailsCopy, (const xmlChar *)"contact");
            if (n) {
                protobuf::v1::Contact *c = detail->mutable_contact();
                try {
                    xmlAttr *epa = xmlHasProp(n, (const xmlChar *)"endpoint");
                    xmlChar *epp = xmlGetProp(n, (const xmlChar *)"endpoint");
                    if (epp && epa) {
                        xmlRemoveProp(epa);
                        c->set_endpoint((const char *)epp);
                    }
                    if (epp)
                        xmlFree(epp);

                    xmlAttr *aepa = xmlHasProp(n, (const xmlChar *)"altendpoints");
                    xmlChar *aepp = xmlGetProp(n, (const xmlChar *)"altendpoints");
                    if (aepp && aepa) {
                        xmlRemoveProp(aepa);
                        c->set_altendpoints((const char *)aepp);
                    }
                    if (aepp)
                        xmlFree(aepp);

                    c->set_callsign(checkedRemoveProp(n, "callsign").c_str());
                    if (n->xmlChildrenNode || n->properties)
                        throw std::invalid_argument("Unmapped attributes remain in XML after mapping contact to protobuf");
                    xmlUnlinkNode(n);
                    xmlFreeNode(n);
                    n = getFirstChildElementByName(detailsCopy, (const xmlChar *)"contact");
                    if (n)
                        throw std::invalid_argument("Multiple contact elements - not changing to protobuf");
                    n = getFirstChildElementByName(detailsElement,  (const xmlChar *)"contact");
                    xmlUnlinkNode(n);
                    xmlFreeNode(n);
                    hasSomeDetail = true;
                } catch (std::invalid_argument &ex) {
                    InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "Problem converting contact to protobuf: %s\n", ex.what());
                    detail->clear_contact();
                }
            }

            n = getFirstChildElementByName(detailsCopy, (const xmlChar *)"__group");
            if (n) {
                protobuf::v1::Group *g = detail->mutable_group();
                try {
                    g->set_name(checkedRemoveProp(n, "name").c_str());
                    g->set_role(checkedRemoveProp(n, "role").c_str());
                    if (n->xmlChildrenNode || n->properties)
                        throw std::invalid_argument("Unmapped children remain on __group element - not mapping to protobuf");
                    xmlUnlinkNode(n);
                    xmlFreeNode(n);
                    n = getFirstChildElementByName(detailsCopy, (const xmlChar *)"__group");
                    if (n)
                        throw std::invalid_argument("Multiple group elements - not changing to protobuf");
                    n = getFirstChildElementByName(detailsElement,  (const xmlChar *)"__group");
                    xmlUnlinkNode(n);
                    xmlFreeNode(n);
                    hasSomeDetail = true;
                } catch (std::invalid_argument &ex) {
                    InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "Problem converting group to protobuf: %s\n", ex.what());
                    detail->clear_group();
                }
            }

            n = getFirstChildElementByName(detailsCopy, (const xmlChar *)"precisionlocation");
            if (n) {
                protobuf::v1::PrecisionLocation *p = detail->mutable_precisionlocation();
                try {
                    p->set_geopointsrc(checkedRemoveProp(n, "geopointsrc").c_str());
                    p->set_altsrc(checkedRemoveProp(n, "altsrc").c_str());
                    if (n->xmlChildrenNode || n->properties)
                        throw std::invalid_argument("Unmapped children remain on precisionlocation element - not mapping to protobuf");
                    xmlUnlinkNode(n);
                    xmlFreeNode(n);
                    n = getFirstChildElementByName(detailsCopy, (const xmlChar *)"precisionlocation");
                    if (n)
                        throw std::invalid_argument("Multiple precisionlocation elements - not changing to protobuf");
                    n = getFirstChildElementByName(detailsElement,  (const xmlChar *)"precisionlocation");
                    xmlUnlinkNode(n);
                    xmlFreeNode(n);
                    hasSomeDetail = true;
                } catch (std::invalid_argument &ex) {
                    InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "Problem converting precisionloc to protobuf: %s\n", ex.what());
                    detail->clear_precisionlocation();
                }
            }

            n = getFirstChildElementByName(detailsCopy, (const xmlChar *)"status");
            if (n) {
                protobuf::v1::Status *s = detail->mutable_status();
                try {
                    std::string bs = checkedRemoveProp(n, "battery");
                    int b = InternalUtils::intFromString(bs.c_str(), 0, INT_MAX);
                    s->set_battery((unsigned)b);
                    if (n->xmlChildrenNode || n->properties)
                        throw std::invalid_argument("Unmapped children remain on status element - not mapping to protobuf");
                    xmlUnlinkNode(n);
                    xmlFreeNode(n);
                    n = getFirstChildElementByName(detailsCopy, (const xmlChar *)"status");
                    if (n)
                        throw std::invalid_argument("Multiple status elements - not changing to protobuf");
                    n = getFirstChildElementByName(detailsElement,  (const xmlChar *)"status");
                    xmlUnlinkNode(n);
                    xmlFreeNode(n);
                    hasSomeDetail = true;
                } catch (std::invalid_argument &ex) {
                    InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "Problem converting status to protobuf: %s\n", ex.what());
                    detail->clear_status();
                }
            }

            n = getFirstChildElementByName(detailsCopy, (const xmlChar *)"takv");
            if (n) {
                protobuf::v1::Takv *t = detail->mutable_takv();
                try {
                    t->set_device(checkedRemoveProp(n, "device").c_str());
                    t->set_platform(checkedRemoveProp(n, "platform").c_str());
                    t->set_os(checkedRemoveProp(n, "os").c_str());
                    t->set_version(checkedRemoveProp(n, "version").c_str());
                    if (n->xmlChildrenNode || n->properties)
                        throw std::invalid_argument("Unmapped children remain on takv element - not mapping to protobuf");
                    xmlUnlinkNode(n);
                    xmlFreeNode(n);
                    n = getFirstChildElementByName(detailsCopy, (const xmlChar *)"takv");
                    if (n)
                        throw std::invalid_argument("Multiple takv elements - not changing to protobuf");
                    n = getFirstChildElementByName(detailsElement,  (const xmlChar *)"takv");
                    xmlUnlinkNode(n);
                    xmlFreeNode(n);
                    hasSomeDetail = true;
                } catch (std::invalid_argument &ex) {
                    InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "Problem converting takv to protobuf: %s", ex.what());
                    detail->clear_takv();
                }
            }

            n = getFirstChildElementByName(detailsCopy, (const xmlChar *)"track");
            if (n) {
                protobuf::v1::Track *t = detail->mutable_track();
                try {
                    std::string ss = checkedRemoveProp(n, "speed");
                    std::string cs = checkedRemoveProp(n, "course");
                    double s = InternalUtils::doubleFromString(ss.c_str());
                    double c = InternalUtils::doubleFromString(cs.c_str());
                    t->set_speed(s);
                    t->set_course(c);
                    if (n->xmlChildrenNode || n->properties)
                        throw std::invalid_argument("Unmapped children remain on track element - not mapping to protobuf");
                    xmlUnlinkNode(n);
                    xmlFreeNode(n);
                    n = getFirstChildElementByName(detailsCopy, (const xmlChar *)"track");
                    if (n)
                        throw std::invalid_argument("Multiple track elements - not changing to protobuf");
                    n = getFirstChildElementByName(detailsElement,  (const xmlChar *)"track");
                    xmlUnlinkNode(n);
                    xmlFreeNode(n);
                    hasSomeDetail = true;
                } catch (std::invalid_argument &ex) {
                    InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "Problem converting track to protobuf: %s", ex.what());
                    detail->clear_track();
                }
            }

            xmlFreeNode(detailsCopy);
            
            


            // Now serialize anything that is left.
            // Consult extensions as we go through the leftovers; if extension
            // is available and enabled, leave that off the leftovers. 
            // Note that extensions cannot be registered
            // for the core protobuf mapping elements (those handled above)
            xmlBufferPtr buffer = xmlBufferCreate();
            int outSize = 0;
            thread::ReadLockPtr extLock;
            for (xmlNode *child = detailsElement->children; child; child = child->next) {
                unsigned int id;
                CoTDetailExtender *ext;
                //InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                //    "looking for extension for name %s", (const char *)child->name);
                bool found = extensions->getByName(extLock, &id, &ext, (const char *)child->name);
                if (found && (useAnyExtension || extensionIds.find(id) != extensionIds.end())) {
                    static const xmlChar NULL_CHAR = '\0';
                    //InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                    //        "extension found for name %s with id %d", (const char *)child->name, (int)id);
                    xmlBufferPtr edocBuf = xmlBufferCreate();
                    xmlBufferWriteChar(edocBuf, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    xmlNodeDump(edocBuf, doc, child, 0, 0);
                    xmlBufferAdd(edocBuf, &NULL_CHAR, 1);
                    uint8_t *extData;
                    size_t extSize;
                    CommoResult erc = ext->encode(&extData, &extSize, (const char *)xmlBufferContent(edocBuf));
                    xmlBufferFree(edocBuf);
                    if (erc == COMMO_SUCCESS) {
                        protobuf::v1::Detail_ExtensionEncodedDetail *edetail =
                                detail->add_extensiondetails();
                        edetail->set_data(extData, extSize);
                        edetail->set_extensionid(id);
                        ext->releaseEncodeResult(extData);
                        // converted; skip this one
                        continue;
                    }
                } else {
                    //InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                    //    "Extension for name %s %s", (const char *)child->name, found ? "found but not enabled for send method" : "not found");
                }
                int ns = xmlNodeDump(buffer, doc, child, 0, 0);
                if (ns < 0) {
                    xmlBufferFree(buffer);
                    throw std::invalid_argument("Unknown error serializing details");
                }
                outSize += ns;
            }
            extLock.reset();
            
            if (outSize) {
                const xmlChar *outPtr = xmlBufferContent(buffer);
                detail->set_xmldetail((const char *)outPtr, outSize);
                hasSomeDetail = true;
            }
            xmlBufferFree(buffer);
            
            if (!hasSomeDetail)
                ev->clear_detail();
        }
                
    } catch (...) {
        InternalUtils::logprintf(logger, CommoLogger::LEVEL_ERROR, 
                "Severe error converting cot to protobuf");
        throw std::invalid_argument("Error converting cot to protobuf");
    }

    xmlFreeDoc(doc);
}

std::string CoTMessage::getEventUid() const
{
    return internalState->uidString;
}

CoTMessageType CoTMessage::getType() const
{
    return internalState->type;
}

TakControlType CoTMessage::getTakControlType() const
{
    TakControlType ret = TakControlType::TYPE_NONE;
    xmlChar *p = xmlGetProp(internalState->eventElement, (const xmlChar *)"type");
    if (p) {
        for (int i = 0; i < TakControlType::TYPE_NONE; ++i) {
            if (strcmp((const char *)p, TAKCONTROL_TYPE_STRINGS[i]) == 0) {
                ret = (TakControlType)i;
                break;
            }
        }
        xmlFree(p);
    }
    return ret;
}

void CoTMessage::getTakControlSupportedVersions(
        std::set<int> *versions, 
        ExtensionIdSet *extensions,
        bool *allExtensionsSupported) const
{
    versions->clear();
    extensions->clear();
    *allExtensionsSupported = false;
    if (internalState->detailsElement) {
        xmlNode *c = getFirstChildElementByName(internalState->detailsElement, (const xmlChar *)"TakControl");
        if (c) {
            for (xmlNode *child = c->children; child; child = child->next) {
                if (xmlStrEqual(child->name, (const xmlChar *)"TakProtocolSupport")) {
                    try {
                        std::string vs = checkedGetProp(child, "version");
                        int v = InternalUtils::intFromString(vs.c_str());
                        versions->insert(v);
                    } catch (std::invalid_argument &) {
                        InternalUtils::logprintf(logger,
                            CommoLogger::LEVEL_WARNING,
                            "Version tag in TakProtocolSupport message is missing or has invalid value");
                    }

                    for (xmlNode *supChild = child->children; supChild; supChild = supChild->next) {
                        if (xmlStrEqual(supChild->name, (const xmlChar *)"DetailExt")) {
                            try {
                                xmlChar *supportsAllVal = xmlGetProp(supChild, (const xmlChar *)"supportsAll");
                                if (supportsAllVal) {
                                    if (xmlStrEqual(supportsAllVal, (const xmlChar *)"true")) {
                                        xmlFree(supportsAllVal);
                                        *allExtensionsSupported = true;
                                        extensions->clear();
                                        break;
                                    }
                                    xmlFree(supportsAllVal);
                                }
                                
                                xmlChar *idVal = xmlGetProp(supChild, (const xmlChar *)"id");
                                if (idVal) {
                                    std::string ids = (char *)idVal;
                                    xmlFree(idVal);
                                    
                                    unsigned int eid = InternalUtils::uintFromString(ids.c_str());
                                    extensions->insert(eid);
                                }

                            } catch (std::invalid_argument &) {
                                InternalUtils::logprintf(logger,
                                    CommoLogger::LEVEL_WARNING,
                                    "Extension tag in TakProtocolSupport message is missing or has invalid value; ignoring");
                            }
                        }
                    }
                }
            }
        }
    }
}

bool CoTMessage::getTakControlResponseStatus() const
{
    bool ret = false;
    if (internalState->detailsElement) {
        xmlNode *c = getFirstChildElementByName(internalState->detailsElement,
                                                (const xmlChar *)"TakControl");
        if (c) {
            xmlNode *r = getFirstChildElementByName(c,
                                              (const xmlChar *)"TakResponse");
            if (r) {
                try {
                    std::string s = checkedGetProp(r, "status");
                    if (s == "true")
                        ret = true;
                } catch (std::invalid_argument &) {
                    InternalUtils::logprintf(logger, CommoLogger::LEVEL_WARNING,
                        "Status tag in TakResponse message is missing");
                }
            }
        }
    }
    return ret;
}


bool CoTMessage::isPong() const
{
    bool ret = false;
    xmlChar *p = xmlGetProp(internalState->eventElement, (const xmlChar *)"type");
    if (p) {
        ret = strcmp((const char *)p, TYPE_PONG) == 0;
        xmlFree(p);
    }
    return ret;
}

const CoTFileTransferRequest *CoTMessage::getFileTransferRequest() const
{
    return internalState->xferReq;
}

std::string CoTMessage::getFileTransferAckSenderUid() const
{
    if (!internalState->fileShareAckElement)
        return std::string("");

    xmlChar *p = xmlGetProp(internalState->fileShareAckElement, (const xmlChar *)"senderUid");
    if (!p)
        return std::string("");
    std::string ret = (char *)p;
    xmlFree(p);
    return ret;
}

std::string CoTMessage::getFileTransferAckUid() const
{
    if (!internalState->fileShareAckElement)
        return std::string("");

    xmlChar *p = xmlGetProp(internalState->fileShareAckElement, (const xmlChar *)"uid");
    if (!p)
        return std::string("");
    std::string ret = (char *)p;
    xmlFree(p);
    return ret;
}

uint64_t CoTMessage::getFileTransferAckSize() const
{
    if (!internalState->fileShareAckElement)
        return 0;

    xmlChar *p = xmlGetProp(internalState->fileShareAckElement, (const xmlChar *)"sizeInBytes");
    if (!p)
        return 0;
    uint64_t ret = atoll((char *)p);
    xmlFree(p);
    return ret;
}

bool CoTMessage::getFileTransferSucceeded() const
{
    if (!internalState->fileShareAckElement)
        return false;

    xmlChar *p = xmlGetProp(internalState->fileShareAckElement, (const xmlChar *)"success");
    bool yes = p && strcmp((char *)p, "true") == 0;
    if (p)
        xmlFree(p);
    return yes;
}


std::string CoTMessage::getFileTransferReason() const
{
    if (!internalState->fileShareAckElement)
        return std::string("");

    xmlChar *p = xmlGetProp(internalState->fileShareAckElement, (const xmlChar *)"reason");
    if (!p)
        return std::string("");
    std::string ret = (char *)p;
    xmlFree(p);
    return ret;
}

std::string CoTMessage::endpointAsString(bool alt) const
{
    std::string s("");
    if (internalState->contactElement == NULL)
        return s;
    xmlChar *ep = xmlGetProp(internalState->contactElement,
                             (const xmlChar *)(alt ? "altendpoints" : "endpoint"));
    if (!ep)
        return s;

    s = (char *)ep;
    xmlFree(ep);

    return s;
}

size_t CoTMessage::getEndpoints(EndpointList *list) const
{
    size_t n = 0;
    std::string primaryEP = endpointAsString(false);
    try {
        EndpointInfo primaryEpInfo(primaryEP);
        n++;
        list->push_back(std::move(primaryEpInfo));
    } catch (std::invalid_argument &) {
    }
    std::string altEPs = endpointAsString(true);
    std::string::size_type s = 0;
    while (s < altEPs.size()) {
        std::string::size_type e = altEPs.find(' ', s);

        if (s != e) {
            std::string::size_type ns;
            if (e == std::string::npos) {
                // do the remainder (npos for substr count is remainder)
                // and quit at next iter
                // (npos by definition must be >= altEPs.size())
                ns = e;
            } else {
                ns = e + 1;
                e = e - s;
            }
            std::string aep = altEPs.substr(s, e);
            try {
                EndpointInfo ep(aep);
                n++;
                list->push_back(std::move(ep));
            } catch (std::invalid_argument &) {
            }
            s = ns;
        } else {
            // we started looking at a space (multiple spaces in a row)
            // so just skip past it
            s++;
        }
    }

    return n;
}

CoTMessage::EndpointInfo::EndpointInfo(const std::string &epString)
                                COMMO_THROW(std::invalid_argument) :
    type(ENDPOINT_NONE),
    host(epString),
    port(-1)
{
    const std::string &s = epString;

    size_t cPos = s.find(":");
    if (cPos == std::string::npos)
        throw std::invalid_argument("Unparsable endpoint - no host portion");

    host = s.substr(0, cPos);

    size_t c2Pos = s.find(":", cPos + 1);
    if (c2Pos == std::string::npos)
        throw std::invalid_argument("Unparsable endpoint - no port portion");
    std::string pStr = s.substr(cPos + 1, c2Pos - cPos - 1);
    try {
        port = InternalUtils::intFromString(pStr.c_str());
    } catch (std::invalid_argument &) {
        // keep -1 default for invalid value
    }

    std::string tStr = s.substr(c2Pos + 1);
    if (tStr == "udp")
        type = ENDPOINT_UDP;
    else if (tStr == "quic")
        type = ENDPOINT_QUIC;
    else if (tStr == "tcp")
        type = ENDPOINT_TCP;
    else if (tStr == "stcp")
        type = ENDPOINT_STREAMING;
    else if (tStr == "srctcp")
        type = ENDPOINT_TCP_USESRC;
    else if (tStr == "srcudp")
        type = ENDPOINT_UDP_USESRC;
    else
        throw std::invalid_argument("Invalid endpoint type");
}

CoTMessage::EndpointInfo::EndpointInfo(const char *host, int port,
                                       EndpointType type) 
                                           COMMO_THROW(std::invalid_argument) :
            type(type), host(host), port(port)
{
    if (type == ENDPOINT_NONE)
        throw std::invalid_argument("Endpoint type none not allowed");
}


std::string CoTMessage::endpointForType(EndpointType type, 
                                        const std::string &ipStr)
{
    std::string epStr;
    std::string protoStr;
    std::string portStr;
    
    switch (type) {
    case ENDPOINT_UDP_USESRC:
        epStr = UDP_USESRC_ENDPOINT;
        portStr = "6969";
        protoStr = "srcudp";
        break;
    case ENDPOINT_TCP_USESRC:
        epStr = TCP_USESRC_ENDPOINT;
        portStr = "4242";
        protoStr = "srctcp";
        break;
    case ENDPOINT_QUIC_USESRC:
        epStr = QUIC_USESRC_ENDPOINT;
        portStr = "4243";
        protoStr = "srcquic";
        break;
    case ENDPOINT_UDP:
        portStr = "6969";
        protoStr = "udp";
        epStr = ipStr;
        break;
    case ENDPOINT_TCP:
        portStr = "4242";
        protoStr = "tcp";
        epStr = ipStr;
        break;
    case ENDPOINT_QUIC:
        portStr = "4243";
        protoStr = "quic";
        epStr = ipStr;
        break;
    case ENDPOINT_STREAMING:
        epStr = STREAMING_ENDPOINT;
        break;
    case ENDPOINT_NONE:
        break;
    }
    
    if (epStr.length() && epStr != STREAMING_ENDPOINT) {
        epStr += ":";
        epStr += portStr;
        epStr += ":";
        epStr += protoStr;
    }
    return epStr;
}


void CoTMessage::setEndpoints(EndpointType type, const std::string &s,
                              const std::vector<EndpointType> *altTypes)
{
    if (!internalState->contactElement)
        return;

    std::string epStr = endpointForType(type, s);
    std::string altEpStr;
    // Only synth alt eps if main is valid
    if (altTypes && !epStr.empty()) {
        bool first = true;
        for (std::vector<EndpointType>::const_iterator iter = altTypes->begin(); iter != altTypes->end(); iter++) {
            std::string alt = endpointForType(*iter, s);
            if (!alt.empty()) {
                if (!first)
                    altEpStr.append(" ");
                altEpStr.append(alt);
                first = false;
            }
        }
    }

    if (internalState->contactElement && 
            xmlHasProp(internalState->contactElement, (const xmlChar *)"endpoint"))
        setEndpointsImpl(internalState->contactElement, epStr, altEpStr);
    if (internalState->ackRequestElement)
        setEndpointsImpl(internalState->ackRequestElement, epStr, altEpStr);
}

std::string CoTMessage::getCallsign() const
{
    std::string s("");
    if (internalState->contactElement == NULL)
        return s;
    xmlChar *ep = xmlGetProp(internalState->contactElement, (const xmlChar *)"callsign");
    if (!ep)
        return s;

    s = (char *)ep;
    xmlFree(ep);

    return s;
}

const ContactUID* CoTMessage::getContactUID() const
{
    // While every message has a UID, the UID is NOT always what
    // we consider a UID for the source contact.....
    // It only is such if the message is a contact-providing SA message,
    // which has the contact information in the details
    // Also, *intentionally* ignore any file share ack messages as older
    // versions of ATAK (and commo as it emulated ATAK) would set the
    // callsign incorrectly on these messages to the UID instead
    // of the actual callsign!  This fouls up callsign tracking of
    // a contact, so we want to say we have no contact for ack messages.
    // See WTK-2132 for more background.
    if (internalState->contactElement && !internalState->fileShareAckElement)
        return internalState->uid;
    else
        return NULL;
}

void CoTMessage::setPeerDestUid(const std::string *destUid)
{
    const xmlChar *destNodeName = (const xmlChar *)"__dest";
    xmlNode *destNode = getFirstChildElementByName(internalState->detailsElement, destNodeName);

    if (!destUid) {
        if (destNode) {
            xmlUnlinkNode(destNode);
            xmlFreeNode(destNode);
        }
        return;
    }

    if (!destNode)
        destNode = xmlNewChild(internalState->detailsElement, NULL, destNodeName, NULL);

    xmlAttr *attr = xmlHasProp(destNode, (const xmlChar *)"uid");
    if (attr)
        xmlSetProp(destNode, (const xmlChar *)"uid", (const xmlChar *)destUid->c_str());
    else
        xmlNewProp(destNode, (const xmlChar *)"uid", (const xmlChar *)destUid->c_str());
}

void CoTMessage::setTAKServerRecipients(
        const std::vector<std::string>* recipients)
{
    const xmlChar *martiNodeName = (const xmlChar *)"marti";
    xmlNode *martiNode = getFirstChildElementByName(internalState->detailsElement, martiNodeName);

    if (recipients == NULL || recipients->empty()) {
        // NULL and empty internally get treated the same since no <marti> list
        // will result in the message going to everyone
        if (martiNode) {
            xmlUnlinkNode(martiNode);
            xmlFreeNode(martiNode);
        }
        return;
    }

    if (!martiNode)
        martiNode = xmlNewChild(internalState->detailsElement, NULL, martiNodeName, NULL);
    else
        // Remove any existing children of the marti node
        removeAllChildren(martiNode);

    std::vector<std::string>::const_iterator iter;
    for (iter = recipients->begin(); iter != recipients->end(); ++iter)
    {
        xmlNode *dest = xmlNewChild(martiNode, NULL, (const xmlChar *)"dest", NULL);
        xmlNewProp(dest, (const xmlChar *)"callsign", (const xmlChar *)iter->c_str());
    }
}

void CoTMessage::setTAKServerMissionRecipient(const std::string &mission)
{
    const xmlChar *martiNodeName = (const xmlChar *)"marti";
    xmlNode *martiNode = getFirstChildElementByName(internalState->detailsElement, martiNodeName);

    if (!martiNode)
        martiNode = xmlNewChild(internalState->detailsElement, NULL, martiNodeName, NULL);
    else
        // Remove any existing children of the marti node
        removeAllChildren(martiNode);

    xmlNode *dest = xmlNewChild(martiNode, NULL, (const xmlChar *)"dest", NULL);
    xmlNewProp(dest, (const xmlChar *)"mission", (const xmlChar *)mission.c_str());
}

CommoLogger *CoTMessage::getLogger() const
{
    return logger;
}


CoTFileTransferRequest::CoTFileTransferRequest(const std::string &access,
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
                       const int httpsPort) :
                            access(access),
                            caveats(caveats),
                            releasableTo(releasableTo),
                            sha256hash(sha256hash),
                            name(name),
                            senderFilename(senderFilename),
                            senderUrl(senderUrl),
                            sizeInBytes(sizeInBytes),
                            senderCallsign(senderCallsign),
                            senderuid(new InternalContactUID(senderuid)),
                            ackuid(ackuid),
                            peerHosted(peerHosted),
                            httpsPort(httpsPort)
{
}
CoTFileTransferRequest::CoTFileTransferRequest(const CoTFileTransferRequest &src) :
        access(src.access),
        caveats(src.caveats),
        releasableTo(src.releasableTo),
        sha256hash(src.sha256hash),
        name(src.name),
        senderFilename(src.senderFilename),
        senderUrl(src.senderUrl),
        sizeInBytes(src.sizeInBytes),
        senderCallsign(src.senderCallsign),
        senderuid(new InternalContactUID(src.senderuid)),
        ackuid(src.ackuid),
        peerHosted(src.peerHosted),
        httpsPort(src.httpsPort)
{
}
CoTFileTransferRequest::~CoTFileTransferRequest()
{
    delete (InternalContactUID *)senderuid;
}
