
package com.atakmap.android.test.helpers.helper_versions;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withParentIndex;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PointF;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.test.core.view.MotionEventBuilder;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.Root;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.PreferenceMatchers;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import com.atak.plugins.impl.AbstractPlugin;
import com.atak.plugins.impl.AtakPluginRegistry;
import com.atak.plugins.impl.PluginContextProvider;
import com.atakmap.android.contact.IndividualContact;
import com.atakmap.android.maps.MapDataRef;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.menu.MapMenuButtonWidget;
import com.atakmap.android.menu.MapMenuReceiver;
import com.atakmap.android.menu.MapMenuWidget;
import com.atakmap.android.menu.MenuLayoutWidget;
import com.atakmap.android.navigation.views.loadout.LoadoutToolsGridVM;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.test.helpers.ClickXYPercent;
import com.atakmap.android.test.helpers.CustomViewMatchers;
import com.atakmap.android.test.helpers.ToastMatcher;
import com.atakmap.android.widgets.MapWidget;
import com.atakmap.android.widgets.WidgetIcon;
import com.atakmap.app.R;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.maps.coords.GeoPoint;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import gov.tak.api.plugin.IServiceController;

public class HelperFunctions {

    public static final String TAG = "ATAK_ESPRESSO_TEST";
    protected Context appContext = InstrumentationRegistry.getInstrumentation()
            .getTargetContext(); // TODO: is this the best way to handle the context?
    protected int action_bar_id = appContext.getResources().getIdentifier(
            "action_bar", "id",
            "android");


    public HelperFunctions() {
    }

    /**
     * Executes a shell command on the Android device and logs any output to logcat for debugging
     * purposes. This also makes the execution synchronous, which is key because in some cases the
     * app will crash if you run another command before the first one finishes.
     *
     * @param command Shell command to run
     * @param logPrefix Prefix to attach to any output when logging it to logcat
     */
    public static void executeShellCommandAndLogOutput(String command,
            String logPrefix) throws IOException {
        // Execute command
        ParcelFileDescriptor pfd = InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .executeShellCommand(command);

        // Log any output -- Mostly because this also ensures the commands complete executing before we continue
        BufferedReader reader = new BufferedReader(
                new FileReader(pfd.getFileDescriptor()));
        String line = reader.readLine();
        while (line != null) {
            Log.i(HelperFunctions.TAG, logPrefix + line);
            line = reader.readLine();
        }
        reader.close();
        pfd.close();
    }

    @SuppressLint("ResourceType")
    public void navigateStartingDialogs() {
        Log.d(TAG, "Starting to navigate dialogs");
        boolean startingUp = true;
        while (startingUp) {

            // TODO: Sometimes each selector takes ~10seconds to run for no apparent reason. Nulling out selectors once they are found could shave at least 1.5minutes off the test run time in that case
            int foundDialog = waitForOne(7000, Arrays.asList(
                    new Pair<>(1,
                            new UiSelector().text(
                                    getString(R.string.preferences_text420b))), // Disabling Battery Optimization
                    new Pair<>(2,
                            new UiSelector().text(
                                    getString(R.string.preferences_text420a))), // Media Server Fix Capable
                    new Pair<>(3,
                            new UiSelector().text(
                                    getString(R.string.preferences_text422a))), // EULA Acceptance
                    new Pair<>(4,
                            new UiSelector().text(
                                    getString(R.string.preferences_text422b))), // TAK Device Setup
                    new Pair<>(5,
                            new UiSelector().text(
                                    getString(R.string.preferences_text417))), // Please wait... (loading dialog)
                    new Pair<>(6, new UiSelector().resourceId(
                            "com.atakmap.app:id/credentials_connection_info")), // CredentialsDialog.java
                    new Pair<>(7,
                            new UiSelector()
                                    .text(getString(R.string.app_mgmt_text3))), // Updates Available (asking if we want to install plugin updates)
                    new Pair<>(8, new UiSelector().resourceIdMatches(
                            ".*:id\\/permission_allow_(foreground_only_)?button")), // Permission granting
                    new Pair<>(9,
                            new UiSelector()
                                    .text(getString(R.string.perm_rationale))), //Permission Rationale
                    new Pair<>(10,
                            new UiSelector().text(getString(
                                    R.string.use_your_location_title))), //Use your Location in the background
                    new Pair<>(11,
                            new UiSelector().text(
                                    getString(R.string.android_11_warning))), //Android 11 warning
                    new Pair<>(12, new UiSelector().resourceId(
                            "com.android.permissioncontroller:id/allow_always_radio_button")), //Allow location all the time
                    new Pair<>(13,
                            new UiSelector().text(getString(
                                    R.string.file_system_access_changes))), //File System Access Changes
                    new Pair<>(14, new UiSelector()
                            .resourceId("android:id/switch_widget"))));

            UiDevice mDevice = UiDevice
                    .getInstance(InstrumentationRegistry.getInstrumentation());
            switch (foundDialog) {
                case 1:
                    Log.d(TAG, "Found battery Optimization");
                    closeBatteryOptimizationDialogs();
                    break;

                case 2:
                    Log.d(TAG, "Found media server fix");
                    onView(withId(android.R.id.button1))
                            .inRoot(RootMatchers.isDialog())
                            .perform(ViewActions.click());
                    Log.i(TAG, "Closed Media Server Fix Dialog");
                    break;

                case 3:
                    Log.d(TAG, "Found EULA");
                    onView(withText("I agree.")).inRoot(RootMatchers.isDialog())
                            .perform(ViewActions.click());
                    Log.d(TAG, "Clicked I agree.");
                    break;

                case 4:
                    Log.d(TAG, "Found TAK device setup");
                    onView(withText("Done")).inRoot(RootMatchers.isDialog())
                            .perform(ViewActions.click());
                    Log.d(TAG, "Clicked Done");
                    break;

                case 5:
                    Log.d(TAG, "Found loading dialog");
                    while (UiDevice
                            .getInstance(InstrumentationRegistry
                                    .getInstrumentation())
                            .findObject(new UiSelector().text(
                                    getString(R.string.preferences_text417)))
                            .exists()) {
                        InstrumentationRegistry.getInstrumentation()
                                .waitForIdleSync();
                    }
                    break;

                case 6:
                    Log.d(TAG, "Found Credentials dialog");
                    onView(withId(android.R.id.button2))
                            .inRoot(RootMatchers.isDialog())
                            .perform(ViewActions.click());
                    Log.d(TAG, "Clicked Done");
                    break;

                case 7:
                    onView(withText(R.string.cancel))
                            .inRoot(RootMatchers.isDialog())
                            .perform(ViewActions.click());
                    Log.d(TAG, "Closed plugin updates available dialog");
                    break;

                case 8:
                    Log.d(TAG, "Found a permission dialog");
                    // NOTE: This is case sensitive, so we may need to tweak this if some devices have the button text as "Allow"
                    UiObject allowPermissionButton = mDevice
                            .findObject(new UiSelector().resourceIdMatches(
                                    ".*:id\\/permission_allow_(foreground_only_)?button"));
                    try {
                        allowPermissionButton.click();
                    } catch (UiObjectNotFoundException e) {
                        Log.d(TAG,
                                "Could not find button to allow a permission: "
                                        + e);
                    }
                    break;
                case 9:
                    Log.d(TAG, "Permission Rationale Found");
                    onView(withId(android.R.id.button1))
                            .inRoot(RootMatchers.isDialog())
                            .perform(ViewActions.click());
                    Log.i(TAG, "Permission Rationale Clicked");
                    break;
                case 10:
                    Log.d(TAG, "Use Your Location in the Background found");
                    onView(withId(android.R.id.button1))
                            .inRoot(RootMatchers.isDialog())
                            .perform(ViewActions.click());
                    Log.i(TAG, "Use your location allowed");
                    break;
                case 11:
                    Log.d(TAG, "Android 11+ Warning found");
                    onView(withId(android.R.id.button1))
                            .inRoot(RootMatchers.isDialog())
                            .perform(ViewActions.click());
                    Log.i(TAG, "Android 11+ Warning Allowed");
                    break;
                case 12:
                    Log.d(TAG, "Location Permission found");
                    UiObject2 allowAllTheTimeButton = mDevice.wait(
                            Until.findObject(By.res(
                                    "com.android.permissioncontroller:id/allow_always_radio_button")),
                            3000L);
                    allowAllTheTimeButton.click();
                    mDevice.pressBack();
                    Log.i(TAG, "Location allowed all the time");
                    break;
                case 13:
                    Log.d(TAG, "File System Access Changes found");
                    onView(withId(android.R.id.button1))
                            .inRoot(RootMatchers.isDialog())
                            .perform(ViewActions.click());
                    Log.i(TAG, "File System Access Changes Allowed");
                    break;
                case 14:
                    Log.d(TAG, "Allow files access");
                    UiObject2 allowFileAccessSwitch = mDevice.wait(Until
                            .findObject(By.res("android:id/switch_widget")),
                            3000L);
                    allowFileAccessSwitch.click();
                    mDevice.pressBack();
                    Log.i(TAG, "Files access switch flipped");
                    break;
                case -1:
                    Log.d(TAG, "Done startup");
                    startingUp = false;
                    break;
            }

        }
    }

    protected void closeBatteryOptimizationDialogs()
            throws NoMatchingViewException {
        onView(withId(android.R.id.button1)).inRoot(RootMatchers.isDialog())
                .perform(ViewActions.click());
        Log.i(TAG, "Closed ATAK Battery Optimization Dialog");

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        UiDevice mDevice = UiDevice
                .getInstance(InstrumentationRegistry.getInstrumentation());
        UiObject2 allowButton = mDevice.wait(Until.findObject(By.text("Allow")),
                5000L);
        if (allowButton != null) {
            allowButton.click();
        }
        mDevice.wait(
                Until.findObject(
                        By.res("com.atakmap.app.civ:id/tak_nav_menu_button")),
                5000L);
        Log.i(TAG, "Closed OS Battery Optimization Dialog");
    }

    /**
     * Automates the installation of a plugin provided a package name.
     * @param name the name of the plugin
     * @param packageName the package name
     */
    public void installPlugin(String name, String packageName) {
        //TODO: pull plugin name from string resource, or use plugin package instead of name? something more robust. but plugin names shouldn't change often!

        if (!packageName.isEmpty()
                && AtakPluginRegistry.get().isPluginLoaded(packageName))
            return;

        pressButtonInOverflow("Settings");
        clickPreferencesListItem(R.string.toolPreferences);
        onView(withText(R.string.package_mgmt_tool))
                .perform(ViewActions.click());

        sleep(1000); // Need to pause briefly on slower emulators
        //openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext());
        onView(withId(R.id.app_mgmt_syncnow)).perform(ViewActions.click());

        onView(withText(R.string.ok)).inRoot(RootMatchers.isDialog())
                .perform(ViewActions.click());

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        while (UiDevice
                .getInstance(InstrumentationRegistry.getInstrumentation())
                .findObject(new UiSelector()
                        .text("Syncing with Product Repositories"))
                .exists()) {
            sleep(250);
        }

        //openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext());

        // Click on plugin in the list
        onView(withId(R.id.app_mgmt_search)).perform(ViewActions.click());
        onView(withId(R.id.app_mgmt_repo_filter)).perform(ViewActions.click());
        onData(Matchers.allOf(
                Matchers.is(CoreMatchers.instanceOf(String.class)),
                Matchers.is("Sideloaded plugins")))
                        .perform(ViewActions.click()); // TODO: pull text from string resource
        onView(withText(name)).perform(ViewActions.click());
        // onData().inAdapterView(withId(R.id.app_mgmt_listview)).perform(click()); // TODO: do this instead (with fixed onData matcher) so it works if the plugin list item is offscreen as well

        try {
            onView(withText("Load")).perform(ViewActions.click());
            onView(withId(android.R.id.button1)).inRoot(RootMatchers.isDialog())
                    .perform(ViewActions.click()); // TODO: is this button gone as of ATAK 3.9?
        } catch (NoMatchingViewException e) {
            // Plugin is probably already loaded, check that's the case and back out
            onView(withText(R.string.app_mgmt_message_loaded_current));
            onView(withText("Cancel")).perform(ViewActions.click());
            Log.i(TAG, "Plugin was already loaded");
        }

        // Back out of preferences
        pressBackTimes(3);
    }

    public void loadPackage(String name) {
        // Open Settings > Tool Preferences > Package Management
        pressButtonInOverflow("Settings");
        clickPreferencesListItem(R.string.toolPreferences);
        onView(withText(R.string.package_mgmt_tool))
                .perform(ViewActions.click());

        sleep(1000); // Need to pause briefly on slower emulators

        // Sync Packages
        onView(withId(R.id.app_mgmt_syncnow)).perform(ViewActions.click());
        onView(withText(R.string.ok)).inRoot(RootMatchers.isDialog())
                .perform(ViewActions.click());

        // Wait for sync to complete
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        while (UiDevice
                .getInstance(InstrumentationRegistry.getInstrumentation())
                .findObject(new UiSelector()
                        .text("Syncing with Product Repositories"))
                .exists()) {
            sleep(250);
        }

        // Search for package
        onView(withId(R.id.app_mgmt_search)).perform(ViewActions.click());
        onView(withId(R.id.app_mgmt_search_text)).perform(typeText(name));

        // Close keyboard
        pressBackTimes(1);

        // Click Loaded / Not Loaded checkbox
        onView(withId(R.id.app_mgmt_row_loadPlugin)).perform(click());

        // Load (if not loaded) or Cancel (if loaded)
        try {
            onView(withText("Load")).perform(ViewActions.click());
        } catch (NoMatchingViewException e) {
            onView(withText("Cancel")).perform(ViewActions.click());
            Log.i(TAG, "Plugin was already loaded");
        }

        // Back out of Settings
        pressBackTimes(3);
    }

    /**
     * Unloads a plugin via the package name.
     * @param name  package name of the plugin to unload
     */
    public void unloadPlugin(String name) {
        // Open Settings > Tool Preferences > Package Management
        pressButtonInOverflow("Settings");
        clickPreferencesListItem(R.string.toolPreferences);
        onView(withText(R.string.package_mgmt_tool))
                .perform(ViewActions.click());

        sleep(1000); // Need to pause briefly on slower emulators

        // Sync Packages
        onView(withId(R.id.app_mgmt_syncnow)).perform(ViewActions.click());
        onView(withText(R.string.ok)).inRoot(RootMatchers.isDialog())
                .perform(ViewActions.click());

        // Wait for sync to complete
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        while (UiDevice
                .getInstance(InstrumentationRegistry.getInstrumentation())
                .findObject(new UiSelector()
                        .text("Syncing with Product Repositories"))
                .exists()) {
            sleep(250);
        }

        // Search for package
        onView(withId(R.id.app_mgmt_search)).perform(ViewActions.click());
        onView(withId(R.id.app_mgmt_search_text)).perform(typeText(name));

        // Close keyboard
        pressBackTimes(1);

        onView(withId(R.id.app_mgmt_row_loadPlugin)).perform(click());
        // Unload (if loaded) or Cancel (if already unloaded)
        try {
            onView(withText("Unload Plugin")).perform(ViewActions.click());
        } catch (NoMatchingViewException e) {
            onView(withText("Cancel")).perform(ViewActions.click());
            Log.i(TAG, "Plugin was already unloaded");
        }

        // Back out of Settings
        pressBackTimes(3);
    }

    /**
     * Given a resource, click the preference list item
     * @param titleResourceId the resource id.
     */
    public void clickPreferencesListItem(int titleResourceId) {
        onData(Matchers.allOf(
                Matchers.not(CoreMatchers.instanceOf(PreferenceCategory.class)),
                PreferenceMatchers.withTitle(titleResourceId)))
                        .inAdapterView(Matchers.allOf( // match list view for the preference menu -- see https://stackoverflow.com/questions/44240274/android-espresso-testing-preferences-multiple-listviews
                                withId(android.R.id.list),
                                Matchers.not(
                                        withParent(withResName("headers")))))
                        .perform(ViewActions.scrollTo(), ViewActions.click());
    }

    /**
     * Given a resource name, attempt to find the Views that match the name
     * @param resName the resource name
     * @return the View.
     */
    public Matcher<View> withResName(final String resName) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("with res-name: " + resName);
            }

            @Override
            public boolean matchesSafely(View view) {
                int identifier = view.getResources().getIdentifier(resName,
                        "id", "android");
                return !TextUtils.isEmpty(resName)
                        && (view.getId() == identifier);
            }
        };
    }

    /**
     * Connect to a TAK server.
     * @param truststoreFileName the trust store file name
     * @param keystoreFileName the keystore file name
     * @param ipAddress the ip address
     * @param port the port
     */
    public void connectToTAKServer(String truststoreFileName,
            String keystoreFileName, String ipAddress, String port) {
        // TODO: just directly manipulate the settings or load a prefs file instead
        pressButtonInOverflow("Settings");
        clickPreferencesListItem(R.string.networkPreferences);
        clickPreferencesListItem(R.string.networkConnectionPreferences);
        clickPreferencesListItem(R.string.preferences_text226);

        // Remove any / all existing connections
        while (true) {
            try {
                onView(withIndex(withId(R.id.manage_ports_delete), 0))
                        .perform(ViewActions.click());
                onView(withId(android.R.id.button1))
                        .inRoot(RootMatchers.isDialog())
                        .perform(ViewActions.click());
            } catch (Exception e) {
                break;
            }
        }

        // Add new connection
        pressButtonInOverflow("Add");
        onView(withId(R.id.add_host)).perform(ViewActions.typeText(ipAddress));
        closeSoftKeyboard();
        onView(withId(R.id.add_port)).perform(ViewActions.typeText(port));
        closeSoftKeyboard();
        onView(withId(R.id.useDefaultCertsCheckbox))
                .perform(ViewActions.scrollTo(), ViewActions.click());

        onView(withId(R.id.import_truststore_button))
                .perform(ViewActions.scrollTo(), ViewActions.click());
        onData(Matchers.hasToString(truststoreFileName))
                .perform(ViewActions.click());
        onView(withId(R.id.truststore_password)).perform(ViewActions.scrollTo(),
                ViewActions.typeText("atakatak"));

        onView(withId(R.id.import_keystore_button))
                .perform(ViewActions.scrollTo(), ViewActions.click());
        onData(Matchers.hasToString(keystoreFileName))
                .perform(ViewActions.click());
        onView(withId(R.id.cert_store_password)).perform(ViewActions.scrollTo(),
                ViewActions.typeText("atakatak"));
        onView(withId(R.id.add_net_button)).perform(ViewActions.scrollTo(),
                ViewActions.click());

        pressBackTimes(4);
    }

    public void toggleTAKServer() {
        // TODO: take in server ip instead of assuming there is only one?
        // TODO: just directly manipulate the settings or load a prefs file instead
        pressButtonInOverflow("Settings");
        clickPreferencesListItem(R.string.networkPreferences);
        clickPreferencesListItem(R.string.networkConnectionPreferences);
        clickPreferencesListItem(R.string.preferences_text226);

        // Toggle server
        onView(withId(R.id.manage_ports_checkbox)).perform(ViewActions.click());

        pressBackTimes(4);
    }

    public void setCallsign(String callsign) {
        AtakPreferences locationPrefs = AtakPreferences.getInstance(appContext);
        locationPrefs.set("locationCallsign", callsign);
    }

    public void setDispatchLocationCotExternal(boolean bool) {
        AtakPreferences locationPrefs = AtakPreferences.getInstance(appContext);
        locationPrefs.set("dispatchLocationCotExternal", bool);
    }

    public void setConstantPositionReportingStrategy() {
        AtakPreferences locationPrefs = AtakPreferences.getInstance(appContext);
        locationPrefs.set("locationReportingStrategy", "Constant");
    }

    public void pressButtonInOverflow(String button) {
        // Support "Red X Tool" from previous implementation
        if (button.equals("Red X Tool")) {
            button = "Red X";
        }

        // Tap menu (3 lines) to bring up Tools menu
        onView(withId(R.id.tak_nav_menu_button)).perform(click());

        // Wait until Tools menu loads
        UiDevice device = UiDevice
                .getInstance(InstrumentationRegistry.getInstrumentation());
        device.wait(Until.hasObject(By
                .res("com.atakmap.app.civ:id/nav_stack_toolbar_buttons")),
                2000);

        // Determine if toolbar has 5 (or only 4) buttons,
        // and use this to determine the index of the gear button
        int gearIndex;
        try {
            onView(allOf(withId(R.id.nav_stack_toolbar_buttons),
                    hasChildCount(5)))
                            .check(matches(isDisplayed()));
            gearIndex = 3;
        } catch (NoMatchingViewException e) {
            gearIndex = 2;
        }

        // Tap gear button
        onView(allOf(withParent(withId(R.id.nav_stack_toolbar_buttons)),
                withParentIndex(gearIndex)))
                        .perform(click());

        // Tap "All tools & plugins"
        onView(allOf(withText("All tools & plugins"),
                withId(R.id.toolbar_title_item_text)))
                        .perform(click());

        // Tap on appropriate tool
        onData(withName(button)).inAdapterView(withId(R.id.tools_list))
                .perform(click());
    }

    public void pressMapLocation(GeoPoint location) {
        panZoomTo(MapView.getMapView().getMaxMapScale(), location);
        onView(withId(R.id.map_view))
                .perform(ClickXYPercent.clickPercent(.5f, .5f));
    }

    public void pressMapLocationMinScale(GeoPoint location) {
        panZoomTo(MapView.getMapView().getMinMapScale(), location);
        onView(withId(R.id.map_view))
                .perform(ClickXYPercent.clickPercent(.5f, .5f));
    }

    public void pressMapLocation(float x, float y) {
        onView(withId(R.id.map_view))
                .perform(ClickXYPercent.clickPercent(x, y));
    }

    public void longPressMapLocation(float x, float y) {
        onView(withId(R.id.map_view))
                .perform(ClickXYPercent.clickPercentLong(x, y));
    }

    public void pressButtonFromLayoutManager(String button) { //TODO: Rename function to match newer name of this tool?
        pressButtonInOverflow(button);
    }

    public void pressMarkerFromOverlay(String marker) {
        sleep(1000); // Need to pause briefly on slower emulators
        pressButtonFromLayoutManager("Overlays");
        onView(withId(R.id.hierarchy_search_btn)).perform(ViewActions.click());
        onView(withId(R.id.hierarchy_search))
                .perform(ViewActions.typeText(marker));
        closeSoftKeyboard();
        onView(Matchers.allOf(withId(R.id.hierarchy_manager_list_item_title),
                withText(marker))).perform(ViewActions.click());
        sleep(1000); // Need to pause briefly on slower emulators
    }

    public boolean pressMarkerOnMap(Marker marker) {
        if (marker != null) {
            PointF point = MapView.getMapView().forward(marker.getPoint());
            Log.i(TAG, "computed forward location for: " + marker.getTitle() +
                    " " + point.x + " " + point.y);
            pressMapLocation(point.x / MapView.getMapView().getWidth(),
                    point.y / MapView.getMapView().getHeight());
            return true;
        } else {
            return false;
        }
    }

    public boolean pressMarkerTypeOnMap(String type) {
        Marker marker = getMarkerOfType(type);
        if (marker == null)
            Log.i(TAG, "marker of type: " + type + " was not found");
        return pressMarkerOnMap(marker);
    }

    public boolean pressMarkerNameOnMap(String name) {
        Marker marker = getMarkerOfName(name);
        if (marker == null)
            Log.i(TAG, "marker of name: " + name + " was not found");
        return pressMarkerOnMap(marker);

    }

    public boolean pressMarkerUidOnMap(String uid) {
        Marker marker = getMarkerOfUid(uid);
        return pressMarkerOnMap(marker);

    }

    public Marker getMarker(final String identifier, String identifierType) {
        if (identifierType.equals("name"))
            identifierType = "callsign";
        List<MapItem> markers = MapView.getMapView().getRootGroup()
                .deepFindItems(identifierType, identifier);
        if (markers.size() > 1) {
            Log.i(TAG, "there are more than one markers on the map with "
                    + identifierType + ": " + identifier);
            return null; // TODO: Throw a new exception, for clear display of the root cause of issues in the log
        } else if (markers.size() == 0) {
            Log.i(TAG, "there are no markers on the map with " + identifierType
                    + ": " + identifier);
            return null;
        }
        return (Marker) markers.get(0);
    }

    public Marker getMarkerOfName(String name) {
        return getMarker(name, "callsign");
    }

    public Marker getMarkerOfUid(String uid) {
        return getMarker(uid, "uid");
    }

    public Marker getMarkerOfType(String type) {
        return getMarker(type, "type");
    }

    public void deleteAllMarkers() {
        pressButtonFromLayoutManager("Overlays");

        onView(withId(R.id.hierarchy_multiselect_btn))
                .perform(ViewActions.click());

        onView(Matchers.allOf( // click delete button in dialog
                withClassName(
                        Matchers.is("com.atakmap.android.gui.TileButtonView")),
                CustomViewMatchers.nthChild(1))).inRoot(RootMatchers.isDialog())
                        .perform(ViewActions.click());

        // Wait until checkbox loads
        UiDevice device = UiDevice
                .getInstance(InstrumentationRegistry.getInstrumentation());
        device.wait(Until.hasObject(By
                .res("com.atakmap.app.civ:id/selectAll_cb")),
                1000);

        onView(withId(R.id.selectAll_cb)).perform(ViewActions.click());
        try {
            //Id of the button's text
            onView(withText(R.string.delete)).perform(ViewActions.click());
            onView(withText("Yes")).perform(ViewActions.click());
        } catch (PerformException e) {
            Log.i(TAG, "No markers to delete");
        }

        // Close the overlays dropdown
        pressBackTimes(1); // TODO: this might not actually close it? Does it fire before the delete is done or something?
    }

    public void zoomTo(Double scale) {
        MapView.getMapView().getMapController().zoomTo(scale, false);
    }

    public void panTo(GeoPoint point) {
        MapView.getMapView().getMapController().panTo(point, false);
    }

    public void panZoomTo(GeoPoint start, GeoPoint end) {

        MapView mapView = MapView.getMapView();

        double minLat = Math.min(start.getLatitude(), end.getLatitude());
        double minLon = Math.min(start.getLongitude(), end.getLongitude());
        double maxLat = Math.max(start.getLatitude(), end.getLatitude());
        double maxLon = Math.max(start.getLongitude(), end.getLongitude());

        panZoomTo(mapView.getMaxMapScale(), new GeoPoint(
                (maxLat - minLat) / 2 + minLat,
                (maxLon - minLon) / 2 + minLon));

        double stepSize = 0.5;

        GeoPoint mapStart = mapView.inverse(0, 0).get();
        GeoPoint mapEnd = mapView
                .inverse(mapView.getWidth(), mapView.getHeight()).get();

        double mapMinLat = Math.min(mapStart.getLatitude(),
                mapEnd.getLatitude());
        double mapMinLon = Math.min(mapStart.getLongitude(),
                mapEnd.getLongitude());
        double mapMaxLat = Math.max(mapStart.getLatitude(),
                mapEnd.getLatitude());
        double mapMaxLon = Math.max(mapStart.getLongitude(),
                mapEnd.getLongitude());

        while (minLat < mapMinLat ||
                minLon < mapMinLon ||
                maxLat > mapMaxLat ||
                maxLon > mapMaxLon) {
            zoomTo(mapView.getMapScale() * stepSize);

            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            mapStart = mapView.inverse(0, 0).get();
            mapEnd = mapView.inverse(mapView.getWidth(), mapView.getHeight())
                    .get();

            mapMinLat = Math.min(mapStart.getLatitude(), mapEnd.getLatitude());
            mapMinLon = Math.min(mapStart.getLongitude(),
                    mapEnd.getLongitude());
            mapMaxLat = Math.max(mapStart.getLatitude(), mapEnd.getLatitude());
            mapMaxLon = Math.max(mapStart.getLongitude(),
                    mapEnd.getLongitude());
        }
    }

    public Pair<Long, Pair<ArrayList<Long>, ArrayList<Long>>> bulkPlaceMarkers(
            int width, int height, GeoPoint start, GeoPoint end,
            Runnable toPlaceState) {

        long startTime = System.currentTimeMillis();
        ArrayList<Long> toPlaceTimes = new ArrayList<>(width * height);
        ArrayList<Long> tapMapTimes = new ArrayList<>(width * height);

        iterateMapPoints(width, height, start, end, (x, y, point) -> {
            Log.d(TAG, "placing marker: " + ((x * height) + y + 1) + " / "
                    + (width * height));

            panTo(point);

            zoomTo(MapView.getMapView().getMaxMapScale());

            long beforePlaceState = System.currentTimeMillis();

            toPlaceState.run();
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            long placeStateTime = System.currentTimeMillis() - beforePlaceState;

            long beforeTap = System.currentTimeMillis();

            pressMapLocation(0.5f, 0.5f);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            long tapTime = System.currentTimeMillis() - beforeTap;

            toPlaceTimes.add(placeStateTime);
            tapMapTimes.add(tapTime);

            pressBackTimes(5);
        });

        //pan to center and zoom out to see all markers

        panZoomTo(start, end);

        return new Pair<>(
                System.currentTimeMillis() - startTime,
                new Pair<>(toPlaceTimes, tapMapTimes));
    }

    public void panZoomTo(Double scale, GeoPoint point) {
        MapView.getMapView().getMapController().panZoomTo(point, scale, false);
        // account for the delay between the panZoomTo and the time in which the map can be touched again.
        sleep(100);
    }

    private MapMenuButtonWidget findButtonWidget(final MapMenuWidget menuWidget,
            final String iconUri) {
        for (MapWidget child : menuWidget.getChildWidgets()) {
            if (child instanceof MapMenuButtonWidget) {
                MapMenuButtonWidget buttonWidget = (MapMenuButtonWidget) child;
                final WidgetIcon widgetIcon = buttonWidget.getIcon();
                final MapDataRef mapDataRef = widgetIcon.getIconRef(0);
                if (mapDataRef != null) {
                    //Log.d(TAG, iconUri + " " + mapDataRef.toUri());
                    if (iconUri.equals(mapDataRef.toUri()))
                        return buttonWidget;
                }
            }
        }
        return null;
    }

    /**
     * Press a radial button for a map item given an icon.
     * @param mapitem the map item
     * @param button the icon asset for example asset://icons/delete.png
     */
    public void pressRadialButton(MapItem mapitem, String button) {
        MenuLayoutWidget mlw = MapMenuReceiver.getMenuWidget();
        if (mlw == null) {
            Log.d(TAG, "could not find radial widget for use with: " + button);
            return;
        }

        MapMenuWidget mmw = mlw.openMenuOnItem(mapitem);
        MapMenuButtonWidget mmbw = findButtonWidget(mmw, button);
        final MotionEventBuilder eventBuilder = MotionEventBuilder.newBuilder();
        final MotionEvent event = eventBuilder.build();
        if (mmbw != null) {
            mmbw.onClick(event);
        } else {
            Log.d(TAG, "could not find radial described by: " + button);
        }
    }

    public void closeHelperDialog() {
        UiDevice device = UiDevice
                .getInstance(InstrumentationRegistry.getInstrumentation());
        if (device.findObject(new UiSelector().text("On Screen Hints"))
                .exists()) {
            onView(withId(android.R.id.button1)).inRoot(RootMatchers.isDialog())
                    .perform(ViewActions.click());
        }
    }

    public void closeDialog(String title, int timeout) {
        UiDevice device = UiDevice
                .getInstance(InstrumentationRegistry.getInstrumentation());
        device.wait(Until.hasObject(By.text(title)), timeout);
        if (device.hasObject(By.text(title))) {
            onView(withId(android.R.id.button1)).perform(click());
        }
    }

    public String getString(@StringRes int res) {
        return appContext.getResources().getString(res);
    }

    protected int waitForOne(long timeout,
            Collection<Pair<Integer, UiSelector>> selectors) {

        long timeOfLastSleep = 0;

        UiDevice device = UiDevice
                .getInstance(InstrumentationRegistry.getInstrumentation());

        while (timeout > 0) {

            if (timeOfLastSleep != 0) {
                // Checking each selector sometimes takes up to 10 seconds or so (even when the
                // Android device is running at full speed), and that quickly adds up with 6 selectors
                // So, account for that time when we count down to the timeout:
                timeout -= SystemClock.elapsedRealtime() - timeOfLastSleep;
            }
            timeOfLastSleep = SystemClock.elapsedRealtime();

            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            for (Pair<Integer, UiSelector> selector : selectors) {
                if (device.findObject(selector.second).exists()) {
                    return selector.first;
                }
            }

            sleep(250);
        }

        return -1;
    }

    protected void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Install a given plugin
     *
     * @param name The plugin's name
     */
    public void installPlugin(String name) {
        installPlugin(name, "");
    }

    /*
        press icon on the action bar that is a number num over
        when counting the icons from left to right
        phone is assumed to be in landscape orientation
        */
    public void pressActionBarIcon(int num) {
        num = num - 1;
        Espresso.onView(ViewMatchers.withId(action_bar_id))
                .perform(ClickXYPercent.clickPercent(.0277f + .055f * num,
                        .5f));
    }

    /*
        press back button given number of times
         */
    public void pressBackTimes(int num) {
        for (int i = 0; i < num; i++) {
            Espresso.pressBack();
            sleep(200);
        }
    }

    /*
        Call the given Callable until it does not return null or the time runs out
        */
    public <V> V nullWait(Callable<V> callable, long timeout) {
        V ret = null;

        try {
            long time = SystemClock.elapsedRealtime();

            // SHB: fix the spin loop in with a timed sleep.
            while (SystemClock.elapsedRealtime() - time < timeout &&
                    (ret = callable.call()) == null) {
                sleep(100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }

    public Matcher<Root> isToast() {
        return new ToastMatcher();
    }

    /*
        Checks if toast with textId is displayed
         */
    public void isToastMessageDisplayed(String textId) {
        Espresso.onView(ViewMatchers.withText(textId)).inRoot(isToast())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    //Disabled Notification Tray test
    public void isNotificationDisplayed(String notifTitle, int timeout,
            boolean click) {
        UiDevice device = UiDevice
                .getInstance(InstrumentationRegistry.getInstrumentation());
        device.openNotification();
        device.wait(Until.hasObject(By.text(notifTitle)), timeout);
        UiObject2 title = device.findObject(By.text(notifTitle));
        //assertEquals(notifTitle, title.getText());
        if (click) {
            title.click();
        }
        device.pressBack();
    }

    /**
     * Iterate over map points in a grid
     *
     * @param width
     * @param height
     * @param start
     * @param end
     * @param call
     */
    public void iterateMapPoints(int width, int height, GeoPoint start,
            GeoPoint end, GeoPointCallback call) {
        double startLat = start.getLatitude();
        double startLon = start.getLongitude();

        double stepX = (end.getLongitude() - startLon) / (width - 1);
        double stepY = (end.getLatitude() - startLat) / (height - 1);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                call.run(x, y, new GeoPoint(
                        startLat + (stepY * y),
                        startLon + (stepX * x)));
            }
        }
    }

    // From https://stackoverflow.com/questions/29378552/in-espresso-how-to-choose-one-the-view-who-have-same-id-to-avoid-ambiguousviewm
    public Matcher<View> withIndex(final Matcher<View> matcher,
            final int index) {
        return new TypeSafeMatcher<View>() {
            int currentIndex;
            int viewObjHash;

            @Override
            public void describeTo(Description description) {
                description.appendText(
                        String.format(LocaleUtil.US, "with index: %d ", index));
                matcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                if (matcher.matches(view) && currentIndex++ == index) {
                    viewObjHash = view.hashCode();
                }
                return view.hashCode() == viewObjHash;
            }
        };
    }

    public interface GeoPointCallback {
        void run(int x, int y, GeoPoint point);
    }

    // Based on https://stackoverflow.com/a/49524291
    private static class WaitForUIUpdate {
        static void waitForWithId(@IdRes int stringId)
                throws NoMatchingViewException {
            int totalWait = 0;
            ViewInteraction element;
            NoMatchingViewException lastException;

            do {
                waitFor(500);
                totalWait += 500;

                element = Espresso.onView(ViewMatchers.withText(stringId));

                try {
                    Espresso.onView(ViewMatchers.withText(stringId)).check(
                            ViewAssertions.matches(ViewMatchers.isDisplayed()));
                    return; //Success!
                } catch (NoMatchingViewException e) {
                    lastException = e;
                }

            } while (totalWait < 15000);

            throw lastException;
        }

        static void waitFor(int ms) {
            final CountDownLatch signal = new CountDownLatch(1);

            try {
                signal.await(ms, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    private static Matcher withName(String name) {
        return new TypeSafeMatcher<LoadoutToolsGridVM>() {
            @Override
            public boolean matchesSafely(LoadoutToolsGridVM l) {
                return name.equals(l.getName());
            }

            @Override
            public void describeTo(Description description) {
            }
        };
    }

    public static Matcher withContactName(String name) {
        return new TypeSafeMatcher<IndividualContact>() {
            @Override
            public boolean matchesSafely(IndividualContact l) {
                return name.equals(l.getName());
            }

            @Override
            public void describeTo(Description description) {
            }
        };
    }


    /**
     * This is an expensive call to get the Loaded Plugin information to include the
     * pluginContext, class loader responsible for the plugin and the package name.
     * There is no caching done presently so make the call once and then save it.
     * @param packageName the package name for the plugin
     * @return the loaded plugin information that is valid until the plugin is unloaded
     * or null if the plugin is not loaded.
     */
    public static LoadedPlugin getLoadedPlugin(@NonNull String packageName) {
        if (!AtakPluginRegistry.get().isPluginLoaded(packageName)) {
            Log.e(TAG, "plugin not loaded: " + packageName);
            return null;
        }
        
        Collection<Object> objects = AtakPluginRegistry.get().getPluginInstantiations();
        for (Object o: objects) {
            if (o instanceof AbstractPlugin) {
                final AbstractPlugin plugin = (AbstractPlugin)o;
                final ClassLoader cl = plugin.getClass().getClassLoader();
                final Field[] fields = AbstractPlugin.class.getDeclaredFields();
                for (Field f: fields) {
                    if (f.getType() == IServiceController.class) {
                        f.setAccessible(true);
                        try {
                            IServiceController iServiceController =
                                    (IServiceController) f.get(plugin);
                            if (iServiceController != null) {
                                Context pluginContext = iServiceController.getService(PluginContextProvider.class).getPluginContext();
                                if (packageName.equals(pluginContext.getPackageName())) {
                                    return new LoadedPlugin(pluginContext, cl, packageName);
                                }
                            }
                        } catch (IllegalAccessException e) {
                            Log.e(TAG, "error obtaining the pluginContext for: " + packageName, e);
                        }
                    }
                }
            }

        }
        return null;

    }

    public static class LoadedPlugin {
        public final Context pContext;
        public final ClassLoader pClassLoader;
        public final String packageName;

        public LoadedPlugin(Context pContext, ClassLoader pClassLoader, String packageName) {
            this.pContext = pContext;
            this.pClassLoader = pClassLoader;
            this.packageName = packageName;
        }
    }
}
