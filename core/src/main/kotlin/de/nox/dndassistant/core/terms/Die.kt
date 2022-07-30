package de.nox.dndassistant.core.terms

import kotlin.math.abs

/** A simple / basic RollingTerm that is a die, which will return a random number between 1 and its max face. */
class Die(_max: Int) : BasicRollingTerm() {
	public val max: Int = abs(_max) // use the absolute value of the given.
	public val average: Double = (1 + max) / 2.0
}
