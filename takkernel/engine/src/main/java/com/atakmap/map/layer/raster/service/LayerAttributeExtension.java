package com.atakmap.map.layer.raster.service;

import com.atakmap.map.layer.Layer2;

public interface LayerAttributeExtension extends Layer2.Extension
{
    void setAttribute(String key, Object value);
    Object getAttribute(String key);
    <T> T getAttribute(String key, Class<T> clazz);
}
