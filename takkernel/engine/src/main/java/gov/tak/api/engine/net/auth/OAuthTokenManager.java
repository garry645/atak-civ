package gov.tak.api.engine.net.auth;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import gov.tak.api.annotation.NonNull;
import gov.tak.platform.engine.net.auth.OAuthTokenEndpoint;

public final class OAuthTokenManager
{
    IOAuthTokenStore _tokenStore;

    Map<String, WeakReference<OAuthAccessToken>> _tokens;

    public OAuthTokenManager(@NonNull IOAuthTokenStore store)
    {
        Objects.requireNonNull(store);
        _tokenStore = store;

        _tokens = new HashMap<>();
    }

    public synchronized OAuthAccessToken getToken(String server, String clientId)
    {
        final String endpointKey = getEndpointKey(server, clientId);
        do {
            WeakReference<OAuthAccessToken> tokenRef = _tokens.get(endpointKey);
            if (tokenRef == null)
                break;
            final OAuthAccessToken token = tokenRef.get();
            if(token == null)
                break;
            return token;
        } while(false);

        final OAuthAccessToken.Data tokenData = _tokenStore.getTokenData(server, clientId);
        if(tokenData == null)
            return null;
        final OAuthAccessToken token = new OAuthAccessToken(tokenData, new OAuthTokenEndpoint(server, clientId), _tokenStore);
        _tokens.put(endpointKey, new WeakReference<>(token));
        return token;
    }

    public synchronized void addToken(String server, String clientId, OAuthAccessToken.Data tokenData)
    {
        removeToken(server, clientId);
        _tokenStore.setTokenData(server, clientId, tokenData);
    }

    public synchronized void removeToken(String server, String clientId)
    {
        final String endpointKey = getEndpointKey(server, clientId);
        do {
            WeakReference<OAuthAccessToken> tokenRef = _tokens.remove(endpointKey);
            if (tokenRef == null)
                break;
            final OAuthAccessToken token = tokenRef.get();
            if(token == null)
                break;
            token.invalidate();
        } while(false);

        _tokenStore.deleteTokenData(server, clientId);
    }

    private static String getEndpointKey(String server, String clientId)
    {
        return server + "::" + clientId;
    }
}
