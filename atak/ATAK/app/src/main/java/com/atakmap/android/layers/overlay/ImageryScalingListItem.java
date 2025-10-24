
package com.atakmap.android.layers.overlay;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.atakmap.android.hierarchy.items.AbstractChildlessListItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.app.R;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.layer.raster.controls.ImageryRelativeScaleControl;

/**
 * Controls relative scaling of the base map imagery
 */
final class ImageryScalingListItem extends AbstractChildlessListItem {

    private final MapView _mapView;
    private final Context _context;
    private final View _view;
    private final ImageButton _increaseScaleButton;
    private final ImageButton _decreaseScaleButton;
    private final TextView _scaleTextView;

    public ImageryScalingListItem(final MapView mapView) {
        _mapView = mapView;
        _context = mapView.getContext();

        _view = LayoutInflater.from(_context).inflate(
                R.layout.imagery_scaling_list_item, mapView, false);
        _scaleTextView = _view.findViewById(R.id.imagery_scale_text_view);
        _increaseScaleButton = _view
                .findViewById(R.id.imagery_scaling_plus_button);
        _increaseScaleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adjustScale(2.00f);
            }
        });
        _decreaseScaleButton = _view
                .findViewById(R.id.imagery_scaling_minus_button);
        _decreaseScaleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adjustScale(0.5f);
            }
        });
    }

    void adjustScale(float factor) {
        ImageryRelativeScaleControl control = _mapView.getRenderer3()
                .getControl(
                        ImageryRelativeScaleControl.class);
        if (control != null) {
            control.setRelativeScale(control.getRelativeScale() * factor);
            final float scale = control.getRelativeScale();
            updateScaleText(scale);
            AtakPreferences.getInstance(_context)
                    .set("prefs_imagery_relative_scaling", scale);
        }
    }

    private void updateScaleText(float scale) {
        if (scale < 1.f)
            _scaleTextView.setText(String.format("1/%dx", (int) Math.pow(2.0,
                    Math.abs(Math.log(scale) / Math.log(2.0)))));
        else
            _scaleTextView.setText(String.format("%dx", (int) scale));
    }

    @Override
    public String getTitle() {
        return _context.getString(R.string.imagery_scaling);
    }

    @Override
    public String getUID() {
        return "imageryScaling";
    }

    @Override
    public Drawable getIconDrawable() {
        return _context.getDrawable(R.drawable.ic_imagery_scale);
    }

    @Override
    public Object getUserObject() {
        return this;
    }

    @Override
    public View getListItemView(View row, ViewGroup parent) {
        final MapRenderer3 mr = _mapView.getRenderer3();

        final ImageryRelativeScaleControl control = (mr == null) ? null
                : mr.getControl(
                        ImageryRelativeScaleControl.class);
        if (control != null) {
            updateScaleText(control.getRelativeScale());
        }

        _increaseScaleButton.setEnabled(control != null);
        _decreaseScaleButton.setEnabled(control != null);
        return _view;
    }
}
