
package com.atakmap.android.test.helpers.helper_versions;

public class HelperFactory {

    public static HelperFunctions getHelper() {
        // In the past this returned the correct version of the helper functions, based on the ATAK
        // version, at runtime. Currently that is simply handled via source sets at compile time instead.
        return new HelperFunctions();
    }

}
