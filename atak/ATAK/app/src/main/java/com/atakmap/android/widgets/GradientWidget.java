
package com.atakmap.android.widgets;

import com.atakmap.android.maps.MapTextFormat;
import com.atakmap.android.maps.MapView;

import java.util.concurrent.ConcurrentLinkedQueue;

public class GradientWidget extends MapWidget2 {

    public interface OnLegendContentChangedListener {
        void onLegendContentChangedListener(GradientWidget widget);
    }

    private int[] argbColorKeys = new int[0];
    private String[] lineItemStrings = new String[0];

    private float _barWidth, _barHeight;

    private final ConcurrentLinkedQueue<OnLegendContentChangedListener> listeners = new ConcurrentLinkedQueue<>();

    public void setLegend(int[] argbColorKeys, String[] lineItemStrings) {
        argbColorKeys = argbColorKeys != null ? argbColorKeys : new int[0];
        lineItemStrings = lineItemStrings != null ? lineItemStrings
                : new String[0];
        this.argbColorKeys = new int[argbColorKeys.length];
        this.lineItemStrings = new String[lineItemStrings.length];
        System.arraycopy(lineItemStrings, 0, this.lineItemStrings, 0,
                this.lineItemStrings.length);
        System.arraycopy(argbColorKeys, 0, this.argbColorKeys, 0,
                this.argbColorKeys.length);
        calculateSize();
        notifyLegendContentChanged();
    }

    public int[] getArgbColorKeys() {
        return argbColorKeys;
    }

    public String[] getLineItemStrings() {
        return lineItemStrings;
    }

    public MapTextFormat getTextFormat() {
        return MapView.getDefaultTextFormat();
    }

    public void addOnLegendContentChangedListener(
            OnLegendContentChangedListener l) {
        listeners.add(l);
    }

    public void removeOnLegendContentChangedListener(
            OnLegendContentChangedListener l) {
        listeners.remove(l);
    }

    public String getLegendText() {
        if (this.lineItemStrings == null)
            return "";

        StringBuilder sb = new StringBuilder();
        String delim = "";
        for (String lineItem : this.lineItemStrings) {
            sb.append(delim);
            sb.append(lineItem);
            delim = "\n";
        }

        return sb.toString();
    }

    public void setBarSize(float barWidth, float barHeight) {
        if (Float.compare(barWidth, _barWidth) != 0
                || Float.compare(barHeight, _barHeight) != 0) {
            _barWidth = barWidth;
            _barHeight = barHeight;
            calculateSize();
        }
    }

    public float[] getBarSize() {
        return new float[] {
                _barWidth, _barHeight
        };
    }

    @Override
    public boolean setPadding(float left, float top, float right,
            float bottom) {
        // Only left and bottom are used for inner padding of labels
        if (Float.compare(left, _padding[LEFT]) != 0
                || Float.compare(bottom, _padding[BOTTOM]) != 0) {
            _padding[LEFT] = left;
            _padding[BOTTOM] = top;
            calculateSize();
            return true;
        }
        return false;
    }

    @Override
    public void orientationChanged() {
        calculateSize();
    }

    private void calculateSize() {
        MapTextFormat format = getTextFormat();
        super.setSize(
                100.f,
                format.measureTextHeight(getLegendText()));
    }

    private void notifyLegendContentChanged() {
        for (OnLegendContentChangedListener l : listeners) {
            l.onLegendContentChangedListener(this);
        }
    }
}
