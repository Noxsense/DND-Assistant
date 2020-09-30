package de.nox.dndassistant.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.View.OnClickListener
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.abilities.*
import kotlinx.android.synthetic.main.health.*
import kotlinx.android.synthetic.main.extra_dice.*

import de.nox.dndassistant.core.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

	private val logger = LoggerFactory.getLogger("D&D Main")

	/* The player character. */
	private lateinit var character : PlayerCharacter

	/* The roll history: Timestamp (milliseconds) -> (Result, Reason). */
	private var rollHistory: List<Pair<Long, Pair<Int, String>>> = listOf()

	data class Replacement(val name: String) {
		var bags: Map<String, String> = HashMap<String,String>()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		/* default loading. */
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		logger.debug("Initiated Activity.")

		// TODO (2020-09-27) load character, create character.
		character = playgroundWithOnyx()

		logger.debug("Player Character is loaded.")

		/* Update the character specific panels:
		 * Fill them with current character's data. */
		updateViews(true) // initiation.

		/* Initiate extra rolls (from extra_dice panel). */
		initiateExtraRolls()

		/* Show rolls. */
		label_rolls.setOnClickListener(this)
		content_rolls.setAdapter(ArrayAdapter<Pair<Long, Pair<Int, String>>>(
			this@MainActivity,
			android.R.layout.simple_list_item_1,
			rollHistory))

		/* Open content panels on click. */
		label_skills.setOnClickListener(this)
		label_attacks.setOnClickListener(this)
		label_spells.setOnClickListener(this)
		label_inventory.setOnClickListener(this)
		label_classes.setOnClickListener(this)
		label_species_background.setOnClickListener(this)
	}

	/** Initiate the panel with extra dice.
	 * There are extra dice and an additional custom field,
	 * to roll outside of possible ability or attack rolls. */
	private fun initiateExtraRolls() {
		/* Assign this listener to each custom dice term. */
		val extraDice = extra_dice as LinearLayout

		(0 until extraDice.getChildCount()).forEach {
			extraDice.getChildAt(it).apply {
				if (this is EditText) {
					// insert term and roll it. (keep text)
					setOnKeyListener(OnKeyEventRoller(rollHistory))
					setTextSize(5.toFloat())

				} else if (this is TextView) {
					// simply roll the die
					val term = this.text.toString()
					val faces: Int = term.substring(1).toInt()
					setTextSize(5.toFloat())
					setOnClickListener(
						OnClickRoller(DiceTerm(faces), term, rollHistory))
				}
			}
		}

		logger.debug("Extra Rolls are initiated.")
	}

	/** Update the character specific panels: fill them with character's data. */
	private fun updateViews(initiation: Boolean = false) {
		/* Show Player Character Name. */
		character_name.text = getString(R.string.character_name, character.name)

		/* Show Level and XP, formatted. */
		experience.text = getString(R.string.level_xp)
			.format(character.level, character.expiriencePoints)

		logger.debug("Lvl (XP) displayed.")

		/* Fill ability panel. */
		showabilities(initiation) // if initiation.: set OnClickListener

		logger.debug("Abilities displayed.")

		/* Update the healthbar, conditions, death saves and also speed and AC. */
		showHealthPanel(initiation) // if initiation: set OnClickListener

		showRestingPanel(initiation)

		// TODO (2020-09-27) previews and content.
	}

	/** Fill the Ability Panel (STR, DEX, CON, INT, WIS, CHA).
	 * Highlight the saving throws abilities.
	 * Add roller to each ability and saving throw.
	 * @param setListener if true, also set the listener.
	 */
	private fun showabilities(setListener: Boolean = false) {
		/* For each "ability" fill in name and the value. */

		var i = 0
		var v : View

		val abilities = abilities_grid as LinearLayout

		enumValues<Ability>().toList().forEach {
			v = (abilities_grid as LinearLayout).getChildAt(i)
			i += 1 // next

			logger.debug("Write ability '$it' into $v, next index $i.")

			/* Set label. */
			(v.findViewById(R.id.ability_title) as TextView).apply {
				text = "${it.fullname}"
			}

			/* Set whole score. */
			(v.findViewById(R.id.ability_value) as TextView).apply {
				text = "%d".format(character.abilityScore(it))
			}

			/* Set modifier and add OnClickRoller (OnClickListener).
			 * onClick: d20 + MOD. */
			(v.findViewById(R.id.ability_modifier) as TextView).apply {
				val mod = character.abilityModifier(it)

				// display
				text = ("%+2d".format(mod))

				// add listener
				if (setListener) {
					setOnClickListener(OnClickRoller(
						DiceTerm(D20, SimpleDice(1, mod)),
						"Ability ${it.name} ($text)",
						rollHistory))
				}
			}
		}
	}

	/** Update the healthbar, conditions, death saves
	 * and also speed and AC.
	 * @param setListener if true, also initiate the listener.
	 */
	private fun showHealthPanel(setListener: Boolean = false) {
		/* Show speed (map). */
		// val healthPanel = findViewById(R.id.panel_health) as LinearLayout

		(speed).apply {
			text = "${character.speed}" // maps: reason --> speed
			if (setListener) {
				setOnClickListener(this@MainActivity)
			}
		}

		(healthbar).run {
			max = character.maxHitPoints
			progress = character.curHitPoints
			setOnClickListener(this@MainActivity)
		}

		/* Show final armor class. */
		(armorclass).apply {
			text = getString(R.string.armorclass).format(character.armorClass)
			if (setListener) {
				// on click open "dresser"
				setOnClickListener(this@MainActivity)
			}
		}
	}

	// replacement and tests. // FIXME (2020-09-28) implement me right and remove me.
	private var hitdice: Map<Int, Pair<Int, Int>> = mapOf() // [face: available, usable]
	private var hitdiceViews: Set<TextView> = setOf() // [hit die: available/used ]

	/** Update the resting panel.
	 * Differentiate spent and available hit die.
	 * On click apply resting results.
	 * Hitdie: Spent it (as short rest) and mark as used.
	 * Long Rest: Long Rest (also visibly restore hitdie accordingly). */
	private fun showRestingPanel(initiate: Boolean = false) {
		// val hitdiceCount = character.countHitdice()

		hitdice += 8 to (3 to 3)
		hitdice += 6 to (1 to 1)

		val hitdiceCount = hitdice.values.sumBy { it.first } // count all available.
		var restCount = resting.getChildCount()

		/* There are missing hit dice. */
		if ((restCount - 1) < hitdiceCount) {
			var displayed: List<String> = listOf() // eg. [d8, d8, d6, d8, ...]

			(1 until restCount).forEach { resting.getChildAt(it).run {
				if (this !is TextView) return

				hitdiceViews += this

				displayed += this.text.toString()
			}}

			val hitdieDisplayed: Map<Int, Int>
				= displayed
					.groupingBy { it.substring(1).toInt() }
					.eachCount()

			logger.debug("Displayed hitdie: {$displayed} => $hitdieDisplayed")

			// add missing hitdice.

			longrest.setOnClickListener {
				val max = hitdiceCount / 2
				var restored = 0

				hitdiceViews.forEach {
					if (restored < max && !it.isClickable()) {
						restored += 1 // restore one more hitdie.
						it.setTextColor(android.graphics.Color.BLACK)
						it.setClickable(true)
						logger.debug("Restored $it, ${it.text}")
					}
				}
			}

			hitdice.toList().forEach {
				val (face, available) = it
				val (count, _) = available
				// var missing: Int = count - hitdieDisplayed.getOrDefault(face, 0) // Android 26
				var missing: Int = count - 0

				while (missing > 0) {
					val view: TextView = TextView(this@MainActivity)

					view.text = "d$face"
					view.setTextSize(20.toFloat())
					view.setPadding(10, 0, 10, 0)

					view.setOnClickListener { v ->
						// roll the healing.
						val roll: Int = (1..face).random()

						val ts: Long = System.currentTimeMillis()
						rollHistory += (ts to (roll to ("Short Rest (d$face)")))

						// disables this hitdie.
						if (v is TextView) {
							v.setTextColor(0x33000000)
							v.setClickable(false)
						}

						Toast.makeText(this@MainActivity, "Short Rest [$roll (d$face)]", Toast.LENGTH_LONG).show()
					}

					resting.addView(view)
					hitdiceViews += (view)

					logger.debug("Display a new hitdie [d$face[: $view")
					missing -= 1
				}
			}
		}
	}

	private fun addHitDie() {
	}

	// TODO
	override fun onClick(view: View) {

		logger.debug("Clicked on $view")

		var context = this@MainActivity
		var (long, short) = Toast.LENGTH_LONG to Toast.LENGTH_SHORT

		when (view.getId()) {
			R.id.healthbar -> {
				// open content panel.
				ac_speed_death_rest.toggleVisibility()
			}

			// TODO (2020-09-29) open content panels, hide the others.
			R.id.label_skills -> {
				closeContentsBut(R.id.content_skills)
			}
			R.id.label_attacks -> {
				closeContentsBut(R.id.content_attacks)
			}
			R.id.label_spells -> {
				closeContentsBut(R.id.content_spells)
			}
			R.id.label_inventory -> {
				closeContentsBut(R.id.content_inventory)
			}
			R.id.label_classes -> {
				closeContentsBut(R.id.content_classes)
			}
			R.id.label_species_background -> {
				closeContentsBut(R.id.content_species_background)
			}

			R.id.label_rolls -> {
				(content_rolls.adapter as ArrayAdapter<*>).notifyDataSetChanged()
				closeContentsBut(R.id.content_rolls)
			}

			R.id.speed -> {
			  Toast.makeText(context, "Go 5ft", short).show()
			  // TODO (2020-09-29)
			}
			R.id.armorclass -> {
			  Toast.makeText(context, "Open Dresser", long).show()
			  // TODO (2020-09-29)
			}
			else -> {
			  Toast.makeText(context, "Clicked on ${view}", long).show()
			  // TODO (2020-09-29)
			}
		}
	}

	/** Close other contents panel, but toggleVisibility() on given one.
	 * @return the matching view to the given id.
	 */
	private fun closeContentsBut(resId: Int) : View {
		val view: View = contents.findViewById(resId)

		(contents as ViewGroup).run {
		  (0 until this.getChildCount()).forEach {
			val child = this.getChildAt(it)
			if (child != view) {
			  child.visibility = View.GONE
			}
		  }
		}

		view.toggleVisibility()

		return view
	}

	/** Toggle View's visibility between "GONE" and "VISIBLE".
	 * @return true, if visible. */
	fun View.toggleVisibility(): Boolean
		= when (visibility) {
			View.GONE -> true.also {
				visibility = View.VISIBLE
				logger.debug("VISIBLE    $this")
			}
			else -> false.also {
				visibility = View.GONE
				logger.debug("GONE       $this")
			}
		}
}

/** Android specific logger. */
object LoggerFactory {
	fun getLogger(tag: String) : Logger
		= object : Logger {
			override fun log(t: LoggingLevel, msg: Any?) {
				when (t) {
					LoggingLevel.ERROR -> Log.e(tag, "${msg}")
					LoggingLevel.INFO -> Log.i(tag, "${msg}")
					LoggingLevel.WARN -> Log.w(tag, "${msg}")
					LoggingLevel.VERBOSE -> Log.v(tag, "${msg}")
					LoggingLevel.DEBUG -> Log.d(tag, "${msg}")
				}
			}
		}
}
