package gov.tak.api.video;

import java.io.File;
import java.util.List;
import java.util.Set;

import gov.tak.api.annotation.Nullable;

public interface IVideoConnectionManager
{
    ConnectionEntry getConnectionEntry(String uid); // -

    /**
     * Get a list of registered connection entries
     *
     * @param uids          Entry UIDs. If {@code null} no UID filter is applied
     * @param remoteOnly    If {@code true} only remote connections will be returned
     * @return Connection entries
     */
    List<ConnectionEntry> getConnectionEntries(@Nullable Set<String> uids, boolean remoteOnly);

    /**
     * Add an entry to the manager (which is then persisted to the filesystem)
     *
     * @param entry Connection entry
     */
    void addConnectionEntry(ConnectionEntry entry);

    /**
     * Calls addEntries with the list and persists the entry to a file automatically
     * @param entries the list of entries
     */
    void addConnectionEntries(List<ConnectionEntry> entries);

    /**
     * Remove an entry from the cache and file system
     * Note that this will remove non-remote videos as well
     *
     * @param uid Connection entry UID
     */
    void removeConnectionEntry(String uid);

    /**
     * Removes an entry from both the filesystem and the local in memory list.
     * @param entry the entry to be removed.
     */
    void removeConnectionEntry(ConnectionEntry entry);

    /**
     * Remove a set of entries by their UIDs
     *
     * @param uids Set of entry UIDs
     */
    void removeConnectionEntries(Set<String> uids);


    /**
     * Add a listener for manager events
     *
     * @param l Listener
     */
    void addConnectionListener(ConnectionListener l);
    void removeConnectionListener(ConnectionListener l);

    File getConnectionEntriesDirectory();

    /**
     * Listener for add/remove events
     */
    interface ConnectionListener {

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
}
