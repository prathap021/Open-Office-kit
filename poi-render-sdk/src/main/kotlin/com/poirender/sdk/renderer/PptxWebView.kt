package com.poirender.sdk.renderer

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.poirender.sdk.model.*

@Composable
fun PptxWebView(
    pptxSlides: List<SlideData>,
    searchQuery: String = "",
    isDarkMode: Boolean = false,
    textScale: Float = 1f
) {
    val htmlContent = remember(pptxSlides, searchQuery, isDarkMode) {
        val sb = java.lang.StringBuilder()
        val bgColor = if (isDarkMode) "#121212" else "#ffffff"
        val textColor = if (isDarkMode) "#ffffff" else "#000000"
        val headerBg = if (isDarkMode) "#2c2c2c" else "#f0f0f0"

        sb.append("<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes\">")
        sb.append("<style>")
        sb.append("body { background-color: $bgColor; color: $textColor; font-family: sans-serif; padding: 16px; margin: 0; word-wrap: break-word; } ")
        sb.append("img { max-width: 100%; height: auto; margin-top: 8px; margin-bottom: 8px; } ")
        sb.append(".highlight { background-color: yellow; color: black; } ")
        sb.append(".slide-title { padding: 8px; background-color: $headerBg; font-weight: bold; font-size: 1.2em; margin-top: 24px; } ")
        sb.append("</style></head><body>")

        fun highlightText(text: String?): String {
            if (text == null) return ""
            val escapedText = text.replace("<", "&lt;").replace(">", "&gt;")
            if (searchQuery.isBlank()) return escapedText
            val escapedSearch = searchQuery.replace("<", "&lt;").replace(">", "&gt;")
            return escapedText.replace(Regex("(${Regex.escape(escapedSearch)})", RegexOption.IGNORE_CASE), "<span class=\"highlight\">$1</span>")
        }

        pptxSlides.forEachIndexed { index, slide ->
            sb.append("<div class=\"slide-title\">Slide ${index + 1}</div>")
            slide.shapes.forEach { shape ->
                when (shape) {
                    is SlideShape.TextShape -> sb.append("<p>${highlightText(shape.text)}</p>")
                    is SlideShape.ImageShape -> {
                        val base64 = android.util.Base64.encodeToString(shape.imageData, android.util.Base64.NO_WRAP)
                        sb.append("<img src=\"data:image/png;base64,$base64\"/>")
                    }
                    else -> {}
                }
            }
            sb.append("<hr/>")
        }

        sb.append("</body></html>")
        sb.toString()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = false
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.setSupportZoom(true)
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            webView.setInitialScale((textScale * 100).toInt())
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
        }
    )
}
