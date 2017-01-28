package com.almoturg.sprog.ui;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.util.Log;
import android.widget.ViewFlipper;

import com.almoturg.sprog.util.ParsePoemsTask;
import com.almoturg.sprog.R;
import com.almoturg.sprog.SprogApplication;
import com.almoturg.sprog.util.PoemsLoader;
import com.almoturg.sprog.util.Util;
import com.almoturg.sprog.model.Poem;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

import static com.almoturg.sprog.SprogApplication.filtered_poems;
import static com.almoturg.sprog.SprogApplication.poems;


public class MainActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    public String sort_order = "Date";
    public BroadcastReceiver downloadPoemsComplete;
    public TextView statusView;
    public ViewFlipper viewFlipper;
    private Tracker mTracker;
    public boolean updating = false; // after processing set last update time if this is true
    public boolean processing = false;
    public boolean show_only_favorites = false;
    public SharedPreferences prefs;
    public ArrayList<String> new_read_poems; // Poems newly marked as read

    // for analytics tracking of search queries
    private String last_search_string = "";
    private boolean sent_search = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SprogApplication application = (SprogApplication) getApplication();
        mTracker = application.getDefaultTracker();

        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        myToolbar.setTitle(null);

        statusView = (TextView) findViewById(R.id.status);
        viewFlipper = (ViewFlipper) findViewById(R.id.poemsListEmptyFlipper);

        final Spinner sortSpinner = (Spinner) findViewById(R.id.sort_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sort_orders, R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);

        sortSpinner.post(new Runnable() {
            public void run() {
                sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (!processing) {
                            sort_order = parent.getItemAtPosition(position).toString();
                            sortPoems();
                        }
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }
        });

        final EditText search_box = (EditText) findViewById(R.id.search_text);
        search_box.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchPoems();
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Log.d("SPROG", "mainactivity preferences:" + prefs.toString());
        new_read_poems = new ArrayList<>();

        FirebaseMessaging.getInstance().subscribeToTopic("PoemUpdates");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTracker.setScreenName("PoemsList");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        // A dialog asking whether to enable new poem notifications is shown
        // the second time MainActivity is started.
        if (Util.timeToShowNotifyDialog(prefs)){
            NotifyDialog notifyDialog = new NotifyDialog();
            FragmentManager fm = this.getSupportFragmentManager();
            notifyDialog.show(fm, "DIALOG_NOTIFY");
        }

        // I don't actually know why this doesn't work in onCreate
        // but everything else to do with the recyclerview does.
        // Maybe it would work there??
        if (mAdapter == null) {
            mAdapter = new PoemsListAdapter(this);
            mRecyclerView.setAdapter(mAdapter);
            if (filtered_poems.size() > 0) {
                setStatusNumPoems(filtered_poems.size());
            }
        } else{
            // E.g. if we come from poem page and it was set to favorite
            searchPoems();
        }
        NotificationManager nm = (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);
        nm.cancelAll();
        boolean update = prefs.getBoolean(Util.PREF_UPDATE_NEXT, false);
        if (update){
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(Util.PREF_UPDATE_NEXT, false);
            editor.apply();
        }

        preparePoems(update);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SprogApplication.getDbHelper(this).addReadPoems(new_read_poems);
        new_read_poems.clear();

        cancelLoadingPoems();
        // TODO: consider also setting processing to false but would need to cancel task if running
    }

    private void cancelLoadingPoems() {
        PoemsLoader.cancelAllDownloads(this);
        updating = false;
        if (downloadPoemsComplete != null){
            unregisterReceiver(downloadPoemsComplete);
            downloadPoemsComplete = null;
        }
    }

    private void preparePoems(boolean update) { // not sure about the name...
        long last_update_tstamp = prefs.getLong("LAST_UPDATE_TIME", -1);
        long last_fcm_tstamp = prefs.getLong(Util.PREF_LAST_FCM_TSTAMP, -1);
        boolean internet_access = Util.isConnected(this);

        if (last_update_tstamp == -1 || !Util.poemsFileExists(this)) {
            if (internet_access) {
                updatePoems(null);
            } else {
                statusView.setText("no internet");
            }
        } else {
            if (!update) {
                update = Util.isUpdateTime(last_update_tstamp, last_fcm_tstamp);
            }

            if (update && internet_access) {
                updatePoems(null);
            } else if (poems.size() == 0) { // file exists by above (except race)
                processPoems();
            }
        }
    }

    private void sortPoems() {
        // sortPoems is automatically called when the spinner is created, poems might not be loaded yet
        if (poems.size() == 0) return;

        if (sort_order.equals("Date")) {
            Collections.sort(poems, new Comparator<Poem>() {
                @Override
                public int compare(Poem p1, Poem p2) {
                    return (int) (p2.timestamp - p1.timestamp);
                }
            });
        } else if (sort_order.equals("Score")) {
            Collections.sort(poems, new Comparator<Poem>() {
                @Override
                public int compare(Poem p1, Poem p2) {
                    return (p2.score - p1.score);
                }
            });
        } else if (sort_order.equals("Gold")) {
            Collections.sort(poems, new Comparator<Poem>() {
                @Override
                public int compare(Poem p1, Poem p2) {
                    return (p2.gold - p1.gold);
                }
            });
        }
        searchPoems();
        mRecyclerView.scrollToPosition(0);
    }

    public void processPoems() {
        processing = true;
        sort_order = "Date";
        ((Spinner) findViewById(R.id.sort_spinner)).setSelection(0); // 0 is Date (is there a better way to do this??)
        statusView.setText("processing");
        viewFlipper.setDisplayedChild(Util.VIEWFLIPPER_RECYCLERVIEW);
        poems = new ArrayList<>();
        filtered_poems = new ArrayList<>();
        toggleSearch(null); // hide search box (checks processing)
        new ParsePoemsTask(this).execute(this);
    }

    public void addPoems(List<Poem> poems_set) {
        poems.addAll(poems_set);
        filtered_poems.addAll(poems_set);
        setStatusNumPoems(poems.size());
        mAdapter.notifyItemRangeInserted(filtered_poems.size(), poems_set.size());
    }

    public void finishedProcessing(boolean status) {
        if (updating) {
            updating = false;
            if (poems.size() > 1000 && status) {
                SharedPreferences.Editor editor = prefs.edit();

                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                editor.putLong(Util.PREF_LAST_UPDATE_TIME, now.getTimeInMillis());
                // Store timestamp of last poem for new poem notifications
                // Can't store double in sharedprefs so store timestamp in milliseconds as long
                editor.putLong(Util.PREF_LAST_POEM_TIME, (long) poems.get(0).timestamp * 1000);
                editor.apply();
            }
        }
        if (!status) {
            statusView.setText("error");
            viewFlipper.setDisplayedChild(Util.VIEWFLIPPER_ERROR);
        }
        processing = false;
    }

    public void updatePoems(View view) {
        if (processing || downloadPoemsComplete != null) {
            return;
        }
        updating = true;
        statusView.setText("");
        disableFavorites();
        poems.clear();
        filtered_poems.clear();
        mAdapter.notifyDataSetChanged();
        findViewById(R.id.action_cancelUpdate).setVisibility(View.INVISIBLE);
        viewFlipper.setDisplayedChild(Util.VIEWFLIPPER_UPDATING);
        if (Util.poemsFileExists(this)) {
            // If loading poems takes more than 3s and an old poems file is available
            // show a button to allow cancelling the download
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (downloadPoemsComplete != null) {
                        findViewById(R.id.action_cancelUpdate).setVisibility(View.VISIBLE);}
                }
            }, 5000);

        }
        PoemsLoader.loadPoems(this);
    }

    public void cancelButton(View view) {
        cancelLoadingPoems();
        processPoems();
    }

    public void toggleFavorites(View view){
        if (updating){
            return;
        }
        show_only_favorites = !show_only_favorites;
        searchPoems();
        if (show_only_favorites){
            // track number of favorites (if search is active add "s" in front)
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("showFavorites")
                    .setAction((last_search_string.length() > 0 ? "s": "") +
                            Integer.toString(filtered_poems.size()))
                    .build());
            findViewById(R.id.toggle_favorites).setBackgroundResource(
                    R.drawable.favorites_button_background);

        } else {
            disableFavorites();
        }
        if (show_only_favorites && last_search_string.length() == 0 && filtered_poems.size() == 0){
            viewFlipper.setDisplayedChild(Util.VIEWFLIPPER_EMPTY_FAVORITES);
        }
    }

    public void disableFavorites() {
        show_only_favorites = false;
        findViewById(R.id.toggle_favorites).setBackgroundColor(Color.TRANSPARENT);
        int toggle_favorites_padding = getResources().getDimensionPixelSize(R.dimen.toggle_favorites_padding);
        findViewById(R.id.toggle_favorites).setPadding(
                toggle_favorites_padding, toggle_favorites_padding,
                toggle_favorites_padding, toggle_favorites_padding);
        viewFlipper.setDisplayedChild(Util.VIEWFLIPPER_RECYCLERVIEW);
    }

    public void toggleSearch(View view) {
        View search_box = findViewById(R.id.search_box);
        EditText search_text = (EditText) findViewById(R.id.search_text);
        if (search_box.getVisibility() == View.GONE && !processing) {
            search_box.setVisibility(View.VISIBLE);
            findViewById(R.id.toggle_search).setBackgroundResource(R.drawable.search_button_background);
            search_text.requestFocus();
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .showSoftInput(search_text, InputMethodManager.SHOW_IMPLICIT);
        } else {
            search_box.setVisibility(View.GONE);
            if ((!sent_search) && last_search_string.length() > 0) {
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("search")
                        .setAction(last_search_string)
                        .build());
                Log.d("SPROG", "search:" + last_search_string);
            }
            last_search_string = "";
            sent_search = false;
            search_text.setText("");
            findViewById(R.id.toggle_search).setBackgroundColor(Color.TRANSPARENT);
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(search_box.getWindowToken(), 0);
            searchPoems();
        }
    }

    public void clearSearch(View view) {
        ((EditText) findViewById(R.id.search_text)).setText("");
        searchPoems();
    }

    public void searchPoems() {
        if (mAdapter == null || updating) { // afterTextChanged gets called when EditText is created...
            return;
        }
        String search_string = ((EditText) findViewById(R.id.search_text))
                .getText().toString().toLowerCase();
        if (!search_string.contains(last_search_string)) {
            if (!sent_search) {
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("search")
                        .setAction(last_search_string)
                        .build());
                Log.d("SPROG", "search:" + last_search_string);
                sent_search = true;
            }
        } else {
            sent_search = false;
        }
        last_search_string = search_string;

        filtered_poems = new ArrayList<>();
        for (Poem p : poems) {
            String content = p.content.toLowerCase();
            if (content.contains(search_string) &&
                    (!show_only_favorites || p.favorite)) {
                filtered_poems.add(p);
            }
        }
        setStatusNumPoems(filtered_poems.size());
        mAdapter.notifyDataSetChanged();
    }

    private void setStatusNumPoems(int numPoems){
        statusView.setText(String.format("× %d", numPoems));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_toolbar, menu);
        menu.findItem(R.id.action_mark_read).setChecked(prefs.getBoolean(Util.PREF_MARK_READ, true));
        menu.findItem(R.id.action_notify_new).setChecked(prefs.getBoolean(Util.PREF_NOTIFY_NEW, false));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

/*        if (id == R.id.action_force_refresh) {
            updatePoems(null);
            return true;
        } else */
        if (id == R.id.action_mark_read) {
            item.setChecked(!item.isChecked());
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(Util.PREF_MARK_READ, item.isChecked());
            editor.apply();
            mAdapter.notifyDataSetChanged();
        } else if (id == R.id.action_reset_read) {
            new_read_poems.clear();
            SprogApplication.getDbHelper(this).clearReadPoems();
            for (Poem p : poems) {
                p.read = false;
            }
            mAdapter.notifyDataSetChanged();
        } else if (id == R.id.action_notify_new) {
            item.setChecked(!item.isChecked());
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(Util.PREF_NOTIFY_NEW, item.isChecked());
            // if the user set the notification manually we don't need to show the dialog
            editor.putInt(Util.PREF_DISPLAYED_NOTIFICATION_DIALOG, 1);
            editor.apply();
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("notificationOption")
                    .setAction(item.isChecked() ? "yes" : "no")
                    .build());
        }

        return super.onOptionsItemSelected(item);
    }
}