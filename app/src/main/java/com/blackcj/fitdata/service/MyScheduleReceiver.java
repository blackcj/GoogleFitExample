package com.blackcj.fitdata.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.blackcj.fitdata.model.UserPreferences;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import java.util.Calendar;
import java.util.Date;

import io.fabric.sdk.android.Fabric;

/**
 * Created by chris.black on 8/2/16.
 */
public class MyScheduleReceiver extends BroadcastReceiver {

    private static final String TAG = "MyScheduleReceiver";
    private static final String ALARM_ID = "com.blackcj.fitdata.MyScheduleReceiver";
    private static final int ALARM_CODE = 55340;

    private final Intent alarmIntent = new Intent(ALARM_ID);

    // restart service every 30 seconds
    private static final long REPEAT_TIME = 1000 * 60 * 60;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            return;
        }
        //Fabric.with(context.getApplicationContext(), new Crashlytics(), new Answers());
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long currentTime = cal.getTimeInMillis();
        if (currentTime - UserPreferences.getLastSync(context) < (REPEAT_TIME * 2) && intent.getAction().equals("com.blackcj.fitdata.START_REFRESH")) {
            Log.i(TAG, "Background refresh already running.");
            return;
        }
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")
                || intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON")
                || intent.getAction().equals("android.intent.action.REBOOT")
                || intent.getAction().equals("com.blackcj.fitdata.START_REFRESH")){


            AlarmManager alarmManager = (AlarmManager) context
                    .getSystemService(Context.ALARM_SERVICE);
            alarmIntent.setData(Uri.parse("custom://" + ALARM_CODE));
            PendingIntent pending = PendingIntent.getBroadcast(context, ALARM_CODE, alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            // adb shell dumpsys alarm > dump.txt
            // Cancel any existing alarms
            try {
                alarmManager.cancel(pending);
            } catch (Exception e) {
                Log.e(TAG, "AlarmManager update was not canceled. " + e.toString());
            }

            cal.add(Calendar.MINUTE, 5);
            // fetch every 30 seconds
            // InexactRepeating allows Android to optimize the energy consumption
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                    cal.getTimeInMillis(), REPEAT_TIME, pending);
            Log.i(TAG, "AlarmManager scheduled. " + ALARM_ID);

            /*Answers.getInstance().logCustom(new CustomEvent("Received Broadcast")
                    .putCustomAttribute("Class", "MyScheduleReceiver")
                    .putCustomAttribute("Details", "Scheduled alarm manager in MyScheduleReceiver"));*/
            // service.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
            // REPEAT_TIME, pending);
        }
    }
}
