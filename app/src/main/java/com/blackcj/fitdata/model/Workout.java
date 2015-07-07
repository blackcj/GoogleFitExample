package com.blackcj.fitdata.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.blackcj.fitdata.Utilities;

import java.io.Serializable;

/**
 * Created by chris.black on 5/1/15.
 */
public class Workout implements Comparable<Workout>, Parcelable {

    public long _id;            // same as start
    public long duration = 0;   // length of activity
    public long start = 0;      // activity start time
    public int type;            // type of activity
    public int stepCount = 0;   // number of steps for activity


    public Workout() {
        _id = 0L;
        duration = 0L;
        start = 0L;
        type = 0;
        stepCount = 0;
    }

    public Workout(Parcel in) {
        _id = in.readLong();
        duration = in.readLong();
        start = in.readLong();
        type = in.readInt();
        stepCount = in.readInt();
    }

    public boolean overlaps(Workout another) {
        boolean result = false;

        if (another.start > start && another.start < start + duration) {
            result = true;
        }

        if (start > another.start && start < another.start + another.duration) {
            result = true;
        }

        return result;
    }

    @Override
    public int compareTo(Workout another) {
        int result = 0;
        Long obj1 = this.start;
        Long obj2 = another.start;

        // Summary is always first, walking is always second
        if(this.start == -1) {
            result = -1;
        } else if(another.start == -1) {
            result = 1;
        } else if (this.type == another.type) {
            result = obj1.compareTo(obj2);
        } else if(this.type == WorkoutTypes.STEP_COUNT.getValue()) {
            result = -1;
        } else if(another.type == WorkoutTypes.STEP_COUNT.getValue()) {
            result = 1;
        }else {
            result = obj1.compareTo(obj2);
        }
        return result;
    }

    @Override
    public String toString() {
        return "You went " + WorkoutTypes.getWorkOutTextById(type) +
                " on " + Utilities.getDateString(start) +
                " at " + Utilities.getTimeString(start) +
                " for " + WorkoutReport.getDurationBreakdown(duration) +
                " with " + stepCount + " steps";
    }

    public String removeText() {
        return "Removed: " + WorkoutTypes.getWorkOutTextById(type) +
                " on " + Utilities.getDayString(start) +
                " for " + WorkoutReport.getDurationBreakdown(duration);
    }

    public String shortText() {
        return "" + Utilities.getDayString(start) +
                " at " + Utilities.getTimeString(start) +
                " for " + WorkoutReport.getDurationBreakdown(duration);
    }

    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(_id);
        dest.writeLong(duration);
        dest.writeLong(start);
        dest.writeInt(type);
        dest.writeInt(stepCount);
    }

    public static final Parcelable.Creator<Workout> CREATOR = new Parcelable.Creator<Workout>()
    {
        public Workout createFromParcel(Parcel in)
        {
            return new Workout(in);
        }
        public Workout[] newArray(int size)
        {
            return new Workout[size];
        }
    };
}
