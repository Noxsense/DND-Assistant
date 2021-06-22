package de.nox.dndassistant.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

import java.util.Stack
import java.util.EmptyStackException

/*
 * Initiate the Logger.
 */
private val log = LoggerFactory.getLogger("RollingTerm")

/*
 * Custom Exceptions.
 */

public abstract class TermParsingException(val msg: String)
	: Exception(msg);

/** Term represents not an basic term. */
public class NoBasicTermException(msg: String = "This Term does not represent a basic RollingTerm.")
	: TermParsingException(msg);

/** Recreating the term is not possible. Not enough terms. */
public class NotEnoughTermsException(msg: String = "Not enough terms, to operate with.")
	: TermParsingException(msg);

/** Recreating the term is not possible. Too many left over terms. */
public class NotEnoughOperatorsException(msg: String = "Not enough operatores to combine all terms")
	: TermParsingException(msg);

/** Incompplete term, for example if a opening bracket was not closed. */
public class IncompleteTermException(msg: String = "Not enough operatores to combine all terms")
	: TermParsingException(msg);

public typealias TermVaribales = (Reference) -> Int

/** The abstract super class of a RollingTerm.
 * Term
 *   := Number (as Number)
 *    | Variable or Function (as Reference)
 *    | Die
 *    | [RollingTerm] .. as an already evaluated term. (handled as Number)
 *    | RollingTerm + RollingTerm
 *    | RollingTerm - RollingTerm
 *    | RollingTerm * RollingTerm
 *    | RollingTerm / RollingTerm
 *    | RollingTerm ^ RollingTerm
 *    | max(RollingTerm, RollingTerm)
 *    | min(RollingTerm, RollingTerm)
 *    | abs(RollingTerm)
 *
 * It can be evaluated with a given map of references (or default all as zero).
 */
public abstract class RollingTerm: Comparable<RollingTerm> {

	/** Object with parser. */
	public companion object {
		val Zero = Number(0)
		val One = Number(1)
		val Negative = Number(-1)

		// most common dice
		val D1 = Die(1)
		val D2 = Die(2)
		val D4 = Die(4)
		val D6 = Die(6)
		val D8 = Die(8)
		val D10 = Die(10)
		val D12 = Die(12)
		val D20 = Die(20)
		val D100 = Die(100)

		/** Create a TermVaribales setup from a given (Name -> Int) setup. */
		public fun mapToVariables(vs: Map<String, Int>) : TermVaribales
			= { r: Reference -> vs.getOrElse(r.name) { 0 } }

		/** Parse a RollingTerm from a String.
		 * @throws NoBasicTermException if the input string does not represent a basic term.
		 * @throws NotEnoughTermsException not enough terms, too many operators.
		 * @throws NotEnoughOperatorsException terms are left over
		 * @throws IncompleteTermException unclosed brackets and parantheses
		 * @return RollingTerm
		 */
		public fun parse(input: String) : RollingTerm {
			var postfix: List<String> = listOf()
			var stack: Stack<String> = Stack()

			var rawTerm: String = ""
			var lastToken: String = ""
			var termRightBefore: Boolean = false // if the last token was a word / constant / number.

			val supportedFunctions = listOf("min", "max", "abs", "rolled", )

			log.debug("Parse:   \"$input\"")

			for (c in input) {
				/* If c is alphanumeric, add to the raw term.
				 * otherwise: add the raw term to postfix or operator stack,
				 * and see what the operator does. */
				if (c.isAlphaNumeric() || c == '.') {
					/*
					 * If the previous token also was a word/number/term, add a hidden multiplier in between
					 * => (x * y) written as "x y" (spaced words). */
					if (termRightBefore) {
						/*
						 * Push a hidden / not explictly written multiplicator.
						 * => Multiplicator is the last read token. */
						lastToken = "*" // multiplicator operation as last received token.
						termRightBefore = false
						stack.push(lastToken)
					}

					/* Alpha follows numeric: as 12d2 => do 12 * d2.
					 * => starts with number, ends with number. */
					if (!c.isNumeric() && rawTerm.isNumeric()) {
						//
						postfix += rawTerm // enqueue numeric part.
						lastToken = "*" // multiplicator operation as last received token.
						termRightBefore = false
						stack.push(lastToken)
						rawTerm = "" // reset.
					}

					rawTerm += c
					continue
				}

				/* Add raw term as operator (if function) or to postfix (of variable or number) */
				if (rawTerm.length > 0) {
					// if raw term is a function name: add to stack; otherwise to postfix.
					if (rawTerm in supportedFunctions) {
						stack.push(rawTerm)
					} else {
						postfix += (rawTerm)
						termRightBefore = true // finished the term => term was right before.
					}
					lastToken = rawTerm // token was the last term.
					rawTerm = "" // reset
				}

				/* RollingTerm ends, decide, where to put. */
				when (c) {
					'[', '(' -> {
						// last token as previous var/fun/num/operator
						// if the last token was an not an operator (var/num == not in stack), add additional (*)
						if (if (stack.isEmpty()) postfix.size > 0 else lastToken != stack.peek()) {
							log.debug("Pushed an additional (*) between \"$lastToken\" and \"$c\"")
							// Add an additional (*)
							stack.push("*")
						}

						// push allways to stack.
						stack.push(c.toString())

						// indicate to evaluate it asap
						if (c == '[') stack.push("rolled");

						termRightBefore = false
					}
					']', ')' -> {
						// dequeue all until "(" or "[" repectively is found.
						val opener = if (c == ']') "[" else "("
						try {
							var op = stack.pop()
							while (op != opener ) {
								postfix += op
								if (stack.isEmpty()) {
									break;
								} else {
									op = stack.pop()
								}
							}
						} catch (e: EmptyStackException) {
							throw IncompleteTermException("Closing \"$c\" found no opening \"$opener\".")
						}
						termRightBefore = true // as finish of a complex term.
					}
					'-', '+', '*', '/', '^' -> {
						// if last operator's precedence was [greater] or [they're equal and current's left associate]

						val (curPrec, leftAssoc) = c.toString().operatorsPrecedenceAssosicativity()

						/* If the current token is a Minus,
						 * and the last token was not a finised term (Num / Const / closing paranthesis)
						 * the previous term was meant to be negative: Push and enque (-1)*.
						 * Eg. it is not A - B or (A-C) - B, but just -B, or +-B  or --B or *-B.
						 */
						if ((c == '-' || c == '+') && !termRightBefore) {
							postfix += "${c}1"
							stack.push("*")
							lastToken = "*"
							termRightBefore = false // right before: (*)
							continue // done.
						}

						/* Pop and enqueue all previous operators, with higher+ precedences. */
						while (!stack.isEmpty()) {
							val prevOp = stack.peek()

							/* Don't pop if it was opening a term. Abort at the beginning of a term. */
							if (prevOp == "(" || prevOp == "[") {
								break
							}

							val (prePrec, _) = prevOp.operatorsPrecedenceAssosicativity()

							if (curPrec < prePrec || curPrec == prePrec && leftAssoc) {
								postfix += stack.pop() // enqueue popped
							} else {
								break // end popping.
							}
						}

						stack.push(c.toString())

						termRightBefore = false
					}
					'\t', '\n', '\r', '=' -> {
						// abort reading the term on line end or equal symbol.
						break
					}
					else -> {} // don't push to stack.
				}

				lastToken = if (c != ' ') c.toString() else lastToken
			}

			/* Enqueue the last not-then-done term. */
			if (rawTerm.length > 0) {
				postfix += rawTerm
			}

			/* Done reading in, enqueue rest of stack. */
			while (!stack.isEmpty()) {
				postfix += stack.pop()
			}

			log.debug("Postfix: $postfix")

			/*
			 * Create the actual RollingTerm (expression tree)
			 * from the parsed postfix notation.
			 */

			var termStack: Stack<RollingTerm> = Stack()

			/* Translate the postfix notation to an expression tree. */
			for (t in postfix) {
				val term = try { when (t) {
					// found unclosed brackets and parantheses
					"(", "[" -> throw IncompleteTermException("Unclosed \"$t\".")

					// pop two items for the operations or functions.
					"-", "+", "max", "min", "*", "/", "^" -> {
						// get the last operated terms to connect them
						val (i0, i1) = termStack.pop() to termStack.pop()

						when (t) {
							// simple
							"-" -> Difference(i1, i0)
							"+" -> Sum(i1, i0)

							// simple
							"max" -> Max(i1,i0)
							"min" -> Min(i1,i0)

							// recursive: pop[-2] # pop[-1]
							"*" -> Product(i1, i0)
							"/" -> Fraction(i1, i0)

							// recursive
							"^" -> Power(i1, i0) // right associative

							// otherwise: this should not happen.
							else -> throw Error("Parsed as operation/function, but it never was (left-over when-case).")
						}
					}

					// simply evaluate: pop[-1]
					"abs" -> Abs(termStack.pop())
					"rolled" -> Rolled(termStack.pop())

					// a (signed) number, a die or a reference.
					else -> parseBasic(t)

				} } catch (e: EmptyStackException) {
					throw NotEnoughTermsException("Not enough terms, to operate ($t) with.")
				}

				termStack.push(term)
			}

			val term = (termStack.pop()) // the term is the last pushed.

			/* Not all terms where operated to each other: More terms received which were needed for the operations. */
			if (!termStack.isEmpty()) {
				throw NotEnoughOperatorsException()
			}

			log.debug("Term:    $term")

			return term
		}

		/** Parse a (signed) number, a die or a reference.
		 * @throws NoBasicTermException if the input string does not represent a basic term.
		 * @return BasicRollingTerm: RollingTerm
		 */
		public fun parseBasic(input: String) : BasicRollingTerm {
			/* Try to check validity. */
			if (!input.all { it.isAlphaNumeric() || it == '+' || it == '-' }) {
				throw NoBasicTermException("Invalid Basic Term String.")
			}

			return when {
				// push as simple number.
				input.isNumeric() -> Number(input.toInt())

				// push as die
				(input[0] == 'd' || input[0] == 'D') && input.substring(1).isNumeric() -> Die(input.substring(1).toInt())

				// push as reference / variable
				else -> Reference(input)
			}
		}
	}

	/** Check if a term is contains any dice. */
	public val hasRollingDice: Boolean by lazy {
		when {
			this is Die -> true
			this is UnaryRollingTerm -> this.value.hasRollingDice
			this is BinaryRollingTerm -> this.left.hasRollingDice || this.right.hasRollingDice
			else -> false
		}
	}

	/** Check if a term is contains any references. */
	public val hasReference: Boolean by lazy {
		when {
			this is Reference -> true
			this is UnaryRollingTerm -> this.value.hasReference
			this is BinaryRollingTerm -> this.left.hasReference || this.right.hasReference
			else -> false
		}
	}

	// public val
	public val postorderedTerms: List<RollingTerm> by lazy {
		when {
			this is BasicRollingTerm -> listOf(this)
			this is UnaryRollingTerm -> value.postorderedTerms
			this is BinaryRollingTerm -> left.postorderedTerms + right.postorderedTerms
			else -> listOf()
		}
	}

	/** Check if the term is without any references or dice. */
	public val isTrueNumeric: Boolean by lazy {
		when (this) {
			is Number -> true
			is UnaryRollingTerm -> this.value.isTrueNumeric
			is BinaryRollingTerm -> this.left.isTrueNumeric && this.right.isTrueNumeric
			else -> false
		}
	}

	/** Check if this term can be handled as a number, at this point it is evaluating. */
	public val isNumericLike: Boolean by lazy {
		when (this) {
			is Die -> false
			is Number, is Reference -> true // simple basics
			is Rolled, is Abs, is Min, is Max -> true // at this point as numbers
			is UnaryRollingTerm -> this.value.isNumericLike // if value is
			is BinaryRollingTerm -> this.left.isNumericLike && this.right.isNumericLike // if left and right are.
			else -> false
		}
	}

	/** Show the operations and terms. */
	override public fun toString() : String
		= toString(null)

	/** Show the term, but set the variables with the demanded values / functions. */
	public fun toString(variables: TermVaribales? = null): String
		= when {
			this is Number -> "$value"
			this is Die -> "d$max"
			this is Reference -> when {
				variables == null -> name
				else -> "{$name=${variables(this)}}"
			}
			this is Abs -> "abs(${value.toString(variables)})"
			this is Rolled -> "[${value.toString(variables)}]"
			this is UnaryRollingTerm -> "(${value.toString(variables)})"

			this is BinaryRollingTerm -> {
				(left.toString(variables) to right.toString(variables)).let { (l, r) ->
					when (this) {
						is Min -> "min($l, $r)"
						is Max -> "max($l, $r)"

						is Difference -> "($l - $r)"
						is Sum -> "($l + $r)"

						is Fraction -> "($l / $r)"
						is Product -> "($l * $r)"

						is Power -> "($l ^ $r)"

						else -> "($l ? $r)"
					}
				}
			}

			else -> toString()
		}

	/** Get the expected average value. */
	public fun average(variables: TermVaribales = { _ -> 0 }) : Double
		= (min(variables) + max(variables)) / 2.0

	/** Get the minimal expected value. */
	public fun min(variables: TermVaribales = { _ -> 0 }) : Int
		= when (this) {
			is Number -> this.value
			is Reference -> variables(this)
			is Die -> 1

			is Abs -> this.value.min(variables) // TODO min of abs([-x ... +y])
			is Rolled -> this.value.min(variables) // TODO min of rolled([-x ... +y])
			is UnaryRollingTerm -> this.value.min(variables)

			is BinaryRollingTerm -> {
				val (lMin, lMax) = left.min(variables) to left.max(variables)
				val (rMin, rMax) = right.min(variables) to right.max(variables)

				when (this) {
					is Min -> min(lMin, rMin) // min by definition
					is Max -> max(lMin, rMin) // both minimized, take the max.

					is Sum -> lMin + lMin // min + min
					is Difference -> lMin - rMax // min - max

					// TODO (anti-thought) (2).min / (-2,1).max => 2 / 1 = 2 instead of (2)/(-2) = -1
					// i can still do listOf(lMin/rMin, lMin/rMax, lMax/rMin, lMax/rMax).min()!!
					is Fraction -> {
						when {
							right.equals(Zero) -> throw ArithmeticException("Dividing by the evaluated Zero (without any die or reference).")
							lMax == lMin && rMax == rMin && rMax != 0 -> lMin / rMax
							rMin != 0 && rMax == 0 -> listOf(lMin / rMin, lMax / rMin).minOrNull()!!
							rMin == 0 && rMax != 0 -> listOf(lMin / rMax, lMax / rMax).minOrNull()!!
							rMax == 0 && rMin == 0 -> rMin // both variants equal 0, handle as 1
							else -> listOf(lMin/rMin, lMin/rMax, lMax/rMin, lMax/rMax).minOrNull()!!
						}
					}

					// TODO e (anti-thought) e.g. (-5, 3) * (-5, 3) = (-5)(-5) = +25 (but it can be -15)
					// i can still do listOf(lMin*rMin, lMin*rMax, lMax*rMin, lMax*rMax).min()!!
					is Product -> listOf(lMin*rMin, lMin*rMax, lMax*rMin, lMax*rMax).minOrNull()!!

					is Power -> when {
						lMin == 0 -> 0
						rMin == 0 || lMin == 1 -> 1
						else -> 0 // TODO
					}

					else -> min(left.min(variables), right.min(variables))
				}
			}

			else -> 0
		}

	/** Get the maxim expected value. */
	public fun max(variables: TermVaribales = { _ -> 0 }) : Int
		= when (this) {
			is Number, is Reference -> this.min(variables)
			is Die -> this.max // max face.

			is Abs -> value.max(variables) // TODO max of abs([-x ... +y])
			is Rolled -> value.max(variables) // TODO max of rolled([-x ... +y])
			is UnaryRollingTerm -> value.max(variables)

			is BinaryRollingTerm -> {
				val (lMin, lMax) = left.min(variables) to left.max(variables)
				val (rMin, rMax) = right.min(variables) to right.max(variables)

				when (this) {
					is Min -> min(lMax, rMax) // both maximised, Min takes the smallest.
					is Max -> max(lMax, rMax) // both maximised, take the max.

					is Sum -> lMax + lMax // max + min
					is Difference -> lMax - rMin // max - min

					// TODO (anti-thought) (2).min / (-2,1).max => 2 / 1 = 2 instead of (2)/(-2) = -1
					// i can still do listOf(lMin/rMin, lMin/rMax, lMax/rMin, lMax/rMax).max()!!
					is Fraction -> {
						when {
							right.equals(Zero) -> throw ArithmeticException("Dividing by the evaluated Zero (without any die or reference).")
							lMax == lMin && rMax == rMin && rMax != 0 -> lMin / rMax
							rMin == 0 && rMin != 0 -> listOf(lMin / rMin, lMax / rMin).maxOrNull()!!
							rMin != 0 && rMin == 0 -> listOf(lMin / rMax, lMax / rMax).maxOrNull()!!
							rMax == 0 && rMin == 0 -> rMin // both variants equal 0, handle as 1
							else -> listOf(lMin/rMin, lMin/rMax, lMax/rMin, lMax/rMax).maxOrNull()!!
						}
					}

					// TODO e (anti-thought) e.g. (-5, 3) * (-5, 3) = (-5)(-5) = +25 (but it can be -15)
					// i can still do listOf(lMin*rMin, lMin*rMax, lMax*rMin, lMax*rMax).max()!!
					is Product -> listOf(lMin*rMin, lMin*rMax, lMax*rMin, lMax*rMax).maxOrNull()!!

					is Power -> 0 // TODO

					else -> min(left.min(variables), right.min(variables))
				}
			}

			else -> min(variables)
		}

	/** Add another Rolling Term. */
	public operator fun plus(other: RollingTerm) : RollingTerm
		= Sum(this, other)

	/** Add another Rolling Term thas is a number. */
	public operator fun plus(other: Int) : RollingTerm
		= Sum(this, other)

	/** Add another simple term that needs to be parsed.
	 * @throws TermParsingException if `other` is an invalid BasicRollingTerm. */
	public operator fun plus(other: String) : RollingTerm
		= Sum(this, parseBasic(other))

	/** Substract another Rolling Term. */
	public operator fun minus(other: RollingTerm) : RollingTerm
		= Difference(this, other)

	/** Substract another Rolling Term that is a number. */
	public operator fun minus(other: Int) : RollingTerm
		= Difference(this, other)

	/** Subtraction another simple term that needs to be parsed.
	 * @throws TermParsingException if `other` is an invalid BasicRollingTerm. */
	public operator fun minus(other: String) : RollingTerm
		= Difference(this, parseBasic(other))

	/** Multiply by another Rolling Term. */
	public operator fun times(other: RollingTerm) : RollingTerm
		= Product(this, other)

	/** Multiply by another Rolling Term that is a number. */
	public operator fun times(other: Int) : RollingTerm
		= Product(this, other)

	/** Multiplication by another simple term that needs to be parsed.
	 * @throws TermParsingException if `other` is an invalid BasicRollingTerm. */
	public operator fun times(other: String) : RollingTerm
		= Product(this, parseBasic(other))

	/** Divide by another Rolling Term. */
	public operator fun div(other: RollingTerm) : RollingTerm
		= Fraction(this, other)

	/** Divide by another Rolling Term that is a number. */
	public operator fun div(other: Int) : RollingTerm
		= Fraction(this, other)

	/** Divide by another  another simple term that needs to be parsed.
	 * @throws TermParsingException if `other` is an invalid BasicRollingTerm. */
	public operator fun div(other: String) : RollingTerm
		= Fraction(this, parseBasic(other))

	/** Just the term itself. */
	public operator fun unaryPlus() : RollingTerm
		= this

	/** Invert signs. */
	public operator fun unaryMinus() : RollingTerm
		= when {
			this is Number -> Number(-this.value) // (-value)
			else -> Product(-1, this) // (-1) * term
		}

	/** Get the last evaluated value. */
	private var lastEval: Int? = null
	private var lastRoll: List<Pair<Die, Int>> = listOf()

	public fun evaluate(variables: TermVaribales? = null) : Int
		 = evaluateIntern(variables = variables ?: { _ -> 0 }, rolled = listOf()).let { (result, ds) ->
			// log.info("Rolled dice ($this): $ds ==> ($result)")
			lastEval = result
			lastRoll = ds
			result
		}

	/** Evaluate the term, roll the dice, insert the variables.
	 * Return sum and maybe recently rolled dice. */
	// TODO keep and show the rolled dice, eg. nat20 or all.

	// internal evaluate => do the methods, tail the rolled dice.
	//
	private fun evaluateIntern(variables: TermVaribales = { _ -> 0 }, rolled: List<Pair<Die, Int>> = listOf()) : Pair<Int, List<Pair<Die,Int>>> {
		return when {
			// already evaluated pure number.
			this.isTrueNumeric && lastEval != null -> lastEval!! to rolled

			this is Number -> value to rolled // constant
			this is Reference -> (variables(this)) to rolled // get

			// roll
			this is Die -> (1 .. max).random().let { v -> v to (rolled + (this to v)) } // append to rolled.

			this is BasicRollingTerm -> 0 to rolled

			this is Abs -> value.evaluateIntern(variables, rolled).let { (v,rs) -> abs(v) to rs }
			this is Rolled -> this.value.evaluateIntern(variables, rolled)
			this is UnaryRollingTerm -> value.evaluateIntern(variables, rolled)

			this.summandsWith(variables).size < 1 -> {
				0 to rolled
			}

			this.summandsWith(variables).size > 1 -> {
				log.debug("  ... .. roll the next sum: ${summandsWith(variables)}")
				this.summandsWith(variables)
					 .map { s -> s.evaluateIntern(variables, rolled) } // [(Sum, Rolled)]
					 .reduce { (sum, sumRolled), (s, sRolled) -> (sum + s) to (sumRolled + sRolled) } // (Sum, Rolled)
			}
			// otherwise this is probably a product / fraction / power which couldn't be unfolded.

			// maybe write product to sum.
			// TODO with late unfolding (with variables)
			(this is Product && (this.left.isNumericLike || this.right.isNumericLike)) -> {
				/*
				 * One of the factors is not rolling a dice, aka it has a valid number at this point.
				 * Try to evaluated (number * term) as repeated (term + term + ...).
				 * Do not evaluate the term before repeating.
				 */
				val (numPre, term) = when {
					this.left.isNumericLike -> left.evaluateIntern(variables, rolled) to right
					else -> right.evaluateIntern(variables, rolled) to left
				}

				val (num, numRs) = numPre

				val rolledWithNum = rolled + numRs

				log.debug("TODO late unfolding.")

				when {
					//  eliminate here
					num == 0 -> 0 to rolledWithNum

					// don't reroll the then-already-rolled / handle-as-num value.
					term.isNumericLike -> term.evaluateIntern(variables, rolled).let { (v,rs) -> (num * v) to (rolled + rs) }

					// just term
					num == 1 -> term.evaluateIntern(variables, rolled)

					// post negate
					num == -1 -> term.evaluateIntern(variables, rolled).let { (v, rs) -> (-v) to (rolledWithNum + rs) }

					// XXX fix to tail the sums.
					// log.debug("#TODO: Late unfoldung products `($num) * ($term)`.")
					else -> (1..abs(num))
						.map { _ -> term.evaluateIntern(variables, rolled) } // repeating => { (int, rolls) }
						.let { unfolded ->
							val (sum, sumRs) = unfolded.reduce { (sum, sumRs), (s, sRs) -> (sum + s) to (sumRs + sRs) }

							log.debug("#TODO: Late unfoldung products `($num) * ($term)`.")

							when {
								num < 0 -> -sum to (sumRs + rolledWithNum)
								else -> +sum to (sumRs + rolledWithNum)
							}
						}
				}
			}

			// evaluate min / max / sum / product / power
			this is BinaryRollingTerm -> {
				/* Can be directly evaluated.
				 * evaluate first left and right.
				 */
				val (leftEval, leftRolled) = left.evaluateIntern(variables)
				val (rightEval, rightRolled) = right.evaluateIntern(variables)

				/* Calculate them repsectively. */
				when (this) {
					is Min -> min(leftEval, rightEval)
					is Max -> max(leftEval, rightEval)

					is Difference -> leftEval - rightEval
					is Sum -> leftEval + rightEval

					is Fraction -> leftEval / rightEval // attention, rigthEval == 0!
					is Product -> leftEval * rightEval

					is Power ->  leftEval.toDouble().pow(rightEval.toDouble()).toInt()

					else -> 0
				} to (rolled + leftRolled + rightRolled)
			}
			else -> 0 to rolled
		}
		// TODO (idea) roll the dice, insert them like the variables., get/pop first matching, etc.
	}

	/** Fetch all dice and their recent rolls of this term. */
	public fun getRecentlyRolled() : List<Pair<Die, Int>>
		= lastRoll

	override public fun compareTo(other: RollingTerm) : Int
		= this.average().compareTo(other.average())

	override public fun equals(other: Any?) : Boolean
		= other != null && (
			// number can be equal with an integer.
			(this is Number && other is Int && this.value == other) ||

			// rolling term equals rolling term
			(other is RollingTerm && when {
				// basic
				this is Number && other is Number -> this.value == other.value
				this is Reference && other is Reference -> this.name.equals(other.name, ignoreCase = true)
				this is Die && other is Die -> this.max == other.max

				// unary functions and operations
				(this is Abs && other is Abs) || (this is Rolled && other is Rolled) -> {
					(this as UnaryRollingTerm).value == (other as UnaryRollingTerm).value
				}

				// binary functions and operations
				this is BinaryRollingTerm && other is BinaryRollingTerm -> {
					// same type.
					(when (this) {
						is Max -> other is Max
						is Min -> other is Min

						is Difference -> other is Difference // not comm
						is Sum -> other is Sum

						is Fraction -> other is Fraction // not comm
						is Product -> other is Product

						is Power -> other is Power // not comm

						else -> false
					} && (
						// left is left, right is right
						(this.left == other.left && this.right == other.right) ||

						// if comm -> maybe alternatively: left = right and right is left
						(
							this.isCommutative && other.isCommutative &&
							this.left == other.right && this.right == other.left
						)
					))
				}
				else -> false
			}) ||

			// sum and differences
			((this is Sum || this is Difference) && (other is Sum || other is Difference)).run {
				if (this) {
					// compare summands, simplify numeric summands even more.
					val a = this@RollingTerm.summands.partition { it.isTrueNumeric }
					val b = (other as RollingTerm).summands.partition { it.isTrueNumeric }

					log.debug("Compare summands: $a == $b")

					// simplify numeric part
					val aNumSummand = a.first.fold(0) { eval, e -> eval + e.evaluate()}
					val bNumSummand = b.first.fold(0) { eval, e -> eval + e.evaluate()}

					val numSummandsEqual = aNumSummand == bNumSummand

					// sorted summand lists should be the same.
					val restSummandsEqual = a.second == b.second

					((numSummandsEqual) && restSummandsEqual)
				} else {
					false
				}
			}
		)

	public fun equalsAlgebraically(other: RollingTerm) : Boolean
		// TODO (2021-02-19) implement algebraic equality
		= other == this

	public val simple: Pair<RollingTerm, Boolean> by lazy {
		this.simplify()
	}

	public val dice: (TermVaribales?) -> List<Die> by lazy {
		// create a function that can return the summands.
		// try to keep reference, except it's
		{ vars: TermVaribales? -> when {
			this is Die -> listOf(this)
			this is BasicRollingTerm -> listOf()
			this is UnaryRollingTerm -> value.dice(vars)
			this.summandsWith(vars).size != 1 -> {
				log.debug("Get Dice count for term: $this")
				log.debug("Use summands to find the dice count: ${this.summandsWith(vars)}")
				summandsWith(vars).flatMap { s -> s.dice(vars) }
			}
			this is BinaryRollingTerm -> left.dice(vars) + right.dice(vars)
			else -> listOf()
		}}
	}

	/** The Summands of the RollingTerm, aka write this as sum. */
	public val summands: List<RollingTerm> by lazy {
		when {
			// ((a + b) + (c + d)) => [a,b,c,d]
			this is Sum -> left.summands + right.summands

			// ((a + b) - (c + d)) => [a,b,-c,-d]
			this is Difference -> left.summands + right.summands.map { -it }

			// (3 * (a + b)) => [(a + b), (a + b), (a + b)]
			// ! Do not unfold `[Term]` (= an already rolled term), handle it as number.
			this is Product && (left.isTrueNumeric || right.isTrueNumeric) -> {
				log.debug("Summands of $this: from a product. with true numerics.")
				log.debug("> left:  (${get(0)!!.isTrueNumeric}) $left")
				log.debug("> right: (${get(1)!!.isTrueNumeric}) $right")
				log.debug("v Eval left and right.")

				val (n, term) = when {
					left.isTrueNumeric -> left.evaluate() to right
					else -> right.evaluate() to left
				}

				log.debug("> n = $n, term = $term.")

				when {
					// no summand left.
					// 0 * term = 0
					n == 0 -> listOf()

					// simple anchors.
					n == 1 -> term.summands
					n == -1 -> term.summands.map { -it } // late negative expansion.

					// do not expand the rolled term, handle as number at this point.
					// => one is true numeric, one is already rolled.
					// n * [term] = n * [term]
					term is Rolled -> listOf(this)

					// repeated term.
					// (+/- n) * (term) = (+/- term) + (+/- term) ... + (+/- term)
					n < 0 -> (1 .. -n).flatMap { (-term).summands }
					else -> (1 .. n).flatMap { term.summands }
				}
			}

			// (a) ^ (b) = (a)* .. *(a)
			this is Power && right.isTrueNumeric -> right.evaluate().let { exponent -> when {
				exponent < 0 -> Fraction(1, Power(left, -exponent)).summands
				exponent == 0 -> One.summands
				exponent == 1 -> left.summands
				else -> (1..exponent).fold<Int, RollingTerm>(One) { b, _ -> b * left }.summands
			}}

			// (a^b) => {(a^b)}, (abc) -> {abc}, min(a,b) => {min(a,b)}
			else -> listOf(this) // summand with just self.
		}.sorted()
	}

	/** The Summands of the RollingTerm, depending of the given variables.
	 * More or less a function, that returns the summands for a given maping of variables. */
	public val summandsWith: (TermVaribales?) -> List<RollingTerm> by lazy {
		// create a function that can return the summands.
		{ variables: TermVaribales? ->
			when {
				// no variable setup.
				variables == null -> {
					log.debug("No change variables (variables == null).")
					this.summands
				}

				// replace reference with evaluated number.
				this is Reference -> {
					log.debug("replaced variabels.")
					listOf(this.refToNum(variables))
				}

				// nothing to replace
				this is BasicRollingTerm -> {
					log.debug("No change variables (BasicRollingTerm).")
					this.summands
				}

				// replace all references with numbers.
				// get summands of the "new evaluated term".
				// this is UnaryRollingTerm -> this.value.summandsWith(variables)
				else -> {
					log.debug("Replace references with variables, unreferenced summands: $summands")
					this.summands
						.flatMap { summand ->
							log.debug("summand: $summand")
							if (summand.hasReference) {
								log.debug("> Replace references.")
								log.debug("> Term postordered terms: ${this.postorderedTerms}")
								summand.refToNum(variables).summands
							} else {
								// log.debug("> Keep unchanged.")
								// java.lang.OutOfMemoryError
								listOf(summand)
							}
						}.also { log.debug("> $this >> $it ")}
				}
			}
		}
	}

	/** Replace all References with their given value, if available. */
	public fun refToNum(variables: TermVaribales?) : RollingTerm
		= when (this) {
			is Reference -> Number(variables?.invoke(this) ?: 0)
			is BasicRollingTerm -> this

			is Rolled -> Rolled(value.refToNum(variables))
			is Abs -> Abs(value.refToNum(variables))

			is Product -> left.refToNum(variables) * right.refToNum(variables)
			is Fraction -> left.refToNum(variables) / right.refToNum(variables)
			is Power -> Power(left.refToNum(variables), right.refToNum(variables))
			is Max -> Max(left.refToNum(variables), right.refToNum(variables))
			is Min -> Min(left.refToNum(variables), right.refToNum(variables))

			else -> this // ??? TODO how to keep relation?
		}


	/** Try to unfold into simple operations (+|-) and numbers, if possible.
	 * Try to reduce as most as possible.  */
	public fun simplify(): Pair<RollingTerm, Boolean>
		// TODO
		// 1. Eleminate so they become sums (mulitplicationd, differences, products)
		// 1.1 Power to Product, if possible
		// 1.2 Division to Product
		// 1.2 Product to Sum
		// 1.3 Difference to Sum
		//
		// log.debug("The term to simplify: ($this)")
		// return simplified term and true, if changes were existing.
		//
		// XXX clean up.
		//
		= when {
			this is BasicRollingTerm -> this to false

			this is Abs -> value.simplify().let { (v, changed) -> Abs(v) to changed }
			this is Rolled -> value.simplify().let { (v, changed) -> Rolled(v) to changed }

			// min ot max of one value, drop function and simplify value
			this is Min && left == right -> left.simplify().first to true
			this is Max && left == right -> left.simplify().first to true

			// Only Sums ans Negatives: Power to Product
			this is Power && right.isTrueNumeric -> right.evaluate().let { n -> when {
					n < 0 -> (One / Power(left, -n)) // 1:(b^h)
					n == 0 -> One // a^0 = 1
					n == 1 -> left
					else -> (1..n).fold<Int, RollingTerm>(One) { b, _ -> Product(b, left) }
				}.simplify().first to true
			}

			(this is Sum || this is Difference) && this.summands.size > 2 -> {
				summands.reduce { sum, s -> sum + s } to true
			}

			this.summands.size > 1 -> {
				summands.reduce { sum, s -> sum + s } to true
			}

			// expand multiplication to sum.
			// basic term is variable, it cannot yet be simplified.
			// 2 * (3d6 + 2) = (3d6 + 2) + (3d6 + 2) = ...
			// a * (3d6 + 2) = a (3d6 + 2) = ...
			this is Product && (left.isTrueNumeric || right.isTrueNumeric) -> {
				val (n, term) = when {
					left.isTrueNumeric -> left.evaluate() to right
					else -> right.evaluate() to left
				}

				log.debug("Simplified the product (numeric) to: ($this)  \u21d2  (($n) * ($term))")

				when {
					// evaluate if both are numbers.
					term.isTrueNumeric -> Number(n * term.evaluate())

					n < 0 -> ((term * (-1)) * n) // (n * (-term))
					n == 0 -> Zero
					n == 1 -> term
					else -> (2..n).fold<Int, RollingTerm>(term){ b, _ -> b + term }
				}.simplify().first to true
			}

			// order the summands.

			// multiply with number: split up.
			// multiply with term: unfold and simplify
			//
			// ((a+b) (c+d)) = (a+b)*c + (a+b)*d = (ac + bc) + (ad +bd)
			// ((a*b)) * c = (a*b*c)
			this is Product -> {
				val leftSummands = left.summands
				val rightSummands = right.summands

				// left or right are sums => can be simplified.
				// ((a+b) (c+d)) = (a+b)*c + (a+b)*d = (ac + bc) + (ad +bd)
				// zip summands.
				if (leftSummands.size > 1 && rightSummands.size > 1) {
					(leftSummands // (a+b) * (c+d)
						.flatMap { lS -> rightSummands.map { rS -> (lS * rS) } } // a(c+d) + b(c+d)
						.reduce { sum, e -> sum + e } // ac+ ad + bc + bd
						.also { log.debug("Simplified the product (expand) to: ($this)  \u21d2  ($it)") }
					to true)
				} else {
					log.debug("Product cannot be more simplified here ($this). Simplify left and right.")
					// cannot be more simplified: Simplify next left and right.
					(left.simplify() to right.simplify()).let { (l, r) ->
						val (lv, lu) = l; val (rv, ru) = r
						((lv * rv) to (lu || ru))
					}
				}
			}

			// write differnce to sum.
			this is Difference -> { when {
				left == Zero -> (-right) // just negative (0 - right) = -right
				right == Zero -> left // just left // (left - 0) = left
				isTrueNumeric -> Number(evaluate()) // evaluate number
				else -> (left + (-right)) // (l - r) = (l + (-r))
			}.simplify().first to true }

			// 1 + 2, x + 0
			this is Sum && isTrueNumeric -> Number(evaluate()) to true // take evaluated num.
			this is Sum && left == Zero -> right.simplify().first to true
			this is Sum && right == Zero -> left.simplify().first to true

			// sum | summand chain.
			// sum sort by size.
			this is Sum -> {
				// try to simplify the summands.
				this.summands
					.map { s -> s.simplify() }
					// .sortBy { a,b -> a.compareTo(b) }
					.reduce { sum, e -> (sum.first + e.first) to (sum.second || e.second) }
					.also {
						log.debug("Simplified sum: $it")
					}
			}

			// a:b + c:b = (a+c):b

			// x:1 == x
			this is Fraction -> { when {
				right == One -> left.simplify().first to true
				else -> (left.simplify() to right.simplify()).let { (l, r) ->
					(l.first / r.first) to (l.second || r.second)
				}
			}}

			// 0:b = 0

			// a:a == 1, if not with random

			//  n*a:a = n
			// shortening (4:2) ((4*x):2) ...

			// get negative up

			// evaluate numeric product

			// sum all and put negative left

			// x * 0, x * 1

			// postpone fraction as late as possible

			else -> this to false // cannot be more simplified.
		}
}

///////////////////////////////////////////////////////////////////////////////

/** A basic term is a term that does not contain more terms. (leaf) */
public abstract class BasicRollingTerm : RollingTerm();

/** A one sided / armed RollingTerm. It contains one directly underlying term. */
public abstract class UnaryRollingTerm(val value: RollingTerm): RollingTerm() {
	/** Check if term contains a certain term. */
	public operator fun contains(t: RollingTerm) : Boolean
		= when {
			t == value -> true

			value is BasicRollingTerm -> false // if t != value, not possible

			value is BinaryRollingTerm -> t in value
			value is UnaryRollingTerm -> t in value

			else -> false
		}
}

/** A one sided / armed RollingTerm. It contains one directly underlying term. */
public abstract class BinaryRollingTerm(val left: RollingTerm, val right: RollingTerm): RollingTerm() {
	/** Check if left and right can be exchanged. */
	public val isCommutative: Boolean
		= this is Sum  || this is Product || this is Min || this is Max

	/** Check if term contains a certain term. */
	public operator fun contains(t: RollingTerm) : Boolean
		= when {
			t == left -> true
			t == right -> true

			left is BasicRollingTerm -> false // if t != left, not possible
			right is BasicRollingTerm -> false // if t != right, not possible

			left is BinaryRollingTerm -> t in left
			left is UnaryRollingTerm -> t in left
			right is BinaryRollingTerm -> t in right
			right is UnaryRollingTerm -> t in right

			else -> false
		}

	/** Get the left (0) or right (0) term. */
	public operator fun get(i: Int) : RollingTerm?
		= when (i) {
			0 -> left
			1 -> right
			else -> null
		}

	/** Get the left ("left/summand0/factor0/divisor/base/min0/min1") or right ("right/summand1/factor1/divident/exponent/min1/max1") term. */
	public operator fun get(name: String) : RollingTerm?
		= when {
			this is Sum -> when (name) {
				"left", "summand0", "1" -> left
				"right", "summand1", "2" -> right
				else -> null
			}
			this is Difference -> right
			this is Product -> right
			this is Fraction -> right
			this is Power -> right
			this is Min -> right
			this is Max -> right
			else -> null
		}
}

///////////////////////////////////////////////////////////////////////////////

/** A simple / basic Number that is an Integer. */
class Number(val value: Int): BasicRollingTerm();

/** A simple / basic RollingTerm, that will be replaced with a function that returns an Int. */
class Reference(val name: String): BasicRollingTerm();

/** A simple / basic RollingTerm that is a die, which will return a random number between 1 and its max face. */
class Die(_max: Int) : BasicRollingTerm() {
	public val max: Int = abs(_max) // use the absolute value of the given.
	public val average: Double = (1 + max) / 2.0
}

/** A term which needs to be early evaluated, it will be further handled as a simple number. */
class Rolled(v: RollingTerm) : UnaryRollingTerm(v) {
	public constructor(l: Int): this(Number(l));
	public constructor(l: String): this(parseBasic(l));
}

/** A term which adds one term to another by Addition. */
class Sum(l: RollingTerm, r: RollingTerm) : BinaryRollingTerm(l, r) {

	// use as number if given as integer..
	public constructor(l: Int, r: Int): this(Number(l), Number(r));
	public constructor(l: Int, r: RollingTerm): this(Number(l), r);
	public constructor(l: RollingTerm, r: Int): this(l, Number(r));

	public constructor(l: String, r: String): this(parseBasic(l), parseBasic(r));
	public constructor(l: String, r: RollingTerm): this(parseBasic(l), r);
	public constructor(l: RollingTerm, r: String): this(l, parseBasic(r));

	public constructor(l: Int, r: String): this(Number(l), parseBasic(r));
	public constructor(l: String, r: Int): this(parseBasic(l), Number(r));
}

/** A term which subtracts a terms from another by Subtraction. */
class Difference(l: RollingTerm, r: RollingTerm) : BinaryRollingTerm(l, r) {

	// use as number if given as integer..
	public constructor(l: Int, r: Int): this(Number(l), Number(r));
	public constructor(l: Int, r: RollingTerm): this(Number(l), r);
	public constructor(l: RollingTerm, r: Int): this(l, Number(r));

	// parse if given as string.
	public constructor(l: String, r: String): this(parseBasic(l), parseBasic(r));
	public constructor(l: String, r: RollingTerm): this(parseBasic(l), r);
	public constructor(l: RollingTerm, r: String): this(l, parseBasic(r));

	public constructor(l: Int, r: String): this(Number(l), parseBasic(r));
	public constructor(l: String, r: Int): this(parseBasic(l), Number(r));
}

/** A term which multiplies a terms to another by Multiplication. */
class Product(l: RollingTerm, r: RollingTerm): BinaryRollingTerm(l, r) {

	// use as number if given as integer..
	public constructor(l: Int, r: Int): this(Number(l), Number(r));
	public constructor(l: Int, r: RollingTerm): this(Number(l), r);
	public constructor(l: RollingTerm, r: Int): this(l, Number(r));

	public constructor(l: String, r: String): this(parseBasic(l), parseBasic(r));
	public constructor(l: String, r: RollingTerm): this(parseBasic(l), r);
	public constructor(l: RollingTerm, r: String): this(l, parseBasic(r));

	public constructor(l: Int, r: String): this(Number(l), parseBasic(r));
	public constructor(l: String, r: Int): this(parseBasic(l), Number(r));
}

/** A term which divides a terms to another by Division. */
class Fraction(l: RollingTerm, r: RollingTerm): BinaryRollingTerm(l, r) {

	// use as number if given as integer..
	public constructor(l: Int, r: Int): this(Number(l), Number(r));
	public constructor(l: Int, r: RollingTerm): this(Number(l), r);
	public constructor(l: RollingTerm, r: Int): this(l, Number(r));

	public constructor(l: String, r: String): this(parseBasic(l), parseBasic(r));
	public constructor(l: String, r: RollingTerm): this(parseBasic(l), r);
	public constructor(l: RollingTerm, r: String): this(l, parseBasic(r));

	public constructor(l: Int, r: String): this(Number(l), parseBasic(r));
	public constructor(l: String, r: Int): this(parseBasic(l), Number(r));
}

/** A term which powers a term by a term as Potenz.
 * If the exponent contains a dice, use the evaluated version.
 * If the base contains a dice (and is not already rolled), use multiplication.
 * `b^n = b * b * .. b`
 *
 * example: (3 + d3)^2 =(3 + d3)*(3 + d3) = 9 + d3*d3 + 2*(3*d3) = 9 + d3*d3 + 6*d3
 */
class Power(l: RollingTerm, r: RollingTerm): BinaryRollingTerm(l, r) {

	// use as number if given as integer..
	public constructor(l: Int, r: Int): this(Number(l), Number(r));
	public constructor(l: Int, r: RollingTerm): this(Number(l), r);
	public constructor(l: RollingTerm, r: Int): this(l, Number(r));

	public constructor(l: String, r: String): this(parseBasic(l), parseBasic(r));
	public constructor(l: String, r: RollingTerm): this(parseBasic(l), r);
	public constructor(l: RollingTerm, r: String): this(l, parseBasic(r));

	public constructor(l: Int, r: String): this(Number(l), parseBasic(r));
	public constructor(l: String, r: Int): this(parseBasic(l), Number(r));
}

/** A term that returns the maximal value of the evaluated terms.. */
class Max(l: RollingTerm, r: RollingTerm) : BinaryRollingTerm(l, r) {

	// use as number if given as integer..
	public constructor(l: Int, r: Int): this(Number(l), Number(r));
	public constructor(l: Int, r: RollingTerm): this(Number(l), r);
	public constructor(l: RollingTerm, r: Int): this(l, Number(r));

	public constructor(l: String, r: String): this(parseBasic(l), parseBasic(r));
	public constructor(l: String, r: RollingTerm): this(parseBasic(l), r);
	public constructor(l: RollingTerm, r: String): this(l, parseBasic(r));

	public constructor(l: Int, r: String): this(Number(l), parseBasic(r));
	public constructor(l: String, r: Int): this(parseBasic(l), Number(r));
}

/** A term that returns the minimal value of the evaluated terms. */
class Min(l: RollingTerm, r: RollingTerm) : BinaryRollingTerm(l, r) {

	// use as number if given as integer..
	public constructor(l: Int, r: Int): this(Number(l), Number(r));
	public constructor(l: Int, r: RollingTerm): this(Number(l), r);
	public constructor(l: RollingTerm, r: Int): this(l, Number(r));

	public constructor(l: String, r: String): this(parseBasic(l), parseBasic(r));
	public constructor(l: String, r: RollingTerm): this(parseBasic(l), r);
	public constructor(l: RollingTerm, r: String): this(l, parseBasic(r));

	public constructor(l: Int, r: String): this(Number(l), parseBasic(r));
	public constructor(l: String, r: Int): this(parseBasic(l), Number(r));
}

/** A term that returns the absolute value of the evaluated term. */
class Abs(v: RollingTerm) : UnaryRollingTerm(v) {
	public constructor(l: Int): this(Number(l));
	public constructor(l: String): this(parseBasic(l));
}

/*
 * ---------------------------------------------------------------------------
 * Supporting functions.
 */

/** Check if a char is a number. */
private fun Char.isNumeric() : Boolean
	= this in '0' .. '9'

/** Check if a char is a letter, [A-Za-z]. */
private fun Char.isAlpha() : Boolean
	= this in 'a' .. 'z' || this in 'A' .. 'Z'

/** Check if a char is either a number or a letter. */
private fun Char.isAlphaNumeric() : Boolean
	= this.isNumeric() || this.isAlpha()

/** Get the precedence and left associativy of an opartor. */
private fun String.operatorsPrecedenceAssosicativity() : Pair<Int, Boolean>
	= when (this) {
		"^" -> 4 to false
		"*", "/", "%" -> 3 to true
		"+", "-" -> 2 to true
		else -> 0 to true
	}

/* A String is numeric, if it is not empty and all chars are a number,
 * the first char can be plus or minus (sign). */
private fun String.isNumeric() : Boolean
	= (length > 0 &&
	// all chars are numeric
	( all { c -> c.isNumeric() }

	// or it starts signed, rest is numeric. (again maybe also signed)
	|| (length > 1 && (get(0) == '+' || get(0) == '-') && substring(1).isNumeric())))
