package com.almoturg.sprog;

import android.os.AsyncTask;
import android.os.Environment;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul on 2016-12-18.
 */

public class ParsePoemsTask extends AsyncTask<Void, Void, List<Poem>> {
    private MainActivity activity;

    @Override
    protected List<Poem> doInBackground(Void... arg0){
        List<Poem> poems = new ArrayList<>();

        File poems_file = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "poems.json");
        File poems_old_file = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "poems_old.json");
        if (!poems_file.exists()) {
            poems_old_file.renameTo(poems_file);
        }

        try {
            poems = PoemParser.readJsonStream(new FileInputStream(poems_file));
        } catch (IOException e) {
        }

        return poems;
    }

    @Override
    protected void onPostExecute(List<Poem> poems){
        activity.setNewPoems(poems);
    }

    public ParsePoemsTask(MainActivity activity){
        this.activity = activity;
    }

}
