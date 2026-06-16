package com.poirender.sdk.model

data class WorkbookData(
    val sheets: List<SheetData>
)

data class SheetData(
    val name: String,
    val rows: List<RowData>
)

data class RowData(
    val cells: List<String>
)
