package com.example.mybudgettree

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class WateringCanView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var fillPercent: Int = 0
    private val canPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.sage_button)
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2.4f)
        color = ContextCompat.getColor(context, R.color.green_text)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.analysis_progress_blue)
        alpha = 140
    }
    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.sow_field)
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.hint_text)
        textSize = dp(9f)
        textAlign = Paint.Align.RIGHT
    }

    fun setFillPercent(value: Int) {
        fillPercent = value.coerceIn(0, 100)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val body = RectF(w * 0.18f, h * 0.28f, w * 0.62f, h * 0.88f)
        val handle = Path().apply {
            moveTo(body.right - dp(8f), body.top + dp(18f))
            cubicTo(w * 0.92f, body.top, w * 0.94f, body.bottom, body.right - dp(8f), body.bottom - dp(22f))
        }
        val spout = Path().apply {
            moveTo(body.left, body.top + dp(16f))
            lineTo(w * 0.06f, body.top + dp(8f))
            lineTo(w * 0.04f, body.top + dp(22f))
            lineTo(body.left, body.top + dp(28f))
            close()
        }
        canvas.drawPath(handle, outlinePaint)
        canvas.drawRoundRect(RectF(body.left + dp(18f), body.top - dp(18f), body.right - dp(18f), body.top + dp(6f)), dp(10f), dp(10f), canPaint)
        canvas.drawRoundRect(RectF(body.left + dp(18f), body.top - dp(18f), body.right - dp(18f), body.top + dp(6f)), dp(10f), dp(10f), outlinePaint)
        canvas.drawPath(spout, canPaint)
        canvas.drawPath(spout, outlinePaint)
        canvas.drawRoundRect(body, dp(18f), dp(18f), glassPaint)
        val waterHeight = body.height() * (fillPercent / 100f)
        if (waterHeight > 0f) {
            canvas.save()
            canvas.clipRect(body.left, body.bottom - waterHeight, body.right, body.bottom)
            canvas.drawRoundRect(body, dp(18f), dp(18f), waterPaint)
            canvas.restore()
        }
        canvas.drawRoundRect(body, dp(18f), dp(18f), outlinePaint)
        listOf(0, 25, 50, 75, 100).forEach { mark ->
            val y = body.bottom - body.height() * (mark / 100f)
            canvas.drawLine(body.right - dp(14f), y, body.right - dp(4f), y, outlinePaint)
            canvas.drawText("$mark%", body.right - dp(18f), y + dp(3f), tickPaint)
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
