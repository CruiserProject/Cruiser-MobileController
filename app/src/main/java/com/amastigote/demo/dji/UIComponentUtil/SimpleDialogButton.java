package com.amastigote.demo.dji.UIComponentUtil;

import android.content.DialogInterface;

/**
 * Created by hwding on 3/29/17.
 */

public class SimpleDialogButton {
    private String text;
    private DialogInterface.OnClickListener onClickListener;

    public SimpleDialogButton(String text, DialogInterface.OnClickListener onClickListener) {
        this.text = text;
        this.onClickListener = onClickListener;
    }

    String getText() {
        return text;
    }

    DialogInterface.OnClickListener getOnClickListener() {
        return onClickListener;
    }
}
