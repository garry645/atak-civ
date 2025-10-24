
package com.atakmap.android.update;

import android.app.Activity;
import android.content.SharedPreferences;

import com.atakmap.app.R;
import com.atakmap.app.preferences.AppMgmtPreferenceFragment;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.filesystem.SecureDelete;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.io.ZipVirtualFile;
import com.atakmap.util.zip.IoUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * Provides custom filesystem based repo
 * Does not use the IO Abstraction for plugin installation
 * Repo index stored at: atak/support/apks/custom/product.inf
 * Based on settings, can load APKs from any directory but product.inf still lives here
 * <p>
 * First look for product.inf, then product.infz
 * If neither found, then attempt to build product.inf from available APK files
 */
public class FileSystemProductProvider extends BaseProductProvider {

    private static final String TAG = "FileSystemProductProvider";

    //TODO rename "custom" to "local"? And migrate existing files over
    public static final String LOCAL_REPO_PATH = AppMgmtUtils.APK_DIR
            + File.separatorChar + "custom" + File.separatorChar;
    public static final String LOCAL_REPO_INDEX = LOCAL_REPO_PATH
            + AppMgmtUtils.REPO_INDEX_FILENAME;
    public static final String LOCAL_REPOZ_INDEX = LOCAL_REPO_PATH
            + AppMgmtUtils.REPOZ_INDEX_FILENAME;

    private static final String ZIP_PREFIX = "zip:";

    private static final String UNPACK_DIR_NAME = ".pkgmgmt-apk-unpack";

    private final SharedPreferences _prefs;

    /**
     * Construct a file system product provider given an activity and a shared preference
     * @param context the activity to use
     * @param prefs the preference to use
     */
    public FileSystemProductProvider(Activity context,
            SharedPreferences prefs) {
        super(context);
        _prefs = prefs;
        File dir = FileSystemUtils.getItem(LOCAL_REPO_PATH);
        if (!dir.exists())
            if (!dir.mkdirs())
                Log.d(TAG, "could not wrap: " + dir);

        SecureDelete.deleteDirectory(new File(context.getCacheDir(), UNPACK_DIR_NAME));
    }

    @Override
    protected ProductRepository load() {
        // we expect apks/custom/product.inf(z)
        return parseRepo(_context.getString(R.string.app_mgmt_filesystem),
                FileSystemUtils.getItem(LOCAL_REPO_INDEX));
    }

    /**
     * Override to convert to use java.io.FileReader outside of IO Abstraction
     *
     * @param repoType The type of repo; such as filesystem in this case
     * @param repoIndex The file for the repo index
     * @return The product repository
     */
    @Override
    public ProductRepository parseRepo(String repoType, File repoIndex) {
        ProductRepository repo = null;
        BufferedReader br = null;
        try {
            repo = ProductRepository.parseRepo(_context,
                    repoIndex.getAbsolutePath(),
                    repoType,
                    br = new BufferedReader(new FileReader(repoIndex)));
        } catch (IOException e) {
            Log.w(TAG, "Failed parse: " + repoIndex.getAbsolutePath(), e);
        } finally {
            IoUtils.close(br);
        }

        if (repo != null && repo.isValid()) {
            Log.d(TAG, "Updating local repo: " + repo);
        } else {
            Log.d(TAG, "Clearing local repo: " + repoIndex);
            repo = null;
        }

        return repo;
    }

    @Override
    public ProductRepository rebuild(
            final ProductProviderManager.ProgressDialogListener l) {

        //pull directory from preference. Read those APKs. create product.inf in the apk/custom dir
        File customInf = FileSystemUtils.getItem(LOCAL_REPO_INDEX);
        Log.d(TAG, "Rebuilding from: " + customInf.getAbsolutePath());

        if (customInf.isFile()) {
            Log.d(TAG, "rebuild, delete INF: " + customInf.getAbsolutePath());
            if (!customInf.delete()) {
                Log.d(TAG,
                        "could not remove INF: " + customInf.getAbsolutePath());
            }
        }

        File customInfz = FileSystemUtils.getItem(LOCAL_REPOZ_INDEX);
        if (customInfz.isFile()) {
            //extract product.infz, if it exists
            Log.d(TAG, "Loading INFZ: " + customInfz.getAbsolutePath());
            ProductRepository c = processInfz(customInfz);
            if (c != null) {
                //check and see if there are other APKs already in the target dir that we should list
                File dir = getCustomDir();
                List<File> list = getAPKs(dir, FileSystemUtils.isZip(dir));
                if (list.size() != c.getProducts().size()) {
                    Log.i(TAG,
                            "Found APKs not listed in INFZ, re-generating to include those: "
                                    + list.size());
                    return _cache = processApks();
                } else {
                    return _cache = c;
                }
            }
        }

        //otherwise, attempt to build product.inf from APKs on file system
        ProductRepository repo = processApks();
        if (repo == null)
            repo = createEmptyRepo();
        return _cache = repo;
    }

    private ProductRepository createEmptyRepo() {
        File index = FileSystemUtils.getItem(LOCAL_REPO_INDEX);
        Log.d(TAG, "wrap empty INF: " + index.getAbsolutePath());
        List<ProductInformation> apps = new ArrayList<>();
        if (!ProductRepository.save(index, apps)) {
            Log.w(TAG, "createEmptyRepo failed to save: "
                    + index.getAbsolutePath());
            return null;
        }

        ProductRepository repo = parseRepo(
                _context.getString(R.string.app_mgmt_filesystem), index);
        if (repo == null || !repo.isValid()) {
            Log.w(TAG, "createEmptyRepo no repo: " + index.getAbsolutePath());
            return null;
        }

        Log.d(TAG, "wrap empty repo: " + repo);
        return repo;
    }

    private ProductRepository processApks() {
        File dir = getCustomDir();
        List<File> list = getAPKs(dir, FileSystemUtils.isZip(dir));

        if (list.isEmpty()) {
            Log.w(TAG, "processApks no APKs: " + dir.getAbsolutePath());
            return null;
        }

        Log.d(TAG, "processApks: " + dir.getAbsolutePath() + " with APK count: "
                + list.size());
        List<ProductInformation> apps = new ArrayList<>();

        for (File file : list) {
            ProductInformation app = ProductInformation.create(null, _context,
                    file);
            if (app == null || !app.isValid()) {
                Log.w(TAG, "Skipping invalid APK: " + file.getAbsolutePath());
                continue;
            }

            if(file instanceof ZipVirtualFile)
                app.appUri = "zip:/" + file.getAbsolutePath();

            Log.d(TAG, "Parsed: " + app.toFullString());
            apps.add(app);
        }

        if (FileSystemUtils.isEmpty(apps)) {
            Log.w(TAG, "processApks no APK apps: " + dir.getAbsolutePath());
            return null;
        }

        File index = FileSystemUtils.getItem(LOCAL_REPO_INDEX);
        Log.d(TAG, "wrap INF: " + index.getAbsolutePath()
                + " with app count: " + apps.size());
        if (!ProductRepository.save(index, apps)) {
            Log.w(TAG,
                    "processApks failed to save: " + index.getAbsolutePath());
            return null;
        }

        ProductRepository repo = parseRepo(
                _context.getString(R.string.app_mgmt_filesystem), index);
        for (ProductInformation app : apps) {
            app.setParent(repo);
        }

        if (repo == null || !repo.isValid()) {
            Log.w(TAG, "processApks no repo: " + dir.getAbsolutePath());
            return null;
        }

        Log.d(TAG, "wrap repo: " + repo);
        return repo;
    }

    private File getCustomDir() {
        String updateLocalPath = _prefs.getString(
                AppMgmtPreferenceFragment.PREF_ATAK_UPDATE_LOCAL_PATH,
                FileSystemUtils.getItem(LOCAL_REPO_PATH).getAbsolutePath());
        Log.d(TAG, "customDir: " + updateLocalPath);
        File customDir = new File(updateLocalPath);
        return FileSystemUtils.isZip(customDir) ?
                new ZipVirtualFile(customDir) :
                customDir;
    }

    /**
     * Post backport, should generate a common listFiles(dir, filter, recurse)
     * within IOProviderFactor or outside in a helper class. See duplicative
     * implementation in AppMgmtPreferenceFragment
     * @param dir the directory to start searching from
     * @param recurse if the directory should be recursed
     * @return the list of files matched after recurse
     * @deprecated
     */
    @Deprecated
    @DeprecatedApi(since = "5.5", removeAt = "5.6", forRemoval = true)
    private List<File> getAPKs(File dir, boolean recurse) {
        ArrayList<File> apks = new ArrayList<>();
        File[] list = IOProviderFactory.listFiles(
                dir,
                AppMgmtUtils.APK_FilenameFilter);
        if(list != null)
            apks.addAll(Arrays.asList(list));
        if (recurse) {
            File[] subdirs = IOProviderFactory.listFiles(
                    dir,
                    new FileFilter() {
                        @Override
                        public boolean accept(File file) {
                            return file.isDirectory();
                        }
                    });
            if (subdirs != null) {
                for (File subdir : subdirs)
                    apks.addAll(getAPKs(subdir, recurse));
            }
        }
        return apks;
    }

    private ProductRepository processInfz(File customInfz) {
        if (!customInfz.isFile()) {
            return null;
        }

        Log.d(TAG, "Processing INFZ: " + customInfz.getAbsolutePath());
        //extract INFZ content
        if (!RemoteProductProvider.extract(customInfz,
                FileSystemUtils.getItem(LOCAL_REPO_PATH))) {
            Log.w(TAG,
                    "Failed to extract infz: " + customInfz.getAbsolutePath());
            return processInf();
        }

        Log.d(TAG, "Extracted infz: " + customInfz.getAbsolutePath()
                + ", now parsing product.inf");
        if (!customInfz.delete()) {
            Log.d(TAG, "could not delete: " + customInfz.getAbsolutePath());
        }

        return processInf();
    }

    private ProductRepository processInf() {
        _cache = load();
        //hashInf(FileSystemUtils.getItem(LOCAL_REPO_INDEX));
        return _cache;
    }

    @Override
    protected File getAPK(ProductInformation product) {
        //check for absolute path
        do {
            if(!product.isValid())
                break;

            if (product.getAppUri()
                    .startsWith(ProductInformation.FILE_PREFIX)) {
                Log.d(TAG,
                        "Already absolute apk file link: "
                                + product.getIconUri());
                //strip out the "file:" prefix
                return new File(FileSystemUtils.sanitizeWithSpacesAndSlashes(product
                        .getAppUri()
                        .substring(ProductInformation.FILE_PREFIX.length())));
            }
            if (product.getAppUri()
                    .startsWith(ZIP_PREFIX)) {
                Log.d(TAG,
                        "Zipped file link: "
                                + product.getIconUri());
                //strip out the "zip:" prefix
                final String packedPath = product
                        .getAppUri()
                        .substring(ZIP_PREFIX.length()+1);
                try {
                    final ZipVirtualFile packedApk = new ZipVirtualFile(
                            FileSystemUtils.sanitizeWithSpacesAndSlashes(packedPath));
                    final File unpackDir = new File(_context.getCacheDir(), UNPACK_DIR_NAME);
                    if (!unpackDir.exists())
                        unpackDir.mkdirs();
                    final File unpackedApk = new File(unpackDir, packedApk.getName());
                    if (!unpackedApk.exists()) {
                        try (InputStream in = packedApk.openStream();
                             FileOutputStream out = new FileOutputStream(unpackedApk)) {

                            FileSystemUtils.copy(in, out);
                        } catch (IOException e) {
                            break;
                        }
                    }

                    return unpackedApk;
                } catch(Exception ex) {
                    break;
                }
            }
        } while(false);

        //assume path is relative to the repo index
        return new File(FileSystemUtils.getItem(LOCAL_REPO_PATH),
                FileSystemUtils
                        .sanitizeWithSpacesAndSlashes(product.getAppUri()));
    }
}
