package com.poirender.sdk.model

data class SlideData(
    val index: Int,
    val shapes: List<SlideShape>,
    val notes: String? = null,
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val slideNumber: Int? = null,
    val transition: String? = null
)

sealed class SlideShape {

    data class TextShape(
        val text: String,
        val x: Float, val y: Float,
        val width: Float, val height: Float,
        val fontSize: Float = 14f,
        val isBold: Boolean = false,
        val color: Int = 0xFF000000.toInt(),
        val indentLevel: Int = 0
    ) : SlideShape()

    data class ImageShape(
        val imageData: ByteArray,
        val x: Float, val y: Float,
        val width: Float, val height: Float
    ) : SlideShape()

    data class RectShape(
        val x: Float, val y: Float,
        val width: Float, val height: Float,
        val fillColor: Int = 0xFFCCCCCC.toInt(),
        val strokeColor: Int = 0xFF000000.toInt()
    ) : SlideShape()

    data class ConnectorShape(
        val startX: Float, val startY: Float,
        val endX: Float, val endY: Float,
        val strokeColor: Int = 0xFF000000.toInt()
    ) : SlideShape()

    data class TableShape(
        val x: Float, val y: Float,
        val width: Float, val height: Float,
        val rows: Int, val cols: Int
    ) : SlideShape()

    data class ChartShape(
        val x: Float, val y: Float,
        val width: Float, val height: Float,
        val type: String
    ) : SlideShape()
}
