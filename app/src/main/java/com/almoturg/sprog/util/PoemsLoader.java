package com.almoturg.sprog.util;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;

import com.almoturg.sprog.ui.MainActivity;

import java.io.File;

public class PoemsLoader {
    public static void loadPoems(final MainActivity activity) {
        File poems_file = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "poems.json");
        File poems_old_file = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "poems_old.json");
        if (poems_file.exists()) {
            poems_file.renameTo(poems_old_file);
        }

        String url = "https://almoturg.com/poems.json";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Sprog poems");
        request.setTitle("Sprog");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

        request.setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS,
                "poems.json");

        // get download service and enqueue file
        DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        activity.downloadPoemsComplete = new BroadcastReceiver() {
            public void onReceive(Context ctxt, Intent intent) {

                activity.statusView.setText("poems\nloaded");
                activity.processPoems();
                activity.unregisterReceiver(activity.downloadPoemsComplete);
                activity.downloadPoemsComplete = null;
            }
        };
        activity.registerReceiver(activity.downloadPoemsComplete,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        manager.enqueue(request);
    }
}
