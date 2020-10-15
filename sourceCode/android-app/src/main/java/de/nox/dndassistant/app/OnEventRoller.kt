package de.nox.dndassistant.app

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnKeyListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

import java.text.SimpleDateFormat
import java.util.Locale

import de.nox.dndassistant.core.DiceTerm

object Rollers {
	var history: List<RollResult> = listOf()
}

private val log = LoggerFactory.getLogger("D&D Main")

data class RollResult(
	val value: Int,
	val single: List<Int> = listOf(value), // default [value]
	val reason: String = "", // default: no reason
	val timestamp: Long = System.currentTimeMillis() // default now!
	) : Comparable<RollResult>
{
	private companion object {
		val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
		val formatter = SimpleDateFormat(DATE_FORMAT, Locale.GERMAN)
	}

	public val timestampString: String = formatter.format(timestamp)

	/** Equality defined by value (sum) and timestamp. */
	override fun equals(other: Any?) : Boolean
		= (other != null && other is RollResult
			&& other.value == value && other.timestamp == timestamp)

	/** Compared by the timestamp. */
	override fun compareTo(other: RollResult) : Int
		= this.timestamp.compareTo(other.timestamp)

	override fun toString() : String
		= ("$value ${single.joinToString("+", " = ", "")}"
		+ (if (reason.length > 0) ": $reason" else "")
		+ " ($timestampString)")
}

/**
 * OnEventRoller extends an OnEventRoller.
 * On click a prepared dice term will be rolled.
 */
public class OnEventRoller(
	var baseTerm: DiceTerm,
	var reason: String = baseTerm.toString())
	: View.OnClickListener, View.OnLongClickListener, View.OnKeyListener {

	/* Roll, when a (DiceView) was clicked. */
	override fun onClick(view: View) {
		/* Roll the result. */
		roll(baseTerm, reason, view.getContext())
	}

	/* Roll, when a (DiceView) was clicked. */
	override fun onLongClick(view: View) : Boolean {
		/* Roll the result. */
		roll(baseTerm, reason, view.getContext())
		return true
	}

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

		// TODO (2020-10-15)
		val profTerm = "+\\s*prof"
		val parsedAdditions: List<Any> = emptyList()

		val parsedTerm = DiceTerm.parse(term.replace(profTerm, ""))

		/* Roll the result, toast it.
		 * Reason: the custom term written. */
		roll(parsedTerm, term, view.getContext())

		return true
	}

	private fun isConfirmedEnter(event: KeyEvent, code: Int) : Boolean
		= event.action == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_ENTER
}

/** Make the actual roll and add the result to the history (with current timestamp).
 * @param term DiceTerm to be rolled.
 * @param reason why the roll was thrown
 * @param toastContext if given, toast the result there. (default: null)
 * @return the value (as int).
 */
private fun roll(term: DiceTerm, reason: String, toastContext: Context? = null) : Int {
	/* Roll the result. */
	val rolls = term.rollList()
	val roll = rolls.sum()
	log.debug("Rolled $rolls => $roll ($reason)")

	/* Add to roll history: <Timestamp, <Result, Reason>>. */
	val result = RollResult(roll, rolls, reason) // default ts: now
	Rollers.history = listOf(result) + Rollers.history // workaround: prepend.


	log.debug("Show roll on MainActivity.panelRolls")

	/* Poke roll history displayer. */
	(MainActivity.instance as MainActivity).notifyRollsUpdated()

	log.debug("Rollers.history: Last entry: ${Rollers.history.last()}")

	/* Show the result. */
	if (toastContext != null) toastRoll(toastContext, result)

	return roll
}

/** Show a number with reason in a toast. */
fun toastRoll(context: Context, result: RollResult) {
	Toast.makeText(context, "Rolled $result", Toast.LENGTH_LONG).show()
}
