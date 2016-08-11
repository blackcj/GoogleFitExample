package com.blackcj.fitdata.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;

import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.database.DataManager;
import com.blackcj.fitdata.model.UserPreferences;
import com.blackcj.fitdata.model.Workout;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import java.util.Calendar;
import java.util.Date;

import io.fabric.sdk.android.Fabric;


/**
 * Created by chris.black on 6/22/16.
 *
 * Service used to refresh data in the background to ensure execution completes. This method is
 * called both from the main view and from the MyStartServiceReciever
 */
public class BackgroundRefreshService extends Service implements DataManager.IDataManager {

    private IBinder mBinder = new LocalBinder();
    private DataManager mDataManager;
    private PowerManager.WakeLock mWakeLock;
    private boolean mInProgress = false;
    private boolean mFirstLoad = true;
    private long startTime;

    private class LocalBinder extends Binder {
        public BackgroundRefreshService getServerInstance() {
            return BackgroundRefreshService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public int onStartCommand (Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);

        PowerManager mgr = (PowerManager)getSystemService(Context.POWER_SERVICE);

        /*
        WakeLock is reference counted so we don't want to create multiple WakeLocks. So do a check before initializing and acquiring.

        This will fix the "java.lang.Exception: WakeLock finalized while still held: MyWakeLock" error that you may find.
        */
        if (this.mWakeLock == null) { //**Added this
            this.mWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FitDataWakeLock");
        }

        if (!this.mWakeLock.isHeld()) { //**Added this
            this.mWakeLock.acquire();
        }

        Fabric.with(this, new Crashlytics(), new Answers());

        if (mDataManager == null) {
            mDataManager = DataManager.getInstance(this);
        }
        mDataManager.addListener(this);

        if(mDataManager.isConnected() && !mInProgress) {
            onConnected();
            return START_STICKY;
        }

        if(!mDataManager.isConnected() && !mInProgress)
        {
            mInProgress = true;

            mDataManager.connect();
        }


        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Toast.makeText(BackgroundRefreshService.this, "Service Created", Toast.LENGTH_SHORT).show();
        mDataManager = DataManager.getInstance(this);
    }

    @Override
    public void onDestroy() {
        // Turn off the request flag
        this.mInProgress = false;

        if (this.mDataManager != null && this.mDataManager.isConnected()) {
            this.mDataManager.removeListener(this);
            this.mDataManager.disconnect();
            // Destroy the current location client
            this.mDataManager = null;
        }
        // Display the connection status
        //Toast.makeText(BackgroundRefreshService.this, "Service Destroyed", Toast.LENGTH_SHORT).show();

        if (this.mWakeLock != null) {
            this.mWakeLock.release();
            this.mWakeLock = null;
        }

        super.onDestroy();
    }

    @Override
    public void insertData(Workout workout) {
        // Not used
    }

    @Override
    public void removeData(Workout workout) {
        // Not used
    }

    @Override
    public void onConnected() {
        if(mDataManager != null && mDataManager.isConnected()) {
            //Toast.makeText(BackgroundRefreshService.this, "Starting new background refresh.", Toast.LENGTH_SHORT).show();
            mInProgress = false;
            Calendar cal = Calendar.getInstance();
            Date now = new Date();
            cal.setTime(now);
            startTime = cal.getTimeInMillis();
            long lastSync = UserPreferences.getLastSync(this.getApplicationContext());
            if (lastSync > 0) {
                mFirstLoad = false;
            }
            mDataManager.quickDataRead();
        } else {
            //Toast.makeText(BackgroundRefreshService.this, "Something went wrong.", Toast.LENGTH_SHORT).show();
            Calendar cal = Calendar.getInstance();
            Date now = new Date();
            cal.setTime(now);

            Answers.getInstance().logCustom(new CustomEvent("Background Refresh Failure")
                    .putCustomAttribute("Time (s)", Math.floor(cal.getTimeInMillis() - startTime) / 1000));
            stopSelf();
        }
    }

    @Override
    public void onDataChanged(Utilities.TimeFrame timeFrame) {}

    @Override
    public void onDataComplete() {
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        if (mFirstLoad) {
            Answers.getInstance().logCustom(new CustomEvent("Initial Load Success")
                    .putCustomAttribute("Time (s)", Math.floor(cal.getTimeInMillis() - startTime) / 1000));
        } else {
            Answers.getInstance().logCustom(new CustomEvent("Background Refresh Success")
                    .putCustomAttribute("Time (s)", Math.floor(cal.getTimeInMillis() - startTime) / 1000));
        }

        stopSelf();
    }
}