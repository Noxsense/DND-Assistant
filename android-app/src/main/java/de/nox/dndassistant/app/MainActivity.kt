package de.nox.dndassistant.app

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.GridLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// import kotlinx.android.synthetic.main.activity_main.*

import de.nox.dndassistant.core.D20

class MainActivity : AppCompatActivity(), View.OnClickListener {

	private val log = LoggerFactory.getLogger("D&D Main")

	companion object {
		lateinit var instance: AppCompatActivity
			private set
	}

	private lateinit var panelAbilities: View
		private set

	/** Check if preview and context (abilities) are initiated. */
	private fun isInitializedPanelAbilities() : Boolean = ::panelAbilities.isInitialized

	private lateinit var panelHealth: Pair<View, ViewGroup>
		private set

	/** Check if preview and context (health) are initiated. */
	private fun isInitializedPanelHealth() : Boolean = ::panelHealth.isInitialized

	private lateinit var panelSkills: Pair<TextView, ViewGroup>
		private set
		// initiated on updateSkills()

	/** Check if preview and context (skills) are initiated. */
	private fun isInitializedPanelSkills() : Boolean = ::panelSkills.isInitialized

	private lateinit var panelAttacks: Pair<TextView, ViewGroup>
		private set
		// initiated on updateAttacks()

	/** Check if preview and context (attacks) are initiated. */
	private fun isInitializedAttacks() : Boolean = ::panelAttacks.isInitialized

	private lateinit var panelInventory: Pair<TextView, ViewGroup>
		private set
		// initiated on updateInventory()

	/** Check if preview and context (inventory) are initiated. */
	private fun isInitializedInventory() : Boolean = ::panelInventory.isInitialized

	private lateinit var panelKlasses: Pair<TextView, ViewGroup>
		private set
		// initiated on updatecClasses()

	/** Check if preview and context (klasses) are initiated. */
	private fun isInitializedKlasses() : Boolean = ::panelKlasses.isInitialized

	private lateinit var panelStory: Pair<TextView, ViewGroup>
		private set
		// initiated on updateStory()

	/** Check if preview and context (story) are initiated. */
	private fun isInitializedStory() : Boolean = ::panelStory.isInitialized

	private lateinit var panelRolls: Pair<TextView, ViewGroup>
		private set
		// initiated on initiateExtraRolls()

	/** Check if preview and context (rolls) are initiated. */
	private fun isInitializedRolls() : Boolean = ::panelRolls.isInitialized

	private lateinit var li: LayoutInflater

	override fun onCreate(savedInstanceState: Bundle?) {
		/* default loading. */
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		li = LayoutInflater.from(this)

		instance = this

		log.debug("Initiated Activity.")

		/* Update the hero specific panels:
		 * Fill them with current hero's data. */
		updateViews(true) // initiation.

		/* Show rolls. */
		// TODO (2020-10-11) proper class for Array adapter or outsources
		label_rolls.setOnClickListener(this)
		(content_rolls.findViewById<ListView>(R.id.list_rolls)).run {
			adapter = object: ArrayAdapter<RollResult>(
				this@MainActivity,
				R.layout.list_item_roll) {
					override fun getItem(p0: Int) : RollResult
						= Rollers.history.toList().get(p0)

					override fun getCount() : Int
						= Rollers.history.size

					override fun getView(i: Int, v: View?, parent: ViewGroup) : View {
						if (v == null) {
							val newView = li.inflate(R.layout.list_item_roll, parent, false)
							return getView(i, newView, parent)
						}

						// no null

						val e = getItem(i) // the element: RollResult to show

						/* +-------+----------------- +
						 * | ROLL  | rolls, why, when |
						 * +-------+------------------+ */

						(v.findViewById<TextView>(R.id.value))
							.text = "${e.value}"

						(v.findViewById<TextView>(R.id.single_rolls))
							.text = e.single.joinToString(" + ")

						(v.findViewById<TextView>(R.id.note))
							.text = e.reason

						(v.findViewById<TextView>(R.id.timestamp))
							.text = e.timestampString

						when (e.single.first()) {
							1 -> v.setBackgroundColor(0xff0000)
							20 -> v.setBackgroundColor(0x00ff00)
							else -> Unit // pass
						}

						return v
					}
				}
		}

		/* Initiate extra rolls (from grid_dice panel). */
		initiateExtraRolls()

		panelRolls = Pair(label_rolls, content_rolls as ViewGroup)

		/* Open content panels on click. */
		// inspiration.setOnClickListener(this)
		// label_skills.setOnClickListener(this)
		// label_attacks.setOnClickListener(this@MainActivity)
		// label_inventory.setOnClickListener(this)
		// label_classes.setOnClickListener(this)
		// label_race_background.setOnClickListener(this)

		// TODO (2020-11-22) keep screen on?
		window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
	}

	/** Poke the roll history view to display potentially updated entries.
	 * Also display the most recent result in the preview. */
	public fun notifyRollsUpdated() {
		if (!isInitializedRolls()) return

		val (preview, content) = panelRolls
		content.findViewById<ListView>(R.id.list_rolls).run {
			(adapter as ArrayAdapter<RollResult>).notifyDataSetChanged()

			/* Show last rolls, if available. */
			preview.text = formatLabel(
				getString(R.string.title_rolls),
				try { "Latest: ${(adapter.getItem(0) as RollResult?)?.value}" }
				catch (e: IndexOutOfBoundsException) { "No rolls yet." }
				)
		}
	}

	// TODO (2020-10-12) handle on resume, to reload views after standby?
	// override fun onResume()

	/** Update the hero specific panels: fill them with hero's data. */
	private fun updateViews(initiation: Boolean = false) {
		log.debug("Lvl (XP) displayed.")

		notifyRollsUpdated()

		// XXX (2020-11-10) Proper ticker.
		findViewById<TextView>(R.id.tick).run {
			text = "\u23f3 Spend 6 seconds"
			setOnClickListener {
				// character.current.tick() // XXX

				Toast.makeText(this@MainActivity,
					"6 sec later...",
					Toast.LENGTH_SHORT).show()
			}
		}
	}

	/** Format the preview label. */
	private fun formatLabel(a: String, b: String) :String
		= getString(R.string.panel_label_format).format(a, b)

	/** Initiate the panel with extra dice.
	 * There are extra dice and an additional custom field,
	 * to roll outside of possible ability or attack rolls. */
	private fun initiateExtraRolls() {
		/* Assign this listener to each custom dice term. */
		val extraDice = content_rolls.findViewById<GridLayout>(R.id.grid_dice)

		log.debug("Set up extra dice to roll.")

		(0 until extraDice.getChildCount()).forEach {
			extraDice.getChildAt(it).apply {
				if (this is EditText) {
					/* Parse the text and return the rolled text. */
					val editRoller = OnEventRoller.Builder(this)
						.setReasonView(this)
						.create()

					/* Insert term and roll it. (keep text) */
					setOnKeyListener(editRoller)

					/* On Long click: Parse last term. */
					setOnLongClickListener(editRoller)

				} else if (this is TextView) {
					/* simply roll the die */
					setOnClickListener(OnEventRoller.Builder(this)
						.setReasonView(this)
						.create())
				}
			}
		}

		log.debug("Extra Rolls are initiated.")
	}

	// TODO (2020-10-06) separate single logics. (onClick(view))
	override fun onClick(view: View) {
		log.debug("Clicked on $view")

		when (view.getId()) {
			R.id.inspiration -> {
				/* toggle inspiration. */
				findViewById<View>(R.id.inspiration).run {
					alpha = when {
						alpha > 0.33f -> 0.3f
						else -> 1.0f
					}
				}
			}
			R.id.label_skills -> {
			}
			R.id.label_attacks -> {
			}
			R.id.label_inventory -> {
			}
			R.id.label_classes -> {
			}
			R.id.label_race_background -> {
			}

			R.id.label_rolls -> {
				/* Update roll shower. */
				// notifyRollsUpdated()
			}

			else -> {
				Toast.makeText(this@MainActivity,
					"Clicked on ${view}",
					Toast.LENGTH_SHORT
				).show()
			  // TODO (2020-09-29) rest case? any click without purpose?
			}
		}
	}

	/** Toggle View's visibility between "GONE" and "VISIBLE".
	 * @return true, if visible. */
	fun View.toggleVisibility(): Boolean
		= when (visibility) {
			View.GONE -> true.also {
				visibility = View.VISIBLE
				log.debug("VISIBLE    $this")
			}
			else -> false.also {
				visibility = View.GONE
				log.debug("GONE       $this")
			}
		}
}
