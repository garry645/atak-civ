
package com.atakmap.comms.http;

import com.atakmap.comms.CommsProvider;
import com.atakmap.comms.CommsProviderFactory;
import com.atakmap.comms.NetConnectString;
import com.atakmap.comms.SslNetCotPort;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.net.AtakAuthenticationCredentials;
import com.atakmap.net.CertificateManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import gov.tak.api.engine.net.IHttpClient;
import gov.tak.api.engine.net.IHttpClientBuilder;
import gov.tak.api.engine.net.IResponse;

/**
 * Creates an HttpClient with proper TLS certificates
 */
public class TakHttpClient {
    private static final String TAG = "TakHttpClient";

    /**
     * Encapsulated client to execute HTTP operations
     */
    private final HttpClient _client;

    /**
     * Base URL for TAK (Server) related URLs. Manages building out the full request URL from
     * a base URL
     */
    private final String _baseUrl;

    /**
     * Setup HTTP client.
     * HTTPS base URLs use TLS to include client certificates. Uses internally stored certificates
     * HTTP URLs use Basic Auth
     * @param url the base url
     */
    public TakHttpClient(String url) {
        if (url.toLowerCase(LocaleUtil.getCurrent())
                .startsWith("https")) {
            //just use last TAK Server SSL socket
            //Log.d(TAG, "Streaming port was of type SSL, attempting to reuse socket factory");
            _baseUrl = url
                    + SslNetCotPort.getServerApiPath(SslNetCotPort.Type.SECURE);
            _client = HttpUtil.GetHttpClient(
                    CertificateManager.getSockFactory(true, _baseUrl));
        } else {
            //use Basic Auth credentials
            //Log.d(TAG, "Streaming port was of type TCP, using preemptive auth");
            _baseUrl = url
                    + SslNetCotPort
                            .getServerApiPath(SslNetCotPort.Type.UNSECURE);
            _client = HttpUtil.GetHttpClient(false);
        }
    }

    /**
     * Setup HTTP client.
     * HTTPS base URLs use TLS to include client certificates. Uses internally stored certificates
     * HTTP URLs use Basic Auth
     * @param url the base url
     * @param connectString the connectString which is in turn used for the certificate lookup to
     *                      ensure that https are made with the correct client cert for the port
     */
    public TakHttpClient(String url, String connectString) {
        if (url.toLowerCase(LocaleUtil.getCurrent())
                .startsWith("https")) {
            //just use last TAK Server SSL socket
            //Log.d(TAG, "Streaming port was of type SSL, attempting to reuse socket factory");
            _baseUrl = url
                    + SslNetCotPort.getServerApiPath(SslNetCotPort.Type.SECURE);
            _client = HttpUtil.GetHttpClient(
                    CertificateManager.getSockFactory(true,
                            NetConnectString.fromString(connectString)));
        } else {
            //use Basic Auth credentials
            //Log.d(TAG, "Streaming port was of type TCP, using preemptive auth");
            _baseUrl = url
                    + SslNetCotPort
                            .getServerApiPath(SslNetCotPort.Type.UNSECURE);
            _client = HttpUtil.GetHttpClient(false);
        }
    }

    /**
     * Setup HTTP client with a given sslSocketFactory. Note it is up to the caller to provide
     * a complete baseUrl with protocol, host, and port info
     * @param baseUrl the base url
     * @param sslSocketFactory the socket factory to be used
     */
    public TakHttpClient(String baseUrl, SSLSocketFactory sslSocketFactory) {
        _baseUrl = baseUrl;
        _client = HttpUtil.GetHttpClient(sslSocketFactory);
    }

    /**
     * Setup HTTP client with a given sslSocketFactory and custom timeout values.
     * Note it is up to the caller to provide a complete baseUrl with protocol, host, and port info
     * @param baseUrl the base url
     * @param sslSocketFactory the socket factory to be used
     * @param connectTimeout the connection timeout
     * @param soTimeout the socket timeout
     */
    public TakHttpClient(String baseUrl, SSLSocketFactory sslSocketFactory,
            int connectTimeout, int soTimeout) {
        _baseUrl = baseUrl;
        _client = HttpUtil.GetHttpClient(connectTimeout, soTimeout,
                sslSocketFactory);
    }

    /**
     * Creates an HttpClient and proper TAK Server base URL
     * Uses internally stored certificates
     *
     * @param url the base url
     * @return the corresponding client
     */
    public static TakHttpClient GetHttpClient(String url) {
        return new TakHttpClient(url);
    }

    /**
     * Creates an HttpClient and proper TAK Server base URL
     * Uses internally stored certificates
     *
     * @param url the base url
     * @return the corresponding client
     * @param connectString the connectString which is in turn used for the certificate lookup to
     *                      ensure that https are made with the correct client cert for the port
     */
    public static TakHttpClient GetHttpClient(String url,
            String connectString) {
        return new TakHttpClient(url, connectString);
    }

    /**
     * Creates an HttpClient and proper TAK Server base URL
     * Uses internally stored certificates
     *
     * @param url the base url
     * @return the corresponding client
     * @param connectString the connectString which is in turn used for the certificate lookup to
     *                      ensure that https are made with the correct client cert for the port;
     *                      a null connectString will result in attempting best effort to derive
     *                      certs from the given url
     */
    public static TakHttpClient GetHttpClient(String url,
            NetConnectString connectString) {
        // null check is primarily to handle the numerous legacy usages 
        // (mostly deprecated but a few remain) elsewhere.  
        if (connectString != null)
            return GetHttpClient(url, connectString.toString());
        else
            return GetHttpClient(url);
    }

    /**
     * Creates an HttpClient using specified socket factory
     *
     * @param factory the socket factory to use
     * @param url the url to use when creating a client
     * @return the client corresponding to the url.
     */
    public static TakHttpClient GetHttpClient(SSLSocketFactory factory,
            String url) {
        return new TakHttpClient(url, factory);
    }

    /**
     * Get the TAK (Server) base URL
     * @return the String representation of the baseURL
     */
    public String getUrl() {
        return _baseUrl;
    }

    /**
     * Get the TAK (Server) URL for specified path
     *
     * @param path the path to be used for lookup
     * @return the path appropriately added to the url
     */
    public String getUrl(String path) {
        String url = _baseUrl;
        if (FileSystemUtils.isEmpty(path))
            return url;

        if (!url.endsWith("/") && !path.startsWith("/"))
            url += "/";

        url += path;
        return url;
    }

    /**
     * False for HTTPs, true for HTTP
     *
     * @return false if the url is https
     */
    public boolean useBasicAuth() {
        return !_baseUrl.toLowerCase(LocaleUtil.getCurrent())
                .startsWith("https");
    }

    /**
     * Add Basic Auth for HTTP, not HTTPS (which typically uses client certificates)
     *
     * @param request the request to add basic auth to.
     */
    public void addBasicAuthentication(HttpRequestBase request) {
        if (request == null) {
            return;
        }

        try {
            HttpUtil.AddBasicAuthentication(request);
        } catch (AuthenticationException e) {
            Log.e(TAG, "Failed to add authentication", e);
        }
    }

    /**
     * Add Basic Auth for the request using the credentials provided
     *
     * @param request the request to add basic auth to.
     * @param credentials the credentials to use to set up the basic auth.
     */
    public void addBasicAuthentication(HttpRequestBase request,
            AtakAuthenticationCredentials credentials) {
        if (request == null) {
            return;
        }

        try {
            HttpUtil.AddBasicAuthentication(request, credentials);
        } catch (AuthenticationException e) {
            Log.e(TAG, "Failed to add authentication", e);
        }
    }

    /**
     * Perform an HTTP GET on the URL
     *
     * @param url       URL to get
     * @return  response body as String if HTTP OK 200 is returned by server
     */
    public String get(String url) throws IOException {
        return get(url, null);
    }

    /**
     * Perform an HTTP GET on the URL
     *
     * @param url       URL to get
     * @param verify    Verify response contains this string
     * @return  response body as String if HTTP OK 200 is returned by server
     */
    public String get(String url, String verify) throws IOException {
        return get(url, verify, null);
    }

    public String get(String url, String verify, String accept)
            throws IOException {
        return get(url, verify, accept, null);
    }

    /**
     * Perform an HTTP GET on the URL, support GZip
     *
     * @param url the url to perform a http get on.
     * @param verify the string that is to be contained in the response
     * @return response body as String if HTTP OK 200 is returned by serve
     * @throws IOException when there is an issue with the gzip'd data
     */
    public String getGZip(String url, String verify) throws IOException {
        return getGZip(url, verify, null);
    }

    /**
     * Perform an HTTP GET on the URL, support GZip
     *
     * @param url the url to perform a http get on.
     * @param verify the string that is to be contained in the response
     * @param accept Set "Accept" header on request
     * @return response body as String if HTTP OK 200 is returned by serve
     * @throws IOException when there is an issue with the gzip'd data
     */
    public String getGZip(String url, String verify, String accept)
            throws IOException {
        return get(url, verify, accept, HttpUtil.GZIP);
    }

    /**
     * Perform an HTTP GET on the URL
     *
     * @param url       URL to get
     * @param verify    Verify response contains this string
     * @param accept    Set "Accept" header on request
     * @return  response body as String if HTTP OK 200 is returned by server
     */
    public String get(String url, String verify, String accept,
            String acceptEncoding) throws IOException {
        // GET file
        HttpGet httpget = new HttpGet(url);
        if (!FileSystemUtils.isEmpty(accept))
            httpget.addHeader("Accept", accept);
        if (!FileSystemUtils.isEmpty(acceptEncoding))
            httpget.addHeader("Accept-Encoding", acceptEncoding);

        TakHttpResponse response = null;
        try {
            response = execute(httpget);
            response.verifyOk();
            return response.getStringEntity(verify);
        } finally {
            if (response != null)
                response.close();
        }
    }

    /**
     * Perform HTTP HEAD on the URL
     *
     * @param url url to perform a http head on.
     * @return true if content is not on server (does not return HTTP OK 200)
     */
    public boolean head(final String url) throws IOException {
        HttpHead httpHead = new HttpHead(url);
        Log.d(TAG,
                "Checking for content on server " + httpHead.getRequestLine());
        TakHttpResponse response = null;

        try {
            response = execute(httpHead);

            if (response.isOk()) {
                Log.d(TAG, "Content exists on server: " + url);
                return true;
            } else {
                Log.d(TAG, "Content not on server: " + url);
                return false;
            }
        } finally {
            if (response != null)
                response.close();
        }
    }

    public TakHttpResponse execute(HttpRequestBase request) throws IOException {
        return execute(request, useBasicAuth());
    }

    public TakHttpResponse execute(HttpRequestBase request, boolean bAddAuth)
            throws IOException {
        if (bAddAuth && !CommsProviderFactory.getProvider()
                .hasFeature(CommsProvider.CommsFeature.MISSION_API)) {
            addBasicAuthentication(request);
        }
        Log.d(TAG, "executing request " + request.getRequestLine());
        // if we have alt comms get our response from the comms provider
        if (CommsProviderFactory.getProvider()
                .hasFeature(CommsProvider.CommsFeature.MISSION_API)) {
            HttpEntity entity = null;
            InputStream requestStream = null;
            InputStream responseStream;
            int responseCode;
            if (request.getMethod().equals(HttpPost.METHOD_NAME)) {
                entity = ((HttpPost) request).getEntity();
            } else if (request.getMethod().equals(HttpPut.METHOD_NAME)) {
                entity = ((HttpPut) request).getEntity();
            }
            if (entity != null) {
                requestStream = entity.getContent();
            }
            // send request to comms
            responseCode = CommsProviderFactory.getProvider().getResponseCode(request.getRequestLine().toString(), requestStream);
            responseStream = CommsProviderFactory.getProvider().getRequestResponse(request.getRequestLine().toString(), requestStream);
            HttpResponse resp = new BasicHttpResponse(new BasicStatusLine(
                    new ProtocolVersion("HTTP", 1, 1), responseCode, null));
            BasicHttpEntity ent = new BasicHttpEntity();
            if (responseStream != null) {
                ent.setContent(responseStream);
            }
            resp.setEntity(ent);
            return new TakHttpResponse(request, resp);
        }
        return new TakHttpResponse(request, _client.execute(request));
    }

    public TakHttpResponse execute(HttpRequestBase request,
            AtakAuthenticationCredentials credentials)
            throws IOException {
        addBasicAuthentication(request, credentials);

        Log.d(TAG, "executing request " + request.getRequestLine());
        return new TakHttpResponse(request, _client.execute(request));
    }

    public void shutdown() {
        if (_client != null)
            _client.getConnectionManager().shutdown();
    }

    public static final class Builder implements IHttpClientBuilder {

        private final TakHttpClient _client;
        private final String _url;
        private HttpRequestBase _request;
        private InputStream _body;

        private final Map<String, String> _queryParams;
        private final Map<String, String> _headers;


        public Builder(TakHttpClient client) {
            this(client, client.getUrl());
        }

        // XXX - allow passthrough of URL as `TakHttpClient` will append `:8080/Marti`...
        public Builder(TakHttpClient client, String url) {
            _client = client;
            _url = url;

            _queryParams = new HashMap<>();
            _headers = new HashMap<>();
        }

        @Override
        public IHttpClientBuilder addQueryParameter(String key, String value) {
            _queryParams.put(key, value);
            return this;
        }

        @Override
        public IHttpClientBuilder addHeader(String key, String value) {
            _headers.put(key, value);
            return this;
        }

        @Override
        public IHttpClientBuilder setBody(String body, String mime) {
            return this;
        }

        @Override
        public IHttpClientBuilder setBody(byte[] body, String mime) {
            return this;
        }

        @Override
        public IHttpClientBuilder setBody(InputStream body, String mime) {
            _body = body;
            return this;
        }

        private String url() {
            if (_queryParams.isEmpty())
                return _url;
            StringBuilder url = new StringBuilder(_url);
            char sep = (_url.indexOf('?') < 0) ? '?' : '&';
            for (Map.Entry<String, String> param : _queryParams.entrySet()) {
                url.append(sep);
                url.append(param.getKey());
                url.append('=');
                url.append(param.getValue());
                sep = '&';
            }

            return url.toString();
        }

        @Override
        public IHttpClient post() {
            _request = new HttpPost(url());
            for (Map.Entry<String, String> header : _headers.entrySet())
                _request.addHeader(header.getKey(), header.getValue());
            // XXX - body
            return new ClientImpl(_client, _request);
        }

        @Override
        public IHttpClient get() {
            _request = new HttpGet(url());
            for (Map.Entry<String, String> header : _headers.entrySet())
                _request.addHeader(header.getKey(), header.getValue());
            return new ClientImpl(_client, _request);
        }
    }

    private static class ClientImpl implements IHttpClient {

        final TakHttpClient _client;
        final HttpRequestBase _request;

        ClientImpl(TakHttpClient client, HttpRequestBase request) {
            _client = client;
            _request = request;
        }

        @Override
        public void setOption(int opt, int value) {
        }

        @Override
        public void setOption(int opt, boolean value) {
        }

        @Override
        public void setOption(int opt, String value) {
        }

        @Override
        public IResponse execute() throws IOException {
            return _client.execute(_request);
        }
    }
}
