
package com.atakmap.android.maps.graphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import android.util.Pair;

import com.atakmap.android.maps.MapItem;
import com.atakmap.map.MapRenderer;
import com.atakmap.util.Collections2;

public final class GLMapItemFactory {

    private final static Map<GLMapItemSpi2, GLMapItemSpi3> spiAdapters = new IdentityHashMap<>();
    final static Comparator<GLMapItemSpi3> spiPriorityComp = new Comparator<GLMapItemSpi3>() {
        @Override
        public int compare(GLMapItemSpi3 a, GLMapItemSpi3 b) {
            final int ap = a.getPriority();
            final int bp = b.getPriority();

            if (ap > bp)
                return -1;
            else if (ap < bp)
                return 1;
            return a.hashCode() - b.hashCode();
        }
    };

    final static Set<GLMapItemSpi3> registry = Collections2
            .newIdentityHashSet();
    static ArrayList<GLMapItemSpi3> spis = new ArrayList<>();

    private GLMapItemFactory() {
    }

    private static void refreshSpis() {
        ArrayList<GLMapItemSpi3> s = new ArrayList<>(registry);
        Collections.sort(s, spiPriorityComp);
        spis = s;
    }

    public synchronized static void registerSpi(GLMapItemSpi2 spi) {
        if (spiAdapters.containsKey(spi))
            return;
        GLMapItemSpi3 adapted = new SpiAdapter(spi);
        spiAdapters.put(spi, adapted);
        registerSpi(adapted);
    }

    public synchronized static void registerSpi(GLMapItemSpi3 spi) {
        if (registry.add(spi))
            refreshSpis();
    }

    public synchronized static void unregisterSpi(GLMapItemSpi2 spi) {
        final GLMapItemSpi3 adapted = spiAdapters.remove(spi);
        if (adapted == null)
            return;
        unregisterSpi(adapted);
    }

    public synchronized static void unregisterSpi(GLMapItemSpi3 spi) {
        if (registry.remove(spi))
            refreshSpis();
    }

    public static GLMapItem2 create3(MapRenderer surface, MapItem item) {
        final Pair<MapRenderer, MapItem> arg = Pair.create(surface, item);
        final ArrayList<GLMapItemSpi3> s = spis;
        for (GLMapItemSpi3 spi : s) {
            GLMapItem2 retval = spi.create(arg);
            if (retval != null)
                return retval;
        }
        return null;
    }

    private final static class SpiAdapter implements GLMapItemSpi3 {
        private final GLMapItemSpi2 impl;

        public SpiAdapter(GLMapItemSpi2 impl) {
            this.impl = impl;
        }

        @Override
        public int getPriority() {
            return this.impl.getPriority();
        }

        @Override
        public GLMapItem2 create(Pair<MapRenderer, MapItem> object) {
            return this.impl.create(object);
        }
    }
}
