package com.example.openofficekit

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.poirender.sdk.PoiRenderSDK
import com.poirender.sdk.renderer.DocxRendererView
import com.poirender.sdk.renderer.ExcelRendererView
import com.poirender.sdk.renderer.PptxRendererView
import kotlinx.coroutines.launch

class DocumentViewerActivity : AppCompatActivity() {

    private lateinit var sdk: PoiRenderSDK
    private lateinit var docxView: DocxRendererView
    private lateinit var pptxView: PptxRendererView
    private lateinit var excelView: ExcelRendererView
    private lateinit var renderContainer: FrameLayout
    private lateinit var tvDocumentName: TextView

    private var currentScale = 1.0f
    private var panX = 0f
    private var panY = 0f
    private lateinit var scaleDetector: ScaleGestureDetector
    private lateinit var gestureDetector: android.view.GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_viewer)

        sdk = PoiRenderSDK.init(this)
        docxView = findViewById(R.id.docxView)
        pptxView = findViewById(R.id.pptxView)
        excelView = findViewById(R.id.excelView)
        renderContainer = findViewById(R.id.renderContainer)
        tvDocumentName = findViewById(R.id.tvDocumentName)

        val etSearch = findViewById<EditText>(R.id.etSearch)
        findViewById<Button>(R.id.btnSearch).setOnClickListener {
            val query = etSearch.text.toString()
            if (docxView.visibility == View.VISIBLE) docxView.highlightSearchTerm(query)
            if (pptxView.visibility == View.VISIBLE) pptxView.highlightSearchTerm(query)
            if (excelView.visibility == View.VISIBLE) excelView.highlightSearchTerm(query)
        }

        scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                updateZoom(currentScale * detector.scaleFactor)
                return true
            }
        })

        gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?, e2: MotionEvent,
                distanceX: Float, distanceY: Float
            ): Boolean {
                if (currentScale > 1.0f) {
                    panX -= distanceX
                    panY -= distanceY
                    applyZoomAndPan()
                    return true
                }
                return false
            }
        })

        val uriString = intent.getStringExtra("document_uri")
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            loadDocument(uri)
        } else {
            Toast.makeText(this, "No document provided.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(ev)
        if (ev.pointerCount == 1) {
            gestureDetector.onTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun updateZoom(newScale: Float) {
        currentScale = newScale.coerceIn(1.0f, 5.0f)
        applyZoomAndPan()
    }

    private fun applyZoomAndPan() {
        val maxPanX = (renderContainer.width * (currentScale - 1)) / 2
        val maxPanY = (renderContainer.height * (currentScale - 1)) / 2

        panX = panX.coerceIn(-maxPanX, maxPanX)
        panY = panY.coerceIn(-maxPanY, maxPanY)

        renderContainer.pivotX = renderContainer.width / 2f
        renderContainer.pivotY = renderContainer.height / 2f
        renderContainer.scaleX = currentScale
        renderContainer.scaleY = currentScale
        renderContainer.translationX = panX
        renderContainer.translationY = panY
    }

    private fun loadDocument(uri: Uri) {
        var name = "unknown"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }

        if (name == "unknown") {
            name = uri.lastPathSegment ?: ""
        }
        tvDocumentName.text = name

        lifecycleScope.launch {
            when {
                name.endsWith(".docx", true) -> {
                    sdk.parseDocx(uri).onSuccess { pages ->
                        docxView.visibility = View.VISIBLE
                        pptxView.visibility = View.GONE
                        excelView.visibility = View.GONE
                        docxView.render(pages)
                    }.onFailure {
                        Toast.makeText(this@DocumentViewerActivity, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                        it.printStackTrace()
                    }
                }
                name.endsWith(".pptx", true) || name.endsWith(".ppt", true) -> {
                    sdk.parsePptx(uri).onSuccess { slides ->
                        docxView.visibility = View.GONE
                        pptxView.visibility = View.VISIBLE
                        excelView.visibility = View.GONE
                        pptxView.render(slides)
                    }.onFailure {
                        Toast.makeText(this@DocumentViewerActivity, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                        it.printStackTrace()
                    }
                }
                name.endsWith(".xlsx", true) || name.endsWith(".xls", true) -> {
                    sdk.parseExcel(uri).onSuccess { workbook ->
                        docxView.visibility = View.GONE
                        pptxView.visibility = View.GONE
                        excelView.visibility = View.VISIBLE
                        excelView.render(workbook)
                    }.onFailure {
                        Toast.makeText(this@DocumentViewerActivity, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                        it.printStackTrace()
                    }
                }
                else -> {
                    Toast.makeText(this@DocumentViewerActivity, "Unsupported file type: $name", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}
