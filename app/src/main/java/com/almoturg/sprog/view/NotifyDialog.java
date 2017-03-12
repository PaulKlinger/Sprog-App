package com.almoturg.sprog.view;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

public class NotifyDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Do you want to receive a notification when new poems are available?" +
                "\nThis can be changed later in the overflow menu in the top right.")
                .setTitle("New Poem Notifications");
        builder.setPositiveButton("Yes", (dialog, id) -> MainActivity.presenter.notifyDialogYes());
        builder.setNegativeButton("No", (dialog, id) -> MainActivity.presenter.notifyDialogNo());

        return builder.create();
    }
}
