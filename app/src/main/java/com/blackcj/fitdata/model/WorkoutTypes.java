package com.blackcj.fitdata.model;

import com.blackcj.fitdata.R;
import com.google.android.gms.fitness.FitnessActivities;

import java.util.ArrayList;

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
    ON_FOOT(2),
    STILL(3),
    UNKNOWN(4),
    WALKING(7),
    RUNNING(8),
    AEROBICS(9),
    GARDENING(31),
    GOLF(32),
    KAYAKING(40),
    ROCK_CLIMBING(52),
    CROSS_COUNTRY_SKIING(67),
    SLEEP(72),
    SNOWBOARDING(73),
    STRENGTH_TRAINING(80),
    TENNIS(87),
    WEIGHT_LIFTING(97);

    private final int value;

    WorkoutTypes(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static boolean isActiveWorkout(int id) {
        boolean result = true;
        switch (id) {
            case 0:
            case 3:
            case 4:
            case 72:
                result = false;
                break;
        }
        return result;
    }

    public static String getNonActivQuery() {
        return "";
    }

    public static String getActivityTextById(int id) {
        String result = "Unknown";
        switch (id) {
            case 0:
                result = FitnessActivities.IN_VEHICLE;
                break;
            case 1:
                result = FitnessActivities.BIKING;
                break;
            case 2:
                result = FitnessActivities.ON_FOOT;
                break;
            case 3:
                result = FitnessActivities.STILL;
                break;
            case 4:
                result = FitnessActivities.UNKNOWN;
                break;
            case 7:
                result = FitnessActivities.WALKING;
                break;
            case 8:
                result = FitnessActivities.RUNNING;
                break;
            case 9:
                result = FitnessActivities.AEROBICS;
                break;
            case 31:
                result = FitnessActivities.GARDENING;
                break;
            case 32:
                result = FitnessActivities.GOLF;
                break;
            case 40:
                result = FitnessActivities.KAYAKING;
                break;
            case 52:
                result = FitnessActivities.ROCK_CLIMBING;
                break;
            case 67:
                result = FitnessActivities.SKIING_CROSS_COUNTRY;
                break;
            case 72:
                result = FitnessActivities.SLEEP;
                break;
            case 73:
                result = FitnessActivities.SNOWBOARDING;
                break;
            case 80:
                result = FitnessActivities.STRENGTH_TRAINING;
                break;
            case 87:
                result = FitnessActivities.TENNIS;
                break;
            case 97:
                result = FitnessActivities.WEIGHTLIFTING;
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
            case 31:
                result = "Gardening";
                break;
            case 32:
                result = "Golf";
                break;
            case 40:
                result = "Kayaking";
                break;
            case 52:
                result = "Rock Climbing";
                break;
            case 67:
                result = "Cross Country Skiing";
                break;
            case 72:
                result = "Sleeping";
                break;
            case 73:
                result = "Snowboarding";
                break;
            case 80:
                result = "Strength training";
                break;
            case 87:
                result = "Tennis";
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

    public static int getColorById(int id) {
        int result = R.color.other;
        switch (id) {
            case 1:
                result = R.color.biking;
                break;
            case 8:
                result = R.color.running;
                break;
            case 7:
                result = R.color.walking;
                break;
            case 32:
                result = R.color.golfing;
                break;
        }
        return result;
    }

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
