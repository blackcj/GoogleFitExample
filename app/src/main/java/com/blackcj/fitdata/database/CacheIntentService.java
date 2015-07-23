package com.blackcj.fitdata.database;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Created by Chris Black
 */
public class CacheIntentService extends IntentService {

    private WeakReference<ResultReceiver> mReceiver;
    public final static String TAG = "SimpleIntentService";

    public CacheIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        ResultReceiver resultReceiver = workIntent.getParcelableExtra("receiverTag");
        int workoutType = workIntent.getIntExtra("receiverTag", 0);
        mReceiver = new WeakReference<>(resultReceiver);
        Log.d(TAG, "IntentService started.");
        //Your other code here for processing request

        ResultReceiver receiver = mReceiver.get();
        if(receiver != null) {
            Bundle b= new Bundle();
            b.putString("ResultTag", "IntentService Result");
            receiver.send(200, b);
        }else {
            Log.d(TAG, "Weak listener is NULL.");
        }
    }

}
