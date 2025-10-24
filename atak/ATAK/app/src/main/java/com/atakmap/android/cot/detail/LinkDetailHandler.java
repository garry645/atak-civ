
package com.atakmap.android.cot.detail;

import android.content.Intent;
import android.net.Uri;

import com.atakmap.android.emergency.EmergencyDetailHandler;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.toolbars.RangeAndBearingMapItem;
import com.atakmap.comms.CommsMapComponent.ImportResult;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;

/**
 * Specific to marker links (pairing lines?)
 */
class LinkDetailHandler extends CotDetailHandler {

    private static final String TAG = "LinkDetailHandler";
    private static final String RELATION_URL = "r-u";

    private final MapView _mapView;

    LinkDetailHandler(MapView view) {
        super("link");
        _mapView = view;
    }

    @Override
    public boolean isSupported(MapItem item, CotEvent event, CotDetail detail) {
        //Note on export/toCotDetail, this detail is the top level cot event/detail
        //Note on import/toItemMetadata, this detail is the link detail

        //during toItemMetadata, we want to import the link
        String relation = detail.getAttribute("relation");
        if (!FileSystemUtils.isEmpty(relation) &&
                FileSystemUtils.isEquals(RELATION_URL,
                        relation.toLowerCase(LocaleUtil.getCurrent())))
            return true;

        //during toCotDetail we want to include the URLs e.g. during persist/send
        ArrayList<String> linkUrl = item
                .getMetaStringArrayList(UrlLinkDetailEntry.ASSOCIATED_URLS_KEY);
        return !FileSystemUtils.isEmpty(linkUrl)
                || item instanceof PointMapItem
                || item instanceof RangeAndBearingMapItem;
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event,
            CotDetail detail) {

        boolean ret = false;

        final ArrayList<String> associatedUrls = item
                .getMetaStringArrayList(UrlLinkDetailEntry.ASSOCIATED_URLS_KEY);

        if (!FileSystemUtils.isEmpty(associatedUrls)) {
            for (String s : associatedUrls) {
                CotDetail link = new CotDetail("link");
                UrlLinkDetailEntry ulde = UrlLinkDetailEntry
                        .fromStringRepresentation(s);
                if (ulde != null) {
                    link.setAttribute("relation", RELATION_URL);
                    link.setAttribute("url", ulde.getUrl());
                    link.setAttribute("uid", ulde.getUid());
                    link.setAttribute("remarks", ulde.getRemark());
                    link.setAttribute("mime", ulde.getMimeType());
                }
                detail.addChild(link);
                ret = true;
            }
        }

        String parent_uid = item.getMetaString("parent_uid", "");
        String parent_type = item.getMetaString("parent_type", "");
        String production_time = item.getMetaString("production_time", "");
        String parent_callsign = item.getMetaString("parent_callsign", "");

        // <link relation='p-p' type='a-f-G-U-C-I' uid='ANDROID-xx:01' />

        if (!FileSystemUtils.isEmpty(parent_uid) &&
                !FileSystemUtils.isEmpty(parent_type)) {
            CotDetail link = new CotDetail("link");
            link.setAttribute("uid", parent_uid);
            link.setAttribute("type", parent_type);
            link.setAttribute("relation", "p-p");
            if (!FileSystemUtils.isEmpty(production_time)) {
                link.setAttribute("production_time", production_time);
            }
            if (!FileSystemUtils.isEmpty(parent_callsign)) {
                link.setAttribute("parent_callsign", parent_callsign);
            }
            detail.addChild(link);
            ret = true;
        }
        return ret;
    }

    @Override
    public ImportResult toItemMetadata(MapItem marker, CotEvent event,
            CotDetail detail) {

        final String markerType = marker.getType();
        final String linkType = detail.getAttribute("type");
        final String linkRelation = detail.getAttribute("relation");
        final String linkUid = detail.getAttribute("uid");

        if (linkRelation != null && linkRelation.equals(RELATION_URL)) {
            String url = detail.getAttribute("url");
            try {
                if (FileSystemUtils.isEmpty(url)) {
                    url = "";
                } else if (url.startsWith("/") || url.startsWith("\\")) {
                    url = "file:///" + url.substring(1);
                } else {
                    Uri uri = Uri.parse(url);
                    if (uri.getScheme() == null) {
                        url = "https://" + url;
                    }
                }
            } catch (Exception ex) {
                // ignore
            }
            String uid = detail.getAttribute("uid");
            String remark = detail.getAttribute("remarks");
            String mime = detail.getAttribute("mime");
            ArrayList<String> associatedUrls = marker.getMetaStringArrayList(
                    UrlLinkDetailEntry.ASSOCIATED_URLS_KEY);

            if (associatedUrls == null)
                associatedUrls = new ArrayList<>();
            else {
                // ATAK-18527 work on a copy so modifications to the array do not impact users
                associatedUrls = new ArrayList<>(associatedUrls);
            }

            // replace an existing entry
            int replaceIdx = -1;

            for (int i = 0; i < associatedUrls.size(); ++i) {
                if (associatedUrls.get(i).startsWith(uid))
                    replaceIdx = i;
            }

            if (replaceIdx >= 0)
                associatedUrls.remove(replaceIdx);

            // single
            if (!FileSystemUtils.isEmpty(url) && !url.startsWith("file")) {
                // TODO: consider how to remove links
                // the big blocker is if you have multiple links
                // how does one know if a message has been removed
                // since each time this is called, it is only for a
                // single link

                UrlLinkDetailEntry ulde = new UrlLinkDetailEntry(uid, url,
                        remark, mime);

                if (replaceIdx < 0)
                    associatedUrls.add(ulde.toStringRepresentation());
                else
                    associatedUrls.add(replaceIdx,
                            ulde.toStringRepresentation());

                if (FileSystemUtils.isEmpty(associatedUrls))
                    marker.removeMetaData(
                            UrlLinkDetailEntry.ASSOCIATED_URLS_KEY);
                else
                    marker.setMetaStringArrayList(
                            UrlLinkDetailEntry.ASSOCIATED_URLS_KEY,
                            associatedUrls);
            }
            return ImportResult.SUCCESS;
        }

        if (markerType.startsWith("b-m-p-s-p-i")
                && !FileSystemUtils.isEmpty(linkUid)) {
            //handle SPI points
            //Log.d(TAG, "Processing SPI marker with link uid: " + linkUid + ", for marker: " + marker.getUID());
            marker.setMetaString("parent_uid", linkUid);
            marker.setMetaString("spoi_uid", linkUid); //TODO what is spoi_uid used for?
            if (!marker.getMetaBoolean("paired", false)) {
                setupLinkLinkLine(marker, linkUid);
            }

            if (!FileSystemUtils.isEmpty(linkType)) {
                //Log.d(TAG, "Setting type: " + linkType + ", for marker: " + marker.getUID());
                marker.setMetaString("parent_type", linkType);
            }
        } else if (markerType
                .startsWith(EmergencyDetailHandler.EMERGENCY_TYPE_PREFIX)) {
            //handle Emergency alerts
            //Log.d(TAG, "Processing Emergency marker: " + marker.getUID());
            if (linkUid != null && linkType != null
                    && FileSystemUtils.isEquals(linkRelation, "p-p")) {
                //Log.d(TAG, "Found p-p relation for marker: " + marker.getUID());
                marker.setMetaString("parent_uid", linkUid);
                marker.setMetaString("parent_type", linkType);
                if (!marker.getMetaBoolean("paired", false)) {
                    setupLinkLinkLine(marker, linkUid);
                } //end !paired
            }
        } else {

            // recorded production time and parent producer for all markers.
            // see PlacePointTool
            marker.setMetaString("parent_uid", linkUid);
            marker.setMetaString("parent_type", linkType);
            final String time = detail.getAttribute("production_time");
            final String parent_callsign = detail
                    .getAttribute("parent_callsign");
            if (!FileSystemUtils.isEmpty(time))
                marker.setMetaString("production_time", time);
            if (!FileSystemUtils.isEmpty(parent_callsign))
                marker.setMetaString("parent_callsign", parent_callsign);

            //fall through to legacy code, not sure what all this impacts
            if (linkUid != null && linkType != null) {
                if (linkType.equals("b-m-p-s-p-i")) {
                    //TODO so the link type is b-m-p-s-p-i? maybe intended to use markerType? If so this code is unnecessary
                    //TODO what is spoi_uid used for?
                    marker.setMetaString("spoi_uid", linkUid);
                }

                if (FileSystemUtils.isEquals(linkRelation, "p-s")) {
                    //TODO what is p-s used for?
                    // Check the relation for mocking
                    // find the old marker
                    MapItem oldMapItem = _mapView.getRootGroup().deepFindUID(
                            linkUid);
                    if (oldMapItem != null) {
                        // and if it exists make sure the types match
                        // and if they do then remove the old marker
                        if (linkType.equals(oldMapItem.getType())) {
                            oldMapItem.removeFromGroup();
                        }
                    }
                }
            }
        } //end legacy handler code
        return ImportResult.SUCCESS;
    }

    private void setupLinkLinkLine(MapItem marker, final String uid) {
        if (marker != null) {
            Log.d(TAG, "Requesting link to " + marker.getUID());
            Intent line = new Intent();
            line.setAction("com.atakmap.app.LINK_LINE");
            line.putExtra("firstUID", uid);
            line.putExtra("secondUID", marker.getUID());
            AtakBroadcast.getInstance().sendBroadcast(line);
        }
    }

}
