package de.gcffm.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Set;

public class PreferencesUtils {

    public static SharedPreferences getPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static String getString(Context context, int keyId, String defaultValue) {
        return getPreferences(context).getString(context.getString(keyId), defaultValue);
    }

    private static void setString(Context context, int keyId, String value) {
        getPreferences(context).edit()
                .putString(context.getString(keyId), value)
                .apply();
    }

    private static void setStringSet(Context context, int keyId, Set<String> values) {
        getPreferences(context).edit()
                .putStringSet(context.getString(keyId), values)
                .apply();
    }

    private static Set<String> getStringSet(Context context, int keyId, Set<String> defaultValue) {
        return getPreferences(context).getStringSet(context.getString(keyId), defaultValue);
    }

    private static int getInt(Context context, int keyId, int defaultValue) {
        return getPreferences(context).getInt(context.getString(keyId), defaultValue);
    }

    private static void setInt(Context context, int keyId, int value) {
        getPreferences(context).edit()
                .putInt(context.getString(keyId), value)
                .apply();
    }

    private static boolean getBoolean(Context context, int keyId, boolean defaultValue) {
        return getPreferences(context).getBoolean(context.getString(keyId), defaultValue);
    }

    private static void setBoolean(Context context, int keyId, boolean value) {
        getPreferences(context).edit()
                .putBoolean(context.getString(keyId), value)
                .apply();
    }

    public static int getMaxKm(Context context) {
        return getInt(context, R.string.PREF_MAX_KM, 100);
    }

    public static void setMaxKm(Context context, int value) {
        setInt(context, R.string.PREF_MAX_KM, value);
    }

    public static boolean getSortByDistance(Context context) {
        return getBoolean(context, R.string.PREF_SORT_BY_DISTANCE, false);
    }

    public static void setSortByDistance(Context context, boolean value) {
        setBoolean(context, R.string.PREF_SORT_BY_DISTANCE, value);
    }

    public static Set<String> getEventFilter(Context context) {
        return getStringSet(context, R.string.PREF_EVENT_FILTER, EventType.allAsSet());
    }

    public static void setEventFilter(Context context, Set<String> eventFilter) {
        setStringSet(context, R.string.PREF_EVENT_FILTER, eventFilter);
    }

    public static boolean getShowOldEvents(Context context) {
        return getBoolean(context, R.string.PREF_SHOW_OLD_EVENTS, true);
    }

    public static void setShowOldEvents(Context context, final boolean showOldEvents) {
        setBoolean(context, R.string.PREF_SHOW_OLD_EVENTS, showOldEvents);
    }

}
