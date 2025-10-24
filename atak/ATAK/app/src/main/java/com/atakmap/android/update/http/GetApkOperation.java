
package com.atakmap.android.update.http;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;

import com.atakmap.android.http.rest.DownloadProgressTracker;
import com.atakmap.android.http.rest.operation.HTTPOperation;
import com.atakmap.android.http.rest.operation.NetworkOperation;
import com.atakmap.android.math.MathUtils;
import com.atakmap.app.R;
import com.atakmap.comms.http.HttpUtil;
import com.atakmap.comms.http.TakHttpClient;
import com.atakmap.comms.http.TakHttpException;
import com.atakmap.comms.http.TakHttpResponse;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.filesystem.HashingUtils;
import com.foxykeep.datadroid.exception.ConnectionException;
import com.foxykeep.datadroid.exception.DataException;
import com.foxykeep.datadroid.requestmanager.Request;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import gov.tak.api.engine.net.HttpClientBuilder;
import gov.tak.api.engine.net.IHttpClientBuilder;
import gov.tak.api.engine.net.IResponse;

/**
 * REST Operation to GET a file much like <code>GetFileOperation</code>, but also 
 * display progress during download. Note, currently if the target file already
 * exists and is the same size as the source file, then the download is skipped.
 * Also checks for HTTP 401 & 403 (unauthorized/forbidden)
 * Also ignores all SSL validation errors
 */
public final class GetApkOperation extends HTTPOperation {
    private static final String TAG = "GetApkOperation";

    public static final String PARAM_GETAPKFILE = GetApkOperation.class
            .getName() + ".PARAM_GETAPKFILE";
    public static final String PARAM_GETCREDENTIALS = GetApkOperation.class
            .getName() + ".PARAM_GETCREDENTIALS";

    @Override
    public Bundle execute(Context context, Request request)
            throws ConnectionException,
            DataException {

        final ApkFileRequest apkFileRequest = (ApkFileRequest) request
                .getParcelable(GetApkOperation.PARAM_GETAPKFILE);

        // Get request data
        if (apkFileRequest != null) {
            TakHttpClient client = null;
            try {
                IHttpClientBuilder clientBuilder;
                final String url = apkFileRequest.getUrl();
                if (apkFileRequest.isHttp2()) {
                    clientBuilder = HttpClientBuilder.newBuilder(url);
                } else {
                    client = TakHttpClient.GetHttpClient(url);
                    clientBuilder = new TakHttpClient.Builder(client, url);
                }
                return GetFile(context,
                        clientBuilder,
                        apkFileRequest, 0, 1);
            } finally {
                if (client != null) {
                    try {
                        client.shutdown();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return null;
    }

    /**
     * Perform the HTTP Get, and display progress during download
     * 
     * @param httpclient
     * @param fileRequest
     * @return
     * @throws DataException, ConnectionException
     */
    static Bundle GetFile(Context context, IHttpClientBuilder httpclient,
            ApkFileRequest fileRequest,
            int progressCurrent, int progressTotal)
            throws DataException, ConnectionException {
        if (fileRequest == null) {
            throw new DataException("Unable to serialize import file request");
        }

        if (!fileRequest.isValid()) {
            throw new DataException(
                    "Unable to serialize invalid import file request");
        }

        try {
            // now start timer
            long startTime = SystemClock.elapsedRealtime();

            // ///////
            // Get file
            // ///////
            String url = fileRequest.getUrl().trim();
            Log.d(TAG, "sending GET File request to: " + url);

            // ///////
            // Send HTTP Head to get size first
            // ///////
            if (fileRequest.hasCredentials())
                HttpUtil.AddBasicAuthentication(httpclient,
                        fileRequest.getCredentials());
            else if (fileRequest.hasBearerToken())
                httpclient.addHeader("Authorization",
                        "Bearer " + fileRequest.getBearerToken());

            HttpHead httphead = new HttpHead(url);
            Log.d(TAG, "executing head " + httphead.getRequestLine());
            IResponse response = httpclient.get().execute();

            //we need to gather credentials
            if (isStatus(response, HttpStatus.SC_UNAUTHORIZED) ||
                    isStatus(response, HttpStatus.SC_FORBIDDEN)) {
                Log.d(TAG, "Not authorized: " + response.toString());
                Bundle output = new Bundle();
                output.putParcelable(PARAM_GETAPKFILE, fileRequest);
                output.putBoolean(PARAM_GETCREDENTIALS, true);
                output.putInt(NetworkOperation.PARAM_STATUSCODE,
                        response.getCode());
                return output;
            }

            verifyOk(response);
            long contentLength = response.getLength();
            if (contentLength < 0) {
                Log.d(TAG,
                        "Content-Length not available: "
                                + httphead.getRequestLine());
            }

            File contentFile = fileRequest.getFile();
            if (checkAlreadyDownloaded(contentFile, contentLength,
                    fileRequest.hasHash() ? fileRequest.getHash() : null)) {
                Bundle output = new Bundle();
                output.putParcelable(PARAM_GETAPKFILE, fileRequest);
                output.putInt(NetworkOperation.PARAM_STATUSCODE,
                        response.getCode());
                return output;
            }

            //go ahead and download APK, we don't have the latest APK local
            HttpGet httpget = new HttpGet(url);
            Log.d(TAG, "executing GET request " + httpget.getRequestLine());

            response.close();

            response = httpclient.get().execute();

            Log.d(TAG, "processing response");

            //we need to gather credentials
            if (isStatus(response, HttpStatus.SC_UNAUTHORIZED) ||
                    isStatus(response, HttpStatus.SC_FORBIDDEN)) {
                Log.d(TAG, "Not authorized: " + response.getCode());
                Bundle output = new Bundle();
                output.putParcelable(PARAM_GETAPKFILE, fileRequest);
                output.putBoolean(PARAM_GETCREDENTIALS, true);
                output.putInt(NetworkOperation.PARAM_STATUSCODE,
                        response.getCode());
                return output;
            }

            verifyOk(response);

            // setup progress notifications
            NotificationManager notifyManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);

            Notification.Builder builder;
            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                builder = new Notification.Builder(context);
            } else {
                builder = new Notification.Builder(context,
                        "com.atakmap.app.def");
            }
            builder.setContentTitle(
                    "Downloading " + context.getString(R.string.app_name)
                            + " Update")
                    .setContentText("Downloading " + fileRequest.getFileName())
                    .setSmallIcon(
                            com.atakmap.android.util.ATAKConstants.getIconId());

            // now stream bytes over network into file
            int len;
            DownloadProgressTracker progressTracker = new DownloadProgressTracker(
                    contentLength);

            byte[] buf = new byte[8192];
            {
                if (!contentFile.getParentFile().exists())
                    contentFile.getParentFile().mkdirs();
                FileOutputStream fos = null;
                InputStream in = null;
                try {
                    fos = new FileOutputStream(contentFile);
                    in = response.getBody();
                    while ((len = in.read(buf)) > 0) {
                        fos.write(buf, 0, len);

                        // see if we should update progress notification 
                        // based on progress or time since last update

                        long currentTime = SystemClock.elapsedRealtime();
                        if (progressTracker.contentReceived(len, currentTime)) {
                            // do we know the content length?
                            if (contentLength > 0) {
                                // compute progress scaled to larger download set
                                int statusProgress = (100 * progressCurrent)
                                        / progressTotal
                                        + progressTracker.getCurrentProgress()
                                                / progressTotal;
                                String message = String
                                        .format(LocaleUtil.getCurrent(),
                                                "Downloading (%d%% of %s)  %s, %s remaining. %s ",
                                                progressTracker
                                                        .getCurrentProgress(),
                                                MathUtils
                                                        .GetLengthString(
                                                                contentLength),
                                                MathUtils
                                                        .GetDownloadSpeedString(
                                                                progressTracker
                                                                        .getAverageSpeed()),
                                                MathUtils
                                                        .GetTimeRemainingString(
                                                                progressTracker
                                                                        .getTimeRemaining()),
                                                fileRequest.getFileName());
                                builder.setProgress(100, statusProgress, false);
                                builder.setContentText(message);
                                if (!fileRequest.isSilent()) {
                                    if (notifyManager != null) {
                                        notifyManager.notify(
                                                fileRequest.getNotificationId(),
                                                builder.build());
                                    }
                                }
                                Log.d(TAG, message + ", overall progress="
                                        + statusProgress);
                                progressTracker.notified(currentTime);
                            } else {
                                // we cannot progress report if we don't know the total size...
                                String message = String
                                        .format(LocaleUtil.getCurrent(),
                                                "Downloading %d of %d, %s downloaded so far...",
                                                (progressCurrent + 1),
                                                progressTotal,
                                                MathUtils
                                                        .GetLengthString(
                                                                progressTracker
                                                                        .getCurrentLength()));
                                builder.setProgress(100, 100, true);
                                builder.setContentText(message);
                                if (!fileRequest.isSilent()) {
                                    if (notifyManager != null) {
                                        notifyManager.notify(
                                                fileRequest.getNotificationId(),
                                                builder.build());
                                    }
                                }
                                Log.d(TAG, message + ", progress unknown...");
                                progressTracker.notified(currentTime);
                            }
                        }
                    }
                } finally {
                    response.close();

                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
                Log.d(TAG, "Content request complete");
            }

            // Now verify we got download correctly
            if (contentFile == null || !contentFile.exists()) {
                contentFile.delete();
                throw new ConnectionException("Failed to download data");
            }

            long downloadSize = contentFile.length();
            long stopTime = SystemClock.elapsedRealtime();

            Log.d(TAG, String.format(LocaleUtil.getCurrent(),
                    "File Request %s downloaded %d bytes in %f seconds",
                    fileRequest.toString(), downloadSize,
                    (stopTime - startTime) / 1000F));

            Bundle output = new Bundle();
            output.putParcelable(PARAM_GETAPKFILE, fileRequest);
            output.putInt(NetworkOperation.PARAM_STATUSCODE,
                    response.getCode());
            return output;
        } catch (TakHttpException e) {
            Log.e(TAG, "Failed to download file: " + fileRequest.getUrl(), e);
            throw new ConnectionException(e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            Log.e(TAG, "Failed to download File: " + fileRequest.getUrl(), e);
            throw new ConnectionException(e.getMessage(),
                    NetworkOperation.STATUSCODE_UNKNOWN);
        }
    }

    private static boolean checkAlreadyDownloaded(File contentFile,
            long expectedContentLength, String expectedHash) {
        if (contentFile.exists()) {
            Log.d(TAG, "File already exists, checking if current: "
                    + contentFile.getAbsolutePath());

            //file already exists, is it complete and unchanged?
            if (contentFile.length() == expectedContentLength) {
                if (expectedHash != null) {
                    //check hash based on value provided in product.inf
                    String sha256 = HashingUtils.sha256sum(contentFile);
                    if (FileSystemUtils.isEquals(sha256,
                            expectedHash)) {
                        //file already here and current
                        Log.d(TAG, "File: " + contentFile.getAbsolutePath()
                                + " of size: "
                                + contentFile.length()
                                + " already exists. SHA256: " + sha256);

                        return true;
                    } else {
                        //same file size, but different hash
                        Log.d(TAG, "File: " + contentFile.getAbsolutePath()
                                + " of size: "
                                + contentFile.length()
                                + " has new hash. SHA256: " + sha256);
                    }
                } else {
                    //no hash available to reference
                    Log.d(TAG, "File: " + contentFile.getAbsolutePath()
                            + " of size: "
                            + contentFile.length()
                            + " has no available hash");
                }
            } else {
                Log.d(TAG,
                        "Overwriting file: "
                                + contentFile.getAbsolutePath()
                                + " of size: "
                                + contentFile.length()
                                + " with new size: "
                                + expectedContentLength);
            }
        }
        return false;
    }

    static void verify(IResponse response, int status) throws TakHttpException {
        if (!isStatus(response, status)) {
            if (response instanceof TakHttpResponse) {
                Log.w(TAG, "HTTP operation: "
                        + ((TakHttpResponse) response).getRequestUrl()
                        + " expected: " + status + ", " + response);
            }
            String errorMessage = getReasonPhrase(response);
            if (FileSystemUtils.isEmpty(errorMessage))
                errorMessage = "HTTP operation failed: " + response.getCode();
            throw new TakHttpException(errorMessage, response.getCode());
        }
    }

    static String getReasonPhrase(IResponse response) {
        if (response instanceof TakHttpResponse)
            return ((TakHttpResponse) response).getReasonPhrase();
        else
            return null;
    }

    static void verifyOk(IResponse response) throws TakHttpException {
        verify(response, HttpStatus.SC_OK);
    }

    static boolean isStatus(IResponse response, int status) {
        return response.getCode() == status;
    }
}
