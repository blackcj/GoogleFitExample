package com.blackcj.fitdata.fragment;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.blackcj.fitdata.R;
import com.blackcj.fitdata.Utilities;
import com.blackcj.fitdata.database.DataManager;
import com.blackcj.fitdata.model.Workout;
import com.blackcj.fitdata.model.WorkoutTypes;

import java.util.Calendar;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by chris.black on 6/19/15.
 */
public class AddEntryFragment extends Fragment {

    public static final String TAG = "AddEntryFragment";

    protected DataManager.IDataManager mCallback;
    Calendar cal = Calendar.getInstance();

    @InjectView(R.id.timeTextView)
    TextView timeTextView;

    @InjectView(R.id.dateTextView)
    TextView dateTextView;

    @InjectView(R.id.activitySpinner)
    Spinner activitySpinner;

    @InjectView(R.id.editTextMinutes)
    EditText editTextMinutes;

    @InjectView(R.id.editTextSteps)
    EditText editTextSteps;

    @InjectView(R.id.editInputLayout)
    TextInputLayout editInputLayout;

    public static AddEntryFragment create() {
        AddEntryFragment fragment = new AddEntryFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_entry, container, false);

        ButterKnife.inject(this, view);

        Calendar mcurrentTime = Calendar.getInstance();
        year = mcurrentTime.get(Calendar.YEAR);
        month = mcurrentTime.get(Calendar.MONTH);
        day = mcurrentTime.get(Calendar.DAY_OF_MONTH);

        hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        minute = mcurrentTime.get(Calendar.MINUTE);


        cal.set(year, month, day, hour, minute);

        timeTextView.setText(Utilities.getTimeString(cal.getTimeInMillis()));
        dateTextView.setText(Utilities.getDateString(cal.getTimeInMillis()));

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(activity instanceof DataManager.IDataManager) {
            mCallback = (DataManager.IDataManager)activity;
        }
    }

    /**
     * Clear callback on detach to prevent null reference errors after the view has been
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    int year;
    int month;
    int day;

    @OnClick(R.id.dateTextView) void onDateSelect() {
        DatePickerDialog mTimePicker;
        mTimePicker = new DatePickerDialog(this.getActivity(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int selectedYear, int monthOfYear, int dayOfMonth) {
                day = dayOfMonth;
                year = selectedYear;
                month = monthOfYear;
                cal.set(year, month, day, hour, minute);
                dateTextView.setText(Utilities.getDateString(cal.getTimeInMillis()));
            }
        }, year,  month, day);
        mTimePicker.setTitle("Select Date");
        mTimePicker.show();
    }

    int hour;
    int minute;

    @OnClick(R.id.timeTextView) void onTimeSelect() {

        TimePickerDialog mTimePicker;
        mTimePicker = new TimePickerDialog(this.getActivity(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                hour = selectedHour;
                minute = selectedMinute;
                cal.set(year, month, day, hour, minute);
                timeTextView.setText(Utilities.getTimeString(cal.getTimeInMillis()));
            }
        }, hour, minute, false);
        mTimePicker.setTitle("Select Time");
        mTimePicker.show();
    }

    @OnClick(R.id.cancel_button) void onCancel() {
        getActivity().getSupportFragmentManager().popBackStack();
    }

    @OnClick(R.id.save_button) void onSave() {
        // need validation
        Workout workout = new Workout();

        cal.set(year, month, day, hour, minute);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.MINUTE, -(Integer.parseInt(editTextMinutes.getText().toString())));
        workout.start = cal.getTimeInMillis();
        workout.duration = endTime - workout.start;
        int selectedIndex = activitySpinner.getSelectedItemPosition();
        switch (selectedIndex) {
            case 0:
                workout.type = WorkoutTypes.WALKING.getValue();
                break;
            case 1:
                workout.type = WorkoutTypes.RUNNING.getValue();
                break;
            case 2:
                workout.type = WorkoutTypes.BIKING.getValue();
                break;
            case 3:
                workout.type = WorkoutTypes.GOLF.getValue();
                break;
            case 4:
                workout.type = WorkoutTypes.KAYAKING.getValue();
                break;
            case 5:
                workout.type = WorkoutTypes.STRENGTH_TRAINING.getValue();
                break;
            case 6:
                workout.type = WorkoutTypes.IN_VEHICLE.getValue();
                break;
        }
        workout.stepCount = Integer.parseInt(editTextSteps.getText().toString());

        boolean valid = true;
        if (workout.type == WorkoutTypes.WALKING.getValue()) {
            if ((workout.stepCount / 1000) * 10 > workout.duration / (1000 * 60)) {
                valid = false;
                editInputLayout.setError("Maximum of 1000 steps per 10 minutes walking");
            }
        }

        if (valid) {
            mCallback.insertData(workout);
            Log.d(TAG, workout.toString());
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }
}
