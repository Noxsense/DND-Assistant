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
import android.widget.LinearLayout.LayoutParams
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

	private var attacks: List<Attack> = listOf()

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

		// XXX (2020-09-27) load character, create character.
		character = playgroundWithOnyx()

		logger.debug("Player Character is loaded.")

		/* Update the character specific panels:
		 * Fill them with current character's data. */
		updateViews(true) // initiation.

		/* Initiate extra rolls (from extra_dice panel). */
		initiateExtraRolls()

		/* Show rolls. */
		label_rolls.setOnClickListener(this)
		(content_rolls.findViewById(R.id.list_rolls) as ListView).run {
			adapter = ArrayAdapter<Pair<Long, Pair<Int, String>>>(
				this@MainActivity,
				android.R.layout.simple_list_item_1,
				rollHistory)
		}

		/* Open content panels on click. */
		label_skills.setOnClickListener(this)
		label_attacks.setOnClickListener(this@MainActivity)
		label_spells.setOnClickListener(this)
		label_inventory.setOnClickListener(this)
		label_classes.setOnClickListener(this)
		label_race_background.setOnClickListener(this)
	}

	/** Initiate the panel with extra dice.
	 * There are extra dice and an additional custom field,
	 * to roll outside of possible ability or attack rolls. */
	private fun initiateExtraRolls() {
		/* Assign this listener to each custom dice term. */
		val extraDice = content_rolls.findViewById(R.id.extra_dice) as LinearLayout

		logger.debug("Set up extra dice to roll.")

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
			.format(character.level, character.experiencePoints)

		logger.debug("Lvl (XP) displayed.")

		/* Fill ability panel. */
		showabilities(initiation) // if initiation.: set OnClickListener

		logger.debug("Abilities displayed.")

		/* Update the healthbar, conditions, death saves and also speed and AC. */
		showHealthPanel(initiation) // if initiation: set OnClickListener

		showRestingPanel(initiation)

		// TODO (2020-09-27) previews and content.

		val formatLabel = { a: String, b: String ->
			getString(R.string.panel_label_format).format(a, b)
		}

		updateSkills(initiation)

		label_skills.text = formatLabel(
			"Abilities",
			"%+d, Passive Perception %2d".format(
				character.proficiencyBonus,
				character.skillScore(Skill.PERCEPTION) + 10
			))

		updateAttacks(clearBefore = initiation)

		label_attacks.text = formatLabel(
			"Attacks",
			"${attacks.maxByOrNull{ it.damage.first.average }}"
			)

		updateSpells(initiation)

		label_spells.text = formatLabel(
			"Spells",
			"*Concentration*, ${character.current.spellSlot(1)}")

		updateInventory(initiation)

		label_inventory.text = formatLabel(
			"Inventory",
			"Money ${character.purse}, Bag weight ${56.6} lb")

		updateKlasses(initiation)

		label_classes.text = formatLabel(
			"Classes",
			"(${character.klassFirst}, ${character.klasses[character.klassFirst]})")

		updateStory(initiation) // story, species, background

		label_race_background.text = formatLabel(
			"Story / Species / Background",
			"${character.background}")

		label_rolls.text = formatLabel(
			"Rolls and extra counters",
			"0") // Last Roll
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
			// text = "${character.speed}" // maps: reason --> speed
			if (setListener) {
				setOnClickListener(this@MainActivity)
			}
		}

		(healthbar).run {
			max = character.hitpoints
			progress = character.current.hitpoints
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

	/** Update the "content_skills" panel.
	 * Clicking on skill will make a skill check.
	 * Here also new proficiencies and languages and changes can be done.
	 */
	private fun updateSkills(setListener: Boolean = false) {
		val skillPanel = contents.findViewById(R.id.content_skills) as LinearLayout

		val skillList = skillPanel.findViewById(R.id.list_skills) as ListView
		val profList = skillPanel.findViewById(R.id.list_proficiencies_and_languages) as ListView

		if (skillList.adapter == null || setListener) {
			logger.debug("Set up te skillList.adapter")

			// XXX (2020-10-06) REFACTOR, proper classes.

			skillList.adapter = ArrayAdapter<Skill>(
				this@MainActivity,
				R.layout.list_item_skill,
				R.id.name,
				enumValues<Skill>().toList())

			profList.adapter = ArrayAdapter<Skillable>(
				this@MainActivity,
				R.layout.list_item_skill,
				R.id.name,
				character.proficiencies.keys.toList().filter {
					it !is Skill
				}) //  + character.knownLanguages)
		}
	}

	/** Set up the attacks for the character.
	 * Setup unarmed strike, equipped, spell attacks
	 * and improvised attacks, bag attacks.
	 * Clicking on an attack activate further options: Try to hit -> Damage dealt.
	 * Here also new shortcuts be added, otherwise it's for overview.
	 */
	private fun updateAttacks(clearBefore: Boolean = false) {
		val str = character.abilityModifier(Ability.STR)
		val dex = character.abilityModifier(Ability.DEX)

		attacks += Attack("Unarmed",
			ranged = false,
			damage = DiceTerm(0) to setOf(DamageType.BLUDGEONING),
			note = "slap, hit, kick, push ...",
			proficientValue = character.proficiencyBonus, // always proficient
			modifierStrDex = str to dex)

		// inventory weapons to attack
		attacks += character.bags.values.map { it.inside }.flatten().toSet()
			.filter { it is Weapon && it != character.hands.first && it != character.hands.second }
			.map { Attack(
				(it as Weapon).name,
				ranged = !it.weaponType.melee,
				damage = it.damage to it.damageType,
				)}

		val attackPanel = content_attacks
			.findViewById(R.id.list_attacks) as ListView

		attackPanel.adapter = ArrayAdapter(
			this@MainActivity,
			android.R.layout.simple_list_item_1,
			attacks)
	}

	/** Make a weapon to an attack for the selected character. */
	private fun weaponToAttack(wpn: Weapon, str: Int = 0, dex: Int = 0) : Attack
		= Attack(
			wpn.name,
			ranged = !wpn.weaponType.melee,
			damage = wpn.damage to wpn.damageType,
			note = wpn.note,
			finesse = wpn.isFinesse,
			proficientValue = character.getProficiencyBonusFor(wpn),
			modifierStrDex = str to dex)

	/** Update the "content_spells" panel.
	 * Show left and available spell slots.
	 * Show data of each learned spell and click to cast them.
	 * Here new spells which may be learnt can be added, or known spells can be prepared.
	 */
	private fun updateSpells(setListener: Boolean = false) {
		val spellPanel = contents.findViewById(R.id.content_spells) as LinearLayout

		/* Spell slots. */
		val spellSlots = spellPanel.findViewById(R.id.spell_slots) as TextView

		/* Known spells. */
		val spellList = spellPanel.findViewById(R.id.list_spells) as ListView

		if (spellList.adapter == null || setListener) {
			logger.debug("Set up te spellList.adapter")
			// XXX (2020-10-06) REFACTOR, proper classes.

			spellSlots.text = (0 .. 9).joinToString("") {
				it.toString()
			}

			spellList.adapter = ArrayAdapter<Spell>(
				this@MainActivity,
				R.layout.list_item_spell,
				R.id.name,
				character.spellsKnown)
		}
	}

	/** Update the "content_inventory" panel.
	 * Show currently carried weight, money and equipped items.
	 * Also show the carried or owned bags.
	 * Click a bag item, to get more options, what to do with it [equip, drop, sell].
	 * Also new items or money can be added, and equipped items can be dropped or stored.
	 */
	private fun updateInventory(setListener: Boolean = false) {
		logger.debug("Update inventory")

		// XXX (2020-10-07) refactor

		/* Weight bar. */
		(content_inventory.findViewById(R.id.bar_carried_weight) as ProgressBar).run {
			max = 100
			progress  = 10
		}

		/* Equipped. */
		(content_inventory.findViewById(R.id.equipped) as TextView).run {
			text = "Equipped and in Hands!"
		}

		/* List of bags. And their content. */
		(content_inventory.findViewById(R.id.list_bags) as ListView).run {
				adapter = ArrayAdapter(
					this@MainActivity,
					android.R.layout.simple_list_item_1,
					(1.. 20).map { it })
			}
	}

	/** Update the "content_classes" panel.
	 * Clicking on a feast will show more information about the feast.
	 * Also limited feasts can be activated.
	 * Here a new klass level can also be added.
	 */
	private fun updateKlasses(setListener: Boolean = false) {
		// XXX (2020-10-07) implement
		(content_classes.findViewById(R.id.list_classes) as ListView)
			.adapter = ArrayAdapter(
				this@MainActivity,
				android.R.layout.simple_list_item_1, // TODO (2020-10-07) complex layout with 1-level nested listviews: klass -> feats.
				character.klasses.keys.toList())
	}

	/** Update the "content_race_background" panel.
	 * Here are information about the race, the background and also pure story focussed notes.
	 * New notes can be added and also race or background features reviewed or activated.
	 */
	private fun updateStory(setListener: Boolean = false) {
		// XXX (2020-10-07) implement
		val findView = { id: Int ->
			content_race_background.findViewById(id) as View
		}

		(findView(R.id.the_species) as TextView)
			.text = "${character.race} (${character.subrace})"

		(findView(R.id.about_species) as TextView)
			.text = """
				Darkvision: ${character.race.darkvision}
				Base-Speed: ${character.race.speed}
				Languages: ${character.race.languages}
				Features: ${character.race.features}
				Description: ${character.race.description}
				""".trimIndent()

		(findView(R.id.the_background) as TextView)
			.text = "${character.background} (${character.backgroundFlavour})"

		(findView(R.id.about_background) as TextView)
			.text = """
				Alignment: ${character.alignment}
				Trait: ${character.trait}
				Ideal: ${character.ideal}
				Bonds: ${character.bonds}
				Flaws: ${character.flaws}
				""".trimIndent()

		(findView(R.id.about_appearance) as TextView)
			.text = """
				Age: ${character.ageString}
				Size: ${character.size}
				Weight: ${character.weight} lb
				Height: ${character.height} ft
				Appearance: ${character.appearance}
				Form: ${character.form}
				""".trimIndent()

		(findView(R.id.history) as TextView)
			.text = character.history.joinToString("\n")
	}

	// TODO (2020-10-06) separate single logics.
	override fun onClick(view: View) {
		logger.debug("Clicked on $view")

		var context = this@MainActivity
		var (long, short) = Toast.LENGTH_LONG to Toast.LENGTH_SHORT

		when (view.getId()) {
			R.id.healthbar -> {
				// open content panel.
				ac_speed_death_rest.toggleVisibility()
			}

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
			R.id.label_race_background -> {
				closeContentsBut(R.id.content_race_background)
			}

			R.id.label_rolls -> {
				// (content_rolls.adapter as ArrayAdapter<*>).notifyDataSetChanged()
				closeContentsBut(R.id.content_rolls)
			}

			R.id.speed -> {
			  Toast.makeText(context, "Go 5ft", short).show()
			  // XXX (2020-09-29)
			}
			R.id.armorclass -> {
			  Toast.makeText(context, "Open Dresser", long).show()
			  // XXX (2020-09-29)
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
		// view.setLayoutParams(VIEW_MATCH_PARENT)

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
