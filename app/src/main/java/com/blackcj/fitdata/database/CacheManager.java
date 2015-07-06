package com.blackcj.fitdata.database;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ResultReceiver;
import android.util.Log;

import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.activity.MainActivity;
import com.blackcj.fitdata.model.Workout;
import com.blackcj.fitdata.model.WorkoutReport;
import com.blackcj.fitdata.service.CacheResultReceiver;
import com.blackcj.fitdata.service.ReadHistoricalService;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

import javax.xml.transform.Result;

import nl.qbusict.cupboard.QueryResultIterable;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by chris.black on 6/22/15.
 */
public class CacheManager {

    public static final String TAG = "CacheManager";

    public CacheManager() {
    }

    public static void getReport(Utilities.TimeFrame timeFrame, ResultReceiver callback, Context context) {
        Intent intentService = new Intent(context.getApplicationContext(), ReadHistoricalService.class);
        intentService.putExtra("TimeFrame", timeFrame);
        intentService.putExtra(MainActivity.RECEIVER_TAG, callback);
        context.startService(intentService);
    }
}
