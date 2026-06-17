package com.poirender.sdk.util

import android.util.Log

object SDKLog {
    private const val BASE_TAG = "PoiRenderSDK"

    fun d(tag: String, message: String) {
        Log.d("$BASE_TAG:$tag", message)
    }

    fun i(tag: String, message: String) {
        Log.i("$BASE_TAG:$tag", message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w("$BASE_TAG:$tag", message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$BASE_TAG:$tag", message, throwable)
    }
}
