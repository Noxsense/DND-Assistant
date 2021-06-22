package de.nox.dndassistant.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.EditText


/** An adapther for story list items.
 * A story item can be a multilined string.
 * Only the first line will be displayed. On Click it opens the full text.
 * On long click, the item can be edited.
 */
class StoryListAdapter(private val context: Context,  val storyList: MutableList<Pair<String, Boolean>> = mutableListOf())
: BaseAdapter() {

	override fun getCount() : Int
		= storyList.size + 1

	/** Get the story string or an empty string of it is out of boundaries. */
	override fun getItem(position: Int) : Pair<String, Boolean>
		= storyList.getOrNull(position) ?: ("" to true)

	/** Add a new item, adds as fulltext (default). */
	public fun add(listItem: String?, fulltext: Boolean = true) {
		if (listItem != null && listItem.trim().length > 0) {
			storyList.add(listItem to fulltext)
		}
	}

	public fun addAll(list: List<String>, fulltext: Boolean = false) {
		storyList.addAll(list.associateWith{ fulltext }.toList())
	}

	override fun getItemId(position: Int) : Long
		= position.toLong() // ?

	override fun getView(position: Int, convertView: View?, parent: ViewGroup) : View
		= ((convertView ?: LayoutInflater.from(context).inflate(R.layout.simple_listitem_edit_confirm, parent, false)))
		.apply {
			val txtView = this.findViewById<TextView>(R.id.text)
			val btnView = this.findViewById<TextView>(R.id.button)

			// end early if views are null (not found)
			if (txtView == null || btnView == null) {
				return this
			}

			txtView.setBackgroundResource(android.R.color.white)
			btnView.setBackgroundResource(android.R.color.white)

			// TODO Setup listeners. after adding, the here fixed indeces are not the same anymore and seems more random.
			// TODO eg. appending with pos >= items.size => last item has suddenly index 0

			/* If valid item, toggle visibility. If not yet, add EditText Entry.*/
			if (!hasOnClickListeners() || position == 0) {
				btnView.setOnClickListener {
					if (position < storyList.size) {
						/* Hide or show the text field. */
						storyList.showAt(position)
					} else {
						this@StoryListAdapter.add(txtView.text.toString())
					}
					this@StoryListAdapter.notifyDataSetChanged() // notify about change.
				}
			}

			/* Simply display the story item.
			 * ELSE: Last Item is not a listed story-item but a textfield to add.
			 */
			if (position < storyList.size) {
				val (storyItem, fulltext) = getItem(position)

				val lines = storyItem.lines()

				txtView.text = storyItem
				txtView.setLines(lines.size)

				if (fulltext) {
					txtView.visibility = View.VISIBLE
					btnView.text = "Hide"
				} else {
					txtView.visibility = View.GONE
					btnView.text = lines.first()
				}

			} else {
				txtView.text = ""
				txtView.setLines(10)
				btnView.text = context.getString(R.string.dialog_story_add_submit)
			}
		}


	/** Show the item, on yes, hide on false, and just toggle with null. */
	private fun Pair<String, Boolean>.show(yes: Boolean? = null)
		= first to (if (yes == null) !second else yes)

	private fun MutableList<Pair<String,Boolean>>.showAt(position: Int, yes: Boolean? = null) {
		set(position, get(position).show(yes))
	}
}
