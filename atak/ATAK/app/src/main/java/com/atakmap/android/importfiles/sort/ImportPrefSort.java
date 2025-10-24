
package com.atakmap.android.importfiles.sort;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Pair;

import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.app.ATAKApplication;
import com.atakmap.app.R;
import com.atakmap.app.preferences.PreferenceControl;
import com.atakmap.comms.http.HttpUtil;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.xml.XMLUtils;
import com.atakmap.net.AtakAuthenticationCredentials;
import com.atakmap.util.zip.IoUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * Sorts ATAK Preferences Files
 *
 * @deprecated use {@link com.atakmap.android.importfiles.sort.ImportPrefResolver}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ImportPrefSort extends ImportResolver {

    private static final String TAG = "ImportPrefSort";

    public static final String CONTENT_TYPE = "ATAK Preferences";

    //require this
    private final static String MATCH_XML = "<preferences";
    //and one of these
    private final static String MATCH_XML1 = "<preference key";
    private final static String MATCH_XML2 = "<entry key";

    private final Context _context;

    private final List<String> entriesToDelete = Arrays.asList(
            AtakAuthenticationCredentials.TYPE_clientPassword,
            AtakAuthenticationCredentials.TYPE_caPassword,
            "certificateLocation",
            "caLocation", "networkMeshKey");
    private boolean containsEntryToDelete = false;
    private final List<String> prefFilesToCleanup = new ArrayList<>();

    public ImportPrefSort(Context context, boolean validateExt,
            boolean copyFile) {
        super(".pref", PreferenceControl.DIRNAME,
                context.getString(R.string.preference_file),
                context.getDrawable(R.drawable.ic_menu_settings));
        _context = context;
    }

    @Override
    public boolean match(File file) {
        if (!super.match(file))
            return false;

        // it is a .pref, now lets see if content inspection passes
        try (InputStream is = IOProviderFactory.getInputStream(file)) {
            return isPreference(is);
        } catch (IOException e) {
            Log.e(TAG, "Failed to match Pref file: " + file.getAbsolutePath(),
                    e);
            return false;
        }
    }

    public boolean isPreference(InputStream stream) {
        try {
            char[] buffer = new char[8192];
            BufferedReader reader = null;
            int numRead;

            try {
                reader = new BufferedReader(new InputStreamReader(
                        stream));
                numRead = reader.read(buffer);
            } finally {
                IoUtils.close(reader);
            }

            if (numRead < 1) {
                Log.d(TAG, "Failed to read .pref stream");
                return false;
            }

            String content = String.valueOf(buffer, 0, numRead);
            boolean match = content.contains(MATCH_XML)
                    &&
                    (content.contains(MATCH_XML1) || content
                            .contains(MATCH_XML2));
            if (!match) {
                Log.d(TAG, "Failed to match content from .pref: ");
            }

            // record whether the current file contains entries that need to be scrubbed
            containsEntryToDelete = false;
            for (String entryToDelete : entriesToDelete) {
                if (content.contains(entryToDelete)) {
                    containsEntryToDelete = true;
                    break;
                }
            }

            return match;
        } catch (Exception e) {
            Log.d(TAG, "Failed to match .pref", e);
            return false;
        }
    }

    @Override
    public boolean beginImport(File file, Set<SortFlags> flags) {
        flags = new HashSet<>(flags);
        flags.add(SortFlags.IMPORT_COPY);
        return super.beginImport(file, flags);
    }

    @Override
    protected void onFileSorted(File src, File dst, Set<SortFlags> flags) {
        super.onFileSorted(src, dst, flags);

        try {
            // store of the path to the current file if it needs to be scrubbed
            if (containsEntryToDelete) {
                prefFilesToCleanup.add(dst.getCanonicalPath());
            }

            final String state = AtakPreferences.getInstance(_context)
                    .get("pref_import_pref_action", "ALLOW");

            if (state.equals("ALLOW")) {
                PreferenceControl.getInstance(_context).loadSettings(
                        dst.getName(),
                        false);
            } else if (state.equals("PROMPT")
                    || dst.getName().equals("enterprise.pref")) {
                // make an allowance for a enterprise.pref to allow for prompting in 
                // case the user has completely turned off preference loading via data package
                // but there still needs to be preferences loaded in an enterprise manner.
                promptBeforeLoad(dst);
            } else if (state.equals("DENY")) {
                Log.d(TAG, "preference importing currently set to deny");
            }

        } catch (Exception e) {
            Log.e(TAG, "exception in onFileSorted!", e);
        }
    }

    @Override
    public void finalizeImport() {
        super.finalizeImport();

        if (!_bFileSorted) {
            return;
        }

        try {
            // iterate over all prefs files that need to be scrubbed
            for (String prefsFile : prefFilesToCleanup) {
                Document doc = parseDocumentFromFile(prefsFile);
                updatePreferenceDocument(doc);
                writeDocumentToFile(doc, prefsFile);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in finalizeImport!", e);
        }
    }

    /**
     * Parse XML document from file at the given path.
     *
     * @param filePath The path of the xml file
     * @return the xml Document parsed from the file
     */
    private Document parseDocumentFromFile(String filePath) throws Exception {
        DocumentBuilderFactory dbf = XMLUtils.getDocumenBuilderFactory();
        DocumentBuilder db = dbf.newDocumentBuilder();

        File configFile = new File(filePath);
        Document doc;
        try (InputStream is = IOProviderFactory.getInputStream(configFile)) {
            doc = db.parse(is);
        }
        return doc;
    }

    /**
     * Navigate the dom and iterate over all entries in the app_preferences section.
     * Remove any entries from our watchlist.
     *
     * @param doc The document to update
     */
    private void updatePreferenceDocument(Document doc) {
        Node root = doc.getDocumentElement();
        NodeList preferences = root.getChildNodes();
        for (int i = 0; i < preferences.getLength(); i++) {
            Node preference = preferences.item(i);
            if (preference.getNodeName().equals("preference")) {
                String name = preference.getAttributes()
                        .getNamedItem("name").getNodeValue();
                if (name.equals("com.atakmap.app_preferences") ||
                        name.equals("cot_streams")) {
                    NodeList items = preference.getChildNodes();
                    for (int j = items.getLength() - 1; j >= 0; j--) {
                        Node entry = items.item(j);
                        if (entry.getNodeName().equals("entry")) {
                            String key = entry.getAttributes()
                                    .getNamedItem("key").getNodeValue();

                            // remove any entries on our watch list
                            // iterate over each entryToDelete and see if the current
                            // key contains the watch list entry. we do the
                            // contains test to find substring matches in the cot_streams
                            // prefs which have their stream index as their suffix
                            for (String entryToDelete : entriesToDelete) {
                                if (key.contains(entryToDelete)) {
                                    preference.removeChild(entry);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Write XML document to file at the given path.
     * @param doc the document to write
     * @param filePath the file to write to
     */
    private void writeDocumentToFile(Document doc, String filePath)
            throws Exception {
        try (FileWriter writer = IOProviderFactory
                .getFileWriter(new File(filePath))) {
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(writer);
            TransformerFactory transformerFactory = XMLUtils
                    .getTransformerFactory();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(source, result);
        }
    }

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>(CONTENT_TYPE, HttpUtil.MIME_XML);
    }

    private void promptBeforeLoad(final File dst) {
        final File nf = new File(_context.getCacheDir(), dst.getName());
        try {
            FileSystemUtils.copyFile(dst, nf);

            // load just the connection preferences for now
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(nf);
                PreferenceControl.getInstance(_context).loadSettings(fis,
                        PreferenceControl.LOAD_CONNECTION_PREFERENCES);
            } catch (Exception ioe) {
                Log.e(TAG,
                        "error importing the preference file " + dst.getName());
            } finally {
                IoUtils.close(fis);
            }

            AlertDialog.Builder ad = new AlertDialog.Builder(_context);
            ad.setTitle(R.string.preferences);
            ad.setCancelable(false);
            ad.setMessage(_context.getString(R.string.apply_config_question,
                    dst.getName()));
            ad.setPositiveButton(R.string.yes,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FileInputStream fis = null;
                            try {
                                fis = new FileInputStream(nf);
                                PreferenceControl.getInstance(_context)
                                        .loadSettings(fis,
                                                PreferenceControl.LOAD_OTHER_PREFERENCES);
                            } catch (Exception ioe) {
                                Log.e(TAG,
                                        "error importing the preference file "
                                                + dst.getName());
                            } finally {
                                IoUtils.close(fis);
                                if (!nf.delete())
                                    Log.e(TAG, "error cleaning up after accept "
                                            + dst.getName());
                            }
                        }
                    });
            ad.setNegativeButton(R.string.no,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.e(TAG,
                                    "user rejected importing the preference file "
                                            + dst.getName());
                            if (!nf.delete())
                                Log.e(TAG,
                                        "error cleaning up after user rejected "
                                                + dst.getName());
                        }
                    });

            ATAKApplication.getCurrentActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ad.show();
                    } catch (Exception e) {
                        Log.e(TAG, "error importing the preference file "
                                + dst.getName(), e);
                    }
                }
            });

        } catch (IOException ioe) {
            Log.e(TAG, "error importing the preference file " + dst.getName());
        }
    }
}
