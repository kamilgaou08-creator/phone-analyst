package com.phoneanalyst.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class EventStore extends SQLiteOpenHelper {
    private static final String DB_NAME = "phone_analyst.db";
    public EventStore(Context c) { super(c, DB_NAME, null, 1); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE events (_id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, package_name TEXT, event_type TEXT)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

    public synchronized void addEvent(long ts, String pkg, String type) {
        ContentValues v = new ContentValues();
        v.put("ts", ts); v.put("package_name", pkg); v.put("event_type", type);
        getWritableDatabase().insert("events", null, v);
    }

    public synchronized int countSince(long since) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM events WHERE ts>=?", new String[]{String.valueOf(since)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public synchronized List<String> topAppsSince(long since) {
        List<String> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT package_name, COUNT(*) n FROM events WHERE ts>=? AND package_name IS NOT NULL " +
                "GROUP BY package_name ORDER BY n DESC LIMIT 5", new String[]{String.valueOf(since)})) {
            while (c.moveToNext()) out.add(c.getString(0) + " — " + c.getInt(1) + " événements");
        }
        return out;
    }
}
