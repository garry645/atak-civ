
package com.atakmap.android.chat;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public interface ChatMessageRenderer2 {

    /**
     * An implementation that is provided that will take a chat message and provide for a visual
     * representation of that message.
     * @param position The index in the chatLines adapter for the message to render
     * @param convertView The old view to reuse, if possible. Note: You should check that this view
     *        is non-null and of an appropriate type before using. If it is not possible to convert
     *        this view to display the correct data, this method can create a new view.
     *        Heterogeneous lists can specify their number of view types, so that this View is}).
     * @param parent The parent that this view will eventually be attached to
     * @param chatLines the collection of chat lines
     * @param latestSelfMsg latest sent message in a thread
     * @param latestSelfChain latest message in a thread
     * @return A view corresponding to the data at the specified position.
     */
    View getView(int position, View convertView, ViewGroup parent,
            List<ChatLine> chatLines, ChatLine latestSelfMsg,
            ChatLine latestSelfChain);
}
