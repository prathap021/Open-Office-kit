package com.poirender.sdk.renderer

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poirender.sdk.R
import com.poirender.sdk.model.DocumentPage
import com.poirender.sdk.model.PageElement

@Composable
fun DocxRenderer(pages: List<DocumentPage>, searchQuery: String = "") {
    val elements = pages.flatMap { it.elements }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.word_page_bg))
    ) {
        items(elements) { item ->
            when (item) {
                is PageElement.HeadingElement -> HeadingComponent(item, searchQuery)
                is PageElement.TextElement -> TextComponent(item, searchQuery)
                is PageElement.TableElement -> TableComponent(item, searchQuery)
                is PageElement.ImageElement -> ImageComponent(item)
                is PageElement.Divider -> DividerComponent()
            }
        }
    }
}

@Composable
fun HeadingComponent(item: PageElement.HeadingElement, searchQuery: String) {
    val fontSize = when (item.level) {
        1 -> 28.sp
        2 -> 22.sp
        else -> 18.sp
    }
    val colorRes = when (item.level) {
        1 -> R.color.word_heading1
        2 -> R.color.word_heading2
        3 -> R.color.word_heading3
        else -> R.color.word_heading4
    }

    val annotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = colorResource(id = colorRes), fontWeight = FontWeight.Bold, fontSize = fontSize)) {
            if (searchQuery.isNotBlank() && item.text.contains(searchQuery, ignoreCase = true)) {
                var startIndex = 0
                while (startIndex < item.text.length) {
                    val matchIndex = item.text.indexOf(searchQuery, startIndex, ignoreCase = true)
                    if (matchIndex == -1) {
                        append(item.text.substring(startIndex))
                        break
                    }
                    append(item.text.substring(startIndex, matchIndex))
                    withStyle(SpanStyle(background = Color.Yellow)) {
                        append(item.text.substring(matchIndex, matchIndex + searchQuery.length))
                    }
                    startIndex = matchIndex + searchQuery.length
                }
            } else {
                append(item.text)
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.word_paper_bg))
            .padding(start = 32.dp, top = 24.dp, end = 32.dp, bottom = 8.dp)
    )
}

@Composable
fun TextComponent(item: PageElement.TextElement, searchQuery: String) {
    val textColor = item.color?.let { Color(it) } ?: colorResource(id = R.color.word_body_text)

    val annotatedString = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                fontWeight = if (item.isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (item.isItalic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = if (item.isUnderline) TextDecoration.Underline else TextDecoration.None,
                fontSize = if (item.fontSize > 0) item.fontSize.sp else 12.sp,
                color = textColor
            )
        ) {
            if (searchQuery.isNotBlank() && item.text.contains(searchQuery, ignoreCase = true)) {
                var startIndex = 0
                while (startIndex < item.text.length) {
                    val matchIndex = item.text.indexOf(searchQuery, startIndex, ignoreCase = true)
                    if (matchIndex == -1) {
                        append(item.text.substring(startIndex))
                        break
                    }
                    append(item.text.substring(startIndex, matchIndex))
                    withStyle(SpanStyle(background = Color.Yellow)) {
                        append(item.text.substring(matchIndex, matchIndex + searchQuery.length))
                    }
                    startIndex = matchIndex + searchQuery.length
                }
            } else {
                append(item.text)
            }
        }
    }

    val composeTextAlign = when (item.alignment) {
        com.poirender.sdk.model.TextAlign.CENTER -> TextAlign.Center
        com.poirender.sdk.model.TextAlign.RIGHT -> TextAlign.Right
        com.poirender.sdk.model.TextAlign.JUSTIFY -> TextAlign.Justify
        else -> TextAlign.Start
    }

    Text(
        text = annotatedString,
        textAlign = composeTextAlign,
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.word_paper_bg))
            .padding(horizontal = 32.dp, vertical = 4.dp)
    )
}

@Composable
fun TableComponent(item: PageElement.TableElement, searchQuery: String) {
    val scrollState = rememberScrollState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.word_paper_bg))
            .padding(horizontal = 32.dp, vertical = 8.dp)
            .horizontalScroll(scrollState)
    ) {
        Column {
            item.rows.forEachIndexed { rowIndex, row ->
                val isHeader = rowIndex == 0
                val bgColorRes = if (isHeader) R.color.word_table_header_bg
                                 else if (rowIndex % 2 == 0) R.color.word_table_row_even
                                 else R.color.word_table_row_odd
                val textColorRes = if (isHeader) R.color.word_table_header_text else R.color.word_body_text
                
                Row {
                    row.forEach { cell ->
                        val cellText = cell.elements.filterIsInstance<PageElement.TextElement>().firstOrNull()?.text ?: ""
                        val annotatedString = buildAnnotatedString {
                            withStyle(SpanStyle(color = colorResource(id = textColorRes))) {
                                if (searchQuery.isNotBlank() && cellText.contains(searchQuery, ignoreCase = true)) {
                                    var startIndex = 0
                                    while (startIndex < cellText.length) {
                                        val matchIndex = cellText.indexOf(searchQuery, startIndex, ignoreCase = true)
                                        if (matchIndex == -1) {
                                            append(cellText.substring(startIndex))
                                            break
                                        }
                                        append(cellText.substring(startIndex, matchIndex))
                                        withStyle(SpanStyle(background = Color.Yellow)) {
                                            append(cellText.substring(matchIndex, matchIndex + searchQuery.length))
                                        }
                                        startIndex = matchIndex + searchQuery.length
                                    }
                                } else {
                                    append(cellText)
                                }
                            }
                        }

                        Text(
                            text = annotatedString,
                            modifier = Modifier
                                .background(colorResource(id = bgColorRes))
                                .border(1.dp, colorResource(id = R.color.word_table_border))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImageComponent(item: PageElement.ImageElement) {
    val bitmap = BitmapFactory.decodeByteArray(item.imageData, 0, item.imageData.size)
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(id = R.color.word_paper_bg))
                .padding(horizontal = 32.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun DividerComponent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp)
            .height(2.dp)
            .background(colorResource(id = R.color.word_divider))
    )
}
