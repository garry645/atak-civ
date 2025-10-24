package com.atakmap.coremap.io;

import org.junit.Assert;

import java.lang.reflect.Field;

public class IOProviderFactoryHelper
{
    public static void registerProvider(IOProvider provider, boolean def)
    {
        try {
            Field IOProviderFactory_registered = IOProviderFactory.class.getDeclaredField("registered");
            IOProviderFactory_registered.setAccessible(true);
            IOProviderFactory_registered.set(null, Boolean.FALSE);
            IOProviderFactory.registerProvider(provider, def);
            IOProviderFactory.isDefault();
        } catch(Throwable t) {
            Assert.fail();
        }
    }
}
