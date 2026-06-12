package dev.gaddal.sifr.core.ui.theme

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Canvas as AndroidCanvas

/**
 * A rounded-rect Gaussian glow drawn into an off-screen software bitmap. Because
 * the blur runs on the software pipeline, BlurMaskFilter works on the full
 * minSdk-24 range (drawing it directly into a hardware-accelerated Compose
 * DrawScope is silently clipped below API 28). Build once and cache via
 * Modifier.drawWithCache. The returned bitmap is [blurRadiusPx]*2 larger than
 * the key on every side so the soft edges aren't cropped.
 */
fun buildGlowBitmap(
    widthPx: Int,
    heightPx: Int,
    cornerRadiusPx: Float,
    blurRadiusPx: Float,
    colorArgb: Int,
): Bitmap {
    val pad = (blurRadiusPx * 2f).toInt().coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(widthPx + pad * 2, heightPx + pad * 2, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorArgb
        maskFilter = BlurMaskFilter(blurRadiusPx, BlurMaskFilter.Blur.NORMAL)
    }
    val rect = RectF(pad.toFloat(), pad.toFloat(), (pad + widthPx).toFloat(), (pad + heightPx).toFloat())
    canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
    return bmp
}
