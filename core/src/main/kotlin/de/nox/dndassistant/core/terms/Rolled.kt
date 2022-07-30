package de.nox.dndassistant.core.terms

/** A term which needs to be early evaluated, it will be further handled as a simple number. */
class Rolled(v: RollingTerm) : UnaryRollingTerm(v) {
	public constructor(l: Int): this(Number(l));
	public constructor(l: String): this(parseBasic(l));
}
