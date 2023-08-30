package com.ask2784.drawingpaint;

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

    // constructor to initialise the attributes
    public Stroke(int color, float strokeWidth, Path path, int position) {
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.path = path;
        this.position = position;
    }

    public Stroke(int color, float strokeWidth, Path path) {
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.path = path;
    }
}
