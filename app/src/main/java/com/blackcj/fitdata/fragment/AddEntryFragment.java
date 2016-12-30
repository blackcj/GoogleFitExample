package com.blackcj.fitdata.fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import java.util.Calendar;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;

/**
 * Created by Chris Black
 *
 * Input form used to add a manual entry.
 */
@SuppressWarnings({"WeakerAccess", "unused"}) // Butterknife requires public reference of injected views
public class AddEntryFragment extends Fragment {

    public static final String ARG_ACTIVITY_TYPE = "ARG_ACTIVITY_TYPE";
    public static final String TAG = "AddEntryFragment";

    private DataManager.IDataManager mCallback;
    private Calendar cal = Calendar.getInstance();

    @Bind(R.id.timeTextView)
    TextView timeTextView;

    @Bind(R.id.dateTextView)
    TextView dateTextView;

    @Bind(R.id.activitySpinner)
    Spinner activitySpinner;

    @Bind(R.id.editTextMinutes)
    EditText editTextMinutes;

    @Bind(R.id.editTextSteps)
    EditText editTextSteps;

    @Bind(R.id.editInputLayout)
    TextInputLayout editInputLayout;

    @Bind(R.id.editInputLayout2)
    TextInputLayout editInputLayout2;

    @Bind(R.id.editInputLayout3)
    TextInputLayout editInputLayoutTime;

    @Bind(R.id.labelText2)
    TextView labelText2;

    private int mActivityType;

    public static AddEntryFragment create(int activityType) {
        Bundle args = new Bundle();
        args.putInt(ARG_ACTIVITY_TYPE, activityType);
        AddEntryFragment fragment = new AddEntryFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityType = getArguments().getInt(ARG_ACTIVITY_TYPE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_entry, container, false);

        ButterKnife.bind(this, view);

        editTextSteps.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {
                if(!editTextSteps.getText().toString().equals("")) {
                    editTextMinutes.setText("" + (int)(Double.parseDouble(editTextSteps.getText().toString()) / 1000.0 * 10));
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });

        Calendar mCurrentTime = Calendar.getInstance();
        mCurrentTime.add(Calendar.MINUTE, -30);
        year = mCurrentTime.get(Calendar.YEAR);
        month = mCurrentTime.get(Calendar.MONTH);
        day = mCurrentTime.get(Calendar.DAY_OF_MONTH);
        hour = mCurrentTime.get(Calendar.HOUR_OF_DAY);
        minute = mCurrentTime.get(Calendar.MINUTE);
        cal.set(year, month, day, hour, minute);

        timeTextView.setText(Utilities.getTimeString(cal.getTimeInMillis()));
        dateTextView.setText(Utilities.getDateString(cal.getTimeInMillis()));


        activitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                updateView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        if (mActivityType < 3) {
            activitySpinner.setSelection(mActivityType);
            updateView();
        }
        if(Answers.getInstance() != null) {
            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName("Add new entry")
                    .putContentType("View")
                    .putContentId("AddEntryFragment"));
        }

        return view;
    }

    private void updateView() {
        int selectedIndex = activitySpinner.getSelectedItemPosition();
        switch (selectedIndex) {
            case 0:
            case 1:
            case 3:
            case 6:
            case 8:
            case 9:
                editInputLayout2.setVisibility(View.VISIBLE);
                labelText2.setVisibility(View.VISIBLE);
                break;
            case 2:
            case 4:
            case 5:
            case 7:
            case 10:
            case 11:
                editInputLayout2.setVisibility(View.GONE);
                labelText2.setVisibility(View.GONE);
                break;
            default:
                editInputLayout2.setVisibility(View.VISIBLE);
                labelText2.setVisibility(View.VISIBLE);
                break;
        }
    }


    @Override
    public void onAttach(Context activity) {
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


    @OnFocusChange(R.id.timeTextView) void onTimeSelect(View v, boolean hasFocus) {
        if (hasFocus) {
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
            if (editTextSteps.getVisibility() == View.VISIBLE) {
                editTextSteps.requestFocus();
            } else {
                editTextMinutes.requestFocus();
            }
        }
    }

    public Workout getWorkout() {
        // need validation
        Workout workout = new Workout();

        cal.set(year, month, day, hour, minute);
        long startTime = cal.getTimeInMillis();
        workout.start = startTime;
        workout._id = startTime;
        cal.add(Calendar.MINUTE, Integer.parseInt(editTextMinutes.getText().toString()));
        workout.duration = cal.getTimeInMillis() - startTime;
        workout.stepCount = Integer.parseInt(editTextSteps.getText().toString());
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
                workout.stepCount = 0;
                break;
            case 3:
                workout.type = WorkoutTypes.GOLF.getValue();
                break;
            case 4:
                workout.type = WorkoutTypes.IN_VEHICLE.getValue();
                workout.stepCount = 0;
                break;
            case 5:
                workout.type = WorkoutTypes.KAYAKING.getValue();
                workout.stepCount = 0;
                break;
            case 6:
                workout.type = WorkoutTypes.STRENGTH_TRAINING.getValue();
                break;
            case 7:
                workout.type = WorkoutTypes.SLEEP.getValue();
                workout.stepCount = 0;
                break;
            case 8:
                workout.type = WorkoutTypes.SNOWBOARDING.getValue();
                break;
            case 9:
                workout.type = WorkoutTypes.TENNIS.getValue();
                break;
            case 10:
                workout.type = WorkoutTypes.ROCK_CLIMBING.getValue();
                break;
            case 11:
                workout.type = WorkoutTypes.GARDENING.getValue();
                break;

        }

        if (workout.type == WorkoutTypes.WALKING.getValue()) {
            if ((workout.stepCount / 1000) * 10 > workout.duration / (1000 * 60)) {
                workout = null;
                editInputLayout2.setError("Maximum of 1000 steps per 10 minutes walking");
            }
        }
        Date now = new Date();
        cal.setTime(now);
        if (workout != null && workout.start + workout.duration > cal.getTimeInMillis()) {
            workout = null;
            editInputLayoutTime.setError("Can't add entry in the future");
        }

        return workout;
    }
}
