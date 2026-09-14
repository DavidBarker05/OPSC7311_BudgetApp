package com.example.mybudgettree

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.pow

class AnalysisChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var labels: List<String> = emptyList()
    private var incomeValues: List<Double> = emptyList()
    private var expenseValues: List<Double> = emptyList()

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        pathEffect = DashPathEffect(floatArrayOf(dp(8f), dp(6f)), 0f)
        color = ContextCompat.getColor(context, R.color.sage_header)
    }
    private val incomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.analysis_bar_income)
    }
    private val expensePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.analysis_bar_expense)
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.hint_text)
        textSize = sp(10f)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.green_text)
        textSize = sp(10f)
        textAlign = Paint.Align.CENTER
    }

    fun setData(labels: List<String>, incomeValues: List<Double>, expenseValues: List<Double>) {
        this.labels = labels
        this.incomeValues = incomeValues
        this.expenseValues = expenseValues
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (labels.isEmpty()) return

        val count = labels.size
        val leftGutter = dp(36f)
        val bottomGutter = dp(22f)
        val topPad = dp(8f)
        val plotLeft = paddingLeft + leftGutter
        val plotRight = width - paddingRight - dp(8f)
        val plotTop = paddingTop + topPad
        val plotBottom = height - paddingBottom - bottomGutter
        val plotHeight = max(1f, plotBottom - plotTop)
        val plotWidth = max(1f, plotRight - plotLeft)
        val maxValue = niceMax(
            max(
                incomeValues.maxOrNull() ?: 0.0,
                expenseValues.maxOrNull() ?: 0.0
            )
        )
        val ticks = listOf(0.25, 0.5, 0.75, 1.0).map { maxValue * it }

        ticks.forEach { tick ->
            val y = plotBottom - (tick / maxValue).toFloat() * plotHeight
            canvas.drawLine(plotLeft, y, plotRight, y, gridPaint)
            val label = MoneyFormatter.compact(tick)
            canvas.drawText(label, paddingLeft.toFloat(), y + sp(3.5f), axisPaint)
        }

        val groupWidth = plotWidth / count
        val barWidth = groupWidth * 0.28f
        val corner = dp(6f)
        val path = Path()
        val rect = RectF()

        labels.forEachIndexed { index, label ->
            val center = plotLeft + groupWidth * index + groupWidth / 2f
            val income = incomeValues.getOrElse(index) { 0.0 }
            val expense = expenseValues.getOrElse(index) { 0.0 }
            val incomeHeight = ((income / maxValue).toFloat() * plotHeight).coerceAtLeast(0f)
            val expenseHeight = ((expense / maxValue).toFloat() * plotHeight).coerceAtLeast(0f)

            drawBar(canvas, path, rect, center - barWidth - dp(2f), plotBottom, barWidth, incomeHeight, corner, incomePaint)
            drawBar(canvas, path, rect, center + dp(2f), plotBottom, barWidth, expenseHeight, corner, expensePaint)
            canvas.drawText(label, center, height - paddingBottom - dp(4f), labelPaint)
        }
    }

    private fun drawBar(
        canvas: Canvas,
        path: Path,
        rect: RectF,
        left: Float,
        bottom: Float,
        width: Float,
        height: Float,
        radius: Float,
        paint: Paint
    ) {
        if (height <= 0f) return
        val top = bottom - height
        val r = radius.coerceAtMost(width / 2f).coerceAtMost(height)
        rect.set(left, top, left + width, bottom)
        path.reset()
        path.addRoundRect(
            rect,
            floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f),
            Path.Direction.CW
        )
        canvas.drawPath(path, paint)
    }

    private fun niceMax(maxValue: Double): Double {
        if (maxValue <= 0.0) return 15_000.0
        val padded = maxValue * 1.2
        val magnitude = 10.0.pow(kotlin.math.floor(kotlin.math.log10(padded)))
        val residual = padded / magnitude
        val nice = when {
            residual <= 1 -> 1.0
            residual <= 2 -> 2.0
            residual <= 5 -> 5.0
            else -> 10.0
        }
        return nice * magnitude
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
}
