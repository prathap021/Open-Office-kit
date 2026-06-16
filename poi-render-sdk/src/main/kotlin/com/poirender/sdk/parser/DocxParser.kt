package com.poirender.sdk.parser

import com.poirender.sdk.model.*
import org.apache.poi.xwpf.usermodel.*
import java.io.InputStream

class DocxParser {

    fun parse(inputStream: InputStream): List<DocumentPage> {
        val doc = XWPFDocument(inputStream)
        val elements = mutableListOf<PageElement>()

        // Parse paragraphs
        for (paragraph in doc.paragraphs) {
            if (paragraph.text.isBlank()) continue

            val headingLevel = getHeadingLevel(paragraph.style ?: "")
            if (headingLevel > 0) {
                elements.add(
                    PageElement.HeadingElement(
                        text = paragraph.text,
                        level = headingLevel
                    )
                )
            } else {
                elements.add(parseTextParagraph(paragraph))
            }
        }

        // Parse tables
        for (table in doc.tables) {
            elements.add(parseTable(table))
        }

        // Parse images
        for (picture in doc.allPictures) {
            elements.add(
                PageElement.ImageElement(imageData = picture.data)
            )
        }

        doc.close()
        return listOf(DocumentPage(elements))
    }

    private fun parseTextParagraph(
        paragraph: XWPFParagraph
    ): PageElement.TextElement {
        val firstRun = paragraph.runs.firstOrNull()
        return PageElement.TextElement(
            text = paragraph.text,
            isBold = firstRun?.isBold ?: false,
            isItalic = firstRun?.isItalic ?: false,
            isUnderline = firstRun?.underline != UnderlinePatterns.NONE,
            fontSize = firstRun?.fontSize?.takeIf { it > 0 } ?: 12,
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
            row.tableCells.map { it.text }
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
