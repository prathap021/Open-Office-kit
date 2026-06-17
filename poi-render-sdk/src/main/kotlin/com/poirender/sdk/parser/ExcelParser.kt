package com.poirender.sdk.parser

import com.poirender.sdk.model.*
import com.poirender.sdk.util.SDKLog
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.FileInputStream

class ExcelParser {
    private val TAG = "ExcelParser"

    fun parse(inputStream: InputStream, onProgress: ((Float) -> Unit)? = null): WorkbookData {
        SDKLog.d(TAG, "Starting Excel parse")
        onProgress?.invoke(0.05f)
        val sheets = mutableListOf<SheetData>()

        // Use a temporary file for more robust parsing on Android.
        // WorkbookFactory.create(File) is more efficient and handles OOXML/OLE2 better than InputStream.
        val tempFile = java.io.File.createTempFile("poi_excel", ".tmp")
        try {
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }

            try {
                WorkbookFactory.create(tempFile).use { workbook ->
                    val formatter = DataFormatter()
                    val totalSheets = workbook.numberOfSheets
                    SDKLog.i(TAG, "Document has $totalSheets sheets")

                    for (i in 0 until totalSheets) {
                        val sheet = workbook.getSheetAt(i)
                        val rows = mutableListOf<RowData>()
                        val mergedRegions = sheet.mergedRegions

                        // Pre-calculate merged regions for faster lookup
                        val mergedMap = mutableMapOf<Pair<Int, Int>, Pair<Int, Int>>() // (row, col) -> (rowSpan, colSpan)
                        val mergedRoots = mutableSetOf<Pair<Int, Int>>()
                        
                        for (region in mergedRegions) {
                            for (r in region.firstRow..region.lastRow) {
                                for (c in region.firstColumn..region.lastColumn) {
                                    if (r == region.firstRow && c == region.firstColumn) {
                                        mergedRoots.add(r to c)
                                        mergedMap[r to c] = (region.lastRow - region.firstRow + 1) to (region.lastColumn - region.firstColumn + 1)
                                    } else {
                                        mergedMap[r to c] = 0 to 0 // Marker for non-root merged cell
                                    }
                                }
                            }
                        }

                        for (row in sheet) {
                            val cells = mutableListOf<CellData>()
                            val lastCellNum = row.lastCellNum.toInt()
                            if (lastCellNum > 0) {
                                for (c in 0 until lastCellNum) {
                                    val cell = row.getCell(c)
                                    val value = if (cell != null) formatter.formatCellValue(cell) else ""
                                    
                                    val mergedInfo = mergedMap[row.rowNum to c]
                                    if (mergedInfo != null) {
                                        val (rowSpan, colSpan) = mergedInfo
                                        if (rowSpan > 0) {
                                            cells.add(CellData(value, isMerged = true, colSpan = colSpan, rowSpan = rowSpan))
                                        } else {
                                            cells.add(CellData("", isMerged = true, colSpan = 0, rowSpan = 0))
                                        }
                                    } else {
                                        cells.add(CellData(value, isMerged = false, colSpan = 1, rowSpan = 1))
                                    }
                                }
                            }
                            rows.add(RowData(cells))
                        }
                        sheets.add(SheetData(sheet.sheetName, rows))
                        onProgress?.invoke((i + 1).toFloat() / totalSheets)
                    }
                }
            } catch (e: Exception) {
                if (e.message?.contains("unsupported file type: XML") == true || e.message?.contains("SpreadsheetML") == true) {
                    SDKLog.w(TAG, "File identified as XML; falling back to SpreadsheetMLParser")
                    return parseSpreadsheetML(FileInputStream(tempFile), onProgress)
                } else {
                    SDKLog.e(TAG, "Error parsing excel: ${e.message}", e)
                    println("Document parse data: $sheets")
                }
            }
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }

        SDKLog.d(TAG, "Finished parsing Excel: ${sheets.size} sheets found")
        return WorkbookData(sheets)
    }

    private fun parseSpreadsheetML(inputStream: InputStream, onProgress: ((Float) -> Unit)?): WorkbookData {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        val sheets = mutableListOf<SheetData>()
        var currentSheetName = "Sheet"
        var currentRows: MutableList<RowData>? = null
        
        var currentRowMap: MutableMap<Int, CellData>? = null
        var colIndex = 0
        var inData = false
        var hasDataInCell = false
        val cellText = java.lang.StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "Worksheet" -> {
                        var name = "Sheet"
                        for (i in 0 until parser.attributeCount) {
                            if (parser.getAttributeName(i).endsWith("Name")) {
                                name = parser.getAttributeValue(i)
                                break
                            }
                        }
                        currentSheetName = name
                        currentRows = mutableListOf()
                    }
                    "Row" -> {
                        currentRowMap = mutableMapOf()
                        colIndex = 0
                    }
                    "Cell" -> {
                        hasDataInCell = false
                        var indexStr: String? = null
                        for (i in 0 until parser.attributeCount) {
                            if (parser.getAttributeName(i).endsWith("Index")) {
                                indexStr = parser.getAttributeValue(i)
                                break
                            }
                        }
                        if (indexStr != null) {
                            colIndex = indexStr.toInt() - 1 // ss:Index is 1-based
                        }
                    }
                    "Data" -> { 
                        inData = true
                        cellText.clear() 
                    }
                }
                XmlPullParser.TEXT -> if (inData) cellText.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "Data" -> { 
                        inData = false
                        hasDataInCell = true
                        currentRowMap?.put(colIndex, CellData(cellText.toString()))
                        colIndex++
                    }
                    "Cell" -> {
                        if (!hasDataInCell) {
                            colIndex++
                        }
                    }
                    "Row" -> {
                        currentRowMap?.let { rowMap ->
                            val maxCol = (rowMap.keys.maxOrNull() ?: -1) + 1
                            val rowList = MutableList(maxCol) { i -> rowMap[i] ?: CellData("") }
                            currentRows?.add(RowData(rowList))
                        }
                    }
                    "Worksheet" -> {
                        currentRows?.let { rows ->
                            // Normalize all rows to the same width
                            val maxCols = rows.maxOfOrNull { it.cells.size } ?: 0
                            val normalizedRows = rows.map { row ->
                                if (row.cells.size < maxCols) {
                                    val paddedCells = row.cells.toMutableList()
                                    while (paddedCells.size < maxCols) {
                                        paddedCells.add(CellData(""))
                                    }
                                    RowData(paddedCells)
                                } else {
                                    row
                                }
                            }
                            sheets.add(SheetData(currentSheetName, normalizedRows))
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return WorkbookData(sheets)
    }
}
