
package com.atakmap.android.test.helpers;

import android.app.Activity;
import androidx.test.rule.ActivityTestRule;

import com.atakmap.android.toolbars.DynamicRangeAndBearingTool;
import com.atakmap.android.tools.ActionBarReceiver;

import java.lang.reflect.Field;

/**
 * Special test rule for ATAK Activity to run multiple tests.
 * <p>
 * Created by vmm0613 on 7/5/2017.
 */
public class ATAKTestRule<T extends Activity> extends ActivityTestRule<T> {
    public ATAKTestRule(Class<T> activityClass) {
        super(activityClass);
    }

    public ATAKTestRule(Class<T> activityClass, boolean initialTouchMode) {
        super(activityClass, initialTouchMode);
    }

    public ATAKTestRule(Class<T> activityClass, boolean initialTouchMode,
            boolean launchActivity) {
        super(activityClass, initialTouchMode, launchActivity);
    }

    @Override
    protected void afterActivityFinished() {
        super.afterActivityFinished();
        try {
            // This code aims to patch in nulling things out in ATAK so it can follow a standard
            // Android lifecycle, being able to fully stop and start again between tests without
            // needing to call System.exit() in between.
            //
            // However, this is incomplete so it isn't actually used currently
            Field field = DynamicRangeAndBearingTool.class
                    .getDeclaredField("_instance");
            field.setAccessible(true);
            field.set(DynamicRangeAndBearingTool.class, null);
            Field field2 = ActionBarReceiver.class
                    .getDeclaredField("_instance");
            field2.setAccessible(true);
            field2.set(ActionBarReceiver.class, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
