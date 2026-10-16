package com.ask2784.drawingapp;

import android.graphics.Path;
import android.graphics.Paint;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Small versioned, app-private drawing-project format. Paths are stored as
 * flattened points so project files do not depend on Java object serialization.
 */
public final class DrawingProjectStore {
    private static final int MAGIC = 0x44524157; // DRAW
    private static final int VERSION = 1;
    private static final int MAX_STROKES = 100_000;
    private static final int MAX_APPROXIMATION_VALUES = 3_000_000;

    private DrawingProjectStore() {}

    public static File projectDirectory(File filesDir) throws IOException {
        File directory = new File(filesDir, "drawing-projects");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Unable to create project directory");
        }
        if (!directory.isDirectory()) {
            throw new IOException("Project path is not a directory");
        }
        return directory;
    }

    public static String sanitizeName(String name) {
        if (name == null) return "";
        String safe = name.trim().replaceAll("[^A-Za-z0-9 _.-]", "_");
        safe = safe.replaceAll("\\s+", " ");
        while (safe.startsWith(".")) safe = safe.substring(1);
        if (safe.length() > 64) safe = safe.substring(0, 64).trim();
        return safe;
    }

    public static File save(File filesDir, String requestedName, DrawingData drawing)
            throws IOException {
        String name = sanitizeName(requestedName);
        if (name.isEmpty()) throw new IOException("Enter a valid project name");
        if (drawing == null || drawing.getPaintList() == null || drawing.getPathList() == null) {
            throw new IOException("Drawing data is unavailable");
        }
        if (drawing.getPaintList().size() != drawing.getPathList().size()) {
            throw new IOException("Drawing data is inconsistent");
        }

        File directory = projectDirectory(filesDir);
        File target = new File(directory, name + ".draw");
        File temporary = new File(directory, name + ".draw.tmp");
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(temporary)))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeInt(drawing.getPaintList().size());
            for (int i = 0; i < drawing.getPaintList().size(); i++) {
                SerializePaint paint = drawing.getPaintList().get(i);
                Path path = drawing.getPathList().get(i);
                if (paint == null || path == null) throw new IOException("Invalid stroke data");
                out.writeInt(paint.getColor());
                out.writeFloat(Math.max(0.5f, paint.getStrokeWidth()));
                out.writeUTF(paint.getShapeType() == null ? ShapeType.BRUSH.name() : paint.getShapeType().name());
                out.writeUTF(paint.getCap() == null ? Paint.Cap.ROUND.name() : paint.getCap().name());
                out.writeUTF(paint.getJoin() == null ? Paint.Join.ROUND.name() : paint.getJoin().name());
                out.writeUTF(paint.getStyle() == null ? Paint.Style.STROKE.name() : paint.getStyle().name());
                float[] points = path.approximate(0.5f);
                if (points.length > MAX_APPROXIMATION_VALUES) {
                    throw new IOException("A stroke is too complex to save");
                }
                out.writeInt(points.length);
                for (float point : points) out.writeFloat(point);
            }
        } catch (Exception error) {
            // Never leave a partial file looking like a valid project.
            //noinspection ResultOfMethodCallIgnored
            temporary.delete();
            if (error instanceof IOException) throw (IOException) error;
            throw new IOException("Unable to save project", error);
        }

        if (target.exists() && !target.delete()) {
            //noinspection ResultOfMethodCallIgnored
            temporary.delete();
            throw new IOException("Unable to replace existing project");
        }
        if (!temporary.renameTo(target)) {
            //noinspection ResultOfMethodCallIgnored
            temporary.delete();
            throw new IOException("Unable to finalize project file");
        }
        return target;
    }

    public static DrawingData load(File file) throws IOException {
        ArrayList<SerializePaint> paints = new ArrayList<>();
        ArrayList<Path> paths = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            if (in.readInt() != MAGIC) throw new IOException("This is not a drawing project");
            int version = in.readInt();
            if (version != VERSION) throw new IOException("Unsupported project version: " + version);
            int count = in.readInt();
            if (count < 0 || count > MAX_STROKES) throw new IOException("Invalid stroke count");

            for (int i = 0; i < count; i++) {
                int color = in.readInt();
                float width = in.readFloat();
                if (!Float.isFinite(width) || width <= 0 || width > 1000) {
                    throw new IOException("Invalid stroke width");
                }
                ShapeType shape = enumValue(ShapeType.class, in.readUTF(), ShapeType.BRUSH);
                Paint.Cap cap = enumValue(Paint.Cap.class, in.readUTF(), Paint.Cap.ROUND);
                Paint.Join join = enumValue(Paint.Join.class, in.readUTF(), Paint.Join.ROUND);
                Paint.Style style = enumValue(Paint.Style.class, in.readUTF(), Paint.Style.STROKE);
                int length = in.readInt();
                if (length < 0 || length > MAX_APPROXIMATION_VALUES || length % 3 != 0) {
                    throw new IOException("Invalid stroke points");
                }
                Path path = new Path();
                boolean hasPoint = false;
                float previousFraction = -1f;
                for (int p = 0; p < length; p += 3) {
                    float fraction = in.readFloat();
                    float x = in.readFloat();
                    float y = in.readFloat();
                    if (!Float.isFinite(fraction) || !Float.isFinite(x) || !Float.isFinite(y)) {
                        throw new IOException("Project contains invalid coordinates");
                    }
                    if (!hasPoint || fraction < previousFraction) {
                        path.moveTo(x, y);
                        hasPoint = true;
                    } else {
                        path.lineTo(x, y);
                    }
                    previousFraction = fraction;
                }
                paints.add(new SerializePaint(color, width, shape, cap, join, style));
                paths.add(path);
            }
            if (in.read() != -1) throw new IOException("Unexpected trailing project data");
        } catch (EOFException error) {
            throw new IOException("Project file is incomplete or corrupted", error);
        } catch (IllegalArgumentException error) {
            throw new IOException("Project file contains invalid values", error);
        }
        return new DrawingData(paints, paths);
    }

    public static File[] list(File filesDir) throws IOException {
        File directory = projectDirectory(filesDir);
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".draw") && new File(dir, name).isFile());
        if (files == null) return new File[0];
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        return files;
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value, T fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (RuntimeException error) {
            return fallback;
        }
    }
}
