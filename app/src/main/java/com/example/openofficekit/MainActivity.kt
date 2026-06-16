package com.example.openofficekit

import android.os.Bundle
import android.provider.OpenableColumns
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.poirender.sdk.PoiRenderSDK
import com.poirender.sdk.renderer.DocxRendererView
import com.poirender.sdk.renderer.ExcelRendererView
import com.poirender.sdk.renderer.PptxRendererView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var sdk: PoiRenderSDK
    private lateinit var docxView: DocxRendererView
    private lateinit var pptxView: PptxRendererView
    private lateinit var excelView: ExcelRendererView
    private lateinit var renderContainer: FrameLayout

    private var currentScale = 1.0f
    private lateinit var scaleDetector: ScaleGestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sdk = PoiRenderSDK.init(this)
        docxView = findViewById(R.id.docxView)
        pptxView = findViewById(R.id.pptxView)
        excelView = findViewById(R.id.excelView)
        renderContainer = findViewById(R.id.renderContainer)

        findViewById<Button>(R.id.btnPick).setOnClickListener {
            filePicker.launch("*/*")
            resetZoom()
        }

        findViewById<Button>(R.id.btnZoomIn).setOnClickListener {
            updateZoom(currentScale * 1.25f)
        }

        findViewById<Button>(R.id.btnZoomOut).setOnClickListener {
            updateZoom(currentScale * 0.8f)
        }

        scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                updateZoom(currentScale * detector.scaleFactor)
                return true
            }
        })
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun updateZoom(newScale: Float) {
        currentScale = newScale.coerceIn(0.5f, 5.0f)
        renderContainer.pivotX = renderContainer.width / 2f
        renderContainer.pivotY = renderContainer.height / 2f
        renderContainer.scaleX = currentScale
        renderContainer.scaleY = currentScale
    }

    private fun resetZoom() {
        currentScale = 1.0f
        renderContainer.scaleX = 1.0f
        renderContainer.scaleY = 1.0f
    }

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@registerForActivityResult

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

        lifecycleScope.launch {
            when {
                name.endsWith(".docx", true) -> {
                    sdk.parseDocx(uri).onSuccess { pages ->
                        docxView.visibility = View.VISIBLE
                        pptxView.visibility = View.GONE
                        excelView.visibility = View.GONE
                        docxView.render(pages)
                    }.onFailure {
                        Toast.makeText(
                            this@MainActivity,
                            "Error: ${it.message}",
                            Toast.LENGTH_LONG
                        ).show()
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
                        Toast.makeText(
                            this@MainActivity,
                            "Error: ${it.message}",
                            Toast.LENGTH_LONG
                        ).show()
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
                        Toast.makeText(
                            this@MainActivity,
                            "Error: ${it.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        it.printStackTrace()
                    }
                }
                else -> {
                    Toast.makeText(this@MainActivity, "Unsupported file type: $name", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
