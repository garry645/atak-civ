
package com.atakmap.android.chat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.contact.Connector;
import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.contact.GroupContact;
import com.atakmap.android.contact.IndividualContact;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.tools.AtakLayerDrawableUtil;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.AfterTextChangedWatcher;
import com.atakmap.app.R;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

class ChatNextDropDownReceiver extends DropDownReceiver implements
        OnStateListener, ChatDatabase.ChatDatabaseListener {

    public static final String TAG = "ChatNextDropDownReceiver";

    public static final String SHOW = "com.atakmap.android.chat.CHAT_NEXT";

    private static final int MINUTES_IN_A_DAY = 1440;

    private final View view;
    private final ImageButton searchButton;
    private final EditText searchLine;
    private final TextView messageTitle;
    private final ConversationListAdapter cla;

    private final ChatDatabase chatDb;

    private final ArrayList<Conversation> conversations = new ArrayList<>();

    static class Conversation {
        String chatid;
        long time;
        ChatLine chat;

        public Conversation(String id, Bundle bundle) {
            chat = ChatLine.fromBundle(bundle);
            chatid = id;
            time = max(chat);
        }

        static long max(ChatLine chat) {
            long time;
            if (chat.timeReceived != null && chat.timeSent != null)
                time = Math.max(chat.timeReceived, chat.timeSent);
            else if (chat.timeReceived != null)
                time = chat.timeReceived;
            else if (chat.timeSent != null)
                time = chat.timeSent;
            else
                time = -1;
            return time;
        }
    }

    ChatNextDropDownReceiver(final MapView mapView) {
        super(mapView);
        chatDb = ChatDatabase.getInstance(mapView.getContext());
        view = PluginLayoutInflater.inflate(mapView.getContext(),
                R.layout.chatnext_layout, null);

        ListView listview = view.findViewById(R.id.chatnext_conversations);
        messageTitle = view.findViewById(R.id.chatnext_messages_title);

        searchLine = view.findViewById(R.id.chatnext_search_line);
        searchButton = view.findViewById(R.id.chatnext_search_btn);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (searchButton.getVisibility() == View.VISIBLE) {
                    searchButton.setVisibility(View.GONE);
                    messageTitle.setVisibility(View.GONE);
                    searchLine.setVisibility(View.VISIBLE);
                    searchLine.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getMapView()
                            .getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(searchLine,
                            InputMethodManager.SHOW_IMPLICIT);
                    cla.getFilter().filter(searchLine.getText());
                }
            }
        });

        searchLine.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                cla.getFilter().filter(s);
            }
        });

        ImageButton addButton = view.findViewById(R.id.chatnext_add_btn);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(
                        "com.atakmap.android.contact.CONTACT_LIST");
                AtakBroadcast.getInstance().sendBroadcast(i);
            }
        });

        cla = new ConversationListAdapter(mapView, mapView.getContext(),
                conversations);
        listview.setAdapter(cla);

        chatDb.registerChatDatabaseListener(this);

    }

    /**************************** PUBLIC METHODS *****************************/

    public void disposeImpl() {
        chatDb.unregisterChatDatabaseListener(this);
    }

    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();
        if (action == null)
            return;

        final List<String> ids = chatDb.getAvailableConversations();

        conversations.clear();

        for (String id : ids) {
            List<Bundle> lastChat = chatDb.getHistory(id, 1, false);
            if (lastChat.isEmpty())
                continue;
            Conversation conversation = new Conversation(id, lastChat.get(0));
            conversations.add(conversation);
        }

        Collections.sort(conversations, TIME_COMPARATOR);
        cla.getFilter().filter("");

        if (action.equals(SHOW)) {
            showDropDown(view, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false, this);
            setRetain(true);
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    protected boolean onBackButtonPressed() {
        if (searchButton.getVisibility() == View.GONE) {
            searchButton.setVisibility(View.VISIBLE);
            messageTitle.setVisibility(View.VISIBLE);
            searchLine.setVisibility(View.GONE);
            cla.getFilter().filter("");

            return true;
        }

        return super.onBackButtonPressed();
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }

    @Override
    public void onDatabaseCleared() {
        getMapView().post(new Runnable() {
            @Override
            public void run() {
                conversations.clear();
                cla.notifyDataSetChanged();
            }
        });

    }

    @Override
    public void onConversationChanged(String conversationId, Bundle bundle) {
        final List<Conversation> tmp = new ArrayList<>(conversations);
        boolean found = false;
        for (Conversation c : tmp) {
            if (c.chatid.equals(conversationId)) {
                c.chat = ChatLine.fromBundle(bundle);
                c.time = Conversation.max(c.chat);
                found = true;
            }
        }
        if (!found)
            tmp.add(new Conversation(conversationId, bundle));

        Collections.sort(tmp, TIME_COMPARATOR);

        getMapView().post(new Runnable() {
            @Override
            public void run() {
                conversations.clear();
                conversations.addAll(tmp);
                if (searchButton.getVisibility() == View.VISIBLE)
                    cla.getFilter().filter(searchLine.getText());
                else
                    cla.getFilter().filter("");

                cla.notifyDataSetChanged();
            }
        });
    }

    static class ConversationListAdapter extends ArrayAdapter<Conversation>
            implements Filterable {
        private final MapView mapView;

        private final List<Conversation> mOriginalValues;
        private final List<Conversation> mObjects;
        private Filter mFilter;

        public ConversationListAdapter(MapView mapView, Context context,
                ArrayList<Conversation> conversations) {
            super(context, 0);
            this.mapView = mapView;
            mOriginalValues = conversations;
            mObjects = new ArrayList<>(conversations);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView,
                @NonNull ViewGroup parent) {
            Conversation item = getItem(position);

            ViewHolder holder = null;
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.chatnext_line, parent, false);

            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            if (holder == null) {
                holder = new ViewHolder();
                // Lookup view for data population
                holder.name = convertView.findViewById(R.id.chatnext_name);
                holder.date = convertView.findViewById(R.id.chatnext_date);
                holder.last_message = convertView
                        .findViewById(R.id.chatnext_message);
                holder.icon = convertView.findViewById(R.id.chatnext_icon);
            }

            if (item == null) {
                convertView.setVisibility(View.GONE);
                return convertView;
            } else
                convertView.setVisibility(View.VISIBLE);

            holder.name.setText(item.chat.chatGrpName);
            holder.date.setText(format(item.time));
            holder.last_message.setText(item.chat.message);
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ChatManagerMapComponent.getInstance().openConversation(
                            item.chatid, item.chat.chatGrpName, item.chatid,
                            true,
                            new ChatManagerMapComponent.MessageDestination(
                                    item.chatid));
                }
            });

            Contact contact = Contacts.getInstance()
                    .getContactByUuid(item.chatid);

            updateHolderBasedOnConnectionStatus(mapView, holder.icon, contact);
            return convertView;
        }

        @Nullable
        @Override
        public Conversation getItem(int position) {
            return mObjects.get(position);
        }

        @Override
        public int getCount() {
            return mObjects.size();
        }

        @NonNull
        @Override
        public Filter getFilter() {
            if (mFilter == null)
                mFilter = new ConversationFilter();
            return mFilter;
        }

        private class ConversationFilter extends Filter {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                final FilterResults results = new FilterResults();

                if (constraint == null || constraint.length() == 0) {
                    ArrayList<Conversation> list = new ArrayList<>(
                            mOriginalValues);
                    results.values = list;
                    results.count = list.size();
                } else {
                    ArrayList<Conversation> newValues = new ArrayList<>();
                    for (int i = 0; i < mOriginalValues.size(); i++) {
                        Conversation item = mOriginalValues.get(i);
                        final String lcConstraint = ((String) constraint)
                                .toLowerCase(LocaleUtil.getCurrent());
                        final String lcChatGrp = item.chat.chatGrpName
                                .toLowerCase(LocaleUtil.getCurrent());
                        final String lcMessage = item.chat.message
                                .toLowerCase(LocaleUtil.getCurrent());

                        if (lcMessage.contains(lcConstraint)
                                || lcChatGrp.contains(lcConstraint)) {
                            newValues.add(item);
                        }
                    }
                    results.values = newValues;
                    results.count = newValues.size();
                }

                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint,
                    FilterResults results) {
                mObjects.clear();
                mObjects.addAll((List<Conversation>) results.values);
                notifyDataSetChanged();
            }
        }
    }

    private static class ViewHolder {
        TextView name;
        TextView date;
        TextView last_message;
        ImageView icon;
    }

    private final Comparator<Conversation> TIME_COMPARATOR = new Comparator<Conversation>() {
        @Override
        public int compare(Conversation o1, Conversation o2) {
            return Long.compare(o2.time, o1.time);
        }
    };

    private static String format(long time) {
        long currTime = new CoordinatedTime().getMilliseconds();
        long minutes = (currTime - time) / 60000;
        if (minutes < 60) {
            return minutes + "m";
        } else if (minutes < MINUTES_IN_A_DAY) {
            return sdft_hh_mm.format(new Date(time));
        } else {
            return sdft_dd_MMM.format(new Date(time));
        }
    }

    private final static CoordinatedTime.SimpleDateFormatThread sdft_dd_MMM = new CoordinatedTime.SimpleDateFormatThread(
            "dd MMM");

    private final static CoordinatedTime.SimpleDateFormatThread sdft_hh_mm = new CoordinatedTime.SimpleDateFormatThread(
            "hh:mm");

    private static void updateHolderBasedOnConnectionStatus(MapView mapView,
            ImageView view,
            Contact contact) {
        if (contact == null) {
            String uri = "android.resource://"
                    + MapView.getMapView().getContext().getPackageName()
                    + "/" + R.drawable.team_human;
            ATAKUtilities.SetIcon(mapView.getContext(), view,
                    uri, Color.GRAY);
            view.setVisibility(View.VISIBLE);
            return;
        }

        // Read from contact properties
        view.setVisibility(
                contact.getUpdateStatus() == null ? View.GONE : View.VISIBLE);

        view.setColorFilter(contact.getIconColor(), PorterDuff.Mode.MULTIPLY);

        ATAKUtilities.SetIcon(mapView.getContext(), view,
                contact.getIconUri(), contact.getIconColor());

        // Set the icon
        // If a Drawable icon is not available then the URI is used as fallback
        // If "gone" is specified for the URI then the icon space is removed
        Drawable iconDr;
        String iconUri = contact.getIconUri();
        if ((iconDr = contact.getIconDrawable()) != null) {
            view.setImageDrawable(iconDr);
            view.setColorFilter(contact.getIconColor(),
                    PorterDuff.Mode.MULTIPLY);
            view.setVisibility(View.VISIBLE);
        } else if (iconUri != null && iconUri.equals("gone"))
            view.setVisibility(View.GONE);
        else
            ATAKUtilities.SetIcon(mapView.getContext(), view,
                    contact.getIconUri(), contact.getIconColor());

        //now overlay presence if available
        LayerDrawable ld = (LayerDrawable) mapView.getContext().getResources()
                .getDrawable(R.drawable.details_badge);
        if (ld != null) {
            int unread = 0;
            if (contact instanceof GroupContact) {
                unread = contact.getExtras().getInt("unreadMessageCount", 0);
            } else if (contact instanceof IndividualContact) {
                Connector c = ((IndividualContact) contact)
                        .getConnector(GeoChatConnector.CONNECTOR_TYPE);
                unread = ((IndividualContact) contact).getUnreadCount(c);
            }
            AtakLayerDrawableUtil.getInstance(mapView.getContext())
                    .setBadgeCount(ld,
                            view.getDrawable(), unread,
                            contact.getPresenceColor());
            view.setImageDrawable(ld);
        }
    }
}
