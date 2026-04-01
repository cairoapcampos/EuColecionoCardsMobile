package com.eucolecionocards.session;

import android.content.Context;
import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class UserSession {
    private static final String KEY_LOCAL_USER_ID = "local_user_id";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_PROFILE_NAME = "perfil_nome";
    private static final String KEY_PROFILE_BIO = "perfil_bio";
    private static final String KEY_PROFILE_AVATAR = "perfil_avatar";

    private UserSession() {}

    public static void saveAuthSession(Context context, String userId, String accessToken, String refreshToken) {
        SharedPreferences prefs = getPrefs(context);
        String sanitizedUserId = sanitizeUuid(userId);
        String previousUserId = prefs.getString(KEY_LOCAL_USER_ID, "");

        // Evita reaproveitar perfil local de outra conta no mesmo dispositivo.
        if (previousUserId != null && !previousUserId.trim().isEmpty() && !previousUserId.equals(sanitizedUserId)) {
            prefs.edit()
                    .remove(KEY_PROFILE_NAME)
                    .remove(KEY_PROFILE_BIO)
                    .remove(KEY_PROFILE_AVATAR)
                    .apply();
        }

        prefs.edit()
                .putString(KEY_LOCAL_USER_ID, sanitizedUserId)
                .putString(KEY_ACCESS_TOKEN, safeValue(accessToken))
                .putString(KEY_REFRESH_TOKEN, safeValue(refreshToken))
                .apply();
    }

    public static boolean isLoggedIn(Context context) {
        return !getAccessToken(context).isEmpty() && !getUserId(context).isEmpty();
    }

    public static String getUserId(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String userId = prefs.getString(KEY_LOCAL_USER_ID, "");
        if (userId == null || userId.trim().isEmpty()) {
            String generated = normalizeToUuid("guest");
            prefs.edit().putString(KEY_LOCAL_USER_ID, generated).apply();
            return generated;
        }
        if (!isUuid(userId)) {
            String migrated = normalizeToUuid(userId);
            prefs.edit().putString(KEY_LOCAL_USER_ID, migrated).apply();
            return migrated;
        }
        return userId;
    }

    public static String getAccessToken(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String token = prefs.getString(KEY_ACCESS_TOKEN, "");
        return token == null ? "" : token.trim();
    }

    public static void clear(Context context) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit()
                .remove(KEY_LOCAL_USER_ID)
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .apply();
    }

    public static void clearProfileCache(Context context) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit()
                .remove(KEY_PROFILE_NAME)
                .remove(KEY_PROFILE_BIO)
                .remove(KEY_PROFILE_AVATAR)
                .apply();
    }

    public static boolean hasCompleteProfile(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String nome = prefs.getString(KEY_PROFILE_NAME, "");
        return nome != null && nome.trim().length() >= 2;
    }

    private static SharedPreferences getPrefs(Context context) {
        String prefsName = context.getPackageName() + "_preferences";
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
    }

    private static String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private static String sanitizeUuid(String userId) {
        String value = safeValue(userId);
        return isUuid(value) ? value : normalizeToUuid(value.isEmpty() ? "guest" : value);
    }

    private static String normalizeToUuid(String input) {
        String source = input == null ? "guest" : input.trim().toLowerCase();
        if (source.isEmpty()) {
            source = "guest";
        }
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static boolean isUuid(String value) {
        return value != null && value.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }
}
