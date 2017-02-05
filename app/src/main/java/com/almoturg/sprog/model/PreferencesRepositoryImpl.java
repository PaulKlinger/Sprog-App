package com.almoturg.sprog.model;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferencesRepositoryImpl implements PreferencesRepository {

    public static final String PREF_NOTIFY_NEW = "NOTIFY_NEW";
    public static final String PREF_DISPLAYED_NOTIFICATION_DIALOG = "DISPLAYED_NOTIFICATION_DIALOG";
    public static final String PREF_MARK_READ = "PREF_MARK_READ";
    public static final String PREF_LAST_UPDATE_TIME = "LAST_UPDATE_TIME";
    public static final String PREF_LAST_POEM_TIME = "LAST_POEM_TIME";
    public static final String PREF_UPDATE_NEXT = "UPDATE_NEXT";
    public static final String PREF_LAST_FCM_TSTAMP = "LAST_FCM_TSTAMP";

    public SharedPreferences prefs;

    public PreferencesRepositoryImpl(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    private void setValue(String id, long newValue) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(id, newValue);
        editor.apply();
    }

    private void setValue(String id, boolean newValue) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(id, newValue);
        editor.apply();
    }

    private void setValue(String id, int newValue) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(id, newValue);
        editor.apply();
    }


    @Override
    public boolean getNotifyNew() {
        return prefs.getBoolean(PREF_NOTIFY_NEW, false);
    }

    @Override
    public void setNotifyNew(boolean newValue) {
        setValue(PREF_NOTIFY_NEW, newValue);
    }

    @Override
    public int getDisplayedNotificationDialog() {
        return prefs.getInt(PREF_DISPLAYED_NOTIFICATION_DIALOG, -1);
    }

    @Override
    public void setDisplayedNotificationDialog(int newValue) {
        setValue(PREF_DISPLAYED_NOTIFICATION_DIALOG, newValue);
    }

    @Override
    public boolean getMarkRead() {
        return prefs.getBoolean(PREF_MARK_READ, true);
    }

    @Override
    public void setMarkRead(boolean newValue) {
        setValue(PREF_MARK_READ, newValue);
    }

    @Override
    public long getLastUpdateTime() {
        return prefs.getLong(PREF_LAST_UPDATE_TIME, -1);
    }

    @Override
    public void setLastUpdateTime(long newValue) {
        setValue(PREF_LAST_UPDATE_TIME, newValue);
    }

    @Override
    public long getLastPoemTime() {
        return prefs.getLong(PREF_LAST_POEM_TIME, Long.MAX_VALUE);
    }

    @Override
    public void setLastPoemTime(long newValue) {
        setValue(PREF_LAST_POEM_TIME, newValue);
    }

    @Override
    public boolean getUpdateNext() {
        return prefs.getBoolean(PREF_UPDATE_NEXT, false);
    }

    @Override
    public void setUpdateNext(boolean newValue) {
        setValue(PREF_UPDATE_NEXT, newValue);
    }

    @Override
    public long getLastFCMTimestamp() {
        return prefs.getLong(PREF_LAST_FCM_TSTAMP, -1);
    }

    @Override
    public void setLastFCMTimestamp(long newValue) {
        setValue(PREF_LAST_FCM_TSTAMP, newValue);
    }
}
