package com.example.todoapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class Constants {

    // Default Local API Base URL matching PHP built-in server (php -S 0.0.0.0:8080)
    // 10.0.2.2 is the Android Emulator loopback alias to host PC's 127.0.0.1
    public static final String DEFAULT_BASE_URL = "http://10.0.2.2:8080/";

    // Intent Extra Keys
    public static final String EXTRA_NOTE = "extra_note";
    public static final String EXTRA_IS_EDIT = "extra_is_edit";
    public static final String EXTRA_CATEGORY = "extra_category";

    // Date & Time Patterns
    public static final String DISPLAY_DATE_FORMAT = "yyyy-MM-dd hh:mm a";
    public static final String SERVER_DATE_FORMAT = "yyyy-MM-dd";

    // SharedPreferences
    private static final String PREF_NAME = "todoapp_prefs";
    private static final String KEY_BASE_URL = "api_base_url";
    private static final String KEY_USE_MOCK_FALLBACK = "use_mock_fallback";

    public static String getBaseUrl(Context context) {
        if (context == null) return DEFAULT_BASE_URL;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String url = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL);
        if ("http://10.0.2.2:8080/api/".equals(url)) {
            url = DEFAULT_BASE_URL;
            prefs.edit().putString(KEY_BASE_URL, DEFAULT_BASE_URL).apply();
        }
        return sanitizeUrl(url);
    }

    public static void setBaseUrl(Context context, String url) {
        if (context == null) return;
        String sanitized = sanitizeUrl(url);
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_BASE_URL, sanitized).apply();
    }

    public static void resetBaseUrl(Context context) {
        setBaseUrl(context, DEFAULT_BASE_URL);
    }

    /**
     * Ensures URLs work seamlessly inside Android Emulator.
     * In the Android Emulator:
     * - '127.0.0.1' and 'localhost' point to the virtual phone itself.
     * - The host PC is ALWAYS reached via '10.0.2.2'.
     */
    public static String sanitizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return DEFAULT_BASE_URL;
        }
        url = url.trim();

        // Convert localhost / 127.0.0.1 to 10.0.2.2 for Android emulator
        if (url.contains("://127.0.0.1")) {
            url = url.replace("://127.0.0.1", "://10.0.2.2");
        } else if (url.contains("://localhost")) {
            url = url.replace("://localhost", "://10.0.2.2");
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        if (!url.endsWith("/")) {
            url = url + "/";
        }
        return url;
    }
}
