package com.blackcj.fitdata.service;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.blackcj.fitdata.activity.MainActivity;
import com.blackcj.fitdata.database.CupboardSQLiteOpenHelper;
import com.blackcj.fitdata.model.SummaryData;
import com.blackcj.fitdata.model.WorkoutReport;

import java.lang.ref.WeakReference;

/**
 * Created by chris.black on 7/8/15.
 */
public class SummaryCacheIntentService  extends IntentService {

    private WorkoutReport workoutReport = new WorkoutReport();
    public final static String TAG = "ReadHistoricalService";
    private WeakReference<ResultReceiver> mReceiver;
    public SummaryCacheIntentService() {
        super(TAG);
    }
    private boolean mockData = true;

    @Override
    protected void onHandleIntent(Intent intent) {
        ResultReceiver resultReceiver = intent.getParcelableExtra(MainActivity.RECEIVER_TAG);
        mReceiver = new WeakReference<>(resultReceiver);
        final CupboardSQLiteOpenHelper dbHelper = new CupboardSQLiteOpenHelper(this);
        final SQLiteDatabase mDb = dbHelper.getWritableDatabase();
        int workoutType = intent.getIntExtra("WorkoutType", 0);;
        SummaryData report = new SummaryData();
        if (mockData) {
            report.activityType = -2;
            report.averageDailyData = 10234;
            report.todayData = 4532;
            report.averageWeeklyData = 70000;
            report.weekData = 12430;
        } else {
            /*
            workoutReport.clearWorkoutData();
            if (!mDb.isOpen()) {
                Log.w(TAG, "db is closed!");
                return;
            }
            QueryResultIterable<Workout> itr = cupboard().withDatabase(mDb).query(Workout.class).withSelection("start >= ?", "" + startTime).query();
            for (Workout workout : itr) {
                if (workout.start > startTime) {
                    workoutReport.addWorkoutData(workout);
                }
            }
            itr.close();
            */
        }
        ResultReceiver receiver = mReceiver.get();
        if(receiver != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelable("workoutSummary", report);
            receiver.send(200, bundle);
        }else {
            Log.w(TAG, "Weak listener is NULL.");
        }
        dbHelper.close();
    }
}
