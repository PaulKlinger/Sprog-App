package com.almoturg.sprog.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.CardView;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;

import com.almoturg.sprog.R;
import com.almoturg.sprog.data.MarkdownConverter;
import com.almoturg.sprog.model.Poem;
import com.almoturg.sprog.model.PreferencesRepository;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class Util {
    private static Calendar cal = null;
    // These are the times when an update should be available on the server
    private static final int FIRST_UPDATE_HOUR = 2;
    private static final int SECOND_UPDATE_HOUR = 14;
    private static final int MIN_HOURS_BETWEEN_UPDATES = 11; // some margin if it runs faster
    private static final int MAX_DAYS_BETWEEN_LOADING_POEMS = 3;

    // minimum length of poems.json file in bytes such that it is assumed to be complete
    // and therefore the cancel button is shown when updating
    private static final int MIN_FILE_LENGTH = 1000 * 1000;

    public static final int VIEWFLIPPER_RECYCLERVIEW = 0;
    public static final int VIEWFLIPPER_EMPTY_FAVORITES = 1;
    public static final int VIEWFLIPPER_UPDATING = 2;
    public static final int VIEWFLIPPER_ERROR = 3;

    public static final int NEW_POEMS_NOTIFICATION_ID = 1;

    public static void update_poem_row_poem_page(Poem poem, View poem_row,
                                                 MarkdownConverter markdownConverter,
                                                 Context context){
        update_poem_row(poem, poem_row, true, false, false, false, markdownConverter, context);
    }

    public static void update_poem_row_mainlist(Poem poem, View poem_row,
                                                boolean show_only_favorites, boolean mark_read,
                                                MarkdownConverter markdownConverter,
                                                Context context){
        update_poem_row(poem, poem_row, false, true, show_only_favorites, mark_read,
                markdownConverter, context);
    }

    private static void update_poem_row(Poem poem, View poem_row, boolean border,
                                        boolean main_list, boolean show_only_favorites,
                                        boolean mark_read, MarkdownConverter markdownConverter,
                                        Context context) {
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
            if (poem.read && mark_read && !show_only_favorites) {
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
                    .setText(markdownConverter.convertPoemMarkdown(poem.content, poem.timestamp));
            poem_row.findViewById(R.id.author).setVisibility(View.VISIBLE);
        }
        ((TextView) poem_row.findViewById(R.id.gold_count))
                .setText(" × " + Long.toString(poem.gold));
        if (poem.gold > 0) {
            poem_row.findViewById(R.id.gold_display).setVisibility(View.VISIBLE);
        } else {
            poem_row.findViewById(R.id.gold_display).setVisibility(View.INVISIBLE);
        }
        if (poem.favorite){
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

    public static boolean timeToShowNotifyDialog(PreferencesRepository prefs){
        // This checks whether it's time to show the dialog asking whether to enable
        // notifications for new poems.
        // The state is stored in the "DISPLAY_NOTIFICATION_DIALOG" pref
        // not set: never launched, 0: launched once, 1: dialog shown
        if (prefs.getDisplayedNotificationDialog() == -1){
            prefs.setDisplayedNotificationDialog(0);
            return false;
        } else if (prefs.getDisplayedNotificationDialog() == 0) {
            prefs.setDisplayedNotificationDialog(1);
            return true;
        } else {
            return false;
        }
    }

    public static boolean poemsFileExists(Context context) {
        File file = new File(context.getExternalFilesDir(
                Environment.DIRECTORY_DOWNLOADS), "poems.json");
        File old_file = new File(context.getExternalFilesDir(
                Environment.DIRECTORY_DOWNLOADS), "poems_old.json");
        return (file.exists() && file.length() > MIN_FILE_LENGTH) ||
                (old_file.exists() && old_file.length() > MIN_FILE_LENGTH);
    }
}
