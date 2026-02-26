package com.sotech.chameleon.execution

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class GraphGenerator @Inject constructor() {

    fun generateGraph(data: GraphData, width: Int = 800, height: Int = 600): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        val padding = 80f
        val graphWidth = width - 2 * padding
        val graphHeight = height - 2 * padding

        drawAxes(canvas, padding, graphWidth, graphHeight, paint, data)

        when (data.type) {
            GraphType.LINE -> drawLineGraph(canvas, data.data, padding, graphWidth, graphHeight, paint)
            GraphType.BAR -> drawBarGraph(canvas, data.data, padding, graphWidth, graphHeight, paint)
            GraphType.SCATTER -> drawScatterPlot(canvas, data.data, padding, graphWidth, graphHeight, paint)
            GraphType.PIE -> drawPieChart(canvas, data.data, width, height, paint)
        }

        return bitmap
    }

    private fun drawAxes(canvas: Canvas, padding: Float, width: Float, height: Float, paint: Paint, data: GraphData) {
        paint.color = Color.BLACK
        paint.strokeWidth = 2f

        canvas.drawLine(padding, padding, padding, padding + height, paint)
        canvas.drawLine(padding, padding + height, padding + width, padding + height, paint)

        paint.textSize = 30f
        paint.style = Paint.Style.FILL

        if (data.xLabel.isNotEmpty()) {
            canvas.drawText(data.xLabel, padding + width / 2 - 50, padding + height + 60, paint)
        }
        if (data.yLabel.isNotEmpty()) {
            canvas.save()
            canvas.rotate(-90f, padding - 60, padding + height / 2)
            canvas.drawText(data.yLabel, padding - 60, padding + height / 2, paint)
            canvas.restore()
        }

        paint.style = Paint.Style.STROKE
    }

    private fun drawLineGraph(canvas: Canvas, points: List<Point>, padding: Float, width: Float, height: Float, paint: Paint) {
        if (points.isEmpty()) return

        val xMin = points.minOf { it.x }
        val xMax = points.maxOf { it.x }
        val yMin = points.minOf { it.y }
        val yMax = points.maxOf { it.y }

        val xScale = width / (xMax - xMin)
        val yScale = height / (yMax - yMin)

        paint.color = Color.BLUE
        paint.strokeWidth = 3f

        val path = Path()
        points.forEachIndexed { index, point ->
            val x = padding + (point.x - xMin) * xScale
            val y = padding + height - (point.y - yMin) * yScale

            if (index == 0) {
                path.moveTo(x.toFloat(), y.toFloat())
            } else {
                path.lineTo(x.toFloat(), y.toFloat())
            }
        }

        canvas.drawPath(path, paint)

        paint.style = Paint.Style.FILL
        points.forEach { point ->
            val x = padding + (point.x - xMin) * xScale
            val y = padding + height - (point.y - yMin) * yScale
            canvas.drawCircle(x.toFloat(), y.toFloat(), 5f, paint)
        }
        paint.style = Paint.Style.STROKE
    }

    private fun drawBarGraph(canvas: Canvas, points: List<Point>, padding: Float, width: Float, height: Float, paint: Paint) {
        if (points.isEmpty()) return

        val yMin = points.minOf { it.y }.coerceAtMost(0.0)
        val yMax = points.maxOf { it.y }
        val yScale = height / (yMax - yMin)

        val barWidth = width / (points.size * 2f)
        val spacing = barWidth * 0.2f

        paint.color = Color.rgb(66, 133, 244)
        paint.style = Paint.Style.FILL

        points.forEachIndexed { index, point ->
            val x = padding + (index * 2 + 0.5f) * barWidth
            val barHeight = ((point.y - yMin) * yScale).toFloat()
            val y = padding + height - barHeight

            canvas.drawRect(
                x + spacing,
                y,
                x + barWidth * 2 - spacing,
                padding + height,
                paint
            )
        }

        paint.style = Paint.Style.STROKE
    }

    private fun drawScatterPlot(canvas: Canvas, points: List<Point>, padding: Float, width: Float, height: Float, paint: Paint) {
        if (points.isEmpty()) return

        val xMin = points.minOf { it.x }
        val xMax = points.maxOf { it.x }
        val yMin = points.minOf { it.y }
        val yMax = points.maxOf { it.y }

        val xScale = width / (xMax - xMin)
        val yScale = height / (yMax - yMin)

        paint.color = Color.RED
        paint.style = Paint.Style.FILL

        points.forEach { point ->
            val x = padding + (point.x - xMin) * xScale
            val y = padding + height - (point.y - yMin) * yScale
            canvas.drawCircle(x.toFloat(), y.toFloat(), 8f, paint)
        }

        paint.style = Paint.Style.STROKE
    }

    private fun drawPieChart(canvas: Canvas, points: List<Point>, width: Int, height: Int, paint: Paint) {
        if (points.isEmpty()) return

        val total = points.sumOf { it.y }
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 3f

        val colors = listOf(
            Color.rgb(66, 133, 244),
            Color.rgb(219, 68, 55),
            Color.rgb(244, 180, 0),
            Color.rgb(15, 157, 88),
            Color.rgb(171, 71, 188),
            Color.rgb(255, 112, 67)
        )

        paint.style = Paint.Style.FILL
        var startAngle = 0f

        points.forEachIndexed { index, point ->
            val sweepAngle = (point.y / total * 360).toFloat()
            paint.color = colors[index % colors.size]

            canvas.drawArc(
                RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius),
                startAngle,
                sweepAngle,
                true,
                paint
            )

            startAngle += sweepAngle
        }

        paint.style = Paint.Style.STROKE
    }

    fun parseFunctionAndGenerateGraph(expression: String, xMin: Double = -10.0, xMax: Double = 10.0, points: Int = 200): GraphData {
        val calculator = MathCalculator()
        val data = mutableListOf<Point>()
        val step = (xMax - xMin) / points

        for (i in 0 until points) {
            val x = xMin + i * step
            try {
                val expr = expression.replace("x", x.toString())
                val y = calculator.evaluate(expr)
                if (!y.isNaN() && !y.isInfinite()) {
                    data.add(Point(x, y))
                }
            } catch (e: Exception) {
            }
        }

        return GraphData(
            type = GraphType.LINE,
            data = data,
            title = expression,
            xLabel = "x",
            yLabel = "f(x)"
        )
    }

    fun generateTruthTable(expression: String, variables: List<String>): String {
        val numRows = (1 shl variables.size)
        val table = StringBuilder()

        table.append(variables.joinToString(" | ") + " | Result\n")
        table.append("-".repeat(variables.size * 4 + 10) + "\n")

        for (i in 0 until numRows) {
            val values = mutableMapOf<String, Boolean>()
            variables.forEachIndexed { index, variable ->
                values[variable] = ((i shr (variables.size - 1 - index)) and 1) == 1
            }

            val row = values.values.map { if (it) "1" else "0" }.joinToString(" | ")
            val result = evaluateBooleanExpression(expression, values)
            table.append("$row | ${if (result) "1" else "0"}\n")
        }

        return table.toString()
    }

    private fun evaluateBooleanExpression(expr: String, values: Map<String, Boolean>): Boolean {
        var expression = expr.uppercase()
        values.forEach { (variable, value) ->
            expression = expression.replace(variable.uppercase(), if (value) "true" else "false")
        }

        expression = expression.replace("AND", "&&")
            .replace("OR", "||")
            .replace("NOT", "!")
            .replace("XOR", "^")
            .replace("NAND", "!&")
            .replace("NOR", "!|")

        return try {
            val rhino = org.mozilla.javascript.Context.enter()
            rhino.optimizationLevel = -1
            val scope = rhino.initStandardObjects()
            val result = rhino.evaluateString(scope, expression, "Boolean", 1, null)
            org.mozilla.javascript.Context.exit()
            result as? Boolean ?: false
        } catch (e: Exception) {
            try {
                org.mozilla.javascript.Context.exit()
            } catch (ignored: Exception) {}
            false
        }
    }
}