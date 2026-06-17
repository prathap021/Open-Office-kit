# Project Overview: OpenOfficeKit

## Mission
OpenOfficeKit provides a fully native, Jetpack Compose-driven SDK for rendering complex Microsoft Office formats (DOCX, XLSX, PPTX) completely offline in Android apps, leveraging the robust Apache POI parsing engine.

## Architecture Structure

The project is split into two primary layers:

### 1. `poi-render-sdk` (The Core Library)
This module acts as the heavy-lifting engine for the project. It removes the complexities of dealing with generic XML/Document data and provides a clean, reactive set of Compose UI elements.

- **Data Models (`model/`)**: 
  - The SDK translates raw Apache POI classes (`XWPFDocument`, `XSSFWorkbook`, `XMLSlideShow`) into abstracted Kotlin Data Classes (`DocumentPage`, `WorkbookData`, `SlideData`).
  - These models act as the absolute source of truth. They are rigorously mapped to support deep formatting features (e.g. `colSpan`/`rowSpan` for tables, rich `TextRun` styling for inline formatting, and Slide shape vectors).
- **Parsers (`parser/`)**:
  - `DocxParser`, `ExcelParser`, and `PptxParser` handle the `InputStream` reading asynchronously, shielding the main thread from heavy I/O operations and mapping the stream into the Data Models.
- **Renderers (`renderer/`)**:
  - `DocxRenderer`: Generates a lazy-loaded rich text document using Compose `AnnotatedString` and nested `Row`/`Column` setups.
  - `ExcelRenderer`: Implements a high-performance, absolutely-positioned Compose `Box` grid, accurately laying out complex spreadsheet cells (including merges) while skipping obscured geometries.
  - `PptxRenderer`: Implements a full vertical-scrolling slideshow viewer using a `LazyColumn`, eliminating mobile collapse issues. It uses a fitted viewport coordinate system inside a custom `Canvas` capable of accurately scaling and drawing vector shapes, text clamped to safe bounds, images, and dynamic grids/placeholders for tables and charts precisely relative to the slide's original aspect ratio.

### 2. `app` (The Implementation Demo)
The `app` module demonstrates the exact way to implement `poi-render-sdk` into a modern Android ecosystem.

- **`DocumentViewerActivity`**:
  - Automatically handles `Intent` routing for `.docx`, `.xlsx`, and `.pptx` documents via the `ContentResolver`.
  - Implements the "Shared Core Viewer Capabilities" wrapper. This is a massive Compose layer that provides:
    - Pan/Pinch-to-zoom gestures via `detectTransformGestures`.
    - Advanced UI states (Loading Skeletons, Password Prompts, Error Handlers with Retry logic).
    - Utility and Accessibility Toolbars (Theme toggling, Search highlighters, Scaling, Mock sharing/printing).

## Technology Stack
- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Document Engine**: Apache POI (poi-ooxml)
- **Coroutines**: Native Kotlin Coroutines & `LaunchedEffect` for async parsing.
