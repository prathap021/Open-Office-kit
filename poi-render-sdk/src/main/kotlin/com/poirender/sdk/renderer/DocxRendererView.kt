package com.poirender.sdk.renderer

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.poirender.sdk.R
import com.poirender.sdk.model.DocumentPage
import com.poirender.sdk.model.PageElement
import com.poirender.sdk.model.TextAlign

class DocxRendererView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {

    init {
        layoutManager = LinearLayoutManager(context)
        setBackgroundColor(ContextCompat.getColor(context, R.color.word_page_bg))
    }

    private var currentElements: List<PageElement> = emptyList()
    private var searchQuery: String = ""

    fun render(pages: List<DocumentPage>) {
        currentElements = pages.flatMap { it.elements }
        adapter = DocxAdapter(currentElements, searchQuery)
    }

    fun highlightSearchTerm(query: String) {
        searchQuery = query
        adapter = DocxAdapter(currentElements, searchQuery)
    }
}

class DocxAdapter(private val items: List<PageElement>, private val searchQuery: String = "") : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADING = 0
        const val TYPE_TEXT = 1
        const val TYPE_TABLE = 2
        const val TYPE_IMAGE = 3
        const val TYPE_DIVIDER = 4
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PageElement.HeadingElement -> TYPE_HEADING
            is PageElement.TextElement -> TYPE_TEXT
            is PageElement.TableElement -> TYPE_TABLE
            is PageElement.ImageElement -> TYPE_IMAGE
            is PageElement.Divider -> TYPE_DIVIDER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        val view = when (viewType) {
            TYPE_HEADING -> TextView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(32, 24, 32, 8)
                setBackgroundColor(ContextCompat.getColor(context, R.color.word_paper_bg))
            }
            TYPE_TEXT -> TextView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(32, 4, 32, 4)
                setTextColor(ContextCompat.getColor(context, R.color.word_body_text))
                setBackgroundColor(ContextCompat.getColor(context, R.color.word_paper_bg))
            }
            TYPE_TABLE -> android.widget.HorizontalScrollView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                isFillViewport = true
                setPadding(32, 8, 32, 8)
                setBackgroundColor(ContextCompat.getColor(context, R.color.word_paper_bg))
            }
            TYPE_IMAGE -> ImageView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                adjustViewBounds = true
                setPadding(32, 8, 32, 8)
                setBackgroundColor(ContextCompat.getColor(context, R.color.word_paper_bg))
            }
            else -> LinearLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 2
                ).also { if (it is ViewGroup.MarginLayoutParams) it.setMargins(32, 8, 32, 8) }
                setBackgroundColor(ContextCompat.getColor(context, R.color.word_divider))
            }
        }
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        val view = holder.itemView
        when (item) {
            is PageElement.HeadingElement -> {
                (view as TextView).apply {
                    val spannable = SpannableStringBuilder(item.text)
                    if (searchQuery.isNotBlank() && item.text.contains(searchQuery, ignoreCase = true)) {
                        var index = item.text.indexOf(searchQuery, ignoreCase = true)
                        while (index >= 0) {
                            spannable.setSpan(android.text.style.BackgroundColorSpan(android.graphics.Color.YELLOW), index, index + searchQuery.length, 0)
                            index = item.text.indexOf(searchQuery, index + searchQuery.length, ignoreCase = true)
                        }
                    }
                    text = spannable
                    textSize = when (item.level) {
                        1 -> 28f
                        2 -> 22f
                        else -> 18f
                    }
                    val colorRes = when (item.level) {
                        1 -> R.color.word_heading1
                        2 -> R.color.word_heading2
                        3 -> R.color.word_heading3
                        else -> R.color.word_heading4
                    }
                    setTextColor(ContextCompat.getColor(context, colorRes))
                    setTypeface(null, Typeface.BOLD)
                }
            }
            is PageElement.TextElement -> {
                (view as TextView).apply {
                    val spannable = SpannableStringBuilder(item.text)
                    val len = spannable.length
                    if (item.isBold) spannable.setSpan(StyleSpan(Typeface.BOLD), 0, len, 0)
                    if (item.isItalic) spannable.setSpan(StyleSpan(Typeface.ITALIC), 0, len, 0)
                    if (item.isUnderline) spannable.setSpan(UnderlineSpan(), 0, len, 0)
                    if (item.fontSize > 0) spannable.setSpan(AbsoluteSizeSpan(item.fontSize, true), 0, len, 0)

                    if (searchQuery.isNotBlank() && item.text.contains(searchQuery, ignoreCase = true)) {
                        var index = item.text.indexOf(searchQuery, ignoreCase = true)
                        while (index >= 0) {
                            spannable.setSpan(android.text.style.BackgroundColorSpan(android.graphics.Color.YELLOW), index, index + searchQuery.length, 0)
                            index = item.text.indexOf(searchQuery, index + searchQuery.length, ignoreCase = true)
                        }
                    }

                    text = spannable
                    if (item.color != null) {
                        setTextColor(item.color)
                    } else {
                        setTextColor(ContextCompat.getColor(context, R.color.word_body_text))
                    }
                    gravity = when (item.alignment) {
                        TextAlign.CENTER -> Gravity.CENTER
                        TextAlign.RIGHT -> Gravity.END
                        TextAlign.JUSTIFY -> Gravity.FILL
                        else -> Gravity.START
                    }
                }
            }
            is PageElement.TableElement -> {
                val tableLayout = TableLayout(view.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    removeAllViews()
                    for ((rowIndex, row) in item.rows.withIndex()) {
                        val tableRow = TableRow(view.context)
                        val isHeader = rowIndex == 0
                        val bgColorRes = if (isHeader) R.color.word_table_header_bg
                                         else if (rowIndex % 2 == 0) R.color.word_table_row_even
                                         else R.color.word_table_row_odd
                        val textColorRes = if (isHeader) R.color.word_table_header_text else R.color.word_body_text
                        
                        for (cell in row) {
                            val cellView = TextView(view.context).apply {
                                val spannable = SpannableStringBuilder(cell)
                                if (searchQuery.isNotBlank() && cell.contains(searchQuery, ignoreCase = true)) {
                                    var index = cell.indexOf(searchQuery, ignoreCase = true)
                                    while (index >= 0) {
                                        spannable.setSpan(android.text.style.BackgroundColorSpan(android.graphics.Color.YELLOW), index, index + searchQuery.length, 0)
                                        index = cell.indexOf(searchQuery, index + searchQuery.length, ignoreCase = true)
                                    }
                                }
                                text = spannable
                                setPadding(16, 12, 16, 12)
                                setTextColor(ContextCompat.getColor(view.context, textColorRes))
                                
                                val bg = GradientDrawable()
                                bg.setColor(ContextCompat.getColor(view.context, bgColorRes))
                                bg.setStroke(1, ContextCompat.getColor(view.context, R.color.word_table_border))
                                background = bg
                            }
                            tableRow.addView(cellView)
                        }
                        addView(tableRow)
                    }
                }
                
                (view as android.widget.HorizontalScrollView).apply {
                    removeAllViews()
                    addView(tableLayout)
                }
            }
            is PageElement.ImageElement -> {
                (view as ImageView).apply {
                    val bitmap = BitmapFactory.decodeByteArray(item.imageData, 0, item.imageData.size)
                    setImageBitmap(bitmap)
                }
            }
            is PageElement.Divider -> {
                // Background color already set
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
