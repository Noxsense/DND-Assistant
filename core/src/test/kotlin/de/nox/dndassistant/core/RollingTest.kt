package de.nox.dndassistant.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.math.abs

import de.nox.dndassistant.core.terms.*
import de.nox.dndassistant.core.terms.exceptions.*

class RollingTest {

	private val log = LoggerFactory.getLogger("RollingTest").also {
		it.displayLevel(LoggingLevel.DEBUG)
	}

	private val repeatRolls = 10000

	private fun Double.shouldBe(other: Double, maxDifference: Double = 0.5)
		= abs(this - other).run {
			this <= maxDifference
		}

	@Test
	fun testParsing() {
		log.info("\n\n>>> Test: testParsing()")

		var term: RollingTerm
		var parsed: RollingTerm

		term = Sum(Number(3), Fraction(Product(4, 2), Power(Difference(1, 5), Power(2, 3))))
		parsed = RollingTerm.parse("3 + 4 * 2 / ( 1 - 5 ) ^ 2 ^ 3")
		assertEquals(term, parsed)

		log.info("Parse (parsed.toString())")
		assertEquals(parsed, RollingTerm.parse(parsed.toString()))

		log.info("\n\n>>>> Ignore white spaces, end on new line or =")

		term = Product(Number(13), Sum(Die(6), Reference("con")))
		parsed = RollingTerm.parse("13*(D6 + CON)")

		assertEquals(term, parsed)
		assertEquals(term, RollingTerm.parse("13*(D6 + CON)                   ")) // ignore whitespaces
		assertEquals(term, RollingTerm.parse("13*(D6+CON)"))
		assertEquals(term, RollingTerm.parse("13(D6 + CON) = 13*d6 + 13*CON")) // ignore / end with =

		assertEquals(parsed, RollingTerm.parse(parsed.toString()))

		log.info("\n>>>> Suppoert implicite multiplication")

		assertEquals(Product(Number(2),        Die(6)),           (RollingTerm.parse("2 d6")))
		assertEquals(Product(Number(13),       Reference("con")), (RollingTerm.parse("13 con")))
		assertEquals(Product(Number(13),       Number(13)),       (RollingTerm.parse("13 13")))
		assertEquals(Product(Die(6),           Die(6)),           (RollingTerm.parse("d6 d6")))
		assertEquals(Product(Reference("con"), Die(6)),           (RollingTerm.parse("con d6")))

		assertEquals(Product(Number(13), Sum(Die(6), Reference("con"))), (RollingTerm.parse("13(D6 + CON)")))

		println("More DiceTerm01: ${(RollingTerm.parse("13*(d6 + con)"))}") // handle case insensitively

		println("More DiceTerm04: ${(RollingTerm.parse("6 + 11(2D3 + CON) - DEX"))}") // original
		println("More DiceTerm03: ${(RollingTerm.parse("11(2D3 + CON) + 6 - DEX"))}") // other order of associative operators
		println("More DiceTerm05: ${(RollingTerm.parse("6 + 11[2D3 + CON] - DEX"))}") // evaluate (2d3 + con) first.

		println("More DiceTerm06: ${(RollingTerm.parse("6 + (d2)(2D3 + CON) - DEX"))}") // multiply with die. => (d2*2d3) + (d2*con)
		println("More DiceTerm07: ${(RollingTerm.parse("6 + (d2)[2D3 + CON] - DEX"))}") // multiply with evaluated

		log.info("\n>>>> Support negative numbers.")

		assertEquals(Product(-1, Number(7)),                           (RollingTerm.parse("-7")))
		assertEquals(Product(-1, Product(-1, Number(7))),              (RollingTerm.parse("-(-7)")))
		assertEquals(Product(-1, Product(-1, Number(7))),              (RollingTerm.parse("--7")))
		assertEquals(Product(-1, Product(-1, Product(-1, Number(7)))), (RollingTerm.parse("---7")))
		assertEquals(Difference(8, 7),                                 (RollingTerm.parse("8-7")))
		assertEquals(Sum(8, Product(-1, 7)),                           (RollingTerm.parse("8+-7")))

		log.info("\n>>>> Throw Exceptions on invalid term.")

		try {
			(RollingTerm.parse("8-"))
			fail("Invalid syntax: \"8-\" => missing second operator.")
		} catch (e: NotEnoughTermsException) { }

		try {
			(RollingTerm.parse("(d7 d7"))
			fail("Invalid syntax: \"(d7 d7\" => missing term closing.")
		} catch (e: IncompleteTermException) { }

		try {
			(RollingTerm.parse("d7) d7"))
			fail("Invalid syntax: \"d7) d7\" => missing term opening.")
		} catch (e: IncompleteTermException) { }
	}

	@Test
	fun testOperations() {
		log.info("\n\n>>> Test: testOperations(), and other initiations.")

		// with int
		assertEquals(Sum(Number(8), Number(9)), Sum(Number(8), 9)) // sum
		assertEquals(Sum(Number(8), Number(9)), Sum(8, Number(9))) // sum
		log.info("Parsing with Numbers is ok.")

		// basic parser (die, number, reference)
		assertEquals(Sum(Number(8), Number(9)), Sum(Number(8), "9"))
		assertEquals(Sum(Number(8), Number(9)), Sum(8, "9"))
		assertEquals(Sum(Number(8), Number(9)), Sum("8", "9")) // number
		assertEquals(Sum(Number(8), Reference("nine")), Sum("8", "nine")) // reference
		assertEquals(Sum(Number(8), Die(9)), Sum("8", "d9")) // die
		log.info("Parsing with Strings is ok.")

		// operations
		assertEquals(Sum(Number(8), Number(9)), Number(8) + Number(9))
		assertEquals(Sum(Number(8), Number(9)), Number(8) + 9)
		assertEquals(Sum(Number(8), Number(9)), Number(8) + "9") // number
		assertEquals(Sum(Number(8), Reference("nine")), Number(8) + "nine") // reference
		assertEquals(Sum(Number(8), Die(9)), Number(8) + "d9") // die
		log.info("Sum with operator (+) is ok.")

		assertEquals(Difference(Number(8), Number(9)), Number(8) - Number(9))
		assertEquals(Difference(Number(8), Number(9)), Number(8) - 9)
		assertEquals(Difference(Number(8), Number(9)), Number(8) - "9") // number
		assertEquals(Difference(Number(8), Reference("nine")), Number(8) - "nine") // reference
		assertEquals(Difference(Number(8), Die(9)), Number(8) - "d9") // die
		log.info("Difference with operator (-) is ok.")

		assertEquals(Product(Number(8), Number(9)), Number(8) * Number(9))
		assertEquals(Product(Number(8), Number(9)), Number(8) * 9)
		assertEquals(Product(Number(8), Number(9)), Number(8) * "9") // number
		assertEquals(Product(Number(8), Reference("nine")), Number(8) * "nine") // reference
		assertEquals(Product(Number(8), Die(9)), Number(8) * "d9") // die
		log.info("Product with operator (*) is ok.")

		assertEquals(Fraction(Number(8), Number(9)), Number(8) / Number(9))
		assertEquals(Fraction(Number(8), Number(9)), Number(8) / 9)
		assertEquals(Fraction(Number(8), Number(9)), Number(8) / "9") // number
		assertEquals(Fraction(Number(8), Reference("nine")), Number(8) / "nine") // reference
		assertEquals(Fraction(Number(8), Die(9)), Number(8) / "d9") // die
		log.info("Fraction with operator (/) is ok.")

		assertEquals(Number(9), +Number(9)) // keep

		assertEquals(Number(-9), -Number(9)) // just flip.

		assertEquals(Product(-1, Reference("nine")), -Reference("nine")) // multiply with negative
		assertEquals(Product(-1, Die(9)), -Die(9)) // multiply with negative
		assertEquals(Product(-1, Sum(8, 9)), -Sum(8, 9)) // (-1)*(8-9) // multiply with negative
		log.info("Unary operators are ok.")
	}

 	// TODO (2021-02-19) not implemented yet.
 	// @Test
	fun testAlgebraicEquality() {
		log.info("\n\n>>> Test: testEquality() .. and simplification.")

		var t0: RollingTerm; var t1: RollingTerm

		log.info("Simple Negative Numbers instead of (-1) multiplications")

		t0 = Power(2, -1)

		// simplification goals:
		//   2 ^ -1
		// = 1 / (2 ^ 1)
		// = 1 / (2)

		t1 = RollingTerm.parse("(2)^(-1)") // parse the term. => (2 ^ (-1 * 1))

		// simplification goals:
		//   2 ^ (-1 * 1)
		// = 2 ^ (-1)
		// = 1 / (2 ^ 1)
		// = 1 / (2)

		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		t1 = RollingTerm.parse("(2)^-1") // left out parentheses
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		t0 = Power(2, 1)
		t1 =  Number(2) // very simple, not simplified
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertEquals(false, t1.simple.second)
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		t0 = Power(2, 1)
		t1 =  RollingTerm.parse("(2)^(--1)") // dupl. negative
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		t1 = RollingTerm.parse("(2)^(-(-1))") // dupl. negative
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		log.info("Addition: Commutative and Assoziative")

		t0 = Sum("d1", "d2")
		t1 = Sum("d2", "d1")
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		 // (d1 + (d2+d3)) == ((d1+d2) + d3)  == ((d3+d2) + d1)
		t0 = Sum(Sum("d1", Sum("d2", "d3")), "d4")

		t1 = Sum(Sum("d1", "d2"), Sum("d3", "d4"))
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")

		log.debug("Check equality of: (summands)\n  >  ${t0.summands}\n  >  ${t1.summands}")

		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		t1 = Sum(Sum("d3", Sum("d2", "d4")), "d1")
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		log.info("Multiplication: Commutative and Assoziative")

		val a = { i: Int -> Reference("a$i") }

		t0 = (a(0)*a(2)) + (a(0)*a(3)) + (a(1)*a(2)) + (a(1)*a(3)) // ac + ad + bc + bd
		t1 = (a(0) + a(1)) * (a(2) + a(3)) // (a+b) * (c+d)

		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")

		t0 = Die(3) * -2 // -2d3
		t1 = (-Die(3)) - Die(3) // -d3 -d3 = -2d3

		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (summands)\n  >  ${t0.summands}\n  >  ${t1.summands}")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")

		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		// (d1 * (d2*d3)) == ((d1*d2) * d3) = ((d3*d2) * d1)
		t0 = Product("d1", Product("d2", "d3"))

		t1 = Product(Product("d1", "d2"), "d3")
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		t1 = Product(Product("d3", "d2"), "d1")
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		// a / b == a * (1/b)
		t0 = Product("a", Fraction(1, "b"))
		t1 = Fraction("a", "b")
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		log.info("'Unfolded' multiplication and exponation")

		// 3d6 == d6 + (d6 + d6)
		t0 = Product(3, "d6")
		t1 = Sum("d6", Sum("d6", "d6"))
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		// d6^3 == d6 * (d6 * d6)
		t0 = Power("d6", 3)
		t1 = Product("d6", Product("d6", "d6"))
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		log.info("Already evaluated easier terms")

		// 2 ( 2d6 + 3)) == 4d6 + 6
		t0 = (Die(6)*2 + 3) * 2
		t1 = (Die(6)*4) + 6
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		t0 = Sum(2, 0)
		t1 = Number(2)
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		t0 = Product(2, -1)
		t1 = Number(-2)
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		// mathematically valid reorder, but may bring different results while rounding the terms in-between.

		// [1/3 + 1/3 + 1/3 + 1/2 + 1/2 = 2] .... or [0 + 0 + 0 + 0 + 0 = 0]
		t0 = RollingTerm.parse("(1/3) + (1/2) + 1/3 + 1/2 + 1/3")
		t1 = Number(2)
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (summands)\n  >  ${t0.summands}\n  >  ${t1.summands}")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		t0 = RollingTerm.parse("(a/3) + (b/2) + 1/3 + 1/2 + 1/3")
		t1 = Sum(Fraction(Reference("b") + 1,2), Fraction(Reference("a") + 2,3)) // (a+2):3 + (b+1):
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		t0 = Sum(Power(2, -1),Power(2, -1))
		t1 = RollingTerm.parse("(2)^(-1) + (2)^(-1)") // == 1/2 + 1/2
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		t0 = Product(10, Power(2, -1))
		t1 = RollingTerm.parse("10 * (2)^(-1)") // == 10 / 2
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		t0 = Product(2, 2)
		t1 = Number(4)
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		t0 = Product("2", Product("d2", "2"))
		t1 = Product("d2", 4) // (2 * (d2*2)) == (4*d2
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		t0 = Product("2", Product("d2", "2"))
		t1 = Die(2)  + "d2" + "d2" + "d2" // (2 * (d2*2)) == d4 + d4 + d4 + d4
		log.debug("\nCheck equality of:\n  >  $t0\n  >  $t1")
		log.debug("Check equality of: (simplified)\n  >  ${t0.simplify()}\n  >  ${t1.simplify()}")
		assertTrue(t0.equalsAlgebraically(t1))
		assertTrue(t1.equalsAlgebraically(t0))

		// not equal: "rolled three times" vs "rolled once and multiplied with 3"
		assertTrue(Product("d2", 3) != Product(Rolled("d2"), 3)) // d2 + d2 + d2 != 3[d3]

		log.info("Equality is OK.")
	}

	@Test
	fun testRoll() {
		log.info("\n\n>>> Test: testRoll()")

		// d20

		testcaseRoll(
			term = Die(20), // d20
			min = 1,
			max = 20,
			expected = (1 .. 20).toList(),
			diceCount = 1,)

		// 4d5
		// 9 or 45 with possibility of 0.000001 => check if roll is in range instead.

		testcaseRoll(
			term = Product(4, Die(5)),
			min = (4*1),
			max = (4*5),
			expected = (4 .. 20).toList(),
			diceCount = 4,
		)

		// -2d3 => roll d3 twice
		// -2d3 = -d3 -d3 = {-1..-3} + {-1..-3} => {-3-3,-3-2,-3-1,-2-2,-2-1,-1-1} = {-6, -5, -4, -4, -3, -2}

		testcaseRoll(
			term = Product(-2, Die(3)),
			min = -6, // -3, -3
			max = -2, // -1, -1
			expected = (-6 .. -2).toList(),
			diceCount =  2,
		)

		// -2[d3] => first roll, then calculate.
		// -2d3 = -2 * {1..3} => {-6, -4, -2}

		testcaseRoll(
			term = Product(-2, Rolled(Die(3))),
			min = -6, // -3, -3
			max = -2, // -1, -1
			expected = listOf(-6, -4, -2),
			diceCount = 1,
		)

		// -2[2d3] = -2[d3 + d3]=> first roll, then calculate.

		testcaseRoll(
			term = Product(-2, Rolled(Product(Die(3), 2))),
			min = -12, // (3 + 3)*(-2)
			max = -4, // (1 + 1)*(-2)
			expected = listOf(3+3, 3+2, 3+1, 2+1, 1+1).map { it * (-2) },
			diceCount = 2,
		)

		// x = 9, with other variables divers.
		val termX = Reference("x")

		testcaseRoll(
			term = termX,
			min = 9,
			max = 9,
			expected = listOf(9),
			diceCount = 0,
			variables = mapOf("x" to 9),
			)

		// x = -20, completly different.
		testcaseRoll(
			term = termX,
			min = -20,
			max = -20,
			expected = listOf(-20),
			diceCount = 0,
			variables = mapOf("x" to -20)
		)

		// x * D6 => can be 0d6 or 4d6 ...
		val xd6:RollingTerm = Reference("x") * Die(6)
		var x0 = mapOf("x"  to 0)
		var x4 = mapOf("x"  to 4)

		// x * D6, with x = 0
		// => with x=0 no die, otherwise, it's unknown or depending on x.
		testcaseRoll(
			term = xd6,
			min = 0,
			max = 0,
			expected = listOf(0),
			diceCount = 0,
			variables = x0
		)

		// x * D6, with x = 4
		// => with x=4 four dice, otherwise, it's unknown or depending on x.
		testcaseRoll(
			term = xd6,
			min = 4*1,
			max = 4*6,
			expected = (4 .. 24).toList(),
			diceCount = 4,
			variables = x4,
		)

		log.info("testRoll: OK")
	}

	/** Roll a term and compare the expected results. */
	// TODO (2021-02-12) maybe compare not by eventually existing / rolled results, but by it's distribution.
	private fun testcaseRoll(
		repetitions: Int = repeatRolls,
		term: RollingTerm,
		min: Int,
		max: Int,
		diceCount: Int,
		expected: List<Int>,
		variables: Map<String, Int> = mapOf()
	) {
		log.info(".".repeat(78))
		log.info("New testcaseRoll with ($term)")
		if (variables.size > 0) log.info("...  with ${variables.toList().joinToString(",", "{", "}") { "${it.first} \u2192 ${it.second}" }}")

		// overshadowing
		val varF = RollingTerm.mapToVariables(variables)

		val tMin = term.min(varF); val tMax = term.max(varF); val tAvg = term.average(varF)

		log.info("The term calculates min/max/avg: $tMin/$tMax/$tAvg")

		assertEquals(min, tMin)
		assertEquals(max, tMax)

		val avg = expected.average()

		assertTrue(avg.shouldBe(tAvg)) // expected average by term and expected results.

		log.info("summandsWith ${term.summandsWith(varF)}")
		log.info("Dice of term: ${term.dice(varF)} (variables: $variables) with summandsWith ${term.summandsWith(varF)}")

		assertEquals(diceCount, term.dice(varF).size, "Num of dice ($term;  ($variables))")

		log.info("Roll: (${avg}) $expected (Expected)")

		val rolled = (1..repetitions).map { term.evaluate(varF) }.sorted()

		val average = rolled.sum() / repeatRolls.toDouble()

		log.info("Roll: $term: ($average) ${rolled.groupBy { it }.mapValues { it.value.size }}")

		log.debug("Recently rolled with: ${term.dice(varF)} \u21d2 ${term.getRecentlyRolled()}")
		assertEquals(term.dice(varF).size, term.getRecentlyRolled().size, "Rolled all dice ($term)")

		expected.forEach { assertTrue(it in rolled, "Dice ($term): Expected $it in ${rolled.toSet()}") }
		rolled.forEach { assertTrue(it in expected, "Dice ($term): Rolled $it in $expected") }

		// test if expected average is close to the thrown results.
		assertTrue(average.shouldBe(avg))

		log.info("Tests for Term `$term` OK.")
	}
}
