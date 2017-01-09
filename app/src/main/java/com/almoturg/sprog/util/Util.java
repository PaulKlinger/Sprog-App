package com.almoturg.sprog.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.CardView;
import android.text.format.DateFormat;
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
    private static int FIRST_UPDATE_HOUR = 2;
    private static int SECOND_UPDATE_HOUR = 14;

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
            if (poem.read && ((MainActivity) context).prefs.getBoolean("MARK_READ_ENABLED", true)) {
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

    public static boolean isUpdateTime(long last_update_tstamp) {
        Calendar last_update_cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

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
}
