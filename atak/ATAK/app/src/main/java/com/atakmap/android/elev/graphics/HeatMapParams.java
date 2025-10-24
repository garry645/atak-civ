
package com.atakmap.android.elev.graphics;

import android.os.CancellationSignal;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.elevation.ElevationData;

class HeatMapParams {
    final GeoPoint upperLeft = GeoPoint.createMutable();
    final GeoPoint upperRight = GeoPoint.createMutable();
    final GeoPoint lowerRight = GeoPoint.createMutable();
    final GeoPoint lowerLeft = GeoPoint.createMutable();

    int elevationModel = ElevationData.MODEL_TERRAIN;

    int xSampleResolution;
    int ySampleResolution;

    byte[] rgbaData;

    float[] elevationData;
    float minElev;
    float maxElev;
    int numSamples;

    int drawVersion;
    boolean needsRefresh;
    boolean quick;

    boolean valid;

    int[] hsvLut;
    float lutAlpha;
    float lutSaturation;
    float lutValue;

    final CancellationSignal querySignal = new CancellationSignal();

}
