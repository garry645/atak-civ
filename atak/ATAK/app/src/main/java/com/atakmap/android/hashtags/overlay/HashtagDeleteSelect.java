
package com.atakmap.android.hashtags.overlay;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import com.atakmap.android.hashtags.HashtagContent;
import com.atakmap.android.hierarchy.HierarchyListItem;
import com.atakmap.android.hierarchy.HierarchyListReceiver;
import com.atakmap.android.hierarchy.HierarchyListUserSelect;
import com.atakmap.android.hierarchy.items.AbstractHierarchyListItem2;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class HashtagDeleteSelect extends HierarchyListUserSelect {

    private static final String TAG = "HashtagListUserSelect";

    private final Context _context;

    public HashtagDeleteSelect(MapView mapView) {
        super(TAG, 0L);
        _context = mapView.getContext();
    }

    @Override
    public String getTitle() {
        return _context.getString(R.string.remove_hashtag);
    }

    @Override
    public String getButtonText() {
        return _context.getString(R.string.ok);
    }

    @Override
    public ButtonMode getButtonMode() {
        return ButtonMode.VISIBLE_WHEN_SELECTED;
    }

    @Override
    public boolean processUserSelections(Context context,
            Set<HierarchyListItem> selected) {

        String tag = getTag();
        if (FileSystemUtils.isEmpty(tag))
            return false;

        List<Pair<String, HashtagContent>> contents = new ArrayList<>();
        for (HierarchyListItem item : selected) {
            contents.addAll(getContents(tag, item));
        }
        for (Pair<String, HashtagContent> content : contents) {
            Collection<String> tags = content.second.getHashtags();
            tags.remove(content.first);
            content.second.setHashtags(tags);
        }

        AtakBroadcast.getInstance().sendBroadcast(new Intent(
                HierarchyListReceiver.CLEAR_HIERARCHY));

        ArrayList<String> paths = new ArrayList<>();
        paths.add(_context.getString(R.string.hashtags));
        paths.add(tag);
        Intent om = new Intent(HierarchyListReceiver.MANAGE_HIERARCHY);
        om.putStringArrayListExtra("list_item_paths", paths);
        om.putExtra("refresh", true);
        om.putExtra("isRootList", true);
        AtakBroadcast.getInstance().sendBroadcast(om);

        return false;
    }

    @Override
    public boolean isExternalUsageSupported() {
        return true;
    }

    @Override
    public Drawable getIcon() {
        return _context.getDrawable(R.drawable.ic_hashtag);
    }

    @Override
    public boolean accept(HierarchyListItem item) {

        return (item instanceof HashtagMapOverlay.ListModel
                || item instanceof HashtagContentListItem
                || item instanceof HashtagListItem)
                && (item.isChildSupported() || item instanceof HashtagContent);
    }

    @Override
    public boolean acceptRootList() {
        return false;
    }

    private List<Pair<String, HashtagContent>> getContents(String tag,
            HierarchyListItem item) {
        List<Pair<String, HashtagContent>> contents = new ArrayList<>();
        if (item instanceof HashtagContent)
            contents.add(new Pair<>(tag, (HashtagContent) item));
        if (item instanceof AbstractHierarchyListItem2
                && item.isChildSupported()) {
            List<HierarchyListItem> items = ((AbstractHierarchyListItem2) item)
                    .getChildren();
            for (HierarchyListItem i : items)
                contents.addAll(getContents(item.getTitle(), i));
        }
        return contents;
    }
}
