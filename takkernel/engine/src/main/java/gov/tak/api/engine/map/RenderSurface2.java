package gov.tak.api.engine.map;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
public interface RenderSurface2 extends RenderSurface
{
    /**
     * @return the width of the surface in native pixels.
     */
    int getSurfaceWidth();

    /**
     * @return the height of the surface in native pixels.
     */
    int getSurfaceHeight();
}
