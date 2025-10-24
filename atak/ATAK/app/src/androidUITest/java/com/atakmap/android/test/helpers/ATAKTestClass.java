
package com.atakmap.android.test.helpers;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.atakmap.android.test.helpers.helper_versions.HelperFactory;
import com.atakmap.android.test.helpers.helper_versions.HelperFunctions;
import com.atakmap.app.ATAKActivity;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

/**
 * Rules and other things common to all ATAK UI tests
 * <p>
 * Created by tcs4465 on 10/5/18.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public abstract class ATAKTestClass {

    static protected HelperFunctions helper = HelperFactory.getHelper();

    // Rule to take screenshot when tests fail
    @Rule
    public RuleChain screenshotRuleChain = RuleChain
            .outerRule(mActivityRule)
            .around(new ScreenshotWatcher());

    // I don't thing we rely on this for anything (ATAKStarter does everything I know of we need it to do)
    // But if we don't specify it a regular ActivityTestRule appears to run, which causes issues!
    @ClassRule
    public static NoFinishActivityTestRule<ATAKActivity> mActivityRule = new NoFinishActivityTestRule<>(
            ATAKActivity.class, false, false);

    /**
     * Handles setup needed for all ATAK UI tests
     */
    @BeforeClass
    public static void setupATAK() {
        helper.navigateStartingDialogs();
    }

}
