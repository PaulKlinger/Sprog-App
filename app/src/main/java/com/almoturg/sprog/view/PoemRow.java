package com.almoturg.sprog.view;

import android.content.Context;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.CardView;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;

import com.almoturg.sprog.R;
import com.almoturg.sprog.data.MarkdownConverter;
import com.almoturg.sprog.model.Poem;

import java.util.Calendar;
import java.util.Locale;

import static com.almoturg.sprog.util.Util.getThemeColor;
import static com.almoturg.sprog.util.Util.isDarkTheme;

public class PoemRow {
    private static Calendar cal;
    private static MarkdownConverter markdownConverter;

    public static void update_poem_row_poem_page(Poem poem, View poem_row,
                                                 Context context){
        update_poem_row(poem, poem_row, true, false, false, false,  context);
    }

    public static void update_poem_row_mainlist(Poem poem, View poem_row,
                                                boolean show_only_favorites, boolean mark_read,
                                                Context context){
        update_poem_row(poem, poem_row, false, true, show_only_favorites, mark_read, context);
    }

    private static void update_poem_row(Poem poem, View poem_row, boolean border,
                                        boolean main_list, boolean show_only_favorites,
                                        boolean mark_read, Context context) {
        if (cal == null) {
            cal = Calendar.getInstance(Locale.ENGLISH);
        }
        if (markdownConverter == null) {
            markdownConverter = new MarkdownConverter(context);
        }

        if (border) {
            if (isDarkTheme(context)) {
                poem_row.findViewById(R.id.container).setBackgroundResource(R.drawable.card_border_darktheme);
            } else {
                poem_row.findViewById(R.id.container).setBackgroundResource(R.drawable.card_border);
            }
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
                        getThemeColor(context, R.attr.inactiveCardBackgroundColor));
            } else {
                ((CardView) poem_row).setCardBackgroundColor(
                        getThemeColor(context, R.attr.activeCardBackgroundColor));
            }

        } else {
            poem_row.findViewById(R.id.first_line).setVisibility(View.GONE);
            poem_row.findViewById(R.id.content_wrapper).setVisibility(View.VISIBLE);
            ((TextView) poem_row.findViewById(R.id.content))
                    .setText(markdownConverter.convertPoemMarkdown(poem.content, poem.timestamp));
            poem_row.findViewById(R.id.author).setVisibility(View.VISIBLE);
        }
        ((TextView) poem_row.findViewById(R.id.gold_count))
                .setText(" \u00D7 " + Long.toString(poem.gold));
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
}
