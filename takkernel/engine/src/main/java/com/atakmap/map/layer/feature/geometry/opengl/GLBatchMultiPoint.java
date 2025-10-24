package com.atakmap.map.layer.feature.geometry.opengl;

import com.atakmap.map.MapRenderer;
import com.atakmap.map.opengl.GLMapSurface;
import com.atakmap.map.opengl.GLMapView;

/** @deprecated to be removed without replacement; use {@link com.atakmap.map.layer.feature.opengl.GLBatchGeometryFeatureDataStoreRenderer#SPI} feature renderer */
@Deprecated
@gov.tak.api.annotation.DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
public class GLBatchMultiPoint extends GLBatchGeometryCollection
{

    public GLBatchMultiPoint(GLMapSurface surface)
    {
        this(surface.getGLMapView());
    }

    public GLBatchMultiPoint(MapRenderer surface)
    {
        super(surface, 11, 1, GLMapView.RENDER_PASS_SPRITES);
    }
}
