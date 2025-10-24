package gov.tak.platform.engine.net.auth;

import gov.tak.api.engine.net.IHttpClientBuilder;
import gov.tak.api.engine.net.auth.IHttpClientAuthenticationHandler;
import gov.tak.api.engine.net.auth.OAuthAccessToken;

public final class OAuthAuthenticationHandler implements IHttpClientAuthenticationHandler
{
    OAuthAccessToken _token;

    public OAuthAuthenticationHandler(OAuthAccessToken token) {
        _token = token;
    }

    @Override
    public IHttpClientBuilder configureClient(IHttpClientBuilder builder) {
        final String accessToken = _token.accessToken();
        if(accessToken != null)
            builder.addHeader("Authorization", "Bearer " + accessToken);
        return builder;
    }
}
