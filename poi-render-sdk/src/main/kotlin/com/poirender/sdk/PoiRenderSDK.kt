package com.poirender.sdk

import android.content.Context
import android.net.Uri
import com.poirender.sdk.model.DocumentPage
import com.poirender.sdk.model.SlideData
import com.poirender.sdk.model.WorkbookData
import com.poirender.sdk.parser.DocxParser
import com.poirender.sdk.parser.ExcelParser
import com.poirender.sdk.parser.PptxParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PoiRenderSDK private constructor(
    private val context: Context
) {
    companion object {
        @Volatile private var instance: PoiRenderSDK? = null

        fun init(context: Context): PoiRenderSDK {
            return instance ?: synchronized(this) {
                instance ?: PoiRenderSDK(
                    context.applicationContext
                ).also { instance = it }
            }
        }

        fun getInstance(): PoiRenderSDK =
            instance ?: throw IllegalStateException(
                "Call PoiRenderSDK.init(context) first"
            )
    }

    suspend fun parseDocx(uri: Uri): Result<List<DocumentPage>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: error("Cannot open file")
                DocxParser().parse(stream)
            }
        }

    suspend fun parsePptx(uri: Uri): Result<List<SlideData>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: error("Cannot open file")
                PptxParser().parse(stream)
            }
        }

    suspend fun parseExcel(uri: Uri): Result<WorkbookData> =
        withContext(Dispatchers.IO) {
            runCatching {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: error("Cannot open file")
                ExcelParser().parse(stream)
            }
        }
}
