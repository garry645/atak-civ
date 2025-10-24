package gov.tak.api.engine.net;

import com.atakmap.interop.Pointer;
import com.atakmap.map.EngineLibrary;
import com.atakmap.util.ReadWriteLock;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;

import gov.tak.api.annotation.DontObfuscate;
import gov.tak.api.annotation.NonNull;

final class LibcurlHttpClientBuilder implements IHttpClientBuilder
{
    static
    {
        EngineLibrary.initialize();
    }

    private final static int METHOD_GET = 0;
    private final static int METHOD_POST = 1;

    String _url;
    InputStream _requestBody;
    String _requestBodyContentType;
    Map<String, String> _queryParameters;
    Set<String> _headers;

    public LibcurlHttpClientBuilder(@NonNull String url)
    {
        Objects.requireNonNull(url);

        _url = url;
        _queryParameters = new HashMap<>();
        _headers = new HashSet<>();
    }

    @Override
    public LibcurlHttpClientBuilder addQueryParameter(@NonNull String key, @NonNull String value)
    {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        _queryParameters.put(key, value);
        return this;
    }

    @Override
    public LibcurlHttpClientBuilder addHeader(String key, String value) {
        _headers.add(key + ": " + value);
        return this;
    }

    @Override
    public LibcurlHttpClientBuilder setBody(String body, String mime) {
        return setBody(body.getBytes(Charset.forName("UTF-8")), mime);
    }

    @Override
    public LibcurlHttpClientBuilder setBody(byte[] body, String mime) {
        return setBody(new ByteArrayInputStream(body), mime);
    }

    @Override
    public LibcurlHttpClientBuilder setBody(InputStream body, String mime) {
        _requestBody = body;
        _requestBodyContentType = mime;
        return this;
    }

    @Override
    public IHttpClient post() {
        return new PostImpl.Client(url(), _headers, _requestBody, _requestBodyContentType);
    }

    @Override
    public IHttpClient get() {
        return new GetImpl.Client(url(), _headers);
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

    @DontObfuscate
    static abstract class ResponseImpl implements IResponse
    {
        final ReadWriteLock rwlock = new ReadWriteLock();
        java.nio.channels.Pipe _pipe;
        InputStream _in;
        OutputStream _sink;

        int _code = -1;
        long _contentLength = -1;
        int _curle = 0; // CURLE_OK

        Pointer pointer;

        ResponseImpl(int method, String url, Set<String> headers, Map<Integer, Object> options, InputStream body, String contentType) throws IOException {
            pointer = create(method);
            if(pointer.raw == 0L)
                throw new IOException("Failed to create client");

            _pipe = Pipe.open();
            _in = Channels.newInputStream(_pipe.source());
            _sink = Channels.newOutputStream(_pipe.sink());

            setUrl(pointer.raw, url);
            if(headers.size() > 0)
                setHeaders(pointer.raw, headers.toArray(new String[0]));
            if(body != null)
                setBody(pointer.raw, body, contentType);
            setResponseHandler(pointer.raw, this);

            for(Map.Entry<Integer, Object> option : options.entrySet()) {
                if(option.getValue() instanceof Integer)
                    setOptionInt(pointer.raw, option.getKey().intValue(), ((Integer)option.getValue()).intValue());
                else if(option.getValue() instanceof Boolean)
                    setOptionBool(pointer.raw, option.getKey().intValue(), ((Boolean)option.getValue()).booleanValue());
                else if(option.getValue() instanceof String)
                    setOptionString(pointer.raw, option.getKey().intValue(), (String)option.getValue());
            }

            // XXX -
            setOptionBool(pointer.raw, IHttpClient.OPTION_DISABLE_SSL_PEER_VERIFY, false);
        }

        @Override
        public InputStream getBody() {
            getCode();
            return _in;
        }

        @Override
        public int getCode() {
            rwlock.acquireRead();
            try {
                while (pointer.raw != 0L) {
                    synchronized (this) {
                        // wait for a response code or an API error
                        if (_code < 0 && _curle == 0)
                            try {
                                this.wait();
                                continue;
                            } catch (InterruptedException e) {
                            }
                        else
                            break;
                    }
                }
            } finally {
                rwlock.releaseRead();
            }
            return _code;
        }

        @Override
        public long getLength() {
            getCode();
            return _contentLength;
        }

        @Override
        public void close() {
            if(_in != null) {
                try {
                    _in.close();
                } catch(IOException ignored) {}
            }
            rwlock.acquireWrite();
            try {
                if (pointer.raw != 0L)
                    destroy(pointer);
            } finally {
                rwlock.releaseWrite();
            }
        }
    }

    static class GetImpl extends ResponseImpl
    {

        GetImpl(String url, Set<String> headers, Map<Integer, Object> options) throws IOException
        {
            super(METHOD_GET, url, headers, options, null, null);

            start(pointer);
        }

        static class Client extends ClientImpl
        {
            Client(String url, Set<String> headers) {
                super(url, headers);
            }

            @Override
            ResponseImpl executeImpl() throws IOException {
                return new GetImpl(_url, _headers, _options);

            }
        }
    }

    static class PostImpl extends ResponseImpl
    {

        PostImpl(String url, Set<String> headers, Map<Integer, Object> options, InputStream body, String contentType) throws IOException
        {
            super(METHOD_POST, url, headers, options, body, contentType);

            start(pointer);
        }

        static class Client extends ClientImpl
        {
            private InputStream _body;
            private String _contentType;

            Client(String url, Set<String> headers, InputStream body, String contentType) {
                super(url, headers);

                _body = body;
                _contentType = contentType;
            }

            @Override
            ResponseImpl executeImpl() throws IOException {
                return new PostImpl(_url, _headers, _options, _body, _contentType);
            }
        }
    }

    static abstract class ClientImpl implements IHttpClient
    {
        final String _url;
        final Set<String> _headers;
        final Map<Integer, Object> _options;

        ClientImpl(String url, Set<String> headers) {
            _url = url;
            _headers = headers;
            _options = new HashMap<>();
        }

        abstract ResponseImpl executeImpl() throws IOException;

        @Override
        public final IResponse execute() throws IOException {
            ResponseImpl response = executeImpl();
            response.getCode();
            Throwable error = null;
            // handle some specific response codes
            switch(response._code) {
                case 404 :
                    error = new FileNotFoundException(_url);
                    break;
                default :
                    break;
            }
            if (error != null) {
                // handle specific CURL error codes
                switch (response._curle) {
                    case 0: //CURLE_OK = 0,
                        break;
                    case 1: //CURLE_UNSUPPORTED_PROTOCOL,    /* 1 */
                        error = new ProtocolException("Error code: " + response._curle);
                        break;
                    case 3: //CURLE_URL_MALFORMAT,           /* 3 */
                        error = new MalformedURLException("Error code: " + response._curle);
                        break;
                    case 6: //CURLE_COULDNT_RESOLVE_HOST,    /* 6 */
                        error = new UnknownHostException("Error code: " + response._curle);
                        break;
                    case 7: //CURLE_COULDNT_CONNECT,         /* 7 */
                        error = new ConnectException("Error code: " + response._curle);
                        break;
                    case 27: //CURLE_OUT_OF_MEMORY,           /* 27 */
                        error = new OutOfMemoryError("Error code: " + response._curle);
                        break;
                    case 28: //CURLE_OPERATION_TIMEDOUT,      /* 28 - the timeout time was reached */
                        error = new SocketTimeoutException("Error code: " + response._curle);
                        break;
                    case 35: //CURLE_SSL_CONNECT_ERROR,       /* 35 - wrong when connecting with SSL */
                        error = new SSLException("Error code: " + response._curle);
                        break;
                    case 47: //CURLE_TOO_MANY_REDIRECTS,      /* 47 - catch endless re-direct loops */
                        error = new ProtocolException("Too many redirects. Error code: " + response._curle);
                        break;
                    case 49: //CURLE_SETOPT_OPTION_SYNTAX,    /* 49 - Malformed setopt option */
                        error = new IllegalArgumentException("Error code: " + response._curle);
                        break;
                    case 53: //CURLE_SSL_ENGINE_NOTFOUND,     /* 53 - SSL crypto engine not found */
                        error = new SSLException("SSL crypto engine not found. Error code: " + response._curle);
                        break;
                    case 54: //CURLE_SSL_ENGINE_SETFAILED,    /* 54 - can not set SSL crypto engine as
                        //            default */
                        error = new SSLException("Cannot set SSL crypto engine as default. Error code: " + response._curle);
                        break;
                    case 58: //CURLE_SSL_CERTPROBLEM,         /* 58 - problem with the local certificate */
                        error = new SSLException("SSL Certificate problem. Error code: " + response._curle);
                        break;
                    case 59: //CURLE_SSL_CIPHER,              /* 59 - couldn't use specified cipher */
                        error = new SSLException("Couldn't used specified cipher. Error code: " + response._curle);
                        break;
                    case 60: //CURLE_PEER_FAILED_VERIFICATION, /* 60 - peer's certificate or fingerprint
                        //             wasn't verified fine */
                        error = new SSLPeerUnverifiedException("Error code: " + response._curle);
                        break;
                    case 64: //CURLE_USE_SSL_FAILED,          /* 64 - Requested FTP SSL level failed */
                        error = new SSLException("Requested FTP SSL level failed. Error code: " + response._curle);
                        break;
                    case 66: //CURLE_SSL_ENGINE_INITFAILED,   /* 66 - failed to initialise ENGINE */
                        error = new SSLException("Failed to initialize SSL engine. Error code: " + response._curle);
                        break;
                    case 68: //CURLE_TFTP_NOTFOUND,           /* 68 - file not found on server */
                        error = new FileNotFoundException(_url);
                        break;
                    case 77: //CURLE_SSL_CACERT_BADFILE,      /* 77 - could not load CACERT file, missing
                        //            or wrong format */
                        error = new SSLException("Could not load CACERT file. Missing or wrong format. Error code: " + response._curle);
                        break;
                    case 78: //CURLE_REMOTE_FILE_NOT_FOUND,   /* 78 - remote file not found */
                        error = new FileNotFoundException(_url);
                        break;
                    case 80: //CURLE_SSL_SHUTDOWN_FAILED,     /* 80 - Failed to shut down the SSL
                        //            connection */
                        error = new SSLException("Failed to shut down SSL connection. Error code: " + response._curle);
                        break;
                    case 82: //CURLE_SSL_CRL_BADFILE,         /* 82 - could not load CRL file, missing or
                        //            wrong format (Added in 7.19.0) */
                        error = new SSLException("Coult not load CRL file. Missing or wrong format. Error code: " + response._curle);
                        break;
                    case 83: //CURLE_SSL_ISSUER_ERROR,        /* 83 - Issuer check failed.  (Added in
                        //            7.19.0) */
                        error = new SSLException("Issuer check failed. Error code: " + response._curle);
                        break;
                    case 90: //CURLE_SSL_PINNEDPUBKEYNOTMATCH, /* 90 - specified pinned public key did not
                        //             match */
                        error = new SSLException("Specified pinned public key did not match. Error code: " + response._curle);
                        break;
                    case 91: //CURLE_SSL_INVALIDCERTSTATUS,   /* 91 - invalid certificate status */
                        error = new SSLException("Invalid certificate status. Error code: " + response._curle);
                        break;
                    default:
                        // post generic `IOException` with error code for all others
                        error = new IOException("Error code: " + response._curle);
                        break;
                }
            }
            if(error != null) {
                try {
                    // the response isn't being returned up the callstack, need to clean up
                    response.close();
                } catch(Throwable ignored) {}
                if(!(error instanceof IOException))
                    error = new IOException(error);
                throw (IOException)error;
            }
            return response;
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


    static native Pointer create(int method);
    static native void destroy(Pointer pointer);

    static native int setUrl(long pointer, String url);
    static native int setHeaders(long pointer, String[] headers);
    static native int setBody(long pointer, InputStream stream, String contentType);
    static native void setResponseHandler(long pointer, ResponseImpl response);

    static native void setOptionInt(long pointer, int opt, int value);
    static native void setOptionBool(long pointer, int opt, boolean value);
    static native void setOptionString(long pointer, int opt, String value);

    static native int start(Pointer pointer);
}
