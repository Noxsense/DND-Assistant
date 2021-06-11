package de.nox.dndassistant.core

import de.nox.dndassistant.core.RollingTerm
import de.nox.dndassistant.core.TermVaribales
import de.nox.dndassistant.core.TermParsingException

public class RollHistory private constructor() {

	public var rolls: List<TimedRolls> = listOf()
		private set

	public data class TimedRolls(val term: RollingTerm, val roll: Int, val timestamp: Long);

	public companion object {
		// Singleton
		public val INSTANCE: RollHistory = RollHistory()

		public fun addRoll(term: RollingTerm, result: Int, timestamp: Long = System.currentTimeMillis())
			= INSTANCE.addRoll(term, result, timestamp)

		public fun roll(term: RollingTerm, variables: TermVaribales? = null)
			= INSTANCE.roll(term, variables)

		public fun rollStr(termStr: String, variables: TermVaribales? = null)
			= INSTANCE.rollStr(termStr, variables)
	}

	/** Add a new timed roll and result. */
	public fun addRoll(term: RollingTerm, result: Int, timestamp: Long = System.currentTimeMillis()) {
		this.rolls += TimedRolls(term, result, timestamp)
	}

	/** Roll a term, put it into the list of rolled terms. */
	public fun roll(term: RollingTerm, variables: TermVaribales? = null) : String
		= term.evaluate(variables)
			.also { result -> this@RollHistory.addRoll(term, result, 0) }
			.toString()

	public fun rollStr(term: String, variables: TermVaribales? = null)
		= try {
			INSTANCE.roll(RollingTerm.parse(term), variables)
		} catch (e: Exception) {
			// TODO could not parse.
			"(invalid: ${e})"
		}
}
