package com.google.android.gms.fit.samples.basichistoryapi.activity;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.fit.samples.basichistoryapi.Utilities;
import com.google.android.gms.fit.samples.basichistoryapi.database.CupboardSQLiteOpenHelper;
import com.google.android.gms.fit.samples.basichistoryapi.database.DataQueries;
import com.google.android.gms.fit.samples.basichistoryapi.R;
import com.google.android.gms.fit.samples.basichistoryapi.adapter.RecyclerViewAdapter;
import com.google.android.gms.fit.samples.basichistoryapi.model.UserPreferences;
import com.google.android.gms.fit.samples.basichistoryapi.model.Workout;
import com.google.android.gms.fit.samples.basichistoryapi.model.WorkoutReport;
import com.google.android.gms.fit.samples.basichistoryapi.model.WorkoutTypes;
import com.google.android.gms.fit.samples.common.logger.Log;
import com.google.android.gms.fit.samples.common.logger.LogView;
import com.google.android.gms.fit.samples.common.logger.LogWrapper;
import com.google.android.gms.fit.samples.common.logger.MessageOnlyLogFilter;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import nl.qbusict.cupboard.QueryResultIterable;

/**
 * This sample demonstrates how to use the History API of the Google Fit platform to insert data,
 * query against existing data, and remove data. It also demonstrates how to authenticate
 * a user with Google Play Services and how to properly represent data in a {@link DataSet}.
 */
public class MainActivity extends ApiClientActivity implements RecyclerViewAdapter.OnItemClickListener {

    public static final String DATE_FORMAT = "MM.dd h:mm a";

    private SQLiteDatabase db;
    private WorkoutReport report = new WorkoutReport();
    private RecyclerViewAdapter adapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarIcon(R.drawable.barchart_icon);

        initializeLogging();

        CupboardSQLiteOpenHelper dbHelper = new CupboardSQLiteOpenHelper(this);
        db = dbHelper.getWritableDatabase();

        List<Workout> items = new ArrayList<>(report.getWorkoutData());

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        adapter = new RecyclerViewAdapter(items, this);
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.contentView);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Grabs 30 days worth of data
                populateHistoricalData();
                populateReport();
            }
        });
    }

    Utilities.TimeFrame timeFrame = Utilities.TimeFrame.BEGINNING_OF_DAY;
    Utilities.TimeFrame lastPosition;

    protected void populateReport() {
        new ReadTodayDataTask().execute();
        lastPosition = timeFrame;
    }

    private void populateHistoricalData() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Run on executer to allow both tasks to run at the same time.
            // This task writes to the DB and the other reads so we shouldn't run into any issues.
            new ReadHistoricalDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }else {
            new ReadHistoricalDataTask().execute();
        }
    }

    Timer timer;

    @Override
    public void onConnect() {
        if(initialDisplay) {
            // Grabs 30 days worth of data
            populateHistoricalData();
            // Data load not complete, could take a while so lets show some data
            populateReport();
        } else {
            cancelTimer();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    // Grabs 30 days worth of data
                    populateHistoricalData();
                    // Data load not complete, could take a while so lets show some data
                    populateReport();
                    timer.cancel();
                }
            }, 750);
        }
    }

    private void cancelTimer() {
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        cancelTimer();
    }

    @Override
    public void onItemClick(View view, Workout viewModel) {
        if(viewModel.type == WorkoutTypes.TIME.getValue()) {
            cancelTimer();
            timeFrame = timeFrame.next();
            adapter.setNeedsAnimate();
            populateReport();
        } else {
            DetailActivity.launch(MainActivity.this, view.findViewById(R.id.image), viewModel);
        }

    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_main;
    }

    // Show partial data on first run to make the app feel faster
    private boolean initialDisplay = true;

    // TODO: Move these AsyncTask's to another class and call them via RetroFit
    private class ReadTodayDataTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {

            report.clearWorkoutData();

            long endTime = Utilities.getTimeFrameEnd(timeFrame);

            // Get data prior to today from cache
            long reportStartTime = Utilities.getTimeFrameStart(timeFrame);

            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            Log.i(TAG, "Range Start: " + dateFormat.format(reportStartTime));
            Log.i(TAG, "Range End: " + dateFormat.format(endTime));


            QueryResultIterable<Workout> itr = cupboard().withDatabase(db).query(Workout.class).withSelection("start > ?", "" + reportStartTime).query();
            for (Workout workout : itr) {
                if(workout.start > reportStartTime && workout.start < endTime) {
                    report.addWorkoutData(workout);
                }
            }
            itr.close();
            if(initialDisplay) {
                // TODO: Make sure the app is still in focus or this will crash.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Update the UI
                        List<Workout> items = report.getWorkoutData();
                        adapter.setItems(items, Utilities.getTimeFrameText(timeFrame));
                    }
                });
            }

            // We don't write the activity duration from the past two hours to the cache.
            // Grab past two hours worth of data.
            if(timeFrame != Utilities.TimeFrame.LAST_MONTH) {
                Calendar cal = Calendar.getInstance();
                Date now = new Date();
                cal.setTime(now);
                cal.add(Calendar.HOUR_OF_DAY, -2);
                long startTime = cal.getTimeInMillis();

                // Estimated duration by Activity within the past two hours
                DataReadRequest activitySegmentRequest = DataQueries.queryActivitySegmentBucket(startTime, endTime);
                DataReadResult dataReadResult = Fitness.HistoryApi.readData(mClient, activitySegmentRequest).await(1, TimeUnit.MINUTES);
                writeActivityDataToWorkout(dataReadResult);
            } else {
                // We don't need this data when reporting on last month.
            }

            // Estimated steps by bucket is more accurate than the step count by activity.
            // Replace walking step count total with this number to more closely match Google Fit.
            DataReadRequest stepCountRequest = DataQueries.queryStepEstimate(reportStartTime, endTime);
            DataReadResult stepCountReadResult = Fitness.HistoryApi.readData(mClient, stepCountRequest).await(1, TimeUnit.MINUTES);
            int stepCount = countStepData(stepCountReadResult);
            Workout workout = new Workout();
            workout.start = reportStartTime;
            workout.type = WorkoutTypes.WALKING.getValue();
            workout.stepCount = stepCount;
            report.setStepData(workout);

            // TODO: Make sure the app is still in focus or this will crash.
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Update the UI
                    List<Workout> items = new ArrayList<>(report.getWorkoutData());
                    adapter.setItems(items, Utilities.getTimeFrameText(timeFrame));
                    initialDisplay = false;
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            });

            return null;
        }
    }

    // TODO: Move these AsyncTask's to another class and call them via RetroFit
    private class ReadHistoricalDataTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {

            // Setting a start and end date using a range of 1 month before this moment.
            Calendar cal = Calendar.getInstance();
            Date now = new Date();
            cal.setTime(now);
            // You might be in the middle of a workout, don't cache the past two hours of data.
            // This could be an issue for workouts longer than 2 hours. Special case for that?
            cal.add(Calendar.HOUR_OF_DAY, -2);
            long endTime = cal.getTimeInMillis();
            long startTime = endTime;
            if(UserPreferences.getBackgroundLoadComplete(MainActivity.this)) {
                Workout w = cupboard().withDatabase(db).query(Workout.class).orderBy("start DESC").get();
                startTime = w.start - 1000*60*60*8; // Go back eight hours just to be safe
            } else {
                cal.add(Calendar.DAY_OF_YEAR, -45);
                startTime = cal.getTimeInMillis();
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

            Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
            Log.i(TAG, "Range End: " + dateFormat.format(endTime));

            // Estimated steps and duration by Activity
            DataReadRequest activitySegmentRequest = DataQueries.queryActivitySegment(startTime, endTime);
            DataReadResult dataReadResult = Fitness.HistoryApi.readData(mClient, activitySegmentRequest).await(1, TimeUnit.MINUTES);
            writeActivityDataToCache(dataReadResult);

            UserPreferences.setBackgroundLoadComplete(MainActivity.this, true);

            // Read cached data and calculate real time step estimates
            populateReport();

            return null;
        }
    }

    /**
     * Retrieve data from the db cache and print it to the log.
     */
    private void printActivityData() {
        WorkoutReport report = new WorkoutReport();

        QueryResultIterable<Workout> itr = cupboard().withDatabase(db).query(Workout.class).query();
        for (Workout workout : itr) {
            report.addWorkoutData(workout);
        }
        itr.close();
        Log.i(TAG, report.toString());
    }

    private boolean writeActivityDataToCache(DataReadResult dataReadResult) {
        boolean wroteDataToCache = false;
        for (DataSet dataSet : dataReadResult.getDataSets()) {
            wroteDataToCache = wroteDataToCache || writeDataSetToCache(dataSet);
        }
        return wroteDataToCache;
    }

    private void writeActivityDataToWorkout(DataReadResult dataReadResult) {
        for (Bucket bucket : dataReadResult.getBuckets()) {
            for (DataSet dataSet : bucket.getDataSets()) {
                parseDataSet(dataSet);
            }
        }
    }

    /**
     * Count step data for a bucket of step count deltas.
     *
     * @param dataReadResult Read result from the step count estimate Google Fit call.
     * @return Step count for data read.
     */
    private int countStepData(DataReadResult dataReadResult) {
        int stepCount = 0;
        for (Bucket bucket : dataReadResult.getBuckets()) {
            for (DataSet dataSet : bucket.getDataSets()) {
                stepCount += parseDataSet(dataSet);
            }
        }
        return stepCount;
    }

    /**
     * Walk through all activity fields in a segment dataset and writes them to the cache. Used to
     * store data to display in reports and graphs.
     *
     * @param dataSet
     */
    private boolean writeDataSetToCache(DataSet dataSet) {
        boolean wroteDataToCache = false;
        for (DataPoint dp : dataSet.getDataPoints()) {
            // Populate db cache with data
            for(Field field : dp.getDataType().getFields()) {
                if(field.getName().equals("activity") && dp.getDataType().getName().equals("com.google.activity.segment")) {
                    long startTime = dp.getStartTime(TimeUnit.MILLISECONDS);
                    int activity = dp.getValue(field).asInt();
                    Workout workout = cupboard().withDatabase(db).get(Workout.class, startTime);

                    // When the workout is null, we need to cache it. If the background task has completed,
                    // then we have at most 8 - 12 hours of data. Recent data is likely to change so over-
                    // write it.
                    if(workout == null || UserPreferences.getBackgroundLoadComplete(MainActivity.this)) {
                        long endTime = dp.getEndTime(TimeUnit.MILLISECONDS);
                        DataReadRequest readRequest = DataQueries.queryStepCount(startTime, endTime);
                        DataReadResult dataReadResult = Fitness.HistoryApi.readData(mClient, readRequest).await(1, TimeUnit.MINUTES);
                        int stepCount = countStepData(dataReadResult);
                        workout = new Workout();
                        workout._id = startTime;
                        workout.start = startTime;
                        workout.duration = endTime - startTime;
                        workout.stepCount = stepCount;
                        workout.type = activity;
                        //Log.v("MainActivity", "Put Cache: " + WorkoutTypes.getWorkOutTextById(workout.type) + " " + workout.duration);
                        cupboard().withDatabase(db).put(workout);
                        wroteDataToCache = true;
                    } else {
                        // Do not overwrite data if the initial load is in progress. This would take too
                        // long and prevent us from accumulating a base set of data.
                    }
                }
            }
        }
        return wroteDataToCache;
    }

    /**
     * Walk through all fields in a step_count dataset and return the sum of steps. Used to
     * calculate step counts.
     *
     * @param dataSet
     */
    private int parseDataSet(DataSet dataSet) {
        int dataSteps = 0;
        for (DataPoint dp : dataSet.getDataPoints()) {
            // Accumulate step count for estimate

            if(dp.getDataType().getName().equals("com.google.step_count.delta")) {
                for (Field field : dp.getDataType().getFields()) {
                    if (dp.getValue(field).asInt() > 0) {
                        dataSteps += dp.getValue(field).asInt();
                    }
                }
            }else {
                Workout workout = new Workout();
                workout.start = 0;
                workout.stepCount = 0;
                for (Field field : dp.getDataType().getFields()) {

                    String fieldName = field.getName();
                    if(fieldName.equals("activity")) {
                        workout.type = dp.getValue(field).asInt();
                    }else if(fieldName.equals("duration")) {
                        workout.duration = dp.getValue(field).asInt();
                    }
                }
                report.addWorkoutData(workout);
            }
        }
        return dataSteps;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh_data) {
            if(connected) {
                UserPreferences.setBackgroundLoadComplete(MainActivity.this, false);
                populateHistoricalData();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     *  Initialize a custom log class that outputs both to in-app targets and logcat.
     */
    private void initializeLogging() {
        // Wraps Android's native log framework.
        LogWrapper logWrapper = new LogWrapper();
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.setLogNode(logWrapper);
        // Filter strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);
        // On screen logging via a customized TextView.
        LogView logView = (LogView) findViewById(R.id.sample_logview);
        logView.setTextAppearance(this, R.style.Log);
        logView.setBackgroundColor(Color.WHITE);
        msgFilter.setNext(logView);
        Log.i(TAG, "Ready");
    }
}
