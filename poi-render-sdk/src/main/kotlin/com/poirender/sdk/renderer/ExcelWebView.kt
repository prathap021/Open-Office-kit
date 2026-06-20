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
fun ExcelWebView(
    excelWorkbook: WorkbookData,
    searchQuery: String = "",
    isDarkMode: Boolean = false,
    textScale: Float = 1f
) {
    val htmlContent = remember(excelWorkbook, searchQuery, isDarkMode) {
        val sb = java.lang.StringBuilder()
        val bgColor = if (isDarkMode) "#121212" else "#ffffff"
        val textColor = if (isDarkMode) "#ffffff" else "#000000"
        val excelBorderColor = if (isDarkMode) "#555555" else "#dddddd"
        val excelHeaderBg = if (isDarkMode) "#2c2c2c" else "#f0f0f0"

        sb.append("<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes\">")
        sb.append("<style>")
        sb.append("body { background-color: $bgColor; color: $textColor; font-family: sans-serif; padding: 16px; margin: 0; word-wrap: break-word; } ")
        sb.append("table { border-collapse: collapse; width: max-content; max-width: 100%; margin-bottom: 24px; } ")
        sb.append("th, td { border: 1px solid $excelBorderColor; padding: 8px; text-align: left; min-width: 80px; } ")
        sb.append("th { background-color: $excelHeaderBg; } ")
        sb.append(".highlight { background-color: yellow; color: black; } ")
        sb.append(".sheet-title { padding: 8px; background-color: $excelHeaderBg; font-weight: bold; font-size: 1.2em; margin-top: 24px; } ")
        sb.append("</style></head><body>")

        fun highlightText(text: String?): String {
            if (text == null) return ""
            val escapedText = text.replace("<", "&lt;").replace(">", "&gt;")
            if (searchQuery.isBlank()) return escapedText
            val escapedSearch = searchQuery.replace("<", "&lt;").replace(">", "&gt;")
            return escapedText.replace(Regex("(${Regex.escape(escapedSearch)})", RegexOption.IGNORE_CASE), "<span class=\"highlight\">$1</span>")
        }

        excelWorkbook.sheets.forEach { sheet ->
            sb.append("<div class=\"sheet-title\">Sheet: ${highlightText(sheet.name)}</div>")
            sb.append("<div style=\"overflow-x: auto;\"><table>")
            sheet.rows.forEachIndexed { rowIndex, row ->
                sb.append("<tr>")
                val isHeader = rowIndex == 0
                val tag = if (isHeader) "th" else "td"
                row.cells.forEach { cell ->
                    if (cell.colSpan > 0 && cell.rowSpan > 0) {
                        val cs = if (cell.colSpan > 1) " colspan=\"${cell.colSpan}\"" else ""
                        val rs = if (cell.rowSpan > 1) " rowspan=\"${cell.rowSpan}\"" else ""
                        val content = highlightText(cell.value).replace("\n", "<br/>")
                        sb.append("<$tag$cs$rs>$content</$tag>")
                    }
                }
                sb.append("</tr>")
            }
            sb.append("</table></div>")
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
