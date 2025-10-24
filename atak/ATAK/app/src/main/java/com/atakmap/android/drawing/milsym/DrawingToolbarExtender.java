
package com.atakmap.android.drawing.milsym;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.atakmap.android.drawing.DrawingToolsMapComponent;
import com.atakmap.android.drawing.DrawingToolsToolbar;
import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.drawing.mapItems.DrawingEllipse;
import com.atakmap.android.drawing.mapItems.DrawingRectangle;
import com.atakmap.android.drawing.mapItems.DrawingShape;
import com.atakmap.android.drawing.tools.DrawingEllipseCreationTool;
import com.atakmap.android.drawing.tools.DrawingRectangleCreationTool;
import com.atakmap.android.drawing.tools.ShapeCreationTool;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.toolbar.IToolbarExtension2;
import com.atakmap.android.toolbar.Tool;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.toolbar.ToolbarBroadcastReceiver;
import com.atakmap.android.tools.ActionBarView;
import com.atakmap.android.tools.DrawingCircleCreationTool;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.app.R;
import com.atakmap.coremap.log.Log;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.platform.marshal.MarshalManager;

public class DrawingToolbarExtender extends BroadcastReceiver
        implements MapGroup.OnItemListChangedListener {
    private static final String TAG = "DrawingToolbarExtender";

    public static final String SHAPE_CREATED = "com.atakmap.android.milsym.SHAPE_CREATED";

    private ImageButton milsymButton;
    private ActionBarView drawingToolsToolbarView;

    private Method getActiveToolbarMethod;
    private final MapView mapView;
    private final Context context;
    private final MilSymReceiver milSymReceiver;

    private String selectedCode;

    private ISymbologyProvider symbology;


    /** @deprecated use {@link DrawingToolbarExtender#DrawingToolbarExtender(MapView, Context, MilSymReceiver)} */
    @Deprecated
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public DrawingToolbarExtender(MapView mapView, Context context,
                                  ISymbologyProvider symbology, MilSymReceiver receiver) {
        this(mapView, context, receiver);
    }

    public DrawingToolbarExtender(MapView mapView, Context context, MilSymReceiver receiver) {
        this.mapView = mapView;
        this.context = context;
        this.milSymReceiver = receiver;
    }

    IToolbarExtension2 getActiveToolbar() {
        IToolbarExtension2 extension = null;
        ToolbarBroadcastReceiver receiver = ToolbarBroadcastReceiver
                .getInstance();
        try {
            if (getActiveToolbarMethod == null) {
                Class<?> clazz = receiver.getClass();

                for (Method method : clazz.getDeclaredMethods()) {

                    //watch to call 'ToolbarBroadcastReceiver.getActiveToolbar()', but because of obfuscation, call
                    //method that is protected and returns IToolbarExtension.
                    if (method.getReturnType() != IToolbarExtension2.class)
                        continue;

                    if (!Modifier.isProtected(method.getModifiers()))
                        continue;

                    if (method.getParameterTypes().length != 0)
                        continue;

                    //uh oh someone added another method with the same signature
                    if (getActiveToolbarMethod != null) {
                        throw new Exception("method signature mismatch");
                    }

                    getActiveToolbarMethod = method;
                    getActiveToolbarMethod.setAccessible(true);

                    Modifier.isProtected(getActiveToolbarMethod.getModifiers());

                    extension = (IToolbarExtension2) getActiveToolbarMethod
                            .invoke(receiver);
                }
            }
            extension = (IToolbarExtension2) getActiveToolbarMethod
                    .invoke(receiver);
        } catch (Exception e) {
            Log.e(TAG,
                    "could not call ToolbarBroadcastReceiver.getActiveToolbar");
        }
        return extension;
    }

    void ensureDrawingToolbarView() {
        if (drawingToolsToolbarView != null)
            return;

        String active = ToolbarBroadcastReceiver.getInstance().getActive();
        if (active == null
                || !active.equals(DrawingToolsToolbar.TOOLBAR_IDENTIFIER))
            return;

        IToolbarExtension2 extension = getActiveToolbar();

        if (extension == null
                || DrawingToolsToolbar.class != extension.getClass())
            return;

        drawingToolsToolbarView = extension.getToolbarView();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null)
            return;

        switch (action) {
            case ToolbarBroadcastReceiver.UNSET_TOOLBAR:
                milsymButton = null;
                drawingToolsToolbarView = null;
                break;
            case ToolbarBroadcastReceiver.SET_TOOLBAR:
                //intents are not guaranteed to be received in any certain order, ensure that the active toolbar has a chance
                //to be set before adding the button
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onDrawingToolsToolbar();
                    }
                }, 50);
                break;
            case SHAPE_CREATED:
                String uid = intent.getStringExtra("uid");
                MapItem item = mapView.getMapItem(uid);
                if (item != null) {
                    item.setMetaString("milsym", selectedCode);
                    final String name = getBestShapeName();
                    if (name != null) {
                        item.setTitle(name);
                        item.setMetaString("callsign", name);

                        // persist the symbology change
                        item.persist(mapView.getMapEventDispatcher(), null,
                                MilSymManager.class);
                    }
                    selectedCode = null;
                }
                break;
            case ToolManagerBroadcastReceiver.END_TOOL:
                setDefaultMilsymButton();
                if (drawingToolsToolbarView != null) {
                    //fix for an ATAK bug where the toolbar wasn't position correctly after
                    //the milsym pane being closed
                    ToolbarBroadcastReceiver.getInstance().repositionToolbar();
                }
                break;
        }
    }

    void setDefaultMilsymButton() {
        if (milsymButton != null) {
            milsymButton.setImageResource(R.drawable.more_horiz);
        }
    }

    void onDrawingToolsToolbar() {
        ensureDrawingToolbarView();

        symbology = ATAKUtilities.getDefaultSymbologyProvider();

        if (drawingToolsToolbarView == null)
            return;

        int height = drawingToolsToolbarView.getHeight();

        if (milsymButton == null) {
            milsymButton = new ImageButton(context);
            milsymButton.setTag(R.id.name, "milsym");
            setDefaultMilsymButton();
            milsymButton.setBackgroundColor(Color.TRANSPARENT);
            milsymButton.setPadding(1, 1, 1, 1);

            milsymButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    milSymReceiver.showPane("",
                            ISymbolTable.MASK_AREAS_AND_LINES, null,
                            new MilSymFragment.OnSymbolSelectedListener() {
                                @Override
                                public void onSymbolSelected(
                                        ISymbolTable.Symbol symbol) {
                                    Class<?> clazz = MilSymUtils.defaultShape(
                                            symbology, symbol.getCode());

                                    String toolID = null;
                                    Bundle bundle = new Bundle();

                                    if (clazz.equals(DrawingShape.class)) {
                                        toolID = ShapeCreationTool.TOOL_IDENTIFIER;
                                    } else if (clazz
                                            .equals(DrawingCircle.class)) {
                                        toolID = DrawingCircleCreationTool.TOOL_IDENTIFIER;
                                    } else if (clazz
                                            .equals(DrawingEllipse.class)) {
                                        toolID = DrawingEllipseCreationTool.TOOL_IDENTIFIER;
                                    } else if (clazz
                                            .equals(DrawingRectangle.class)) {
                                        toolID = DrawingRectangleCreationTool.TOOL_IDENTIFIER;
                                    }

                                    bundle.putParcelable("callback",
                                            new Intent(SHAPE_CREATED));

                                    if (toolID != null) {
                                        ToolManagerBroadcastReceiver
                                                .getInstance()
                                                .startTool(toolID, bundle);
                                        selectedCode = symbol.getCode();
                                    }
                                    Drawable d = MarshalManager.marshal(
                                            symbol.getPreviewDrawable(-1),
                                            gov.tak.api.commons.graphics.Drawable.class,
                                            Drawable.class);
                                    if (d != null && milsymButton != null)
                                        milsymButton.setImageDrawable(d);
                                }
                            });
                }
            });

            milsymButton.setLayoutParams(
                    new LinearLayout.LayoutParams(height, height));
            ((ViewGroup) drawingToolsToolbarView.getParent())
                    .addView(milsymButton);
        }
    }

    @Override
    public void onItemAdded(MapItem item, MapGroup mapGroup) {
        Tool activeTool = ToolManagerBroadcastReceiver.getInstance()
                .getActiveTool();

        if (mapGroup == DrawingToolsMapComponent.getGroup() &&
                item.hasMetaValue("creating") &&
                activeTool != null &&
                activeTool.getIdentifier()
                        .equals(ShapeCreationTool.TOOL_IDENTIFIER)) {

            if (selectedCode != null) {
                item.setMetaString("milsym", selectedCode);
                final String name = getBestShapeName();
                if (name != null) {
                    item.setTitle(name);
                    item.setMetaString("callsign", name);
                }
            }
        }

    }

    private String getBestShapeName() {
        ISymbolTable.Symbol symbol = symbology.getSymbolTable().getSymbol(selectedCode);
        if (symbol == null) {
            Log.e(TAG, "could not resolve the symbol for: " + selectedCode);
            return null;
        }

        final String name = MilSym.getTranslatedName(symbol);

        int count = 0;
        for (MapItem item : DrawingToolsMapComponent.getGroup().getItems()) {
            if (selectedCode.equals(item.getMetaString("milsym", ""))) {
                count++;
            }
        }

        if (count == 0)
            return name;
        else
            return name + " " + count;
    }

    @Override
    public void onItemRemoved(MapItem mapItem, MapGroup mapGroup) {
        if (mapItem.hasMetaValue("creating"))
            mapItem.removeMetaData("milsym");
    }
}
