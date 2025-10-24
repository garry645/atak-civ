
package com.atakmap.app;

import static androidx.test.espresso.Espresso.pressBack;

import android.content.Intent;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.atakmap.android.test.helpers.helper_versions.HelperFactory;
import com.atakmap.android.test.helpers.helper_versions.HelperFunctions;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class NavigationManipulationTest {

    @Rule
    public ActivityTestRule<ATAKActivity> mActivityRule = new ActivityTestRule<>(
            ATAKActivity.class,
            true,
            false);

    private HelperFunctions helper = HelperFactory.getHelper();

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void aTAKActivityTest1() {
        mActivityRule.launchActivity(new Intent());
        sleep(5000);

        helper.navigateStartingDialogs();

        helper.pressButtonInOverflow("Alert");
        pressBack();
        helper.pressButtonInOverflow("Contacts");
        pressBack();

    }

}
