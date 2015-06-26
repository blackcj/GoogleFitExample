package com.blackcj.fitdata.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.model.Workout;
import com.blackcj.fitdata.model.WorkoutReport;

import java.util.List;

import nl.qbusict.cupboard.QueryResultIterable;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by chris.black on 6/22/15.
 */
public class CacheManager {

    public static final String TAG = "CacheManager";
    final private SQLiteDatabase mDb;
    private boolean mockData = false;
    WorkoutReport workoutReport = new WorkoutReport();

    public CacheManager(SQLiteDatabase db) {
        mDb = db;
    }

    public List<Workout> getReport(Utilities.TimeFrame timeFrame) {
        List<Workout> report;
        if (mockData) {
            switch (timeFrame) {
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
            long startTime = Utilities.getTimeFrameStart(timeFrame);
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
        return report;
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
