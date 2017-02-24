package com.almoturg.sprog.util;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;

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
}
