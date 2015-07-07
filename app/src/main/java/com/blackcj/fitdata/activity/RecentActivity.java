package com.blackcj.fitdata.activity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.blackcj.fitdata.R;
import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.database.CacheManager;
import com.blackcj.fitdata.database.CupboardSQLiteOpenHelper;
import com.blackcj.fitdata.database.DataManager;
import com.blackcj.fitdata.fragment.AddEntryFragment;
import com.blackcj.fitdata.fragment.RecentFragment;
import com.blackcj.fitdata.model.Workout;
import com.blackcj.fitdata.service.CacheResultReceiver;

import butterknife.Bind;
import butterknife.ButterKnife;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by chris.black on 7/2/15.
 */
public class RecentActivity extends BaseActivity implements DataManager.IDataManager, IMainActivityCallback {
    public static final String TAG = "RecentActivity";
    private static SQLiteDatabase mDb;

    RecentFragment fragment;
    public static final String ARG_ACTIVITY_TYPE = "ARG_ACTIVITY_TYPE";
    private CupboardSQLiteOpenHelper mHelper;
    private DataManager mDataManager;
    private Cursor mCursor;
    private Workout lastWorkout;

    @Bind(R.id.container) View container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ButterKnife.bind(this);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Recent History");
        }

        mHelper = new CupboardSQLiteOpenHelper(this);
        mDb = mHelper.getWritableDatabase();
        mDataManager = DataManager.getInstance(this);

        int activityType = getIntent().getExtras().getInt(ARG_ACTIVITY_TYPE);

        fragment = RecentFragment.create();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.placeholder, fragment, RecentFragment.TAG);
        transaction.commit();

        mCursor = cupboard().withDatabase(mDb).query(Workout.class).withSelection("type != ? AND type != ?", "3", "-2").orderBy("start DESC").limit(200).query().getCursor();

    }

    @Override
    public void onStart() {
        super.onStart();
        mDataManager.addListener(this);
        mDataManager.connect();
    }

    @Override
    protected void onStop() {
        mDataManager.removeListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mCursor.close();
        mHelper.close();
        mDataManager.disconnect();
        super.onDestroy();
    }

    @Override
    public void launch(View transitionView, Workout workout) {

    }

    @Override
    public Cursor getCursor() {
        return mCursor;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_recent;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finishAfterTransition();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void launch(BaseActivity activity, int activityType) {
        ActivityOptionsCompat options =
                ActivityOptionsCompat.makeCustomAnimation(activity, R.anim.enter_anim, R.anim.no_anim);
        Intent intent = new Intent(activity, RecentActivity.class);
        intent.putExtra(ARG_ACTIVITY_TYPE, activityType);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @Override
    public void insertData(Workout workout) {

    }

    @Override
    public void removeData(Workout workout) {
        lastWorkout = workout;
        Snackbar.make(container, "Removed entry", Snackbar.LENGTH_LONG).setAction("UNDO", clickListener).show();
        Log.d(TAG, "Removed: " + workout.toString());
        mDataManager.deleteWorkout(workout);
        dataChanged();
    }

    final View.OnClickListener clickListener = new View.OnClickListener() {
        public void onClick(View v) {
            mDataManager.insertData(lastWorkout);
        }
    };

    @Override
    public void dataChanged() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCursor = cupboard().withDatabase(mDb).query(Workout.class).withSelection("type != ? AND type != ?", "3", "-2").orderBy("start DESC").limit(200).query().getCursor();
                fragment.swapCursor(mCursor);
                Log.d(TAG, "Refresh cursor");
            }
        });

    }
}
