package com.poirender.sdk.parser

import com.poirender.sdk.model.*
import org.apache.poi.sl.usermodel.*
import java.io.InputStream

class PptxParser {

    fun parse(inputStream: InputStream): List<SlideData> {
        val ppt = SlideShowFactory.create(inputStream)
        val slides = mutableListOf<SlideData>()

        val pageSize = ppt.pageSize
        val slideW = pageSize.width.toFloat()
        val slideH = pageSize.height.toFloat()

        for ((index, slide) in ppt.slides.withIndex()) {
            val shapes = mutableListOf<SlideShape>()

            for (shape in slide.shapes) {
                val a = shape.anchor
                val x = a.x.toFloat() / slideW
                val y = a.y.toFloat() / slideH
                val w = a.width.toFloat() / slideW
                val h = a.height.toFloat() / slideH

                when (shape) {
                    is TextShape<*, *> -> {
                        val text = shape.text ?: ""
                        if (text.isNotBlank()) {
                            val run = shape.textParagraphs
                                .firstOrNull()?.textRuns?.firstOrNull()
                            var fontSize = 14f
                            if (run != null && run.fontSize != null) {
                                fontSize = run.fontSize.toFloat()
                            }
                            var isBold = false
                            if (run != null) {
                                isBold = run.isBold
                            }

                            shapes.add(
                                SlideShape.TextShape(
                                    text = text,
                                    x = x, y = y,
                                    width = w, height = h,
                                    fontSize = fontSize,
                                    isBold = isBold
                                )
                            )
                        }
                    }
                    is PictureShape<*, *> -> {
                        shapes.add(
                            SlideShape.ImageShape(
                                imageData = shape.pictureData.data,
                                x = x, y = y, width = w, height = h
                            )
                        )
                    }
                    is SimpleShape<*, *> -> {
                        shapes.add(
                            SlideShape.RectShape(
                                x = x, y = y, width = w, height = h
                            )
                        )
                    }
                }
            }
            slides.add(SlideData(index = index, shapes = shapes))
        }

        ppt.close()
        return slides
    }
}
