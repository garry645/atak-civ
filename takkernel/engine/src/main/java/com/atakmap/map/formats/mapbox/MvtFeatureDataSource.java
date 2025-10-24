package com.atakmap.map.formats.mapbox;

import com.atakmap.interop.Pointer;
import com.atakmap.map.elevation.ElevationChunk;
import com.atakmap.map.layer.feature.NativeFeatureDataSource;

/**
 * @deprecated use {@link com.atakmap.map.layer.raster.tilematrix.TileMatrix} API and
 * {@link com.atakmap.map.layer.feature.vectortiles.GLVectorTiles} to render vector tiles
 */
@Deprecated
@gov.tak.api.annotation.DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
public final class MvtFeatureDataSource extends NativeFeatureDataSource
{

    public MvtFeatureDataSource()
    {
        super(create());
    }

    static native Pointer create();
}

