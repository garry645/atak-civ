
package com.atakmap.android.cot.detail;

import android.graphics.Color;

import com.atakmap.android.maps.Association;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.user.PlacePointTool;
import gov.tak.platform.lang.Parsers;
import com.atakmap.comms.CommsMapComponent.ImportResult;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.concurrent.ConcurrentHashMap;

public class SPIDetailHandler extends CotDetailHandler
        implements Marker.OnTitleChangedListener,
        MapItem.OnVisibleChangedListener,
        MapEventDispatcher.MapEventDispatchListener {

    public static final String TAG = "SPIHandler";

    private final MapView mapView;
    private final ConcurrentHashMap<String, Holder> table = new ConcurrentHashMap<>();
    private static SPIDetailHandler _instance;

    /**
     * Handles spi details with the following structure:
     * <p>
     * <__spi lat="42.00" lon="-72.00" alt="200" />
     * <p>
     * Allows for points to be defined which have a embedded SPI information:
     * - lat is a decimal value [-90,90)
     * - lon is the decimal value [-180, 180)
     * - alt is defined as HAE in meters
     *
     */
    private SPIDetailHandler() {
        super("__spi");
        mapView = MapView.getMapView();
        mapView.getMapEventDispatcher()
                .addMapEventListener(MapEvent.ITEM_REMOVED, this);
    }

    /**
     * Returns the instance of the SPI detail handler
     * @return the spi detail handler
     */
    public synchronized static SPIDetailHandler getInstance() {
        if (_instance == null)
            _instance = new SPIDetailHandler();
        return _instance;
    }

    @Override
    public boolean isSupported(MapItem item, CotEvent event, CotDetail detail) {
        return item instanceof Marker;
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail) {
        String s = item.getMetaString("spi_uid", null);
        if (s != null) {
            MapItem spi = getMapItem(s);
            if (spi != null) {
                GeoPoint point = ((PointMapItem) spi).getPoint();
                CotDetail cd = new CotDetail("__spi");
                String[] args = CotPoint.decimate(point).split(",", -1);
                cd.setAttribute("lat", args[0]);
                cd.setAttribute("lon", args[1]);
                if (args.length > 2)
                    cd.setAttribute("alt", args[2]);
                detail.addChild(cd);
            }
        }
        return true;
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event,
            CotDetail detail) {
        String latStr = detail.getAttribute("lat");
        String lonStr = detail.getAttribute("lon");
        String altStr = detail.getAttribute("alt");
        double lat = Parsers.parseDouble(latStr, Double.NaN);
        double lon = Parsers.parseDouble(lonStr, Double.NaN);
        double alt = Parsers.parseDouble(altStr, Double.NaN);

        if (Double.isNaN(lat) || Double.isNaN(lon))
            return ImportResult.FAILURE;

        create((Marker) item, new GeoPoint(lat, lon, alt));

        return ImportResult.SUCCESS;

    }

    @Override
    public void onTitleChanged(Marker marker) {
        MapItem mi = getMapItem(marker.getUID() + ".SPI");
        if (mi == null)
            marker.removeOnTitleChangedListener(this);
        else
            mi.setTitle(marker.getTitle() + ".SPI");
    }

    @Override
    public void onVisibleChanged(MapItem item) {
        MapItem mi = getMapItem(item.getUID() + ".SPI");
        if (mi == null)
            item.addOnVisibleChangedListener(this);
        else
            mi.setVisible(item.getVisible());

    }

    @Override
    public void onMapEvent(MapEvent mapEvent) {
        final String uid = mapEvent.getItem().getUID();
        Holder h = table.remove(uid);
        if (h != null)
            h.remove();

    }

    private static class Holder {
        Marker item;
        Association association;
        Marker spi;

        public void remove() {
            if (item != null)
                item.removeFromGroup();
            if (association != null)
                association.removeFromGroup();
            if (spi != null)
                spi.removeFromGroup();
        }
    }

    /**
     * Assists with the construction of a Marker with an arbitrary SPI
     * @param item the marker
     * @param spiPoint the geopoint of the SPI to produce
     * @return the Marker if it is constructed successfully
     */
    public synchronized Marker create(Marker item, GeoPoint spiPoint) {

        Holder holder = table.get(item.getUID());
        if (holder == null) {
            table.put(item.getUID(), holder = new Holder());
            holder.item = item;
        }

        if (holder.spi == null || holder.spi.getGroup() == null) {
            String spiuid = item.getUID() + ".SPI";
            holder.spi = new PlacePointTool.MarkerCreator(spiPoint)
                    .setUid(spiuid)
                    .setCallsign(item.getTitle() + ".SPI")
                    .setType("b-m-p-s-p-i")
                    .setArchive(false)
                    .setNeverPersist(true)
                    .showCotDetails(false)
                    .placePoint();
            holder.spi.setMetaBoolean("movable", false);
            item.addOnTitleChangedListener(this);
            item.addOnVisibleChangedListener(this);
        }

        holder.spi.setMetaBoolean("removable", false);
        holder.spi.setPoint(spiPoint);
        String parent = holder.item.getMetaString("parent_uid", "");
        if (!FileSystemUtils.isEmpty(parent)
                && parent.equals(mapView.getSelfMarker().getUID())) {
            holder.spi.setMetaBoolean("movable", true);
        }
        holder.spi.setMetaString("sensor_uid", item.getUID());
        holder.item.setMetaString("spi_uid", holder.spi.getUID());

        if (holder.association == null
                || holder.association.getGroup() == null) {
            holder.association = new Association(
                    holder.spi.getUID() + ".ASSOCIATION");
            holder.association.setFirstItem(item);
            holder.association.setSecondItem(holder.spi);
            holder.association.setColor(Color.YELLOW);
            holder.association.setMetaBoolean("removable", false);
            holder.association.setClickable(false);
            holder.association.setMetaBoolean("neverpersist", true);
            mapView.getRootGroup().addItem(holder.association);
        }
        return holder.spi;
    }
}
