package com.poirender.sdk.parser

import com.poirender.sdk.model.*
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

class ExcelParser {
    fun parse(inputStream: InputStream): WorkbookData {
        // WorkbookFactory automatically handles both HSSFWorkbook (.xls) and XSSFWorkbook (.xlsx)
        val workbook = WorkbookFactory.create(inputStream)
        val formatter = DataFormatter()
        val sheets = mutableListOf<SheetData>()

        for (i in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(i)
            val rows = mutableListOf<RowData>()

            for (row in sheet) {
                val cells = mutableListOf<String>()
                val lastCellNum = row.lastCellNum.toInt()
                if (lastCellNum > 0) {
                    for (c in 0 until lastCellNum) {
                        val cell = row.getCell(c)
                        cells.add(if (cell != null) formatter.formatCellValue(cell) else "")
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
