package com.google.android.gms.fit.samples.basichistoryapi.observable;


import com.google.android.gms.fit.samples.basichistoryapi.model.WorkoutReport;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by chris.black on 5/4/15.
 *
 * http://blog.danlew.net/2014/09/15/grokking-rxjava-part-1/
 */
public class WorkoutObservable {
    public static Observable<WorkoutReport> downloadFileObservable() {
        return Observable.create(
                new Observable.OnSubscribe<WorkoutReport>() {
                    @Override
                    public void call(Subscriber<? super WorkoutReport> sub) {
                        WorkoutReport report = new WorkoutReport();
                        sub.onNext(report);
                        sub.onCompleted();
                    }
                }
        );
    }
}
