package com.almoturg.sprog;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import static com.almoturg.sprog.SprogApplication.filtered_poems;
import static com.almoturg.sprog.SprogApplication.poems;

public class PoemActivity extends AppCompatActivity {
    private Poem poem; // The mainpoem corresponding to the selected one.
    private Poem selectedPoem; // The selected poem.
    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poem);

        SprogApplication application = (SprogApplication) getApplication();
        mTracker = application.getDefaultTracker();

        Toolbar myToolbar = (Toolbar) findViewById(R.id.poem_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        myToolbar.setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Intent mIntent = getIntent();
        selectedPoem = filtered_poems.get((int) mIntent.getSerializableExtra("POEM_ID"));
        if (selectedPoem.main_poem != null) { // This poem is in the parents of another one
            poem = selectedPoem.main_poem;
        } else {
            poem = selectedPoem;
        }

        ViewGroup mainlist = (ViewGroup) findViewById(R.id.single_poem_main_list);

        View v;
        v = LayoutInflater.from(this).inflate(R.layout.post_row, mainlist, false);
        ((TextView) v.findViewById(R.id.title)).setText(Util.convertMarkdown(poem.post_title, this));
        ((TextView) v.findViewById(R.id.author)).setText(poem.post_author);
        if (poem.post_content != null && poem.post_content.length() > 0) {
            ((TextView) v.findViewById(R.id.content)).setText(Util.convertMarkdown(poem.post_content, this));
            ((TextView) v.findViewById(R.id.content)).setMovementMethod(LinkMovementMethod.getInstance());
            v.findViewById(R.id.content).setVisibility(View.VISIBLE);
        }
        mainlist.addView(v);

        for (ParentComment parent : poem.parents) {
            if (parent.is_poem != null) {
                v = LayoutInflater.from(this).inflate(R.layout.poem_row, mainlist, false);
                ((TextView) v.findViewById(R.id.content)).setMovementMethod(LinkMovementMethod.getInstance());
                Util.update_poem_row(parent.is_poem, v, true, false, this);
            } else {
                v = LayoutInflater.from(this)
                        .inflate(R.layout.parents_list_row, mainlist, false);
                ((TextView) v.findViewById(R.id.content)).setText(Util.convertMarkdown(parent.content, this));
                ((TextView) v.findViewById(R.id.content)).setMovementMethod(LinkMovementMethod.getInstance());
                ((TextView) v.findViewById(R.id.author)).setText(parent.author);
            }
            mainlist.addView(v);
        }

        if (poem.content != null && poem.content.length() > 0) {
            v = LayoutInflater.from(this).inflate(R.layout.poem_row, mainlist, false);
            ((TextView) v.findViewById(R.id.content)).setMovementMethod(LinkMovementMethod.getInstance());
            Util.update_poem_row(poem, v, true, false, this);
            mainlist.addView(v);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        mTracker.setScreenName("Poem");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("PoemPage")
                .setAction(Util.last(poem.link.split("/")))
                .build());
    }

    public void toReddit(View view) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        } else if (item.getItemId() == R.id.action_copy) {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("copy")
                    .setAction(Util.last(poem.link.split("/")))
                    .build());
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(this.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("simple text",
                    Util.convertMarkdown(selectedPoem.content, this).toString());
            clipboard.setPrimaryClip(clip);
            Toast toast = Toast.makeText(this, "Poem copied to clipboard", Toast.LENGTH_SHORT);
            toast.show();
        } else if (item.getItemId() == R.id.action_toReddit) {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("toReddit")
                    .setAction(Util.last(poem.link.split("/")))
                    .build());
            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(poem.link + "?context=100")));
        } else if (item.getItemId() == R.id.action_share) {
            Toast toast = Toast.makeText(this, "sharing", Toast.LENGTH_SHORT);
            toast.show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.poem_toolbar, menu);

        MenuItem item = menu.findItem(R.id.action_share);

        ShareActionProvider mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                Util.convertMarkdown(selectedPoem.content, this));
        mShareActionProvider.setShareIntent(shareIntent);
        return true;
    }
}
