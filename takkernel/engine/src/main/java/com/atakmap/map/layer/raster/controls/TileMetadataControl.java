package com.atakmap.map.layer.raster.controls;

import java.util.Map;

/**
 * Control providing access to per-tile metadata.
 *
 * @see TilesMetadataControl
 */
public interface TileMetadataControl {
    /**
     * Returns an immutable {@link Map} containing metadata about the specified tile.
     *
     * @return  An immutable {@link Map} containing metadata about the tiles or {@code null} if
     *          there is no metadata for the requested tile index
     */
    Map<String, Object> getTileMetadata(int zoom, int x, int y);
}
