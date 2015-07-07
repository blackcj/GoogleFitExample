package com.blackcj.fitdata.reports;

import android.app.Activity;
import android.graphics.Color;

import com.blackcj.fitdata.R;
import com.blackcj.fitdata.model.WorkoutTypes;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.chart.CombinedXYChart;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

/**
 * Created by chris.black on 6/29/15.
 */
public class SingleBarGraphWithGoal extends BaseReportGraph {

    /** The most recently added series. */
    protected XYSeries mCurrentSeries;
    /** The most recently added series. */
    protected XYSeries mGoalSeries;

    @Override
    public GraphicalView getChartGraph(Activity activity) {
        setUpRenderer(activity);
        mCurrentSeries = new XYSeries("Data");
        mGoalSeries = new XYSeries("Goal");
        mDataset.addSeries(mCurrentSeries);
        mDataset.addSeries(mGoalSeries);
        CombinedXYChart.XYCombinedChartDef[] types = new CombinedXYChart.XYCombinedChartDef[] {new CombinedXYChart.XYCombinedChartDef(BarChart.TYPE, 0), new CombinedXYChart.XYCombinedChartDef(BarChart.TYPE, 1)};
        return ChartFactory.getCombinedXYChartView(activity, mDataset, mRenderer, types);
    }

    @Override
    public void clearData() {
        maxData = 70;
        minData = 70;
        mCurrentSeries.clear();
        mGoalSeries.clear();
    }

    private void setUpRenderer(Activity activity) {
        // Now we create the renderer
        XYSeriesRenderer mCurrentRenderer = new XYSeriesRenderer();
        mCurrentRenderer.setLineWidth(getDPI(2));
        mCurrentRenderer.setColor(activity.getResources().getColor(R.color.colorBarGraph));
        // Include low and max value
        mCurrentRenderer.setDisplayBoundingPoints(true);
        // we add point markers
        mCurrentRenderer.setPointStyle(PointStyle.CIRCLE);
        mCurrentRenderer.setPointStrokeWidth(getDPI(5));
        mCurrentRenderer.setShowLegendItem(false);

        // Creating XYSeriesRenderer to customize expenseSeries
        XYSeriesRenderer expenseRenderer = new XYSeriesRenderer();
        expenseRenderer.setColor(activity.getResources().getColor(R.color.colorBarGraph2));
        expenseRenderer.setPointStyle(PointStyle.POINT);
        expenseRenderer.setFillPoints(true);
        expenseRenderer.setLineWidth(getDPI(3));
        expenseRenderer.setShowLegendItem(false);

        mRenderer = new XYMultipleSeriesRenderer();
        mRenderer.addSeriesRenderer(mCurrentRenderer);
        mRenderer.addSeriesRenderer(expenseRenderer);
        // We want to avoid black border
        mRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00)); // transparent margins
        // Disable Pan on two axis
        mRenderer.setPanEnabled(true, false);
        mRenderer.setZoomEnabled(true, false);
        mRenderer.setLabelsTextSize(getDPI(15));
        mRenderer.setXLabelsColor(activity.getResources().getColor(R.color.colorWhite));
        mRenderer.setYLabelsColor(0, activity.getResources().getColor(R.color.colorWhite));
        mRenderer.setLabelsColor(activity.getResources().getColor(R.color.colorWhite));
        mRenderer.setAxesColor(activity.getResources().getColor(R.color.colorWhite));
        mRenderer.setBarSpacing(0.2);

        mRenderer.setShowGrid(true); // we show the grid
        mRenderer.setXLabels(0);
    }

    @Override
    public void addWorkout(int series, int data, int position) {
        if (data >= 0) {
            mGoalSeries.add(position, mGoal);
        }
        if (data > maxData) maxData = data;
        if (data < minData) minData = data;
        mCurrentSeries.add(position, data);
    }

    @Override
    public void updateRenderer() {
        mRenderer.clearXTextLabels();
        mRenderer.setYAxisMax(Math.round(maxData) + (maxData / 5));
        mRenderer.setYAxisMin(0);
        mRenderer.setXAxisMin(mCurrentSeries.getItemCount() - 7);
        mRenderer.setXAxisMax(mCurrentSeries.getItemCount());
        mRenderer.setPanLimits(new double[]{-5, mCurrentSeries.getItemCount() + 5, 0, 0});
        mRenderer.setZoomLimits(new double[]{-5, mCurrentSeries.getItemCount() + 5, 0, 0});
        // An array containing the margin size values, in this order: top, left, bottom, right
        mRenderer.setMargins(new int [] {getDPI(12),0,getDPI(10),0});

        mRenderer.setYLabelsPadding(-getDPI(35));

        mDataset.clear();
        mDataset.addSeries(mCurrentSeries);
        mDataset.addSeries(mGoalSeries);
    }

    @Override
    public void addRenderer(int series, Activity activity, int color) {

    }

}
