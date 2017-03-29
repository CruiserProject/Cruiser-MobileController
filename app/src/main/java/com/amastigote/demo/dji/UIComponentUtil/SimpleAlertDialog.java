package com.amastigote.demo.dji.UIComponentUtil;

import android.app.AlertDialog;
import android.content.Context;

/**
 * Created by hwding on 3/29/17.
 */

public class SimpleAlertDialog {
    public static void show(Context context, boolean cancelable, String title, String message, SimpleDialogButton button) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(cancelable)
                .setPositiveButton(button.getText(), button.getOnClickListener())
                .show();
    }
}
