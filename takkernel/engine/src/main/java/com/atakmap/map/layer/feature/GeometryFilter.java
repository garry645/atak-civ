package com.atakmap.map.layer.feature;

import com.atakmap.map.layer.feature.geometry.Geometry;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
public interface GeometryFilter {
    boolean accept(Geometry geometry);
}
