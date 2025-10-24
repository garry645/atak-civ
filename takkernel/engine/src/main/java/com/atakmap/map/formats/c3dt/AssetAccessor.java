package com.atakmap.map.formats.c3dt;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.io.FileProtocolHandler;
import com.atakmap.io.UriFactory;
import com.atakmap.io.WebProtocolHandler;

import com.atakmap.io.ZipProtocolHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
class AssetAccessor
{
    public static final String TAG = "AssetAccessor";
    private final ContentContainer cache;
    private final FileProtocolHandler fileProtocolHandler = new FileProtocolHandler();
    private final WebProtocolHandler webProtocolHandler = new WebProtocolHandler();
    private final ZipProtocolHandler zipProtocolHandler = new ZipProtocolHandler();

    public AssetAccessor(ContentContainer cache)
    {
        this.cache = cache;
    }

    AssetRequest get(String url, Map<String, String> headers)
    {
        AssetResponse response;
        byte[] data = null;
        if (cache != null)
        {
            // Try the cache first
            data = cache.getData(url, null);
        }
        if (data == null) {
            try (UriFactory.OpenResult openResult = fileProtocolHandler.handleURI(url)) {
                if (openResult != null) {
                    data = FileSystemUtils.read(openResult.inputStream);
                    if (cache != null) cache(url, data, new long[]{System.currentTimeMillis()});
                }
            } catch (IOException e) {
                Log.e(TAG, "Error loading asset: " + url, e);
            }
        }
        if (data == null) {
            try (UriFactory.OpenResult openResult = webProtocolHandler.handleURI(url)) {
                if (openResult != null) {
                    data = FileSystemUtils.read(openResult.inputStream);
                    if (cache != null) cache(url, data, new long[]{System.currentTimeMillis()});
                }
            } catch (IOException e) {
                Log.e(TAG, "Error loading asset: " + url, e);
            }
        }
        if (data == null) {
            try (UriFactory.OpenResult openResult = zipProtocolHandler.handleURI(url)) {
                if (openResult != null) {
                    data = FileSystemUtils.read(openResult.inputStream);
                    if (cache != null) cache(url, data, new long[]{System.currentTimeMillis()});
                }
            } catch (IOException e) {
                Log.e(TAG, "Error loading asset: " + url, e);
            }
        }
        if (data != null) {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length);
            byteBuffer.put(data);
            byteBuffer.flip();
            response = new AssetResponse(0, "", headers, byteBuffer);
        } else {
            Log.w(TAG, "Could not load content at " + url);
            response = new AssetResponse(404, "", headers, null);
        }
        return new AssetRequest("method", url, headers, response);
    }

    AssetRequest request(String verb, String url, Map<String, String> headers, String contentPayload)
    {
        // TODO
        return get(url, headers);
    }

    private boolean cache(String uri, byte[] blob, long[] version)
    {
        // if refreshing a tileset JSON, verify it can be parsed
        String contentName = uri;
        final int queryIdx = contentName.indexOf('?');
        if(queryIdx >  0)
            contentName = contentName.substring(0, queryIdx);
        if(contentName.toLowerCase(LocaleUtil.getCurrent()).endsWith(".json"))
        {
            try
            {
                final String json = new String(blob, FileSystemUtils.UTF8_CHARSET);
                new JSONObject(json);
            } catch(Throwable t) {
                return false;
            }
        }

        this.cache.put(uri, blob, (version != null) ? version[0] : System.currentTimeMillis());
        return true;
    }
}
