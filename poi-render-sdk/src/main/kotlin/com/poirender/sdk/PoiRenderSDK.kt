package com.poirender.sdk
import android.content.Context
import android.net.Uri
import com.poirender.sdk.model.DocumentPage
import com.poirender.sdk.model.SlideData
import com.poirender.sdk.model.WorkbookData
import com.poirender.sdk.parser.DocxParser
import com.poirender.sdk.parser.ExcelParser
import com.poirender.sdk.parser.PptxParser
import com.poirender.sdk.util.SDKLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PoiRenderSDK private constructor(
    private val context: Context
) {
    private val TAG = "PoiRenderSDK"

    companion object {
        fun init(context: Context): PoiRenderSDK {
            return PoiRenderSDK(context.applicationContext)
        }
    }

    suspend fun parseDocx(uri: Uri, onProgress: ((Float) -> Unit)? = null): Result<List<DocumentPage>> =
        withContext(Dispatchers.IO) {
            SDKLog.i(TAG, "Requesting DOCX parse for URI: $uri")
            runCatching {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: error("Cannot open file")
                DocxParser().parse(stream, onProgress)
            }.onFailure { SDKLog.e(TAG, "DOCX parse failed", it) }
        }

    suspend fun parsePptx(uri: Uri, onProgress: ((Float) -> Unit)? = null): Result<List<SlideData>> =
        withContext(Dispatchers.IO) {
            SDKLog.i(TAG, "Requesting PPTX parse for URI: $uri")
            runCatching {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: error("Cannot open file")
                PptxParser().parse(stream, onProgress)
            }.onFailure { SDKLog.e(TAG, "PPTX parse failed", it) }
        }

    suspend fun parseExcel(uri: Uri, onProgress: ((Float) -> Unit)? = null): Result<WorkbookData> =
        withContext(Dispatchers.IO) {
            SDKLog.i(TAG, "Requesting Excel parse for URI: $uri")
            runCatching {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: error("Cannot open file")
                ExcelParser().parse(stream, onProgress)
            }.onFailure { SDKLog.e(TAG, "Excel parse failed", it) }
        }
}
