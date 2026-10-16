# Let's Draw — Advanced Android Drawing Studio

A lightweight native Android drawing app built with Java, Android Canvas and Material Components.

## Features

- **Freehand drawing** with smooth rounded strokes.
- **Shape tools:** line, rectangle, square, circle and triangle.
- **Brush customization:** color picker with alpha/brightness controls and adjustable stroke width.
- **Eraser tool** for removing strokes on the white canvas.
- **Undo and redo** for recent drawing actions.
- **Selection mode** for selecting and adjusting existing strokes.
- **Export and share** artwork as PNG or JPEG through Android's MediaStore and Android Sharesheet.
- **No broad storage permission** needed to save exported images on modern Android.
- **On-demand export rendering** avoids allocating a full-size bitmap during every canvas redraw.
- **Debug builds** use Android's standard debug signing and do not require release keystore secrets.

## Build

Requirements: JDK 17+, Android SDK platform 34, and network access for Gradle dependencies.

```bash
git clone https://github.com/techwithkaushik/drawing-app.git
cd drawing-app
./gradlew assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

For a release build, provide the signing environment variables used by the CI workflow:
`SIGNING_KEY_STORE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, and `SIGNING_KEY_PASSWORD`.

## Usage

1. Start a new drawing.
2. Choose the brush or a shape tool from the drawing toolbar.
3. Set the stroke color and width.
4. Use Undo/Redo as needed, or select the eraser to paint over marks on the white canvas.
5. Export to PNG/JPEG or share the image with another app.

## Known limitations

- Project save/load and canvas zoom are not implemented yet.
- Erasing paints white, so it is intended for the white canvas rather than transparent artwork.
- The canvas currently uses a single white background.

## Contributing

Issues and pull requests are welcome. Keep changes lightweight and compatible with the project's native Java/Android architecture.

## Author

[Arvind Kaushik](https://github.com/ask9027)
