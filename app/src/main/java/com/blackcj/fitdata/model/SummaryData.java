package com.blackcj.fitdata.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Chris Black
 *
 * Model object used to display fitness summary data.
 */
public class SummaryData implements Parcelable {

    public int activityType;
    public long averageDailyData;
    public long todayData;
    public long averageWeeklyData;
    public long weekData;

    public SummaryData() {
        activityType = 0;
        averageDailyData = 0L;
        todayData = 0L;
        averageWeeklyData = 0L;
        weekData = 0L;
    }

    public SummaryData(Parcel in) {
        activityType = in.readInt();
        averageDailyData = in.readLong();
        todayData = in.readLong();
        averageWeeklyData = in.readLong();
        weekData = in.readLong();
    }

    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(activityType);
        dest.writeLong(averageDailyData);
        dest.writeLong(todayData);
        dest.writeLong(averageWeeklyData);
        dest.writeLong(weekData);
    }

    public static final Parcelable.Creator<SummaryData> CREATOR = new Parcelable.Creator<SummaryData>()
    {
        public SummaryData createFromParcel(Parcel in)
        {
            return new SummaryData(in);
        }
        public SummaryData[] newArray(int size)
        {
            return new SummaryData[size];
        }
    };
}
