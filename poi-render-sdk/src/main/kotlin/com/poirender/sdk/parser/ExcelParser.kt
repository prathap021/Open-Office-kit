package com.poirender.sdk.parser

import com.poirender.sdk.model.*
import com.poirender.sdk.util.SDKLog
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

class ExcelParser {
    private val TAG = "ExcelParser"

    fun parse(inputStream: InputStream, onProgress: ((Float) -> Unit)? = null): WorkbookData {
        SDKLog.d(TAG, "Starting Excel parse")
        onProgress?.invoke(0.05f)
        val workbook = WorkbookFactory.create(inputStream)
        val formatter = DataFormatter()
        val sheets = mutableListOf<SheetData>()
        val totalSheets = workbook.numberOfSheets
        SDKLog.i(TAG, "Document has $totalSheets sheets")

        try {
            for (i in 0 until totalSheets) {
            val sheet = workbook.getSheetAt(i)
            val rows = mutableListOf<RowData>()
            val mergedRegions = sheet.mergedRegions

            for (row in sheet) {
                val cells = mutableListOf<CellData>()
                val lastCellNum = row.lastCellNum.toInt()
                if (lastCellNum > 0) {
                    for (c in 0 until lastCellNum) {
                        val cell = row.getCell(c)
                        val value = if (cell != null) formatter.formatCellValue(cell) else ""
                        
                        var colSpan = 1
                        var rowSpan = 1
                        var isMerged = false
                        var isRoot = true

                        for (region in mergedRegions) {
                            if (region.isInRange(row.rowNum, c)) {
                                if (region.firstRow == row.rowNum && region.firstColumn == c) {
                                    colSpan = region.lastColumn - region.firstColumn + 1
                                    rowSpan = region.lastRow - region.firstRow + 1
                                    isMerged = true
                                    isRoot = true
                                } else {
                                    isMerged = true
                                    isRoot = false
                                }
                                break
                            }
                        }

                        if (isMerged && !isRoot) {
                            cells.add(CellData("", isMerged = true, colSpan = 0, rowSpan = 0))
                        } else {
                            cells.add(CellData(value, isMerged = false, colSpan = colSpan, rowSpan = rowSpan))
                        }
                    }
                }
                rows.add(RowData(cells))
            }
            sheets.add(SheetData(sheet.sheetName, rows))
            onProgress?.invoke((i + 1).toFloat() / totalSheets)
            }
        } catch (e: Exception) {
            SDKLog.e(TAG, "Error parsing excel: ${e.message}", e)
        } finally {
            workbook.close()
        }
        SDKLog.d(TAG, "Finished parsing Excel: ${sheets.size} sheets found")
        return WorkbookData(sheets)
    }
}
