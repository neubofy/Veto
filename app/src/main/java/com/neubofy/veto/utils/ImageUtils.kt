package com.neubofy.veto.utils

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import java.io.ByteArrayOutputStream


fun imageToByteArray(image: Image): ByteArray {
    return when (image.format) {
        // XXX: I'm not sure if the ImageCapture use case ever returns YUV_420_888 (or always JPEG).
        // Still, let's include it just in case.
        ImageFormat.YUV_420_888 -> yuv420488ImageToJpegImage(image, 100) ?: ByteArray(0)
        ImageFormat.JPEG -> jpegImageToByteArray(image)
        else -> {
            Log.e("ImageUtils", "Unknown image format: ${image.format}")
            ByteArray(0)
        }
    }
}

// From Neubofy's Veto
private fun jpegImageToByteArray(image: Image): ByteArray {
    val planes = image.planes
    val buffer = planes[0].buffer
    val data = ByteArray(buffer.capacity())
    buffer[data]
    return data
}

// Taken from: https://blog.minhazav.dev/how-to-convert-yuv-420-sp-android.media.Image-to-Bitmap-or-jpeg/
fun yuv420488ImageToJpegImage(image: Image, imageQuality: Int): ByteArray? {
    require(image.format == ImageFormat.YUV_420_888) { "Invalid image format" }
    val yuvImage: YuvImage = toYuvImage(image)
    val width = image.width
    val height = image.height

    // Convert to jpeg
    var jpegImage: ByteArray? = null
    ByteArrayOutputStream().use { out ->
        yuvImage.compressToJpeg(Rect(0, 0, width, height), imageQuality, out)
        jpegImage = out.toByteArray()
    }
    return jpegImage
}

// Taken from: https://blog.minhazav.dev/how-to-convert-yuv-420-sp-android.media.Image-to-Bitmap-or-jpeg/
fun toYuvImage(image: Image): YuvImage {
    require(image.format == ImageFormat.YUV_420_888) { "Invalid image format" }
    val width = image.width
    val height = image.height

    // Order of U/V channel guaranteed, read more:
    // https://developer.android.com/reference/android/graphics/ImageFormat#YUV_420_888
    val yPlane = image.planes[0]
    val uPlane = image.planes[1]
    val vPlane = image.planes[2]
    val yBuffer = yPlane.buffer
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer

    // Full size Y channel and quarter size U+V channels.
    val numPixels = (width * height * 1.5f).toInt()
    val nv21 = ByteArray(numPixels)
    var index = 0

    // Copy Y channel.
    val yRowStride = yPlane.rowStride
    val yPixelStride = yPlane.pixelStride
    for (y in 0 until height) {
        for (x in 0 until width) {
            nv21[index++] = yBuffer[y * yRowStride + x * yPixelStride]
        }
    }

    // Copy VU data; NV21 format is expected to have YYYYVU packaging.
    // The U/V planes are guaranteed to have the same row stride and pixel stride.
    val uvRowStride = uPlane.rowStride
    val uvPixelStride = uPlane.pixelStride
    val uvWidth = width / 2
    val uvHeight = height / 2
    for (y in 0 until uvHeight) {
        for (x in 0 until uvWidth) {
            val bufferIndex = y * uvRowStride + x * uvPixelStride
            // V channel.
            nv21[index++] = vBuffer[bufferIndex]
            // U channel.
            nv21[index++] = uBuffer[bufferIndex]
        }
    }
    return YuvImage(
        nv21, ImageFormat.NV21, width, height, null
    )
}
