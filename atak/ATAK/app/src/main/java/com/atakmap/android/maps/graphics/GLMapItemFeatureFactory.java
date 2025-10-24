
package com.atakmap.android.maps.graphics;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MetaMapPoint;
import com.atakmap.android.maps.MetaShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;

final class GLMapItemFeatureFactory {
    final static Comparator<Map.Entry<GLMapItemFeatureSpi, Integer>> spiPriorityComp = new Comparator<Map.Entry<GLMapItemFeatureSpi, Integer>>() {
        @Override
        public int compare(Map.Entry<GLMapItemFeatureSpi, Integer> a,
                Map.Entry<GLMapItemFeatureSpi, Integer> b) {
            final int ap = a.getValue();
            final int bp = b.getValue();

            if (ap > bp)
                return -1;
            else if (ap < bp)
                return 1;
            return a.getKey().hashCode() - b.getKey().hashCode();
        }
    };

    final static Map<GLMapItemFeatureSpi, Integer> registry = new IdentityHashMap<>();
    static ArrayList<GLMapItemFeatureSpi> spis = new ArrayList<>();

    private GLMapItemFeatureFactory() {
    }

    private static void refreshSpis() {
        ArrayList<Map.Entry<GLMapItemFeatureSpi, Integer>> e = new ArrayList<>(
                registry.entrySet());
        Collections.sort(e, spiPriorityComp);
        ArrayList<GLMapItemFeatureSpi> s = new ArrayList<>(e.size());
        for (Map.Entry<GLMapItemFeatureSpi, Integer> spi : e)
            s.add(spi.getKey());
        spis = s;
    }

    synchronized static void registerSpi(GLMapItemFeatureSpi spi,
            int priority) {
        registry.put(spi, priority);
        refreshSpis();
    }

    synchronized static void unregisterSpi(GLMapItemFeatureSpi spi) {
        if (registry.remove(spi) != null)
            refreshSpis();
    }

    static boolean isSupported(final MapItem item) {
        // Item is flagged not to render
        // Useful for parent map items that render using children items
        if (item.hasMetaValue("ignoreRender")
                || item instanceof MetaMapPoint
                || item instanceof MetaShape)
            return false;

        final ArrayList<GLMapItemFeatureSpi> s = spis;
        for (GLMapItemFeatureSpi spi : s)
            if (spi.isSupported(item))
                return true;
        return false;
    }

    static GLMapItemFeature create(GLMapItemFeatures features, MapItem item) {
        // Item is flagged not to render
        // Useful for parent map items that render using children items
        if (item.hasMetaValue("ignoreRender")
                || item instanceof MetaMapPoint
                || item instanceof MetaShape)
            return null;

        final ArrayList<GLMapItemFeatureSpi> s = spis;
        for (GLMapItemFeatureSpi spi : s) {
            GLMapItemFeature f = spi.create(features, item);
            if (f != null)
                return f;
        }
        return null;
    }
}
