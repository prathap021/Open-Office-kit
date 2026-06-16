package com.poirender.sdk.model

data class DocumentPage(
    val elements: List<PageElement>
)

sealed class PageElement {

    data class TextElement(
        val text: String,
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val isUnderline: Boolean = false,
        val fontSize: Int = 12,
        val color: Int? = null,
        val alignment: TextAlign = TextAlign.LEFT
    ) : PageElement()

    data class HeadingElement(
        val text: String,
        val level: Int
    ) : PageElement()

    data class TableElement(
        val rows: List<List<String>>
    ) : PageElement()

    data class ImageElement(
        val imageData: ByteArray,
        val width: Int = 0,
        val height: Int = 0
    ) : PageElement()

    object Divider : PageElement()
}

enum class TextAlign { LEFT, CENTER, RIGHT, JUSTIFY }
