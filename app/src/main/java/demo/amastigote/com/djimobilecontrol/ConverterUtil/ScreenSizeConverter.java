package demo.amastigote.com.djimobilecontrol.ConverterUtil;


import android.content.Context;
import android.graphics.Point;
import android.view.WindowManager;

public class ScreenSizeConverter {
    private Point point;
    private float videoTextureFrameLayout_height;
    private float videoTextureFrameLayout_width;
    private float screen_density;
    private final int statusbar_dp = 30;

    public ScreenSizeConverter(Context context) {
        point = new Point();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealSize(point);
        videoTextureFrameLayout_height = point.y;
        videoTextureFrameLayout_width = point.x;
        screen_density = context.getResources().getDisplayMetrics().density;

        videoTextureFrameLayout_height -= statusbar_dp * screen_density;
    }

    public byte convertX2XPercent(float x) {
        return (byte) ((int) (x / videoTextureFrameLayout_width * 100 + 0.5f));
    }

    public byte convertY2YPercent(float y) {
        return (byte) ((int) (y / videoTextureFrameLayout_height * 100 + 0.5f));
    }

    public float convertXPercent2X(byte x_perc) {
        return (float) (x_perc / 100.0 * videoTextureFrameLayout_width);
    }

    public float convertYPercent2Y(byte y_perc) {
        return (float) (y_perc / 100.0 * videoTextureFrameLayout_height);
    }


}
