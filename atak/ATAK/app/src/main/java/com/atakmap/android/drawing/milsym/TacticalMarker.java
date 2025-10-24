
package com.atakmap.android.drawing.milsym;

import com.atakmap.android.icons.UserIcon;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.preference.UnitPreferences;
import com.atakmap.android.user.icon.Icon2525cPallet;
import com.atakmap.android.util.IconUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.math.PointD;

import java.util.Collection;

import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.Modifier;
import gov.tak.api.util.AttributeSet;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.symbology.SymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilSymStandardAttributes;

final class TacticalMarker implements
        MapItem.OnMetadataChangedListener, MapItem.OnTypeChangedListener {

    private final static String TAG = "TacticalMarker";

    private final Marker subject;
    private final UnitPreferences _prefs;

    TacticalMarker(Marker subject) {
        _prefs = new UnitPreferences(MapView.getMapView().getContext());

        this.subject = subject;

        this.subject.addOnMetadataChangedListener("milsym", this);
        this.subject.addOnMetadataChangedListener("modifierVersion", this);
        this.subject.addOnTypeChangedListener(this);

        onMetadataChanged(this.subject, "milsym");
        onMetadataChanged(this.subject, "modifierVersion");
    }


    @Override
    public void onMetadataChanged(MapItem mapItem, String s) {
        if ((s.equals("modifierVersion") || s.equals("milsym")) &&
                mapItem.getMetaBoolean("adapt_marker_icon", true)) {
            final String iconsetPath = this.subject
                    .getMetaString(UserIcon.IconsetPath, null);
            if (iconsetPath != null && !iconsetPath
                    .startsWith(Icon2525cPallet.COT_MAPPING_2525))
                return;
            final String milsym = this.subject.getMetaString("milsym", null);

            Bitmap bitmap;

            final AttributeSet modifierAS = new AttributeSet();

            final Collection<Modifier> modifiers = SymbologyProvider.getModifiers(milsym);
            if (modifiers != null) {
                for (Modifier modifier: modifiers) {
                    String key = MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX
                            + modifier.getId();
                    if (mapItem.hasMetaValue(key)) {
                        modifierAS.setAttribute(key,
                                mapItem.getMetaString(key, ""));
                    }
                }

                boolean agl =  _prefs.get("alt_display_agl", false);
                modifierAS.setAttribute("milsym.altitudeReference", agl ? "AGL" : _prefs.getAltitudeReference());
                modifierAS.setAttribute("milsym.altitudeUnits", _prefs.getAltitudeUnits().getPlural());
            }

            ISymbologyProvider.RendererHints renderHints = new ISymbologyProvider.RendererHints();
            // explicitly set `iconSize` to 32 here rather than on the icon
            // builder. the icon builder will use the size of the bitmap to
            // defer to any adjustments made by the milsym renderer to
            // account for modifiers
            renderHints.iconSize = 32;
            renderHints.iconCenterOffset = new PointD();
            renderHints.controlPoint = MarshalManager.marshal(subject.getPoint(), GeoPoint.class, IGeoPoint.class);
            bitmap = SymbologyProvider.renderSinglePointIcon(milsym, modifierAS,
                    renderHints);
            if (bitmap == null) {
                Log.e(TAG, "could not render: " + milsym, new Exception());
                return;
            }

            final android.graphics.Bitmap marshalledBitmap =
                    MarshalManager.marshal(
                            bitmap,
                            Bitmap.class,
                            android.graphics.Bitmap.class);
            final String encoded = IconUtilities.encodeBitmap(marshalledBitmap);

            if (encoded == null) {
                Log.e(TAG, "could not encode: " + milsym +
                        " the bitmap " + marshalledBitmap + " recycled: " +
                        marshalledBitmap.isRecycled(), new Exception());
                return;
            }


            com.atakmap.coremap.maps.assets.Icon.Builder markerIconBuilder =
                    new com.atakmap.coremap.maps.assets.Icon.Builder()
                    .setImageUri(0, encoded);
            markerIconBuilder.setSize(bitmap.getWidth(),
                    bitmap.getHeight());
            markerIconBuilder.setAnchor(
                    (int) renderHints.iconCenterOffset.x,
                    (int) renderHints.iconCenterOffset.y);
            subject.setIcon(markerIconBuilder.build());

        }
    }

    @Override
    public void onTypeChanged(MapItem mapItem) {
        final String milsym = mapItem.getMetaString("milsym", null);
        if (milsym == null)
            return;

        Affiliation affiliation = SymbologyProvider.getAffiliation(milsym);
        final String type = mapItem.getType();

        if (!type.startsWith("a-") && type.length() > 3)
            return; // not an atom

        final char affiliationChar = type.charAt(2);

        switch (affiliationChar) {
            case 'p':
                affiliation = Affiliation.Pending;
                break;
            case 'u':
                affiliation = Affiliation.Unknown;
                break;
            case 'a':
                affiliation = Affiliation.AssumedFriend;
                break;
            case 'f':
                affiliation = Affiliation.Friend;
                break;
            case 'n':
                affiliation = Affiliation.Neutral;
                break;
            case 's':
                affiliation = Affiliation.Suspect;
                break;
            case 'h':
                affiliation = Affiliation.Hostile;
                break;
            case 'g':
                affiliation = Affiliation.ExercisePending;
                break;
            case 'w':
                affiliation = Affiliation.ExerciseUnknown;
                break;
            case 'm':
                affiliation = Affiliation.ExerciseAssumedFriend;
                break;
            case 'd':
                affiliation = Affiliation.ExerciseFriend;
                break;
            case 'l':
                affiliation = Affiliation.ExerciseNeutral;
                break;
            case 'j':
                affiliation = Affiliation.Joker;
                break;
            case 'k':
                affiliation = Affiliation.Faker;
                break;
            default:
                break;
        }
        // update the affiliation and `milsym` (if changed)
        final String s = SymbologyProvider.setAffiliation(milsym, affiliation);
        if (s != null && !s.equals(milsym))
            mapItem.setMetaString("milsym", s);


        // change groups if needed based on the change of the type
        final MapGroup source = mapItem.getGroup();
        if (source != null) {
            MapGroup parent = source.getParentGroup();
            final String sourceFriendlyName = source.getFriendlyName();
            if ((!FileSystemUtils.isEmpty(sourceFriendlyName))
                    &&
                    (affiliationChar != Character.toLowerCase(sourceFriendlyName
                            .charAt(0)))) {
                // if the group is not null, as in this item already exists in a group then
                if (parent != null) {
                    for (MapGroup dest : parent.getChildGroups()) {
                        final String destGroupName = dest.getFriendlyName();
                        if ((!FileSystemUtils.isEmpty(destGroupName))
                                &&
                                (affiliationChar == Character.toLowerCase(destGroupName
                                        .charAt(0)))) {
                            if (dest != source) {
                                dest.addItem(mapItem);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
