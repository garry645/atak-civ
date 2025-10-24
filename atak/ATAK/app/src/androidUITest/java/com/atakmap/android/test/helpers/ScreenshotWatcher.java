
package com.atakmap.android.test.helpers;

import android.os.Environment;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.File;

/**
 * The screenshot watcher test rule takes a screenshot of the current
 * state of the android application when any Espresso test fails
 * <p>
 * Created by Vicky McDermott on 7/10/2017.
 */
public class ScreenshotWatcher extends TestWatcher {

    private TestWatcherListener listener;

    /**
     * Initializes the TestWatcherListener. Required to handle any test clean up
     * after failure screenshot is captured.
     *
     * @param listener The TestWatcherListener that will handle the test finished event
     */
    public void setListener(TestWatcherListener listener) {
        this.listener = listener;
    }

    @Override
    public void failed(Throwable e, Description description) {
        // Save to external storage (usually /sdcard/screenshots)
        System.out.println(
                Environment.getExternalStorageDirectory().getAbsolutePath());
        File path = new File(
                Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/screenshots/failures/"
                        + description.getClassName());
        if (!path.exists()) {
            if (!path.mkdirs()) {
                System.out.println("Failed to create directory");
            }
        }

        // Take advantage of UiAutomator screenshot method
        UiDevice device = UiDevice
                .getInstance(InstrumentationRegistry.getInstrumentation());
        String filename = description.getMethodName() + ".png";
        device.takeScreenshot(new File(path, filename));
    }

    @Override
    protected void finished(Description description) {
        if (listener != null) {
            listener.onTestFinished(description);
        }
    }

    public interface TestWatcherListener {
        /**
         * Handles teardown needed for each ATAK UI test. Should be implemented instead of using @After
         * for test clean up.
         */
        void onTestFinished(Description description);

    }
}
