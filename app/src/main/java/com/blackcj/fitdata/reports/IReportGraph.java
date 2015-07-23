package com.blackcj.fitdata.reports;

import android.app.Activity;
import android.util.DisplayMetrics;

import com.blackcj.fitdata.model.Workout;

import org.achartengine.GraphicalView;

/**
 * Created by chris.black on 6/29/15.
 */
public interface IReportGraph {
    public GraphicalView getChartGraph(Activity activity);
    public void clearData();
    public void addWorkout(int series, int data, int position);
    public void updateRenderer();
    public void setGoal(int goalValue);
    public void setDisplayMetrics(int dpi);
    public void addRenderer(int series, Activity activity, int color);
    public double getDataAtPoint(double xPos);
}
