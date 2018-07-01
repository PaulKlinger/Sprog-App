package com.almoturg.sprog;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationManagerCompat;

import com.almoturg.sprog.model.PreferencesRepository;
import com.almoturg.sprog.model.PreferencesRepositoryImpl;
import com.almoturg.sprog.model.SprogDbHelper;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.messaging.FirebaseMessaging;


public class SprogApplication extends Application {
    private Tracker mTracker;
    private PreferencesRepository preferences;

    private static SprogDbHelper sprogDbHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        FirebaseMessaging.getInstance().subscribeToTopic("PoemUpdates");
        if (BuildConfig.DEBUG) {
            FirebaseMessaging.getInstance().subscribeToTopic("testPoemUpdates");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel poemChannel = new NotificationChannel("poem_channel",
                    "Sprog Poems", NotificationManager.IMPORTANCE_LOW);
            poemChannel.setDescription("Poem for your Sprog");
            poemChannel.enableLights(true);
            poemChannel.setLightColor(Color.GREEN);
            poemChannel.enableVibration(false);
            mNotificationManager.createNotificationChannel(poemChannel);
        }
    }

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

    synchronized public PreferencesRepository getPreferences() {
        if (preferences == null) {
            preferences = new PreferencesRepositoryImpl(getApplicationContext());
        }
        return preferences;
    }

    public void setTheme(Context context){
        PreferencesRepository preferences = getPreferences();
        if (preferences.getDarkTheme()){
            context.setTheme(R.style.AppThemeDark);
        } else {
            context.setTheme(R.style.AppTheme);
        }
    }
}