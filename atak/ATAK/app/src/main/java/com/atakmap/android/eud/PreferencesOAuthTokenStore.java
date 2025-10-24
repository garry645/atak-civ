
package com.atakmap.android.eud;

import android.content.SharedPreferences;

import gov.tak.api.engine.net.auth.IOAuthTokenStore;
import gov.tak.api.engine.net.auth.OAuthAccessToken;

class PreferencesOAuthTokenStore implements IOAuthTokenStore {
    SharedPreferences _prefs;

    PreferencesOAuthTokenStore(SharedPreferences prefs) {
        _prefs = prefs;
    }

    static String getTokenKey(String server, String clientId) {
        return "com.atakmap.android.eud.token[" + server + "][" + clientId
                + "]";
    }

    @Override
    public OAuthAccessToken.Data getTokenData(String server, String clientId) {
        final String refreshToken = _prefs
                .getString(getTokenKey(server, clientId), null);
        if (refreshToken == null)
            return null;
        OAuthAccessToken.Data t = new OAuthAccessToken.Data();
        t.refresh_token = refreshToken;
        return t;
    }

    @Override
    public void setTokenData(String server, String clientId,
            OAuthAccessToken.Data tokenData) {
        SharedPreferences.Editor prefs = _prefs.edit();
        prefs.putString(getTokenKey(server, clientId), tokenData.refresh_token);
        prefs.apply();
    }

    @Override
    public void deleteTokenData(String server, String clientId) {
        SharedPreferences.Editor prefs = _prefs.edit();
        prefs.remove(getTokenKey(server, clientId));
        prefs.apply();
    }
}
