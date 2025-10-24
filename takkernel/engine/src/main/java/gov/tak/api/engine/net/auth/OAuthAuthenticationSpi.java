package gov.tak.api.engine.net.auth;

import java.util.Map;

import gov.tak.platform.engine.net.auth.OAuthAuthenticationHandler;

public final class OAuthAuthenticationSpi implements IHttpClientAuthenticationHandlerSpi
{
    OAuthTokenManager _tokenManager;

    public OAuthAuthenticationSpi(OAuthTokenManager tokenManager)
    {
        _tokenManager = tokenManager;
    }

    @Override
    public String getType() {
        return "oauth2.0";
    }

    @Override
    public IHttpClientAuthenticationHandler create(String server, Map<String, String> extras) {
        final String clientId = extras.get("clientId");
        if(clientId == null)
            return null;
        final OAuthAccessToken token = _tokenManager.getToken(server, clientId);
        if(token == null)
            return null;
        return new OAuthAuthenticationHandler(token);
    }
}
