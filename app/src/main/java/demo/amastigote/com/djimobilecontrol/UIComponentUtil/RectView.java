package demo.amastigote.com.djimobilecontrol.UIComponentUtil;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class RectView extends View {
    private float x1 = 0.0f;
    private float x2 = 0.0f;
    private float y1 = 0.0f;
    private float y2 = 0.0f;

    public RectView(Context context) {
        super(context);
    }

    public RectView(Context context, float left, float top, float right, float bottom) {
        super(context);
        x1 = left;
        x2 = right;
        y1 = top;
        y2 = bottom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint p = new Paint();
        p.setColor(Color.RED);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.0f);

        canvas.drawRect(x1, y1, x2, y2, p);

    }


    public float getX1() {
        return x1;
    }

    public void setX1(float x1) {
        this.x1 = x1;
    }

    public float getX2() {
        return x2;
    }

    public void setX2(float x2) {
        this.x2 = x2;
    }

    public float getY1() {
        return y1;
    }

    public void setY1(float y1) {
        this.y1 = y1;
    }

    public float getY2() {
        return y2;
    }

    public void setY2(float y2) {
        this.y2 = y2;
    }
}

