/*
 * Copyright (C) 2014 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.fit.samples.basichistoryapi.activity;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.fit.samples.basichistoryapi.database.CupboardSQLiteOpenHelper;
import com.google.android.gms.fit.samples.basichistoryapi.database.DataQueries;
import com.google.android.gms.fit.samples.basichistoryapi.R;
import com.google.android.gms.fit.samples.basichistoryapi.adapter.RecyclerViewAdapter;
import com.google.android.gms.fit.samples.basichistoryapi.model.Workout;
import com.google.android.gms.fit.samples.basichistoryapi.model.WorkoutReport;
import com.google.android.gms.fit.samples.common.logger.Log;
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
import java.util.concurrent.TimeUnit;

import nl.qbusict.cupboard.QueryResultIterable;

/**
 * This sample demonstrates how to use the History API of the Google Fit platform to insert data,
 * query against existing data, and remove data. It also demonstrates how to authenticate
 * a user with Google Play Services and how to properly represent data in a {@link DataSet}.
 */
public class MainActivity extends ApiClientActivity implements RecyclerViewAdapter.OnItemClickListener {


    public static final String DATE_FORMAT = "MM.dd h:mm a";


    private CupboardSQLiteOpenHelper dbHelper;
    private SQLiteDatabase db;
    private WorkoutReport report = new WorkoutReport();
    private RecyclerViewAdapter adapter;
    private RecyclerView recyclerView;
    private boolean needsHistoricalData = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarIcon(R.drawable.barchart_icon);

        dbHelper = new CupboardSQLiteOpenHelper(this);
        db = dbHelper.getWritableDatabase();

        ArrayList<Workout> items = new ArrayList<>(report.map.values());

        recyclerView = (RecyclerView) findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new RecyclerViewAdapter(items, this);
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    int lastPosition = -1;

    protected void populateReport() {

        if(lastPosition != position) {
            new ReadTodayDataTask().execute();
            lastPosition = position;
        }
    }

    @Override
    public void onConnect() {
        if(needsHistoricalData) {
            // Grabs 30 days worth of data
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                new ReadHistoricalDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }else {
                new ReadHistoricalDataTask().execute();
            }

            needsHistoricalData = false;
        }
        populateReport();
    }

    int[] values = {1, 7, 30};
    String[] valueDesc = {"Today","This Week","This Month"};
    int position = 0;

    @Override
    public void onItemClick(View view, Workout viewModel) {
        if(viewModel.type == -1) {
            position += 1;
            position = position % 3;
            populateReport();
        } else {
            DetailActivity.launch(MainActivity.this, view.findViewById(R.id.image), viewModel);
        }

    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_main;
    }

    private boolean initialDisplay = true;

    private class ReadTodayDataTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {

            report.map.clear();
            // Get data prior to today
            Calendar cal1 = Calendar.getInstance();
            Date now = new Date();
            cal1.setTime(now);
            cal1.add(Calendar.DAY_OF_YEAR, -values[position]);
            long reportStartTime = cal1.getTimeInMillis();
            QueryResultIterable<Workout> itr = cupboard().withDatabase(db).query(Workout.class).withSelection("start > ?", "" + reportStartTime).query();
            for (Workout workout : itr) {
                if(workout.start > reportStartTime) {
                    report.addWorkoutData(workout);
                } else {
                    // Ignore the workout
                    Log.i("Test","test");
                }
            }
            itr.close();
            if(initialDisplay) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //stuff that updates ui
                        ArrayList<Workout> items = new ArrayList<>(report.map.values());
                        adapter.setItems(items, valueDesc[position], initialDisplay);
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            // Setting a start and end date using a range of 1 month before this moment.
            Calendar cal = Calendar.getInstance();
            cal.setTime(now);
            long endTime = cal.getTimeInMillis();
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.add(Calendar.HOUR_OF_DAY, -2);
            long startTime = cal.getTimeInMillis();

            // Estimated steps and duration by Activity
            DataReadRequest activitySegmentRequest = DataQueries.queryActivitySegmentBucket(startTime, endTime);
            DataReadResult dataReadResult = Fitness.HistoryApi.readData(mClient, activitySegmentRequest).await(1, TimeUnit.MINUTES);
            writeActivityDataToWorkout(dataReadResult);

            cal.setTime(now);
            endTime = cal.getTimeInMillis();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            startTime = cal.getTimeInMillis();

            // Estimated steps by bucket
            DataReadRequest stepCountRequest = DataQueries.queryStepEstimate(startTime, endTime);
            dataReadResult = Fitness.HistoryApi.readData(mClient, stepCountRequest).await(1, TimeUnit.MINUTES);
            int stepCount = countStepData(dataReadResult);
            Workout workout = new Workout();
            workout.start = 0;
            workout.duration = 0;
            workout.type = 7;
            workout.stepCount = stepCount;
            report.replaceWorkoutData(workout);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //stuff that updates ui
                    ArrayList<Workout> items = new ArrayList<>(report.map.values());
                    adapter.setItems(items, valueDesc[position], !initialDisplay);
                    adapter.notifyDataSetChanged();
                    initialDisplay = false;
                }
            });


            return null;
        }
    }

    private class ReadHistoricalDataTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            // Setting a start and end date using a range of 1 month before this moment.
            Calendar cal = Calendar.getInstance();
            Date now = new Date();
            cal.setTime(now);
            //cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.add(Calendar.HOUR_OF_DAY, -2);
            long endTime = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_YEAR, -30);
            long startTime = cal.getTimeInMillis();

            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
            Log.i(TAG, "Range End: " + dateFormat.format(endTime));

            // Estimated steps and duration by Activity
            DataReadRequest activitySegmentRequest = DataQueries.queryActivitySegment(startTime, endTime);
            DataReadResult dataReadResult = Fitness.HistoryApi.readData(mClient, activitySegmentRequest).await(1, TimeUnit.MINUTES);
            writeActivityDataToCache(dataReadResult);

            return null;
        }
    }

    /**
     * Retrieve data from the db cache and display it on the screen.
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

    private void printEstimatedStepData(int estimatedSteps) {
        Log.i(TAG, "Estimated steps: " + estimatedSteps);
    }

    private void writeActivityDataToCache(DataReadResult dataReadResult) {
        for (DataSet dataSet : dataReadResult.getDataSets()) {
            writeDataSetToCache(dataSet);
        }
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
     * @param dataReadResult
     * @return
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
    private void writeDataSetToCache(DataSet dataSet) {
        for (DataPoint dp : dataSet.getDataPoints()) {
            // Populate db cache with data
            for(Field field : dp.getDataType().getFields()) {
                if(field.getName().equals("activity") && dp.getDataType().getName().equals("com.google.activity.segment")) {
                    long startTime = dp.getStartTime(TimeUnit.MILLISECONDS);
                    int activity = dp.getValue(field).asInt();
                    Workout workout = cupboard().withDatabase(db).get(Workout.class, startTime);
                    if(workout == null) {
                        long endTime = dp.getEndTime(TimeUnit.MILLISECONDS);
                        //Log.i(TAG, "\tField: " + field.getName() + " Value: " + dp.getValue(field));
                        DataReadRequest readRequest = DataQueries.queryStepCount(startTime, endTime);
                        DataReadResult dataReadResult = Fitness.HistoryApi.readData(mClient, readRequest).await(1, TimeUnit.MINUTES);
                        int stepCount = countStepData(dataReadResult);
                        workout = new Workout();
                        workout._id = startTime;
                        workout.start = startTime;
                        workout.duration = endTime - startTime;
                        workout.stepCount = stepCount;
                        workout.type = activity;
                        cupboard().withDatabase(db).put(workout);
                    }
                }
            }
        }
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
                    if(fieldName == "activity") {
                        workout.type = dp.getValue(field).asInt();
                    }else if(fieldName == "duration") {
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
        if (id == R.id.action_delete_data) {
            if(connected) {
                new ReadHistoricalDataTask().execute();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
