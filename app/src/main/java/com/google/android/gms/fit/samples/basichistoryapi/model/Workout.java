package com.google.android.gms.fit.samples.basichistoryapi.model;

/**
 * Created by chris.black on 5/1/15.
 */
public class Workout implements Comparable<Workout> {

    public long _id;            // same as start
    public long duration = 0;   // length of activity
    public long start = 0;      // activity start time
    public int type;            // type of activity
    public int stepCount = 0;   // number of steps for activity

    @Override
    public int compareTo(Workout another) {
        Long obj1 = new Long(this.start);
        Long obj2 = new Long(another.start);
        return obj1.compareTo(obj2);
    }
}
