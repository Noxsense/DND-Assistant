package de.nox.dndassistant.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// TODO (2020-10-01) Refactor !

/* A complexer dice term with different dice and constants.*/
class DiceTerm(vararg ds: SimpleDice) {
	val dice : Array<SimpleDice> = arrayOf(*ds)

	companion object {
		val log: Logger = LoggerFactory.getLogger("D&D Dice")

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
				log.error("Throw Error: Not a valid Dice Term!")
				throw Exception("Not a valid Dice Term")
			}

			log.debug("Parse dice term from '${str}'")

			/* Map terms to parsed simple dice terms.*/
			return DiceTerm(*terms.map {
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
			}.toTypedArray())
		}
	}

	// initiate with a list of faces: 6 =~ 1D6, 1 =~ 1d1, 20 =~ 1D20, ...
	constructor(vararg fs: Int) : this(
			*fs.groupBy { it }.entries
			.map { SimpleDice(it.key, it.value.size) }
			.toTypedArray())

	/** Check, if a Dice Term contains given dice (simple dice term).*/
	operator fun contains(other: SimpleDice) : Boolean
		= other in dice

	/** Check equality to another DiceTerm.*/
	override fun equals(other: Any?) : Boolean
		= when {
			other == null -> false
			other !is DiceTerm -> false
			else -> (same(other)
				|| contracted().dice.contentEquals(other.contracted().dice))
		}

	/** Dice term is equal by elements with another Dice term.*/
	fun same(other: DiceTerm) : Boolean
		= (dice.sorted() == other.dice.sorted())

	/** Contract dice with same faces.*/
	fun contracted() : DiceTerm
		= DiceTerm(*dice.toList().sortedDescending()
		.fold<SimpleDice, List<SimpleDice>>(listOf(), { xs, x -> when {
			!xs.isEmpty() && xs.last().faces == x.faces -> {
				xs.dropLast(1) + xs.last().addDie(x.factor) // add to the same faced die.
			}
			else -> xs + x // add a new faced die.
		}}).toTypedArray())

	/** Split all dice to single die.*/
	fun split() : DiceTerm
		= DiceTerm(*dice.toList()
		.fold<SimpleDice, List<SimpleDice>>(listOf(), { xs, x -> when (x.count) {
			1 -> xs + x
			else -> xs + ((1..x.count).map { SimpleDice(x.faces, x.sign) })
		}}).toTypedArray())

	/** Create new dice term with added bonus.*/
	operator fun plus(b: Int) : DiceTerm
		= DiceTerm(*dice, SimpleDice(1, b))

	/** Create new dice term with added dice.*/
	operator fun plus(d: SimpleDice) : DiceTerm
		= DiceTerm(*dice, d)

	/** Create new dice term with added dice term.*/
	operator fun plus(ds: DiceTerm) : DiceTerm
		= DiceTerm(*dice, *ds.dice)

	/** Create new dice term with subtracted bonus.*/
	operator fun minus(b: Int) : DiceTerm
		= DiceTerm(*dice, SimpleDice(1, -b))

	/** Create new dice term with reducing dice (more dice, but negativly).*/
	operator fun minus(d: SimpleDice) : DiceTerm
		= DiceTerm(*dice, d.flip())

	/** Create new dice term with reduced dice term (more dice, but negativly).*/
	operator fun minus(ds: DiceTerm) : DiceTerm
		= DiceTerm(*dice, *ds.dice.map { it.flip() }.toTypedArray())

	/** Get the average value to expeced from the dice term.*/
	val average : Double get()
		= dice.map { it.average }.sum()

	/** Get the sum of all dice and constants in this term.
	  *@return sum:Int */
	fun roll() : Int
		= rollList().sum()

	/** Roll every SimpleDice and constants in this term.
	  *@return List of Ints */
	fun rollList() : List<Int>
		= dice.flatMap { it.rollList() }

	/** String representation of simpleDice.*/
	override fun toString() : String
		= dice.map { it.toString() }.joinToString(separator = " ").let {
			// remove a leading "+"
			if (it[0] == '+') it.substring(1) else it
		}
}

// TODO (2020-10-01) Refactor !

/* A simple dice term with only one kind of die and number.*/
data class SimpleDice(val max: Int, val times: Int = 1) : Comparable<SimpleDice> {
	internal val sign = if (times < 0 != max < 0) -1 else 1
	internal val count = abs(times)
	val faces = if (times == 0 || max == 0) 1 else abs(max) /* faces of the die.*/
	val factor = if (max == 0) 0 else count * sign /* how often */

	/* Compare by highst (lowest) value.*/
	override fun compareTo(other: SimpleDice) : Int
		= when {
			faces == other.faces -> factor - other.factor
			else -> faces - other.faces
		}

	/** Simple conversion to a DiceTerm.
	 * @return DiceTerm with only this/these SimpleDice. */
	fun toTerm() : DiceTerm = DiceTerm(this)

	/** Get a new simple dice term with same count of dice, but other faces.*/
	fun addFaces(i: Int) : SimpleDice
		= SimpleDice(faces + i, factor)

	/** Get a new simple dice term with an added die (same face).*/
	fun addDie(i: Int) : SimpleDice
		= SimpleDice(faces, factor + i)

	/** Flip the sign of these dice: Get a term to roll the flipped values.*/
	fun flip() : SimpleDice
		= SimpleDice(faces, -factor)

	operator fun plus(other: SimpleDice) : DiceTerm
		= when {
			faces != other.faces -> DiceTerm(this, other)
			else -> DiceTerm(SimpleDice(faces, factor + other.factor))
		}

	operator fun minus(other: SimpleDice) : DiceTerm
		= this + other.flip()

	fun asList() : List<SimpleDice>
		= (0 until count).map { SimpleDice(sign * faces, 1) }

	fun asFaceList() : List<Int>
		= (0 until count).map { sign * faces }

	/** Check equality to another simpleDice.*/
	override fun equals(other: Any?) : Boolean
		= when {
			other == null -> false
			other !is SimpleDice -> false
			else -> (((faces == other.faces) && (factor == other.factor))
				// 0d20 == 21d0
				|| ((factor == 0 || faces == 0)
				&& (other.factor == 0 || other.faces == 0))
			)
		}

	/** String representation of simpleDice.*/
	override fun toString() : String
		= when {
			// faces == 0 || factor == 0 -> "+ 0" // just 0
			factor == 0 || faces < 2 -> "%+d".format(factor) // constant (d1)
			else -> "%+dd%d".format(factor, faces) // default term: +1d6
		}

	fun toStringFaces() : String
		= "${factor}d${faces}"

	/** Get the average value of the dice. */
	val average : Double get()
		= (1 + faces) / 2.0 * factor

	/** Roll this die.
	  * @return random int between one and max/faces.
	  */
	fun roll() : Int
		= rollList().sum()

	/** Roll these dice.
	 * @return a list of rolled faces. */
	fun rollList(): List<Int>
		= when {
			faces < 2 -> listOf(count * sign) // constant
			else -> (0 until count).map { (1 .. faces).random() * sign }
		}

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
