package com.blackcj.fitdata.database;

import com.blackcj.fitdata.model.Workout;
import com.blackcj.fitdata.model.WorkoutReport;
import com.blackcj.fitdata.model.WorkoutTypes;

/**
 * Created by Chris Black
 *
 * Mock data that can be used for debugging
 */
public class MockData {

    public static WorkoutReport getDailyMockData() {
        WorkoutReport workoutReport = new WorkoutReport();

        // Mock data
        Workout workout = new Workout();
        workout.type = WorkoutTypes.WALKING.getValue();
        workout.stepCount = 8953;
        workout.duration = 3654304;

        workoutReport.addWorkoutData(workout);

        Workout workout2 = new Workout();
        workout2.type = WorkoutTypes.BIKING.getValue();
        workout2.duration = 2654304;
        workoutReport.addWorkoutData(workout2);
        return workoutReport;
    }

    public static WorkoutReport getWeeklyMockData() {
        WorkoutReport workoutReport = new WorkoutReport();

        // Mock data
        Workout workout = new Workout();
        workout.type = WorkoutTypes.WALKING.getValue();
        workout.stepCount = 8953;
        workout.duration = 3654304 * 5;

        workoutReport.addWorkoutData(workout);

        Workout workout2 = new Workout();
        workout2.type = WorkoutTypes.BIKING.getValue();
        workout2.duration = 2654304 * 2;
        workoutReport.addWorkoutData(workout2);

        Workout workout3 = new Workout();
        workout3.type = WorkoutTypes.RUNNING.getValue();
        workout3.duration = 2654304;
        workoutReport.addWorkoutData(workout3);

        Workout workout4 = new Workout();
        workout4.type = WorkoutTypes.KAYAKING.getValue();
        workout4.duration = 4654304;
        workoutReport.addWorkoutData(workout4);

        return workoutReport;
    }

    public static WorkoutReport getMonthlyMockData() {
        WorkoutReport workoutReport = new WorkoutReport();

        // Mock data
        Workout workout = new Workout();
        workout.type = WorkoutTypes.WALKING.getValue();
        workout.stepCount = 8953;
        workout.duration = 3654304 * 28;
        workoutReport.addWorkoutData(workout);

        Workout workout6 = new Workout();
        workout6.type = WorkoutTypes.AEROBICS.getValue();
        workout6.duration = 4654304 * 5;
        workoutReport.addWorkoutData(workout6);

        Workout workout5 = new Workout();
        workout5.type = WorkoutTypes.STRENGTH_TRAINING.getValue();
        workout5.duration = 4654304 * 2;
        workoutReport.addWorkoutData(workout5);

        Workout workout2 = new Workout();
        workout2.type = WorkoutTypes.BIKING.getValue();
        workout2.duration = 2654304 * 8;
        workoutReport.addWorkoutData(workout2);

        Workout workout3 = new Workout();
        workout3.type = WorkoutTypes.RUNNING.getValue();
        workout3.duration = 2654304 * 8;
        workoutReport.addWorkoutData(workout3);

        Workout workout4 = new Workout();
        workout4.type = WorkoutTypes.KAYAKING.getValue();
        workout4.duration = 4654304 * 3;
        workoutReport.addWorkoutData(workout4);

        return workoutReport;
    }
}
