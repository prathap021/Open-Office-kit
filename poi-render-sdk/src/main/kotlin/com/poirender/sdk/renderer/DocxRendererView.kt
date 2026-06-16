package com.poirender.sdk.renderer

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Typeface
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.poirender.sdk.model.DocumentPage
import com.poirender.sdk.model.PageElement
import com.poirender.sdk.model.TextAlign

class DocxRendererView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {

    init {
        layoutManager = LinearLayoutManager(context)
    }

    fun render(pages: List<DocumentPage>) {
        val elements = pages.flatMap { it.elements }
        adapter = DocxAdapter(elements)
    }
}

class DocxAdapter(private val items: List<PageElement>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
            }
            TYPE_TEXT -> TextView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(32, 4, 32, 4)
            }
            TYPE_TABLE -> TableLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(32, 8, 32, 8)
            }
            TYPE_IMAGE -> ImageView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                adjustViewBounds = true
                setPadding(32, 8, 32, 8)
            }
            else -> LinearLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 2
                ).also { if (it is ViewGroup.MarginLayoutParams) it.setMargins(32, 8, 32, 8) }
                setBackgroundColor(0xFFCCCCCC.toInt())
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
                    text = item.text
                    textSize = when (item.level) {
                        1 -> 28f
                        2 -> 22f
                        else -> 18f
                    }
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

                    text = spannable
                    gravity = when (item.alignment) {
                        TextAlign.CENTER -> Gravity.CENTER
                        TextAlign.RIGHT -> Gravity.END
                        TextAlign.JUSTIFY -> Gravity.FILL
                        else -> Gravity.START
                    }
                }
            }
            is PageElement.TableElement -> {
                (view as TableLayout).apply {
                    removeAllViews()
                    for (row in item.rows) {
                        val tableRow = TableRow(context)
                        for (cell in row) {
                            val cellView = TextView(context).apply {
                                text = cell
                                setPadding(12, 8, 12, 8)
                                setBackgroundResource(android.R.drawable.editbox_background)
                            }
                            tableRow.addView(cellView)
                        }
                        addView(tableRow)
                    }
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
