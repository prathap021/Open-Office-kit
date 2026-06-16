package com.poirender.sdk.renderer

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.poirender.sdk.model.WorkbookData

class ExcelRendererView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ScrollView(context, attrs) {

    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(32, 32, 32, 32)
    }

    init {
        addView(container)
    }

    fun render(workbook: WorkbookData) {
        container.removeAllViews()

        for (sheet in workbook.sheets) {
            // Sheet Name Title
            val titleView = TextView(context).apply {
                text = "Sheet: ${sheet.name}"
                textSize = 20f
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 24, 0, 16)
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
            }

            for (row in sheet.rows) {
                val tableRow = TableRow(context)
                for (cell in row.cells) {
                    val cellView = TextView(context).apply {
                        text = cell
                        setPadding(16, 12, 16, 12)
                        setBackgroundResource(android.R.drawable.editbox_background)
                        gravity = Gravity.CENTER_VERTICAL
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
