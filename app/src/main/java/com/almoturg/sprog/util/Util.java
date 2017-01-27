package com.almoturg.sprog.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.CardView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.almoturg.sprog.R;
import com.almoturg.sprog.SprogApplication;
import com.almoturg.sprog.model.Poem;
import com.almoturg.sprog.ui.MainActivity;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import in.uncod.android.bypass.Bypass;


public final class Util {
    private static Calendar cal = null;
    // These are the times when an update should be available on the server
    private static final int FIRST_UPDATE_HOUR = 2;
    private static final int SECOND_UPDATE_HOUR = 14;
    private static final int MIN_HOURS_BETWEEN_UPDATES = 11; // some margin if it runs faster
    private static final int MAX_DAYS_BETWEEN_LOADING_POEMS = 3;

    public static final int NEW_POEMS_NOTIFICATION_ID = 1;
    public static final String PREF_NOTIFY_NEW = "NOTIFY_NEW";
    public static final String PREF_DISPLAYED_NOTIFICATION_DIALOG = "DISPLAYED_NOTIFICATION_DIALOG";
    public static final String PREF_MARK_READ = "PREF_MARK_READ";
    public static final String PREF_LAST_UPDATE_TIME = "LAST_UPDATE_TIME";
    public static final String PREF_LAST_POEM_TIME = "LAST_POEM_TIME";
    public static final String PREF_UPDATE_NEXT = "UPDATE_NEXT";
    public static final String PREF_LAST_FCM_TSTAMP = "LAST_FCM_TSTAMP";

    private static Bypass bypass;

    public static void update_poem_row(Poem poem, View poem_row, boolean border,
                                boolean main_list, Context context) {
        if (cal == null) {
            cal = Calendar.getInstance(Locale.ENGLISH);
        }

        if (border) {
            poem_row.findViewById(R.id.container).setBackgroundResource(R.drawable.card_border);
            // setBackgroundResource removes padding...
            int card_padding = context.getResources().getDimensionPixelSize(R.dimen.card_padding);
            poem_row.findViewById(R.id.container).setPadding(
                    card_padding, card_padding, card_padding, card_padding);
        }

        if (main_list) {
            poem_row.findViewById(R.id.first_line).setVisibility(View.VISIBLE);
            poem_row.findViewById(R.id.content_wrapper).setVisibility(View.GONE);
            ((TextView) poem_row.findViewById(R.id.first_line)).setText(poem.first_line);
            if (poem.read && ((MainActivity) context).prefs.getBoolean(Util.PREF_MARK_READ, true)
                    && !((MainActivity) context).show_only_favorites) {
                ((CardView) poem_row).setCardBackgroundColor(
                        ResourcesCompat.getColor(context.getResources(),
                                R.color.colorReadPoem, null));
            } else {
                ((CardView) poem_row).setCardBackgroundColor(
                        ResourcesCompat.getColor(context.getResources(),
                                R.color.colorUnReadPoem, null));
            }

        } else {
            poem_row.findViewById(R.id.first_line).setVisibility(View.GONE);
            poem_row.findViewById(R.id.content_wrapper).setVisibility(View.VISIBLE);
            ((TextView) poem_row.findViewById(R.id.content))
                    .setText(convertMarkdown(poem.content, context));
            poem_row.findViewById(R.id.author).setVisibility(View.VISIBLE);
        }
        ((TextView) poem_row.findViewById(R.id.gold_count))
                .setText(" × " + Long.toString(poem.gold));
        if (poem.gold > 0) {
            poem_row.findViewById(R.id.gold_display).setVisibility(View.VISIBLE);
        } else {
            poem_row.findViewById(R.id.gold_display).setVisibility(View.INVISIBLE);
        }
        if (poem.favorite && main_list){
            poem_row.findViewById(R.id.favorite_icon).setVisibility(View.VISIBLE);
        } else {
            poem_row.findViewById(R.id.favorite_icon).setVisibility(View.INVISIBLE);
        }

        ((TextView) poem_row.findViewById(R.id.score)).setText(Long.toString(poem.score));

        cal.setTimeInMillis((long) poem.timestamp * 1000);
        ((TextView) poem_row.findViewById(R.id.datetime)).setText(
                DateFormat.format("yyyy-MM-dd HH:mm:ss", cal).toString());
    }

    public static <T> T last(T[] array) {
        return array[array.length - 1];
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnected();
    }

    public static boolean isUpdateTime(long last_update_tstamp, long last_fcm_tstamp) {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
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

    public static CharSequence convertMarkdown(String markdown, Context context) {
        if (bypass == null) {
            synchronized (SprogApplication.bypassLock) {
                bypass = new Bypass(context);
            }
        }
        markdown = markdown.replaceAll("(?:^|[^(\\[])(https?://\\S*\\.\\S*)(?:\\s|$)", "[$1]($1)");

        CharSequence converted;
        synchronized (SprogApplication.bypassLock) {
            converted = bypass.markdownToSpannable(markdown);
        }
        return converted;
    }

    public static boolean timeToShowNotifyDialog(SharedPreferences prefs){
        // This checks whether it's time to show the dialog asking whether to enable
        // notifications for new poems.
        // The state is stored in the "DISPLAY_NOTIFICATION_DIALOG" pref
        // not set: never launched, 0: launched once, 1: dialog shown
        Log.d("SPROG", String.format("notify_pref: %d", prefs.getInt(Util.PREF_DISPLAYED_NOTIFICATION_DIALOG, -1)));
        if (prefs.getInt(Util.PREF_DISPLAYED_NOTIFICATION_DIALOG, -1) == -1){
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(Util.PREF_DISPLAYED_NOTIFICATION_DIALOG, 0);
            editor.apply();
            return false;
        } else if (prefs.getInt(Util.PREF_DISPLAYED_NOTIFICATION_DIALOG, -1) == 0) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(Util.PREF_DISPLAYED_NOTIFICATION_DIALOG, 1);
            editor.apply();
            return true;
        } else {
            return false;
        }
    }

}
