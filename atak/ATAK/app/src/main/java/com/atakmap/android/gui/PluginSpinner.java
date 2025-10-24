
package com.atakmap.android.gui;

import android.content.Context;
import android.content.MutableContextWrapper;
import android.util.AttributeSet;
import android.widget.Spinner;

import com.atakmap.app.ATAKApplication;

public class PluginSpinner extends Spinner {

    /**
     * This is a facade for a Context and as such, the methods implemented might 
     * not actually be used by the system.   Please note that this is private access
     * and only used by PluginSpinner
     */
    private static class ContextSensitiveContext extends MutableContextWrapper {

        final Context origContext;

        public ContextSensitiveContext(final Context origContext) {
            super(origContext);
            this.origContext = origContext;
        }

        void useAppContext(boolean e) {
            if (e) {
                setBaseContext(ATAKApplication.getCurrentActivity());
            } else {
                setBaseContext(origContext);
            }
        }

    }

    public static final String TAG = "PluginSpinner";

    public PluginSpinner(Context context) {
        super(new ContextSensitiveContext(context), null);
    }

    public PluginSpinner(Context context, int mode) {
        super(new ContextSensitiveContext(context), mode);
    }

    public PluginSpinner(Context context, AttributeSet attrs) {
        super(new ContextSensitiveContext(context), attrs);
    }

    public PluginSpinner(Context context, AttributeSet attrs, int mode) {
        super(new ContextSensitiveContext(context), attrs, mode);
    }

    public PluginSpinner(Context context, AttributeSet attrs, int style,
            int mode) {
        super(new ContextSensitiveContext(context), attrs, style, mode);
    }

    @Override
    public boolean performClick() {
        ((ContextSensitiveContext) getContext()).useAppContext(true);
        final boolean clickHandled = super.performClick();
        ((ContextSensitiveContext) getContext()).useAppContext(false);
        return clickHandled;

    }

}
