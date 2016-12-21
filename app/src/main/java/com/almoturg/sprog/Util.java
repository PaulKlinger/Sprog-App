package com.almoturg.sprog;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

import in.uncod.android.bypass.Bypass;

/**
 * Created by Paul on 2016-12-18.
 */

public final class Util {
    private static Calendar cal = null;

    public static void update_poem_row(Poem poem, View poem_row, boolean border,
                                       boolean only_first_line, Context context) {
        if (cal == null) {
            cal = Calendar.getInstance(Locale.ENGLISH);
        }

        if (border) {
            poem_row.findViewById(R.id.container).setBackgroundResource(R.drawable.card_border);
            // setBackgroundResource removes padding...
            int card_padding = context.getResources().getDimensionPixelSize(R.dimen.card_padding);
            poem_row.findViewById(R.id.container).setPadding(card_padding, card_padding, card_padding, card_padding);
        }

        if (false && only_first_line){
            ((TextView) poem_row.findViewById(R.id.content)).setText(poem.first_line + "...");
        }
        ((TextView) poem_row.findViewById(R.id.content)).setText(poem.content);
        ((TextView) poem_row.findViewById(R.id.gold_count)).setText(" × " + Long.toString(poem.gold));
        if (poem.gold > 0) {
            poem_row.findViewById(R.id.gold_display).setVisibility(View.VISIBLE);
        } else {
            poem_row.findViewById(R.id.gold_display).setVisibility(View.INVISIBLE);
        }
        ((TextView) poem_row.findViewById(R.id.score)).setText(Long.toString(poem.score));

        cal.setTimeInMillis((long) poem.timestamp * 1000);
        ((TextView) poem_row.findViewById(R.id.datetime)).setText(DateFormat.format("yyyy-MM-dd HH:mm:ss", cal).toString());
    }

    public static <T> T last(T[] array) {
        return array[array.length - 1];
    }
}
