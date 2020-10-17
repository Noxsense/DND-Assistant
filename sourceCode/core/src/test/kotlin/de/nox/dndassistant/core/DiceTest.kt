package de.nox.dndassistant.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.math.abs

class DiceTest {

	private val log = LoggerFactory.getLogger("DiceTest")

	private val repeatRolls = 10000

	private fun Double.shouldBe(other: Double)
		= abs(this - other).run {
			log.info("Difference: $this")
			this < 0.5
		}

	@Test
	fun testRoll() {
		var dice: SimpleDice
		var expected: IntRange
		var rolled: List<Int>
		var avg: Double

		log.info("\n>>> Test: testRoll()")

		// d20

		dice = D20 // d20
		expected = (1..20)
		rolled = (1..repeatRolls).map { dice.roll() }.sorted()
		avg = rolled.sum() / repeatRolls.toDouble()
		log.info("Roll: $dice: (${expected.average()}) $expected (Expected)")
		log.info("Roll: $dice: ($avg) $rolled")

		// test if every expected number is thrown.
		rolled.forEach { assertTrue(it in expected, "Dice ($dice): $it in $expected") }

		// test if expected average is close to the thrown results.
		assertEquals(expected.average(), dice.average)
		assertTrue(dice.average.shouldBe(avg))

		// 9d5
		// 9 or 45 with possibility of 0.000001 => check if roll is in range instead.

		dice = SimpleDice(5, 9) // 9d5 => (all 1: 9) upto (all 5: 45)
		expected = (9 .. (5*9))
		rolled = (1..repeatRolls).map { dice.roll() }.sorted()
		avg = rolled.sum() / repeatRolls.toDouble()
		log.info("Roll: $dice: (${expected.average()}) $expected (Expected)")
		log.info("Roll: $dice: ($avg) $rolled")

		// test if every expected number is thrown.
		rolled.forEach { assertTrue(it in expected, "Dice ($dice): $it in $expected") }

		// test if expected average is close to the thrown results.
		assertEquals(expected.average(), dice.average)
		assertTrue(dice.average.shouldBe(avg))

		// -2d3

		dice = SimpleDice(3, -2) // -2d3 = -d3 -d3 = {-3..-1}x2
		expected = (-6) .. (-2)
		rolled = (1..repeatRolls).map { dice.roll() }.sorted()
		avg = rolled.sum() / repeatRolls.toDouble()
		log.info("Roll: $dice: (${expected.average()}) $expected (Expected)")
		log.info("Roll: $dice: ($avg) $rolled")

		// test if every expected value occurred.
		rolled.forEach { assertTrue(it in expected, "Dice ($dice): $it in $expected") }

		// test if expected average is close to the thrown results.
		assertEquals(expected.average(), dice.average)
		assertTrue(dice.average.shouldBe(avg))
	}

	@Test
	fun testRollList() {
		var dice: SimpleDice
		var expectedNum: Int
		var expectedRange: IntRange
		var expectedSumRange: IntRange
		var rolls: List<Int>
		// var sum: Int

		log.info("\n>>> Test: testRollList()")

		// d20

		dice = SimpleDice(20, 1)
		expectedNum = 1
		expectedRange = (1..20)
		expectedSumRange = 1 .. 20

		for (i in (1 .. repeatRolls)) {
			rolls = dice.rollList()
			// sum = rolls.sum()
			// log.info("Rolled $dice: ($sum) $rolls")

			assertEquals(expectedNum, rolls.size, "Expected Number of rolls.")
			assertTrue(rolls.all { it in expectedRange }, "All rolls in expected range.")
			assertTrue(rolls.sum() in expectedSumRange, "All rolls in expected range.")
		}

		// 5d9

		dice = SimpleDice(5, 9)
		expectedNum = 9
		expectedRange = (1..5)
		expectedSumRange = (9) .. (45)

		for (i in (1..repeatRolls)) {
			rolls = dice.rollList()
			// sum = rolls.sum()
			// log.info("Rolled $dice: ($sum) $rolls")

			assertEquals(expectedNum, rolls.size, "Expected Number of rolls.")
			assertTrue(rolls.all { it in expectedRange }, "All rolls in expected range.")
			assertTrue(rolls.sum() in expectedSumRange, "All rolls in expected range.")
		}

		// -2d3

		dice = SimpleDice(3, -2)
		expectedNum = 2
		expectedRange = (-3) .. (-1)
		expectedSumRange = (-6) .. (-2)

		for (i in (1 .. repeatRolls)) {
			rolls = dice.rollList()
			// sum = rolls.sum()
			// log.info("Rolled $dice: ($sum) $rolls")

			assertEquals(expectedNum, rolls.size, "Expected Number of rolls.")
			assertTrue(rolls.all { it in expectedRange }, "All rolls in expected range.")
			assertTrue(rolls.sum() in expectedSumRange, "All rolls in expected range.")
		}

		log.info(">>> Do not unfold Bonus aka. SimpleDice(1, bonus) == '+bonus'")

		dice = SimpleDice(0, +3)
		expectedNum = 1
		expectedRange = (3) .. (3)
		expectedSumRange = expectedRange

		for (i in (1 .. repeatRolls)) {
			rolls = dice.rollList()
			// sum = rolls.sum()
			// log.info("Rolled $dice: ($sum) $rolls")

			assertEquals(expectedNum, rolls.size, "Expected Number of rolls.")
			assertTrue(rolls.all { it in expectedRange }, "All rolls in expected range.")
			assertTrue(rolls.sum() in expectedSumRange, "All rolls in expected range.")
		}
	}

	@Test
	fun testToString() {
		log.info("\n>>> Test testToString()")

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
		log.info("\n>>> Test testAverage()")

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
	fun testCustomDice() {
		log.info("\n>>> Test testCustomDice()")

		val dice = SimpleDice(-3, 1)
		val rolled = (1..repeatRolls).map { dice.roll() }
		log.info("Roll: $dice: $rolled")
		for (i in 1..3) {
			assertTrue((-i) in rolled, "Thrown ${-i} with $dice")
		}
	}

	@Test
	fun testRollBonus() {
		log.info("\n>>> Test testRollBonus()")

		/// only fixied values.
		val dice = SimpleDice(1, -3)
		val rolled = (1..repeatRolls).map { dice.roll() }
		log.info("Roll: $dice: $rolled")
		assertTrue(rolled.all { it == (-3) }, "Thrown only (-3) with $dice")
	}

	@Test
	fun testTermInitiators() {
		log.info("\n>>> Test testTermInitiators()")

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
		log.info("\n>>> Test testSimplifyTerm()")

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

		log.info("Expected: $expected")
		log.info("Input:    $dice")
		log.info("Simple:   $simplified")

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
		log.info("\n>>> Test testDiceParsing()")

		var string : String // the string to parse
		var dice: DiceTerm // the parsed dice term.
		var diceDice: DiceTerm // the parsed diceterm string to dice term

		/* Easy test run. No hidden gems. */

		string = "3d8 + d12 - D21 + 3 + 3 - 3"
		dice = DiceTerm.parse(string)
		assertTrue(SimpleDice(8, 3) in dice, "3d8 in $dice")
		assertTrue(SimpleDice(12) in dice, "3d8 in $dice")
		assertTrue(SimpleDice(-21) in dice, "3d8 in $dice")
		assertTrue((3) in dice, "3d8 in $dice")

		diceDice = DiceTerm.parse(dice.toString())
		log.info("Parsed back: ($dice) \u21d2 ($diceDice)")
		assertEquals(dice, diceDice, "The dice to string to dice should be equal")

		/* Incorrect parsing will throw error. */

		try {
			// not a correct term with x instead of d
			DiceTerm.parse("x20 + -1")
			fail("Didn't failed on parsing invalid term")
		} catch (e: Exception) {}

		/* Parsing mathematically correct terms. */

		string = "-1" // typical having a bonus of (-2)
		dice = DiceTerm.parse(string)
		log.debug("Parsed [$string] => [$dice]")

		assertTrue((-1) in dice, "(-1) correctly parsed.")
		assertFalse(1 in dice, "No fixed one for empty terms or such..")

		diceDice = DiceTerm.parse(dice.toString())
		log.info("Parsed back: ($dice) \u21d2 ($diceDice)")
		assertEquals(dice, diceDice, "The dice to string to dice should be equal")

		dice = (dice + 1).contracted() // should be empty?

		log.debug("Removed main features (-1) => $dice")

		assertEquals(DiceTerm.EMPTY, dice, "The term is empty")

		string = "D20 + -2" // typical having a bonus of (-2)
		dice = DiceTerm.parse(string)
		log.debug("Parsed [$string] => [$dice]")

		assertTrue(D20 in dice, "D20 correctly parsed.")
		assertTrue((-2) in dice, "(-2) correctly parsed.")

		diceDice = DiceTerm.parse(dice.toString())
		log.info("Parsed back: ($dice) \u21d2 ($diceDice)")
		assertEquals(dice, diceDice, "The dice to string to dice should be equal")

		dice = ((dice - D20) + 2).contracted() // should be empty?

		log.debug("Removed main features (d20, -2) => $dice")

		assertEquals(DiceTerm.EMPTY, dice, "The term is empty")

		// TODO (2020-10-17) feature more complex terms.
		if (true) return

		/* Complexity Level 1. */

		string = "12*(D6 + 3)" // typical levelling. => 12d6 + 12*CON
		dice = DiceTerm.parse(string)
		assertTrue(D6 in dice, "(D6) in dice $dice.")
		assertTrue(3 in dice, "(3) in dice $dice.")

		diceDice = DiceTerm.parse(dice.toString())
		log.info("Parsed back: ($dice) \u21d2 ($diceDice)")
		assertEquals(dice, diceDice, "The dice to string to dice should be equal")

		/* Complexity Level 2. */
		string = "12*(D6 + 3)" // typical levelling. => 12d6 + 12*CON
		dice = DiceTerm.parse(string)
		assertTrue(D6 in dice, "(D6) in dice $dice.")
		assertTrue(3 in dice, "(3) in dice $dice.")

		diceDice = DiceTerm.parse(dice.toString())
		log.info("Parsed back: ($dice) \u21d2 ($diceDice)")
		assertEquals(dice, diceDice, "The dice to string to dice should be equal")
	}

	@Test
	fun testTake3of4() {
		log.info("\n>>> Test testTake3of4()")

		// TODO (2020-07-14)
		D6.rollTake(3, 4, true)
	}

}
