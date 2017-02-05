package com.almoturg.sprog.view;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.almoturg.sprog.SprogApplication;
import com.google.android.gms.analytics.HitBuilders;
import com.google.firebase.messaging.FirebaseMessaging;

public class NotifyDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        final SprogApplication application = ((SprogApplication) getActivity().getApplication());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Do you want to receive a notification when new poems are available?" +
                "\nThis can be changed later in the overflow menu in the top right.")
                .setTitle("New Poem Notifications");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                FirebaseMessaging.getInstance().subscribeToTopic("PoemUpdates");
                application.getPreferences().setNotifyNew(true);
                getActivity().invalidateOptionsMenu();
                application.getDefaultTracker().send(new HitBuilders.EventBuilder()
                            .setCategory("notificationDialog")
                            .setAction("yes")
                            .build());
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("PoemUpdates");
                application.getPreferences().setNotifyNew(false);
                getActivity().invalidateOptionsMenu();
                application.getDefaultTracker().send(new HitBuilders.EventBuilder()
                            .setCategory("notificationDialog")
                            .setAction("no")
                            .build());
            }
        });

        return builder.create();
    }
}
