
package gov.tak.platform.layers;

import com.atakmap.map.Globe;
import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.Layer;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.annotation.Nullable;
import gov.tak.api.engine.map.IMapRendererEnums;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.engine.map.MapSceneModel;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.platform.graphics.Rect;

public class MagnifierLayer extends AbstractLayer {

    private final ConcurrentLinkedQueue<OnLocationChangedListener> _onLocationChangedListener = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<OnScaleChangedListener> _onScaleChangedListener = new ConcurrentLinkedQueue<>();
    private final Rect viewport;

    private final Globe globe;
    private double _latitude;
    private double _longitude;
    private double _scale;
    private boolean _scaleIsRelative;
    public MagnifierLayer(String name,
                          List<? extends Layer> layers,
                          @Nullable Rect viewport,
                          @NonNull IGeoPoint location,
                          double mapScale,
                          boolean scaleIsRelative) {
        super(name);

        this.globe = new Globe();
        for (Layer layer : layers)
            this.globe.addLayer(layer);

        this.viewport = viewport;

        // XXX - should never happen, need to trace up to mosaic type layer to
        // find out why we're getting a bad resolution/scale
        if (mapScale <= 0.0d)
            mapScale = scaleIsRelative ? 4.0d : 1.0d / 1926.0d;

        _latitude = location.getLatitude();
        _longitude = location.getLongitude();

        _scale = mapScale;
        _scaleIsRelative = scaleIsRelative;
    }

    public Rect getViewport() {
        return this.viewport;
    }
    public void setLocation(final double latitude, final double longitude) {
        _latitude = latitude;
        _longitude = longitude;
        onLocationChanged(latitude, longitude);
    }

    public List<Layer> getLayers() {
        return this.globe.getLayers();
    }

    public Globe getGlobe() {
        return this.globe;
    }

    public double getLatitude() {
        return _latitude;
    }

    public double getLongitude() {
        return _longitude;
    }

    /**
     * Returns an appropriate map scale for displaying the content in the bubble based on the
     * available layers.
     * 
     * @return the map scale.
     */
    public double getMagnifierScale() {
        return _scale;
    }

    public boolean isMagnifierScaleRelative() {
        return _scaleIsRelative;
    }

    /**
     * Sets the map scale for displaying the content of the bubble based on the available layers
     * @param scale the scale to set
     */
    public void setMagnifierScale(double scale) {
        setMagnifierScale(scale, _scaleIsRelative);
    }

    public void setMagnifierScale(boolean scaleIsRelative) {
        setMagnifierScale(_scale, scaleIsRelative);
    }

    public void setMagnifierScale(double scale, boolean scaleIsRelative) {
        _scale = scale;
        _scaleIsRelative = scaleIsRelative;
        onScaleChanged(scale, scaleIsRelative);
    }
    public interface OnLocationChangedListener {
        void onMagnifierLocationChanged(MagnifierLayer bubble, double latitude, double longitude);
    }

    public void addOnLocationChangedListener(OnLocationChangedListener l) {
        _onLocationChangedListener.add(l);
    }

    public void removeOnLocationChangedListener(OnLocationChangedListener l) {
        _onLocationChangedListener.remove(l);
    }

    protected void onLocationChanged(double latitude, double longitude) {
        for (OnLocationChangedListener l : _onLocationChangedListener) {
            l.onMagnifierLocationChanged(this, latitude, longitude);
        }
    }

    public interface OnScaleChangedListener {
        void onMagnifierScaleChanged(MagnifierLayer bubble, double scale, boolean scaleIsRelative);
    }

    public void addOnScaleChangedListener(OnScaleChangedListener l) {
        _onScaleChangedListener.add(l);
    }

    public void removeOnScaleChangedListener(OnScaleChangedListener l) {
        _onScaleChangedListener.remove(l);
    }

    protected void onScaleChanged(double scale, boolean scaleIsRelative) {
        for (OnScaleChangedListener l : _onScaleChangedListener) {
            l.onMagnifierScaleChanged(this, scale, scaleIsRelative);
        }
    }

    public final static class Binder implements MapRenderer.OnCameraChangedListener
    {
        private AtomicBoolean _invalid = new AtomicBoolean(false);
        private WeakReference<MapRenderer> _subjectRef;
        private WeakReference<MagnifierLayer> _magnifierRef;

        public Binder(MapRenderer camera, MagnifierLayer magnifier)
        {
            _subjectRef = new WeakReference<>(camera);
            _magnifierRef = new WeakReference<>(magnifier);

            camera.addOnCameraChangedListener(this);
            setLocation(magnifier, camera);
        }

        @Override
        public void onCameraChanged(final MapRenderer renderer) {
            final MagnifierLayer magnifier = _magnifierRef.get();
            if(magnifier == null) {
                if(!_invalid.getAndSet(true)) {
                    final Binder self = this;
                    new Thread() {
                        public void run() {
                            renderer.removeOnCameraChangedListener(self);
                        }
                    }.start();
                }
            } else {
                setLocation(magnifier, renderer);
            }
        }

        /**
         * Updates the magnifier location, per the specified camera
         * @param magnifier
         * @param camera
         */
        public static void setLocation(@NonNull MagnifierLayer magnifier, @NonNull MapRenderer camera)
        {
            MapSceneModel scene = camera.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
            GeoPoint focus = GeoPoint.createMutable();
            if(scene.mapProjection.inverse(scene.camera.target, focus)) {
                magnifier.setLocation(focus.getLatitude(), focus.getLongitude());
            }
        }
    }
}
