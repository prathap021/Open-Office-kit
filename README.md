# OpenOfficeKit

OpenOfficeKit is an advanced, native Jetpack Compose SDK for parsing and rendering Microsoft Office documents (DOCX, XLSX, PPTX) directly in Android using Apache POI.

## Features Supported

The SDK has been fundamentally aligned with the complete Open Office structure and supports a rich variety of content out-of-the-box via Jetpack Compose.

### 📱 Core Viewer Capabilities (Shared Features)
*   **Navigation:** Smooth Pinch-to-zoom and Pan/Drag gestures via native Compose pointers.
*   **Zoom Controls:** Toolbar dropdown for exact scaling (50%, 75%, 100%, Fit to Width / Screen).
*   **Toolbar & Search:** Persistent TopAppBar with File Name, Format Badge, and an interactive Search/Find field that highlights text across all document types.
*   **Utility Menu:** Overflow options for Printing, Sharing links, and Downloading files.
*   **Accessibility:**
    *   Dark / Light Mode toggle.
    *   Text Size Overrides (+20% zooming).
    *   High Contrast toggle.
    *   Loading Skeleton UI while background threads parse the file.
*   **Error & Edge States:**
    *   Password-protected file lock prompts.
    *   "Unsupported file type" and corrupted file error screens.
    *   "Failed to load" states with manual Retry buttons.

### 📄 DOCX (Word Documents)
*   **Headings:** H1-H6 scaled appropriately.
*   **Paragraphs & Runs:** Supports complex inline text formats containing:
    *   Bold, Italic, Underline
    *   Strikethrough
    *   Superscript / Subscript
    *   Highlights / Colors
    *   Hyperlinks
*   **Lists:** Ordered, Unordered, and Nested / Indented lists via text formatting.
*   **Tables:** 
    *   Row and column structures natively built with Compose.
    *   Merged cells (colSpan & rowSpan).
    *   Cell borders, background shading, and text alignment.
*   **Media & Layout:**
    *   Inline Images.
    *   Paragraph indentation and line spacing.
    *   Page breaks and Dividers.
    *   Footnotes / Endnotes references.

### 📊 XLSX (Excel Spreadsheets)
*   **Sheet Structure:**
    *   Tabbed interface for navigating multiple sheets.
    *   Complete grid UI using a specialized absolute Compose `Box` layout.
    *   Accurate render of Merged Cells spanning both rows and columns.
    *   Frozen panes and Hidden rows/columns mapping capabilities.
*   **Cell Content:**
    *   Text, Boolean, and Number values mapping.
    *   Formula extraction and display.
    *   Comments / notes attached to specific cells.
*   **Cell Formatting:**
    *   Bold, Italic, Underline, and Text Wrap.
    *   Number formats (%, $, etc.)
    *   Fill / Background color and Text color.
    *   Cell borders and Text alignment.

### 📽️ PPTX (PowerPoint Presentations)
*   **Presentation Navigation UI:**
    *   **Slide Thumbnail Panel**: Scrollable horizontal panel to tap and jump to any slide.
    *   **Speaker Notes Panel**: Toggleable drawer containing the presenter's notes for the current slide.
    *   **Navigation Bar**: Prev, Next, and Jump actions.
    *   **Fullscreen Mode**: Tap a slide to hide controls and enter fullscreen slideshow mode.
*   **Mobile-Fidelity Slide Rendering (New):**
    *   **Fitted Viewports**: Slides strictly preserve their original aspect ratio and scale responsively to fit the mobile screen, avoiding clipping or distortion.
    *   **Dynamic Scaling**: Text, shapes, and images use precise scaled coordinate systems relative to the viewport.
    *   **High Color Fidelity**: Extracts actual run-level font colors and exact shape fill/stroke colors using AWT `PaintStyle` translations.
    *   **Advanced Shapes**: Draws accurate inner grid lines for `TableShape` using rows/cols data and dynamic miniature representations for `ChartShape` (e.g. Pie, Bar, Line).
*   **Slide Content Rendering:**
    *   Slide titles, body text, and complex multi-level Bullet Lists.
    *   Font size clamping, color, bold formatting, and text alignments.
    *   Images and Background Fills.
    *   Connectors / Arrows painted precisely via the Jetpack Compose `Canvas`.

## SDK Usage

Integrating `OpenOfficeKit` into your Jetpack Compose application is straightforward. The SDK handles heavy parsing on background threads using coroutines.

### 1. Initialize the SDK
Initialize the SDK singleton using your application or activity context:

```kotlin
import com.poirender.sdk.PoiRenderSDK

val officeSDK = PoiRenderSDK.init(context)
```

### 2. Parse a Document
Use the provided coroutine suspend functions to safely parse a file URI. You can optionally listen to parsing progress.

```kotlin
// Example: Parsing a PPTX file
lifecycleScope.launch {
    val result = officeSDK.parsePptx(fileUri) { progress ->
        // progress is a Float between 0.0f and 1.0f
        Log.d("SDK", "Loading: ${progress * 100}%")
    }

    result.onSuccess { slides ->
        // Document parsed successfully!
        // Pass 'slides' to your Jetpack Compose UI
    }.onFailure { error ->
        // Handle parsing error / corrupted file
    }
}
```
*(Use `parseDocx` for Word documents or `parseExcel` for Spreadsheets)*

### 3. Render in Jetpack Compose
Pass the parsed data directly to the built-in Compose renderers. They are highly optimized and handle all native drawing.

```kotlin
import com.poirender.sdk.renderer.PptxRenderer

@Composable
fun DocumentViewerScreen(slides: List<SlideData>) {
    // Renders the fully featured PPTX viewer
    PptxRenderer(
        slides = slides,
        searchQuery = "" // Optional: dynamically highlight matching text
    )
}
```

## Getting Started
The `app` module contains a complete sample `DocumentViewerActivity`. Simply launch it, pick an office file, and the SDK will automatically read the POI stream and trigger the appropriate parser and renderer.
