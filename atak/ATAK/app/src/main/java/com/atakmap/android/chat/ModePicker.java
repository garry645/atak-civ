
package com.atakmap.android.chat;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.atakmap.app.R;

import java.util.ArrayList;
import java.util.List;

public class ModePicker extends LinearLayout {

    public interface ModeUpdateListener {
        void onModeUpdate(String mode);
    }

    private final Context _context;
    private String[] _values;
    private int _index = -1;
    final TextView _currentMode;
    final View _buttonChange;
    GridLayout _imageRotator;

    private final List<ModeUpdateListener> _listeners = new ArrayList<>();

    public ModePicker(Context context) {
        this(context, null, null);
    }

    public ModePicker(Context context, AttributeSet attrs) {
        this(context, null, attrs);
    }

    public ModePicker(Context context, String[] values, AttributeSet attrs) {
        super(context, attrs);
        _context = context;
        _values = values;
        LayoutInflater inflater = LayoutInflater.from(_context);
        inflater.inflate(R.layout.mode_picker, this, true);

        _currentMode = this.findViewById(R.id.currentMode);
        advanceMode();
        _buttonChange = this.findViewById(R.id.buttonChange);
        _buttonChange.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                advanceMode();
            }

        });
        _buttonChange.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return ModePicker.this.performLongClick();
            }
        });
        if (_values != null && _values.length > 0) {
            _imageRotator = this.findViewById(R.id.image_rotator);
            for (String _value : _values) {
                ImageView child = new ImageView(_context);
                child.setImageResource(R.drawable.not_selected);
                _imageRotator.addView(child);
            }
        }

    }

    /**
     * Given a value, return the index of the name provided or if the name is not found return -1
     */
    public int getIndex(String name) {
        for (int i = 0; i < _values.length; ++i) {
            if (_values[i].equals(name))
                return i;
        }
        return -1;
    }

    public int getCurrentIndex() {
        return _index;
    }

    public void setIndex(int index) {
        if (index < 0 || index > _values.length) {
            throw new IllegalArgumentException("invalid index");
        } else if (_index == index) {
            return;
        } else {
            _index = index;
            _currentMode.setText(_values[_index]);
            ImageView child2 = (ImageView) _imageRotator.getChildAt(_index);
            child2.setImageResource(R.drawable.selected);
            for (ModeUpdateListener listener : _listeners) {
                listener.onModeUpdate(_values[_index]);
            }
        }
    }

    public void setOnModeUpdateListener(ModeUpdateListener listener) {
        _listeners.add(listener);
    }

    private void advanceMode() {
        if (_values != null) {
            if (_index != -1) {
                ImageView child = (ImageView) _imageRotator.getChildAt(_index);
                child.setImageResource(R.drawable.not_selected);
            }
            _index++;
            _index %= _values.length;
            _currentMode.setText(_values[_index]);
            ImageView child2 = (ImageView) _imageRotator.getChildAt(_index);
            child2.setImageResource(R.drawable.selected);
            for (ModeUpdateListener listener : _listeners) {
                listener.onModeUpdate(_values[_index]);
            }
        }
    }

    public void setValues(String[] values) {
        _values = values;
        if (_values != null && _values.length > 0) {
            _imageRotator = this.findViewById(R.id.image_rotator);
            _imageRotator.removeAllViews();
            for (String _value : _values) {
                ImageView child = new ImageView(_context);
                child.setImageResource(R.drawable.not_selected);
                _imageRotator.addView(child);
            }
        }
    }

    public String[] getValues() {
        return _values;
    }
}
