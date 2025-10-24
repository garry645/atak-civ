package com.atakmap.map.formats.c3dt;

import java.util.Arrays;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
abstract class Volume
{
    @DontObfuscate
    public static final class Box extends Volume
    {
        public double centerX;
        public double centerY;
        public double centerZ;
        public double[] xDirHalfLen;
        public double[] yDirHalfLen;
        public double[] zDirHalfLen;

        public boolean equals(Object o) {
            if(!(o instanceof Box))
                return false;
            Box other = (Box)o;
            return centerX == other.centerX &&
                    centerY == other.centerY &&
                    centerZ == other.centerZ &&
                    Arrays.equals(xDirHalfLen, other.xDirHalfLen) &&
                    Arrays.equals(yDirHalfLen, other.yDirHalfLen) &&
                    Arrays.equals(zDirHalfLen, other.zDirHalfLen);
        }
    }

    @DontObfuscate
    public static final class Region extends Volume
    {
        public double west;
        public double south;
        public double east;
        public double north;
        public double minimumHeight;
        public double maximumHeight;

        public boolean equals(Object o) {
            if(!(o instanceof Region))
                return false;
            Region other = (Region)o;
            return west == other.west &&
                    south == other.south &&
                    east == other.east &&
                    north == other.north &&
                    minimumHeight == other.minimumHeight &&
                    maximumHeight == other.maximumHeight;
        }
    }

    @DontObfuscate
    public static final class Sphere extends Volume
    {
        public double centerX;
        public double centerY;
        public double centerZ;
        public double radius;

        public boolean equals(Object o) {
            if(!(o instanceof Sphere))
                return false;
            Sphere other = (Sphere) o;
            return centerX == other.centerX &&
                    centerY == other.centerY &&
                    centerZ == other.centerZ &&
                    radius == other.radius;
        }
    }
}
