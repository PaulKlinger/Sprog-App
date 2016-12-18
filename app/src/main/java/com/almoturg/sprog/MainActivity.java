package com.almoturg.sprog;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
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
import android.util.JsonReader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import in.uncod.android.bypass.Bypass;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "Sprog";
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private List<Poem> poems;
    public String sort_order = "Date";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        myToolbar.setTitle(null);

        Spinner sortSpinner = (Spinner) findViewById(R.id.sort_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sort_orders, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                sort_order = selectedItem;
                sortPoems();
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

        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "poems.json");
        if (! file.exists()) {
            updatePoems(null);
        } else {
            processPoems();
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
        poems = new ArrayList<>();

        File poems_file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "poems.json");
        File poems_old_file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "poems_old.json");
        if (! poems_file.exists()){
            poems_old_file.renameTo(poems_file);
        }

        TextView tv1 = (TextView) findViewById(R.id.update_status);
        try {
            poems = PoemParser.readJsonStream(new FileInputStream(poems_file));
            tv1.setText(String.format("%d poems", poems.size()));
        } catch (IOException e) {
            tv1.setText("IO Error" + e.toString());
        }

        mAdapter = new MyAdapter(poems, this);
        mRecyclerView.setAdapter(mAdapter);
        sortPoems();
    }

    public void updatePoems(View view) {
        File poems_file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "poems.json");
        File poems_old_file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "poems_old.json");
        if (poems_file.exists()){
            poems_file.renameTo(new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "poems_old.json"));
        }

        String url = "https://almoturg.com/poems.json";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Sprog poems");
        request.setTitle("Sprog");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

        request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "poems.json");

        // get download service and enqueue file
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        BroadcastReceiver onComplete = new BroadcastReceiver() {
            public void onReceive(Context ctxt, Intent intent) {

                TextView tv1 = (TextView) findViewById(R.id.update_status);
                tv1.setText("json downloaded");
                processPoems();
            }
        };
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

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
            //poem_content.setText(bypass.markdownToSpannable(mDataset.get(position).content));
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
        if (((MainActivity) context).sort_order.equals("Date")){
            cal.setTimeInMillis((long) poems.get(position).timestamp * 1000);
            return DateFormat.format("yyyy-MM", cal).toString();
        } else if (((MainActivity) context).sort_order.equals("Score")){
            return Integer.toString(poems.get(position).score);
        } else if  (((MainActivity) context).sort_order.equals("Gold")) {
            return Integer.toString(poems.get(position).gold);
        }
        return "";
    }
}