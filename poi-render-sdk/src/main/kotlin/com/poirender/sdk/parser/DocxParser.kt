package com.poirender.sdk.parser

import androidx.core.graphics.toColorInt
import com.poirender.sdk.model.*
import com.poirender.sdk.util.SDKLog
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.xwpf.usermodel.*
import java.io.InputStream
import java.io.FileInputStream

class DocxParser {
    private val TAG = "DocxParser"

    fun parse(inputStream: InputStream, onProgress: ((Float) -> Unit)? = null): List<DocumentPage> {
        SDKLog.d(TAG, "Starting DOCX/DOC parse")
        onProgress?.invoke(0.05f)

        val tempFile = java.io.File.createTempFile("poi_word", ".tmp")
        try {
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }

            return try {
                parseXwpf(FileInputStream(tempFile), onProgress)
            } catch (e: Exception) {
                if (e.message?.contains("OLE2") == true || e.cause?.message?.contains("OLE2") == true || e.javaClass.simpleName.contains("OLE2")) {
                    SDKLog.i(TAG, "File is OLE2, falling back to HWPFDocument")
                    parseHwpf(FileInputStream(tempFile), onProgress)
                } else {
                    throw e
                }
            }
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private fun parseHwpf(inputStream: InputStream, onProgress: ((Float) -> Unit)? = null): List<DocumentPage> {
        val doc = HWPFDocument(inputStream)
        val elements = mutableListOf<PageElement>()
        val range = doc.range
        val totalParagraphs = range.numParagraphs()
        
        var i = 0
        while (i < totalParagraphs) {
            val paragraph = range.getParagraph(i)
            if (paragraph.isInTable) {
                val table = range.getTable(paragraph)
                val rows = mutableListOf<List<PageElement.TableCell>>()
                for (r in 0 until table.numRows()) {
                    val row = table.getRow(r)
                    val rowCells = mutableListOf<PageElement.TableCell>()
                    for (c in 0 until row.numCells()) {
                        val cell = row.getCell(c)
                        val cellText = java.lang.StringBuilder()
                        for (p in 0 until cell.numParagraphs()) {
                            cellText.append(cell.getParagraph(p).text().replace("\r", "").replace("\u0007", "").trim()).append(" ")
                        }
                        rowCells.add(PageElement.TableCell(listOf(PageElement.TextElement(cellText.toString().trim()))))
                    }
                    rows.add(rowCells)
                }
                elements.add(PageElement.TableElement(rows))
                i += table.numParagraphs() // skip all paragraphs in this table
                continue
            }
            
            val text = paragraph.text().replace("\r", "").replace("\u0007", "")
            if (text.isNotBlank()) {
                var isBold = false
                var isItalic = false
                if (paragraph.numCharacterRuns() > 0) {
                    val run = paragraph.getCharacterRun(0)
                    isBold = run.isBold
                    isItalic = run.isItalic
                }
                elements.add(PageElement.TextElement(text.trim(), isBold = isBold, isItalic = isItalic))
            }
            i++
            onProgress?.invoke(0.2f + (0.8f * i / totalParagraphs))
        }
        return listOf(DocumentPage(elements))
    }

    private fun parseXwpf(inputStream: InputStream, onProgress: ((Float) -> Unit)? = null): List<DocumentPage> {
        val doc = XWPFDocument(inputStream)
        val elements = mutableListOf<PageElement>()

        // 1. Core Properties
        try {
            val props = doc.properties.coreProperties
            val title = props.title
            val author = props.creator
            SDKLog.i(TAG, "Document Title: $title, Author: $author")
            if (!title.isNullOrBlank() || !author.isNullOrBlank()) {
                if (!title.isNullOrBlank()) elements.add(PageElement.HeadingElement(title, 1))
                if (!author.isNullOrBlank()) elements.add(PageElement.TextElement("Author: $author", isItalic = true))
                elements.add(PageElement.Divider)
            }
        } catch (_: Throwable) {}
        onProgress?.invoke(0.1f)

        try {
            // 2. Headers
        for (header in doc.headerList) {
            for (bodyElement in header.bodyElements) {
                parseBodyElement(bodyElement, elements)
            }
            elements.add(PageElement.Divider)
        }
        onProgress?.invoke(0.2f)

        // 3. Body Elements (Paragraphs, Tables, Inline Images in correct order)
        val totalElements = doc.bodyElements.size
        for ((index, bodyElement) in doc.bodyElements.withIndex()) {
            parseBodyElement(bodyElement, elements)
            onProgress?.invoke(0.2f + (0.6f * (index + 1) / totalElements))
        }

        // 4. Footers
        for (footer in doc.footerList) {
            elements.add(PageElement.Divider)
            for (bodyElement in footer.bodyElements) {
                parseBodyElement(bodyElement, elements)
            }
        }
            onProgress?.invoke(1.0f)
        } catch (e: Throwable) {
            SDKLog.e(TAG, "Error parsing docx: ${e.message}", e)
        } finally {
            doc.close()
        }
        SDKLog.d(TAG, "Finished parsing DOCX: ${elements.size} elements found")
        return listOf(DocumentPage(elements))
    }

    private fun parseBodyElement(bodyElement: IBodyElement, elements: MutableList<PageElement>) {
        when (bodyElement) {
            is XWPFParagraph -> {
                // Extract inline pictures from runs
                for (run in bodyElement.runs) {
                    try {
                        for (pic in run.embeddedPictures) {
                            try {
                                elements.add(PageElement.ImageElement(pic.pictureData.data))
                            } catch (e: Throwable) {
                                SDKLog.w(TAG, "Warning: Failed to extract a specific picture", e)
                            }
                        }
                    } catch (e: Throwable) {
                        SDKLog.w(TAG, "Warning: Failed to read embedded pictures in run", e)
                    }
                }

                if (bodyElement.text.isNotBlank()) {
                    val headingLevel = getHeadingLevel(bodyElement.style ?: "")
                    if (headingLevel > 0) {
                        elements.add(PageElement.HeadingElement(bodyElement.text, headingLevel))
                    } else {
                        elements.add(parseTextParagraph(bodyElement))
                    }
                }
            }
            is XWPFTable -> {
                elements.add(parseTable(bodyElement))
            }
        }
    }

    private fun parseTextParagraph(paragraph: XWPFParagraph): PageElement.TextElement {
        val firstRun = paragraph.runs.firstOrNull { it.text().isNotBlank() } ?: paragraph.runs.firstOrNull()
        var text = paragraph.text
        
        // Handle Lists (Bullets / Numbering)
        if (paragraph.numID != null) {
            text = "• $text"
        }

        // Handle Text Color
        var textColor: Int? = null
        val colorHex = firstRun?.color
        if (colorHex != null && colorHex != "auto") {
            try {
                textColor = (if (colorHex.startsWith("#")) colorHex else "#$colorHex").toColorInt()
            } catch (_: Throwable) {}
        }

        return PageElement.TextElement(
            text = text,
            isBold = firstRun?.isBold ?: false,
            isItalic = firstRun?.isItalic ?: false,
            isUnderline = firstRun?.underline != null && firstRun.underline != UnderlinePatterns.NONE,
            fontSize = firstRun?.fontSizeAsDouble?.toInt()?.takeIf { it > 0 } ?: 12,
            color = textColor,
            alignment = when (paragraph.alignment) {
                ParagraphAlignment.CENTER -> TextAlign.CENTER
                ParagraphAlignment.RIGHT  -> TextAlign.RIGHT
                ParagraphAlignment.BOTH   -> TextAlign.JUSTIFY
                else                      -> TextAlign.LEFT
            }
        )
    }

    private fun parseTable(table: XWPFTable): PageElement.TableElement {
        val rows = table.rows.map { row ->
            row.tableCells.map { cell ->
                PageElement.TableCell(listOf(PageElement.TextElement(cell.text)))
            }
        }
        return PageElement.TableElement(rows)
    }

    private fun getHeadingLevel(style: String): Int = when {
        style.contains("Heading1", true) -> 1
        style.contains("Heading2", true) -> 2
        style.contains("Heading3", true) -> 3
        else -> 0
    }
}
