
package com.atakmap.android.firstperson;

import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.MapSceneModel;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.math.MathUtils;

import gov.tak.api.engine.map.IMapRendererEnums;
import gov.tak.api.widgets.IJoystickWidget;

class JoystickDriver
        implements IJoystickWidget.OnJoystickMotionListener, Runnable {
    enum Mode {
        ThrottleYaw,
        PitchRoll,
        PitchYaw,
        ThrottleRoll,
        CameraPanTilt,
    }

    /** Scale factor applied to camera pan/tilt motion */
    final static double panTiltVelocityScale = 2d;

    double _minVelocity = 0.5d;
    double _velocity = 32d;

    MapRenderer3 _renderer;
    IJoystickWidget _stick1;
    Mode _stick1Mode;
    IJoystickWidget _stick2;
    Mode _stick2Mode;

    Thread _animationThread;

    double _azVelocity = Double.NaN;
    double _elVelocity = Double.NaN;
    double _xVelocity = Double.NaN;
    /**  */
    double _yVelocity = Double.NaN;
    double _zVelocity = Double.NaN;

    GeoPoint _lookFrom;

    JoystickDriver(MapRenderer3 renderer, IJoystickWidget stick1,
            IJoystickWidget stick2) {
        this(renderer, stick1, Mode.ThrottleRoll, stick2, Mode.PitchYaw);
    }

    JoystickDriver(MapRenderer3 renderer, IJoystickWidget stick1,
            Mode stick1Mode, IJoystickWidget stick2, Mode stick2Mode) {
        _renderer = renderer;
        _stick1 = stick1;
        _stick1Mode = stick1Mode;
        _stick2 = stick2;
        _stick2Mode = stick2Mode;

        _stick1.addOnJoystickMotionListener(this);
        _stick2.addOnJoystickMotionListener(this);

        _lookFrom = GeoPoint.createMutable();
    }

    /**
     * Activates the driver to start processing joystick events to update the view.
     *
     * @param lookFrom  The _look from_ location. The provided point is considered _live_ and will
     *                  be updated when the driver is processing inputs.
     */
    public synchronized void start(GeoPoint lookFrom) {
        _xVelocity = 0d;
        _yVelocity = 0d;
        _zVelocity = 0d;
        _azVelocity = 0d;
        _elVelocity = 0d;

        _lookFrom = lookFrom;

        if (_animationThread == null) {
            _animationThread = new Thread(this, "animationJoystickDriver");
            _animationThread.setPriority(Thread.NORM_PRIORITY);
            _animationThread.start();
        }
    }

    public synchronized void stop() {
        if (_animationThread != null) {
            _animationThread = null;
            notify();
        }
    }

    public Mode getStick1Mode() {
        return _stick1Mode;
    }

    public void setStick1Mode(Mode mode) {
        _stick1Mode = mode;
    }

    public Mode getStick2Mode() {
        return _stick2Mode;
    }

    public void setStick2Mode(Mode mode) {
        _stick2Mode = mode;
    }

    @Override
    public void onJoystickMotion(IJoystickWidget joystick, float direction,
            float distance) {
        final Mode mode = (joystick == _stick1) ? _stick1Mode : _stick2Mode;
        if (mode == Mode.ThrottleYaw) {
            _azVelocity = Math.sin(Math.toRadians(direction)) * distance;
            _zVelocity = Math.cos(Math.toRadians(direction)) * distance;

            // square `z` velocity to dampen
            _zVelocity = Math.signum(_zVelocity) * _zVelocity * _zVelocity;
        } else if (mode == Mode.ThrottleRoll) {
            _xVelocity = Math.sin(Math.toRadians(direction)) * distance;
            _zVelocity = Math.cos(Math.toRadians(direction)) * distance;

            // square `z` velocity to dampen
            _zVelocity = Math.signum(_zVelocity) * _zVelocity * _zVelocity;
        } else if (mode == Mode.PitchRoll) {
            _yVelocity = Math.cos(Math.toRadians(direction)) * distance;
            _xVelocity = Math.sin(Math.toRadians(direction)) * distance;

            // square `x` velocity to dampen
            _xVelocity = Math.signum(_xVelocity) * _xVelocity * _xVelocity;
        } else if (mode == Mode.PitchYaw) {
            _azVelocity = Math.sin(Math.toRadians(direction)) * distance;
            _yVelocity = Math.cos(Math.toRadians(direction)) * distance;
        } else if (mode == Mode.CameraPanTilt) {
            _elVelocity = Math.cos(Math.toRadians(direction)) * distance
                    * panTiltVelocityScale;
            _azVelocity = Math.sin(Math.toRadians(direction)) * distance
                    * panTiltVelocityScale;
        }

        synchronized (this) {
            this.notify();
        }
    }

    @Override
    public void run() {
        while (true) {
            synchronized (this) {
                if (_animationThread != Thread.currentThread())
                    break;
                if (_xVelocity == 0d &&
                        _yVelocity == 0d &&
                        _zVelocity == 0d &&
                        _azVelocity == 0d &&
                        _elVelocity == 0d) {

                    try {
                        this.wait();
                    } catch (InterruptedException ignored) {
                    }
                    continue;
                }
            }

            MapSceneModel sm = _renderer.getMapSceneModel(false,
                    IMapRendererEnums.DisplayOrigin.UpperLeft);
            double cameraTerrainElevation = ElevationManager
                    .getElevation(_lookFrom, null);
            double cameraAgl = _lookFrom.getAltitude();
            if (!Double.isNaN(cameraTerrainElevation))
                cameraAgl -= cameraTerrainElevation;
            cameraAgl = Math.abs(cameraAgl);

            final double maxVelocity = Math
                    .max(MapSceneModel.gsd(cameraAgl, sm.camera.fov, sm.height)
                            * _velocity, _minVelocity);

            sm.camera.azimuth += _azVelocity;
            sm.camera.elevation = MathUtils
                    .clamp(sm.camera.elevation + _elVelocity, -90d, 90d);

            if (_yVelocity != 0d) {
                GeoPoint moved = GeoCalculations.pointAtDistance(_lookFrom,
                        sm.camera.azimuth, maxVelocity * _yVelocity);
                _lookFrom.set(moved.getLatitude(), moved.getLongitude(),
                        _lookFrom.getAltitude());
            }
            if (_xVelocity != 0d || _zVelocity != 0d) {
                GeoPoint moved = GeoCalculations.pointAtDistance(_lookFrom,
                        sm.camera.azimuth + 90d, maxVelocity * _xVelocity);
                _lookFrom.set(
                        moved.getLatitude(),
                        moved.getLongitude(),
                        _lookFrom.getAltitude() + maxVelocity * _zVelocity);
            }

            _renderer.lookFrom(
                    _lookFrom,
                    sm.camera.azimuth,
                    sm.camera.elevation,
                    IMapRendererEnums.CameraCollision.Ignore,
                    false);

            try {
                Thread.sleep(30);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
