
package com.atak.plugins.impl;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import com.atakmap.android.ipc.AtakBroadcast;

import gov.tak.platform.ui.MotionEvent;

abstract public class AbstractPluginTool implements IToolbarItem2 {

    private final Context context;
    private final String shortDescription;
    private final String description;
    private final Drawable icon;
    private final String action;

    /**
     * Construct an abstract PluginTool
     * @param context the context to use
     * @param shortDescription the short description
     * @param description the description
     * @param icon the icon
     * @param action the action
     */
    public AbstractPluginTool(Context context,
            String shortDescription,
            String description,
            Drawable icon,
            String action) {
        this.context = context;
        this.shortDescription = shortDescription;
        this.description = description;
        this.icon = icon;
        this.action = action;
    }

    @Override
    final public String getDescription() {
        return description;
    }

    @Override
    final public Drawable getIcon() {
        return icon;
    }

    @Override
    final public String getShortDescription() {
        return shortDescription;
    }

    @Override
    public void onItemEvent(MotionEvent motionEvent) {
        final Intent i = new Intent(action);
        AtakBroadcast.getInstance().sendBroadcast(i);
    }

    @Override
    public String getReference() {
        final String pkgName = context.getPackageName();
        return "plugin://" + pkgName + "/" + this.getClass().getName();
    }

}
