package com.atakmap.android.importexport;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.atakmap.android.bluetooth.BluetoothDevicesConfig;
import com.atakmap.android.favorites.FavoriteListAdapter;
import com.atakmap.android.importfiles.sort.ImportTXTSort;
import com.atakmap.android.importfiles.ui.ImportManagerView;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.layers.LayersManagerBroadcastReceiver;
import com.atakmap.android.layers.LayersMapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.util.NotificationUtil;
import com.atakmap.android.wfs.WFSImporter;
import com.atakmap.app.R;
import com.atakmap.app.preferences.GeocoderPreferenceFragment;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.map.layer.feature.wfs.XMLWFSSchemaHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import gov.tak.api.importfiles.ImportTXTResolver;

final class TxtImportSignatures {
    private static final String TAG = "TxtImportSignatures";

    private static volatile TxtImportSignatures instance;

    public static TxtImportSignatures getInstance() {
        if (instance == null) {
            synchronized (TxtImportSignatures.class) {
                if (instance == null) {
                    instance = new TxtImportSignatures();
                    ImportTXTResolver.addSignature("<remoteResources", ImportManagerView.XML_FOLDER,
                            null);
                    ImportTXTResolver.addSignature("<NominatimProperties",
                            GeocoderPreferenceFragment.ADDRESS_DIR, geocoderaction);
                    ImportTXTResolver.addSignature("<devices", BluetoothDevicesConfig.DIRNAME, null);
                    ImportTXTResolver.addSignature(XMLWFSSchemaHandler.WFS_CONFIG_ROOT, "wfs", wfsaction); // ATAK/wfs
                    ImportTXTResolver.addSignature(FavoriteListAdapter.FAVS, FavoriteListAdapter.DIRNAME,
                            favaction);
                }
            }
        }
        return instance;
    }

    ImportTXTResolver.TxtType.AfterAction wmsaction = new ImportTXTResolver.TxtType.AfterAction() {
        @Override
        public void doAction(File dst) {
            Log.d(TAG, "notify that a new wms file was imported: " + dst);
            Intent loadwmsintent = new Intent(
                    ImportExportMapComponent.ACTION_IMPORT_DATA);
            loadwmsintent.putExtra(ImportReceiver.EXTRA_CONTENT,
                    LayersMapComponent.IMPORTER_CONTENT_TYPE);
            loadwmsintent.putExtra(ImportReceiver.EXTRA_MIME_TYPE,
                    LayersMapComponent.IMPORTER_DEFAULT_MIME_TYPE);
            loadwmsintent.putExtra(ImportReceiver.EXTRA_URI,
                    Uri.fromFile(dst).toString());
            AtakBroadcast.getInstance().sendBroadcast(loadwmsintent);
        }
    };

    private final static ImportTXTResolver.TxtType.AfterAction geocoderaction = new ImportTXTResolver.TxtType.AfterAction() {
        @Override
        public void doAction(File dst) {
            Log.d(TAG, "notify that a new geocoder file was imported: " + dst);
            GeocoderPreferenceFragment.load(dst);
        }
    };

    private final static ImportTXTResolver.TxtType.AfterAction wfsaction = new ImportTXTResolver.TxtType.AfterAction() {
        @Override
        public void doAction(File dst) {
            Log.d(TAG, "notify that a new wfs file was imported: " + dst);
            Intent loadwmsintent = new Intent(
                    ImportExportMapComponent.ACTION_IMPORT_DATA);
            loadwmsintent.putExtra(ImportReceiver.EXTRA_CONTENT,
                    WFSImporter.CONTENT);
            loadwmsintent.putExtra(ImportReceiver.EXTRA_MIME_TYPE,
                    WFSImporter.MIME_XML);
            loadwmsintent.putExtra(ImportReceiver.EXTRA_URI,
                    Uri.fromFile(dst).toString());

            AtakBroadcast.getInstance().sendBroadcast(loadwmsintent);

        }
    };

    private final static ImportTXTResolver.TxtType.AfterAction favaction = new ImportTXTResolver.TxtType.AfterAction() {
        @Override
        public void doAction(File dst) {
            Log.d(TAG, "notify that a fav file was imported: " + dst);

            //file has been moved, import each fav in the file
            List<FavoriteListAdapter.Favorite> favs = FavoriteListAdapter
                    .loadList(dst.getAbsolutePath());
            if (FileSystemUtils.isEmpty(favs)) {
                Log.w(TAG, "favaction none loaded");
                return;
            }

            Intent intent = new Intent(
                    LayersManagerBroadcastReceiver.ACTION_ADD_FAV);
            intent.putExtra("favorites", favs
                    .toArray(new FavoriteListAdapter.Favorite[0]));
            AtakBroadcast.getInstance().sendBroadcast(intent);

            //intent to view one of the favs
            FavoriteListAdapter.Favorite fav = favs.get(0);
            if (fav != null && !FileSystemUtils.isEmpty(fav.layer)
                    && !FileSystemUtils.isEmpty(fav.selection)) {
                final Context context = MapView.getMapView().getContext();
                intent = new Intent(
                        LayersManagerBroadcastReceiver.ACTION_VIEW_FAV);
                intent.putExtra("favorite", fav);
                NotificationUtil.getInstance().postNotification(
                        R.drawable.spi1_icon,
                        context.getString(R.string.fav_notif_title, fav.title),
                        context.getString(R.string.fav_notif_msg, fav.title),
                        context.getString(R.string.fav_notif_msg, fav.title),
                        intent);
            }
        }
    };
}
