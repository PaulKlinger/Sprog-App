package com.almoturg.sprog;

import android.content.Context;
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

public class ParsePoemsTask extends AsyncTask<Context, List<Poem>, Boolean> {
    private MainActivity activity;

    @Override
    protected Boolean doInBackground(Context... context){
        List<Poem> poems;

        File poems_file = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "poems.json");
        File poems_old_file = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "poems_old.json");
        if (!poems_file.exists()) {
            poems_old_file.renameTo(poems_file);
        }

        try {
            PoemParser parser = new PoemParser(new FileInputStream(poems_file), context[0]);
            while (true){
                poems = parser.getPoems(10);
                if (poems == null){break;}
                publishProgress(poems);
            }

        } catch (IOException e) {
            return false;
        }

        return true;
    }

    @Override
    protected void onProgressUpdate(List<Poem>... poems_set){
        activity.addPoems(poems_set[0]);
    }

    @Override
    protected void onPostExecute(Boolean status){
        activity.finishedProcessing(status);
    }

    public ParsePoemsTask(MainActivity activity){
        this.activity = activity;
    }

}
