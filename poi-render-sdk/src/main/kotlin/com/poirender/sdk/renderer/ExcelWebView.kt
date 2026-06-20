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
    isDarkMode: Boolean = false,
    textScale: Float = 1f,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    val htmlContent = remember(excelWorkbook, isDarkMode) {
        val sb = java.lang.StringBuilder()
        val bgColor = if (isDarkMode) "#121212" else "#F0FDF4"
        val textColor = if (isDarkMode) "#ffffff" else "#1F2937"
        val excelBorderColor = if (isDarkMode) "#555555" else "#D1D5DB"
        val excelHeaderBg = if (isDarkMode) "#2c2c2c" else "#1D6F42"
        val excelHeaderText = if (isDarkMode) "#ffffff" else "#FFFFFF"

        sb.append("<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes\">")
        sb.append("<style>")
        sb.append("body { background-color: $bgColor; color: $textColor; font-family: sans-serif; padding: 16px; margin: 0; word-wrap: break-word; } ")
        sb.append("table { border-collapse: collapse; width: max-content; max-width: 100%; margin-bottom: 24px; } ")
        sb.append("th, td { border: 1px solid $excelBorderColor; padding: 8px; text-align: left; min-width: 80px; } ")
        sb.append("th { background-color: $excelHeaderBg; color: $excelHeaderText; } ")
        sb.append(".sheet-title { padding: 8px; background-color: $excelHeaderBg; color: $excelHeaderText; font-weight: bold; font-size: 1.2em; margin-top: 24px; } ")
        sb.append("</style></head><body>")

        excelWorkbook.sheets.forEach { sheet ->
            val sheetName = sheet.name.replace("<", "&lt;").replace(">", "&gt;")
            sb.append("<div class=\"sheet-title\">Sheet: $sheetName</div>")
            sb.append("<div style=\"overflow-x: auto;\"><table>")
            sheet.rows.forEachIndexed { rowIndex, row ->
                sb.append("<tr>")
                val isHeader = rowIndex == 0
                val tag = if (isHeader) "th" else "td"
                row.cells.forEach { cell ->
                    if (cell.colSpan > 0 && cell.rowSpan > 0) {
                        val cs = if (cell.colSpan > 1) " colspan=\"${cell.colSpan}\"" else ""
                        val rs = if (cell.rowSpan > 1) " rowspan=\"${cell.rowSpan}\"" else ""
                        
                        var inlineStyle = ""
                        if (cell.backgroundColor != null) {
                            val hexBg = "#${Integer.toHexString(cell.backgroundColor and 0x00ffffff).padStart(6, '0')}"
                            inlineStyle += "background-color: $hexBg; "
                        }
                        if (cell.textColor != null) {
                            val hexText = "#${Integer.toHexString(cell.textColor and 0x00ffffff).padStart(6, '0')}"
                            inlineStyle += "color: $hexText; "
                        }
                        
                        var content = cell.value.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>")
                        if (cell.isBold) content = "<b>$content</b>"
                        if (cell.isItalic) content = "<i>$content</i>"
                        if (cell.isUnderline) content = "<u>$content</u>"
                        
                        val styleAttr = if (inlineStyle.isNotEmpty()) " style=\"$inlineStyle\"" else ""
                        
                        sb.append("<$tag$cs$rs$styleAttr>$content</$tag>")
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
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            webView.setInitialScale((textScale * 100).toInt())
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
        }
    )
}
