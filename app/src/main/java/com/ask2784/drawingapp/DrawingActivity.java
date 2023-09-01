package com.ask2784.drawingapp;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ask2784.drawingapp.databinding.ActivityDrawingBinding;
import com.ask2784.drawingapp.databinding.SaveDrawingBinding;
import com.google.android.material.slider.RangeSlider;

import java.io.OutputStream;

import java.util.ArrayList;
import java.util.List;
import yuku.ambilwarna.AmbilWarnaDialog;

public class DrawingActivity extends AppCompatActivity {
    private ActivityDrawingBinding binding;
    private PaintView paintView;
    private SharedPreferences settings;

    SharedPreferences.Editor editor;
    RangeSlider strokeSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.WHITE);
        binding = ActivityDrawingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        settings = getSharedPreferences("PAINTVIEW", MODE_PRIVATE);
        editor = settings.edit();
        paintView = binding.drawLayout;
        strokeSize = binding.changeSize;
        strokeSize.setValueFrom(0.5f);
        strokeSize.setValueTo(50.0f);
        strokeSize.setValues(settings.getFloat("STROKEWIDTH", 5.0f));
        binding.draw.setOnClickListener(
                v -> {
                    paintView.startDrawing();
                    strokeSize.setValues(settings.getFloat("STROKEWIDTH", 5.0f));
                });

        binding.undo.setOnClickListener(v -> paintView.undo());

        binding.redo.setOnClickListener(v -> paintView.redo());

        binding.clearDrawing.setOnClickListener(v -> paintView.clear());

        binding.selectPath.setOnClickListener(v -> paintView.startSelect(strokeSize));
        binding.saveImage.setOnClickListener(
                v -> {
                    SaveDrawingBinding saveBinding =
                            SaveDrawingBinding.inflate(getLayoutInflater());
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Save Drawing")
                            .setView(saveBinding.getRoot())
                            .setPositiveButton("Save", null)
                            .setNegativeButton("Cancel", null);
                    AlertDialog dialog = builder.create();
                    dialog.setOnShowListener(
                            i -> {
                                String[] suffixList = {"png", "jpg"};
                                ArrayAdapter<String> suffixAdapter =
                                        new ArrayAdapter<>(
                                                this,
                                                android.R.layout.simple_spinner_item,
                                                suffixList);
                                suffixAdapter.setDropDownViewResource(
                                        android.R.layout.simple_spinner_dropdown_item);
                                saveBinding.suffixSpinner.setAdapter(suffixAdapter);
                                saveBinding.suffixSpinner.setOnItemSelectedListener(
                                        new AdapterView.OnItemSelectedListener() {

                                            @Override
                                            public void onItemSelected(
                                                    AdapterView<?> parent,
                                                    View view,
                                                    int position,
                                                    long id) {
                                                saveBinding.fileNameLayout.setSuffixText(
                                                        parent.getSelectedItem().toString());
                                            }

                                            @Override
                                            public void onNothingSelected(AdapterView<?> arg0) {}
                                        });

                                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                        .setOnClickListener(
                                                v2 -> {
                                                    String fileName =
                                                            saveBinding
                                                                    .fileName
                                                                    .getText()
                                                                    .toString()
                                                                    .trim();
                                                    String suffix =
                                                            saveBinding
                                                                    .fileNameLayout
                                                                    .getSuffixText()
                                                                    .toString();
                                                    if (fileName != null && fileName.length() > 0) {
                                                        Bitmap btmap = paintView.exportImage();
                                                        OutputStream imgOut;
                                                        ContentValues cv = new ContentValues();
                                                        cv.put(
                                                                MediaStore.Images.Media
                                                                        .DISPLAY_NAME,
                                                                fileName + "." + suffix);
                                                        cv.put(
                                                                MediaStore.Images.Media.MIME_TYPE,
                                                                "image/" + suffix);
                                                        cv.put(
                                                                MediaStore.Images.Media
                                                                        .RELATIVE_PATH,
                                                                Environment.DIRECTORY_PICTURES);
                                                        Uri uri =
                                                                getContentResolver()
                                                                        .insert(
                                                                                MediaStore.Images
                                                                                        .Media
                                                                                        .EXTERNAL_CONTENT_URI,
                                                                                cv);
                                                        try {
                                                            assert uri != null;
                                                            imgOut =
                                                                    getContentResolver()
                                                                            .openOutputStream(uri);
                                                            Bitmap.CompressFormat imageFormat =
                                                                    suffix == "png"
                                                                            ? Bitmap.CompressFormat
                                                                                    .PNG
                                                                            : Bitmap.CompressFormat
                                                                                    .JPEG;

                                                            btmap.compress(
                                                                    imageFormat, 100, imgOut);
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
                            });
                    dialog.show();
                });

        Drawable drawable = binding.draw.getDrawable();
        drawable.setTint(settings.getInt("PAINT_COLOR", Color.GREEN));
        binding.draw.setOnLongClickListener(
                v -> {
                if (paintView.getSelectedPath() != null || paintView.isDrawPath())
                    changeColorOnLongClick(editor, drawable);
                    return true;
                });

        strokeSize.addOnChangeListener(
                (slider, value, userValue) -> {
                    if (paintView.isDrawPath()) {
                        editor.putFloat("STROKEWIDTH", value);
                        editor.apply();
                    }
                    paintView.setStrokeWidth(value);
                });
    }

    private void changeColorOnLongClick(SharedPreferences.Editor editor, Drawable drawable) {
        AmbilWarnaDialog colorDailog =
                new AmbilWarnaDialog(
                        this,
                        settings.getInt("PAINT_COLOR", Color.GREEN),
                        new AmbilWarnaDialog.OnAmbilWarnaListener() {
                            @Override
                            public void onOk(AmbilWarnaDialog dialog, int color) {
                                if (paintView.isDrawPath()) {
                                    editor.putInt("PAINT_COLOR", color);
                                    editor.apply();
                                    drawable.setTint(settings.getInt("PAINT_COLOR", Color.GREEN));
                                }
                                paintView.setStrokeColor(color);
                            }

                            @Override
                            public void onCancel(AmbilWarnaDialog dialog) {}
                        });
        colorDailog.show();
    }

    @Override
    @MainThread
    public void onBackPressed() {
        super.onBackPressed();
        this.binding = null;
    }
}
