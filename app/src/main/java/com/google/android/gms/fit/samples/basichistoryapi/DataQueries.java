package com.google.android.gms.fit.samples.basichistoryapi;

import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.request.DataReadRequest;

import java.util.concurrent.TimeUnit;

/**
 * Created by chris.black on 5/1/15.
 */
public class DataQueries {

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
}
