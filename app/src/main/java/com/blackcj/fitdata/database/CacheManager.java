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
import com.blackcj.fitdata.model.Workout;
import com.blackcj.fitdata.model.WorkoutReport;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

import nl.qbusict.cupboard.QueryResultIterable;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by chris.black on 6/22/15.
 */
public class CacheManager {

    public static final String TAG = "CacheManager";
    private static SQLiteDatabase mDb;
    private static boolean mockData = false;
    private static WorkoutReport workoutReport = new WorkoutReport();
    private static HashMap<Utilities.TimeFrame, WeakReference<ICacheCallback>> cache = new HashMap<>();

    public CacheManager(SQLiteDatabase db) {
        mDb = db;
    }

    public void getReport(Utilities.TimeFrame timeFrame, ICacheCallback callback, Context context) {
        cache.put(timeFrame, new WeakReference<>(callback));
        Intent intentService = new Intent(context.getApplicationContext(), ReadHistoricalService.class);
        intentService.putExtra("TimeFrame", timeFrame);
        context.startService(intentService);
    }

    public static class ReadHistoricalService extends IntentService {

        public final static String TAG = "ReadHistoricalService";
        public ReadHistoricalService() {
            super(TAG);
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            Utilities.TimeFrame mTimeFrame = (Utilities.TimeFrame) intent.getSerializableExtra("TimeFrame");;
            List<Workout> report;
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
                    default:
                        report = MockData.getDailyMockData().getWorkoutData();
                        break;
                }
            } else {
                long startTime = Utilities.getTimeFrameStart(mTimeFrame);
                workoutReport.clearWorkoutData();
                QueryResultIterable<Workout> itr = cupboard().withDatabase(mDb).query(Workout.class).withSelection("start >= ?", "" + startTime).query();
                for (Workout workout : itr) {
                    if (workout.start > startTime) {
                        workoutReport.addWorkoutData(workout);
                    }
                }
                itr.close();
                report = workoutReport.getWorkoutData();

            }
            WeakReference<ICacheCallback> callbackRef = cache.get(mTimeFrame);
            if (callbackRef != null) {
                ICacheCallback callback = callbackRef.get();
                if (callback != null) {
                    callback.loadData(report);
                }
            }
        }
    }

    public interface ICacheCallback {
        public void loadData(List<Workout> workoutList);
    }

    /**
     * Retrieve data from the db cache and print it to the log.
     */
    private void printActivityData() {

        WorkoutReport report = new WorkoutReport();

        QueryResultIterable<Workout> itr = cupboard().withDatabase(mDb).query(Workout.class).query();
        for (Workout workout : itr) {
            report.addWorkoutData(workout);
        }
        itr.close();
        Log.i(TAG, report.toString());

    }
}
