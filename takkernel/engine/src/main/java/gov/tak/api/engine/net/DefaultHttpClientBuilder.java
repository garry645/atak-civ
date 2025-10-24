package gov.tak.api.engine.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import gov.tak.api.annotation.NonNull;

final class DefaultHttpClientBuilder implements IHttpClientBuilder
{
    String _url;
    InputStream _requestBody;
    String _requestBodyContentType;
    Map<String, String> _queryParameters;
    Map<String, String> _headers;

    public DefaultHttpClientBuilder(@NonNull String url)
    {
        Objects.requireNonNull(url);

        _url = url;
        _queryParameters = new HashMap<>();
        _headers = new HashMap<>();
    }

    @Override
    public DefaultHttpClientBuilder addQueryParameter(@NonNull String key, @NonNull String value)
    {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        _queryParameters.put(key, value);
        return this;
    }

    @Override
    public DefaultHttpClientBuilder addHeader(String key, String value) {
        _headers.put(key, value);
        return this;
    }

    @Override
    public DefaultHttpClientBuilder setBody(String body, String mime) {
        return setBody(body.getBytes(Charset.forName("UTF-8")), mime);
    }

    @Override
    public DefaultHttpClientBuilder setBody(byte[] body, String mime) {
        return setBody(new ByteArrayInputStream(body), mime);
    }

    @Override
    public DefaultHttpClientBuilder setBody(InputStream body, String mime) {
        _requestBody = body;
        _requestBodyContentType = mime;
        return this;
    }

    @Override
    public IHttpClient post() {
        return new PostImpl(url(), _headers, _requestBody, _requestBodyContentType);
    }

    @Override
    public IHttpClient get() {
        return new GetImpl(url(), _headers);
    }

    private String url() {
        if(_queryParameters.isEmpty())
            return _url;
        StringBuilder url = new StringBuilder(_url);
        char sep = (_url.indexOf('?') < 0) ? '?' : '&';
        for(Map.Entry<String, String> param : _queryParameters.entrySet()) {
            url.append(sep);
            url.append(param.getKey());
            url.append('=');
            url.append(param.getValue());
            sep = '&';
        }

        return url.toString();
    }

    final class ResponseImpl implements IResponse
    {
        final HttpURLConnection _impl;
        InputStream _in;
        InputStream _err;
        int _code;
        long _contentLength;

        ResponseImpl(HttpURLConnection impl) throws IOException {
            _impl = impl;
            _code = -1;
            _contentLength = -1L;

            _code = _impl.getResponseCode();
            _in = _impl.getInputStream();
            _err = _impl.getErrorStream();
            _contentLength = _impl.getContentLength();
        }

        @Override
        public InputStream getBody() {
            return (_code/100 == 2) ? _in : _err;
        }

        @Override
        public int getCode() {
            return _code;
        }

        @Override
        public long getLength() {
            return _contentLength;
        }

        @Override
        public void close() {
            if(_in != null) {
                try {
                    _in.close();
                } catch(IOException ignored) {}
            }
            if(_err != null) {
                try {
                    _err.close();
                } catch(IOException ignored) {}
            }
        }
    }

    abstract class ClientImpl implements IHttpClient
    {
        final String _url;
        final Map<String, String> _headers;
        final String _method;
        final Map<Integer, Object> _options;

        ClientImpl(String method, String url, Map<String, String> headers) {
            _method = method;
            _url = url;
            _headers = headers;
            _options = new HashMap<>();
        }

        protected HttpURLConnection executeImpl() throws IOException
        {
            URL u = new URL(_url);
            HttpURLConnection c = (HttpURLConnection)u.openConnection();
            for(Map.Entry<String, String> header : _headers.entrySet())
                c.setRequestProperty(header.getKey(), header.getValue());
            c.setRequestMethod(_method);

            if(_options.containsKey(OPTION_CONNECT_TIMEOUT))
                c.setConnectTimeout(((Integer)_options.get(OPTION_CONNECT_TIMEOUT)).intValue());
            if(_options.containsKey(OPTION_READ_TIMEOUT))
                c.setReadTimeout(((Integer)_options.get(OPTION_READ_TIMEOUT)).intValue());
            if(_options.containsKey(OPTION_FOLLOW_REDIRECTS))
                c.setInstanceFollowRedirects(((Boolean)_options.get(OPTION_FOLLOW_REDIRECTS)).booleanValue());
            return c;
        }

        @Override
        public IResponse execute() throws IOException {
            return new ResponseImpl(executeImpl());
        }

        @Override
        public void setOption(int opt, int value) {
            _options.put(opt, value);
        }

        @Override
        public void setOption(int opt, boolean value) {
            _options.put(opt, value);
        }

        @Override
        public void setOption(int opt, String value) {
            _options.put(opt, value);
        }
    }

    final class GetImpl extends ClientImpl
    {
        GetImpl(String url, Map<String, String> headers) {
            super("GET", url, headers);
        }
    }

    final class PostImpl extends ClientImpl
    {
        final InputStream _requestBody;
        final String _contentType;

        PostImpl(String url, Map<String, String> headers, InputStream requestBody, String contentType) {
            super("POST", url, headers);
            _requestBody = requestBody;
            _contentType = contentType;
        }

        @Override
        protected HttpURLConnection executeImpl() throws IOException {
            HttpURLConnection c = super.executeImpl();
            if(_contentType != null)
                c.setRequestProperty("Content-Type", _contentType);
            c.setDoOutput(true);
            try (OutputStream out = c.getOutputStream()) {
                // XXX - should mark/rewind request body
                if(_requestBody != null) {
                    byte[] buf = new byte[4096];
                    while(true) {
                        int n = _requestBody.read(buf);
                        if(n < 0)
                            break;
                        if(n > 0)
                            out.write(buf, 0, n);
                    }
                }
            }
            return c;
        }
    }
}
