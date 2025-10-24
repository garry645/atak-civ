
package com.atakmap.comms;

import android.os.Bundle;

import com.atakmap.android.contact.Contact;
import com.atakmap.android.util.ParallelTrackExecutorService;
import com.atakmap.commoncommo.CoTSendMethod;
import com.atakmap.coremap.concurrent.NamedThreadFactory;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Dispatches CoT event Messages. The interface to this class is thread safe.
 */
public class CotDispatcher {

    public static final String TAG = "CotDispatcher";
    // Determines how messages are routed (internally or externally)
    private volatile int flags = DispatchFlags.INTERNAL;

    private final ExecutorService internalDispatchExecutor;
    private final ParallelTrackExecutorService parallelInternalDispatchExecutor;

    private static final ParallelTrackExecutorService sharedConcurrentInternalDispatchExecutor = new ParallelTrackExecutorService(
            4, // based on latest testing, anything more has diminishing returns
            60, // keep threads around for 60 seconds (same as default thread-pool executor)
            TimeUnit.SECONDS,
            new NamedThreadFactory(TAG));

    /**
     * Get the Application-wide shared ExecutorService tuned for handling INTERNAL dispatched
     * CoT efficiently and concurrently. The executor service is guaranteed to be a first-in, first
     * executed, however, simultaneous updates are possible and must be accounted for in internal
     * CoT handling.
     *
     * @return the shared ExecutorService
     */
    public static ExecutorService getSharedConcurrentInternalDispatchExecutor() {
        return sharedConcurrentInternalDispatchExecutor;
    }

    /**
     * Get the Application-wide shared ParallelTrackExecutorService tuned for handling INTERNAL
     * CoT efficiently and concurrently. This ParallelTrackExecutorService differs from the
     * getSharedConcurrentInternalDispatchExecutor ExecutorService in that it guarantees
     * CoT Events with the same UID are processed serially.
     *
     * @return the shared ParallelTrackExecutorService
     */
    public static ParallelTrackExecutorService getSharedConcurrentInternalDispatchTrackExecutor() {
        return sharedConcurrentInternalDispatchExecutor;
    }

    /**
     * Create a default dispatcher that handles INTERNAL dispatched CoT events immediately on
     * the calling thread.
     */
    public CotDispatcher() {
        this(null);
    }

    /**
     * Create a dispatcher with an executor to handle internally routed CoT events. Passing null
     * for the executor results in handling of INTERNAL CoT immediately on the calling thread.
     *
     * @param internalDispatchExecutor the executor to handle
     */
    public CotDispatcher(ExecutorService internalDispatchExecutor) {
        this.internalDispatchExecutor = internalDispatchExecutor;
        this.parallelInternalDispatchExecutor = null;
    }

    /**
     * Create a dispatcher with a "parallel track" executor to handle internally routed CoT events.
     * Passing null for the executor results in handling of INTERNAL CoT immediately on the calling
     * thread. The passed ParallelTrackExecutorService will be used such that each UID is guaranteed
     * to be handled serially.
     *
     * @param internalDispatchParallelTrackExecutor the instance of ParallelTrackExecutorService or
     *                                              null
     */
    public CotDispatcher(
            ParallelTrackExecutorService internalDispatchParallelTrackExecutor) {
        this.parallelInternalDispatchExecutor = internalDispatchParallelTrackExecutor;
        this.internalDispatchExecutor = internalDispatchParallelTrackExecutor;
    }

    /**
     * Set the dispatch flags to determine basic dispatch behavior (@see DispatchFlags).
     *
     * @param flags one of INTERNAL or EXTERNAL.
     */
    public final void setDispatchFlags(final int flags) {
        this.flags = flags;
    }

    /**
     * Used to dispatch a CotEvent with no additional directives passed in as a bundle.   This method
     * uses the dispatch flags set on the CoTDispatcher. If you want to pass in additional directives
     * such as the "from" field, please use dispatch(CotEvent, Bundle)
     * @param event the cot event to dispatch
     */
    public final void dispatch(final CotEvent event) {
        dispatch(event, null);
    }

    /**
     * Used to dispatch a CotEvent with no additional directions passed in as a bundle.   This method
     * uses the dispatch flags set on the CoTDispatcher.
     * <p>
     * Note:  If the bundle contains the "from" field and it is "internal" or is missing, the data
     * is assumed to be internally generated and may trigger different code paths.
     *
     * @param event the cot event to dispatch
     */
    public void dispatch(final CotEvent event, final Bundle data) {
        boolean reliable = (flags & DispatchFlags.DISPATCH_RELIABLE) != 0;
        boolean unreliable = (flags & DispatchFlags.DISPATCH_UNRELIABLE) != 0;
        if (reliable && !unreliable) {
            dispatch(event, data, CoTSendMethod.TAK_SERVER);
        } else if (unreliable && !reliable) {
            dispatch(event, data, CoTSendMethod.POINT_TO_POINT);
        } else {
            dispatch(event, data, CoTSendMethod.ANY);
        }
    }

    /**
     * Returns the contacts which are unknown, known to no longer be reachable, or 
     * who do not match the provided CoTSendMethod.  Will always return an empty list
     * for data Bundles that contain connect strings and no contacts, broadcasts
     * (no contacts specified), or for internal-only dispatches since no actual
     * remote contacts are involved.
     * <p>
     * Data may contain a list of destination contacts
     * "toUIDs" is a list of UIDs
     * "toConnectStrings" is a list of stringified <code>{@link NetConnectString}</code>
     */
    public List<String> dispatch(final CotEvent event, final Bundle data,
            final CoTSendMethod sendMethod) {
        List<String> ret = new ArrayList<>();
        String[] toUIDs = null;
        String[] toConnectStrings = null;
        boolean broadcast = false;
        if (data == null) {
            broadcast = true;
        } else {
            toUIDs = data.getStringArray("toUIDs");
            if (toUIDs == null) {
                toConnectStrings = data.getStringArray("toConnectStrings");
            }
        }

        if ((flags & DispatchFlags.DISPATCH_EXTERNAL) != 0) {
            if (broadcast || toUIDs != null || toConnectStrings == null)
                CommsMapComponent.getInstance().sendCoT(ret, event, toUIDs,
                        sendMethod);
            else {
                Log.w(TAG,
                        "Got a dispatchEvent command w/o Contacts, using OLD NetConnectStr method... ",
                        new Exception());
                for (String toConnectString : toConnectStrings)
                    dispatchToConnectString(event, toConnectString);
            }
        }
        if ((flags & DispatchFlags.DISPATCH_INTERNAL) != 0) {
            if (!tryDispatchWithExecutor(event, data)) {
                CommsMapComponent.getInstance().sendCoTInternally(event, data);
            }
        }
        return ret;
    }

    /**
     * Instead of the connect string, we will just need to pass down the appropriate 
     * multicast information.
     * @param event the cot event to dispatch
     * @param connectString the connect string to send the cot event too.
     */
    public final void dispatchToConnectString(final CotEvent event,
            final String connectString) {
        CommsMapComponent.getInstance().sendCoTToEndpoint(event, connectString);
    }

    /**
     * Send a CotEvent to a specific contact by any known connection type.
     * @param event the cot event to dispatch
     * @param contact the contact to send the event to
     */
    public final void dispatchToContact(CotEvent event, Contact contact) {
        dispatchToContact(event, contact, CoTSendMethod.ANY);
    }

    /**
     * Send a CotEvent to a specific contact by a specified sending method.
     * @param event the cot event to dispatch
     * @param contact the contact to send the event to
     * @param method can be ANY, TAK_SERVER or POINT TO POINT.
     */
    public final void dispatchToContact(CotEvent event, Contact contact,
            CoTSendMethod method) {
        String[] c;
        if (contact == null)
            c = null;
        else
            c = new String[] {
                    contact.getUID()
            };
        CommsMapComponent.getInstance().sendCoT(null, event, c, method);
    }

    /**
     * Send a CotEvent to a specific contact by any known connection type.
     * @param event the cot event to dispatch
     * @param contacts the list of contacts to send the event to
     */
    public final void dispatchToContacts(CotEvent event,
            Collection<Contact> contacts) {
        dispatchToContacts(event, contacts, CoTSendMethod.ANY);
    }

    /**
     * Send a CotEvent to a specific contact by a specified sending method.
     * @param event the cot event to dispatch
     * @param contacts the list of contacts to send the event to
     * @param method can be ANY, TAK_SERVER or POINT TO POINT.
     */
    public final void dispatchToContacts(CotEvent event,
            Collection<Contact> contacts,
            CoTSendMethod method) {
        String[] c;
        if (contacts == null) {
            c = null;
        } else {
            c = new String[contacts.size()];
            int i = 0;
            for (Contact cont : contacts) {
                c[i] = cont.getUID();
                i++;
            }
        }
        CommsMapComponent.getInstance().sendCoT(null, event, c, method);
    }

    /**
     * Sends a CoTEvent out as a broadcast over all connections, streaming and peer to peer.
     * @param event the event to dispatch.
     */
    public final void dispatchToBroadcast(CotEvent event) {
        dispatchToBroadcast(event, CoTSendMethod.ANY);
    }

    /**
     * Sends a CoTEvent out as a broadcast over a specified connection type.
     * @param event the event to dispatch.
     * @param method can be ANY, TAK_SERVER or POINT TO POINT.
     */
    public final void dispatchToBroadcast(CotEvent event,
            CoTSendMethod method) {
        CommsMapComponent.getInstance().sendCoT(null, event, null, method);
    }

    /**
     * Used as an internal dispatcher with the fromString only specifying which internal component sent it.
     *
     * @param event the cot event to dispatch
     * @param fromString what component is sending this event.
     */
    public final void dispatchFrom(CotEvent event, String fromString) {
        Bundle extras = new Bundle();
        extras.putString("from", fromString);
        CommsMapComponent.getInstance().sendCoTInternally(event, extras);
    }

    private String toDebugString(Bundle b) {
        StringBuilder sb = new StringBuilder();
        final Set<String> keySet = b.keySet();
        for (final String key : keySet) {
            sb.append('\"');
            sb.append(key);
            sb.append("\"=\"");
            sb.append(b.get(key));
            sb.append("\", ");
        }
        return sb.toString();
    }

    private boolean tryDispatchWithExecutor(CotEvent event, Bundle data) {

        if (parallelInternalDispatchExecutor == null
                && internalDispatchExecutor == null)
            return false;

        final CotEvent eventCopy = new CotEvent(event);
        final Bundle dataCopy = data != null ? new Bundle(data) : null;
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                CommsMapComponent.getInstance().sendCoTInternally(eventCopy,
                        dataCopy);
            }
        };

        final Future<?> future;
        if (parallelInternalDispatchExecutor != null) {
            final String eventUID = eventCopy.getUID();

            // in that bad case where a plugin has constructed a CotEvent without providing a UID
            // then fallback to previously implemented behavior
            if (eventUID == null)
                return false;

            future = parallelInternalDispatchExecutor.submit(task, eventUID);
        } else {
            future = internalDispatchExecutor.submit(task);
        }
        // running parallel, calling code must WAIT unless flagged otherwise to meet legacy
        // behavior
        if ((flags & DispatchFlags.NO_WAIT) == 0) {
            try {
                future.get();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }
}
