package com.almoturg.sprog.util;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import com.almoturg.sprog.model.Poem;
import com.almoturg.sprog.ui.MainActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;


public class ParsePoemsTask extends AsyncTask<Context, List<Poem>, Boolean> {
    private MainActivity activity;

    @Override
    protected Boolean doInBackground(Context... context) {
        File poems_file = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "poems.json");
        File poems_old_file = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "poems_old.json");
        File used_file = poems_file;
        if (!poems_file.exists()) {
            if (poems_old_file.exists()){
                    used_file = poems_old_file;
            } else {return false;}
        }

        try {
            processFile(context[0], used_file);

        } catch (IOException e) {
            used_file.delete();
            if (used_file != poems_old_file){
                try {
                    processFile(context[0], poems_old_file);
                } catch (IOException e2){
                    poems_old_file.delete();
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private void processFile(Context context, File poems_file) throws IOException {
        List<Poem> poems;
        PoemParser parser = new PoemParser(new FileInputStream(poems_file), context);
        while (true) {
            poems = parser.getPoems(10);
            if (poems == null) {
                break;
            }
            publishProgress(poems);
        }
    }

    @Override
    protected void onProgressUpdate(List<Poem>... poems_set) {
        activity.addPoems(poems_set[0]);
    }

    @Override
    protected void onPostExecute(Boolean status) {
        activity.presenter.finishedProcessing(status);
    }

    public ParsePoemsTask(MainActivity activity) {
        this.activity = activity;
    }

}
