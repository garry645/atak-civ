
package com.atakmap.android.gui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.SeekBar;

import androidx.annotation.RequiresApi;

import com.atakmap.app.ATAKApplication;
import com.atakmap.app.R;

@RequiresApi(Build.VERSION_CODES.N)
public class DiscreteSeekBar extends SeekBar {

    private String[] vals;
    private int textColor;
    private float textSize;
    private final Paint textPaint = new Paint();
    private final Rect textBounds = new Rect();

    public DiscreteSeekBar(Context context) {
        super(context);
    }

    public DiscreteSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DiscreteSeekBar(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public DiscreteSeekBar(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(18f);
        if (getTickMark() == null) {
            setTickMark(ATAKApplication.getCurrentActivity()
                    .getDrawable(R.drawable.tickmark));
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (vals != null) {

            int offset = this.getThumb().getMinimumWidth();
            int max = getMax();
            float interval = (float) ((getWidth() - offset * 1.0) / max);
            for (int i = 0; i <= max; i++) {
                final String val = i < vals.length ? vals[i] : "";
                textPaint.getTextBounds(val, 0, val.length(), textBounds);
                final float x = i * interval + offset / 2f
                        - textBounds.width() / 2f;
                canvas.drawText(val, x, getHeight(), textPaint);
            }
        }
    }

    /**
     * Provide a list of labels for the discrete ticks on the Seek Bar
     * @param labels the list of strings
     */
    public void setDiscreteLabels(String[] labels) {
        vals = labels;
    }

    /**
     * Allows for adjustment of the text size for the discrete labels.
     * @param textSize the size
     */
    public void setDiscreteLabelTextSize(float textSize) {
        this.textSize = textSize;
    }

    /**
     * Allows for adjustment of the text size for the discrete labels.
     * @param color the color
     */
    public void setDiscreteLabelTextColor(int color) {
        this.textColor = color;
    }

}
