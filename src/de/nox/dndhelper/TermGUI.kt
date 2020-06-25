package de.nox.dndhelper;

import kotlin.math.abs;

fun main() {
	println(
		"DnD Application, display stats and roll your dice!\n"
		+ "Happy Gaming! :D\n"
		+ "==================================================\n");

	val die: SimpleDice = SimpleDice(20);

	for (i in 1..10) {
		println(die.roll());
	}

	for (i in 1..10) {
		print(SimpleDice(1, -1).roll())
		print("; ")
		print(SimpleDice(3, -1).roll())
		print("; ")
		println(SimpleDice(1, -3).roll())
	}

	val diceRegex = "3d8 + d12 - D21 + 3 + 3 - 3"
	val diceTerm = parseDiceTerm(diceRegex)

	println(diceTerm)

	// val diceRegexInvalid = "3d8 + d12 + 3 + 3 - 3 + 12d"
	// parseDiceTerm(diceRegexInvalid)
}

/** Parse string to DiceTerm.
  * @param str the string to parse.
  * @throws Error If the string term was invalid (doesn't contain a dice term)
  * @return DiceTerm which is described by the string.
  */
fun parseDiceTerm(str: String) : DiceTerm {

	// remove whitespaces.
	val nowhite = Regex("\\s").replace(str, "")

	// split before + or -
	val terms = Regex("(?=[\\+-])").split(nowhite)

	val valid = Regex("([\\+-]?\\d*)([dD])?(\\d*)")

	/* Check, if all terms are valid on the first view. */
	if (! terms.all { valid.matches(it) } ) {
		println("Throw Error!")
		throw Exception()
	}

	println(str)

	/* Map terms to parsed simple dice terms.*/
	val parsedDice = terms.map {
		val match = valid.find(it)!!
			val (numStr, d, dieStr) = match.destructured

		val num = when (numStr) {
			"" -> 1
			"+" -> 1
			"-" -> -1
			else -> numStr.toInt(10)
		}
		val die = if (dieStr == "") 1 else dieStr.toInt(10)

		/* parsed a die without max.*/
		if (d != "" && dieStr == "") {
			throw Exception("Invalid die.")
		}

		SimpleDice(die, num)
	}

	return DiceTerm(parsedDice.toTypedArray())
}

/* A complexer dice term with different dice and constants.*/
class DiceTerm(val dice : Array<SimpleDice>) {

	/** Get the sum of all dice and and constants in this term. @return sum:Int*/
	fun roll() : Int = dice.map { it.roll() }.sum();

	/** String representation of simpleDice.*/
	override fun toString() : String
		= dice.map { it.toString() }.joinToString(separator = " ")
}

/* A simple dice term with only one kind of die and number.*/
data class SimpleDice(val max: Int, val times: Int = 1) {

	private val absMax = abs(max)
	private val absTimes = abs(times)
	private val timesNeg = times < 0

	/** Roll this die. @return random int between one and max.*/
	fun roll() : Int
		= (1..absTimes)
		. map { if (absMax < 2) 1 else (1..absMax).random() }
		. sum() * (if (timesNeg) -1 else 1)

	/** String representation of simpleDice.*/
	override fun toString() : String
		= "%+d".format(times) + (if (absMax > 1) "D${absMax}" else "")
}
