
package com.atakmap.android.chat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.atakmap.android.chat.ModePicker.ModeUpdateListener;
import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.contact.FilteredContactsManager;
import com.atakmap.android.contact.IndividualContact;
import com.atakmap.android.hierarchy.action.GoTo;
import com.atakmap.android.hierarchy.items.MapItemUser;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.ILocation;
import com.atakmap.android.maps.MapTouchController;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.metrics.MetricsUtils;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.app.ATAKApplication;
import com.atakmap.app.R;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.map.CameraController;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class ConversationFragment extends Fragment implements
        ModeUpdateListener, View.OnClickListener {

    static private class ButtonHolder {
        public final int index; // Not really used
        public final String text;
        public final String value;

        ButtonHolder(int i, String t, String v) {
            index = i;
            text = t;
            value = v;
        }

        @NonNull
        public String toString() {
            return "Button - index: " + index + " - text: " + text
                    + " - value: " + value;
        }
    }

    private class Mode {
        public final ArrayList<ButtonHolder> buttons;
        public final String name;

        public Mode(String n, ArrayList<ButtonHolder> b) {
            name = n;
            buttons = b;
            buttons.ensureCapacity(8);
        }

        public Mode(String n) {
            name = n;
            buttons = primeList(8);
        }

        @Override
        public int hashCode() {
            int result = ((buttons == null) ? 0 : buttons.hashCode());
            result = 31 * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Mode && ((Mode) o).name.equals(this.name);
        }
    }

    /**Returns the attached ChatLineAdapter
     * Can be @null
     * @return ChatLineAdapter
     */
    public ConversationListAdapter getChatAdapter() {
        return _mChatLineAdapter;
    }

    private ArrayList<ButtonHolder> primeList(int size) {
        ArrayList<ButtonHolder> output = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            output.add(new ButtonHolder(i, "", ""));
        }
        return output;
    }

    private static final String TAG = "ConversationFragment";

    private int editAreaVisibility = View.VISIBLE;
    ListView lineList = null;
    TextView titleText = null;
    MapView mapView = null;
    String targetUID = null;
    EditText inputMessage = null;
    private ConversationListAdapter _mChatLineAdapter;
    private boolean _isGroup = false;
    private TableLayout _table;
    private AtakPreferences _chatPrefs;
    private ModePicker _picker;
    private final List<Mode> modes;

    String title = "";

    private String pendingScrollTo = null;

    public ConversationFragment() {
        modes = initButtons();
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getLineAdapter();
        // Now, add history...
        if (getChatCount() == 0)
            populateHistory();
    }

    private final ChatDatabase.ChatDatabaseListener historyReceiver = new ChatDatabase.ChatDatabaseListener() {
        public void onDatabaseCleared() {
            update();
        }

        @Override
        public void onConversationChanged(String conversationId, Bundle bundle) {
        }

        private void update() {
            getLineAdapter().clearChat();
            populateHistory();
        }
    };

    private ConversationListAdapter getLineAdapter() {
        if (_mChatLineAdapter == null) {
            _mChatLineAdapter = new ConversationListAdapter(MapView
                    .getMapView().getContext());

            ChatDatabase.getInstance(null).registerChatDatabaseListener(historyReceiver);

        }
        // DocumentedIntentFilter for incoming chat messages
        AtakBroadcast.DocumentedIntentFilter contactFilter = new AtakBroadcast.DocumentedIntentFilter();
        contactFilter
                .addAction(FilteredContactsManager.ATAK_FILTER_CONTACT);
        AtakBroadcast.getInstance().registerReceiver(contactFilterReceiver,
                contactFilter);
        return _mChatLineAdapter;
    }

    private final BroadcastReceiver contactFilterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                for (Contact c : Contacts.getInstance().getAllContacts()) {
                    if (c instanceof IndividualContact
                            && c.getName().equals(title))
                        c.setUnreadCount(_mChatLineAdapter.getUnreadCount());
                }
                Contacts.getInstance().updateTotalUnreadCount();
            }
        }
    };

    void scrollToMessage(String messageId) {
        if (_mChatLineAdapter == null || lineList == null) {
            pendingScrollTo = messageId;
        } else if (messageId != null) {
            mapView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < _mChatLineAdapter.getCount(); ++i) {
                        ChatLine line = (ChatLine) _mChatLineAdapter.getItem(i);
                        if (line.messageId.equals(messageId)) {
                            lineList.setSelection(i);
                            break;
                        }
                    }
                }
            }, 30);
        }
    }

    public boolean addOrAckChatLine(ChatLine toAddOrAck) {
        if (!acksAvailable)
            toAddOrAck.acked = true;

        if (isVisible() && !toAddOrAck.read) {
            toAddOrAck.read = true;
            GeoChatService.getInstance().sendReadStatus(toAddOrAck);
        }

        return getLineAdapter().addOrAckChatLine(toAddOrAck);
    }

    public ConversationFragment setIsGroup(boolean isGroup) {
        this._isGroup = isGroup;
        return this;
    }

    public void populateHistory() {
        if (onHistoryRequest != null) {
            List<ChatLine> history = onHistoryRequest.onHistoryRequest();
            for (ChatLine line : history) {
                line.read = true;
                line.acked = false;
                getLineAdapter().addChatLine(line);
            }
        }
    }

    private ChatManagerMapComponent.MessageDestination _destinations = null;

    public ConversationFragment setDests(
            ChatManagerMapComponent.MessageDestination destinations) {
        _destinations = destinations;
        return this;
    }

    public ChatManagerMapComponent.MessageDestination getDests() {
        return _destinations;
    }

    public boolean isGroup() {
        return this._isGroup;
    }

    public ConversationFragment setTitle(String title) {
        this.title = title;
        return this;
    }

    public ConversationFragment setMapView(final MapView mapView) {
        if (this.mapView == null)
            this.mapView = mapView;
        return this;
    }

    public ConversationFragment setTargetUID(final String targetUID) {
        this.targetUID = targetUID;
        return this;
    }

    public MapView getMapView() {
        // ensure that every effort is made to make sure that mapView is
        // not null -- see ATAK-19462
        if (mapView == null)
            mapView = MapView.getMapView();

        return mapView;
    }

    public String getTargetUID() {
        return targetUID;
    }

    public Contact getTarget() {
        return Contacts.getInstance().getContactByUuid(getTargetUID());
    }

    public String getTitle() {
        return title;
    }

    interface SendBehavior {
        void onSend(Bundle chatMessage);
    }

    private SendBehavior onSend = null;

    public ConversationFragment setSendBehavior(SendBehavior onSend) {
        this.onSend = onSend;
        return this;
    }

    interface HistoryBehavior {
        List<ChatLine> onHistoryRequest();
    }

    private HistoryBehavior onHistoryRequest = null;

    public ConversationFragment setHistoryBehavior(
            HistoryBehavior onHistoryRequest) {
        this.onHistoryRequest = onHistoryRequest;
        return this;
    }

    private boolean acksAvailable = false;

    public ConversationFragment setAckEnabled(boolean enabled) {
        acksAvailable = enabled;
        return this;
    }

    public void setUserEntryAreaVisibility(boolean visible) {
        editAreaVisibility = (visible) ? View.VISIBLE : View.GONE;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        getLineAdapter().markAllRead();
        View rootView = inflater.inflate(R.layout.conversation_main, container,
                false);

        lineList = rootView.findViewById(R.id.lineList);
        lineList.setAdapter(getLineAdapter());
        lineList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        lineList.setStackFromBottom(true);

        titleText = rootView
                .findViewById(R.id.conversationTitleText);
        if (titleText != null) {
            titleText.setText(getTitle());
        }

        rootView.findViewById(R.id.chat_user_entry_area).setVisibility(
                editAreaVisibility);

        final ImageButton panTo = rootView
                .findViewById(R.id.conversationPanButton);
        panTo.setOnClickListener(this);

        final Contact target = getTarget();
        panTo.setVisibility(target instanceof MapItemUser
                && ((MapItemUser) target).getMapItem() != null
                || target instanceof ILocation
                || (target != null && target.getAction(GoTo.class) != null)
                        ? View.VISIBLE
                        : View.GONE);

        inputMessage = rootView.findViewById(R.id.messageBox);

        MetricsUtils.attachKeyStrokeListener(MetricsUtils.CATEGORY_CHAT,
                "ConversationFragment", "messageBox", inputMessage);

        ImageButton sendDpButton = rootView.findViewById(R.id.sendDpButton);
        sendDpButton.setOnClickListener(this);
        if (GeoChatService.DEFAULT_CHATROOM_NAME.equals(targetUID))
            sendDpButton.setVisibility(View.GONE);

        Button sendButton = rootView.findViewById(R.id.sendButton);
        sendButton.setOnClickListener(this);

        _chatPrefs = AtakPreferences.getInstance(mapView.getContext());
        _table = rootView.findViewById(R.id.button_table_layout);
        _picker = rootView.findViewById(R.id.mode_picker);
        _picker.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                final int pidx = _picker.getCurrentIndex();

                final Context ctx = ConversationFragment.this.getActivity();
                final AlertDialog.Builder builder = new AlertDialog.Builder(
                        ctx);
                final LayoutInflater inflater = LayoutInflater.from(ctx);
                final View customButton = inflater.inflate(
                        R.layout.custom_button,
                        null);
                builder.setView(customButton);
                builder.setTitle(R.string.chat_text1);
                final EditText buttonName = customButton
                        .findViewById(R.id.buttonName);
                buttonName.setFilters(new InputFilter[] {
                        new InputFilter.LengthFilter(6)
                });

                buttonName.setText(_chatPrefs.get("chatModeName" + pidx,
                        modes.get(pidx).name));
                customButton.findViewById(R.id.buttonValueField)
                        .setVisibility(View.GONE);
                builder.setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface,
                                    int i) {
                                final String name = buttonName.getText()
                                        .toString();
                                _chatPrefs.set("chatModeName" + pidx, name);
                                _picker.setValues(parseModes());
                                _picker.setIndex(pidx);
                            }
                        });
                builder.setNegativeButton(R.string.cancel, null);
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            }
        });

        String[] modeNames = parseModes();
        if (modeNames != null) {
            _picker.setValues(modeNames);
            this.onModeUpdate(
                    modeNames[_chatPrefs.get("lastChatModeIndex", 0)]);
            _picker.setOnModeUpdateListener(this);
        }

        int rowCount = 1;
        int btnCount = 4;

        for (int r = 0; r < rowCount; r++) {
            TableRow row = (TableRow) _table.getChildAt(r);
            for (int i = 0; i < btnCount; i++) {
                final Button btn = (Button) row.getChildAt(i);
                btn.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // When using quick keys, just add in a " " at the end to provide the user the ability
                        // to either add another quick key or start typing without hitting the space bar.
                        if (btn.getTag() != null)
                            inputMessage.append(btn.getTag() + " ");
                        v.setPressed(false);

                        Bundle b = new Bundle();
                        b.putString(MetricsUtils.FIELD_SELECTED,
                                "" + btn.getTag());
                        b.putString(MetricsUtils.FIELD_RESULTING_MESSAGE,
                                inputMessage.getText().toString());
                        MetricsUtils.record(MetricsUtils.CATEGORY_CHAT,
                                MetricsUtils.EVENT_CHAT_PREDEFINED_SELECTED,
                                "quickPreset", b);
                    }
                });
                btn.setHint(String.valueOf(r * 4 + i)); // We'll use the hint as a way to store the
                                                        // buttons index
                btn.setOnLongClickListener(new PresetButtonEditor(btn));
            }
        }

        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction("com.atakmap.app.PREFERENCES_LOADED");

        rootView.setBackgroundColor(Color.BLACK);

        scrollToMessage(pendingScrollTo);
        pendingScrollTo = null;

        return rootView;

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        // Pan to contact
        if (id == R.id.conversationPanButton) {
            Contact target = getTarget();
            if (target instanceof GoTo)
                ((GoTo) target).goTo(false);
            else if (target instanceof MapItemUser)
                MapTouchController.goTo(((MapItemUser) target).getMapItem(),
                        false);
            else if (target instanceof ILocation) {
                GeoPoint point = ((ILocation) target).getPoint(null);
                MapView mv = getMapView();
                if (point.isValid() && mv != null) {
                    CameraController.Programmatic.panTo(
                            mv.getRenderer3(),
                            point, false);
                }
            }
        }

        else if (id == R.id.sendDpButton) {
            final List<Contact> contacts = _destinations.getDestinations();
            final List<Contact> recipients = new ArrayList<>();
            for (Contact c : contacts) {
                if (c != null) {
                    final List<Contact> filtered = c.getFiltered(true, true);
                    if (filtered != null) {
                        for (Contact c2 : filtered) {
                            if (c2 instanceof IndividualContact)
                                recipients.add(c2);
                        }
                    }
                } else {
                    Log.d(TAG, "contact was null for: " + _destinations);
                }

                ChatDataPackageService.getInstance(mapView).sendDataPackage(
                        recipients,
                        new ChatDataPackageService.ChatDataPackageCallback() {
                            @Override
                            public void onDataSendComplete(
                                    @NonNull List<String> uids,
                                    @NonNull List<String> files,
                                    boolean success) {

                                final MapView mv = mapView;
                                if (mv == null)
                                    return;

                                final Context ctx = mv.getContext();
                                if (ctx == null)
                                    return;

                                int count = uids.size() + files.size();
                                ArrayList<String> chatlinks = new ArrayList<>();

                                for (String uid : uids) {
                                    try {
                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("uid", uid);
                                        jsonObject.put("name",
                                                ATAKUtilities.getDisplayName(
                                                        mv.getMapItem(uid)));
                                        chatlinks.add(jsonObject.toString());
                                    } catch (JSONException ignored) {
                                    }
                                }
                                for (String file : files) {
                                    try {
                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("uid", "");
                                        jsonObject.put("name",
                                                new File(file).getName());
                                        chatlinks.add(jsonObject.toString());
                                    } catch (JSONException ignored) {
                                    }
                                }

                                final Bundle bundle = new Bundle();
                                if (!chatlinks.isEmpty()) {
                                    bundle.putStringArrayList("chatlinks",
                                            chatlinks);
                                }

                                bundle.putStringArray("plugin_chatlinks_uid",
                                        uids.toArray(new String[0]));
                                bundle.putStringArray("plugin_chatlinks_files",
                                        files.toArray(new String[0]));

                                if (count == 1) {
                                    sendMessage(ctx.getString(
                                            R.string.chat_datapackage_one),
                                            bundle);
                                } else {
                                    sendMessage(ctx.getString(
                                            R.string.sent_you_a_datapackage_with_items,
                                            count), bundle);
                                }

                            }
                        });
            }

            // Send message
        } else if (id == R.id.sendButton) {
            // trim any trailing spaces
            String msg = inputMessage.getText().toString();

            // trim any trailing spaces
            try {
                msg = msg.replaceFirst("\\s++$", "");
            } catch (Exception ignored) {
            }

            // Ignore empty message
            if (msg.isEmpty()) {
                Bundle b = new Bundle();
                b.putString(MetricsUtils.FIELD_STATUS,
                        MetricsUtils.EVENT_STATUS_FAILED);
                b.putString(MetricsUtils.FIELD_REASON,
                        MetricsUtils.REASON_NO_MESSAGE);
                MetricsUtils.record(MetricsUtils.CATEGORY_CHAT,
                        MetricsUtils.EVENT_CHAT_MESSAGE,
                        "sendButton", b);
                return;
            }

            sendMessage(msg, null);
        }
    }

    private void sendMessage(String msg, Bundle additionalData) {
        // Check if the message has anywhere to go
        final List<Contact> contacts = _destinations.getDestinations();
        final List<Contact> recipients = new ArrayList<>();
        for (Contact c : contacts) {
            if (c != null) {
                final List<Contact> filtered = c.getFiltered(true, true);
                if (filtered != null) {
                    for (Contact c2 : filtered) {
                        if (c2 instanceof IndividualContact)
                            recipients.add(c2);
                    }
                }
            } else {
                Log.d(TAG, "contact was null for: " + _destinations);
            }
        }

        // Nobody to send it to
        if (recipients.isEmpty()) {
            Bundle b = new Bundle();
            b.putString(MetricsUtils.FIELD_STATUS,
                    MetricsUtils.EVENT_STATUS_FAILED);
            b.putString(MetricsUtils.FIELD_REASON,
                    MetricsUtils.REASON_NO_RECIPIENT);
            MetricsUtils.record(MetricsUtils.CATEGORY_CHAT,
                    MetricsUtils.EVENT_CHAT_MESSAGE,
                    "sendButton", b);

            final MapView mv = mapView;
            if (mv == null)
                return;

            final Context ctx = mv.getContext();
            if (ctx == null)
                return;

            Toast.makeText(ctx,
                    R.string.chat_message_no_recipients,
                    Toast.LENGTH_LONG).show();
            return;
        }

        ChatLine toAdd = new ChatLine();
        toAdd.messageId = UUID.randomUUID().toString();
        toAdd.timeSent = (new CoordinatedTime()).getMilliseconds();
        toAdd.senderUid = MapView.getDeviceUid();
        toAdd.message = msg;
        final Bundle arguments = getArguments();
        String conversationId = arguments != null
                ? arguments.getString("id")
                : null;
        toAdd.conversationId = (conversationId != null) ? conversationId
                : "";
        toAdd.conversationName = getTitle();
        if (additionalData != null)
            toAdd.extras.putAll(additionalData);

        if (onSend != null) {
            Bundle b = toAdd.toBundle();
            onSend.onSend(b);

            MetricsUtils.record(MetricsUtils.CATEGORY_CHAT,
                    MetricsUtils.EVENT_CHAT_SENT,
                    "sendButton", b);
        } else {
            Log.w(TAG,
                    "No service available to send outgoing chat message");
        }

        addOrAckChatLine(toAdd);
        inputMessage.setText("");
    }

    private String[] parseModes() {
        String[] modeNames = new String[modes.size()];
        for (int i = 0; i < modes.size(); ++i)
            modeNames[i] = _chatPrefs.get("chatModeName" + i,
                    modes.get(i).name);
        return modeNames;
    }

    private List<Mode> initButtons() {
        final Activity activity = ATAKApplication.getCurrentActivity();

        final List<Mode> modes = new ArrayList<>();

        if (activity != null) {
            final XmlResourceParser chat_modes = activity.getResources()
                    .getXml(R.xml.chat_modes);
            try {
                int eventType = chat_modes.getEventType();
                int modeIndex = -1;

                ArrayList<ButtonHolder> output = null;
                String modeName = null;

                while (eventType != XmlResourceParser.END_DOCUMENT) {
                    if (eventType == XmlResourceParser.START_TAG) {
                        if (chat_modes.getName().equals("mode")) {
                            modeName = chat_modes.getAttributeValue(null,
                                    "name");
                            output = primeList(4);
                            modeIndex = Integer.parseInt(chat_modes
                                    .getAttributeValue(null, "index"));
                        } else if (chat_modes.getName().equals("button")
                                && output != null) {
                            // Log.e(TAG, "Found button");
                            int buttonIndex = Integer.parseInt(chat_modes
                                    .getAttributeValue(null,
                                            "index"));
                            output.set(buttonIndex,
                                    new ButtonHolder(buttonIndex, chat_modes
                                            .getAttributeValue(null,
                                                    "label"),
                                            chat_modes
                                                    .getAttributeValue(null,
                                                            "value")));
                        }
                    } else if (eventType == XmlResourceParser.END_TAG) {
                        // Log.e(TAG, "End of element: " + chat_modes.getName());
                        if (chat_modes.getName().equals("mode")) {
                            modes.add(modeIndex, new Mode(modeName, output));
                            modeName = null;
                            output = null;
                            modeIndex = -1;
                        }
                    }
                    eventType = chat_modes.next();
                }
            } catch (Exception e) {
                Log.e(TAG, "encountered error", e);
            } finally {
                if (chat_modes != null)
                    chat_modes.close();
            }
        }
        return modes;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        AtakBroadcast.getInstance().unregisterReceiver(contactFilterReceiver);

    }

    private boolean skipUpdate = false;

    @Override
    public void onModeUpdate(String modeName) {
        if (skipUpdate)
            return;

        MetricsUtils.record(MetricsUtils.CATEGORY_CHAT,
                MetricsUtils.EVENT_CHAT_MODE_CHANGED,
                "ConversationFragment", modeName);

        int modeIndex = -1;

        Iterator<ButtonHolder> buttons = null;
        modeIndex = _picker.getIndex(modeName);

        if (modeIndex < 0)
            return;

        Mode mode = modes.get(modeIndex);
        buttons = mode.buttons.iterator();
        _chatPrefs.set("lastChatModeIndex", modeIndex);

        skipUpdate = true;
        if (_picker.getCurrentIndex() != modeIndex)
            _picker.setIndex(modeIndex);
        skipUpdate = false;

        if (buttons.hasNext()) {
            for (int r = 0; r < _table.getChildCount(); r++) {
                TableRow row = (TableRow) _table.getChildAt(r);
                for (int b = 0; b < row.getChildCount(); b++) {
                    Button btn = (Button) row.getChildAt(b);
                    int pos = r * row.getChildCount() + b;
                    if (buttons.hasNext()) {
                        ButtonHolder button = buttons.next();
                        String text = _chatPrefs.get(
                                "btnText" + modeIndex + "" + pos, button.text);
                        String msg = _chatPrefs.get(
                                "btnMsg" + modeIndex + "" + pos, button.value);
                        btn.setText(text);
                        btn.setTag(msg);
                    } else {
                        btn.setText(" ");
                        btn.setTag("");
                    }
                }
            }
        }

    }

    public int getChatCount() {
        int count = 0;
        if (_mChatLineAdapter != null)
            count = _mChatLineAdapter.getCount();
        return count;
    }

    public int getUnreadCount() {
        int unreadCount = 0;

        if (_mChatLineAdapter != null) {
            unreadCount = _mChatLineAdapter.getUnreadCount();
            Log.d(TAG, "size: " + _mChatLineAdapter.getCount());
        }
        return unreadCount;
    }

    public void removeLastChatLine() {
        if (_mChatLineAdapter != null)
            _mChatLineAdapter.removeLastChatLine();
    }

    //Need to know when the list is created because of how "read" chat lines are implemented
    private final List<ChatConvoFragCreateWatcher> watcherList = new ArrayList<>();

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        while (!watcherList.isEmpty()) {
            final ChatConvoFragCreateWatcher watcher = watcherList.remove(0);
            // the next call will eventually remove the watcher from
            // the list.
            watcher.onChatConvoFragCreated(this);
        }
    }

    public void addChatConvoFragCreateWatcher(
            ChatConvoFragCreateWatcher watcher) {
        if (!watcherList.contains(watcher))
            watcherList.add(watcher);
    }

    public void removeChatConvoFragCreateWatcher(
            ChatConvoFragCreateWatcher watcher) {
        watcherList.remove(watcher);
    }

    private class PresetButtonEditor implements OnLongClickListener {

        private final Button btn;

        public PresetButtonEditor(Button btn) {
            this.btn = btn;
        }

        @Override
        public boolean onLongClick(View arg0) {
            final Context ctx = ConversationFragment.this.getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            final LayoutInflater inflater = LayoutInflater.from(ctx);
            final View customButton = inflater.inflate(R.layout.custom_button,
                    null);
            builder.setView(customButton);
            builder.setTitle(R.string.chat_text1);
            final EditText buttonName = customButton
                    .findViewById(R.id.buttonName);

            final EditText buttonValue = customButton
                    .findViewById(R.id.buttonValue);

            final int pidx = _picker.getCurrentIndex();

            buttonName.setText(btn.getText());

            final String valueString = modes.get(pidx).buttons
                    .get(Integer.parseInt(
                            (String) btn.getHint())).value;
            if (valueString != null)
                buttonValue.setText(valueString);

            builder.setPositiveButton(R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface,
                                int i) {

                            final String text = buttonName.getText().toString();
                            final String msg = buttonValue.getText().toString();
                            final String btnLoc = (String) btn.getHint();

                            if (!text.isEmpty() && !msg.isEmpty()) {

                                _chatPrefs.set("btnText" + pidx + "" + btnLoc,
                                        text);
                                _chatPrefs.set("btnMsg" + pidx + "" + btnLoc,
                                        msg);
                                btn.setText(text);
                                btn.setTag(msg);
                                modes.get(pidx).buttons.set(
                                        Integer.parseInt(btnLoc),
                                        new ButtonHolder(
                                                Integer.parseInt(btnLoc),
                                                text,
                                                msg));
                            } else {
                                // display error prompt here
                                AlertDialog.Builder builder2 = new AlertDialog.Builder(
                                        ctx);
                                View errorView = inflater
                                        .inflate(R.layout.custom_button_error,
                                                null);
                                builder2.setView(errorView);
                                builder2.setTitle(R.string.chat_text2);
                                builder2.setPositiveButton(R.string.ok, null);
                                final AlertDialog dialog2 = builder2.create();
                                dialog2.show();
                            }
                        }
                    });
            builder.setNegativeButton(R.string.cancel, null);
            final AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        }

    }
}
