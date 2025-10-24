package com.atakmap.map;

import android.content.Context;

import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.MockLayer;

import gov.tak.test.KernelJniTest;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GlobeTest extends KernelJniTest
{
    @Test
    public void Globe_add_layer() {
        final OnLayerChangedCallbackRecorder cbrecorder = new OnLayerChangedCallbackRecorder();
        Globe mapView = new Globe();
        mapView.addOnLayersChangedListener(cbrecorder);
        final Layer layer = new MockLayer("add_layer", true);
        mapView.addLayer(layer);
        assertEquals(1, cbrecorder.onLayerAdded.size());
        assertSame(layer, cbrecorder.onLayerAdded.get(0).layer);
        assertSame(mapView,
                cbrecorder.onLayerAdded.get(0).view);

        List<Layer> layers = mapView.getLayers();

        assertNotNull(layers);

        assertTrue(layers.contains(layer));
    }

    @Test
    public void Globe_remove_layer() {
        final OnLayerChangedCallbackRecorder cbrecorder = new OnLayerChangedCallbackRecorder();
        Globe mapView = new Globe();
        mapView.addOnLayersChangedListener(cbrecorder);
        final Layer layer = new MockLayer("remove_layer", true);
        mapView.addLayer(layer);
        mapView.removeLayer(layer);
        assertEquals(1, cbrecorder.onLayerRemoved.size());
        assertSame(layer,
                cbrecorder.onLayerRemoved.get(0).layer);
        assertSame(mapView,
                cbrecorder.onLayerRemoved.get(0).view);

        List<Layer> layers = mapView.getLayers();

        assertNotNull(layers);
        assertTrue(layers.isEmpty());
    }

    @Test
    public void Globe_set_layer_position_forward() {
        final OnLayerChangedCallbackRecorder cbrecorder = new OnLayerChangedCallbackRecorder();
        Globe mapView = new Globe();
        mapView.addOnLayersChangedListener(cbrecorder);
        final Layer layer1 = new MockLayer("layer1", true);
        final Layer layer2 = new MockLayer("layer2", true);
        final Layer layer3 = new MockLayer("layer3", true);
        final Layer layer4 = new MockLayer("layer4", true);
        mapView.addLayer(layer1);
        mapView.addLayer(layer2);
        mapView.addLayer(layer3);
        mapView.addLayer(layer4);

        assertEquals(4, cbrecorder.onLayerAdded.size());
        assertSame(layer1,
                cbrecorder.onLayerAdded.get(0).layer);
        assertSame(layer2,
                cbrecorder.onLayerAdded.get(1).layer);
        assertSame(layer3,
                cbrecorder.onLayerAdded.get(2).layer);
        assertSame(layer4,
                cbrecorder.onLayerAdded.get(3).layer);

        List<Layer> layers = mapView.getLayers();

        assertNotNull(layers);
        assertEquals(4, layers.size());
        assertSame(layer1, layers.get(0));
        assertSame(layer2, layers.get(1));
        assertSame(layer3, layers.get(2));
        assertSame(layer4, layers.get(3));

        mapView.setLayerPosition(layer2, 2);
        assertEquals(1,
                cbrecorder.onLayerPositionChanged.size());
        assertSame(layer2,
                cbrecorder.onLayerPositionChanged.get(0).layer);
        assertEquals(1, cbrecorder.onLayerPositionChanged
                .get(0).oldPos);
        assertEquals(2, cbrecorder.onLayerPositionChanged
                .get(0).newPos);
        assertSame(mapView,
                cbrecorder.onLayerPositionChanged.get(0).view);

        layers = mapView.getLayers();

        assertNotNull(layers);
        assertEquals(4, layers.size());
        assertSame(layer1, layers.get(0));
        assertSame(layer3, layers.get(1));
        assertSame(layer2, layers.get(2));
        assertSame(layer4, layers.get(3));
    }

    @Test
    public void Globe_set_layer_position_to_end() {
        final OnLayerChangedCallbackRecorder cbrecorder = new OnLayerChangedCallbackRecorder();
        Globe mapView = new Globe();
        mapView.addOnLayersChangedListener(cbrecorder);
        final Layer layer1 = new MockLayer("layer1", true);
        final Layer layer2 = new MockLayer("layer2", true);
        final Layer layer3 = new MockLayer("layer3", true);
        final Layer layer4 = new MockLayer("layer4", true);
        mapView.addLayer(layer1);
        mapView.addLayer(layer2);
        mapView.addLayer(layer3);
        mapView.addLayer(layer4);

        assertEquals(4, cbrecorder.onLayerAdded.size());
        assertSame(layer1,
                cbrecorder.onLayerAdded.get(0).layer);
        assertSame(layer2,
                cbrecorder.onLayerAdded.get(1).layer);
        assertSame(layer3,
                cbrecorder.onLayerAdded.get(2).layer);
        assertSame(layer4,
                cbrecorder.onLayerAdded.get(3).layer);

        List<Layer> layers = mapView.getLayers();

        assertNotNull(layers);
        assertEquals(4, layers.size());
        assertSame(layer1, layers.get(0));
        assertSame(layer2, layers.get(1));
        assertSame(layer3, layers.get(2));
        assertSame(layer4, layers.get(3));

        mapView.setLayerPosition(layer2, 3);
        assertEquals(1,
                cbrecorder.onLayerPositionChanged.size());
        assertSame(layer2,
                cbrecorder.onLayerPositionChanged.get(0).layer);
        assertEquals(1, cbrecorder.onLayerPositionChanged
                .get(0).oldPos);
        assertEquals(3, cbrecorder.onLayerPositionChanged
                .get(0).newPos);
        assertSame(mapView,
                cbrecorder.onLayerPositionChanged.get(0).view);

        layers = mapView.getLayers();

        assertNotNull(layers);
        assertEquals(4, layers.size());
        assertSame(layer1, layers.get(0));
        assertSame(layer3, layers.get(1));
        assertSame(layer4, layers.get(2));
        assertSame(layer2, layers.get(3));
    }

    @Test
    public void Globe_set_layer_position_backward() {
        final OnLayerChangedCallbackRecorder cbrecorder = new OnLayerChangedCallbackRecorder();
        Globe mapView = new Globe();
        mapView.addOnLayersChangedListener(cbrecorder);
        final Layer layer1 = new MockLayer("layer1", true);
        final Layer layer2 = new MockLayer("layer2", true);
        final Layer layer3 = new MockLayer("layer3", true);
        final Layer layer4 = new MockLayer("layer4", true);
        mapView.addLayer(layer1);
        mapView.addLayer(layer2);
        mapView.addLayer(layer3);
        mapView.addLayer(layer4);

        assertEquals(4, cbrecorder.onLayerAdded.size());
        assertSame(layer1,
                cbrecorder.onLayerAdded.get(0).layer);
        assertSame(layer2,
                cbrecorder.onLayerAdded.get(1).layer);
        assertSame(layer3,
                cbrecorder.onLayerAdded.get(2).layer);
        assertSame(layer4,
                cbrecorder.onLayerAdded.get(3).layer);

        List<Layer> layers = mapView.getLayers();

        assertNotNull(layers);
        assertEquals(4, layers.size());
        assertSame(layer1, layers.get(0));
        assertSame(layer2, layers.get(1));
        assertSame(layer3, layers.get(2));
        assertSame(layer4, layers.get(3));

        mapView.setLayerPosition(layer3, 1);
        assertEquals(1,
                cbrecorder.onLayerPositionChanged.size());
        assertSame(layer3,
                cbrecorder.onLayerPositionChanged.get(0).layer);
        assertEquals(2, cbrecorder.onLayerPositionChanged
                .get(0).oldPos);
        assertEquals(1, cbrecorder.onLayerPositionChanged
                .get(0).newPos);
        assertSame(mapView,
                cbrecorder.onLayerPositionChanged.get(0).view);

        layers = mapView.getLayers();

        assertNotNull(layers);
        assertEquals(4, layers.size());
        assertSame(layer1, layers.get(0));
        assertSame(layer3, layers.get(1));
        assertSame(layer2, layers.get(2));
        assertSame(layer4, layers.get(3));
    }

    @Test
    public void Globe_set_layer_position_backward_to_start() {
        final OnLayerChangedCallbackRecorder cbrecorder = new OnLayerChangedCallbackRecorder();
        Globe mapView = new Globe();
        mapView.addOnLayersChangedListener(cbrecorder);
        final Layer layer1 = new MockLayer("layer1", true);
        final Layer layer2 = new MockLayer("layer2", true);
        final Layer layer3 = new MockLayer("layer3", true);
        final Layer layer4 = new MockLayer("layer4", true);
        mapView.addLayer(layer1);
        mapView.addLayer(layer2);
        mapView.addLayer(layer3);
        mapView.addLayer(layer4);

        assertEquals(4, cbrecorder.onLayerAdded.size());
        assertSame(layer1,
                cbrecorder.onLayerAdded.get(0).layer);
        assertSame(layer2,
                cbrecorder.onLayerAdded.get(1).layer);
        assertSame(layer3,
                cbrecorder.onLayerAdded.get(2).layer);
        assertSame(layer4,
                cbrecorder.onLayerAdded.get(3).layer);

        List<Layer> layers = mapView.getLayers();

        assertNotNull(layers);
        assertEquals(4, layers.size());
        assertSame(layer1, layers.get(0));
        assertSame(layer2, layers.get(1));
        assertSame(layer3, layers.get(2));
        assertSame(layer4, layers.get(3));

        mapView.setLayerPosition(layer3, 0);
        assertEquals(1,
                cbrecorder.onLayerPositionChanged.size());
        assertSame(layer3,
                cbrecorder.onLayerPositionChanged.get(0).layer);
        assertEquals(2, cbrecorder.onLayerPositionChanged
                .get(0).oldPos);
        assertEquals(0, cbrecorder.onLayerPositionChanged
                .get(0).newPos);
        assertSame(mapView,
                cbrecorder.onLayerPositionChanged.get(0).view);

        layers = mapView.getLayers();

        assertNotNull(layers);
        assertEquals(4, layers.size());
        assertSame(layer3, layers.get(0));
        assertSame(layer1, layers.get(1));
        assertSame(layer2, layers.get(2));
        assertSame(layer4, layers.get(3));
    }

    final static class OnLayerChangedCallbackRecorder
            implements Globe.OnLayersChangedListener {

        public final static class LayerAdded {
            public final Globe view;
            public final Layer layer;

            public LayerAdded(Globe view, Layer layer) {
                this.view = view;
                this.layer = layer;
            }
        }

        public final static class LayerRemoved {
            public final Globe view;
            public final Layer layer;

            public LayerRemoved(Globe view, Layer layer) {
                this.view = view;
                this.layer = layer;
            }
        }

        public final static class LayerPositionChanged {
            public final Globe view;
            public final Layer layer;
            public final int oldPos;
            public final int newPos;

            public LayerPositionChanged(Globe view, Layer layer,
                    int oldPos, int newPos) {
                this.view = view;
                this.layer = layer;
                this.oldPos = oldPos;
                this.newPos = newPos;
            }
        }

        public List<LayerAdded> onLayerAdded = new LinkedList<>();
        public List<LayerRemoved> onLayerRemoved = new LinkedList<>();
        public List<LayerPositionChanged> onLayerPositionChanged = new LinkedList<>();

        @Override
        public void onLayerAdded(Globe mapView, Layer layer) {
            this.onLayerAdded.add(new LayerAdded(mapView, layer));
        }

        @Override
        public void onLayerRemoved(Globe mapView, Layer layer) {
            this.onLayerRemoved.add(new LayerRemoved(mapView, layer));
        }

        @Override
        public void onLayerPositionChanged(Globe mapView, Layer layer,
                int oldPosition, int newPosition) {
            this.onLayerPositionChanged.add(new LayerPositionChanged(mapView,
                    layer, oldPosition, newPosition));
        }
    }
}