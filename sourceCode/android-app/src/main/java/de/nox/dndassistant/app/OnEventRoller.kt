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
import de.nox.dndassistant.core.SimpleDice

object Rollers {
	var history: List<RollResult> = listOf()
}

private val log = LoggerFactory.getLogger("D&D Roller")

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

	/** String representation. */
	override fun toString() : String
		= (brief() + " ($timestampString)")

	/** Brief string representation (without date). */
	fun brief() : String
		= ("$value ${single.joinToString("+", " = ", "")}"
		+ (if (reason.length > 0) ": $reason" else ""))
}

/**
 * OnEventRoller extends an OnEventRoller.
 * On click a prepared dice term will be rolled.
 * If proficiencyConnected is true, add also the proficiencyBonus to the end roll.
 */
public class OnEventRoller
	private constructor (
		val rawDice: List<Any>,
		val reasonView: TextView?,
		val reasonStr: String?,
		val formatStr: String = "%s"
	) : View.OnClickListener, View.OnLongClickListener, View.OnKeyListener {

	public class Builder {
		private var diceSources: List<Any> = listOf()
		private var alternativeDiceTerm: DiceTerm? = null
		private var reasonView: TextView? = null
		private var reasonStr: String? = null
		private var formatString: String = "%s"

		/** Construct with first dice Term (TextView). */
		constructor(tv: TextView) { addDiceView(tv) }

		/** Construct with first dice Term (DiceTerm). */
		constructor(dt: DiceTerm) { addDiceTerm(dt) }

		/** Construct with first simple dice Term (DiceTerm). */
		constructor(sd: SimpleDice) : this(sd.toTerm()) ;

		/** Add another TextView to the list to parse. */
		public fun addDiceView(tv: TextView) = apply {
			this.diceSources += (tv)
		}

		/** Set the alternative dice term. */
		public fun addDiceTerm(dt: DiceTerm) = apply {
			this.diceSources += (dt)
		}

		/** Set the alternative simple dice. */
		public fun addDiceTerm(sd: SimpleDice) = apply {
			this.diceSources += (sd.toTerm())
		}

		/** Set the alternative written boni. */
		public fun addDiceTerm(i: Int) = apply {
			this.diceSources += (SimpleDice(1, i))
		}

		/** Set reason from a string. */
		public fun setReasonView(tv: TextView) = apply {
			this.reasonView = (tv)
		}

		/** Set an alternative String as fallback. */
		public fun setReasonString(str: String) = apply {
			this.reasonStr = str
		}

		public fun setFormatString(str: String) = apply {
			this.formatString = str
		}

		/** Create the EventRoller.
		 * @throws Exception if both dice views and alternative DiceTerm are empty/null. */
		public fun create() : OnEventRoller {
			if (reasonView == null && reasonStr == null)
				reasonStr = "" // set it to empty in the last moment.

			log.debug("Create new OnEventRoller, "
				+ "with $diceSources, "
				+ "[$reasonView, $reasonStr => $formatString].")

			return OnEventRoller(
				diceSources,
				reasonView, reasonStr,
				formatString)
		}
	}

	// observe data changes
	public val rawDiceTerm: DiceTerm?
		= (rawDice.filter { it is DiceTerm } as List<DiceTerm>).run {
			if (size < 1) {
				null // list was empty
			} else {
				val head: DiceTerm = first()

				if (size < 2) {
					head
				} else {
					this.drop(1).fold(head) {acc, next ->
						acc + next
					}
				}
			}
		}

	public val rawDiceTexts: List<TextView>
		= rawDice.filter { it is TextView } as List<TextView>

	private var parsedDiceText: String = ""
	private var parsedUpdated: Boolean = false // if the resulting term changed => recalculate.
	private var parsedDiceTerm: DiceTerm? = null

	private var finalDiceTerm: DiceTerm = rawDiceTerm ?: SimpleDice(0, 0).toTerm()

	/** Parse the given parseDiceTermViews or use alternative term. */
	private fun collectDiceTexts() : String
		= rawDiceTexts.joinToString("+") {
			it.text.toString().replace("\\s", "") // trim of white space.
		}.also {
			log.debug("Just parsed dice term text: $it")
		}

	/** Collect current string of all text views and parse the resulting dice term.
	 * @return DiceTerm which with the latest updates (may be old though).
	 * @throws DiceTermFormatException if the parsed terms are invalid dice terms.*/
	private fun updateFromDiceTexts() : DiceTerm? {
		val freshlyParsed = collectDiceTexts()

		/* Update `parsedDiceText` on changes, or if not yet initialled. */
		if (freshlyParsed != parsedDiceText) {
			try {
				parsedDiceTerm = DiceTerm.parse(freshlyParsed)
				parsedUpdated = true
				log.debug("Parsed (updated) dice term text")
			} catch (e: Exception) {
				log.error("DiceTerm parsing error: $e")
			}
		}

		parsedDiceText = freshlyParsed // update.

		log.debug("Latest TextViews' dice term: $parsedDiceTerm")

		return parsedDiceTerm // contains the latest updates.
	}

	/** Parse the given parseDiceTermViews or use alternative term. */
	public val reason: String
		get() = formatStr.format(reasonView?.text?.toString() ?: reasonStr)

	/** Get the final dice term of the raw dice terms and the parsed dice terms.
	  * @return latest DiceTerm
	  * @throws DiceTermFormatException if the parsed dice texts are incorrectly formatted. */
	private fun rawDiceBaked() : DiceTerm {
		updateFromDiceTexts() // get parsed dice term.

		if (parsedUpdated && parsedDiceTerm != null) {
			// we want fixed first, if available.
			finalDiceTerm = when {
				rawDiceTerm != null -> (rawDiceTerm + parsedDiceTerm!!)
				else -> (parsedDiceTerm!!)
			}
			parsedUpdated = false // reset the "new" state
		}

		return finalDiceTerm. also {
			log.debug("Raw Dice Sources to Final DiceTerm: $it")
		}
	}

	/* Roll, when a (DiceView) was clicked. */
	override public fun onClick(view: View) {
		/* Roll the result. */
		roll(rawDiceBaked(), reason, view.getContext())
	}

	/* Roll, when a (DiceView) was clicked. */
	override public fun onLongClick(view: View) : Boolean {
		/* Roll the result. */
		roll(rawDiceBaked(), reason, view.getContext())
		return true
	}

	/** Parse TextView text to DiceTerm and return rolled result. */
	override public fun onKey(view: View, code: Int, event: KeyEvent) : Boolean {
		/* Don't do anything, if this wasn't a TextView (or Heritance). */
		if (view !is TextView) {
			return false
		}

		/* Skip on not Enter. */
		if (!isConfirmedEnter(event, code)) {
			return false
		}

		/* Roll the result, toast it.
		 * Reason: the custom term written. */
		roll(rawDiceBaked(), reason, view.getContext())

		return true
	}

	private fun isConfirmedEnter(event: KeyEvent, code: Int) : Boolean
		= event.action == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_ENTER

	public var lastRoll: Int = 0
		private set

	/** Make the actual roll and add the result to the history (with current timestamp).
	 * @param term DiceTerm to be rolled.
	 * @param reason why the roll was thrown
	 * @param toastContext if given, toast the result there. (default: null)
	 * @return the value (as int).
	 */
	private fun roll(term: DiceTerm, reason: String, toastContext: Context? = null) : Int {
		/* Roll the result. */
		val rolls = term.rollList()
		lastRoll = rolls.sum()

		/* Add to roll history: <Timestamp, <Result, Reason>>. */
		val result = RollResult(lastRoll, rolls, reason) // default ts: now
		log.debug("$result")

		Rollers.history = listOf(result) + Rollers.history // workaround: prepend.

		log.debug("Show roll on MainActivity.panelRolls")

		/* Poke roll history displayer. */
		(MainActivity.instance as MainActivity).notifyRollsUpdated()

		log.debug("Rollers.history: Last entry: ${Rollers.history.last()}")

		/* Show the result. */
		if (toastContext != null) toastRoll(toastContext, result)

		return lastRoll
	}

	/** Show a number with reason in a toast. */
	fun toastRoll(context: Context, result: RollResult) {
		Toast.makeText(context, "Rolled ${result.brief()}", Toast.LENGTH_LONG).show()
	}
}
