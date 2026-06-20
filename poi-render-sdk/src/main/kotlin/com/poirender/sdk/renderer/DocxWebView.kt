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
fun DocxWebView(
    docxPages: List<DocumentPage>,
    searchQuery: String = "",
    isDarkMode: Boolean = false,
    textScale: Float = 1f
) {
    val htmlContent = remember(docxPages, searchQuery, isDarkMode) {
        val sb = java.lang.StringBuilder()
        val bgColor = if (isDarkMode) "#121212" else "#ffffff"
        val textColor = if (isDarkMode) "#ffffff" else "#000000"
        val tableBorderColor = if (isDarkMode) "#555555" else "#dddddd"

        sb.append("<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes\">")
        sb.append("<style>")
        sb.append("body { background-color: $bgColor; color: $textColor; font-family: sans-serif; padding: 16px; margin: 0; word-wrap: break-word; } ")
        sb.append("table { border-collapse: collapse; width: max-content; max-width: 100%; margin-bottom: 24px; } ")
        sb.append("th, td { border: 1px solid $tableBorderColor; padding: 8px; text-align: left; min-width: 80px; } ")
        sb.append("img { max-width: 100%; height: auto; margin-top: 8px; margin-bottom: 8px; } ")
        sb.append(".highlight { background-color: yellow; color: black; } ")
        sb.append("</style></head><body>")

        fun highlightText(text: String?): String {
            if (text == null) return ""
            val escapedText = text.replace("<", "&lt;").replace(">", "&gt;")
            if (searchQuery.isBlank()) return escapedText
            val escapedSearch = searchQuery.replace("<", "&lt;").replace(">", "&gt;")
            return escapedText.replace(Regex("(${Regex.escape(escapedSearch)})", RegexOption.IGNORE_CASE), "<span class=\"highlight\">$1</span>")
        }

        docxPages.forEach { page ->
            page.elements.forEach { el ->
                when (el) {
                    is PageElement.HeadingElement -> {
                        val level = el.level.coerceIn(1, 6)
                        sb.append("<h$level>${highlightText(el.text)}</h$level>")
                    }
                    is PageElement.TextElement -> {
                        var htmlText = highlightText(el.text)
                        if (el.isBold) htmlText = "<b>$htmlText</b>"
                        if (el.isItalic) htmlText = "<i>$htmlText</i>"
                        if (el.isUnderline) htmlText = "<u>$htmlText</u>"
                        val colorStyle = if (el.color != null) "color: #${Integer.toHexString(el.color!! and 0x00ffffff).padStart(6, '0')};" else ""
                        val alignStyle = when (el.alignment) {
                            TextAlign.CENTER -> "text-align: center;"
                            TextAlign.RIGHT -> "text-align: right;"
                            TextAlign.JUSTIFY -> "text-align: justify;"
                            else -> ""
                        }
                        val style = if (colorStyle.isNotEmpty() || alignStyle.isNotEmpty()) "style=\"$colorStyle $alignStyle\"" else ""
                        sb.append("<p $style>$htmlText</p>")
                    }
                    is PageElement.Divider -> sb.append("<hr/>")
                    is PageElement.ImageElement -> {
                        val base64 = android.util.Base64.encodeToString(el.imageData, android.util.Base64.NO_WRAP)
                        sb.append("<img src=\"data:image/png;base64,$base64\"/>")
                    }
                    is PageElement.TableElement -> {
                        sb.append("<div style=\"overflow-x: auto;\"><table>")
                        el.rows.forEach { row ->
                            sb.append("<tr>")
                            row.forEach { cell ->
                                sb.append("<td>")
                                cell.elements.forEach { cellEl ->
                                    if (cellEl is PageElement.TextElement) {
                                        sb.append("<p style=\"margin:0;\">${highlightText(cellEl.text)}</p>")
                                    }
                                }
                                sb.append("</td>")
                            }
                            sb.append("</tr>")
                        }
                        sb.append("</table></div>")
                    }
                    else -> {}
                }
            }
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
