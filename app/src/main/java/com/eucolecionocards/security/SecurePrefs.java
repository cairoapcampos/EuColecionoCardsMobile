package com.eucolecionocards.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

/**
 * Fornece uma instancia de SharedPreferences criptografada usando
 * AES-256 (GCM para valores, SIV para chaves) via AndroidX Security.
 *
 * Se a criacao falhar, cai de volta para SharedPreferences normal.
 */
public final class SecurePrefs {

    private static final String TAG = "SecurePrefs";
    private static final String PREFS_FILE = "eucolecionocards_secure_prefs";

    private SecurePrefs() {}

    public static SharedPreferences get(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            return EncryptedSharedPreferences.create(
                    PREFS_FILE,
                    masterKeyAlias,
                    context.getApplicationContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.e(TAG, "Falha ao criar EncryptedSharedPreferences, usando fallback", e);
            return context.getApplicationContext()
                    .getSharedPreferences(PREFS_FILE + "_fallback", Context.MODE_PRIVATE);
        }
    }
}

