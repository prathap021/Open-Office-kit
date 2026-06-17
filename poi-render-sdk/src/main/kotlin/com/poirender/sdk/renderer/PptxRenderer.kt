package com.poirender.sdk.renderer

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poirender.sdk.R
import com.poirender.sdk.model.SlideData
import com.poirender.sdk.model.SlideShape
import kotlin.math.max
import kotlin.math.min

@Composable
fun PptxRenderer(
    slides: List<SlideData>,
    searchQuery: String = ""
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.ppt_viewer_bg))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(slides, key = { _, slide -> slide.index }) { _, slide ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(slide.aspectRatio)
                    ) {
                        SlideCanvas(slide = slide, searchQuery = searchQuery)
                    }

                    if (!slide.notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Notes:\n${slide.notes}",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SlideCanvas(
    slide: SlideData,
    searchQuery: String
) {
    val textMeasurer = rememberTextMeasurer()
    val bitmaps = remember(slide.index) {
        slide.shapes.filterIsInstance<SlideShape.ImageShape>().associateWith { shape ->
            BitmapFactory.decodeByteArray(shape.imageData, 0, shape.imageData.size)?.asImageBitmap()
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val slideW = slide.slideWidth.takeIf { it > 0f } ?: 1f
        val slideH = slide.slideHeight.takeIf { it > 0f } ?: 1f

        val scale = minOf(size.width / slideW, size.height / slideH)
        val contentW = slideW * scale
        val contentH = slideH * scale
        val offsetX = (size.width - contentW) / 2f
        val offsetY = (size.height - contentH) / 2f

        fun sx(value: Float) = offsetX + value * scale
        fun sy(value: Float) = offsetY + value * scale
        fun sw(value: Float) = value * scale
        fun sh(value: Float) = value * scale

        drawRect(color = Color(slide.backgroundColor), size = size)

        for (shape in slide.shapes) {
            when (shape) {
                is SlideShape.RectShape -> {
                    drawRect(
                        color = Color(shape.fillColor),
                        topLeft = Offset(sx(shape.x * slideW), sy(shape.y * slideH)),
                        size = Size(sw(shape.width * slideW), sh(shape.height * slideH))
                    )
                    drawRect(
                        color = Color(shape.strokeColor),
                        topLeft = Offset(sx(shape.x * slideW), sy(shape.y * slideH)),
                        size = Size(sw(shape.width * slideW), sh(shape.height * slideH)),
                        style = Stroke(width = max(1f, 2f * scale))
                    )
                }

                is SlideShape.TextShape -> {
                    val baseFont = shape.fontSize.coerceIn(10f, 24f)
                    val fontSizePx = (baseFont * scale).coerceIn(10f, 30f)
                    val style = TextStyle(
                        color = Color(shape.color),
                        fontSize = fontSizePx.sp,
                        fontWeight = if (shape.isBold) FontWeight.Bold else FontWeight.Normal
                    )

                    val lines = shape.text.split("\n")
                    var y = sy(shape.y * slideH) + fontSizePx

                    for (line in lines) {
                        if (searchQuery.isNotBlank() && line.contains(searchQuery, ignoreCase = true)) {
                            val idx = line.indexOf(searchQuery, ignoreCase = true)
                            if (idx >= 0) {
                                val before = line.substring(0, idx)
                                val query = line.substring(idx, idx + searchQuery.length)
                                val beforeW = textMeasurer.measure(before, style).size.width.toFloat()
                                val queryW = textMeasurer.measure(query, style).size.width.toFloat()
                                drawRect(
                                    color = Color.Yellow.copy(alpha = 0.35f),
                                    topLeft = Offset(sx(shape.x * slideW) + beforeW, y - fontSizePx),
                                    size = Size(queryW, fontSizePx * 1.25f)
                                )
                            }
                        }

                        drawText(
                            textMeasurer = textMeasurer,
                            text = line,
                            topLeft = Offset(sx(shape.x * slideW), y - fontSizePx),
                            style = style
                        )
                        y += fontSizePx * 1.35f
                    }
                }

                is SlideShape.ImageShape -> {
                    val bitmap = bitmaps[shape] ?: continue
                    val dstW = max(1, sw(shape.width * slideW).toInt())
                    val dstH = max(1, sh(shape.height * slideH).toInt())
                    drawImage(
                        image = bitmap,
                        dstOffset = IntOffset(sx(shape.x * slideW).toInt(), sy(shape.y * slideH).toInt()),
                        dstSize = IntSize(dstW, dstH)
                    )
                }

                is SlideShape.ConnectorShape -> {
                    drawLine(
                        color = Color(shape.strokeColor),
                        start = Offset(sx(shape.startX * slideW), sy(shape.startY * slideH)),
                        end = Offset(sx(shape.endX * slideW), sy(shape.endY * slideH)),
                        strokeWidth = max(1f, 2f * scale)
                    )
                }

                is SlideShape.TableShape -> {
                    val x = sx(shape.x * slideW)
                    val y = sy(shape.y * slideH)
                    val w = sw(shape.width * slideW)
                    val h = sh(shape.height * slideH)
                    val rows = shape.rows.coerceAtLeast(1)
                    val cols = shape.cols.coerceAtLeast(1)
                    val cellW = w / cols
                    val cellH = h / rows

                    drawRect(
                        color = Color.LightGray.copy(alpha = 0.25f),
                        topLeft = Offset(x, y),
                        size = Size(w, h)
                    )
                    drawRect(
                        color = Color.Gray,
                        topLeft = Offset(x, y),
                        size = Size(w, h),
                        style = Stroke(width = max(1f, 1.5f * scale))
                    )

                    for (c in 1 until cols) {
                        val lineX = x + c * cellW
                        drawLine(Color.Gray, Offset(lineX, y), Offset(lineX, y + h), strokeWidth = max(1f, 1f * scale))
                    }
                    for (r in 1 until rows) {
                        val lineY = y + r * cellH
                        drawLine(Color.Gray, Offset(x, lineY), Offset(x + w, lineY), strokeWidth = max(1f, 1f * scale))
                    }
                }

                is SlideShape.ChartShape -> {
                    val x = sx(shape.x * slideW)
                    val y = sy(shape.y * slideH)
                    val w = sw(shape.width * slideW)
                    val h = sh(shape.height * slideH)
                    val labelSize = (12f * scale).coerceIn(10f, 18f)
                    val graphicW = w * 0.6f
                    val graphicH = h * 0.5f
                    val graphicX = x + (w - graphicW) / 2f
                    val graphicY = y + (h - graphicH) / 2f + labelSize
                    val minDim = min(graphicW, graphicH)

                    drawRect(
                        color = Color.LightGray.copy(alpha = 0.2f),
                        topLeft = Offset(x, y),
                        size = Size(w, h)
                    )
                    drawRect(
                        color = Color.Gray,
                        topLeft = Offset(x, y),
                        size = Size(w, h),
                        style = Stroke(width = max(1f, 1.5f * scale))
                    )

                    drawText(
                        textMeasurer = textMeasurer,
                        text = "Chart: ${shape.type}",
                        topLeft = Offset(x + 8f * scale, y + 8f * scale),
                        style = TextStyle(
                            color = Color.DarkGray,
                            fontSize = labelSize.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    when (shape.type.lowercase()) {
                        "pie" -> {
                            val left = graphicX + (graphicW - minDim) / 2f
                            val top = graphicY + (graphicH - minDim) / 2f
                            drawArc(Color.Blue.copy(alpha = 0.5f), 0f, 270f, true, Offset(left, top), Size(minDim, minDim))
                            drawArc(Color.Red.copy(alpha = 0.5f), 270f, 90f, true, Offset(left, top), Size(minDim, minDim))
                        }

                        "bar", "column" -> {
                            val numBars = 4
                            val barSpacing = graphicW / (numBars * 2f)
                            val barW = barSpacing
                            for (i in 0 until numBars) {
                                val barHeight = graphicH * (0.3f + 0.2f * i)
                                drawRect(
                                    color = Color.Blue.copy(alpha = 0.5f),
                                    topLeft = Offset(graphicX + i * (barW + barSpacing), graphicY + graphicH - barHeight),
                                    size = Size(barW, barHeight)
                                )
                            }
                            drawLine(Color.Black, Offset(graphicX, graphicY + graphicH), Offset(graphicX + graphicW, graphicY + graphicH), strokeWidth = max(1f, 2f * scale))
                            drawLine(Color.Black, Offset(graphicX, graphicY), Offset(graphicX, graphicY + graphicH), strokeWidth = max(1f, 2f * scale))
                        }

                        else -> {
                            drawLine(Color.Black, Offset(graphicX, graphicY + graphicH), Offset(graphicX + graphicW, graphicY + graphicH), strokeWidth = max(1f, 2f * scale))
                            drawLine(Color.Black, Offset(graphicX, graphicY), Offset(graphicX, graphicY + graphicH), strokeWidth = max(1f, 2f * scale))
                            val p1 = Offset(graphicX + graphicW * 0.1f, graphicY + graphicH * 0.8f)
                            val p2 = Offset(graphicX + graphicW * 0.4f, graphicY + graphicH * 0.3f)
                            val p3 = Offset(graphicX + graphicW * 0.7f, graphicY + graphicH * 0.5f)
                            val p4 = Offset(graphicX + graphicW * 0.9f, graphicY + graphicH * 0.1f)
                            drawLine(Color.Blue.copy(alpha = 0.7f), p1, p2, strokeWidth = max(1f, 3f * scale))
                            drawLine(Color.Blue.copy(alpha = 0.7f), p2, p3, strokeWidth = max(1f, 3f * scale))
                            drawLine(Color.Blue.copy(alpha = 0.7f), p3, p4, strokeWidth = max(1f, 3f * scale))
                        }
                    }
                }
            }
        }
    }
}