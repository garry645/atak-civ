
package com.atakmap.android.firstperson;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;

import androidx.core.graphics.ColorUtils;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.navigation.views.NavView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.toolbar.Tool;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.widgets.DrawableWidget;
import com.atakmap.android.widgets.LinearLayoutWidget;
import com.atakmap.android.widgets.RootLayoutWidget;
import com.atakmap.app.R;
import com.atakmap.app.preferences.CustomActionBarFragment;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.MapSceneModel;

import gov.tak.api.commons.graphics.Font;
import gov.tak.api.commons.graphics.TextFormat;
import gov.tak.api.engine.map.IMapRendererEnums;
import gov.tak.api.widgets.ILinearLayoutWidget;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.platform.graphics.PointF;
import gov.tak.platform.ui.MotionEvent;
import gov.tak.platform.widgets.JoystickWidget;
import gov.tak.platform.widgets.TextWidget;

/**
 * Broadcast receiver that can handle First Person events via intent.
 */
final class FirstPersonTool extends Tool implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "FirstPersonTool";

    private final MapView _mapView;
    private final AtakPreferences _prefs;
    private ILinearLayoutWidget _layoutV;
    private ILinearLayoutWidget _fpvWidget;
    private ILinearLayoutWidget _streetViewWidget;

    GeoPoint _at;
    double _gsd;
    double _azimuth;
    double _tilt;

    TouchDriver _touchDriver;
    JoystickDriver _joystickDriver;
    JoystickWidget _leftJoystick;
    JoystickWidget _rightJoystick;

    TextWidget _throttleModeWidget;
    TextWidget _tiltModeWidget;

    public static final String TOOL_NAME = "com.atakmap.android.firstperson.FirstPersonTool";

    final IMapWidget.OnWidgetSizeChangedListener _widgetResizeHandler = new IMapWidget.OnWidgetSizeChangedListener() {
        @Override
        public void onWidgetSizeChanged(IMapWidget widget) {
            updateJoystickLayout();
        }
    };

    FirstPersonTool(MapView mapView) {
        super(mapView, TOOL_NAME);
        _mapView = mapView;
        _prefs = AtakPreferences.getInstance(mapView.getContext());
        ToolManagerBroadcastReceiver.getInstance().registerTool(TOOL_NAME,
                this);
        _prefs.registerListener(this);
        _prefs.remove("fpvStreetViewUid");
        _prefs.remove("fpvStreetViewCallsign");

        _touchDriver = new TouchDriver(_mapView);
    }

    @Override
    protected boolean onToolBegin(Bundle extras) {
        final String p = extras.getString("fromPoint");
        GeoPoint lookFrom = null;
        if (p != null)
            lookFrom = new GeoPoint(GeoPoint.parseGeoPoint(p),
                    GeoPoint.Access.READ_WRITE);
        else
            lookFrom = new GeoPoint(_mapView.getCenterPoint().get(),
                    GeoPoint.Access.READ_WRITE);

        // XXX - workaround for bounds check that `GeoPoint` should not be responsible for
        if (Double.isNaN(lookFrom.getAltitude()))
            lookFrom.set(extras.getDouble("fromAltitude", 0d));

        // capture current state
        MapSceneModel sm = _mapView.getRenderer3().getMapSceneModel(false,
                IMapRendererEnums.DisplayOrigin.UpperLeft);
        _at = sm.mapProjection.inverse(sm.camera.target, null);
        _azimuth = sm.camera.azimuth;
        _tilt = sm.camera.elevation + 90d;
        _gsd = sm.gsd;

        // enter first person mode
        _mapView.getRenderer3().lookFrom(lookFrom, _azimuth, -10,
                IMapRendererEnums.CameraCollision.Ignore, true);

        if (_fpvWidget == null) {
            // look-at widget
            final Resources res = _mapView.getContext().getResources();
            final int colorMaize = _mapView.getResources()
                    .getColor(R.color.maize);
            float size = res.getDimensionPixelSize(R.dimen.button_small);
            float pd = res.getDimensionPixelSize(R.dimen.auto_margin);
            _fpvWidget = new LinearLayoutWidget();
            _fpvWidget.setNinePatchBG(true);
            _fpvWidget.setPadding(pd, pd, pd, pd);
            _fpvWidget.setVisible(false);
            DrawableWidget dr = new DrawableWidget(res.getDrawable(
                    R.drawable.nav_firstperson));
            dr.setSize(size, size);
            dr.setColor(colorMaize);
            dr.addOnClickListener(new IMapWidget.OnClickListener() {
                @Override
                public void onMapWidgetClick(IMapWidget widget,
                        MotionEvent event) {
                    endTool();
                }
            });
            _fpvWidget.addChildWidget(dr);

            _streetViewWidget = new LinearLayoutWidget();
            _streetViewWidget.setNinePatchBG(true);
            _streetViewWidget.setPadding(pd, pd, pd, pd);
            _streetViewWidget.setMargins(pd, 0, 0, 0);
            _streetViewWidget.setVisible(false);
            dr = new DrawableWidget(res.getDrawable(R.drawable.ic_street_view));
            dr.setSize(size, size);
            dr.setColor(Color.WHITE);
            dr.addOnClickListener(new IMapWidget.OnClickListener() {
                @Override
                public void onMapWidgetClick(IMapWidget widget,
                        MotionEvent event) {
                    AtakBroadcast.getInstance().sendBroadcast(
                            new Intent(FirstPersonReceiver.STREET_VIEW));
                }
            });
            _streetViewWidget.addChildWidget(dr);

            RootLayoutWidget root = (RootLayoutWidget) _mapView
                    .getComponentExtra(
                            "rootLayoutWidget");
            LinearLayoutWidget tlLayout = root
                    .getLayout(RootLayoutWidget.TOP_LEFT);
            _layoutV = tlLayout.getOrCreateLayout("TL_H");
            _layoutV.addChildWidget(_fpvWidget);
            _layoutV.addChildWidget(_streetViewWidget);

            _leftJoystick = new JoystickWidget(384);
            _rightJoystick = new JoystickWidget(384);
            _joystickDriver = new JoystickDriver(_mapView.getRenderer3(),
                    _leftJoystick, _rightJoystick);

            final JoystickWidget yawJoystick;
            switch (_joystickDriver.getStick1Mode()) {
                case PitchYaw:
                case ThrottleYaw:
                case CameraPanTilt:
                    yawJoystick = _leftJoystick;
                    break;
                default:
                    yawJoystick = _rightJoystick;
                    break;
            }

            final TextFormat font = new TextFormat(
                    new Font(null, Font.Style.Normal, 24), 0);
            _throttleModeWidget = new TextWidget(
                    (yawJoystick == _leftJoystick) ? "UP/DN" : "DRIVE", font,
                    true);
            _throttleModeWidget.setEnterable(false);
            _tiltModeWidget = new TextWidget("TILT", font, true);
            _tiltModeWidget.setEnterable(false);

            _throttleModeWidget
                    .addOnPressListener(new IMapWidget.OnPressListener() {
                        @Override
                        public void onMapWidgetPress(IMapWidget widget,
                                MotionEvent event) {
                            if (yawJoystick == _leftJoystick)
                                _joystickDriver.setStick1Mode(
                                        JoystickDriver.Mode.ThrottleYaw);
                            else
                                _joystickDriver.setStick2Mode(
                                        JoystickDriver.Mode.PitchYaw);
                            _throttleModeWidget.setColor(colorMaize);
                            _tiltModeWidget.setColor(-1);
                        }
                    });
            _tiltModeWidget
                    .addOnPressListener(new IMapWidget.OnPressListener() {
                        @Override
                        public void onMapWidgetPress(IMapWidget widget,
                                MotionEvent event) {
                            if (yawJoystick == _leftJoystick)
                                _joystickDriver.setStick1Mode(
                                        JoystickDriver.Mode.CameraPanTilt);
                            else
                                _joystickDriver.setStick2Mode(
                                        JoystickDriver.Mode.CameraPanTilt);
                            _throttleModeWidget.setColor(-1);
                            _tiltModeWidget.setColor(colorMaize);
                        }
                    });

            switch ((yawJoystick == _leftJoystick)
                    ? _joystickDriver.getStick1Mode()
                    : _joystickDriver.getStick2Mode()) {

                case ThrottleYaw:
                case PitchYaw:
                    _throttleModeWidget.setColor(colorMaize);
                    _tiltModeWidget.setColor(-1);
                    break;
                case CameraPanTilt:
                    _throttleModeWidget.setColor(-1);
                    _tiltModeWidget.setColor(colorMaize);
                    break;
            }

            _leftJoystick.setVisible(false);
            _rightJoystick.setVisible(false);
            _throttleModeWidget.setVisible(false);
            _tiltModeWidget.setVisible(false);

            root.addChildWidget(_leftJoystick);
            root.addChildWidget(_rightJoystick);
            root.addChildWidget(_throttleModeWidget);
            root.addChildWidget(_tiltModeWidget);
        }

        updateColors();
        updateJoystickLayout();

        _fpvWidget.setVisible(true);
        _streetViewWidget.setVisible(true);
        _leftJoystick.setVisible(true);
        _rightJoystick.setVisible(true);
        _throttleModeWidget.setVisible(true);
        _tiltModeWidget.setVisible(true);

        _touchDriver.start(lookFrom);
        _joystickDriver.start(lookFrom);

        ((RootLayoutWidget) _mapView
                .getComponentExtra(
                        "rootLayoutWidget")).addOnWidgetSizeChangedListener(
                                _widgetResizeHandler);

        MapItem item = _mapView.getMapItem(extras.getString("itemUid"));
        if (item == null) {
            _prefs.remove("fpvStreetViewUid");
            _prefs.remove("fpvStreetViewCallsign");
        } else {
            _prefs.set("fpvStreetViewUid", item.getUID());
            _prefs.set("fpvStreetViewCallsign",
                    ATAKUtilities.getDisplayName(item));
        }

        return super.onToolBegin(extras);
    }

    @Override
    protected void onToolEnd() {
        ((RootLayoutWidget) _mapView
                .getComponentExtra(
                        "rootLayoutWidget")).removeOnWidgetSizeChangedListener(
                                _widgetResizeHandler);

        _touchDriver.stop();
        _joystickDriver.stop();
        if (_fpvWidget != null)
            _fpvWidget.setVisible(false);
        if (_streetViewWidget != null)
            _streetViewWidget.setVisible(false);
        if (_leftJoystick != null)
            _leftJoystick.setVisible(false);
        if (_rightJoystick != null)
            _rightJoystick.setVisible(false);
        if (_throttleModeWidget != null)
            _throttleModeWidget.setVisible(false);
        if (_tiltModeWidget != null)
            _tiltModeWidget.setVisible(false);
        _prefs.remove("fpvStreetViewUid");
        _prefs.remove("fpvStreetViewCallsign");
        _mapView.getRenderer3().lookAt(_at, _gsd, _azimuth, _tilt, true);
        super.onToolEnd();
    }

    @Override
    public void dispose() {
        _layoutV.removeChildWidget(_fpvWidget);
        _layoutV.removeChildWidget(_streetViewWidget);
        _prefs.unregisterListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

        if (key == null)
            return;

        if (key.equals(CustomActionBarFragment.ACTIONBAR_BACKGROUND_COLOR_KEY)
                || key.equals(CustomActionBarFragment.ACTIONBAR_ICON_COLOR_KEY))
            updateColors();
    }

    private void updateColors() {
        final int bgColor = ColorUtils.setAlphaComponent(NavView.getInstance()
                .getUserIconShadowColor(), 128);
        if (_fpvWidget != null)
            _fpvWidget.setBackingColor(bgColor);
        if (_streetViewWidget != null)
            _streetViewWidget.setBackingColor(bgColor);

        // joystick colors
        final int fgColor = NavView.getInstance().getUserIconColor();
        if (_leftJoystick != null)
            _leftJoystick.setColor(fgColor);
        if (_rightJoystick != null)
            _rightJoystick.setColor(fgColor);
    }

    private void updateJoystickLayout() {
        if (_leftJoystick == null ||
                _rightJoystick == null ||
                _throttleModeWidget == null ||
                _tiltModeWidget == null) {

            return;
        }

        RootLayoutWidget root = (RootLayoutWidget) _mapView
                .getComponentExtra(
                        "rootLayoutWidget");

        final float viewWidth = _mapView.getWidth();
        final float viewHeight = _mapView.getHeight();
        final float joystickSize = Math.min(viewWidth / 4f, 384f);
        _leftJoystick.setSize(joystickSize);
        _rightJoystick.setSize(joystickSize);

        // compute the minimum offset from the bottom
        IMapWidget[] bottomLayouts = new IMapWidget[] {
                root.getLayout(RootLayoutWidget.BOTTOM_LEFT),
                root.getLayout(RootLayoutWidget.BOTTOM_EDGE),
                root.getLayout(RootLayoutWidget.BOTTOM_RIGHT),
        };

        float minimumBottomOffset = 0f;
        for (final IMapWidget bottomLayout : bottomLayouts) {
            if (bottomLayout == null)
                continue;
            final PointF position = bottomLayout.getAbsoluteWidgetPosition();
            if (position == null)
                continue;
            minimumBottomOffset = Math.max(
                    _mapView.getHeight()
                            - position.y,
                    minimumBottomOffset);
        }

        float minimumLeftOffset = _fpvWidget.getAbsoluteWidgetPosition().x;

        final float joystickHeight = Math.max(
                _leftJoystick.getHeight(), _rightJoystick.getHeight());
        final float buttonsHeight = Math.max(
                _throttleModeWidget.getHeight(), _tiltModeWidget.getHeight());

        final float padding = 16f;
        float joystickPositionY = Math.min(
                viewHeight - minimumBottomOffset - joystickHeight - padding
                        - buttonsHeight,
                viewHeight / 3f * 2f - joystickHeight / 2f);

        _leftJoystick.setPoint(minimumLeftOffset, joystickPositionY);
        _rightJoystick.setPoint(
                viewWidth - _rightJoystick.getWidth() - minimumLeftOffset,
                joystickPositionY);

        final JoystickWidget yawJoystick;
        switch (_joystickDriver.getStick1Mode()) {
            case PitchYaw:
            case ThrottleYaw:
            case CameraPanTilt:
                yawJoystick = _leftJoystick;
                break;
            default:
                yawJoystick = _rightJoystick;
                break;
        }

        final float minButtonWidth = Math.min(
                _throttleModeWidget.getWidth(), _tiltModeWidget.getWidth());
        final float joystickCenterX = yawJoystick.getWidth() / 2f;
        final float centerXOffset = joystickCenterX - minButtonWidth;
        final float buttonsX = yawJoystick.getPointX() + joystickCenterX;
        final float buttonsY = yawJoystick.getPointY() - buttonsHeight;

        _tiltModeWidget.setPoint(buttonsX - _tiltModeWidget.getWidth() / 2,
                yawJoystick.getPointY() + yawJoystick.getHeight());
        _throttleModeWidget.setPoint(
                buttonsX - _throttleModeWidget.getWidth() / 2,
                yawJoystick.getPointY() - (buttonsHeight * 1.5f));
    }
}
