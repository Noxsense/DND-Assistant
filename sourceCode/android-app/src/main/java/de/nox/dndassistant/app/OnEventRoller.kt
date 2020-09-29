package de.nox.dndassistant.app

import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnKeyListener
import android.widget.TextView
import android.widget.Toast

import de.nox.dndassistant.core.DiceTerm

class OnClickRoller(
	val diceTerm: DiceTerm,
	var reason: String = diceTerm.toString(),
	var rollHistory: List<Pair<Long, Pair<Int,String>>>? = null
) : View.OnClickListener {

	/* Return the last roll, this Roller returned. */
	var roll: Int = -1
		private set

	/* Roll, when a (DiceView) was clicked. */
	override fun onClick(view: View) {
		/* Roll the result. */

		val roll = diceTerm.roll()

		Log.d("D&D Roller", "Rolled $roll => $roll ($reason)")

		/* Add to roll history: <Timestamp, <Result, Reason>>. */
		val ts: Long = System.currentTimeMillis()
		rollHistory?.plus(ts to (roll to "$roll <== $reason"))

		/* Show the result. */
		toastRoll(view.getContext(), roll, "$roll \u21d0 $reason")
	}
}

class OnKeyEventRoller(
	var rollHistory: List<Pair<Long, Pair<Int,String>>>? = null
) : View.OnKeyListener {
	
	/** Parse TextView text to DiceTerm and return rolled result. */
	override fun onKey(view: View, code: Int, event: KeyEvent) : Boolean {
		/* Don't do anything, if this wasn't a TextView (or Heritance). */
		if (view !is TextView) {
			return false
		}

		/* Skip on not Enter. */
		if (!isConfirmedEnter(event, code)) {
			return false
		}

		/* On enter, parse and roll the term inserted. */

		/* Parse the current dice term. */
		val term = view.text.toString().trim()
		val diceTerm = DiceTerm.parse(term)

		/* Roll the result. */
		val roll = diceTerm.roll()

		Log.d("D&D Roller", "Rolled $roll => $roll ($term)")

		/* Add to roll history: <Timestamp, <Result, Reason>>. */
		val ts: Long = System.currentTimeMillis()
		rollHistory?.plus(ts to (roll to (
			"$roll" + if (term.length > 0) " <== $term" else "")))

		toastRoll(view.getContext(), roll, "$roll \u21d0 $term")
		return true
	}

	private fun isConfirmedEnter(event: KeyEvent, code: Int) : Boolean
		= event.action == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_ENTER
}

/** Show a number with reason in a toast. */
fun toastRoll(context: Context, roll: Int, reason: String = "") {
	val text = "Rolled $roll " + (if (reason.length > 0) "($reason)" else "")
	Toast.makeText(context, text, Toast.LENGTH_LONG).show()
}
