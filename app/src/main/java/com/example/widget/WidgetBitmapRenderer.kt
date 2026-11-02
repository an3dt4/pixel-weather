package com.example.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Renders crisp, aspect-ratio-accurate anti-aliased bitmaps for Glance RemoteViews:
 * - Stadium capsule pill (`Semi-transparent`, `Transparent` with 1.5dp outline border, `Solid`, or any 0%..100% alpha)
 * - Rounded card container with optional border and 0%..100% alpha
 * - Deep ocean blue weather squircle badge (`[⛅ / 12°C]`)
 */
object WidgetBitmapRenderer {

    private const val RENDER_DENSITY = 2.75f

    fun createAtAGlanceContainerBitmap(
        widthDp: Float,
        heightDp: Float,
        fillColor: Color,
        alpha: Float,
        showBorder: Boolean,
        borderColor: Color,
        isStadiumPill: Boolean,
        cornerRadiusDp: Float = 28f
    ): Bitmap {
        val clampedAlpha = alpha.coerceIn(0f, 1f)
        val widthPx = (widthDp * RENDER_DENSITY).toInt().coerceIn(220, 1080)
        val heightPx = (heightDp * RENDER_DENSITY).toInt().coerceIn(130, 720)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        if (clampedAlpha <= 0.005f && !showBorder) {
            return bitmap
        }

        val canvas = Canvas(bitmap)
        val strokePx = 1.6f * RENDER_DENSITY
        val inset = if (showBorder) strokePx / 2f + 1f else 1f
        val rect = RectF(inset, inset, widthPx - inset, heightPx - inset)
        val radiusPx = if (isStadiumPill) {
            rect.height() / 2f
        } else {
            (cornerRadiusDp * RENDER_DENSITY).coerceAtMost(rect.height() / 2f)
        }

        if (clampedAlpha > 0.005f) {
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = fillColor.copy(alpha = clampedAlpha).toArgb()
            }
            canvas.drawRoundRect(rect, radiusPx, radiusPx, fillPaint)
        }

        if (showBorder) {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokePx
                color = borderColor.copy(alpha = 0.82f).toArgb()
            }
            canvas.drawRoundRect(rect, radiusPx, radiusPx, strokePaint)
        }

        return bitmap
    }

    fun createWeatherSquircleBitmap(
        badgeColor: Color,
        widthDp: Float = 58f,
        heightDp: Float = 54f,
        cornerRadiusDp: Float = 18f
    ): Bitmap {
        val widthPx = (widthDp * RENDER_DENSITY).toInt().coerceAtLeast(100)
        val heightPx = (heightDp * RENDER_DENSITY).toInt().coerceAtLeast(100)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rect = RectF(0.5f, 0.5f, widthPx - 0.5f, heightPx - 0.5f)
        val radiusPx = cornerRadiusDp * RENDER_DENSITY
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = badgeColor.toArgb()
        }
        canvas.drawRoundRect(rect, radiusPx, radiusPx, paint)
        return bitmap
    }
}
