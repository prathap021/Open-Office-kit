package com.poirender.sdk.parser

import com.poirender.sdk.model.*
import org.apache.poi.xslf.usermodel.*
import java.io.InputStream

class PptxParser {

    fun parse(inputStream: InputStream): List<SlideData> {
        val ppt = XMLSlideShow(inputStream)
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
                    is XSLFTextShape -> {
                        val text = shape.textParagraphs
                            .joinToString("\n") { it.text ?: "" }
                        if (text.isNotBlank()) {
                            val run = shape.textParagraphs
                                .firstOrNull()?.textRuns?.firstOrNull()
                            shapes.add(
                                SlideShape.TextShape(
                                    text = text,
                                    x = x, y = y,
                                    width = w, height = h,
                                    fontSize = run?.fontSize?.toFloat() ?: 14f,
                                    isBold = run?.isBold ?: false
                                )
                            )
                        }
                    }
                    is XSLFPictureShape -> {
                        shapes.add(
                            SlideShape.ImageShape(
                                imageData = shape.pictureData.data,
                                x = x, y = y, width = w, height = h
                            )
                        )
                    }
                    is XSLFSimpleShape -> {
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
