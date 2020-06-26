package de.nox.dndassistant;

import kotlin.math.abs;
import kotlin.math.max;
import kotlin.math.min;

/* Roll the SimpleDice {num} times, take {take} best/worst values. */
fun rollTake(dice: SimpleDice, take : Int = 3, num : Int = 4, best : Boolean = true) : List<Int> {
	val rolls = (1..abs(num)).map { dice.roll() }.toTypedArray()
	rolls.sort()

	return if (best) rolls.toList().takeLast(take) // highest
		else rolls.toList().take(take) // lowest
}

/* Roll with advenage: Take the best of two rolls.*/
fun rollWithAdventage(dice: SimpleDice) : Int
	= rollTake(dice, 1, 2, true).first()

/* Roll with advenage: Take the worst of two rolls.*/
fun rollWithDisdventage(dice: SimpleDice) : Int
	= rollTake(dice, 1, 2, false).first()

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

	/** Get the sum of all dice and and constants in this term.
	  *@return sum:Int
	  */
	fun roll() : Int = dice.map { it.roll() }.sum();

	/** String representation of simpleDice.*/
	override fun toString() : String
		= dice.map { it.toString() }.joinToString(separator = " ")
}

fun Bonus(v : Int) : SimpleDice = SimpleDice(1, v)
fun Die(v : Int) : SimpleDice = SimpleDice(v, 1)

/* A simple dice term with only one kind of die and number.*/
data class SimpleDice(val max: Int, val times: Int = 1) : Comparable<SimpleDice> {

	private val absMax = abs(max)
	private val absTimes = abs(times)
	private val timesNeg = times < 0

	/** Roll this die.
	  * @return random int between one and max.
	  */
	fun roll() : Int
		= (1..absTimes)
		.map { if (absMax < 2) 1 else (1..absMax).random() }
		.sum() * (if (timesNeg) -1 else 1)

	/* Compare to another SimpleDice term.*/
	override fun compareTo(other: SimpleDice) : Int
		= when {
			absMax == other.absMax -> times - other.times
			else -> absMax - other.absMax
		}

	/** String representation of simpleDice.*/
	override fun toString() : String
		= "%+d".format(times) + (if (absMax > 1) "D${absMax}" else "")
}
