package de.nox.dndassistant.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import java.util.Date
import java.text.SimpleDateFormat

import de.nox.dndassistant.core.RollHistory

/** An adapther to display the RollHistory.
 * Each Item of the History is a TimedRolls,
 * which is a result the term it comes from (and the variables) and a timestamp.
 */
class RollHistoryAdapter(private val context: Context)
: BaseAdapter() {

	private val rolls: List<RollHistory.TimedRolls> get() = RollHistory.rolls

	// display date from long as ISO. @see timestampToString(long)
	private val dateFormatter = SimpleDateFormat("YYYY-MM-dd HH:mm:ss")

	override fun getCount() : Int
		= rolls.size

	/**
	 * Get the story string or an empty string of it is out of boundaries.
	 * Get reversed (most recent on top).
	 */
	override fun getItem(position: Int) : RollHistory.TimedRolls
		= rolls.get(getCount() - 1 - position)

	override fun getItemId(position: Int) : Long
		= position.toLong() // ?

	// simple long to string formatter, @see dateFormatter and ISO format.
	private fun timestampToString(timestamp: Long) : String
		= dateFormatter.format(Date(timestamp))

	override fun getView(position: Int, convertView: View?, parent: ViewGroup) : View
		= ((convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_timedrolls, parent, false)))
		.apply {
			if (position < 0 || position >= getCount()) {
				return this // end early
			}

			val (label, term, vars, rolls, sum, timestamp) = getItem(position)

			this.findViewById<TextView>(R.id.term).apply {
				text = "$label $term"
			}
			this.findViewById<TextView>(R.id.timestamp).apply {
				text = timestampToString(timestamp)
			}

			this.findViewById<TextView>(R.id.sum).apply {
				text = "$sum"
			}

			this.findViewById<TextView>(R.id.rolls).apply {
				text = "${rolls.joinToString()}"
			}

			android.util.Log.d("RollHistoryAdapter", "Show Timed Roll: sum: $sum = { ${rolls.joinToString()} }, by $label ($term) at $timestamp")
		}
}
