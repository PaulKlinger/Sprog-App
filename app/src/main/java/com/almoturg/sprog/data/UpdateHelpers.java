package com.almoturg.sprog.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

import java.io.File;
import java.util.Calendar;
import java.util.TimeZone;

public class UpdateHelpers {
    // These are the times when an update should be available on the server
    private static final int FIRST_UPDATE_HOUR = 2;
    private static final int SECOND_UPDATE_HOUR = 14;
    private static final int MIN_HOURS_BETWEEN_UPDATES = 11; // some margin if it runs faster
    private static final int MAX_DAYS_BETWEEN_LOADING_POEMS = 3;

    // minimum length of poems.json file in bytes such that it is assumed to be complete
    // and therefore the cancel button is shown when updating
    private static final int MIN_FILE_LENGTH = 1000 * 1000;

    public static boolean isUpdateTime(Calendar now,
                                       long last_update_tstamp, long last_fcm_tstamp) {
        // Always load JSON when more than MAX_DAYS_BETWEEN_LOADING_POEMS days have passed
        // This is mainly to get updated scores/gold counts and to remove deleted poems.
        if (now.getTimeInMillis() - last_update_tstamp >
                MAX_DAYS_BETWEEN_LOADING_POEMS * 24 * 60 * 60 * 1000){
            return true;
        }

        // last_fcm_tstamp is the time when the last FCM message was sent
        // if it showed that updates were available this function would not have been called
        if (now.getTimeInMillis() - last_fcm_tstamp < MIN_HOURS_BETWEEN_UPDATES * 60 * 60 * 1000){
            return false;
        }

        Calendar last_update_cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        last_update_cal.setTimeInMillis(last_update_tstamp);
        long diff_in_ms = now.getTimeInMillis() - last_update_tstamp;
        long ms_today = now.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000
                + now.get(Calendar.MINUTE) * 60 * 1000
                + now.get(Calendar.SECOND) * 1000
                + now.get(Calendar.MILLISECOND);

        return (now.get(Calendar.HOUR_OF_DAY) >= FIRST_UPDATE_HOUR
                && diff_in_ms > ms_today - FIRST_UPDATE_HOUR * 60 * 60 * 1000)
                ||
                (now.get(Calendar.HOUR_OF_DAY) >= SECOND_UPDATE_HOUR
                        && diff_in_ms > ms_today - SECOND_UPDATE_HOUR * 60 * 60 * 1000);
    }

    public static boolean poemsFileExists(Context context) {
        File file = new File(context.getExternalFilesDir(
                Environment.DIRECTORY_DOWNLOADS), "poems.json");
        File old_file = new File(context.getExternalFilesDir(
                Environment.DIRECTORY_DOWNLOADS), "poems_old.json");
        return (file.exists() && file.length() > MIN_FILE_LENGTH) ||
                (old_file.exists() && old_file.length() > MIN_FILE_LENGTH);
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnected();
    }
}
