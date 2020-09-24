package de.nox.dndassistant.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiceTest {

	private val logger = LoggerFactory.getLogger("DiceTest")

	@Test
	fun testToString() {
		var str = "Just null, 0d20"
		assertEquals("+0", SimpleDice(20, 0).toString(), str)
		assertEquals("+0", SimpleDice(0, 20).toString(), str)
		assertEquals(SimpleDice(20, 0), SimpleDice(0, 20), str)

		str = "default, 1d20 / d20"
		assertEquals("+1d20", SimpleDice(20).toString(), str)
		assertEquals("+1d20", SimpleDice(-20, -1).toString(), str)
		assertEquals("+1d20", SimpleDice(20, 1).toString(), str)
		assertEquals(SimpleDice(20, 1), SimpleDice(20))
		assertEquals(SimpleDice(20, 1), SimpleDice(-20, -1))
		assertEquals(SimpleDice(20), SimpleDice(-20, -1), str)

		str = "Minus, 1d(-3) => d(-3)"
		assertEquals("-1d3", SimpleDice(max = -3, times = 1).toString(), str)
		assertEquals("-1d3", SimpleDice(max = 3, times = -1).toString(), str)
		assertEquals(SimpleDice(-3), SimpleDice(-3, 1), str)
		assertEquals(SimpleDice(-3), SimpleDice(3, -1), str)

		str = "Bonus, (-3)d1 => (-3)"
		assertEquals("-3", SimpleDice(max = 1, times = -3).toString(), str)
		assertEquals("-3", SimpleDice(max = -1, times = 3).toString(), str)
		assertEquals("-3", Bonus(-3).toString(), str)
		assertEquals(SimpleDice(1, -3), SimpleDice(-1, 3), str)
		assertEquals(SimpleDice(1, -3), Bonus(-3), str)
	}

	@Test
	fun testAverage() {
		assertEquals(  1.0, SimpleDice(1).average)
		assertEquals(  1.5, SimpleDice(2).average)
		assertEquals(  3.5, SimpleDice(6).average)
		assertEquals( 10.5, SimpleDice(20).average)
		assertEquals( 50.5, SimpleDice(100).average)
		assertEquals( 13.5, SimpleDice(8, 3).average) // 3d8

		// constansts
		(0..100).forEach { assertEquals(it * 1.0, SimpleDice(1, it).average, "constant") }
	}

	@Test
	fun testRoll() {
		var dice = D20 // d20
		var rolled = (1..1000).map { dice.roll() }.sorted()
		println("Roll: $dice: $rolled")
		for (i in 1..20) {
			assertTrue(i in rolled, "Thrown $i with $dice")
		}

		dice = SimpleDice(5, 9) // 9d5 => (all 1: 9) upto (all 5: 45)
		rolled = (1..1000).map { dice.roll() }.sorted()
		println("Roll: $dice: $rolled")
		for (i in 9..(5*9)) {
			assertTrue(i in rolled, "Thrown $i with $dice")
		}
	}

	@Test
	fun testCustomDice() {
		val dice = SimpleDice(-3, 1)
		val rolled = (1..1000).map { dice.roll() }
		println("Roll: $dice: $rolled")
		for (i in 1..3) {
			assertTrue((-i) in rolled, "Thrown ${-i} with $dice")
		}
	}

	@Test
	fun testRollBonus() {
		/// only fixied values.
		val dice = SimpleDice(1, -3)
		val rolled = (1..1000).map { dice.roll() }
		println("Roll: $dice: $rolled")
		assertTrue(rolled.all { it == (-3) }, "Thrown only (-3) with $dice")
	}

	@Test
	fun testTermInitiators() {
		// 2d6
		var a = DiceTerm(SimpleDice(6, 2))
		var b = SimpleDice(6) + SimpleDice(6)
		var c = DiceTerm(6, 6)

		assertEquals(a, b, "'2d6': Init grouped and D6.plus(D6)")
		assertEquals(a, c, "'2d6': Init grouped with faces")

		// 2d6 + 1d20
		a = DiceTerm(SimpleDice(6, 2), SimpleDice(20))
		b = b + SimpleDice(20) // plus
		c = DiceTerm(6, 20, 6)

		assertEquals(a, b, "'2d6 + d20': Init with separated and plus")
		assertEquals(a, c, "'2d6 + d20': Init with separated, single faces")

		// 2d6 + 1d20
		a = DiceTerm(SimpleDice(6, 2), SimpleDice(20), Bonus(-2))
		b = b - Bonus(2) // minus
		c = DiceTerm(6, 20, -1, 6, -1)

		assertEquals(a, b, "'2d6 + d20 - 2': Init with separated, and minus")
		assertEquals(a, c, "'2d6 + d20 - 2': Init with separated, single faces")
	}

	@Test
	fun testSimplifyTerm() {
		val dice = DiceTerm(
			// + 3 + 3 - 3 + 3d8 - d8 + 5d12 + d12 + 2d21 - d21
			// (+ 3 + 3 - 3) (+ 3d8 - d8) (+ 5d12 + d12) (+ 2d21 - d21)
			// (+ 3) (+ 2d8) (+ 6d12) (+ 1d21)
			Bonus(+3),
			SimpleDice(8, 3),
			SimpleDice(12),
			SimpleDice(-21),
			Bonus(+3),
			SimpleDice(8, -1),
			SimpleDice(21, 2),
			Bonus(-3),
			SimpleDice(12, 5)
		)
		val expected = DiceTerm(
			// 2d21 + 6d12 + 2d8 + 3
			SimpleDice(21),
			SimpleDice(12, 6),
			SimpleDice(8, 2),
			SimpleDice(1, +3)
		)
		val simplified = dice.contracted()

		println("Expected: $expected")
		println("Input:    $dice")
		println("Simple:   $simplified")

		assertTrue(expected.same(simplified), "Simple: {$dice \u21D2 $simplified} vs {$expected}")

		assertEquals(expected, simplified, "simplified as expected")
		assertEquals(dice, simplified, "same rolls (simplified)")
		assertEquals(expected, dice, "same rolls (in the first place)")

		val splitInput = dice.split()

		val dice2 = DiceTerm(0) + Bonus(-2) // +0 -2
		val expect2 = DiceTerm(Bonus(-2)) // -2
		val simple2 = dice2.contracted()

		assertTrue(expect2.same(simple2), "Simple (v2): {$dice2 \u21D2 $simple2} vs {$expect2}")

		assertEquals(expect2, simple2, "simplified as expected")
		assertEquals(dice2, simple2, "same rolls (simplified)")
		assertEquals(expect2, dice2, "same rolls (in the first place)")

		// will roll the same.
		assertEquals(dice, splitInput, "will also roll the same.")
	}

	@Test
	fun testDiceParsing() {
		val string = "3d8 + d12 - D21 + 3 + 3 - 3"
		val dice = DiceTerm.parse(string)
		val rolled = (1..1000).map { dice.roll() }
		println("Roll: $dice: $rolled")
		assertTrue(SimpleDice(8, 3) in dice, "3d8 in $dice")
		assertTrue(SimpleDice(12) in dice, "3d8 in $dice")
		assertTrue(SimpleDice(-21) in dice, "3d8 in $dice")
		assertTrue(Bonus(3) in dice, "3d8 in $dice")

		// 1. parse, 2. to string, 3. parse
		val diceStr = dice.toString()
		val diceStrDice = DiceTerm.parse(diceStr)
		println("($string) \u21d2 ($diceStr)")
		println("($dice) \u21d2 ($diceStrDice)")
		// assertEquals(dice, diceStrDice, "String \u2192 Dice \u2192 String \u2192 Dice")
		// TODO (2020-07-14)
	}

	@Test
	fun testTake3of4() {
		// TODO (2020-07-14)
		D6.rollTake(3, 4, true)
	}

}
