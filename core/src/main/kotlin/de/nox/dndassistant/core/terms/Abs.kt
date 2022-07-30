package de.nox.dndassistant.core.terms

/** A term that returns the absolute value of the evaluated term. */
class Abs(v: RollingTerm) : UnaryRollingTerm(v) {
	public constructor(l: Int): this(Number(l));
	public constructor(l: String): this(parseBasic(l));
}
