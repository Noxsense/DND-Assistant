package de.nox.dndassistant.core.terms

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
