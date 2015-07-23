package com.blackcj.fitdata.database;

import android.content.Context;

import com.blackcj.fitdata.model.Workout;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Device;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.SessionInsertRequest;

import java.util.concurrent.TimeUnit;

/**
 * Created by chris.black on 5/1/15.
 */
public class DataQueries {
    public static final String TAG = "FitData";
    /**
     * GET data by ACTIVITY
     *
     * The Google Fit API suggests that we don't want granular data, HOWEVER, we need granular
     * data to do anything remotely interesting with the data. This takes a while so we'll store
     * the results in a local db.
     *
     * After retrieving all activity segments, we go back and ask for step counts for each
     * segment. This allows us to summarize steps and duration per activity.
     *
     */
    public static DataReadRequest queryActivitySegment(long startTime, long endTime) {
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .read(DataType.TYPE_ACTIVITY_SEGMENT)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
        return readRequest;
    }

    public static DataReadRequest queryActivitySegmentBucket(long startTime, long endTime) {
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
        return readRequest;
    }

    /**
     * GET total estimated STEP_COUNT
     *
     * Retrieves an estimated step count.
     *
     */
    public static DataReadRequest queryStepEstimate(long startTime, long endTime) {
        DataSource ESTIMATED_STEP_DELTAS = new DataSource.Builder()
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setType(DataSource.TYPE_DERIVED)
                .setStreamName("estimated_steps")
                .setAppPackageName("com.google.android.gms")
                .build();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(ESTIMATED_STEP_DELTAS, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
        return readRequest;
    }

    /**
     * GET step count for a specific ACTIVITY.
     *
     * Retrieves a raw step count.
     *
     */
    public static DataReadRequest queryStepCount(long startTime, long endTime) {
        // [START build_read_data_request]
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
        // [END build_read_data_request]

        return readRequest;
    }

    /**
     * Sessions can include multiple data set types.
     *
     * @param startTime Start time for the activity in milliseconds
     * @param endTime End time for the activity in milliseconds
     * @param stepCountDelta Number of steps during the activity
     * @param activityName Name of the activity, must match FitnessActivities types
     * @param packageName Package name for the app
     * @return Resulting insert request
     */
    public static SessionInsertRequest createSession(long startTime, long endTime, int stepCountDelta, String activityName, String packageName, Device device) {
        // Create a session with metadata about the activity.
        Session session = new Session.Builder()
                .setName(TAG)
                //.setDescription("Long run around Shoreline Park")
                .setIdentifier(packageName + ":" + startTime)
                .setActivity(FitnessActivities.WALKING)
                .setStartTime(startTime, TimeUnit.MILLISECONDS)
                .setEndTime(endTime, TimeUnit.MILLISECONDS)
                .build();

        // Build a session insert request
        SessionInsertRequest insertRequest = new SessionInsertRequest.Builder()
                .setSession(session)
                .addDataSet(createStepDeltaDataSet(startTime, endTime, stepCountDelta, packageName, device))
                .addDataSet(createActivityDataSet(startTime, endTime, activityName, packageName, device))
                .build();

        return insertRequest;
    }

    /**
     * DataSets can only include one data type.
     *
     * @param startTime Start time for the activity in milliseconds
     * @param endTime End time for the activity in milliseconds
     * @param stepCountDelta Number of steps during the activity
     * @param packageName Package name for the app
     * @return Resulting DataSet
     */
    public static DataSet createStepDeltaDataSet(long startTime, long endTime, int stepCountDelta, String packageName, Device device) {

        // Create a data source
        DataSource dataSource = new DataSource.Builder()
                .setAppPackageName(packageName)
                .setDevice(device)
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setName(TAG + " - step count")
                .setType(DataSource.TYPE_RAW)
                .build();

        // Create a data set
        DataSet dataSet = DataSet.create(dataSource);
        // For each data point, specify a start time, end time, and the data value -- in this case,
        // the number of new steps.
        DataPoint dataPoint = dataSet.createDataPoint()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS);
        dataPoint.getValue(Field.FIELD_STEPS).setInt(stepCountDelta); // Can't do this on an Activity Segment
        dataSet.add(dataPoint);

        return dataSet;
    }

    public static DataSet createActivityDataSet(long startTime, long endTime, String activityName, String packageName, Device device) {

        // Create a data source
        DataSource dataSource = new DataSource.Builder()
                .setAppPackageName(packageName)
                .setDevice(device)
                .setDataType(DataType.TYPE_ACTIVITY_SEGMENT)
                .setName(TAG + " - activity")
                .setType(DataSource.TYPE_RAW)
                .build();

        // Create a data set
        DataSet dataSet = DataSet.create(dataSource);
        // For each data point, specify a start time, end time, and the data value -- in this case,
        // the number of new steps.
        DataPoint activityDataPoint = dataSet.createDataPoint()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS);
        //dataPoint.getValue(Field.FIELD_STEPS).setInt(stepCountDelta); // Can't do this on an Activity Segment
        activityDataPoint.getValue(Field.FIELD_ACTIVITY).setActivity(activityName);
        dataSet.add(activityDataPoint);

        return dataSet;
    }
}
