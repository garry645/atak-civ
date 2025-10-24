
package com.atakmap.android.eud;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.atak.plugins.impl.AtakPluginRegistry;
import com.atakmap.android.cot.detail.TakVersionDetailHandler;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.update.ApkUpdateComponent;
import com.atakmap.android.update.ApkUpdateReceiver;
import com.atakmap.android.update.AppMgmtUtils;
import com.atakmap.android.update.BaseProductProvider;
import com.atakmap.android.update.ProductInformation;
import com.atakmap.android.update.ProductProviderManager;
import com.atakmap.android.update.ProductRepository;
import com.atakmap.android.util.ATAKConstants;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProvider;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import gov.tak.api.engine.net.HttpClientBuilder;
import gov.tak.api.engine.net.IHttpClient;
import gov.tak.api.engine.net.IResponse;
import gov.tak.api.engine.net.auth.OAuthAccessToken;
import gov.tak.api.engine.net.auth.OAuthTokenManager;
import gov.tak.platform.client.eud.EndPoints;
import gov.tak.platform.client.eud.Plugins;

final class EudApiProductProvider extends BaseProductProvider {

    private static final String TAG = "EudApiProductProvider";

    final OAuthTokenManager _tokenManager;
    final File _cacheDir;
    final File _apksDir;
    final File _iconsDir;
    final File _pluginsListing;

    public EudApiProductProvider(Activity context, File cacheDir,
            OAuthTokenManager tokenManager) {
        super(context);
        _tokenManager = tokenManager;

        _cacheDir = cacheDir;
        _pluginsListing = new File(_cacheDir, "plugins.json");
        _apksDir = new File(_cacheDir, "apks");
        _iconsDir = new File(_cacheDir, "icons");
    }

    @Override
    protected ProductRepository load() {
        return parseRepo(
                _context.getString(
                        com.atakmap.app.R.string.app_mgmt_update_server),
                _pluginsListing);
    }

    /**
     * Only execute from non-UI thread
     *
     * @param l the listener for the rebuild action
     * @return the ProductRepository as a result of calling rebuild
     */
    @Override
    public ProductRepository rebuild(
            final ProductProviderManager.ProgressDialogListener l) {

        OAuthAccessToken token = _tokenManager.getToken(EndPoints.AUTH_SERVER,
                EndPoints.client_id);
        if (token == null || token.accessToken() == null) {
            clearLocalCache("Update Server URL not set");
            return null;
        }

        Log.d(TAG, "rebuild: Remote repo URL: " + EndPoints.PLUGINS);

        File repoDir = _pluginsListing.getParentFile();
        if (!IOProviderFactory.exists(repoDir)) {
            if (!IOProviderFactory.mkdirs(repoDir)) {
                Log.e(TAG, "failed to wrap" + repoDir);
            }
        }

        if (l != null) {
            l.update(new ProductProviderManager.ProgressDialogUpdate(1,
                    "Downloading remote repo index"));
        }

        //Note it is assumed we are running on background thread, so we execute network operation
        //directly, rather than using HTTPRequestService
        try {
            final String product = TakVersionDetailHandler.Platform.ATAK + "-" +
                    ATAKConstants.getVersionBrand();
            final String version = AtakPluginRegistry
                    .getCoreApi(ATAKConstants.getPluginApi(true));

            IHttpClient client = HttpClientBuilder.newBuilder(EndPoints.PLUGINS)
                    .addQueryParameter("product", product)
                    .addQueryParameter("product_version", version)
                    .addHeader("Authorization", "Bearer " + token.accessToken())
                    .get();
            File pluginsListing = new File(_pluginsListing.getParentFile(),
                    _pluginsListing.getName() + ".download");
            try (IResponse response = client.execute()) {
                FileSystemUtils.copyStream(response.getBody(), false,
                        IOProviderFactory.getOutputStream(pluginsListing),
                        true);
            }
            ProductRepository repo = parseRepo(
                    _context.getString(
                            com.atakmap.app.R.string.app_mgmt_update_server),
                    pluginsListing, l);
            if (repo != null) {
                IOProviderFactory.delete(_pluginsListing);
                IOProviderFactory.renameTo(pluginsListing, _pluginsListing);
            } else {
                IOProviderFactory.delete(pluginsListing);
            }
            _cache = repo;
        } catch (IOException e) {
            Log.w(TAG, "Failed to get repo index: " + EndPoints.PLUGINS, e);
            clearLocalCache(e.getMessage());
        }
        return _cache;
    }

    private void clearLocalCache(final String reason) {
        Log.d(TAG, reason);

        if (reason != null && !reason.isEmpty()) {
            _context.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(_context, reason, Toast.LENGTH_SHORT).show();
                }
            });
        }

        IOProviderFactory.delete(_pluginsListing.getParentFile(),
                IOProvider.CONTENTS_ONLY | IOProvider.RECURSIVE);
        _cache = null;
    }

    @Override
    public File getAPK(final ProductInformation product) {
        //See APKDownloader
        Log.w(TAG, "getAPK not supported: " + product.toString());
        return null;
    }

    /**
     * Leverage HTTPRequestService to download APK
     * @param product the product to install.
     */
    @Override
    public void install(final ProductInformation product) {
        Log.d(TAG, "install: " + product.toString());
        File apkFile = new File(
                _apksDir,
                FileSystemUtils.sanitizeWithSpacesAndSlashes(
                        product.getPackageName() + "/" + product.getHash() + "/"
                                +
                                product.getSimpleName() + ".apk"));
        download(product, apkFile);
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    /**
     * Override to convert to use java.io.FileReader outside of IO Abstraction
     *
     * @param repoType The type of repo; such as remote in this case
     * @param repoIndex The file for the repo index
     * @return The product repository
     */
    @Override
    public ProductRepository parseRepo(String repoType, File repoIndex) {
        return parseRepo(repoType, repoIndex, null);
    }

    private ProductRepository parseRepo(String repoType, File repoIndex,
            ProductProviderManager.ProgressDialogListener l) {
        ProductRepository repo = null;
        try {
            if (!FileSystemUtils.isFile(repoIndex)) {
                Log.i(TAG,
                        "File does not exist: " + repoIndex.getAbsolutePath());
            } else {
                Plugins plugins = Plugins
                        .parse(FileSystemUtils.copyStreamToString(repoIndex));
                if (plugins != null) {
                    final AtomicInteger iconDownloadIndex = new AtomicInteger(
                            -1);
                    Thread t = new Thread(new IconDownloader(plugins.elements,
                            iconDownloadIndex));
                    t.setPriority(Thread.NORM_PRIORITY);
                    t.start();

                    List<ProductInformation> products = new ArrayList<>(
                            plugins.elements.length);
                    repo = new ProductRepository(repoIndex.getAbsolutePath(),
                            repoType, products);
                    for (int i = 0; i < plugins.elements.length; i++) {
                        final Plugins.Element plugin = plugins.elements[i];
                        int installedVersion = -1;
                        if (AppMgmtUtils.isInstalled(
                                MapView.getMapView().getContext(),
                                plugin.package_name)) {
                            installedVersion = AppMgmtUtils.getAppVersionCode(
                                    MapView.getMapView().getContext(),
                                    plugin.package_name);
                        }
                        // icon file is downloaded asynchronously
                        final File iconFile = new File(_iconsDir,
                                plugin.package_name + "@" + plugin.apk_hash);
                        final String iconUri = "file:"
                                + iconFile.getAbsolutePath();
                        ProductInformation info = new ProductInformation(
                                repo, // repo
                                ProductInformation.Platform
                                        .valueOf(plugin.platform),
                                ProductInformation.ProductType
                                        .valueOf(plugin.apk_type), // product type
                                plugin.package_name,
                                plugin.display_name,
                                plugin.version,
                                plugin.revision_code,
                                plugin.apk_url,
                                iconUri,
                                plugin.description,
                                plugin.apk_hash,
                                plugin.os_requirement,
                                plugin.tak_prerequisite,
                                installedVersion);
                        products.add(info);
                        if (l != null) {
                            l.update(
                                    new ProductProviderManager.ProgressDialogUpdate(
                                            (int) ((i + 1)
                                                    / (double) plugins.elements.length
                                                    * 100d),
                                            "Parsing Product Information "
                                                    + (i + 1) + " of "
                                                    + plugins.elements.length));
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed parse: " + repoIndex.getAbsolutePath(), e);
        }

        if (repo != null && repo.isValid()) {
            Log.d(TAG, "Updating local repo: " + repo);
        } else {
            Log.d(TAG, "Clearing local repo: " + repoIndex);
            repo = null;
        }

        return repo;
    }

    private void downloadResource(OAuthAccessToken token, String url, File file)
            throws IOException {
        IHttpClient client = HttpClientBuilder.newBuilder(url)
                .addHeader("Authorization", "Bearer " + token.accessToken())
                .get();
        try (IResponse response = client.execute()) {
            if (!IOProviderFactory.exists(file.getParentFile()))
                IOProviderFactory.mkdirs(file.getParentFile());
            FileSystemUtils.copyStream(response.getBody(), false,
                    IOProviderFactory.getOutputStream(file), true);
        }
    }

    private void download(ProductInformation product, File apkFile) {
        //do not prompt, just proceed to download and install
        Toast.makeText(_context, String.format(LocaleUtil.getCurrent(),
                        _context.getString(R.string.installing_s), product.getSimpleName()),
                Toast.LENGTH_SHORT).show();
        Log.d(TAG, "download and install: " + product + " from: "
                + product.getAppUri());

        //add to list of outstanding installs
        ApkUpdateComponent updateComponent = ApkUpdateComponent.getInstance();
        if (updateComponent != null)
            updateComponent.getApkUpdateReceiver().addInstalling(product);

        //do not prompt, just proceed to download and install
        Toast.makeText(_context, String.format(LocaleUtil.getCurrent(),
                        _context.getString(R.string.installing_s), product.getSimpleName()),
                Toast.LENGTH_SHORT).show();
        Log.d(TAG, "download and install: " + product + " from: "
                + product.getAppUri());

        // push token retrieval and download trigger into background thread
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                final OAuthAccessToken token = _tokenManager
                        .getToken(EndPoints.AUTH_SERVER, EndPoints.client_id);

                final File pFile = apkFile.getParentFile();
                if (pFile != null && token != null) {
                    AtakBroadcast.getInstance().sendBroadcast(
                            new Intent(ApkUpdateReceiver.DOWNLOAD_APK)
                                    .putExtra("url", product.getAppUri())
                                    .putExtra("package",
                                            product.getPackageName())
                                    .putExtra("hash", product.getHash())
                                    .putExtra("filename", apkFile.getName())
                                    .putExtra("apkDir",
                                            pFile.getAbsolutePath())
                                    .putExtra("http2", true)
                                    .putExtra("token", token.accessToken())
                                    .putExtra("install", true));
                }
            }
        }, "eud-api-provider-download");
        t.setPriority(Thread.NORM_PRIORITY);
        t.start();
    }

    private class IconDownloader implements Runnable {
        private final Plugins.Element[] plugins;
        private final AtomicInteger downloadIndex;

        IconDownloader(Plugins.Element[] plugins, AtomicInteger downloadIndex) {
            this.plugins = plugins;
            this.downloadIndex = downloadIndex;
        }

        @Override
        public void run() {
            final OAuthAccessToken token = _tokenManager
                    .getToken(EndPoints.AUTH_SERVER, EndPoints.client_id);
            while (true) {
                final int i = downloadIndex.incrementAndGet();
                if (i >= plugins.length)
                    break;
                if (!token.isValid())
                    break;
                final Plugins.Element plugin = plugins[i];
                String iconUri = plugin.icon_url;
                File iconFile = new File(_iconsDir,
                        plugin.package_name + "@" + plugin.apk_hash);
                if (!IOProviderFactory.exists(iconFile)) {
                    try {
                        downloadResource(token, iconUri, iconFile);
                    } catch (Throwable ignored) {
                        IOProviderFactory.delete(iconFile);
                    }
                }
            }
        }
    }
}
