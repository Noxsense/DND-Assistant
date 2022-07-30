package de.nox.dndassistant.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// TODO (2020-10-01) Refactor !

/** Default die. */
val D1 : DiceTerm = DiceTerm.xDy(y = 1)
val D2 : DiceTerm = DiceTerm.xDy(y = 2)
val D4 : DiceTerm = DiceTerm.xDy(y = 4)
val D6 : DiceTerm = DiceTerm.xDy(y = 6)
val D8 : DiceTerm = DiceTerm.xDy(y = 8)
val D10 : DiceTerm = DiceTerm.xDy(y = 10)
val D12 : DiceTerm = DiceTerm.xDy(y = 12)
val D20 : DiceTerm = DiceTerm.xDy(y = 20)
val D100 : DiceTerm = DiceTerm.xDy(y = 100)

/* A complexer dice term with different dice and constants.
 * What is dice term: "1d4 + 1d10 + 1 + { STR } + { my_var }".
 */
class DiceTerm(_faces: List<SimpleTerm>) {
	companion object {
		private val log: Logger = LoggerFactory.getLogger("D&D Dice")

		/** An empty dice term, where no term will be rolled and without any bony. */
		public val EMPTY = DiceTerm(Num(0))

		/** Create a quick dice term with just one face. */
		fun xDy(x: Int = 1, y: Int) : DiceTerm
			= DiceTerm(Die(y)) * x

		/** Create a new DiceTerm which is just a Bonus. */
		fun bonus(n: Int) : DiceTerm
			= DiceTerm(Num(n))

		/** Create a new DiceTerm which just fetches its value from a function. */
		fun mod(str: String, f: () -> Int) : DiceTerm
			= DiceTerm(Fun(str, f))

		/** Interprete every number as face of a die. */
		fun fromDieFaces(f: Int, vararg fs: Int) : DiceTerm
			= DiceTerm((fs.toList() + f).map { face -> Die(face) })

		private val RGX_NUM = Regex("^[+-][0-9]+$") // x
		private val RGX_DIE = Regex("^([+-]*[0-9]*)[dD]([0-9]+)$") // => [1..x]
		private val RGX_VAR = Regex("^([+-]*[0-9]* *\\*? *)([a-zA-Z]\\w*)$") // signed, maybe factoriezed variable name

		/** Parse the factor. */
		private fun parseFactor(str: String) : Int
			= when (str) {
				"", "+", "+1", "1" -> 1
				"-", "-1" -> -1
				else -> str.toInt()
			}

		/** Try to parse a simple term with a factor from a given string.
		 * @throws Exception */
		fun parseSimpleTerm(str: String) : Pair<SimpleTerm, Int>
			= str.trim().let { s -> when {
				RGX_NUM.matches(s) -> Num(s.toInt()) to 1
				RGX_DIE.matches(s) -> {
					val (num, die) = RGX_DIE.find(s)!!.destructured
					(Die(die.toInt()) to parseFactor(num))
				}
				RGX_VAR.matches(s) -> {
					val (num, abi) = RGX_VAR.find(s)!!.destructured
					Fun(abi) { 0 /* place holder. */ } to parseFactor(num)
				}
				else -> throw Exception("Cannot parse SimpleTerm from \"$s\"")
			}}

		/** Parse string to DiceTerm.
		 * @param str the string to parse.
		 * @throws Error If the string term was invalid (doesn't contain a dice term)
		 * @return DiceTerm which is described by the string. */
		fun parse(str: String) : DiceTerm {
			// remove whitespaces.
			val nowhite = Regex("\\s").replace(str, "")

			/* substitute simple math equalisations, like +- or -+ */
			// TODO (2020-10-17)
			var quickFix = nowhite.replace("+-", "-").replace("-+", "-") // WORK_AROUND

			// split before operators (+ or -). Remove empty terms (like "+2").
			val terms = Regex("(?=[\\+-])").split(quickFix).filter { it != "" }

			val valid = Regex("([\\+-]?\\d*)([dD])?(\\d*)")

			/* Map terms to parsed simple dice terms.*/
			val parsedTerms =  terms.map { parseSimpleTerm(it) }

			log.debug("Parsed Terms \"$str\" => $terms => $parsedTerms")

			// avoid on "toMap()" the following example
			// [(+3, 1), (+3, +2)] = {+3 = 2)}
			// (+3,1) was replaced by (+3, +2) and not summed.
			return DiceTerm(parsedTerms.fold(mapOf<SimpleTerm, Int>()) { m, (a,x) ->
				m + (a to (m[a] ?: 0) + x)
			})
		}
	}

	/** A simple term is a comparable component the dice term exists of. */
	abstract class SimpleTerm : Comparable<SimpleTerm> {
		abstract val value: Int
		abstract fun fetchValue() : Int

		/* Compare a simple term by their main feature: the value. */
		override fun compareTo(other: SimpleTerm)
			= value.compareTo(other.value)

		/* Print with the given factor. */
		fun toStringWithFactor(x: Int) : String
			= abs(x).let {absX ->
				val factor = if (value * x < 0) -absX else absX

				when {
					this !is Fun && value == 0 -> "+0"
					this is Die -> "%+dd%d".format(factor, abs(value))
					this is Num -> "%+d".format(x * value)
					x == -1 -> "-$this"
					x == 1 -> "+$this"
					else -> "%+d %s".format(factor, this.toString())
				}
			}

		fun copy() : SimpleTerm = when {
			this is Die -> toDie()
			this is Fun -> toFun()
			this is Num -> toNum()
			else -> this
		}

		fun toNum() : Num = Num(value)
		fun toDie() : Die = Die(value)
		fun toFun() : Fun = Fun(if (this is Fun) this.label else "fun"){ this.fetchValue() }

		/** Append two simple terms to a dice term. */
		operator fun plus(other: SimpleTerm) : DiceTerm
			= DiceTerm(this, other)

		/** Subtract two terms from each other. */
		operator fun minus(other: SimpleTerm) : DiceTerm
			= DiceTerm(this, other.unaryMinus())

		/** Flip the term's value. */
		abstract operator fun unaryMinus(): SimpleTerm

		/** Get a maybe cumulated term.*/
		abstract fun cumulate(other: SimpleTerm) : DiceTerm
	}

	/* Compare a factored simple term, if it's equal to another factored simple term. */
	fun Pair<SimpleTerm, Int>.equalsFactoredTerm(other: Pair<SimpleTerm, Int>) : Boolean
		= run {
			val (terma, xa) = this
			val (termb, xb) = other
			when {
				xb == 0 && xa == 0 -> true // factor Zero, equals everything.
				terma.equals(termb) -> xa == xb // same term, then same factor.
				terma !is Fun && terma.value == 0 -> (termb !is Fun && terma.value == 0) || xb == 0 // if "Die(0)" or "Num(0)" => then also when factor 0
				termb !is Fun && termb.value == 0 -> xa == 0 // if "Die(0)" or "Num(0)" => then also when factor 0
				else -> false
			}
		}

	/** A num is a component, which returns just the fixed value. */
	class Num(override val value: Int) : SimpleTerm() {
		override fun toString() : String = "%+d".format(value)

		/* New hashcode, working on different DiceTerms. */
		override fun hashCode() : Int = (7 * 31 + "Num".hashCode()) * 31 + value

		override fun equals(other: Any?) : Boolean
			=  when {
				other == null -> false
				other is Num -> other.value == this.value
				other is Int -> other == this.value
				else -> false
			}

		override fun fetchValue() : Int = value

		override fun unaryMinus() : Num = Num(-value)

		/** If the other SimpleTerm is also a Num, get the sum of the values. */
		override fun cumulate(other: SimpleTerm) : DiceTerm
			= when {
				other is Num -> DiceTerm(Num(this.value + other.value))
				else -> DiceTerm(this, other)
			}
	}

	/** A mod is a component, which draws a changeable, but actually not random number.
	 * It has a connected lambda function. */
	class Fun(val label: String, val getter: () -> Int) : SimpleTerm() {
		override val value = 0 // it should be handled like a fixed constant.

		/* New hashcode, working on different DiceTerms. */
		override fun hashCode() : Int = (7 * 31 + "Fun".hashCode()) * 31 + label.hashCode()

		override fun toString() : String = label

		override fun equals(other: Any?) : Boolean
			= (other != null && other is Fun && other.getter == this.getter)

		/** Get the value, which will be received from the getter. */
		override fun fetchValue() : Int = getter()

		override fun unaryMinus() : Fun = Fun("-($label)") { -getter() }

		/** Just adding them side by side, even if they would fetch the same value. */
		override fun cumulate(other: SimpleTerm) : DiceTerm
			= DiceTerm(this, other)
	}

	/** A die is component, which returns a random value between 1 and its face. */
	class Die(override val value: Int) : SimpleTerm() {
		override fun toString() : String = "d${value}"

		/* New hashcode, working on different DiceTerms. */
		override fun hashCode() : Int = (7 * 31 + "Die".hashCode()) * 31 + value

		override fun equals(other: Any?) : Boolean
			= (other != null && other is Die && other.value == this.value)

		/** Get a new random value for the die on fetch value. */
		override fun fetchValue() : Int = when {
			value < 0 -> (1 .. abs(value)).random() * (-1)
			value == 0 -> 0
			else -> (1..value).random()
		}

		override fun unaryMinus() : Die = Die(-value)

		/** If the other term is also a Die, with the negative value, they neutralize each other,
		 * otherwise, they are coexisting. */
		override fun cumulate(other: SimpleTerm) : DiceTerm
			= when {
				other is Die && other.value == -this.value -> DiceTerm(listOf())
				else -> DiceTerm(this, other)
			}
	}

	/** All dice, constants, variables and simple terms inside this term. */
	val term: Array<SimpleTerm>

	/** All 'Num' summed in this term. */
	val fixedSum: Int

	/** For every term, count how often they appeared (Face: Count). */
	val facesGrouped: Map<SimpleTerm, Int>

	/** String representation for the term of this term .*/
	val facesString: String

	/** The max value which could be rolled. */
	val max: Int

	/** the min value which could be rolled. */
	val min: Int

	/** get the average value to expected from the dice term.*/
	val average : Double

	/** Construct a term with dynamic variables as term. */
	constructor(n: SimpleTerm, vararg ns: SimpleTerm) : this(ns.toList() + n)

	/** Construct a Dice Term as from a Map representing: { Face = count }.
	 * For example: { D20=1, N1=7, D4=1, D2=-5, -D4=-4, -D100=1 } => ( +1D20 +7D1 +1D4 -5D2 +4D4 + -1D100 ).*/
	constructor(appearances: Map<SimpleTerm, Int>) : this( appearances.toList()
		.flatMap { (sTerm, count) ->
			val absCnt: Int = abs(count)

			/* Make a list with (absolute count) times the maybe swapped term. */
			(0 until absCnt).map { if (count < 0) -sTerm else sTerm }
		}.toList<SimpleTerm>()
	)

	/** While initiating, the 0 will be removed, expect it would be empty term.
	 * Also simplify the ones and negative ones, save them boni extra.
	 * No further simplifications, because maybe sometimes such effects are wanted
	 * 1d4 - 1d4 => [1, -3] => -2 // or [-3, 3] instead of 0.*/
	init {
		/* Put back the cumulated fixed and random for the term. */
		term = _faces
			.filterNot { (it is Num || it is Die) && it.value == 0 }
			.let { if (it.size < 1) listOf(Num(0)) else it } // at least one item.
			.map { it.copy() }
			.sortedDescending()
			.toTypedArray()

		fixedSum = term.fold(0) { countedNums, t ->
			countedNums + (if (t is Num) t.value else  0)
		}

		/* Prepared Group and strings. */
		facesGrouped = term.fold(mapOf<SimpleTerm, Int>()) { counted, t ->
			counted + (t to counted.getOrDefault(t, 0) + 1)
		}
		facesString = facesGrouped.toList()
			.sortedByDescending { it.first }
			.joinToString(" ") { (sTerm, x) -> sTerm.toStringWithFactor(x) }

		val expected = listOf(
			term.sumBy { it.value }, // highest rolls
			term.sumBy { when {
				it !is Die || it.value == 0 -> it.value
				it.value < 0 -> -1
				else -> 1
			}} // lowest rolls
		)

		max = expected.maxOrNull()!!
		min = expected.minOrNull()!!

		average = expected.sum() / 2.0

		log.debug("New DiceTerm(${term.toList()}) => {max: $max, min: $min, avg: $average, grouped: $facesGrouped, fixed: $fixedSum string: $facesString }")
	}

	/** Implement DiceTerm[t] = num with default 0, if not available.
	 * Attention: The requested simple term must fit this dice term fingerprint. */
	operator fun get(t: SimpleTerm) : Int
		= facesGrouped.filterKeys { when {
			t is Fun && it is Fun -> t.label == it.label
			t !is Fun && it !is Fun -> t == it
			else -> false
		}}.values.sum()

	/** Check, if a term contains a function term with a given label.*/
	operator fun contains(label: String) : Boolean
		= term.any { it is Fun && it.label == label }

	/** Check, if a term contains at least given constant.*/
	operator fun contains(x: Int) : Boolean
		= (x < 0 == fixedSum < 0) && abs(x) <= abs(fixedSum)

	/** Check, if a term contains a die with a given face count.*/
	operator fun contains(x: SimpleTerm) : Boolean
		= x in term

	/** Check, if a term contains a die with a given face count.*/
	operator fun contains(termFactor: Pair<SimpleTerm, Int>) : Boolean
		= termFactor.let { (a, x) -> when {
			x == 0 -> true // factor is null.
			else -> this[a].let { y ->
				// this[a] = y is at least x (covering x) (as absolute value), with same sign.
				(x < 0 == y < 0) && abs(x) <= abs(y)
			}
		}}

	/** Check, if the term contains all dice given by the other term.
	 * For example: (4d8) is in (12d8 + 1). */
	operator fun contains(other: DiceTerm) : Boolean {
		/* Check if all non constants are at least as much available as the other. */
		val containsAllNoNums = other.facesGrouped.toList().all {
			/* The term is at least in this. */
			(it.first is Num || it in this)
		}

		/* If other has constants, these constants must be at least like those (in sum). */
		val containsFixedSums = other.term.filter { it is Num }
			.let { constantsNeeded -> when {
				// no constants needed
				(constantsNeeded.size < 1) -> true

				// those constants are covered (in sum)
				// same sign, absolute those are covered
				else -> (other.fixedSum < 0 == this.fixedSum < 0) && abs(other.fixedSum) <= abs(this.fixedSum)
			}}

		return (containsFixedSums && containsAllNoNums)
	}

	/** Check equality to another term.*/
	override fun equals(other: Any?) : Boolean
		= when {
			other == null -> false
			other !is DiceTerm -> false
			else -> (this in other) && (other in this) // mutally including the other's term.
		}

	/** Roll every dice one and constants in this term.
	  *@return List<Int> containing each rolled result and the fixed constants. */
	fun roll() : List<Int> = term.map { it.fetchValue() }

	/** Roll the term x times, each result(s) into a own list. */
	fun rollTimes(x: Int) : List<List<Int>>
		= if (x < 1) listOf<List<Int>>() else (0 until x).map { roll() }

	/** Roll the term x times, take the first y rolls. */
	fun rollTake(takeFirst: Int, maxRolls: Int) : List<List<Int>>
		= rollTimes(maxRolls).take(takeFirst)

	/** Roll the term x times, the one with the maximum sum. */
	fun rollTakeMax(maxRolls: Int = 2) : List<Int>
		= rollTimes(maxRolls).maxByOrNull { it.sum() } ?: listOf<Int>()

	/** Roll the term x times, the one with the mininum sum. */
	fun rollTakeMin(maxRolls: Int = 2) : List<Int>
		= rollTimes(maxRolls).minByOrNull { it.sum() } ?: listOf<Int>()

	/** Create new dice term with added fixed number (fixed).*/
	operator fun plus(bonus: Int) : DiceTerm
		= DiceTerm(term.asList() + Num(bonus))

	/** Create new dice term with added simple term.*/
	operator fun plus(t: SimpleTerm) : DiceTerm
		= DiceTerm(term.asList() + t)

	/** Create new dice term with added dice term.*/
	operator fun plus(other: DiceTerm) : DiceTerm
		= DiceTerm(term.toList() + other.term.toList())

	/** Repeat this term x times. */
	operator fun times(x: Int) : DiceTerm
		= when {
			term.size < 1 || x == 0 -> DiceTerm.EMPTY
			x < 0 -> (-this) * (-x)
			else -> term.toList().let { fs -> DiceTerm((0 until x).map { fs }.flatten() ) }
		}

	/** Flip the whole term. */
	operator fun unaryMinus() : DiceTerm
		= DiceTerm(term.map { -it }) // flip all inside.

	/** Create new dice term with with an additional subtracting bonus(?).*/
	operator fun minus(bonus: Int) : DiceTerm
		= DiceTerm(term.asList() + Num(-bonus))

	/** Create new dice term with with an additional subtracting simple term.*/
	operator fun minus(t: SimpleTerm) : DiceTerm
		= DiceTerm(term.asList() + (-t))

	/** Create new dice term with reduced dice term (more dice, but negatively).*/
	operator fun minus(other: DiceTerm) : DiceTerm
		= this + (-other) // add other, where all is internally flipped.

	/** Get a new DiceTerm, with given filter applied. */
	fun filter(f: (SimpleTerm) -> Boolean) : DiceTerm
		= DiceTerm(term.filter(f))

	/** Get a new DiceTerm, with given map applied. */
	fun map(f: (SimpleTerm) -> SimpleTerm) : DiceTerm
		= DiceTerm(term.map(f))

	/** Collect same term with negative and positive counts, and summarize the.
	 * For example: (3d4 - 2d4 + 4d2 - 1d3) => (1d4 + 4d2 - 1d3). */
	fun cumulated() : DiceTerm
		= DiceTerm(facesGrouped.toList().fold(mapOf<SimpleTerm, Int>()) { map, (a, x) ->
		/* Instead [Signed Die]=(Occurrence) make a map of [Absolute Die]=(signed count).
		 * Example: Instead of { 4=3, -3=1 } do  {4=3, 3=-1}.
		 * Term: x1*a1 + x2*a1 + x3*a2 = (x1+x2)*a1 + x3*a2.
		 * See: constructor(appearances: Map<SimpleTerm, Int>). */
			val absA = when {
				a !is Fun && a.value < 0 -> -a
				else -> a
			}
			val sigX = if (a.value < 0) -x else x
			map + (absA to (map[absA] ?: 0) + sigX)
		})

	/** String representation of dice term.*/
	override fun toString() : String = facesString // pre-calculated
}
