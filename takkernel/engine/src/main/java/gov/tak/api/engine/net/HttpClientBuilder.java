package gov.tak.api.engine.net;

import java.lang.reflect.Constructor;

public abstract class HttpClientBuilder
{
    private static Class<? extends IHttpClientBuilder> _impl = OkHttpClientBuilder.class;

    private static Constructor<? extends IHttpClientBuilder> _ctor = null;

    private HttpClientBuilder() {}

    public static IHttpClientBuilder newBuilder(String url)
    {
        try {
            if(_ctor == null)
                _ctor = _impl.getConstructor(String.class);
            return _ctor.newInstance(url);
        } catch(Throwable t) {
            throw new RuntimeException("Failed to create new builder", t);
        }
    }
}
