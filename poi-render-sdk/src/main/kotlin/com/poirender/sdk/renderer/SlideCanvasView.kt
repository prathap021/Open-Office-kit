package com.poirender.sdk.renderer

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.poirender.sdk.R
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
        if (slide.backgroundColor != 0) {
            bgPaint.color = slide.backgroundColor
        } else {
            bgPaint.color = ContextCompat.getColor(context, R.color.ppt_slide_bg)
        }
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        for (shape in slide.shapes) {
            when (shape) {
                is SlideShape.RectShape -> {
                    if (shape.fillColor != 0) {
                        shapePaint.color = shape.fillColor
                    } else {
                        shapePaint.color = ContextCompat.getColor(context, R.color.ppt_shape_fill)
                    }
                    shapePaint.style = Paint.Style.FILL
                    canvas.drawRect(
                        shape.x * w, shape.y * h,
                        (shape.x + shape.width) * w,
                        (shape.y + shape.height) * h,
                        shapePaint
                    )
                    
                    // Draw Stroke
                    shapePaint.color = ContextCompat.getColor(context, R.color.ppt_shape_stroke)
                    shapePaint.style = Paint.Style.STROKE
                    shapePaint.strokeWidth = 2f
                    canvas.drawRect(
                        shape.x * w, shape.y * h,
                        (shape.x + shape.width) * w,
                        (shape.y + shape.height) * h,
                        shapePaint
                    )
                }
                is SlideShape.TextShape -> {
                    if (shape.color != 0) {
                        textPaint.color = shape.color
                    } else {
                        textPaint.color = ContextCompat.getColor(context, R.color.ppt_body_text)
                    }
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
