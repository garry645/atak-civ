
package com.atakmap.android.chat;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatLine {

    private static final String TAG = "ChatLine";

    public String conversationId = null;
    public String conversationName = null;
    public String protocol = null;
    public String type = null;
    public Long timeReceived = null;
    public Long timeSent = null;
    public Long timeRead = null;
    public String senderUid = null;
    public String senderName = null;
    public String message = null;
    public String messageId = null;
    public Status status = Status.NONE;
    public CotEvent cotEvent = null;
    public String[] destinations = null;
    public final Bundle extras = new Bundle();
    public String chatGrpName = null;

    // XXX - Deprecate now that "status" field exists?
    public boolean acked = false;
    public transient boolean read = false;

    static class ChatLink {
        final String uid;
        final String name;

        ChatLink(String uid, String name) {
            this.uid = uid;
            this.name = name;
        }
    }

    public enum Status {
        NONE(null),
        DELIVERED("b-t-f-d"),
        READ("b-t-f-r"),
        PENDING("b-t-f-p");

        public final String cotType;

        Status(String cotType) {
            this.cotType = cotType;
        }

        public static Status forCotType(String type) {
            for (Status r : values()) {
                if (r.cotType != null && r.cotType.equals(type))
                    return r;
            }
            return NONE;
        }
    }

    /**
     * Typically, only one of these is set, so get the one that is not null. Returns NULL if both
     * are unset.
     */
    Long getTimeSentOrReceived() {
        return timeSent == null ? timeReceived : timeSent;
    }

    @NonNull
    public String toString() {
        Long time = getTimeSentOrReceived();
        String timestamp = time != null ? ("(" + new Date(time) + ") ") : "";
        return timestamp + senderUid + ": " + message;
    }

    /**
     * @param chatBundle with the following properties: conversationId -> String (reference this ID
     *            to send messages back to sender/grp) conversationName -> String (display name for
     *            conversation) protocol -> String (e.g., xmpp, geochat, etc.) type -> String
     *            (relevant info about the message type) receiveTime -> Long (time message was
     *            received) sendTime -> Long (time message was sent) senderName -> String (display
     *            name for sender) message -> String (text sent as message) messageId -> String
     *            (uuid for this message)
     */
    static ChatLine fromBundle(Bundle chatBundle) {

        ChatLine ret = new ChatLine();
        ret.conversationId = chatBundle.getString("conversationId");
        ret.conversationName = chatBundle.getString("conversationName");
        ret.protocol = chatBundle.getString("protocol");
        ret.type = chatBundle.getString("type");
        ret.timeReceived = getTime(chatBundle, "receiveTime");
        ret.timeSent = getTime(chatBundle, "sentTime");
        ret.timeRead = getTime(chatBundle, "readTime");
        if (ret.timeSent == null && ret.timeReceived == null) //just in case
            ret.timeReceived = new CoordinatedTime().getMilliseconds(); //set it to now just to be on the safe side.
        ret.senderUid = chatBundle.getString("senderUid");
        ret.senderName = chatBundle.getString("senderCallsign");
        ret.message = chatBundle.getString("message");
        ret.messageId = chatBundle.getString("messageId");
        ret.destinations = getDestinations(chatBundle);
        ret.status = getMessageStatus(chatBundle);
        ret.cotEvent = getCotEvent(chatBundle);
        ret.read = chatBundle.getBoolean("read", false);

        if (ret.cotEvent != null) {
            CotDetail detail = ret.cotEvent.getDetail();
            for (GeoChatService.GeoChatDetailHandler gcdh : GeoChatService
                    .getInstance().getGeoChatDetailHandlers()) {
                CotDetail child = detail.getFirstChildByName(0,
                        gcdh.getDetailName());
                if (child != null)
                    gcdh.toChatBundle(ret.extras, ret.cotEvent, child);
            }
        }

        // crazy eights of what this chat is named before the first message comes in
        // never a problem before we looked at historic chats.
        if (ret.cotEvent != null) {
            CotDetail detail = ret.cotEvent.getDetail();
            CotDetail chatdetail = detail.getChild("__chat");
            if (chatdetail != null) {
                ret.chatGrpName = chatdetail.getAttribute("chatroom");
                CotDetail chtgrpdetail = chatdetail.getChild("chatgrp");
                if (chtgrpdetail != null) {
                    String uid0 = chtgrpdetail.getAttribute("uid0");
                    String uid1 = chtgrpdetail.getAttribute("uid1");
                    String self = MapView.getMapView().getSelfMarker().getUID();
                    String id = chtgrpdetail.getAttribute("id");

                    if (self.equals(uid0))
                        ret.chatGrpName = chatdetail.getAttribute("chatroom");
                    else if (self.equals(uid1)) {
                        if (id.equals(uid0))
                            ret.chatGrpName = chatdetail
                                    .getAttribute("chatroom");
                        else if (id.equals(uid1))
                            ret.chatGrpName = chatdetail
                                    .getAttribute("senderCallsign");
                        else
                            ret.chatGrpName = chatdetail
                                    .getAttribute("chatroom");
                    } else {
                        ret.chatGrpName = chatdetail.getAttribute("chatroom");
                    }
                }
            }
        } else {
            ret.chatGrpName = ret.conversationName;
        }
        if (FileSystemUtils.isEmpty(ret.chatGrpName))
            ret.chatGrpName = "<unknown>";

        return ret;
    }

    static List<ChatLink> parseChatLinks(List<String> chatlinks) {
        final List<ChatLink> retval = new ArrayList<>();
        if (chatlinks != null) {
            for (String chatlink : chatlinks) {
                try {
                    final JSONObject jsonObject = new JSONObject(chatlink);
                    final String uid = jsonObject.getString("uid");
                    final String name = jsonObject.getString("name");
                    retval.add(new ChatLink(uid, name));
                } catch (Exception e) {
                    Log.e(TAG, "error parsing: " + chatlink, e);
                }
            }
        }
        return retval;
    }

    private static Status getMessageStatus(Bundle bundle) {
        String status = bundle.getString("status");
        if (FileSystemUtils.isEmpty(status))
            return Status.NONE;
        try {
            return Status.valueOf(status);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse message status: " + status);
            return Status.NONE;
        }
    }

    private static String[] getDestinations(final Bundle chatBundle) {
        try {
            String[] destinations1 = chatBundle.getStringArray("destinations");
            if (destinations1 == null)
                return new String[0];
            else
                return destinations1;
        } catch (ClassCastException e) {
            // possibly legacy?
            String destinationsString = chatBundle.getString("destinations");
            if (destinationsString == null)
                destinationsString = "";
            return destinationsString.split(ChatDatabase.ARRAY_DELIMITER);
        }
    }

    private static CotEvent getCotEvent(final Bundle chatBundle) {
        String rawCotEvent = chatBundle.getString("rawCotEvent");
        if (rawCotEvent == null)
            return null;
        return CotEvent.parse(rawCotEvent);
    }

    /**
     * @return Bundle with the following properties: conversationId -> String (reference this ID to
     *         send messages back to sender/grp) conversationName -> String (display name for
     *         conversation) protocol -> String (e.g., xmpp, geochat, etc.) type -> String (relevant
     *         info about the message type) receiveTime -> Long (time message was received) sendTime
     *         -> Long (time message was sent) senderName -> String (display name for sender)
     *         message -> String (text sent as message) messageId -> String (uuid for this message)
     */
    Bundle toBundle() {
        Bundle ret = new Bundle();
        ret.putString("conversationId", conversationId);
        ret.putString("conversationName", conversationName);
        ret.putString("protocol", protocol);
        ret.putString("type", type);
        ret.putString("messageId", messageId);
        ret.putString("senderUid", senderUid);
        ret.putString("message", message);
        ret.putString("status", status.name());
        setTime(ret, "receiveTime", timeReceived);
        setTime(ret, "sentTime", timeSent);
        setTime(ret, "readTime", timeRead);
        ret.putStringArray("destinations", destinations);
        if (cotEvent != null) {
            ret.putString("rawCotEvent", cotEvent.toString());
            ret.putString("cotEventUid", cotEvent.getUID());
            CotDetail detail = cotEvent.getDetail();
            for (GeoChatService.GeoChatDetailHandler gcdh : GeoChatService
                    .getInstance().getGeoChatDetailHandlers()) {
                CotDetail child = detail.getFirstChildByName(0,
                        gcdh.getDetailName());
                if (child != null)
                    gcdh.toChatBundle(extras, cotEvent, child);
            }
        }
        ret.putAll(extras);
        return ret;
    }

    private static Long getTime(Bundle chatBundle, String key) {
        Long ret = chatBundle.getLong(key, -1);
        if (ret < 0)
            ret = null;
        return ret;
    }

    private static void setTime(Bundle chatBundle, String key, Long value) {
        if (value != null)
            chatBundle.putLong(key, value);
    }

    /**
     * Check if this chat message belongs to the local user
     * @return True if the chat message is from the local user
     */
    boolean isSelfChat() {
        return FileSystemUtils.isEquals(this.senderUid, MapView.getDeviceUid());
    }
}
