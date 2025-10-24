package com.atakmap.map.layer.raster.controls;

import java.util.Map;

/**
 * Control providing access to metadata for all tiles accessible from a given container or client
 */
public interface TilesMetadataControl
{
    /**
     * Returns an immutable {@link Map} containing metadata about the tiles.
     *
     * @return  An immutable {@link Map} containing metadata about the tiles.
     */
    Map<String, Object> getMetadata();
}
