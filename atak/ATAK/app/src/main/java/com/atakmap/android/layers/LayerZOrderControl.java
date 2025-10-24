
package com.atakmap.android.layers;

import com.atakmap.map.layer.Layer2;

public interface LayerZOrderControl extends Layer2.Extension {
    interface OnSelectionZOrderChangedListener {
        void onSelectionZOrderChanged(LayerZOrderControl ctrl);
    }

    int getPosition(String selection);

    void setPosition(String selection, int z);

    void addOnSelectionZOrderChangedListener(
            OnSelectionZOrderChangedListener l);

    void removeOnSelectionZOrderChangedListener(
            OnSelectionZOrderChangedListener l);
}
