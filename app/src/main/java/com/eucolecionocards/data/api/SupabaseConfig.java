package com.eucolecionocards.data.api;

import com.eucolecionocards.BuildConfig;

public final class SupabaseConfig {
    private SupabaseConfig() {}

    public static String getBaseUrl() {
        String value = BuildConfig.SUPABASE_URL == null ? "" : BuildConfig.SUPABASE_URL.trim();
        if (value.isEmpty()) {
            return "";
        }
        if (!value.endsWith("/")) {
            value += "/";
        }
        return value;
    }

    public static String getAnonKey() {
        return BuildConfig.SUPABASE_ANON_KEY == null ? "" : BuildConfig.SUPABASE_ANON_KEY.trim();
    }

    public static boolean isConfigured() {
        String url = getBaseUrl();
        String key = getAnonKey();
        return !url.isEmpty() && !key.isEmpty();
    }

    public static String buildPublicCardImageUrl(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return "";
        }
        String normalized = imagePath.trim();
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }
        return getBaseUrl() + "storage/v1/object/public/card-images/" + normalized;
    }
}
