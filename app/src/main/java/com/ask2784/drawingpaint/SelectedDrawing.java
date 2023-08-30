package com.ask2784.drawingpaint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

public class SelectedDrawing extends View {
    private Stroke mPath;
    Paint mPaint;

    public SelectedDrawing(Context context, AttributeSet attrs) {
        super(context, attrs);
        editPath();
    }

    private void editPath() {
        mPaint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        mPaint.setColor(mPath.color);
        mPaint.setStrokeWidth(mPath.strokeWidth);

        canvas.drawPath(mPath.path, mPaint);
        invalidate();
    }

    public void setSelectedPath(Stroke modifiedPath) {
        mPath = modifiedPath;
        invalidate();
    }
}
