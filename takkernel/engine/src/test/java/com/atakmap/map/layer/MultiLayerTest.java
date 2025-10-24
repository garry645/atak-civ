
package com.atakmap.map.layer;

import org.junit.Assert;
import org.junit.Test;

public class MultiLayerTest {

    @Test
    public void setLayerPosition() {
        MultiLayer layer = new MultiLayer("");
        Layer layerA = new TestLayer("A");
        Layer layerB = new TestLayer("B");
        Layer layerC = new TestLayer("C");
        Layer layerD = new TestLayer("D");
        layer.addLayer(layerA);
        layer.addLayer(layerB);
        layer.addLayer(layerC);
        layer.addLayer(layerD);
        layer.setLayerPosition(layerB, 0);
        Assert.assertEquals(0, layer.getLayers().indexOf(layerB));
        layer.setLayerPosition(layerB, 1);
        Assert.assertEquals(1, layer.getLayers().indexOf(layerB));
        layer.setLayerPosition(layerC, 3);
        Assert.assertEquals(3, layer.getLayers().indexOf(layerC));
        layer.setLayerPosition(layerC, 2);
        Assert.assertEquals(2, layer.getLayers().indexOf(layerC));
        layer.setLayerPosition(layerA, 2);
        Assert.assertEquals(2, layer.getLayers().indexOf(layerA));
        layer.setLayerPosition(layerD, 2);
        Assert.assertEquals(2, layer.getLayers().indexOf(layerD));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void setLayerPositionLessThanZero() {
        MultiLayer layer = new MultiLayer("");
        Layer layerA = new TestLayer("A");
        layer.setLayerPosition(layerA, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void setLayerPositionGreaterThanSizeMinusOne() {
        MultiLayer layer = new MultiLayer("");
        Layer layerA = new TestLayer("A");
        layer.setLayerPosition(layerA, 1);
    }

    private static class TestLayer extends AbstractLayer {
        protected TestLayer(String name) {
            super(name);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
