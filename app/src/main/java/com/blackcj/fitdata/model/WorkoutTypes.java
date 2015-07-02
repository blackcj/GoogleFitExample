package com.blackcj.fitdata.model;

import com.blackcj.fitdata.R;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;

/**
 * Created by chris.black on 5/1/15.
 *
 * https://developers.google.com/fit/rest/v1/reference/activity-types
 */
public enum WorkoutTypes {
    STEP_COUNT(-2),
    TIME(-1),
    IN_VEHICLE(0),
    BIKING(1),
    STILL(3),
    UNKNOWN(4),
    WALKING(7), // We are technically using walking for total estimated "steps"
    RUNNING(8),
    AEROBICS(9),
    GOLF(32),
    KAYAKING(40),
    STRENGTH_TRAINING(80),
    WEIGHT_LIFTING(97);

    private final int value;

    WorkoutTypes(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static String getActivityTextById(int id) {
        String result = "Unknown";
        switch (id) {
            case 1:
                result = FitnessActivities.BIKING;
                break;
            case 7:
                result = FitnessActivities.WALKING;
                break;
            case 8:
                result = FitnessActivities.RUNNING;
                break;
            case 97:
                result = FitnessActivities.WEIGHTLIFTING;
                break;
            case 9:
                result = FitnessActivities.AEROBICS;
                break;
            case 3:
                result = FitnessActivities.STILL;
                break;
            case 32:
                result = FitnessActivities.GOLF;
                break;
            case 40:
                result = FitnessActivities.KAYAKING;
                break;
            case 80:
                result = FitnessActivities.STRENGTH_TRAINING;
                break;
            case 4:
                result = FitnessActivities.UNKNOWN;
                break;
            case 0:
                result = FitnessActivities.IN_VEHICLE;
                break;
            default:
                result = "ID: " + id + " not defined";
                break;
        }
        return result;
    }

    public static String getWorkOutTextById(int id) {
        String result = "Unknown";
        switch (id) {
            case -2:
                result = "Step Count";
                break;
            case -1:
                result = "Time";
                break;
            case 0:
                result = "In vehicle";
                break;
            case 1:
                result = "Biking";
                break;
            case 4:
                result = "Unknown";
                break;
            case 7:
                result = "Walking";
                break;
            case 8:
                result = "Running";
                break;
            case 9:
                result = "Aerobics";
                break;
            case 3:
                result = "Still";
                break;
            case 32:
                result = "Golf";
                break;
            case 40:
                result = "Kayaking";
                break;
            case 80:
                result = "Strength training";
                break;
            case 97:
                result = "Weightlifting";
                break;
            default:
                result = "ID: " + id + " not defined";
                break;
        }
        return result;
    }

    public static final Integer[] mIcons = new Integer[]{
            R.drawable.heart_icon_red,
            R.drawable.trends_icon,
            R.drawable.shoeprints_icon_color,
            R.drawable.biker_icon_color,
            R.drawable.car_icon_color,
            R.drawable.running_icon_color
    };

    public static int getImageById(int id) {
        int result = R.drawable.heart_icon_gray;
        switch (id) {
            case -2:
                result = R.drawable.shoeprints_icon_color;
                break;
            case -1:
                result = R.drawable.calendar_icon;
                break;
            case 0:
                result = R.drawable.car_icon_color;
                break;
            case 1:
                result = R.drawable.biker_icon_color;
                break;
            case 8:
                result = R.drawable.running_icon_color;
                break;
            case 7:
                result = R.drawable.walk_icon_color;
                break;
            case 32:
                result = R.drawable.flag_icon;
                break;
            case 40:
                result = R.drawable.paddle_icon_color;
                break;
            case 80:
                result = R.drawable.weights_icon_color;
                break;
            case 97:
                result = R.drawable.weights_icon_color;
                break;
        }
        return result;
    }
}
