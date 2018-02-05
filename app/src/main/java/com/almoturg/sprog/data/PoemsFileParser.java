package com.almoturg.sprog.data;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import com.almoturg.sprog.model.Poem;
import com.almoturg.sprog.model.SprogDbHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class PoemsFileParser {
    private Context context;
    private ParsePoemsTask task;

    public interface ParsePoemsCallbackInterface {
        void addPoems(List<Poem> poems);

        void finishedProcessing(boolean status);
    }

    public PoemsFileParser(Context context) {
        this.context = context;
    }

    public void parsePoems(ParsePoemsCallbackInterface callback,
                           SprogDbHelper dbHelper, MarkdownConverter markdownConverter) {
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

            long min_partial_timestamp = handleFile(params[0],
                    PoemsLoader.UpdateType.PARTIAL, Long.MAX_VALUE);
            // we only want to take poems from the full file if they are older
            // than the oldest one in the partial file (in case they were subsequently deleted)
            if (min_partial_timestamp == -1) { // parsing of partial file failed
                min_partial_timestamp = Long.MAX_VALUE; // so take all poems from the
            }
            return -1 != handleFile(params[0], PoemsLoader.UpdateType.FULL, min_partial_timestamp);
        }

        private long handleFile(ParsePoemsTaskParams params, PoemsLoader.UpdateType updateType,
                                long max_timestamp) {
            File poems_file = new File(params.context
                    .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    PoemsLoader.getFilename(updateType, PoemsLoader.FileType.CURRENT));
            File poems_old_file = new File(params.context
                    .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    PoemsLoader.getFilename(updateType, PoemsLoader.FileType.PREV));
            File used_file = poems_file;
            if (!poems_file.exists()) {
                if (poems_old_file.exists()) {
                    used_file = poems_old_file;
                } else {
                    return -1;
                }
            }

            try {
                return processFile(params, used_file, max_timestamp);

            } catch (IOException e) {
                used_file.delete();
                if (used_file != poems_old_file) {
                    try {
                        return processFile(params, poems_old_file, max_timestamp);
                    } catch (IOException e2) {
                        poems_old_file.delete();
                        return -1;
                    }
                } else {
                    return -1;
                }
            }
        }

        private long processFile(ParsePoemsTaskParams params, File poems_file,
                                 long max_timestamp) throws IOException {
            long min_timestamp = Long.MAX_VALUE;
            List<Poem> poems;
            PoemParser parser = new PoemParser(new GZIPInputStream(new FileInputStream(poems_file)),
                    params.dbHelper, params.markdownConverter);
            while (true) {
                poems = parser.getPoems(10);
                if (poems == null) {
                    break;
                }
                List<Poem> newPoems = new ArrayList<>();
                for (Poem p : poems) {
                    if (p.timestamp_long < max_timestamp) {
                        newPoems.add(p);
                    }
                    if (p.timestamp_long < min_timestamp) {
                        min_timestamp = p.timestamp_long;
                    }
                }
                publishProgress(newPoems);
            }
            return min_timestamp;
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
