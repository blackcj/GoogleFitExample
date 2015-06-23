package com.blackcj.fitdata.activity;

import android.view.View;

import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.model.Workout;

import java.util.List;

/**
 * Created by chris.black on 5/2/15.
 */
public interface IMainActivityCallback {
    public void launch(View transitionView, Workout workout);
    List<Workout> getData(Utilities.TimeFrame timeFrame);
}
