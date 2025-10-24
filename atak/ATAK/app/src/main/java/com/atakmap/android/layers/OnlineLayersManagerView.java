
package com.atakmap.android.layers;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;

import com.atakmap.app.R;

public class OnlineLayersManagerView extends AbstractLayersManagerView {

    public OnlineLayersManagerView(Context context, AttributeSet attrs) {
        super(context, attrs, R.id.layers_manager_online_list, 0);
    }

    private void restoreOfflineModeSwitch() {
        final ListView listView = findViewById(R.id.layers_manager_online_list);
        final ListAdapter adapter = listView.getAdapter();
        if ((adapter == null)
                || !(adapter instanceof MobileLayerSelectionAdapter))
            return;

        final Switch offlineModeSwitch = findViewById(R.id.offlineModeSwitch);
        offlineModeSwitch.setChecked(
                !((MobileLayerSelectionAdapter) adapter).isOfflineOnly());
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);

        restoreOfflineModeSwitch();
    }

    @Override
    protected void dispatchRestoreInstanceState(
            SparseArray<Parcelable> container) {
        super.dispatchRestoreInstanceState(container);
        restoreOfflineModeSwitch();
    }
}
