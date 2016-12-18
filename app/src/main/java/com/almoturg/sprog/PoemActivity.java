package com.almoturg.sprog;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

import in.uncod.android.bypass.Bypass;

public class PoemActivity extends AppCompatActivity {
    private Bypass bypass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poem);
        bypass = new Bypass(this, new Bypass.Options());

        Intent mIntent=getIntent();
        Poem poem = (Poem) mIntent.getSerializableExtra("POEM");
        ((TextView) findViewById(R.id.post_title)).setText(poem.post_title);
        ViewGroup mainlist = (ViewGroup) findViewById(R.id.single_poem_main_list);

        View v;
        if (poem.post_content != null && poem.post_content.length()>0){
            v = LayoutInflater.from(this).inflate(R.layout.parents_list_row, mainlist, false);
            ((TextView) v.findViewById(R.id.content)).setText(bypass.markdownToSpannable(poem.post_content));
            mainlist.addView(v);
        }

        for (ParentComment parent : poem.parents){
            v = LayoutInflater.from(this)
                    .inflate(R.layout.parents_list_row, mainlist, false);
            ((TextView) v.findViewById(R.id.content)).setText(bypass.markdownToSpannable(parent.content));
            ((TextView) v.findViewById(R.id.author)).setText("/u/" + parent.author);
            mainlist.addView(v);
        }

        if (poem.content != null && poem.content.length()>0){
            v = LayoutInflater.from(this).inflate(R.layout.poem_row, mainlist, false);
            v.findViewById(R.id.container).setBackgroundResource(R.drawable.card_border);
            // setBackgroundResource removes padding...
            int card_padding = this.getResources().getDimensionPixelSize(R.dimen.card_padding);
            v.findViewById(R.id.container).setPadding(card_padding, card_padding, card_padding, card_padding);

            ((TextView) v.findViewById(R.id.content)).setText(bypass.markdownToSpannable(poem.content));
            ((TextView) v.findViewById(R.id.gold_count)).setText(" × " + Long.toString(poem.gold));
            if (poem.gold > 0){
                v.findViewById(R.id.gold_display).setVisibility(View.VISIBLE);
            }
            ((TextView) v.findViewById(R.id.score)).setText(Long.toString(poem.score));

            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis((long) poem.timestamp * 1000);
            ((TextView) v.findViewById(R.id.datetime)).setText(DateFormat.format("yyyy-MM-dd HH:mm:ss", cal).toString());
            mainlist.addView(v);
        }

    }
}
