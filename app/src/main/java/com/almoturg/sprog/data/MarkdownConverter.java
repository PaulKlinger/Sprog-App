package com.almoturg.sprog.data;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.style.URLSpan;

import in.uncod.android.bypass.Bypass;

public class MarkdownConverter {
    private static Bypass bypass;
    private static final Object bypassLock = new Object();
    private Context context;

    public MarkdownConverter(Context context){
        this.context = context;
    }

    public CharSequence convertPoemMarkdown(String markdown, double timestamp) {
        if (timestamp <  1360540800){ // = 2013-02-11
            // Sprog's early poems had paragraph breaks at the end of each line.
            // This regex replaces them with single linebreaks, except between stanzas.
            // (Stanza breaks are detected by checking if the line above is italicized (enclosed in
            // "*"s) while the one below is not, or vice versa. Some lines look like
            // "*lorem ipsum*," (punctuation after the closing "*").
            // There is a special case for one poem with "2." on a single line (c6juhtd).)
            markdown = markdown.replaceAll(
                    "(?:(?<=(?<!\n)\\*[.,]?)\\n\\n(?=\\*))|(?:(?<!(?:\\*[.,]?)|(?:2\\.))\\n\\n(?!\\*))",
                    "  \n");
        }
        return convertMarkdown(markdown);
    }

    public CharSequence convertMarkdown(String markdown) {
        if (bypass == null) {
            synchronized (bypassLock) {
                bypass = new Bypass(context);
            }
        }

        markdown = markdown.replaceAll("(?:^|[^(\\[])(https?://\\S*\\.\\S*)(?:\\s|$)", "[$1]($1)");

        CharSequence converted;
        synchronized (bypassLock) {
            converted = bypass.markdownToSpannable(markdown);
        }
        return converted;
    }
}
