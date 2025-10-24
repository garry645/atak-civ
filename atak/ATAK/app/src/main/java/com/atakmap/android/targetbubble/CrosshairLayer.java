
package com.atakmap.android.targetbubble;

import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.util.Collections2;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import gov.tak.platform.marshal.AbstractMarshal;
import gov.tak.platform.marshal.MarshalManager;

public final class CrosshairLayer extends AbstractLayer {
    static {
        MarshalManager.registerMarshal(new AbstractMarshal(CrosshairLayer.class,
                gov.tak.platform.layers.CrosshairLayer.class) {
            @Override
            protected <T, V> T marshalImpl(V in) {
                return (T) ((CrosshairLayer) in).impl;
            }
        }, CrosshairLayer.class, gov.tak.platform.layers.CrosshairLayer.class);
    }

    public interface OnCrosshairColorChangedListener {
        void onCrosshairColorChanged(CrosshairLayer layer, int color);
    }

    private final Set<OnCrosshairColorChangedListener> listeners = Collections
            .newSetFromMap(new ConcurrentHashMap<>());

    final gov.tak.platform.layers.CrosshairLayer impl;

    public CrosshairLayer(String name) {
        super(name);

        this.impl = new gov.tak.platform.layers.CrosshairLayer(name);
        this.impl.addOnCrosshairColorChangedListener(
                new gov.tak.platform.layers.CrosshairLayer.OnCrosshairColorChangedListener() {
                    @Override
                    public void onCrosshairColorChanged(
                            gov.tak.platform.layers.CrosshairLayer layer,
                            int color) {
                        for (OnCrosshairColorChangedListener l : listeners)
                            l.onCrosshairColorChanged(CrosshairLayer.this,
                                    color);
                    }
                });
    }

    public void setColor(int color) {
        impl.setColor(color);

    }

    public int getCrosshairColor() {
        return impl.getCrosshairColor();
    }

    public void addOnCrosshairColorChangedListener(
            OnCrosshairColorChangedListener l) {
        this.listeners.add(l);
    }

    public void removeOnCrosshairColorChangedListener(
            OnCrosshairColorChangedListener l) {
        this.listeners.remove(l);
    }
}
