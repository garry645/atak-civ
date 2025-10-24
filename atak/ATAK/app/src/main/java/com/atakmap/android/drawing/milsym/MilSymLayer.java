
package com.atakmap.android.drawing.milsym;

import android.util.Pair;

import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import com.atakmap.map.layer.opengl.GLLayerSpi2;

final class MilSymLayer extends AbstractLayer {
    static {
        GLLayerFactory.register(new GLLayerSpi2() {
            @Override
            public int getPriority() {
                return 1;
            }

            @Override
            public GLLayer2 create(Pair<MapRenderer, Layer> arg) {
                if (!(arg.second instanceof MilSymLayer))
                    return null;
                return new GLMilSymLayer(arg.first, (MilSymLayer) arg.second);
            }
        });
    }

    final FeatureDataStore2 datastore;

    MilSymLayer(FeatureDataStore2 datastore) {
        super("MilSym");
        this.datastore = datastore;
    }
}
