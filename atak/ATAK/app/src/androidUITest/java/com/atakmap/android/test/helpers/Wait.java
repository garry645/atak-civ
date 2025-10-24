
package com.atakmap.android.test.helpers;

import android.os.SystemClock;

public class Wait {

    private static final int CHECK_INTERVAL = 100;
    private static final int TIMEOUT = 10000;

    public interface Condition {
        boolean check();
    }

    private Condition mCondition;

    public Wait(Condition condition) {
        mCondition = condition;
    }

    public void waitForIt() {
        boolean state = mCondition.check();
        long startTime = SystemClock.elapsedRealtime();
        while (!state) {
            try {
                Thread.sleep(CHECK_INTERVAL);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (SystemClock.elapsedRealtime() - startTime > TIMEOUT) {
                throw new AssertionError("Wait timeout.");
            }
            state = mCondition.check();
        }
    }
}
