package demo.amastigote.com.djimobilecontrol.ConverterUtil;


import android.content.Context;
import android.view.WindowManager;

public class ScreenSizeConverter {
    private WindowManager windowManager;
    private int height;
    private int width;

    public ScreenSizeConverter(Context context) {
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

//    public static int convertX(){
//
//    }
}
