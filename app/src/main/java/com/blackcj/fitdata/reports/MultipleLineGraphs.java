package com.blackcj.fitdata.reports;

import android.app.Activity;
import android.graphics.Color;

import com.blackcj.fitdata.R;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.chart.CombinedXYChart;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.ArrayList;

/**
 * Created by Chris Black
 *
 * Class that renders a line graph for each fitness activity type.
 */
public class MultipleLineGraphs extends BaseReportGraph {

    protected ArrayList<XYSeries> mSeriesDataSet;

    @Override
    public GraphicalView getChartGraph(Activity activity) {
        mSeriesDataSet = new ArrayList<>();
        setUpRenderer(activity);
        return ChartFactory.getLineChartView(activity, mDataset, mRenderer);
    }

    private void setUpRenderer(Activity activity) {
        mRenderer = new XYMultipleSeriesRenderer();
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

        mRenderer.setShowGrid(true); // we show the grid
        mRenderer.setXLabels(0);
    }

    @Override
    public void clearData() {
        maxData = 15;
        minData = 10;
        for (int n = 0; n < mSeriesDataSet.size(); n++) {
            mSeriesDataSet.get(n).clear();
        }
    }

    @Override
    public void addWorkout(int series, int data, int position) {
        if (data > maxData) maxData = data;
        if (data < minData) minData = data;
        mSeriesDataSet.get(series).add(position, data);
    }

    @Override
    public void updateRenderer() {
        mRenderer.clearXTextLabels();
        mRenderer.setYAxisMax(Math.round(maxData) + (maxData / 5));
        mRenderer.setXAxisMin(mSeriesDataSet.get(0).getItemCount() - 7);
        mRenderer.setXAxisMax(mSeriesDataSet.get(0).getItemCount());
        mRenderer.setPanLimits(new double[]{-5, mSeriesDataSet.get(0).getItemCount() + 5, 0, 0});
        mRenderer.setZoomLimits(new double[]{-5, mSeriesDataSet.get(0).getItemCount() + 5, 0, 0});
        // An array containing the margin size values, in this order: top, left, bottom, right
        mRenderer.setMargins(new int[]{5, 0, 5, 0});

        mRenderer.setYLabelsPadding(-getDPI(35));

        mDataset.clear();
        for (int n = 0; n < mSeriesDataSet.size(); n++) {
            mDataset.addSeries(n, mSeriesDataSet.get(n));
        }
    }

    @Override
    public void addRenderer(int series, Activity activity, int color) {
        XYSeriesRenderer mCurrentRenderer = new XYSeriesRenderer();
        mCurrentRenderer.setLineWidth(getDPI(3));
        mCurrentRenderer.setColor(color);
        // Include low and max value
        mCurrentRenderer.setDisplayBoundingPoints(true);
        // we add point markers
        mCurrentRenderer.setPointStyle(PointStyle.CIRCLE);
        mCurrentRenderer.setPointStrokeWidth(getDPI(5));
        mCurrentRenderer.setShowLegendItem(false);

        XYSeries seriesData = new XYSeries("data" + series);
        mSeriesDataSet.add(series, seriesData);

        mRenderer.addSeriesRenderer(series, mCurrentRenderer);
    }

    @Override
    public double getDataAtPoint(double yPos) {
        return 0;
    }
}
