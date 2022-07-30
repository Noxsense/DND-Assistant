package de.nox.dndassistant.core.terms

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
