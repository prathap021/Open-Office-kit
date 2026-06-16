package com.poirender.sdk.parser

import com.poirender.sdk.model.*
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

class ExcelParser {
    fun parse(inputStream: InputStream): WorkbookData {
        val workbook = WorkbookFactory.create(inputStream)
        val formatter = DataFormatter()
        val sheets = mutableListOf<SheetData>()

        for (i in 0 until workbook.numberOfSheets) {
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
        }

        workbook.close()
        return WorkbookData(sheets)
    }
}
