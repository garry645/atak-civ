
package com.atakmap.android.user.icon;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.atakmap.android.cotselector.CoTSelector;
import com.atakmap.android.gui.AlertDialogHelper;
import com.atakmap.android.icons.CotDescriptions;
import com.atakmap.android.icons.UserIcon;
import com.atakmap.android.icons.UserIconSet;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.metrics.MetricsApi;
import com.atakmap.android.metrics.MetricsUtils;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.toolbar.Tool;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.user.CustomNamingView;
import com.atakmap.android.user.EnterLocationTool;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.android.user.icon.IconPallet.CreatePointException;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.app.R;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gov.tak.api.cot.CotUtils;
import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.platform.symbology.SymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilStd2525;
import gov.tak.platform.symbology.milstd2525.MilStd2525Base;

public class Icon2525cPalletFragment extends Fragment  {

    private static final String TAG = "Icon2525cPalletFragment";

    private static final String ICON2525_POINT_DROPPER_PREF = "2525c_pallet_dropper_custom_types";
    private static final String PROVIDER_2525_TYPE_PREF = "provider_2525_type";
    private static final String PROVIDER_2525_MIL_PREF = "provider_2525_mil";

    private ImageButton unknownRb;
    private ImageButton neutralRb;
    private ImageButton hostileRb;
    private ImageButton friendlyRb;

    private static String _currType;
    private static Affiliation _currAffiliation;
    private ImageButton _typeChecked;
    private Button _subtypeButton;
    private int checkedPosition;
    private CustomNamingView _customNamingView;
    private SubTypePair currPair;

    private final AtakPreferences _prefs;
    private final CoTSelector selector;
    private final Context ctx;

    ISymbologyProvider defaultProvider;

    public Icon2525cPalletFragment() {
        MapView mv = MapView.getMapView();
        ctx = mv.getContext();
        selector = new CoTSelector(mv);
        _prefs = AtakPreferences.getInstance(ctx);

    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCurrPair();
    }

    /**
     * Create a new instance of CountingFragment, providing "num"
     * as an argument.
     */
    public static Icon2525cPalletFragment newInstance(UserIconSet iconset) {
        Icon2525cPalletFragment f = new Icon2525cPalletFragment();

        Bundle args = new Bundle();
        args.putInt("id", iconset.getId());
        args.putString("name", iconset.getName());
        args.putString("uid", iconset.getUid());
        f.setArguments(args);
        return f;
    }

    private boolean isSubtypeCompatibleProvider(ISymbologyProvider provider) {
        final String name = provider.getName();
        switch(name) {
            case "2525C" :
            case "2525D" :
            case "2525E" :
                return true;
            default :
                return false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.enter_location_2525c, container,
                false);
        ArrayList<SubTypePair> pairs = new ArrayList<>(Arrays.asList(
                new SubTypePair(getString(R.string.ground), "G", "S*G*------*****",
                        "10001000001200000000", false),
                new SubTypePair(getString(R.string.air_defense), "G-U-C-D","S*G*UCD---*****",
                        "10001000001301000000",false),
                new SubTypePair(getString(R.string.anti_aircraft_artillery),
                        "G-E-W-A", "SUG*EWA--------","10011500001105000000",false),
                new SubTypePair(getString(R.string.aircraft), "A", "S*A*------*****",
                        "10000100000000000000",false),
                new SubTypePair(getString(R.string.artillery), "G-U-C-F","S*G*UCF---*****",
                        "10001000001303000000",false),
                new SubTypePair(getString(R.string.building), "G-I", "S*G*I-----H****",
                        "10002000000000000000",false),
                new SubTypePair(getString(R.string.mine), "G-E-X-M", "S*G*EXM---*****",
                        "10001500002101000000",false),
                new SubTypePair(getString(R.string.ship), "S", "S*S*------*****",
                        "10003000000000000000",false),
                new SubTypePair(getString(R.string.sniper), "G-U-C-I-d", "S*G*UCI---*****",
                        "10001000001215000000",false),
                new SubTypePair(getString(R.string.surface_to_air_missile),
                        "G-U-C-D-M", "S*G*UCDM--*****",
                        "10001000001301020000",false),
                new SubTypePair(getString(R.string.tank), "G-E-V-A-T", "S*G*EVAT--*****",
                        "10001500001202000000",false),
                new SubTypePair(getString(R.string.troops), "G-U-C-I", "S*G*UCI---*****",
                        "10001000001211000000",false),
                new SubTypePair(getString(R.string.vehicles), "G-E-V", "S*G*EV----*****",
                        "10001500001200000000",false)));

        List<String> set = _prefs.getStringList(ICON2525_POINT_DROPPER_PREF);
        if (set != null) {
            for (String s : set) {
                String cotType = CotUtils.cotTypeFromMil2525C(s);

                if (cotType.length() > 4)
                    cotType = cotType.substring(4);

                final String rs = CotDescriptions.getHumanName(ctx, s);
                pairs.add(0, new SubTypePair(rs, cotType, s, true));
            }
        }

        SubTypeAdapter adapter = new SubTypeAdapter(ctx, pairs);


        _customNamingView = new CustomNamingView(
                CustomNamingView.DEFAULT);
        LinearLayout _mainView = _customNamingView.getMainView();
        LinearLayout holder = v.findViewById(R.id.customHolder);
        holder.addView(_mainView);

        unknownRb = v
                .findViewById(R.id.enterLocationTypeUnknown);
        neutralRb = v
                .findViewById(R.id.enterLocationTypeNeutral);
        hostileRb = v
                .findViewById(R.id.enterLocationTypeHostile);
        friendlyRb = v
                .findViewById(R.id.enterLocationTypeFriendly);

        //Setting listeners to the 4 main buttons on the top
        unknownRb.setOnClickListener(_typeCheckedChangedListener);
        neutralRb.setOnClickListener(_typeCheckedChangedListener);
        hostileRb.setOnClickListener(_typeCheckedChangedListener);
        friendlyRb.setOnClickListener(_typeCheckedChangedListener);

        _subtypeButton = v
                .findViewById(R.id.enterLocationSubtypeButton);

        View subtype = LayoutInflater.from(ctx).inflate(R.layout.subtype_menu,
                null);

        ListView subtypeListView = subtype.findViewById(R.id.subtypeListView);
        subtypeListView.setAdapter(adapter);
        subtypeListView.setDivider(null);
        subtypeListView.setDividerHeight(0);

        final AlertDialog.Builder builder = getAddTypeFavoriteDialogBuilder(subtype);
        builder.setNegativeButton(R.string.close, null);
        builder.setPositiveButton(R.string.add_type_favorite,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface,
                            int i) {
                        Marker m = new Marker("dummy");
                        m.setType(_prefs.get("lastCoTTypeSet", "a-f"));

                        selector.show(m, new CoTSelector.TypeChangedListener2() {
                            @Override
                            public void onTypeChanged(String type, String milsym) {
                                if (type.length() > 4) {
                                    final String rs = CotDescriptions.getHumanName(ctx, milsym);
                                    SubTypePair subTypePair = new SubTypePair(
                                            rs, type.substring(4),
                                            milsym, true);
                                    List<String> s = _prefs.getStringList(
                                            ICON2525_POINT_DROPPER_PREF);
                                    if (s == null) {
                                        s = new ArrayList<>();
                                    }
                                    if (!s.contains(subTypePair.milsym)) {
                                        s.add(subTypePair.milsym);
                                        _prefs.set(ICON2525_POINT_DROPPER_PREF,
                                                s);
                                        pairs.add(0, subTypePair);
                                        adapter.notifyDataSetChanged();
                                    }
                                }
                                _subtypeButton.callOnClick();
                            }
                        });
                    }
                });
        AlertDialog dialog = builder.create();

        _subtypeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(isSubtypeCompatibleProvider(ATAKUtilities.getDefaultSymbologyProvider())) {
                    adapter.setAffiliation(_currAffiliation);
                    dialog.show();
                    MapView mapView = MapView.getMapView();
                    if (mapView.isPortrait()) {
                        AlertDialogHelper.adjustWidth(dialog, .90);
                    } else {
                        AlertDialogHelper.adjustWidth(dialog, .80);
                    }

                }
                else {
                    Marker m = new Marker("dummy");
                    m.setType(_prefs.get("lastCoTTypeSet", "a-f"));

                    selector.show(m, new CoTSelector.TypeChangedListener2() {
                        @Override
                        public void onTypeChanged(String type, String milsym) {
                            if (type != null && type.length() > 4) {
                                currPair = new SubTypePair(SymbologyProvider.getFullName(milsym),
                                        type.substring(4), milsym, true);
                                if(!isSubtypeCompatibleProvider(defaultProvider)) {
                                    _prefs.set(PROVIDER_2525_TYPE_PREF + "_" + defaultProvider.getName(), type.substring(4));
                                    _prefs.set(PROVIDER_2525_MIL_PREF + "_" + defaultProvider.getName(), milsym);
                                }

                                _subtypeButton
                                        .setText(currPair.readableString);
//                            dialogInterface.dismiss();
                            }
                        }
                    });
                }
            }
        });

        subtypeListView
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView,
                            View view, int i, long l) {
                        currPair = adapter.getItem(i);
                        if (currPair != null) {
                            _subtypeButton.setText(currPair.readableString);
                        }
                        dialog.dismiss();
                    }
                });

        refreshCurrPair();

        return v;
    }

    @NonNull
    private AlertDialog.Builder getAddTypeFavoriteDialogBuilder(View subtype) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(
                getContext());
        builder.setTitle(R.string.point_dropper_text55);
        builder.setView(subtype);
        builder.setNeutralButton(R.string.search,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface,
                            int i) {
                        Marker m = new Marker("dummy");
                        m.setType(_prefs.get("lastCoTTypeSet", "a-f"));

                        selector.show(m, new CoTSelector.TypeChangedListener2() {
                            @Override
                            public void onTypeChanged(String type, String milsym) {
                                if (type != null && type.length() > 4) {
                                    currPair = new SubTypePair(SymbologyProvider.getFullName(milsym),
                                            type.substring(4), milsym, true);
                                    _subtypeButton
                                            .setText(currPair.readableString);
                                    dialogInterface.dismiss();
                                }
                            }
                        });
                    }
                });
        return builder;
    }

    private void refreshCurrPair() {

        if(defaultProvider != null &&
            isSubtypeCompatibleProvider(defaultProvider) == isSubtypeCompatibleProvider(ATAKUtilities.getDefaultSymbologyProvider())) {
            return;
        }

        defaultProvider = ATAKUtilities.getDefaultSymbologyProvider();
        if(isSubtypeCompatibleProvider(ATAKUtilities.getDefaultSymbologyProvider())) {
            currPair = new SubTypePair(getString(R.string.ground), "G", false);
        }
        else {
            String sym = _prefs.get(PROVIDER_2525_MIL_PREF + "_" + defaultProvider.getName(), null);
            String type = _prefs.get(PROVIDER_2525_TYPE_PREF + "_" + defaultProvider.getName(), null);
            if(sym != null && type != null) {
                currPair = new SubTypePair(defaultProvider.getSymbolTable().getSymbol(sym).getName(), type, sym, false);
            }
            else
               currPair = new SubTypePair(getString(R.string.select_a_type), null, false);
        }
        _subtypeButton.setText(currPair.readableString);
    }

    private static class SubTypePair {
        final String readableString;
        final String cotString;
        final boolean user;

        final String milsym;
        final String milsymd;
        final String milsyme;

        SubTypePair(String rs, String cot, String milsymc, String milsymd, boolean user) {
            this.readableString = rs;
            this.cotString = cot;
            this.milsym = milsymc;
            this.milsymd = milsymd;

            if(this.milsymd != null) {
                this.milsyme = MilStd2525.get2525EFrom2525D(this.milsymd);
                if(!this.milsymd.equals(MilStd2525Base.get2525DFrom2525C(milsymc)))
                    Log.e("MILMISMATCH",this.milsymd + " " +  MilStd2525Base.get2525DFrom2525C(milsymc));
            } else {
                this.milsyme = null;
            }

            this.user = user;

        }
        SubTypePair(String rs, String cot, String milsymc, boolean user) {
            this(rs, cot, milsymc, null, user);
        }

        SubTypePair(String rs, String cot, boolean user) {
            this(rs, cot, null, null, user);
        }

        SubTypePair(Context ctx, String cot, boolean user) {
            this(CotDescriptions
                    .getHumanName(ctx, "a-f-" + cot),
                    cot, user);
        }
    }

    private final Button.OnClickListener _typeCheckedChangedListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            //Clear the edit text focus whenever we switch subtypes
            _customNamingView.clearEditTextFocus();
            if (_typeChecked == v) {
                _typeChecked.setSelected(false);
                _onTypeCheckedChanged(_typeChecked, false);
                _typeChecked = null;
            } else {
                if (_typeChecked != null) {
                    _typeChecked.setSelected(false);
                }
                if (v instanceof ImageButton) {
                    _typeChecked = (ImageButton) v;
                    _typeChecked.setSelected(true);
                    _onTypeCheckedChanged(_typeChecked, true);
                }
            }
        }
    };

    private void _onTypeCheckedChanged(ImageButton buttonView,
            boolean isChecked) {
        // on tool begin
        //Clear the edit text focus whenever we switch main types
        _customNamingView.clearEditTextFocus();
        checkedPosition = -1;
        //One of the main buttons is checked
        if (isChecked) {
            int checkedId = buttonView.getId();

            //These all function the same,
            //setType based on what is checked
            //set the text based off of that type
            //Get a finalText by combining type and sub_type
            //Set that text to the screen
            //Set a checked position
            if (checkedId == R.id.enterLocationTypeUnknown) {
                _currType = "a-u";
                _currAffiliation = Affiliation.Unknown;
                checkedPosition = 0;
            } else if (checkedId == R.id.enterLocationTypeNeutral) {
                _currType = "a-n";
                _currAffiliation = Affiliation.Neutral;
                checkedPosition = 1;
            } else if (checkedId == R.id.enterLocationTypeHostile) {
                _currType = "a-h";
                _currAffiliation = Affiliation.Hostile;
                checkedPosition = 2;
            } else if (checkedId == R.id.enterLocationTypeFriendly) {
                _currType = "a-f";
                _currAffiliation = Affiliation.Friend;
                checkedPosition = 3;
            }
        }
        Log.d(TAG, "Checked position is now " + checkedPosition);
        if (checkedPosition == -1) {
            _currType = null;
            _currAffiliation = Affiliation.Unknown;
        } else {
            _customNamingView.clearEditTextFocus();
        }

        if (MetricsApi.shouldRecordMetric()) {
            Bundle b = new Bundle();
            b.putString(MetricsUtils.FIELD_INFO,
                    MetricsUtils.EVENT_STATUS_STARTED);
            b.putString(MetricsUtils.FIELD_MAPITEM_TYPE, _currType);
            MetricsUtils.record(MetricsUtils.CATEGORY_TOOL,
                    MetricsUtils.EVENT_CLICKED,
                    "Icon2525cPalletFragment",
                    "Icon2525cPalletFragment",
                    b);
        }

        if (checkedPosition == -1) {
            Intent myIntent = new Intent();
            myIntent.setAction(ToolManagerBroadcastReceiver.END_TOOL);
            myIntent.putExtra("tool", EnterLocationTool.TOOL_NAME);
            AtakBroadcast.getInstance().sendBroadcast(myIntent);
        } else {
            //if point select tool is already active, do not relaunch b/c it is 
            //"ended" by Tool Mgr in the process
            Tool tool = ToolManagerBroadcastReceiver.getInstance()
                    .getActiveTool();
            if (tool != null
                    && EnterLocationTool.TOOL_NAME
                            .equals(tool.getIdentifier())) {
                //Log.d(TAG, "Skipping BEGIN_TOOL intent");
                return;
            }

            Intent myIntent = new Intent();
            myIntent.setAction(ToolManagerBroadcastReceiver.BEGIN_TOOL);
            myIntent.putExtra("tool", EnterLocationTool.TOOL_NAME);
            myIntent.putExtra("current_type", _currType);
            myIntent.putExtra("checked_position", checkedPosition);
            AtakBroadcast.getInstance().sendBroadcast(myIntent);
        }
    }

    /**
     * Main function that is responsible for actually placing the new point on the map
     * @param p the point
     * @param uid - uid of us
     * @return a newly created marker
     * @throws CreatePointException thrown if nothing is selected and the user tries to
     * create a point
     */
    Marker getPointPlacedIntent(final GeoPointMetaData p, final String uid)
            throws CreatePointException {

        //Set the UID as that will be the same for all points
        PlacePointTool.MarkerCreator mc = new PlacePointTool.MarkerCreator(p)
                .setUid(uid);

        //Make sure a type is selected
        if (_currType == null) {
            throw new CreatePointException(
                    "Select an entry type before entering a location.");
            //If we are of type point
        } else {

            String type = _currType;
            //If we have a sub-type set it
            if (currPair != null) {
                if(currPair.readableString.equals(getString(R.string.select_a_type))) {
                    throw new CreatePointException("Choose a type before entering a location");
                }

                type += "-" + currPair.cotString;
            }
            //Get the iconPath
            String iconsetPath = UserIcon.GetIconsetPath(
                    Icon2525cPallet.COT_MAPPING_2525C, _currType, type);
            _prefs.set("lastCoTTypeSet", type);
            _prefs.set("lastIconsetPath", iconsetPath);
            Affiliation affil;
            String milsym = null;
            if(currPair.milsym != null && type.length() >= 3) {
                char c = type.charAt(2);
                switch(c) {
                    case 'n':
                        affil = Affiliation.Neutral;
                        break;
                    case 'f':
                        affil = Affiliation.Friend;
                        break;
                    case 'h':
                        affil = Affiliation.Hostile;
                        break;
                    case 'u':
                    default:
                        affil = Affiliation.Unknown;
                        break;
                }
                final String[] sidcs = new String[] {
                        currPair.milsym,
                        currPair.milsymd,
                        currPair.milsyme
                };
                final ISymbologyProvider defaultProvider = ATAKUtilities.getDefaultSymbologyProvider();
                for(String sidc : sidcs) {
                    if (SymbologyProvider.getProviderFromSymbol(sidc) == defaultProvider) {
                        milsym = defaultProvider.setAffiliation(sidc, affil);
                        break;
                    }
                }
            }
            //Set the type, icon path
            mc = mc
                    .setType(type)
                    .setIconPath(iconsetPath)
                    .showCotDetails(false).setMetaString("milsym", milsym);

            _customNamingView.setCallsignIfEnabled(mc);

        }
        return mc.placePoint();
    }

    //Function that clears a selection when the user selects an already selected type
    public void clearSelection(boolean bPauseListener) {
        _currType = null;
        _typeChecked = null;

        //TODO hack to avoid _typeCheckedChangedListener sending the END_TOOL intent
        if (bPauseListener) {
            if (unknownRb != null)
                unknownRb.setOnClickListener(null);
            if (neutralRb != null)
                neutralRb.setOnClickListener(null);
            if (hostileRb != null)
                hostileRb.setOnClickListener(null);
            if (friendlyRb != null)
                friendlyRb.setOnClickListener(null);
        }

        if (unknownRb != null)
            unknownRb.setSelected(false);
        if (neutralRb != null)
            neutralRb.setSelected(false);
        if (hostileRb != null)
            hostileRb.setSelected(false);
        if (friendlyRb != null)
            friendlyRb.setSelected(false);

        if (bPauseListener) {
            if (unknownRb != null)
                unknownRb
                        .setOnClickListener(_typeCheckedChangedListener);
            if (neutralRb != null)
                neutralRb
                        .setOnClickListener(_typeCheckedChangedListener);
            if (hostileRb != null)
                hostileRb
                        .setOnClickListener(_typeCheckedChangedListener);
            if (friendlyRb != null)
                friendlyRb
                        .setOnClickListener(_typeCheckedChangedListener);
        }
    }

    private static class SubTypeAdapter extends ArrayAdapter<SubTypePair> {
        final List<SubTypePair> subtypes;
        private Affiliation affiliation = Affiliation.Unknown;

        public SubTypeAdapter(@NonNull Context context,
                @NonNull List<SubTypePair> subtypes) {
            super(context, R.layout.subtype_menu_item, subtypes);
            this.subtypes = subtypes;
        }

        public void setAffiliation(@NonNull Affiliation affiliation) {
            this.affiliation = affiliation;
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView,
                @NonNull ViewGroup parent) {
            SubTypePair subtypePair = getItem(position);
            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.subtype_menu_item,
                        parent, false);
                holder = new ViewHolder();
                holder.icon = convertView.findViewById(R.id.icon);
                holder.name = convertView
                        .findViewById(R.id.subtype_common_name);
                holder.delete = convertView
                        .findViewById(R.id.subtype_delete);
                convertView.setTag(holder);

            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (subtypePair == null) {
                convertView.setVisibility(View.GONE);
                return convertView;
            } else {
                convertView.setVisibility(View.VISIBLE);
            }


            BitmapDrawable bmd = CotDescriptions.getIcon(getContext(),
                    subtypePair.milsym + ".png", affiliation);
            holder.icon.setImageDrawable(bmd);

            holder.name.setText(subtypePair.readableString);
            holder.delete
                    .setVisibility(subtypePair.user ? View.VISIBLE : View.GONE);
            holder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    subtypes.remove(subtypePair);

                    notifyDataSetChanged();
                    AtakPreferences _prefs = AtakPreferences
                            .getInstance(getContext());
                    List<String> s = _prefs
                            .getStringList(ICON2525_POINT_DROPPER_PREF);
                    if (s != null && s.contains(subtypePair.milsym)) {
                        s.remove(subtypePair.milsym);
                        _prefs.set(ICON2525_POINT_DROPPER_PREF, s);
                    }
                }
            });

            return convertView;
        }

    }

    private static class ViewHolder {
        ImageView icon;
        TextView name;
        ImageButton delete;
    }
}
