package gov.tak.platform.engine.net.auth;

import java.io.IOException;

import gov.tak.api.engine.net.HttpClientBuilder;
import gov.tak.api.engine.net.IHttpClient;
import gov.tak.api.engine.net.IResponse;
import gov.tak.api.engine.net.auth.OAuthAccessToken;

public final class OAuthTokenEndpoint
{
    private String _authServer;
    private String _clientId;


    public OAuthTokenEndpoint(String authUrl, String clientId)
    {
        _authServer = authUrl;
        _clientId = clientId;
    }

    public String getAuthServerUrl()
    {
        return _authServer;
    }

    public String getClientId()
    {
        return _clientId;
    }

    public OAuthAccessToken.Data authorizeDevice(String deviceCode) throws IOException
    {
        String content = "client_id=" + _clientId + "&grant_type=urn:ietf:params:oauth:grant-type:device_code&device_code=" + deviceCode;
        IHttpClient client = HttpClientBuilder.newBuilder(_authServer + "/token")
                .setBody(content, "application/x-www-form-urlencoded")
                .post();
        try (IResponse response = client.execute()) {
            String s = IResponse.getString(response);
            return OAuthAccessToken.parseTokenData(s);
        }
    }

    public OAuthAccessToken.Data refeshToken(OAuthAccessToken.Data token) throws IOException
    {
        if (token == null)
            return null;

        String content = "client_id=" + _clientId + "&grant_type=refresh_token&refresh_token=" + token.refresh_token;
        IHttpClient client = HttpClientBuilder.newBuilder(_authServer + "/token")
                .setBody(content, "application/x-www-form-urlencoded")
                .post();
        try (IResponse response = client.execute()) {
            String s = IResponse.getString(response);
            return OAuthAccessToken.parseTokenData(s);
        }
    }
}
