
package com.atakmap.android.gui.coordinateentry;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.atakmap.android.util.AfterTextChangedWatcher;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

abstract class AbstractPane implements CoordinateEntryPane {

    protected final String uid;
    protected final Context context;
    protected final View view;

    protected String[] _originalRepresentation;
    protected GeoPointMetaData _originalPoint;

    private OnChangedListener onChangedListener;

    protected AbstractPane(Context context, String uid, View view) {
        this.context = context;
        this.uid = uid;
        this.view = view;
    }

    /**
     * Determines if a coordinate string is empty by checking to see if it is empty or if it
     * is composed of strings that are entirely empty.
     * @param coord the coordinate string
     * @return if it is empty
     */
    protected boolean isEmptyCoordinates(String[] coord) {
        if (FileSystemUtils.isEmpty(coord))
            return true;
        for (String s : coord) {
            if (!FileSystemUtils.isEmpty(s))
                return false;
        }
        return true;
    }

    /**
     * Check if the coordinate has been changed
     * @param coord New input coordinate
     * @param currCoord Original input coordinate
     * @return true if changed
     */
    protected boolean compareCoordinates(String[] coord, String[] currCoord) {
        if (FileSystemUtils.isEmpty(coord) &&
                FileSystemUtils.isEmpty(currCoord))
            return true;
        if (_originalPoint != null && !FileSystemUtils.isEmpty(coord)
                && !FileSystemUtils.isEmpty(currCoord)) {
            if (coord.length != currCoord.length)
                return true;
            // Check if the MGRS input has been changed
            // If not then use the same input point to avoid unexpected
            // point modifications (ATAK-8585)
            boolean changed = false;
            for (int i = 0; i < currCoord.length; i++) {
                if (!FileSystemUtils.isEquals(currCoord[i], coord[i])) {
                    changed = true;
                    break;
                }
            }
            return changed;
        }
        return true;
    }

    /**
     * Automates the setup of edit text elements that could be configured by the user or
     * programmatically set.
     * @param et the edit text to add a change listener to.
     */
    protected void addOnChangeListener(final EditText et) {
        et.addTextChangedListener(new AfterTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (et.hasFocus())
                    fireOnChange();
            }
        });
    }

    /**
     * Attempt to provide a better way to view a non-editable view.
     * @param v the view
     * @param editable true if it is editable.
     */
    protected void setEditable(View v, boolean editable) {
        if (v != null) {
            v.setEnabled(editable);
            if (v instanceof EditText)
                ((EditText) v).setTextColor(editable ? 0xFFFFFFFF : 0xFFCCCCCC);
        }
    }

    /**
     * Register EditText fields that would be able to handle a custom paste
     * @param list the list of edit text values
     */
    protected void customCopy(@NonNull
    final EditText[] list) {

        final ActionMode.Callback cb = new ActionMode.Callback() {

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case android.R.id.paste:
                        // custom paste:
                        ClipboardManager myClipboard = (ClipboardManager) context
                                .getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clipdata = myClipboard.getPrimaryClip();
                        if (clipdata != null && clipdata.getItemCount() > 0) {
                            ClipData.Item cdi = clipdata.getItemAt(0);
                            final String data = cdi.getText().toString();
                            if (data != null)
                                return processPaste(data);
                        }
                    default:
                        break;
                }
                return false;
            }
        };

        for (EditText l : list) {
            l.setCustomSelectionActionModeCallback(cb);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                l.setCustomInsertionActionModeCallback(cb);
            }
        }
    }

    /**
     * Callback if the user pastes a complex bit of data into the pane and the pane has
     * elected to try to parse it appropriately
     * @param data the String data to paste
     * @return if the paste was handled by the pane or false if the paste failed and will
     * be pasted via the default android paste capability.
     */
    protected boolean processPaste(@NonNull String data) {
        return false;
    }

    protected void hideKeyboard() {
        // Check if no view has focus:
        if (view instanceof ViewGroup) {
            View child = ((ViewGroup) view).getFocusedChild();
            if (child != null) {
                child.clearFocus();
                InputMethodManager inputManager = (InputMethodManager) context
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    protected void showKeyboard() {
        // Check if no view has focus:
        InputMethodManager inputManager = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
    }

    @Override
    public void setOnChangedListener(
            final OnChangedListener onChangedListener) {
        this.onChangedListener = onChangedListener;
    }

    /**
     * Call whenever human input has happened that would change the geospatial information
     */
    protected void fireOnChange() {
        if (onChangedListener != null)
            onChangedListener.onChange(this);
    }

    @Override
    public void dispose() {

    }
}
