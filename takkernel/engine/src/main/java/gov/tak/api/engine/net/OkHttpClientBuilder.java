package gov.tak.api.engine.net;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.net.AtakCertificateDatabase;
import com.atakmap.net.CertificateManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import gov.tak.api.annotation.NonNull;
import gov.tak.platform.marshal.MarshalManager;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

final class OkHttpClientBuilder implements IHttpClientBuilder
{
    enum Method {
        Get,
        Post,
    }

    final static OkHttpClient _client = new OkHttpClient();
    final static Map<String, OkHttpClient> _localTrustClients = new ConcurrentHashMap<>();

    HttpUrl.Builder _url;
    Request.Builder _impl;
    RequestBody _body;
    IOException _bodyStreamError;
    final boolean _isLocalTrust;

    public OkHttpClientBuilder(@NonNull String url)
    {
        Objects.requireNonNull(url);
        final HttpUrl u = HttpUrl.parse(url);
        _url = u.newBuilder();
        _impl = new Request.Builder();

        _isLocalTrust = AtakCertificateDatabase.getCertificateForServer(ICertificateStore.TYPE_TRUST_STORE_CA, u.host()) != null;
    }

    @Override
    public IHttpClientBuilder addQueryParameter(@NonNull String key, @NonNull String value)
    {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        _url.addQueryParameter(key, value);
        return this;
    }

    @Override
    public IHttpClientBuilder addHeader(String key, String value) {
        _impl.addHeader(key, value);
        return this;
    }

    @Override
    public IHttpClientBuilder setBody(String body, String mime) {
        _body = (mime != null) ?
                RequestBody.create(body, MediaType.get(mime)) :
                RequestBody.create(body.getBytes(Charset.forName("UTF-8")));
        return this;
    }

    @Override
    public IHttpClientBuilder setBody(byte[] body, String mime) {
        _body = (mime != null) ?
                RequestBody.create(body, MediaType.get(mime)) :
                RequestBody.create(body);
        return this;
    }

    @Override
    public IHttpClientBuilder setBody(InputStream body, String mime) {
        try {
            return setBody(FileSystemUtils.read(body), mime);
        } catch(IOException e) {
            _bodyStreamError = e;
            return this;
        }
    }

    @Override
    public IHttpClient post() {
        final HttpUrl url = _url.build();
        final OkHttpClient client = _isLocalTrust ?
                getLocalTrustClient(url) :
                _client;
        return new ClientImpl(client, _impl.url(url).post(_body).build(), _bodyStreamError);
    }

    @Override
    public IHttpClient get() {
        final HttpUrl url = _url.build();
        final OkHttpClient client = _isLocalTrust ?
                getLocalTrustClient(url) :
                _client;
        return new ClientImpl(client, _impl.url(url).get().build(), null);
    }

    final class ResponseImpl implements IResponse
    {
        final Response _impl;

        ResponseImpl(Response impl) {
            _impl = impl;
        }

        @Override
        public InputStream getBody() {
            return _impl.body().byteStream();
        }

        @Override
        public int getCode() {
            return _impl.code();
        }

        @Override
        public long getLength() {
            return _impl.body().contentLength();
        }

        @Override
        public void close() {
            _impl.close();
        }
    }

    final class ClientImpl implements IHttpClient
    {
        final Request _request;
        final IOException _bodyStreamError;
        final OkHttpClient _client;

        OkHttpClient.Builder _customClient;

        ClientImpl(OkHttpClient client, Request request, IOException bodyStreamError) {
            _client = client;
            _customClient = null;
            _request = request;
            _bodyStreamError = bodyStreamError;
        }

        @Override
        public IResponse execute() throws IOException {
            if(_bodyStreamError != null)
                throw _bodyStreamError;

            OkHttpClient client = (_customClient != null) ?
                    _customClient.build() : _client;
            return new ResponseImpl(client.newCall(_request).execute());
        }

        @Override
        public void setOption(int opt, int value) {
            if(_customClient == null)
                _customClient = _client.newBuilder();
            switch(opt) {
                case OPTION_CONNECT_TIMEOUT:
                    _customClient.connectTimeout(value, TimeUnit.MILLISECONDS);
                    break;
                case OPTION_FOLLOW_REDIRECTS:
                    _customClient.followRedirects(value != 0);
                    break;
                case OPTION_READ_TIMEOUT:
                    _customClient.readTimeout(value, TimeUnit.MILLISECONDS);
                    break;
                case OPTION_DISABLE_SSL_PEER_VERIFY:
                default :
                    break;
            }
        }

        @Override
        public void setOption(int opt, boolean value) {
            if(_customClient == null)
                _customClient = _client.newBuilder();
            switch(opt) {
                case OPTION_FOLLOW_REDIRECTS:
                    _customClient.followRedirects(value);
                    break;
                case OPTION_CONNECT_TIMEOUT:
                case OPTION_READ_TIMEOUT:
                case OPTION_DISABLE_SSL_PEER_VERIFY:
                default :
                    break;
            }
        }

        @Override
        public void setOption(int opt, String value) {
            // no string options supported
        }
    }

    static OkHttpClient getLocalTrustClient(HttpUrl url) {
        OkHttpClient client = _localTrustClients.get(url.host());
        if(client != null)
            return client;

        final CertificateManager.ExtendedSSLSocketFactory f = CertificateManager
                .getSockFactory(true, "https://" + url.host() + ":" + url.port());
        final SSLSocketFactory sslSocketFactory = new SSLSocketFactory() {
            @Override
            public String[] getDefaultCipherSuites() {
                return ((SSLSocketFactory) SSLSocketFactory.getDefault()).getDefaultCipherSuites();
            }

            @Override
            public String[] getSupportedCipherSuites() {
                return ((SSLSocketFactory) SSLSocketFactory.getDefault()).getSupportedCipherSuites();
            }

            @Override
            public Socket createSocket(Socket socket, String host, int port, boolean autoclose) throws IOException {
                return f.createSocket(socket, host, port, autoclose);
            }

            @Override
            public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
                return f.createSocket(f.createSocket(), host, port, true);
            }

            @Override
            public Socket createSocket(String host, int port, InetAddress inetAddress, int localPort) throws IOException, UnknownHostException {
                return createSocket(host, port);
            }

            @Override
            public Socket createSocket(InetAddress inetAddress, int localPort) throws IOException {
                return f.createSocket();
            }

            @Override
            public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
                return f.createSocket(f.createSocket(), address.toString(), port, true);
            }
        };

        client = _client.newBuilder()
                    .sslSocketFactory(sslSocketFactory,
                            MarshalManager.marshal(f,
                                    CertificateManager.ExtendedSSLSocketFactory.class,
                                    X509TrustManager.class))
                    // the host is pinned for this certificate
                    .hostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String s, SSLSession sslSession) {
                            return Objects.equals(s, url.host()) && Objects.equals(sslSession.getPeerHost(), url.host());
                        }
                    })
                .build();
        _localTrustClients.put(url.host(), client);
        return client;
    }
}