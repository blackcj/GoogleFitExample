package com.blackcj.fitdata.service;

/**
 * Created by chris.black on 8/2/16.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import io.fabric.sdk.android.Fabric;

public class MyStartServiceReceiver extends BroadcastReceiver {
    private static final String TAG = "MyStartServiceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals("com.blackcj.fitdata.MyScheduleReceiver")) {
            Intent service = new Intent(context, BackgroundRefreshService.class);
            // TODO: Add refresh command to queue
            Log.i(TAG, "AlarmManager triggered background refresh.");

            context.startService(service);

            /*Answers.getInstance().logCustom(new CustomEvent("Received Broadcast")
                    .putCustomAttribute("Class", "MyStartServiceReceiver")
                    .putCustomAttribute("Details", "Started background task in MyStartServiceReceiver"));*/
        }
    }
}
