package com.blackcj.fitdata.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.adapter.TabPagerAdapter;
import com.blackcj.fitdata.R;
import com.blackcj.fitdata.database.DataManager;
import com.blackcj.fitdata.fragment.PageFragment;
import com.blackcj.fitdata.fragment.SettingsFragment;
import com.blackcj.fitdata.model.UserPreferences;
import com.blackcj.fitdata.model.Workout;
import com.blackcj.fitdata.service.BackgroundRefreshService;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.fitness.data.DataSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * This sample demonstrates how to use the History API of the Google Fit platform to insert data,
 * query against existing data, and remove data. It also demonstrates how to authenticate
 * a user with Google Play Services and how to properly represent data in a {@link DataSet}.
 *
 * https://developers.google.com/fit/android/get-api-key
 */
@SuppressWarnings("WeakerAccess") // Butterknife requires public reference of injected views
public class MainActivity extends BaseActivity implements SearchView.OnQueryTextListener,
        FragmentManager.OnBackStackChangedListener, AppBarLayout.OnOffsetChangedListener,
        IMainActivityCallback, DataManager.IDataManager, FloatingActionsMenu.OnFloatingActionsMenuUpdateListener {

    private static final String TAG = "MainActivity";
    public static final  String RECEIVER_TAG = "MainActivityReceiver";

    public static final String ACTION_ADD_ACTIVITY = "com.blackcj.fitdata.ADD_ACTIVITY";
    public static boolean active = false;
    protected DataManager mDataManager;

    private TabPagerAdapter mAdapter;

    @Bind(R.id.coordinatorLayout) CoordinatorLayout coordinatorLayout;
    @Bind(R.id.appBarLayout) AppBarLayout appBarLayout;
    @Bind(R.id.viewPager) ViewPager mViewPager;
    @Bind(R.id.main_overlay) View overlay;
    @Bind(R.id.floatingActionMenu) FloatingActionsMenu floatingActionMenu;
    @Bind(R.id.floatingActionButton) FloatingActionButton fab;


    ///////////////////////////////////////
    // LIFE CYCLE
    ///////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Transition fade = new ChangeBounds();
        fade.excludeTarget(android.R.id.navigationBarBackground, true);
        getWindow().setExitTransition(fade);
        getWindow().setEnterTransition(fade);

        setActionBarIcon(R.drawable.barchart_icon);
        ButterKnife.bind(this);

        mDataManager = DataManager.getInstance(this);

        mAdapter = new TabPagerAdapter(this.getSupportFragmentManager());

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        mViewPager = (ViewPager) findViewById(R.id.viewPager);
        mViewPager.setAdapter(mAdapter);

        if (tabLayout != null) {
            tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
            tabLayout.setupWithViewPager(mViewPager);
            tabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);
            tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    mViewPager.setCurrentItem(tab.getPosition());
                    //animateFab(tab.getPosition());
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });
        }

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));


        overlay.setVisibility(View.GONE);

        overlay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                floatingActionMenu.collapse();
                return true;
            }
        });

        floatingActionMenu.setOnFloatingActionsMenuUpdateListener(this);

        Intent intent = new Intent("com.blackcj.fitdata.START_REFRESH");
        sendBroadcast(intent);

        if (ACTION_ADD_ACTIVITY.equals(getIntent().getAction())) {
            // Invoked via the manifest shortcut.
            AddEntryActivity.launch(MainActivity.this, 3);
        }
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_main;
    }

    @Override
    protected void onResume() {
        super.onResume();
        overlay.setAlpha(0f);
        floatingActionMenu.collapse();
    }

    @Override
    public void onStart() {
        super.onStart();
        active = true;
        mDataManager = DataManager.getInstance(this);
        mDataManager.addListener(this);
        mDataManager.setContext(this);
        mDataManager.connect();
        appBarLayout.addOnOffsetChangedListener(this);
        getSupportFragmentManager().addOnBackStackChangedListener(this);

    }

    @Override
    protected void onStop() {
        active = false;
        mDataManager.removeListener(this);
        mDataManager.disconnect();
        appBarLayout.removeOnOffsetChangedListener(this);
        getSupportFragmentManager().removeOnBackStackChangedListener(this);
        super.onStop();
    }

    @Override
    protected void onPause() {
        floatingActionMenu.collapse();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mAdapter.destroy();
        //mDataManager.disconnect();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DataManager.REQUEST_OAUTH) {
            mDataManager.authInProgress = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                mDataManager.connect();
            }
        }
    }

    int index = 0;

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        index = i;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                PageFragment pageFragment = mAdapter.getFragment(mViewPager.getCurrentItem());
                if (pageFragment != null) {
                    if (index == 0) {
                        pageFragment.setSwipeToRefreshEnabled(true);
                    } else {
                        pageFragment.setSwipeToRefreshEnabled(false);
                    }
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onBackStackChanged()
    {
        FragmentManager manager = getSupportFragmentManager();
        if (manager != null && manager.getBackStackEntryCount() == 0 && overlay.getVisibility() == View.VISIBLE) {
            overlay.setAlpha(0f);

            floatingActionMenu.collapse();
        }
    }

    ///////////////////////////////////////
    // OPTIONS MENU
    ///////////////////////////////////////

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (menu != null) {
            MenuItem mSearchItem = menu.findItem(R.id.search_participants);
            SearchView mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setQueryHint("Search");
            MenuItemCompat.setOnActionExpandListener(mSearchItem, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    return true;
                }
            });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        boolean loadComplete = UserPreferences.getBackgroundLoadComplete(this);
        long lastSync = UserPreferences.getLastSync(this);
        switch (id) {
            case R.id.action_history:
                RecentActivity.launch(MainActivity.this);
                break;
            case R.id.action_reload_data:
                if (loadComplete) {
                    long syncStart = lastSync - (1000 * 60 * 60 * 24 * 7);
                    UserPreferences.setLastSync(this, syncStart);
                    startService(new Intent(this, BackgroundRefreshService.class));
                }
                return true;
            case R.id.action_settings:
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.enter_anim, R.anim.exit_anim, R.anim.enter_anim, R.anim.exit_anim);
                transaction.add(R.id.top_container, new SettingsFragment());
                transaction.addToBackStack("settings");
                transaction.commit();
                return true;
            case android.R.id.home:
                getSupportFragmentManager().popBackStack();
                return true;
            case R.id.action_import:
                if (loadComplete) {
                    restoreDatabase();
                    //long syncStart = lastSync - (1000 * 60 * 60 * 24 * 4);
                    //UserPreferences.setLastSync(this, syncStart);
                    //startService(new Intent(this, BackgroundRefreshService.class));

                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static final int REQUEST_WRITE_STORAGE = 112;

    /**
     * Used for debugging to restore some meaningful data
     */
    public void restoreDatabase() {
        boolean hasPermission = (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermission) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
        }
        Log.d("MainActivity", "Restoring database.");
        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();

            if (sd.canWrite()) {
                String backupDBPath = "//data//com.blackcj.fitdata//databases//googlefitexample.db";
                String currentDBPath = "sdcard/backup.db";
                File currentDB = new File(currentDBPath);
                File backupDB = new File(data, backupDBPath);

                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                    Log.d("MainActivity", "Database restored.");
                } else {
                    Log.d("MainActivity", "Database doesn't exist.");
                }
            }else {
                Log.d("MainActivity", "Unable to write file.");
            }
        } catch (Exception e) {
            Log.d("MainActivity", "Exception restoring database");
        }
    }

    ///////////////////////////////////////
    // SEARCH CALLBACKS
    ///////////////////////////////////////

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mAdapter.filter = newText;
        if (mAdapter.getFragment(mViewPager.getCurrentItem()) != null) {
            mAdapter.getFragment(mViewPager.getCurrentItem()).setFilterText(newText);
        }
        if (mAdapter.getFragment(mViewPager.getCurrentItem() + 1) != null) {
            mAdapter.getFragment(mViewPager.getCurrentItem() + 1).setFilterText(newText);
        }
        if (mAdapter.getFragment(mViewPager.getCurrentItem() - 1) != null) {
            mAdapter.getFragment(mViewPager.getCurrentItem() - 1).setFilterText(newText);
        }
        Log.d("MainActivity", "Query text: " + newText);
        return false;
    }

    @OnClick({R.id.step_button, R.id.bike_button, R.id.run_button, R.id.other_button})
    void showAddEntryFragment(View view) {
        int activityType;
        switch (view.getId()) {
            case R.id.step_button:
                activityType = 0;
                break;
            case R.id.run_button:
                activityType = 1;
                break;
            case R.id.bike_button:
                activityType = 2;
                break;
            default:
                activityType = 3;
        }

        AddEntryActivity.launch(MainActivity.this, activityType);

    }

    ///////////////////////////////////////
    // ACTIVITY CALLBACKS - IMainActivity
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
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        final long currentTime = cal.getTimeInMillis();

        Context context = getApplicationContext();
        long lastSync = UserPreferences.getLastSync(context);
        long lastStart = UserPreferences.getLastSyncStart(context);
        if (lastSync == 0 && (currentTime - lastStart) >  1000 * 60 * 20) {
            startService(new Intent(this, BackgroundRefreshService.class));
        }
    }

    @Override
    public void onDataChanged(final Utilities.TimeFrame timeFrame) {
        Log.d(TAG, "DATA CHANGED");
        // TODO: Refresh prev / next page too
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (timeFrame == Utilities.TimeFrame.BEGINNING_OF_DAY) {
                    if (mAdapter.getFragment(0) != null) {
                        mAdapter.getFragment(0).refreshData();
                    }
                } else if (timeFrame == Utilities.TimeFrame.BEGINNING_OF_WEEK) {
                    if (mAdapter.getFragment(1) != null) {
                        mAdapter.getFragment(1).refreshData();
                    }
                } else {
                    if (mAdapter.getFragment(mViewPager.getCurrentItem()) != null) {
                        mAdapter.getFragment(mViewPager.getCurrentItem()).refreshData();
                    }
                    if (mAdapter.getFragment(mViewPager.getCurrentItem() + 1) != null) {
                        mAdapter.getFragment(mViewPager.getCurrentItem() + 1).refreshData();
                    }
                    if (mAdapter.getFragment(mViewPager.getCurrentItem() - 1) != null) {
                        mAdapter.getFragment(mViewPager.getCurrentItem() - 1).refreshData();
                    }
                }
            }
        });
    }

    @Override
    public void onDataComplete() {

    }

    public void setStepCounting(boolean active) {
        if (mDataManager != null && UserPreferences.getCountSteps(this) != active) {
            mDataManager.setStepCounting(active);
        }
    }

    public void setActivityTracking(boolean active) {
        if (mDataManager != null && UserPreferences.getActivityTracking(this) != active) {
            mDataManager.setActivityTracking(active);
        }
    }

    @Override
    public void quickDataRead() {
        startService(new Intent(this, BackgroundRefreshService.class));
    }

    @Override
    public void launch(View transitionView, Workout workout) {
        getWindow().setExitTransition(null);
        DetailActivity.launch(MainActivity.this, transitionView, workout);
    }

    ///////////////////////////////////////
    // FLOATING MENU
    ///////////////////////////////////////

    @Override
    public void onMenuExpanded() {
        overlay.clearAnimation();
        float viewAlpha = overlay.getAlpha();
        overlay.setVisibility(View.VISIBLE);

        ValueAnimator fadeAnim = ObjectAnimator.ofFloat(overlay, "alpha", viewAlpha, 1f);
        fadeAnim.setDuration(200L);

        fadeAnim.start();
    }

    @Override
    public void onMenuCollapsed() {
        overlay.clearAnimation();
        float viewAlpha = overlay.getAlpha();
        ValueAnimator fadeAnim = ObjectAnimator.ofFloat(overlay, "alpha", viewAlpha, 0f);
        fadeAnim.setDuration(300L);
        fadeAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                overlay.setVisibility(View.GONE);
            }
        });

        fadeAnim.start();
    }
}
