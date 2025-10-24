package com.atakmap.map.layer.feature;

import com.atakmap.map.layer.feature.geometry.Envelope;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
public interface EnvelopeFilter {
    boolean accept(Envelope bounds);
}
