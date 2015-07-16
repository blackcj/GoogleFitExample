package com.blackcj.fitdata.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.blackcj.fitdata.R;
import com.blackcj.fitdata.database.CacheManager;
import com.blackcj.fitdata.database.CupboardSQLiteOpenHelper;
import com.blackcj.fitdata.database.DataManager;
import com.blackcj.fitdata.fragment.AddEntryFragment;
import com.blackcj.fitdata.model.Workout;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by chris.black on 6/26/15.
 */
public class AddEntryActivity extends BaseActivity implements DataManager.IDataManager {

    public static final String TAG = "AddEntryActivity";

    AddEntryFragment fragment;
    public static final String ARG_ACTIVITY_TYPE = "ARG_ACTIVITY_TYPE";
    private DataManager mDataManager;

    @Bind(R.id.container) View container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ButterKnife.bind(this);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Add Entry");
        }

        mDataManager = DataManager.getInstance(this);
        mDataManager.connect();
        int activityType = getIntent().getExtras().getInt(ARG_ACTIVITY_TYPE);
        fragment = AddEntryFragment.create(activityType);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.placeholder, fragment, AddEntryFragment.TAG);
        transaction.commit();
    }

    @Override
    public void onStart() {
        super.onStart();
        mDataManager.connect();
        mDataManager.addListener(this);
    }

    @Override
    protected void onStop() {
        mDataManager.removeListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mDataManager.disconnect();
        Log.w(TAG, "Closing db");
        super.onDestroy();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_add_entry;
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
                // Reverse the animation back to the previous view.
                //finish();
                //push from top to bottom
                //overridePendingTransition(R.anim.no_anim, R.anim.exit_anim);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void launch(BaseActivity activity, int activityType) {
        ActivityOptionsCompat options =
                ActivityOptionsCompat.makeCustomAnimation(activity, R.anim.enter_anim, R.anim.no_anim);
        Intent intent = new Intent(activity, AddEntryActivity.class);
        intent.putExtra(ARG_ACTIVITY_TYPE, activityType);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @OnClick(R.id.save_button) void onSave() {
        Workout workout = fragment.getWorkout();
        if (workout != null) {
            Log.d(TAG, "Added: " + workout.toString());
            // Validate workout doesn't overlap
            if (CacheManager.checkConflict(this, workout)) {
                Log.d(TAG, "Overlap detected!");
                Snackbar.make(container, "Overlap detected. Please change start time.", Snackbar.LENGTH_LONG).show();
            } else {
                mDataManager.insertData(workout);
                finishAfterTransition();
            }
        }
    }

    @OnClick(R.id.cancel_button) void onCancel() {
        finishAfterTransition();
    }

    private void hideKeyboard() {
        // Check if no view has focus:
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void insertData(Workout workout) {
        mDataManager.insertData(workout);
    }

    @Override
    public void removeData(Workout workout) {
        mDataManager.deleteWorkout(workout);
    }

    @Override
    public void dataChanged() {

    }
}
