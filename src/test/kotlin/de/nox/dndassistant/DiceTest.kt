package de.nox.dndassistant

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiceTest {

	private val logger = LoggerFactory.getLogger("DiceTest")

	@Test
	fun testToString() {
		// TODO (2020-07-14)
		return
		assertEquals(SimpleDice(20, 0), SimpleDice(0, 20))

		assertEquals(SimpleDice(20, 1), SimpleDice(20))
		assertEquals(SimpleDice(20, 1), SimpleDice(-20, -1))
		assertEquals(SimpleDice(20), SimpleDice(-20, -1))

		assertEquals(SimpleDice(-3), SimpleDice(-3, 1))
		assertEquals(SimpleDice(-3), SimpleDice(3, -1))

		assertEquals(SimpleDice(1, -3), SimpleDice(-1, 3))
		assertEquals(SimpleDice(1, -3), Bonus(-3))
	}

	@Test
	fun testD20() {
		val dice = D20
		val rolled = (1..1000).map { dice.roll() }
		println("Roll: $dice: $rolled")
		for (i in 1..20) {
			assertTrue(i in rolled, "Thrown $i with $dice")
		}
	}

	@Test
	fun testCustomDice() {
		val dice = SimpleDice(-3, 1)
		val rolled = (1..1000).map { dice.roll() }
		println("Roll: $dice: $rolled")
		for (i in 1..3) {
			continue
			assertTrue((-i) in rolled, "Thrown ${-i} with $dice")
			// TODO (2020-07-14)
		}
	}

	@Test
	fun testRollBonus() {
		/// only fixied values.
		val dice = SimpleDice(1, -3)
		val rolled = (1..1000).map { dice.roll() }
		println("Roll: $dice: $rolled")
		// assertTrue(rolled.all { it == (-3) }, "Thrown only (-3) with $dice")
		// XXX (2020-07-14)
	}

	@Test
	fun testSimplifyTerm() {
		val dice = DiceTerm(arrayOf(
			SimpleDice(8, 3),
			SimpleDice(12),
			SimpleDice(-21),
			Bonus(+3),
			Bonus(+3),
			Bonus(-3)
		))
		// TODO (2020-07-14)
		println("Simplify dice term: $dice")
		println(dice.dice.toList().joinToString())
		println(dice.dice.sortBy { it.max })
	}

	@Test
	fun testDiceParsing() {
		val string = "3d8 + d12 - D21 + 3 + 3 - 3"
		val dice = DiceTerm.parse(string)
		val rolled = (1..1000).map { dice.roll() }
		println("Roll: $dice: $rolled")
		// assertTrue(SimpleDice(8, 3) in dice, "3d8 in $dice")
		// assertTrue(SimpleDice(12) in dice, "3d8 in $dice")
		// assertTrue(SimpleDice(-21) in dice, "3d8 in $dice")
		// assertTrue(Bonus(3) in dice, "3d8 in $dice")

		// 1. parse, 2. to string, 3. parse
		val diceStr = dice.toString()
		val diceStrDice = DiceTerm.parse(diceStr)
		println("($string) \u21d2 ($diceStr)")
		// assertEquals(dice, diceStrDice, "String \u2192 Dice \u2192 String \u2192 Dice")
		// TODO (2020-07-14)
	}

	@Test
	fun testTake3of4() {
		// TODO (2020-07-14)
		D6.rollTake(3, 4, true)
	}

}
