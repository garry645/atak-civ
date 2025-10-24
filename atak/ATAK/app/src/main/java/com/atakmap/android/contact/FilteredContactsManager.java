
package com.atakmap.android.contact;

import android.content.Intent;

import com.atakmap.android.cot.importer.MapItemImporter;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import gov.tak.api.util.Disposable;

public class FilteredContactsManager implements Disposable {

    private static FilteredContactsManager instance;

    private final FilteredContactsDatabase db;

    private final Set<String> filteredContacts = new HashSet<>();

    public static final String ATAK_FILTER_CONTACT = "gov.tak.android" +
            ".FILTER_CONTACT";

    private final MapView mapView;

    private FilteredContactsManager() {

        mapView = MapView.getMapView();

        db = new FilteredContactsDatabase();
        // update filter status from storage
        for (String uid : db.getAllUids())
            setContactFiltered(uid, true, false);
        if (!filteredContacts.isEmpty())
            fireFilteredContactsChangedIntent();

        // install notification filter
        MapItemImporter.addNotificationFilter(
                new MapItemImporter.NotificationFilter() {
                    @Override
                    public boolean accept(MapItem item) {
                        return !isItemFiltered(item);
                    }
                });

        //register items for getting added to the map
        MapGroup root = MapView.getMapView().getRootGroup();
        GlobalItemListener globalItemListener = new GlobalItemListener();
        globalItemListener.onGroupAdded(root, root);
    }

    /**
     * Retrieve the instance of the FilteredContactsManager to be used
     * @return a valid single instance of the filtered contacts manager.
     */
    public synchronized static FilteredContactsManager getInstance() {
        if (instance == null) {
            instance = new FilteredContactsManager();
        }
        return instance;
    }

    void fireFilteredContactsChangedIntent() {
        Intent contactFilterIntent = new Intent(ATAK_FILTER_CONTACT);
        AtakBroadcast.getInstance().sendBroadcast(contactFilterIntent);
    }

    /**
     * If there are any filtered contacts
     * @return true if there is at least one filtered contact.
     */
    public boolean anyContactsFiltered() {
        return (!filteredContacts.isEmpty());
    }

    /**
     * Sets a contact as filtered if it is unfiltered or to remove its
     * filter status if it is already filtered.
     * @param c The contact to be filtered. FilteredContactsDatabase stores filters
     *            contacts based on UID
     * @param filterStatus desired filtered status of the contact, if <code>true</code> the contact
     *                     and all items authored by the contact will be hidden on the map and chat
     *                     messages received from the contact will not be shown.
     * @param fireIntent used to update the contact to display and act as filtered.
     *                   It is true by default.
     */

    public void setContactFiltered(Contact c, boolean filterStatus,
            boolean fireIntent) {
        if (c != null) {
            setContactFiltered(c.contactUUID, filterStatus, fireIntent);
        }
    }

    private void setChildMapItemsForContactVisible(MapView mv,
            String contactUid, final boolean visible) {
        if (contactUid != null) {
            final MapGroup rg = mv.getRootGroup();
            rg.deepForEachItem(
                    new MapGroup.OnItemCallback<MapItem>(MapItem.class) {
                        @Override
                        protected boolean onMapItem(MapItem mapItem) {
                            if (visible != mapItem.getVisible()) {
                                final String parentUid = getParentUid(rg,
                                        mapItem, orphanOwnedItems, true);
                                if (parentUid != null
                                        && parentUid.equals(contactUid)) {
                                    mapItem.setVisible(visible
                                            || determineHighestPriorityLevel(
                                                    mapItem)
                                            || determineIsEmergencyOrBailout(
                                                    mapItem));
                                }
                            }
                            return false;
                        }
                    });
        }
    }

    private void setContactFiltered(String uid, boolean filterStatus,
            boolean fireIntent) {
        synchronized (filteredContacts) {

            if (filterStatus == filteredContacts.contains(uid))
                return;

            // update bookkeeping
            if (filterStatus) {
                filteredContacts.add(uid);
                db.addUid(uid);
            } else {
                filteredContacts.remove(uid);
                db.removeUid(uid);
            }

            // reset visibility

            // contact is visible if:
            //   - it is not filtered
            // OR
            //   - it is an emergency or bailout beacon
            final MapItem contact = mapView.getMapItem(uid);
            if (contact != null)
                contact.setVisible(!filterStatus
                        || determineIsEmergencyOrBailout(contact)
                        || determineHighestPriorityLevel(contact));
            // children adopt filter status of contact
            setChildMapItemsForContactVisible(mapView, uid, !filterStatus);

            // fire intent
            if (fireIntent) {
                fireFilteredContactsChangedIntent();
            }
        }
    }

    /**
     * Given a contact, return true if it is to be filtered
     * @param c the contact
     * @return true if it is filtered.
     */
    public boolean isContactFiltered(Contact c) {
        if (c == null)
            return false;
        return isContactFiltered(c.contactUUID);
    }

    /**
     * Given a UID for a contact, return true if it is to be filtered.
     * @param uid the uid for the contact
     * @return true if it is filtered
     */
    public boolean isContactFiltered(String uid) {
        if (uid == null)
            return false;
        synchronized (filteredContacts) {
            return filteredContacts.contains(uid);
        }
    }

    /**
     * Determines if the map item is filtered. If it is filtered or is a child of a filtered item AND
     * it is not an emergency or bailout point and is not something labeled as a highest priority point,
     * it is filtered
     * @param mapItem the map item to be used to determine if it is not filtered
     * @return  <code>true</code> if the _item_ is considered filtered
     */
    private boolean isItemFiltered(MapItem mapItem) {
        if (isContactFiltered(mapItem.getUID())
                || isContactFiltered(getParentUid(mapView.getRootGroup(),
                        mapItem, orphanOwnedItems, true))) {
            if (determineHighestPriorityLevel(mapItem)
                    || determineIsEmergencyOrBailout(mapItem)) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Return a list of all of the uids for the filtered contacts.
     * @return a copy of a list for all filtered contacts.
     */
    public Set<String> getFilteredContacts() {
        Set<String> list;
        synchronized (filteredContacts) {
            list = new HashSet<>(filteredContacts);
        }
        return list;
    }

    private boolean determineIsEmergencyOrBailout(MapItem item) {
        // walk up the parent/owner hierarchy checking each level for emergency/bailout
        final MapGroup rootGroup = mapView.getRootGroup();
        while (item != null) {
            if (item.getType().startsWith("a-f") &&
                    (item.getMetaBoolean("isBailoutIndicator", false) ||
                            item.getMetaBoolean("isEmergencyIndicator",
                                    false))) {
                return true;
            }
            final String parentUid = getParentUid(rootGroup, item, null, false);
            if (parentUid == null || parentUid.equals(item.getUID()))
                break;
            item = rootGroup.deepFindUID(parentUid);
        }
        return false;
    }

    private boolean determineHighestPriorityLevel(MapItem item) {
        // walk up the parent/owner hierarchy checking each level for priority
        final MapGroup rootGroup = mapView.getRootGroup();
        while (item != null) {
            if (item.getMetaString("priorityLevel", "")
                    .equals(PriorityLevel.HIGHEST.getValue())) {
                return true;
            }
            final String parentUid = getParentUid(rootGroup, item, null, false);
            if (parentUid == null || parentUid.equals(item.getUID()))
                break;
            item = rootGroup.deepFindUID(parentUid);
        }
        return false;
    }

    private static String getParentUid(MapGroup rootGroup, MapItem item,
            Set<MapItem> orphans, boolean recurse) {
        if (item == null)
            return null;
        String parentUid = item.getMetaString("parent_uid", null);
        if (parentUid == null) {
            // no associated parent, look for the owner and check for its parent
            String ownerUid = item.getMetaString("ownerUID", null);
            if (ownerUid != null) {
                parentUid = ownerUid;
                final MapItem owner = rootGroup.deepFindUID(parentUid);
                if (owner != null && recurse)
                    parentUid = getParentUid(rootGroup, owner, orphans,
                            recurse);
                else if (owner == null && orphans != null)
                    orphans.add(item);
            }
        }
        return parentUid;
    }

    @Override
    public void dispose() {
        db.close();
    }

    public enum PriorityLevel {
        HIGHEST("HIGHEST"),
        HIGH("HIGH"),
        MEDIUM("MEDIUM"),
        LOW("LOW"),
        LOWEST("LOWEST");

        private final String priorityString;

        PriorityLevel(String priorityString) {
            this.priorityString = priorityString;
        }

        public String getValue() {
            return priorityString;
        }
    }

    private final Set<MapItem> orphanOwnedItems = Collections
            .newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Handler for when MapItems are received and how to filter them.
     * @param item - the received MapItem
     */
    private void registerItems(MapItem item) {
        if (!anyContactsFiltered())
            return;

        final boolean itemFiltered = isItemFiltered(item);
        if (itemFiltered) {
            item.setVisible(false);
            // check for any orphans and update visibility/mark as adopted as appropriate
            final String itemUid = item.getUID();
            Iterator<MapItem> iter = orphanOwnedItems.iterator();
            while (iter.hasNext()) {
                final MapItem orphan = iter.next();
                if (itemUid.equals(orphan.getMetaString("ownerUID", ""))) {
                    orphan.setVisible(item.getVisible());
                    iter.remove();
                }
            }
        }

    }

    /**
     * Listener for anything added to the map
     */
    private class GlobalItemListener
            implements MapGroup.OnGroupListChangedListener,
            MapGroup.OnItemListChangedListener {
        @Override
        public void onGroupAdded(MapGroup group, MapGroup parent) {
            // recursively register on all children
            for (MapGroup child : group.getChildGroups())
                onGroupAdded(child, group);

            // subscribe on group add/remove
            group.addOnGroupListChangedListener(this);

            // post item
            for (MapItem item : group.getItems()) {
                onItemAdded(item, group);
            }

            // subscribe on item add/remove
            group.addOnItemListChangedListener(this);
        }

        @Override
        public void onGroupRemoved(MapGroup group, MapGroup parent) {
            // recursively register on all children
            for (MapGroup child : group.getChildGroups())
                onGroupRemoved(child, group);

            // subscribe on group add/remove
            group.removeOnGroupListChangedListener(this);

            // post item
            for (MapItem item : group.getItems())
                onItemRemoved(item, group);

            // subscribe on item add/remove
            group.addOnItemListChangedListener(this);
        }

        @Override
        public void onItemAdded(MapItem item, MapGroup group) {
            registerItems(item);
        }

        @Override
        public void onItemRemoved(MapItem item, MapGroup group) {

        }
    }
}
