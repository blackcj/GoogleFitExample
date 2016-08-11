package com.blackcj.fitdata.service;

/**
 * Created by chris.black on 8/2/16.
 *
 * Receiver that's triggered by the alarm manager and kick starts the background refresh service. Is this
 * necessary or can we just fire the service directly from the alarm manager?
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyStartServiceReceiver extends BroadcastReceiver {
    private static final String TAG = "MyStartServiceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals("com.blackcj.fitdata.MyScheduleReceiver")) {
            Intent service = new Intent(context, BackgroundRefreshService.class);
            // TODO: Add refresh command to queue
            Log.i(TAG, "AlarmManager triggered background refresh.");

            context.startService(service);
        }
    }
}
