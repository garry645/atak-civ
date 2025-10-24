
package com.atakmap.android.cotselector;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.annotation.NonNull;

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.app.ATAKApplication;
import com.atakmap.app.R;
import com.atakmap.util.Collections2;

import java.util.ArrayList;
import java.util.List;

import gov.tak.api.cot.detail.Role;
import gov.tak.api.cot.detail.RolesManager;

public class CustomRoleListView extends ListView
        implements View.OnClickListener, View.OnKeyListener, TextWatcher {

    public interface OnTypeChangedListener {
        void notifyChanged(Role role);
    }

    private OnTypeChangedListener cs;
    private ImageButton backB = null;

    private EditText searchText = null;

    private CustomRoleAdapter currentAdapter = null;
    private List<Role> searchResults = null;

    private RolesManager rolesManager;

    public CustomRoleListView(Context context) {
        super(context);
    }

    public void init(final View v, final OnTypeChangedListener cs) {
        this.rolesManager = CotMapComponent.getInstance().getRolesManager();

        this.currentAdapter = new CustomRoleAdapter(getContext(),
                R.layout.a2525listitem, this, rolesManager.getRoles());
        this.setAdapter(currentAdapter);

        this.cs = cs;

        setScrollbarFadingEnabled(false);

        backB = v.findViewById(R.id.BackB);
        backB.setOnClickListener(this);

        final ImageButton searchB = v.findViewById(R.id.hierarchy_search_btn);
        searchB.setEnabled(false);

        searchText = v.findViewById(R.id.hierarchy_search_text);
        searchText.addTextChangedListener(this);
        searchText.setInputType(InputType.TYPE_CLASS_TEXT);
        InputMethodManager imm = (InputMethodManager) ATAKApplication.getCurrentActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.showSoftInput(searchText, InputMethodManager.SHOW_IMPLICIT);

        // only need to listen for the key listener when the search text is active
        searchText.setOnKeyListener(this);
    }

    @Override
    public void onVisibilityChanged(@NonNull View changedView,
                                    int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (changedView.equals(this)) {
            cancelSearch();
        }
    }

    public void goDeeper(int position) {
        this.currentAdapter = new CustomRoleAdapter(getContext(),
                R.layout.a2525listitem, this,
                currentAdapter.getRoles().get(position).getChildren());
        setAdapter(currentAdapter);
    }

    public void allowAffiliationChange(boolean affiliationChange) {
    }

    public void onSelected(Role selectedRole) {
        cs.notifyChanged(selectedRole);
    }

    @Override
    public void onClick(View view) {
        if (view.equals(backB)) {
            goBack(Collections2.firstOrNull(currentAdapter.getRoles()));
        }
    }

    private void cancelSearch() {
        dismissKeyboard(searchText);
        searchText.setText("");
        goBack(null, false);
    }

    private boolean dismissKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) ATAKApplication.getCurrentActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null)
            return false;
        final boolean showing = imm.isAcceptingText();
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        return showing;
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1,
            int i2) {

        if (charSequence == null)
            return;

        if (!searchText.hasFocus()) {
            return;
        }

        String searchString = charSequence.toString();

        if (searchString.length() >= 3) {
            searchResults = rolesManager.search(searchString);
            ArrayList<Role> matchingRoles = new ArrayList<>(searchResults);
            currentAdapter = new CustomRoleAdapter(getContext(),
                    R.layout.a2525listitem, this, matchingRoles);
            setAdapter(currentAdapter);
        } else {
            if (searchResults != null) {
                goBack(null, false);
            }
            searchResults = null;
        }

        currentAdapter.notifyDataSetChanged();
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1,
            int i2) {

    }

    public boolean goBack(final Role role) {
        return goBack(role, true);
    }

    private boolean goBack(final Role role, boolean cancelSearch) {
        if (cancelSearch)
            cancelSearch();

        List<Role> roles = rolesManager.getRoles();
        if (role != null) {
            Role parent = role.getParent();
            if (parent != null) {
                parent = parent.getParent();
                if (parent != null)
                    roles = parent.getChildren();
            }
        }
        currentAdapter = new CustomRoleAdapter(getContext(),
                R.layout.a2525listitem, this, roles);
        setAdapter(currentAdapter);
        return true;
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (dismissKeyboard(searchText))
                return true;

            final CharSequence searchString = searchText.getText();
            if (searchString != null && searchString.length() > 0) {
                goBack(null);
                return true;
            }
        }
        return false;
    }
}
