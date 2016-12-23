package com.almoturg.sprog;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

import static com.almoturg.sprog.SprogApplication.filtered_poems;
import static com.almoturg.sprog.SprogApplication.poems;


public class MainActivity extends AppCompatActivity {
    public static final String TAG = "Sprog";
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    public String sort_order = "Date";
    private BroadcastReceiver onComplete;
    public TextView statusView;
    private Tracker mTracker;
    private boolean updating = false; // after processing set last update time if this is true
    private boolean processing = false;

    // These are the times when an update should be available on the server
    static int FIRST_UPDATE_HOUR = 2;
    static int SECOND_UPDATE_HOUR = 14;

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
                            String selectedItem = parent.getItemAtPosition(position).toString();
                            sort_order = selectedItem;
                            sortPoems();
                        }
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }
        });

        final EditText search_box = (EditText) findViewById(R.id.search_box);
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
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

    }

    @Override
    protected void onStart() {
        super.onStart();
        mTracker.setScreenName("PoemsList");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        autoUpdate();
    }

    private void autoUpdate() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        long last_update_tstamp = prefs.getLong("LAST_UPDATE_TIME", -1);

        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "poems.json");
        if (last_update_tstamp == -1 || !file.exists()) {
            updatePoems(null);
        } else {
            Log.i(TAG, "Checking if it's time to update");
            Log.i(TAG, String.format("last update time %d", last_update_tstamp));
            Calendar last_update_cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

            last_update_cal.setTimeInMillis(last_update_tstamp);
            long diff_in_ms = now.getTimeInMillis() - last_update_tstamp;
            long ms_today = now.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000
                    + now.get(Calendar.MINUTE) * 60 * 1000
                    + now.get(Calendar.SECOND) * 1000
                    + now.get(Calendar.MILLISECOND);

            if (
                    (now.get(Calendar.HOUR_OF_DAY) >= FIRST_UPDATE_HOUR
                            && diff_in_ms > ms_today - FIRST_UPDATE_HOUR * 60 * 60 * 1000
                    ) ||
                            (now.get(Calendar.HOUR_OF_DAY) >= SECOND_UPDATE_HOUR
                                    && diff_in_ms > ms_today - SECOND_UPDATE_HOUR * 60 * 60 * 1000)
                    ) {
                updatePoems(null);
            } else if (poems == null) {
                processPoems();
            } else if (mAdapter == null) {
                // TODO: This really has nothing to do with autoupdate, should put somewhere else...
                mAdapter = new PoemsListAdapter(filtered_poems, this);
                mRecyclerView.setAdapter(mAdapter);
                statusView.setText(String.format("%d poems", filtered_poems.size()));
            }
        }
    }

    private void sortPoems() {
        // sortPoems is automatically called when the spinner is created, poems might not be loaded yet
        if (poems == null) return;

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
    }

    public void processPoems() {
        processing = true;
        sort_order = "Date";
        ((Spinner) findViewById(R.id.sort_spinner)).setSelection(0); // 0 is Date (is there a better way to do this??)
        statusView.setText("processing");
        poems = new ArrayList<>();
        filtered_poems = new ArrayList<>();
        toggleSearch(null); // hide search box (checks processing)
        mAdapter = new PoemsListAdapter(filtered_poems, this);
        mRecyclerView.setAdapter(mAdapter);
        new ParsePoemsTask(this).execute(this);
    }

    public void addPoems(List<Poem> poems_set) {
        poems.addAll(poems_set);
        filtered_poems.addAll(poems_set);
        statusView.setText(String.format("%d poems", poems.size()));
        mAdapter.notifyDataSetChanged();
    }

    public void finishedProcessing(boolean status) {
        if (updating) {
            updating = false;
            if (poems.size() > 1000) {
                SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                editor.putLong("LAST_UPDATE_TIME", now.getTimeInMillis());
                editor.apply();

            }
        }
        if (!status) {
            statusView.setText("error");
        }
        processing = false;
    }

    public void updatePoems(View view) {
        if (onComplete != null) {
            return;
        }
        updating = true;

        File poems_file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "poems.json");
        File poems_old_file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "poems_old.json");
        if (poems_file.exists()) {
            poems_file.renameTo(poems_old_file);
        }

        String url = "https://almoturg.com/poems.json";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Sprog poems");
        request.setTitle("Sprog");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

        request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "poems.json");

        // get download service and enqueue file
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        onComplete = new BroadcastReceiver() {
            public void onReceive(Context ctxt, Intent intent) {

                statusView.setText("poems downloaded");
                processPoems();
                unregisterReceiver(onComplete);
                onComplete = null;
            }
        };
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        statusView.setText("loading poems");
        manager.enqueue(request);
    }

    public void toggleSearch(View view) {
        EditText search_box = (EditText) findViewById(R.id.search_box);
        if (search_box.getVisibility() == View.GONE && !processing) {
            search_box.setVisibility(View.VISIBLE);
            findViewById(R.id.toggle_search).setBackgroundResource(R.drawable.search_button_background);
            search_box.requestFocus();
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .showSoftInput(search_box, InputMethodManager.SHOW_IMPLICIT);
        } else {
            search_box.setVisibility(View.GONE);
            search_box.setText("");
            findViewById(R.id.toggle_search).setBackgroundColor(Color.TRANSPARENT);
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(search_box.getWindowToken(),0);
            searchPoems();
        }
    }

    public void searchPoems() {
        Log.d(TAG, "searching");
        if (mAdapter == null) { // afterTextChanged gets called when EditText is created...
            return;
        }
        String search_string = ((EditText) findViewById(R.id.search_box)).getText().toString().toLowerCase();
        String[] keywords = search_string.split(" ");
        filtered_poems = new ArrayList<>();
        for (Poem p : poems) {
            String content = p.content.toString().toLowerCase();
            boolean add = true;
            for (String keyword : keywords) {
                if (!content.contains(keyword)) {
                    add = false;
                }
            }
            if (add) {
                filtered_poems.add(p);
            }
        }
        statusView.setText(filtered_poems.size() + " poems");
        mAdapter = new PoemsListAdapter(filtered_poems, this);
        mRecyclerView.setAdapter(mAdapter);
    }
}


