
package com.atakmap.app.system;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

public class ResourceUtil {

    /**
     * Allow for proper lookup of Strings based on the capabilities of the flavor
     * @param c The context to use when looking up a resource
     * @param resIdCiv the civilian capability resource
     * @param resIdMil the military capability resource
     * @return the appropriate string for the given state of the system.
     */
    public static String getString(Context c, int resIdCiv, int resIdMil) {
        FlavorProvider fp = SystemComponentLoader.getFlavorProvider();
        if (fp != null)
            if (fp.hasMilCapabilities())
                return c.getString(resIdMil);

        return c.getString(resIdCiv);
    }

    /**
     * Allow for proper lookup of resource id based on the capabilities of the flavor
     * @param resIdCiv the civilian capability resource
     * @param resIdMil the military capability resource
     * @return the appropriate string for the given state of the system
     */
    public static int getResource(int resIdCiv, int resIdMil) {
        FlavorProvider fp = SystemComponentLoader.getFlavorProvider();
        if (fp != null)
            if (fp.hasMilCapabilities())
                return resIdMil;

        return resIdCiv;
    }

    /**
     * Obtain a string in a requested language
     * @param c the context to use to resolve the string
     * @param resId the resource identifier
     * @param locale the locale
     * @return the string for the provided resource id in the appropriate locale.
     */
    public static String getString(Context c, int resId, Locale locale) {
        // clone the configuration
        final Configuration conf = new Configuration(
                c.getResources().getConfiguration());
        conf.setLocale(locale);

        // create the context for the specific locale
        Context lContext = c.createConfigurationContext(conf);
        return lContext.getResources().getString(resId);
    }

}
