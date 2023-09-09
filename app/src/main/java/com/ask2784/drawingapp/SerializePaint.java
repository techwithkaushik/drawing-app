package com.ask2784.drawingapp;

import android.graphics.Paint;
import java.io.Serializable;

public class SerializePaint implements Serializable {

    private int color;
    private float strokeWidth;
    private int position;
    private ShapeType shapeType;
    private Paint.Cap cap;
    private Paint.Join join;
    private Paint.Style style;

    public SerializePaint(
            int color,
            float strokeWidth,
            ShapeType shapeType,
            Paint.Cap cap,
            Paint.Join join,
            Paint.Style style) {
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.shapeType = shapeType;
        this.cap = cap;
        this.join = join;
        this.style = style;
    }

    public int getColor() {
        return this.color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public float getStrokeWidth() {
        return this.strokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public int getPosition() {
        return this.position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public ShapeType getShapeType() {
        return this.shapeType;
    }

    public void setShapeType(ShapeType shapeType) {
        this.shapeType = shapeType;
    }

    public Paint.Cap getCap() {
        return this.cap;
    }

    public void setCap(Paint.Cap cap) {
        this.cap = cap;
    }

    public Paint.Join getJoin() {
        return this.join;
    }

    public void setJoin(Paint.Join join) {
        this.join = join;
    }

    public Paint.Style getStyle() {
        return this.style;
    }

    public void setStyle(Paint.Style style) {
        this.style = style;
    }
}
