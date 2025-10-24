package com.atakmap.android.importfiles.callbacks;

import android.content.Intent;

import com.atakmap.android.icons.IconsMapAdapter;
import com.atakmap.android.ipc.AtakBroadcast;

import gov.tak.api.importfiles.ImportResolver;

final class UserIconSetCallback implements ImportResolver.FileSortedListener {
    @Override
    public void onFileSorted(ImportResolver.FileSortedNotification notification) {
        Intent loadIntent = new Intent();
        loadIntent.setAction(IconsMapAdapter.ADD_ICONSET);
        loadIntent.putExtra("filepath", notification.getSrcUri());
        AtakBroadcast.getInstance().sendBroadcast(loadIntent);
    }
}
