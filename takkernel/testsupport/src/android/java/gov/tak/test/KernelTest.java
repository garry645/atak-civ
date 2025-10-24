package gov.tak.test;

import android.Manifest;
import android.content.Context;
import android.content.res.Configuration;

import org.junit.Rule;

import java.util.Locale;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

public abstract class KernelTest
{
    final static String[] PermissionsList = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.CAMERA,
            Manifest.permission.VIBRATE,
            Manifest.permission.SET_WALLPAPER,
            Manifest.permission.INTERNET,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.DISABLE_KEYGUARD,
            Manifest.permission.GET_TASKS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.NFC,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,

            // 23 - protection in place
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,

            // 26 - protection in place
            //Manifest.permission.REQUEST_DELETE_PACKAGES,

            //"com.atakmap.app.ALLOW_TEXT_SPEECH",
    };

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule
            .grant(PermissionsList);

    protected Context testContext;

    protected KernelTest()
    {
    }

    protected Context getTestContext()
    {
        if (testContext == null)
            testContext = ApplicationProvider.getApplicationContext();
        return testContext;
    }

    protected void setLocale(Locale locale)
    {
        Locale.setDefault(locale);
        final Context baseContext = getTestContext();
        Configuration config = baseContext.getResources().getConfiguration();
        config.locale = locale;
        baseContext.getResources().updateConfiguration(config,
                baseContext.getResources().getDisplayMetrics());
    }

    protected void runOnMainThread(Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }
}
