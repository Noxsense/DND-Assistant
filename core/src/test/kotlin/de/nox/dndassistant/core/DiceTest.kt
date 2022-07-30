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

		dice = DiceTerm.xDy(x = 9, y = 5) // 9d5 => (all 1: 9) upto (all 5: 45)
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

		dice = DiceTerm.xDy(x = -2, y = 3) // -2d3 = -d3 -d3 = {-3..-1}x2
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
		log.info("testRoll: OK")
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

		dice = DiceTerm.xDy(x = 1, y = 20)
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

		dice = DiceTerm.xDy(x = 9, y = 5)
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

		dice = DiceTerm.xDy(x = -2, y = 3)
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

		dice = DiceTerm.xDy(x = 1, y = 0) * (+3)
		expectedNum = 1
		expectedRange = (0) .. (0)
		expectedSumRange = expectedRange

		log.info(">>>> Part: Roll -3d0 = $dice")

		for (i in (1 .. repeatRolls)) {
			rolls = dice.roll()
			// log.info("Rolled $dice: (${rolls.sum()}) $rolls")

			assertEquals(expectedNum, rolls.size, "Expected Number of rolls.") // = 1
			assertTrue(rolls.all { it in expectedRange }, "All rolls in expected range.")
			assertTrue(rolls.sum() in expectedSumRange, "All rolls in expected range.")
		}
		log.info("testRollList: OK")
	}

	@Test
	fun testToString() {
		log.info("\n\n>>> Test testToString()")

		var str = "Just null, 0d20"
		assertEquals("+0", (DiceTerm.xDy(x = 1, y = 20) * 0).toString(), str + " (d20 * 0)")
		assertEquals("+0", (DiceTerm.xDy(x = 1, y = 0) * 20).toString(), str + " (d0 * 20)")
		assertEquals(DiceTerm.xDy(x = 0, y = 20), DiceTerm.xDy(x = 20, y = 0), str + " (0d20 == 20d0)")

		str = "default, 1d20 / d20"
		assertEquals("+1d20", DiceTerm.xDy(x = 1, y = 20).toString(), str)
		assertEquals("+1d20", (DiceTerm.xDy(x = -1, y = -20)).toString(), str)
		assertEquals("+1d20", DiceTerm.xDy(x = 1, y = 20).toString(), str)
		assertEquals(DiceTerm.xDy(x = 1, y = 20), DiceTerm.xDy(x = 1, y = 20))
		assertEquals(DiceTerm.xDy(x = 1, y = 20), DiceTerm.xDy(x = -1, y = -20))
		assertEquals(DiceTerm.xDy(x = 1, y = 20), DiceTerm.xDy(x = -1, y = -20), str)

		str = "Minus, 1d(-3) => d(-3)"
		assertEquals("-1d3", (DiceTerm.xDy(x = 1, y = -3)).toString(), str)
		assertEquals("-1d3", (DiceTerm.xDy(x = -1, y = 3)).toString(), str)
		assertEquals(DiceTerm.xDy(x = 1, y = -3), DiceTerm.xDy(x = 1, y = -3), str)
		assertEquals(DiceTerm.xDy(x = 1, y = -3), DiceTerm.xDy(x = -1, y = 3), str)

		str = "bonus, (-3) => (-3)"
		// assertEquals("-3", (DiceTerm.xDy(x = -3, y = 1)).toString(), str)
		// assertEquals("-3", (DiceTerm.xDy(x = 3, y = -1)).toString(), str)
		assertEquals("-3", DiceTerm.bonus(-3).toString(), str)
		assertEquals(DiceTerm.xDy(x = -3, y = 1), DiceTerm.xDy(x = 3, y = -1), str)
		log.info("testToString: OK")
	}

	@Test
	fun testAverage() {
		log.info("\n\n>>> Test testAverage()")

		assertEquals(  1.0, DiceTerm.xDy(x = 1, y = 1).average)
		assertEquals(  1.5, DiceTerm.xDy(x = 1, y = 2).average)
		assertEquals(  3.5, DiceTerm.xDy(x = 1, y = 6).average)
		assertEquals( 10.5, DiceTerm.xDy(x = 1, y = 20).average)
		assertEquals( 50.5, DiceTerm.xDy(x = 1, y = 100).average)
		assertEquals( 13.5, (DiceTerm.xDy(x = 3, y = 8)).average) // 3d8

		// constansts
		(0..100).forEach { assertEquals(it * 1.0, DiceTerm.xDy(x = it, y = 1).average, "constant") }
		log.info("testAverage: OK")
	}

	@Test
	fun testCustomDice() {
		log.info("\n\n>>> Test testCustomDice()")

		val dice = DiceTerm.xDy(x = 1, y = -3)
		val rolled = (1..repeatRolls).map { dice.roll().sum() }
		log.info("Roll: $dice: $rolled")
		for (i in 1..3) {
			assertTrue((-i) in rolled, "Thrown ${-i} with $dice")
		}
		log.info("testCustomDice: OK")
	}

	@Test
	fun testRollbonus() {
		log.info("\n\n>>> Test testRollbonus()")

		/// only fixied values.
		val dice = DiceTerm.xDy(x = -3, y = 1)
		val rolled = (1..repeatRolls).map { dice.roll().sum() }
		log.info("Roll: $dice: $rolled")
		assertTrue(rolled.all { it == (-3) }, "Thrown only (-3) with $dice")
		log.info("testRollbonus: OK")
	}

	@Test
	fun testTermInitiators() {
		log.info("\n\n>>> Test testTermInitiators()")

		// 2d6
		var a =(DiceTerm.xDy(x = 2, y = 6))
		var b = DiceTerm.xDy(x = 1, y = 6) + DiceTerm.xDy(x = 1, y = 6)
		var c = DiceTerm.fromDieFaces(6, 6)

		assertEquals(a, b, "'2d6': Init grouped and D6.plus(D6)")
		assertEquals(a, c, "'2d6': Init grouped with faces")

		// 2d6 + 1d20
		a =(DiceTerm.xDy(x = 2, y = 6) + DiceTerm.xDy(x = 1, y = 20))
		b = b + DiceTerm.xDy(x = 1, y = 20) // plus
		c = DiceTerm.fromDieFaces(6, 20, 6)

		assertEquals(a, b, "'2d6 + d20': Init with separated and plus")
		assertEquals(a, c, "'2d6 + d20': Init with separated, single faces")

		println("\n\n2d6 + 1d20")

		// +2d6 +1d20 -2
		a =(DiceTerm.xDy(x = 2, y = 6) + DiceTerm.xDy(x = 1, y = 20) + DiceTerm.bonus(-2))
		b = b - DiceTerm.bonus(2) // +2d6 -1d20 -2
		c = DiceTerm.fromDieFaces(6, 20, 6) - DiceTerm.bonus(1) - DiceTerm.bonus(1)
		// - 2d1 != -2 ... ?

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
			DiceTerm.bonus(+3)
			+ DiceTerm.xDy(x = 3, y = 8)
			+ DiceTerm.xDy(x = 1, y = 12)
			+ DiceTerm.xDy(x = 1, y = -21)
			+ DiceTerm.bonus(+3)
			+ DiceTerm.xDy(x = -1, y = 8)
			+ DiceTerm.xDy(x = 2, y = 21)
			+ DiceTerm.bonus(-3)
			+ DiceTerm.xDy(x = 5, y = 12)
		)

		val expected =(
			// 2d21 + 6d12 + 2d8 + 3
			DiceTerm.xDy(x = 1, y = 21)
			+ DiceTerm.xDy(x = 6, y = 12)
			+ DiceTerm.xDy(x = 2, y = 8)
			+ DiceTerm.bonus(+3)
		)

		assertEquals(dice.cumulated(), expected)

		log.info("testSimplifyTerm: OK")
	}

	@Test
	fun testSubTerms() {
		// will fit
		assertTrue(DiceTerm.xDy(x = 1, y = 4) in DiceTerm.xDy(x = 1, y = 4), "1d4 in 1d4")
		assertTrue(DiceTerm.xDy(x = 1, y = 4) in DiceTerm.fromDieFaces(4, 4, 4, 4), "1d4 in 4d4")

		// missing face, not matching face
		assertFalse(DiceTerm.xDy(x = 1, y = 3) in DiceTerm.xDy(x = 1, y = 4), "1d3 not in 1d4")
		assertFalse(DiceTerm.fromDieFaces(4, 5) in DiceTerm.fromDieFaces(4, 4, 4, 4), "1d5 not in 4d4")

		// needs higher count than given
		assertFalse(DiceTerm.fromDieFaces(4, 4) in DiceTerm.xDy(x = 1, y = 4), "2d4 not in 1d4")

		// is looking for negative terms. // always simplyfied.
		assertFalse(DiceTerm.xDy(x = 1, y = -4) in DiceTerm.xDy(x = 1, y = 4), " (0 - 1d4) not in (+1d4)")
		assertFalse(DiceTerm.xDy(x = 1, y = 4) in DiceTerm.xDy(x = 1, y = -4), " (0 - 1d4) not in (+1d4)")

		assertFalse(DiceTerm.fromDieFaces(4, -4, -4) in DiceTerm.fromDieFaces(4, -4), " (0 - 1d4) not in (+0)")
	}

	@Test
	fun testDiceParsing() {
		log.info("\n\n>>> Test testDiceParsing()")

		var string : String // the string to parse
		var dice: DiceTerm // the parsed dice term.
		var diceDice: DiceTerm // the parsed diceterm string to dice term

		/* Easy test run. No hidden gems. */

		string = "d12 + 3d8  - D21 + 3 + 3 -\t 3"
		dice = DiceTerm.parse(string)
		assertTrue(DiceTerm.xDy(x = 3, y = 8) in dice, "(3d8) in ($dice)")
		assertTrue(DiceTerm.xDy(x = 1, y = 12) in dice, "(1d12) in ($dice)")
		assertTrue(DiceTerm.xDy(x = 1, y = -21) in dice, "(-1d21) in ($dice)")
		assertTrue(DiceTerm.bonus(3) in dice, "(+3) in ($dice)")

		diceDice = DiceTerm.parse(dice.toString())
		log.info("Parsed back: ($dice) \u21d2 ($diceDice)")
		assertEquals(dice, diceDice, "The dice to string to dice should be equal")

		/* Parsing strange names as variable names will throw error. */

		var note = "Try to parse: [x20] -> interpret it as Fun."
		assertEquals((DiceTerm.mod("x20") { 1 }) + DiceTerm.bonus(-1), DiceTerm.parse("x20 + -1"), note)

		try {
			DiceTerm.parse("x:20 + -1") // [x:20] is an invalid variable name.
			fail("Not event the function label fits.")
		} catch (e: Exception) {}

		/* Parsing mathematically correct terms. */

		string = "-1" // typical having a bonus of (-1)
		dice = DiceTerm.parse(string)
		log.debug("Parsed [$string] => [$dice]")

		assertTrue(DiceTerm.bonus(-1) in dice, "(-1) correctly parsed.")
		assertFalse(DiceTerm.bonus(1) in dice, "No other fixed one (+1) for empty terms or such..")

		diceDice = DiceTerm.parse(dice.toString())
		log.info("Parsed back: ($dice) \u21d2 ($diceDice)")
		assertEquals(dice, diceDice, "The dice to string to dice should be equal")

		string = "D20 + -2" // typical having a bonus of (-2)
		dice = DiceTerm.parse(string)
		log.debug("Parsed [$string] => [$dice]")

		assertTrue(D20 in dice, "D20 correctly parsed (in '$string' => ($dice)).")
		assertTrue(DiceTerm.bonus(-2) in dice, "(-2) correctly parsed.")

		diceDice = DiceTerm.parse(dice.toString())
		log.info("Parsed back: ($dice) \u21d2 ($diceDice)")
		assertEquals(dice, diceDice, "The dice to string to dice should be equal")

		log.info("testDiceParsing: OK")

		// local abilityModifier
		val mod: (Ability) -> Int = { when (it) {
			Ability.STR -> -2
			Ability.DEX -> +2
			Ability.CON -> +4
			Ability.WIS -> +1
			Ability.INT -> +1
			Ability.CHA -> +4
			else -> 0
		}}

		// map a label to a pseudo ability modifier getter.
		val termMapper: (DiceTerm.SimpleTerm) -> (DiceTerm.SimpleTerm) = { term ->
			if (term !is DiceTerm.Fun) {
				term
			} else {
				DiceTerm.Fun(term.label) { mod(Ability.CON) }
			}
		}

		string = "D6 + CON" // typical levelling. => 6 + CON

		log.debug("Parse '$string'")

		dice = DiceTerm.parse(string).map(termMapper)
		assertTrue(D6 in dice, "(D6) in dice $dice.")
		assertTrue(("CON") in dice, "(CON) in dice $dice.")
		assertEquals(D6 + DiceTerm.mod("CON", { 1 }), dice)

		log.debug("Roll with $dice: ${dice.roll()}")

		diceDice = DiceTerm.parse(dice.toString())
		log.info("Parsed back: ($dice) \u21d2 ($diceDice)")
		assertEquals(dice, diceDice, "The dice to string to dice should be equal")

		log.info("testDiceParsing (lvl 2): OK")

		if (true) return

		/* Complexity Level 2. */
		// TODO should there be a recogninizeable difference between:
		// 12*(d6 + CON) as 12d6 + 12*CON  // multiplied before rolling
		// and 12*(d6 + CON) as 12 * ([1..6] + |CON|) = 12 * x  // multiplied after rolling, but the term was parsed

		string = "12*(D6 + CON)" // typical levelling. => 12d6 + 12*CON
		dice = DiceTerm.parse(string).map(termMapper)

		assertEquals((D6 + DiceTerm.mod("CON", { 1 })) * 12, dice)

		diceDice = DiceTerm.parse(dice.toString())
		log.info("Parsed back: ($dice) \u21d2 ($diceDice)")
		assertEquals(dice, diceDice, "The dice to string to dice should be equal")

		log.info("testDiceParsing (lvl 3): OK")
	}

	@Test
	fun testTake3of4() {
		log.info("\n\n>>> Test testTake3of4()")

		// TODO (2020-07-14)
		// D6.rollTake(3, 4, true)
		log.info("testTake3of4: OK")
	}

}
