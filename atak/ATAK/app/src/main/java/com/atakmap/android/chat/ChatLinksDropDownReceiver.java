
package com.atakmap.android.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.menu.MapMenuReceiver;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.AttachmentManager;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.util.ArrayList;
import java.util.List;

class ChatLinksDropDownReceiver extends DropDownReceiver
        implements DropDown.OnStateListener {

    private static final String TAG = "ChatLinksDropDownReceiver";

    static final String CHAT_MORE_DETAILS = "com.atakmap.android.chat.CHAT_LINKS_DETAIL_VIEW";
    private final View view;
    private final ListView listView;
    private final TextView conversationName;
    private final TextView numberOfLinks;

    protected ChatLinksDropDownReceiver(MapView mapView) {
        super(mapView);
        view = LayoutInflater.from(mapView.getContext())
                .inflate(R.layout.chat_links_view, null);
        listView = view.findViewById(R.id.chat_links);
        conversationName = view.findViewById(R.id.conversation_name);
        numberOfLinks = view.findViewById(R.id.number_links);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (!isClosed())
            return;

        if (action == null)
            return;

        final Bundle bundle = intent.getBundleExtra("extras");
        if (bundle == null)
            return;

        List<String> chatlinks = bundle.getStringArrayList("chatlinks");
        ArrayList<ChatLine.ChatLink> links = new ArrayList<>(
                ChatLine.parseChatLinks(chatlinks));
        if (links.isEmpty())
            return;

        listView.setAdapter(new ChatLinksAdapter(getMapView(), context, links));

        final String name = intent.getStringExtra("conversationName");
        conversationName.setText(
                name == null ? context.getString(R.string.unknown_brackets)
                        : name);

        numberOfLinks.setText(
                context.getString(R.string.item_count_colon, links.size()));

        showDropDown(view, DropDownReceiver.HALF_WIDTH,
                DropDownReceiver.FULL_HEIGHT,
                DropDownReceiver.FULL_WIDTH, DropDownReceiver.HALF_HEIGHT,
                this);
        setRetain(true);

    }

    @Override
    public void onDropDownSelectionRemoved() {

    }

    @Override
    public void onDropDownClose() {

    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {

    }

    @Override
    public void onDropDownVisible(boolean v) {

    }

    @Override
    protected void disposeImpl() {

    }

    public static class ChatLinksAdapter
            extends ArrayAdapter<ChatLine.ChatLink> {
        private final MapView mapView;

        public ChatLinksAdapter(MapView mapView, Context context,
                ArrayList<ChatLine.ChatLink> users) {
            super(context, 0, users);
            this.mapView = mapView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            ChatLine.ChatLink item = getItem(position);

            ViewHolder holder = null;
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.chat_link_item, parent, false);

            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            if (holder == null) {
                holder = new ViewHolder();
                // Lookup view for data population
                holder.item_radial = convertView.findViewById(R.id.item_radial);
                holder.item_name = convertView.findViewById(R.id.item_name);
                holder.item_panto = convertView.findViewById(R.id.item_panto);
                holder.item_attachment = convertView
                        .findViewById(R.id.item_attachment);
                holder.attachmentManager = new AttachmentManager(mapView,
                        holder.item_attachment);
                convertView.setTag(holder);
            }
            if (item == null) {
                convertView.setVisibility(View.GONE);
                return convertView;
            } else {
                convertView.setVisibility(View.VISIBLE);
            }

            if (!FileSystemUtils.isEmpty(item.uid)) {
                MapItem mi = mapView.getMapItem(item.uid);
                if (mi == null) {
                    holder.item_name.setText(R.string.item_deleted_or_missing);
                    holder.item_panto.setEnabled(false);
                    holder.item_attachment.setVisibility(View.INVISIBLE);
                    holder.item_radial.setVisibility(View.INVISIBLE);
                } else {
                    holder.item_radial.setVisibility(View.VISIBLE);

                    holder.item_name.setText(ATAKUtilities.getDisplayName(mi));

                    holder.item_radial
                            .setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    AtakBroadcast.getInstance()
                                            .sendBroadcast(new Intent(
                                                    MapMenuReceiver.HIDE_MENU));
                                    ATAKUtilities.scaleToFit(mi);

                                    //display details and break cam lock
                                    Intent intent = new Intent();
                                    intent.setAction(
                                            "com.atakmap.android.maps.SHOW_DETAILS");
                                    intent.putExtra("uid", mi.getUID());
                                    AtakBroadcast.getInstance()
                                            .sendBroadcast(intent);

                                    intent = new Intent();
                                    intent.setAction(
                                            "com.atakmap.android.maps.SHOW_MENU");
                                    intent.putExtra("uid", mi.getUID());
                                    AtakBroadcast.getInstance()
                                            .sendBroadcast(intent);

                                }
                            });

                    holder.item_panto.setEnabled(true);
                    holder.item_panto
                            .setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    AtakBroadcast.getInstance()
                                            .sendBroadcast(new Intent(
                                                    MapMenuReceiver.HIDE_MENU));
                                    ATAKUtilities.scaleToFit(mi);
                                }
                            });

                    if (AttachmentManager
                            .getNumberOfAttachments(item.uid) > 0) {
                        holder.attachmentManager.setMapItem(mi);
                        holder.item_attachment.setVisibility(View.VISIBLE);
                    } else {
                        holder.item_attachment.setVisibility(View.INVISIBLE);
                    }
                }
            } else {
                holder.item_name.setText(item.name);
                holder.item_panto.setEnabled(false);
                holder.item_attachment.setVisibility(View.INVISIBLE);
                holder.item_radial.setVisibility(View.INVISIBLE);

            }
            // Return the completed view to render on screen
            return convertView;
        }
    }

    private static class ViewHolder {
        TextView item_name;
        ImageButton item_radial;
        ImageButton item_attachment;
        ImageButton item_panto;
        AttachmentManager attachmentManager;
    }
}
