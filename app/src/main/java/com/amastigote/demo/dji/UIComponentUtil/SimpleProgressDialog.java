package com.amastigote.demo.dji.UIComponentUtil;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * Created by hwding on 3/28/17.
 *
 * Provide a simpler way to create and manipulate ProgressDialogs
 */

public class SimpleProgressDialog {
    private ProgressDialog progressDialog;
    private Context context;
    private String message;

    public SimpleProgressDialog(Context context, String message) {
        this.context = context;
        this.message = message;
    }

    public void show() {
        progressDialog = ProgressDialog.show(context, null, message);
    }

    public void dismiss() {
        progressDialog.dismiss();
    }
}
