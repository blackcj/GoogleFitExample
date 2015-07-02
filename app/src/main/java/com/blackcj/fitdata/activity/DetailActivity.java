package com.blackcj.fitdata.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.graphics.Palette;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.blackcj.fitdata.R;
import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.adapter.TabPagerAdapter;
import com.blackcj.fitdata.fragment.ReportsFragment;
import com.blackcj.fitdata.model.Workout;
import com.blackcj.fitdata.model.WorkoutTypes;

import butterknife.Bind;
import butterknife.ButterKnife;


/**
 * Created by chris.black on 5/2/15.
 */
public class DetailActivity extends BaseActivity {

    private static final String EXTRA_TYPE = "DetailActivity:type";
    private static final String EXTRA_TITLE = "DetailActivity:title";
    private static final String EXTRA_IMAGE = "DetailActivity:image";

    @Bind(R.id.spinner) Spinner navigationSpinner;

    private ReportsFragment mReportsFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
        ImageView image = (ImageView) findViewById(R.id.image);
        ViewCompat.setTransitionName(image, EXTRA_IMAGE);
        image.setImageResource(getIntent().getIntExtra(EXTRA_IMAGE, R.drawable.heart_icon));

        Bitmap bitmap = ((BitmapDrawable)image.getDrawable()).getBitmap();
        Palette palette = Palette.generate(bitmap);
        int vibrant = palette.getVibrantColor(0x000000);
        //int vibrant = palette.getMutedColor(0x000000);
        if(vibrant == 0) {
            //vibrant = palette.getLightMutedColor(0x000000);
            vibrant = palette.getMutedColor(0x000000);
        }
        image.setBackgroundColor(Utilities.lighter(vibrant, 0.4f));

        View container = findViewById(R.id.container);
        container.setBackgroundColor(vibrant);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setTitle(getIntent().getStringExtra(EXTRA_TITLE));
        }

        toolbar.setBackgroundColor(vibrant);

        ArrayAdapter spinnerAdapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.graph_types, R.layout.spinner_item);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        navigationSpinner.setAdapter(spinnerAdapter);

        navigationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                updateReport();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        getWindow().setStatusBarColor(vibrant);


        FragmentManager fragmentManager = getSupportFragmentManager();
        mReportsFragment = ReportsFragment.newInstance(getIntent().getIntExtra(EXTRA_TYPE, 0), 1);
        fragmentManager.beginTransaction()
                       .replace(R.id.chart_container, mReportsFragment)
                       .commit();
    }

    private void updateReport() {
        int selectedIndex = navigationSpinner.getSelectedItemPosition();
        switch (selectedIndex) {
            case 0:
                mReportsFragment.setGroupCount(1);
                break;
            case 1:
                mReportsFragment.setGroupCount(7);
                break;
        }
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_detail;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                // Reverse the animation back to the previous view.
                finishAfterTransition();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void launch(BaseActivity activity, View transitionView, Workout workout) {
        ActivityOptionsCompat options =
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                        activity, transitionView, EXTRA_IMAGE);
        Intent intent = new Intent(activity, DetailActivity.class);
        intent.putExtra(EXTRA_IMAGE, WorkoutTypes.getImageById(workout.type));
        intent.putExtra(EXTRA_TITLE, WorkoutTypes.getWorkOutTextById(workout.type));
        intent.putExtra(EXTRA_TYPE, workout.type);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }
}