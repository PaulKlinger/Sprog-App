package com.almoturg.sprog;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import in.uncod.android.bypass.Bypass;

public class PoemActivity extends AppCompatActivity {
    private Bypass bypass;
    private Poem poem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poem);
        bypass = new Bypass(this, new Bypass.Options());

        Toolbar myToolbar = (Toolbar) findViewById(R.id.poem_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        myToolbar.setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Intent mIntent=getIntent();
        poem = (Poem) mIntent.getSerializableExtra("POEM");
        if (poem.main_poem != null){ // This poem is in the parents of another one
            poem = poem.main_poem;
        }

        ViewGroup mainlist = (ViewGroup) findViewById(R.id.single_poem_main_list);

        View v;
        v = LayoutInflater.from(this).inflate(R.layout.post_row, mainlist, false);
        ((TextView) v.findViewById(R.id.title)).setText(poem.post_title);
        ((TextView) v.findViewById(R.id.author)).setText(poem.post_author);
        if (poem.post_content != null && poem.post_content.length()>0){
            ((TextView) v.findViewById(R.id.content)).setText(bypass.markdownToSpannable(poem.post_content));
            v.findViewById(R.id.content).setVisibility(View.VISIBLE);
        }
        mainlist.addView(v);

        for (ParentComment parent : poem.parents){
            if (parent.is_poem != null){
                v = LayoutInflater.from(this).inflate(R.layout.poem_row, mainlist, false);
                Util.update_poem_row(parent.is_poem, v, true, this);
            } else {
                v = LayoutInflater.from(this)
                        .inflate(R.layout.parents_list_row, mainlist, false);
                ((TextView) v.findViewById(R.id.content)).setText(bypass.markdownToSpannable(parent.content));
                ((TextView) v.findViewById(R.id.author)).setText(parent.author);
            }
            mainlist.addView(v);
        }

        if (poem.content != null && poem.content.length()>0){
            v = LayoutInflater.from(this).inflate(R.layout.poem_row, mainlist, false);
            Util.update_poem_row(poem, v, true, this);
            mainlist.addView(v);
        }

    }

    public void toReddit(View view){
        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(poem.link + "?context=100")));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }
}
