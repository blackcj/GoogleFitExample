package com.blackcj.fitdata.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by chris.black on 5/1/15.
 *
 * Data model for the workout reports.
 */
public class WorkoutReport {
    private Map<Integer, Workout> map =  new HashMap<>();


    public void addWorkoutData(Workout workout) {

        if(workout.type == WorkoutTypes.STILL.getValue() || (workout.stepCount == 0 && workout.duration < 60000)) {
            // Ignore "still" time or workouts less than 1 minute.
            return;
        }

        if(map.get(workout.type) == null) {
            map.put(workout.type, workout);
        }else {
            Workout w = map.get(workout.type);
            w.stepCount += workout.stepCount;
            w.duration += workout.duration;
        }
    }

    public void clearWorkoutData() {
        map.clear();
    }

    public ArrayList<Workout> getWorkoutData() {
        Workout summary = new Workout();
        summary.type = WorkoutTypes.TIME.getValue();
        summary.duration = getTotalDuration();
        summary.start = -1;
        replaceWorkout(summary);
        ArrayList<Workout> result = new ArrayList<>(map.values());
        Collections.sort(result);
        return result;
    }

    public void replaceWorkout(Workout workout) {
        if(map.get(workout.type) == null) {
            map.put(workout.type, workout);
        }else {
            Workout w = map.get(workout.type);
            w = workout;
        }
    }

    public long getTotalDuration() {
        long totalDuration = 0;
        Set keys = map.keySet();
        for (Object key1 : keys) {
            int key = (int) key1;
            Workout workout = map.get(key);
            if (workout.type != WorkoutTypes.TIME.getValue() && workout.type != WorkoutTypes.STILL.getValue()
                    && workout.type != WorkoutTypes.IN_VEHICLE.getValue()) {
                totalDuration += workout.duration;
            }
        }
        return totalDuration;
    }

    /**
     * Special case for steps. Maybe track estimated steps separately from walking?
     * @param workout
     */
    public void setStepData(Workout workout) {
        if(map.get(workout.type) == null) {
            map.put(workout.type, workout);
        }else {
            Workout w = map.get(workout.type);
            w.start = 0; // TODO: Remove this when we have step summary cached
            w.stepCount = workout.stepCount;
        }
    }

    public Workout getWorkoutByType(int type) {
        return map.get(type);
    }

    public String toString() {
        String result = "";
        Set keys = map.keySet();
        for (Object key1 : keys) {
            int key = (int) key1;
            Workout value = map.get(key);
            result += WorkoutTypes.getWorkOutTextById(value.type) + " steps: " + value.stepCount + "\n";
            result += WorkoutTypes.getWorkOutTextById(value.type) + " duration: " + getDurationBreakdown(value.duration) + "\n";
        }
        return result;
    }

    private double miliToMinutes(long mili) {
        return Math.floor(mili / 1000 / 60);
    }

    /**
     * Convert a millisecond duration to a string format
     *
     * @param millis A duration to convert to a string form
     * @return A string of the form "X Days Y Hours Z Minutes A Seconds".
     */
    public static String getDurationBreakdown(long millis)
    {
        if(millis < 0)
        {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder(64);
        if(days > 0) {
            sb.append(days);
            sb.append(" days ");
        }
        if(hours > 0) {
            sb.append(hours);
            sb.append(" hrs ");
        }
        sb.append(minutes);
        sb.append(" mins ");

        return(sb.toString());
    }
}
