package com.blackcj.fitdata.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.widget.Toast;

import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.database.DataManager;
import com.blackcj.fitdata.model.Workout;


/**
 * Created by chris.black on 6/22/16.
 */
public class BackgroundRefreshService extends Service implements DataManager.IDataManager {

    IBinder mBinder = new LocalBinder();
    protected DataManager mDataManager;
    private PowerManager.WakeLock mWakeLock;
    private boolean mInProgress = false;

    public class LocalBinder extends Binder {
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
        //Toast.makeText(BackgroundRefreshService.this, "Service Started", Toast.LENGTH_SHORT).show();
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

        if (mDataManager == null) {
            mDataManager = DataManager.getInstance(this);
        }

        if(mDataManager.isConnected() && !mInProgress) {
            onConnected();
            return START_STICKY;
        }

        if(!mDataManager.isConnected() && !mInProgress)
        {
            mInProgress = true;
            mDataManager.addListener(this);
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
        Toast.makeText(BackgroundRefreshService.this, "Service Destroyed", Toast.LENGTH_SHORT).show();

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
            if(mDataManager.isRefreshInProgress()) {
                //stopSelf();
                Toast.makeText(BackgroundRefreshService.this, "Refresh already in progress.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(BackgroundRefreshService.this, "Starting new background refresh.", Toast.LENGTH_SHORT).show();
                mInProgress = false;
                mDataManager.quickDataRead();
            }
        } else {
            Toast.makeText(BackgroundRefreshService.this, "Something went wrong.", Toast.LENGTH_SHORT).show();
            stopSelf();
        }
    }

    @Override
    public void onDataChanged(Utilities.TimeFrame timeFrame) {};
    @Override
    public void onDataComplete() {
        stopSelf();
    };
}