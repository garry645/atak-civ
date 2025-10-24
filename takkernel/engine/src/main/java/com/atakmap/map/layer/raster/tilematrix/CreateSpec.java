package com.atakmap.map.layer.raster.tilematrix;

import android.graphics.Bitmap;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.controls.TilesMetadataControl;
import com.atakmap.math.PointD;

import java.util.Collection;
import java.util.Map;

final class CreateSpec implements TileMatrix, Controls, TilesMetadataControl {
    final int srid;
    final PointD origin;
    final ZoomLevel[] zoomLevels;
    final String name;
    final Map<String, Object> metadata;
    final Envelope bounds;

    CreateSpec(String name, int srid, PointD origin, ZoomLevel[] zoomLevels, Envelope bounds, Map<String, Object> metadata) {
        this.name = name;
        this.srid = srid;
        this.origin = origin;
        this.zoomLevels = zoomLevels;
        this.bounds = bounds;
        this.metadata = metadata;
    }

    @Override
    public <T> T getControl(Class<T> controlClazz) {
        if(controlClazz.isAssignableFrom(getClass()))
            return (T) this;
        return null;
    }

    @Override
    public void getControls(Collection<Object> controls) {
        controls.add(this);
    }

    @Override
    public Map<String, Object> getMetadata() {
        return this.metadata;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getSRID() {
        return this.srid;
    }

    @Override
    public ZoomLevel[] getZoomLevel() {
        return this.zoomLevels;
    }

    @Override
    public double getOriginX() {
        return this.origin.x;
    }

    @Override
    public double getOriginY() {
        return this.origin.y;
    }

    @Override
    public Bitmap getTile(int zoom, int x, int y, Throwable[] error) {
        return null;
    }

    @Override
    public byte[] getTileData(int zoom, int x, int y, Throwable[] error) {
        return new byte[0];
    }

    @Override
    public Envelope getBounds() {
        return this.bounds;
    }

    @Override
    public void dispose() {}
}
