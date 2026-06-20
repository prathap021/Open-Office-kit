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
    isDarkMode: Boolean = false,
    textScale: Float = 1f,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    val htmlContent = remember(pptxSlides, isDarkMode) {
        val sb = java.lang.StringBuilder()
        val bgColor = if (isDarkMode) "#121212" else "#1F1F1F"
        val textColor = if (isDarkMode) "#ffffff" else "#2D2D2D"
        val headerBg = if (isDarkMode) "#2c2c2c" else "#B7322C"
        val headerText = "#FFFFFF"
        val slideBgColor = if (isDarkMode) "#1E1E1E" else "#FFFFFF"

        sb.append("<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes\">")
        sb.append("<style>")
        sb.append("body { background-color: $bgColor; color: $textColor; font-family: sans-serif; padding: 16px; margin: 0; word-wrap: break-word; } ")
        sb.append("img { max-width: 100%; height: auto; margin-top: 8px; margin-bottom: 8px; } ")
        sb.append(".slide-title { padding: 8px; background-color: $headerBg; color: $headerText; font-weight: bold; font-size: 1.2em; margin-top: 24px; } ")
        sb.append(".slide-container { background-color: $slideBgColor; padding: 16px; margin-bottom: 24px; border: 1px solid #ccc; } ")
        sb.append("</style></head><body>")

        pptxSlides.forEachIndexed { index, slide ->
            val slideBg = "#${Integer.toHexString(slide.backgroundColor and 0x00ffffff).padStart(6, '0')}"
            sb.append("<div style=\"background-color: $slideBg; padding: 16px; margin-bottom: 24px; border: 1px solid #ccc;\">")
            sb.append("<div class=\"slide-title\">Slide ${index + 1}</div>")
            slide.shapes.forEach { shape ->
                when (shape) {
                    is SlideShape.TextShape -> {
                        val textColorHex = "#${Integer.toHexString(shape.color and 0x00ffffff).padStart(6, '0')}"
                        var content = shape.text.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>")
                        if (shape.isBold) content = "<b>$content</b>"
                        val mobileFontSize = (shape.fontSize * 0.5f).toInt().coerceAtLeast(12)
                        sb.append("<p style=\"color: $textColorHex; font-size: ${mobileFontSize}px; margin-top: 4px; margin-bottom: 4px;\">$content</p>")
                    }
                    is SlideShape.ImageShape -> {
                        val base64 = android.util.Base64.encodeToString(shape.imageData, android.util.Base64.NO_WRAP)
                        sb.append("<img src=\"data:image/png;base64,$base64\"/>")
                    }
                    else -> {}
                }
            }
            sb.append("</div>")
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
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            webView.setInitialScale((textScale * 100).toInt())
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
        }
    )
}
