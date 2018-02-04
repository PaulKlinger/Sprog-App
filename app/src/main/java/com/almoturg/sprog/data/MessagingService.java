package com.almoturg.sprog.data;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.almoturg.sprog.R;
import com.almoturg.sprog.SprogApplication;
import com.almoturg.sprog.model.PreferencesRepository;
import com.almoturg.sprog.view.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;

public class MessagingService extends FirebaseMessagingService {
    public static final int NEW_POEMS_NOTIFICATION_ID = 1;

    public MessagingService() {
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            Log.d("SPROG", "Message data payload: " + remoteMessage.getData());
            double[] last_update_timestamps;
            try {
                JSONArray jsonArray = new JSONArray(remoteMessage.getData().get("last_poems"));
                last_update_timestamps = new double[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++) {
                    last_update_timestamps[i] = jsonArray.getDouble(i);
                }
            } catch (JSONException e) {
                return;
            }
            PreferencesRepository prefs = ((SprogApplication) getApplication()).getPreferences();

            long last_poem_time = prefs.getLastPoemTime();
            int new_poems_count = getNewPoemCount(last_update_timestamps, last_poem_time);

            prefs.setLastFCMTimestamp(remoteMessage.getSentTime());
            if (new_poems_count > 0) {
                prefs.setUpdateNext(true);
                if (prefs.getNotifyNew()) {
                    createNotification(new_poems_count);
                }
            }
        }
    }

    private int getNewPoemCount(double[] last_update_timestamps, long last_poem_time) {

        int new_poems_count = 0;
        for (double last_update_timestamp : last_update_timestamps) {
            if (last_update_timestamp * 1000 >
                    last_poem_time) {
                new_poems_count++;
            }
        }
        return new_poems_count;
    }

    private void createNotification(int new_poems_count) {
        String notification_title;
        if (new_poems_count > 10) {
            notification_title = "More than 10 new poems available!";
        } else {
            notification_title = String.format("%d new poem%s available!",
                    new_poems_count, new_poems_count > 1 ? "s" : "");
        }
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, "poem_channel")
                        .setSmallIcon(R.drawable.ic_stat_fleuronwhite)
                        .setContentTitle(notification_title)
                        .setContentText("Tap to view.")
                        .setColor(ContextCompat.getColor(this, R.color.colorLauncherIcon))
                        .setAutoCancel(false)
                        .setLights(Color.GREEN, 400, 3000);
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.putExtra("UPDATE", true);
        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(this);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(mainIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        // If a previous notification is still visible it is updated automatically
        mNotificationManager.notify(NEW_POEMS_NOTIFICATION_ID, mBuilder.build());

    }
}
