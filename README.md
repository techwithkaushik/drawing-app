# Let's Draw — Advanced Android Drawing Studio

A lightweight native Android drawing app built with Java, Android Canvas and Material 3 components.

## Features

- **Freehand brush** with smooth strokes and adjustable stroke width.
- **Shape tools:** line, rectangle, square, circle and triangle, with outline or filled shapes.
- **Color picker** with alpha and brightness controls.
- **Eraser** for removing marks on the white canvas.
- **Undo and redo**, including restoring strokes after Clear.
- **Selection and move** tools for adjusting existing strokes.
- **Pinch-to-zoom and two-finger pan**, plus a Reset Zoom / Pan menu action.
- **Custom solid canvas background** with a matching eraser color.
- **Editable project save/load** in app-private storage, using a versioned project format.
- **Export and share** artwork as PNG or JPEG.
- **Modern home screen** with direct access to new drawings and saved projects.
- **No broad storage permission** needed to export images through Android MediaStore.
- **On-demand export rendering** avoids allocating a full-size bitmap during every canvas redraw.
- **Debug CI builds** do not require release keystore secrets.

## Build

Requirements: JDK 17+, Android SDK platform 34, and network access for Gradle dependencies.

```bash
git clone https://github.com/techwithkaushik/drawing-app.git
cd drawing-app
./gradlew assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

For a release build, provide the signing environment variables:
`SIGNING_KEY_STORE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, and `SIGNING_KEY_PASSWORD`.

## Project storage

Projects are saved privately inside the app's internal files directory under `drawing-projects/`.
They remain available to the app and are listed from the home screen. Uninstalling the app removes its private project files unless they have been backed up.

The project format stores flattened path coordinates and stroke settings. Saving/loading is intended for editable drawing projects; it is not a vector interchange format.

## Current limitations

- Eraser paints white and is intended for the white canvas; it does not erase to transparency.
- Transparent backgrounds and importing a reference image are not implemented yet.
- Project paths are flattened when saved, so some curve precision may be lost after reopening.
- Zoom and pan affect the editor viewport; exports render the drawing at the canvas view's pixel dimensions.

## Contributing

Issues and pull requests are welcome. Keep changes lightweight and compatible with the project's native Java/Android architecture.

## Author

[Arvind Kaushik](https://github.com/ask9027)
