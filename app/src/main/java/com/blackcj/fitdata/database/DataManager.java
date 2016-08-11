package com.blackcj.fitdata.database;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.model.UserPreferences;
import com.blackcj.fitdata.model.Workout;
import com.blackcj.fitdata.model.WorkoutTypes;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
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
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Device;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.result.ListSubscriptionsResult;
import com.google.android.gms.plus.Plus;

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
 *
 * https://github.com/googlesamples/android-fit/blob/master/BasicHistorySessions/app/src/main/java/com/google/android/gms/fit/samples/basichistorysessions/MainActivity.java
 * https://github.com/d4rxh4wx/MultiThreadSQLiteOpenHelper
 */
public class DataManager implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static DataManager _instance;

    public static final String TAG = "DataManager";
    public static final String DATE_FORMAT = "MM.dd h:mm a";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private WeakReference<SQLiteDatabase> mDb;
    private WeakReference<Context> mContext;
    private GoogleApiClient mClient;
    private final List<WeakReference<IDataManager>> mListeners;
    private boolean refreshInProgress = false;
    private SimpleDBHelper mHelper;
    private int retryCount = 0;

    private DataManager(Context context) {
        mContext = new WeakReference<>(context);
        this.mListeners = new ArrayList<>();
        buildFitnessClient(context);
    }

    public static synchronized DataManager getInstance(Context context)
    {
        if (_instance == null)
        {
            _instance = new DataManager(context);
        } else {
            _instance.setContext(context);
        }
        return _instance;
    }

    public boolean isRefreshInProgress() {
        return refreshInProgress;
    }

    public boolean hasContext() { return getContext() != null; }

    private void notifyListenersDataChanged(Utilities.TimeFrame timeFrame) {
        int i = 0;
        for(int z = this.mListeners.size(); i < z; ++i) {
            WeakReference<IDataManager> ref = this.mListeners.get(i);
            if (ref != null) {
                IDataManager dataManager = ref.get();
                if (dataManager != null) {
                    dataManager.onDataChanged(timeFrame);
                }
            }
        }
    }

    private void notifyListenersConnected() {
        Log.v(TAG, "Notifying " + this.mListeners.size() + " listeners that data manager connected.");
        int i = 0;
        for(int z = this.mListeners.size(); i < z; ++i) {
            WeakReference<IDataManager> ref = this.mListeners.get(i);
            if (ref != null) {
                IDataManager dataManager = ref.get();
                if (dataManager != null) {
                    dataManager.onConnected();
                }
            }
        }
    }

    private void notifyListenersLoadComplete() {
        Log.v(TAG, "Notifying " + this.mListeners.size() + " listeners of data load complete.");
        int i = 0;
        for(int z = this.mListeners.size(); i < z; ++i) {
            WeakReference<IDataManager> ref = this.mListeners.get(i);
            if (ref != null) {
                IDataManager dataManager = ref.get();
                if (dataManager != null) {
                    dataManager.onDataComplete();
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
        Log.v(TAG, "Added " + listener.getClass().getName() + " to listeners.");
        this.mListeners.add(new WeakReference<>(listener));
    }

    public void removeListener(IDataManager listener) {
        Iterator i = this.mListeners.iterator();
        Log.v(TAG, "Removing " + listener.getClass().getName() + " from listeners.");
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

    public boolean isConnected() {
        boolean result = false;
        if (mClient != null) {
            if (connected) {
                result = mClient.isConnected();
            } else if(!mClient.isConnecting()) {
               connect();
            }
        }
        if (mClient == null) {
            buildFitnessClient(getContext());
        }
        return result;
    }

    public void connect() {
        if (checkPlayServices()) {
            if (mClient == null || !connected) {
                connected = false;
                buildFitnessClient(getContext());
                mClient.connect();
            } else if (!mClient.isConnecting() && !mClient.isConnected()) {
                Log.v(TAG, "Connecting client.");
                mClient.reconnect();
            }
            getDatabase();
        } else {
            Log.w(TAG, "WARNING: No play services.");
        }
    }

    public void disconnect() {
        if (mListeners.size() == 0) {
            if (mClient != null && (mClient.isConnected() || mClient.isConnecting())) {
                Log.v(TAG, "Disconnecting client.");
                mClient.disconnect();
            }
            closeDatabase();
            mClient = null;
            connected = false;
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Context context = getContext();
        if (context != null) {
            if (context instanceof Activity) {
                int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
                if (resultCode != ConnectionResult.SUCCESS) {
                    if (apiAvailability.isUserResolvableError(resultCode)) {
                        apiAvailability.getErrorDialog((Activity) context, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                                .show();
                    } else {
                        Log.w(TAG, "This device is not supported.");
                    }
                    return false;
                }
                return true;
            } else {
                // We must have been started in the background, ignore since we can't resolve from here.
                int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
                if (resultCode != ConnectionResult.SUCCESS) {
                    Log.w(TAG, "WARNING: Can't resolve without an Activity context.");
                    return false;
                }
                return true;
            }

        }
        return false;

    }

    public void closeDatabase() {
        SimpleDBHelper.INSTANCE.close();
    }

    private SQLiteDatabase getDatabase() {
        Context context = getApplicationContext();
        if (context != null) {
            return SimpleDBHelper.INSTANCE.open(context);
        }

        return null;
    }

    private Context getApplicationContext() {
        if (mContext != null) {
            Context context = mContext.get();
            if (context != null) {
                return context.getApplicationContext();
            }
        } else {
            Log.w(TAG, "Warning: Application context is null.");
        }
        return null;
    }

    private Context getContext() {
        if (mContext != null) {
            Context context = mContext.get();
            if (context != null) {
                return context;
            }
        } else {
            Log.w(TAG, "Warning: Context is null.");
        }
        return null;
    }

    public void quickDataRead() {
        Context context = getApplicationContext();
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        final long currentTime = cal.getTimeInMillis();
        if (context != null && refreshInProgress) {

            if (currentTime - UserPreferences.getLastSyncStart(context) > 1000 * 60 * 10) {
                // TODO: Add start sync time
                // More than 10 minutes have passed. Let the user execute another refresh.
                refreshInProgress = false;
                retryCount = 0;
                Log.w(TAG, "Warning: Refresh timed out.");
            }
        }
        if (isConnected()) {
            if (!refreshInProgress) {
                dumpSubscriptionsList();
                refreshInProgress = true;
                UserPreferences.setLastSyncStart(context, currentTime);
                long syncStart = UserPreferences.getLastSync(context);// - (1000 * 60 * 60 * 8);
                //long syncStart = Utilities.getTimeFrameStart(Utilities.TimeFrame.BEGINNING_OF_DAY);
                if (context != null) {
                    SQLiteDatabase db = getDatabase();
                    if (db != null && db.isOpen()) {
                        //db.execSQL("DELETE FROM " + Workout.class.getSimpleName()); // This deletes all data
                        cupboard().withDatabase(db).delete(Workout.class, "start >= ?", "" + syncStart);
                    }

                    //UserPreferences.setBackgroundLoadComplete(context, false);
                    //UserPreferences.setLastSync(context, syncStart);
                    populateHistoricalData();
                }
            }
        } else {
            refreshInProgress = false;
        }
    }

    public void dataRead(long syncStart) {
        if (isConnected()) {
            if (!refreshInProgress) {
                // TODO: Add timeout limit to refreshInProgress
                refreshInProgress = true;
                Context context = getApplicationContext();
                if (context != null) {
                    SQLiteDatabase db = getDatabase();
                    if (db != null && db.isOpen()) {
                        //db.execSQL("DELETE FROM " + Workout.class.getSimpleName()); // This deletes all data
                        cupboard().withDatabase(db).delete(Workout.class, "start >= ?", "" + syncStart);
                    }
                    //closeDatabase();
                    //UserPreferences.setBackgroundLoadComplete(context, false);
                    UserPreferences.setLastSync(context, syncStart);
                    populateHistoricalData();
                }
            }
        } else {
            refreshInProgress = false;
        }
    }

    public void refreshData() {
        if (mClient.isConnected() && !refreshInProgress) {
            refreshInProgress = true;
            Context context = getApplicationContext();
            long syncStart = Utilities.getTimeFrameStart(Utilities.TimeFrame.THIRTY_DAYS);
            if (context != null) {
                SQLiteDatabase db = getDatabase();
                if (db != null && db.isOpen()) {
                    //db.execSQL("DELETE FROM " + Workout.class.getSimpleName()); // This deletes all data
                    cupboard().withDatabase(db).delete(Workout.class, "start >= ?", "" + syncStart);
                }
                //closeDatabase();
                //UserPreferences.setBackgroundLoadComplete(context, false);
                UserPreferences.setLastSync(context, syncStart);
                populateHistoricalData();
            }
        } else {
            Log.w(TAG, "Warning: Already running.");
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
        if (db != null && db.isOpen()) {
            cupboard().withDatabase(db).delete(Workout.class, "start >= ?", "" + syncStart);
        } else {
            Log.w(TAG, "Warning: db is null");
        }
        //closeDatabase();
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
        if (isConnected()) {
            // Set a start and end time for our data, using a start time of 1 day before this moment.
            long endTime = workout.start + workout.duration;
            long startTime = workout.start;
            //long syncStart = workout.start - (1000 * 60 * 60 * 8);
            SQLiteDatabase db = getDatabase();
            if (db != null && db.isOpen()) {
                //cupboard().withDatabase(db).delete(Workout.class, "start >= ?", "" + syncStart);
                cupboard().withDatabase(db).delete(Workout.class, "start = ? AND type = ?", "" + startTime, "" + workout.type);
            } else {
                Log.w(TAG, "Warning: db is null");
            }
            //closeDatabase();
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
            notifyListenersDataChanged(Utilities.TimeFrame.ALL_TIME);

        }
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

            Answers.getInstance().logCustom(new CustomEvent("Insert Session")
                    .putCustomAttribute("Type Id", workout.type)
                    .putCustomAttribute("Details", workout.shortText())
                    .putCustomAttribute("Step Count", workout.stepCount));

            String activityName = WorkoutTypes.getActivityTextById(workout.type);

            // Then, invoke the History API to insert the data and await the result, which is
            // possible here because of the {@link AsyncTask}. Always include a timeout when calling
            // await() to prevent hanging that can occur from the service being shutdown because
            // of low memory or other conditions.
            Log.i(TAG, "Inserting the session in the History API");
            Context context = getApplicationContext();
            if(context != null && isConnected()) {
                workout.packageName = context.getPackageName();
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

                SQLiteDatabase db = getDatabase();
                if (db != null && db.isOpen()) {
                    if (workout.duration > 0) {
                        long start = workout.start - (1000 * 60 * 60 * 2);
                        cupboard().withDatabase(db).delete(Workout.class, "start >= ? AND start < ?", "" + start, "" + (workout.start + workout.duration));
                        cupboard().withDatabase(db).put(workout);
                        notifyListenersDataChanged(Utilities.TimeFrame.ALL_TIME);
                    } else {
                        Log.w(TAG, "Warning: workout duration is 0.");
                    }
                } else {
                    Log.w(TAG, "Warning: db is null");
                }

                if(!refreshInProgress) {
                    //UserPreferences.setBackgroundLoadComplete(context, false);
                    UserPreferences.setLastSync(context, workout.start);

                    // TODO: This could be optimized to select an end time.
                    populateHistoricalData();
                }
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
                                .await(10, TimeUnit.MINUTES);

                // Before querying the session, check to see if the insertion succeeded.
                if (!insertStatus.isSuccess()) {
                    Log.i(TAG, "There was a problem inserting the dataset: " +
                            insertStatus.getStatusMessage());
                    return null;
                }

                // At this point, the session has been inserted and can be read.
                Log.i(TAG, "Data insert was successful!");

                //UserPreferences.setBackgroundLoadComplete(context, false);
                UserPreferences.setLastSync(context, workout.start - (1000 * 60 * 60 * 8));

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
    private void buildFitnessClient(final Context context) {
        if (context != null) {
            Log.i(TAG, "Creating the Google API Client with context: " + context.getClass().getName());
            // Create the Google API Client
            mClient = new GoogleApiClient.Builder(context)
                    .addApiIfAvailable(Plus.API)
                    .addApiIfAvailable(Fitness.SENSORS_API)
                    .addApi(Fitness.SESSIONS_API)
                    .addApi(Fitness.HISTORY_API)
                    .addApi(Fitness.RECORDING_API)
                    //.addApi(LocationServices.API)
                    .addScope(new Scope(Scopes.PROFILE))
                    //.addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                    .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }

    }


    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed. Cause: " + result.toString());
        Context context = getContext();
        if (!result.hasResolution()) {
            // Show the localized error dialog
            if (context != null) {
                if (context instanceof Activity) {
                    GoogleApiAvailability gApi = GoogleApiAvailability.getInstance();
                    gApi.getErrorDialog((Activity) context, result.getErrorCode(), 0).show();
                }

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
                    if (context instanceof Activity) {
                        authInProgress = true;
                        result.startResolutionForResult((Activity) context,
                                REQUEST_OAUTH);
                    }
                }
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG,
                        "Exception while starting resolution activity", e);
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "onConnected called");
        // Now you can make calls to the Fitness APIs.  What to do?
        // Look at some data!!
        if (mClient.isConnected()) {
            connected = true;


        /*
        Plus.PeopleApi.loadVisible(mClient, null);
        if (Plus.PeopleApi.getCurrentPerson(mClient) != null) {
            Person currentPerson = Plus.PeopleApi
                    .getCurrentPerson(mClient);
            String email = Plus.AccountApi.getAccountName(mClient);
            Log.v(TAG, "Connected with: " + currentPerson.getName() + " - " +email);
        }*/
            notifyListenersConnected();
            //connect();
        } else {
            Log.w(TAG, "WARNING: NOT CONNECTED");
        }
    }

    /**
     * Fetch a list of all active subscriptions and log it. Since the logger for this sample
     * also prints to the screen, we can see what is happening in this way.
     */
    private void dumpSubscriptionsList() {
        Log.i(TAG, "Dumping subscriptions.");
        // [START list_current_subscriptions]
        Fitness.RecordingApi.listSubscriptions(mClient)
                // Create the callback to retrieve the list of subscriptions asynchronously.
                .setResultCallback(new ResultCallback<ListSubscriptionsResult>() {
                    @Override
                    public void onResult(ListSubscriptionsResult listSubscriptionsResult) {
                        for (Subscription sc : listSubscriptionsResult.getSubscriptions()) {
                            DataType dt = sc.getDataType();
                            Log.i(TAG, "Active subscription for data type: " + dt.getName());
                        }
                    }
                });
        // [END list_current_subscriptions]
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

    public void setStepCounting(boolean active) {
        if (UserPreferences.getCountSteps(getContext()) == active) {
            return;
        }
        if (active) {
            subscribeSteps();
        }else {
            unsubscribeSteps();
        }
    }

    public void setActivityTracking(boolean active) {
        if (UserPreferences.getActivityTracking(getContext()) == active) {
            return;
        }
        if (active) {
            subscribeActivity();
        }else {
            unsubscribeActivity();
        }
    }

    public void setContext(Context activityContext) {
        if (!_instance.hasContext()) {
            mContext = new WeakReference<>(activityContext);
        }
        if (activityContext instanceof Activity) {
            // Always give priority to Acitivty context
            mContext = new WeakReference<>(activityContext);
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
        if (isConnected()) {
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
        } else {
            Context context = getApplicationContext();
            if (context != null) {
                Toast.makeText(context, "Error: mClient not connected.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void unsubscribeSteps() {

        if (isConnected()) {
            Fitness.RecordingApi.unsubscribe(mClient, DataType.TYPE_STEP_COUNT_DELTA)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            Context context = getApplicationContext();
                            if (context != null) {
                                if (status.isSuccess()) {
                                    UserPreferences.setCountSteps(context, false);
                                    Toast.makeText(context, "Successfully unsubscribed type: steps", Toast.LENGTH_LONG).show();
                                    Log.i(TAG, "Successfully unsubscribed for data type: step count delta");
                                } else {
                                    // Subscription not removed
                                    Toast.makeText(context, "Error:" + status.getStatusMessage(), Toast.LENGTH_LONG).show();
                                    Log.i(TAG, "Failed to unsubscribe for data type: step count delta");
                                }
                            }
                        }
                    });
        } else {
            Context context = getApplicationContext();
            if (context != null) {
                Toast.makeText(context, "Error: mClient not connected.", Toast.LENGTH_LONG).show();
            }
            connect();
        }
    }

    private void unsubscribeActivity() {
        if (isConnected()) {
            Fitness.RecordingApi.unsubscribe(mClient, DataType.TYPE_ACTIVITY_SEGMENT)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            Context context = getApplicationContext();
                            if (context != null) {
                                if (status.isSuccess()) {
                                    Log.i(TAG, "Successfully unsubscribed for data type: activity");
                                    Toast.makeText(context, "Successfully unsubscribed type: activity", Toast.LENGTH_LONG).show();
                                    UserPreferences.setActivityTracking(context, false);
                                } else {
                                    // Subscription not removed
                                    Toast.makeText(context, "Error:" + status.getStatusMessage(), Toast.LENGTH_LONG).show();
                                    Log.i(TAG, "Failed to unsubscribe for data type: activity");
                                }
                            }
                        }
                    });
        } else {
            Context context = getApplicationContext();
            if (context != null) {
                Toast.makeText(context, "Error: mClient not connected.", Toast.LENGTH_LONG).show();
            }
            connect();
        }
    }

    private void subscribeSteps() {
        if (isConnected()) {
            Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_STEP_COUNT_DELTA)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                if (status.getStatusCode()
                                        == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                    Log.i(TAG, "Existing subscription for steps detected.");

                                } else {
                                    Log.i(TAG, "Successfully subscribed!");
                                }
                                Context context = getApplicationContext();
                                if (context != null) {
                                    Toast.makeText(context, "Successfully subscribed!", Toast.LENGTH_LONG).show();
                                    UserPreferences.setCountSteps(context, true);
                                }
                            } else {
                                Log.i(TAG, "There was a problem subscribing.");
                                Context context = getApplicationContext();
                                if (context != null) {
                                    Toast.makeText(context, "Error:" + status.getStatusMessage(), Toast.LENGTH_LONG).show();
                                    UserPreferences.setCountSteps(context, false);
                                }
                            }
                        }
                    });
        } else {
            Context context = getApplicationContext();
            if (context != null) {
                Toast.makeText(context, "Error: mClient not connected.", Toast.LENGTH_LONG).show();
            }
            connect();
        }
    }

    private void subscribeActivity() {
        if (isConnected()) {
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
                                Context context = getApplicationContext();
                                if (context != null) {
                                    Toast.makeText(context, "Successfully subscribed!", Toast.LENGTH_LONG).show();
                                    UserPreferences.setActivityTracking(context, true);
                                }
                            } else {
                                Log.i(TAG, "There was a problem subscribing.");
                                Context context = getApplicationContext();
                                if (context != null) {
                                    Toast.makeText(context, "Error:" + status.getStatusMessage(), Toast.LENGTH_LONG).show();
                                    UserPreferences.setActivityTracking(context, false);
                                    buildFitnessClient(getContext());
                                }
                            }
                        }
                    });
        } else {
            Context context = getApplicationContext();
            if (context != null) {
                Toast.makeText(context, "Error: mClient not connected.", Toast.LENGTH_LONG).show();
            }
            connect();
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
            long endTime;
            long startTime;
            Context context = getApplicationContext();
            if (context != null) {
                refreshInProgress = true;
                long lastSync = UserPreferences.getLastSync(context);

                // Update step count
                cal.setTime(now);
                endTime = cal.getTimeInMillis();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 1);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                startTime = cal.getTimeInMillis();

                int numberOfDays = 30;
                if (lastSync > 0) {
                    lastSync -=  (1000 * 60 * 60 * 2);
                    if (lastSync < startTime) {
                        double diff =  startTime - lastSync;
                        if (diff / (1000 * 60 * 60 * 24) < 30) {
                            numberOfDays = (int)Math.floor(diff / (1000 * 60 * 60 * 24));
                            numberOfDays += 1;
                        }
                    } else {
                        numberOfDays = 1;
                    }
                }
                int totalSteps = 0;

                SQLiteDatabase db = getDatabase();
                if (db == null || !db.isOpen()) {
                    notifyListenersDataChanged(Utilities.TimeFrame.ALL_TIME);
                    notifyListenersLoadComplete();
                    return null;
                }
                Log.i(TAG, "Loading " + numberOfDays + " days step count");
                for (int i = 0; i < numberOfDays; i++) {
                    DataReadRequest stepCountRequest = DataQueries.queryStepEstimate(startTime, endTime);
                    final DataReadResult stepCountReadResult = Fitness.HistoryApi.readData(mClient, stepCountRequest).await(5, TimeUnit.MINUTES);

                    if (stepCountReadResult.getStatus().isSuccess()) {
                        retryCount = 0;
                        int stepCount = countStepData(stepCountReadResult);
                        Workout workout = new Workout();
                        workout.start = startTime;
                        workout._id = startTime;
                        workout.type = WorkoutTypes.STEP_COUNT.getValue();
                        workout.stepCount = stepCount;
                        totalSteps += stepCount;
                        //workout.duration = 1000*60*10;
                        if (numberOfDays < 10) {
                            Log.i(TAG, "Step count: " + workout.toString());
                        }

                        if (db != null && db.isOpen()) {
                            if (stepCount > 0) {
                                //cupboard().withDatabase(db).delete(Workout.class, "id = ? AND type = ?", "" + startTime, "" + workout.type);
                                cupboard().withDatabase(db).put(workout);
                            } else {
                                Log.w(TAG, "Warning: step count is 0");
                            }
                        } else {
                            Log.w(TAG, "Warning: db is null");
                            return null;
                        }

                        endTime = startTime;
                        cal.add(Calendar.DAY_OF_YEAR, -1);
                        startTime = cal.getTimeInMillis();
                    } else {


                        refreshInProgress = false;
                        //closeDatabase();
                        if (retryCount < 3) {
                            Log.i(TAG, "Attempting to reconnect client.");
                            retryCount++;
                            mClient.reconnect();
                        } else {
                            final Context activityContext = getContext();
                            if (activityContext instanceof Activity) {
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        GoogleApiAvailability gApi = GoogleApiAvailability.getInstance();
                                        gApi.getErrorDialog((Activity) activityContext, stepCountReadResult.getStatus().getStatusCode(), 0).show();
                                        Log.w(TAG, "FAILURE: Unable to read data.");

                                    }
                                });

                            }
                            // TODO: Notify listeners of failure

                            notifyListenersDataChanged(Utilities.TimeFrame.ALL_TIME);
                            notifyListenersLoadComplete();
                        }

                        return null;
                    }
                }

                Log.i(TAG, "Loaded " + numberOfDays + " days. Step count: " + totalSteps);

                // Update activities
                cal.setTime(now);
                endTime = cal.getTimeInMillis();
                if (lastSync > 0) {
                    Log.i(TAG, "Fast data read starting: " + Utilities.getTimeDateString(lastSync));
                    startTime = lastSync;
                } else {
                    Log.i(TAG, "Slow data read");
                    cal.setTime(now);
                    cal.add(Calendar.DAY_OF_YEAR, -90);
                    startTime = cal.getTimeInMillis();
                }

                SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

                Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
                Log.i(TAG, "Range End: " + dateFormat.format(endTime));

                boolean wroteDataToCache = false;

                // Load today
                long dayStart = Utilities.getTimeFrameStart(Utilities.TimeFrame.BEGINNING_OF_DAY);
                if(startTime <= dayStart && dayStart < endTime) {
                    Log.i(TAG, "Loading today");
                    // Estimated steps and duration by Activity
                    DataReadRequest activitySegmentRequest = DataQueries.queryActivitySegment(dayStart, endTime);
                    DataReadResult dataReadResult = Fitness.HistoryApi.readData(mClient, activitySegmentRequest).await(10, TimeUnit.MINUTES);
                    wroteDataToCache = writeActivityDataToCache(dataReadResult);
                    endTime = dayStart;
                    notifyListenersDataChanged(Utilities.TimeFrame.BEGINNING_OF_DAY);
                }

                // Load week
                long weekStart = Utilities.getTimeFrameStart(Utilities.TimeFrame.BEGINNING_OF_WEEK);

                //Log.i(TAG, "Range Start: " + dateFormat.format(weekStart));
                //Log.i(TAG, "Range End: " + dateFormat.format(endTime));
                if(startTime <= weekStart && weekStart < endTime) {
                    Log.i(TAG, "Loading week");
                    // Estimated steps and duration by Activity
                    DataReadRequest activitySegmentRequest = DataQueries.queryActivitySegment(weekStart, endTime);
                    DataReadResult dataReadResult = Fitness.HistoryApi.readData(mClient, activitySegmentRequest).await(10, TimeUnit.MINUTES);
                    wroteDataToCache = writeActivityDataToCache(dataReadResult);
                    endTime = weekStart;
                    notifyListenersDataChanged(Utilities.TimeFrame.BEGINNING_OF_WEEK);
                }
                boolean hasMoreThanOneWeek = false;
                // Load rest
                if (startTime < endTime) {

                    hasMoreThanOneWeek = true;
                    Log.i(TAG, "Range Start: " + startTime);
                    Log.i(TAG, "Range End: " + endTime);
                    Log.i(TAG, "Loading rest");
                    DataReadRequest activitySegmentRequest = DataQueries.queryActivitySegment(startTime, endTime);
                    DataReadResult dataReadResult = Fitness.HistoryApi.readData(mClient, activitySegmentRequest).await(15, TimeUnit.MINUTES);
                    wroteDataToCache = writeActivityDataToCache(dataReadResult);
                }

                cal.setTime(now);
                Log.i(TAG, "Background load complete");
                if (wroteDataToCache) {
                    UserPreferences.setLastSync(context, cal.getTimeInMillis());
                    UserPreferences.setBackgroundLoadComplete(context, true);
                }
                refreshInProgress = false;
                notifyListenersDataChanged(Utilities.TimeFrame.ALL_TIME);
                notifyListenersLoadComplete();
                //closeDatabase();
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
     * @param dataSet set of data from the Google Fit API
     */
    private boolean writeDataSetToCache(DataSet dataSet) {
        boolean wroteDataToCache = true;
        SQLiteDatabase db = getDatabase();
        for (DataPoint dp : dataSet.getDataPoints()) {
            // Populate db cache with data
            for(Field field : dp.getDataType().getFields()) {
                if(field.getName().equals("activity") && dp.getDataType().getName().equals("com.google.activity.segment")) {
                    //Log.i(TAG, dp.getOriginalDataSource().getAppPackageName());
                    dp.getVersionCode();
                    long startTime = dp.getStartTime(TimeUnit.MILLISECONDS);
                    int activity = dp.getValue(field).asInt();
                    Log.i(TAG, "Found: " + WorkoutTypes.getActivityTextById(activity) + " entered by: " + dp.getOriginalDataSource().getAppPackageName());
                    Workout workout;

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
                    if (context != null && isConnected()) {
                        //if (workout == null || UserPreferences.getBackgroundLoadComplete(context)) {
                        long endTime = dp.getEndTime(TimeUnit.MILLISECONDS);
                        DataReadRequest readRequest = DataQueries.queryStepCount(startTime, endTime);
                        DataReadResult dataReadResult = Fitness.HistoryApi.readData(mClient, readRequest).await(2, TimeUnit.MINUTES);
                        int stepCount = countStepData(dataReadResult);
                        workout = new Workout();
                        workout._id = startTime;
                        workout.start = startTime;
                        workout.duration = endTime - startTime;
                        workout.stepCount = stepCount;
                        workout.type = activity;
                        workout.packageName = dp.getOriginalDataSource().getAppPackageName();
                        //Log.v("MainActivity", "Put Cache: " + WorkoutTypes.getWorkOutTextById(workout.type) + " " + workout.duration);
                        if (db != null && db.isOpen()) {
                            if (workout.duration > 0) {
                                Log.i(TAG, "Wrote to DB: " + workout.toString());
                                cupboard().withDatabase(db).put(workout);
                            } else {
                                Log.w(TAG, "Warning: duration is 0");
                            }

                            //wroteDataToCache = true;
                        } else {
                            wroteDataToCache = false;
                        }
                        //} else {
                            // Do not overwrite data if the initial load is in progress. This would take too
                            // long and prevent us from accumulating a base set of data.
                        //}
                    } else {
                        wroteDataToCache = false;
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
        if (dataReadResult.getBuckets().size() > 0) {
            //Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    stepCount += parseDataSet(dataSet);
                }

            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            //Log.i(TAG, "Number of returned DataSets is: " + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                stepCount += parseDataSet(dataSet);
            }
        }

        return stepCount;
    }

    /**
     * Walk through all fields in a step_count dataset and return the sum of steps. Used to
     * calculate step counts.
     *
     * @param dataSet set of data from the Google Fit API
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
        void onConnected();
        void onDataChanged(Utilities.TimeFrame timeFrame);
        //void onDataFailure();
        void onDataComplete();
    }
}
