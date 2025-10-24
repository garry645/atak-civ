
package com.atakmap.android.contact;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.atakmap.android.contact.Contact.UpdateStatus;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.navigation.NavButtonManager;
import com.atakmap.android.navigation.models.NavButtonModel;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactListener;
import gov.tak.api.contact.IContactService;
import gov.tak.api.util.Disposable;
import gov.tak.platform.util.LimitingThread;

public class Contacts implements MapEventDispatcher.MapEventDispatchListener,
        IContactListener, Disposable {

    final static public String TAG = "Contacts";
    public static final String USER_GROUPS = "UserGroups";
    public static final String TEAM_GROUPS = "TeamGroups";

    private final IContactService contactStore;
    private final GroupContact rootGroup;

    private final ConcurrentLinkedQueue<OnContactsChangedListener> contactsChangedListeners = new ConcurrentLinkedQueue<>();

    private static Contacts instance;
    private final String selfUid;
    private final Executor executor;

    private Contacts() {
        this(getContext(MapView.getMapView()),
                getMapEventDispatcher(MapView.getMapView()),
                MapView.getDeviceUid(),
                new ViewEventQueueExecutor(MapView.getMapView()),
                gov.tak.platform.contact.Contacts.getDefaultContactStore());
    }

    /**
     *
     * @param ctx the context
     * @param dispatcher the map event dispatcher
     * @param deviceUid the devices uid derived from mapView.getSelfMarker
     * @param executor      Queues events on the UI thread
     */
    Contacts(Context ctx,
            MapEventDispatcher dispatcher,
            String deviceUid,
            Executor executor,
            IContactService contactStore) {

        this.contactStore = contactStore;
        contactStore.registerContactListener(this);

        this.rootGroup = new GroupContact("RootContactGroup",
                ctx != null ? ctx.getString(R.string.actionbar_contacts)
                        : "Contacts",
                new ArrayList<>(),
                false, GroupContact.getDefaultIconUri(ctx));
        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction("com.atakmap.utc_time_set");

        AtakBroadcast.getInstance().registerReceiver(timeDriftDetected, filter);
        dispatcher.addMapEventListener(MapEvent.ITEM_REMOVED, this);

        selfUid = deviceUid;
        this.executor = executor;
    }

    /**
     * Return the singleton instance of the Contacts
     * @return the contacts object
     */
    synchronized public static Contacts getInstance() {
        if (instance == null)
            instance = new Contacts();

        return instance;

    }

    /**
     * Get the root group for the group contacts
     * @return the group contact root group
     */
    public GroupContact getRootGroup() {
        return this.rootGroup;
    }

    /**
     * Check if the supplied contact is registered (valid)
     * @param c Contact object
     * @return True if the contact is valid
     */
    public boolean validContact(final Contact c) {
        if (c == null) {
            return false;
        }
        synchronized (contactStore) {
            return contactStore.getContact(c.getUid()) != null;
        }
    }

    /**
     * Add contact to collection of contacts
     *  
     * @param contact Contact to add
     */
    public void addContact(GroupContact parent, final Contact contact) {
        if (contact == null)
            return;
        if (parent == null) {
            Log.w(TAG, "Failed to add contact " + contact.getName()
                    + " to null parent, adding to root group instead.");
            parent = this.rootGroup;
        }

        //Log.d(TAG, "Adding " + contact.getName() + " to " + parent.getName());
        if (contact instanceof GroupContact) {
            // In case children weren't added to this list
            GroupContact gc = (GroupContact) contact;
            for (Contact c : gc.getAllContacts(false)) {
                // Don't automatically add individual contact children
                // These should be added explicitly when the user joins
                if (!(c instanceof IndividualContact))
                    addContact(gc, c);
            }
        }
        synchronized (contactStore) {
            contactStore.addContact(contact);
        }
        parent.addContact(contact);
    }

    /**
     * Adds a contact to the contacts.
     * @param contact the non null contact
     */
    public void addContact(Contact contact) {
        if (contact != null)
            addContact(rootGroup, contact);
    }

    /**
     * Remove contact from collection of contacts
     *  
     * @param contact Contact to remove
     */
    public void removeContact(final Contact contact) {
        if (contact == null)
            return;

        Log.d(TAG, "removeContact: " + contact);
        // Remove contact from master list
        removeContactByUuid(contact.getUid());

        // Remove contact from hierarchy
        Contact parent = getContactByUuid(contact.getParentUID());
        if (parent instanceof GroupContact)
            ((GroupContact) parent).removeContact(contact);
    }

    /**
     * Return a contact based on the provided uid
     * @param uid the uid
     */
    public void removeContactByUuid(final String uid) {
        synchronized (contactStore) {
            contactStore.removeContact(uid);
        }
    }

    /**
     * Given a list of UIDs, return a list of valid contacts.   The list of contacts can be
     * shorter if the uid does not map to a valid contact
     * @param uids the list of uids
     * @return the list of valid contacts
     */
    public static List<Contact> fromUIDs(List<String> uids) {
        final List<Contact> contacts = new ArrayList<>(uids.size());
        for (String uid : uids) {
            Contact contact = Contacts.getInstance().getContactByUuid(uid);
            if (contact != null)
                contacts.add(contact);
        }
        return contacts;
    }

    /**
     * Given a list of contacts, produce a list of corresponding uids
     * @param contacts the list of contacts
     * @return the list of uids that represent the list of contacts.
     */
    public static List<String> toUIDs(List<Contact> contacts) {
        List<String> uids = new ArrayList<>();
        for (Contact c : contacts) {
            if (c != null)
                uids.add(c.getUid());
        }
        return uids;
    }

    /**
     * Get a copy of all contacts, so it can be used without violating thread 
     * safety.
     * 
     * @return a copy of all of the contacts
     */
    public List<Contact> getAllContacts() {
        synchronized (contactStore) {
            List<Contact> allContacts = new ArrayList<>();
            for (IContact contact : contactStore.getAllContacts()) {
                allContacts.add((Contact) contact);
            }
            return allContacts;
        }
    }

    /**
     * Return all individual contacts uids
     * @return the list of uids to representing the contacts
     */
    public List<String> getAllIndividualContactUuids() {
        return getAllContactsOfClass(IndividualContact.class);
    }

    private List<String> getAllContactsOfClass(Class<?> classType) {
        List<String> uuidsToReturn = new ArrayList<>();
        synchronized (contactStore) {
            for (IContact c : contactStore.getAllContacts()) {
                Contact contact = (Contact) c;
                UpdateStatus status = contact.getUpdateStatus();
                if (classType.isInstance(contact) && status != null
                        && !status.equals(UpdateStatus.NA)
                        && !contact.getExtras().getBoolean("fakeGroup")) {
                    uuidsToReturn.add(contact.getUID());
                }
            }
            return uuidsToReturn;
        }
    }

    /**
     * Iterate through contacts to find contact with specified uuid.
     * 
     * @param uuid the unique identifier to use
     * @return get a contact given the unique identifier.
     */
    public Contact getContactByUuid(final String uuid) {
        if (FileSystemUtils.isEmpty(uuid))
            return null;

        if (this.rootGroup != null
                && FileSystemUtils.isEquals(this.rootGroup.getUID(), uuid))
            return this.rootGroup;

        synchronized (contactStore) {
            return (Contact) contactStore.getContact(uuid);
        }
    }

    /**
     * Create a paths bundle for everything under this group
     * Meant to be called on user groups only
     * @param group Group contact
     * @return Bundle containing the path string arrays
     */
    public static Bundle buildPaths(Contact group) {
        Bundle pathsBundle = new Bundle();
        if (!(group instanceof GroupContact))
            return pathsBundle;

        // Find top-level user group (Groups -> [group name])
        GroupContact gc = ((GroupContact) group).getRootUserGroup();
        if (gc == null)
            return pathsBundle;

        // Build paths for all users under top-level user group
        Bundle rootPaths = buildLocalPaths(gc);
        if (gc.isUserCreated())
            rootPaths.putBundle(getInstance().selfUid, buildLocalPaths(
                    CotMapComponent.getInstance().getSelfContact(false)));
        pathsBundle.putBundle(gc.getUID(), rootPaths);
        return pathsBundle;
    }

    private static Bundle buildLocalPaths(Contact contact) {
        Bundle paths = new Bundle();
        if (contact == null)
            return paths;
        paths.putString("uid", contact.getUID());
        paths.putString("name", contact.getName());
        paths.putString("type",
                contact instanceof GroupContact ? "group" : "contact");
        if (!(contact instanceof GroupContact))
            return paths;
        for (Contact c : ((GroupContact) contact).getAllContacts(false)) {
            Bundle childPaths = buildLocalPaths(c);
            paths.putBundle(c.getUID(), childPaths);
        }
        return paths;
    }

    /**
     * Iterate through contacts to find contacts with specified uuids.
     * 
     * @param uuids List of UUIDs to find
     * @return array of individual contacts
     */
    public IndividualContact[] getIndividualContactsByUuid(List<String> uuids) {
        Set<IndividualContact> ret = new HashSet<>();
        synchronized (contactStore) {
            for (String uuid : uuids) {
                IContact contact = contactStore.getContact(uuid);
                if (contact instanceof IndividualContact)
                    ret.add((IndividualContact) contact);
            }
        }
        return ret.toArray(new IndividualContact[0]);
    }

    /**
     * Iterate through contacts and find first contact with specified name.
     * 
     * @param callsign the callsign to use in the search
     * @return the first contact that matches
     */
    public Contact getFirstContactWithCallsign(final String callsign) {
        if (FileSystemUtils.isEmpty(callsign))
            return null;
        synchronized (contactStore) {
            for (IContact contact : contactStore.getAllContacts()) {
                if (callsign.equals(contact.getName())) {
                    return (Contact) contact;
                }
            }
        }
        return null;
    }

    /**
     * Return a list of all contacts that are in a specified team
     * @param team the string designation of the team
     * @return an array list of the contacts that are in the team
     */
    public List<String> getAllContactsInTeam(final String team) {
        List<String> ret = new ArrayList<>();
        synchronized (contactStore) {
            for (IContact contact : contactStore.getAllContacts()) {
                if (((Contact) contact).getExtras().getString("team", "none")
                        .equals(team))
                    ret.add(contact.getUid());
            }
        }
        return ret;
    }

    /**
     * Return a list of all contacts with a given role
     * @param role the role
     * @return the list of contacts
     */
    public List<String> getAllContactsWithRole(final String role) {
        List<String> ret = new ArrayList<>();
        synchronized (contactStore) {
            for (IContact contact : contactStore.getAllContacts()) {
                if (((Contact) contact).getExtras().getString("role", "none")
                        .equals(role))
                    ret.add(contact.getUid());
            }
            return ret;
        }
    }

    /**
     * Get uuids of all the contacts.
     * 
     * @return a list of all uids
     */
    public List<String> getAllContactUuids() {
        List<String> uuidsToReturn = new ArrayList<>();
        synchronized (contactStore) {
            for (IContact contact : contactStore.getAllContacts()) {
                if (contact instanceof Contact) {
                    UpdateStatus status = ((Contact) contact).getUpdateStatus();
                    if (status == null || status.equals(UpdateStatus.NA))
                        continue;
                }
                uuidsToReturn.add(contact.getUid());
            }

            return uuidsToReturn;
        }
    }

    /**
     * Called internally to signal that the total unread count might have been changed.
     */
    public void updateTotalUnreadCount() {

        // Refresh unread count at most once per second
        // Too many action bar refreshes prevents buttons from being clickable
        refreshUnread.exec();

        //refresh the contact list rows and other listeners as well...
        dispatchContactChangedEvent(null);
    }

    private void disposeAllContacts() {
        synchronized (contactStore) {
            for (IContact contact : contactStore.getAllContacts()) {
                contactStore.removeContact(contact.getUid());
            }
        }
    }

    @Override
    public void contactAdded(IContact addedContact) {
        rootGroup.refreshImpl();
        dispatchSizeChangedEvents();
        updateTotalUnreadCount();
    }

    @Override
    public void contactRemoved(IContact removedContact) {
        rootGroup.refreshImpl();
        dispatchSizeChangedEvents();
        updateTotalUnreadCount();
    }

    @Override
    public void contactUpdated(IContact updatedContact) {
        rootGroup.refreshImpl();
        dispatchContactChangedEvent(null);
    }

    /*
     *  Used when properties are changed on a Contact in the Contracts list.
     */
    public interface OnContactsChangedListener {
        /**
         * Fired when the contact list changes size
         * @param contacts the contact list
         */
        void onContactsSizeChange(Contacts contacts);

        /**
         * Fired when a contact changes such as name or if the contact is stale
         * @param uuid the uuid for the contact that changed
         */
        void onContactChanged(String uuid);
    }

    /**
     * Adds a listener for when the Contact list or a specific contact is changed
     * @param listener the listener for the event.
     */
    public void addListener(OnContactsChangedListener listener) {
        contactsChangedListeners.add(listener);
    }

    /**
     * Removes the listener for when the Contact list or a specific contact is changed
     * @param listener the listener for the event.
     */
    public void removeListener(OnContactsChangedListener listener) {
        contactsChangedListeners.remove(listener);
    }

    @Override
    public void dispose() {
        contactsChangedListeners.clear();
        disposeAllContacts();
        contactStore.unregisterContactListener(this);
        AtakBroadcast.getInstance().unregisterReceiver(timeDriftDetected);

    }

    private void dispatchSizeChangedEvents() {
        for (OnContactsChangedListener listener : contactsChangedListeners) {
            try {
                listener.onContactsSizeChange(this);
            } catch (Exception e) {
                Log.e(TAG, "error", e);
            }
        }
    }

    void dispatchContactChangedEvent(String uuid) {
        for (OnContactsChangedListener listener : contactsChangedListeners) {
            try {
                listener.onContactChanged(uuid);
            } catch (Exception e) {
                Log.e(TAG, "error", e);
            }
        }
    }

    private final BroadcastReceiver timeDriftDetected = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (contactStore) {
                Log.d(TAG, "time drift detected based on GPS");
                for (IContact contact : contactStore.getAllContacts()) {
                    Contact c = (Contact) contact;
                    /**
                    if (c instanceof IndividualContact) {
                        IndividualContact ic = (IndividualContact)c;
                        IpConnector ipConnector = (IpConnector)ic.getConnector(IndividualContact.ConnectorType.IP);
                    
                        if (ipConnector != null) {
                            ipConnector.updateLastSeen(ipConnector.getLastSeen().addMilliseconds( -1 * (int)CoordinatedTime.getCoordinatedTimeOffset()));
                            Log.d(TAG, "updating individual contact based on time shift: " + ic + " " + CoordinatedTime.getCoordinatedTimeOffset());
                        }
                    }
                     **/

                }
            }

        }
    };

    @Override
    public void onMapEvent(MapEvent event) {
        MapItem mi = event.getItem();
        if (mi != null && mi.getType().startsWith("a-f"))
            removeContactByUuid(mi.getUID());
    }

    private static final int refreshRate = 2000;
    private final LimitingThread refreshUnread = new LimitingThread(
            "RefreshUnread", new Runnable() {

                private int _totalUnread = 0;

                @Override
                public void run() {
                    try {
                        if (CotMapComponent.getInstance() != null
                                && CotMapComponent.getInstance()
                                        .getContactConnectorMgr() != null) {
                            int totalUnread = rootGroup.calculateUnread();
                            if (_totalUnread != totalUnread) {
                                _totalUnread = totalUnread;
                                // Update the unread count on both chat icons
                                setUnreadCount("contacts.xml", totalUnread);
                                setUnreadCount("chatnext.xml", totalUnread);
                                //setUnreadCount("groupchat.xml", totalUnread);
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG,
                                "Failed to refresh unread count for contacts.");
                    } finally {
                        try {
                            Thread.sleep(refreshRate);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
            });

    private void setUnreadCount(String ref, int totalUnread) {
        NavButtonModel mdl = NavButtonManager.getInstance()
                .getModelByReference(ref);
        if (mdl != null) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    mdl.setBadgeCount(totalUnread);
                    NavButtonManager.getInstance().notifyModelChanged(mdl);
                }
            });
        }
    }

    final static class ViewEventQueueExecutor implements Executor {

        final View view;

        ViewEventQueueExecutor(View view) {
            this.view = view;
        }

        @Override
        public void execute(Runnable runnable) {
            view.post(runnable);
        }
    }

    static Context getContext(MapView mv) {
        return (mv != null) ? mv.getContext() : null;
    }

    static MapEventDispatcher getMapEventDispatcher(MapView mv) {
        return (mv != null) ? mv.getMapEventDispatcher() : null;
    }
}
