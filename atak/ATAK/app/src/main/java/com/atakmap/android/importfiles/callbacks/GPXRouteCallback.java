package com.atakmap.android.importfiles.callbacks;

import android.content.Intent;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.routes.RouteMapReceiver;

import gov.tak.api.importfiles.ImportResolver;

final class GPXRouteCallback implements ImportResolver.FileSortedListener {
    @Override
    public void onFileSorted(ImportResolver.FileSortedNotification notification) {
        Intent i = new Intent(RouteMapReceiver.ROUTE_IMPORT);
        i.putExtra("filename", notification.getSrcUri());
        AtakBroadcast.getInstance().sendBroadcast(i);
    }
}
