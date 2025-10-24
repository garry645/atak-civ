package gov.tak.api.engine.net.auth;

import java.util.Map;

public interface IHttpClientAuthenticationHandlerSpi
{
    String getType();
    IHttpClientAuthenticationHandler create(String server, Map<String, String> extras);
}
