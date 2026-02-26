package com.sotech.chameleon.execution

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CodeParser @Inject constructor() {

    data class CodeBlock(
        val language: String,
        val code: String,
        val startIndex: Int,
        val endIndex: Int
    )

    private val codeBlockPattern = Regex("```(\\w+)?\\s*\\n([\\s\\S]*?)```", RegexOption.MULTILINE)
    private val mathPattern = Regex("\\$\\$([^$]+)\\$\\$|\\$([^$]+)\\$")

    private val languageKeywords = mapOf(
        "python" to listOf("def ", "import ", "from ", "class ", "if __name__", "print(", "for ", "while ", "try:", "except:", "with "),
        "javascript" to listOf("function ", "const ", "let ", "var ", "console.log(", "=>", "async ", "await ", "class ", "extends "),
        "java" to listOf("public class", "public static void", "System.out", "import java", "package ", "extends ", "implements "),
        "c" to listOf("#include", "int main(", "printf(", "scanf(", "void ", "struct ", "typedef "),
        "cpp" to listOf("#include", "using namespace", "std::", "cout <<", "cin >>", "class ", "template<", "vector<"),
        "kotlin" to listOf("fun ", "val ", "var ", "class ", "object ", "companion object", "data class", "sealed class"),
        "go" to listOf("package main", "func ", "import ", "var ", "type ", "struct {", "interface {", "go func"),
        "rust" to listOf("fn ", "let ", "mut ", "impl ", "struct ", "enum ", "trait ", "use "),
        "sql" to listOf("SELECT ", "FROM ", "WHERE ", "INSERT INTO", "UPDATE ", "DELETE FROM", "CREATE TABLE", "ALTER TABLE"),
        "html" to listOf("<html", "<div", "<body", "<head", "<script", "<style", "<!DOCTYPE"),
        "css" to listOf("{", "}", ":", ";", ".class", "#id", "@media", "display:", "color:", "font-"),
        "bash" to listOf("#!/bin/bash", "echo ", "cd ", "ls ", "grep ", "awk ", "sed ", "chmod "),
        "r" to listOf("<-", "library(", "data.frame(", "ggplot(", "function(", "c(", "summary("),
        "matlab" to listOf("function ", "end", "plot(", "disp(", "fprintf(", "for ", "while "),
        "swift" to listOf("func ", "let ", "var ", "class ", "struct ", "enum ", "protocol ", "import "),
        "php" to listOf("<?php", "echo ", "function ", "$", "->", "=>", "namespace ", "use "),
        "ruby" to listOf("def ", "end", "class ", "module ", "puts ", "require ", "attr_accessor"),
        "scala" to listOf("def ", "val ", "var ", "class ", "object ", "trait ", "extends ", "case class"),
        "math" to listOf("sin(", "cos(", "tan(", "log(", "sqrt(", "exp(", "∫", "∑", "∂", "π", "∞")
    )

    fun parseCodeBlocks(text: String): List<CodeBlock> {
        val blocks = mutableListOf<CodeBlock>()

        codeBlockPattern.findAll(text).forEach { match ->
            val language = match.groupValues[1].lowercase().ifEmpty { detectLanguage(match.groupValues[2]) }
            val code = match.groupValues[2].trim()
            blocks.add(CodeBlock(language, code, match.range.first, match.range.last))
        }

        mathPattern.findAll(text).forEach { match ->
            val math = match.groupValues[1].ifEmpty { match.groupValues[2] }.trim()
            blocks.add(CodeBlock("math", math, match.range.first, match.range.last))
        }

        return blocks
    }

    private fun detectLanguage(code: String): String {
        val codeLines = code.lines().map { it.trim() }
        val scores = mutableMapOf<String, Int>()

        languageKeywords.forEach { (lang, keywords) ->
            var score = 0
            keywords.forEach { keyword ->
                score += codeLines.count { it.contains(keyword, ignoreCase = true) }
            }
            scores[lang] = score
        }

        return scores.maxByOrNull { it.value }?.key ?: "unknown"
    }

    fun shouldExecuteCode(text: String): Boolean {
        val lowerText = text.lowercase()
        return lowerText.contains("execute") ||
                lowerText.contains("run") ||
                lowerText.contains("calculate") ||
                lowerText.contains("compute") ||
                lowerText.contains("evaluate") ||
                codeBlockPattern.containsMatchIn(text) ||
                mathPattern.containsMatchIn(text)
    }

    fun extractExecutionIntent(text: String): ExecutionIntent? {
        val lowerText = text.lowercase()

        when {
            lowerText.contains("plot") || lowerText.contains("graph") -> {
                return ExecutionIntent.GRAPH
            }
            lowerText.contains("table") || lowerText.contains("tabulate") -> {
                return ExecutionIntent.TABLE
            }
            lowerText.contains("calculate") || lowerText.contains("compute") -> {
                return ExecutionIntent.CALCULATE
            }
            lowerText.contains("execute") || lowerText.contains("run") -> {
                return ExecutionIntent.EXECUTE
            }
        }

        return null
    }

    enum class ExecutionIntent {
        EXECUTE, CALCULATE, GRAPH, TABLE
    }
}