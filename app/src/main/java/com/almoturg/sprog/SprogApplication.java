package com.almoturg.sprog;

import android.app.Application;
import android.content.Context;

import com.almoturg.sprog.model.PreferencesRepository;
import com.almoturg.sprog.model.PreferencesRepositoryImpl;
import com.almoturg.sprog.model.SprogDbHelper;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;


public class SprogApplication extends Application {
    private Tracker mTracker;
    private PreferencesRepository preferences;

    private static SprogDbHelper sprogDbHelper;


    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(R.xml.app_tracker);
        }
        return mTracker;
    }

    synchronized public static SprogDbHelper getDbHelper(Context context) {
        if (sprogDbHelper == null) {
            sprogDbHelper = new SprogDbHelper(context);
        }
        return sprogDbHelper;
    }

    synchronized public  PreferencesRepository getPreferences() {
        if (preferences == null){
            preferences = new PreferencesRepositoryImpl(getApplicationContext());
        }
        return preferences;
    }
}