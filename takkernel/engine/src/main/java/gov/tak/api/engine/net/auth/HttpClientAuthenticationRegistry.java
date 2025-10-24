package gov.tak.api.engine.net.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpClientAuthenticationRegistry {
    final static Map<String, IHttpClientAuthenticationHandlerSpi> _spis = new ConcurrentHashMap<>();

    public static IHttpClientAuthenticationHandler createAuthenticationHandler(String type, String server, Map<String, String> extras)
    {
        IHttpClientAuthenticationHandlerSpi spi = _spis.get(type);
        if(spi == null)
            return null;
        return spi.create(server, extras);
    }

    public static void registerSpi(IHttpClientAuthenticationHandlerSpi spi)
    {
        _spis.put(spi.getType(), spi);
    }

    public static void unregisterSpi(IHttpClientAuthenticationHandlerSpi spi)
    {
        _spis.values().remove(spi);
    }
}
