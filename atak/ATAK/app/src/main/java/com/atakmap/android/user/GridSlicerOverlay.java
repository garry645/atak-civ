
package com.atakmap.android.user;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.atakmap.android.hierarchy.HierarchyListReceiver;
import com.atakmap.android.icons.Icon2525cIconAdapter;
import com.atakmap.android.icons.UserIcon;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.user.icon.Icon2525cPallet;
import com.atakmap.app.R;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.maps.assets.Icon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.tak.api.cot.CotUtils;
import gov.tak.api.util.AttributeSet;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.symbology.SymbologyProvider;

/**
 * The GridSlicerOverlay is a component of the OverlayManager that allows a user to toggle the visibility
 * states of CoT markers on a global scale to reduce clutter and improve performance. Icons can be toggled
 * between the standard MIL2525 icons, a generic minimal icon, or be made invisible. These selections can be
 * applied to all icons of a particular affinity (i.e. friendly, neutral, etc.) or a particular dimension
 * of operation (i.e. air, ground, etc.) by toggling a particular row or column of the grid respectively.
 * Icon display state can be toggled for a specific affinity/dimension individually as well.
 */
class GridSlicerOverlay implements View.OnClickListener,
        MapEventDispatcher.MapEventDispatchListener {

    private static final String TAG = "GridSlicerOverlay";

    private final List<Integer> BUTTON_IDS = Arrays.asList(
            R.id.allButton, R.id.c1Button, R.id.c2Button, R.id.c3Button,
            R.id.c4Button, R.id.c5Button,
            R.id.r1Button, R.id.e11Button, R.id.e12Button, R.id.e13Button,
            R.id.e14Button, R.id.e15Button,
            R.id.r2Button, R.id.e21Button, R.id.e22Button, R.id.e23Button,
            R.id.e24Button, R.id.e25Button,
            R.id.r3Button, R.id.e31Button, R.id.e32Button, R.id.e33Button,
            R.id.e34Button, R.id.e35Button,
            R.id.r4Button, R.id.e41Button, R.id.e42Button, R.id.e43Button,
            R.id.e44Button, R.id.e45Button);

    private final List<Integer> ROW_AND_BUTTON_IDS = Arrays.asList(
            R.id.c1Button, R.id.c2Button, R.id.c3Button, R.id.c4Button,
            R.id.c5Button,
            R.id.r1Button, R.id.r2Button, R.id.r3Button, R.id.r4Button);

    private final Map<Character, Icon> minIcons = new HashMap<>();
    private final Map<String, View> buttonMap = new HashMap<>();
    private final MapView _mapView;
    private final AtakPreferences prefs;

    private final View _header;
    private final View _slicer;
    private boolean showSlicer = false;

    private static GridSlicerOverlay _instance;

    private GridSlicerOverlay() {
        this._mapView = MapView.getMapView();
        prefs = AtakPreferences.getInstance(_mapView.getContext());

        this.minIcons.put('h', createIcon(R.drawable.white_dot, 0xFFFF8080));
        this.minIcons.put('f', createIcon(R.drawable.white_dot, 0xFF80E0FF));
        this.minIcons.put('n', createIcon(R.drawable.white_dot, 0xFFAAFFAA));
        this.minIcons.put('u', createIcon(R.drawable.white_dot, 0xFFFFFF80));
        this._header = LayoutInflater.from(_mapView.getContext())
                .inflate(R.layout.filter_overlay_header, _mapView, false);
        this._slicer = LayoutInflater.from(_mapView.getContext())
                .inflate(R.layout.grid_slicer_layout, _mapView, false);
        ImageButton slicerButton = _header.findViewById(R.id.slicer_button);
        slicerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSlicer = true;
                AtakBroadcast.getInstance()
                        .sendBroadcast(new Intent(
                                HierarchyListReceiver.REFRESH_HIERARCHY));
            }
        });
        this.slicerInit();

        _mapView.getMapEventDispatcher()
                .addMapEventListener(MapEvent.ITEM_ADDED, this);
    }

    public synchronized static GridSlicerOverlay getInstance() {
        if (_instance == null) {
            _instance = new GridSlicerOverlay();
        }
        return _instance;
    }

    /**
    * The header contains the button for activating the GridSlicer.
    *
    * @return Inflated view containing the grid slicer toggle button
    */
    public View getHeaderView() {
        return _header;
    }

    /**
    * Show either the grid slicer (custom view) or the normal hierarchical marker list view.
    *
    * @return The custom grid slicer layout if it is to be shown otherwise null to trigger the default
    */
    public View getCustomLayout() {
        if (showSlicer) {
            return _slicer;
        } else {
            return null;
        }
    }

    /**
    * Associate on click handlers with the various grid slicer buttons.
    */
    private void slicerInit() {
        for (Integer buttonId : BUTTON_IDS) {
            View button = _slicer.findViewById(buttonId);
            String buttonTag = button.getTag().toString();
            if (button instanceof ImageButton) {
                String cotType = "a" + buttonTag.substring(1);
                ((ImageButton) button)
                        .setImageDrawable(get2525Drawable(cotType));

                ViewGroup parentView = (ViewGroup) button.getParent();
                ImageView minImage = (ImageView) parentView.getChildAt(2);
                minImage.setImageURI(Uri.parse(
                        minIcons.get(buttonTag.charAt(2)).getImageUri(0)));
                minImage.setColorFilter(
                        minIcons.get(buttonTag.charAt(2)).getColor(0),
                        PorterDuff.Mode.MULTIPLY);
            }
            button.setOnClickListener(this);
            buttonMap.put(buttonTag.substring(1), button);
        }
        restoreGrid();

        final ImageButton listButton = _slicer.findViewById(R.id.list_button);
        listButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSlicer = false;
                AtakBroadcast.getInstance()
                        .sendBroadcast(new Intent(
                                HierarchyListReceiver.REFRESH_HIERARCHY));
            }
        });
    }

    /**
    * Set the state of a particular image button associated with a CoT affinity/dimension button in
    * the grid slicer table.
    *
    * @param imageButton The image button to update
    * @param state The state to set it to (if not specified toggle to the next state)
    */
    private void setState(ImageButton imageButton, String state) {
        String tag = imageButton.getTag().toString();
        char currentState = tag.charAt(0);
        char nextState = (state == null || state.isEmpty())
                ? getNextState(currentState)
                : state.charAt(0);

        String newTag = nextState + tag.substring(1);
        imageButton.setTag(newTag);

        String cotType = "a-" + newTag.substring(2);
        _mapView.getRootGroup().deepForEachItem(
                new MapGroup.OnItemCallback<MapItem>(MapItem.class) {
                    @Override
                    protected boolean onMapItem(MapItem item) {
                        if (item instanceof Marker) {
                            Marker marker = (Marker) item;
                            if (item.getType() != null
                                    && item.getType().startsWith(cotType)) {
                                // only apply grid slicer behavior to markers using default MIL-2525 icons
                                String iconPath = item.getMetaString(
                                        UserIcon.IconsetPath, null);
                                if (iconPath == null || iconPath.startsWith(
                                        Icon2525cPallet.COT_MAPPING_2525)) {
                                    setMarkerState(marker, nextState);
                                }
                            }
                        }
                        return false;
                    }
                });

        final ViewGroup parentView = (ViewGroup) imageButton.getParent();
        for (int i = 1; i < parentView.getChildCount(); i++) {
            View childView = parentView.getChildAt(i);
            String childTag = childView.getTag().toString();
            switch (nextState) {
                case 'E':
                    if (childTag.equals("disabled") || childTag.equals("min")) {
                        childView.setVisibility(View.INVISIBLE);
                    }
                    break;
                case 'O':
                    if (childTag.equals("disabled") || childTag.equals("min")) {
                        childView.setVisibility(View.VISIBLE);
                    }
                    break;
                case 'D':
                    if (childTag.equals("disabled")) {
                        childView.setVisibility(View.VISIBLE);
                    } else if (childTag.equals("min")) {
                        childView.setVisibility(View.INVISIBLE);
                    }
            }
        }
    }

    /**
    * States transition from enabled (E), min (O), and disabled (D) and then back to enabled.
    *
    * @param state Character specifying the current state
    * @return The next state in to the toggle sequence
    */
    private char getNextState(char state) {
        switch (state) {
            case 'E':
                return 'O';
            case 'O':
                return 'D';
            case 'D':
            default:
                return 'E';
        }
    }

    /**
    * Look up the current display state in the button map.
    *
    * @param type The specific CoT type whose display state is being queried
    * @return The current display state for given CoT type
    */
    public char getCurrentState(String type) {
        if (type.length() > 3)
            type = type.substring(0, 4);
        View view = buttonMap.get(type);
        if (view != null) {
            String tag = view.getTag().toString();
            return tag.charAt(0);
        }
        return 'X';
    }

    /**
    * Get the current states for all CoT types (used to save state in an associated shared preference).
    *
    * @return A string containing the states for all CoT types
    */
    private String getCurrentStates() {
        List<String> tags = new ArrayList<>();
        for (View view : buttonMap.values()) {
            tags.add(view.getTag().toString());
        }
        return String.join(",", tags);
    }

    /**
    * Set the current marker icon based on grid slicer settings.
    *
    * @param marker The marker to be updated
    * @param state The state of the icon: enabled (E), min (O), or disabled (D)
    */
    public void setMarkerState(Marker marker, char state) {
        switch (state) {
            case 'E':
                marker.setState(Marker.ICON_STATE_MASK, Marker.STATE_DEFAULT);
                marker.setVisible(true);
                break;
            case 'O':
                marker.setState(Marker.ICON_STATE_MASK,
                        Icon2525cIconAdapter.MINIMAL_REPRESENTATION);
                marker.setVisible(true);
                break;
            case 'D':
                marker.setVisible(false);
        }
    }

    /**
    * Save the current state of the grid slicer table in a shared preference.
    */
    private void saveGrid() {
        String buttonStates = getCurrentStates();
        prefs.set("button_states", buttonStates);
    }

    /**
    * Restore the state of the grid slicer table from a shared preference.
    */
    private void restoreGrid() {
        String buttonStates = prefs.get("button_states", "");
        if (!buttonStates.isEmpty()) {
            Map<String, String> stateMap = new HashMap<>();
            for (String tag : buttonStates.split(",")) {
                stateMap.put(tag.substring(1), tag);
            }
            for (String subTag : buttonMap.keySet()) {
                final View button = buttonMap.get(subTag);
                if (button instanceof ImageButton) {
                    ImageButton imageButton = (ImageButton) buttonMap
                            .get(subTag);
                    if (imageButton != null)
                        setState(imageButton, stateMap.get(subTag));
                } else if (button instanceof Button) {
                    button.setTag(stateMap.get(subTag));
                }
            }
        }
    }

    /**
    * Create an icon from an android resource.
    *
    * @param icon The ID of the android resource to create an icon from
    * @return An icon created from the provided android resource ID
    */
    private Icon createIcon(final int icon, final int color) {
        Icon.Builder b = new Icon.Builder();
        b.setAnchor(Icon.ANCHOR_CENTER, Icon.ANCHOR_CENTER);
        b.setColor(Icon.STATE_DEFAULT, color);
        b.setSize(40, 40);

        final String uri = "android.resource://"
                + _mapView.getContext().getPackageName()
                + "/" + icon;

        b.setImageUri(Icon.STATE_DEFAULT, uri);
        return b.build();
    }

    /**
    * Create a drawable resource based on the provided CoT type.
    *
    * @param type CoT type
    * @return Drawable resource corresponding to provided CoT type
    */
    private Drawable get2525Drawable(String type) {

        String type2525 = CotUtils.mil2525cFromCotType(type)
                .toUpperCase(LocaleUtil.US);
        gov.tak.api.commons.graphics.Bitmap bmp = SymbologyProvider
                .renderSinglePointIcon(type2525,
                        new AttributeSet(), null);
        Bitmap abmp = MarshalManager.marshal(bmp,
                gov.tak.api.commons.graphics.Bitmap.class, Bitmap.class);
        return new BitmapDrawable(_mapView.getContext().getResources(), abmp);
    }

    /**
    * Click handler for the buttons in the grid slicer table.
    *
    * @param view The button in the grid slicer table that was clicked
    */
    @Override
    public void onClick(View view) {
        String tag = view.getTag().toString();
        char state = tag.charAt(0);
        char position = tag.charAt(2);
        char entity = tag.charAt(4);

        char nextState = getNextState(state);

        // if it's the all, column, or row header buttons change
        // other buttons as appropriate
        if ("arc".indexOf(position) >= 0) {
            // must be all, row, or column button
            for (View buttonView : buttonMap.values()) {
                if (!(buttonView instanceof ImageButton))
                    continue;
                ImageButton childButton = (ImageButton) buttonView;
                String childTag = childButton.getTag().toString();
                String newTag = nextState + childTag.substring(1);
                switch (position) {
                    case 'a':
                        // all
                        setState(childButton, newTag);
                        updateRowAndColumn(newTag);
                        break;
                    case 'r':
                        // row (aka affinity)
                        if (entity == childTag.charAt(2)) {
                            setState(childButton, newTag);
                        }
                        break;
                    case 'c':
                        // column (aka dimension)
                        if (entity == childTag.charAt(4)) {
                            setState(childButton, newTag);
                        }
                }
            }
            String newTag = nextState + tag.substring(1);
            view.setTag(newTag);
        } else {
            setState((ImageButton) view, null);
        }
        saveGrid();
    }

    private void updateRowAndColumn(String nextState) {
        for (int id : ROW_AND_BUTTON_IDS) {
            Button b = _slicer.findViewById(id);
            String tag = b.getTag().toString();
            b.setTag(nextState.charAt(0) + tag.substring(1));
            //Log.d(TAG, "req: " + nextState + " b: " + b.getText() + " " + tag);
        }
    }

    /**
    * Update the icon/display of newly added markers based on the current settings of the grid slicer.
    *
    * @param event Map item added event
    */
    @Override
    public void onMapEvent(MapEvent event) {
        MapItem item = event.getItem();
        if (item == null)
            return;
        String type = item.getType();
        if (type.startsWith("a-")) {
            final boolean vis = item.getVisible();
            final boolean stateSaver = !item.getMetaBoolean("transient", true);
            final boolean archived = item.getMetaBoolean("archive", false);
            String entry = item.getMetaString("entry", "");

            char currentState = getCurrentState(type.substring(1));
            setMarkerState((Marker) item, currentState);

            if (archived && stateSaver) {
                // honor the state saver visibility
                item.setVisible(vis);
            } else if (entry.equals("user")) {
                // user added this map item, show it
                item.setVisible(true);

            }
        }
    }
}
