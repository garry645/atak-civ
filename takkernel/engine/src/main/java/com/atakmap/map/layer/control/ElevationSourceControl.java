package com.atakmap.map.layer.control;

import com.atakmap.map.MapControl;
import com.atakmap.map.elevation.ElevationSource;

public interface ElevationSourceControl extends MapControl
{
    /**
     * Sets the {@link ElevationSource} to be used by the
     * {@link com.atakmap.map.opengl.TerrainRenderService}
     *
     * @param source The {@link ElevationSource} or {@code null} to use the default
     */
    void setElevationSource(ElevationSource source);

    /**
     * Returns the {@link ElevationSource} currently in use by the
     * {@link com.atakmap.map.opengl.TerrainRenderService}.
     *
     * @return  The {@link ElevationSource} currently in use; {@code null} if the default
     */
    ElevationSource getElevationSource();
}
