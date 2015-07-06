package com.blackcj.fitdata.fragment;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.database.CupboardSQLiteOpenHelper;
import com.blackcj.fitdata.model.Workout;
import com.blackcj.fitdata.R;
import com.blackcj.fitdata.model.WorkoutTypes;
import com.blackcj.fitdata.reports.BaseReportGraph;
import com.blackcj.fitdata.reports.MultipleLineGraphs;
import com.blackcj.fitdata.reports.SingleBarGraphWithGoal;

import org.achartengine.GraphicalView;
import org.achartengine.tools.PanListener;
import org.achartengine.tools.ZoomEvent;
import org.achartengine.tools.ZoomListener;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import nl.qbusict.cupboard.QueryResultIterable;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by chris.black on 5/2/15.
 */
@SuppressWarnings("WeakerAccess") // Butterknife requires public reference of injected views
public class ReportsFragment extends BaseFragment {

    public static final String ARG_WORKOUT_TYPE = "workout_type";
    public static final String ARG_GROUP_COUNT = "group_count";
    public static final String TAG = "ReportsFragment";

    private BaseReportGraph reportGraph;
    private int multiplier = 1;
    private int numDays = 60;
    private int numSegments;
    private long millisecondsInSegment;

    private SQLiteDatabase db;

    /** The chart view that displays the data. */
    private GraphicalView mChartView;

    private int workoutType;
    @Bind(R.id.chart)
    FrameLayout mChartLayout;

    public static ReportsFragment newInstance(int workoutType, int groupCount) {
        ReportsFragment f = new ReportsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_WORKOUT_TYPE, workoutType);
        args.putInt(ARG_GROUP_COUNT, groupCount);
        f.setArguments(args);
        return f;
    }

    int densityDpi = 0;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        densityDpi = metrics.densityDpi;
        if (reportGraph != null) {
            reportGraph.setDisplayMetrics(metrics.densityDpi);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        workoutType = getArguments() != null ? getArguments().getInt(ARG_WORKOUT_TYPE) : 0;
        multiplier = getArguments() != null ? getArguments().getInt(ARG_GROUP_COUNT) : 1;
        numSegments = (int)Math.ceil((double)numDays / (double)multiplier);
        millisecondsInSegment = 1000*60*60*24*multiplier;
        Log.i(TAG, "Workout type:" + workoutType);
        if (workoutType == WorkoutTypes.TIME.getValue()) {
            reportGraph = new MultipleLineGraphs();
        } else {
            reportGraph = new SingleBarGraphWithGoal();
        }
        if (workoutType == WorkoutTypes.STEP_COUNT.getValue()) {
            reportGraph.setGoal(9500 * multiplier);
        } else {
            // TODO: Create system for managing goals
            reportGraph.setGoal(100 * multiplier);
        }
        reportGraph.setDisplayMetrics(densityDpi);
    }

    long currentTimeStamp;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_report, container, false);

        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        currentTimeStamp = cal.getTimeInMillis();
        ButterKnife.bind(this, view);
        CupboardSQLiteOpenHelper dbHelper = new CupboardSQLiteOpenHelper(this.getActivity());
        db = dbHelper.getWritableDatabase();

        mChartView = reportGraph.getChartGraph(getActivity());

        mChartView.addZoomListener(new ZoomListener() {
            public void zoomApplied(ZoomEvent e) {
                updateLabels();
            }

            public void zoomReset() {
            }
        }, true, true);
        mChartView.addPanListener(new PanListener() {
            @Override
            public void panApplied() {
                updateLabels();
            }
        });
        mChartLayout.addView(mChartView);

        // Now return the SwipeRefreshLayout as this fragment's content view
        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    public void setGroupCount(int groupCount) {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        if (groupCount == 1) {
            numDays = 60;
        } else {
            numDays = 60 + day;
        }
        Log.d(TAG, "GroupCount: " + groupCount + " Number of days: " + numDays);
        multiplier = groupCount;
        numSegments = (int)Math.ceil((double) (numDays) / (double) multiplier);
        millisecondsInSegment = 1000*60*60*24*multiplier;
        if (workoutType == WorkoutTypes.STEP_COUNT.getValue()) {
            reportGraph.setGoal(9500 * multiplier);
        } else {
            // TODO: Create system for managing goals
            reportGraph.setGoal(100 * multiplier);
        }
        showData();
    }

    private void updateLabels() {
        double start = reportGraph.getRenderer().getXAxisMin();
        double stop = reportGraph.getRenderer().getXAxisMax();
        double quarterStep = (stop - start) / 8;
        double halfStep = (stop - start) / 2;
        reportGraph.getRenderer().clearXTextLabels();
        long index = numSegments - normalize((int) (start + quarterStep)) - 1;
        reportGraph.getRenderer().addXTextLabel(start + quarterStep, Utilities.getDayString(currentTimeStamp - index * millisecondsInSegment));
        index = numSegments - normalize((int) (start + halfStep)) - 1;
        reportGraph.getRenderer().addXTextLabel(start + halfStep, Utilities.getDayString(currentTimeStamp - index * millisecondsInSegment));
        index = numSegments - normalize((int) (stop - quarterStep)) - 1;
        reportGraph.getRenderer().addXTextLabel(stop - quarterStep, Utilities.getDayString(currentTimeStamp - index * millisecondsInSegment));
    }

    private int normalize(int index) {
        int result = index > 0 ? index : 0;
        result = result < numSegments ? result : numSegments - 1;
        return result;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mChartView != null) {
            mChartView.repaint();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        showData();
    }

    @Override
    public void onStop() {
        super.onStop();
        //mDataSource.removeListeners();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save the current data, for instance when changing screen orientation
        outState.putSerializable("reportGraph", reportGraph);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            // Restore last state for checked position.
            reportGraph = (BaseReportGraph) savedInstanceState.getSerializable("reportGraph");
        }
    }

    // TODO: Fix this ugly function
    public void showData() {
        Map<Integer, Integer[]> map = new HashMap<>();
        reportGraph.clearData();

        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        int week_of_year = cal.get(Calendar.WEEK_OF_YEAR);
        if (multiplier == 7) {
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_YEAR, -numDays + 1);        // 30 days of history
        long startTime = cal.getTimeInMillis();
        long baseline = (startTime - startTime % millisecondsInSegment) / millisecondsInSegment;
        QueryResultIterable<Workout> itr = cupboard().withDatabase(db).query(Workout.class).withSelection("start >= ?", "" + startTime).query();
        for (Workout workout : itr) {
            cal.setTimeInMillis(workout.start);
            long id = (workout.start - workout.start % millisecondsInSegment) / millisecondsInSegment - baseline;
            if (multiplier == 7) {
                id = numSegments - (week_of_year - cal.get(Calendar.WEEK_OF_YEAR));
            }
            if (workout.type == workoutType) {
                //Log.d(TAG, id + " | " + numSegments + " | " + workout.toString());
            }
            if (id < numSegments) {

                if (workoutType == WorkoutTypes.TIME.getValue() && workout.type != WorkoutTypes.STEP_COUNT.getValue() && workout.type != WorkoutTypes.STILL.getValue() && workout.type != WorkoutTypes.IN_VEHICLE.getValue()) {
                    // Put all data here to show totals
                    if (map.get(workoutType) == null) {
                        Integer[] dataMap = new Integer[numSegments];
                        Arrays.fill(dataMap, 0);
                        dataMap[(int) id] = (int) (workout.duration / 1000 / 60);
                        map.put(workoutType, dataMap);
                    } else {
                        Integer[] dataMap = map.get(workoutType);
                        dataMap[(int) id] += (int) (workout.duration / 1000 / 60);
                    }

                    if (map.get(workout.type) == null) {
                        Integer[] dataMap = new Integer[numSegments];
                        Arrays.fill(dataMap, 0);
                        dataMap[(int) id] = (int) (workout.duration / 1000 / 60);
                        map.put(workout.type, dataMap);
                    } else {
                        Integer[] dataMap = map.get(workout.type);
                        dataMap[(int) id] += (int) (workout.duration / 1000 / 60);
                    }
                } else if (workout.type == workoutType) {
                    if (map.get(workout.type) == null) {
                        Integer[] dataMap = new Integer[numSegments];
                        Arrays.fill(dataMap, 0);
                        if (workout.type == WorkoutTypes.STEP_COUNT.getValue()) {
                            dataMap[(int) id] = workout.stepCount;
                        } else {
                            dataMap[(int) id] = (int) (workout.duration / 1000 / 60);
                        }

                        map.put(workout.type, dataMap);
                    } else {
                        Integer[] dataMap = map.get(workout.type);
                        if (workout.type == WorkoutTypes.STEP_COUNT.getValue()) {
                            dataMap[(int) id] += workout.stepCount;
                        } else {
                            dataMap[(int) id] += (int) (workout.duration / 1000 / 60);
                        }
                    }
                }
            }
        }
        itr.close();

        // TODO: END
        int series = 0;
        for (Integer workoutType : map.keySet()) {
            int color = getResources().getColor(R.color.other_graph);
            if (workoutType == WorkoutTypes.WALKING.getValue()) {
                color = getResources().getColor(R.color.walking_graph);
            } else if (workoutType == WorkoutTypes.RUNNING.getValue()) {
                color = getResources().getColor(R.color.running_graph);
            } else if (workoutType == WorkoutTypes.BIKING.getValue()) {
                color = getResources().getColor(R.color.biking_graph);
            }
            reportGraph.addRenderer(series, getActivity(), color);
            Integer[] dataMap = map.get(workoutType);
            for (int n = 0; n < numSegments; n++) {
                reportGraph.addWorkout(series, dataMap[n], n);
            }
            series++;
        }

        reportGraph.updateRenderer();
        updateLabels();
        mChartView.repaint();
    }
}