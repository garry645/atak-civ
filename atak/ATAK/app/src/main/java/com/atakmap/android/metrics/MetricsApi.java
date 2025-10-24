
package com.atakmap.android.metrics;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.util.ArrayList;
import java.util.List;

import gov.tak.api.util.AttributeSet;

/**
 * Simple interface for tools and plugins to log metrics records.  Implementation for metrics
 * collection and handling should be done as a plugin so that it not adversely effect system
 * performance.
 */
public class MetricsApi {

    // note - this uses local broadcasts to implement the record methods 
    // because they will occur asynchronously instead of a add/remove listener
    // paradigm.   This class also cannot call AtakBroadcast because AtakBroadcast
    // directly calls into this class when firing intents.

    private static final String METRIC_INTENT = "com.atakmap.metric";

    private static LocalBroadcastManager lbm;

    private static final List<BroadcastReceiver> registered = new ArrayList<>();

    public static final String METRIC_KEY_CATEGORY = "category";
    public static final String METRIC_KEY_BUNDLE = "bundle";
    public static final String METRIC_KEY_INTENT = "intent";
    public static final String METRIC_KEY_STACKTRACE = "stacktrace";

    private static final AttributeSet historicValues = new AttributeSet();

    /**
     * Must be called only once by the AtakBroadcast class. 
     * Prevent future reinitialization.
     * @param lbm the local broadcast manager in use by the metrics api.
     */
    synchronized public static void init(final LocalBroadcastManager lbm) {
        if (MetricsApi.lbm == null) {
            MetricsApi.lbm = lbm;
        }

    }

    /**
     * Call to register a BroadcastReceiver that will obtain metric information.
     */
    public static void register(BroadcastReceiver receiver) {
        // note - does not require synchronization because it is only used for 
        // book keeping and not iteration.
        registered.add(receiver);
        try {
            AtakBroadcast.getInstance().registerReceiver(receiver,
                    new DocumentedIntentFilter(METRIC_INTENT));
        } catch (Exception ignored) {
        }
    }

    /**
     * Call to unregister a BroadcastReceiver that will obtain metric information.
     */
    public static void unregister(BroadcastReceiver receiver) {
        // note - does not require synchronization because it is only used for 
        // book keeping and not iteration.
        registered.remove(receiver);
        try {
            AtakBroadcast.getInstance().unregisterReceiver(receiver);
        } catch (Exception ignored) {
        }
    }

    /**
     * Should be called prior to calling record metric in order to make sure 
     * that the recording will be used.
     */
    public static boolean shouldRecordMetric() {
        return !registered.isEmpty();
    }

    /**
     * Used to record an nonnull intent event.   This call will accurately 
     * capture all attributes related to the intent and pass them onto 
     * passive listeners that implement MetricsApi.METRIC_INTENT broadcast
     * receiver.
     * @param intent either a local or system intent.
     */
    public static void record(final Intent intent) {
        if (intent != null) {
            Intent metricIntent = new Intent(MetricsApi.METRIC_INTENT);
            metricIntent.putExtra(METRIC_KEY_STACKTRACE, getBackStack());

            if (intent.hasExtra(MetricsUtils.INTENT_EXTRA_CATEGORY)) {
                metricIntent.putExtra(METRIC_KEY_CATEGORY, intent
                        .getStringExtra(MetricsUtils.INTENT_EXTRA_CATEGORY));
            } else {
                metricIntent.putExtra(METRIC_KEY_CATEGORY,
                        MetricsUtils.CATEGORY_UNKNOWN);
            }

            Bundle bundle = new Bundle();
            bundle.putParcelable(METRIC_KEY_INTENT, intent);
            if (intent.hasExtra(MetricsUtils.INTENT_EXTRA_CLASS)) {
                bundle.putString(MetricsUtils.FIELD_CLASS,
                        intent.getStringExtra(MetricsUtils.INTENT_EXTRA_CLASS));
            }
            if (intent.hasExtra(MetricsUtils.INTENT_EXTRA_ELEMENT)) {
                bundle.putString(MetricsUtils.FIELD_ELEMENT_NAME, intent
                        .getStringExtra(MetricsUtils.INTENT_EXTRA_ELEMENT));
            }
            metricIntent.putExtra(METRIC_KEY_BUNDLE, bundle);

            lbm.sendBroadcast(metricIntent);
        }
    }

    /**
     * Used to record a category and a bundle, this call will pass the bundle onto 
     * passive listeners that implement the MetricApis.METRIC_INTENT broadcast
     * receiver.
     * @param category is the category used to bin the bundle
     * @param b the name value pairs passed in as a bundle
     */
    public static void record(final String category, final Bundle b) {
        if (b != null) {
            Intent metricIntent = new Intent(MetricsApi.METRIC_INTENT);
            if (!FileSystemUtils.isEmpty(category))
                metricIntent.putExtra(METRIC_KEY_CATEGORY, category);
            metricIntent.putExtra(METRIC_KEY_BUNDLE, b);
            lbm.sendBroadcast(metricIntent);
        }
    }

    private static String[] getBackStack() {
        Thread thread = Thread.currentThread();
        StackTraceElement[] elements = thread.getStackTrace();
        String[] retval = new String[Math.min(elements.length - 4, 12)];
        for (int i = 0; i < retval.length; ++i) {
            retval[i] = elements[i + 4].toString();
        }
        return retval;
    }

    /**
     * For one off values that might be recorded prior to the plugin being loaded or might
     * change infrequently such as the ATAK load time.
     * @param name the key for the value to be recorded
     * @param value the value to be recorded
     */
    public void recordValue(String name, Object value) {
        if (value instanceof String) {
            historicValues.setAttribute(name, (String)value);
        } else if (value instanceof Double) {
            historicValues.setAttribute(name, (Double)value);
        } else if (value instanceof Integer) {
            historicValues.setAttribute(name, (Integer) value);
        }
    }

    /**
     * Obtains a set of historic values that are being recorded for metric purposes as entered
     * by recordValue(String, Object)
     * @return a copy of the internal attribute set
     */
    public static AttributeSet getRecordedValues() {
        return new AttributeSet(historicValues);
    }
}
