package com.atakmap.map.layer.raster.tilematrix;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.map.layer.raster.tilematrix.TileMatrix;
import com.atakmap.map.projection.Projection;
import com.atakmap.map.projection.ProjectionFactory;
import com.atakmap.math.MathUtils;
import com.atakmap.math.PointD;

public final class TileGrid {

    public final static TileGrid WGS84 = new TileGrid(
            "WGS84",
            4326,
            new PointD(-180d, 90d),
            new TileMatrix.ZoomLevel.Builder()
                    .setResolution(OSMUtils.mapnikTileResolution(1))
                    .setTileSize(256)
                    .setPixelSize(180d / 256d)
                    .build()
    );

    public final static TileGrid WGS84_3D = new TileGrid(
            "WGS84 3D",
            4979,
            new PointD(-180d, 90d),
            new TileMatrix.ZoomLevel.Builder()
                    .setResolution(OSMUtils.mapnikTileResolution(1))
                    .setTileSize(256)
                    .setPixelSize(180d / 256d)
                    .build()
    );

    public final static TileGrid WebMercator = new TileGrid(
            "WebMercator",
            3857,
            new PointD(-2.003750834279e7, 2.003750834279e7),
            new TileMatrix.ZoomLevel.Builder()
                    .setResolution(OSMUtils.mapnikTileResolution(0))
                    .setTileSize(256)
                    .setPixelSize(OSMUtils.mapnikTileResolution(0))
                    .build()
    );

    public final static TileGrid WorldMercator = new TileGrid(
            "WorldMercator",
            3395,
            new PointD(-2.003750834E7, 2.0037508E7),
            new TileMatrix.ZoomLevel.Builder()
                    .setResolution(156543.034)
                    .setTileSize(256)
                    .setPixelSize(156543.034, 156543.034)
                    .build()
    );

    private final String name;
    public final int srid;
    public final PointD origin;
    public final TileMatrix.ZoomLevel[] zoomLevels;
    public final Envelope bounds_wgs84;

    private TileGrid(String name, int srid, PointD origin, TileMatrix.ZoomLevel zoom0) {
        this.name = name;
        this.srid = srid;
        this.origin = origin;
        this.zoomLevels = TileMatrix.Util.createQuadtree(zoom0, 24);
        // XXX - better impl
        switch(this.srid) {
            case 4326 :
            case 4979 :
                this.bounds_wgs84 = new Envelope(-180d, -90d, 0d, 180d, 90d, 0d);
                break;
            case 3395:
            case 3857:
            {
                final Projection proj = ProjectionFactory.getProjection(this.srid);
                final GeoPoint ul = proj.inverse(new PointD(this.origin.x, this.origin.y), null);
                final GeoPoint ur = proj.inverse(new PointD(-this.origin.x, this.origin.y), null);
                final GeoPoint lr = proj.inverse(new PointD(-this.origin.x, -this.origin.y), null);
                final GeoPoint ll = proj.inverse(new PointD(this.origin.x, -this.origin.y), null);
                this.bounds_wgs84 = new Envelope(
                        Math.max(-180d, MathUtils.min(ul.getLongitude(), ur.getLongitude(), lr.getLongitude(), ll.getLongitude())),
                        Math.max(-90d, MathUtils.min(ul.getLatitude(), ur.getLatitude(), lr.getLatitude(), ll.getLatitude())),
                        0d,
                        Math.min(180d, MathUtils.max(ul.getLongitude(), ur.getLongitude(), lr.getLongitude(), ll.getLongitude())),
                        Math.min(90d, MathUtils.max(ul.getLatitude(), ur.getLatitude(), lr.getLatitude(), ll.getLatitude())),
                        0d);
                break;
            }
            default :
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static int findZoomLevel(TileMatrix.ZoomLevel[] zoomLevels, double gsd, int round) {
        if(gsd >= zoomLevels[0].resolution)
            return zoomLevels[0].level;
        else if(gsd <= zoomLevels[zoomLevels.length-1].resolution)
            return zoomLevels[zoomLevels.length-1].level;

        for(int i = 1; i < zoomLevels.length; i++) {
            if(gsd > zoomLevels[i].resolution) {
                final int lower = i-1;
                final int upper = i;
                if(round < 0)
                    return lower;
                else if(round > 0)
                    return upper;
                else
                    return (gsd / zoomLevels[i].resolution) > 1.5 ? lower : upper;
            }
        }

        return zoomLevels[zoomLevels.length-1].level;
    }
}

