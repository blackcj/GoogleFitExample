package com.blackcj.fitdata.database;

import com.blackcj.fitdata.model.Workout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by chris.black on 8/2/16.
 *
 * Data structure used to store write requests made to the database while
 * current opperations finish.
 */
public class DataRequestQueue {

    private List<WorkoutDataRequest> list = Collections.synchronizedList(new ArrayList());

    public boolean hasNext() {
        return list.size() > 0;
    }

    public void addRequest(WorkoutDataRequest wdr) {
        list.add(wdr);
    }

    public class WorkoutDataRequest {
        public String command;
        public Workout workout;
    }
}
