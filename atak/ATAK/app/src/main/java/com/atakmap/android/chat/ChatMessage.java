
package com.atakmap.android.chat;

import android.os.Bundle;

import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.util.ArrayList;
import java.util.List;

/**
 * Chat message Class
 */
class ChatMessage {

    private String conversationName;
    private String conversationId;
    private String messageId;
    private String access;
    private String caveat;
    private String releasableTo;
    private String protocol = "CoT"; //how we got it (i.e. CotDispatcher)
    private CoordinatedTime receiveTime;
    private CoordinatedTime sentTime;
    private String senderUid;
    private String senderName;
    private String parentUid;
    private String deleteGroup;
    private boolean groupOwner = false;
    private Bundle destinationPaths;
    private ArrayList<String> destinations = new ArrayList<>();
    private boolean tadilJ = false;

    private String message;
    private ChatVersion messageVersion = ChatVersion.CHAT3; //how we should talk to this contact

    private CotEvent cotEvent;

    enum ChatVersion {
        GEO_CHAT,
        CHAT3
    }

    /**
     * Method to determine the validity of a chant message
     * @return true if the Chat Message is considered valid
     */
    public boolean isValid() {
        boolean conversationNamePresent = (conversationName != null);
        boolean conversationIdPresent = (conversationId != null
                && !conversationId
                        .trim().isEmpty());
        boolean messageIdPresent = (messageId != null && !messageId.trim()
                .isEmpty());
        boolean protocolPresent = (protocol != null);

        boolean receiveTimePresent = (receiveTime != null);
        boolean sentTimePresent = (sentTime != null);
        boolean timePresent = receiveTimePresent || sentTimePresent;

        boolean senderUidPresent = (senderUid != null);
        boolean senderNamePresent = (senderName != null);

        boolean messagePresent = (message != null);
        boolean destinationsPresent = (!destinations.isEmpty());

        return conversationNamePresent && conversationIdPresent
                && messageIdPresent && protocolPresent && timePresent
                && (senderUidPresent || senderNamePresent) && messagePresent
                && destinationsPresent;
    }

    private void validate() throws InvalidChatMessageException {
        if (isValid())
            return;

        if (conversationName == null || conversationName.isEmpty()) {
            throw new InvalidChatMessageException(
                    "conversationName is not assigned");
        }

        if (conversationId == null || conversationId.isEmpty()) {
            throw new InvalidChatMessageException("conversationId is null");
        }

        if (messageId == null || messageId.isEmpty()) {
            throw new InvalidChatMessageException("messageId is not set");
        }

        if (protocol == null) {
            throw new InvalidChatMessageException("protocol is null");
        }

        if (receiveTime == null && sentTime == null) {
            throw new InvalidChatMessageException(
                    "neither sent nor receive time is set");
        }

        //TODO: look at the version to determine this instead
        if (senderName == null || senderUid == null) {
            throw new InvalidChatMessageException(
                    "neither senderName or senderUid is set");
        }

        if (message == null) {
            throw new InvalidChatMessageException("message is null");
        }

        if (destinations.isEmpty()) {
            throw new InvalidChatMessageException(
                    "no destinations for this message");
        }

    }

    /**
     * Convert the chat message into a bundle for usage.
     * @return a bundle
     * @throws InvalidChatMessageException
     */
    Bundle toBundle() throws InvalidChatMessageException {
        validate();

        final Bundle bundleToReturn = new Bundle();

        bundleToReturn.putString("conversationName", conversationName);
        bundleToReturn.putString("conversationId", conversationId);
        bundleToReturn.putString("messageId", messageId);
        bundleToReturn.putString("protocol", protocol);

        if (receiveTime != null) {
            bundleToReturn
                    .putLong("receiveTime", receiveTime.getMilliseconds());
        }

        if (sentTime != null) {
            bundleToReturn.putLong("sentTime", sentTime.getMilliseconds());
        }

        if (access != null) {
            bundleToReturn.putString("access", access);
        }

        if (caveat != null) {
            bundleToReturn.putString("caveat", caveat);
        }

        if (releasableTo != null) {
            bundleToReturn.putString("releasableTo", releasableTo);
        }

        bundleToReturn.putString("senderUid", senderUid);
        bundleToReturn.putString("senderCallsign", senderName);

        bundleToReturn.putString("parent", parentUid);
        bundleToReturn.putBundle("paths", destinationPaths);
        bundleToReturn.putString("deleteChild", deleteGroup);
        bundleToReturn.putBoolean("groupOwner", groupOwner);
        bundleToReturn.putBoolean("tadilj", tadilJ);

        //TODO: change this
        bundleToReturn.putString("type", messageVersion.name());

        bundleToReturn.putString("message", message);

        if (!destinations.isEmpty())
            bundleToReturn.putStringArray("destinations",
                    destinations.toArray(new String[0]));

        if (cotEvent != null) {
            bundleToReturn.putString("rawCotEvent", cotEvent.toString());
            bundleToReturn.putString("cotEventUid", cotEvent.getUID());
            CotDetail detail = cotEvent.getDetail();
            for (GeoChatService.GeoChatDetailHandler gcdh : GeoChatService
                    .getInstance().getGeoChatDetailHandlers()) {
                CotDetail child = detail.getFirstChildByName(0,
                        gcdh.getDetailName());
                if (child != null)
                    gcdh.toChatBundle(bundleToReturn, cotEvent, child);
            }
        }

        return bundleToReturn;
    }

    /**
     * Sets the conversation identifier
     * @param conversationId the conversation identifier to use.
     */
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    /**
     * Allows for setting of the the identifier associated with the chat message.
     * @param messageId  the message identifier
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * Sets the protocol that was used to deliver this message.
     * @param protocol  the protocol
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    void setReceiveTime(CoordinatedTime receiveTime) {
        this.receiveTime = receiveTime;
    }

    /**
     * Returns the sent time for the message
     * @return the sent time as a coordinated time.
     */
    public CoordinatedTime getSentTime() {
        return sentTime;
    }

    /**
     * Set the sent time for the message.
     * @param sentTime the snt time as a coordinated time
     */
    public void setSentTime(CoordinatedTime sentTime) {
        this.sentTime = sentTime;
    }

    /**
     * Set the sender uid for the message
     * @param senderUid the sender uid
     */
    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    /**
     * Set the destinations used for the chat message
     * @param destinations the destinations
     */
    public void setDestinations(List<String> destinations) {
        this.destinations = new ArrayList<>(destinations);
    }

    /**
     * Set the message
     * @param message the message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    void setParentUid(String uid) {
        this.parentUid = uid;
    }

    public void setPaths(Bundle paths) {
        this.destinationPaths = paths;
    }

    void setMessageVersion(ChatVersion messageVersion) {
        this.messageVersion = messageVersion;
    }

    public void setConversationName(String conversationName) {
        this.conversationName = conversationName;
    }

    void setSenderName(String sender) {
        this.senderName = sender;
    }

    void deleteChild(String uid) {
        deleteGroup = uid;
    }

    void setGroupOwner(boolean isOwner) {
        groupOwner = isOwner;
    }

    void setTadilJ(boolean tadilJ) {
        this.tadilJ = tadilJ;
    }

    /**
     * Set the access control for a chat message as defined by the CoT profile
     * @param access the access control for a message.
     */
    public void setAccess(String access) {
        this.access = access;
    }

    /**
     * Sets the caveat for the chat message as defined by the CoT profile.
     * @param caveat a string that identifies a valid caveat
     */
    public void setCaveat(String caveat) {
        this.caveat = caveat;
    }

    /**
     * Sets the releasableTo for the chat message as defined by the CoT profile.
     * @param releasableTo a string that identifies a valid releasableTo
     */
    public void setReleasableTo(String releasableTo) {
        this.releasableTo = releasableTo;
    }

    /**
     * Sets the raw CoT event that was used for the construction of the message.
     * This event is saved in the database for future reference.
     */
    public void setCotEvent(CotEvent cotDetail) {
        this.cotEvent = cotDetail;
    }

}
