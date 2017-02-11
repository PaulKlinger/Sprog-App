package com.almoturg.sprog.view;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.almoturg.sprog.presenter.MainPresenter;


public class ConfirmClearReadDialog extends DialogFragment {
    MainPresenter presenter;

    public void setPresenter(MainPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Do you really want to set all poems to unread?\n" +
                "This can't be undone.")
                .setTitle("");
        builder.setPositiveButton("Yes", (dialog, id) -> presenter.clearReadPoems());
        builder.setNegativeButton("No", (dialog, id) -> {});

        return builder.create();
    }
}
