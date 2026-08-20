package com.shuowen.point24.game

/**
 * 24 点计算器
 *
 * 使用表达式树进行搜索，并在搜索阶段完成标准化去重：
 * 1. 加法/乘法节点拍平，消除仅由括号位置造成的重复。
 * 2. 加法/乘法子项排序，消除交换律导致的重复。
 * 3. 状态键基于标准表达式，避免相同中间状态被重复搜索。
 */
object Point24 {

    fun calc24(vararg numbers: Int): List<String> {
        val expressions = numbers.map { it.toString() }.toTypedArray()
        return calc24(*expressions)
    }

    fun calc24(vararg expressions: String): List<String> {
        val initialNodes = expressions.map { expression ->
            val token = expression.trim()
            ExprNode(Expr.Num(token), parseRational(token))
        }

        val result = linkedMapOf<String, String>()
        val visited = hashSetOf<String>()
        solve(initialNodes, result, visited)
        return result.values.toList()
    }

    private fun solve(
        nodes: List<ExprNode>,
        result: MutableMap<String, String>,
        visited: MutableSet<String>
    ) {
        val stateKey = nodes.map { it.expr.canonicalKey() }.sorted().joinToString("|")
        if (!visited.add(stateKey)) {
            return
        }

        if (nodes.size == 1) {
            val node = nodes.first()
            if (node.value == TARGET) {
                result.putIfAbsent(node.expr.canonicalKey(), node.expr.render())
            }
            return
        }

        for (i in 0 until nodes.lastIndex) {
            for (j in i + 1 until nodes.size) {
                val rest = buildList {
                    for (index in nodes.indices) {
                        if (index != i && index != j) {
                            add(nodes[index])
                        }
                    }
                }

                for (candidate in combine(nodes[i], nodes[j])) {
                    solve(rest + candidate, result, visited)
                }
            }
        }
    }

    private fun combine(left: ExprNode, right: ExprNode): List<ExprNode> {
        val results = ArrayList<ExprNode>(6)

        results += ExprNode(Expr.add(left.expr, right.expr), left.value + right.value)
        results += ExprNode(Expr.multiply(left.expr, right.expr), left.value * right.value)
        results += ExprNode(Expr.subtract(left.expr, right.expr), left.value - right.value)
        results += ExprNode(Expr.subtract(right.expr, left.expr), right.value - left.value)

        if (!right.value.isZero()) {
            results += ExprNode(Expr.divide(left.expr, right.expr), left.value / right.value)
        }
        if (!left.value.isZero()) {
            results += ExprNode(Expr.divide(right.expr, left.expr), right.value / left.value)
        }

        return results.distinctBy { it.expr.canonicalKey() }
    }

    private fun parseRational(text: String): Rational {
        require(text.isNotEmpty()) { "Empty expression" }
        return Rational(text.toLong(), 1)
    }

    private data class ExprNode(
        val expr: Expr,
        val value: Rational
    )

    private sealed interface Expr {
        fun canonicalKey(): String
        fun render(parentPrecedence: Int = 0): String
        fun precedence(): Int

        data class Num(val token: String) : Expr {
            override fun canonicalKey(): String = token

            override fun render(parentPrecedence: Int): String = token

            override fun precedence(): Int = 4
        }

        data class Sum(
            private val positiveTerms: List<Expr>,
            private val negativeTerms: List<Expr>
        ) : Expr {
            override fun canonicalKey(): String =
                "S(${positiveTerms.joinToString(",") { it.canonicalKey() }}|${negativeTerms.joinToString(",") { it.canonicalKey() }})"

            override fun render(parentPrecedence: Int): String {
                val text = buildString {
                    if (positiveTerms.isEmpty()) {
                        append("0")
                    } else {
                        append(positiveTerms.first().render(precedence()))
                        for (term in positiveTerms.drop(1)) {
                            append("+")
                            append(term.render(precedence()))
                        }
                    }

                    for (term in negativeTerms) {
                        append("-")
                        append(term.render(precedence() + 1))
                    }
                }
                return parenthesizeIfNeeded(text, precedence(), parentPrecedence)
            }

            override fun precedence(): Int = 1

            fun positiveTerms(): List<Expr> = positiveTerms

            fun negativeTerms(): List<Expr> = negativeTerms
        }

        data class Product(
            private val numerators: List<Expr>,
            private val denominators: List<Expr>
        ) : Expr {
            override fun canonicalKey(): String =
                "P(${numerators.joinToString(",") { it.canonicalKey() }}|${denominators.joinToString(",") { it.canonicalKey() }})"

            override fun render(parentPrecedence: Int): String {
                val numeratorText = renderProductChain(numerators, wrapComplexFactor = false)
                val denominatorText = renderProductChain(denominators, wrapComplexFactor = true)
                val text = if (denominators.isEmpty()) {
                    numeratorText
                } else {
                    "$numeratorText/$denominatorText"
                }
                return parenthesizeIfNeeded(text, precedence(), parentPrecedence)
            }

            override fun precedence(): Int = 2

            fun numerators(): List<Expr> = numerators

            fun denominators(): List<Expr> = denominators
        }

        companion object {
            fun add(left: Expr, right: Expr): Expr {
                val positiveTerms = mutableListOf<Expr>()
                val negativeTerms = mutableListOf<Expr>()
                collectSignedTerms(left, positiveTerms, negativeTerms, isPositive = true)
                collectSignedTerms(right, positiveTerms, negativeTerms, isPositive = true)
                return buildSum(positiveTerms, negativeTerms)
            }

            fun multiply(left: Expr, right: Expr): Expr {
                val numeratorFactors = mutableListOf<Expr>()
                val denominatorFactors = mutableListOf<Expr>()

                collectProductParts(left, numeratorFactors, denominatorFactors)
                collectProductParts(right, numeratorFactors, denominatorFactors)

                return buildProduct(numeratorFactors, denominatorFactors)
            }

            fun divide(left: Expr, right: Expr): Expr {
                rewriteDistributedDivision(left, right)?.let { return it }

                val numeratorFactors = mutableListOf<Expr>()
                val denominatorFactors = mutableListOf<Expr>()

                collectProductParts(left, numeratorFactors, denominatorFactors)

                if (right is Product) {
                    numeratorFactors += right.denominators()
                    denominatorFactors += right.numerators()
                } else {
                    denominatorFactors += right
                }

                return buildProduct(numeratorFactors, denominatorFactors)
            }

            fun subtract(left: Expr, right: Expr): Expr {
                val positiveTerms = mutableListOf<Expr>()
                val negativeTerms = mutableListOf<Expr>()
                collectSignedTerms(left, positiveTerms, negativeTerms, isPositive = true)
                collectSignedTerms(right, positiveTerms, negativeTerms, isPositive = false)
                return buildSum(positiveTerms, negativeTerms)
            }

            private fun collectAddTerms(expr: Expr, terms: MutableList<Expr>) {
                if (expr is Sum && expr.negativeTerms().isEmpty()) {
                    terms += expr.positiveTerms()
                } else {
                    terms += expr
                }
            }

            private fun collectSignedTerms(
                expr: Expr,
                positiveTerms: MutableList<Expr>,
                negativeTerms: MutableList<Expr>,
                isPositive: Boolean
            ) {
                if (expr is Sum) {
                    if (isPositive) {
                        positiveTerms += expr.positiveTerms()
                        negativeTerms += expr.negativeTerms()
                    } else {
                        positiveTerms += expr.negativeTerms()
                        negativeTerms += expr.positiveTerms()
                    }
                    return
                }

                if (isPositive) {
                    positiveTerms += expr
                } else {
                    negativeTerms += expr
                }
            }

            private fun collectProductParts(
                expr: Expr,
                numerators: MutableList<Expr>,
                denominators: MutableList<Expr>
            ) {
                if (expr is Product) {
                    numerators += expr.numerators()
                    denominators += expr.denominators()
                } else {
                    numerators += expr
                }
            }

            private fun rewriteDistributedDivision(left: Expr, right: Expr): Expr? {
                if (left !is Product || left.denominators().isNotEmpty()) {
                    return null
                }

                val sumFactorIndex = left.numerators().indexOfFirst {
                    it is Sum && it.negativeTerms().isEmpty() && it.positiveTerms().size >= 2
                }
                if (sumFactorIndex < 0) {
                    return null
                }

                val cancelFactorIndex = left.numerators().indexOfFirst {
                    it.canonicalKey() == right.canonicalKey()
                }
                if (cancelFactorIndex < 0 || cancelFactorIndex == sumFactorIndex) {
                    return null
                }

                val sumFactor = left.numerators()[sumFactorIndex] as Sum
                val cancelFactor = left.numerators()[cancelFactorIndex]
                val remainingFactors = left.numerators().filterIndexed { index, _ ->
                    index != sumFactorIndex && index != cancelFactorIndex
                }
                val sortedTerms = sumFactor.positiveTerms().sortedBy { it.canonicalKey() }
                val positiveTerms = mutableListOf<Expr>()

                val firstTermFactors = remainingFactors + sortedTerms.first()
                positiveTerms += buildProduct(firstTermFactors, emptyList())

                for (term in sortedTerms.drop(1)) {
                    val termFactors = remainingFactors + cancelFactor + term
                    positiveTerms += buildProduct(termFactors, listOf(right))
                }

                return buildSum(positiveTerms, emptyList())
            }

            private fun buildSum(
                positiveTerms: List<Expr>,
                negativeTerms: List<Expr>
            ): Expr {
                val normalizedPositive = positiveTerms.sortedBy { it.canonicalKey() }
                val normalizedNegative = negativeTerms.sortedBy { it.canonicalKey() }

                if (normalizedNegative.isEmpty() && normalizedPositive.size == 1) {
                    return normalizedPositive.first()
                }

                return Sum(normalizedPositive, normalizedNegative)
            }

            private fun buildProduct(
                numeratorFactors: List<Expr>,
                denominatorFactors: List<Expr>
            ): Expr {
                val denominatorOnes = denominatorFactors.filter { it is Num && it.token == "1" }
                val normalizedNumerators = (numeratorFactors + denominatorOnes)
                    .sortedBy { it.canonicalKey() }
                val normalizedDenominators = denominatorFactors
                    .filterNot { it is Num && it.token == "1" }
                    .sortedBy { it.canonicalKey() }

                if (normalizedDenominators.isEmpty() && normalizedNumerators.size == 1) {
                    return normalizedNumerators.first()
                }

                return Product(normalizedNumerators, normalizedDenominators)
            }
        }
    }

    private data class Rational(
        val numerator: Long,
        val denominator: Long
    ) {
        init {
            require(denominator != 0L) { "Division by zero" }
        }

        operator fun plus(other: Rational): Rational =
            Rational(
                numerator * other.denominator + other.numerator * denominator,
                denominator * other.denominator
            ).normalize()

        operator fun minus(other: Rational): Rational =
            Rational(
                numerator * other.denominator - other.numerator * denominator,
                denominator * other.denominator
            ).normalize()

        operator fun times(other: Rational): Rational =
            Rational(
                numerator * other.numerator,
                denominator * other.denominator
            ).normalize()

        operator fun div(other: Rational): Rational =
            Rational(
                numerator * other.denominator,
                denominator * other.numerator
            ).normalize()

        fun isZero(): Boolean = numerator == 0L

        private fun normalize(): Rational {
            if (numerator == 0L) {
                return Rational(0, 1)
            }

            val sign = if (denominator < 0) -1L else 1L
            val gcd = gcd(kotlin.math.abs(numerator), kotlin.math.abs(denominator))
            return Rational(
                sign * numerator / gcd,
                sign * denominator / gcd
            )
        }
    }

    private fun parenthesizeIfNeeded(text: String, precedence: Int, parentPrecedence: Int): String {
        return if (precedence < parentPrecedence) "($text)" else text
    }

    private fun renderProductChain(factors: List<Expr>, wrapComplexFactor: Boolean): String {
        return factors.joinToString("*") { factor ->
            val rendered = factor.render(2)
            if (wrapComplexFactor && factor.precedence() <= 2) {
                "($rendered)"
            } else {
                rendered
            }
        }
    }

    private fun gcd(a: Long, b: Long): Long {
        var x = a
        var y = b
        while (y != 0L) {
            val remainder = x % y
            x = y
            y = remainder
        }
        return if (x == 0L) 1L else x
    }

    private val TARGET = Rational(24, 1)
}
