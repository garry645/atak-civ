package gov.tak.api.engine.net.auth;

import android.os.SystemClock;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.annotation.Nullable;
import gov.tak.api.engine.net.HttpClientBuilder;
import gov.tak.api.engine.net.IHttpClient;
import gov.tak.api.engine.net.IResponse;
import gov.tak.platform.engine.net.auth.OAuthTokenEndpoint;

public final class OAuthAccessToken
{
    private static long ACCESS_TOKEN_REFRESH_THRESHOLD = 3000L;

    private Data _tokenData;
    private IOAuthTokenStore _tokenStore;
    private OAuthTokenEndpoint _tokenEndpoint;

    private long _systemClockAccessTokenExpiry;

    public OAuthAccessToken(@NonNull Data tokenData, @NonNull OAuthTokenEndpoint endpoint, @Nullable IOAuthTokenStore store)
    {
        Objects.requireNonNull(tokenData);
        Objects.requireNonNull(endpoint);

        _tokenData = tokenData;
        _tokenEndpoint = endpoint;
        _tokenStore = store;

        // token is considered expired
        _systemClockAccessTokenExpiry = SystemClock.uptimeMillis();
    }

    public synchronized String accessToken()
    {
        if((_systemClockAccessTokenExpiry-SystemClock.uptimeMillis()) < ACCESS_TOKEN_REFRESH_THRESHOLD) {
            try {
                final Data token = _tokenEndpoint.refeshToken(_tokenData);
                if(token != null && token.error != null) {
                    // an error response was received from the server; consider the token revoked
                    if(_tokenStore != null)
                        _tokenStore.deleteTokenData(_tokenEndpoint.getAuthServerUrl(), _tokenEndpoint.getClientId());
                    _tokenData = null;
                } else if(token != null) {
                    _tokenData = token;
                    _systemClockAccessTokenExpiry = SystemClock.uptimeMillis() + (1000L*token.expires_in);
                    if(_tokenStore != null)
                        _tokenStore.setTokenData(_tokenEndpoint.getAuthServerUrl(), _tokenEndpoint.getClientId(), token);
                }
            } catch(IOException ignored) {
                // we failed to refresh the token because of an IO issue, return current
            }
        }
        return (_tokenData == null)?null:_tokenData.access_token;
    }

    public synchronized String refreshToken()
    {
        return (_tokenData != null) ? _tokenData.refresh_token : null;
    }

    public synchronized boolean isValid()
    {
        return _tokenData != null;
    }

    public synchronized void invalidate()
    {
        _tokenData = null;
        _tokenStore = null;
    }

    static Data refresh(String url, Data token, String clientId) throws IOException {
        String content = "client_id=" + clientId + "&grant_type=refresh_token&refresh_token=" + token.refresh_token;
        IHttpClient client = HttpClientBuilder.newBuilder(url)
                .setBody(content, "application/x-www-form-urlencoded")
                .post();
        try (IResponse response = client.execute()) {
            String s= IResponse.getString(response);
            return parseTokenData(s);
        }
    }

    public static Data parseTokenData(String response) {
        try {
            JSONObject json = new JSONObject(response);

            Data struct = new Data();
            struct.error = json.optString("error", null);
            struct.error_description = json.optString("error_description", null);
            struct.access_token = json.optString("access_token", null);
            struct.expires_in = json.optInt("expires_in");
            struct.refresh_expires_in = json.optInt("refresh_expires_in");
            struct.refresh_token = json.optString("refresh_token", null);
            struct.token_type = json.optString("token_type", null);
            struct.id_token = json.optString("id_token", null);
            struct.not_before_policy = json.optInt("not_before_policy");
            struct.session_state = json.optString("session_state", null);
            struct.scope = json.optString("scope", null);

            return struct;
        } catch(Throwable t) {
            return null;
        }
    }

    public final static class Data {
        public String error;
        public String error_description;
        public String access_token;
        public int expires_in;
        public int refresh_expires_in;
        public String refresh_token;
        public String token_type;
        public String id_token;
        public int not_before_policy;
        public String session_state;
        public String scope;
    }
}
