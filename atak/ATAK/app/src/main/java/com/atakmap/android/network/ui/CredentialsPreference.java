
package com.atakmap.android.network.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.atakmap.android.gui.PanPreference;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.util.AfterTextChangedWatcher;
import gov.tak.platform.lang.Parsers;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.net.AtakAuthenticationCredentials;
import com.atakmap.net.AtakAuthenticationDatabase;

import java.util.HashMap;
import java.util.Map;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * The ability to enter in the username and password credentials.    Upon changing the
 * preference the time in which the preference changed is recorded in the plain text key.
 * The user is required to listen for this change and then consult the AtakAuthenticationDatabase
 * for the credentials they are looking for.
 */
public class CredentialsPreference extends DialogPreference {

    private static final String TAG = "CredentialsPreference";
    @DeprecatedApi(since = "5.1", removeAt = "5.4", forRemoval = true)
    public static final String CREDENTIALS_UPDATED = "com.atakmap.android.network.ui.CREDENTIALS_UPDATED";

    private final Map<String, Integer> otherAttributes = new HashMap<>();

    protected View view;
    protected String credentialsType = AtakAuthenticationCredentials.TYPE_UNKNOWN;
    private static Context appContext;
    private final Context pContext;
    private ImageView imageView;

    protected boolean passwordOnly = false;
    private boolean expires = false;

    /**
     * For plugins we are REQUIRED to set the application context to the
     * ATAK owned Activity and not the context owned by the plugin.
     */
    public static void setContext(Context c) {
        appContext = c;
    }

    /**
     * Creates a credential preference screen that knows how to interact
     * with the AtakAuthenticationCredentials stored in ATAK
     * @param context the context to use
     * @param attrs the attributes
     * @param defStyle the default style
     */
    public CredentialsPreference(Context context, AttributeSet attrs,
            int defStyle) {
        super((appContext == null) ? context : appContext, attrs, defStyle);
        pContext = context;
        init(context, attrs);
    }

    public CredentialsPreference(Context context, AttributeSet attrs) {
        super((appContext == null) ? context : appContext, attrs);
        pContext = context;
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        PanPreference.setup(attrs, context, this, otherAttributes);
        if (attrs == null)
            return;

        for (int i = 0; i < attrs.getAttributeCount(); i++) {
            String attr = attrs.getAttributeName(i);
            String val = attrs.getAttributeValue(i);
            if (attr.equalsIgnoreCase("credentialsType")) {
                credentialsType = val;
            } else if (attr.equalsIgnoreCase("passwordOnly")) {
                if (Boolean.parseBoolean(val))
                    passwordOnly = true;
            } else if (attr.equalsIgnoreCase("expires")) {
                expires = Parsers.parseBoolean(val, false);
            }
        }

    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View v = super.onCreateView(parent);
        if (!isEnabled())
            v.setEnabled(false);

        if (v instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) v;
            ImageView iv = new ImageView(appContext);
            iv.setImageDrawable(
                    appContext.getDrawable(R.drawable.arrow_right));
            layout.addView(iv);
            imageView = iv;
        }
        setRightSideIconVisibility(isEnabled());
        return v;
    }

    private void setRightSideIconVisibility(boolean enabled) {
        try {
            imageView.setVisibility(enabled ? View.VISIBLE : View.GONE);
        } catch (Exception ignored) {

        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        setRightSideIconVisibility(isEnabled() && isSelectable());
    }

    @Override
    public void setSelectable(boolean selectable) {
        super.setSelectable(selectable);
        setRightSideIconVisibility(isSelectable());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setRightSideIconVisibility(isEnabled());
    }

    @Override
    protected View onCreateDialogView() {

        LayoutInflater inflater = LayoutInflater.from(getContext());
        view = inflater.inflate(R.layout.login_dialog, null);

        AtakAuthenticationCredentials credentials = AtakAuthenticationDatabase
                .getCredentials(credentialsType);

        final EditText username = view.findViewById(R.id.txt_name);
        if (passwordOnly) {
            final TextView txtlabel = view.findViewById(R.id.txt_label);
            txtlabel.setVisibility(View.GONE);
            username.setVisibility(View.GONE);
        }

        final EditText pwdText = view.findViewById(R.id.password);

        final CheckBox checkBox = view.findViewById(R.id.password_checkbox);
        checkBox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton,
                            boolean isChecked) {
                        if (isChecked) {
                            pwdText.setTransformationMethod(
                                    HideReturnsTransformationMethod
                                            .getInstance());
                        } else {
                            pwdText.setTransformationMethod(
                                    PasswordTransformationMethod.getInstance());
                        }
                    }
                });

        if (credentials != null) {
            username.setText(credentials.username);
            pwdText.setText(credentials.password);

            if (!FileSystemUtils.isEmpty(credentials.password)) {
                checkBox.setEnabled(false);
                pwdText.addTextChangedListener(new AfterTextChangedWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        if (s != null && s.length() == 0) {
                            checkBox.setEnabled(true);
                            pwdText.removeTextChangedListener(this);
                        }
                    }
                });
            } else {
                checkBox.setEnabled(true);
            }
        }

        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {

        if (!positiveResult)
            return;

        String username = ((EditText) view.findViewById(R.id.txt_name))
                .getText().toString();
        if (username != null)
            username = username.trim();
        String password = ((EditText) view.findViewById(R.id.password))
                .getText().toString();

        AtakAuthenticationDatabase.saveCredentials(
                credentialsType,
                username, password, expires);

        AtakBroadcast.getInstance().sendBroadcast(
                new Intent(CREDENTIALS_UPDATED).putExtra("type",
                        credentialsType));
        // persist the time which this preference was changed
        persistLong(CoordinatedTime.currentTimeMillis());
        notifyDependencyChange(shouldDisableDependents());
        notifyChanged();
    }

    @Override
    protected void showDialog(Bundle bundle) {
        super.showDialog(bundle);
        Dialog dialog = getDialog();
        if (dialog != null) {

            try {
                Integer resId = otherAttributes.get("name");
                if (resId != null)
                    dialog.setTitle(pContext.getString(resId));
            } catch (Exception ignored) {
            }
            final Window window = dialog.getWindow();
            try {
                if (window != null) {
                    window.setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                                    |
                                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

                    Rect displayRectangle = new Rect();
                    window.getDecorView()
                            .getWindowVisibleDisplayFrame(displayRectangle);
                    window.setLayout(
                            (int) (displayRectangle.width() * 1.0f),
                            (int) (displayRectangle.height() * 1.0f));
                }
            } catch (IllegalArgumentException e) {
                //     ATAK-7278 Preferences IllegalArgumentException
                Log.d(TAG, "guarding against an issue from a crash log",
                        e);
            }

        }
    }
}
