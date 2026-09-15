package com.example.mybudgettree

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class DonutTargetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var percent: Int = 0
    private var showPercent: Boolean = true
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.analysis_donut_track)
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.analysis_donut_progress)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.green_text)
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val arc = RectF()

    fun setPercent(value: Int) {
        percent = value.coerceIn(0, 100)
        invalidate()
    }

    fun setShowPercent(show: Boolean) {
        showPercent = show
        invalidate()
    }

    fun setRingColors(trackColor: Int, progressColor: Int) {
        trackPaint.color = trackColor
        progressPaint.color = progressColor
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val stroke = width.coerceAtMost(height) * 0.12f
        trackPaint.strokeWidth = stroke
        progressPaint.strokeWidth = stroke
        textPaint.textSize = height * 0.16f
        val pad = stroke / 2f + dp(6f)
        arc.set(pad, pad, width - pad, height - pad)
        canvas.drawArc(arc, 0f, 360f, false, trackPaint)
        canvas.drawArc(arc, -90f, 360f * (percent / 100f), false, progressPaint)
        if (showPercent) {
            val label = context.getString(R.string.target_percent, percent)
            canvas.drawText(label, width / 2f, height / 2f - (textPaint.ascent() + textPaint.descent()) / 2f, textPaint)
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
