
package com.atakmap.android.maps.tilesets;

import gov.tak.api.annotation.DeprecatedApi;

/** @deprecated use to {@link com.atakmap.map.layer.raster.controls.TileClientControl} API */
@Deprecated
@DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
public interface OnlineTilesetSupport
{

    public void setOfflineMode(boolean offlineOnly);

    public boolean isOfflineMode();
}
