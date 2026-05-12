```kotlin
package com.yourapp.catsear

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

class RadarView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    data class Detection(val label: String, val angleDeg: Float, val distanceFt: Float, val timestamp: Long)

    private val detections = mutableListOf<Detection>()
    private val paintCircle = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.argb(100,0,255,0) }
    private val paintDot = Paint().apply { style = Paint.Style.FILL; color = Color.RED }
    private val paintText = Paint().apply { color = Color.WHITE; textSize = 36f; textAlign = Paint.Align.CENTER; setShadowLayer(2f,0f,0f,Color.BLACK) }
    private var phoneOrientation = 0f

    fun addDetection(d: Detection) {
        detections.add(d)
        val now = System.currentTimeMillis()
        detections.removeAll { now - it.timestamp > 5000 }
        invalidate()
    }

    fun updateOrientation(deg: Float) { phoneOrientation = deg; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height - 120f // leave space for ad at bottom
        val radius = 200f
        canvas.drawCircle(cx, cy, radius, paintCircle)
        for (d in detections) {
            val ang = Math.toRadians((d.angleDeg + phoneOrientation).toDouble())
            val r = d.distanceFt / 10f * radius
            val x = cx + r * sin(ang).toFloat()
            val y = cy - r * cos(ang).toFloat()
            canvas.drawCircle(x, y, 12f, paintDot)
            canvas.drawText(d.label, x, y - 20f, paintText)
        }
    }
}
```
