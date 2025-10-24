package com.atakmap.map.layer.model;

import android.util.Pair;

import com.atakmap.interop.Interop;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.Layer2;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import com.atakmap.map.layer.opengl.GLLayerSpi2;

import java.lang.ref.WeakReference;
import java.util.IdentityHashMap;
import java.util.Map;

import gov.tak.api.annotation.Nullable;

public final class SceneLayer implements Layer2
{
    final static Interop<Layer> Layer_interop = Interop.findInterop(Layer.class);

    static
    {
        GLLayerFactory.register(new GLLayerSpi2()
        {
            @Override
            public int getPriority()
            {
                return 1;
            }

            @Override
            public GLLayer2 create(Pair<MapRenderer, Layer> object)
            {
                if (!(object.second instanceof SceneLayer))
                    return null;
                return GLLayerFactory.create3(object.first, ((SceneLayer) object.second).impl);
            }
        });
    }

    final Map<OnLayerVisibleChangedListener, OnLayerVisibleChangedListener> listenerWrappers = new IdentityHashMap<>();
    final Layer impl;

    public SceneLayer(String name, @Nullable String cacheDir)
    {
        this.impl = newInstance(name, cacheDir);
    }

    @Override
    public void setVisible(boolean visible)
    {
        impl.setVisible(visible);
    }

    @Override
    public boolean isVisible()
    {
        return impl.isVisible();
    }

    @Override
    public void addOnLayerVisibleChangedListener(final OnLayerVisibleChangedListener l)
    {
        final OnLayerVisibleChangedListener wrapper;
        synchronized (listenerWrappers)
        {
            if (listenerWrappers.containsKey(l))
                return;
            wrapper = new SceneLayer.OnLayerVisibleChangedListenerWrapper(this, l);
        }
        impl.addOnLayerVisibleChangedListener(wrapper);
    }

    @Override
    public void removeOnLayerVisibleChangedListener(OnLayerVisibleChangedListener l)
    {
        final OnLayerVisibleChangedListener wrapper;
        synchronized (listenerWrappers)
        {
            wrapper = listenerWrappers.remove(l);
            if (wrapper == null)
                return;
        }
        impl.removeOnLayerVisibleChangedListener(wrapper);
    }

    public void add(String uri)
    {
        add(uri, null);
    }

    public void add(String uri, String hint)
    {
        add(Layer_interop.getPointer(impl), uri, hint);
    }

    public boolean remove(String uri)
    {
        return remove(Layer_interop.getPointer(impl), uri);
    }

    public boolean contains(String uri)
    {
        return contains(Layer_interop.getPointer(impl), uri);
    }

    public void setVisible(String uri, boolean visible)
    {
        setVisible(Layer_interop.getPointer(impl), uri, visible);
    }

    public void shutdown()
    {
        shutdown(Layer_interop.getPointer(impl));
    }

    @Override
    public String getName()
    {
        return impl.getName();
    }

    @Override
    public <T extends Extension> T getExtension(Class<T> clazz)
    {
        return null;
    }

    final static class OnLayerVisibleChangedListenerWrapper implements OnLayerVisibleChangedListener
    {
        final WeakReference<Layer> ref;
        final OnLayerVisibleChangedListener cb;

        OnLayerVisibleChangedListenerWrapper(Layer impl, OnLayerVisibleChangedListener cb)
        {
            this.ref = new WeakReference<>(impl);
            this.cb = cb;
        }

        @Override
        public void onLayerVisibleChanged(Layer layer)
        {
            Layer impl = ref.get();
            if (impl == null)
                return;
            cb.onLayerVisibleChanged(impl);
        }
    }

    static native Layer newInstance(String name, String cacheDir);
    static native void add(long ptr, String uri, String hint);
    static native boolean remove(long ptr, String uri);
    static native boolean contains(long ptr, String uri);
    static native void setVisible(long ptr, String uri, boolean visible);
    static native void shutdown(long ptr);
}
