package de.nox.dndassistant.app

import android.view.ViewGroup
import android.widget.TextView
import android.graphics.Color

import com.google.android.material.snackbar.Snackbar

import de.nox.dndassistant.core.RollHistory
import de.nox.dndassistant.core.RollingTerm
import de.nox.dndassistant.core.TermVaribales

object Utils {

	private val logger = LoggerFactory.getLogger("Utils")

	/**
	 * Roll the given rolling term and display it.
	 * The display will fire a dissmissable Snackbar.
	 * @return a pair of the sum and the single rolls (TODO)
	 */
	public fun showRolledTerm(view: ViewGroup, label: String, term: RollingTerm, variables: TermVaribales? = null) : Pair<Int, String> {
		// get the result, auto saved in history.
		val (sum, eachRoll) = RollHistory.roll(label, term, null)

		val display = "Rolled $sum\n = {$eachRoll} with $term and $variables"

		// TODO prettier / maybe custom (wip) snackbar for rolls.
		// create display
		Snackbar
			.make(view, display, Snackbar.LENGTH_INDEFINITE)
			.setAction("OK") { /* empty dismiss. */ }
			.apply { getView().let { view ->
				// modify the design
				val tv = view.findViewById<TextView>(R.id.snackbar_text)
				val btn = view.findViewById<TextView>(R.id.snackbar_action)

				tv.setTextColor(Color.WHITE)
				btn.setTextColor(Color.WHITE)
				view.setBackgroundColor(Color.BLACK)
			}}
			.show()

		return (sum to eachRoll)
	}

	/**
	 * Roll the term parsed from the given string and display it.
	 * @return a pair of the sum and the single rolls (TODO)
	 * @see showRolledTerm
	 */
	public fun showRolledParsedTerm(view: ViewGroup, label: String, termStr: String, variables: TermVaribales? = null)
		= showRolledTerm(view, label, RollingTerm.parse(termStr), variables)
}
