package com.atakmap.map.elevation;

import com.atakmap.map.layer.raster.ImageInfo;
import com.atakmap.spi.ServiceProvider;

/** @deprecated use {@link ElevationChunkSpi} API */
@Deprecated
@gov.tak.api.annotation.DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
public interface ElevationDataSpi extends ServiceProvider<ElevationData, ImageInfo>
{
    public int getPriority();
}
