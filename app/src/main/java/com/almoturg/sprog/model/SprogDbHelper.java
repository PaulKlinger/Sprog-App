package com.almoturg.sprog.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashSet;

public class SprogDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "Sprog.db";
    private static final String READ_TABLE = "read_poems";
    private static final String FAVORITES_TABLE = "favorite_poems";

    private static final String SQL_CREATE_READ_TABLE =
            "CREATE TABLE " + READ_TABLE + " (link TEXT PRIMARY KEY NOT NULL ON CONFLICT IGNORE);";
    private static final String SQL_CREATE_FAVORITES_TABLE =
            "CREATE TABLE " + FAVORITES_TABLE + " (link TEXT PRIMARY KEY NOT NULL);";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + READ_TABLE + ";" +
                    "DROP TABLE IF EXISTS " + FAVORITES_TABLE + ";";

    private static final String SQL_CLEAR_READ_POEMS =
            "DELETE FROM " + READ_TABLE;

    public SprogDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_READ_TABLE);
        db.execSQL(SQL_CREATE_FAVORITES_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion == 2) {
            db.execSQL(SQL_CREATE_FAVORITES_TABLE);
        }
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public HashSet<String> getReadPoems() {
        SQLiteDatabase db = this.getReadableDatabase();
        HashSet<String> read_poems = new HashSet<>();
        try (Cursor cur = db.rawQuery("SELECT link from " + READ_TABLE, null)) {
            while (cur.moveToNext()) {
                String link = cur.getString(cur.getColumnIndexOrThrow("link"));
                read_poems.add(link);
            }
        }
        return read_poems;
    }

    public void addReadPoems(ArrayList<String> new_read_poems) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();

            ContentValues values;
            for (String link : new_read_poems) {
                values = new ContentValues();
                values.put("link", link);
                db.insert(READ_TABLE, null, values);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void clearReadPoems() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(SQL_CLEAR_READ_POEMS);
    }

    public HashSet<String> getFavoritePoems() {
        SQLiteDatabase db = this.getReadableDatabase();
        HashSet<String> favorite_poems = new HashSet<>();
        try (Cursor cur = db.rawQuery("SELECT link from " + FAVORITES_TABLE, null)) {
            while (cur.moveToNext()) {
                String link = cur.getString(cur.getColumnIndexOrThrow("link"));
                favorite_poems.add(link);
            }
        }
        return favorite_poems;
    }

    public void addFavoritePoem(String link) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();

            ContentValues values = new ContentValues();
            values.put("link", link);
            db.insert(FAVORITES_TABLE, null, values);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void removeFavoritePoem(String link) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();

            db.delete(FAVORITES_TABLE, "link=?", new String[]{link});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
