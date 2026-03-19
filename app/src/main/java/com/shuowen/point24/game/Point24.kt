package com.shuowen.point24.game

/**
 * 24 点计算器 - Kotlin 版本
 */
object Point24 {

    fun calc24(vararg numbers: Int): List<String> {
        // ✅ 添加 as Array<String> 解决类型推断问题
        val expressions = numbers.map { it.toString() }.toTypedArray()
        return calc24(*expressions)
    }

    fun calc24(vararg expressions: String): List<String> {
        val result = linkedSetOf<String>()
        val hash = hashMapOf<String, Boolean>()
        val operator = arrayOf("+", "-", "*", "/")

        // ✅ 修复：复制数组解决类型协变问题
        solve(expressions.copyOf(), result, hash, operator)

        return result.toList()
    }

    private fun solve(
        expressions: Array<out String>,
        result: MutableSet<String>,
        hash: MutableMap<String, Boolean>,
        operator: Array<String>
    ) {
        val len = expressions.size
        val groupStr = expressions.sorted().toString()

        if (hash[groupStr] != true) {
            hash[groupStr] = true

            if (len > 1) {
                for (i in 0 until len - 1) {
                    for (j in i + 1 until len) {
                        val expList = expressions.toMutableList()
                        val exp1 = expList.removeAt(j)
                        val exp2 = expList.removeAt(i)

                        for (n in 0..3) {
                            val newExpList = expList.toMutableList()
                            val newExpr = buildExpression(exp1, exp2, operator[n], len)
                            newExpList.add(0, newExpr)
                            solve(newExpList.toTypedArray(), result, hash, operator)

                            // 非交换运算尝试反向
                            if (exp1 != exp2 && n % 2 == 1) {
                                val reverseExpr = buildExpression(exp2, exp1, operator[n], len)
                                newExpList[0] = reverseExpr
                                solve(newExpList.toTypedArray(), result, hash, operator)
                            }
                        }
                    }
                }
            } else if (len == 1) {
                try {
                    val value = evaluate(expressions[0])
                    if (kotlin.math.abs(value - 24) < 1e-6) {
                        result.add(expressions[0])
                    }
                } catch (_: Exception) {
                    // 跳过无效表达式
                }
            }
        }
    }

    private fun buildExpression(exp1: String, exp2: String, op: String, len: Int): String {
        return if (op in listOf("*", "/") || len == 2) {
            "$exp1$op$exp2"
        } else {
            "($exp1$op$exp2)"
        }
    }

    private fun evaluate(expression: String): Double {
        var expr = expression.replace("\\s+".toRegex(), "")

        // 处理括号
        while ("(" in expr) {
            val lastOpen = expr.lastIndexOf('(')
            val nextClose = expr.indexOf(')', lastOpen)
            val subExpr = expr.substring(lastOpen + 1, nextClose)
            val subValue = evaluate(subExpr)
            expr = expr.substring(0, lastOpen) + subValue + expr.substring(nextClose + 1)
        }

        // 解析数字和运算符
        val numbers = mutableListOf<Double>()
        val ops = mutableListOf<Char>()
        var i = 0

        // 解析第一个数字
        val firstNum = parseNumber(expr, 0)
        numbers.add(firstNum.value)
        i = firstNum.endIndex

        while (i < expr.length) {
            val op = expr[i++]
            val numResult = parseNumber(expr, i)
            val num = numResult.value
            i = numResult.endIndex

            when (op) {
                '*', '/' -> {
                    val last = numbers.removeAt(numbers.lastIndex)
                    numbers.add(if (op == '*') last * num else {
                        require(kotlin.math.abs(num) > 1e-10) { "Division by zero" }
                        last / num
                    })
                }
                '+', '-' -> {
                    numbers.add(num)
                    ops.add(op)
                }
                else -> throw IllegalArgumentException("Invalid operator: $op")
            }
        }

        // 处理加减
        var result = numbers[0]
        for (j in ops.indices) {
            result = if (ops[j] == '+') result + numbers[j + 1] else result - numbers[j + 1]
        }
        return result
    }

    private data class ParseResult(val value: Double, val endIndex: Int)

    private fun parseNumber(expr: String, start: Int): ParseResult {
        var i = start
        val numStr = StringBuilder()
        while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
            numStr.append(expr[i++])
        }
        require(numStr.isNotEmpty()) { "Invalid number at $start" }
        return ParseResult(numStr.toString().toDouble(), i)
    }
}