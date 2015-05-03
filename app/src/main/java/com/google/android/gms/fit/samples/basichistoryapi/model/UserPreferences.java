package com.google.android.gms.fit.samples.basichistoryapi.model;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by chris.black on 5/3/15.
 */
public class UserPreferences {
    public static final String PREFS_NAME = "GoogleFitExample";

    public static boolean getBackgroundLoadComplete(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        boolean complete = settings.getBoolean("backgroundLoadComplete", false);
        return complete;
    }

    public static void setBackgroundLoadComplete(Context context, boolean value) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("backgroundLoadComplete", value);

        // Commit the edits!
        editor.commit();
    }


}
