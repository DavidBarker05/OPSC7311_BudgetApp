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
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

class SpendingChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var days: List<DailySpending> = emptyList()
    private var min: Double? = null
    private var max: Double? = null

    private val dayFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        pathEffect = DashPathEffect(floatArrayOf(dp(8f), dp(6f)), 0f)
        color = ContextCompat.getColor(context, R.color.sage_header)
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.analysis_bar_expense)
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = ContextCompat.getColor(context, R.color.green_dark)
    }
    private val minPaint = goalPaint(R.color.warning_orange)
    private val maxPaint = goalPaint(R.color.destructive_red)
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.hint_text)
        textSize = sp(10f)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.green_text)
        textSize = sp(10f)
        textAlign = Paint.Align.CENTER
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.green_text)
        textSize = sp(12f)
        isFakeBoldText = true
    }
    private val minLabelPaint = goalLabelPaint(R.color.warning_orange)
    private val maxLabelPaint = goalLabelPaint(R.color.destructive_red)

    private val path = Path()
    private val rect = RectF()

    private fun goalPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
        pathEffect = DashPathEffect(floatArrayOf(dp(10f), dp(6f)), 0f)
        this.color = ContextCompat.getColor(context, color)
    }

    private fun goalLabelPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = ContextCompat.getColor(context, color)
        textSize = sp(10f)
        isFakeBoldText = true
        textAlign = Paint.Align.RIGHT
    }

    /**
     * Sets what to draw
     *
     * @param days One entry per day of the period, in order
     * @param min The minimum to draw on the running total panel, or null for none
     * @param max The maximum to draw on the running total panel, or null for none
     */
    fun setData(days: List<DailySpending>, min: Double?, max: Double?) {
        this.days = days
        // A minimum or maximum of zero counts as not set, as it does everywhere else in the app
        this.min = min?.takeIf { it > 0.0 }
        this.max = max?.takeIf { it > 0.0 }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (days.isEmpty()) return

        val leftGutter = dp(44f)
        val rightPad = dp(8f)
        val titleHeight = dp(18f)
        val gap = dp(14f)
        val bottomGutter = dp(22f)
        val plotLeft = paddingLeft + leftGutter
        val plotRight = width - paddingRight - rightPad
        val plotWidth = max(1f, plotRight - plotLeft)

        val top = paddingTop.toFloat()
        val bottom = height - paddingBottom - bottomGutter
        val usable = bottom - top - 2 * titleHeight - gap
        val dailyHeight = usable * 0.4f
        val totalHeight = usable - dailyHeight

        val dailyTop = top + titleHeight
        val dailyBottom = dailyTop + dailyHeight
        val totalTop = dailyBottom + gap + titleHeight
        val totalBottom = totalTop + totalHeight

        drawDailyPanel(canvas, plotLeft, plotRight, plotWidth, dailyTop, dailyBottom, top + dp(12f))
        drawTotalPanel(canvas, plotLeft, plotRight, plotWidth, totalTop, totalBottom, dailyBottom + gap + dp(12f))
        drawDayLabels(canvas, plotLeft, plotWidth, totalBottom)
    }

    private fun drawDailyPanel(canvas: Canvas, left: Float, right: Float, width: Float, top: Float, bottom: Float, titleY: Float) {
        canvas.drawText(context.getString(R.string.chart_daily_spending), paddingLeft.toFloat(), titleY, titlePaint)
        val scaleMax = niceMax(days.maxOf { it.amount })
        drawGrid(canvas, left, right, top, bottom, scaleMax)

        val slot = width / days.size
        val barWidth = (slot * 0.6f).coerceAtMost(dp(28f))
        days.forEachIndexed { index, day ->
            val barHeight = (day.amount / scaleMax).toFloat() * (bottom - top)
            if (barHeight <= 0f) return@forEachIndexed
            val center = left + slot * index + slot / 2f
            val r = dp(4f).coerceAtMost(barWidth / 2f).coerceAtMost(barHeight)
            rect.set(center - barWidth / 2f, bottom - barHeight, center + barWidth / 2f, bottom)
            path.reset()
            path.addRoundRect(rect, floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f), Path.Direction.CW)
            canvas.drawPath(path, barPaint)
        }
    }

    private fun drawTotalPanel(canvas: Canvas, left: Float, right: Float, width: Float, top: Float, bottom: Float, titleY: Float) {
        canvas.drawText(context.getString(R.string.chart_running_total), paddingLeft.toFloat(), titleY, titlePaint)
        val scaleMax = niceMax(max(days.last().runningTotal, max(min ?: 0.0, max ?: 0.0)))
        drawGrid(canvas, left, right, top, bottom, scaleMax)

        // The minimum and maximum are drawn behind the line so the running total stays easy to follow
        min?.let { drawGoal(canvas, it, "${context.getString(R.string.chart_min)} ${MoneyFormatter.format(it)}", minPaint, minLabelPaint, right, top, bottom, scaleMax) }
        max?.let { drawGoal(canvas, it, "${context.getString(R.string.chart_max)} ${MoneyFormatter.format(it)}", maxPaint, maxLabelPaint, right, top, bottom, scaleMax) }

        val slot = width / days.size
        path.reset()
        days.forEachIndexed { index, day ->
            val x = left + slot * index + slot / 2f
            val y = bottom - (day.runningTotal / scaleMax).toFloat() * (bottom - top)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, linePaint)
    }

    private fun drawGoal(
        canvas: Canvas,
        value: Double,
        text: String,
        paint: Paint,
        labelPaint: Paint,
        right: Float,
        top: Float,
        bottom: Float,
        scaleMax: Double
    ) {
        val y = bottom - (value / scaleMax).toFloat() * (bottom - top)
        canvas.drawLine(paddingLeft + dp(44f), y, right, y, paint)
        canvas.drawText(text, right, y - dp(3f), labelPaint)
    }

    private fun drawGrid(canvas: Canvas, left: Float, right: Float, top: Float, bottom: Float, scaleMax: Double) {
        listOf(0.5, 1.0).forEach { fraction ->
            val y = bottom - fraction.toFloat() * (bottom - top)
            canvas.drawLine(left, y, right, y, gridPaint)
            canvas.drawText(MoneyFormatter.compact(scaleMax * fraction), paddingLeft.toFloat(), y + sp(3.5f), axisPaint)
        }
    }

    private fun drawDayLabels(canvas: Canvas, left: Float, width: Float, baseline: Float) {
        // About seven labels fit along the bottom, so show every few days instead of all of them
        val step = ceil(days.size / 7.0).toInt().coerceAtLeast(1)
        val slot = width / days.size
        days.forEachIndexed { index, day ->
            if (index % step != 0) return@forEachIndexed
            val x = left + slot * index + slot / 2f
            canvas.drawText(day.date.format(dayFormatter), x, baseline + dp(14f), labelPaint)
        }
    }

    private fun niceMax(value: Double): Double {
        if (value <= 0.0) return 100.0
        val padded = value * 1.15
        val magnitude = 10.0.pow(floor(log10(padded)))
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
