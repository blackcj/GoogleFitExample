package com.blackcj.fitdata.reports;

import android.util.DisplayMetrics;

import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import java.io.Serializable;

/**
 * Created by Chris BLack
 *
 * Base class for report graphs
 */
public abstract class BaseReportGraph implements IReportGraph, Serializable {

    protected int densityDPI = 1;
    double maxData = 15;
    double minData = 10;
    int mGoal = 0;

    /** The main data set that includes all the series that go into a chart. */
    protected XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    /** The main renderer that includes all the renderer customizing a chart. */
    protected XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();


    public void setDisplayMetrics(int dpi) {
        densityDPI = dpi;
    }

    public XYMultipleSeriesRenderer getRenderer() {
        return mRenderer;
    }

    /**
     * Baseline an int to the correct pixel density.
     *
     * @param size int to convert
     * @return Size in DP
     */
    public int getDPI(int size){
        return (size * densityDPI) / DisplayMetrics.DENSITY_DEFAULT;
    }

    @Override
    public void setGoal(int goalValue) {
        mGoal = goalValue;
    }
}
