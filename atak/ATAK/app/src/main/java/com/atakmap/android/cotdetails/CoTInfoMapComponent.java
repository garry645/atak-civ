
package com.atakmap.android.cotdetails;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.atakmap.android.attachment.AttachmentBroadcastReceiver;
import com.atakmap.android.attachment.AttachmentGalleryProvider;
import com.atakmap.android.attachment.AttachmentMapOverlay;
import com.atakmap.android.attachment.export.AttachmentExportMarshal;
import com.atakmap.android.cotdetails.sensor.SensorDetailsReceiver;
import com.atakmap.android.data.URIContentManager;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.icons.Icon2525cTypeResolver;
import com.atakmap.android.importexport.ExporterManager;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.ipc.DocumentedExtra;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.tools.SensorFOVTool;
import com.atakmap.app.R;
import com.atakmap.app.preferences.ToolsPreferenceFragment;
import com.atakmap.coremap.filesystem.FileOpsExecUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.os.FileObserver;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import gov.tak.api.cot.CotUtils;
import gov.tak.api.symbology.Status;
import gov.tak.platform.symbology.SymbologyProvider;

/**
 * Provides for the capability to view arbitrary CoT derived markers from 
 * the map.  
 */
public class CoTInfoMapComponent extends DropDownMapComponent {

    private static final String TAG = "CoTInfoMapComponent";

    protected CoTInfoBroadcastReceiver cibr;
    private SensorDetailsReceiver sensorReceiver;
    private SensorFOVTool _fovTool;


    private AttachmentBroadcastReceiver abr;
    private AttachmentMapOverlay _overlay;
    private AttachmentGalleryProvider _provider;

    private MapView mapView;
    private AttachmentWatcher attachmentWatcher;
    private FileObserver fObserver;
    private final Map<String, MapItem> markerAttachment = new HashMap<>();

    private final List<AttachmentEventListener> _listeners = new ArrayList<>();

    static private CoTInfoMapComponent _instance;
    private ExtendedInfoView symbolStatus;

    @Override
    public void onCreate(Context context, Intent intent, MapView view) {
        super.onCreate(context, intent, view);
        mapView = view;

        cibr = new CoTInfoBroadcastReceiver(view);
        abr = new AttachmentBroadcastReceiver(view);

        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction(CoTInfoBroadcastReceiver.COTINFO_DETAILS,
                "mechanism for bringing up the cot information details based on an existing marker unique identifier",
                new DocumentedExtra[] {
                        new DocumentedExtra("targetUID",
                                "the unique identifier for the marker to be used when opening up the cot information view")
                });
        filter.addAction("com.atakmap.android.cotdetails.COTINFO_SETTYPE");
        registerDropDownReceiver(cibr, filter);

        filter = new DocumentedIntentFilter();
        filter.addAction(AttachmentBroadcastReceiver.ATTACHMENT_RECEIVED);
        filter.addAction(AttachmentBroadcastReceiver.SEND_ATTACHMENT);
        filter.addAction(AttachmentBroadcastReceiver.GALLERY);
        registerDropDownReceiver(abr, filter);

        sensorReceiver = new SensorDetailsReceiver(view);
        registerDropDownReceiver(sensorReceiver,
                sensorReceiver.getIntentFilter());

        _fovTool = new SensorFOVTool(MapView.getMapView(), null);


        _instance = this;

        //now that _instance is set, create attachment overlay
        _overlay = new AttachmentMapOverlay(view);
        addOverlay(view, _overlay);

        URIContentManager.getInstance().registerProvider(
                _provider = new AttachmentGalleryProvider(view));

        //register Overlay Manager exporter
        ExporterManager.registerExporter(
                context.getString(R.string.media),
                R.drawable.camera,
                AttachmentExportMarshal.class);
        setupWatcher();

        CoordinateEntryPreferenceFragment cepf = new CoordinateEntryPreferenceFragment();

        ToolsPreferenceFragment.register(
                new ToolsPreferenceFragment.ToolPreference(
                        context.getString(R.string.coordEntryPreference),
                        context.getString(
                                R.string.coordEntryPreference_summary),
                        "coordEntryPreferences",
                        context.getResources().getDrawable(
                                R.drawable.tabs),
                        cepf));
    }

    static public CoTInfoMapComponent getInstance() {
        return _instance;
    }

    private class AttachmentWatcher implements
            MapEventDispatcher.MapEventDispatchListener {
        @Override
        public void onMapEvent(final MapEvent event) {
            final String etype = event.getType();
            final MapItem mi = event.getItem();
            final boolean removed = etype.equals(MapEvent.ITEM_REMOVED);
            final boolean added = etype.equals(MapEvent.ITEM_ADDED);
            if (added || removed) {
                FileOpsExecUtils.getGeneralFileOpsExecutor()
                        .submit(new Runnable() {
                            @Override
                            public void run() {
                                final File dir = FileSystemUtils
                                        .getItem("attachments");
                                File f = new File(dir,
                                        FileSystemUtils
                                                .sanitizeFilename(mi.getUID()));
                                if (IOProviderFactory.exists(f) && added) {
                                    addAttachment(mi);
                                } else {
                                    removeAttachment(mi);
                                }
                            }
                        });
            }
        }
    }

    char statusToCode(Status status) {
        switch(status) {
            case PlannedAnticipatedSuspect:
                return 'A';
            case PresentFullyCapable:
                return 'C';
            case PresentDamaged:
                return 'D';
            case PresentDestroyed:
                return 'X';
            case PresentFullToCapacity:
                return 'F';
            case Present:
            default:
                return 'P';
        }
    }

    Status codeToStatus(char code) {
        switch(code) {
            case 'A':
                return Status.PlannedAnticipatedSuspect;
            case 'C':
                return Status.PresentFullyCapable;
            case 'D':
                return Status.PresentDamaged;
            case 'X':
                return Status.PresentDestroyed;
            case 'F':
                return Status.PresentFullToCapacity;
            case 'P':
            default:
                return Status.Present;

        }
    }

    private void setupWatcher() {
        attachmentWatcher = new AttachmentWatcher();
        mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.ITEM_ADDED, attachmentWatcher);
        mapView.getMapEventDispatcher().addMapEventListener(
                MapEvent.ITEM_REMOVED, attachmentWatcher);

        fObserver = new FileObserver(FileSystemUtils.getItem("attachments")
                .toString()) {
            @Override
            public void onEvent(final int event, final String path) {
                switch (event & FileObserver.ALL_EVENTS) {
                    case FileObserver.CREATE:
                        //Log.d(TAG, "wrap = " + path);
                        MapItem ami = mapView.getMapItem(path);
                        if (ami != null)
                            addAttachment(ami);
                        break;
                    case FileObserver.DELETE:
                        //Log.d(TAG, "remove = " + path);
                        MapItem dmi = mapView.getMapItem(path);
                        if (dmi != null)
                            removeAttachment(dmi);
                        break;
                    default:
                        break;
                }
            }
        };

        fObserver.startWatching();

        symbolStatus = new ExtendedInfoView(mapView.getContext()) {
            private View view;
            private Spinner spinner;
            private final String[] statusValues = getContext().getResources()
                    .getStringArray(R.array.symbol_status_values);

            private int indexOf(char c, String[] array) {
                for (int i = 0; i < array.length; ++i) {
                    if (c == array[i].charAt(0))
                        return i;
                }
                return -1;
            }

            @Override
            public void setMarker(PointMapItem pointMapItem) {
                if (view == null) {
                    view = LayoutInflater.from(mapView.getContext())
                            .inflate(R.layout.marker_status, null);
                    spinner = view.findViewById(R.id.markerStatusSpinner);

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            view.getContext(),
                            android.R.layout.simple_spinner_item,
                            view.getResources().getStringArray(
                                    R.array.symbol_status_names));
                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item);

                    spinner.setAdapter(adapter);
                    addView(view);
                }
                String type = pointMapItem.getType();

                final String sidc = pointMapItem.getMetaString("milsym", null);

                final String fsidc;
                if (sidc == null && type != null) {
                    final String mil2525c = CotUtils.mil2525cFromCotType(type);
                    fsidc = (mil2525c != null) ? mil2525c.toUpperCase(Locale.US) : null;
                } else {
                    fsidc = sidc;
                }

                // https://issues.tak.gov/browse/ATAK-19242
                // need to swap out the 4th position with the appropriate code
                if (fsidc != null) {
                    view.setVisibility(View.VISIBLE);
                    Status status = SymbologyProvider.getStatus(fsidc);

                    spinner.setOnItemSelectedListener(null);
                    int i = indexOf(statusToCode(status), statusValues);
                    if (i < 0) {
                        i = 1;
                    }
                    spinner.setSelection(i);
                    spinner.setOnItemSelectedListener(
                            new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(
                                        AdapterView<?> adapterView, View view,
                                        int i, long l) {
                                    Status s = codeToStatus(statusValues[i].charAt(0));
                                    String sym = SymbologyProvider.setStatus(fsidc, s);
                                    if(sym != null) {

                                        pointMapItem.setMetaString("milsym", sym);
                                        pointMapItem.refresh(
                                                mapView.getMapEventDispatcher(),
                                                null, this.getClass());
                                        pointMapItem.persist(
                                                mapView.getMapEventDispatcher(),
                                                null, this.getClass());
                                    }
                                }

                                @Override
                                public void onNothingSelected(
                                        AdapterView<?> adapterView) {

                                }
                            });
                } else {
                    view.setVisibility(View.GONE);
                }
            }
        };
        register(symbolStatus);
    }

    /**
     * Register additional views within the CotInfoView 
     * @param eiv the extended info view that is related to the CoTInfoView.
     */
    public void register(final ExtendedInfoView eiv) {
        // can be null if shutting down
        if (cibr != null)
            cibr.register(eiv);
    }

    public void unregister(final ExtendedInfoView eiv) {
        // can be null if shutting down
        if (cibr != null)
            cibr.unregister(eiv);
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        // cleans up all receivers and overlays
        super.onDestroyImpl(context, view);

        if (cibr != null) {
            cibr.dispose();
            cibr = null;
        }

        if (abr != null) {
            abr.dispose();
            abr = null;
        }
        if (attachmentWatcher != null) {
            mapView.getMapEventDispatcher().removeMapEventListener(
                    MapEvent.ITEM_ADDED, attachmentWatcher);
            mapView.getMapEventDispatcher().removeMapEventListener(
                    MapEvent.ITEM_REMOVED, attachmentWatcher);
            attachmentWatcher = null;
        }
        if (fObserver != null) {
            fObserver.stopWatching();
            fObserver = null;
        }

        URIContentManager.getInstance().unregisterProvider(_provider);
        ExporterManager.unregisterExporter(context.getString(R.string.media));
    }

    private void addAttachment(MapItem item) {
        synchronized (markerAttachment) {
            markerAttachment.put(item.getUID(), item);
        }
        synchronized (_listeners) {
            for (AttachmentEventListener ael : _listeners)
                ael.onAttachmentAdded(item);
        }
    }

    private void removeAttachment(MapItem item) {
        MapItem removed;
        synchronized (markerAttachment) {
            removed = markerAttachment.remove(item.getUID());
        }
        if (removed != null) {
            synchronized (_listeners) {
                for (AttachmentEventListener ael : _listeners)
                    ael.onAttachmentRemoved(item);
            }
        }
    }

    public void addAttachmentListener(AttachmentEventListener ael) {
        synchronized (_listeners) {
            if (!_listeners.contains(ael))
                _listeners.add(ael);
        }
    }

    public void removeAttachmentListener(AttachmentEventListener ael) {
        synchronized (_listeners) {
            _listeners.remove(ael);
        }
    }

    public interface AttachmentEventListener {
        void onAttachmentAdded(MapItem item);

        void onAttachmentRemoved(MapItem item);
    }

}
