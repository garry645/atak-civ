
package com.atakmap.android.test.helpers;

import android.os.Build;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.test.helpers.helper_versions.HelperFunctions;
import com.atakmap.app.ATAKActivity;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

import java.util.UUID;

/**
 * ATAKStarter starts ATAK before all tests and closes it after all tests, since it can't
 * reopen before / after each test due to ATAK-2270.
 * Developers should note that the state of the app at the end of one test will be the state
 * of the app at the beginning of the next. Since the order of tests can not be guaranteed,
 * all tests should reset the state of the app at the end.
 */

public class ATAKStarter extends RunListener {

    public static NoFinishActivityTestRule<ATAKActivity> mActivityRule;

    @Override
    public void testRunStarted(Description description) throws Exception {
        Log.i(HelperFunctions.TAG, "Starting ATAK");

        AtakPreferences prefs = AtakPreferences.getInstance(
                InstrumentationRegistry.getInstrumentation()
                        .getTargetContext());

        prefs.set("callSystemExit", false);
        prefs.set("nav_orientation_right", false);

        // Pre-whitelist the app for battery optimizations, since that dialog sometimes isn't closed correctly on slower emulators
        HelperFunctions.executeShellCommandAndLogOutput(
                "dumpsys deviceidle whitelist +"
                        + com.atakmap.app.BuildConfig.APPLICATION_ID,
                "Output while whitelisting ATAK for battery optimizations: ");

        // Pre-agree to the EULA, since sometimes the dialog can't be found by UIAutomator for some reason
        prefs.set("AgreedToEULA", true);

        if (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic")
                        && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT)) { // Conditional from https://stackoverflow.com/questions/2799097/how-can-i-detect-when-an-android-application-is-running-in-the-emulator
            // Always use random UIDs on the Android emulator, which provides no unique IDs
            prefs.set("bestDeviceUID", UUID.randomUUID().toString());
        }

        mActivityRule = new NoFinishActivityTestRule<>(ATAKActivity.class);
        mActivityRule.launchActivity(null);
    }

    @Override
    public void testRunFinished(Result result) {
        Log.i(HelperFunctions.TAG, "Stopping ATAK");
        mActivityRule.reallyFinishActivity();

        // Undo preference changes that could cause issues outside of tests
        AtakPreferences prefs = AtakPreferences.getInstance(
                InstrumentationRegistry.getInstrumentation()
                        .getTargetContext());

        prefs.set("callSystemExit", true);
    }
}
