package com.ask2784.drawingapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GestureDetectorCompat;

import com.google.android.material.slider.RangeSlider;
import java.util.ArrayList;

public class PaintView extends View {
    private static final float TOLERANCE = 5;
    private ArrayList<Stroke> redoPaths = new ArrayList<>();
    private final ArrayList<Stroke> paths = new ArrayList<>();
    private final GestureDetectorCompat gestureDetector;
    private final RectF expandedBounds = new RectF();
    boolean isDraw = true;
    SharedPreferences settings =
            getContext().getSharedPreferences("PAINTVIEW", Context.MODE_PRIVATE);
    private float mX, mY, endX, endY;
    private Paint dPaint;
    private Path mPath;
    private int currentColor;
    private float currentStrokeWidth = 5;
    private Bitmap bitmap;
    private Stroke selectedPath = null;
    private boolean isMove = false;
    private boolean isMoving = false;
    private boolean isSelecting = false;
    private boolean isSelect = false;
    private Path selectionAreaPath;
    private Paint dashedRectanglePaint;
    private DrawMethods method;
    Canvas mCanvas;

    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);

        gestureDetector = new GestureDetectorCompat(getContext(), new CanvasGestureDetector());

        setupDraw();
    }

    private void setupDraw() {
        method = DrawMethods.BRUSH;
        currentStrokeWidth = settings.getFloat("STROKEWIDTH", 5.0f);
        currentColor = settings.getInt("PAINT_COLOR", Color.GREEN);
        dPaint = new Paint();
        dPaint.setColor(currentColor);
        dPaint.setAntiAlias(true);
        dPaint.setAlpha(0x80);
        dPaint.setStrokeWidth(currentStrokeWidth);
        dPaint.setStyle(Paint.Style.STROKE);
        dPaint.setStrokeJoin(Paint.Join.ROUND);
        dPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.drawColor(Color.WHITE);

        drawSelectedPathHighlight(canvas);

        bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(bitmap);
        mCanvas.drawColor(Color.WHITE);

        for (Stroke st : paths) {
            dPaint.setColor(st.color);
            dPaint.setStrokeWidth(st.strokeWidth);
            canvas.drawPath(st.path, dPaint);
            mCanvas.drawPath(st.path, dPaint);
        }

        canvas.restore();
    }

    public void setDrawMethod(DrawMethods method) {
        this.method = method;
    }

    private void drawSelectedPathHighlight(Canvas canvas) {
        if (isSelecting) canvas.drawRect(mX, mY, endX, endY, dashedRectanglePaint);
        dashedRectanglePaint = new Paint();
        dashedRectanglePaint.setColor(Color.BLACK); // Set the color as needed
        dashedRectanglePaint.setStyle(Paint.Style.STROKE);
        dashedRectanglePaint.setStrokeWidth(2);
        dashedRectanglePaint.setPathEffect(new DashPathEffect(new float[] {10, 5}, 0));

        if (selectedPath != null) {
            Region r = new Region();
            RectF selectedPathBounds = getExpandedBounds(selectedPath);
            // selectedPath.path.computeBounds(selectedPathBounds, true);
            r.setPath(
                    selectedPath.path,
                    new Region(
                            (int) (selectedPathBounds.left),
                            (int) (selectedPathBounds.top),
                            (int) (selectedPathBounds.right),
                            (int) (selectedPathBounds.bottom)));

            canvas.drawRect(selectedPathBounds, dashedRectanglePaint);
        }
        invalidate();
    }

    public void startDrawing() {
        if (!isDraw) {
            isDraw = true;
            isMove = false;
            selectedPath = null;
            isSelecting = false;
            isSelect = false;
        }
    }

    private RangeSlider slider;

    public void startSelect(RangeSlider slider) {
        if (!isSelect) {
            isSelect = true;
            isDraw = false;
            selectedPath = null;
            isMove = false;
            this.slider = slider;
        }
    }

    public Stroke getSelectedPath() {
        return selectedPath;
    }

    public void setStrokeWidth(float strokeWidth) {

        if (selectedPath != null) {
            if (selectedPath.position >= 0 && selectedPath.position < paths.size()) {
                selectedPath.strokeWidth = strokeWidth;
                invalidate();
            }
        } else {
            currentStrokeWidth = strokeWidth;
        }
    }

    public boolean isDrawPath() {
        return isDraw;
    }

    public void setStrokeColor(int strokeColor) {
        if (selectedPath != null) {
            if (selectedPath.position >= 0 && selectedPath.position < paths.size()) {
                selectedPath.color = strokeColor;
                invalidate();
            }
        } else {
            currentColor = strokeColor;
        }
    }

    public void undo() {
        if (!paths.isEmpty()) {
            redoPaths.add(paths.remove(paths.size() - 1));
            invalidate();
        }
    }

    public void redo() {
        if (!redoPaths.isEmpty()) {
            paths.add(redoPaths.remove(redoPaths.size() - 1));
            invalidate();
        }
    }

    public void clear() {
        if (!paths.isEmpty()) {
            if (!redoPaths.isEmpty()) {
                redoPaths.clear();
            }
            for (int i = 0; i < paths.size(); i++) {
                redoPaths.add(paths.get(i));
            }
            paths.clear();
            selectedPath = null;
            invalidate();
        }
    }

    public Bitmap exportImage() {
        return bitmap;
    }

    private void startDrawing(float x, float y) {
        if (!redoPaths.isEmpty()) {
            redoPaths.clear();
        }
        mPath = new Path();
        Stroke st = new Stroke(currentColor, currentStrokeWidth, mPath);
        paths.add(st);
        mPath.reset();
        mX = x;
        mY = y;
        mPath.moveTo(x, y);
    }

    private void moveDrawing(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOLERANCE || dy >= TOLERANCE) {
            switch (method) {
                case LINE:
                    mX = x;
                    mY = y;
                    break;
                case BRUSH:
                    mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                    mX = x;
                    mY = y;
                    break;
            }
        }
    }

    private void endDrawing(float x, float y) {

        switch (method) {
            case LINE:
                mPath.lineTo(mX, mY);
                break;
            case BRUSH:
                mPath.lineTo(mX, mY);
                break;
            case RECTANGLE:
                mPath.addRect(mX, mY, x, y, Path.Direction.CW);
                break;
            case SQUARE:
                float dx = Math.abs(x - mX);
                float dy = Math.abs(y - mY);
                float sideLength = Math.min(dx, dy);
                float left = Math.min(mX, x);
                float top = Math.min(mY, y);
                float right = left + sideLength;
                float bottom = top + sideLength;
                mPath.addRect(left, top, right, bottom, Path.Direction.CW);
                break;
            case CIRCLE:
                float radius = (float) Math.sqrt(Math.pow(x - mX, 2) + Math.pow(y - mY, 2));
                mPath.addCircle(mX, mY, radius, Path.Direction.CW);
                break;
            case TRIANGLE:
                // Draw a triangle (you can customize the triangle logic)
                float halfWidth = (mX - x) / 2;
                Path trianglePath = new Path();
                trianglePath.moveTo(mX, mY - halfWidth);
                trianglePath.lineTo(x - halfWidth, y + halfWidth);
                trianglePath.lineTo(x, y + y - halfWidth);
                trianglePath.close();
                // canvas.drawPath(trianglePath, dPaint);
                // mCanvas.drawPath(trianglePath, dPaint);
                mPath.addPath(trianglePath);
                break;
        }
    }

    private RectF getExpandedBounds(Stroke stroke) {
        RectF bounds = new RectF();
        float calculatedMargin = calculateDynamicMargin(stroke);

        stroke.path.computeBounds(bounds, true);
        bounds.inset(-calculatedMargin, -calculatedMargin);
        // Expand the bounds by the specified margin
        bounds.left -= 10;
        bounds.top -= 10;
        bounds.right += 10;
        bounds.bottom += 10;

        return bounds;
    }

    private void startPathMoving(float x, float y) {
        if (selectedPath != null) {
            selectedPath.path.computeBounds(expandedBounds, true);
            if (expandedBounds.contains(x, y)) {
                isMoving = true;
                isSelecting = false;
            }
        }
    }

    private void movePathMoving(float x, float y) {

        if (selectedPath != null && isMoving) {
            float offsetX = x - expandedBounds.centerX();
            float offsetY = y - expandedBounds.centerY();
            Matrix translateMatrix = new Matrix();

            translateMatrix.setTranslate(offsetX, offsetY);
            selectedPath.path.transform(translateMatrix);
            expandedBounds.offset(offsetX, offsetY);
        }
    }

    private void endPathMoving() {
        if (isMoving) {
            isMoving = false;
        }
    }

    private void startSelectingPaths(float x, float y) {
        if (!isSelecting) {
            isSelecting = true;
            mX = x;
            mY = y;
            endX = x;
            endY = y;
            selectionAreaPath = new Path();
            selectionAreaPath.moveTo(x, y);
        }
    }

    private void moveSelectingPaths(float x, float y) {
        if (isSelecting) {
            endX = x;
            endY = y;
            selectionAreaPath.lineTo(x, y);
        }
    }

    private void endSelectingPaths() {
        if (isSelecting) {
            selectPathsInArea(selectionAreaPath);
            selectionAreaPath.reset();
            isSelecting = false;
            isMove = true;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (isDraw) {
                    startDrawing(x, y);
                }
                if (isSelect) {
                    startSelectingPaths(x, y);
                }
                if (isMove) {
                    startPathMoving(x, y);
                }
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDraw) {
                    moveDrawing(x, y);
                }
                if (isSelect) {
                    moveSelectingPaths(x, y);
                }
                if (isMove) {
                    movePathMoving(x, y);
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (isDraw) {
                    endDrawing(x, y);
                }
                if (isSelect) {
                    endSelectingPaths();
                }
                if (isMove) {
                    endPathMoving();
                }
                invalidate();
                break;
            default:
                return false;
        }

        gestureDetector.onTouchEvent(event);
        return true;
    }

    private void selectPathsInArea(Path selectionArea) {
        if (selectedPath != null) selectedPath = null;
        for (int i = 0; i < paths.size(); i++) {
            Stroke stroke = paths.get(i);
            // Simplified check for intersection with the selection area
            if (isPathIntersectingArea(stroke, selectionArea)) {
                stroke.position = i;
                selectedPath = stroke;
                slider.setValues(selectedPath.strokeWidth);
            }
        }
    }

    private boolean isPathIntersectingArea(Stroke stroke, Path selectionArea) {
        // Simplified check for intersection by comparing bounding boxes
        RectF pathBounds = getExpandedBounds(stroke);
        RectF areaBounds = new RectF();
        Region r1 = new Region();
        r1.setPath(
                stroke.path,
                new Region(
                        (int) (pathBounds.left),
                        (int) (pathBounds.top),
                        (int) (pathBounds.right),
                        (int) (pathBounds.bottom)));
        Region r2 = new Region();
        r2.setPath(
                selectionArea,
                new Region(
                        (int) (areaBounds.left),
                        (int) (areaBounds.top),
                        (int) (areaBounds.right),
                        (int) (areaBounds.bottom)));
        stroke.path.computeBounds(pathBounds, true);
        selectionArea.computeBounds(areaBounds, true);
        return RectF.intersects(pathBounds, areaBounds);
    }

    private float calculateDynamicMargin(Stroke stroke) {
        // Calculate the size of the path
        RectF bounds = new RectF();
        stroke.path.computeBounds(bounds, true);
        float pathSize = Math.max(bounds.width(), bounds.height());

        // Define a threshold and corresponding margin increase
        float sizeThreshold = 10; // Adjust as needed
        float marginIncrease = 10; // Adjust as needed

        // Calculate dynamic margin
        if (pathSize < sizeThreshold) {
            return stroke.strokeWidth / 2 + marginIncrease;
        } else {
            return stroke.strokeWidth / 2;
        }
    }

    class CanvasGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(@NonNull MotionEvent event) {
            if (!isDraw) {
                if (selectedPath != null) {
                    isMoving = false;
                    int p = selectedPath.position;
                    if (p >= 0 && p < paths.size()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle("Delete");
                        builder.setMessage("Do You want to delete selected Path?");
                        builder.setCancelable(false);
                        builder.setPositiveButton(
                                "Delete",
                                (dialog, id) -> {
                                    paths.remove(p);
                                    selectedPath = null;
                                    invalidate();
                                });
                        builder.setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                }
                super.onLongPress(event);
            }
        }
    }
}
