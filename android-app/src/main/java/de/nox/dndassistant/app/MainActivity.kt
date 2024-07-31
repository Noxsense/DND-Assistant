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

		// TODO (2020-11-22) keep screen on?
		window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
	}

	// TODO (2020-10-12) handle on resume, to reload views after standby?
	// override fun onResume()

	/** Update the hero specific panels: fill them with hero's data. */
	private fun updateViews(initiation: Boolean = false) {
		log.debug("Lvl (XP) displayed.")

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
