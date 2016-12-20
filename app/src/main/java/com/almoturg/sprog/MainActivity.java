package com.almoturg.sprog;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.io.File;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import in.uncod.android.bypass.Bypass;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "Sprog";
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private List<Poem> poems;
    public String sort_order = "Date";
    private BroadcastReceiver onComplete;
    public TextView statusView;
    private Tracker mTracker;
    private boolean updating = false; // after processing set last update time if this is true

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

        statusView = (TextView) findViewById(R.id.update_status);

        Spinner sortSpinner = (Spinner) findViewById(R.id.sort_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sort_orders, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (poems != null) {
                    String selectedItem = parent.getItemAtPosition(position).toString();
                    sort_order = selectedItem;
                    sortPoems();
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {
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
            } else if (poems == null){
                processPoems();
            }
        }
    }

    private void sortPoems() {
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
        mAdapter.notifyDataSetChanged();
    }

    public void processPoems() {
        statusView.setText("processing");
        new ParsePoemsTask(this).execute();
    }

    public void setNewPoems(List<Poem> poems) {
        if (updating){
            updating = false;
            if (poems.size() > 1000){
                SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                editor.putLong("LAST_UPDATE_TIME", now.getTimeInMillis());
                editor.apply();

            }
        }

        this.poems = poems;
        mAdapter = new MyAdapter(poems, this);
        mRecyclerView.setAdapter(mAdapter);
        sortPoems();
        statusView.setText(String.format("%d poems", poems.size()));
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
}


class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder>
        implements FastScrollRecyclerView.SectionedAdapter {
    private Bypass bypass;
    private Context context;
    private List<Poem> poems;
    private Calendar cal;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        public int position;
        public TextView poem_content;
        public CardView view;

        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(this);
            this.poem_content = (TextView) v.findViewById(R.id.content);
            this.view = (CardView) v;
        }

        @Override
        public void onClick(View v) {
            if (poem_content.getEllipsize() == null) {
                Intent intent = new Intent(context, PoemActivity.class);
                intent.putExtra("POEM", poems.get(position));
                context.startActivity(intent);
            } else {
                poem_content.setEllipsize(null);
                poem_content.setMaxLines(Integer.MAX_VALUE);
            }
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    MyAdapter(List<Poem> poems, Context context) {
        this.poems = poems;
        this.bypass = new Bypass(context, new Bypass.Options());
        this.context = context;
        this.cal = Calendar.getInstance(Locale.ENGLISH);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.poem_row, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Poem poem = (poems.get(position));

        holder.position = position;
        holder.poem_content.setEllipsize(TextUtils.TruncateAt.END);
        holder.poem_content.setMaxLines(1);
        Util.update_poem_row(poem, holder.view, false, context);

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return poems.size();
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        if (((MainActivity) context).sort_order.equals("Date")) {
            cal.setTimeInMillis((long) poems.get(position).timestamp * 1000);
            return DateFormat.format("yyyy-MM", cal).toString();
        } else if (((MainActivity) context).sort_order.equals("Score")) {
            return Integer.toString(poems.get(position).score);
        } else if (((MainActivity) context).sort_order.equals("Gold")) {
            return Integer.toString(poems.get(position).gold);
        }
        return "";
    }
}