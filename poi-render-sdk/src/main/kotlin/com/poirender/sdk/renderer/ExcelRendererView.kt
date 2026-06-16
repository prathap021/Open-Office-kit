package com.poirender.sdk.renderer

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.poirender.sdk.R
import com.poirender.sdk.model.WorkbookData

class ExcelRendererView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ScrollView(context, attrs) {

    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(32, 32, 32, 32)
        setBackgroundColor(ContextCompat.getColor(context, R.color.excel_sheet_bg))
    }

    private var currentWorkbook: WorkbookData? = null
    private var searchQuery: String = ""

    init {
        addView(container)
    }

    fun render(workbook: WorkbookData) {
        currentWorkbook = workbook
        applyRender()
    }

    fun highlightSearchTerm(query: String) {
        searchQuery = query
        applyRender()
    }

    private fun applyRender() {
        val workbook = currentWorkbook ?: return
        container.removeAllViews()

        for (sheet in workbook.sheets) {
            // Sheet Name Title (Using active tab colors for representation)
            val titleView = TextView(context).apply {
                text = "Sheet: ${sheet.name}"
                textSize = 20f
                setTypeface(null, Typeface.BOLD)
                setPadding(16, 24, 16, 16)
                setTextColor(ContextCompat.getColor(context, R.color.excel_tab_active_text))
                setBackgroundColor(ContextCompat.getColor(context, R.color.excel_tab_active_bg))
            }
            container.addView(titleView)

            // Make the table horizontally scrollable
            val horizontalScroll = HorizontalScrollView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val table = TableLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 0, 0, 24)
                setBackgroundColor(ContextCompat.getColor(context, R.color.excel_grid_bg))
            }

            for ((rowIndex, row) in sheet.rows.withIndex()) {
                val tableRow = TableRow(context)
                val isHeader = rowIndex == 0
                val bgColorRes = if (isHeader) R.color.excel_col_header_bg
                                 else if (rowIndex % 2 == 0) R.color.excel_row_even
                                 else R.color.excel_row_odd
                val textColorRes = if (isHeader) R.color.excel_col_header_text else R.color.excel_cell_text

                for (cell in row.cells) {
                    val cellView = TextView(context).apply {
                        val spannable = android.text.SpannableStringBuilder(cell)
                        if (searchQuery.isNotBlank() && cell.contains(searchQuery, ignoreCase = true)) {
                            var index = cell.indexOf(searchQuery, ignoreCase = true)
                            while (index >= 0) {
                                spannable.setSpan(android.text.style.BackgroundColorSpan(android.graphics.Color.YELLOW), index, index + searchQuery.length, 0)
                                index = cell.indexOf(searchQuery, index + searchQuery.length, ignoreCase = true)
                            }
                        }
                        text = spannable
                        setPadding(16, 12, 16, 12)
                        setTextColor(ContextCompat.getColor(context, textColorRes))
                        gravity = Gravity.CENTER_VERTICAL
                        
                        val bg = GradientDrawable()
                        bg.setColor(ContextCompat.getColor(context, bgColorRes))
                        bg.setStroke(1, ContextCompat.getColor(context, R.color.excel_cell_border))
                        background = bg
                    }
                    tableRow.addView(cellView)
                }
                table.addView(tableRow)
            }
            horizontalScroll.addView(table)
            container.addView(horizontalScroll)
        }
    }
}
