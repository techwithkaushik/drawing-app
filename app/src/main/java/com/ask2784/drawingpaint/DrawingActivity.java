package com.ask2784.drawingpaint;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.icu.text.DecimalFormat;
import android.provider.MediaStore;
import androidx.annotation.MainThread;
import java.io.OutputStream;
import android.content.ContentValues;
import android.os.Environment;
import android.net.Uri;
import android.widget.Toast;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.ask2784.drawingpaint.databinding.ActivityDrawingBinding;
import com.google.android.material.slider.RangeSlider;

import yuku.ambilwarna.AmbilWarnaDialog;

public class DrawingActivity extends AppCompatActivity {
    private ActivityDrawingBinding binding;
    private PaintView paintView;
    private RangeSlider strokeSize;
    private SharedPreferences settings;
    private boolean showStrokeSize = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.WHITE);
        binding = ActivityDrawingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        binding.draw.setOnClickListener(
                v -> {
                    paintView.startDrawing();
                });

        paintView = binding.drawLayout;
        strokeSize = binding.changeSize;
        settings = getSharedPreferences("PAINTVIEW", MODE_PRIVATE);

        binding.undo.setOnClickListener(
                v -> {
                    paintView.undo();
                });

        binding.redo.setOnClickListener(
                v -> {
                    paintView.redo();
                });

        binding.clearDrawing.setOnClickListener(
                v -> {
                    paintView.clear();
                });

        binding.selectPath.setOnClickListener(
                v -> {
                    paintView.startSelect();
                });
        binding.strokeSize.setOnClickListener(
                v -> {
                    showStrokeSize = !showStrokeSize ? true : false;
                    strokeSize.setVisibility(showStrokeSize ? View.VISIBLE : View.GONE);
                });
        binding.saveImage.setOnClickListener(
                v -> {
                    Bitmap btmap = paintView.exportImage();
                    OutputStream imgOut = null;
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Images.Media.DISPLAY_NAME, "drawing.png");
                    cv.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                    cv.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
                    Uri uri =
                            getContentResolver()
                                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
                    try {
                        imgOut = getContentResolver().openOutputStream(uri);
                        btmap.compress(Bitmap.CompressFormat.PNG, 100, imgOut);
                        imgOut.close();
                        Toast.makeText(this, "Image Saved at " + uri.getPath(), Toast.LENGTH_SHORT)
                                .show();
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                });
        SharedPreferences.Editor editor = settings.edit();
        binding.changeColor.setBackgroundColor(settings.getInt("PAINT_COLOR", Color.GREEN));
        binding.changeColor.setOnClickListener(
                v -> {
                    AmbilWarnaDialog colorDailog =
                            new AmbilWarnaDialog(
                                    this,
                                    settings.getInt("PAINT_COLOR", Color.GREEN),
                                    new AmbilWarnaDialog.OnAmbilWarnaListener() {
                                        @Override
                                        public void onOk(AmbilWarnaDialog dialog, int color) {
                                            if (paintView.isDrawPath()) {
                                                editor.putInt("PAINT_COLOR", color);
                                                editor.commit();
                                                binding.changeColor.setBackgroundColor(color);
                                            }
                                            paintView.setStrokeColor(color);
                                        }

                                        @Override
                                        public void onCancel(AmbilWarnaDialog dialog) {
                                            return;
                                        }
                                    });
                    colorDailog.show();
                });

        strokeSize.setValueFrom(0.f);
        strokeSize.setValueTo(50.0f);
        strokeSize.setValues(settings.getFloat("STROKEWIDTH", 5.0f));
        binding.strokeSize.setText("" + sizeFormat.format(settings.getFloat("STROKEWIDTH", 5.0f)));
        strokeSize.addOnChangeListener(
                new RangeSlider.OnChangeListener() {

                    @Override
                    public void onValueChange(RangeSlider slider, float value, boolean userValue) {
                        if (paintView.isDrawPath()) {
                            editor.putFloat("STROKEWIDTH", value);
                            editor.commit();
                            binding.strokeSize.setText("" + sizeFormat.format(value));
                        }
                        paintView.setStrokeWidth(value);
                    }
                });
    }

    @Override
    @MainThread
    public void onBackPressed() {
        super.onBackPressed();
        this.binding = null;
    }

    private DecimalFormat sizeFormat = new DecimalFormat("#.##");
}
