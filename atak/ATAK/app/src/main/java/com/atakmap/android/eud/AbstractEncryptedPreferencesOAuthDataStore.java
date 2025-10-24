
package com.atakmap.android.eud;

import android.content.SharedPreferences;
import android.util.Base64;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;

import gov.tak.api.engine.net.auth.OAuthAccessToken;

abstract class AbstractEncryptedPreferencesOAuthDataStore
        extends PreferencesOAuthTokenStore {
    private final static String TAG = "AbstractEncryptedPreferencesOAuthDataStore";

    protected static final String AndroidKeyStore = "AndroidKeyStore";
    protected static final String AES_MODE = "AES/GCM/NoPadding";

    KeyStore keyStore;
    final String keyAlias;
    final String provider;

    AbstractEncryptedPreferencesOAuthDataStore(SharedPreferences prefs,
            String alias, String provider) {
        super(prefs);

        keyAlias = alias;
        this.provider = provider;
    }

    @Override
    public final void setTokenData(String server, String clientId,
            OAuthAccessToken.Data tokenData) {
        try {
            initKeyStore();

            // encrypt the token
            Cipher cipher = (provider != null)
                    ? Cipher.getInstance(AES_MODE, provider)
                    : Cipher.getInstance(AES_MODE);

            SecureRandom randomSecureRandom = new SecureRandom();
            byte[] iv = new byte[12];
            randomSecureRandom.nextBytes(iv);

            initCipher(cipher, Cipher.ENCRYPT_MODE, iv);

            _prefs.edit()
                    .putString(getTokenKey(server, clientId) + ".iv",
                            Base64.encodeToString(iv, Base64.URL_SAFE))
                    .apply();

            OAuthAccessToken.Data encryptedTokenData = new OAuthAccessToken.Data();
            encryptedTokenData.refresh_token = encrypt(cipher,
                    tokenData.refresh_token);

            super.setTokenData(server, clientId, encryptedTokenData);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to encrypt token", t);
        }
    }

    final void initKeyStore() throws Exception {
        if (keyStore == null) {
            keyStore = KeyStore.getInstance(AndroidKeyStore);
            keyStore.load(null);

            if (!keyStore.containsAlias(keyAlias)) {
                generateKey();
            }
        }
    }

    protected abstract void generateKey() throws Exception;

    @Override
    public final OAuthAccessToken.Data getTokenData(String server,
            String clientId) {
        try {
            initKeyStore();

            // decrypt the token
            Cipher cipher = (provider != null)
                    ? Cipher.getInstance(AES_MODE, provider)
                    : Cipher.getInstance(AES_MODE);

            final String iv = _prefs
                    .getString(getTokenKey(server, clientId) + ".iv", null);
            if (iv == null)
                return null;

            initCipher(cipher, Cipher.DECRYPT_MODE,
                    Base64.decode(iv, Base64.URL_SAFE));

            OAuthAccessToken.Data encryptedToken = super.getTokenData(server,
                    clientId);
            if (encryptedToken == null)
                return null;
            OAuthAccessToken.Data decryptedToken = new OAuthAccessToken.Data();
            decryptedToken.refresh_token = decrypt(cipher,
                    encryptedToken.refresh_token);

            return decryptedToken;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to decrypt token", t);
            return null;
        }
    }

    private String encrypt(Cipher cipher, String data) throws Exception {
        if (data == null)
            return null;
        byte[] encodedBytes = cipher
                .doFinal(data.getBytes(FileSystemUtils.UTF8_CHARSET));
        return Base64.encodeToString(encodedBytes, Base64.URL_SAFE);
    }

    private String decrypt(Cipher cipher, String encrypted64) throws Exception {
        if (encrypted64 == null)
            return null;
        byte[] data = cipher
                .doFinal(Base64.decode(encrypted64, Base64.URL_SAFE));
        if (data == null)
            return null;
        return new String(data, FileSystemUtils.UTF8_CHARSET);
    }

    private void initCipher(Cipher c, int mode, byte[] iv)
            throws Exception {
        c.init(mode, getSecretKey(), new GCMParameterSpec(128, iv));
    }

    protected abstract java.security.Key getSecretKey() throws Exception;
}
