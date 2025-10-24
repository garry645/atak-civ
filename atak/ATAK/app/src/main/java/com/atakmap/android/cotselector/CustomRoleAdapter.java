
package com.atakmap.android.cotselector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;

import com.atakmap.android.chat.RoleGroup;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.util.ArrayList;
import java.util.List;

import gov.tak.api.cot.detail.Role;

class CustomRoleAdapter extends ArrayAdapter<String> {

    private final CustomRoleListView clv;
    private final Context context;
    private final List<Role> roles;

    public List<Role> getRoles() {
        return roles;
    }

    private static String _currentType = "undefined";

    static List<String> objectsFromRoles(List<Role> roles) {
        ArrayList<String> list = new ArrayList<>();
        for (Role r : roles) {
            list.add(r.getName());
        }
        return list;
    }

    CustomRoleAdapter(Context context, int textViewResourceId,
            CustomRoleListView clv,
            List<Role> roles) {
        super(context, textViewResourceId, objectsFromRoles(roles));
        this.context = context;
        this.clv = clv;
        this.roles = roles;

    }

    public void setType(String type) {
        _currentType = type;
    }

    boolean canGoDeeper(int position) {
        Role role = roles.get(position);
        return !role.getChildren().isEmpty();
    }

    private boolean compareType(String type) {
        if (FileSystemUtils.isEmpty(_currentType))
            return false;
        String currentType = _currentType.substring(0,
                _currentType.indexOf("-"));
        String compareType = type.substring(0, type.indexOf("-"));
        return currentType.startsWith(compareType);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView,
            @NonNull ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater li = LayoutInflater.from(context);
            convertView = li.inflate(R.layout.a2525listitem, null, true);
            viewHolder = new ViewHolder();
            viewHolder.current = convertView.findViewById(R.id.button1);
            viewHolder.lower = convertView.findViewById(R.id.button2);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String item = getItem(position);

        if (item != null) {

            viewHolder.current.setText(RoleGroup.getLocaleRoleName(item));
            OnClickListener onClickListener = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (canGoDeeper(position))
                        clv.goDeeper(position);
                    else
                        clv.onSelected(roles.get(position));
                }

            };

            viewHolder.current.setOnClickListener(onClickListener);

            if (canGoDeeper(position)) {
                viewHolder.lower.setVisibility(View.VISIBLE);// since it could be being adapted, make sure
                // it's visible
                viewHolder.lower.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        clv.goDeeper(position);
                    }

                });
            } else {
                viewHolder.lower.setVisibility(View.INVISIBLE);
            }
        }
        return convertView;
    }

    static class ViewHolder {
        Button current;
        ImageButton lower;
    }
}
