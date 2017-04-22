package demo.amastigote.com.djimobilecontrol.UIComponentUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;

import dji.common.error.DJIError;

/**
 * Created by hwding on 3/29/17.
 */

public class SimpleAlertDialog {
    public static void show(final Context context, final boolean cancelable, final String title, final String message, final SimpleDialogButton button) {
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(context)
                        .setTitle(title)
                        .setMessage(message)
                        .setCancelable(cancelable)
                        .setPositiveButton(button.getText(), button.getOnClickListener())
                        .show();
            }
        });
    }

    public static void showException(final Context context, final Exception e) {
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                show(context, false, "Exception", e.getMessage(), new SimpleDialogButton("ok", null));
            }
        });
    }

    public static void showDJIError(final Context context, final DJIError djiError) {
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                show(context, false, "DJIError", djiError.getDescription(), new SimpleDialogButton("ok", null));
            }
        });
    }
}
