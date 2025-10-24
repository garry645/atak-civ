
package com.atakmap.android.androidtest;

import android.content.Context;
import android.os.Bundle;
import androidx.test.runner.AndroidJUnitRunner;

import com.atakmap.coremap.log.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ATAKTestRunner extends AndroidJUnitRunner {

    private final static String TAG = "ATAKTestRunner";

    @Override
    public void onStart() {
        // if available, call TestButler.setup(getTargetContext());
        invokeTestButlerStaticContextMethod("setup", getTargetContext());
        super.onStart();
    }

    @Override
    public void finish(int resultCode, Bundle results) {
        // if available, call TestButler.teardown(getTargetContext());
        invokeTestButlerStaticContextMethod("teardown", getTargetContext());
        super.finish(resultCode, results);
    }

    private static Class<?> getTestButler() {
        try {
            return Class.forName("com.linkedin.android.testbutler.TestButler");
        } catch (Exception e) {
            Log.d(TAG, "TestButler not present");
            return null;
        }
    }

    private static void invokeTestButlerStaticContextMethod(String methodName,
            Context context) {
        Class<?> testButler = getTestButler();
        if (testButler != null) {
            try {
                Method method = testButler.getDeclaredMethod(methodName,
                        Context.class);
                method.setAccessible(true);
                method.invoke(null, context);
            } catch (NoSuchMethodException | InvocationTargetException
                    | IllegalAccessException e) {
                Log.d(TAG, "TestButler found but failed to invoke '"
                        + methodName + "'");
            }
        }
    }
}
