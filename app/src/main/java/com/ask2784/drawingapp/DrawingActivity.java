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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
        strokeSize.setValueFrom(0.5f);
        strokeSize.setValueTo(50.0f);
        strokeSize.setValues(settings.getFloat("strokeWidth", 5.0f));
        binding.draw.setOnClickListener(
                v -> {
                    paintView.startDrawing();
                    strokeSize.setValues(settings.getFloat("strokeWidth", 5.0f));
                    isExtra = false;
                    binding.extraDraw.setVisibility(View.GONE);
                });

        binding.selectPath.setOnClickListener(v -> paintView.startSelect(strokeSize));

        Drawable colorDrawable = binding.setColor.getDrawable();
        colorDrawable.setTint(settings.getInt("paintColor", Color.GREEN));
        binding.draw.setOnLongClickListener(
                v -> {
                    if (paintView.getSelectedDrawing() != null || paintView.isDrawPath())
                        changeDrawMethodOnLongClick();
                    return true;
                });

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
                                        }
                                    })
                            .setNegativeButton("Cancel", null)
                            .create()
                            .show();
                });
        changeDrawMethod();
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
                    Bitmap.CompressFormat imageFormat =
                            suffix.equals("png")
                                    ? Bitmap.CompressFormat.PNG
                                    : Bitmap.CompressFormat.JPEG;

                    fileName = saveBinding.fileName.getText().toString().trim();
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setOnClickListener(
                                    v2 -> {
                                        if (fileName != null && fileName.length() > 0) {

                                            OutputStream imgOut;
                                            ContentValues cv = new ContentValues();
                                            cv.put(
                                                    MediaStore.Images.Media.DISPLAY_NAME,
                                                    fileName + "." + suffix);
                                            cv.put(
                                                    MediaStore.Images.Media.MIME_TYPE,
                                                    "image/" + suffix);
                                            cv.put(
                                                    MediaStore.Images.Media.RELATIVE_PATH,
                                                    Environment.DIRECTORY_PICTURES);
                                            Uri uri =
                                                    getContentResolver()
                                                            .insert(
                                                                    MediaStore.Images.Media
                                                                            .EXTERNAL_CONTENT_URI,
                                                                    cv);
                                            try {
                                                assert uri != null;
                                                imgOut = getContentResolver().openOutputStream(uri);

                                                bitmap.compress(imageFormat, 100, imgOut);
                                                assert imgOut != null;
                                                imgOut.close();
                                                Toast.makeText(
                                                                this,
                                                                saveBinding
                                                                                .fileName
                                                                                .getText()
                                                                                .toString()
                                                                        + " Saved",
                                                                Toast.LENGTH_SHORT)
                                                        .show();
                                                dialog.dismiss();
                                            } catch (Exception err) {
                                                err.printStackTrace();
                                            }
                                        } else {
                                            Toast.makeText(
                                                            getApplicationContext(),
                                                            "FileName is Not Valid.",
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                        }
                                    });
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                            .setOnClickListener(
                                    vs -> {
                                        fileName = saveBinding.fileName.getText().toString().trim();
                                        File imageFolder = new File(getCacheDir(), "images");
                                        Uri uri = null;
                                        try {
                                            imageFolder.mkdir();
                                            File file =
                                                    new File(imageFolder, fileName + "." + suffix);
                                            FileOutputStream outup = new FileOutputStream(file);
                                            bitmap.compress(imageFormat, 100, outup);
                                            outup.flush();
                                            outup.close();
                                            uri =
                                                    FileProvider.getUriForFile(
                                                            this,
                                                            "com.ask2784.drawingapp.fileprovider",
                                                            file);
                                            Intent intent = new Intent(Intent.ACTION_SEND);
                                            intent.putExtra(Intent.EXTRA_STREAM, uri);
                                            intent.setType(
                                                    suffix.equals("png")
                                                            ? "image/png"
                                                            : "image/jpeg");
                                            startActivity(
                                                    Intent.createChooser(intent, "Share image"));
                                        } catch (Exception err) {
                                            err.printStackTrace();
                                        }
                                    });
                });
        dialog.show();
    }

    private void changeDrawMethodOnLongClick() {
        isExtra = !isExtra ? true : false;
        binding.extraDraw.setVisibility(isExtra ? View.VISIBLE : View.GONE);
    }

    Drawable drawable;

    private void changeDrawMethod() {
        binding.drawBrush.setOnClickListener(
                v -> {
                    drawable = binding.drawBrush.getDrawable();
                    binding.draw.setImageDrawable(drawable);
                    paintView.setDrawMethod(ShapeType.BRUSH);
                });
        binding.drawPencil.setOnClickListener(
                v -> {
                    drawable = binding.drawPencil.getDrawable();
                    binding.draw.setImageDrawable(drawable);
                    paintView.setDrawMethod(ShapeType.LINE);
                });
        binding.drawRectangle.setOnClickListener(
                v -> {
                    drawable = binding.drawRectangle.getDrawable();
                    binding.draw.setImageDrawable(drawable);
                    paintView.setDrawMethod(ShapeType.RECTANGLE);
                });
        binding.drawSquare.setOnClickListener(
                v -> {
                    drawable = binding.drawSquare.getDrawable();
                    binding.draw.setImageDrawable(drawable);
                    paintView.setDrawMethod(ShapeType.SQUARE);
                });
        binding.drawCircle.setOnClickListener(
                v -> {
                    drawable = binding.drawCircle.getDrawable();
                    binding.draw.setImageDrawable(drawable);
                    paintView.setDrawMethod(ShapeType.CIRCLE);
                });
        binding.drawTriangle.setOnClickListener(
                v -> {
                    drawable = binding.drawTriangle.getDrawable();
                    binding.draw.setImageDrawable(drawable);
                    paintView.setDrawMethod(ShapeType.TRIANGLE);
                });
    }

    boolean isExtra = false;

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
        else if (id == R.id.clear_drawing) paintView.clear();
        else if (id == R.id.export_image) exportImage();
        else if (id == R.id.save_project) saveProject();
        else if (id == R.id.load_project) loadProject();
        return super.onOptionsItemSelected(item);
    }

    private void saveProject() {}

    private void loadProject() {}

    @Override
    @MainThread
    public void onBackPressed() {
        super.onBackPressed();
        this.binding = null;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
