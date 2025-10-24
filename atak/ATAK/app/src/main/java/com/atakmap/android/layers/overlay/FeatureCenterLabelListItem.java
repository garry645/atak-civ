
package com.atakmap.android.layers.overlay;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.atakmap.android.hierarchy.action.Visibility;
import com.atakmap.android.hierarchy.items.AbstractChildlessListItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.app.R;

/**
 * Overlay manager list item for toggling visibility file overlay labels
 */
public final class FeatureCenterLabelListItem extends AbstractChildlessListItem
        implements
        Visibility {

    static public final String PREF_FILE_LABEL = "prefs_file_label";

    private final Context _context;
    private final AtakPreferences _prefs;

    public FeatureCenterLabelListItem(MapView mapView) {
        _context = mapView.getContext();
        _prefs = AtakPreferences.getInstance(_context);
    }

    @Override
    public String getTitle() {
        return _context.getString(R.string.display_shape_labels_summary);
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getUID() {
        return "fileOverlayVisibility";
    }

    @Override
    public Drawable getIconDrawable() {
        return _context.getDrawable(R.drawable.ic_menu_overlays);
    }

    @Override
    public Object getUserObject() {
        return this;
    }

    /**
     * Mass change of all of the controls for file overlay label visibility.
     */
    private void refreshState() {
    }

    @Override
    public boolean isVisible() {
        return _prefs.get(PREF_FILE_LABEL, false);
    }

    @Override
    public boolean setVisible(boolean visible) {
        _prefs.set(PREF_FILE_LABEL, visible);
        return visible;
    }
}
