
package com.atakmap.android.chat;

import com.atakmap.android.contact.Contacts;
import com.atakmap.android.contact.GroupContact;
import com.atakmap.android.icons.Icon2525cIconAdapter;
import com.atakmap.android.maps.MapView;
import com.atakmap.app.R;

import java.util.ArrayList;

public class TeamGroup extends GroupContact {

    private final String teamName;
    private final int teamColor;

    public TeamGroup(final String team) {
        super(team, team, new ArrayList<>(), false);
        this.teamName = team;
        this.teamColor = Icon2525cIconAdapter.teamToColor(this.teamName);
        _iconUri = "asset://icons/roles/team.png";
        this.getExtras().putBoolean("fakeGroup", false);
        ChatDatabase.getInstance(MapView.getMapView().getContext())
                .changeLocallyCreated(this.getUID(), false);
    }

    @Override
    public int getIconColor() {
        return this.teamColor;
    }

    @Override
    protected void refreshImpl() {
        setContacts(Contacts.fromUIDs(Contacts.getInstance()
                .getAllContactsInTeam(this.teamName)));
        String userTeam = ChatManagerMapComponent.getTeamName();
        setUnmodifiable(!this.teamName.equals(userTeam));
        super.refreshImpl();
    }

    /**
     * Given a team value which is one of the team colors, translate it to the appropriate locale.
     * @param teamValue a color value in the squad list
     * @return the locale specific translation
     */
    public static String getLocaleTeamName(final String teamValue) {
        MapView _mapView = MapView.getMapView();
        if (_mapView == null)
            return teamValue;

        final String[] values = _mapView.getContext().getResources()
                .getStringArray(R.array.squad_values);
        final String[] names = _mapView.getContext().getResources()
                .getStringArray(R.array.squad_names);
        for (int i = 0; i < values.length; ++i) {
            if (values[i].equals(teamValue))
                return names[i];
        }
        return teamValue;
    }
}
