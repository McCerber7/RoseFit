package com.pinkfitness.tracker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File

object PhotoUtil {

    private fun photosDir(ctx: Context, day: Int): File {
        val d = File(ctx.filesDir, "photos/day$day")
        d.mkdirs()
        return d
    }

    fun newCameraFile(ctx: Context, day: Int): File =
        File(photosDir(ctx, day), "img_${System.currentTimeMillis()}.jpg")

    private fun orientationToAngle(orientation: Int): Float = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

    private fun rotate(bmp: Bitmap, angle: Float): Bitmap {
        if (angle == 0f) return bmp
        val m = Matrix()
        m.postRotate(angle)
        val r = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        if (r != bmp) bmp.recycle()
        return r
    }

    private fun sampleSize(w: Int, h: Int, maxDim: Int): Int {
        var sample = 1
        while (maxOf(w, h) / (sample * 2) >= maxDim) sample *= 2
        return sample
    }

    /** Копирует изображение из галереи во внутреннее хранилище, уменьшая до ~1440px. */
    fun importFromUri(ctx: Context, uri: Uri, day: Int): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                ?: return null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, 1440) }
            val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return null
            val angle = try {
                ctx.contentResolver.openInputStream(uri)?.use { ins ->
                    orientationToAngle(ExifInterface(ins).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL))
                } ?: 0f
            } catch (e: Exception) { 0f }
            val rotated = rotate(bmp, angle)
            val out = newCameraFile(ctx, day)
            out.outputStream().use { rotated.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            rotated.recycle()
            out.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Декодирует фото из файла с учётом EXIF-поворота, уменьшая до reqSize px. */
    fun decode(path: String, reqSize: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0) return null
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, reqSize) }
            val bmp = BitmapFactory.decodeFile(path, opts) ?: return null
            val angle = orientationToAngle(
                ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            )
            rotate(bmp, angle)
        } catch (e: Exception) {
            null
        }
    }

    /** Уменьшает файл фото на месте (после съёмки камерой), применяя EXIF-поворот. */
    fun shrinkInPlace(path: String, maxDim: Int = 1440) {
        try {
            val f = File(path)
            if (!f.exists()) return
            val bmp = decode(path, maxDim) ?: return
            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            bmp.recycle()
        } catch (e: Exception) {
            // оставляем оригинал
        }
    }

    fun deleteFile(path: String?) {
        path?.let { runCatching { File(it).delete() } }
    }
}
