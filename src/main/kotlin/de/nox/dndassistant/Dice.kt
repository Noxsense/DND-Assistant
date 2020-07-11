package de.nox.dndassistant

import kotlin.math.abs;
import kotlin.math.max;
import kotlin.math.min;

private val logger = LoggerFactory.getLogger("Dice")

/* A complexer dice term with different dice and constants.*/
class DiceTerm(val dice : Array<SimpleDice>) {

	companion object {

		/** Parse string to DiceTerm.
		  * @param str the string to parse.
		  * @throws Error If the string term was invalid (doesn't contain a dice term)
		  * @return DiceTerm which is described by the string.
		  */
		fun parse(str: String) : DiceTerm {

			// remove whitespaces.
			val nowhite = Regex("\\s").replace(str, "")

			// split before + or -
			val terms = Regex("(?=[\\+-])").split(nowhite)

			val valid = Regex("([\\+-]?\\d*)([dD])?(\\d*)")

			/* Check, if all terms are valid on the first view. */
			if (! terms.all { valid.matches(it) } ) {
				logger.error("Throw Error: Not a valid Dice Term!")
				throw Exception("Not a valid Dice Term")
			}

			logger.debug("Parse dice term from '${str}'")

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
	}

	constructor(d: SimpleDice) : this(arrayOf(d)) {
	}

	/** Get the average value to expeced from the dice term.*/
	fun average() : Int
		= dice.map { it.average() }.sum()

	/** Get the sum of all dice and and constants in this term.
	  *@return sum:Int
	  */
	fun roll() : Int
		= dice.map { it.roll() }.sum();

	/** String representation of simpleDice.*/
	override fun toString() : String
		= dice.map { it.toString() }.joinToString(separator = " ")
}

/* A simple dice term with only one kind of die and number.*/
data class SimpleDice(val max: Int, val times: Int = 1) : Comparable<SimpleDice> {

	private val absMax = abs(max)
	private val absTimes = abs(times)
	private val timesNeg = times < 0

	/* Compare to another SimpleDice term.*/
	override fun compareTo(other: SimpleDice) : Int
		= when {
			absMax == other.absMax -> times - other.times
			else -> absMax - other.absMax
		}

	/** String representation of simpleDice.*/
	override fun toString() : String
		= "%+d".format(times) + (if (absMax > 1) "D${absMax}" else "")

	/** Get the average value of the dice. */
	fun average() : Int
		= (absTimes * absMax) / (when {
			absMax == 1 && timesNeg -> -1
			absMax == 1 -> 1
			timesNeg -> -2
			else -> 2
		})

	/** Roll this die.
	  * @return random int between one and max.
	  */
	fun roll() : Int
		= (1..absTimes)
		.map { if (absMax < 2) 1 else (1..absMax).random() }
		.sum() * (if (timesNeg) -1 else 1)

	/* Roll the SimpleDice {num} times, take {take} best/worst values. */
	fun rollTake(take: Int = 3, num: Int = 4, best: Boolean = true) : List<Int> {
		val rolls = (1..abs(num)).map { roll() }.toTypedArray()
		rolls.sort()

		return if (best) rolls.toList().takeLast(take) // highest
			else rolls.toList().take(take) // lowest
	}

	/* Roll with advenage: Take the best of two rolls.*/
	fun rollWithAdventage() : Int
		= rollTake(1, 2, true).first()

	/* Roll with advenage: Take the worst of two rolls.*/
	fun rollWithDisdventage() : Int
		= rollTake(1, 2, false).first()
}

fun Bonus(v : Int) : SimpleDice = SimpleDice(1, v)

val D2 : SimpleDice = SimpleDice(2, 1)
val D4 : SimpleDice = SimpleDice(4, 1)
val D6 : SimpleDice = SimpleDice(6, 1)
val D8 : SimpleDice = SimpleDice(8, 1)
val D10 : SimpleDice = SimpleDice(10, 1)
val D12 : SimpleDice = SimpleDice(12, 1)
val D20 : SimpleDice = SimpleDice(20, 1)
val D100 : SimpleDice = SimpleDice(100, 1)
