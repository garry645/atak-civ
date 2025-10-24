
package com.atakmap.android.metrics.activity;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import com.atakmap.android.metrics.MetricsApi;
import com.atakmap.android.util.ATAKConstants;

/**
 * Augments the PreferenceActivity class to provide for metric logging utilizing the same metrics api.
 */
public class MetricPreferenceActivity extends PreferenceActivity {

    private void recordState(String state) {
        if (MetricsApi.shouldRecordMetric()) {
            final Bundle b = new Bundle();
            b.putString("class", this.getClass().toString());
            b.putString("state", state);
            MetricsApi.record("activity", b);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setIcon(ATAKConstants.getIcon());

        recordState("onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        recordState("onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        recordState("onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        recordState("onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        recordState("onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recordState("onDestroy");
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean res = super.dispatchKeyEvent(event);
        if (res && MetricsApi.shouldRecordMetric()) {
            Bundle b = new Bundle();
            b.putString("class", this.getClass().toString());
            b.putParcelable("keyevent", event);
            MetricsApi.record("activity", b);

        }
        return res;

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean res = super.dispatchTouchEvent(event);
        if (res && MetricsApi.shouldRecordMetric()) {
            Bundle b = new Bundle();
            b.putString("class", this.getClass().toString());
            b.putParcelable("keyevent", event);
            MetricsApi.record("activity", b);
        }
        return res;
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        ActivityHelper.removeEdgeToEdge(this);
    }

}
