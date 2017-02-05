package com.almoturg.sprog.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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

import com.almoturg.sprog.R;
import com.almoturg.sprog.SprogApplication;
import com.almoturg.sprog.data.MarkdownConverter;
import com.almoturg.sprog.presenter.PoemPresenter;
import com.almoturg.sprog.model.Poem;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;


public class PoemActivity extends AppCompatActivity {
    private PoemPresenter presenter;

    private Tracker mTracker;
    private View selectedPoemView;
    private ViewGroup mainlist;
    private MenuItem favoriteItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (presenter == null) {
            Context appcontext = getApplicationContext();
            presenter = new PoemPresenter(SprogApplication.getDbHelper(appcontext),
                    new MarkdownConverter(appcontext));
        }
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
        mainlist = (ViewGroup) findViewById(R.id.single_poem_main_list);

        presenter.attachView(this, (int) mIntent.getSerializableExtra("POEM_ID"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTracker.setScreenName("Poem");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        trackEvent("PoemPage", presenter.getSelectedPoemID(), null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.detachView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        } else if (item.getItemId() == R.id.action_copy) {
            presenter.onActionCopy();
        } else if (item.getItemId() == R.id.action_toReddit) {
            presenter.onActionToReddit();
        } else if (item.getItemId() == R.id.action_share) {
            Toast toast = Toast.makeText(this, "sharing", Toast.LENGTH_SHORT);
            toast.show();
        } else if (item.getItemId() == R.id.action_addToFavorites){
            presenter.onActionToggleFavorite();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.poem_toolbar, menu);

        favoriteItem = menu.findItem(R.id.action_addToFavorites);
        if (presenter.isFavorite()){
            favoriteItem.setIcon(R.drawable.ic_star_full);
        }

        MenuItem item = menu.findItem(R.id.action_share);

        ShareActionProvider mShareActionProvider =
                (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, presenter.getPoemContentString());
        mShareActionProvider.setShareIntent(shareIntent);
        return true;
    }

    public void displayPost(CharSequence title, CharSequence author, CharSequence content) {
        View v = LayoutInflater.from(this).inflate(R.layout.post_row, mainlist, false);
        ((TextView) v.findViewById(R.id.title))
                .setText(title);
        ((TextView) v.findViewById(R.id.author)).setText(author);
        if (content != null) {
            ((TextView) v.findViewById(R.id.content))
                    .setText(content);
            ((TextView) v.findViewById(R.id.content))
                    .setMovementMethod(LinkMovementMethod.getInstance());
            v.findViewById(R.id.content).setVisibility(View.VISIBLE);
        }
        mainlist.addView(v);
    }

    public void displayParentPoem(Poem poem, boolean is_selected) {
        View v = LayoutInflater.from(this).inflate(R.layout.poem_row, mainlist, false);
        ((TextView) v.findViewById(R.id.content))
                .setMovementMethod(LinkMovementMethod.getInstance());
        PoemRow.update_poem_row_poem_page(poem, v, this);
        mainlist.addView(v);
        if (is_selected){
            selectedPoemView = v;
        }
    }

    public void displayParentComment(CharSequence content, CharSequence author) {
        View v = LayoutInflater.from(this)
                .inflate(R.layout.parents_list_row, mainlist, false);
        ((TextView) v.findViewById(R.id.content))
                .setText(content);
        ((TextView) v.findViewById(R.id.content))
                .setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView) v.findViewById(R.id.author)).setText(author);
    }

    public void displayMainPoem(Poem poem, boolean is_selected) {
        View v = LayoutInflater.from(this).inflate(R.layout.poem_row, mainlist, false);
        ((TextView) v.findViewById(R.id.content))
                .setMovementMethod(LinkMovementMethod.getInstance());
        PoemRow.update_poem_row_poem_page(poem, v,  this);
        mainlist.addView(v);
        if (is_selected) {
            selectedPoemView = v;
        }
    }

    public void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(this.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("simple text", text);
        clipboard.setPrimaryClip(clip);
        Toast toast = Toast.makeText(this, "Poem copied to clipboard", Toast.LENGTH_SHORT);
        toast.show();
    }

    public void openLink(String link) {
        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(link)));
    }

    public void addedFavorite(Poem selectedPoem) {
        Toast toast = Toast.makeText(this, "added to favorites", Toast.LENGTH_SHORT);
        toast.show();
        favoriteItem.setIcon(R.drawable.ic_star_full);
        PoemRow.update_poem_row_poem_page(selectedPoem, selectedPoemView, this);
    }

    public void removedFavorite(Poem selectedPoem) {
        Toast toast = Toast.makeText(this, "removed from favorites", Toast.LENGTH_SHORT);
        toast.show();
        favoriteItem.setIcon(R.drawable.ic_star_empty);
        PoemRow.update_poem_row_poem_page(selectedPoem, selectedPoemView, this);
    }

    public void trackEvent(String category, String action, String label){
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .build());
    }
}
