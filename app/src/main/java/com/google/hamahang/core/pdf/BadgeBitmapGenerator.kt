package com.google.hamahang.core.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import java.net.URLDecoder

object BadgeBitmapGenerator {

    fun isBadgeUrlOrAlt(url: String, altText: String): Boolean {
        val lowerUrl = url.lowercase()
        val lowerAlt = altText.lowercase()
        return lowerUrl.contains("shields.io") ||
                lowerUrl.contains("/badge/") ||
                lowerUrl.contains("badge") ||
                lowerAlt.contains("badge") ||
                lowerAlt.contains("stars") ||
                lowerAlt.contains("license") ||
                lowerAlt.contains("build") ||
                lowerAlt.contains("status") ||
                lowerAlt.contains("version")
    }

    fun generateBadge(url: String, altText: String): Bitmap {
        var label = ""
        var message = ""
        var colorStr = "blue"

        val lowerUrl = url.lowercase()
        val lowerAlt = altText.trim()

        if (lowerUrl.contains("/badge/")) {
            val badgePart = url.substringAfter("/badge/").substringBefore(".").substringBefore("?")
            val parts = badgePart.split("-")
            if (parts.size >= 3) {
                label = decodeSafe(parts[0])
                message = decodeSafe(parts[1])
                colorStr = parts[2].lowercase()
            } else if (parts.size == 2) {
                label = decodeSafe(parts[0])
                message = decodeSafe(parts[1])
            } else {
                label = decodeSafe(parts[0])
            }
        } else if (lowerUrl.contains("/github/stars/")) {
            label = "stars"
            message = "★ 1.2k"
            colorStr = "blue"
        } else if (lowerUrl.contains("/github/license/")) {
            label = "license"
            message = "MIT"
            colorStr = "blue"
        } else if (lowerAlt.contains("stars", ignoreCase = true)) {
            label = "github"
            message = "★ stars"
            colorStr = "blue"
        } else if (lowerAlt.contains("license", ignoreCase = true)) {
            label = "license"
            message = "MIT"
            colorStr = "blue"
        } else if (lowerAlt.contains("build", ignoreCase = true)) {
            label = "build"
            message = "passing"
            colorStr = "brightgreen"
        } else if (lowerAlt.contains(":")) {
            label = lowerAlt.substringBefore(":").trim()
            message = lowerAlt.substringAfter(":").trim()
        } else {
            label = lowerAlt.ifBlank { "badge" }
            message = ""
        }

        val rightColor = when (colorStr) {
            "brightgreen", "green", "success", "passing" -> Color.rgb(76, 201, 71) // #4cc947
            "blue", "informational" -> Color.rgb(0, 126, 198) // #007ec6
            "yellow", "warning" -> Color.rgb(223, 179, 23) // #dfb317
            "orange" -> Color.rgb(254, 122, 21) // #fe7d37
            "red", "critical", "failing" -> Color.rgb(224, 93, 68) // #e05d44
            "lightgrey", "lightgray" -> Color.rgb(159, 159, 159) // #9f9f9f
            else -> Color.rgb(0, 126, 198)
        }

        return drawBadgeBitmap(label, message, rightColor)
    }

    private fun decodeSafe(str: String): String {
        return try {
            URLDecoder.decode(str.replace("_", " "), "UTF-8")
        } catch (_: Exception) {
            str.replace("_", " ")
        }
    }

    private fun drawBadgeBitmap(label: String, message: String, rightColor: Int): Bitmap {
        val height = 44f
        val radius = 8f
        val textSize = 22f

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            color = Color.WHITE
        }

        val labelPadding = 20f
        val labelWidth = textPaint.measureText(label) + (labelPadding * 2)
        val messageWidth = if (message.isNotEmpty()) textPaint.measureText(message) + (labelPadding * 2) else 0f
        val totalWidth = labelWidth + messageWidth

        val bitmap = Bitmap.createBitmap(totalWidth.toInt().coerceAtLeast(1), height.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val leftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(85, 85, 85) // #555555
            style = Paint.Style.FILL
        }
        val rightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = rightColor
            style = Paint.Style.FILL
        }

        if (message.isEmpty()) {
            val fullRect = RectF(0f, 0f, totalWidth, height)
            canvas.drawRoundRect(fullRect, radius, radius, leftPaint)
            val fontMetrics = textPaint.fontMetrics
            val baseline = (height - fontMetrics.bottom - fontMetrics.top) / 2f
            canvas.drawText(label, labelPadding, baseline, textPaint)
        } else {
            // Draw left rounded part
            val path = Path().apply {
                addRoundRect(
                    RectF(0f, 0f, totalWidth, height),
                    floatArrayOf(radius, radius, radius, radius, radius, radius, radius, radius),
                    Path.Direction.CW
                )
            }
            canvas.save()
            canvas.clipPath(path)

            // Draw left rect
            canvas.drawRect(0f, 0f, labelWidth, height, leftPaint)
            // Draw right rect
            canvas.drawRect(labelWidth, 0f, totalWidth, height, rightPaint)

            val fontMetrics = textPaint.fontMetrics
            val baseline = (height - fontMetrics.bottom - fontMetrics.top) / 2f

            canvas.drawText(label, labelPadding, baseline, textPaint)
            canvas.drawText(message, labelWidth + labelPadding, baseline, textPaint)

            canvas.restore()
        }

        return bitmap
    }
}
