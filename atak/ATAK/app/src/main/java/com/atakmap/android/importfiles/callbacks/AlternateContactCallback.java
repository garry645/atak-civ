package com.atakmap.android.importfiles.callbacks;

import android.content.Context;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProvider;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import gov.tak.api.importfiles.ImportResolver;

final class AlternateContactCallback implements ImportResolver.BeginImportListener {
    private static final String TAG = "AlternateContactCallback";

    private final static String CONTACT_MATCH = "::ALTERNATE CONTACT v2";
    private static final String COMMENT = "::";
    private static final String SPLIT = ",";
    private static final List<String> IGNORE;

    private final Context _context;

    static {
        IGNORE = new ArrayList<>();
        IGNORE.add("NA");
        IGNORE.add("N/A");
    }

    public AlternateContactCallback() {
        _context = MapView.getMapView().getContext();
    }

    @Override
    public boolean onBeginImport(File file, EnumSet<ImportResolver.SortFlags> sortFlags, Object opaque) {
        try {
            importContact(file);
        } catch (IOException e) {
            Log.w(TAG,
                    "Failed to parse contact info: " + file.getAbsolutePath(),
                    e);
        }

        // remove the .csv file from source location so it won't be reimported next time ATAK starts
        File atakdata = new File(_context.getCacheDir(),
                FileSystemUtils.ATAKDATA);
        if (file.getAbsolutePath().startsWith(atakdata.getAbsolutePath())
                && IOProviderFactory.delete(file, IOProvider.SECURE_DELETE))
            Log.d(TAG,
                    "Deleted imported contact info: " + file.getAbsolutePath());

        return true;
    }

    private void importContact(File file) throws IOException {
        String myCallsign = MapView.getMapView().getDeviceCallsign()
                .toLowerCase(LocaleUtil.getCurrent());
        AtakPreferences prefs = AtakPreferences
                .getInstance(MapView.getMapView().getContext());

        try (Reader r = IOProviderFactory.getFileReader(file);
             BufferedReader br = new BufferedReader(r)) {
            String line;
            String[] parse;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(COMMENT)) {
                    Log.d(TAG, "Skipping comment: " + line);
                    continue;
                }

                parse = line.split(SPLIT);
                if (parse.length != 5) {
                    Log.w(TAG, "Invalid contact format. Skipping.");
                    continue;
                }

                String callsign = parse[0];
                String includePhone = parse[1];
                String voip = parse[2];
                String email = parse[3];
                String xmpp = parse[4];

                if (FileSystemUtils.isEmpty(callsign)) {
                    Log.w(TAG, "Invalid callsign. Skipping.");
                    continue;
                }

                callsign = callsign.toLowerCase(LocaleUtil.getCurrent());
                if (!FileSystemUtils.isEquals(myCallsign, callsign)) {
                    Log.w(TAG, "Not my callsign. Skipping: " + callsign);
                    continue;
                }

                if (!FileSystemUtils.isEmpty(includePhone)
                        && !IGNORE.contains(includePhone)) {
                    boolean bIncludePhone = Boolean.parseBoolean(includePhone);
                    prefs.set("saHasPhoneNumber", bIncludePhone);
                    Log.d(TAG, "Setting includePhone=" + bIncludePhone
                            + ", for callsign: " + myCallsign);
                }

                if (!FileSystemUtils.isEmpty(voip) && !IGNORE.contains(voip)) {
                    //set VoIP number, and use "manual entry"
                    prefs.set("saSipAddress", voip);
                    prefs.set("saSipAddressAssignment",
                            _context.getString(
                                    R.string.voip_assignment_manual_entry));
                    Log.d(TAG, "Setting sip address=" + voip
                            + ", for callsign: " + myCallsign);
                }

                if (!FileSystemUtils.isEmpty(email)
                        && !IGNORE.contains(email)) {
                    prefs.set("saEmailAddress", email);
                    Log.d(TAG, "Setting email address=" + email
                            + ", for callsign: " + myCallsign);
                }

                if (!FileSystemUtils.isEmpty(xmpp) && !IGNORE.contains(xmpp)) {
                    prefs.set("saXmppUsername", xmpp);
                    Log.d(TAG, "Setting xmpp username=" + xmpp
                            + ", for callsign: " + myCallsign);
                }
            }
        }
    }
}
