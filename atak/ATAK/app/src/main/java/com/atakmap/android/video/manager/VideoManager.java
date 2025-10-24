
package com.atakmap.android.video.manager;

import android.content.Intent;
import android.util.Pair;

import com.atakmap.android.data.URIContentManager;
import com.atakmap.android.hierarchy.HierarchyListReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.video.ConnectionEntry;
import com.atakmap.android.video.StreamManagementUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProvider;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.annotation.NonNull;
import gov.tak.api.util.Disposable;
import gov.tak.api.video.IVideoConnectionManager;

/**
 * Class responsible for managing video aliases persisted to the filesystem
 */
public class VideoManager implements IVideoConnectionManager, Disposable {

    private static final String TAG = "VideoManager";

    public static final File DIR = new File(FileSystemUtils.getItem(
            FileSystemUtils.TOOL_DATA_DIRECTORY), "videos");
    public static final File ENTRIES_DIR = new File(DIR, ".entries");

    private static VideoManager _instance;

    public static VideoManager getInstance() {
        return _instance;
    }

    // Handles file scanning for videos under /atak/tools/videos
    private final VideoFileWatcher _scanner;

    // Handles reading and writing connection entries to and from XML
    private final VideoXMLHandler _xmlHandler;

    // Content resolver for mapping file paths to entries
    private final VideoContentResolver _contentResolver;

    // UID -> Connection entry cache
    private final Map<String, EntryRecord> _entries = new HashMap<>();

    // Event listeners
    private final Set<ConnectionListener> _listeners = new HashSet<>();

    public VideoManager(MapView mapView) {
        _contentResolver = new VideoContentResolver(mapView);
        _xmlHandler = new VideoXMLHandler();
        _scanner = new VideoFileWatcher(mapView, this);
        File[] roots = FileSystemUtils.getDeviceRoots();
        for (File root : roots) {
            root = new File(root, "atak/tools/videos");
            _scanner.addDirectory(root);
        }
    }


    /**
     * Given a DISPLAY intent, with all of the legacy parameters, create the appropriate video
     * ConnectionEntry.
     * @param intent the intent containing the legacy parameters
     * @return the ConnectionEntry extracted from the intent.
     */
    public static ConnectionEntry parse(final Intent intent) {

        ConnectionEntry recvEntry = null;

        final String videoUID = intent.getStringExtra("videoUID");
        if (!FileSystemUtils.isEmpty(videoUID)) {
            List<ConnectionEntry> entries1 = VideoManager.getInstance()
                    .getRemoteEntries();
            Map<String, ConnectionEntry> ret = new HashMap<>();
            for (ConnectionEntry entry : entries1)
                ret.put(entry.getUID(), entry);
            if (ret.containsKey(videoUID))
                recvEntry = ret.get(videoUID);
            else {
                //check for same alias if the uid is not in the list
                if (intent.getStringExtra("videoUrl") != null) {
                    String url = intent.getStringExtra("videoUrl");
                    List<ConnectionEntry> conns = VideoManager.getInstance()
                            .getEntries();

                    for (ConnectionEntry ce : conns) {
                        if (ConnectionEntry.getURL(ce, false).equals(url)) {
                            recvEntry = ce;
                            break;
                        }
                    }
                }
            }

        } else if (intent.getStringExtra("videoUrl") != null) {
            // the user clicked on the button in the radial menu (from the map)
            String videoUrl = intent.getStringExtra("videoUrl");
            String aliasName = "new video";
            String uid = null;

            final String uidExtra = intent.getStringExtra("uid");
            if (!FileSystemUtils.isEmpty(uidExtra))
                uid = uidExtra;

            final String callsignExtra = intent.getStringExtra("callsign");

            if (!FileSystemUtils.isEmpty(callsignExtra)) {
                aliasName = callsignExtra;
            } else if (uid != null) {
                aliasName = uid;
            }

            recvEntry = StreamManagementUtils.createConnectionEntryFromUrl(
                    aliasName, videoUrl);
            if (recvEntry != null) {
                if (uid != null)
                    recvEntry.setUID(uid);

                String preferredInterfaceAddress = intent
                        .getStringExtra("preferredInterfaceAddress");
                if (preferredInterfaceAddress != null
                        && !preferredInterfaceAddress.trim().isEmpty())
                    recvEntry.setPreferredInterfaceAddress(
                            preferredInterfaceAddress);

                String buffer = intent.getStringExtra("buffer");
                if (buffer != null && !buffer.trim().isEmpty())
                    recvEntry.setBufferTime(Integer.parseInt(buffer));

                String timeout = intent.getStringExtra("timeout");
                if (timeout != null && !timeout.trim().isEmpty())
                    recvEntry.setNetworkTimeout(Integer.parseInt(timeout));
                // if the alias is not in the list add it
                VideoManager.getInstance().addEntry(recvEntry);
                Log.d(TAG, "received url: " + recvEntry);
            }
        } else {
            // the callee has passed in a ConnectionEntry to use.
            recvEntry = (ConnectionEntry) intent
                    .getSerializableExtra(
                            ConnectionEntry.EXTRA_CONNECTION_ENTRY);
            Log.d(TAG, "received connection entry: " + recvEntry);
        }

        return recvEntry;

    }

    /**
     * Given a ConnectionEntry, refresh it based on either the backing Map Item or an updated
     * entry in the Video Manager.
     * @param ce the original connection entry
     * @return the updated connection entry.
     */
    public static ConnectionEntry refreshConnectionEntry(ConnectionEntry ce) {
        if (ce != null) {
            final MapView mapView = MapView.getMapView();
            final MapItem item = (mapView != null)?mapView.getMapItem(ce.getUID()):null;

            if (item != null) {

                // reuse as much code as possible
                final Intent i = new Intent("com.atakmap.maps.video.DISPLAY");
                copy(item, "videoUID", i, "videoUID");
                copy(item, "videoUrl", i, "videoUrl");
                copy(item, "uid", i, "uid");
                copy(item, "callsign", i, "callsign");
                copy(item, "buffer", i, "buffer");
                copy(item, "timeout", i, "timeout");
                copy(item, "standalone", i, "standalone");
                copy(item, "spi_uid", i, "spi_uid");
                copy(item, "sensor_uid", i, "sensor_uid");
                copy(item, "cancelClose", i, "cancelClose");
                final ConnectionEntry newCe = parse(i);

                final VideoManager vm = VideoManager.getInstance();
                if (vm.getConnectionEntry(ce.getUID()) != null) {
                    vm.removeConnectionEntry(ce.getUID());
                    vm.addEntry(newCe);
                }
                return newCe;
            } else {
                final ConnectionEntry newCe = getInstance().getEntry(ce.getUID());
                if (newCe != null)
                    return newCe.copy();
            }
        }
        return ce;
    }

    private static void copy(@NonNull MapItem item, @NonNull String srcKey, @NonNull Intent i, @NonNull String dstKey) {
        String val = item.getMetaString(srcKey, null);
        if (val != null)
            i.putExtra(dstKey, val);
    }

    public void init() {
        if (_instance == this)
            return;

        _instance = this;
        if (!IOProviderFactory.exists(ENTRIES_DIR)
                && !IOProviderFactory.mkdirs(ENTRIES_DIR))
            Log.w(TAG, "Failed to create entries dir");

        // Register the content resolver
        URIContentManager.getInstance().registerResolver(_contentResolver);
        addListener(_contentResolver);

        // Start scanning for videos
        _scanner.start();

        // Add any video files or folders under /videos directory
        List<ConnectionEntry> entries = new ArrayList<>();

        // Read from the entries directory
        File[] files = IOProviderFactory.listFiles(ENTRIES_DIR,
                VideoFileWatcher.XML_FILTER);
        if (!FileSystemUtils.isEmpty(files)) {
            for (File f : files)
                entries.addAll(_xmlHandler.parse(f));
        }

        // Cache entries
        addEntries(entries, false);
    }

    /**
     * Dispose the video manager
     */
    @Override
    public void dispose() {
        URIContentManager.getInstance().unregisterResolver(_contentResolver);
        removeListener(_contentResolver);
        _contentResolver.dispose();
        _scanner.dispose();
        _entries.clear();
        _instance = null;
    }

    /**
     * Get a connection entry by its UID
     *
     * @param uid Connection entry UID
     * @return Connection entry or null if not found
     * @deprecated use {@link #getConnectionEntry(String)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    public synchronized ConnectionEntry getEntry(String uid) {
        final Pair<gov.tak.api.video.ConnectionEntry, ConnectionEntry> entry = getConnectionEntryImpl(
                uid, true);
        return (entry != null) ? entry.second : null;
    }

    /**
     * Get the full list of registered connection entries
     *
     * @return List of connection entries (unsorted)
     *
     * @deprecated use {@link #getConnectionEntries(Set, boolean)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    public synchronized List<ConnectionEntry> getEntries() {
        return new ArrayList<>(
                getConnectionEntries(null, false, true).values());
    }

    /**
     * Get a list of registered connection entries
     *
     * @param uids Entry UIDs
     * @return Connection entries
     * @deprecated use {@link #getConnectionEntries(Set, boolean)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    public synchronized List<ConnectionEntry> getEntries(Set<String> uids) {
        return new ArrayList<>(
                getConnectionEntries(uids, false, true).values());

    }

    /**
     * Get a list of all remote connection entries
     *
     * @return List of remote connection entries (unsorted)
     * @deprecated use {@link #getConnectionEntries(Set, boolean)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    public synchronized List<ConnectionEntry> getRemoteEntries() {
        return new ArrayList<>(getConnectionEntries(null, true, true).values());
    }

    /**
     * Add an entry to the manager (which is then persisted to the filesystem)
     *
     * @param entry Connection entry
     *
     * @deprecated use {@link #addConnectionEntry(gov.tak.api.video.ConnectionEntry)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    public void addEntry(ConnectionEntry entry) {
        final gov.tak.api.video.ConnectionEntry e;
        synchronized (this) {
            e = addEntryNoSync(entry.get(), entry);
        }
        if (e != null) {
            persist(e);
            for (ConnectionListener l : getListeners())
                l.onEntryAdded(e);
        }
    }

    /**
     * Add a list of connection entries to the manager
     *
     * @param entries List of connection entries
     *
     * @deprecated use {@link #addConnectionEntries(List)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    public void addEntries(List<ConnectionEntry> entries, boolean persist) {
        if (FileSystemUtils.isEmpty(entries))
            return;
        List<gov.tak.api.video.ConnectionEntry> added = new ArrayList<>(
                entries.size());
        synchronized (this) {
            for (ConnectionEntry entry : entries) {
                final gov.tak.api.video.ConnectionEntry e = addEntryNoSync(
                        entry.get(), entry);
                if (e != null)
                    added.add(e);
            }
        }
        for (gov.tak.api.video.ConnectionEntry entry : added) {
            if (persist)
                persist(entry);
            for (ConnectionListener l : getListeners())
                l.onEntryAdded(entry);
        }
    }

    /**
     * Calls addEntries with the list and persists the entry to a file automatically
     * @param entries the list of entries
     * @deprecated use {@link #addConnectionEntries(List)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    public void addEntries(List<ConnectionEntry> entries) {
        addEntries(entries, true);
    }

    /**
     * Add a connection entry to the local cache
     *
     * @param entry Connection entry
     * @return ConnectionEntry if this entry was added/modified, null if no change
     */
    private gov.tak.api.video.ConnectionEntry addEntryNoSync(
            gov.tak.api.video.ConnectionEntry entry, ConnectionEntry legacy) {
        if (entry == null)
            return null;
        String uid = entry.getUID();
        if (FileSystemUtils.isEmpty(uid))
            return null; // Invalid UID
        EntryRecord existing = _entries.get(uid);
        if (existing != null && entry.equals(existing.value))
            return null; // No change
        if (existing != null) {
            existing.value.copy(entry);
            return existing.value;
        } else {
            _entries.put(uid, new EntryRecord(entry, legacy));
            return entry;
        }
    }

    /**
     * Remove an entry from the cache and file system
     * Note that this will remove non-remote videos as well
     *
     * @param uid Connection entry UID
     * @deprecated use {@link #removeConnectionEntry(String)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    public void removeEntry(String uid) {
        removeConnectionEntry(uid);
    }

    /**
     * Removes an entry from both the filesystem and the local in memory list.
     * @param entry the entry to be removed.
     * @deprecated use {@link #removeConnectionEntry(gov.tak.api.video.ConnectionEntry)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    public void removeEntry(ConnectionEntry entry) {
        if (entry != null)
            removeConnectionEntry(entry.get());
    }

    /**
     * Remove a set of entries by their UIDs
     *
     * @param uids Set of entry UIDs
     *             
     * @deprecated use {@link #removeConnectionEntries(Set)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    public void removeEntries(Set<String> uids) {
        removeConnectionEntries(uids);
    }

    private gov.tak.api.video.ConnectionEntry removeEntryNoSync(String uid) {
        EntryRecord record = _entries.remove(uid);
        if (record == null)
            return null;
        final gov.tak.api.video.ConnectionEntry removed = record.value;
        if (removed.isRemote()) {
            File file = new File(ENTRIES_DIR, uid + ".xml");
            if (IOProviderFactory.exists(file))
                FileSystemUtils.delete(file);
        }
        // if the removed entry is a child, remove it from its parent
        do {
            if (removed.getParentUID() == null)
                break;
            gov.tak.api.video.ConnectionEntry parent = getConnectionEntry(
                    removed.getParentUID());
            if (parent == null)
                break;

            // remove the child from its parent
            List<gov.tak.api.video.ConnectionEntry> children = parent
                    .getChildren();
            Iterator<gov.tak.api.video.ConnectionEntry> iter = children
                    .iterator();
            while (iter.hasNext()) {
                if (iter.next().getUID().equals(removed.getUID())) {
                    iter.remove();
                    break;
                }
            }
            parent.setChildren(children);
        } while (false);
        return removed;
    }

    /** @deprecated use {@link #persist(gov.tak.api.video.ConnectionEntry)}  */
    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    public void persist(ConnectionEntry entry) {
        persist(entry.get());
    }

    /**
     * Persist a connection entry to the file system
     * Entries use their UID for the file name
     *
     * @param entry Connection entry
     */
    public void persist(gov.tak.api.video.ConnectionEntry entry) {
        if (!entry.isRemote() || entry.isTemporary())
            return;
        String uid = entry.getUID();
        if (FileSystemUtils.isEmpty(uid))
            return;
        File file = entry.getLocalFile();
        if (!FileSystemUtils.isFile(file)) {
            file = new File(ENTRIES_DIR, entry.getUID() + ".xml");
            entry.setLocalFile(file);
        }
        _xmlHandler.write(entry, file);
    }

    /**
     * Get the video XML handler used by the manager
     * Using this handler over a newly created one prevents sync issues
     * when reading and writing entries
     *
     * @return XML handler
     */
    public VideoXMLHandler getXMLHandler() {
        return _xmlHandler;
    }

    /**
     * Add a listener for manager events
     *
     * @param l Listener
     *
     * @deprecated use {@link #addConnectionListener(ConnectionListener)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    public synchronized void addListener(Listener l) {
        for (ConnectionListener cl : _listeners) {
            if ((cl instanceof ListenerAdapter)
                    && ((ListenerAdapter) cl)._impl == l)
                return;
        }
        _listeners.add(new ListenerAdapter(l));
    }

    /** @deprecated use {@link #removeConnectionListener(ConnectionListener)} */
    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    public synchronized void removeListener(Listener l) {
        for (ConnectionListener cl : _listeners) {
            if ((cl instanceof ListenerAdapter)
                    && ((ListenerAdapter) cl)._impl == l) {
                _listeners.remove(l);
                return;
            }
        }
    }

    private synchronized List<ConnectionListener> getListeners() {
        return new ArrayList<>(_listeners);
    }

    @Override
    public gov.tak.api.video.ConnectionEntry getConnectionEntry(String uid) {
        final Pair<gov.tak.api.video.ConnectionEntry, ConnectionEntry> entry = getConnectionEntryImpl(
                uid, false);
        return (entry != null) ? entry.first : null;
    }

    private synchronized Pair<gov.tak.api.video.ConnectionEntry, ConnectionEntry> getConnectionEntryImpl(
            String uid, boolean resolveLegacy) {
        EntryRecord record = _entries.get(uid);
        if (record == null)
            return null;
        ConnectionEntry legacy = record.legacyRef.get();
        if (legacy == null && resolveLegacy) {
            legacy = new ConnectionEntry(record.value);
            record.legacyRef = new WeakReference<>(legacy);
        }
        return Pair.create(record.value, legacy);
    }

    @Override
    public synchronized List<gov.tak.api.video.ConnectionEntry> getConnectionEntries(
            Set<String> uids, boolean remoteOnly) {
        return new ArrayList<>(
                getConnectionEntries(uids, remoteOnly, false).keySet());
    }

    private synchronized Map<gov.tak.api.video.ConnectionEntry, ConnectionEntry> getConnectionEntries(
            Set<String> uids, boolean remoteOnly, boolean resolveLegacyRefs) {
        Map<gov.tak.api.video.ConnectionEntry, ConnectionEntry> ret = new HashMap<>();
        for (EntryRecord entry : _entries.values()) {
            if ((uids == null || uids.contains(entry.value.getUID()))
                    && (!remoteOnly || entry.value.isRemote())) {
                ConnectionEntry legacy = entry.legacyRef.get();
                if (legacy == null && resolveLegacyRefs) {
                    legacy = new ConnectionEntry(entry.value);
                    entry.legacyRef = new WeakReference<>(legacy);
                }
                ret.put(entry.value, legacy);
            }
        }
        return ret;
    }

    @Override
    public void addConnectionEntry(gov.tak.api.video.ConnectionEntry entry) {
        final gov.tak.api.video.ConnectionEntry e;
        synchronized (this) {
            e = addEntryNoSync(entry, null);
        }
        if (e != null) {
            persist(e);
            for (ConnectionListener l : getListeners())
                l.onEntryAdded(e);
        }
        AtakBroadcast.getInstance().sendBroadcast(
                new Intent(HierarchyListReceiver.REFRESH_HIERARCHY));
    }

    @Override
    public void addConnectionEntries(
            List<gov.tak.api.video.ConnectionEntry> entries) {
        addConnectionEntries(entries, true);
    }

    /**
     * Add a list of connection entries to the manager
     *
     * @param entries List of connection entries
     */
    public void addConnectionEntries(
            List<gov.tak.api.video.ConnectionEntry> entries, boolean persist) {
        if (FileSystemUtils.isEmpty(entries))
            return;
        List<gov.tak.api.video.ConnectionEntry> added = new ArrayList<>(
                entries.size());
        synchronized (this) {
            for (gov.tak.api.video.ConnectionEntry entry : entries) {
                final gov.tak.api.video.ConnectionEntry e = addEntryNoSync(
                        entry, null);
                if (e != null)
                    added.add(e);
            }
        }
        for (gov.tak.api.video.ConnectionEntry entry : added) {
            if (persist)
                persist(entry);
            for (ConnectionListener l : getListeners())
                l.onEntryAdded(entry);
        }
        AtakBroadcast.getInstance().sendBroadcast(
                new Intent(HierarchyListReceiver.REFRESH_HIERARCHY));
    }

    @Override
    public void removeConnectionEntry(String uid) {
        gov.tak.api.video.ConnectionEntry removed;
        synchronized (this) {
            removed = removeEntryNoSync(uid);
        }

        // Notify listeners
        if (removed != null) {
            for (ConnectionListener l : getListeners())
                l.onEntryRemoved(removed);
        }
        AtakBroadcast.getInstance().sendBroadcast(
                new Intent(HierarchyListReceiver.REFRESH_HIERARCHY));
    }

    @Override
    public void removeConnectionEntry(gov.tak.api.video.ConnectionEntry entry) {
        if (entry == null)
            return;

        // Delete local content
        if (!entry.isRemote()) {
            File file = new File(entry.getPath());
            List<gov.tak.api.video.ConnectionEntry> children = entry
                    .getChildren();
            if (!FileSystemUtils.isEmpty(children)) {
                for (gov.tak.api.video.ConnectionEntry child : children)
                    removeConnectionEntry(child.getUID());
            }
            if (!IOProviderFactory.delete(file, IOProvider.SECURE_DELETE))
                return;
        }

        removeEntry(entry.getUID());
    }

    @Override
    public void removeConnectionEntries(Set<String> uids) {
        if (uids == null || uids.isEmpty())
            return;
        List<gov.tak.api.video.ConnectionEntry> removed = new ArrayList<>();
        synchronized (this) {
            for (String uid : uids) {
                gov.tak.api.video.ConnectionEntry entry = removeEntryNoSync(
                        uid);
                if (entry != null)
                    removed.add(entry);
            }
        }
        if (removed.isEmpty())
            return;
        List<ConnectionListener> listeners = getListeners();
        for (gov.tak.api.video.ConnectionEntry entry : removed) {
            for (ConnectionListener l : listeners)
                l.onEntryRemoved(entry);
        }
        AtakBroadcast.getInstance().sendBroadcast(
                new Intent(HierarchyListReceiver.REFRESH_HIERARCHY));
    }

    @Override
    public File getConnectionEntriesDirectory() {
        return ENTRIES_DIR;
    }

    @Override
    public synchronized void addConnectionListener(ConnectionListener l) {
        _listeners.add(l);
    }

    @Override
    public synchronized void removeConnectionListener(ConnectionListener l) {
        _listeners.remove(l);
    }

    /**
     * Listener for add/remove events
     */
    public interface Listener {

        /**
         * Connection entry was added or updated to the video manager
         *
         * @param entry Connection entry
         */
        void onEntryAdded(ConnectionEntry entry);

        /**
         * Connection entry was removed from the video manager
         *
         * @param entry Connection entry
         */
        void onEntryRemoved(ConnectionEntry entry);
    }

    private static class EntryRecord {
        gov.tak.api.video.ConnectionEntry value;
        WeakReference<ConnectionEntry> legacyRef;

        public EntryRecord(gov.tak.api.video.ConnectionEntry entry,
                ConnectionEntry legacy) {
            this.value = entry;
            this.legacyRef = new WeakReference<>(legacy);
        }
    }

    final class ListenerAdapter implements ConnectionListener {

        final Listener _impl;

        ListenerAdapter(Listener impl) {
            _impl = impl;
        }

        @Override
        public void onEntryAdded(gov.tak.api.video.ConnectionEntry entry) {
            final Pair<gov.tak.api.video.ConnectionEntry, ConnectionEntry> record = getConnectionEntryImpl(
                    entry.getUID(), true);
            if (record != null)
                _impl.onEntryAdded(record.second);
        }

        @Override
        public void onEntryRemoved(gov.tak.api.video.ConnectionEntry entry) {
            final Pair<gov.tak.api.video.ConnectionEntry, ConnectionEntry> record = getConnectionEntryImpl(
                    entry.getUID(), true);
            if (record != null)
                _impl.onEntryRemoved(record.second);
        }
    }
}
