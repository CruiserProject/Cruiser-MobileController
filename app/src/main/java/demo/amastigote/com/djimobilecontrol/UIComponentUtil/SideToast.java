package demo.amastigote.com.djimobilecontrol.UIComponentUtil;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import demo.amastigote.com.djimobilecontrol.R;


public class SideToast {
    private static boolean isShow = false;

    private WindowManager windowManager;
    private WindowManager.LayoutParams params;

    private View toastView;
    private int duration;


    private Timer timer;

    public static final int LENGTH_SHORT = 0;
    public static final int LENGTH_LONG = 1;
    public static final int TYPE_NORMAL = 1;
    public static final int TYPE_WARNING = 2;
    public static final int TYPE_ERROR = 3;

    public SideToast(Context context, String text, int duration) {
        this.duration = duration;
        windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        LinearLayout linearLayout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.toast_view,null);
        TextView toastTextView = (TextView)linearLayout.findViewById(R.id.toast_text);
        toastTextView.setText(text);
        toastView = linearLayout;
        timer = new Timer();

        configureParms();

    }

    public SideToast(Context context, String text, int duration, int viewId) {
        this.duration = duration;
        windowManager = (WindowManager)context.getSystemService(context.WINDOW_SERVICE);
        LinearLayout linearLayout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.toast_view,null);
        switch (viewId){
            case 1:
                linearLayout.setBackgroundResource(R.drawable.toast_shape_normal);
                break;
            case 2:
                linearLayout.setBackgroundResource(R.drawable.toast_shape_warning);
                break;
            case 3:
                linearLayout.setBackgroundResource(R.drawable.toast_shape_error);
        }

        TextView toastTextView = (TextView)linearLayout.findViewById(R.id.toast_text);
        toastTextView.setText(text);
        toastView = linearLayout;
        timer = new Timer();

        configureParms();
    }

    public View getToastView() {
        return toastView;
    }


    public void setToastView(View toastView) {
        this.toastView = toastView;
    }


    public void setGravity(int gravity){
        params.gravity = gravity;
    }


    public void setX(int x){
        params.x = x;
    }

    public void setY(int y){
        params.y = y;
    }



    private void configureParms(){
        params = new WindowManager.LayoutParams();
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.format = PixelFormat.TRANSLUCENT;
        params.windowAnimations = R.style.anim_view;
        params.type = WindowManager.LayoutParams.TYPE_TOAST;
        params.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        params.gravity = Gravity.LEFT | Gravity.TOP;
        params.x = 0;
        params.y = 400;


    }



    public static SideToast makeText(Context context,String text,int duration){
        SideToast sideToast = new SideToast(context,text,duration);
        return sideToast;
    }

    public static SideToast makeText(Context context,String text,int duration,int viewID){
        SideToast sideToast = new SideToast(context,text,duration,viewID);
        return sideToast;
    }

    public void show(){
        if(!isShow){
            isShow = true;
            windowManager.addView(toastView,params);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    windowManager.removeView(toastView);
                    isShow = false;
                }
            },(long)(duration == 1 ? 3500 : 2000));
        }
    }





}
