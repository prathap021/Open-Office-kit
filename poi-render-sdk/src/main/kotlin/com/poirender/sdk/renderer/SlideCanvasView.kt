package com.poirender.sdk.renderer

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.poirender.sdk.model.*

class SlideCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var slideData: SlideData? = null

    private val bgPaint = Paint()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setSlide(slide: SlideData) {
        this.slideData = slide
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val slide = slideData ?: return
        val w = width.toFloat()
        val h = height.toFloat()

        // Background
        bgPaint.color = slide.backgroundColor
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        for (shape in slide.shapes) {
            when (shape) {
                is SlideShape.RectShape -> {
                    shapePaint.color = shape.fillColor
                    shapePaint.style = Paint.Style.FILL
                    canvas.drawRect(
                        shape.x * w, shape.y * h,
                        (shape.x + shape.width) * w,
                        (shape.y + shape.height) * h,
                        shapePaint
                    )
                }
                is SlideShape.TextShape -> {
                    textPaint.color = shape.color
                    textPaint.textSize = shape.fontSize * (w / 400f)
                    textPaint.typeface = if (shape.isBold)
                        Typeface.DEFAULT_BOLD else Typeface.DEFAULT

                    // Draw multiline text
                    val lines = shape.text.split("\n")
                    var lineY = shape.y * h + textPaint.textSize
                    for (line in lines) {
                        canvas.drawText(line, shape.x * w, lineY, textPaint)
                        lineY += textPaint.textSize * 1.4f
                    }
                }
                is SlideShape.ImageShape -> {
                    val bitmap = BitmapFactory.decodeByteArray(
                        shape.imageData, 0, shape.imageData.size
                    ) ?: continue
                    val dst = RectF(
                        shape.x * w, shape.y * h,
                        (shape.x + shape.width) * w,
                        (shape.y + shape.height) * h
                    )
                    canvas.drawBitmap(bitmap, null, dst, null)
                }
            }
        }
    }
}
