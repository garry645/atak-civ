package com.atakmap.map.layer.feature.control;

import com.atakmap.map.layer.feature.EnvelopeFilter;
import com.atakmap.map.layer.feature.GeometryFilter;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
public interface SpatialFilterControl {
    void setFilter(EnvelopeFilter filter);
    void setFilter(GeometryFilter filter);
}
