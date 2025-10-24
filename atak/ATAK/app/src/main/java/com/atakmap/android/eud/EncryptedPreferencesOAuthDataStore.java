
package com.atakmap.android.eud;

import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import javax.crypto.KeyGenerator;

final class EncryptedPreferencesOAuthDataStore
        extends AbstractEncryptedPreferencesOAuthDataStore {
    EncryptedPreferencesOAuthDataStore(SharedPreferences prefs, String alias) {
        super(prefs, alias, null);
    }

    @Override
    protected void generateKey() throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore);
            keyGenerator.init(
                    new KeyGenParameterSpec.Builder(keyAlias,
                            KeyProperties.PURPOSE_ENCRYPT
                                    | KeyProperties.PURPOSE_DECRYPT)
                                            .setBlockModes(
                                                    KeyProperties.BLOCK_MODE_GCM)
                                            .setEncryptionPaddings(
                                                    KeyProperties.ENCRYPTION_PADDING_NONE)
                                            .setRandomizedEncryptionRequired(
                                                    false)
                                            .build());
            keyGenerator.generateKey();
        } else {
            throw new UnsupportedClassVersionError("Requires Android M");
        }
    }

    @Override
    protected java.security.Key getSecretKey() throws Exception {
        return keyStore.getKey(keyAlias, null);
    }
}
