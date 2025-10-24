
package com.atak.plugins.impl;

import android.content.Context;
import android.view.LayoutInflater;

final class PluginContext extends android.content.ContextWrapper {
    private final ClassLoader classLoader;

    public PluginContext(Context ctx, ClassLoader classLoader) {
        super(ctx);
        this.classLoader = classLoader;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    @Override
    public Object getSystemService(String service) {
        Object retval = super.getSystemService(service);
        if (retval instanceof LayoutInflater)
            retval = ((LayoutInflater) retval).cloneInContext(this);
        return retval;
    }
}
