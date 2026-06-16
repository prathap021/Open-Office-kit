package com.poirender.sdk.parser

import com.poirender.sdk.model.*
import org.apache.poi.sl.usermodel.*
import java.io.InputStream

class PptxParser {

    fun parse(inputStream: InputStream): List<SlideData> {
        // Create the SlideShow from the InputStream (supports both binary PPT and XML PPTX)
        val ppt: SlideShow<*, *>
        try {
            ppt = SlideShowFactory.create(inputStream)
        } catch (e: Exception) {
            println("Critical Error parsing PPT/PPTX file header: ${e.message}")
            throw e
        }

        val slides = mutableListOf<SlideData>()

        val pageSize = ppt.pageSize
        val slideW = pageSize.width.toFloat()
        val slideH = pageSize.height.toFloat()

        for ((index, slide) in ppt.slides.withIndex()) {
            val shapes = mutableListOf<SlideShape>()

            try {
                for (shape in slide.shapes) {
                    try {
                        val a = shape.anchor
                        val x = a.x.toFloat() / slideW
                        val y = a.y.toFloat() / slideH
                        val w = a.width.toFloat() / slideW
                        val h = a.height.toFloat() / slideH

                        when (shape) {
                            is TextShape<*, *> -> {
                                val sb = java.lang.StringBuilder()
                                var fontSize = 14f
                                var isBold = false
                                for (p in shape.textParagraphs) {
                                    val hasBullet = try {
                                        p.javaClass.getMethod("isBullet").invoke(p) as? Boolean ?: false
                                    } catch (_: Exception) {
                                        try { p.bulletStyle != null } catch (_: Exception) { false }
                                    }
                                    if (hasBullet) sb.append("• ")
                                    for (run in p.textRuns) {
                                        sb.append(run.rawText)
                                        if (run.fontSize != null && fontSize == 14f) fontSize = run.fontSize.toFloat()
                                        if (run.isBold) isBold = true
                                    }
                                    sb.append("\n")
                                }
                                val text = sb.toString().trimEnd()
                                if (text.isNotBlank()) {
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
                    } catch (e: Exception) {
                        println("Warning: Failed to parse a specific shape on slide $index. (Likely corrupted OLE/ExEmbedAtom record). Message: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                println("Warning: Failed to iterate shapes on slide $index due to record reading issue. Message: ${e.message}")
            }
            slides.add(SlideData(index = index, shapes = shapes))
        }

        ppt.close()
        return slides
    }
}
