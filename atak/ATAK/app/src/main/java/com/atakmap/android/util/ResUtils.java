
package com.atakmap.android.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;

import com.atakmap.app.R;
import com.atakmap.coremap.log.Log;

import java.lang.reflect.Field;

import gov.tak.api.commons.graphics.Drawable;
import gov.tak.platform.marshal.MarshalManager;

public class ResUtils {

    private static final String TAG = "ResUtils";

    /***
     *
     * @param name - name of drawable resource id ie, "ic_nav_radio"
     * @return the resource id
     */
    public static Integer getDrawableResID(String name) {
        try {
            Class res = R.drawable.class;
            Field field = res.getField(name);
            return field.getInt(null);
        } catch (Exception e) {
            Log.e(TAG, "Failure to get drawable id.", e);
        }
        return null;
    }

    /***
     *
     * @param name - name of string resource id ie, "nav_radio_title"
     * @return the resource id
     */
    public static Integer getStringsID(String name) {
        try {
            Class res = R.string.class;
            Field field = res.getField(name);
            return field.getInt(null);
        } catch (Exception e) {
            Log.e(TAG, "Failure to get string id.", e);
        }
        return null;
    }

    /**
     * Closes the container for an array of values that were retrieved with
     * Resources.Theme#obtainStyledAttributes(AttributeSet, int[], int, int) or
     * Resources#obtainAttributes. Be sure to call recycle() when done with them.
     * The indices used to retrieve values from this structure correspond to the
     * positions of the attributes given to obtainStyledAttributes.
     * @param typedArray the typed array
     */
    public static void close(TypedArray typedArray) {
        if (typedArray != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                typedArray.close();
            } else {
                typedArray.recycle();
            }
        }
    }

    /**
     * Makes Drawable marshalling (from android to gov.tak.api.commons.graphics) a little less
     * verbose
     * @param context
     * @param resId
     * @return a {@link gov.tak.api.commons.graphics.Drawable}
     */
    public static Drawable getDrawable(Context context, int resId) {
        return MarshalManager.marshal(context.getDrawable(resId),
                android.graphics.drawable.Drawable.class,
                gov.tak.api.commons.graphics.Drawable.class);
    }
}
