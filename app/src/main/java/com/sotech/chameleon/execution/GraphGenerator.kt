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

// Restored Data Classes
data class GraphData(
    val type: GraphType = GraphType.LINE,
    val data: List<Point> = emptyList(),
    val title: String = "",
    val xLabel: String = "",
    val yLabel: String = ""
)

data class Point(val x: Double, val y: Double)

enum class GraphType { LINE, BAR, SCATTER, PIE }

// Restored Math Calculator for legacy logic
class MathCalculator {
    private val constants = mapOf(
        "pi" to PI,
        "e" to E,
        "π" to PI,
        "phi" to (1 + sqrt(5.0)) / 2
    )

    private val functions = mapOf(
        "sin" to { x: Double -> sin(x) },
        "cos" to { x: Double -> cos(x) },
        "tan" to { x: Double -> tan(x) },
        "asin" to { x: Double -> asin(x) },
        "acos" to { x: Double -> acos(x) },
        "atan" to { x: Double -> atan(x) },
        "sinh" to { x: Double -> sinh(x) },
        "cosh" to { x: Double -> cosh(x) },
        "tanh" to { x: Double -> tanh(x) },
        "exp" to { x: Double -> exp(x) },
        "ln" to { x: Double -> ln(x) },
        "log" to { x: Double -> log10(x) },
        "log10" to { x: Double -> log10(x) },
        "log2" to { x: Double -> log2(x) },
        "sqrt" to { x: Double -> sqrt(x) },
        "abs" to { x: Double -> abs(x) },
        "ceil" to { x: Double -> ceil(x) },
        "floor" to { x: Double -> floor(x) },
        "round" to { x: Double -> round(x) }
    )

    fun evaluate(expression: String): Double {
        var expr = expression.lowercase().replace(" ", "").replace("**", "^")
        constants.forEach { (name, value) ->
            expr = expr.replace(name, value.toString())
        }
        return evaluateExpression(expr)
    }

    private fun evaluateExpression(expr: String): Double = evaluateAddSub(expr)

    private fun evaluateAddSub(expr: String): Double {
        var result = evaluateMulDiv(getNextToken(expr, 0).first)
        var i = getNextToken(expr, 0).second
        while (i < expr.length) {
            when (expr[i]) {
                '+' -> {
                    val (token, nextI) = getNextToken(expr, i + 1)
                    result += evaluateMulDiv(token)
                    i = nextI
                }
                '-' -> {
                    val (token, nextI) = getNextToken(expr, i + 1)
                    result -= evaluateMulDiv(token)
                    i = nextI
                }
                else -> break
            }
        }
        return result
    }

    private fun evaluateMulDiv(expr: String): Double {
        var result = evaluatePower(getNextFactor(expr, 0).first)
        var i = getNextFactor(expr, 0).second
        while (i < expr.length) {
            when (expr[i]) {
                '*' -> {
                    val (token, nextI) = getNextFactor(expr, i + 1)
                    result *= evaluatePower(token)
                    i = nextI
                }
                '/' -> {
                    val (token, nextI) = getNextFactor(expr, i + 1)
                    result /= evaluatePower(token)
                    i = nextI
                }
                '%' -> {
                    val (token, nextI) = getNextFactor(expr, i + 1)
                    result %= evaluatePower(token)
                    i = nextI
                }
                else -> break
            }
        }
        return result
    }

    private fun evaluatePower(expr: String): Double {
        val parts = expr.split("^")
        if (parts.size == 1) return evaluateFactor(parts[0])
        var result = evaluateFactor(parts.last())
        for (i in parts.size - 2 downTo 0) {
            result = evaluateFactor(parts[i]).pow(result)
        }
        return result
    }

    private fun evaluateFactor(expr: String): Double {
        if (expr.isEmpty()) throw IllegalArgumentException("Empty expression")

        if (expr[0] == '(') {
            val closingIndex = findMatchingParenthesis(expr, 0)
            return evaluateExpression(expr.substring(1, closingIndex))
        }

        if (expr[0] == '-') return -evaluateFactor(expr.substring(1))
        if (expr[0] == '+') return evaluateFactor(expr.substring(1))

        for ((name, func) in functions) {
            if (expr.startsWith(name)) {
                val argStart = expr.indexOf('(', name.length)
                if (argStart != -1) {
                    val argEnd = findMatchingParenthesis(expr, argStart)
                    val arg = expr.substring(argStart + 1, argEnd)
                    return func(evaluateExpression(arg))
                }
            }
        }

        return expr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number: $expr")
    }

    private fun getNextToken(expr: String, start: Int): Pair<String, Int> {
        var i = start
        val token = StringBuilder()
        while (i < expr.length && expr[i] !in "+-") {
            if (expr[i] == '(') {
                val end = findMatchingParenthesis(expr, i)
                token.append(expr.substring(i, end + 1))
                i = end + 1
            } else {
                token.append(expr[i])
                i++
            }
        }
        return Pair(token.toString(), i)
    }

    private fun getNextFactor(expr: String, start: Int): Pair<String, Int> {
        var i = start
        val token = StringBuilder()
        while (i < expr.length && expr[i] !in "*/%") {
            if (expr[i] == '(') {
                val end = findMatchingParenthesis(expr, i)
                token.append(expr.substring(i, end + 1))
                i = end + 1
            } else {
                token.append(expr[i])
                i++
            }
        }
        return Pair(token.toString(), i)
    }

    private fun findMatchingParenthesis(expr: String, start: Int): Int {
        var count = 1
        var i = start + 1
        while (i < expr.length && count > 0) {
            when (expr[i]) {
                '(' -> count++
                ')' -> count--
            }
            i++
        }
        return i - 1
    }
}

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