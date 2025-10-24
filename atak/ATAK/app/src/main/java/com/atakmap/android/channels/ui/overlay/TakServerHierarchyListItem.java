
package com.atakmap.android.channels.ui.overlay;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.atakmap.android.channels.prefs.ChannelsPrefs;

import com.atakmap.android.hierarchy.HierarchyListItem;
import com.atakmap.android.hierarchy.action.Action;
import com.atakmap.android.hierarchy.action.Visibility;
import com.atakmap.android.hierarchy.action.Visibility2;
import com.atakmap.android.hierarchy.action.Search;
import com.atakmap.android.hierarchy.items.AbstractHierarchyListItem2;
import com.atakmap.android.http.rest.ServerGroup;
import com.atakmap.app.R;
import com.atakmap.comms.NetConnectString;
import com.atakmap.comms.TAKServer;
import com.atakmap.comms.TAKServerListener;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressLint("LongLogTag")
public class TakServerHierarchyListItem extends AbstractHierarchyListItem2
        implements Visibility2, Search {

    private static final String TAG = "ChannelsTakServerHierarchyLI";

    private final Context context;
    private final ChannelsOverlay overlay;
    private final String host;

    public static final String TAK_SERVER_URL_TAG = "TAK_SERVER_URL";

    public TakServerHierarchyListItem(Context context,
            ChannelsOverlayListModel listModel, String host) {
        this.context = context;
        this.overlay = (ChannelsOverlay) listModel.getUserObject();
        this.host = host;

        this.asyncRefresh = true;
        this.reusable = true;
    }

    @Override
    protected void refreshImpl() {
        try {
            List<HierarchyListItem> items = new ArrayList<>();

            List<DistinguishedNameHierarchyListItem> roots = overlay.getOuRoots(host);
            if (roots != null && !roots.isEmpty()) {
                for (DistinguishedNameHierarchyListItem listItem : roots) {
                    listItem.syncRefresh(this.listener,
                            this.filter);
                    items.add(listItem);
                }
            } else {
                List<ServerGroupHierarchyListItem> groups = overlay
                        .getServerGroups(host);
                if (groups != null) {
                    for (ServerGroupHierarchyListItem listItem : groups) {
                        listItem.syncRefresh(this.listener,
                                this.filter);
                        items.add(listItem);
                    }
                }
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
        TAKServer server = getServer();
        return server != null ? server.getDescription() : host;
    }

    @Override
    public String getDescription() {
        int numGroups = overlay.getChildCount(host, null);
        if (numGroups == 0)
            return context.getString(R.string.no_channels_found);
        else
            return context.getString(numGroups == 1 ? R.string.channel_singular
                    : R.string.channel_plural, numGroups);
    }

    @Override
    public String getUID() {
        return host;
    }

    @Override
    public int getDescendantCount() {
        return getChildCount();
    }

    @Override
    public Object getUserObject() {
        return getServer();
    }

    @Override
    public boolean isMultiSelectSupported() {
        return false;
    }

    @Override
    public Drawable getIconDrawable() {
        TAKServer server = getServer();
        return context.getDrawable(server != null && server.isConnected()
                ? R.drawable.ic_server_success
                : R.drawable.ic_server_error);
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

    public TAKServer getServer() {

        TAKServer[] servers = TAKServerListener.getInstance()
                .getConnectedServers();
        if (servers == null) {
            Log.e(TAG, "getServer: getConnectedServers returned null!");
            return null;
        }

        for (TAKServer server : servers) {
            if (server == null) {
                Log.e(TAG, "getServer: found null server!!");
                continue;
            }

            NetConnectString ncs = NetConnectString
                    .fromString(server.getConnectString());
            if (ncs.getHost().equals(host)) {
                return server;
            }
        }

        return null;
    }

    @Override
    public String getAssociationKey() {
        return ChannelsPrefs.ASSOCIATION_KEY;
    }

    @Override
    public Set<HierarchyListItem> find(String terms) {

        Set<String> searched = new HashSet<>();
        Map<String, HierarchyListItem> ret = new HashMap<>();
        terms = terms.toLowerCase(LocaleUtil.getCurrent());
        List<HierarchyListItem> children = getChildren();

        if (FileSystemUtils.isEmpty(children))
            return new HashSet<>(ret.values());

        for (HierarchyListItem item : children) {

            if (searched.contains(item.getUID()))
                continue;

            if (item instanceof ServerGroupHierarchyListItem) {

                ServerGroupHierarchyListItem serverGroupHierarchyListItem = (ServerGroupHierarchyListItem) item;

                ServerGroup serverGroup = serverGroupHierarchyListItem
                        .getUserObject();
                if (serverGroup == null) {
                    Log.e(TAG,
                            "Found a null ServerGroup while searching! Ignoring and continuing the search.");
                    continue;
                }

                if ((serverGroup.getName() != null && serverGroup.getName()
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

            } else if (item instanceof DistinguishedNameHierarchyListItem) {
                if (item.getTitle().toLowerCase(LocaleUtil.getCurrent())
                        .contains(terms.toLowerCase(LocaleUtil.getCurrent()))) {
                    ret.put(item.getUID(), item);
                }
            }

            searched.add(item.getUID());
        }

        return new HashSet<>(ret.values());
    }

    @Override
    public boolean setVisible(boolean visible) {
        return false;
    }

    @Override
    public <T extends Action> T getAction(Class<T> clazz) {
        // Disable visibility
        if (clazz == Visibility.class || clazz == Visibility2.class)
            return null;
        return super.getAction(clazz);
    }
}
