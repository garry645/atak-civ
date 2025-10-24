package com.atakmap.android.channels.ui.overlay;

import android.content.Context;
import com.atakmap.android.hierarchy.HierarchyListItem;
import com.atakmap.android.hierarchy.action.Search;
import com.atakmap.android.hierarchy.action.Visibility2;
import com.atakmap.android.hierarchy.items.AbstractHierarchyListItem2;
import com.atakmap.android.http.rest.ServerGroup;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class DistinguishedNameHierarchyListItem extends AbstractHierarchyListItem2
        implements Visibility2, Search  {

    private static final String TAG = "DistinguishedNameHLI";

    private final Context context;
    private final ChannelsOverlay overlay;
    private final String server;
    private final String distinguishedName;

    public DistinguishedNameHierarchyListItem(Context context,
                                              ChannelsOverlay overlay,
                                              String server,
                                              String distinguishedName) {
        this.context = context;
        this.overlay = overlay;
        this.server = server;
        this.distinguishedName = distinguishedName;
        this.asyncRefresh = true;
        this.reusable = true;
    }

    public String getDistinguishedName() { return distinguishedName; }

    @Override
    protected void refreshImpl() {
        try {
            List<HierarchyListItem> items = new ArrayList<>();
            List<DistinguishedNameHierarchyListItem> children = overlay
                    .getChildren(server, distinguishedName);
            if (!children.isEmpty()) {
                for (DistinguishedNameHierarchyListItem dnHierarchyListItem : children) {
                    ServerGroupHierarchyListItem sgHierarchyListItem =  overlay.getServerGroup(
                            server, dnHierarchyListItem.getDistinguishedName());
                    if (sgHierarchyListItem != null) {
                        items.add(sgHierarchyListItem);
                    } else {
                        items.add(dnHierarchyListItem);
                    }
                }
            }

            for (HierarchyListItem item : items) {
                ((AbstractHierarchyListItem2)item).syncRefresh(this.listener, this.filter);
            }

            sortItems(items);
            updateChildren(items);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public boolean hideIfEmpty() {
        return false;
    }

    @Override
    public String getTitle() {
        return distinguishedName.replaceAll(
                "(?i:cn|ou|uid|dc)="," ");
    }

    @Override
    public String getDescription() {
        int numGroups = overlay.getChildCount(server, distinguishedName);
        if (numGroups == 0)
            return context.getString(R.string.no_channels_found);
        else
            return context.getString(numGroups == 1 ? R.string.channel_singular
                    : R.string.channel_plural, numGroups);
    }

    @Override
    public int getDescendantCount() {
        return getChildCount();
    }

    @Override
    public Object getUserObject() {
        // TODO what is this used for?
        return null;
    }

    @Override
    public boolean isMultiSelectSupported() {
        return false;
    }

    @Override
    public List<Sort> getSorts() {
        try {
            List<Sort> sorts = new ArrayList<>();
            sorts.add(new SortAlphabet());
            sorts.add(new SortAlphabetDesc());
            return sorts;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Set<HierarchyListItem> find(String terms) {

        Log.d(TAG, "searching for " + terms);
        Map<String, HierarchyListItem> ret = new HashMap<>();
        terms = terms.toLowerCase(LocaleUtil.getCurrent());
        List<HierarchyListItem> children = getChildren();

        if (FileSystemUtils.isEmpty(children))
            return new HashSet<>(ret.values());

        for (HierarchyListItem item : children) {

            if (item instanceof ServerGroupHierarchyListItem) {

                ServerGroupHierarchyListItem serverGroupHierarchyListItem = (ServerGroupHierarchyListItem) item;

                ServerGroup serverGroup = serverGroupHierarchyListItem
                        .getUserObject();
                if (serverGroup == null) {
                    Log.e(TAG,
                            "Found a null ServerGroup while searching! Ignoring and continuing the search.");
                    continue;
                }

                if (serverGroup.getName() != null) {
                    Log.d(TAG, "comparing ServerGroup " + serverGroup.getName()
                            + ", " + serverGroup.getDescription());

                    if ((serverGroup.getName()
                            .toLowerCase(LocaleUtil.getCurrent())
                            .contains(terms.toLowerCase(LocaleUtil.getCurrent()))) ||
                            (serverGroup.getDescription() != null
                                    && serverGroup.getDescription()
                                    .toLowerCase(LocaleUtil.getCurrent())
                                    .contains(terms.toLowerCase(
                                            LocaleUtil.getCurrent())))) {
                        ret.put(serverGroupHierarchyListItem.getUID(),
                                serverGroupHierarchyListItem);
                    }

                }

            } else if (item instanceof DistinguishedNameHierarchyListItem) {
                Log.d(TAG, "comparing DistinguishedName " + item.getTitle());

                if (item.getTitle().toLowerCase(LocaleUtil.getCurrent())
                        .contains(terms.toLowerCase(LocaleUtil.getCurrent()))) {
                    ret.put(item.getUID(), item);
                }
            }
        }

        return new HashSet<>(ret.values());
    }

    @Override
    public boolean setVisible(boolean visible) {
        for (HierarchyListItem item : getChildren()) {
            if (item instanceof ServerGroupHierarchyListItem) {
                ((ServerGroupHierarchyListItem)item).setVisible(visible);
            } else if (item instanceof DistinguishedNameHierarchyListItem) {
                ((DistinguishedNameHierarchyListItem)item).setVisible(visible);
            }
        }
        return true;
    }

    @Override
    public String getIconUri() {
        return "gone";
    }
}
