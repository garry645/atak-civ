
package com.atakmap.android.util;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * Service for gating less "up-to-date" times from triggering some processing done
 * by the calling user code. The current and most common use is the CotImporterManager
 * can throw out events that should not apply since they are out of date.
 */
public class TimeTrackingProcessService {

    private final HashMap<String, TimeRecord> timeRecords = new HashMap<>();
    private final Map<String, PendingToken> pendingProcesses = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            ? new ConcurrentHashMap<>()
            : new HashMap<>();

    private static class TimeRecord {

        TimeRecord(String uid, long ts) {
            this.timestamp = ts;
            this.uid = uid;
        }

        private final long timestamp;
        private final String uid;
    }

    /**
     * Token used for user code tracking and triggering commit or cancel of a process
     */
    public static class PendingToken {

        private final TimeRecord timeRecord;

        private final long newTimestamp;

        private final Callbacks callbacks;

        private PendingToken(String uid, long timestamp, Callbacks cb) {
            this.timeRecord = new TimeRecord(uid, timestamp);
            this.newTimestamp = timestamp;
            this.callbacks = cb;
        }

        private PendingToken(TimeRecord timeRecord, long newTimestamp,
                Callbacks cb) {
            this.timeRecord = timeRecord;
            this.newTimestamp = newTimestamp;
            this.callbacks = cb;
        }

        /**
         * Get the timestamp of the pending process
         * @return CoT timestamp
         */
        public long getPendingTimestamp() {
            return newTimestamp;
        }

        /**
         * Get the timestamp of the previous recorded record
         * @return CoT timestamp
         */
        public long getRecordedTimestamp() {
            return timeRecord.timestamp;
        }

        /**
         * The CoT UID
         * @return UID
         */
        public String getUid() {
            return timeRecord.uid;
        }
    }

    /**
     * Callbacks used by the service to notify user code of various events.
     */
    public interface Callbacks {
        /**
         * Called when a process token is overshadowed by another process
         *
         * @param shadowedProcess the process that has been overshadowed
         */
        void onPendingEventProcessOvershadowed(PendingToken shadowedProcess);
    }

    /**
     * Begin a process with a given time. The result of begin is 'null' when a process
     * exists that already has a greater time OR a time record exists with a later time.
     *
     * @param uid Unique CoT Event ID
     * @param time CoT time to consider as latest pending process
     *
     * @return an instance of PendingToken IFF the time supersedes the current time record or no
     *         process is pending and no time record exists.
     */
    public PendingToken begin(@NonNull String uid,
            @NonNull CoordinatedTime time) {
        return begin(uid, time.getMilliseconds());
    }

    /**
     * Begin a process with a given time. The result of begin is 'null' when a process
     * exists that already has a greater time OR a time record exists with a later time.
     *
     * @param uid Unique CoT Event ID
     * @param timestamp CoT time to consider as latest pending process
     * @return an instance of PendingToken IFF the time supersedes the current time record or no
     *         process is pending and no time record exists.
     */
    public PendingToken begin(@NonNull String uid, long timestamp) {
        return begin(uid, timestamp, null);
    }

    /**
     * Begin a process with a given time. The result of begin is 'null' when a process
     * exists that already has a greater time OR a time record exists with a later time.
     *
     * @param uid Unique CoT Event ID
     * @param timestamp CoT time to consider as latest pending process
     * @param callbacks Callbacks instance for handling changes in process token status
     * @return an instance of PendingToken IFF the time supersedes the current time record or no
     *         process is pending and no time record exists.
     */
    public PendingToken begin(@NonNull String uid, long timestamp,
            Callbacks callbacks) {

        final PendingToken[] overshadowed = {
                null
        };
        final PendingToken[] result = {
                null
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pendingProcesses.compute(uid,
                    new BiFunction<String, PendingToken, PendingToken>() {
                        @Override
                        public PendingToken apply(String s,
                                PendingToken existing) {
                            return getReplacementPendingToken(existing,
                                    timestamp, overshadowed, result, callbacks,
                                    uid);
                        }
                    });
        } else {
            synchronized (pendingProcesses) {
                PendingToken existing = pendingProcesses.get(uid);
                PendingToken replacement = getReplacementPendingToken(existing,
                        timestamp, overshadowed, result, callbacks, uid);
                if (replacement != existing)
                    pendingProcesses.put(uid, replacement);
            }
        }

        if (overshadowed[0] != null && overshadowed[0].callbacks != null)
            overshadowed[0].callbacks
                    .onPendingEventProcessOvershadowed(overshadowed[0]);

        return result[0];
    }

    @Nullable
    private PendingToken getReplacementPendingToken(PendingToken existing,
            long timestamp, PendingToken[] overshadowed, PendingToken[] result,
            Callbacks callbacks, @NonNull String uid) {
        if (existing != null) {
            if (timestamp > existing.newTimestamp) {
                overshadowed[0] = existing;
                return (result[0] = new PendingToken(existing.timeRecord,
                        timestamp, callbacks));
            }
        } else {
            TimeRecord timeRecord = timeRecords.get(uid);
            if (timeRecord == null)
                timeRecord = new TimeRecord(uid, 0);
            if (timestamp > timeRecord.timestamp)
                return result[0] = new PendingToken(timeRecord, timestamp,
                        callbacks);
        }
        return existing;
    }

    /**
     * Commit a pending process
     *
     * @param token the pending process token
     * @return true if committed as the latest time record, otherwise false if it was rejected as
     *         there is a later token pending
     */
    public boolean commit(@NonNull PendingToken token) {

        final boolean[] result = {
                false
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pendingProcesses.compute(token.timeRecord.uid,
                    new BiFunction<String, PendingToken, PendingToken>() {
                        @Override
                        public PendingToken apply(String uid,
                                PendingToken pending) {
                            if (pending == token) {
                                result[0] = true;
                                timeRecords.put(uid, new TimeRecord(uid,
                                        token.newTimestamp));
                                return null;
                            }
                            return pending;
                        }
                    });
        } else {
            final String uid = token.timeRecord.uid;
            synchronized (pendingProcesses) {
                PendingToken pending = pendingProcesses.get(uid);
                if (pending == token) {
                    result[0] = true;
                    timeRecords.put(uid,
                            new TimeRecord(uid, token.newTimestamp));
                    pendingProcesses.remove(uid);
                }
            }
        }

        return result[0];
    }

    /**
     * Cancels a pending process token
     * @param token the pending process token
     * @return true if the token was the latest and false if there is another later pending token
     */
    public boolean cancel(PendingToken token) {
        final boolean[] result = {
                false
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pendingProcesses.compute(token.timeRecord.uid,
                    new BiFunction<String, PendingToken, PendingToken>() {
                        @Override
                        public PendingToken apply(String uid,
                                PendingToken pending) {
                            if (pending == token) {
                                result[0] = true;
                                return null;
                            }
                            return pending;
                        }
                    });
        } else {
            final String uid = token.timeRecord.uid;
            synchronized (pendingProcesses) {
                PendingToken pending = pendingProcesses.get(uid);
                if (pending == token) {
                    result[0] = true;
                    pendingProcesses.remove(uid);
                }
            }
        }
        return result[0];
    }
}
