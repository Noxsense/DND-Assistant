package de.nox.dndassistant.core

import de.nox.dndassistant.core.RollingTerm
import de.nox.dndassistant.core.TermVaribales
import de.nox.dndassistant.core.TermParsingException

public class RollHistory private constructor() {

	private var rolls: List<TimedRolls> = listOf()
		private set

	public data class TimedRolls(
		val label: String,
		val term: RollingTerm,
		val variables: TermVaribales?,
		val rolls: Array<Int>,
		val sum: Int,
		val timestamp: Long
	);

	public companion object {
		// Singleton
		public val INSTANCE: RollHistory = RollHistory()

		public val rolls: List<TimedRolls> get() = INSTANCE.rolls

		/**
		 * Add a new timed roll (term, result, timestamp).
		 */
		public fun addRoll(
			label: String,
			term: RollingTerm,
			variables: TermVaribales?,
			rolls: Array<Int>,
			sum: Int = rolls.sum(),
			timestamp: Long = System.currentTimeMillis()
		) {
			INSTANCE.rolls += TimedRolls(label, term, variables, rolls, sum, timestamp)
		}


		/**
		 * Roll a term, put it into the list of rolled terms.
		 */
		public fun roll(label: String, term: RollingTerm, variables: TermVaribales? = null)
			= term.evaluate(variables).let { result ->
				RollHistory.addRoll(label, term, variables, arrayOf(result), result)
				result to result.toString()
				// TODO do not return as string but as list of each rolled die
				// arrayOf(result)
			}

		/**
		 * Roll a term which is parsed from the given String.
		 * Add the results and the command to the history.
		 */
		public fun rollStr(label: String, termStr: String, variables: TermVaribales? = null)
			= try {
				RollHistory.roll(label, RollingTerm.parse(termStr), variables)
			} catch (e: Exception) {
				// TODO could not parse.
				"(invalid: ${e})"
			}
	}
}
