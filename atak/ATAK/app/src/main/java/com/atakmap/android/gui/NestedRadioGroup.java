
package com.atakmap.android.gui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Implementation of a Radio Group that allows for nested Radio Buttons allowing for formatting
 * to occur on devices where there needs to be line breaks betwen the buttons.
 */
public class NestedRadioGroup extends RadioGroup
        implements CompoundButton.OnCheckedChangeListener {

    public NestedRadioGroup(Context context) {
        super(context);
    }

    public NestedRadioGroup(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        registerRecursive(child);
    }

    private void registerRecursive(View child) {
        if (child instanceof RadioButton) {
            radioButtonMap.add((RadioButton) child);
            ((RadioButton) child).setOnCheckedChangeListener(this);
        } else if (child instanceof ViewGroup) {
            ViewGroup group = ((ViewGroup) child);
            for (int i = 0; i < group.getChildCount(); ++i) {
                registerRecursive(group.getChildAt(i));
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (ignore)
            return;
        ignore = true;
        for (RadioButton button : radioButtonMap) {
            button.setChecked(button == compoundButton);
        }
        if (mocl != null)
            mocl.onCheckedChanged(this, compoundButton.getId());
        ignore = false;
    }

    boolean ignore = false;
    RadioGroup.OnCheckedChangeListener mocl = null;

    @Override
    public void setOnCheckedChangeListener(
            RadioGroup.OnCheckedChangeListener listener) {
        mocl = listener;
    }

    @Override
    public void check(int id) {
        for (RadioButton rb : radioButtonMap) {
            rb.setChecked(rb.getId() == id);
        }
    }

    @Override
    public void clearCheck() {
        check(-1);
    }

    public int getCheckedRadioButtonId() {
        for (RadioButton rb : radioButtonMap) {
            if (rb.isChecked())
                return rb.getId();
        }
        return -1;
    }

    private final Set<RadioButton> radioButtonMap = new CopyOnWriteArraySet<>();
}
