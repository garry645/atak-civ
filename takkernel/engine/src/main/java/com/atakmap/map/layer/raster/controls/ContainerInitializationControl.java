package com.atakmap.map.layer.raster.controls;

import java.util.Map;

/**
 * Allows for injection of metadata when a new container is being initialized.
 */
public interface ContainerInitializationControl
{
    void setMetadata(Map<String, Object> metadata);
}
