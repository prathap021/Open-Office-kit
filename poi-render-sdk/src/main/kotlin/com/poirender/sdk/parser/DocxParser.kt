package com.poirender.sdk.parser

import android.graphics.Color
import com.poirender.sdk.model.*
import org.apache.poi.xwpf.usermodel.*
import java.io.InputStream

class DocxParser {

    fun parse(inputStream: InputStream): List<DocumentPage> {
        val doc = XWPFDocument(inputStream)
        val elements = mutableListOf<PageElement>()

        // 1. Core Properties
        try {
            val props = doc.properties.coreProperties
            val title = props.title
            val author = props.creator
            if (!title.isNullOrBlank() || !author.isNullOrBlank()) {
                if (!title.isNullOrBlank()) elements.add(PageElement.HeadingElement(title, 1))
                if (!author.isNullOrBlank()) elements.add(PageElement.TextElement("Author: $author", isItalic = true))
                elements.add(PageElement.Divider)
            }
        } catch (e: Exception) {}

        // 2. Headers
        for (header in doc.headerList) {
            for (bodyElement in header.bodyElements) {
                parseBodyElement(bodyElement, elements)
            }
            elements.add(PageElement.Divider)
        }

        // 3. Body Elements (Paragraphs, Tables, Inline Images in correct order)
        for (bodyElement in doc.bodyElements) {
            parseBodyElement(bodyElement, elements)
        }

        // 4. Footers
        for (footer in doc.footerList) {
            elements.add(PageElement.Divider)
            for (bodyElement in footer.bodyElements) {
                parseBodyElement(bodyElement, elements)
            }
        }

        doc.close()
        return listOf(DocumentPage(elements))
    }

    private fun parseBodyElement(bodyElement: IBodyElement, elements: MutableList<PageElement>) {
        when (bodyElement) {
            is XWPFParagraph -> {
                // Extract inline pictures from runs
                for (run in bodyElement.runs) {
                    for (pic in run.embeddedPictures) {
                        elements.add(PageElement.ImageElement(pic.pictureData.data))
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
                textColor = Color.parseColor(if (colorHex.startsWith("#")) colorHex else "#$colorHex")
            } catch (e: Exception) {}
        }

        return PageElement.TextElement(
            text = text,
            isBold = firstRun?.isBold ?: false,
            isItalic = firstRun?.isItalic ?: false,
            isUnderline = firstRun?.underline != null && firstRun.underline != UnderlinePatterns.NONE,
            fontSize = firstRun?.fontSize?.takeIf { it > 0 } ?: 12,
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
