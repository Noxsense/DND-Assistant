package de.nox.dndassistant.core.terms

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
