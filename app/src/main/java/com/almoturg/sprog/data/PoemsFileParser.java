package com.almoturg.sprog.data;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import com.almoturg.sprog.model.Poem;
import com.almoturg.sprog.model.SprogDbHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class PoemsFileParser {
    private Context context;
    private ParsePoemsTask task;

    public interface ParsePoemsCallbackInterface {
        void addPoems(List<Poem> poems);
        void finishedProcessing(boolean status);
    }

    public PoemsFileParser(Context context){
        this.context = context;
    }

    public void parsePoems(ParsePoemsCallbackInterface callback,
                           SprogDbHelper dbHelper, MarkdownConverter markdownConverter){
        task = new ParsePoemsTask(callback);
        task.execute(new ParsePoemsTaskParams(context, dbHelper, markdownConverter));
    }

    public void cancelParsing() {
        if (task != null) {
            task.cancel(true);
        }
    }

    private static class ParsePoemsTaskParams {
        Context context;
        MarkdownConverter markdownConverter;
        SprogDbHelper dbHelper;

        ParsePoemsTaskParams(Context context, SprogDbHelper dbHelper,
                             MarkdownConverter markdownConverter) {
            this.context = context;
            this.markdownConverter = markdownConverter;
            this.dbHelper = dbHelper;
        }
    }

    public class ParsePoemsTask extends AsyncTask<ParsePoemsTaskParams, List<Poem>, Boolean> {
        ParsePoemsCallbackInterface callback;

        @Override
        protected Boolean doInBackground(ParsePoemsTaskParams... params) {
            File poems_file = new File(params[0].context
                    .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "poems.json");
            File poems_old_file = new File(params[0].context
                    .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "poems_old.json");
            File used_file = poems_file;
            if (!poems_file.exists()) {
                if (poems_old_file.exists()) {
                    used_file = poems_old_file;
                } else {
                    return false;
                }
            }

            try {
                processFile(params[0], used_file);

            } catch (IOException e) {
                used_file.delete();
                if (used_file != poems_old_file) {
                    try {
                        processFile(params[0], poems_old_file);
                    } catch (IOException e2) {
                        poems_old_file.delete();
                        return false;
                    }
                } else {
                    return false;
                }
            }
            return true;
        }

        private void processFile(ParsePoemsTaskParams params, File poems_file) throws IOException {
            List<Poem> poems;
            PoemParser parser = new PoemParser(new FileInputStream(poems_file),
                    params.dbHelper, params.markdownConverter);
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
            callback.addPoems(poems_set[0]);
        }

        @Override
        protected void onPostExecute(Boolean status) {
            callback.finishedProcessing(status);
        }

        public ParsePoemsTask(ParsePoemsCallbackInterface callback) {
            this.callback = callback;
        }

    }
}
