package com.blackcj.fitdata.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.blackcj.fitdata.R;
import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.database.CacheManager;
import com.blackcj.fitdata.database.DataManager;
import com.blackcj.fitdata.fragment.AddEntryFragment;
import com.blackcj.fitdata.model.Workout;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Chris Black
 *
 * The AddEntryActivity displays a form field allowing the user to manually add an
 * entry into the Fit API.
 */
@SuppressWarnings({"WeakerAccess", "unused"}) // Butterknife requires public reference of injected views
public class AddEntryActivity extends BaseActivity implements DataManager.IDataManager {

    private static final String TAG = "AddEntryActivity";

    private AddEntryFragment fragment;
    public static final String ARG_ACTIVITY_TYPE = "ARG_ACTIVITY_TYPE";
    private DataManager mDataManager;

    @Bind(R.id.container) View container;

    /**
     * Used to start the activity using a custom animation.
     *
     * @param activity Reference to the Android Activity we are animating from
     * @param activityType Fitness activity type used to pre-populate the form field
     */
    public static void launch(BaseActivity activity, int activityType) {
        ActivityOptionsCompat options =
                ActivityOptionsCompat.makeCustomAnimation(activity, R.anim.enter_anim, R.anim.no_anim);
        Intent intent = new Intent(activity, AddEntryActivity.class);
        intent.putExtra(ARG_ACTIVITY_TYPE, activityType);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    ///////////////////////////////////////
    // LIFE CYCLE
    ///////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ButterKnife.bind(this);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Add Entry");
        }
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_close_white, null));
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
        super.onDestroy();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_add_entry;
    }

    ///////////////////////////////////////
    // OPTIONS MENU
    ///////////////////////////////////////

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                // Reverse animation back to previous activity
                ActivityCompat.finishAfterTransition(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    ///////////////////////////////////////
    // EVENT HANDLERS
    ///////////////////////////////////////
    @OnClick(R.id.save_button) void onSave() {
        final Workout workout = fragment.getWorkout();
        if (workout != null) {
            Log.d(TAG, "Added: " + workout.toString());
            // Validate workout doesn't overlap
            if (CacheManager.checkConflict(this, workout)) {
                Log.d(TAG, "Overlap detected.");
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Overlap detected.")
                        .setPositiveButton("CONTINUE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mDataManager.insertData(workout);
                                finishAfterTransition();
                            }
                        })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
                /*
                Snackbar.make(container, "Overlap detected.", Snackbar.LENGTH_INDEFINITE).setAction("OVERLAP", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mDataManager.insertData(workout);
                        finishAfterTransition();
                    }
                }).show();*/
            } else {
                mDataManager.insertData(workout);
                finishAfterTransition();
            }
        }
    }

    @OnClick(R.id.cancel_button) void onCancel() {
        finishAfterTransition();
    }

    ///////////////////////////////////////
    // CALLBACKS
    ///////////////////////////////////////

    @Override
    public void insertData(Workout workout) {
        mDataManager.insertData(workout);
    }

    @Override
    public void removeData(Workout workout) {
        mDataManager.deleteWorkout(workout);
    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onDataChanged(Utilities.TimeFrame timeFrame) {

    }

    @Override
    public void onDataComplete() {

    }
}
