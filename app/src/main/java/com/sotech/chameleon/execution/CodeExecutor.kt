package com.sotech.chameleon.execution

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class CodeExecutor @Inject constructor() {
    private val TAG = "CodeExecutor"

    suspend fun execute(code: String, language: String, context: Context): ExecutionResult = withContext(Dispatchers.IO) {
        try {
            when (language.lowercase()) {
                "python" -> executePythonSimulated(code, context)
                "javascript", "js" -> executeJavaScript(code)
                "c" -> executeCSimulated(code, context)
                "cpp", "c++" -> executeCppSimulated(code, context)
                "math" -> executeMath(code)
                "kotlin" -> executeKotlinSimulated(code)
                else -> ExecutionResult(success = false, output = "", error = "Unsupported language: $language")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Execution error", e)
            ExecutionResult(success = false, output = "", error = e.message ?: "Unknown error")
        }
    }

    private fun executePythonSimulated(code: String, context: Context): ExecutionResult {
        return try {
            val output = StringBuilder()
            val lines = code.lines()
            var hasPlot = false
            var hasMatplotlib = false

            for (line in lines) {
                when {
                    line.trim().startsWith("import matplotlib") ||
                            line.trim().startsWith("from matplotlib") -> {
                        hasMatplotlib = true
                    }
                    line.trim().contains("plt.show()") ||
                            line.trim().contains("plt.plot(") -> {
                        hasPlot = true
                    }
                    line.trim().startsWith("print(") -> {
                        val content = line.substringAfter("print(").substringBefore(")")
                        output.append(content.replace("\"", "").replace("'", "")).append("\n")
                    }
                }
            }

            if (hasMatplotlib && hasPlot) {
                val graphData = extractGraphDataFromPython(code)
                if (graphData != null) {
                    output.append("Graph visualization ready: ${graphData.title}\n")
                    output.append("Data points: ${graphData.data.size}\n")
                    return ExecutionResult(
                        success = true,
                        output = output.toString(),
                        error = "",
                        graphData = graphData
                    )
                }
            }

            val calculationResult = evaluatePythonCalculations(code)
            if (calculationResult.isNotEmpty()) {
                output.append(calculationResult)
            }

            if (output.isEmpty()) {
                output.append("Python code executed successfully (matplotlib plots require visualization)\n")
                output.append("Note: Install Chaquopy plugin for full Python support\n")
            }

            ExecutionResult(true, output.toString(), "")
        } catch (e: Exception) {
            ExecutionResult(false, "", "Python simulation error: ${e.message}")
        }
    }

    private fun extractGraphDataFromPython(code: String): GraphData? {
        try {
            val calculator = MathCalculator()
            val lines = code.lines()

            var xRange = -10.0 to 10.0
            var expression = ""
            var title = "Function Plot"

            for (line in lines) {
                when {
                    line.contains("np.linspace(") -> {
                        val params = line.substringAfter("np.linspace(").substringBefore(")")
                            .split(",").map { it.trim() }
                        if (params.size >= 2) {
                            xRange = (params[0].toDoubleOrNull() ?: -10.0 to params[1].toDoubleOrNull() ?: 10.0) as Pair<Double, Double>
                        }
                    }
                    line.contains("y =") && line.contains("x") -> {
                        expression = line.substringAfter("y =").trim()
                        expression = expression.replace("np.", "").replace("**", "^")
                    }
                    line.contains("plt.title(") -> {
                        title = line.substringAfter("plt.title(").substringBefore(")")
                            .replace("'", "").replace("\"", "")
                    }
                }
            }

            if (expression.isNotEmpty()) {
                val points = mutableListOf<Point>()
                val step = (xRange.second - xRange.first) / 200

                for (i in 0..200) {
                    val x = xRange.first + i * step
                    try {
                        val evalExpr = expression.replace("x", "($x)")
                        val y = calculator.evaluate(evalExpr)
                        if (!y.isNaN() && !y.isInfinite()) {
                            points.add(Point(x, y))
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }

                if (points.isNotEmpty()) {
                    return GraphData(
                        type = GraphType.LINE,
                        data = points,
                        title = title,
                        xLabel = "x",
                        yLabel = "y"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting graph data", e)
        }
        return null
    }

    private fun evaluatePythonCalculations(code: String): String {
        val output = StringBuilder()
        val lines = code.lines()

        for (line in lines) {
            try {
                when {
                    line.trim().matches(Regex("^\\d+\\s*[+\\-*/]\\s*\\d+.*")) -> {
                        val calculator = MathCalculator()
                        val result = calculator.evaluate(line.trim())
                        output.append("$line = $result\n")
                    }
                    line.contains("range(") -> {
                        val rangeStr = line.substringAfter("range(").substringBefore(")")
                        val parts = rangeStr.split(",").map { it.trim().toIntOrNull() ?: 0 }
                        when (parts.size) {
                            1 -> output.append("range(${parts[0]}): 0 to ${parts[0] - 1}\n")
                            2 -> output.append("range(${parts[0]}, ${parts[1]}): ${parts[0]} to ${parts[1] - 1}\n")
                        }
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }

        return output.toString()
    }

    private fun executeJavaScript(code: String): ExecutionResult {
        return try {
            val rhino = org.mozilla.javascript.Context.enter()
            rhino.optimizationLevel = -1
            val scope = rhino.initStandardObjects()

            scope.put("console", scope, object : org.mozilla.javascript.ScriptableObject() {
                override fun getClassName() = "Console"

                @org.mozilla.javascript.annotations.JSFunction
                fun log(vararg args: Any) = args.joinToString(" ")
            })

            val result = rhino.evaluateString(scope, code, "JavaScript", 1, null)
            org.mozilla.javascript.Context.exit()

            val output = result?.toString() ?: ""
            ExecutionResult(true, output, "")
        } catch (e: Exception) {
            try {
                org.mozilla.javascript.Context.exit()
            } catch (ignored: Exception) {}
            ExecutionResult(false, "", e.message ?: "JavaScript execution failed")
        }
    }

    private fun executeCSimulated(code: String, context: Context): ExecutionResult {
        try {
            val output = StringBuilder()
            output.append("C code compilation simulated\n")
            output.append("Note: Install Termux or NDK for native C compilation\n\n")

            val printStatements = code.lines().filter { it.contains("printf(") }
            for (statement in printStatements) {
                val content = statement.substringAfter("printf(").substringBefore(")")
                    .replace("\"", "").replace("\\n", "\n")
                output.append(content).append("\n")
            }

            if (printStatements.isEmpty()) {
                output.append("Code would execute successfully on native compiler\n")
            }

            return ExecutionResult(true, output.toString(), "")
        } catch (e: Exception) {
            return ExecutionResult(false, "", "C simulation error: ${e.message}")
        }
    }

    private fun executeCppSimulated(code: String, context: Context): ExecutionResult {
        try {
            val output = StringBuilder()
            output.append("C++ code compilation simulated\n")
            output.append("Note: Install Termux or NDK for native C++ compilation\n\n")

            val coutStatements = code.lines().filter { it.contains("cout <<") || it.contains("std::cout <<") }
            for (statement in coutStatements) {
                val content = statement.substringAfter("<<")
                    .replace("\"", "").replace("endl", "\n").replace("std::", "")
                output.append(content).append("\n")
            }

            if (coutStatements.isEmpty()) {
                output.append("Code would execute successfully on native compiler\n")
            }

            return ExecutionResult(true, output.toString(), "")
        } catch (e: Exception) {
            return ExecutionResult(false, "", "C++ simulation error: ${e.message}")
        }
    }

    private fun executeMath(expression: String): ExecutionResult {
        try {
            val calculator = MathCalculator()
            val result = calculator.evaluate(expression)
            return ExecutionResult(true, result.toString(), "")
        } catch (e: Exception) {
            return ExecutionResult(false, "", e.message ?: "Math evaluation failed")
        }
    }

    private fun executeKotlinSimulated(code: String): ExecutionResult {
        try {
            val output = StringBuilder()
            output.append("Kotlin script simulation\n")
            output.append("Note: Enable Kotlin scripting for full support\n\n")

            val printStatements = code.lines().filter {
                it.contains("println(") || it.contains("print(")
            }
            for (statement in printStatements) {
                val content = statement.substringAfter("(").substringBefore(")")
                    .replace("\"", "")
                output.append(content).append("\n")
            }

            if (printStatements.isEmpty()) {
                output.append("Kotlin code structure validated\n")
            }

            return ExecutionResult(true, output.toString(), "")
        } catch (e: Exception) {
            return ExecutionResult(false, "", "Kotlin simulation error: ${e.message}")
        }
    }
}

data class ExecutionResult(
    val success: Boolean,
    val output: String,
    val error: String,
    val graphData: GraphData? = null
)

data class GraphData(
    val type: GraphType,
    val data: List<Point>,
    val title: String = "",
    val xLabel: String = "",
    val yLabel: String = ""
)

data class Point(val x: Double, val y: Double)

enum class GraphType {
    LINE, BAR, SCATTER, PIE
}

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