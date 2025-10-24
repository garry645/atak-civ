
package com.atakmap.android.test.helpers;

import android.app.Activity;
import androidx.test.rule.ActivityTestRule;
import android.util.Log;

/**
 * this is a rule that does not finish the activity under test at the end of the test.
 * <p>
 * useful for debugging and development. use in the same way as ActivityTestRule.
 * <p>
 * From: https://gist.github.com/dbachelder/4d0588ab6adf0aa6e69a, with the addition of
 * reallyFinishActivity for when the activity is really ready to be finished.
 */
public class NoFinishActivityTestRule<T extends Activity>
        extends ActivityTestRule<T> {
    private static final String TAG = "NoFinishActivityRule";

    public NoFinishActivityTestRule(Class<T> activityClass) {
        super(activityClass);
    }

    public NoFinishActivityTestRule(Class<T> activityClass,
            boolean initialTouchMode) {
        super(activityClass, initialTouchMode);
    }

    public NoFinishActivityTestRule(Class<T> activityClass,
            boolean initialTouchMode, boolean launchActivity) {
        super(activityClass, initialTouchMode, launchActivity);
    }

    public void finishActivity() {
        // do nothing on purpose.
        Log.d(TAG, "Rule attempted to finish activity and we didn't!");
    }

    public void reallyFinishActivity() {
        super.finishActivity();
    }
}
