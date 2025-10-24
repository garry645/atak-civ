
package com.atakmap.android.cot.importer;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.atakmap.android.importexport.AbstractImporter;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.comms.CommsMapComponent.ImportResult;
import com.atakmap.coremap.log.Log;
import com.atakmap.util.zip.IoUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import gov.tak.api.cot.detail.RolesManager;

/**
 * Importer for user icon markers
 */
public final class CustomRoleImporter extends AbstractImporter {
    public final static String JSON_CSV = "text/json";
    public final static String CONTENT_TYPE = "Custom Role JSON";
    private static final String TAG = "ImportRolesJSONSort";
    private final static String ROLESMATCH = "\"roles\"";
    public final static String IMPORT_INDEX_KEY = "customRolesImports";

    private final Context context;
    private final RolesManager rolesManager;

    private Set<String> imports = new HashSet<>();

    /**
     *
     * @param rolesManager  The roles manager
     */
    public CustomRoleImporter(Context context, RolesManager rolesManager) {
        super(CONTENT_TYPE);

        this.context = context;
        this.rolesManager = rolesManager;

        try {
            JSONArray importIndex = new JSONArray(AtakPreferences
                    .getInstance(context).get(IMPORT_INDEX_KEY, "[]"));
            for (int i = 0; i < importIndex.length(); i++)
                imports.add(importIndex.getString(i));
        } catch (JSONException e) {
            AtakPreferences.getInstance(context).remove(IMPORT_INDEX_KEY);
        }
    }

    @Override
    public Set<String> getSupportedMIMETypes() {
        return Collections.singleton(JSON_CSV);
    }

    @Override
    public CommsMapComponent.ImportResult importData(InputStream inputStream,
            String s, Bundle bundle) throws IOException {

        return rolesManager.importRoles(inputStream) ? ImportResult.SUCCESS
                : ImportResult.FAILURE;
    }

    @Override
    public synchronized CommsMapComponent.ImportResult importData(Uri uri,
            String s, Bundle bundle) throws IOException {

        String path = uri.getPath();
        if (path == null)
            return ImportResult.FAILURE;
        if (imports.add(path)) {
            final CommsMapComponent.ImportResult result = importUriAsStream(
                    this, uri, s, bundle);
            if (result != ImportResult.SUCCESS) {
                imports.remove(s);
            } else {
                try {
                    JSONArray importIndex = new JSONArray(AtakPreferences
                            .getInstance(context).get(IMPORT_INDEX_KEY, "[]"));
                    importIndex.put(path);
                    AtakPreferences.getInstance(context).set(IMPORT_INDEX_KEY,
                            importIndex.toString());
                } catch (JSONException e) {
                    Log.w(TAG, "Failed to update Custom Roles import index", e);
                }
            }
            return result;
        } else {
            return ImportResult.IGNORE;
        }
    }

    public static boolean isRoles(InputStream stream) {
        try {
            // read first few hundred bytes and search for known roles strings
            char[] buffer = new char[8192];
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    stream));

            int numRead;
            try {
                numRead = reader.read(buffer);
            } finally {
                IoUtils.close(reader);
            }

            if (numRead < 1) {
                Log.d(TAG, "Failed to read .json stream");
                return false;
            }

            String content = String.valueOf(buffer, 0, numRead);
            boolean match = content.contains(ROLESMATCH);
            if (!match) {
                Log.d(TAG, "Failed to match roles json content");
            } else if (numRead < buffer.length) {
                // try to verify JSON
                new JSONObject(content);
            }

            return match;
        } catch (Exception e) {
            Log.d(TAG, "Failed to match roles.json", e);
            return false;
        }
    }
}
