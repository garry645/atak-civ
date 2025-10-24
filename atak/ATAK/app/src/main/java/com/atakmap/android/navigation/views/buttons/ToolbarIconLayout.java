
package com.atakmap.android.navigation.views.buttons;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.atakmap.app.R;

public class ToolbarIconLayout extends RelativeLayout {
    private Drawable _toolIconDrawable;
    private final Context _context;

    public ToolbarIconLayout(Context context, Drawable toolIcon) {
        super(context);
        _context = context;
        _toolIconDrawable = toolIcon;
        _init();
    }

    public ToolbarIconLayout(Context context, AttributeSet attrs, int color) {
        super(context, attrs);
        _context = context;
        _init();
    }

    private void _init() {
        LayoutInflater.from(_context).inflate(R.layout.toolbar_close_layout,
                this,
                true);
        ImageView toolIcon = findViewById(R.id.tool_icon);

        int size = getResources().getDimensionPixelSize(
                R.dimen.nav_child_button_size);
        setLayoutParams(new LayoutParams(size, size));

        toolIcon.setColorFilter(_context.getResources().getColor(R.color.sand),
                android.graphics.PorterDuff.Mode.MULTIPLY);
        if (_toolIconDrawable != null) {
            toolIcon.setImageDrawable(_toolIconDrawable);
        }
    }
}
