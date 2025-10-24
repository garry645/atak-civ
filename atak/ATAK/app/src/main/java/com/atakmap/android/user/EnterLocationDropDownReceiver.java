
package com.atakmap.android.user;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import com.atakmap.android.contact.ContactPresenceDropdown;
import com.atakmap.android.coordoverlay.CoordOverlayMapReceiver;
import com.atakmap.android.cotdetails.CoTInfoBroadcastReceiver;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.gui.HintDialogHelper;
import com.atakmap.android.gui.coordinateentry.CoordinateEntryCapability;
import com.atakmap.android.icons.IconManagerDropdown;
import com.atakmap.android.icons.IconsMapAdapter;
import com.atakmap.android.icons.UserIcon;
import com.atakmap.android.icons.UserIconDatabase;
import com.atakmap.android.icons.UserIconSet;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapTouchController;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.menu.MapMenuReceiver;
import com.atakmap.android.metrics.MetricsApi;
import com.atakmap.android.metrics.MetricsUtils;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.user.icon.Icon2525cPallet;
import com.atakmap.android.user.icon.IconPallet;
import com.atakmap.android.user.icon.IconPallet.CreatePointException;
import com.atakmap.android.user.icon.MissionSpecificPallet;
import com.atakmap.android.user.icon.SpotMapPallet;
import com.atakmap.android.user.icon.SpotMapPalletFragment;
import com.atakmap.android.user.icon.UserIconPallet;
import com.atakmap.android.user.icon.UserIconPalletFragment;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.PointOfInterest;
import com.atakmap.app.R;
import com.atakmap.app.system.ResourceUtil;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 */
public class EnterLocationDropDownReceiver extends DropDownReceiver implements
        OnStateListener, View.OnAttachStateChangeListener {

    private static final String TAG = "EnterLocationDropDownReceiver";

    public static final String START = "com.atakmap.android.user.ENTER_LOCATION_DROP_DOWN";

    private final Context _context;
    private ImageButton _iconManagerButton;
    private ImageButton _recentlyAddedButton;

    private Button _palletTitle;
    private View _layout;

    private static final String HAND_JAM_TITLE = "Go-To";

    private GeoPointMetaData _currCoord;

    private ViewPager _iconPalletPager;
    private IconPalletAdapter _iconPalletAdapter;
    protected IconPallet _selectedIconPallet;
    protected Intent _callbackIntent;

    protected boolean _initialLoad;

    private MapItem _lastPoint;
    private ImageView _lastPointIcon;
    private TextView _lastPointTitle;
    private TextView _lastPointLabel;
    private LinearLayout _lastPoint_ll;

    private final AtakPreferences _prefs;

    protected static EnterLocationDropDownReceiver instance;

    // *********************************** Constructors ***********************************//
    synchronized public static EnterLocationDropDownReceiver getInstance(
            MapView mapView) {
        if (instance == null) {
            instance = new EnterLocationDropDownReceiver(mapView);
        }
        return instance;
    }

    protected EnterLocationDropDownReceiver(MapView mapView) {
        super(mapView);
        _context = mapView.getContext();
        _prefs = AtakPreferences.getInstance(_context);
        _selectedIconPallet = null;
        _initialLoad = true;
        mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.ITEM_REFRESH, _mapListener);
        mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.ITEM_PERSIST, _mapListener);
        mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.ITEM_REMOVED, _mapListener);
        _initView();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null)
            return;

        switch (action) {
            case START:

                // Bring up specific pallet
                Serializable iconPalletClass = intent
                        .getSerializableExtra("iconPallet");
                if (iconPalletClass != null) {
                    for (IconPallet ip : _iconPalletAdapter.pallets) {
                        if (ip.getClass().equals(iconPalletClass)) {
                            _selectedIconPallet = ip;
                            break;
                        }
                    }
                } else if (_initialLoad && !FileSystemUtils.isEmpty(
                        _iconPalletAdapter.pallets)) {
                    // Last pallet selected preference
                    String iconsetUid = _prefs.get("iconset.selected.uid",
                            "");
                    setPallet(iconsetUid);
                }

                if (_selectedIconPallet != null) {
                    _selectedIconPallet.select(intent.getIntExtra("select", 0));
                    _selectedIconPallet.refresh();
                }

                _initialLoad = false;
                this.showDropDown(_layout, THIRD_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                        HALF_HEIGHT, this);
                setRetain(true);

                // Point dropped callback
                _callbackIntent = intent.getParcelableExtra("callback");

                HintDialogHelper
                        .showHint(
                                getMapView().getContext(),
                                context.getString(R.string.point_dropper_hint1),
                                context.getString(R.string.point_dropper_hint2),
                                "iconset", new HintDialogHelper.HintActions() {
                                    @Override
                                    public void preHint() {
                                        // Create animation to fade button into/out of visibility switch
                                        final Animation animation = new AlphaAnimation(
                                                1, 0);
                                        animation.setDuration(500); // duration - half a second
                                        animation
                                                .setInterpolator(
                                                        new LinearInterpolator()); // do not alter animation rate
                                        animation
                                                .setRepeatCount(
                                                        Animation.INFINITE); // Repeat animation infinitely
                                        animation.setRepeatMode(
                                                Animation.REVERSE); // Reverse animation at the end so the
                                        // button will fade back in

                                        _palletTitle.startAnimation(animation);
                                        _iconPalletPager
                                                .startAnimation(animation);
                                    }

                                    @Override
                                    public void postHint() {
                                        _palletTitle.clearAnimation();
                                        _iconPalletPager.clearAnimation();
                                    }
                                });
                break;
            case IconsMapAdapter.ICONSET_ADDED: {
                String uid = intent.getStringExtra("uid");
                if (_iconPalletAdapter == null
                        || FileSystemUtils.isEmpty(uid)) {
                    Log.w(TAG, "Failed to add iconset: " + uid);
                    return;
                }

                Log.d(TAG, "Adding pallet: " + uid);
                UserIconSet iconset = UserIconDatabase.instance(
                        getMapView().getContext())
                        .getIconSet(uid, false, false);
                if (iconset == null || !iconset.isValid()) {
                    Log.w(TAG, "Cannot add iconset: " + uid);
                    return;
                }

                int order = intent.getIntExtra("order", -1);

                addPallet(new UserIconPallet(iconset), order);
                break;
            }
            case IconsMapAdapter.ICONSET_REMOVED: {
                String uid = intent.getStringExtra("uid");
                if (_iconPalletAdapter == null
                        || FileSystemUtils.isEmpty(uid)) {
                    Log.w(TAG, "Failed to remove iconset: " + uid);
                    return;
                }

                Log.d(TAG, "Removing pallet: " + uid);
                _iconPalletAdapter.removePallet(uid);

                if (_selectedIconPallet != null
                        && uid.equals(_selectedIconPallet.getUid())) {
                    _selectedIconPallet = _iconPalletAdapter
                            .getPallet(Icon2525cPallet.COT_MAPPING_2525C);
                    if (_selectedIconPallet != null) {
                        _palletTitle.setText(_selectedIconPallet.getTitle());
                        setPallet(_selectedIconPallet.getUid());
                    }
                }
                break;
            }
            case "com.atakmap.android.user.GO_TO":

                _callbackIntent = null;
                goToDialog();
                break;
        }
    }

    /**
     * Allows for the ability to add a pallet to the existing pallet list.
     * If this is used by a plugin, the plugin must call removePallet when
     * the plugin is unloaded or bad things will happen.
     */
    public synchronized void addPallet(final IconPallet p, int order) {
        _iconPalletAdapter.addPallet(p, order);
    }

    public void addPallet(final IconPallet p) {
        addPallet(p, -1);
    }

    /**
     * Allows for the ability to remove a pallet to the existing pallet list.
     */
    public synchronized IconPallet removePallet(final IconPallet p) {
        return _iconPalletAdapter.removePallet(p.getUid());
    }

    /**
     * Given the uid of an existing pallet remove the pallet from the list.
     * @param uid the uid for a specific pallet.
     */
    public synchronized IconPallet removePallet(final String uid) {
        return _iconPalletAdapter.removePallet(uid);
    }

    private List<IconPallet> loadPallets() {
        //add 2525C, dot map, user icons
        List<IconPallet> pallets = new ArrayList<>();
        IconPallet pallet = new Icon2525cPallet(ResourceUtil
                .getString(getMapView().getContext(), R.string.civ_cot2525C,
                        R.string.cot2525C));
        pallets.add(pallet);
        Log.d(TAG, "Adding Icon2525cPallet");
        if (_selectedIconPallet == null)
            _selectedIconPallet = pallet;

        pallets.add(new MissionSpecificPallet());
        Log.d(TAG, "Adding MissionSpecificPallet");

        pallets.add(new SpotMapPallet());
        Log.d(TAG, "Adding SpotMapPallet");

        List<UserIconSet> iconsets = UserIconDatabase.instance(
                getMapView().getContext()).getIconSets(true, false);
        if (!FileSystemUtils.isEmpty(iconsets)) {
            for (UserIconSet iconset : iconsets) {
                if (iconset != null && iconset.isValid()) {
                    pallets.add(new UserIconPallet(iconset));
                    Log.d(TAG, "Adding pallet: " + iconset);
                } else {
                    Log.w(TAG, "Skipping invalid icon pallet: "
                            + (iconset == null ? "" : iconset.toString()));
                }
            }
        }

        return pallets;
    }

    /**
     * Return intent to create the point
     *
     */
    public MapItem processPoint(final GeoPointMetaData point,
            final MapItem clicked) {
        if (point == null || !point.get().isValid()) {
            Log.w(TAG, "Unable to process invalid point");
            return null;
        }

        if (_selectedIconPallet == null) {
            Log.w(TAG, "Unable to process point with no selected pallet");
            return null;
        }

        // Show radial when last dropped point is clicked
        if (clicked != null && RecentlyAddedDropDownReceiver.instance
                .isRecentlyAdded(clicked)) {
            List<Intent> intents = new ArrayList<>(3);
            intents.add(new Intent(CoordOverlayMapReceiver.SHOW_DETAILS)
                    .putExtra("uid", clicked.getUID()));
            intents.add(new Intent(FocusBroadcastReceiver.FOCUS)
                    .putExtra("uid", clicked.getUID()));
            intents.add(new Intent(MapMenuReceiver.SHOW_MENU)
                    .putExtra("uid", clicked.getUID()));
            AtakBroadcast.getInstance().sendIntents(intents);
            return null;
        }

        MapItem createdPoint = null;
        try {
            createdPoint = _selectedIconPallet.getPointPlacedIntent(point,
                    UUID.randomUUID().toString());
        } catch (CreatePointException e) {
            Log.w(TAG,
                    _selectedIconPallet.toString()
                            + " Failed to create point: "
                            + point.get().toStringRepresentation(),
                    e);
            Toast.makeText(getMapView().getContext(), e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
        if (createdPoint == null) {
            Log.w(TAG, "Unable to process point with empty intent");
            return null;
        }

        if (_callbackIntent != null) {
            _callbackIntent.putExtra("itemUID", createdPoint.getUID());
            AtakBroadcast.getInstance().sendBroadcast(_callbackIntent);
        }

        _lastPoint = createdPoint;
        getMapView().post(new Runnable() {
            @Override
            public void run() {
                refreshLastPoint();
            }
        });

        return createdPoint;
    }

    public MapItem processPoint(final GeoPointMetaData point) {
        return processPoint(point, null);
    }

    public Marker processCASEVAC(final GeoPointMetaData point) {
        if (point == null || !point.get().isValid()) {
            Log.w(TAG, "Unable to process invalid point");
            return null;
        }
        Marker createdPoint = null;
        try {
            //Set the UID as that will be the same for all points
            PlacePointTool.MarkerCreator mc = new PlacePointTool.MarkerCreator(
                    point).setUid(UUID.randomUUID().toString());
            //Set the options associated with a med-evac
            mc = mc
                    .setType("b-r-f-h-c")
                    .setReadiness(false)
                    .setShowMedNineLine(true);
            createdPoint = mc.placePoint();
        } catch (Exception e) {
            Log.w(TAG, "unable to create CASEVAC");
        }

        return createdPoint;
    }

    public void clearSelection(final boolean bPauseListeners) {
        if (_selectedIconPallet == null)
            return;
        _selectedIconPallet.clearSelection(bPauseListeners);
    }

    public void close() {
        this.closeDropDown();
    }

    private void _initView() {
        // Layout the initial view
        LayoutInflater inflater = LayoutInflater
                .from(getMapView().getContext());
        _layout = inflater.inflate(R.layout.enter_location_dropdown_view, null);

        _palletTitle = _layout.findViewById(R.id.enterLocationTitle);
        _palletTitle.setOnClickListener(_palletButtonListener);

        _iconManagerButton = _layout
                .findViewById(R.id.iconManagerSettings);
        _iconManagerButton.setOnClickListener(_iconManagerListener);

        _recentlyAddedButton = _layout
                .findViewById(R.id.iconManagerRecentlyAdded);
        _recentlyAddedButton.setOnClickListener(_recentlyAddedListener);

        //setup last point UI
        _lastPointIcon = _layout.findViewById(R.id.lastPointIcon);
        _lastPointTitle = _layout.findViewById(R.id.lastPointTitle);
        _lastPointLabel = _layout.findViewById(R.id.lastPointLabel);
        _lastPoint_ll = _layout.findViewById(R.id.lastPoint_ll);
        _lastPoint_ll.setOnClickListener(_lastPointZoomListener);

        ImageButton del = _layout.findViewById(R.id.lastPointDelete);
        del.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_lastPoint == null) {
                    Log.w(TAG, "Last point not set");
                    return;
                }

                AlertDialog.Builder b = new AlertDialog.Builder(_context);
                b.setTitle(R.string.confirmation_dialogue);
                b.setMessage(_context.getString(
                        R.string.confirmation_remove_details,
                        _lastPoint.getTitle()));
                b.setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                synchronized (EnterLocationDropDownReceiver.this) {
                                    if (_lastPoint != null) {
                                        _lastPoint.removeFromGroup();

                                        MetricsUtils.record(
                                                MetricsUtils.CATEGORY_MAPITEM,
                                                MetricsUtils.EVENT_MAPITEM_REMOVED,
                                                "EnterLocationDropDownReceiver",
                                                _lastPoint,
                                                MetricsUtils.EVENT_STATUS_SUCCESS);
                                        _lastPoint = null; // it's already been removed, don't try to
                                    }
                                }
                            }
                        });
                b.setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                synchronized (EnterLocationDropDownReceiver.this) {
                                    if (_lastPoint != null) {
                                        MetricsUtils.record(
                                                MetricsUtils.CATEGORY_MAPITEM,
                                                MetricsUtils.EVENT_MAPITEM_REMOVED,
                                                "EnterLocationDropDownReceiver",
                                                _lastPoint,
                                                MetricsUtils.EVENT_STATUS_CANCELLED);
                                    }
                                }
                            }
                        });
                b.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (_lastPoint != null) {
                            MetricsUtils.record(MetricsUtils.CATEGORY_MAPITEM,
                                    MetricsUtils.EVENT_MAPITEM_REMOVED,
                                    "EnterLocationDropDownReceiver",
                                    _lastPoint,
                                    MetricsUtils.EVENT_STATUS_CANCELLED);
                        }
                    }
                });
                b.show();
            }
        });

        ImageButton send = _layout.findViewById(R.id.lastPointSend);
        send.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_lastPoint == null) {
                    Log.w(TAG, "Last point not set");
                    return;
                }

                // Prompt user to include marker attachments
                final String uid = _lastPoint.getUID();
                CoTInfoBroadcastReceiver.promptSendAttachments(_lastPoint,
                        null,
                        null, new Runnable() {
                            @Override
                            public void run() {
                                // Send marker only
                                Intent contactList = new Intent(
                                        ContactPresenceDropdown.SEND_LIST);
                                contactList.putExtra("targetUID", uid);
                                AtakBroadcast.getInstance().sendBroadcast(
                                        contactList);
                            }
                        });
            }
        });

        ImageButton rename = _layout.findViewById(R.id.lastPointRename);

        rename.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (_lastPoint == null) {
                    Log.w(TAG, "Last point not set");
                    return;
                }

                final EditText input = new EditText(getMapView().getContext());
                input.setSingleLine(true);
                input.setText(_lastPoint.getTitle());
                input.selectAll();

                AlertDialog.Builder b = new AlertDialog.Builder(_context);
                b.setTitle(R.string.rename);
                b.setView(input);
                b.setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                if (_lastPoint == null) {
                                    Log.w(TAG, "Last point not set");
                                    return;
                                }

                                String newName = input.getText().toString()
                                        .trim();
                                if (FileSystemUtils.isEmpty(newName)) {
                                    Log.w(TAG, "New name not set");
                                    return;
                                }

                                if (MetricsApi.shouldRecordMetric()) {
                                    final Bundle b = new Bundle();
                                    b.putString("old_name",
                                            _lastPoint.getTitle());
                                    b.putString("new_name", newName);
                                    MetricsUtils.record(
                                            MetricsUtils.CATEGORY_MAPITEM,
                                            MetricsUtils.EVENT_MAPITEM_CHANGED,
                                            "EnterLocationDropDownReceiver",
                                            _lastPoint, b);
                                }

                                _lastPoint.setMetaString("callsign", newName);
                                _lastPoint.setTitle(newName);
                                _lastPointTitle.setText(newName);
                                _lastPoint.refresh(
                                        getMapView().getMapEventDispatcher(),
                                        null, this.getClass());
                                _lastPoint.persist(
                                        getMapView().getMapEventDispatcher(),
                                        null, this.getClass());
                            }
                        });
                b.setNegativeButton(R.string.cancel, null);
                AlertDialog d = b.create();
                Window w = d.getWindow();
                if (w != null)
                    w.setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                d.show();
            }
        });

        _lastPoint = null;

        _iconPalletAdapter = new IconPalletAdapter(
                ((FragmentActivity) getMapView().getContext())
                        .getSupportFragmentManager());
        _iconPalletPager = _layout
                .findViewById(R.id.enterLocationIconPalletPager);
        _iconPalletPager.setAdapter(_iconPalletAdapter);
        _iconPalletPager.setOnPageChangeListener(_iconPalletChangeListener);
        _iconPalletPager.addOnAttachStateChangeListener(this);

        _iconPalletAdapter.setPallets(loadPallets());
        if (_selectedIconPallet != null)
            _palletTitle.setText(_selectedIconPallet.getTitle());
    }

    /**
     * Sets a pallet to be current based on the iconset UID.
     * @param iconsetUid the iconset UID for the pallet to be made current otherwise no pallet is
     *                   changed.
     */
    public synchronized void setPallet(String iconsetUid) {
        int index = -1, count = 0;
        for (IconPallet pallet : _iconPalletAdapter.pallets) {
            if (!FileSystemUtils.isEmpty(pallet.getUid())
                    && pallet.getUid().equals(iconsetUid)) {
                index = count;
                break;
            }

            count++;
        }

        if (index >= 0) {
            //Log.d(TAG, "Setting pallet index: " + index);
            _iconPalletPager.setCurrentItem(index);
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownVisible(boolean v) {
        if (!v) {
            Intent myIntent = new Intent();
            myIntent.setAction(ToolManagerBroadcastReceiver.END_TOOL);
            myIntent.putExtra("tool", EnterLocationTool.TOOL_NAME);
            AtakBroadcast.getInstance().sendBroadcast(myIntent);
        }

    }

    @Override
    public void onViewAttachedToWindow(@NonNull View v) {
        if (_selectedIconPallet != null)
            setPallet(_selectedIconPallet.getUid());
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull View v) {
    }

    @Override
    public void onDropDownClose() {
        Log.d(TAG, "onDropDownClose");
        //pause listeners, since we explicitly close tool below
        clearSelection(true);

        Intent myIntent = new Intent();
        myIntent.setAction(ToolManagerBroadcastReceiver.END_TOOL);
        myIntent.putExtra("tool", EnterLocationTool.TOOL_NAME);
        AtakBroadcast.getInstance().sendBroadcast(myIntent);

        _callbackIntent = null;
    }

    @Override
    public void disposeImpl() {
        if (_iconPalletAdapter != null) {
            _iconPalletAdapter.clear();
            _iconPalletAdapter = null;
        }

        _iconPalletPager.removeOnPageChangeListener(_iconPalletChangeListener);
        _iconPalletPager.removeOnAttachStateChangeListener(this);
        _iconPalletPager = null;
        _layout = null;

        _lastPoint = null;
        if (_mapListener != null) {
            getMapView().getMapEventDispatcher().removeMapEventListener(
                    MapEvent.ITEM_REFRESH, _mapListener);
            getMapView().getMapEventDispatcher().removeMapEventListener(
                    MapEvent.ITEM_PERSIST, _mapListener);
            getMapView().getMapEventDispatcher().removeMapEventListener(
                    MapEvent.ITEM_REMOVED, _mapListener);
        }
    }

    private void goToDialog() {

        CoordinateEntryCapability.getInstance(_context).showDialog(
                _context.getString(R.string.actionbar_goto),
                _prefs.get("goto_tool_use_last_point", true) ? _currCoord
                        : null,
                true,
                getMapView().getPoint(), null, true,
                new CoordinateEntryCapability.ResultCallback() {
                    @Override
                    public void onResultCallback(String pane,
                            GeoPointMetaData point,
                            String suggestedAffiliation) {
                        _currCoord = point;
                        Intent intent = new Intent(
                                "com.atakmap.android.map.ZOOM");
                        intent.putExtra("focusPoint", point.get().toString());
                        intent.putExtra("maintainZoomLevel", true);
                        AtakBroadcast.getInstance().sendBroadcast(intent);

                        final String usagehint = (String) point
                                .getMetaData("address_usage_hint");
                        if (suggestedAffiliation == null && usagehint != null
                                && usagehint.equals("title")) {
                            showAddressWithoutDrop(point);
                        } else {
                            if (suggestedAffiliation != null) {
                                String uid = UUID.randomUUID().toString();

                                if (!suggestedAffiliation.isEmpty()) {
                                    if (suggestedAffiliation.equals("poi")) {
                                        PointOfInterest poi = PointOfInterest
                                                .getInstance();
                                        // Include DTED elevation
                                        poi.setPoint(point);
                                    } else if (suggestedAffiliation
                                            .equals("redx")) {

                                        Bundle b = new Bundle();
                                        b.putParcelable("point", point.get());
                                        // move the redx instead of dropping a point.
                                        ToolManagerBroadcastReceiver
                                                .getInstance().startTool(
                                                        "com.atakmap.android.user.SELECTPOINTBUTTONTOOL",
                                                        b);
                                    } else {

                                        PlacePointTool.MarkerCreator creator = new PlacePointTool.MarkerCreator(
                                                _currCoord)
                                                        .showCotDetails(false)
                                                        .setUid(uid)
                                                        .setType(
                                                                suggestedAffiliation);
                                        Marker m = creator.placePoint();
                                        com.atakmap.android.drawing.details.GenericPointDetailsView
                                                .setAddress(point, m, null);
                                    }
                                }
                            }
                        }
                    }
                });
    }

    private void showAddressWithoutDrop(GeoPointMetaData point) {
        AlertDialog.Builder builder = new AlertDialog.Builder(_context);
        builder.setTitle(R.string.confirm);
        builder.setMessage(R.string.goto_add_address_without_point);
        builder.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface,
                            int i) {
                        String uid = UUID.randomUUID().toString();
                        PlacePointTool.MarkerCreator creator = new PlacePointTool.MarkerCreator(
                                _currCoord)
                                        .showCotDetails(false)
                                        .setUid(uid)
                                        .setType("b-m-p-s-m");
                        Marker m = creator.placePoint();
                        m.setMetaString(UserIcon.IconsetPath,
                                SpotMapPalletFragment.LABEL_ONLY_ICONSETPATH);
                        m.setIconVisibility(Marker.ICON_GONE);
                        com.atakmap.android.drawing.details.GenericPointDetailsView
                                .setAddress(point, m, null);

                    }
                }).setNegativeButton(R.string.no, null);
        builder.show();

    }

    private final OnClickListener _iconManagerListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setRetain(true);
            AtakBroadcast.getInstance().sendBroadcast(new Intent(
                    IconManagerDropdown.DISPLAY_DROPDOWN));
        }
    };

    private final OnClickListener _recentlyAddedListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent myIntent = new Intent();
            myIntent.setAction(ToolManagerBroadcastReceiver.END_TOOL);
            myIntent.putExtra("tool", EnterLocationTool.TOOL_NAME);
            AtakBroadcast.getInstance().sendBroadcast(myIntent);

            setRetain(true);
            AtakBroadcast.getInstance().sendBroadcast(new Intent(
                    RecentlyAddedDropDownReceiver.START));
        }
    };

    private final OnClickListener _palletButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (_iconPalletAdapter == null
                    || _iconPalletAdapter.pallets == null ||
                    _iconPalletAdapter.pallets.isEmpty()) {
                Log.w(TAG, "No icon pallets to choose from");
                return;
            }

            final String[] items = new String[_iconPalletAdapter.pallets
                    .size()];
            for (int i = 0; i < _iconPalletAdapter.pallets.size(); i++) {
                if (_iconPalletAdapter.pallets
                        .get(i) instanceof UserIconPalletFragment)
                    items[i] = "'"
                            + _iconPalletAdapter.pallets.get(i).getTitle()
                            + getMapView().getContext().getString(
                                    R.string.mapping_iconset);
                else
                    items[i] = _iconPalletAdapter.pallets.get(i).getTitle();
            }

            AlertDialog.Builder b = new AlertDialog.Builder(getMapView()
                    .getContext());
            b.setTitle(getMapView().getResources().getString(
                    R.string.point_dropper_pallet_dialog));
            b.setItems(items, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    IconPallet pallet = _iconPalletAdapter.pallets.get(which);
                    if (pallet == null) {
                        Log.w(TAG, "Unable to get pallet UID for index: "
                                + which);
                        return;
                    }

                    //check if already selected, if so no-op 
                    if (_selectedIconPallet != null
                            && !FileSystemUtils.isEmpty(pallet.getUid())) {
                        if (pallet.getUid()
                                .equals(_selectedIconPallet.getUid())) {
                            Log.d(TAG,
                                    "Pallet already active: "
                                            + pallet);
                        } else {
                            setPallet(pallet.getUid());
                        }
                    }

                    boolean displayHint = _prefs.get(
                            "iconset.display.hint2", true);
                    if (displayHint) {
                        _prefs.set("iconset.display.hint2", false);

                        // Create animation to fade button into/out of visibility switch
                        final Animation animation = new AlphaAnimation(1, 0);
                        animation.setDuration(500); // duration - half a second
                        animation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
                        animation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
                        animation.setRepeatMode(Animation.REVERSE); // Reverse animation at the end so the
                        // button will fade back in

                        _iconPalletPager.startAnimation(animation);

                        new AlertDialog.Builder(getMapView().getContext())
                                .setTitle(
                                        getMapView().getResources().getString(
                                                R.string.point_dropper_hint3))
                                .setCancelable(false)
                                .setMessage(
                                        getMapView().getResources().getString(
                                                R.string.point_dropper_hint4))
                                .setPositiveButton(
                                        getMapView().getResources().getString(
                                                R.string.ok),
                                        new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int which) {
                                                dialog.dismiss();

                                                _iconPalletPager
                                                        .clearAnimation();
                                            }
                                        })
                                .show();
                    }
                }
            });
            b.setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (MetricsApi.shouldRecordMetric()) {
                                Bundle b = new Bundle();
                                b.putString("pallet",
                                        _selectedIconPallet.getUid());
                                b.putString(MetricsUtils.FIELD_ELEMENT_NAME,
                                        "pager");
                                b.putString(MetricsUtils.FIELD_INFO,
                                        MetricsUtils.EVENT_STATUS_CANCELLED);
                                MetricsUtils.record(MetricsUtils.CATEGORY_TOOL,
                                        MetricsUtils.EVENT_WIDGET_STATE,
                                        "EnterLocationDropDownReceiver", b);
                            }
                        }
                    });
            AlertDialog d = b.create();
            d.getListView().setScrollbarFadingEnabled(false);
            d.show();
        }
    };

    private final OnPageChangeListener _iconPalletChangeListener = new ViewPager.SimpleOnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            IconPallet pallet = _iconPalletAdapter.pallets.get(position);
            if (pallet == null) {
                Log.w(TAG, "Unable to select pallet position: " + position);
                return;
            }

            IconPallet previous = _selectedIconPallet;
            _selectedIconPallet = pallet;
            _palletTitle.setText(_selectedIconPallet.getTitle());
            _prefs.set("iconset.selected.uid",
                    _selectedIconPallet.getUid());
            //Log.d(TAG, "Setting iconset.selected.uid:" + _selectedIconPallet.getUID());

            //clear selections and end tool
            if (previous != null && previous != _selectedIconPallet)
                previous.clearSelection(true);
            //else {
            //Log.d(TAG, "No previous pallet to clear...");
            //}

            Intent myIntent = new Intent();
            myIntent.setAction(ToolManagerBroadcastReceiver.END_TOOL);
            myIntent.putExtra("tool", EnterLocationTool.TOOL_NAME);
            AtakBroadcast.getInstance().sendBroadcast(myIntent);

            if (MetricsApi.shouldRecordMetric()) {
                Bundle b = new Bundle();
                b.putString("pallet", _selectedIconPallet.getUid());
                b.putString("pallet_name", _selectedIconPallet.getTitle());
                b.putString(MetricsUtils.FIELD_ELEMENT_NAME, "pager");
                b.putString(MetricsUtils.FIELD_INFO,
                        MetricsUtils.EVENT_STATUS_SUCCESS);
                MetricsUtils.record(MetricsUtils.CATEGORY_TOOL,
                        MetricsUtils.EVENT_WIDGET_STATE,
                        "EnterLocationDropDownReceiver", b);
            }
        }
    };

    private void refreshLastPoint() {
        if (_lastPoint == null) {
            Log.w(TAG, "null marker or CASEVAC");
            return;
        }

        _lastPointLabel.setVisibility(View.VISIBLE);
        _lastPoint_ll.setVisibility(View.VISIBLE);
        _lastPointTitle.setText(_lastPoint.getTitle());

        //Log.d(TAG, "setLastPoint: " + marker.getTitle());

        // ICON
        ATAKUtilities.setIcon(_lastPointIcon, _lastPoint);
    }

    private void unsetLastPoint() {

        _lastPoint = null;
        _lastPointTitle.setText("");
        //d(TAG, "unsetLastPoint: " + marker.getTitle());

        _lastPointLabel.setVisibility(View.GONE);
        _lastPoint_ll.setVisibility(View.GONE);
    }

    private final OnClickListener _lastPointZoomListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (_lastPoint == null) {
                Log.w(TAG, "Cannot zoom to unset last point");
                return;
            }

            MapTouchController.goTo(_lastPoint, false);
        }
    };

    private final MapEventDispatcher.MapEventDispatchListener _mapListener = new MapEventDispatcher.MapEventDispatchListener() {
        @Override
        public void onMapEvent(MapEvent event) {
            if (event == null || event.getItem() == null)
                return;

            final String type = event.getType();
            MapItem item = event.getItem();

            if (_lastPoint == null || !item.getUID().equals(
                    _lastPoint.getUID()))
                return;

            getMapView().post(new Runnable() {
                @Override
                public void run() {
                    if (type.equals(MapEvent.ITEM_REFRESH)
                            || type.equals(MapEvent.ITEM_PERSIST))
                        refreshLastPoint();
                    else if (type.equals(MapEvent.ITEM_REMOVED))
                        unsetLastPoint();
                }
            });
        }
    };

    private static class IconPalletAdapter extends FragmentPagerAdapter {
        private static final String TAG = "IconPalletAdapter";
        List<IconPallet> pallets;

        IconPalletAdapter(FragmentManager fm) {
            super(fm);
        }

        synchronized void setPallets(List<IconPallet> p) {
            pallets = p;
            notifyDataSetChanged();
        }

        void clear() {
            setPallets(null);
        }

        synchronized IconPallet getPallet(String uid) {
            if (FileSystemUtils.isEmpty(pallets)
                    || FileSystemUtils.isEmpty(uid))
                return null;

            for (IconPallet p : pallets) {
                if (p != null && uid.equals(p.getUid()))
                    return p;
            }

            //Log.d(TAG, "Unable to get pallet: " + uid);
            return null;
        }

        synchronized IconPallet removePallet(String uid) {
            if (FileSystemUtils.isEmpty(pallets)
                    || FileSystemUtils.isEmpty(uid)) {
                Log.w(TAG, "Unable to remove pallet");
                return null;
            }

            //find pallet to remove
            int pos = 0;
            IconPallet toRemove = null;
            for (IconPallet p : pallets) {
                if (p != null && uid.equals(p.getUid())) {
                    toRemove = p;
                    break;
                }

                pos++;
            }

            //remove fragment for specified pallet
            if (toRemove != null) {
                Log.d(TAG, "Removing pallet: " + toRemove
                        + " at pos " + pos);
                FragmentManager fm = toRemove.getFragment()
                        .getFragmentManager();
                if (fm == null && !pallets.isEmpty()) {
                    Log.w(TAG, "No Fragment Manager available for " + uid
                            + " checking first fragment");
                    fm = pallets.get(0).getFragment().getFragmentManager();
                }

                if (fm == null) {
                    Log.w(TAG,
                            "no fragment manager, exists - just remove the pallet at position: "
                                    + pos);
                    if (pallets.remove(toRemove))
                        return toRemove;
                    else
                        return null;

                }

                fm.beginTransaction().remove(toRemove.getFragment()).commit();

                //now remove all trailing fragments as they must be re-added
                //at proper index/tag
                for (int i = 0; i < pallets.size(); i++) {
                    if (i > pos) {
                        IconPallet p = pallets.get(i);
                        if (p == null) {
                            Log.w(TAG, "Unable to remove pallet at pos: " + i);
                            continue;
                        }

                        Log.d(TAG, "Removing pallet: " + p
                                + " at pos: " + i);
                        fm.beginTransaction().remove(p.getFragment()).commit();
                    }
                }

                //we only remove that one pallet from our list, others' fragments will 
                //get re-added at proper position
                pallets.remove(toRemove);
                notifyDataSetChanged();
                return toRemove;
            } else {
                Log.w(TAG, "Unable to remove pallet: " + uid);
                return null;
            }
        }

        synchronized void addPallet(IconPallet pallet, int order) {
            IconPallet existing = getPallet(pallet.getUid());
            if (existing != null) {
                Log.w(TAG, "Pallet already exists: " + pallet);
                return;
            }

            if (order >= 0)
                pallets.add(Math.min(order, pallets.size()), pallet);
            else
                pallets.add(pallet);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return (FileSystemUtils.isEmpty(pallets) ? 0 : pallets.size());
        }

        @Override
        public Fragment getItem(int position) {
            if (FileSystemUtils.isEmpty(pallets) || position > pallets.size())
                return null;

            return pallets.get(position).getFragment();
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            //return position none so pager will create (re)added fragments
            return PagerAdapter.POSITION_NONE;
        }
    }
}
