package com.poirender.sdk.renderer

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poirender.sdk.R
import com.poirender.sdk.model.SlideData
import com.poirender.sdk.model.SlideShape
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PptxRenderer(slides: List<SlideData>, searchQuery: String = "") {
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val coroutineScope = rememberCoroutineScope()
    
    var isFullscreen by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    var showThumbnails by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.ppt_viewer_bg))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Main Slide View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable { isFullscreen = !isFullscreen }
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    SlideCanvas(slide = slides[page], searchQuery = searchQuery)
                }
            }

            // Controls & Panels (Hidden in Fullscreen)
            AnimatedVisibility(visible = !isFullscreen) {
                Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF2D2D2D))) {
                    
                    // Speaker Notes Panel
                    if (showNotes) {
                        val currentNotes = slides[pagerState.currentPage].notes
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp, max = 150.dp)
                                .background(Color(0xFFF9FAFB))
                                .border(1.dp, Color(0xFFE5E7EB))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = if (!currentNotes.isNullOrBlank()) currentNotes else "No speaker notes.",
                                color = Color(0xFF374151),
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Thumbnails Panel
                    if (showThumbnails) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(Color(0xFF3D3D3D))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(slides) { index, slide ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .aspectRatio(16f / 9f)
                                        .background(Color.White)
                                        .border(
                                            width = if (pagerState.currentPage == index) 3.dp else 1.dp,
                                            color = if (pagerState.currentPage == index) Color(0xFFB7322C) else Color.Gray
                                        )
                                        .clickable {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        }
                                ) {
                                    // A mini canvas to draw a tiny version of the slide
                                    SlideCanvas(slide = slide, searchQuery = "")
                                    
                                    // Overlay slide number
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .background(Color.White.copy(alpha = 0.8f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Navigation Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1B4F8A))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = {
                                    if (pagerState.currentPage > 0) {
                                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                    }
                                },
                                enabled = pagerState.currentPage > 0,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB7322C))
                            ) { Text("Prev") }
                            
                            Button(
                                onClick = {
                                    if (pagerState.currentPage < slides.size - 1) {
                                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                    }
                                },
                                enabled = pagerState.currentPage < slides.size - 1,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB7322C))
                            ) { Text("Next") }
                        }

                        Text(
                            text = "${pagerState.currentPage + 1} / ${slides.size}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = { showThumbnails = !showThumbnails },
                                colors = ButtonDefaults.buttonColors(containerColor = if (showThumbnails) Color(0xFFD97706) else Color.DarkGray)
                            ) { Text("Thumbs") }

                            Button(
                                onClick = { showNotes = !showNotes },
                                colors = ButtonDefaults.buttonColors(containerColor = if (showNotes) Color(0xFFD97706) else Color.DarkGray)
                            ) { Text("Notes") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SlideCanvas(slide: SlideData, searchQuery: String) {
    val textMeasurer = rememberTextMeasurer()
    val defaultBgColor = colorResource(id = R.color.ppt_slide_bg)
    val defaultShapeFillColor = colorResource(id = R.color.ppt_shape_fill)
    val defaultShapeStrokeColor = colorResource(id = R.color.ppt_shape_stroke)
    val defaultTextColor = colorResource(id = R.color.ppt_body_text)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val bgColor = if (slide.backgroundColor != 0) Color(slide.backgroundColor) else defaultBgColor
        drawRect(color = bgColor, size = Size(w, h))

        for (shape in slide.shapes) {
            when (shape) {
                is SlideShape.RectShape -> {
                    val fillColor = if (shape.fillColor != 0) Color(shape.fillColor) else defaultShapeFillColor
                    drawRect(
                        color = fillColor,
                        topLeft = Offset(shape.x * w, shape.y * h),
                        size = Size(shape.width * w, shape.height * h)
                    )
                    drawRect(
                        color = defaultShapeStrokeColor,
                        topLeft = Offset(shape.x * w, shape.y * h),
                        size = Size(shape.width * w, shape.height * h),
                        style = Stroke(width = 2f)
                    )
                }
                is SlideShape.TextShape -> {
                    val textColor = if (shape.color != 0) Color(shape.color) else defaultTextColor
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
                    val bitmap = BitmapFactory.decodeByteArray(shape.imageData, 0, shape.imageData.size)
                    if (bitmap != null) {
                        drawImage(
                            image = bitmap.asImageBitmap(),
                            dstOffset = androidx.compose.ui.unit.IntOffset((shape.x * w).toInt(), (shape.y * h).toInt()),
                            dstSize = androidx.compose.ui.unit.IntSize((shape.width * w).toInt(), (shape.height * h).toInt())
                        )
                    }
                }
                is SlideShape.ConnectorShape -> {
                    drawLine(
                        color = if (shape.strokeColor != 0) Color(shape.strokeColor) else defaultShapeStrokeColor,
                        start = Offset(shape.startX * w, shape.startY * h),
                        end = Offset(shape.endX * w, shape.endY * h),
                        strokeWidth = 2f
                    )
                }
                is SlideShape.TableShape -> {
                     drawRect(
                        color = defaultShapeStrokeColor,
                        topLeft = Offset(shape.x * w, shape.y * h),
                        size = Size(shape.width * w, shape.height * h),
                        style = Stroke(width = 2f)
                    )
                }
                is SlideShape.ChartShape -> {
                    // Placeholder for chart
                    drawRect(
                        color = Color.LightGray,
                        topLeft = Offset(shape.x * w, shape.y * h),
                        size = Size(shape.width * w, shape.height * h)
                    )
                }
            }
        }
    }
}
