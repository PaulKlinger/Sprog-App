package com.almoturg.sprog.util;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.util.TypedValue;

import com.almoturg.sprog.R;

public final class Util {
    public static <T> T last(T[] array) {
        return array[array.length - 1];
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String source) {
        // This converts html to Spanned.
        // fromHtml(String) is deprecated in Nougat so we need to check the version.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(source);
        }
    }

    public static int getDisplayWidthDp(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        return configuration.screenWidthDp;
    }

    public static Spanned linkToSpan(String link) {
        return fromHtml(String.format("<a href=\"%s\">%s</a>", link, link));
    }

    public static int getThemeColor(Context context, int attrId) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attrId, value, true);
        return value.data;
    }

    public static int getThemeReference(Context context, int attrId) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attrId, value, false);
        return value.data;
    }

    public static boolean isDarkTheme(Context context) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.themeName, value, true);
        return value.string.equals("dark");
    }
}
