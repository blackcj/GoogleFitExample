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
package com.google.android.gms.fit.samples.basichistoryapi;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.fit.samples.common.logger.Log;
import com.google.android.gms.fit.samples.common.logger.LogView;
import com.google.android.gms.fit.samples.common.logger.LogWrapper;
import com.google.android.gms.fit.samples.common.logger.MessageOnlyLogFilter;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nl.qbusict.cupboard.QueryResultIterable;
import java.util.Iterator;

/**
 * This sample demonstrates how to use the History API of the Google Fit platform to insert data,
 * query against existing data, and remove data. It also demonstrates how to authenticate
 * a user with Google Play Services and how to properly represent data in a {@link DataSet}.
 */
public class MainActivity extends ApiClientActivity {


    private static final String DATE_FORMAT = "yyyy.MM.dd HH:mm:ss";


    private CupboardSQLiteOpenHelper dbHelper;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarIcon(R.drawable.barchart_icon);

        final GridView gridView = (GridView) findViewById(R.id.gridView);
        gridView.setAdapter(new GridViewAdapter());
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Integer index = (Integer) view.getTag();
                Integer url = GridViewAdapter.mIcons[index];
                String title = GridViewAdapter.mTitles[index];
                DetailActivity.launch(MainActivity.this, view.findViewById(R.id.image), url, title);
            }
        });

        // This method sets up our custom logger, which will print all log messages to the device
        // screen, as well as to adb logcat.
        initializeLogging();

        dbHelper = new CupboardSQLiteOpenHelper(this);
        db = dbHelper.getWritableDatabase();

        printActivityData();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_main;
    }

    private class ReadDataTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            // Setting a start and end date using a range of 1 month before this moment.
            Calendar cal = Calendar.getInstance();
            Date now = new Date();
            cal.setTime(now);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.add(Calendar.DAY_OF_YEAR, 0);
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
            printActivityData();

            // Estimated steps by bucket
            DataReadRequest stepCountRequest = DataQueries.queryStepEstimate(startTime, endTime);
            dataReadResult = Fitness.HistoryApi.readData(mClient, stepCountRequest).await(1, TimeUnit.MINUTES);
            int stepCount = countStepData(dataReadResult);
            printEstimatedStepData(stepCount);

            return null;
        }
    }

    /**
     * Retrieve data from the db cache and display it on the screen.
     */
    private void printActivityData() {
        WorkoutReport report = new WorkoutReport();

        // Iterate Bunnys
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
            for(Field field : dp.getDataType().getFields()) {
                if(dp.getDataType().getName().equals("com.google.step_count.delta") && dp.getValue(field).asInt() > 0) {
                    dataSteps += dp.getValue(field).asInt();
                }
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
                new ReadDataTask().execute();
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

    private static class GridViewAdapter extends BaseAdapter {

        public static final Integer[] mIcons = new Integer[]{
                R.drawable.heart_icon_red,
                R.drawable.trends_icon,
                R.drawable.shoeprints_icon_color,
                R.drawable.biker_icon_color,
                R.drawable.car_icon_color,
                R.drawable.running_icon_color
        };

        public static final String[] mTitles = new String[]{
                "Heart",
                "Summary",
                "Walking",
                "Biking",
                "Driving",
                "Running"
        };

        @Override public int getCount() {
            return mIcons.length;
        }

        @Override public Object getItem(int i) {
            return mTitles[i];
        }

        @Override public long getItemId(int i) {
            return i;
        }

        @Override public View getView(int i, View view, ViewGroup viewGroup) {

            if (view == null) {
                view = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.grid_item, viewGroup, false);
            }

            ImageView image = (ImageView) view.findViewById(R.id.image);
            image.setImageResource(mIcons[i]);
            view.setTag(i);

            Bitmap bitmap = ((BitmapDrawable)image.getDrawable()).getBitmap();
            Palette palette = Palette.generate(bitmap);
            int vibrant = palette.getVibrantColor(0x000000);

            image.setBackgroundColor(Utilities.lighter(vibrant, 0.4f));
            LinearLayout container = (LinearLayout) view.findViewById(R.id.container);
            container.setBackgroundColor(vibrant);

            TextView text = (TextView) view.findViewById(R.id.text);
            text.setText(getItem(i).toString());

            return view;
        }


    }
}
