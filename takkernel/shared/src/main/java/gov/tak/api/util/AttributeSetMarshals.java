package gov.tak.api.util;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

import java.util.HashMap;
import java.util.Map;

import gov.tak.api.marshal.IMarshal;
import gov.tak.platform.marshal.AbstractMarshal;
import gov.tak.platform.marshal.MarshalManager;

class AttributeSetMarshals {
    static final IMarshal IN_MAP = new AbstractMarshal(Map.class, AttributeSet.class) {
        @Override
        protected <T, V> T marshalImpl(V in) {
            if (!(in instanceof Map))
                return (T) new AttributeSet();
            return (T)AttributeSetUtils.fromMap((Map<String, Object>)in, true);
        }
    };

    static final IMarshal IN_GEOPOINTMETADATA = new AbstractMarshal(GeoPointMetaData.class, AttributeSet.class) {
        @Override
        protected <T, V> T marshalImpl(V in) {
            if (!(in instanceof GeoPointMetaData))
                return (T) new AttributeSet();
            GeoPointMetaData inGpmd = (GeoPointMetaData)in;
            AttributeSet attrSet = new AttributeSet();
            attrSet.setAttribute("lat", inGpmd.get().getLatitude());
            attrSet.setAttribute("lon", inGpmd.get().getLongitude());
            attrSet.setAttribute("alt", inGpmd.get().getAltitude());
            attrSet.setAttribute("altRef", inGpmd.get().getAltitudeReference().toString());
            attrSet.setAttribute("ce", inGpmd.get().getCE());
            attrSet.setAttribute("le", inGpmd.get().getLE());
            // XXX - required for implicit marshaling
            attrSet.setAttribute(AttributeSetUtils.MAPPED_TYPE, GeoPointMetaData.class.getName());
            return (T) attrSet;
        }
    };

    static final IMarshal OUT_GEOPOINTMETADATA = new AbstractMarshal(AttributeSet.class, GeoPointMetaData.class) {
        @Override
        protected <T, V> T marshalImpl(V in) {
            if (!(in instanceof AttributeSet))
                return (T) new GeoPointMetaData();
            AttributeSet inAttrs = (AttributeSet)in;
            double lat = inAttrs.getDoubleAttribute("lat");
            double lon = inAttrs.getDoubleAttribute("lon");
            double alt = inAttrs.getDoubleAttribute("alt");
            GeoPoint.AltitudeReference altRef =
                    GeoPoint.AltitudeReference.get(inAttrs.getStringAttribute("altRef"));
            double ce = inAttrs.getDoubleAttribute("ce");
            double le = inAttrs.getDoubleAttribute("le");
            GeoPointMetaData gpmd = new GeoPointMetaData(new GeoPoint(lat, lon, alt, altRef, ce, le));
            return (T) gpmd;
        }
    };
}
