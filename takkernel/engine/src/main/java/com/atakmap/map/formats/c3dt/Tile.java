package com.atakmap.map.formats.c3dt;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.interop.Pointer;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.projection.ECEFProjection;
import com.atakmap.math.MathUtils;
import com.atakmap.math.Matrix;
import com.atakmap.math.PointD;

import gov.tak.api.annotation.DontObfuscate;
import gov.tak.api.engine.map.coords.GeoCalculations;

@DontObfuscate
final class Tile
{
    final Pointer pointer;

    public enum ContentType{
        Unknown,
        Empty,
        External,
        Render
    }

    public enum TileRefine
    {
        Add,
        Replace
    }

    public enum BoundingVolumeType
    {
        Box,
        Region,
        Sphere
    }

    private String tileId;
    private Volume volume;
    private Matrix transform;

    Tile(Pointer ptr)
    {
        this.pointer = ptr;
    }

    public Tile getParent()
    {
        return getParent(pointer.raw);
    }

    public Tile[] getChildren()
    {
        return getChildren(pointer.raw);
    }

    public BoundingVolumeType getBoundingVolumeType()
    {
        int type = getBoundingVolumeType(pointer.raw);
        return BoundingVolumeType.values()[type];
    }

    public Volume getBoundingVolume()
    {
        if (volume == null)
        {
            volume = getBoundingVolume(pointer.raw);
        }
        return volume;
    }

    public Envelope getBoundingBox()
    {
        double[] globeRectangle = getGlobeRectangle(pointer.raw);
        return globeRectangle != null ? new Envelope(
                Math.toDegrees(globeRectangle[0]),
                Math.toDegrees(globeRectangle[1]),
                0,
                Math.toDegrees(globeRectangle[2]),
                Math.toDegrees(globeRectangle[3]),
                0) : null;
    }

    public Matrix getTransform()
    {
        if (transform == null)
        {
            double[] t = new double[16];
            getTransform(pointer.raw, t);
            transform = new Matrix(
                    t[0], t[4], t[8], t[12],
                    t[1], t[5], t[9], t[13],
                    t[2], t[6], t[10], t[14],
                    t[3], t[7], t[11], t[15]
            );
        }
        return transform;
    }

    public ContentType getContentType()
    {
        int contentType = getContentType(pointer.raw);
        return ContentType.values()[contentType];
    }

    public TileExternalContent getExternalContent()
    {
        return getExternalContent(pointer.raw);
    }

    public TileRenderContent getRenderContent()
    {
        return getRenderContent(pointer.raw);
    }

    public String getTileId()
    {
        if (tileId == null)
        {
            tileId = getTileId(pointer.raw);
        }
        return tileId;
    }

    public TileRefine getRefine()
    {
        return TileRefine.values()[getRefine(pointer.raw)];
    }

    public boolean isRenderable()
    {
        return isRenderable(pointer.raw);
    }

    public double getGeometricError()
    {
        return getGeometricError(pointer.raw);
    }

    public static Envelope approximateBounds(Tile tile)
    {
        Volume boundingVolume = tile.getBoundingVolume();
        if (boundingVolume instanceof Volume.Region)
        {
            Volume.Region region = (Volume.Region) boundingVolume;
            return new Envelope(Math.toDegrees(region.west), Math.toDegrees(region.south), region.minimumHeight, Math.toDegrees(region.east), Math.toDegrees(region.north), region.maximumHeight);
        }

        PointD center;
        double radius;
        if (boundingVolume instanceof Volume.Sphere)
        {
            Volume.Sphere sphere = (Volume.Sphere) boundingVolume;

            radius = sphere.radius;

            center = new PointD(sphere.centerX, sphere.centerY, sphere.centerZ);

            GeoPoint centroid = (center.x == 0d && center.y == 0d && center.z == 0d) ?
                    GeoPoint.ZERO_POINT :
                    ECEFProjection.INSTANCE.inverse(center, null);

            final double metersDegLat = GeoCalculations.approximateMetersPerDegreeLatitude(centroid.getLatitude());
            final double metersDegLng = GeoCalculations.approximateMetersPerDegreeLongitude(centroid.getLongitude());

            final double radiusLat = (radius / metersDegLat);
            final double radiusLng = (radius / metersDegLng);

            return new Envelope(
                    Math.max(centroid.getLongitude() - radiusLng, -180d),
                    Math.max(centroid.getLatitude() - radiusLat, -90d),
                    Math.max(centroid.getAltitude() - radius, -900),
                    Math.min(centroid.getLongitude() + radiusLng, 180d),
                    Math.min(centroid.getLatitude() + radiusLat, 90d),
                    Math.min(centroid.getAltitude() + radius, 9000d));
        } else if (boundingVolume instanceof Volume.Box)
        {
            Volume.Box box = (Volume.Box) boundingVolume;
            center = new PointD(box.centerX, box.centerY, box.centerZ);

            GeoPoint centroid = (center.x == 0d && center.y == 0d && center.z == 0d) ?
                    GeoPoint.ZERO_POINT :
                    ECEFProjection.INSTANCE.inverse(center, null);

            // construct the corners of the OBB

            final double aMinX = center.x-box.xDirHalfLen[0]-box.yDirHalfLen[0]-box.zDirHalfLen[0];
            final double aMinY = center.y-box.xDirHalfLen[1]-box.yDirHalfLen[1]-box.zDirHalfLen[1];
            final double aMinZ = center.z-box.xDirHalfLen[2]-box.yDirHalfLen[2]-box.zDirHalfLen[2];
            final double aMaxX = center.x+box.xDirHalfLen[0]+box.yDirHalfLen[0]+box.zDirHalfLen[0];
            final double aMaxY = center.y+box.xDirHalfLen[1]+box.yDirHalfLen[1]+box.zDirHalfLen[1];
            final double aMaxZ = center.z+box.xDirHalfLen[2]+box.yDirHalfLen[2]+box.zDirHalfLen[2];

            final double bMinX = center.x-box.xDirHalfLen[0]+box.yDirHalfLen[0]-box.zDirHalfLen[0];
            final double bMinY = center.y-box.xDirHalfLen[1]+box.yDirHalfLen[1]-box.zDirHalfLen[1];
            final double bMinZ = center.z-box.xDirHalfLen[2]+box.yDirHalfLen[2]-box.zDirHalfLen[2];
            final double bMaxX = center.x+box.xDirHalfLen[0]-box.yDirHalfLen[0]+box.zDirHalfLen[0];
            final double bMaxY = center.y+box.xDirHalfLen[1]-box.yDirHalfLen[1]+box.zDirHalfLen[1];
            final double bMaxZ = center.z+box.xDirHalfLen[2]-box.yDirHalfLen[2]+box.zDirHalfLen[2];

            final double cMinX = center.x-box.xDirHalfLen[0]+box.yDirHalfLen[0]+box.zDirHalfLen[0];
            final double cMinY = center.y-box.xDirHalfLen[1]+box.yDirHalfLen[1]+box.zDirHalfLen[1];
            final double cMinZ = center.z-box.xDirHalfLen[2]+box.yDirHalfLen[2]+box.zDirHalfLen[2];
            final double cMaxX = center.x+box.xDirHalfLen[0]-box.yDirHalfLen[0]-box.zDirHalfLen[0];
            final double cMaxY = center.y+box.xDirHalfLen[1]-box.yDirHalfLen[1]-box.zDirHalfLen[1];
            final double cMaxZ = center.z+box.xDirHalfLen[2]-box.yDirHalfLen[2]-box.zDirHalfLen[2];

            final double dMinX = center.x-box.xDirHalfLen[0]-box.yDirHalfLen[0]+box.zDirHalfLen[0];
            final double dMinY = center.y-box.xDirHalfLen[1]-box.yDirHalfLen[1]+box.zDirHalfLen[1];
            final double dMinZ = center.z-box.xDirHalfLen[2]-box.yDirHalfLen[2]+box.zDirHalfLen[2];
            final double dMaxX = center.x+box.xDirHalfLen[0]+box.yDirHalfLen[0]-box.zDirHalfLen[0];
            final double dMaxY = center.y+box.xDirHalfLen[1]+box.yDirHalfLen[1]-box.zDirHalfLen[1];
            final double dMaxZ = center.z+box.xDirHalfLen[2]+box.yDirHalfLen[2]-box.zDirHalfLen[2];


            // derive radius from OBB corners
            radius = MathUtils.max(
                    MathUtils.distance(aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ) / 2d,
                    MathUtils.distance(bMinX, bMinY, bMinZ, bMaxX, bMaxY, bMaxZ) / 2d,
                    MathUtils.distance(cMinX, cMinY, cMinZ, cMaxX, cMaxY, cMaxZ) / 2d,
                    MathUtils.distance(dMinX, dMinY, dMinZ, dMaxX, dMaxY, dMaxZ) / 2d
            );

            final double metersDegLat = GeoCalculations.approximateMetersPerDegreeLatitude(centroid.getLatitude());
            final double metersDegLng = GeoCalculations.approximateMetersPerDegreeLongitude(centroid.getLongitude());

            final double radiusLat = (radius / metersDegLat);
            final double radiusLng = (radius / metersDegLng);

            // XXX - try to handle case for very large volumes
            if(radiusLat > 90d || radiusLng > 90d) {
                return new Envelope(
                        Math.max(centroid.getLongitude() - radiusLng, -180d),
                        Math.max(centroid.getLatitude() - radiusLat, -90d),
                        Math.max(centroid.getAltitude() - radius, -900),
                        Math.min(centroid.getLongitude() + radiusLng, 180d),
                        Math.min(centroid.getLatitude() + radiusLat, 90d),
                        Math.min(centroid.getAltitude() + radius, 9000d));
            }

            GeoPoint lla = GeoPoint.createMutable();
            PointD xyz = new PointD(0d, 0d, 0d);

            // transform each corner to LLA
            { xyz.x = aMinX; xyz.y = aMinY; xyz.z = aMinZ; }
            ECEFProjection.INSTANCE.inverse(xyz, lla);
            final double aMinLat = lla.getLatitude();
            final double aMinLng = lla.getLongitude();
            final double aMinAlt = lla.getAltitude();

            { xyz.x = aMaxX; xyz.y = aMaxY; xyz.z = aMaxZ; }
            ECEFProjection.INSTANCE.inverse(xyz, lla);
            final double aMaxLat = lla.getLatitude();
            final double aMaxLng = lla.getLongitude();
            final double aMaxAlt = lla.getAltitude();

            { xyz.x = bMinX; xyz.y = bMinY; xyz.z = bMinZ; }
            ECEFProjection.INSTANCE.inverse(xyz, lla);
            final double bMinLat = lla.getLatitude();
            final double bMinLng = lla.getLongitude();
            final double bMinAlt = lla.getAltitude();

            { xyz.x = bMaxX; xyz.y = bMaxY; xyz.z = bMaxZ; }
            ECEFProjection.INSTANCE.inverse(xyz, lla);
            final double bMaxLat = lla.getLatitude();
            final double bMaxLng = lla.getLongitude();
            final double bMaxAlt = lla.getAltitude();

            { xyz.x = cMinX; xyz.y = cMinY; xyz.z = cMinZ; }
            ECEFProjection.INSTANCE.inverse(xyz, lla);
            final double cMinLat = lla.getLatitude();
            final double cMinLng = lla.getLongitude();
            final double cMinAlt = lla.getAltitude();

            { xyz.x = cMaxX; xyz.y = cMaxY; xyz.z = cMaxZ; }
            ECEFProjection.INSTANCE.inverse(xyz, lla);
            final double cMaxLat = lla.getLatitude();
            final double cMaxLng = lla.getLongitude();
            final double cMaxAlt = lla.getAltitude();

            { xyz.x = dMinX; xyz.y = dMinY; xyz.z = dMinZ; }
            ECEFProjection.INSTANCE.inverse(xyz, lla);
            final double dMinLat = lla.getLatitude();
            final double dMinLng = lla.getLongitude();
            final double dMinAlt = lla.getAltitude();

            { xyz.x = dMaxX; xyz.y = dMaxY; xyz.z = dMaxZ; }
            ECEFProjection.INSTANCE.inverse(xyz, lla);
            final double dMaxLat = lla.getLatitude();
            final double dMaxLng = lla.getLongitude();
            final double dMaxAlt = lla.getAltitude();

            return new Envelope(
                    Math.min(MathUtils.min(aMinLng, bMinLng, cMinLng, dMinLng), MathUtils.min(aMaxLng, bMaxLng, cMaxLng, dMaxLng)),
                    Math.min(MathUtils.min(aMinLat, bMinLat, cMinLat, dMinLat), MathUtils.min(aMaxLat, bMaxLat, cMaxLat, dMaxLat)),
                    Math.min(MathUtils.min(aMinAlt, bMinAlt, cMinAlt, dMinAlt), MathUtils.min(aMaxAlt, bMaxAlt, cMaxAlt, dMaxAlt)),
                    Math.max(MathUtils.max(aMinLng, bMinLng, cMinLng, dMinLng), MathUtils.max(aMaxLng, bMaxLng, cMaxLng, dMaxLng)),
                    Math.max(MathUtils.max(aMinLat, bMinLat, cMinLat, dMinLat), MathUtils.max(aMaxLat, bMaxLat, cMaxLat, dMaxLat)),
                    Math.max(MathUtils.max(aMinAlt, bMinAlt, cMinAlt, dMinAlt), MathUtils.max(aMaxAlt, bMaxAlt, cMaxAlt, dMaxAlt))
            );
        } else
        {
            throw new IllegalStateException();
        }
    }

    private static native Tile getParent(long ptr);
    private static native Tile[] getChildren(long ptr);
    private static native int getBoundingVolumeType(long ptr);
    private static native Volume getBoundingVolume(long ptr);
    private static native double[] getGlobeRectangle(long ptr);
    private static native int getTransform(long ptr, double[] result);
    private static native int getContentType(long ptr);
    private static native TileExternalContent getExternalContent(long ptr);
    private static native TileRenderContent getRenderContent(long ptr);
    private static native String getTileId(long ptr);
    private static native int getRefine(long ptr);
    private static native boolean isRenderable(long ptr);
    private static native double getGeometricError(long ptr);
}
