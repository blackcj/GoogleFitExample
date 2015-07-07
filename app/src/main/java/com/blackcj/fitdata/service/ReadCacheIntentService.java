package com.blackcj.fitdata.service;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.activity.MainActivity;
import com.blackcj.fitdata.database.CacheManager;
import com.blackcj.fitdata.database.CupboardSQLiteOpenHelper;
import com.blackcj.fitdata.database.MockData;
import com.blackcj.fitdata.model.Workout;
import com.blackcj.fitdata.model.WorkoutReport;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import nl.qbusict.cupboard.QueryResultIterable;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by chris.black on 7/6/15.
 */
public class ReadCacheIntentService extends IntentService {

    private WorkoutReport workoutReport = new WorkoutReport();
    public final static String TAG = "ReadHistoricalService";
    private WeakReference<ResultReceiver> mReceiver;
    public ReadCacheIntentService() {
        super(TAG);
    }
    private boolean mockData = false;

    @Override
    protected void onHandleIntent(Intent intent) {
        ResultReceiver resultReceiver = intent.getParcelableExtra(MainActivity.RECEIVER_TAG);
        mReceiver = new WeakReference<>(resultReceiver);
        final CupboardSQLiteOpenHelper dbHelper = new CupboardSQLiteOpenHelper(this);
        final SQLiteDatabase mDb = dbHelper.getWritableDatabase();
        Utilities.TimeFrame mTimeFrame = (Utilities.TimeFrame) intent.getSerializableExtra("TimeFrame");;
        ArrayList<Workout> report;
        if (mockData) {
            switch (mTimeFrame) {
                case BEGINNING_OF_DAY: // 1 day
                    report = MockData.getDailyMockData().getWorkoutData();
                    break;
                case BEGINNING_OF_WEEK: // 1 week
                    report = MockData.getWeeklyMockData().getWorkoutData();
                    break;
                case BEGINNING_OF_MONTH: // 1 month
                    report = MockData.getMonthlyMockData().getWorkoutData();
                    break;
                case LAST_MONTH: // 1 month
                    report = MockData.getMonthlyMockData().getWorkoutData();
                    break;
                default:
                    report = MockData.getDailyMockData().getWorkoutData();
                    break;
            }
        } else {
            long startTime = Utilities.getTimeFrameStart(mTimeFrame);
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
            report = workoutReport.getWorkoutData();

        }
        ResultReceiver receiver = mReceiver.get();
        if(receiver != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList("workoutList", report);
            receiver.send(200, bundle);
        }else {
            Log.w(TAG, "Weak listener is NULL.");
        }
        dbHelper.close();
    }
}
