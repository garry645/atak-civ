package com.atakmap.map.formats.mapbox;

import com.atakmap.map.layer.raster.tilematrix.TileMatrix;
import com.atakmap.map.projection.Ellipsoid;

final class WorldMercator {
    final static double originX = -2.003750834E7;
    final static double originY = 2.0037508E7;
    final static double z0_pixelSize = 156543.034;
    final static double a = Ellipsoid.WGS84.WGS84.semiMajorAxis;         // WGS84 semi-major axis
    final static double b = Ellipsoid.WGS84.WGS84.semiMinorAxis;         // WGS84 semi-minor axis
    final static double e = Math.sqrt(1.0 - ((b*b) / (a*a)));  // ellipsoid eccentricity

    final static TileMatrix.ZoomLevel[] zoomLevels = TileMatrix.Util.createQuadtree(z0(), 23);

    static double proj2lng(double x) {
        return Math.toDegrees(x/a);
    }

    static double proj2lat(double y) {
        final double t = Math.exp(-1.0*y/a);
        double lat = 90.0 - 2.0*Math.toDegrees(Math.atan(t));
        double delta;
        int it = 0;
        do {
            final double sinLat = Math.sin(Math.toRadians(lat));
            double lat1 = 90.0 - 2.0*Math.toDegrees(Math.atan(t*Math.pow((1.0-e*sinLat)/(1.0+e*sinLat), e/2.0)));
            delta = Math.abs(lat1-lat);
            lat = lat1;
        } while(++it < 10 && delta > 0.0000001);
        return lat;
    }

    static double lng2proj(double lng) {
        return a * Math.toRadians(lng);
    }

    static double lat2proj(double lat) {
        final double c = Math.pow((1.0 - e*Math.sin(Math.toRadians(lat))) / (1.0 + e*Math.sin(Math.toRadians((lat)))), e/2.0);
        return a * Math.log(Math.tan(Math.PI/4.0 + Math.toRadians(lat)/2.0) * c);
    }

    static TileMatrix.ZoomLevel z0() {
        TileMatrix.ZoomLevel zoom = new TileMatrix.ZoomLevel();
        zoom.resolution = z0_pixelSize;
        zoom.pixelSizeX = z0_pixelSize;
        zoom.pixelSizeY = z0_pixelSize;
        zoom.tileWidth = 256;
        zoom.tileHeight = 256;
        zoom.level = 0;
        return zoom;
    }

    static double tilePixelXd(int tileZoom, int tileColumn, double longitude) {
        final TileMatrix.ZoomLevel zoom = zoomLevels[tileZoom];
        // world mercator easting
        final double projX = lng2proj(longitude);
        // tile relative easting
        final double tileProjX = projX-(originX + (tileColumn*zoom.pixelSizeX*zoom.tileWidth));
        return tileProjX / zoom.pixelSizeX;
    }

    static double tilePixelYd(int tileZoom, int tileRow, double latitude) {
        final TileMatrix.ZoomLevel zoom = zoomLevels[tileZoom];
        // world mercator northing
        final double projY = lat2proj(latitude);
        // tile relative northing
        final double tileProjY = (originY - (tileRow*zoom.pixelSizeY*zoom.tileHeight)) - projY;
        return tileProjY / zoom.pixelSizeY;
    }

    static double tileLat(int tz, int ty) {
        final TileMatrix.ZoomLevel zoom = zoomLevels[tz];
        final double projY = originY - (ty*zoom.pixelSizeY*zoom.tileHeight);
        return proj2lat(projY);
    }

    public static double tileLng(int tz, int tx) {
        final TileMatrix.ZoomLevel zoom = zoomLevels[tz];
        final double projX = originX + (tx*zoom.pixelSizeX*zoom.tileWidth);
        return proj2lng(projX);
    }
}
