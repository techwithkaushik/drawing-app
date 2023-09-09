package com.ask2784.drawingapp;

import android.graphics.Path;

import java.io.Serializable;
import java.util.ArrayList;

public class DrawingData implements Serializable {
    private ArrayList<SerializePaint> paintList;
    private ArrayList<Path> pathList;
    private SerializePaint paint;
    private Path path;

    public DrawingData(SerializePaint paint, Path path) {
        this.paint = paint;
        this.path = path;
    }

    public DrawingData() {}

    public DrawingData(ArrayList<SerializePaint> paintList, ArrayList<Path> pathList) {
        this.paintList = paintList;
        this.pathList = pathList;
    }

    public ArrayList<SerializePaint> getPaintList() {
        return this.paintList;
    }

    public void setPaintList(ArrayList<SerializePaint> paintList) {
        this.paintList = paintList;
    }

    public ArrayList<Path> getPathList() {
        return this.pathList;
    }

    public void setPathList(ArrayList<Path> pathList) {
        this.pathList = pathList;
    }

    public SerializePaint getPaint() {
        return this.paint;
    }

    public void setPaint(SerializePaint paint) {
        this.paint = paint;
    }

    public Path getPath() {
        return this.path;
    }

    public void setPath(Path path) {
        this.path = path;
    }
}
