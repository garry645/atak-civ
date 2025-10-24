
package com.atakmap.android.chat;

import com.atakmap.android.contact.Contacts;
import com.atakmap.android.contact.GroupContact;
import com.atakmap.android.maps.MapView;
import com.atakmap.app.R;
import com.atakmap.coremap.locale.LocaleUtil;

import java.util.ArrayList;

/* Per josh Role Groups should:
 *  Team leads talk to their team, other team leads, and HQ.
 *  HQ talks to other HQ and team leads 
 *  Team members talk to other team members and their team lead (instead we just don't generate a role group and they can use the Team Group (i.e Cyan)
 *
 */

public class RoleGroup extends GroupContact {

    private final String roleName;

    public RoleGroup(final String role) {
        super(role, role, new ArrayList<>(), false);
        this.roleName = role;
        this.getExtras().putBoolean("fakeGroup", false);
        _iconUri = "asset://icons/roles/"
                + role.toLowerCase(LocaleUtil.getCurrent()).replace(" ", "")
                + ".png";
        setHideIfEmpty(true);
        ChatDatabase.getInstance(MapView.getMapView().getContext())
                .changeLocallyCreated(this.getUID(), false);
    }

    @Override
    protected void refreshImpl() {
        String myRole = ChatManagerMapComponent.getRoleName();
        setContacts(Contacts.fromUIDs(Contacts.getInstance()
                .getAllContactsWithRole(roleName)));
        setUnmodifiable(myRole == null || !myRole.equals(roleName));
        super.refreshImpl();
    }

    /**
     * Given a role value which is one of the roles, translate it to the appropriate locale.
     * @param roleValue a value in the role list
     * @return the locale specific translation
     */

    public static String getLocaleRoleName(final String roleValue) {
        MapView _mapView = MapView.getMapView();
        if (_mapView == null)
            return roleValue;

        if ("Default".equals(roleValue))
            return _mapView.getContext().getString(R.string.default_grouping);

        final String[] values = _mapView.getContext().getResources()
                .getStringArray(R.array.role_values);
        final String[] names = _mapView.getContext().getResources()
                .getStringArray(R.array.role_names);
        for (int i = 0; i < values.length; ++i) {
            if (values[i].equals(roleValue))
                return names[i];
        }
        return roleValue;
    }
}
