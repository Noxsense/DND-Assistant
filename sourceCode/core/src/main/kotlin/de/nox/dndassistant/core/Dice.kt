package de.nox.dndassistant.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// TODO (2020-10-01) Refactor !

/** Create a quick dice term with just one face.
 * d(x) = DiceTerm(x). */
fun d(face: Int, count: Int = 1) : DiceTerm = DiceTerm(face) * count
fun bonus(n: Int) : DiceTerm = d(1, n)

/** Default die. */
val D1 : DiceTerm = d(1)
val D2 : DiceTerm = d(2)
val D4 : DiceTerm = d(4)
val D6 : DiceTerm = d(6)
val D8 : DiceTerm = d(8)
val D10 : DiceTerm = d(10)
val D12 : DiceTerm = d(12)
val D20 : DiceTerm = d(20)
val D100 : DiceTerm = d(100)

/* A complexer dice term with different dice and constants.
 * What is dice term: "1d4 + 1d10 + 1 + { STR } + { my_var }".
 */
class DiceTerm(_faces: List<Int>) {
	companion object {
		private val log: Logger = LoggerFactory.getLogger("D&D Dice")

		/** An empty dice term, where no faces will be rolled and without any bony. */
		public val EMPTY = DiceTerm(0)

		/** Parse string to DiceTerm.
		 * @param str the string to parse.
		 * @throws Error If the string term was invalid (doesn't contain a dice term)
		 * @return DiceTerm which is described by the string.
		 */
		fun parse(str: String) : DiceTerm {
			// remove whitespaces.
			val nowhite = Regex("\\s").replace(str, "")

			/* substitute simple math equalisations, like +- or -+ */
			// TODO (2020-10-17)
			var quickFix = nowhite.replace("+-", "-").replace("-+", "-") // WORK_AROUND

			// split before operators (+ or -). Remove empty terms (like "+2").
			val terms = Regex("(?=[\\+-])").split(quickFix).filter { it != "" }

			val valid = Regex("([\\+-]?\\d*)([dD])?(\\d*)")

			/* Check, if all terms are valid on the first view. */
			if (!terms.all { valid.matches(it) }) {
				log.error("Throw Error: Not a valid Dice Term!")
				throw Exception("Not a valid Dice Term")
			}

			/* Map terms to parsed simple dice terms.*/
			return DiceTerm(terms.flatMap {
				val match = valid.find(it)!!
					val (numStr, d, dieStr) = match.destructured

				/* Parse x in _Dy, where _ can be
				 * -  '' => 1Dy
				 * -  '+' => -1Dy
				 * -  '-' => +1Dy
				 * -  'n' => nDy
				 */
				val num = when (numStr) {
					"" -> 1
					"+" -> 1
					"-" -> -1
					else -> numStr.toInt(10)
				}
				val die = when {
					numStr == dieStr && dieStr == "" -> 0 // whole term was empty
					(dieStr == "") -> 1
					else -> dieStr.toInt(10)
				}

				/* parsed a die without max.*/
				if (d != "" && dieStr == "") {
					throw Exception("Invalid die.")
				}

				val absCnt = abs(num)
				val sigDie = if (num < 0) -die else die

				(0 until absCnt).map { sigDie }
			}).also {
				log.debug("Parse dice term from '${str}' to ($it)")
			}
		}
	}

	/** All die, variables and simple terms inside this term. */
	val faces: IntArray

	/** The random part of the term, given by the dice that is a subset of faces. */
	val dice: IntArray

	/** The fixed part of the term, which may come from a changing variable.
	 * Even though, that it's always fixed to this. */
	val fixed: Int
	private val fixedList: List<Int> get() = listOf(fixed)

	/** For every faces, count how often they appeared (Face: Count). */
	val facesGrouped: Map<Int, Int>

	/** String representation for the faces of this term .*/
	val facesString: String

	/** The max value which could be rolled. */
	val max: Int

	/** the min value which could be rolled. */
	val min: Int

	/** get the average value to expected from the dice term.*/
	val average : Double

	/** Construct a term with dynamic variables as faces. */
	constructor(n: Int, vararg ns: Int) : this(ns.toList() + n)

	/** Construct a Dice Term as from a Map representing: { Face = count }.
	 * For example: { 20=1, 1=7, 4=1, 2=-5, -4=-4, -100=1 } => ( +1d20 +1d7 +1d4 -5d2 +4d4 + -1d100 ).*/
	constructor(facesGroup: Map<Int, Int>) : this( facesGroup.let {
		it.toList().flatMap { (face, count) ->
			val sigDie = abs(face).let { f -> if (face * count > 0) f else -f }
			val absCnt = abs(count)

			/* Make a list with (absolute count) times the signed die. */
			(0 until absCnt).map { sigDie }
		}.toList<Int>()
	})

	/** While initiating, the 0 will be removed, expect it would be empty term.
	 * Also simplify the ones and negative ones, save them boni extra.
	 * No further simplifications, because maybe sometimes such effects are wanted
	 * 1d4 - 1d4 => [1, -3] => -2 // or [-3, 3] instead of 0.*/
	init {
		/* Separate zeros, ones and variables. */
		val (ones, random) = _faces.partition { it == 0 || it == 1 } // TODO extend VAR

		/* Get all the ones and zeros, avoid { 1=n, -1=m } as single terms. */
		fixed = ones.sum()
		dice = random.sortedDescending().toIntArray()

		/* Put back the cumulated fixed and random for the faces. */
		faces = (random + (0 until abs(fixed)).map { if (fixed < 0) -1 else 1 })
			.sortedDescending()
			.let { when {
				it.size < 1 -> IntArray(1) { 0 } // empty
				else -> it.toIntArray() // the _faces without nulls.
			}}

		/* Prepared Group and strings. */
		facesGrouped = faces.groupBy { it }.mapValues { it.value.size }
		facesString = facesGrouped.toList()
			.sortedByDescending { it.first }
			.joinToString(" ") { (face, count) ->
				/* [+-] X d Y. */
				val (x, y) = abs(count) to abs(face)
				val positive = face * count >= 0
				val signedX = if (positive) x else -x // x would never be null.

				when {
					y == 0 || x == 0-> "+0"
					y == 1 -> "%+d".format(signedX)
					else -> "%+dd%d".format(signedX, y)
				}
			}

		val expected = listOf(
			faces.sum(), // highest rolls
			faces.map { when { it == 0 -> 0; it < 0 -> -1; else -> 1} }.sum() // lowest rolls
		)

		max = expected.maxOrNull()!!
		min = expected.minOrNull()!!

		average = expected.sum() / 2.0

		log.debug("New DiceTerm(${faces.toList()}) => {max: $max, min: $min, avg: $average, grouped: $facesGrouped, string: $facesString }")
	}

	/** Check, if a term contains a die with a given face count.*/
	operator fun contains(face: Int) : Boolean
		= face in faces

	/** Check, if the term contains all dice given by the other term.
	 * For example: (4d8) is in (12d8 + 1). */
	operator fun contains(other: DiceTerm) : Boolean
		= other.facesGrouped.all { (face, count) ->
			this.facesGrouped.getOrElse(face) { 0 } >= count // this is covering other's dice.
		}.also {
			log.debug("($other) ${if (it) "" else "not "}in ($this)")
		}

	/** Check equality to another term.*/
	override fun equals(other: Any?) : Boolean
		= when {
			other == null -> false
			other !is DiceTerm -> false
			else -> faces.contentEquals(other.faces)
		}

	/** Roll every dice one and constants in this term.
	  *@return List<Int> containing each rolled result and the fixed constants. */
	fun roll() : List<Int> = dice.map { die -> when {
		die == 0 -> 0
		die < 0 -> -(1..(-die)).random()
		else -> (1..die).random()
	}}.let { rolled -> when {
		rolled.size < 1 -> listOf(fixed)
		fixed == 0 -> rolled
		else -> rolled + fixed
	}}

	/** Roll the term x times, each result(s) into a own list. */
	fun rollTimes(x: Int) : List<List<Int>>
		= (0 until x).map { roll() }

	/** Create new dice term with added bonus.*/
	operator fun plus(face: Int) : DiceTerm
		= DiceTerm(faces.asList() + face)

	/** Create new dice term with added dice term.*/
	operator fun plus(other: DiceTerm) : DiceTerm
		= DiceTerm(faces.toList() + other.faces.toList())

	/** Repeat this term x times. */
	operator fun times(x: Int) : DiceTerm
		= when {
			x < 0 -> (-this) * (-x)
			x == 0 -> DiceTerm(0)
			else -> faces.toList().let { fs -> DiceTerm((0 until x).map { fs }.flatten() ) }
		}

	/** Flip the whole term. */
	operator fun unaryMinus() : DiceTerm
		= DiceTerm(faces.map { -it })

	/** Create new dice term with with an additional subtracting face.*/
	operator fun minus(face: Int) : DiceTerm
		= DiceTerm(faces.asList() + (-face))

	/** Create new dice term with reduced dice term (more dice, but negativly).*/
	operator fun minus(other: DiceTerm) : DiceTerm
		= this + (-other)

	/** Collect same faces with negative and positive counts, and summarize the.
	 * For example: (3d4 - 2d4 + 4d2 - 1d3) => (1d4 + 4d2 - 1d3). */
	fun cumulated() : DiceTerm
		= facesGrouped.run {
			/* Instead [Signed Die]=(Occurance) make a map of [Absolute Die]=(signed count).
			 * Example: Instead of { 4=3, -3=1 } do  {4=3, 3=-1}. */
			DiceTerm(toList().fold(mapOf<Int, Int>()) { map, (die, cnt) ->
				val absDie = abs(die)
				val sigCnt = if (die < 0) -cnt else cnt
				map + (absDie to map.getOrDefault(absDie, 0) + sigCnt)
			})
		}.also {
			log.debug("Simplify: ($this) => ($it)")
		}

	/** String representation of simpleDice.*/
	override fun toString() : String = facesString
}
