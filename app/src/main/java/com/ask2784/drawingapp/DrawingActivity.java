package com.ask2784.drawingapp;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.text.InputType;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.ask2784.drawingapp.databinding.ActivityDrawingBinding;
import com.ask2784.drawingapp.databinding.SaveDrawingBinding;
import com.google.android.material.slider.RangeSlider;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DrawingActivity extends AppCompatActivity {
    private ActivityDrawingBinding binding;
    private PaintView paintView;
    private SharedPreferences settings;
    String suffix = "", fileName = "MyDrawing";
    SharedPreferences.Editor editor;
    RangeSlider strokeSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDrawingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.includeDraw.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(fileName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        settings = getSharedPreferences("paintView", MODE_PRIVATE);
        editor = settings.edit();
        paintView = binding.drawLayout;
        strokeSize = binding.changeSize;
        registerForContextMenu(binding.draw);
        strokeSize.setValueFrom(0.5f);
        strokeSize.setValueTo(50.0f);
        strokeSize.setValues(settings.getFloat("strokeWidth", 5.0f));
        binding.draw.setOnClickListener(
                v -> {
                    paintView.startDrawing();
                    binding.eraser.setAlpha(1.0f);
                    strokeSize.setValues(settings.getFloat("strokeWidth", 5.0f));
                });

        binding.selectPath.setOnClickListener(v -> paintView.startSelect(strokeSize));
        binding.eraser.setOnClickListener(v -> {
            boolean enabled = !paintView.isEraserEnabled();
            paintView.setEraserEnabled(enabled);
            binding.eraser.setAlpha(enabled ? 0.55f : 1.0f);
        });

        Drawable colorDrawable = binding.setColor.getDrawable();
        colorDrawable.setTint(settings.getInt("paintColor", Color.GREEN));

        strokeSize.addOnChangeListener(
                (slider, value, userValue) -> {
                    if (paintView.isDrawPath()) {
                        editor.putFloat("strokeWidth", value);
                        editor.apply();
                    }
                    paintView.setStrokeWidth(value);
                });
        binding.setColor.setOnClickListener(
                v -> {
                    new ColorPickerDialog.Builder(this)
                            .setPreferenceName("colorPicker")
                            .attachAlphaSlideBar(true)
                            .attachBrightnessSlideBar(true)
                            .setTitle("Select Color")
                            .setPositiveButton(
                                    "Select",
                                    new ColorEnvelopeListener() {

                                        @Override
                                        public void onColorSelected(
                                                ColorEnvelope envelope, boolean fromUser) {
                                            if (paintView.isDrawPath()) {
                                                editor.putInt("paintColor", envelope.getColor());
                                                editor.apply();
                                                colorDrawable.setTint(
                                                        settings.getInt("paintColor", Color.BLACK));
                                            }
                                            paintView.setStrokeColor(envelope.getColor());
                                            binding.eraser.setAlpha(1.0f);
                                        }
                                    })
                            .setNegativeButton("Cancel", null)
                            .create()
                            .show();
                });

        String requestedProject = getIntent().getStringExtra("project_name");
        if (requestedProject != null && !requestedProject.trim().isEmpty()) {
            openProjectByName(requestedProject);
        }
    }

    private void chooseCanvasBackground() {
        new ColorPickerDialog.Builder(this)
                .setPreferenceName("backgroundColorPicker")
                .attachBrightnessSlideBar(true)
                .setTitle("Canvas Background")
                .setPositiveButton("Apply", (ColorEnvelopeListener) (envelope, fromUser) ->
                        paintView.setCanvasBackgroundColor(envelope.getColor() | 0xFF000000))
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void exportImage() {
        SaveDrawingBinding saveBinding = SaveDrawingBinding.inflate(getLayoutInflater());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save Drawing")
                .setView(saveBinding.getRoot())
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Share", null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(
                i -> {
                    String[] suffixList = {"png", "jpg"};
                    ArrayAdapter<String> suffixAdapter =
                            new ArrayAdapter<>(
                                    this, android.R.layout.simple_spinner_item, suffixList);
                    suffixAdapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item);
                    saveBinding.suffixSpinner.setAdapter(suffixAdapter);
                    saveBinding.suffixSpinner.setOnItemSelectedListener(
                            new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(
                                        AdapterView<?> parent, View view, int position, long id) {
                                    saveBinding.fileNameLayout.setSuffixText(
                                            parent.getSelectedItem().toString());
                                    suffix = parent.getSelectedItem().toString();
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> arg0) {}
                            });
                    Bitmap bitmap = paintView.exportImage();
                    if (bitmap == null) {
                        Toast.makeText(this, "Canvas is not ready yet.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        return;
                    }
                    dialog.setOnDismissListener(ignored -> {
                        if (!bitmap.isRecycled()) bitmap.recycle();
                    });
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setOnClickListener(v2 -> {
                                fileName = DrawingProjectStore.sanitizeName(
                                        saveBinding.fileName.getText().toString());
                                if (fileName.isEmpty()) {
                                    Toast.makeText(this, "Enter a valid file name.",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                String mimeType = "png".equalsIgnoreCase(suffix)
                                        ? "image/png" : "image/jpeg";
                                ContentValues values = new ContentValues();
                                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName + "." + suffix);
                                values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
                                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                                        Environment.DIRECTORY_PICTURES + "/Lets Draw");
                                values.put(MediaStore.Images.Media.IS_PENDING, 1);
                                Uri uri = getContentResolver().insert(
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                                if (uri == null) {
                                    Toast.makeText(this, "Could not create image file.",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }
                                try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                                    if (output == null) throw new IOException("Unable to open output stream");
                                    Bitmap.CompressFormat format = "png".equalsIgnoreCase(suffix)
                                            ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
                                    if (!bitmap.compress(format, 100, output)) {
                                        throw new IOException("Image encoding failed");
                                    }
                                    ContentValues published = new ContentValues();
                                    published.put(MediaStore.Images.Media.IS_PENDING, 0);
                                    getContentResolver().update(uri, published, null, null);
                                    Toast.makeText(this, fileName + " saved to Pictures/Lets Draw.",
                                            Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                } catch (Exception error) {
                                    getContentResolver().delete(uri, null, null);
                                    Toast.makeText(this, "Could not save image: " + error.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                            .setOnClickListener(
                                    vs -> {
                                        fileName = DrawingProjectStore.sanitizeName(
                                                saveBinding.fileName.getText().toString());
                                        if (fileName.isEmpty()) {
                                            Toast.makeText(this, "Enter a valid file name.",
                                                    Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        File imageFolder = new File(getCacheDir(), "images");
                                        Uri uri = null;
                                        try {
                                            if (!imageFolder.exists() && !imageFolder.mkdirs()) {
                                                throw new IOException("Unable to prepare share cache");
                                            }
                                            File file =
                                                    new File(imageFolder, fileName + "." + suffix);
                                            Bitmap.CompressFormat imageFormat =
                                                    "png".equalsIgnoreCase(suffix)
                                                            ? Bitmap.CompressFormat.PNG
                                                            : Bitmap.CompressFormat.JPEG;
                                            try (FileOutputStream output = new FileOutputStream(file)) {
                                                if (!bitmap.compress(imageFormat, 100, output)) {
                                                    throw new IOException("Image encoding failed");
                                                }
                                                output.flush();
                                            }
                                            uri = FileProvider.getUriForFile(
                                                    this, "com.ask2784.drawingapp.fileprovider", file);
                                            Intent intent = new Intent(Intent.ACTION_SEND);
                                            intent.setType("png".equalsIgnoreCase(suffix)
                                                    ? "image/png" : "image/jpeg");
                                            intent.putExtra(Intent.EXTRA_STREAM, uri);
                                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                            startActivity(Intent.createChooser(intent, "Share image"));
                                        } catch (Exception err) {
                                            Toast.makeText(this, "Could not share image: "
                                                    + err.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                });
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.draw_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.undo) paintView.undo();
        else if (id == R.id.redo) paintView.redo();
        else if (id == R.id.clear_drawing) {
            new AlertDialog.Builder(this)
                    .setTitle("Clear canvas?")
                    .setMessage("This removes all strokes from the canvas. You can use Redo to restore them.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Clear", (dialog, which) -> paintView.clear())
                    .show();
        }
        else if (id == R.id.export_image) exportImage();
        else if (id == R.id.save_project) saveProject();
        else if (id == R.id.load_project) loadProject();
        else if (id == R.id.reset_view) {
            paintView.resetViewport();
            Toast.makeText(this, "Canvas view reset.", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.set_background) {
            chooseCanvasBackground();
        } else if (id == R.id.fill_shapes) {
            boolean enabled = !item.isChecked();
            item.setChecked(enabled);
            paintView.setFillShapes(enabled);
            Toast.makeText(this, enabled ? "Shape fill enabled." : "Shape outline enabled.",
                    Toast.LENGTH_SHORT).show();
        }
        if (id == R.id.undo || id == R.id.redo || id == R.id.clear_drawing
                || id == R.id.export_image || id == R.id.save_project || id == R.id.load_project
                || id == R.id.reset_view || id == R.id.set_background || id == R.id.fill_shapes) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(
            ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.draw_shapes, menu);
        menu.setHeaderTitle("Select Shape");
    }

    private void changeDrawMethod(MenuItem item, ShapeType shape) {
        binding.draw.setImageDrawable(item.getIcon());
        paintView.setDrawMethod(shape);
        binding.eraser.setAlpha(1.0f);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.brush) changeDrawMethod(item, ShapeType.BRUSH);
        else if (id == R.id.line) changeDrawMethod(item, ShapeType.LINE);
        else if (id == R.id.rectangle) changeDrawMethod(item, ShapeType.RECTANGLE);
        else if (id == R.id.square) changeDrawMethod(item, ShapeType.SQUARE);
        else if (id == R.id.triangle) changeDrawMethod(item, ShapeType.TRIANGLE);
        else if (id == R.id.circle) changeDrawMethod(item, ShapeType.CIRCLE);
        if (id == R.id.brush || id == R.id.line || id == R.id.rectangle || id == R.id.square
                || id == R.id.triangle || id == R.id.circle) return true;
        return super.onContextItemSelected(item);
    }

    private void saveProject() {
        EditText nameInput = new EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nameInput.setHint("Project name");
        nameInput.setText(getSupportActionBar() == null ? "My Drawing"
                : String.valueOf(getSupportActionBar().getTitle()));
        new AlertDialog.Builder(this)
                .setTitle("Save project")
                .setMessage("Your editable strokes will be saved on this device.")
                .setView(nameInput)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String projectName = DrawingProjectStore.sanitizeName(nameInput.getText().toString());
                    if (projectName.isEmpty()) {
                        Toast.makeText(this, "Enter a valid project name.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        File directory = DrawingProjectStore.projectDirectory(getFilesDir());
                        File target = new File(directory, projectName + ".draw");
                        if (target.exists()) {
                            new AlertDialog.Builder(this)
                                    .setTitle("Replace project?")
                                    .setMessage(projectName + " already exists. Replace it?")
                                    .setNegativeButton("Cancel", null)
                                    .setPositiveButton("Replace", (confirm, choice) -> persistProject(projectName))
                                    .show();
                        } else {
                            persistProject(projectName);
                        }
                    } catch (IOException error) {
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private void openProjectByName(String projectName) {
        try {
            File file = new File(DrawingProjectStore.projectDirectory(getFilesDir()),
                    DrawingProjectStore.sanitizeName(projectName) + ".draw");
            if (!file.isFile()) {
                Toast.makeText(this, "Project no longer exists.", Toast.LENGTH_SHORT).show();
                return;
            }
            DrawingData project = DrawingProjectStore.load(file);
            paintView.loadProject(project);
            fileName = projectName;
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(fileName);
            paintView.startDrawing();
            Toast.makeText(this, "Project opened: " + fileName, Toast.LENGTH_SHORT).show();
        } catch (IOException error) {
            Toast.makeText(this, "Could not open project: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void persistProject(String projectName) {
        try {
            DrawingProjectStore.save(getFilesDir(), projectName, paintView.saveProject());
            fileName = projectName;
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(projectName);
            Toast.makeText(this, "Project saved: " + projectName, Toast.LENGTH_SHORT).show();
        } catch (IOException error) {
            Toast.makeText(this, "Could not save project: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void loadProject() {
        try {
            File[] projects = DrawingProjectStore.list(getFilesDir());
            if (projects.length == 0) {
                Toast.makeText(this, "No saved projects yet.", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] names = new String[projects.length];
            for (int i = 0; i < projects.length; i++) {
                String storedName = projects[i].getName();
                names[i] = storedName.substring(0, storedName.length() - ".draw".length());
            }
            new AlertDialog.Builder(this)
                    .setTitle("Open project")
                    .setItems(names, (dialog, index) -> {
                        openProjectByName(names[index]);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (IOException error) {
            Toast.makeText(this, "Could not list projects: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
