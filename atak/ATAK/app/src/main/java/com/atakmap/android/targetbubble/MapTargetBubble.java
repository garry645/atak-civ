
package com.atakmap.android.targetbubble;

import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.Globe;
import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Polygon;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.platform.graphics.Rect;
import gov.tak.platform.layers.MagnifierLayer;
import gov.tak.platform.marshal.AbstractMarshal;
import gov.tak.platform.marshal.MarshalManager;

public class MapTargetBubble extends AbstractLayer {

    static {
        MarshalManager.registerMarshal(new AbstractMarshal(
                MapTargetBubble.class, MagnifierLayer.class) {
            @Override
            protected <T, V> T marshalImpl(V in) {
                return (T) ((MapTargetBubble) in).impl;
            }
        }, MapTargetBubble.class, MagnifierLayer.class);
    }

    private final ConcurrentLinkedQueue<OnLocationChangedListener> _onLocationChangedListener = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<OnScaleChangedListener> _onScaleChangedListener = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<OnCrosshairColorChangedListener> _onCrosshairColorChangedListener = new ConcurrentLinkedQueue<>();

    private CrosshairLayer crosshair;
    private boolean legacyCrosshair = true;
    private final Polygon viewport;
    private final boolean coordExtraction;
    final MagnifierLayer impl;

    public MapTargetBubble(MapView mapView, int x, int y, int width,
            int height, double mapScale) {
        this(mapView,
                legacyLayerSet(mapView),
                x, y, width, height,
                mapScale);
    }

    public MapTargetBubble(MapView mapView, List<? extends Layer> layers,
            int x, int y, int width,
            int height, double mapScale) {

        this(mapView, layers, createRectangle(x, y, width, height), mapScale,
                false);
    }

    public MapTargetBubble(MapView mapView, List<? extends Layer> layers,
            Polygon viewport, double mapScale, boolean coordExtraction) {
        super("Target Bubble");

        this.crosshair = new CrosshairLayer("Target Bubble Crosshair");
        this.crosshair.setColor(0xFF000000);

        this.viewport = viewport;
        Rect implViewport;
        if (this.viewport != null) {
            Envelope bounds = this.viewport.getEnvelope();
            implViewport = new Rect(
                    (int) bounds.minX,
                    (int) bounds.minY,
                    (int) bounds.maxX,
                    (int) bounds.maxY);
        } else {
            implViewport = new Rect(
                    0,
                    0,
                    mapView.getWidth(),
                    mapView.getHeight());
        }

        this.impl = new MagnifierLayer(
                getName(),
                layers,
                implViewport,
                MarshalManager.marshal(mapView.getPoint().get(), GeoPoint.class,
                        IGeoPoint.class),
                mapScale,
                false);

        this.coordExtraction = coordExtraction;

        this.impl.addOnScaleChangedListener(
                new MagnifierLayer.OnScaleChangedListener() {
                    @Override
                    public void onMagnifierScaleChanged(MagnifierLayer bubble,
                            double scale, boolean scaleIsRelative) {
                        onScaleChanged();
                    }
                });
        this.impl.addOnLocationChangedListener(
                new MagnifierLayer.OnLocationChangedListener() {
                    @Override
                    public void onMagnifierLocationChanged(
                            MagnifierLayer bubble, double latitude,
                            double longitude) {
                        onLocationChanged();
                    }
                });
    }

    public boolean isCoordExtractionBubble() {
        return coordExtraction;
    }

    public Polygon getViewport() {
        return this.viewport;
    }

    public void shiftLocation(final double latShift, final double lngShift) {
        setLocation(getLatitude() + latShift, getLongitude() + lngShift);
    }

    public void setLocation(final double latitude, final double longitude) {
        impl.setLocation(latitude, longitude);
    }

    public List<Layer> getLayers() {
        return impl.getLayers();
    }

    public Globe getGlobe() {
        return impl.getGlobe();
    }

    public CrosshairLayer getCrosshair() {
        return crosshair;
    }

    public double getLatitude() {
        return impl.getLatitude();
    }

    public double getLongitude() {
        return impl.getLongitude();
    }

    public int getX() {
        return impl.getViewport().left;
    }

    public int getY() {
        return impl.getViewport().top;
    }

    public int getWidth() {
        return impl.getViewport().width();
    }

    public int getHeight() {
        return impl.getViewport().height();
    }

    public void setLegacyCrosshair() {
        this.legacyCrosshair = false;
    }

    public boolean isLegacyCrosshair() {
        return legacyCrosshair;
    }

    /**
     * Returns an appropriate map scale for displaying the content in the bubble based on the
     * available layers.
     * 
     * @return the map scale.
     */
    public double getMapScale() {
        return impl.getMagnifierScale();
    }

    /**
     * Sets the map scale for displaying the content of the bubble based on the available layers
     * @param scale the scale to set
     */
    public void setMapScale(double scale) {
        impl.setMagnifierScale(scale, false);
    }

    public int getCrosshairColor() {
        return this.crosshair.getCrosshairColor();
    }

    /**
     * Sets the  color of the crosshair.
     * @param color the crosshair changed
     */
    public void setCrosshairColor(int color) {
        if (getCrosshairColor() != color) {
            this.crosshair.setColor(color);

            this.onCrosshairColorChanged();
        }
    }

    public interface OnLocationChangedListener {
        void onMapTargetBubbleLocationChanged(MapTargetBubble bubble);
    }

    public void addOnLocationChangedListener(OnLocationChangedListener l) {
        _onLocationChangedListener.add(l);
    }

    public void removeOnLocationChangedListener(OnLocationChangedListener l) {
        _onLocationChangedListener.remove(l);
    }

    protected void onLocationChanged() {
        for (OnLocationChangedListener l : _onLocationChangedListener) {
            l.onMapTargetBubbleLocationChanged(this);
        }
    }

    public interface OnScaleChangedListener {
        void onMapTargetBubbleScaleChanged(MapTargetBubble bubble);
    }

    public void addOnScaleChangedListener(OnScaleChangedListener l) {
        _onScaleChangedListener.add(l);
    }

    public void removeOnScaleChangedListener(OnScaleChangedListener l) {
        _onScaleChangedListener.remove(l);
    }

    protected void onScaleChanged() {
        for (OnScaleChangedListener l : _onScaleChangedListener) {
            l.onMapTargetBubbleScaleChanged(this);
        }
    }

    public interface OnCrosshairColorChangedListener {
        void onMapTargetBubbleCrosshairColorChanged(MapTargetBubble bubble);
    }

    public void addOnCrosshairColorChangedListener(
            OnCrosshairColorChangedListener l) {
        _onCrosshairColorChangedListener.add(l);
    }

    public void removeOnCrosshairColorChangedListener(
            OnCrosshairColorChangedListener l) {
        _onCrosshairColorChangedListener.remove(l);
    }

    protected void onCrosshairColorChanged() {
        for (OnCrosshairColorChangedListener l : _onCrosshairColorChangedListener) {
            l.onMapTargetBubbleCrosshairColorChanged(this);
        }
    }

    private static List<Layer> legacyLayerSet(MapView mapView) {
        List<Layer> retval = new LinkedList<>();

        retval.addAll(mapView.getLayers(MapView.RenderStack.BASEMAP));
        retval.addAll(mapView.getLayers(MapView.RenderStack.MAP_LAYERS));
        retval.addAll(mapView.getLayers(MapView.RenderStack.RASTER_OVERLAYS));

        return retval;
    }

    private static Polygon createRectangle(int x, int y, int w, int h) {
        LineString rect = new LineString(2);
        rect.addPoint(x, y);
        rect.addPoint(x + w, y);
        rect.addPoint(x + w, y + h);
        rect.addPoint(x, y + h);
        rect.addPoint(x, y);

        Polygon retval = new Polygon(rect.getDimension());
        retval.addRing(rect);
        return retval;
    }
}
