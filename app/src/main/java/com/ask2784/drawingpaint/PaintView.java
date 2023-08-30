package com.ask2784.drawingpaint;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GestureDetectorCompat;

import com.ask2784.drawingpaint.databinding.ModifyStrokeBinding;
import com.google.android.material.slider.RangeSlider;

import yuku.ambilwarna.AmbilWarnaDialog;

import java.util.ArrayList;

public class PaintView extends View {
    private static final float TOLERANCE = 5;
    private float mX, mY, endX, endY;
    private Paint dPaint;
    private Path mPath;
    private int currentColor;
    private ArrayList<Stroke> paths = new ArrayList<>();
    private final ArrayList<Stroke> redoPaths = new ArrayList<>();
    private float currentStrokeWidth = 5;
    private Bitmap bitmap;
    private GestureDetectorCompat gestureDetector;
    private Stroke selectedPath = null;
    boolean isDraw = true;
    private Canvas sCanvas;
    private boolean isMove = false;
    private boolean isMoving = false;
    private boolean isSelecting = false;
    private boolean isSelect = false;
    private Path selectionAreaPath;
    private Paint dashedRectanglePaint;
    private RectF expandedBounds = new RectF();

    SharedPreferences settings =
            getContext().getSharedPreferences("PAINTVIEW", Context.MODE_PRIVATE);

    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);

        gestureDetector = new GestureDetectorCompat(getContext(), new CanvasGestureDetector());

        setupDraw();
    }

    private void setupDraw() {

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
        sCanvas = canvas;
        canvas.save();
        canvas.drawColor(Color.WHITE);
        bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas mCanvas = new Canvas(bitmap);
        mCanvas.drawColor(Color.WHITE);
        for (Stroke st : paths) {
            dPaint.setColor(st.color);
            dPaint.setStrokeWidth(st.strokeWidth);
            canvas.drawPath(st.path, dPaint);
            mCanvas.drawPath(st.path, dPaint);
        }
        if (isSelecting) canvas.drawRect(mX, mY, endX, endY, dashedRectanglePaint);
        drawSelectedPathHighlight(canvas);
        canvas.restore();
    }

    //    private SelectedPath findPathAtPoints(float x, float y) {
    //        if (!paths.isEmpty()) {
    //
    //            for (int i = 0; i < paths.size(); i++) {
    //                Stroke stroke = paths.get(i);
    //                Path p = stroke.path;
    //                if (selectedPathContainPoints(p, x, y)) {
    //                    return new SelectedPath(stroke, i);
    //                }
    //            }
    //        }
    //        return null;
    //    }
    //
    //    private boolean selectedPathContainPoints(Path path, float x, float y) {
    //        Region r = new Region();
    //        RectF bounds = new RectF();
    //        path.computeBounds(bounds, true);
    //        float touchThreshold = 50.0f;
    //        bounds.inset(-touchThreshold, -touchThreshold);
    //        r.setPath(
    //                path,
    //                new Region(
    //                        (int) bounds.left,
    //                        (int) bounds.top,
    //                        (int) bounds.right,
    //                        (int) bounds.bottom));
    //        return r.contains((int) x, (int) y);
    //    }

    //    private void highlightSelectedPath(MotionEvent event) {
    //        if (!isDraw) {
    //            SelectedPath selected = findPathAtPoints(event.getX(0), event.getY(0));
    //            if (selected != null) {
    //                selectedPath = selected.getStroke();
    //                if (selectedPathContainPoints(selectedPath.path, event.getX(0),
    // event.getY(0))) {
    //                    isMove = true;
    //                    invalidate();
    //                } else {
    //                    isMove = false;
    //                }
    //            }
    //        }
    //    }

    public void drawSelectedPathHighlight(Canvas canvas) {
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

    public void startSelect() {
        if (!isSelect) {
            isSelect = true;
            isDraw = false;
            selectedPath = null;
            isMove = false;
        }
    }

    //    int changedColor;
    //    RangeSlider changeStroke, resizePath;
    //    float changeStrokeWidth;
    //
    //    private void showPathOptionsDialog(Stroke stroke) {
    //        ModifyStrokeBinding strokeBinding =
    //                ModifyStrokeBinding.inflate(LayoutInflater.from(getContext()));
    //        changedColor = stroke.color;
    //        changeStrokeWidth = stroke.strokeWidth;
    //        // SelectedDrawing selectedDraw = strokeBinding.selectedDraw;
    //        // selectedDraw.setSelectedPath(path.getStroke());
    //        if (selectedPath != null) {
    //            changeStroke = strokeBinding.changeSize;
    //            resizePath = strokeBinding.resize;
    //            changeStroke.setValueFrom(0.0f);
    //            changeStroke.setValueTo(100.0f);
    //            changeStroke.setValues(changeStrokeWidth);
    //            changeStroke.addOnChangeListener(
    //                    new RangeSlider.OnChangeListener() {
    //                        @Override
    //                        public void onValueChange(
    //                                RangeSlider slider, float value, boolean userValue) {
    //                            changeStrokeWidth = value;
    //                        }
    //                    });
    //            strokeBinding.changeColor.setBackgroundColor(changedColor);
    //            strokeBinding.changeColor.setOnClickListener(
    //                    v -> {
    //                        AmbilWarnaDialog colorDailog =
    //                                new AmbilWarnaDialog(
    //                                        getContext(),
    //                                        changedColor,
    //                                        new AmbilWarnaDialog.OnAmbilWarnaListener() {
    //                                            @Override
    //                                            public void onOk(AmbilWarnaDialog dialog, int
    // color) {
    //                                                changedColor = color;
    //                                                strokeBinding.changeColor.setBackgroundColor(
    //                                                        changedColor);
    //                                            }
    //
    //                                            @Override
    //                                            public void onCancel(AmbilWarnaDialog dialog) {
    //                                                return;
    //                                            }
    //                                        });
    //                        colorDailog.show();
    //                    });
    //            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    //            builder.setView(strokeBinding.getRoot());
    //            builder.setCancelable(false)
    //                    .setPositiveButton(
    //                            "Done",
    //                            new DialogInterface.OnClickListener() {
    //                                public void onClick(DialogInterface dialog, int id) {
    //                                    stroke.color = changedColor;
    //                                    stroke.strokeWidth = changeStrokeWidth;
    //                                    paths.set(stroke.position, stroke);
    //                                    paths.set(stroke.position, stroke);
    //                                    invalidate();
    //                                }
    //                            })
    //                    .setNegativeButton(
    //                            "Cancel",
    //                            new DialogInterface.OnClickListener() {
    //                                public void onClick(DialogInterface dialog, int id) {
    //                                    dialog.dismiss();
    //                                }
    //                            })
    //                    .setNeutralButton(
    //                            "Delete",
    //                            new DialogInterface.OnClickListener() {
    //                                @Override
    //                                public void onClick(DialogInterface dialog, int id) {
    //                                    paths.remove(stroke.position);
    //                                    selectedPath = null;
    //                                    invalidate();
    //                                }
    //                            });
    //
    //            AlertDialog dialog = builder.create();
    //            dialog.show();
    //        }
    //    }

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
        if (isDraw && !isSelect) {
            return true;
        }
        return false;
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
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void moveDrawing(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOLERANCE || dy >= TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void endDrawing() {
        mPath.lineTo(mX, mY);
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
                    endDrawing();
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
        public void onLongPress(MotionEvent event) {
            if (!isDraw) {
                if (selectedPath != null) {
                    int p = selectedPath.position;
                    if (p >= 0 && p < paths.size()) {
                        // showPathOptionsDialog(selectedPath);
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle("Delete");
                        builder.setMessage("Do You want to delete selected Path?");
                        builder.setPositiveButton(
                                "Delete",
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        paths.remove(p);
                                        selectedPath = null;
                                        invalidate();
                                    }
                                });
                        builder.setNegativeButton("Cancel", null);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                }
                super.onLongPress(event);
            }
        }
    }
}
