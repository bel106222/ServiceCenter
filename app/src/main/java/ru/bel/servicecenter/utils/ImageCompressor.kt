package ru.bel.servicecenter.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import java.io.ByteArrayOutputStream

object ImageCompressor {

    /**
     * Сжимает изображение из Uri и возвращает байты JPEG.
     * @param context контекст приложения
     * @param uri Uri выбранного изображения
     * @param maxSize максимальный размер по длинной стороне (1280)
     * @param quality качество сжатия (0-100)
     */
    fun compress(context: Context, uri: Uri, maxSize: Int = 1280, quality: Int = 80): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            inputStream.close()

            // Масштабируем, если нужно
            val resizedBitmap = scaleDown(bitmap, maxSize)
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            resizedBitmap.recycle()
            bitmap.recycle()
            outputStream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    private fun scaleDown(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxDimension = maxOf(width, height)
        if (maxDimension <= maxSize) return bitmap

        val scale = maxSize.toFloat() / maxDimension
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}