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
		var dice: DiceTerm
		var expected: IntRange
		var rolled: List<Int>
		var avg: Double

		log.info("\n\n>>> Test: testRoll()")

		// d20

		dice = D20 // d20
		assertEquals(dice.min, 1)
		assertEquals(dice.max, 20)
		expected = (dice.min..dice.max)
		rolled = (1..repeatRolls).map { dice.roll().sum() }.sorted()
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

		dice = DiceTerm(5) * (9) // 9d5 => (all 1: 9) upto (all 5: 45)
		assertEquals(dice.min, 9 )
		assertEquals(dice.max, (5*9))
		expected = (dice.min.. dice.max)
		rolled = (1..repeatRolls).map { dice.roll().sum() }.sorted()
		avg = rolled.sum() / repeatRolls.toDouble()
		log.info("Roll: $dice: (${expected.average()}) $expected (Expected)")
		log.info("Roll: $dice: ($avg) $rolled")

		// test if every expected number is thrown.
		rolled.forEach { assertTrue(it in expected, "Dice ($dice): $it in $expected") }

		// test if expected average is close to the thrown results.
		assertEquals(expected.average(), dice.average)
		assertTrue(dice.average.shouldBe(avg))

		// -2d3

		dice = DiceTerm(3) * (-2) // -2d3 = -d3 -d3 = {-3..-1}x2
		assertEquals(dice.min, -6) // -3, -3
		assertEquals(dice.max, -2) // -1, -1
		expected = dice.min .. dice.max
		rolled = (1..repeatRolls).map { dice.roll().sum() }.sorted()
		avg = rolled.sum() / repeatRolls.toDouble()
		log.info("Roll: $dice: (${expected.average()}) $expected (Expected)")
		log.info("Roll: $dice: ($avg) $rolled")

		// test if every expected value occurred.
		rolled.forEach { assertTrue(it in expected, "Dice ($dice): $it in $expected") }

		// test if expected average is close to the thrown results.
		assertEquals(expected.average(), dice.average)
		assertTrue(dice.average.shouldBe(avg))
		log.info("OK")
	}

	@Test
	fun testRollList() {
		var dice: DiceTerm
		var expectedNum: Int
		var expectedRange: IntRange
		var expectedSumRange: IntRange
		var rolls: List<Int>

		log.info("\n\n>>> Test: testRollList()")

		// d20

		log.info(">>>> Part: Roll d20")

		dice = DiceTerm(20)
		expectedNum = 1
		expectedRange = (1..20)
		expectedSumRange = 1 .. 20

		for (i in (1 .. repeatRolls)) {
			rolls = dice.roll()
			// log.info("Rolled $dice: (${rolls.sum()}) $rolls")

			assertEquals(expectedNum, rolls.size, "Expected Number of rolls.")
			assertTrue(rolls.all { it in expectedRange }, "All rolls in expected range.")
			assertTrue(rolls.sum() in expectedSumRange, "All rolls in expected range.")
		}

		// 5d9

		log.info(">>>> Part: Roll 5d9")

		dice = DiceTerm(5) * (9)
		expectedNum = 9
		expectedRange = (1..5)
		expectedSumRange = (9) .. (45)

		for (i in (1..repeatRolls)) {
			rolls = dice.roll()
			// log.info("Rolled $dice: (${rolls.sum()}) $rolls")

			assertEquals(expectedNum, rolls.size, "Expected Number of rolls.")
			assertTrue(rolls.all { it in expectedRange }, "All rolls in expected range.")
			assertTrue(rolls.sum() in expectedSumRange, "All rolls in expected range.")
		}

		// -2d3

		log.info(">>>> Part: Roll -2d3")

		dice = DiceTerm(3) * (-2)
		expectedNum = 2
		expectedRange = (-3) .. (-1)
		expectedSumRange = (-6) .. (-2)

		for (i in (1 .. repeatRolls)) {
			rolls = dice.roll()
			// log.info("Rolled $dice: (${rolls.sum()}) $rolls")

			assertEquals(expectedNum, rolls.size, "Expected Number of rolls.")
			assertTrue(rolls.all { it in expectedRange }, "All rolls in expected range.")
			assertTrue(rolls.sum() in expectedSumRange, "All rolls in expected range.")
		}

		log.info(">>> Do not unfold bonus aka. SimpleDice(1, bonus) == '+bonus'")

		dice = DiceTerm(0) * (+3)
		expectedNum = 1
		expectedRange = (0) .. (0)
		expectedSumRange = expectedRange

		log.info(">>>> Part: Roll -3d0 = $dice")

		for (i in (1 .. repeatRolls)) {
			rolls = dice.roll()
			// log.info("Rolled $dice: (${rolls.sum()}) $rolls")

			assertEquals(expectedNum, rolls.size, "Expected Number of rolls.")
			assertTrue(rolls.all { it in expectedRange }, "All rolls in expected range.")
			assertTrue(rolls.sum() in expectedSumRange, "All rolls in expected range.")
		}
		log.info("OK")
	}

	@Test
	fun testToString() {
		log.info("\n\n>>> Test testToString()")

		var str = "Just null, 0d20"
		assertEquals("+0", (DiceTerm(20) * 0).toString(), str + " (d20 * 0)")
		assertEquals("+0", (DiceTerm(0) * 20).toString(), str + " (d0 * 20)")
		assertEquals(DiceTerm(20) * (0), DiceTerm(0) * (20), str + " (0d20 == 20d0)")

		str = "default, 1d20 / d20"
		assertEquals("+1d20", DiceTerm(20).toString(), str)
		assertEquals("+1d20", (DiceTerm(-20) * (-1)).toString(), str)
		assertEquals("+1d20", DiceTerm(20).toString(), str)
		assertEquals(DiceTerm(20), DiceTerm(20))
		assertEquals(DiceTerm(20), DiceTerm(-20) * (-1))
		assertEquals(DiceTerm(20), DiceTerm(-20) * (-1), str)

		str = "Minus, 1d(-3) => d(-3)"
		assertEquals("-1d3", (DiceTerm(-3) * (1)).toString(), str)
		assertEquals("-1d3", (DiceTerm(3) * (-1)).toString(), str)
		assertEquals(DiceTerm(-3), DiceTerm(-3) * (1), str)
		assertEquals(DiceTerm(-3), DiceTerm(3) * (-1), str)

		str = "bonus, (-3)d1 => (-3)"
		assertEquals("-3", (DiceTerm(1) * (-3)).toString(), str)
		assertEquals("-3", (DiceTerm(-1) * (3)).toString(), str)
		assertEquals("-3", bonus(-3).toString(), str)
		assertEquals(DiceTerm(1) * (-3), DiceTerm(-1) * (3), str)
		assertEquals(DiceTerm(1) * (-3), bonus(-3), str)
		log.info("OK")
	}

	@Test
	fun testAverage() {
		log.info("\n\n>>> Test testAverage()")

		assertEquals(  1.0, DiceTerm(1).average)
		assertEquals(  1.5, DiceTerm(2).average)
		assertEquals(  3.5, DiceTerm(6).average)
		assertEquals( 10.5, DiceTerm(20).average)
		assertEquals( 50.5, DiceTerm(100).average)
		assertEquals( 13.5, (DiceTerm(8) * (3)).average) // 3d8

		// constansts
		(0..100).forEach { assertEquals(it * 1.0, d(1, it).average, "constant") }
		log.info("OK")
	}

	@Test
	fun testCustomDice() {
		log.info("\n\n>>> Test testCustomDice()")

		val dice = DiceTerm(-3) * (1)
		val rolled = (1..repeatRolls).map { dice.roll().sum() }
		log.info("Roll: $dice: $rolled")
		for (i in 1..3) {
			assertTrue((-i) in rolled, "Thrown ${-i} with $dice")
		}
		log.info("OK")
	}

	@Test
	fun testRollbonus() {
		log.info("\n\n>>> Test testRollbonus()")

		/// only fixied values.
		val dice = DiceTerm(1) * (-3)
		val rolled = (1..repeatRolls).map { dice.roll().sum() }
		log.info("Roll: $dice: $rolled")
		assertTrue(rolled.all { it == (-3) }, "Thrown only (-3) with $dice")
		log.info("OK")
	}

	@Test
	fun testTermInitiators() {
		log.info("\n\n>>> Test testTermInitiators()")

		// 2d6
		var a =(DiceTerm(6) * (2))
		var b = DiceTerm(6) + DiceTerm(6)
		var c = DiceTerm(6, 6)

		assertEquals(a, b, "'2d6': Init grouped and D6.plus(D6)")
		assertEquals(a, c, "'2d6': Init grouped with faces")

		// 2d6 + 1d20
		a =(DiceTerm(6) * (2) + DiceTerm(20))
		b = b + DiceTerm(20) // plus
		c = DiceTerm(6, 20, 6)

		assertEquals(a, b, "'2d6 + d20': Init with separated and plus")
		assertEquals(a, c, "'2d6 + d20': Init with separated, single faces")

		// 2d6 + 1d20
		a =(DiceTerm(6) * (2) + DiceTerm(20) + bonus(-2))
		b = b - bonus(2) // minus
		c = DiceTerm(6, 20, -1, 6, -1)

		assertEquals(a, b, "'2d6 + d20 - 2': Init with separated, and minus")
		assertEquals(a, c, "'2d6 + d20 - 2': Init with separated, single faces")

		log.info("OK.")
	}

	@Test
	fun testSimplifyTerm() {
		log.info("\n\n>>> Test testSimplifyTerm()")

		val dice = (
			// + 3 + 3 - 3 + 3d8 - d8 + 5d12 + d12 + 2d21 - d21
			// (+ 3 + 3 - 3) (+ 3d8 - d8) (+ 5d12 + d12) (+ 2d21 - d21)
			// (+ 3) (+ 2d8) (+ 6d12) (+ 1d21)
			bonus(+3)
			+ DiceTerm(8) * (3)
			+ DiceTerm(12)
			+ DiceTerm(-21)
			+ bonus(+3)
			+ DiceTerm(8) * (-1)
			+ DiceTerm(21) * (2)
			+ bonus(-3)
			+ DiceTerm(12) * (5)
		)

		val expected =(
			// 2d21 + 6d12 + 2d8 + 3
			DiceTerm(21)
			+ DiceTerm(12) * (6)
			+ DiceTerm(8) * (2)
			+ d(1, +3)
		)

		assertEquals(dice.cumulated(), expected)

		log.info("OK")
	}

	@Test
	fun testSubTerms() {
		// will fit
		assertTrue(DiceTerm(4) in DiceTerm(4), "1d4 in 1d4")
		assertTrue(DiceTerm(4) in DiceTerm(4, 4, 4, 4), "1d4 in 4d4")

		// missing face, not matching face
		assertFalse(DiceTerm(3) in DiceTerm(4), "1d3 not in 1d4")
		assertFalse(DiceTerm(4, 5) in DiceTerm(4, 4, 4, 4), "1d5 not in 4d4")

		// needs higher count than given
		assertFalse(DiceTerm(4, 4) in DiceTerm(4), "2d4 not in 1d4")

		// is looking for negative terms. // always simplyfied.
		assertFalse(DiceTerm(-4) in DiceTerm(4), " (0 - 1d4) not in (+1d4)")
		assertFalse(DiceTerm(4) in DiceTerm(-4), " (0 - 1d4) not in (+1d4)")

		assertFalse(DiceTerm(4, -4, -4) in DiceTerm(4, -4), " (0 - 1d4) not in (+0)")
	}

	@Test
	fun testDiceParsing() {
		log.info("\n\n>>> Test testDiceParsing()")

		var string : String // the string to parse
		var dice: DiceTerm // the parsed dice term.
		var diceDice: DiceTerm // the parsed diceterm string to dice term

		/* Easy test run. No hidden gems. */

		string = "3d8 + d12 - D21 + 3 + 3 - 3"
		dice = DiceTerm.parse(string)
		assertTrue(DiceTerm(8) * (3) in dice, "(3d8) in ($dice)")
		assertTrue(DiceTerm(12) in dice, "(1d12) in ($dice)")
		assertTrue(DiceTerm(-21) in dice, "(-1d21) in ($dice)")
		assertTrue(d(1, 3) in dice, "(+3) in ($dice)")

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

		string = "D20 + -2" // typical having a bonus of (-2)
		dice = DiceTerm.parse(string)
		log.debug("Parsed [$string] => [$dice]")

		assertTrue(D20 in dice, "D20 correctly parsed.")
		assertTrue(d(1, -2) in dice, "(-2) correctly parsed.")

		diceDice = DiceTerm.parse(dice.toString())
		log.info("Parsed back: ($dice) \u21d2 ($diceDice)")
		assertEquals(dice, diceDice, "The dice to string to dice should be equal")

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
		log.info("OK")
	}

	@Test
	fun testTake3of4() {
		log.info("\n\n>>> Test testTake3of4()")

		// TODO (2020-07-14)
		// D6.rollTake(3, 4, true)
		log.info("OK")
	}

}
