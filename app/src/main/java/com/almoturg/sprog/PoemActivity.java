package com.almoturg.sprog;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
        ((TextView) findViewById(R.id.post_author)).setText(poem.post_author);
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
            Util.update_poem_row(poem, v, true, this);
            mainlist.addView(v);
        }

    }
}
