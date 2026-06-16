package com.poirender.sdk.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poirender.sdk.R
import com.poirender.sdk.model.WorkbookData

@Composable
fun ExcelRenderer(workbook: WorkbookData, searchQuery: String = "") {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.excel_sheet_bg))
            .padding(16.dp)
    ) {
        items(workbook.sheets) { sheet ->
            Text(
                text = "Sheet: ${sheet.name}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.excel_tab_active_text),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorResource(id = R.color.excel_tab_active_bg))
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            )

            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .horizontalScroll(scrollState)
            ) {
                // Determine max columns and rows to size the Box correctly
                var maxCols = 0
                sheet.rows.forEach { row ->
                    if (row.cells.size > maxCols) maxCols = row.cells.size
                }
                val totalRows = sheet.rows.size

                val cellWidth = 120.dp
                val cellHeight = 48.dp

                Box(
                    modifier = Modifier
                        .width(cellWidth * maxCols)
                        .height(cellHeight * totalRows)
                        .background(colorResource(id = R.color.excel_grid_bg))
                ) {
                    sheet.rows.forEachIndexed { rowIndex, row ->
                        val isHeader = rowIndex == 0
                        val bgColorRes = if (isHeader) R.color.excel_col_header_bg
                                         else if (rowIndex % 2 == 0) R.color.excel_row_even
                                         else R.color.excel_row_odd
                        val textColorRes = if (isHeader) R.color.excel_col_header_text else R.color.excel_cell_text

                        row.cells.forEachIndexed { colIndex, cell ->
                            if (cell.colSpan > 0 && cell.rowSpan > 0) {
                                val annotatedString = buildAnnotatedString {
                                    withStyle(SpanStyle(color = colorResource(id = textColorRes))) {
                                        if (searchQuery.isNotBlank() && cell.value.contains(searchQuery, ignoreCase = true)) {
                                            var startIndex = 0
                                            while (startIndex < cell.value.length) {
                                                val matchIndex = cell.value.indexOf(searchQuery, startIndex, ignoreCase = true)
                                                if (matchIndex == -1) {
                                                    append(cell.value.substring(startIndex))
                                                    break
                                                }
                                                append(cell.value.substring(startIndex, matchIndex))
                                                withStyle(SpanStyle(background = Color.Yellow)) {
                                                    append(cell.value.substring(matchIndex, matchIndex + searchQuery.length))
                                                }
                                                startIndex = matchIndex + searchQuery.length
                                            }
                                        } else {
                                            append(cell.value)
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .offset(x = cellWidth * colIndex, y = cellHeight * rowIndex)
                                        .width(cellWidth * cell.colSpan)
                                        .height(cellHeight * cell.rowSpan)
                                        .background(colorResource(id = bgColorRes))
                                        .border(1.dp, colorResource(id = R.color.excel_cell_border))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(text = annotatedString)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
