package com.sourcey.linachatbot;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

/**
 * Created by amrezzat on 4/6/2017.
 */

public class CustomToast {
    CustomToast(Context context, String text, boolean chat) {
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        View toastView = toast.getView();
        if(chat) {
            toastView.setBackgroundResource(R.drawable.toast_shape_chat);
        }
        else {
            toastView.setBackgroundResource(R.drawable.toast_shape_login);
        }
        toast.setView(toastView);
        toast.show();
    }
}
