package com.google.android.gms.fit.samples.basichistoryapi;

/**
 * Created by chris.black on 5/1/15.
 *
 * https://developers.google.com/fit/rest/v1/reference/activity-types
 */
public class WorkoutTypes {
    public static String getWorkOutById(int id) {
        String result = "Unknown";
        switch (id) {
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
}
