package com.atakmap.map.layer.raster.opengl;

import android.util.Pair;

import com.atakmap.map.MapControl;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.opengl.GLMapRenderable2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import com.atakmap.spi.PriorityServiceProviderRegistry2;

import java.util.Collection;

import gov.tak.api.annotation.DeprecatedApi;

public final class GLMapLayerFactory
{

    private static PriorityServiceProviderRegistry2<GLMapLayer3, Pair<MapRenderer, DatasetDescriptor>, GLMapLayerSpi3> registry = new PriorityServiceProviderRegistry2<GLMapLayer3, Pair<MapRenderer, DatasetDescriptor>, GLMapLayerSpi3>();

    private GLMapLayerFactory()
    {
    }

    /** @deprecated use {@link #create4(MapRenderer, DatasetDescriptor)} */
    @Deprecated
    @DeprecatedApi(since = "5.3", forRemoval = true, removeAt = "5.6")
    public static GLMapLayer3 create3(MapRenderer surface, DatasetDescriptor info)
    {
        final Pair<MapRenderer, DatasetDescriptor> arg = Pair.create(surface, info);
        return registry.create(arg);
    }

    /**
     *
     * @param surface
     * @param info
     * @return
     */
    public static GLMapRenderable2 create4(MapRenderer surface, DatasetDescriptor info)
    {
        final Pair<MapRenderer, DatasetDescriptor> arg = Pair.create(surface, info);
        final GLMapLayer3 impl = registry.create(arg);
        if(impl == null)
            return null;
        do {
            if(!(impl instanceof GLMapRenderable2))
                break;
            if(!(impl instanceof Controls))
                break;
            final Controls controls = (Controls)impl;
            if(controls.getControl(DatasetDescriptor.class) != info)
                break;
            return (GLMapRenderable2) impl;
        } while(true);
        return new Adapter(impl);
    }

    public static void registerSpi(GLMapLayerSpi3 spi)
    {
        registry.register(spi, spi.getPriority());
    }

    public static void unregisterSpi(GLMapLayerSpi3 spi)
    {
        registry.unregister(spi);
    }

    final static class Adapter implements GLMapRenderable2, Controls
    {
        final GLMapLayer3 _impl;
        final GLMapRenderable2 _renderable;

        Adapter(GLMapLayer3 impl)
        {
            _impl = impl;
            _renderable = (_impl instanceof GLMapRenderable2) ? (GLMapRenderable2) _impl : null;
        }

        @Override
        public void draw(GLMapView view, int renderPass) {
            if(_renderable != null)
                _renderable.draw(view, renderPass);
            else if((renderPass&(GLMapView.RENDER_PASS_SURFACE_PREFETCH|GLMapView.RENDER_PASS_SURFACE)) != 0)
                _impl.draw(view);
        }

        @Override
        public void release() {
            _impl.release();
        }

        @Override
        public int getRenderPass() {
            return (_renderable != null) ? _renderable.getRenderPass() : GLMapView.RENDER_PASS_SURFACE;
        }

        @Override
        public <T> T getControl(Class<T> controlClazz) {
            if(controlClazz.equals(DatasetDescriptor.class))
                return (T) _impl.getInfo();
            else if(MapControl.class.isAssignableFrom(controlClazz))
                return (T) _impl.getControl((Class<? extends MapControl>)controlClazz);
            else if(controlClazz.isAssignableFrom(_impl.getClass()))
                return (T) _impl;
            else
                return null;
        }

        @Override
        public void getControls(Collection<Object> controls) {
            controls.add(_impl);
            controls.add(_impl.getInfo());
        }
    }
}
