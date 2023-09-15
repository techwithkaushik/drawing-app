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

import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;

import com.google.android.material.slider.RangeSlider;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class PaintView extends View {
    private static final float TOLERANCE = 5;
    private ArrayList<SerializePaint> redoPaint = new ArrayList<>();
    private ArrayList<SerializePaint> paints = new ArrayList<>();
    // private ArrayList<SerializePath> redoPaths = new ArrayList<>();
    // private ArrayList<SerializePath> paths = new ArrayList<>();

    private ArrayList<Path> redoPaths = new ArrayList<>();
    private ArrayList<Path> paths = new ArrayList<>();
    private final GestureDetectorCompat gestureDetector;
    private final RectF expandedBounds = new RectF();
    private boolean isDraw = true;
    private SharedPreferences settings =
            getContext().getSharedPreferences("paintView", Context.MODE_PRIVATE);
    private float startX, startY, endX, endY;
    private Paint dPaint;
    // private SerializePath mPath, selectionAreaPath;
    private Path mPath, selectionAreaPath;
    private int currentColor;
    private float currentStrokeWidth = 5;
    private Bitmap bitmap;
    private DrawingData selectedDrawing = null;
    private boolean isMove = false;

    private boolean isMoving = false;
    private boolean isSelecting = false;
    private boolean isSelect = false;
    private Paint dashedRectanglePaint;
    private ShapeType shapeType;
    private Canvas mCanvas;
    private boolean isShapeDrawing = false;
    private Paint.Cap cap = Paint.Cap.ROUND;
    private Paint.Join join = Paint.Join.ROUND;
    private Paint.Style style = Paint.Style.STROKE;

    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        gestureDetector = new GestureDetectorCompat(getContext(), new CanvasGestureDetector());
        setupDraw();
    }

    private void setupDraw() {
        shapeType = ShapeType.BRUSH;
        currentStrokeWidth = settings.getFloat("strokeWidth", 5.0f);
        currentColor = settings.getInt("paintColor", Color.GREEN);
        dPaint = new Paint();
        dPaint.setColor(currentColor);
        dPaint.setAntiAlias(true);
        dPaint.setAlpha(0x80);
        dPaint.setStrokeWidth(currentStrokeWidth);
        dPaint.setStyle(style);
        dPaint.setStrokeJoin(join);
        dPaint.setStrokeCap(cap);
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
        for (int i = 0; i < paints.size(); i++) {
            dPaint.setColor(paints.get(i).getColor());
            dPaint.setStrokeWidth(paints.get(i).getStrokeWidth());
            dPaint.setStrokeJoin(paints.get(i).getJoin());
            dPaint.setStrokeCap(paints.get(i).getCap());
            dPaint.setStyle(paints.get(i).getStyle());
            canvas.drawPath(paths.get(i), dPaint);
            mCanvas.drawPath(paths.get(i), dPaint);
        }

        if (isShapeDrawing) {
            switch (shapeType) {
                case LINE:
                    drawLine(canvas);
                    break;
                case RECTANGLE:
                    drawRectangle(canvas);
                    break;
                case SQUARE:
                    drawSquare(canvas);
                    break;
                case CIRCLE:
                    drawCircle(canvas);
                    break;
                case TRIANGLE:
                    drawTriangle(canvas);
                    break;
            }
        }

        canvas.restore();
    }

    public void setDrawMethod(ShapeType shapeType) {
        this.shapeType = shapeType;
        switch (shapeType) {
            case LINE:
            case BRUSH:
            case CIRCLE:
                join = Paint.Join.ROUND;
                cap = Paint.Cap.ROUND;
                style = Paint.Style.STROKE;
                break;
            case RECTANGLE:
            case SQUARE:
            case TRIANGLE:
                join = Paint.Join.MITER;
                cap = Paint.Cap.BUTT;
                style = Paint.Style.STROKE;
                break;
        }
    }

    public DrawingData saveProject() {
        DrawingData data = new DrawingData();
        data.setPaintList(paints);
        data.setPathList(paths);
        return data;
    }

    public void loadProject(DrawingData drawingData) {
        if (!drawingData.getPaintList().isEmpty() && !drawingData.getPathList().isEmpty()) {
            paints = drawingData.getPaintList();
            paths = drawingData.getPathList();
            invalidate();
        } else {
            Toast.makeText(getContext(), "Project is Empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawLine(Canvas canvas) {
        canvas.drawLine(startX, startY, endX, endY, dPaint);
        invalidate();
    }

    private void drawRectangle(Canvas canvas) {
        canvas.drawRect(startX, startY, endX, endY, dPaint);
        invalidate();
    }

    private void drawSquare(Canvas canvas) {
        float dx = Math.abs(endX - startX);
        float dy = Math.abs(endY - startY);
        float sideLength = Math.min(dx, dy);
        float left = Math.min(startX, endX);
        float top = Math.min(startY, endY);
        float right = left + sideLength;
        float bottom = top + sideLength;
        canvas.drawRect(left, top, right, bottom, dPaint);
        invalidate();
    }

    private void drawCircle(Canvas canvas) {
        float radius = (float) Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
        canvas.drawCircle(endX, endY, radius, dPaint);
        invalidate();
    }

    private void drawTriangle(Canvas canvas) {
        Path p = new Path();
        p.moveTo(startX, startY);
        p.lineTo(endX, endY);
        p.lineTo(startX + (startX - endX), endY);
        p.close();
        canvas.drawPath(p, dPaint);
    }

    private void drawSelectedPathHighlight(Canvas canvas) {
        if (isSelecting) canvas.drawRect(startX, startY, endX, endY, dashedRectanglePaint);
        dashedRectanglePaint = new Paint();
        dashedRectanglePaint.setColor(Color.BLACK); // Set the color as needed
        dashedRectanglePaint.setStyle(Paint.Style.STROKE);
        dashedRectanglePaint.setStrokeWidth(2);
        dashedRectanglePaint.setPathEffect(new DashPathEffect(new float[] {10, 5}, 0));

        if (selectedDrawing != null) {
            Region r = new Region();
            RectF selectedPathBounds = getExpandedBounds(selectedDrawing);
            // selectedPath.path.computeBounds(selectedPathBounds, true);
            r.setPath(
                    selectedDrawing.getPath(),
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
            selectedDrawing = null;
            isSelecting = false;
            isSelect = false;
        }
    }

    private RangeSlider slider;

    public void startSelect(RangeSlider slider) {
        if (!isSelect) {
            isSelect = true;
            isDraw = false;
            selectedDrawing = null;
            isMove = false;
            this.slider = slider;
        }
    }

    public DrawingData getSelectedDrawing() {
        return selectedDrawing;
    }

    public void setStrokeWidth(float strokeWidth) {

        if (selectedDrawing != null) {
            if (selectedDrawing.getPaint().getPosition() >= 0
                    && selectedDrawing.getPaint().getPosition() < paths.size()) {
                selectedDrawing.getPaint().setStrokeWidth(strokeWidth);
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
        if (selectedDrawing != null) {
            if (selectedDrawing.getPaint().getPosition() >= 0
                    && selectedDrawing.getPaint().getPosition() < paints.size()) {
                selectedDrawing.getPaint().setColor(strokeColor);
                invalidate();
            }
        } else {
            currentColor = strokeColor;
        }
    }

    public void undo() {
        if (!paths.isEmpty() && !paints.isEmpty()) {
            redoPaint.add(paints.remove(paints.size() - 1));
            redoPaths.add(paths.remove(paths.size() - 1));
            invalidate();
        }
    }

    public void redo() {
        if (!redoPaths.isEmpty() && !redoPaint.isEmpty()) {
            paints.add(redoPaint.remove(redoPaint.size() - 1));
            paths.add(redoPaths.remove(redoPaths.size() - 1));
            invalidate();
        }
    }

    public void clear() {
        if (!paths.isEmpty() && !paints.isEmpty()) {
            if (!redoPaths.isEmpty() && !redoPaint.isEmpty()) {
                redoPaint.clear();
                redoPaths.clear();
            }
            for (int i = 0; i < paths.size(); i++) {
                redoPaint.add(paints.get(i));
                redoPaths.add(paths.get(i));
            }
            paints.clear();
            paths.clear();
            selectedDrawing = null;
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
        SerializePaint st =
                new SerializePaint(currentColor, currentStrokeWidth, shapeType, cap, join, style);
        paints.add(st);
        paths.add(mPath);
        mPath.reset();
        mPath.moveTo(x, y);
        startX = x;
        startY = y;
        endX = x;
        endY = y;
    }

    private void moveDrawing(float x, float y) {
        float dx = Math.abs(x - startX);
        float dy = Math.abs(y - startY);
        if (dx >= TOLERANCE || dy >= TOLERANCE) {
            if (shapeType == ShapeType.BRUSH) {
                mPath.quadTo(endX, endY, (x + endX) / 2, (y + endY) / 2);
            } else {
                isShapeDrawing = true;
            }
            endX = x;
            endY = y;
        }
    }

    private void endDrawing(float x, float y) {
        if (shapeType == ShapeType.BRUSH) {
            mPath.lineTo(endX, endY);
        } else {
            switch (shapeType) {
                case LINE:
                    mPath.lineTo(endX, endY);
                    break;
                case BRUSH:
                    break;
                case RECTANGLE:
                    mPath.addRect(startX, startY, x, y, Path.Direction.CW);
                    mPath.addRect(x, y, startX, startY, Path.Direction.CW);
                    mPath.addRect(x, startY, startX, y, Path.Direction.CW);
                    mPath.addRect(startX, y, x, startY, Path.Direction.CW);
                    break;
                case SQUARE:
                    float dx = Math.abs(x - startX);
                    float dy = Math.abs(y - startY);
                    float sideLength = Math.min(dx, dy);
                    float left = Math.min(startX, x);
                    float top = Math.min(startY, y);
                    float right = left + sideLength;
                    float bottom = top + sideLength;
                    mPath.addRect(left, top, right, bottom, Path.Direction.CW);
                    break;
                case CIRCLE:
                    float radius =
                            (float) Math.sqrt(Math.pow(x - startX, 2) + Math.pow(y - startY, 2));
                    mPath.addCircle(endX, endY, radius, Path.Direction.CW);
                    break;
                case TRIANGLE:
                    // Draw a triangle (you can customize the triangle logic)
                    Path trianglePath = new Path();
                    trianglePath.moveTo(startX, startY);
                    trianglePath.lineTo(endX, endY);
                    trianglePath.lineTo(startX + (startX - endX), endY);
                    trianglePath.close();
                    mPath.addPath(trianglePath);
                    break;
            }
            isShapeDrawing = false;
        }
    }

    private RectF getExpandedBounds(DrawingData drawingData) {
        RectF bounds = new RectF();
        float calculatedMargin = calculateDynamicMargin(drawingData);

        drawingData.getPath().computeBounds(bounds, true);
        bounds.inset(-calculatedMargin, -calculatedMargin);
        // Expand the bounds by the specified margin
        bounds.left -= 10;
        bounds.top -= 10;
        bounds.right += 10;
        bounds.bottom += 10;

        return bounds;
    }

    private void startPathMoving(float x, float y) {
        if (selectedDrawing != null) {
            selectedDrawing.getPath().computeBounds(expandedBounds, true);
            if (expandedBounds.contains(x, y)) {
                isMoving = true;
                isSelecting = false;
            }
        }
    }

    private void movePathMoving(float x, float y) {

        if (selectedDrawing != null && isMoving) {
            float offsetX = x - expandedBounds.centerX();
            float offsetY = y - expandedBounds.centerY();
            Matrix translateMatrix = new Matrix();

            translateMatrix.setTranslate(offsetX, offsetY);
            selectedDrawing.getPath().transform(translateMatrix);
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
            startX = x;
            startY = y;
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
        if (selectedDrawing != null) selectedDrawing = null;
        for (int i = 0; i < paths.size(); i++) {
            DrawingData drawingData = new DrawingData(paints.get(i), paths.get(i));
            // Simplified check for intersection with the selection area
            if (isPathIntersectingArea(drawingData, selectionArea)) {
                drawingData.getPaint().setPosition(i);
                selectedDrawing = drawingData;
                slider.setValues(selectedDrawing.getPaint().getStrokeWidth());
            }
        }
    }

    private boolean isPathIntersectingArea(DrawingData drawingData, Path selectionArea) {
        // Simplified check for intersection by comparing bounding boxes
        RectF pathBounds = getExpandedBounds(drawingData);
        RectF areaBounds = new RectF();
        Region r1 = new Region();
        r1.setPath(
                drawingData.getPath(),
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
        drawingData.getPath().computeBounds(pathBounds, true);
        selectionArea.computeBounds(areaBounds, true);
        return RectF.intersects(pathBounds, areaBounds);
    }

    private float calculateDynamicMargin(DrawingData drawingData) {
        // Calculate the size of the path
        RectF bounds = new RectF();
        drawingData.getPath().computeBounds(bounds, true);
        float pathSize = Math.max(bounds.width(), bounds.height());

        // Define a threshold and corresponding margin increase
        float sizeThreshold = 10; // Adjust as needed
        float marginIncrease = 10; // Adjust as needed

        // Calculate dynamic margin
        if (pathSize < sizeThreshold) {
            return drawingData.getPaint().getStrokeWidth() / 2 + marginIncrease;
        } else {
            return drawingData.getPaint().getStrokeWidth() / 2;
        }
    }

    class CanvasGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(@NonNull MotionEvent event) {
            if (!isDraw) {
                if (selectedDrawing != null) {
                    isMoving = false;
                    int p = selectedDrawing.getPaint().getPosition();
                    if (p >= 0 && p < paths.size()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle("Delete");
                        builder.setMessage("Do You want to delete selected Path?");
                        builder.setCancelable(false);
                        builder.setPositiveButton(
                                "Delete",
                                (dialog, id) -> {
                                    paths.remove(p);
                                    paints.remove(p);
                                    selectedDrawing = null;
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
