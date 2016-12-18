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
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.JsonReader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import in.uncod.android.bypass.Bypass;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "Sprog";
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private List<Poem> poems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        processPoems();
    }

    public void processPoems() {
        TextView tv1 = (TextView) findViewById(R.id.update_status);
        try {
            poems = readJsonStream(new FileInputStream(new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "poems.json")));
            tv1.setText(String.format("%d poems loaded", poems.size()));
        } catch (IOException e) {
            tv1.setText("IO Error" + e.toString());
        }

        // specify an adapter (see also next example)
        mAdapter = new MyAdapter(poems, this);
        mRecyclerView.setAdapter(mAdapter);
    }

    public void updatePoems(View view) {
        String url = "https://almoturg.com/poems.json";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Sprog poems");
        request.setTitle("Sprog");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

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

    public List<Poem> readJsonStream(InputStream in) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        try {
            return readPoemsArray(reader);
        } finally {
            reader.close();
        }
    }

    public List<Poem> readPoemsArray(JsonReader reader) throws IOException {
        List<Poem> Poems = new ArrayList<Poem>();

        reader.beginArray();
        while (reader.hasNext()) {
            Poems.add(readPoem(reader));
        }
        reader.endArray();
        return Poems;
    }

    public Poem readPoem(JsonReader reader) throws IOException {
        int gold = -1;
        int score = -1;
        String content = null;
        double timestamp = -1;
        String post_title = null;
        String post_content = null;
        List<ParentComment> parents = null;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("gold")) {
                gold = reader.nextInt();
            } else if (name.equals("score")){
                score = reader.nextInt();
            } else if (name.equals("orig_content")) {
                content = reader.nextString();
            } else if (name.equals("timestamp")) {
                timestamp = reader.nextDouble();
            } else if (name.equals("submission_title")) {
                post_title = reader.nextString();
            } else if (name.equals("orig_submission_content")){
                post_content = reader.nextString();
            } else if (name.equals("parents")) {
                parents = readParentCommentArray(reader);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return new Poem(gold, score, content, timestamp, post_title, post_content, parents);
    }

    public List<ParentComment> readParentCommentArray(JsonReader reader) throws IOException {
        List<ParentComment> parents = new ArrayList<ParentComment>();

        reader.beginArray();
        while (reader.hasNext()) {
            parents.add(readParentComment(reader));
        }
        reader.endArray();
        return parents;
    }

    public ParentComment readParentComment(JsonReader reader) throws IOException {
        String content = null;
        String author = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("author")) {
                author = reader.nextString();
            } else if (name.equals("orig_body")) {
                content = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return new ParentComment(content, author);
    }
}

class Poem implements Serializable {
    public int gold;
    public int score;
    public String content;
    public double timestamp;
    public String post_title;
    public String post_content;
    public List<ParentComment> parents;

    public Poem(int gold, int score, String content, double timestamp,
                String post_title, String post_content,
                List<ParentComment> parents) {
        this.content = content;
        this.gold = gold;
        this.score = score;
        this.timestamp = timestamp;
        this.post_title = post_title;
        this.post_content = post_content;
        this.parents = parents;
    }
}

class ParentComment implements Serializable {
    public String content;
    public String author;

    public ParentComment(String content, String author) {
        this.content = content;
        this.author = author;
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
        holder.poem_content.setText(bypass.markdownToSpannable(poems.get(position).content));


        ((TextView) holder.view.findViewById(R.id.gold_count)).setText(" × " + Long.toString(poem.gold));
        if (poem.gold > 0){
            holder.view.findViewById(R.id.gold_display).setVisibility(View.VISIBLE);
        } else {
            holder.view.findViewById(R.id.gold_display).setVisibility(View.INVISIBLE);
        }
        ((TextView) holder.view.findViewById(R.id.score)).setText(Long.toString(poem.score));

        cal.setTimeInMillis((long) poem.timestamp * 1000);
        ((TextView) holder.view.findViewById(R.id.datetime)).setText(DateFormat.format("yyyy-MM-dd HH:mm:ss", cal).toString());

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return poems.size();
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        cal.setTimeInMillis((long) poems.get(position).timestamp * 1000);
        return DateFormat.format("yyyyMM", cal).toString();
    }
}