package com.poirender.sdk.model

data class WorkbookData(
    val sheets: List<SheetData>
)

data class SheetData(
    val name: String,
    val rows: List<RowData>,
    val frozenRows: Int = 0,
    val frozenCols: Int = 0,
    val hiddenRows: Set<Int> = emptySet(),
    val hiddenCols: Set<Int> = emptySet()
)

data class CellData(
    val value: String,
    val type: String = "STRING",
    val formula: String? = null,
    val comment: String? = null,
    val isMerged: Boolean = false,
    val colSpan: Int = 1,
    val rowSpan: Int = 1,
    val numberFormat: String? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val backgroundColor: Int? = null,
    val textColor: Int? = null,
    val wrapText: Boolean = false,
    val hyperlink: String? = null
)

data class RowData(
    val cells: List<CellData>
)
