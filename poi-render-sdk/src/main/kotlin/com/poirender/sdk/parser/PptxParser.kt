package com.poirender.sdk.parser

import com.poirender.sdk.model.*
import com.poirender.sdk.util.SDKLog
import org.apache.poi.sl.usermodel.*
import java.io.InputStream

class PptxParser {
    private val TAG = "PptxParser"

    fun parse(
        inputStream: InputStream,
        onProgress: ((Float) -> Unit)? = null
    ): List<SlideData> {
        SDKLog.d(TAG, "Starting PPTX parse")
        onProgress?.invoke(0.05f)

        val ppt: SlideShow<*, *>
        try {
            ppt = SlideShowFactory.create(inputStream)
        } catch (e: Throwable) {
            SDKLog.e(TAG, "Critical Error parsing PPT/PPTX file header", e)
            throw e
        }

        val slides = mutableListOf<SlideData>()
        val totalSlides = ppt.slides.size.coerceAtLeast(1)
        val pageSize = ppt.pageSize
        val slideW = pageSize.width.toFloat().coerceAtLeast(1f)
        val slideH = pageSize.height.toFloat().coerceAtLeast(1f)

        try {
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
                                    val sb = StringBuilder()
                                    var fontSize = 14f
                                    var isBold = false

                                    for (p in shape.textParagraphs) {
                                        val hasBullet = try {
                                            p.javaClass.getMethod("isBullet").invoke(p) as? Boolean ?: false
                                        } catch (_: Throwable) {
                                            try { p.bulletStyle != null } catch (_: Throwable) { false }
                                        }

                                        if (hasBullet) sb.append("• ")

                                        for (run in p.textRuns) {
                                            sb.append(run.rawText)
                                            if (run.fontSize != null && fontSize == 14f) {
                                                fontSize = run.fontSize.toFloat()
                                            }
                                            if (run.isBold) isBold = true
                                        }
                                        sb.append("\n")
                                    }

                                    val text = sb.toString().trimEnd()
                                    if (text.isNotBlank()) {
                                        shapes.add(
                                            SlideShape.TextShape(
                                                text = text,
                                                x = x,
                                                y = y,
                                                width = w,
                                                height = h,
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
                                            x = x,
                                            y = y,
                                            width = w,
                                            height = h
                                        )
                                    )
                                }

                                is SimpleShape<*, *> -> {
                                    shapes.add(
                                        SlideShape.RectShape(
                                            x = x,
                                            y = y,
                                            width = w,
                                            height = h
                                        )
                                    )
                                }
                            }
                        } catch (e: Throwable) {
                            SDKLog.w(TAG, "Warning: Failed to parse a specific shape on slide $index", e)
                        }
                    }
                } catch (e: Throwable) {
                    SDKLog.w(TAG, "Warning: Failed to iterate shapes on slide $index", e)
                }

                var notesText: String? = null
                try {
                    val notesObj = slide.javaClass.getMethod("getNotes").invoke(slide)
                    if (notesObj != null) {
                        val textParagraphsList = notesObj.javaClass.getMethod("getTextParagraphs").invoke(notesObj) as? List<*>
                        if (textParagraphsList != null) {
                            val sb = StringBuilder()
                            for (p in textParagraphsList) {
                                if (p == null) continue
                                val textRunsList = p.javaClass.getMethod("getTextRuns").invoke(p) as? List<*>
                                if (textRunsList != null) {
                                    for (run in textRunsList) {
                                        if (run == null) continue
                                        val text = run.javaClass.getMethod("getRawText").invoke(run) as? String
                                        if (text != null) sb.append(text)
                                    }
                                    sb.append("\n")
                                }
                            }
                            notesText = sb.toString().trimEnd()
                        }
                    }
                } catch (_: Throwable) {
                }

                slides.add(
                    SlideData(
                        index = index,
                        shapes = shapes,
                        slideWidth = slideW,
                        slideHeight = slideH,
                        slideNumber = index + 1,
                        notes = notesText?.takeIf { it.isNotBlank() }
                    )
                )

                onProgress?.invoke((index + 1).toFloat() / totalSlides.toFloat())
            }
        } catch (e: Throwable) {
            SDKLog.e(TAG, "Error parsing pptx: ${e.message}", e)
        } finally {
            ppt.close()
        }

        SDKLog.d(TAG, "Finished parsing PPTX: ${slides.size} slides found")
        return slides
    }
}