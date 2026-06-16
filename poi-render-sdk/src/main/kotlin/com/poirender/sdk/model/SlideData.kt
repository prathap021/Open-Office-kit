package com.poirender.sdk.model

data class SlideData(
    val index: Int,
    val shapes: List<SlideShape>,
    val backgroundColor: Int = 0xFFFFFFFF.toInt()
)

sealed class SlideShape {

    data class TextShape(
        val text: String,
        val x: Float, val y: Float,
        val width: Float, val height: Float,
        val fontSize: Float = 14f,
        val isBold: Boolean = false,
        val color: Int = 0xFF000000.toInt()
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
}
