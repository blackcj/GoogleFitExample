package com.google.android.gms.fit.samples.basichistoryapi.model;

import com.google.android.gms.fit.samples.basichistoryapi.R;

/**
 * Created by chris.black on 5/1/15.
 *
 * https://developers.google.com/fit/rest/v1/reference/activity-types
 */
public enum WorkoutTypes {
    TIME(-1),
    IN_VEHICLE(0),
    BIKING(1),
    STILL(3),
    UNKNOWN(4),
    WALKING(7), // We are technically using walking for total estimated "steps"
    RUNNING(8),
    AEROBICS(9),
    STRENGTH_TRAINING(80),
    WEIGHT_LIFTING(97);

    private final int value;

    WorkoutTypes(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static String getWorkOutTextById(int id) {
        String result = "Unknown";
        switch (id) {
            case -1:
                result = "Time";
                break;
            case 1:
                result = "Biking";
                break;
            case 7:
                result = "Walking";
                break;
            case 8:
                result = "Running";
                break;
            case 97:
                result = "Weightlifting";
                break;
            case 9:
                result = "Aerobics";
                break;
            case 3:
                result = "Still";
                break;
            case 80:
                result = "Strength training";
                break;
            case 4:
                result = "Unknown";
                break;
            case 0:
                result = "In vehicle";
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
            case -1:
                result = R.drawable.calendar_icon;
                break;
            case 1:
                result = R.drawable.biker_icon_color;
                break;
            case 7:
                result = R.drawable.shoeprints_icon_color;
                break;
            case 8:
                result = R.drawable.running_icon_color;
                break;
            case 0:
                result = R.drawable.car_icon_color;
                break;
        }
        return result;
    }
}
