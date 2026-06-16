package com.poirender.sdk.renderer

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.poirender.sdk.model.SlideData

class PptxRendererView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val viewPager = ViewPager2(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    init {
        setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, com.poirender.sdk.R.color.ppt_viewer_bg))
        addView(viewPager)
    }

    private var searchQuery = ""

    fun render(slides: List<SlideData>) {
        viewPager.adapter = PptxAdapter(slides, searchQuery)
    }

    fun highlightSearchTerm(query: String) {
        searchQuery = query
        (viewPager.adapter as? PptxAdapter)?.updateSearchQuery(query)
    }
}

class PptxAdapter(private val slides: List<SlideData>, private var searchQuery: String = "") : RecyclerView.Adapter<PptxAdapter.SlideViewHolder>() {

    fun updateSearchQuery(query: String) {
        searchQuery = query
        notifyDataSetChanged()
    }

    class SlideViewHolder(val slideCanvasView: SlideCanvasView) : RecyclerView.ViewHolder(slideCanvasView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
        val slideCanvasView = SlideCanvasView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return SlideViewHolder(slideCanvasView)
    }

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        holder.slideCanvasView.setSlide(slides[position], searchQuery)
    }

    override fun getItemCount(): Int = slides.size
}
