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
    isDarkMode: Boolean = false,
    textScale: Float = 1f,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    val htmlContent = remember(docxPages, isDarkMode) {
        val sb = java.lang.StringBuilder()
        val bgColor = if (isDarkMode) "#121212" else "#F3F4F6"
        val textColor = if (isDarkMode) "#ffffff" else "#1F2937"
        val tableBorderColor = if (isDarkMode) "#555555" else "#CBD5E1"

        sb.append("<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes\">")
        sb.append("<style>")
        sb.append("body { background-color: $bgColor; color: $textColor; font-family: sans-serif; padding: 16px; margin: 0; word-wrap: break-word; } ")
        sb.append("table { border-collapse: collapse; width: max-content; max-width: 100%; margin-bottom: 24px; } ")
        sb.append("th, td { border: 1px solid $tableBorderColor; padding: 8px; text-align: left; min-width: 80px; } ")
        sb.append("img { max-width: 100%; height: auto; margin-top: 8px; margin-bottom: 8px; } ")
        sb.append("</style></head><body>")

        docxPages.forEach { page ->
            page.elements.forEach { el ->
                when (el) {
                    is PageElement.HeadingElement -> {
                        val level = el.level.coerceIn(1, 6)
                        val textHtml = el.text.replace("<", "&lt;").replace(">", "&gt;")
                        sb.append("<h$level>$textHtml</h$level>")
                    }
                    is PageElement.TextElement -> {
                        val alignStyle = when (el.alignment) {
                            TextAlign.CENTER -> "text-align: center;"
                            TextAlign.RIGHT -> "text-align: right;"
                            TextAlign.JUSTIFY -> "text-align: justify;"
                            else -> ""
                        }
                        
                        if (el.runs.isNotEmpty()) {
                            val paragraphStyle = if (alignStyle.isNotEmpty()) "style=\"$alignStyle\"" else ""
                            sb.append("<p $paragraphStyle>")
                            el.runs.forEach { run ->
                                var runText = run.text.replace("<", "&lt;").replace(">", "&gt;")
                                if (run.isBold) runText = "<b>$runText</b>"
                                if (run.isItalic) runText = "<i>$runText</i>"
                                if (run.isUnderline) runText = "<u>$runText</u>"
                                val runColorStyle = if (run.color != null) "color: #${Integer.toHexString(run.color!! and 0x00ffffff).padStart(6, '0')};" else ""
                                val runStyleAttr = if (runColorStyle.isNotEmpty()) " style=\"$runColorStyle\"" else ""
                                sb.append("<span$runStyleAttr>$runText</span>")
                            }
                            sb.append("</p>")
                        } else {
                            var htmlText = el.text.replace("<", "&lt;").replace(">", "&gt;")
                            if (el.isBold) htmlText = "<b>$htmlText</b>"
                            if (el.isItalic) htmlText = "<i>$htmlText</i>"
                            if (el.isUnderline) htmlText = "<u>$htmlText</u>"
                            val colorStyle = if (el.color != null) "color: #${Integer.toHexString(el.color!! and 0x00ffffff).padStart(6, '0')};" else ""
                            val style = if (colorStyle.isNotEmpty() || alignStyle.isNotEmpty()) "style=\"$colorStyle $alignStyle\"" else ""
                            sb.append("<p $style>$htmlText</p>")
                        }
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
                                var styleAttr = ""
                                if (cell.backgroundColor != null) {
                                    val hexBg = "#${Integer.toHexString(cell.backgroundColor and 0x00ffffff).padStart(6, '0')}"
                                    styleAttr = " style=\"background-color: $hexBg;\""
                                }
                                val cs = if (cell.colSpan > 1) " colspan=\"${cell.colSpan}\"" else ""
                                val rs = if (cell.rowSpan > 1) " rowspan=\"${cell.rowSpan}\"" else ""
                                
                                sb.append("<td$cs$rs$styleAttr>")
                                cell.elements.forEach { cellEl ->
                                    if (cellEl is PageElement.TextElement) {
                                        var htmlText = cellEl.text.replace("<", "&lt;").replace(">", "&gt;")
                                        if (cellEl.isBold) htmlText = "<b>$htmlText</b>"
                                        if (cellEl.isItalic) htmlText = "<i>$htmlText</i>"
                                        if (cellEl.isUnderline) htmlText = "<u>$htmlText</u>"
                                        val colorStyle = if (cellEl.color != null) "color: #${Integer.toHexString(cellEl.color!! and 0x00ffffff).padStart(6, '0')};" else ""
                                        val alignStyle = when (cellEl.alignment) {
                                            TextAlign.CENTER -> "text-align: center;"
                                            TextAlign.RIGHT -> "text-align: right;"
                                            TextAlign.JUSTIFY -> "text-align: justify;"
                                            else -> ""
                                        }
                                        val textStyle = if (colorStyle.isNotEmpty() || alignStyle.isNotEmpty()) " style=\"$colorStyle $alignStyle margin:0;\"" else " style=\"margin:0;\""
                                        sb.append("<p$textStyle>$htmlText</p>")
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
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            webView.setInitialScale((textScale * 100).toInt())
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
        }
    )
}
