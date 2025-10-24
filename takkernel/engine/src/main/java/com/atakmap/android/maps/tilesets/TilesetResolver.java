
package com.atakmap.android.maps.tilesets;

import gov.tak.api.annotation.DeprecatedApi;

/** @deprecated migrate to {@link com.atakmap.map.layer.raster.tilematrix.TileMatrix} API */
@Deprecated
@DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")

public interface TilesetResolver
{
    /**
     * Resolves the URI for a given tile. The tile grid offsets are defined by the native tiling
     * format.
     *
     * @param info
     * @param tileRow
     * @param tileColumn
     * @param level
     * @return
     */
    public String resolve(TilesetInfo info, int tileRow, int tileColumn,
                          int level);
}
