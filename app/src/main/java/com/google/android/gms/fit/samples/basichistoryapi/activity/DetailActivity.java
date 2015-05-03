package com.google.android.gms.fit.samples.basichistoryapi.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.graphics.Palette;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.gms.fit.samples.basichistoryapi.R;
import com.google.android.gms.fit.samples.basichistoryapi.Utilities;
import com.google.android.gms.fit.samples.basichistoryapi.fragment.ReportsFragment;
import com.google.android.gms.fit.samples.basichistoryapi.model.Workout;
import com.google.android.gms.fit.samples.basichistoryapi.model.WorkoutTypes;


/**
 * Created by chris.black on 5/2/15.
 */
public class DetailActivity extends BaseActivity {


    private static final String EXTRA_TYPE = "DetailActivity:type";
    private static final String EXTRA_TITLE = "DetailActivity:title";
    private static final String EXTRA_IMAGE = "DetailActivity:image";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImageView image = (ImageView) findViewById(R.id.image);
        ViewCompat.setTransitionName(image, EXTRA_IMAGE);
        image.setImageResource(getIntent().getIntExtra(EXTRA_IMAGE, R.drawable.heart_icon));

        Bitmap bitmap = ((BitmapDrawable)image.getDrawable()).getBitmap();
        Palette palette = Palette.generate(bitmap);
        int vibrant = palette.getVibrantColor(0x000000);
        image.setBackgroundColor(Utilities.lighter(vibrant, 0.4f));

        RelativeLayout container = (RelativeLayout) findViewById(R.id.container);
        container.setBackgroundColor(vibrant);

        getSupportActionBar().setTitle(getIntent().getStringExtra(EXTRA_TITLE));

        toolbar.setBackgroundColor(vibrant);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(vibrant);
        }

        // TODO: Pass in workout type instead of title.
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                       .replace(R.id.chart_container, ReportsFragment.newInstance(getIntent().getIntExtra(EXTRA_TYPE, 0)))
                       .commit();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_detail;
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
                // do something useful
                finishAfterTransition();
                return true;
            case R.id.action_settings:
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