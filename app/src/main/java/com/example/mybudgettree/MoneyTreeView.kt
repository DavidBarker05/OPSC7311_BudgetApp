package com.example.mybudgettree

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

/**
 * Draws the user's one money tree. The picture follows [GoalTreeGame.stageForLevel]
 */
class MoneyTreeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var growthLevel: Int = 1

    private val soilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.analysis_donut_track)
    }
    private val trunkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.green_dark)
    }
    private val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.sage_button)
    }
    private val leafDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.green_primary)
    }
    private val seedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.green_text)
    }

    fun setLevel(level: Int) {
        growthLevel = level.coerceAtLeast(1)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val widthPx = width.toFloat()
        val heightPx = height.toFloat()
        if (widthPx <= 0f || heightPx <= 0f) return
        val groundTop = heightPx * 0.78f
        canvas.drawOval(
            RectF(widthPx * 0.22f, groundTop, widthPx * 0.78f, heightPx * 0.94f),
            soilPaint
        )
        val stage = GoalTreeGame.stageForLevel(growthLevel)
        val centerX = widthPx / 2f
        when (stage) {
            TreeStage.SEED -> drawSeed(canvas, centerX, groundTop)
            TreeStage.SPROUT -> drawSprout(canvas, centerX, groundTop)
            TreeStage.SAPLING -> drawSapling(canvas, centerX, groundTop)
            TreeStage.YOUNG -> drawCanopy(canvas, centerX, groundTop, layers = 1, trunkWidth = dp(8f))
            TreeStage.MATURE -> {
                val extra = (growthLevel - 5).coerceIn(0, 4)
                drawCanopy(canvas, centerX, groundTop, layers = 2, trunkWidth = dp(10f + extra))
            }
        }
    }

    private fun drawSeed(canvas: Canvas, centerX: Float, groundTop: Float) {
        canvas.drawOval(
            RectF(centerX - dp(7f), groundTop - dp(10f), centerX + dp(7f), groundTop + dp(2f)),
            seedPaint
        )
    }

    private fun drawSprout(canvas: Canvas, centerX: Float, groundTop: Float) {
        canvas.drawRoundRect(
            RectF(centerX - dp(2f), groundTop - dp(28f), centerX + dp(2f), groundTop),
            dp(2f),
            dp(2f),
            trunkPaint
        )
        canvas.drawOval(
            RectF(centerX - dp(16f), groundTop - dp(34f), centerX - dp(2f), groundTop - dp(20f)),
            leafPaint
        )
        canvas.drawOval(
            RectF(centerX + dp(2f), groundTop - dp(34f), centerX + dp(16f), groundTop - dp(20f)),
            leafPaint
        )
    }

    private fun drawSapling(canvas: Canvas, centerX: Float, groundTop: Float) {
        canvas.drawRoundRect(
            RectF(centerX - dp(3f), groundTop - dp(52f), centerX + dp(3f), groundTop),
            dp(3f),
            dp(3f),
            trunkPaint
        )
        canvas.drawCircle(centerX, groundTop - dp(58f), dp(16f), leafPaint)
        canvas.drawCircle(centerX - dp(12f), groundTop - dp(46f), dp(11f), leafDarkPaint)
        canvas.drawCircle(centerX + dp(12f), groundTop - dp(46f), dp(11f), leafPaint)
    }

    private fun drawCanopy(canvas: Canvas, centerX: Float, groundTop: Float, layers: Int, trunkWidth: Float) {
        val trunkTop = groundTop - dp(if (layers == 1) 70f else 84f)
        canvas.drawRoundRect(
            RectF(centerX - trunkWidth / 2f, trunkTop, centerX + trunkWidth / 2f, groundTop),
            dp(4f),
            dp(4f),
            trunkPaint
        )
        val canopyY = trunkTop + dp(6f)
        canvas.drawCircle(centerX, canopyY, dp(if (layers == 1) 22f else 26f), leafDarkPaint)
        canvas.drawCircle(centerX - dp(18f), canopyY + dp(10f), dp(16f), leafPaint)
        canvas.drawCircle(centerX + dp(18f), canopyY + dp(10f), dp(16f), leafPaint)
        if (layers > 1) {
            canvas.drawCircle(centerX, canopyY - dp(18f), dp(18f), leafPaint)
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
