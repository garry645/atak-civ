
package com.atakmap.android.gpx;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.NamespaceList;
import org.simpleframework.xml.Root;

import com.atakmap.android.routes.Route.RouteOrder;
import com.atakmap.android.routes.Route.RouteMethod;
import com.atakmap.android.routes.Route.RouteDirection;
import com.atakmap.android.routes.Route.RouteType;

/**
 * GPX Route Extension contain optional information that governs how the route is displayed
 */

@NamespaceList({
   // Using NamespaceList to avoid namespacing being applied on top level extensions element
   @Namespace(reference = "http://tak.gov", prefix = "tak")
})
public class GpxRouteExtension {
    @Namespace(reference = "http://tak.gov", prefix = "tak")
    @Element(name="LineColor", required = false)
    Integer lineColor;

    @Namespace(reference = "http://tak.gov", prefix = "tak")
    @Element(name="LineWidth", required = false)
    Double lineWidth;

    @Namespace(reference = "http://tak.gov", prefix = "tak")
    @Element(name="LineStyle", required = false)
    Integer lineStyle;

    @Namespace(reference = "http://tak.gov", prefix = "tak")
    @Element(name="RouteOrder", required = false)
    RouteOrder routeOrder;

    @Namespace(reference = "http://tak.gov", prefix = "tak")
    @Element(name="RouteMethod", required = false)
    RouteMethod routeMethod;

    @Namespace(reference = "http://tak.gov", prefix = "tak")
    @Element(name="RouteDirection", required = false)
    RouteDirection routeDirection;

    @Namespace(reference = "http://tak.gov", prefix = "tak")
    @Element(name="RouteType", required = false)
    RouteType routeType;

    @Namespace(reference = "http://tak.gov", prefix = "tak")
    @Element(name="PlanningMethod", required = false)
    String planningMethod;

    public Integer getLineColor() {
        return lineColor;
    }

    public void setLineColor(int lineColor) {
        this.lineColor = lineColor;
    }

    public Double getLineWidth() {
       return lineWidth;
    }

    public void setLineWidth(Double lineWidth) {
       this.lineWidth = lineWidth;
    }

    public Integer getLineStyle() { return lineStyle; }

    public void setLineStyle(int lineStyle) {
        this.lineStyle = lineStyle;
    }

    public RouteOrder getRouteOrder() {
       return routeOrder;
    }

    public void setRouteOrder(RouteOrder routeOrder) {
       this.routeOrder = routeOrder;
    }

    public RouteMethod getRouteMethod() {
       return routeMethod;
    }

    public void setRouteMethod(RouteMethod routeMethod) {
       this.routeMethod = routeMethod;
    }

    public RouteDirection getRouteDirection() {
       return routeDirection;
    }

    public void setRouteDirection(RouteDirection routeDirection) {
       this.routeDirection = routeDirection;
    }

    public RouteType getRouteType() {
       return routeType;
    }

    public void setRouteType(RouteType routeType) {
       this.routeType = routeType;
    }

    public String getPlanningMethod() {
       return planningMethod;
    }

    public void setPlanningMethod(String planningMethod) {
       this.planningMethod = planningMethod;
    }
}
