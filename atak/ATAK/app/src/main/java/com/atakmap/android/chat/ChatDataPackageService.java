
package com.atakmap.android.chat;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.atakmap.android.attachment.DeleteAfterSendCallback;
import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.contact.IndividualContact;
import com.atakmap.android.contact.PluginConnector;
import com.atakmap.android.data.URIContentManager;
import com.atakmap.android.data.URIContentProvider;
import com.atakmap.android.data.URIHelper;
import com.atakmap.android.gui.AlertDialogHelper;
import com.atakmap.android.gui.TileButtonDialog;
import com.atakmap.android.hierarchy.HierarchyListItem;
import com.atakmap.android.hierarchy.HierarchyListReceiver;
import com.atakmap.android.importfiles.ui.ImportManagerFileBrowser;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapActivity;
import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.missionpackage.MapItemSelectTool;
import com.atakmap.android.missionpackage.api.MissionPackageApi;
import com.atakmap.android.missionpackage.file.MissionPackageConfiguration;
import com.atakmap.android.missionpackage.file.MissionPackageContent;
import com.atakmap.android.missionpackage.file.MissionPackageManifest;
import com.atakmap.android.missionpackage.file.NameValuePair;
import com.atakmap.android.missionpackage.file.task.MissionPackageBaseTask;
import com.atakmap.android.missionpackage.ui.HierarchyListUserMissionPackage;
import com.atakmap.android.routes.Route;
import com.atakmap.android.routes.RouteMapComponent;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.AttachmentManager;
import com.atakmap.app.R;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import gov.tak.api.util.Disposable;

class ChatDataPackageService implements Disposable {

    private final static String TAG = "ChatDataPackageService";
    private final static String DATAPACKAGE_SELECT = "com.atakmap.android.chat.ChatDataPackageService.MAP_SELECT";

    private static ChatDataPackageService _instance;

    private final MapView view;
    private final Map<String, ChatDataPackageCallback> pendingCallbacks = new HashMap<>();

    private final Map<String, String> dpUidTwoCallback = new HashMap<>();

    private final GeoChatService.GeoChatDetailHandler datapackageHandler = new GeoChatService.GeoChatDetailHandler() {
        @Override
        public String getDetailName() {
            return "__chatlinks";
        }

        @Override
        public boolean toCotDetail(Bundle bundle, CotEvent cotEvent,
                CotDetail detail) {
            ArrayList<String> chatlinks = bundle
                    .getStringArrayList("chatlinks");
            if (chatlinks != null) {
                CotDetail cd = new CotDetail("__chatlinks");
                for (String chatlink : chatlinks) {
                    try {
                        JSONObject jsonObject = new JSONObject(chatlink);
                        String name = jsonObject.getString("name");
                        String uid = jsonObject.getString("uid");
                        CotDetail item = new CotDetail("item");
                        item.setAttribute("name", name);
                        item.setAttribute("uid", uid);
                        cd.addChild(item);
                    } catch (Exception e) {
                        Log.e(TAG, "error creating a chatlink", e);
                    }
                }
                if (cd.childCount() > 0)
                    detail.addChild(cd);
                return true;
            }
            return false;

        }

        @Override
        public boolean toChatBundle(Bundle bundle, CotEvent event,
                CotDetail detail) {
            List<CotDetail> children = detail.getChildren();
            ArrayList<String> chatlinks = new ArrayList<>();
            for (CotDetail child : children) {
                if (child.getElementName().equals("item")) {
                    String name = child.getAttribute("name");
                    String uid = child.getAttribute("uid");
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("name", name);
                        jsonObject.put("uid", uid);
                        chatlinks.add(jsonObject.toString());
                    } catch (Exception e) {
                        Log.e(TAG, "error creating a name,uid pair" + name + " "
                                + uid);
                    }
                }
            }
            if (!chatlinks.isEmpty()) {
                bundle.putStringArrayList("chatlinks", chatlinks);
            }
            return true;
        }
    };

    /**
     * Callback for actions on the chat data package functionality in TAK
     */
    public interface ChatDataPackageCallback {

        /**
         * Called when the data package send is completed
         * @param uids the list of map items
         * @param files the list of files
         * @param success true if the send was successful.
         */
        void onDataSendComplete(@NonNull List<String> uids,
                @NonNull List<String> files, boolean success);
    }

    private final BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final Bundle extras = intent.getExtras();
            if (action == null || extras == null)
                return;

            final List<String> uids = new ArrayList<>();
            final String[] uidArray = extras.getStringArray("itemUIDs");
            final String mapItemUID = extras.getString("itemUID");
            if (!FileSystemUtils.isEmpty(uidArray))
                uids.addAll(Arrays.asList(uidArray));
            else if (mapItemUID != null)
                uids.add(mapItemUID);

            if (FileSystemUtils.isEmpty(uids))
                return;

            final List<String> filtered = new ArrayList<>();
            for (String uid : uids) {
                String newUID = findMapItem(uid);
                if (!FileSystemUtils.isEmpty(newUID))
                    filtered.add(newUID);
            }
            uids.clear();
            uids.addAll(filtered);

            final String[] contactUIDs = intent.getStringArrayExtra("contacts");
            final String callbackUID = intent.getStringExtra("callbackUID");
            final List<Contact> contacts = new ArrayList<>();
            if (contactUIDs != null)
                for (String cuid : contactUIDs) {
                    Contact c = Contacts.getInstance().getContactByUuid(cuid);
                    if (c != null)
                        contacts.add(c);
                }
            sendUids(uids, contacts, callbackUID);
        }
    };

    synchronized static ChatDataPackageService getInstance(
            final MapView mapView) {
        if (_instance == null) {
            _instance = new ChatDataPackageService(mapView);
        }
        return _instance;
    }

    private ChatDataPackageService(final MapView view) {
        this.view = view;
        GeoChatService.getInstance()
                .registerGeoChatDetailHandler(datapackageHandler);
        AtakBroadcast.getInstance().registerReceiver(br,
                new AtakBroadcast.DocumentedIntentFilter(DATAPACKAGE_SELECT));
    }

    /**
     * Logic for displaying a dialog to allow for the user to pick what type of
     * data they would like to send to the current contact or list of contacts
     * @param contacts the contacts to send the data to
     * @param callback the callback for when the sending is complete
     */
    void sendDataPackage(final List<Contact> contacts,
            final ChatDataPackageCallback callback) {

        final String callbackUID = UUID.randomUUID().toString();
        pendingCallbacks.put(callbackUID, callback);

        final View v = LayoutInflater.from(view.getContext())
                .inflate(R.layout.include_attachment, null);
        final CheckBox attCb = v.findViewById(R.id.include_attachment);
        attCb.setChecked(true);
        attCb.setVisibility(View.GONE);

        final TileButtonDialog d = new TileButtonDialog(view);
        d.setTitle(R.string.select_items);
        d.setCustomView(v);
        d.addButton(R.drawable.select_from_map, R.string.map_select);
        d.addButton(R.drawable.ic_menu_import_file, R.string.file_select);
        d.addButton(R.drawable.select_from_overlay, R.string.overlay_title);

        final List<URIContentProvider> providers = URIContentManager
                .getInstance().getProviders("Data Package");
        for (URIContentProvider provider : providers)
            d.addButton(provider.getIcon(), provider.getName());

        d.setOnClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which < 0) {
                    pendingCallbacks.remove(callbackUID);
                    return;
                }

                // Select from map
                if (which == 0)
                    startMapSelectTool(view.getContext(), contacts,
                            callbackUID);

                // Select from file browser
                else if (which == 1)
                    addFiles(contacts, callbackUID);

                // Select from OM
                else if (which == 2) {
                    Intent i = new Intent(
                            HierarchyListReceiver.MANAGE_HIERARCHY);
                    i.putExtra("hier_userselect_handler",
                            ChatHierarchyListUserMissionPackage.class
                                    .getName());
                    i.putExtra("hier_usertag",
                            ChatDataPackageService.this.toTag(callbackUID,
                                    contacts));
                    AtakBroadcast.getInstance().sendBroadcast(i);
                }

                // Content providers
                else if (which - 3 < providers.size()) {
                    URIContentProvider provider = providers.get(which - 3);
                    provider.addContent("Data Package", new Bundle(),
                            new URIContentProvider.Callback() {
                                @Override
                                public void onAddContent(URIContentProvider p,
                                        List<String> uris) {
                                    if (FileSystemUtils.isEmpty(uris))
                                        return;
                                    sendUris(uris, contacts, callbackUID);
                                }
                            });
                }
            }
        });

        d.show(true);

    }

    private static void startMapSelectTool(final Context context,
            final List<Contact> contacts,
            final String callbackUID) {
        Intent callback = new Intent(DATAPACKAGE_SELECT);

        final String[] uids = new String[contacts.size()];
        for (int i = 0; i < contacts.size(); ++i)
            uids[i] = contacts.get(i).getUid();
        callback.putExtra("contacts", uids);
        callback.putExtra("callbackUID", callbackUID);

        Intent i = new Intent(ToolManagerBroadcastReceiver.BEGIN_TOOL);
        i.putExtra("tool", MapItemSelectTool.TOOL_NAME);
        i.putExtra("title", context.getString(R.string.mission_package_name));
        i.putExtra("disallowKeys", new String[]
        // Disallow TAK user, emergency markers, and non-CoT items
        {
                "atakRoleType", "emergency", "nevercot"
        });
        i.putExtra("disallowTypes", new String[]
        // Disallow SPIs and self marker
        {
                "b-m-p-s-p-i", "self"
        });
        i.putExtra("callback", callback);
        AtakBroadcast.getInstance().sendBroadcast(i);
    }

    private void addFiles(List<Contact> contacts, String callbackUID) {
        final ImportManagerFileBrowser importView = ImportManagerFileBrowser
                .inflate(view);

        importView.setTitle(R.string.select_files_to_import);
        importView.setStartDirectory(
                ATAKUtilities.getStartDirectory(view.getContext()));

        importView.setExtensionTypes(new String[] {
                "*"
        });
        importView.allowDirectorySelect(false);
        AlertDialog.Builder b = new AlertDialog.Builder(view.getContext());
        b.setView(importView);
        b.setNegativeButton(R.string.cancel, null);
        b.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User has selected items and touched OK. Import the data.
                List<File> sFiles = importView.getSelectedFiles();

                if (sFiles.isEmpty()) {
                    Toast.makeText(view.getContext(),
                            R.string.no_import_files,
                            Toast.LENGTH_SHORT).show();
                } else {
                    List<File> selectedFiles = new ArrayList<>(sFiles);

                    Log.d(TAG, "selected " + selectedFiles.size() + " files");
                    sendFiles(selectedFiles, contacts, callbackUID);

                }
            }
        });
        final AlertDialog alert = b.create();

        // Show the dialog
        alert.show();

        AlertDialogHelper.adjustWidth(alert, 0.90d);
    }

    private void sendUids(final List<String> uids, final List<Contact> contacts,
            final String callbackUID) {
        send(null, uids, contacts, callbackUID);
    }

    private void sendFiles(List<File> files, final List<Contact> contacts,
            final String callbackUID) {
        send(files, null, contacts, callbackUID);
    }

    private void sendUris(List<String> uris, List<Contact> contacts,
            String callbackUID) {
        final List<File> files = new ArrayList<>();
        final List<String> uids = new ArrayList<>();

        for (String uri : uris) {
            File f = URIHelper.getFile(uri);
            if (f == null) {
                final MapItem item = URIHelper.getMapItem(view, uri);
                if (item != null) {
                    if (!item.getMetaBoolean("nevercot", false))
                        uids.add(item.getUID());
                }
            } else {
                files.add(f);
            }

        }
        send(files, uids, contacts, callbackUID);
    }

    private void send(final List<File> files,
            final List<String> uids,
            final List<Contact> contacts,
            final String callbackUID) {

        if (files == null && uids == null)
            return;

        List<Contact> filteredContacts = new ArrayList<>();
        for (Contact c : contacts) {
            if (c instanceof IndividualContact) {
                IndividualContact ic = ((IndividualContact) c);
                if (!ic.hasConnector(PluginConnector.CONNECTOR_TYPE)) {
                    filteredContacts.add(ic);
                }
            }
        }

        if (FileSystemUtils.isEmpty(filteredContacts)) {
            final List<String> flist = new ArrayList<>();
            if (files != null) {
                for (File f : files)
                    flist.add(f.getAbsolutePath());
            }
            ChatDataPackageService.ChatDataPackageCallback cb = pendingCallbacks
                    .remove(callbackUID);
            if (cb != null)
                cb.onDataSendComplete(uids == null ? new ArrayList<>() : uids,
                        flist, true);

            return;

        }

        MissionPackageManifest manifest = MissionPackageApi
                .CreateTempManifest("chat-transfer", true, true, null);

        dpUidTwoCallback.put(manifest.getUID(), callbackUID);

        if (files != null) {
            for (File file : files) {
                manifest.addFile(file, null);
            }
        }
        if (uids != null) {
            for (String uid : uids) {
                MapItem mi = view.getMapItem(uid);
                if (mi != null) {
                    manifest.addMapItem(uid);
                    List<File> attachments = AttachmentManager
                            .getAttachments(uid);
                    for (File attachment : attachments) {
                        manifest.addFile(attachment, uid);
                    }
                }
            }
        }

        MissionPackageApi.Send(view.getContext(), manifest,
                ChatDeleteAfterSendCallback.class,
                filteredContacts.toArray(new Contact[0]));
    }

    public static class ChatHierarchyListUserMissionPackage
            extends HierarchyListUserMissionPackage {
        @Override
        public boolean processUserSelections(Context context,
                Set<HierarchyListItem> selected) {
            AtakBroadcast.getInstance().sendBroadcast(new Intent(
                    HierarchyListReceiver.CLOSE_HIERARCHY));
            List<String> uids = new ArrayList<>();
            discoverUIDs(selected, uids);
            Pair<String, List<Contact>> vals = _instance.fromTag(getTag());

            if (FileSystemUtils.isEmpty(uids))
                return true;

            _instance.send(null, uids, vals.second, vals.first);
            return true;
        }

        private void discoverUIDs(Set<HierarchyListItem> items,
                List<String> uids) {
            for (HierarchyListItem item : items) {
                if (item.isChildSupported()) {
                    Set<HierarchyListItem> children = new HashSet<>();
                    for (int i = 0; i < item.getChildCount(); ++i)
                        children.add(item.getChildAt(i));

                    discoverUIDs(children, uids);
                } else {
                    uids.add(item.getUID());
                }
            }
        }
    }

    public static class ChatDeleteAfterSendCallback
            extends DeleteAfterSendCallback {

        @Override
        public void onMissionPackageTaskComplete(MissionPackageBaseTask task,
                boolean success) {
            final String mpUID = task.getManifest().getUID();
            final String callbackUID = _instance.dpUidTwoCallback.remove(mpUID);
            final ChatDataPackageCallback cb = _instance.pendingCallbacks
                    .remove(callbackUID);
            if (cb != null) {
                MissionPackageManifest missionPackage = task.getManifest();
                List<String> uids = new ArrayList<>();
                List<String> files = new ArrayList<>();

                // go through the CoT items first
                for (MissionPackageContent mc : missionPackage.getContents()
                        .getContents()) {
                    try {
                        if (mc.isCoT()) {
                            final NameValuePair pathPair = mc.getParameter(
                                    MissionPackageConfiguration.PARAMETER_UID);
                            uids.add(pathPair.getValue());
                        }
                    } catch (Exception ignored) {
                    }
                }
                // count the files not attached to to CoT items second
                for (MissionPackageContent mc : missionPackage.getContents()
                        .getContents()) {
                    try {
                        if (!mc.isCoT()) {
                            NameValuePair pathPair = mc.getParameter(
                                    MissionPackageConfiguration.PARAMETER_UID);
                            if (pathPair != null && pathPair.getValue() != null
                                    &&
                                    uids.contains(pathPair.getValue()))
                                continue;

                            pathPair = mc.getParameter(
                                    MissionPackageContent.PARAMETER_LOCALPATH);
                            files.add(pathPair.getValue());
                        }
                    } catch (Exception ignored) {
                    }

                }

                cb.onDataSendComplete(uids, files, success);
            }
            super.onMissionPackageTaskComplete(task, success);
        }
    }

    private String findMapItem(String mapItemUID) {
        MapItem item = view.getRootGroup()
                .deepFindUID(mapItemUID);
        if (item == null)
            return null;

        // TODO could clean this up by pushing it into IMissionPackageEventHandler
        // Here we check some special cases to include more/expected data rather than default
        // map item that was touched by the user
        // if this is a route checkpoint, include entire route, rather than just this checkpoint
        String itemType = item.getType();
        if (item instanceof PointMapItem
                && "b-m-p-w".equals(itemType)) {
            Log.d(TAG,
                    "Processing check point selected by user via map, looking for parent route... "
                            + mapItemUID);

            Route route = getRouteWithPoint(view,
                    (PointMapItem) item);
            if (route != null) {
                String routeUID = route.getUID();
                if (!FileSystemUtils.isEmpty(routeUID)) {
                    Log.d(TAG, "Using parent route for check point: "
                            + mapItemUID);
                    mapItemUID = routeUID;
                }
            }
        } else if (item.hasMetaValue("shapeUID")) {
            // Use shape UID - all shape markers should have this set
            mapItemUID = item.getMetaString("shapeUID", mapItemUID);
        } else if (item instanceof PointMapItem
                && (itemType.contains("center_")
                        || itemType.equals("shape_marker") || itemType
                                .contains("-c-c"))) {

            // If shapeUID isn't set for some reason rely on shapeName

            // See if this is a center point for a circle/rectangle or a free-form shape,
            // attempt to find the actual shape
            // Assume shape has same "shapeName" and is in "Drawing Objects" map group
            String shapeName = item.getTitle();
            Log.d(TAG, "Processing center point selected by user ("
                    + mapItemUID
                    + ") via map, looking for parent shape: " + shapeName);

            if (!FileSystemUtils.isEmpty(shapeName)) {
                MapGroup group = view.getRootGroup().findMapGroup(
                        "Drawing Objects");
                if (group != null) {
                    // this seems to pick up circle/rectangle
                    MapItem shape = group.findItem(
                            "shapeName", shapeName);
                    if (shape == null) {
                        // and this seems to get the free-form shapes
                        shape = group.findItem("title",
                                shapeName);
                    }

                    if (shape != null) {
                        String shapeUID = shape.getUID();
                        if (!FileSystemUtils.isEmpty(shapeUID)) {
                            Log.d(TAG,
                                    "Using parent shape for center point: "
                                            + mapItemUID);
                            mapItemUID = shapeUID;
                        }
                    }
                }
            }
        }
        return mapItemUID;
    }

    public static Route getRouteWithPoint(MapView mapView, PointMapItem item) {
        if (!(mapView.getContext() instanceof MapActivity)) {
            Log.w(TAG, "Unable to find route without MapActivity");
            return null;
        }

        MapActivity activity = (MapActivity) mapView.getContext();
        MapComponent mc = activity.getMapComponent(RouteMapComponent.class);
        if (!(mc instanceof RouteMapComponent)) {
            Log.w(TAG, "Unable to find route without RouteMapComponent");
            return null;
        }

        RouteMapComponent routeComponent = (RouteMapComponent) mc;
        return routeComponent.getRouteMapReceiver().getRouteWithPoint(item);
    }

    @Override
    public void dispose() {
        GeoChatService.getInstance()
                .unregisterGeoChatDetailHandler(datapackageHandler);
        AtakBroadcast.getInstance().unregisterReceiver(br);
        pendingCallbacks.clear();
        _instance = null;
    }

    private String toTag(String callbackUID, List<Contact> contacts) {
        StringBuilder sb = new StringBuilder();
        sb.append(callbackUID);

        for (Contact c : contacts) {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(c.getUid());
        }
        return sb.toString();
    }

    private Pair<String, List<Contact>> fromTag(String value) {
        List<Contact> contacts = new ArrayList<>();
        String[] uids = value.split(",");
        String tag = null;
        for (String uid : uids) {
            if (tag == null)
                tag = uid;
            else {
                Contact c = Contacts.getInstance().getContactByUuid(uid);
                if (c != null)
                    contacts.add(c);
            }
        }
        return new Pair<>(tag, contacts);
    }
}
