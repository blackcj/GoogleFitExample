package com.blackcj.fitdata.reports;

import android.util.DisplayMetrics;

import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import java.io.Serializable;

/**
 * Created by chris.black on 6/29/15.
 */
public abstract class BaseReportGraph implements IReportGraph, Serializable {

    protected int densityDPI = 1;
    double maxData = 15;
    double minData = 10;
    int mGoal = 0;

    /** The main dataset that includes all the series that go into a chart. */
    protected XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    /** The main renderer that includes all the renderers customizing a chart. */
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
     * @return
     */
    public int getDPI(int size){
        return (size * densityDPI) / DisplayMetrics.DENSITY_DEFAULT;
    }

    @Override
    public void setGoal(int goalValue) {
        mGoal = goalValue;
    }
}
