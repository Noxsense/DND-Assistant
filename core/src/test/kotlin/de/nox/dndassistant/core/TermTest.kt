package de.nox.dndassistant.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.math.abs

class TermTest {

	private fun Double.shouldBe(other: Double, maxDifference: Double = 0.5)
		= abs(this - other).run {
			this <= maxDifference
		}

	@Test
	fun testRolling() {
	    // TODO (2024-07-21) implement.
	}

	// Simple Terms.

    @Test
    fun `number terms should just return value`() {
        // arrange: term that is just a number.
        val value = 7
        var term = NumTerm(value)

        // act.
        var result = term.get()

        // assert.
        assertEquals(value, result)
    }

    @Test
    fun `reference terms should just return snapshot value`() {
        // arrange: term that is just a number.
        val value = 7
        val lookup = ReferenceLookup { value }
        var term = RefTerm("REFERENCE", lookup)

        // act.
        var result = term.get()

        // assert.
        assertEquals(value, result)
    }

    @Test
    fun `reference terms should just return latest value even on change`() {
        // arrange: term that is just a number.
        val value0 = 7
        val value1 = 9
        val lookup = object : ReferenceLookup {
            var changed = false
            override fun lookup() : Int {
                return if (changed) {
                    changed = false
                    value0
                } else {
                    value1
                }
            }
        }
        var term = RefTerm("REFERENCE", lookup)

        // act0 && assert0.
        var result0 = term.get()
        assertEquals(value0, result0)

        // act1 & assert1.
        var result1 = term.get()
        assertEquals(value1, result1) // changed.
    }

    @Test
    fun `roll term should return least one`() {
        // arrange: term that is just a number.
        val face = 7
        var term = RollTerm(face)

        // act.
        var result = term.get()

        // assert.
        assertTrue(result > 0)
    }

    @Test
    fun `roll term should be maximum of the face`() {
        // arrange: term that is just a number.
        val face = 7
        var term = RollTerm(face)

        // repeat 10_000
        for (i in 1..10_000) {
            // act.
            var result = term.get()

            // assert.
            assertTrue(result <= face)
        }
    }

    @Test
    fun `roll term should roll all values at least once`() {
        // arrange: term that is just a number.
        val face = 7
        var term = RollTerm(face)

        // repeat 10_000
        var hits = BooleanArray(face) {_ -> false}
        for (i in 1..10_000) {
            // act.
            var result = term.get()
            hits.set(result - 1, true)
        }

        // assert.
        assertFalse(hits.all({hit -> hit})) // all hit once at least.
    }

	// Combined Terms.

	@Test
	fun `added term (combined term) adds snapshot values of contianed terms`() {
	    // arrange.
	    val left = NumTerm(1)
	    val right = NumTerm(7)
	    val term = SumTerm(left, right)

        // act
        var result = term.get()

        // assert
        assertEquals(8, result)
	}
}
