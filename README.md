# OpenOfficeKit

[![Release](https://jitpack.io/v/prathap021/Open-Office-kit.svg)](https://jitpack.io/#prathap021/Open-Office-kit)

OpenOfficeKit is an advanced, hybrid SDK for parsing and rendering Microsoft Office documents (both Modern XML like DOCX, XLSX, PPTX, and Legacy OLE2 like DOC, XLS) directly in Android using Apache POI and high-performance WebViews embedded in Jetpack Compose.

## Features Supported

The SDK has been fundamentally aligned with the complete Open Office structure and supports a rich variety of content out-of-the-box via highly modular Compose WebViews.

### 📱 Core Viewer Capabilities (Shared Features)
*   **High Performance Rendering:** Uses a native Compose AndroidView wrapping a specialized WebView to convert extracted Apache POI document models into highly scalable, responsive HTML/CSS structures.
*   **Legacy Format Support:** Seamlessly parses both newer XML formats (`.docx`, `.xlsx`, `.pptx`) and older OLE2 formats (`.doc`, `.xls`) utilizing `poi-scratchpad` fallback logic.
*   **Native Searching:** Integrated `TopAppBar` search bar using the underlying WebView's native search engine (`findAllAsync` and `findNext`), allowing blazing fast text search with Next/Previous highlighting without breaking scroll state.
*   **Dynamic UI Theming:** The toolbar and document backgrounds automatically theme themselves to match the specific file type (Word Blue, Excel Green, PowerPoint Red) using native `colors.xml` parameters.
*   **Accessibility:**
    *   Dark / Light Mode toggle.
    *   Dynamic Text Scaling/Zoom parameters injected directly into the HTML engine.
    *   Loading Skeleton UI while background coroutines parse the file.
*   **Error & Edge States:**
    *   Password-protected file lock prompts.
    *   "Unsupported file type" and corrupted file error screens.

### 📄 DOCX & DOC (Word Documents)
*   **Headings:** H1-H6 scaled appropriately.
*   **Paragraphs & Runs:** Supports complex inline text formats via `TextRun` extraction:
    *   Bold, Italic, Underline
    *   Mixed text colors and highlighting within the same paragraph.
*   **Lists:** Ordered, Unordered, and Nested / Indented lists via text formatting.
*   **Tables:** 
    *   Accurate cell grid systems with native HTML `colspan` and `rowspan` behavior.
    *   Extracts native cell background colors and inner paragraph formatting.

### 📊 XLSX & XLS (Excel Spreadsheets)
*   **Responsive Grid:** Horizontal scrolling HTML tables mapped directly to spreadsheet sheets.
*   **Cell Content:** Text, Boolean, and Number values mapped seamlessly.
*   **Cell Formatting:**
    *   Extracts literal Hex `backgroundColor` and `textColor` from cells and applies them inline.
    *   Bold, Italic, Underline properties.
    *   Correct parsing of merged cells across rows and columns.

### 📽️ PPTX (PowerPoint Presentations)
*   **Presentation Flow:** Renders slides chronologically in a scrolling web view.
*   **Dynamic Theming:** Extracts individual `slide.backgroundColor` or falls back to standard PPTX dark/light themes.
*   **Shape Extraction:**
    *   Text shapes maintain exact relative font sizing and Hex font coloring.
    *   Images are seamlessly converted to `Base64` and embedded into the HTML flow.

## Installation (JitPack)

Add the JitPack repository to your `settings.gradle.kts` or `build.gradle.kts` (project level):

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.prathap021:Open-Office-kit:v0.1.0-beta")
}
```

## SDK Usage

Integrating `OpenOfficeKit` into your Jetpack Compose application is straightforward. The SDK handles heavy parsing on background threads and exposes customized WebViews for each file type.

### 1. Initialize the SDK
Initialize the SDK singleton using your application or activity context:

```kotlin
import com.poirender.sdk.PoiRenderSDK

val officeSDK = PoiRenderSDK.init(context)
```

### 2. Parse and Render Documents

#### Word Documents (DOCX / DOC)
```kotlin
import com.poirender.sdk.renderer.DocxWebView

// 1. Parsing
lifecycleScope.launch {
    officeSDK.parseDocx(fileUri) { progress ->
        Log.d("SDK", "Loading DOCX: ${progress * 100}%")
    }.onSuccess { pages ->
        // 2. Rendering in Compose
        setContent {
            DocxWebView(
                docxPages = pages,
                isDarkMode = false,
                textScale = 1f,
                onWebViewCreated = { webView -> 
                    // Optional: Reference the WebView to use native methods like webView.findAllAsync("Search Text")
                }
            )
        }
    }.onFailure { error ->
        Log.e("SDK", "Error parsing DOCX", error)
    }
}
```

#### Excel Spreadsheets (XLSX / XLS)
```kotlin
import com.poirender.sdk.renderer.ExcelWebView

// 1. Parsing
lifecycleScope.launch {
    officeSDK.parseExcel(fileUri) { progress ->
        Log.d("SDK", "Loading Excel: ${progress * 100}%")
    }.onSuccess { workbookData ->
        // 2. Rendering in Compose
        setContent {
            ExcelWebView(
                excelWorkbook = workbookData,
                isDarkMode = false,
                textScale = 1f,
                onWebViewCreated = { webView -> 
                    // Store WebView reference for Search navigation
                }
            )
        }
    }.onFailure { error ->
        Log.e("SDK", "Error parsing Excel", error)
    }
}
```

#### PowerPoint Presentations (PPTX)
```kotlin
import com.poirender.sdk.renderer.PptxWebView

// 1. Parsing
lifecycleScope.launch {
    officeSDK.parsePptx(fileUri) { progress ->
        Log.d("SDK", "Loading PPTX: ${progress * 100}%")
    }.onSuccess { slides ->
        // 2. Rendering in Compose
        setContent {
            PptxWebView(
                pptxSlides = slides,
                isDarkMode = false,
                textScale = 1f,
                onWebViewCreated = { webView -> 
                    // Handle WebView interactions
                }
            )
        }
    }.onFailure { error ->
        Log.e("SDK", "Error parsing PPTX", error)
    }
}
```

## Getting Started
The `app` module contains a complete sample `DocumentViewerActivity`. It demonstrates loading an Office file, dynamically changing the TopAppBar themes via `colors.xml`, and executing active text searches across the generated WebViews.
