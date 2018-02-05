package com.almoturg.sprog.data;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import com.almoturg.sprog.presenter.MainPresenter;

import java.io.File;

public class PoemsLoader {
    public static long downloadID;

    public static BroadcastReceiver receiver;

    public static void loadPoems(final Context context, final MainPresenter presenter) {
        File poems_file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "poems.json");
        File poems_old_file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "poems_old.json");
        if (poems_file.exists()) {
            poems_file.renameTo(poems_old_file);
        }

        String url = "https://almoturg.com/poems.json";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Sprog poems");
        request.setTitle("Sprog");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setVisibleInDownloadsUi(false);

        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS,
                "poems.json");

        // get download service and enqueue file
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        receiver = new BroadcastReceiver() {
            public void onReceive(Context ctxt, Intent intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != downloadID){return;}
                context.unregisterReceiver(PoemsLoader.receiver);
                PoemsLoader.receiver = null;
                presenter.downloadComplete();
            }
        };
        context.registerReceiver(receiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        downloadID = manager.enqueue(request);
    }

    public static void cancelAllDownloads(Context context){
        if (receiver != null) {
            context.unregisterReceiver(receiver);
            receiver = null;
        }

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterByStatus(
                DownloadManager.STATUS_FAILED|DownloadManager.STATUS_PAUSED|
                DownloadManager.STATUS_PENDING|DownloadManager.STATUS_RUNNING);
        Cursor cur = manager.query(query);
        while (cur.moveToNext()){
            manager.remove(cur.getLong(cur.getColumnIndex(DownloadManager.COLUMN_ID)));
        }
        cur.close();
    }
}
