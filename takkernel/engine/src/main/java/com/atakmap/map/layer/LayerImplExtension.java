package com.atakmap.map.layer;

public final class LayerImplExtension implements Layer2.Extension {

    public LayerImplExtension(Layer implLayer) {
        instance = implLayer;
    }

    public Layer getLayer() {
        return instance;
    }

    private final Layer instance;
}
