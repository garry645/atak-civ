#include "contactmanager.h"
#include "commothread.h"
#include <string.h>
#include <sstream>

using namespace atakmap::commoncommo;
using namespace atakmap::commoncommo::impl;


namespace {
    // Time in seconds where a direct connection to a contact must be stagnant
    // (no rx'd data) to prefer any known streaming endpoint.
    const float CONTACT_DIRECT_TIMEOUT = 60.0f;
    // Time in seconds where a higher preference direct connection endpoint 
    // type will yield to one one that is this much more recently advertised
    const float CONTACT_DIRECT_EPSTALE = 30.0f;

    const char *THREAD_NAMES[] = {
        "cmoctact.evnt", 
        "cmotcp.proto",
    };
}


/**********************************************************************/
// Constructor/destructor


ContactManager::ContactManager(CommoLogger* logger,
        DatagramSocketManagement *dgMgmt,
        TcpSocketManagement *tcpMgmt,
        QuicManagement *quicMgmt,
        StreamingSocketManagement *streamMgmt) : 
                ThreadedHandler(2, THREAD_NAMES),
                DatagramListener(),
                StreamingMessageListener(),
                logger(logger),
                dgMgr(dgMgmt), tcpMgmt(tcpMgmt),
                quicMgmt(quicMgmt), streamMgmt(streamMgmt),
                contacts(), contactMapMutex(thread::RWMutex::Policy::Policy_Fair),
                protoVMutex(), protoVMonitor(),
                protoVersion(TakProtoInfo::SELF_MAX),
                protoVDirty(false),
                preferStreaming(false),
                destUidInsertionEnabled(false),
                listeners(), listenerMutex(), eventQueue(),
                eventQueueMutex(), eventQueueMonitor()
{
    // Register as a listener with the datagram layer
    // and streaming layer
    dgMgr->addDatagramReceiver(this);
    dgMgr->protoLevelChange(protoVersion);
    streamMgmt->addStreamingMessageListener(this);
    streamMgmt->addInterfaceStatusListener(this);

    startThreads();
}

ContactManager::~ContactManager()
{
    // De-register as a listener
    dgMgr->removeDatagramReceiver(this);
    streamMgmt->removeStreamingMessageListener(this);

    stopThreads();

    // Clean up all the contact states
    ContactMap::iterator iter;
    for (iter = contacts.begin(); iter != contacts.end(); iter++) {
        delete iter->second;
        delete (const InternalContactUID *)iter->first;
    }

    // Clean any remaining queue items
    std::deque<std::pair<InternalContactUID *, bool> >::iterator qiter;
    for (qiter = eventQueue.begin(); qiter != eventQueue.end(); ++qiter)
        delete qiter->first;
}


/**********************************************************************/
// ThreadedHandler stuff


void ContactManager::threadStopSignal(size_t threadNum)
{
    switch (threadNum) {
    case EVENT_THREADID:
        {
            thread::Lock lock(eventQueueMutex);
            eventQueueMonitor.broadcast(lock);
            break;
        }
    case PROTOV_THREADID:
        {
            thread::Lock lock(protoVMutex);
            protoVMonitor.broadcast(lock);
            break;
        }
    }
}

void ContactManager::threadEntry(size_t threadNum)
{
    switch (threadNum) {
    case EVENT_THREADID:
        queueThread();
        break;
    case PROTOV_THREADID:
        protoVThreadProcess();
        break;
    }
}

/**********************************************************************/
// Protocol version management thread

void ContactManager::protoVThreadProcess()
{
    int fireVersion = 0;

    while (!threadShouldStop(PROTOV_THREADID)) {
        bool fireUpdate = false;
        bool fireExtUpdate = false;
        ExtensionIdSet commonExtensions;
        CommoTime nowTime = CommoTime::now();
        {
            thread::Lock lock(protoVMutex);
            
            unsigned int uberMin = TakProtoInfo::SELF_MIN;
            unsigned int uberMax = TakProtoInfo::SELF_MAX;
            bool firstContact = true;
            int newProtoV = uberMax;
            {
                thread::ReadLock lock(contactMapMutex);
                
                ContactMap::iterator iter;
                for (iter = contacts.begin(); iter != contacts.end(); iter++) {
                    ContactState *state = iter->second;
                    thread::Lock stateLock(state->mutex);
                    
                    std::string dbgUid((const char *)iter->first->contactUID,
                                       iter->first->contactUIDLen);
                    bool epStale = nowTime >= state->meshEndpointExpireTime;
#if 0
                    InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                        "ProtoScan: %s %s EpExp %s ProtoInfExp %s min %d max %d fromep %d",
                        dbgUid.c_str(), state->callsign.c_str(), 
                        !epStale ? InternalUtils::intToString((state->meshEndpointExpireTime - nowTime).getSeconds()).c_str() : "StaleOrNever",
                        state->protoInfValid ? InternalUtils::intToString((state->protoInfExpireTime - nowTime).getSeconds()).c_str() : "NoProtoInf",
                        state->protoInf.first,
                        state->protoInf.second,
                        state->lastMeshEndpointProto);
#endif
                    
                    if (state->protoInfValid &&
                            nowTime >= state->protoInfExpireTime) {
                        state->protoInfValid = false;
                        state->protoInf.first = state->protoInf.second = 
                                  state->lastMeshEndpointProto;
                    }

                    if (epStale) {
                        // Don't factor this guy into protov calc
                        // or shared extensions considerations.
                        // He's disappeared or never had a mesh endpoint
                        continue;
                    }
                    
                    if (newProtoV) {
                        uberMin = 
                            uberMin > state->protoInf.first ?
                                uberMin : state->protoInf.first;
                        uberMax = 
                            uberMax < state->protoInf.second ?
                                uberMax : state->protoInf.second;
                        if (uberMin > uberMax)
                            // No overlap; give up, go to 0
                            newProtoV = 0;
                        else if (firstContact) {
                            commonExtensions = state->extensions;
                            firstContact = false;
                        } else {
                            ExtensionIdSet::iterator ci = commonExtensions.begin();
                            while (ci != commonExtensions.end()) {
                                ExtensionIdSet::iterator cur = ci;
                                ci++;
                                if (state->extensions.count(*cur) == 0)
                                    commonExtensions.erase(cur);
                            }
                        }
                    }
                }
            }
            
            if (newProtoV)
                newProtoV = uberMax;
            if (newProtoV != protoVersion) {
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                    "ProtoScan: Switching broadcast protocol version from %d to %d",
                    protoVersion, newProtoV);
                protoVersion = fireVersion = newProtoV;
                fireUpdate = true;
            } else {
#if 0
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                    "ProtoScan: No change to broadcast protocol version (%d)",
                    protoVersion);
#endif
            }
            if (commonExtensions != protoExtensions) {
                std::stringstream ss;
                for (ExtensionIdSet::value_type id : commonExtensions)
                    ss << id << " ";
                std::string s = ss.str();
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                    "ProtoScan: Switching broadcast protocol extensions (%s)",
                    s.c_str());
                protoExtensions = commonExtensions;
                fireExtUpdate = true;
            } else {
#if 0
                InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG,
                    "ProtoScan: No change to broadcast extension set");
#endif
            }
            protoVDirty = false;
        }

        if (fireExtUpdate) {
            protoVThreadFireExtSetChange(commonExtensions);
        }
        if (fireUpdate) {
            protoVThreadFireVersionChange(fireVersion);
        }

        {
            thread::Lock lock(protoVMutex);

            if (!protoVDirty && !threadShouldStop(PROTOV_THREADID)) {
                protoVMonitor.wait(lock, 
                    TakMessage::PROTOINF_TIMEOUT_SEC / 4 * 1000);
            }
        }
        
    }
}

void ContactManager::protoVThreadFireVersionChange(int newVersion)
{
    dgMgr->protoLevelChange(newVersion);
}

void ContactManager::protoVThreadFireExtSetChange(const ExtensionIdSet &extIds)
{
    dgMgr->protoExtSetChange(extIds);
}

int ContactManager::getProtoVersion()
{
    return protoVersion;
}


/**********************************************************************/
// Rx data


void ContactManager::datagramReceivedGeneric(
        const std::string *endpointId,
        const uint8_t *data, size_t length)
{
}

void ContactManager::datagramReceived(
        const std::string *endpointId,
        const NetAddress *sender,
        const TakMessage *takmsg)
{
    const CoTMessage *msg = takmsg->getCoTMessage();
    const TakProtoInfo *protoInf = takmsg->getProtoInfo();
    const ExtensionIdSet *extIds = takmsg->getExtensions();
    const ContactUID *uid = takmsg->getContactUID();
    unsigned int msgVer = takmsg->getProtoVersion();

    msgRxImpl(msgVer, msg, uid, NULL, sender, protoInf, extIds);
}

void ContactManager::streamingMessageReceived(
        std::string streamingEndpoint, const CoTMessage *msg)
{
    msgRxImpl(0, msg, msg->getContactUID(), &streamingEndpoint,
              NULL, NULL, NULL);
}


void ContactManager::msgRxImpl(
       unsigned int msgVer,
       const CoTMessage *msg,
       const ContactUID *uid,
       const std::string *streamSource, 
       const NetAddress *sender,
       const TakProtoInfo *protoInf,
       const ExtensionIdSet *extIds)
{
    if (!uid)
        return;

    CoTMessage::EndpointList endpoints;
    std::string callsign;
    bool protoOnly = msg == NULL;
    if (!protoOnly) {
        callsign = msg->getCallsign();
        msg->getEndpoints(&endpoints);
        if (endpoints.empty()) {
            if (protoInf)
                // No valid endpoint/contact update,
                // but should still update proto info if present
                protoOnly = true;
            else
                return;
        }
    }

    processContactUpdate(protoOnly, msgVer, uid, callsign, streamSource,
                         false, sender, protoInf, extIds,
                         &endpoints);
}

CommoResult ContactManager::processContactUpdate(
        bool protoInfOnly,
        unsigned int msgVer,
        const ContactUID *uid,
        const std::string &callsign,
        const std::string *streamSource,
        bool knownEP,
        const NetAddress *sender,
        const TakProtoInfo *protoInf,
        const ExtensionIdSet *extIds,
        CoTMessage::EndpointList *endpoints)
{
    ContactState *state = NULL;
    bool isOk = true;
    bool protoNeedsRefresh = false;
    {
        thread::ReadLock lock(contactMapMutex);
        ContactMap::iterator iter = contacts.find(uid);
        if (iter != contacts.end()) {
            state = iter->second;
            isOk = processContactStateUpdate(state, &protoNeedsRefresh,
                                             protoInfOnly,
                                             msgVer, callsign,
                                             streamSource, knownEP,
                                             sender, protoInf,
                                             extIds, endpoints);
        }
    }
    if (!protoInfOnly && !state) {
        // Retry with the write lock so we can add new if needed
        thread::WriteLock lock(contactMapMutex);
        ContactMap::iterator iter = contacts.find(uid);
        bool isNew = (iter == contacts.end());
        if (!isNew) {
            state = iter->second;
        } else {
            state = new ContactState(knownEP);
        }
        isOk = processContactStateUpdate(state, &protoNeedsRefresh,
                                         protoInfOnly,
                                         msgVer, callsign,
                                         streamSource, knownEP,
                                         sender, protoInf,
                                         extIds, endpoints);

        if (isNew) {
            if (!isOk) {
                delete state;
            } else {
                ContactUID *intUID = new InternalContactUID(uid);
                contacts[intUID] = state;
                queueContactPresenceChange(intUID, true);
                protoNeedsRefresh = true;
            }
        }
    }
    if (protoNeedsRefresh) {
        thread::Lock vLock(protoVMutex);
        protoVDirty = true;
        protoVMonitor.broadcast(vLock);
    }
    return isOk ? COMMO_SUCCESS : COMMO_ILLEGAL_ARGUMENT;
}

bool ContactManager::processContactStateUpdate(ContactState *state,
                            bool *protoNeedsRefresh,
                            bool protoInfOnly,
                            unsigned int msgVer,
                            const std::string &callsign,
                            const std::string *streamSource,
                            bool knownEP,
                            const NetAddress *sender,
                            const TakProtoInfo *protoInf,
                            const ExtensionIdSet *extIds,
                            CoTMessage::EndpointList *endpoints)
{
    bool isOk = true;
    bool needRecompute = false;
    CommoTime nowTime = CommoTime::now();
    {
        thread::Lock sLock(state->mutex);
        if (knownEP != state->knownEndpoint) {
            *protoNeedsRefresh = false;
            return false;
        }
        
        bool epWasNotStale = nowTime <= state->meshEndpointExpireTime;
        // If it fails, we ignore this message and keep old endpoint(s)
        if (!protoInfOnly) {
            if (!streamSource) {
                isOk = processContactStateEndpointsUpdate(state, sender,
                                                          endpoints);
            } else {
                isOk = updateStateStreamEndpoint(state, *streamSource);
            }
            state->callsign = callsign;
        }

        // Protocol tracking.  Don't bother for messages that arrived via
        // streaming EPs
        if (!streamSource && !knownEP) {
            bool validEpPresent = isOk && !protoInfOnly;
            bool epNotStale = epWasNotStale || validEpPresent;
            std::pair<unsigned int, unsigned int> oldInfo = state->protoInf;
            bool extChange = false;

            if (validEpPresent)
                state->lastMeshEndpointProto = msgVer;

            if (protoInf) {
                state->protoInfValid = true;
                state->protoInfExpireTime = nowTime + 
                                        (float)TakMessage::PROTOINF_TIMEOUT_SEC;
                state->protoInf.first = protoInf->getMin();
                state->protoInf.second = protoInf->getMax();
                if (state->extensions != *extIds) {
                    extChange = true;
                    state->extensions = *extIds;
                }
            } else if (!state->protoInfValid && validEpPresent) {
                state->protoInf.first = state->protoInf.second = msgVer;
            }
            needRecompute = epNotStale && (!epWasNotStale ||
                            extChange || (oldInfo != state->protoInf));
        }
    }
    
    *protoNeedsRefresh = needRecompute;
    
    return isOk;
}

bool ContactManager::processContactStateEndpointsUpdate(ContactState *state,
                           const NetAddress *sender,
                           CoTMessage::EndpointList *endpoints)
{
    bool isOk = false;
    for (CoTMessage::EndpointList::iterator iter = endpoints->begin();
            iter != endpoints->end(); ++iter) {
        CoTMessage::EndpointInfo &endpoint = *iter;
        CoTEndpoint::InterfaceType itype;
        switch (endpoint.type) {
            case ENDPOINT_UDP:
                itype = CoTEndpoint::DATAGRAM;
                break;
            case ENDPOINT_TCP_USESRC:
                if (sender) {
                    itype = CoTEndpoint::TCP;
                    // Use sender's source address
                    sender->getIPString(&endpoint.host);
                } else
                    continue;
                break;
            case ENDPOINT_QUIC_USESRC:
                if (sender) {
                    // Use sender's source address
                    itype = CoTEndpoint::QUIC;
                    // Use sender's source address
                    sender->getIPString(&endpoint.host);
                } else
                    continue;
                break;
            case ENDPOINT_UDP_USESRC:
                if (sender) {
                    itype = CoTEndpoint::DATAGRAM;
                    // Use sender's source address
                    sender->getIPString(&endpoint.host);
                } else
                    continue;
                break;
            case ENDPOINT_QUIC:
                itype = CoTEndpoint::QUIC;
                break;
            case ENDPOINT_TCP:
                itype = CoTEndpoint::TCP;
                break;
            default:
                // We don't allow stream (or others?) types
                // off non-stream input
                continue;
        }
        if (endpoint.host.empty())
            continue;

        switch (itype) {
        case CoTEndpoint::DATAGRAM:
            isOk = updateStateDatagramEndpoint(state, endpoint.host,
                                               endpoint.port) || isOk;
            break;
        case CoTEndpoint::TCP:
            isOk = updateStatePeerEndpoint(state, &state->tcpEndpoint,
                                           itype, endpoint.host,
                                           endpoint.port) || isOk;
            break;
        case CoTEndpoint::QUIC:
            isOk = updateStatePeerEndpoint(state, &state->quicEndpoint,
                                           itype, endpoint.host,
                                           endpoint.port) || isOk;
            break;
        default:
            break;
        }
    }
    return isOk;
}

void ContactManager::interfaceUp(NetInterface *iface)
{
}

void ContactManager::interfaceDown(NetInterface *iface)
{
    // We only presently listen to streams
    StreamingNetInterface *siface = (StreamingNetInterface *)iface;
    const char *ep = siface->remoteEndpointId;
    removeStream(ep);
}

void ContactManager::removeStream(const std::string &epString)
{
    // We only presently listen to streams
    std::set<ContactUID *> removedUids;

    {
        thread::WriteLock lock(contactMapMutex);
        ContactMap::iterator iter = contacts.begin();
        while (iter != contacts.end()) {
            ContactMap::iterator curIter = iter;
            iter++;
            bool killState = false;
            ContactState *state = curIter->second;
            {
                thread::Lock sLock(state->mutex);
                if (state->streamEndpoint &&
                            state->streamEndpoint->getEndpointString() == epString) {
                    if (!state->datagramEndpoint && !state->tcpEndpoint) {
                        queueContactPresenceChange(curIter->first, false);
                        delete (const InternalContactUID *)curIter->first;
                        contacts.erase(curIter);
                        killState = true;
                    } else {
                        // Don't delete this one - still has other EPs
                        delete state->streamEndpoint;
                        state->streamEndpoint = NULL;
                    }
                    
                }
            }
            if (killState)
                delete state;
        }
    }
}



/**********************************************************************/
// Tx data

CommoResult ContactManager::sendCoT(ContactList *destinations,
        CoTMessage *cotMessage, CoTSendMethod sendMethod)
{
    std::vector<const ContactUID *> vdest;
    for (size_t i = 0; i < destinations->nContacts; ++i)
        vdest.push_back(destinations->contacts[i]);
    CommoResult ret = sendCoT(&vdest, cotMessage, sendMethod);
    destinations->nContacts = vdest.size();
    for (size_t i = 0; i < destinations->nContacts; ++i)
        destinations->contacts[i] = vdest[i];
    return ret;
}

CommoResult ContactManager::sendCoT(
        std::vector<const ContactUID *> *destinations,
        CoTMessage *cotMessage, CoTSendMethod sendMethod)
{
    std::vector<const ContactUID *> ret;
    bool destUidProcessed = false;

    typedef std::pair<std::vector<std::string>, std::vector<const ContactUID *> > CallContactPair;
    std::map<std::string, CallContactPair > streamMap;
    {
        thread::ReadLock lock(contactMapMutex);

        std::map<std::string, CallContactPair >::iterator smIter;
        std::vector<const ContactUID *>::iterator iter;
        for (iter = destinations->begin(); iter != destinations->end(); ++iter) {
            ContactMap::iterator contactIter = contacts.find(*iter);
            if (contactIter == contacts.end()) {
                // This contact is gone
                ret.push_back(*iter);
                continue;
            }
            ContactState *state = contactIter->second;
            {
                thread::Lock stateLock(state->mutex);
                CoTEndpoint *ep = getCurrentEndpoint(state, sendMethod);
                try {
                    if (!ep)
                        // Available endpoints don't match available methods
                        // They aren't "gone", just can't send the
                        // way the caller intends. But per API we treat
                        // them as "gone"
                        throw std::invalid_argument("");

                    std::string contactUidStr(
                            (const char *)contactIter->first->contactUID,
                            contactIter->first->contactUIDLen);

                    switch (ep->getType()) {
                    case CoTEndpoint::DATAGRAM:
                    {
                        DatagramCoTEndpoint *dgce = (DatagramCoTEndpoint *)ep;
                        InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "Sending CoT to contact %s using datagram endpoint %s and protocol version %d", contactUidStr.c_str(), dgce->getEndpointString().c_str(), 0);

                        int version = getSendProtoVersion(state);
                        if (destUidInsertionEnabled) {
                            cotMessage->setPeerDestUid(&contactUidStr);
                            destUidProcessed = true;
                        }

                        dgMgr->sendDatagram(dgce->getNetAddr(cotMessage->getType()),
                                            cotMessage, version, state->extensions);
                        break;
                    }
                    case CoTEndpoint::TCP:
                    {
                        PeerCoTEndpoint *tcpce = (PeerCoTEndpoint *)ep;
                        int port = tcpce->getPortNum(cotMessage->getType());
                        std::string host = tcpce->getHostString();
                        int version = getSendProtoVersion(state);
                        InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "Sending CoT to contact %s using tcp endpoint %s:%d and protocol version %d", contactUidStr.c_str(), host.c_str(), port, version);

                        if (destUidInsertionEnabled) {
                            cotMessage->setPeerDestUid(&contactUidStr);
                            destUidProcessed = true;
                        }
                        tcpMgmt->sendMessage(host, port, cotMessage,
                                             state->extensions,
                                             version);
                        break;
                    }
                    case CoTEndpoint::QUIC:
                    {
                        PeerCoTEndpoint *quicce = (PeerCoTEndpoint *)ep;
                        int port = quicce->getPortNum(cotMessage->getType());
                        std::string host = quicce->getHostString();
                        int version = getSendProtoVersion(state);
                        InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "Sending CoT to contact %s using quic endpoint %s:%d and protocol version %d", contactUidStr.c_str(), host.c_str(), port, version);

                        if (destUidInsertionEnabled) {
                            cotMessage->setPeerDestUid(&contactUidStr);
                            destUidProcessed = true;
                        }
                        quicMgmt->sendMessage(host, port, cotMessage,
                                             state->extensions,
                                             version);
                        break;
                    }
                    case CoTEndpoint::STREAMING:
                    {
                        StreamingCoTEndpoint *sce = (StreamingCoTEndpoint *)ep;
                        std::string epString = sce->getEndpointString();

                        InternalUtils::logprintf(logger, CommoLogger::LEVEL_DEBUG, "Sending CoT to contact %s using TAK server endpoint %s", contactUidStr.c_str(), epString.c_str());

                        smIter = streamMap.find(epString);
                        if (smIter != streamMap.end()) {
                            smIter->second.first.push_back(state->callsign);
                            smIter->second.second.push_back(*iter);
                        } else {
                            CallContactPair ccp;
                            ccp.first.push_back(state->callsign);
                            ccp.second.push_back(*iter);
                            streamMap.insert(std::pair<std::string, CallContactPair>(epString, ccp));
                        }
                        break;
                    }
                    default:
                        throw std::invalid_argument("");
                    }
                } catch (std::invalid_argument &) {
                    // for datagram, interface is gone, which means so is our contact
                    // for streaming, connection is down, which means we can't
                    // reach the contact.
                    // or... Unsupported endpoint type?
                    // all given same treatment.
                    ret.push_back(*iter);
                }
            }
        }
    }
    
    // Clear off dest uid if set; not desired for streaming traffic
    if (destUidProcessed)
        cotMessage->setPeerDestUid(NULL);

    {
        // Note carefully: We gave up context lock up above
        // (intentionally, to avoid calling back into streamMgmt while holding
        // that lock). Thus, it is UNSAFE
        // to use the UID part of the values of streamMap as they could
        // have been cleaned up externally.
        std::map<std::string, CallContactPair >::iterator smIter;
        for (smIter = streamMap.begin(); smIter != streamMap.end(); ++smIter) {
            try {
                CoTMessage sMsg(*cotMessage);
                sMsg.setEndpoints(ENDPOINT_STREAMING, "", NULL);
                CallContactPair ccp = smIter->second;
                std::vector<std::string> &v = ccp.first;
                sMsg.setTAKServerRecipients(&v);
                streamMgmt->sendMessage(smIter->first, &sMsg);
            } catch (std::invalid_argument &) {
                // either stream is down or there some error copying the
                // message (unlikely).
                ret.insert(ret.end(), smIter->second.second.begin(),
                                      smIter->second.second.end());
            }
        }
    }

    destinations->clear();
    destinations->insert(destinations->begin(), ret.begin(), ret.end());
    return ret.empty() ? COMMO_SUCCESS : COMMO_CONTACT_GONE;
}


/**********************************************************************/
// Contact presence listener management

void ContactManager::addContactPresenceListener(
        ContactPresenceListener* listener) COMMO_THROW (std::invalid_argument)
{
    thread::Lock lock(listenerMutex);
    if (!listeners.insert(listener).second)
        throw std::invalid_argument("Listener already registered");
}

void ContactManager::removeContactPresenceListener(
        ContactPresenceListener* listener) COMMO_THROW (std::invalid_argument)
{
    thread::Lock lock(listenerMutex);
    if (listeners.erase(listener) != 1)
        throw std::invalid_argument("Provided listener for removal is not registered");
}

void ContactManager::fireContactPresenceChange(const ContactUID *c, bool present)
{
    thread::Lock lock(listenerMutex);
    std::set<ContactPresenceListener *>::iterator iter;
    for (iter = listeners.begin(); iter != listeners.end(); ++iter) {
        ContactPresenceListener *listener = *iter;
        if (present)
            listener->contactAdded(c);
        else
            listener->contactRemoved(c);
    }
}

void ContactManager::queueContactPresenceChange(const ContactUID *c, bool present)
{
    thread::Lock lock(eventQueueMutex);

    eventQueue.push_front(std::pair<InternalContactUID *, bool>(new InternalContactUID(c), present));
    eventQueueMonitor.broadcast(lock);
}

void ContactManager::queueThread()
{
    while (!threadShouldStop(EVENT_THREADID)) {
        std::pair<InternalContactUID *, bool> event;
        {
            thread::Lock lock(eventQueueMutex);
            if (eventQueue.empty()) {
                eventQueueMonitor.wait(lock);
                continue;
            }

            event = eventQueue.back();
            eventQueue.pop_back();
        }
        fireContactPresenceChange(event.first, event.second);
        delete event.first;
    }
}


/**********************************************************************/
// Contact access


void ContactManager::setPreferStreamEndpoint(bool preferStream)
{
    this->preferStreaming = preferStream;
}

void ContactManager::setDestUidInsertionEnabled(bool destUidInsertionEnabled)
{
    thread::WriteLock lock(contactMapMutex);
    this->destUidInsertionEnabled = destUidInsertionEnabled;
}

const ContactList *ContactManager::getAllContacts()
{
    thread::ReadLock lock(contactMapMutex);
    size_t n = contacts.size();
    const ContactUID **extContacts = new const ContactUID*[n];
    ContactMap::iterator iter;
    int i;
    for (i = 0, iter = contacts.begin(); iter != contacts.end(); i++, iter++)
        extContacts[i] = new InternalContactUID(iter->first);
    return new ContactList(n, extContacts);
}

void ContactManager::freeContactList(const ContactList *list)
{
    for (size_t i = 0; i < list->nContacts; ++i)
        delete (InternalContactUID *)(list->contacts[i]);
    delete[] list->contacts;
    delete list;
}


CommoResult ContactManager::configKnownEndpointContact(
                                       const ContactUID *contact,
                                       const char *callsign,
                                       const char *ipAddr,
                                       int destPort)
{
    if (((ipAddr == NULL) ^ (callsign == NULL)) || destPort < 0)
        return COMMO_ILLEGAL_ARGUMENT;

    if (!ipAddr && !callsign) {
        // Delete this "known" contact
        thread::WriteLock lock(contactMapMutex);
        ContactMap::iterator iter = contacts.find(contact);
        if (iter == contacts.end())
            return COMMO_ILLEGAL_ARGUMENT;

        ContactState *state = iter->second;
        if (!state->knownEndpoint)
            return COMMO_ILLEGAL_ARGUMENT;

        contacts.erase(iter);
        delete state;
        return COMMO_SUCCESS;
        
    } else if (strlen(ipAddr) == 0 || strlen(callsign) == 0) {
        return COMMO_ILLEGAL_ARGUMENT;
    } else {
        CoTMessage::EndpointList endpoints;
        endpoints.emplace_back(ipAddr, destPort, EndpointType::ENDPOINT_UDP);
        return processContactUpdate(false, 0, contact, callsign,
                                    NULL, true, NULL, NULL, NULL,
                                    &endpoints);
    }
}



/**********************************************************************/
// Contact endpoint info

std::string ContactManager::getActiveEndpointHost(const ContactUID *contact,
                                          CoTEndpoint::InterfaceType *epType,
                                          int *port)
{
    thread::ReadLock lock(contactMapMutex);
    ContactMap::iterator iter = contacts.find(contact);
    if (iter == contacts.end())
        return "";

    {
        thread::Lock lock2(iter->second->mutex);
        CoTEndpoint *ep = getCurrentEndpoint(iter->second);
        std::string ret;
        if (ep) {
            if (epType)
                *epType = ep->getType();
            int portval = -1;
            switch (ep->getType()) {
            case CoTEndpoint::STREAMING:
                ret = "";
                break;
            case CoTEndpoint::TCP:
            case CoTEndpoint::QUIC:
            {
                PeerCoTEndpoint *pep = (PeerCoTEndpoint *)ep;
                ret = pep->getHostString();
                if (port)
                    portval = pep->getPortNum(SITUATIONAL_AWARENESS);
                break;
            }
            case CoTEndpoint::DATAGRAM:
                DatagramCoTEndpoint *dge = (DatagramCoTEndpoint *)ep;
                dge->getBaseAddr()->getIPString(&ret);
                break;
            }
            if (port)
                *port = portval;
        }
        return ret;
    }
}

bool ContactManager::hasContact(const ContactUID *contact)
{
    thread::ReadLock lock(contactMapMutex);
    ContactMap::iterator iter = contacts.find(contact);
    return iter != contacts.end();
}

bool ContactManager::hasStreamingEndpoint(const ContactUID *contact)
{
    thread::ReadLock lock(contactMapMutex);
    ContactMap::iterator iter = contacts.find(contact);
    if (iter == contacts.end())
        return false;

    {
        thread::Lock lock2(iter->second->mutex);
        return iter->second->streamEndpoint != NULL;
    }
}

std::string ContactManager::getStreamEndpointIdentifier(const ContactUID *contact, bool ifActive) COMMO_THROW (std::invalid_argument)
{
    thread::ReadLock lock(contactMapMutex);
    ContactMap::iterator iter = contacts.find(contact);
    if (iter == contacts.end())
        throw std::invalid_argument("Specified contact is not known");

    {
        thread::Lock lock2(iter->second->mutex);
        if (iter->second->streamEndpoint == NULL)
            throw std::invalid_argument("Specified contact has no streaming endpoint");
        else if (!ifActive || getCurrentEndpoint(iter->second) == iter->second->streamEndpoint)
            return iter->second->streamEndpoint->getEndpointString();
        else
            throw std::invalid_argument("Specified contact has no active streaming endpoint");
    }
}


/**********************************************************************/
// Private: Endpoint management

int ContactManager::getSendProtoVersion(ContactState *state)
{
    if (state->protoInf.second < TakProtoInfo::SELF_MIN ||
         state->protoInf.first > TakProtoInfo::SELF_MAX)
        // No overlap - revert to legacy
        return 0;
    return state->protoInf.second < TakProtoInfo::SELF_MAX ? 
        state->protoInf.second : TakProtoInfo::SELF_MAX;
}

// NOTE: assumes holding of the mutex for the contact
CoTEndpoint* ContactManager::getCurrentEndpoint(ContactState *state, CoTSendMethod sendMethod)
{
    // preferStreaming == false (and legacy before this option):
    // Priority given to tcp, quic or datagram connection over stream if known
    // AND last seen via tcp, quic or datagram time is less than some time ago
    //
    // preferStreaming == true
    // Priority given to stream endpoint if known, else fall back to non-stream
    CoTEndpoint *ret = NULL;
    CoTEndpoint *meshEp = NULL;
    CoTEndpoint *streamEp = NULL;
    
    if ((sendMethod & SEND_TAK_SERVER) != 0)
        streamEp = state->streamEndpoint;

    if ((sendMethod & SEND_POINT_TO_POINT) &&
                (state->tcpEndpoint || state->datagramEndpoint || state->quicEndpoint)) {
        // quic first (if enabled), then tcp, then udp
        // pass over higher priority to lower if that ep type when lower
        // priority has reporting time at least CONTACT_DIRECT_EPSTALE seconds more
        // recent than the higher priority

        CommoTime nearestTime = CommoTime::ZERO_TIME;
        if (state->quicEndpoint && quicMgmt->hasInboundInterface()) {
            nearestTime = state->quicEndpoint->getLastRxTime();
            meshEp = state->quicEndpoint;
        }
        if (state->tcpEndpoint && (!meshEp || 
                state->tcpEndpoint->getLastRxTime().minus(nearestTime) > 
                        CONTACT_DIRECT_EPSTALE)) {
            nearestTime = state->tcpEndpoint->getLastRxTime();
            meshEp = state->tcpEndpoint;
        }
        if (state->datagramEndpoint && (!meshEp || 
                state->datagramEndpoint->getLastRxTime().minus(nearestTime) > 
                        CONTACT_DIRECT_EPSTALE)) {
            nearestTime = state->datagramEndpoint->getLastRxTime();
            meshEp = state->datagramEndpoint;
        }
        if (meshEp && streamEp &&
                    CommoTime::now().minus(nearestTime) > CONTACT_DIRECT_TIMEOUT)
            // Force fallback to streaming ep below, if exists
            meshEp = NULL;
    }
    
    if (meshEp && streamEp) {
        ret = preferStreaming ? streamEp : meshEp;
    } else {
        ret = meshEp ? meshEp : streamEp;
    }

    return ret;
}


// NOTE: assumes holding of the mutex for the contact
bool ContactManager::updateStateDatagramEndpoint(ContactState *state,
        const std::string &ep, int port)
{
    try {
        if (state->knownEndpoint) {
            if (!state->datagramEndpoint)
                state->datagramEndpoint = new DatagramCoTEndpoint(ep, port);
            else
                state->datagramEndpoint->updateKnownEndpoint(ep, port);
        } else {
            if (!state->datagramEndpoint)
                state->datagramEndpoint = new DatagramCoTEndpoint(ep);
            else
                state->datagramEndpoint->updateFromCoTEndpoint(ep);
            state->meshEndpointExpireTime = 
                state->datagramEndpoint->getLastRxTime() +
                (float)TakMessage::PROTOINF_TIMEOUT_SEC;
        }
        return true;
    } catch (std::invalid_argument &) {
        return false;
    }
}

// NOTE: assumes holding of the mutex for the contact
bool ContactManager::updateStatePeerEndpoint(ContactState *state,
        PeerCoTEndpoint **stateEndpoint,
        CoTEndpoint::InterfaceType type, const std::string &ep, int port)
{
    try {
        if (!*stateEndpoint)
            *stateEndpoint = new PeerCoTEndpoint(type, ep, port);
        else
            (*stateEndpoint)->updateFromCoTEndpoint(ep, port);
        state->meshEndpointExpireTime = 
            state->tcpEndpoint->getLastRxTime() +
            (float)TakMessage::PROTOINF_TIMEOUT_SEC;
        return true;
    } catch (std::invalid_argument &) {
        return false;
    }
}

// NOTE: assumes holding of the mutex for the contact
bool ContactManager::updateStateStreamEndpoint(ContactState *state,
        const std::string &ep)
{
    try {
        if (!state->streamEndpoint)
            state->streamEndpoint = new StreamingCoTEndpoint(ep);
        else
            state->streamEndpoint->updateFromStreamingEndpoint(ep);
        return true;
    } catch (std::invalid_argument &) {
        return false;
    }
}


/**********************************************************************/
// CotEndpoint and subclasses

CoTEndpoint::CoTEndpoint(InterfaceType type) : type(type),
        lastRxTime(CommoTime::now())
{
}

CoTEndpoint::~CoTEndpoint()
{
}

CoTEndpoint::InterfaceType CoTEndpoint::getType() const
{
    return type;
}

CommoTime CoTEndpoint::getLastRxTime() const
{
    return lastRxTime;
}

void CoTEndpoint::touchRxTime()
{
    lastRxTime = CommoTime::now();
}

DatagramCoTEndpoint::DatagramCoTEndpoint(const std::string& endpointString)
            COMMO_THROW (std::invalid_argument) :
                CoTEndpoint(DATAGRAM),
                baseAddr(NULL),
                chatAddr(NULL),
                saAddr(NULL),
                cachedEndpointString(""),
                isKnown(false),
                port(0)
{
    updateFromCoTEndpoint(endpointString);
}

DatagramCoTEndpoint::DatagramCoTEndpoint(const std::string& ipString, int port)
            COMMO_THROW (std::invalid_argument) :
                CoTEndpoint(DATAGRAM),
                baseAddr(NULL),
                chatAddr(NULL),
                saAddr(NULL),
                cachedEndpointString(""),
                isKnown(true),
                port(port)
{
    updateKnownEndpoint(ipString, port);
}

DatagramCoTEndpoint::~DatagramCoTEndpoint()
{
    delete baseAddr;
    delete saAddr;
    delete chatAddr;
}

const NetAddress* DatagramCoTEndpoint::getBaseAddr() const
{
    return baseAddr;
}

const NetAddress* DatagramCoTEndpoint::getNetAddr(CoTMessageType type) const
{
    NetAddress *ret;
    switch (type) {
    case CHAT:
        ret = chatAddr;
        break;
    case SITUATIONAL_AWARENESS:
    default:
        ret = saAddr;
        break;
    }
    return ret;
}

void DatagramCoTEndpoint::updateFromCoTEndpoint(
        const std::string& endpointString)
                COMMO_THROW (std::invalid_argument)
{
    if (isKnown)
        throw std::invalid_argument("Cannot update endpoint of known contact");
    if (endpointString.empty())
        throw std::invalid_argument("Endpoint cannot be empty");
    if (endpointString.compare(cachedEndpointString) != 0) {
        NetAddress *addr = NetAddress::create(endpointString.c_str());
        if (!addr)
            throw std::invalid_argument("Cannot parse endpoint string");

        cachedEndpointString = endpointString;
        delete this->baseAddr;
        this->baseAddr = addr;
        delete this->saAddr;
        this->saAddr = baseAddr->deriveNewAddress(6969);
        delete this->chatAddr;
        this->chatAddr = baseAddr->deriveNewAddress(17012);
    }
    touchRxTime();
}

void DatagramCoTEndpoint::updateKnownEndpoint(const std::string &ipString,
                                              int port)
                                              COMMO_THROW (std::invalid_argument)
{
    if (!isKnown)
        throw std::invalid_argument("Cannot update endpoint of discovered contact");

    if (ipString.empty())
        throw std::invalid_argument("IP string cannot be empty");

    NetAddress *addr = NetAddress::create(ipString.c_str());
    if (!addr)
        throw std::invalid_argument("Cannot parse endpoint string");

    cachedEndpointString = ipString;
    delete this->baseAddr;
    this->baseAddr = addr;
    delete this->saAddr;
    this->saAddr = baseAddr->deriveNewAddress(port);
    delete this->chatAddr;
    this->chatAddr = baseAddr->deriveNewAddress(port);
}

std::string DatagramCoTEndpoint::getEndpointString()
{
    return cachedEndpointString;
}

PeerCoTEndpoint::PeerCoTEndpoint(InterfaceType type, 
                                 const std::string& ipString, int port)
            COMMO_THROW (std::invalid_argument) :
                CoTEndpoint(type),
                cachedEndpointString(""),
                port(0)
{
    updateFromCoTEndpoint(ipString, port);
}

PeerCoTEndpoint::~PeerCoTEndpoint()
{
}

const std::string PeerCoTEndpoint::getHostString() const
{
    return cachedEndpointString;
}

const int PeerCoTEndpoint::getPortNum(CoTMessageType type) const
{
    return port;
}

void PeerCoTEndpoint::updateFromCoTEndpoint(
        const std::string& endpointString, int port)
                COMMO_THROW (std::invalid_argument)
{
    if (endpointString.empty())
        throw std::invalid_argument("Endpoint cannot be empty");
    if (port <= 0)
        throw std::invalid_argument("Endpoint port cannot be <= 0");
    if (endpointString.compare(cachedEndpointString) != 0 || this->port != port) {
        cachedEndpointString = endpointString;
        this->port = port;
    }
    touchRxTime();
}

StreamingCoTEndpoint::StreamingCoTEndpoint(const std::string &ep) :
        CoTEndpoint(CoTEndpoint::STREAMING), endpointString(ep)
{
}

StreamingCoTEndpoint::~StreamingCoTEndpoint()
{
}

void StreamingCoTEndpoint::updateFromStreamingEndpoint(
        const std::string& sourceEndpoint)
{
    if (sourceEndpoint.empty())
        throw std::invalid_argument("Endpoint cannot be empty");
    endpointString = sourceEndpoint;
    touchRxTime();
}

std::string StreamingCoTEndpoint::getEndpointString()
{
    return endpointString;
}



/**********************************************************************/
// ContactState

ContactState::ContactState(bool knownEP) :
        protoInfValid(false),
        protoInfExpireTime(CommoTime::ZERO_TIME),
        protoInf(0, 0),
        meshEndpointExpireTime(CommoTime::ZERO_TIME),
        lastMeshEndpointProto(0),
        streamEndpoint(NULL),
        datagramEndpoint(NULL), tcpEndpoint(NULL), quicEndpoint(NULL),
        callsign(), knownEndpoint(knownEP), mutex() {
}

ContactState::~ContactState() {
    delete streamEndpoint;
    delete datagramEndpoint;
    delete tcpEndpoint;
    delete quicEndpoint;
}
