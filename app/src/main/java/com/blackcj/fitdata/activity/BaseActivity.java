package com.blackcj.fitdata.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.blackcj.fitdata.R;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;

import io.fabric.sdk.android.Fabric;


/**
 * Created by Chris Black
 *
 * Contains functionality common to all Activities. Code here should be kept to the bare
 * minimum.
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics(), new Answers());
        setContentView(getLayoutResource());
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    protected abstract int getLayoutResource();

    protected void setActionBarIcon(int iconRes) {
        toolbar.setNavigationIcon(iconRes);
    }
}