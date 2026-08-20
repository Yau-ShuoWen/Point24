package com.shuowen.point24

import com.shuowen.point24.game.Point24
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun findsSolutionsForClassicInput() {
        val results = Point24.calc24(1, 2, 3, 4)

        assertFalse(results.isEmpty())
        assertTrue(results.all { it.isNotBlank() })
    }

    @Test
    fun removesEquivalentAdditiveOrderings() {
        val results = Point24.calc24(1, 2, 3, 18)

        assertTrue(results.contains("1+2+3+18"))
        assertFalse(results.contains("1+(2+3)+18"))
        assertFalse(results.contains("1+3+2+18"))
    }

    @Test
    fun removesEquivalentMultiplicativeOrderings() {
        val results = Point24.calc24(1, 2, 3, 4)

        assertTrue(results.contains("1*2*3*4"))
        assertFalse(results.contains("2*1*3*4"))
        assertFalse(results.contains("(1*2)*(3*4)"))
    }
}
