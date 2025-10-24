
package com.atakmap.android.metrics.activity;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.atakmap.android.metrics.MetricsApi;
import com.atakmap.android.metrics.MetricsUtils;
import com.atakmap.android.preference.AtakPreferenceFragment;

/**
 * Augments the FragmentActivity class to provide for metric logging utilizing the same metrics api.
 */
public class MetricFragmentActivity extends FragmentActivity {

    private void recordState(String state) {
        if (MetricsApi.shouldRecordMetric()) {
            final Bundle b = new Bundle();
            b.putString(MetricsUtils.FIELD_CLASS,
                    this.getClass().getCanonicalName());
            b.putString(MetricsUtils.FIELD_STATE, state);
            MetricsApi.record(MetricsUtils.CATEGORY_ACTIVITY, b);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AtakPreferenceFragment.setLocale(this);
        recordState("onCreate");
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        AtakPreferenceFragment.setLocale(this);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStart() {
        super.onStart();
        recordState("onStart");
    }

    @Override
    protected void onResume() {
        AtakPreferenceFragment.setLocale(this);
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

    @SuppressLint("RestrictedApi")
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean res = superDispatchKeyEvent(event);

        if (res && MetricsApi.shouldRecordMetric()) {
            Bundle b = new Bundle();
            b.putString(MetricsUtils.FIELD_CLASS,
                    this.getClass().getCanonicalName());
            b.putParcelable(MetricsUtils.FIELD_KEYEVENT, event);
            MetricsApi.record(MetricsUtils.CATEGORY_ACTIVITY, b);

        }
        return res;

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean res = super.dispatchTouchEvent(event);
        if (res && MetricsApi.shouldRecordMetric()) {
            Bundle b = new Bundle();
            b.putString(MetricsUtils.FIELD_CLASS,
                    this.getClass().getCanonicalName());
            b.putParcelable(MetricsUtils.FIELD_KEYEVENT, event);
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
