package com.blackcj.fitdata.database;

import java.util.Map;
import java.util.WeakHashMap;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * MultiThreadSQLiteOpenHelper:<br>
 * enhanced SQLiteOpenHelper for android applications where several threads might access and close the same database<br>
 * <p>
 * With SQLiteOpenHelper, if one thread is closing database, then other threads will crash while accessing a closed database.<br>
 * With MultiThreadSQLiteOpenHelper, a thread does not close the database anymore, but asks for a close with the closeIfNeeded method.
 * It then verifies that each thread asked for closing before really closing the database.
 *
 * @author d4rxh4wx
 *
 */
public abstract class MultiThreadSQLiteOpenHelper extends SQLiteOpenHelper {

    private final static String TAG = "MULTI-THREAD-DB-HELPER";

    // Tells for each thread if it requires that database should be opened or not opened (closed)
    // Using WeakHashMap so that thread can be released by GC when needed (no strong references on threads)
    private WeakHashMap<Thread, Boolean> states = new WeakHashMap<>();

    public MultiThreadSQLiteOpenHelper(Context context, String name,
                                       CursorFactory factory, int version) {
        super(context, name, factory, version);
        Log.d(TAG, "database helper built");
    }

    public SQLiteDatabase getWritableDatabase() {
        // synchronized because it may be accessible by multi threads (all dbHelper methods are synchronized)
        // and synchronized on the object because open/close are related on each other
        synchronized(this) {
            Thread currentThread = Thread.currentThread();

            states.put(currentThread, true);  // this thread requires that this database should be opened

            Log.d(TAG, "getting database");

            return super.getWritableDatabase();
        }
    }

    /**
     * Close database if all threads don't need the database anymore
     * @return true if closed, false otherwise
     */
    public boolean closeIfNeeded() {
        // synchronized because it may be accessible by multi threads (all dbHelper methods are synchronized)
        // and synchronized on the object because open/close are related on each other
        synchronized(this) {
            Thread currentThread = Thread.currentThread();

            Log.d(TAG, "requesting closing");

            states.put(currentThread, false); // this thread requires that this database should be closed

            boolean mustBeClosed = true;

            // if all threads asked for closing database, then close it
            Boolean opened = null;
            Thread thread = null;
            for (Map.Entry<Thread, Boolean> entry : states.entrySet()) {
                thread = entry.getKey();
                opened = entry.getValue();
                if (thread != null && opened != null) {
                    Log.d(TAG, String.format("Thread [%s] requires database must be %s", thread.getId(), opened.booleanValue() ? "OPENED" : "CLOSED"));
                    if (opened.booleanValue()) {
                        // one thread still requires that database should be opened
                        mustBeClosed = false;
                    }
                }
            }

            Log.d(TAG, mustBeClosed ? "database must be closed" : "database still needs to be opened");

            if (mustBeClosed) {
                super.close();
                Log.d(TAG, "database is closed");
            }

            return mustBeClosed;
        }
    }
}
