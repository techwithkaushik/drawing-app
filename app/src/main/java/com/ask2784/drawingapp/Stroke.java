package com.ask2784.drawingapp;

import android.graphics.Path;

public class Stroke {

    // color of the stroke
    public int color;

    // width of the stroke
    public float strokeWidth;

    // a Path object to
    // represent the path drawn
    public Path path;
    public int position;
    public ShapeType shapeType;

    public Stroke(int color, float strokeWidth, Path path, ShapeType shapeType) {
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.path = path;
        this.shapeType = shapeType;
    }
}
