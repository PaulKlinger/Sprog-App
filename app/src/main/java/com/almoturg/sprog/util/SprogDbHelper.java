package com.almoturg.sprog.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class SprogDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Sprog.db";
    private static final String READ_TABLE = "read_poems";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + READ_TABLE + " (link TEXT PRIMARY KEY NOT NULL ON CONFLICT IGNORE)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + READ_TABLE;

    private static final String SQL_CLEAR_READ_POEMS =
            "DELETE FROM " + READ_TABLE;

    public SprogDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    ArrayList<String> getReadPoems() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cur = db.rawQuery("SELECT link from " + READ_TABLE, null);
        ArrayList<String> read_poems = new ArrayList<>();
        while (cur.moveToNext()) {
            String link = cur.getString(cur.getColumnIndexOrThrow("link"));
            read_poems.add(link);
        }
        cur.close();
        db.close();
        return read_poems;
    }

    public void addReadPoems(ArrayList<String> new_read_poems) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        ContentValues values;
        for (String link : new_read_poems) {
            values = new ContentValues();
            values.put("link", link);
            db.insert(READ_TABLE, null, values);
        }

        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    public void clearReadPoems() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(SQL_CLEAR_READ_POEMS);
    }
}
