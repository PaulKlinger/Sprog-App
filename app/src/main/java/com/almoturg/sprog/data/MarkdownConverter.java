package com.almoturg.sprog.data;

import android.content.Context;
import android.text.SpannableStringBuilder;

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

        // Reddit markdown only interprets underscores as italics if they are not followed by a
        // non-whitespace character. E.g. "_fizz_" becomes italic but "_fizz_buzz" does not.
        // As the Bypass markdown library does not allow that, we replace such underscores
        // with a unicode private use char and substitute back afterwards
        markdown = markdown.replaceAll("(?=\\S)_(?=\\S|$)","\uE000");

        CharSequence converted;
        synchronized (bypassLock) {
            converted = bypass.markdownToSpannable(markdown);
        }

        // Can't directly replace in SpannableString so convert to SpannableStringBuilder first.
        SpannableStringBuilder converted_builder = new SpannableStringBuilder(converted);
        for (int i=0; i<converted_builder.length();i++) {
            if (converted_builder.charAt(i) == '\uE000') {
                converted_builder.replace(i,i+1,"_");
            }
        }
        return converted_builder;
    }
}
