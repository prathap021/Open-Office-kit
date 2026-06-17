package com.poirender.sdk.model

data class SlideData(
    val index: Int,
    val shapes: List<SlideShape>,
    val notes: String? = null,
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val slideNumber: Int? = null,
    val transition: String? = null,
    val slideWidth: Float = 0f,
    val slideHeight: Float = 0f
) {
    val aspectRatio: Float get() = if (slideWidth > 0 && slideHeight > 0) slideWidth / slideHeight else 16f / 9f
}

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
    ) : SlideShape() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ImageShape

            if (x != other.x) return false
            if (y != other.y) return false
            if (width != other.width) return false
            if (height != other.height) return false
            if (!imageData.contentEquals(other.imageData)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = x.hashCode()
            result = 31 * result + y.hashCode()
            result = 31 * result + width.hashCode()
            result = 31 * result + height.hashCode()
            result = 31 * result + imageData.contentHashCode()
            return result
        }
    }

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
