package com.atakmap.map.layer.raster.tilereader;

import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.math.MathUtils;
import com.atakmap.math.Rectangle;

import java.util.Comparator;

final class ReadRequestComparator implements Comparator<TileReader.ReadRequest>
{
    // NOTE: tail corresponds to higher priority

    private final GeoPoint distanceFrom;
    private final GeoPoint closest;

    ReadRequestComparator(GeoPoint distanceFrom)
    {
        this.distanceFrom = distanceFrom;
        this.closest = GeoPoint.createMutable();
    }

    @Override
    public int compare(TileReader.ReadRequest a, TileReader.ReadRequest b)
    {
        // cost > distance > level > age

        // cost
        if (a.cost < b.cost)
            return 1;
        else if (a.cost > b.cost)
            return -1;

        // level

        // cost is known to be equal here. We will prioritize requests with the lowest
        // resolution, as expressed via subsampling rate. This approach may not prioritize
        // lowest resolution expressed as GSD, but will should work reasonably well as
        // a heuristic to enforce relative resolution within a given dataset. If this
        // method is not observed to be sufficient, an additional property for GSD may
        // be added
        if (a.subsample > b.subsample)
            return 1;
        else if (a.subsample < b.subsample)
            return -1;

        // distance
        final boolean aContains = Rectangle.contains(a.bounds.minX, a.bounds.minY, a.bounds.maxX, a.bounds.maxY, distanceFrom.getLongitude(), distanceFrom.getLatitude());
        final boolean bContains = Rectangle.contains(b.bounds.minX, b.bounds.minY, b.bounds.maxX, b.bounds.maxY, distanceFrom.getLongitude(), distanceFrom.getLatitude());
        if (aContains && !bContains)
            return 1;
        else if (!aContains && bContains)
            return -1;

        closest.set(
                MathUtils.clamp(distanceFrom.getLatitude(), a.bounds.minY, a.bounds.maxY),
                MathUtils.clamp(distanceFrom.getLongitude(), a.bounds.minX, a.bounds.maxX));
        final double aDist = GeoCalculations.distanceTo(this.distanceFrom, closest);
        final boolean aDistNaN = Double.isNaN(aDist);

        closest.set(
                MathUtils.clamp(distanceFrom.getLatitude(), b.bounds.minY, b.bounds.maxY),
                MathUtils.clamp(distanceFrom.getLongitude(), b.bounds.minX, b.bounds.maxX));
        final double bDist = GeoCalculations.distanceTo(this.distanceFrom, closest);
        final boolean bDistNaN = Double.isNaN(bDist);

        if (!aDistNaN && !bDistNaN)
        {
            if (aDist < bDist)
                return 1;
            else if (aDist > bDist)
                return -1;
        }

        // age
        return b.id - a.id;
    }
}
