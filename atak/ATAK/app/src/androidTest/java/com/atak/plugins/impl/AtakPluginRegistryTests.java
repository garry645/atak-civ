
package com.atak.plugins.impl;

import com.atakmap.android.androidtest.ATAKInstrumentedTest;
import com.atakmap.android.util.ATAKConstants;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import androidx.test.core.app.ApplicationProvider;

public class AtakPluginRegistryTests extends ATAKInstrumentedTest {
    @BeforeClass
    public static void init() {
        ATAKConstants.init(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void isTakCompatible_malformed_version_false() {
        isTakCompatible_impl(
                "com.atakmap.app@4.10.0a." + ATAKConstants.getVersionBrand(),
                false);
    }

    @Test
    public void isTakCompatible_same_version_and_flavor_compatible() {
        isTakCompatible_impl(ATAKConstants.getPluginApi(false), true);
    }

    @Test
    public void isTakCompatible_same_version_and_different_flavor_not_compatible() {
        isTakCompatible_impl(ATAKConstants.getPluginApi(false)
                .replace("." + ATAKConstants.getVersionBrand(), ".FOO"), false);
    }

    @Test
    public void isTakCompatible_same_version_and_civ_flavor_compatible() {
        isTakCompatible_impl(ATAKConstants.getPluginApi(false)
                .replace("." + ATAKConstants.getVersionBrand(), ".CIV"), true);
    }

    @Test
    public void isTakCompatible_older_version_and_flavor_in_range_compatible() {
        isTakCompatible_impl(
                "com.atakmap.app@4.10.0." + ATAKConstants.getVersionBrand(),
                true);
    }

    @Test
    public void isTakCompatible_older_version_and_different_flavor_in_range_not_compatible() {
        isTakCompatible_impl("com.atakmap.app@4.10.0.FOO", false);
    }

    @Test
    public void isTakCompatible_older_version_and_civ_flavor_in_range_compatible() {
        isTakCompatible_impl("com.atakmap.app@4.10.0.CIV", true);
    }

    @Test
    public void isTakCompatible_older_version_and_flavor_out_of_range_not_compatible() {
        isTakCompatible_impl(
                "com.atakmap.app@4.8.1." + ATAKConstants.getVersionBrand(),
                false);
    }

    @Test
    public void isTakCompatible_older_version_and_different_flavor_out_of_range_not_compatible() {
        isTakCompatible_impl("com.atakmap.app@4.8.1.FOO", false);
    }

    @Test
    public void isTakCompatible_older_version_and_civ_flavor_out_of_range_not_compatible() {
        isTakCompatible_impl("com.atakmap.app@4.9.0.CIV", false);
    }

    @Test
    public void isTakCompatible_newer_version_and_flavor_not_compatible() {
        isTakCompatible_impl(
                "com.atakmap.app@99.0.0." + ATAKConstants.getVersionBrand(),
                false);
    }

    @Test
    public void isTakCompatible_newer_version_and_different_flavor_not_compatible() {
        isTakCompatible_impl("com.atakmap.app@99.0.0.FOO", false);
    }

    @Test
    public void isTakCompatible_newer_version_and_civ_flavor_not_compatible() {
        isTakCompatible_impl("com.atakmap.app@99.0.0.CIV", false);
    }

    private void isTakCompatible_impl(String pluginApiVersion,
            boolean expected) {
        final boolean actual = AtakPluginRegistry.isTakCompatible(
                "com.atakmap.android.instrumentedtests", pluginApiVersion);
        Assert.assertTrue(actual == expected);
    }
}
