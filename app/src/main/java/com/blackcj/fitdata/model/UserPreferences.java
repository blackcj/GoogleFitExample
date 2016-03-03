package com.blackcj.fitdata.model;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by chris.black on 5/3/15.
 */
public class UserPreferences {
    public static final String PREFS_NAME = "GoogleFitExample";

    public static boolean getBackgroundLoadComplete(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        return settings.getBoolean("backgroundLoadComplete", false);
    }

    public static void setBackgroundLoadComplete(Context context, boolean value) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("backgroundLoadComplete", value);

        // Commit the edits!
        editor.commit();
    }

    public static boolean getCountSteps(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        return settings.getBoolean("shouldCountSteps", false);
    }

    public static void setCountSteps(Context context, boolean value) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("shouldCountSteps", value);

        // Commit the edits!
        editor.commit();
    }

    public static boolean getActivityTracking(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        return settings.getBoolean("shouldTrackActivity", false);
    }

    public static void setActivityTracking(Context context, boolean value) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("shouldTrackActivity", value);

        // Commit the edits!
        editor.commit();
    }

    public static long getLastSync(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        return settings.getLong("lastSuccessfulSync", 0);
    }

    public static void setLastSync(Context context, long value) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("lastSuccessfulSync", value);

        // Commit the edits!
        editor.commit();
    }

}
