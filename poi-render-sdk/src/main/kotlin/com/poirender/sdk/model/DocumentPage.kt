package com.poirender.sdk.model

data class DocumentPage(
    val elements: List<PageElement>
)

sealed class PageElement {

    data class TextRun(
        val text: String,
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val isUnderline: Boolean = false,
        val isStrikethrough: Boolean = false,
        val isSuperscript: Boolean = false,
        val isSubscript: Boolean = false,
        val highlightColor: Int? = null,
        val fontSize: Int = 12,
        val color: Int? = null,
        val hyperlink: String? = null
    )

    data class TextElement(
        val text: String,
        val runs: List<TextRun> = emptyList(),
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val isUnderline: Boolean = false,
        val fontSize: Int = 12,
        val color: Int? = null,
        val alignment: TextAlign = TextAlign.LEFT,
        val indentLevel: Int = 0,
        val lineSpacing: Float = 1.0f
    ) : PageElement()

    data class HeadingElement(
        val text: String,
        val level: Int
    ) : PageElement()

    data class TableCell(
        val elements: List<PageElement>,
        val colSpan: Int = 1,
        val rowSpan: Int = 1,
        val backgroundColor: Int? = null,
        val alignment: TextAlign = TextAlign.LEFT
    )

    data class TableElement(
        val rows: List<List<TableCell>>
    ) : PageElement()

    data class ImageElement(
        val imageData: ByteArray,
        val width: Int = 0,
        val height: Int = 0
    ) : PageElement()

    object Divider : PageElement()

    object PageBreak : PageElement()

    data class FootnoteElement(
        val referenceId: String,
        val text: String
    ) : PageElement()
}

enum class TextAlign { LEFT, CENTER, RIGHT, JUSTIFY }
