package com.debloatyourtv.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages PIN lock, daily watch timer limits, day-bypass states, and app protection guards.
 */
public final class AppLockManager {
    private static final String PREF_NAME = "tvhome_app_lock";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_LOCKED_PKGS = "locked_packages";
    private static final String KEY_TIMER_LIMIT_MINS = "timer_limit_minutes_";
    private static final String KEY_USAGE_DATE = "usage_date";
    private static final String KEY_USAGE_MS = "usage_ms_";
    private static final String KEY_BYPASS_DATE = "bypass_date_";
    private static final String KEY_EXTRA_MINUTES = "extra_minutes_";

    private static final Set<String> sessionUnlocked = new HashSet<>();

    private AppLockManager() {}

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isPinSet(Context context) {
        return getPrefs(context).contains(KEY_PIN_HASH);
    }

    public static boolean setPin(Context context, String pin) {
        if (pin == null || pin.length() < 4) return false;
        String hash = sha256(pin);
        getPrefs(context).edit().putString(KEY_PIN_HASH, hash).apply();
        return true;
    }

    public static boolean verifyPin(Context context, String enteredPin) {
        if (enteredPin == null) return false;
        String storedHash = getPrefs(context).getString(KEY_PIN_HASH, null);
        if (storedHash == null) {
            return "1234".equals(enteredPin);
        }
        return storedHash.equals(sha256(enteredPin));
    }

    public static boolean isAppLocked(Context context, String packageName) {
        if (packageName == null) return false;
        if (sessionUnlocked.contains(packageName)) return false;
        Set<String> locked = getPrefs(context).getStringSet(KEY_LOCKED_PKGS, null);
        return locked != null && locked.contains(packageName);
    }

    public static void setAppLocked(Context context, String packageName, boolean locked) {
        if (packageName == null) return;
        SharedPreferences sp = getPrefs(context);
        Set<String> set = new HashSet<>(sp.getStringSet(KEY_LOCKED_PKGS, new HashSet<>()));
        if (locked) set.add(packageName);
        else set.remove(packageName);
        sp.edit().putStringSet(KEY_LOCKED_PKGS, set).apply();
    }

    public static void markSessionUnlocked(String packageName) {
        if (packageName != null) sessionUnlocked.add(packageName);
    }

    public static boolean isSessionUnlocked(String packageName) {
        return packageName != null && sessionUnlocked.contains(packageName);
    }

    public static void clearSessionLocks() {
        sessionUnlocked.clear();
    }

    public static int getTimerLimitMinutes(Context context, String packageName) {
        return getPrefs(context).getInt(KEY_TIMER_LIMIT_MINS + packageName, 0);
    }

    public static void setTimerLimitMinutes(Context context, String packageName, int minutes) {
        getPrefs(context).edit().putInt(KEY_TIMER_LIMIT_MINS + packageName, minutes).apply();
    }

    public static boolean isTimeLimitExceeded(Context context, String packageName) {
        int limitMins = getTimerLimitMinutes(context, packageName);
        if (limitMins <= 0) return false;
        if (isBypassedToday(context, packageName)) return false;

        int extra = getExtraMinutesToday(context, packageName);
        int effectiveLimit = limitMins + extra;

        long usedMs = getTodayUsageMs(context, packageName);
        return usedMs >= (effectiveLimit * 60_000L);
    }

    public static boolean isBypassedToday(Context context, String packageName) {
        String today = java.time.LocalDate.now().toString();
        String savedDate = getPrefs(context).getString(KEY_BYPASS_DATE + packageName, "");
        return today.equals(savedDate);
    }

    public static void bypassForToday(Context context, String packageName) {
        String today = java.time.LocalDate.now().toString();
        getPrefs(context).edit().putString(KEY_BYPASS_DATE + packageName, today).apply();
        markSessionUnlocked(packageName);
    }

    public static int getExtraMinutesToday(Context context, String packageName) {
        String today = java.time.LocalDate.now().toString();
        String savedDate = getPrefs(context).getString(KEY_USAGE_DATE, "");
        if (!today.equals(savedDate)) return 0;
        return getPrefs(context).getInt(KEY_EXTRA_MINUTES + packageName, 0);
    }

    public static void addExtraMinutes(Context context, String packageName, int additionalMinutes) {
        SharedPreferences sp = getPrefs(context);
        int current = getExtraMinutesToday(context, packageName);
        sp.edit().putInt(KEY_EXTRA_MINUTES + packageName, current + additionalMinutes).apply();
        markSessionUnlocked(packageName);
    }

    public static boolean isGuarded(Context context, String packageName) {
        if (packageName == null) return false;
        if (isSessionUnlocked(packageName)) return false;
        // Lock only when the timer is over:
        return isTimeLimitExceeded(context, packageName);
    }

    public static long getTodayUsageMs(Context context, String packageName) {
        SharedPreferences sp = getPrefs(context);
        String today = java.time.LocalDate.now().toString();
        String savedDate = sp.getString(KEY_USAGE_DATE, "");
        if (!today.equals(savedDate)) return 0;
        return sp.getLong(KEY_USAGE_MS + packageName, 0);
    }

    public static void recordUsage(Context context, String packageName, long deltaMs) {
        if (packageName == null || deltaMs <= 0) return;
        SharedPreferences sp = getPrefs(context);
        String today = java.time.LocalDate.now().toString();
        String savedDate = sp.getString(KEY_USAGE_DATE, "");
        SharedPreferences.Editor ed = sp.edit();
        if (!today.equals(savedDate)) {
            ed.putString(KEY_USAGE_DATE, today);
        }
        long current = today.equals(savedDate) ? sp.getLong(KEY_USAGE_MS + packageName, 0) : 0;
        ed.putLong(KEY_USAGE_MS + packageName, current + deltaMs);
        ed.apply();
    }

    public static Set<String> getLockedPackages(Context context) {
        return getPrefs(context).getStringSet(KEY_LOCKED_PKGS, new HashSet<>());
    }

    /**
     * Human-readable remaining watch time status for the app today.
     * Examples: "45m left today", "Locked for today", "Bypassed today", or null if no timer.
     */
    public static String getRemainingTimeDisplay(Context context, String packageName) {
        if (packageName == null) return null;
        int limitMins = getTimerLimitMinutes(context, packageName);
        if (limitMins <= 0) return null;

        if (isBypassedToday(context, packageName)) {
            return "Bypassed today";
        }

        int extra = getExtraMinutesToday(context, packageName);
        int effectiveLimit = limitMins + extra;
        long usedMs = getTodayUsageMs(context, packageName);
        long remainingMs = (effectiveLimit * 60_000L) - usedMs;

        if (remainingMs <= 0) {
            return "Locked for today";
        }

        int remainingMins = (int) Math.ceil(remainingMs / 60_000.0);
        if (remainingMins >= 60) {
            int h = remainingMins / 60;
            int m = remainingMins % 60;
            return m > 0 ? (h + "h " + m + "m left today") : (h + "h left today");
        }
        return remainingMins + "m left today";
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
