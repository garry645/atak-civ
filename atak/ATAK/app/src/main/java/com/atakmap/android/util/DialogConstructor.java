
package com.atakmap.android.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.gui.EditTextWithKeyPadDismissEvent;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import gov.tak.api.util.AttributeSet;
import gov.tak.platform.lang.Parsers;


public class DialogConstructor {

    public static void buildDialog(final Context _context, final EditText et,
            final String metaValueToSet, String title, int inputType,
            final Boolean storeAsString, final PointMapItem target) {
        EditTextWithKeyPadDismissEvent customEt = createCustomEditText(_context,
                et, inputType);
        AlertDialog.Builder b = createDialogBuilder(_context, title, customEt);
        AlertDialog d = buildDialog(b);
        setOnEditActionListener(_context, et, d);

        d.show();
        d.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String txt = customEt.getText().toString();
                        if (metaValueToSet != null) {
                            if (storeAsString) {
                                target.setMetaString(metaValueToSet, txt);
                            } else {
                                if (FileSystemUtils.isEmpty(txt)) {
                                    target.removeMetaData(metaValueToSet);
                                } else {
                                    try {
                                        int v = Parsers.parseInt(txt, 0);
                                        target.setMetaInteger(metaValueToSet,
                                                v);
                                    } catch (Exception e) {
                                        Toast.makeText(_context,
                                                R.string.invalid_number,
                                                Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                }
                            }
                        }
                        et.setText(txt);
                        d.dismiss();
                    }
                });
    }

    public static void buildDialog(final Context _context, final EditText et,
            final String metaValueToSet, String title, int inputType,
            final Boolean storeAsString, final AttributeSet attrs) {
        EditTextWithKeyPadDismissEvent customEt = createCustomEditText(_context,
                et, inputType);
        AlertDialog.Builder b = createDialogBuilder(_context, title, customEt);
        AlertDialog d = buildDialog(b);
        setOnEditActionListener(_context, et, d);

        d.show();
        d.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String txt = customEt.getText().toString();
                        if (metaValueToSet != null) {
                            if (storeAsString) {
                                attrs.setAttribute(metaValueToSet, txt);
                            } else {
                                if (FileSystemUtils.isEmpty(txt)) {
                                    attrs.removeAttribute(metaValueToSet);
                                } else {
                                    try {
                                        int v = Parsers.parseInt(txt, 0);
                                        attrs.setAttribute(metaValueToSet,
                                                v);
                                    } catch (Exception e) {
                                        Toast.makeText(_context,
                                                R.string.invalid_number,
                                                Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                }
                            }
                        }
                        et.setText(txt);
                        d.dismiss();
                    }
                });
    }

    private static AlertDialog.Builder createDialogBuilder(Context _context,
            String title,
            View view) {
        AlertDialog.Builder b = new AlertDialog.Builder(_context);
        b.setTitle(title);
        b.setPositiveButton(R.string.ok, null);
        b.setNegativeButton(R.string.cancel, null);
        b.setView(view);
        return b;
    }

    private static AlertDialog buildDialog(AlertDialog.Builder b) {
        final AlertDialog d = b.create();
        Window w = d.getWindow();
        if (w != null)
            w.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return d;
    }

    private static EditTextWithKeyPadDismissEvent createCustomEditText(
            Context _context,
            EditText et, int inputType) {
        final EditTextWithKeyPadDismissEvent o = new EditTextWithKeyPadDismissEvent(
                _context);
        o.setInputType(inputType);
        o.setSingleLine(true);
        o.setFilters(et.getFilters());
        o.setText(et.getText().toString());
        o.selectAll();
        o.requestFocus();

        return o;
    }

    private static void setOnEditActionListener(Context _context, EditText et,
            AlertDialog d) {
        et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                    KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) _context
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null)
                        imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
                    et.clearFocus();
                    d.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                    return true;
                } else
                    return false;
            }
        });
    }
}
