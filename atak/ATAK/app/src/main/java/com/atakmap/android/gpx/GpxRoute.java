
package com.atakmap.android.gpx;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

import java.util.List;

/**
 * GPX rteType rte represents route - an ordered list of waypoints representing a series of turn
 * points leading to a destination.
 */
public class GpxRoute extends GpxBase {

    // TODO xsd:nonNegativeInteger, restrictions on other fields too
    /**
     * GPS route number.
     */
    @Element(required = false)
    private Integer number;
    
    @Element(name = "extensions", required = false)
    private GpxRouteExtension extensions;

    /**
     * A list of route points.
     */
    @ElementList(entry = "rtept", inline = true, required = false)
    private List<GpxWaypoint> rtept;

    public Integer getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public List<GpxWaypoint> getPoints() {
        return rtept;
    }

    public void setPoints(List<GpxWaypoint> rtept) {
        this.rtept = rtept;
    }

    public GpxRouteExtension getRouteExtension() {
        return extensions;
    }

    public void setRouteExtension(GpxRouteExtension extensions) {
        this.extensions = extensions;
    }
}
