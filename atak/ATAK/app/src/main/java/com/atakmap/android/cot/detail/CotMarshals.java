
package com.atakmap.android.cot.detail;

import gov.tak.api.annotation.DeprecatedApi;
import com.atakmap.coremap.cot.event.CotAttribute;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.marshal.IMarshal;
import gov.tak.platform.marshal.AbstractMarshal;
import gov.tak.platform.marshal.MarshalManager;

/**
 * @deprecated these {@code IMarshal}s only exist as a go-between for old (com.atakmap) and new
 * (gov.tak) {@code CotEvent}s. When the former's removed, this will go with it.
 */
@Deprecated
@DeprecatedApi(since = "5.2", forRemoval = true)
class CotMarshals {
    static final IMarshal ATAKMAP_GOVTAK_COT_EVENT = new AbstractMarshal(
            com.atakmap.coremap.cot.event.CotEvent.class, CotEvent.class) {
        @Override
        protected <T, V> T marshalImpl(V in) {
            if (in instanceof com.atakmap.coremap.cot.event.CotEvent) {
                com.atakmap.coremap.cot.event.CotEvent event = (com.atakmap.coremap.cot.event.CotEvent) in;

                IGeoPoint point = new GeoPoint(event.getCotPoint().getLat(),
                        event.getCotPoint().getLon(),
                        event.getGeoPoint().getAltitude());
                CotDetail detail = MarshalManager.marshal(event.getDetail(),
                        com.atakmap.coremap.cot.event.CotDetail.class,
                        CotDetail.class);
                CoordinatedTime time = event.getTime() != null
                        ? new CoordinatedTime(
                                event.getTime().getMilliseconds())
                        : null;
                CoordinatedTime start = event.getStart() != null
                        ? new CoordinatedTime(
                                event.getStart().getMilliseconds())
                        : null;
                CoordinatedTime stale = event.getStale() != null
                        ? new CoordinatedTime(
                                event.getStale().getMilliseconds())
                        : null;

                CotEvent marshaled = new CotEvent();
                if (event.getUID() != null)
                    marshaled.setUID(event.getUID());
                if (event.getType() != null)
                    marshaled.setType(event.getType());
                if (point != null)
                    marshaled.setPoint(point);
                if (time != null)
                    marshaled.setTime(time);
                if (stale != null)
                    marshaled.setStale(stale);
                if (event.getHow() != null)
                    marshaled.setHow(event.getHow());
                if (detail != null)
                    marshaled.setDetail(detail);
                if (event.getOpex() != null)
                    marshaled.setOpex(event.getOpex());
                if (event.getQos() != null)
                    marshaled.setQos(event.getQos());
                if (event.getAccess() != null)
                    marshaled.setAccess(event.getAccess());
                return (T) marshaled;
            }

            return (T) new CotEvent();
        }
    };

    static final IMarshal ATAKMAP_GOVTAK_COT_DETAIL = new AbstractMarshal(
            com.atakmap.coremap.cot.event.CotDetail.class, CotDetail.class) {
        @Override
        protected <T, V> T marshalImpl(V in) {
            if (in instanceof com.atakmap.coremap.cot.event.CotDetail) {
                com.atakmap.coremap.cot.event.CotDetail detail = (com.atakmap.coremap.cot.event.CotDetail) in;

                CotDetail govTakDetail = new CotDetail(detail.getElementName());
                recursiveDetailMarshal(detail, govTakDetail);

                return (T) govTakDetail;
            }

            return (T) new CotDetail();
        }

        private void recursiveDetailMarshal(
                com.atakmap.coremap.cot.event.CotDetail fromDetail,
                CotDetail toDetail) {
            for (CotAttribute attr : fromDetail.getAttributes()) {
                toDetail.setAttribute(attr.getName(), attr.getValue());
            }
            for (com.atakmap.coremap.cot.event.CotDetail childDetail : fromDetail
                    .getChildren()) {
                CotDetail toChildDetail = new CotDetail(
                        childDetail.getElementName(),
                        childDetail.getInnerText());
                toDetail.addChild(toChildDetail);
                recursiveDetailMarshal(childDetail, toChildDetail);
            }
        }
    };

    static final IMarshal GOVTAK_ATAKMAP_COT_DETAIL = new AbstractMarshal(
            CotDetail.class, com.atakmap.coremap.cot.event.CotDetail.class) {
        @Override
        protected <T, V> T marshalImpl(V in) {
            if (in instanceof CotDetail) {
                CotDetail govTakDetail = (CotDetail) in;

                com.atakmap.coremap.cot.event.CotDetail detail = new com.atakmap.coremap.cot.event.CotDetail(
                        govTakDetail.getElementName());
                recursiveDetailMarshal(govTakDetail, detail);

                return (T) detail;
            }

            return (T) new com.atakmap.coremap.cot.event.CotDetail();
        }

        private void recursiveDetailMarshal(CotDetail fromDetail,
                com.atakmap.coremap.cot.event.CotDetail toDetail) {
            for (gov.tak.api.cot.event.CotAttribute attr : fromDetail
                    .getAttributes()) {
                toDetail.setAttribute(attr.getName(),
                        attr.getValue());
            }
            if (!fromDetail.getChildren().isEmpty()) {
                for (CotDetail childDetail : fromDetail.getChildren()) {
                    com.atakmap.coremap.cot.event.CotDetail toChildDetail = new com.atakmap.coremap.cot.event.CotDetail(
                            childDetail.getElementName());
                    recursiveDetailMarshal(childDetail, toChildDetail);
                    toDetail.addChild(toChildDetail);
                }
            } else {
                toDetail.setInnerText(fromDetail.getInnerText());
            }
        }
    };
}
