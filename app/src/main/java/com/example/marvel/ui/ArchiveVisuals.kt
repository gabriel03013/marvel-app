package com.example.marvel.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.marvel.R
import com.example.marvel.data.ArchiveItem
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class ConnectionNode(val relation: String, val item: ArchiveItem)
data class ArchiveMetric(val label: String, val value: Int)

/** A small native canvas graph; the records remain available as accessible rows below it. */
class ConnectionGraphView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var centerLabel: String = "Character"
        set(value) { field = value; contentDescription = description(); invalidate() }
    var nodes: List<ConnectionNode> = emptyList()
        set(value) { field = value.take(8); contentDescription = description(); invalidate() }
    var onNodeClick: ((ArchiveItem) -> Unit)? = null
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG)
    private val nodeCenters = mutableListOf<Pair<Float, Float>>()
    private val paper = context.getColor(R.color.warm_white)
    private val red = context.getColor(R.color.archive_red)
    private val blue = context.getColor(R.color.archive_blue)
    private val rule = context.getColor(R.color.paper_rule)
    init { contentDescription = description(); isClickable = true; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES }
    private fun description() = buildString {
        append("Connection map for $centerLabel.")
        if (nodes.isEmpty()) append(" No documented connections.")
        else nodes.forEach { append(" ${it.relation}: ${it.item.name}.") }
        append(" Activate the map or use the connection records below to open a dossier.")
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desired = (160 * resources.displayMetrics.density).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), resolveSize(desired, heightMeasureSpec))
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(paper)
        val cx = width / 2f; val cy = height / 2f
        val radius = min(width * .37f, height * .36f)
        nodeCenters.clear()
        if (nodes.isEmpty()) return
        nodes.forEachIndexed { index, node ->
            val angle = -Math.PI / 2.0 + (Math.PI * 2.0 * index / nodes.size)
            val x = cx + (cos(angle) * radius).toFloat(); val y = cy + (sin(angle) * radius).toFloat()
            nodeCenters += x to y
            ink.color = rule; ink.strokeWidth = dp(1.5f); ink.style = Paint.Style.STROKE
            canvas.drawLine(cx, cy, x, y, ink)
            ink.style = Paint.Style.FILL; ink.color = if (node.relation in listOf("Power", "Team")) blue else red
            canvas.drawCircle(x, y, dp(25f), ink)
            ink.color = paper; ink.textAlign = Paint.Align.CENTER; ink.textSize = dp(8.5f); ink.typeface = android.graphics.Typeface.DEFAULT_BOLD
            val shortName = node.item.name.take(12)
            canvas.drawText(shortName, x, y + dp(3f), ink)
            ink.color = context.getColor(R.color.muted_ink); ink.textSize = dp(8f); ink.typeface = android.graphics.Typeface.DEFAULT
            canvas.drawText(node.relation, x, y + dp(37f), ink)
        }
        ink.color = red; ink.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, dp(31f), ink)
        ink.color = paper; ink.textAlign = Paint.Align.CENTER; ink.textSize = dp(10f); ink.typeface = android.graphics.Typeface.DEFAULT_BOLD
        val center = centerLabel.take(16)
        canvas.drawText(center, cx, cy + dp(4f), ink)
    }
    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_UP) {
            val hit = nodeCenters.indices.minByOrNull { i ->
                val (x, y) = nodeCenters[i]; val dx = x - event.x; val dy = y - event.y; dx * dx + dy * dy
            }
            if (hit != null) {
                val (x, y) = nodeCenters[hit]; val dx = x - event.x; val dy = y - event.y
                if (dx * dx + dy * dy <= dp(34f) * dp(34f)) { performClick(); onNodeClick?.invoke(nodes[hit].item); return true }
            }
            return true
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
    private fun dp(value: Float) = value * resources.displayMetrics.density
}

/** Native horizontal bars for dossier counts; the caller omits metrics that are not documented. */
class ArchiveMetricsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var metrics: List<ArchiveMetric> = emptyList()
        set(value) { field = value; contentDescription = value.joinToString(". ") { "${it.label}: ${it.value}" }; requestLayout(); invalidate() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val inkColor = context.getColor(R.color.ink_black)
    private val blue = context.getColor(R.color.archive_blue)
    private val track = context.getColor(R.color.paper_rule)
    init { contentDescription = "Archive intelligence chart"; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = (metrics.size * 44 * resources.displayMetrics.density).toInt().coerceAtLeast((48 * resources.displayMetrics.density).toInt())
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), resolveSize(h, heightMeasureSpec))
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (metrics.isEmpty()) return
        val density = resources.displayMetrics.density
        val labelWidth = width * .42f
        val barWidth = width - labelWidth - 44 * density
        val maxValue = metrics.maxOf { it.value }.coerceAtLeast(1)
        metrics.forEachIndexed { index, metric ->
            val y = (index * 44 + 16) * density
            paint.color = inkColor; paint.textSize = 13 * density; paint.textAlign = Paint.Align.LEFT
            canvas.drawText(metric.label.take(24), 0f, y, paint)
            paint.color = track; canvas.drawRoundRect(labelWidth, y - 11 * density, labelWidth + barWidth, y - 2 * density, 4 * density, 4 * density, paint)
            paint.color = blue
            val filled = barWidth * metric.value / maxValue.toFloat()
            canvas.drawRoundRect(labelWidth, y - 11 * density, labelWidth + filled.coerceAtLeast(3 * density), y - 2 * density, 4 * density, 4 * density, paint)
            paint.color = inkColor; paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(metric.value.toString(), width.toFloat(), y, paint)
        }
    }
}
