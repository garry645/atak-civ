
package com.atakmap.android.gui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;

import com.atakmap.android.maps.MapView;
import com.atakmap.app.R;

public class CheckBoxTriState extends CheckBox {

    public enum State {
        UNCHECKED(0),
        SEMI_CHECKED(1),
        CHECKED(2);
        final int val;

        State(int val) {
            this.val = val;
        }

        public int getValue() {
            return val;
        }

        public static State find(int val) {
            for (State s : values())
                if (s.val == val)
                    return s;

            return UNCHECKED;
        }
    }

    private State state;

    private final OnCheckedChangeListener stateTracker = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
            switch (state) {
                case UNCHECKED:
                    setState(State.SEMI_CHECKED);
                    break;
                case SEMI_CHECKED:
                    setState(State.CHECKED);
                    break;
                case CHECKED:
                    setState(State.UNCHECKED);
                    break;
            }
        }
    };

    /**
     Holds a reference to the listener set by a client, if any.
     */
    private OnCheckedChangeListener listener;

    private boolean restoring;

    public CheckBoxTriState(Context context) {
        super(context);
        init();
    }

    public CheckBoxTriState(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CheckBoxTriState(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public State getState() {
        return state;
    }

    @Override
    public boolean isChecked() {
        // consider checked to be either semi checked or fully checked.
        return state != State.UNCHECKED;
    }

    /**
     * Set the state of the check box
     * @param state one of { UNCHECKED, SEMI_CHECKED, CHECKED }
     */
    public void setState(State state) {
        // do not fire the state change if in the middle of restoring a state
        if (!this.restoring && this.state != state) {
            this.state = state;

            if (this.listener != null) {
                this.listener.onCheckedChanged(this, this.isChecked());
            }

            updateBtn();
        }

    }

    @Override
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        if (stateTracker != listener) {
            this.listener = listener;
        }
        super.setOnCheckedChangeListener(stateTracker);

    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.state = state;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        this.restoring = true; // indicates that the ui is restoring its state
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setState(ss.state);
        requestLayout();
        this.restoring = false;
    }

    private void init() {
        state = State.UNCHECKED;
        updateBtn();
        setOnCheckedChangeListener(this.stateTracker);
    }

    private void updateBtn() {
        Drawable drawable;

        // need to make use of the ATAK resources
        Resources res = MapView.getMapView().getResources();
        switch (state) {
            case SEMI_CHECKED:
                drawable = res.getDrawable(R.drawable.ic_check_box);
                drawable.setColorFilter(res.getColor(R.color.white),
                        PorterDuff.Mode.SRC_ATOP);
                break;
            case CHECKED:
                drawable = res.getDrawable(R.drawable.ic_check_box);
                drawable.setColorFilter(res.getColor(R.color.green),
                        PorterDuff.Mode.SRC_ATOP);
                break;
            case UNCHECKED:
            default:
                drawable = res
                        .getDrawable(R.drawable.ic_check_box_outline_blank);
                drawable.setColorFilter(res.getColor(R.color.gray),
                        PorterDuff.Mode.SRC_ATOP);
                break;
        }
        setButtonDrawable(drawable);
    }

    static class SavedState extends BaseSavedState {
        State state;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            state = State.find(in.readInt());
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeValue(state.getValue());
        }

        @NonNull
        @Override
        public String toString() {
            return "TriStateCheckBox{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " state=" + state.getValue() + "}";
        }

        @SuppressWarnings("hiding")
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

    }
}
