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
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
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
    private int canvasBackgroundColor = Color.WHITE;
    private int colorBeforeEraser = Color.GREEN;
    private boolean eraserEnabled = false;
    private float currentStrokeWidth = 5;
    private Bitmap bitmap;
    private DrawingData selectedDrawing = null;
    private boolean isMove = false;

    private boolean isMoving = false;
    private boolean isSelecting = false;
    private boolean isSelect = false;
    private final Paint dashedRectanglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path previewPath = new Path();
    private ShapeType shapeType;
    private Canvas mCanvas;
    private boolean isShapeDrawing = false;
    private float canvasScale = 1f;
    private float canvasOffsetX = 0f;
    private float canvasOffsetY = 0f;
    private float lastGestureX = 0f;
    private float lastGestureY = 0f;
    private float lastMoveX = 0f;
    private float lastMoveY = 0f;
    private boolean viewportGesture = false;
    private final ScaleGestureDetector scaleGestureDetector;
    private Paint.Cap cap = Paint.Cap.ROUND;
    private Paint.Join join = Paint.Join.ROUND;
    private Paint.Style style = Paint.Style.STROKE;
    private boolean fillShapes = false;

    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        gestureDetector = new GestureDetectorCompat(getContext(), new CanvasGestureDetector());
        scaleGestureDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float oldScale = canvasScale;
                        float newScale = Math.max(0.5f, Math.min(5f, oldScale * detector.getScaleFactor()));
                        float focusX = detector.getFocusX();
                        float focusY = detector.getFocusY();
                        float canvasFocusX = (focusX - canvasOffsetX) / oldScale;
                        float canvasFocusY = (focusY - canvasOffsetY) / oldScale;
                        canvasScale = newScale;
                        canvasOffsetX = focusX - canvasFocusX * newScale;
                        canvasOffsetY = focusY - canvasFocusY * newScale;
                        invalidate();
                        return true;
                    }
                });
        setupDraw();
    }

    private void setupDraw() {
        shapeType = ShapeType.BRUSH;
        float savedStrokeWidth = settings.getFloat("strokeWidth", 5.0f);
        if (!Float.isFinite(savedStrokeWidth) || savedStrokeWidth < 0.5f || savedStrokeWidth > 50f) {
            savedStrokeWidth = 5.0f;
            settings.edit().putFloat("strokeWidth", savedStrokeWidth).apply();
        }
        currentStrokeWidth = savedStrokeWidth;
        currentColor = settings.getInt("paintColor", Color.GREEN);
        canvasBackgroundColor = settings.getInt("canvasBackgroundColor", Color.WHITE) | 0xFF000000;
        dPaint = new Paint();
        dPaint.setColor(currentColor);
        dPaint.setAntiAlias(true);
        dPaint.setAlpha(0x80);
        dPaint.setStrokeWidth(currentStrokeWidth);
        dPaint.setStyle(style);
        dPaint.setStrokeJoin(join);
        dPaint.setStrokeCap(cap);
        dashedRectanglePaint.setColor(Color.BLACK);
        dashedRectanglePaint.setStyle(Paint.Style.STROKE);
        dashedRectanglePaint.setStrokeWidth(2f);
        dashedRectanglePaint.setPathEffect(new DashPathEffect(new float[] {10, 5}, 0));
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(canvasBackgroundColor);
        canvas.save();
        canvas.translate(canvasOffsetX, canvasOffsetY);
        canvas.scale(canvasScale, canvasScale);

        for (int i = 0; i < paints.size() && i < paths.size(); i++) {
            SerializePaint stroke = paints.get(i);
            dPaint.setColor(stroke.getColor());
            dPaint.setStrokeWidth(stroke.getStrokeWidth());
            dPaint.setStrokeJoin(stroke.getJoin());
            dPaint.setStrokeCap(stroke.getCap());
            dPaint.setStyle(stroke.getStyle());
            canvas.drawPath(paths.get(i), dPaint);
        }

        // Draw selection overlays last so they remain visible over the selected stroke.
        drawSelectedPathHighlight(canvas);

        if (isShapeDrawing) {
            // Preview must use the current tool settings, not the last saved stroke's paint.
            dPaint.setColor(currentColor);
            dPaint.setStrokeWidth(currentStrokeWidth);
            dPaint.setStrokeJoin(join);
            dPaint.setStrokeCap(cap);
            dPaint.setStyle(style);
            Path currentPreview = buildShapePath(previewPath, startX, startY, endX, endY);
            if (currentPreview != null) canvas.drawPath(currentPreview, dPaint);
        }

        canvas.restore();
    }

    public void setDrawMethod(ShapeType shapeType) {
        setEraserEnabled(false);
        this.shapeType = shapeType == null ? ShapeType.BRUSH : shapeType;
        isShapeDrawing = false;
        isDraw = true;
        isSelect = false;
        isMove = false;
        isSelecting = false;
        isMoving = false;
        selectedDrawing = null;
        mPath = null;
        switch (this.shapeType) {
            case LINE:
            case BRUSH:
            case CIRCLE:
                join = Paint.Join.ROUND;
                cap = Paint.Cap.ROUND;
                break;
            case RECTANGLE:
            case SQUARE:
            case TRIANGLE:
                join = Paint.Join.MITER;
                cap = Paint.Cap.BUTT;
                break;
        }
        style = fillShapes && shapeType != ShapeType.BRUSH && shapeType != ShapeType.LINE
                ? Paint.Style.FILL : Paint.Style.STROKE;
        dPaint.setStrokeJoin(join);
        dPaint.setStrokeCap(cap);
        dPaint.setStyle(style);
        invalidate();
    }

    public void setFillShapes(boolean enabled) {
        fillShapes = enabled;
        setDrawMethod(shapeType);
    }

    public boolean areShapesFilled() {
        return fillShapes;
    }

    public DrawingData saveProject() {
        DrawingData data = new DrawingData();
        data.setPaintList(new ArrayList<>(paints));
        data.setPathList(new ArrayList<>(paths));
        data.setBackgroundColor(canvasBackgroundColor);
        return data;
    }

    public void loadProject(DrawingData drawingData) {
        if (drawingData == null || drawingData.getPaintList() == null
                || drawingData.getPathList() == null
                || drawingData.getPaintList().size() != drawingData.getPathList().size()) {
            Toast.makeText(getContext(), "Project file is invalid.", Toast.LENGTH_SHORT).show();
            return;
        }
        for (int i = 0; i < drawingData.getPaintList().size(); i++) {
            if (drawingData.getPaintList().get(i) == null || drawingData.getPathList().get(i) == null) {
                Toast.makeText(getContext(), "Project file is invalid.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        paints = new ArrayList<>(drawingData.getPaintList());
        paths = new ArrayList<>(drawingData.getPathList());
        canvasBackgroundColor = drawingData.getBackgroundColor() | 0xFF000000;
        settings.edit().putInt("canvasBackgroundColor", canvasBackgroundColor).apply();
        if (eraserEnabled) currentColor = canvasBackgroundColor;
        redoPaint.clear();
        redoPaths.clear();
        selectedDrawing = null;
        isSelecting = false;
        isMoving = false;
        isMove = false;
        isSelect = false;
        isDraw = true;
        isShapeDrawing = false;
        invalidate();
    }

    private Path buildShapePath(float x1, float y1, float x2, float y2) {
        return buildShapePath(new Path(), x1, y1, x2, y2);
    }

    private Path buildShapePath(Path path, float x1, float y1, float x2, float y2) {
        path.reset();
        switch (shapeType) {
            case LINE:
                path.moveTo(x1, y1);
                path.lineTo(x2, y2);
                return path;
            case RECTANGLE:
                path.addRect(Math.min(x1, x2), Math.min(y1, y2),
                        Math.max(x1, x2), Math.max(y1, y2), Path.Direction.CW);
                return path;
            case SQUARE: {
                float dx = x2 - x1;
                float dy = y2 - y1;
                float side = Math.min(Math.abs(dx), Math.abs(dy));
                float right = x1 + Math.copySign(side, dx == 0f ? 1f : dx);
                float bottom = y1 + Math.copySign(side, dy == 0f ? 1f : dy);
                path.addRect(Math.min(x1, right), Math.min(y1, bottom),
                        Math.max(x1, right), Math.max(y1, bottom), Path.Direction.CW);
                return path;
            }
            case CIRCLE: {
                float radius = (float) Math.hypot(x2 - x1, y2 - y1);
                path.addCircle(x1, y1, radius, Path.Direction.CW);
                return path;
            }
            case TRIANGLE:
                path.moveTo(x1, y1);
                path.lineTo(x2, y2);
                path.lineTo(x1 - (x2 - x1), y2);
                path.close();
                return path;
            case BRUSH:
            default:
                return null;
        }
    }

    private void drawLine(Canvas canvas) {
        canvas.drawLine(startX, startY, endX, endY, dPaint);
    }

    private void drawRectangle(Canvas canvas) {
        canvas.drawRect(Math.min(startX, endX), Math.min(startY, endY),
                Math.max(startX, endX), Math.max(startY, endY), dPaint);
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
    }

    private void drawCircle(Canvas canvas) {
        float radius = (float) Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
        canvas.drawCircle(startX, startY, radius, dPaint);
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

        if (selectedDrawing != null) {
            RectF selectedPathBounds = getExpandedBounds(selectedDrawing);
            canvas.drawRect(selectedPathBounds, dashedRectanglePaint);
        }
    }

    public void startDrawing() {
        setEraserEnabled(false);
        isDraw = true;
        isMove = false;
        isMoving = false;
        selectedDrawing = null;
        isSelecting = false;
        isSelect = false;
        isShapeDrawing = false;
        mPath = null;
        invalidate();
    }

    private RangeSlider slider;

    public void startSelect(RangeSlider slider) {
        setEraserEnabled(false);
        isSelect = true;
        isDraw = false;
        selectedDrawing = null;
        isMoving = false;
        isMove = false;
        isSelecting = false;
        this.slider = slider;
        invalidate();
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

    public void setEraserEnabled(boolean enabled) {
        if (enabled == eraserEnabled) return;
        if (enabled) {
            colorBeforeEraser = currentColor;
            currentColor = canvasBackgroundColor;
        } else {
            currentColor = colorBeforeEraser;
        }
        eraserEnabled = enabled;
        selectedDrawing = null;
        isDraw = true;
        isSelect = false;
        isMove = false;
        isSelecting = false;
        isMoving = false;
        mPath = null;
        isShapeDrawing = false;
        invalidate();
    }

    public boolean isEraserEnabled() {
        return eraserEnabled;
    }

    public void setCanvasBackgroundColor(int color) {
        canvasBackgroundColor = color | 0xFF000000;
        settings.edit().putInt("canvasBackgroundColor", canvasBackgroundColor).apply();
        if (eraserEnabled) currentColor = canvasBackgroundColor;
        selectedDrawing = null;
        invalidate();
    }

    public int getCanvasBackgroundColor() {
        return canvasBackgroundColor;
    }

    public void setStrokeColor(int strokeColor) {
        eraserEnabled = false;
        currentColor = strokeColor;
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
        if (!paths.isEmpty() && paths.size() == paints.size()) {
            redoPaint.add(paints.remove(paints.size() - 1));
            redoPaths.add(paths.remove(paths.size() - 1));
            selectedDrawing = null;
            isSelecting = false;
            isMoving = false;
            invalidate();
        }
    }

    public void redo() {
        if (!redoPaths.isEmpty() && redoPaths.size() == redoPaint.size()) {
            paints.add(redoPaint.remove(redoPaint.size() - 1));
            paths.add(redoPaths.remove(redoPaths.size() - 1));
            selectedDrawing = null;
            isSelecting = false;
            isMoving = false;
            invalidate();
        }
    }

    public void clear() {
        if (!paths.isEmpty() && paths.size() == paints.size()) {
            redoPaint.clear();
            redoPaths.clear();
            for (int i = paths.size() - 1; i >= 0; i--) {
                redoPaint.add(paints.get(i));
                redoPaths.add(paths.get(i));
            }
            paints.clear();
            paths.clear();
            selectedDrawing = null;
            isSelecting = false;
            isMoving = false;
            isShapeDrawing = false;
            mPath = null;
            invalidate();
        }
    }

    public void resetViewport() {
        canvasScale = 1f;
        canvasOffsetX = 0f;
        canvasOffsetY = 0f;
        invalidate();
    }

    public Bitmap exportImage() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas exportCanvas = new Canvas(result);
        exportCanvas.drawColor(canvasBackgroundColor);
        Paint exportPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        for (int i = 0; i < paints.size() && i < paths.size(); i++) {
            SerializePaint stroke = paints.get(i);
            exportPaint.setColor(stroke.getColor());
            exportPaint.setStrokeWidth(stroke.getStrokeWidth());
            exportPaint.setStrokeJoin(stroke.getJoin());
            exportPaint.setStrokeCap(stroke.getCap());
            exportPaint.setStyle(stroke.getStyle());
            exportCanvas.drawPath(paths.get(i), exportPaint);
        }
        return result;
    }

    private void startDrawing(float x, float y) {
        if (!redoPaths.isEmpty() || !redoPaint.isEmpty()) {
            redoPaths.clear();
            redoPaint.clear();
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
        if (mPath == null) return;
        if (shapeType == ShapeType.BRUSH) {
            // Include the final touch coordinate so fast short strokes are not clipped.
            mPath.lineTo(x, y);
        } else {
            Path shapePath = buildShapePath(startX, startY, x, y);
            if (shapePath != null) mPath.addPath(shapePath);
        }
        endX = x;
        endY = y;
        isShapeDrawing = false;
        mPath = null;
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
            expandedBounds.set(getExpandedBounds(selectedDrawing));
            if (expandedBounds.contains(x, y)) {
                isMoving = true;
                isSelecting = false;
                lastMoveX = x;
                lastMoveY = y;
            }
        }
    }

    private void movePathMoving(float x, float y) {

        if (selectedDrawing != null && isMoving) {
            float offsetX = x - lastMoveX;
            float offsetY = y - lastMoveY;
            Matrix translateMatrix = new Matrix();
            translateMatrix.setTranslate(offsetX, offsetY);
            selectedDrawing.getPath().transform(translateMatrix);
            expandedBounds.offset(offsetX, offsetY);
            lastMoveX = x;
            lastMoveY = y;
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
            isSelect = false;
            isMove = true;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        scaleGestureDetector.onTouchEvent(event);
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_CANCEL) {
            if (isDraw && mPath != null && !paths.isEmpty()
                    && paths.get(paths.size() - 1) == mPath) {
                paths.remove(paths.size() - 1);
                paints.remove(paints.size() - 1);
            }
            mPath = null;
            isShapeDrawing = false;
            isSelecting = false;
            isMoving = false;
            viewportGesture = false;
            lastGestureX = 0f;
            lastGestureY = 0f;
            invalidate();
            return true;
        }
        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            viewportGesture = true;
            if (isDraw && mPath != null && !paths.isEmpty() && paths.get(paths.size() - 1) == mPath) {
                paths.remove(paths.size() - 1);
                paints.remove(paints.size() - 1);
                mPath = null;
                isShapeDrawing = false;
            }
            lastGestureX = (event.getX(0) + event.getX(1)) / 2f;
            lastGestureY = (event.getY(0) + event.getY(1)) / 2f;
            invalidate();
            return true;
        }
        if (action == MotionEvent.ACTION_POINTER_UP) {
            if (event.getPointerCount() <= 2) {
                lastGestureX = 0f;
                lastGestureY = 0f;
            }
            invalidate();
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE && event.getPointerCount() > 1) {
            float midX = 0f;
            float midY = 0f;
            for (int i = 0; i < event.getPointerCount(); i++) {
                midX += event.getX(i);
                midY += event.getY(i);
            }
            midX /= event.getPointerCount();
            midY /= event.getPointerCount();
            if (!scaleGestureDetector.isInProgress()
                    && (lastGestureX != 0f || lastGestureY != 0f)) {
                canvasOffsetX += midX - lastGestureX;
                canvasOffsetY += midY - lastGestureY;
            }
            lastGestureX = midX;
            lastGestureY = midY;
            invalidate();
            return true;
        }
        if (action == MotionEvent.ACTION_UP && viewportGesture) {
            viewportGesture = false;
            lastGestureX = 0f;
            lastGestureY = 0f;
            invalidate();
            return true;
        }
        if (viewportGesture && action != MotionEvent.ACTION_DOWN) return true;

        float x = (event.getX() - canvasOffsetX) / canvasScale;
        float y = (event.getY() - canvasOffsetY) / canvasScale;
        switch (action) {
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
        selectedDrawing = null;
        if (selectionArea == null || slider == null) return;
        // Select the top-most stroke in the drag rectangle, matching drawing z-order.
        for (int i = paths.size() - 1; i >= 0; i--) {
            DrawingData candidate = new DrawingData(paints.get(i), paths.get(i));
            if (isPathIntersectingArea(candidate, selectionArea)) {
                candidate.getPaint().setPosition(i);
                selectedDrawing = candidate;
                slider.setValues(candidate.getPaint().getStrokeWidth());
                break;
            }
        }
    }

    private boolean isPathIntersectingArea(DrawingData drawingData, Path selectionArea) {
        if (drawingData == null || drawingData.getPath() == null || selectionArea == null) {
            return false;
        }
        RectF pathBounds = getExpandedBounds(drawingData);
        RectF areaBounds = new RectF();
        selectionArea.computeBounds(areaBounds, true);
        // A tap or a nearly horizontal/vertical drag still gets a usable hit area.
        float hitPadding = Math.max(TOLERANCE, drawingData.getPaint().getStrokeWidth());
        areaBounds.inset(-hitPadding, -hitPadding);
        return RectF.intersects(pathBounds, areaBounds)
                || areaBounds.contains(pathBounds.centerX(), pathBounds.centerY());
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
                                    // A destructive edit invalidates any pending redo strokes.
                                    redoPaint.clear();
                                    redoPaths.clear();
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
