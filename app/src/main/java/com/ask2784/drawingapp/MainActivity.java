package com.ask2784.drawingapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ask2784.drawingapp.databinding.ActivityMainBinding;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.newDrawing.setOnClickListener(
                v -> startActivity(new Intent(this, DrawingActivity.class)));
        binding.openProjects.setOnClickListener(v -> openSavedProjects());
    }

    private void openSavedProjects() {
        try {
            File[] projects = DrawingProjectStore.list(getFilesDir());
            if (projects.length == 0) {
                Toast.makeText(this, "No saved projects yet. Create a drawing and choose Save Project.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            String[] names = new String[projects.length];
            for (int i = 0; i < projects.length; i++) {
                String storedName = projects[i].getName();
                names[i] = storedName.substring(0, storedName.length() - ".draw".length());
            }
            final int[] selectedIndex = {0};
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Your projects")
                    .setSingleChoiceItems(names, 0, (whichDialog, which) -> selectedIndex[0] = which)
                    .setPositiveButton("Open", null)
                    .setNeutralButton("Delete", null)
                    .setNegativeButton("Cancel", null)
                    .create();
            dialog.setOnShowListener(ignored -> {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    Intent intent = new Intent(this, DrawingActivity.class);
                    intent.putExtra("project_name", names[selectedIndex[0]]);
                    startActivity(intent);
                    dialog.dismiss();
                });
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                    int index = selectedIndex[0];
                    new AlertDialog.Builder(this)
                            .setTitle("Delete project?")
                            .setMessage("Delete \"" + names[index] + "\" from this device? This cannot be undone.")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Delete", (confirm, which) -> {
                                if (projects[index].delete()) {
                                    Toast.makeText(this, "Project deleted.", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(this, "Could not delete project.", Toast.LENGTH_LONG).show();
                                }
                            })
                            .show();
                });
            });
            dialog.show();
        } catch (IOException error) {
            Toast.makeText(this, "Could not list projects: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
