package com.poirender.sdk.renderer

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poirender.sdk.R
import com.poirender.sdk.model.SlideData
import com.poirender.sdk.model.SlideShape

@Composable
fun PptxRenderer(slides: List<SlideData>, searchQuery: String = "") {
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
fun SlideCanvas(slide: SlideData, searchQuery: String) {
    val textMeasurer = rememberTextMeasurer()
    val defaultShapeFillColor = colorResource(id = R.color.ppt_shape_fill)
    val defaultShapeStrokeColor = colorResource(id = R.color.ppt_shape_stroke)
    val defaultTextColor = colorResource(id = R.color.ppt_body_text)

    // Pre-decode bitmaps to avoid doing it on every draw frame
    val bitmaps = remember(slide.index) {
        slide.shapes.filterIsInstance<SlideShape.ImageShape>().associateWith { shape ->
            BitmapFactory.decodeByteArray(shape.imageData, 0, shape.imageData.size)?.asImageBitmap()
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val bgColor = Color(slide.backgroundColor)
        drawRect(color = bgColor, size = Size(w, h))

        for (shape in slide.shapes) {
            when (shape) {
                is SlideShape.RectShape -> {
                    val fillColor = Color(shape.fillColor)
                    drawRect(
                        color = fillColor,
                        topLeft = Offset(shape.x * w, shape.y * h),
                        size = Size(shape.width * w, shape.height * h)
                    )
                    drawRect(
                        color = Color(shape.strokeColor),
                        topLeft = Offset(shape.x * w, shape.y * h),
                        size = Size(shape.width * w, shape.height * h),
                        style = Stroke(width = 2f)
                    )
                }
                is SlideShape.TextShape -> {
                    val textColor = Color(shape.color)
                    val fontSize = shape.fontSize * (w / 400f)
                    val fontWeight = if (shape.isBold) FontWeight.Bold else FontWeight.Normal

                    val lines = shape.text.split("\n")
                    var lineY = shape.y * h + fontSize
                    for (line in lines) {
                        if (searchQuery.isNotBlank() && line.contains(searchQuery, ignoreCase = true)) {
                            var startIndex = line.indexOf(searchQuery, ignoreCase = true)
                            while (startIndex >= 0) {
                                val textBefore = line.substring(0, startIndex)
                                val xOffset = textMeasurer.measure(textBefore, TextStyle(fontSize = fontSize.sp, fontWeight = fontWeight)).size.width
                                val queryWidth = textMeasurer.measure(line.substring(startIndex, startIndex + searchQuery.length), TextStyle(fontSize = fontSize.sp, fontWeight = fontWeight)).size.width
                                drawRect(
                                    color = Color.Yellow,
                                    topLeft = Offset(shape.x * w + xOffset, lineY - fontSize),
                                    size = Size(queryWidth.toFloat(), fontSize * 1.4f)
                                )
                                startIndex = line.indexOf(searchQuery, startIndex + searchQuery.length, ignoreCase = true)
                            }
                        }
                        drawText(
                            textMeasurer = textMeasurer,
                            text = line,
                            topLeft = Offset(shape.x * w, lineY - fontSize),
                            style = TextStyle(color = textColor, fontSize = fontSize.sp, fontWeight = fontWeight)
                        )
                        lineY += fontSize * 1.4f
                    }
                }
                is SlideShape.ImageShape -> {
                    val bitmap = bitmaps[shape]
                    if (bitmap != null) {
                        drawImage(
                            image = bitmap,
                            dstOffset = androidx.compose.ui.unit.IntOffset((shape.x * w).toInt(), (shape.y * h).toInt()),
                            dstSize = androidx.compose.ui.unit.IntSize((shape.width * w).toInt(), (shape.height * h).toInt())
                        )
                    }
                }
                is SlideShape.ConnectorShape -> {
                    drawLine(
                        color = Color(shape.strokeColor),
                        start = Offset(shape.startX * w, shape.startY * h),
                        end = Offset(shape.endX * w, shape.endY * h),
                        strokeWidth = 2f
                    )
                }
                is SlideShape.TableShape -> {
                    val tableX = shape.x * w
                    val tableY = shape.y * h
                    val tableW = shape.width * w
                    val tableH = shape.height * h
                    
                    // Outer border and background
                    drawRect(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        topLeft = Offset(tableX, tableY),
                        size = Size(tableW, tableH)
                    )
                    drawRect(
                        color = Color.Gray,
                        topLeft = Offset(tableX, tableY),
                        size = Size(tableW, tableH),
                        style = Stroke(width = 2f)
                    )

                    // Inner grid lines
                    val rows = shape.rows.coerceAtLeast(1)
                    val cols = shape.cols.coerceAtLeast(1)
                    val cellW = tableW / cols
                    val cellH = tableH / rows

                    for (c in 1 until cols) {
                        val lineX = tableX + c * cellW
                        drawLine(
                            color = Color.Gray,
                            start = Offset(lineX, tableY),
                            end = Offset(lineX, tableY + tableH),
                            strokeWidth = 1f
                        )
                    }
                    for (r in 1 until rows) {
                        val lineY = tableY + r * cellH
                        drawLine(
                            color = Color.Gray,
                            start = Offset(tableX, lineY),
                            end = Offset(tableX + tableW, lineY),
                            strokeWidth = 1f
                        )
                    }
                }
                is SlideShape.ChartShape -> {
                    val chartX = shape.x * w
                    val chartY = shape.y * h
                    val chartW = shape.width * w
                    val chartH = shape.height * h
                    
                    // Background & Border
                    drawRect(
                        color = Color.LightGray.copy(alpha = 0.2f),
                        topLeft = Offset(chartX, chartY),
                        size = Size(chartW, chartH)
                    )
                    drawRect(
                        color = Color.Gray,
                        topLeft = Offset(chartX, chartY),
                        size = Size(chartW, chartH),
                        style = Stroke(width = 2f)
                    )

                    // Text label for chart type
                    val label = "Chart: ${shape.type}"
                    val fontSize = 12f * (w / 400f)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        topLeft = Offset(chartX + 8f, chartY + 8f),
                        style = TextStyle(color = Color.DarkGray, fontSize = fontSize.sp, fontWeight = FontWeight.Bold)
                    )

                    // Mini graphic based on type
                    val graphicW = chartW * 0.6f
                    val graphicH = chartH * 0.5f
                    val graphicX = chartX + (chartW - graphicW) / 2
                    val graphicY = chartY + (chartH - graphicH) / 2 + fontSize
                    val minDim = minOf(graphicW, graphicH)
                    
                    when (shape.type.lowercase()) {
                        "pie" -> {
                            drawArc(
                                color = Color.Blue.copy(alpha = 0.5f),
                                startAngle = 0f,
                                sweepAngle = 270f,
                                useCenter = true,
                                topLeft = Offset(graphicX + (graphicW - minDim) / 2, graphicY + (graphicH - minDim) / 2),
                                size = Size(minDim, minDim)
                            )
                            drawArc(
                                color = Color.Red.copy(alpha = 0.5f),
                                startAngle = 270f,
                                sweepAngle = 90f,
                                useCenter = true,
                                topLeft = Offset(graphicX + (graphicW - minDim) / 2, graphicY + (graphicH - minDim) / 2),
                                size = Size(minDim, minDim)
                            )
                        }
                        "bar", "column" -> {
                            val numBars = 4
                            val barSpacing = graphicW / (numBars * 2)
                            val barW = barSpacing
                            for (i in 0 until numBars) {
                                val barHeight = graphicH * (0.3f + 0.2f * i)
                                drawRect(
                                    color = Color.Blue.copy(alpha = 0.5f),
                                    topLeft = Offset(graphicX + i * (barW + barSpacing), graphicY + graphicH - barHeight),
                                    size = Size(barW, barHeight)
                                )
                            }
                            // Axes
                            drawLine(color = Color.Black, start = Offset(graphicX, graphicY + graphicH), end = Offset(graphicX + graphicW, graphicY + graphicH), strokeWidth = 2f)
                            drawLine(color = Color.Black, start = Offset(graphicX, graphicY), end = Offset(graphicX, graphicY + graphicH), strokeWidth = 2f)
                        }
                        else -> {
                            // Axes
                            drawLine(color = Color.Black, start = Offset(graphicX, graphicY + graphicH), end = Offset(graphicX + graphicW, graphicY + graphicH), strokeWidth = 2f)
                            drawLine(color = Color.Black, start = Offset(graphicX, graphicY), end = Offset(graphicX, graphicY + graphicH), strokeWidth = 2f)
                            // Line chart points
                            val p1 = Offset(graphicX + graphicW * 0.1f, graphicY + graphicH * 0.8f)
                            val p2 = Offset(graphicX + graphicW * 0.4f, graphicY + graphicH * 0.3f)
                            val p3 = Offset(graphicX + graphicW * 0.7f, graphicY + graphicH * 0.5f)
                            val p4 = Offset(graphicX + graphicW * 0.9f, graphicY + graphicH * 0.1f)
                            drawLine(color = Color.Blue.copy(alpha = 0.7f), start = p1, end = p2, strokeWidth = 3f)
                            drawLine(color = Color.Blue.copy(alpha = 0.7f), start = p2, end = p3, strokeWidth = 3f)
                            drawLine(color = Color.Blue.copy(alpha = 0.7f), start = p3, end = p4, strokeWidth = 3f)
                        }
                    }
                }
            }
        }
    }
}
