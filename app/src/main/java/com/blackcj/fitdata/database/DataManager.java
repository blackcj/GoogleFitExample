package com.blackcj.fitdata.database;

import android.app.Activity;
import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.content.IntentSender;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.model.UserPreferences;
import com.blackcj.fitdata.model.Workout;
import com.blackcj.fitdata.model.WorkoutTypes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Device;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.result.ListSubscriptionsResult;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by chris.black on 6/22/15.
 *
 * Used for reading from Google Fit and writing to the local database.
 */
public class DataManager {

    private static DataManager _instance;

    public static final String TAG = "DataManager";
    public static final String DATE_FORMAT = "MM.dd h:mm a";

    private WeakReference<SQLiteDatabase> mDb;
    private WeakReference<Context> mApplicationContext;
    private GoogleApiClient mClient;
    private final List<WeakReference<IDataManager>> mListeners;

    private DataManager(Activity context) {
        mApplicationContext = new WeakReference<>(context.getApplicationContext());
        this.mListeners = new ArrayList<>();
        buildFitnessClient(context);
    }

    public static DataManager getInstance(Activity context)
    {
        if (_instance == null)
        {
            _instance = new DataManager(context);
        }
        // TODO: Re-init context?
        return _instance;
    }

    private void notifyListeners() {
        int i = 0;
        for(int z = this.mListeners.size(); i < z; ++i) {
            WeakReference<IDataManager> ref = this.mListeners.get(i);
            if (ref != null) {
                IDataManager dataManager = ref.get();
                if (dataManager != null) {
                    dataManager.dataChanged();
                }
            }
        }
    }

    public void addListener(IDataManager listener) {
        int i = 0;

        for(int z = this.mListeners.size(); i < z; ++i) {
            WeakReference ref = (WeakReference)this.mListeners.get(i);
            if(ref != null && ref.get() == listener) {
                return;
            }
        }

        this.mListeners.add(new WeakReference<>(listener));
    }

    public void removeListener(IDataManager listener) {
        Iterator i = this.mListeners.iterator();

        while(true) {
            IDataManager item;
            do {
                if(!i.hasNext()) {
                    return;
                }

                WeakReference ref = (WeakReference)i.next();
                item = (IDataManager)ref.get();
            } while(item != listener && item != null);

            i.remove();
        }
    }

    public void connect() {
        if (!mClient.isConnecting() && !mClient.isConnected()) {
            mClient.connect();
        }
    }

    public void disconnect() {
        if (mClient.isConnected() && mListeners.size() == 0) {
            mClient.disconnect();
            connected = false;
        }
    }

    public void close() {
        if (mDb != null) {
            SQLiteDatabase db = mDb.get();
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    private SQLiteDatabase getDatabase() {

        if (mDb != null) {
            SQLiteDatabase db = mDb.get();
            if (db != null) {
                return db;
            }
        }

        Context context = getApplicationContext();
        if (context != null) {
            final CupboardSQLiteOpenHelper mHelper = new CupboardSQLiteOpenHelper(context);
            final SQLiteDatabase db = mHelper.getWritableDatabase();
            mDb = new WeakReference<>(db);
            return db;
        }

        return null;
    }

    private Context getApplicationContext() {
        if (mApplicationContext != null) {
            Context context = mApplicationContext.get();
            if (context != null) {
                return context;
            }
        }
        return null;
    }

    public void quickDataRead() {
        if (mClient.isConnected()) {
            Context context = getApplicationContext();
            long syncStart = Utilities.getTimeFrameStart(Utilities.TimeFrame.BEGINNING_OF_WEEK);
            if (context != null) {
                SQLiteDatabase db = getDatabase();
                if (db != null && db.isOpen()) {
                    //db.execSQL("DELETE FROM " + Workout.class.getSimpleName()); // This deletes all data
                    cupboard().withDatabase(db).delete(Workout.class, "start >= ?", "" + syncStart);
                }
                UserPreferences.setBackgroundLoadComplete(context, false);
                UserPreferences.setLastSync(context, syncStart);
                populateHistoricalData();
            }
        }
    }

    public void refreshData() {
        if (mClient.isConnected()) {
            Context context = getApplicationContext();
            long syncStart = Utilities.getTimeFrameStart(Utilities.TimeFrame.THIRTY_DAYS);
            if (context != null) {
                SQLiteDatabase db = getDatabase();
                if (db != null && db.isOpen()) {
                    //db.execSQL("DELETE FROM " + Workout.class.getSimpleName()); // This deletes all data
                    cupboard().withDatabase(db).delete(Workout.class, "start >= ?", "" + syncStart);
                }
                UserPreferences.setBackgroundLoadComplete(context, false);
                UserPreferences.setLastSync(context, syncStart);
                populateHistoricalData();
            }
        }
    }

    public void deleteDay() {
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        // Set a start and end time for our data, using a start time of 1 day before this moment.
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        long startTime = cal.getTimeInMillis();
        long syncStart = startTime - (1000 * 60 * 60 * 24);
        SQLiteDatabase db = getDatabase();
        if (db != null) {
            cupboard().withDatabase(db).delete(Workout.class, "start >= ?", "" + syncStart);
        } else {
            Log.w(TAG, "Warning: db is null");
        }

        // https://developers.google.com/android/reference/com/google/android/gms/fitness/request/DataDeleteRequest
        //  Create a delete request object, providing a data type and a time interval
        DataDeleteRequest request = new DataDeleteRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .deleteAllData()
                .deleteAllSessions()
                .build();

        // Invoke the History API with the Google API client object and delete request, and then
        // specify a callback that will check the result.
        Fitness.HistoryApi.deleteData(mClient, request)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Successfully deleted last day of data.");
                        } else {
                            // The deletion will fail if the requesting app tries to delete data
                            // that it did not insert.
                            Log.i(TAG, "Failed to delete workout");
                        }
                    }
                });
        Context context = getApplicationContext();
        if(context != null) {
            UserPreferences.setLastSync(context, syncStart);
        }
        populateHistoricalData();
    }

    public void deleteWorkout(final Workout workout) {
        // Set a start and end time for our data, using a start time of 1 day before this moment.
        long endTime = workout.start + workout.duration;
        long startTime = workout.start;
        long syncStart = workout.start - (1000 * 60 * 60 * 24);
        SQLiteDatabase db = getDatabase();
        if (db != null) {
            //cupboard().withDatabase(db).delete(Workout.class, "start >= ?", "" + syncStart);
            cupboard().withDatabase(db).delete(Workout.class, "start = ?", "" + startTime);
        } else {
            Log.w(TAG, "Warning: db is null");
        }

        // https://developers.google.com/android/reference/com/google/android/gms/fitness/request/DataDeleteRequest
        //  Create a delete request object, providing a data type and a time interval
        DataDeleteRequest request = new DataDeleteRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .deleteAllData()
                .deleteAllSessions()
                .build();

        // Invoke the History API with the Google API client object and delete request, and then
        // specify a callback that will check the result.
        Fitness.HistoryApi.deleteData(mClient, request)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Successfully deleted: " + workout.toString());
                        } else {
                            // The deletion will fail if the requesting app tries to delete data
                            // that it did not insert.
                            Log.i(TAG, "Failed to delete workout");
                        }
                    }
                });
        Context context = getApplicationContext();
        if(context != null) {
            UserPreferences.setLastSync(context, syncStart);
        }
        populateHistoricalData();
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

    // https://developers.google.com/fit/android/using-sessions
    public void insertData(Workout workout) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Run on executer to allow both tasks to run at the same time.
            // This task writes to the DB and the other reads so we shouldn't run into any issues.
            new InsertSessionTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, workout);
        }else {
            new InsertSessionTask().execute(workout);
        }
    }

    private class InsertSessionTask extends AsyncTask<Workout, Void, Void> {
        protected Void doInBackground(Workout... params) {

            Workout workout = params[0];
            String activityName = WorkoutTypes.getActivityTextById(workout.type);

            // Then, invoke the History API to insert the data and await the result, which is
            // possible here because of the {@link AsyncTask}. Always include a timeout when calling
            // await() to prevent hanging that can occur from the service being shutdown because
            // of low memory or other conditions.
            Log.i(TAG, "Inserting the session in the History API");
            Context context = getApplicationContext();
            if(context != null) {
                Device device = Device.getLocalDevice(context);
                com.google.android.gms.common.api.Status insertStatus =
                        Fitness.SessionsApi.insertSession(mClient, DataQueries.createSession(workout.start, workout.start + workout.duration, workout.stepCount, activityName, context.getPackageName(), device))
                                .await(1, TimeUnit.MINUTES);

                // Before querying the session, check to see if the insertion succeeded.
                if (!insertStatus.isSuccess()) {
                    Log.i(TAG, "There was a problem inserting the session: " +
                            insertStatus.getStatusMessage());
                    return null;
                }

                // At this point, the session has been inserted and can be read.
                Log.i(TAG, "Session insert was successful!");


                //cupboard().withDatabase(mDb).put(workout);
                UserPreferences.setBackgroundLoadComplete(context, false);
                UserPreferences.setLastSync(context, workout.start - (1000 * 60 * 60 * 24));

                populateHistoricalData();
            }

            return  null;
        }

    }

    private class InsertDataTask extends AsyncTask<Workout, Void, Void> {
        protected Void doInBackground(Workout... params) {

            Workout workout = params[0];
            String activityName = WorkoutTypes.getActivityTextById(workout.type);

            // Then, invoke the History API to insert the data and await the result, which is
            // possible here because of the {@link AsyncTask}. Always include a timeout when calling
            // await() to prevent hanging that can occur from the service being shutdown because
            // of low memory or other conditions.
            Log.i(TAG, "Inserting the dataset in the History API");
            Context context = getApplicationContext();
            if (context != null) {
                Device device = Device.getLocalDevice(context);
                com.google.android.gms.common.api.Status insertStatus =
                        Fitness.HistoryApi.insertData(mClient, DataQueries.createActivityDataSet(workout.start, workout.start + workout.duration, activityName, context.getPackageName(), device))
                                .await(1, TimeUnit.MINUTES);

                // Before querying the session, check to see if the insertion succeeded.
                if (!insertStatus.isSuccess()) {
                    Log.i(TAG, "There was a problem inserting the dataset: " +
                            insertStatus.getStatusMessage());
                    return null;
                }

                // At this point, the session has been inserted and can be read.
                Log.i(TAG, "Data insert was successful!");

                UserPreferences.setBackgroundLoadComplete(context, false);
                UserPreferences.setLastSync(context, workout.start - (1000 * 60 * 60 * 24));

                //populateHistoricalData();
            }

            return  null;
        }

    }

    /**
     *  Track whether an authorization activity is stacking over the current activity, i.e. when
     *  a known auth error is being resolved, such as showing the account chooser or presenting a
     *  consent dialog. This avoids common duplications as might happen on screen rotations, etc.
     */
    protected static final String AUTH_PENDING = "auth_state_pending";
    public boolean authInProgress = false;
    protected boolean connected = false;
    public static final int REQUEST_OAUTH = 1;

    /**
     *  Build a {@link GoogleApiClient} that will authenticate the user and allow the application
     *  to connect to Fitness APIs. The scopes included should match the scopes your app needs
     *  (see documentation for details). Authentication will occasionally fail intentionally,
     *  and in those cases, there will be a known resolution, which the OnConnectionFailedListener()
     *  can address. Examples of this include the user never having signed in before, or
     *  having multiple accounts on the device and needing to specify which account to use, etc.
     */
    private void buildFitnessClient(final Activity context) {
        if (context != null) {
            // Create the Google API Client
            mClient = new GoogleApiClient.Builder(context)
                    .addApi(Fitness.HISTORY_API)
                    .addApi(Fitness.SESSIONS_API)
                    .addApi(Fitness.RECORDING_API)
                    .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                    .addConnectionCallbacks(
                            new GoogleApiClient.ConnectionCallbacks() {
                                @Override
                                public void onConnected(Bundle bundle) {
                                    Log.i(TAG, "Connected!!!");
                                    // Now you can make calls to the Fitness APIs.  What to do?
                                    // Look at some data!!

                                    connected = true;
                                    onConnect();

                                }

                                @Override
                                public void onConnectionSuspended(int i) {
                                    // If your connection to the sensor gets lost at some point,
                                    // you'll be able to determine the reason and react to it here.
                                    if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                        Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                                    } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                        Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                                    }
                                    connected = false;
                                }
                            }
                    )
                    .addOnConnectionFailedListener(
                            new GoogleApiClient.OnConnectionFailedListener() {
                                // Called whenever the API client fails to connect.
                                @Override
                                public void onConnectionFailed(ConnectionResult result) {
                                    Log.i(TAG, "Connection failed. Cause: " + result.toString());
                                    if (!result.hasResolution()) {
                                        // Show the localized error dialog
                                        if (context != null) {
                                            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(),
                                                    context, 0).show();

                                        }
                                        return;
                                    }
                                    // The failure has a resolution. Resolve it.
                                    // Called typically when the app is not yet authorized, and an
                                    // authorization dialog is displayed to the user.
                                    if (!authInProgress) {
                                        try {
                                            Log.i(TAG, "Attempting to resolve failed connection");
                                            if (context != null) {
                                                authInProgress = true;
                                                result.startResolutionForResult(context,
                                                        REQUEST_OAUTH);
                                            }
                                        } catch (IntentSender.SendIntentException e) {
                                            Log.e(TAG,
                                                    "Exception while starting resolution activity", e);
                                        }
                                    }
                                }
                            }
                    )
                    .build();
        }

    }

    public void onConnect() {
        // TODO: Only populate history data on first load.
        //populateHistoricalData();
        // Data load not complete, could take a while so lets show some data
        //populateReport();
        //subscribeSteps();
    }

    public void setStepCounting(boolean active) {
        if (active) {
            subscribeSteps();
        }else {
            unsubscribeSteps();
        }
    }

    public void setActivityTracking(boolean active) {
        if (active) {
            subscribeActivity();
        }else {
            unsubscribeActivity();
        }
    }

    private void listSubscriptions() {
        Fitness.RecordingApi.listSubscriptions(mClient).setResultCallback(new ResultCallback<ListSubscriptionsResult>() {
            @Override
            public void onResult(ListSubscriptionsResult result) {
                for (Subscription sc : result.getSubscriptions()) {
                    DataType dt = sc.getDataType();
                    Log.i(TAG, "found subscription for data type: " + dt.getName());
                }
            }
        });
    }

    private void unsubscribeAll() {
        Fitness.RecordingApi.listSubscriptions(mClient).setResultCallback(new ResultCallback<ListSubscriptionsResult>() {
            @Override
            public void onResult(ListSubscriptionsResult result) {
                for (Subscription sc : result.getSubscriptions()) {
                    DataType dt = sc.getDataType();
                    Log.i(TAG, "Unsubscribing: " + dt.getName());
                    Fitness.RecordingApi.unsubscribe(mClient, sc)
                            .setResultCallback(new ResultCallback<Status>() {
                                @Override
                                public void onResult(Status status) {
                                    if (status.isSuccess()) {
                                        Log.i(TAG, "Successfully unsubscribed for data type: step count delta");
                                    } else {
                                        // Subscription not removed
                                        Log.i(TAG, "Failed to unsubscribe for data type: step count delta");
                                    }
                                }
                            });
                }
            }
        });
    }

    private void unsubscribeSteps() {
        Context context = getApplicationContext();
        if (context != null) {
            UserPreferences.setCountSteps(context, false);
            Fitness.RecordingApi.unsubscribe(mClient, DataType.TYPE_STEP_COUNT_DELTA)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                Log.i(TAG, "Successfully unsubscribed for data type: step count delta");
                            } else {
                                // Subscription not removed
                                Log.i(TAG, "Failed to unsubscribe for data type: step count delta");
                            }
                        }
                    });
        }
    }

    private void unsubscribeActivity() {
        Context context = getApplicationContext();
        if (context != null) {
            UserPreferences.setActivityTracking(context, false);
            Fitness.RecordingApi.unsubscribe(mClient, DataType.TYPE_ACTIVITY_SEGMENT)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                Log.i(TAG, "Successfully unsubscribed for data type: step count delta");
                            } else {
                                // Subscription not removed
                                Log.i(TAG, "Failed to unsubscribe for data type: step count delta");
                            }
                        }
                    });
        }
    }

    private void subscribeSteps() {
        Context context = getApplicationContext();
        if (context != null) {
            UserPreferences.setCountSteps(context, true);
            Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_STEP_COUNT_DELTA)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                if (status.getStatusCode()
                                        == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                    Log.i(TAG, "Existing subscription for activity detected.");
                                } else {
                                    Log.i(TAG, "Successfully subscribed!");
                                }
                            } else {
                                Log.i(TAG, "There was a problem subscribing.");
                            }
                        }
                    });
        }
    }

    private void subscribeActivity() {
        Context context = getApplicationContext();
        if (context != null) {
            UserPreferences.setActivityTracking(context, true);
            Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_ACTIVITY_SEGMENT)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                if (status.getStatusCode()
                                        == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                    Log.i(TAG, "Existing subscription for activity detected.");
                                } else {
                                    Log.i(TAG, "Successfully subscribed!");
                                }
                            } else {
                                Log.i(TAG, "There was a problem subscribing.");
                            }
                        }
                    });
        }
    }

    private class ReadHistoricalDataTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {

            // Setting a start and end date using a range of 1 month before this moment.
            Calendar cal = Calendar.getInstance();
            Date now = new Date();
            cal.setTime(now);
            // You might be in the middle of a workout, don't cache the past two hours of data.
            // This could be an issue for workouts longer than 2 hours. Special case for that?
            //cal.add(Calendar.HOUR_OF_DAY, -2);
            long endTime = cal.getTimeInMillis();
            long startTime;
            Context context = getApplicationContext();
            if (context != null) {
                long lastSync = UserPreferences.getLastSync(context);
                if (lastSync != 0) {
                    Log.i(TAG, "Fast data read starting: " + Utilities.getTimeDateString(lastSync));
                    startTime = lastSync; // Go back 8 hours just to be safe
                } else {
                    Log.i(TAG, "Slow data read");
                    cal.add(Calendar.DAY_OF_YEAR, -90);
                    startTime = cal.getTimeInMillis();
                }

                SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

                Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
                Log.i(TAG, "Range End: " + dateFormat.format(endTime));

                // Load today
                long dayStart = Utilities.getTimeFrameStart(Utilities.TimeFrame.BEGINNING_OF_DAY);
                if(startTime < dayStart && dayStart < endTime) {
                    Log.i(TAG, "Loading today");
                    // Estimated steps and duration by Activity
                    DataReadRequest activitySegmentRequest = DataQueries.queryActivitySegment(dayStart, endTime);
                    DataReadResult dataReadResult = Fitness.HistoryApi.readData(mClient, activitySegmentRequest).await(1, TimeUnit.MINUTES);
                    writeActivityDataToCache(dataReadResult);
                    endTime = dayStart;
                }



                // Load week
                long weekStart = Utilities.getTimeFrameStart(Utilities.TimeFrame.BEGINNING_OF_WEEK);

                Log.i(TAG, "Range Start: " + dateFormat.format(weekStart));
                Log.i(TAG, "Range End: " + dateFormat.format(endTime));
                if(startTime < weekStart && weekStart < endTime) {
                    Log.i(TAG, "Loading week");
                    // Estimated steps and duration by Activity
                    DataReadRequest activitySegmentRequest = DataQueries.queryActivitySegment(weekStart, endTime);
                    DataReadResult dataReadResult = Fitness.HistoryApi.readData(mClient, activitySegmentRequest).await(1, TimeUnit.MINUTES);
                    writeActivityDataToCache(dataReadResult);
                    endTime = weekStart;
                }

                Log.i(TAG, "Loading month");
                // Load rest
                if (startTime < endTime) {
                    DataReadRequest activitySegmentRequest = DataQueries.queryActivitySegment(startTime, endTime);
                    DataReadResult dataReadResult = Fitness.HistoryApi.readData(mClient, activitySegmentRequest).await(1, TimeUnit.MINUTES);
                    writeActivityDataToCache(dataReadResult);
                }

                cal.setTime(now);
                endTime = cal.getTimeInMillis();
                cal.set(Calendar.HOUR_OF_DAY, 1);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                startTime = cal.getTimeInMillis();

                int numberOfDays = 150;

                if (lastSync != 0) {
                    if (lastSync < startTime) {
                        double diff =  startTime - lastSync;
                        if (diff / (1000 * 60 * 60 * 24) < 30) {
                            numberOfDays = (int)Math.floor(diff / (1000 * 60 * 60 * 24));
                            numberOfDays += 1;
                        }
                    } else {
                        numberOfDays = 2;
                    }
                }
                Log.i(TAG, "Loading " + numberOfDays + " days step count");
                for (int i = 0; i < numberOfDays; i++) {
                    DataReadRequest stepCountRequest = DataQueries.queryStepEstimate(startTime, endTime);
                    DataReadResult stepCountReadResult = Fitness.HistoryApi.readData(mClient, stepCountRequest).await(1, TimeUnit.MINUTES);
                    int stepCount = countStepData(stepCountReadResult);
                    Workout workout = new Workout();
                    workout.start = startTime;
                    workout._id = startTime;
                    workout.type = WorkoutTypes.STEP_COUNT.getValue();
                    workout.stepCount = stepCount;
                    //workout.duration = 1000*60*10;
                    Log.i(TAG, "Step count: " + stepCount);
                    SQLiteDatabase db = getDatabase();
                    if (db != null && db.isOpen()) {
                        cupboard().withDatabase(db).put(workout);
                    } else {
                        Log.w(TAG, "Warning: db is null");
                        return null;
                    }

                    endTime = startTime;
                    cal.add(Calendar.DAY_OF_YEAR, -1);
                    startTime = cal.getTimeInMillis();
                }
                cal.setTime(now);
                Log.i(TAG, "Background load complete");
                if (UserPreferences.getBackgroundLoadComplete(context)) {
                    UserPreferences.setLastSync(context, cal.getTimeInMillis());
                } else {
                    UserPreferences.setBackgroundLoadComplete(context, true);
                }
                notifyListeners();

                // Read cached data and calculate real time step estimates
                //populateReport();
            }

            return null;
        }
    }

    private boolean writeActivityDataToCache(DataReadResult dataReadResult) {
        boolean wroteDataToCache = false;
        for (DataSet dataSet : dataReadResult.getDataSets()) {
            wroteDataToCache = wroteDataToCache || writeDataSetToCache(dataSet);
        }
        return wroteDataToCache;
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
                    Log.i(TAG, dp.getOriginalDataSource().getAppPackageName());
                    dp.getVersionCode();
                    long startTime = dp.getStartTime(TimeUnit.MILLISECONDS);
                    int activity = dp.getValue(field).asInt();
                    Workout workout;
                    SQLiteDatabase db = getDatabase();
                    if (db != null && db.isOpen()) {
                        workout = cupboard().withDatabase(db).get(Workout.class, startTime);
                    } else {
                        Log.w(TAG, "Warning: db is null");
                        return false;
                    }


                    // When the workout is null, we need to cache it. If the background task has completed,
                    // then we have at most 8 - 12 hours of data. Recent data is likely to change so over-
                    // write it.
                    Context context = getApplicationContext();
                    if (context != null) {
                        if (workout == null || UserPreferences.getBackgroundLoadComplete(context)) {
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
                            workout.packageName = dp.getOriginalDataSource().getAppPackageName();
                            //Log.v("MainActivity", "Put Cache: " + WorkoutTypes.getWorkOutTextById(workout.type) + " " + workout.duration);
                            if (db != null) {
                                cupboard().withDatabase(db).put(workout);
                            } else {
                                Log.w(TAG, "Warning: db is null");
                            }

                            wroteDataToCache = true;
                        } else {
                            // Do not overwrite data if the initial load is in progress. This would take too
                            // long and prevent us from accumulating a base set of data.
                        }
                    }

                }
            }
        }
        return wroteDataToCache;
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
            }
        }
        return dataSteps;
    }

    public interface IDataManager {
        void insertData(Workout workout);
        void removeData(Workout workout);
        void dataChanged();
    }
}
