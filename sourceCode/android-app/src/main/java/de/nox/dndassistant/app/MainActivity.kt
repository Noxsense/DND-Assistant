package de.nox.dndassistant.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.abilities.*
import kotlinx.android.synthetic.main.extra_dice.*

import de.nox.dndassistant.core.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

	private val log = LoggerFactory.getLogger("D&D Main")

	/* The player character. */
	private lateinit var character : PlayerCharacter

	companion object {
		lateinit var instance: AppCompatActivity
			private set

		lateinit var panelAbilities: View
			private set

		/** Check if preview and context (abilities) are initiated. */
		fun isInitializedPanelAbilities() : Boolean = ::panelAbilities.isInitialized

		lateinit var panelHealth: Pair<View, ViewGroup>
			private set

		/** Check if preview and context (health) are initiated. */
		fun isInitializedPanelHealth() : Boolean = ::panelHealth.isInitialized

		lateinit var panelSkills: Pair<TextView, ViewGroup>
			private set
			// initiated on updateSkills()

		/** Check if preview and context (skills) are initiated. */
		fun isInitializedPanelSkills() : Boolean = ::panelSkills.isInitialized

		lateinit var panelAttacks: Pair<TextView, ViewGroup>
			private set
			// initiated on updateAttacks()

		/** Check if preview and context (attacks) are initiated. */
		fun isInitializedAttacks() : Boolean = ::panelAttacks.isInitialized

		lateinit var panelSpells: Pair<TextView, ViewGroup>
			private set
			// initiated on updateSpells()

		/** Check if preview and context (spells) are initiated. */
		fun isInitializedSpells() : Boolean = ::panelSpells.isInitialized

		lateinit var panelInventory: Pair<TextView, ViewGroup>
			private set
			// initiated on updateInventory()

		/** Check if preview and context (inventory) are initiated. */
		fun isInitializedInventory() : Boolean = ::panelInventory.isInitialized

		lateinit var panelKlasses: Pair<TextView, ViewGroup>
			private set
			// initiated on updatecClasses()

		/** Check if preview and context (klasses) are initiated. */
		fun isInitializedKlasses() : Boolean = ::panelKlasses.isInitialized

		lateinit var panelStory: Pair<TextView, ViewGroup>
			private set
			// initiated on updateStory()

		/** Check if preview and context (story) are initiated. */
		fun isInitializedStory() : Boolean = ::panelStory.isInitialized

		lateinit var panelRolls: Pair<TextView, ViewGroup>
			private set
			// initiated on initiateExtraRolls()

		/** Check if preview and context (rolls) are initiated. */
		fun isInitializedRolls() : Boolean = ::panelRolls.isInitialized
	}

	private var attacks: List<Attack> = listOf()

	data class Replacement(val name: String) {
		var bags: Map<String, String> = HashMap<String,String>()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		/* default loading. */
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		instance = this

		log.debug("Initiated Activity.")


		// XXX (2020-09-27) load character, create character.
		character = playgroundWithOnyx()

		log.debug("Player Character is loaded.")

		/* Update the character specific panels:
		 * Fill them with current character's data. */
		updateViews(true) // initiation.

		/* Show rolls. */
		label_rolls.setOnClickListener(this)
		(content_rolls.findViewById(R.id.list_rolls) as ListView).run {
			adapter = object: ArrayAdapter<RollResult>(
				this@MainActivity,
				R.layout.list_item_roll) {
					override fun getItem(p0: Int) : RollResult
						= Rollers.history.toList().get(p0)

					override fun getCount() : Int
						= Rollers.history.size

					override fun getView(i: Int, v: View?, parent: ViewGroup) : View {
						if (v == null) {
							val newView = LayoutInflater.from(this@MainActivity).run {
								inflate(R.layout.list_item_roll, parent, false)
							}
							return getView(i, newView, parent)
						}

						// no null

						val e = getItem(i) // the element: RollResult to show

						/* +-------+----------------- +
						 * | ROLL  | rolls, why, when |
						 * +-------+------------------+ */

						(v.findViewById(R.id.value) as TextView)
							.text = "${e.value}"

						(v.findViewById(R.id.single_rolls) as TextView)
							.text = e.single.joinToString(" + ")

						(v.findViewById(R.id.note) as TextView)
							.text = e.reason

						(v.findViewById(R.id.timestamp) as TextView)
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

		/* Initiate extra rolls (from extra_dice panel). */
		initiateExtraRolls()

		panelRolls = Pair(label_rolls, content_rolls as ViewGroup)

		/* Open content panels on click. */
		label_skills.setOnClickListener(this)
		label_attacks.setOnClickListener(this@MainActivity)
		label_spells.setOnClickListener(this)
		label_inventory.setOnClickListener(this)
		label_classes.setOnClickListener(this)
		label_race_background.setOnClickListener(this)
	}

	/** Update the character specific panels: fill them with character's data. */
	private fun updateViews(initiation: Boolean = false) {
		/* Show Player Character Name. */
		character_name.text = getString(R.string.character_name, character.name)

		/* Show Level and XP, formatted. */
		experience.text = getString(R.string.level_xp)
			.format(character.level, character.experiencePoints)

		log.debug("Lvl (XP) displayed.")

		/* Fill ability panel. */
		showabilities(initiation) // if initiation.: set OnClickListener

		log.debug("Abilities displayed.")

		/* Update the healthbar, conditions, death saves and also speed and AC. */
		updateLifestate(initiation) // if initiation: set OnClickListener

		// TODO (2020-09-27) previews and content. (less hacked, pls)

		updateSkills(initiation)

		updateAttacks(clearBefore = initiation)

		updateSpells(initiation)

		updateInventory(initiation)

		updateKlasses(initiation)

		updateStory(initiation) // story, species, background

		label_rolls.text = formatLabel(
			"Rolls and extra counters",
			"0") // Last Roll
	}

	/** Format the preview label. */
	private fun formatLabel(a: String, b: String) :String
		= getString(R.string.panel_label_format).format(a, b)

	/** Fill the Ability Panel (STR, DEX, CON, INT, WIS, CHA).
	 * Highlight the saving throws abilities.
	 * Add roller to each ability and saving throw.
	 * @param setListener if true, also set the listener.
	 */
	private fun showabilities(setListener: Boolean = false) {
		if (!isInitializedPanelAbilities()) {
			panelAbilities = abilities_grid
		}

		var i = 0 // starting view.
		var v : View // current child

		/* For each "ability" fill in name and the value. */
		enumValues<Ability>().toList().forEach {
			v = (abilities_grid as LinearLayout).getChildAt(i)
			i += 1 // next

			log.debug("Write ability '$it' into $v, next index $i.")

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
						"Ability ${it.name} ($text)"))
				}
			}
		}
	}

	/** Update the healthbar, conditions, death saves
	 * and also speed and AC.
	 * @param setListener if true, also initiate the listener.
	 */
	private fun updateLifestate(setListener: Boolean = false) {
		if (!isInitializedPanelHealth()) {
			panelHealth = Pair(
				content_health.findViewById(R.id.healthbar),
				content_health.findViewById(R.id.ac_speed_death_rest))

		}

		/* Show speed (map). */
		(panelHealth.second.findViewById(R.id.speed) as TextView).apply {
			// text = "${character.speed}" // maps: reason --> speed
			text = "${character.current.feetLeft}" // maps: reason --> speed
			if (setListener) {
				setOnClickListener(this@MainActivity)

				// TODO (2020-10-08) set on long click: change taken steps.
			}
		}

		/* Set up heatlh bar (progressbar) */
		(panelHealth.first as ProgressBar).run {
			max = character.hitpoints
			progress = character.current.hitpoints
			setOnClickListener(this@MainActivity)
			setOnLongClickListener {
				// TODO (2020-10-08) implement hit / heal dialog.
				Toast.makeText(
					this@MainActivity,
					"HIT/HEAL Dialog.",
					Toast.LENGTH_LONG
				).show()
				true
			}
		}

		/* Show final armor class. */
		(panelHealth.second.findViewById(R.id.armorclass) as TextView).apply {
			text = getString(R.string.armorclass).format(character.armorClass)
			if (setListener) {
				// on click open "dresser"
				setOnClickListener(this@MainActivity)
			}
		}

		/* Update. */
		// showRestingPanel(setListener)
		showRestingPanel()

		// TODO (2020-10-08) setup heal / damage dialog.
	}

	// replacement and tests. // FIXME (2020-09-28) implement me right and remove me.
	private var hitdiceViews: Set<TextView> = setOf() // [hit die: available/used ]

	/** Update the resting panel.
	 * Differentiate spent and available hit die.
	 * On click apply resting results.
	 * Hitdie: Spent it (as short rest) and mark as used.
	 * Long Rest: Long Rest (also visibly restore hitdie accordingly). */
	private fun showRestingPanel(initiate: Boolean = false) {
		log.debug("Set up hit dice.")

		val hitdiceCount = character.hitdice.size

		// count displayed hit dice.
		val resting = panelHealth.second.findViewById(R.id.resting) as LinearLayout
		var restCount = resting.getChildCount()

		val longrest = resting.findViewById(R.id.longrest) as TextView

		// TODO (2020-10-07) consider gridview or listview for hitdice.

		/* There are missing hit dice. */
		if ((restCount - 1) < hitdiceCount) {
			var displayed: List<String> = listOf() // eg. [d8, d8, d6, d8, ...]

			(1 until restCount).forEach { resting.getChildAt(it).run {
				if (this !is TextView) return

				hitdiceViews += this

				displayed += this.text.toString()
			}}

			val hitdieDisplayed: Map<Int, Int> = displayed
				.groupingBy { it.substring(1).toInt() }
				.eachCount()

			log.debug("Displayed hitdie: {$displayed} => $hitdieDisplayed")

			// add missing hitdice.

			/* Long rest. */
			if (!longrest.hasOnClickListeners()) {
				longrest.setOnClickListener {
					val max = Math.max(hitdiceCount / 2, 1) // at least 1
					var restored = 0

					// TODO (2020-10-07) avoid deep nesting.
					log.debug("Restore $max hitdice")

					hitdiceViews.forEach {
						if (restored < max && !it.isClickable()) {
							restored += 1 // restore one more hitdie.
							it.setTextColor(android.graphics.Color.BLACK)
							it.setClickable(true)
							log.debug("Restored $it, ${it.text}")
							log.debug("Restored $restored/$max hitdice")
						}
					}
				}
			}

			// TODO (2020-10-07) refactor: Update only needed / missing hitdice.
			character.hitdice.forEach { face ->
				val view: TextView = TextView(this@MainActivity)

				view.text = "d$face"
				view.setTextSize(20.toFloat())
				view.setPadding(10, 0, 10, 0)

				view.setOnClickListener { v ->
					// roll the healing.
					val roll: Int = (1..face).random()

					Rollers.history += listOf(RollResult(
						value = roll,
						reason = "Short Rest (d$face)")) + Rollers.history

					// disables this hitdie.
					if (v is TextView) {
						v.setTextColor(0x33000000)
						v.setClickable(false)
					}

					Toast.makeText(this@MainActivity,
						"Short Rest [$roll (d$face)]",
						Toast.LENGTH_LONG).show()
				}

				resting.addView(view)
				hitdiceViews += (view)

				log.debug("Display a new hitdie [d$face[: $view")
			}
		}
	}

	/** Update the "content_skills" panel.
	 * Clicking on skill will make a skill check.
	 * Here also new proficiencies and languages and changes can be done.
	 */
	private fun updateSkills(setListener: Boolean = false) {
		// find content.
		if (!isInitializedPanelSkills()) {
			panelSkills = Pair(
				label_skills,
				contents.findViewById(R.id.content_skills) as ViewGroup)
		}

		/* Set companion object's skillpanel label and Content. */

		val skillPanel = panelSkills.second

		/* Set preview. */
		label_skills.text = formatLabel(
			"Abilities",
			"%+d, Passive Perception %2d".format(
				character.proficiencyBonus,
				character.skillModifier(Skill.PERCEPTION) + 10
			))

		val profValue = skillPanel.findViewById(R.id.proficiency_value) as TextView
		val skillList = skillPanel.findViewById(R.id.list_skills) as ListView
		val profList = skillPanel.findViewById(R.id.list_proficiencies_and_languages) as ListView

		profValue.text = "Proficiency: %+d".format(character.proficiencyBonus)

		if (skillList.adapter == null || setListener) {
			log.debug("Set up te skillList.adapter")

			// XXX (2020-10-06) REFACTOR, proper classes. (content_skills)
			// TODO (2020-10-07) actual filling (content_skills)
			// TODO (2020-10-07) add roller (content_skills)

			skillList.adapter = object: ArrayAdapter<Skill>(
				this@MainActivity,
				R.layout.list_item_skill,
				R.id.name,
				enumValues<Skill>().toList()
			) {
					override fun getView(i: Int, view: View?, parent: ViewGroup) : View {
						log.debug("getView($i, $view, $parent)")

						if (view == null) {
							val newView = LayoutInflater.from(this@MainActivity).run {
								inflate(R.layout.list_item_skill, parent, false)
							}
							return getView(i, newView, parent)
						}

						// view now not null for sure.

						val skill = getItem(i)!!
						val smod = character.skillModifier(skill)
						val proficiency = character.getProficiencyFor(skill).first

						log.debug("Update skill value ($skill: $smod).")

						(view.findViewById(R.id.name) as TextView).run {
							text = skill.name.toLowerCase().capitalize()
						}

						(view.findViewById(R.id.value) as TextView).run {
							/* Show skill modifier. */
							text = "%+d".format(smod)

							/* Add skill check roll. */
							setOnClickListener(OnClickRoller(
								DiceTerm(D20, SimpleDice(1, smod)),
								"Skill Check ($skill)"))
						}

						/* Highlight the item according to it's proficiency level. */
						view.setBackground(getDrawable(when (proficiency) {
							Proficiency.EXPERT -> R.drawable.bg_expert
							Proficiency.PROFICIENT -> R.drawable.bg_proficient
							else -> R.drawable.framed
						}))

						return view
					}
				}

			profList.adapter = ArrayAdapter<Skillable>(
				this@MainActivity,
				android.R.layout.simple_list_item_1,
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
		if (!isInitializedAttacks()) {
			/* Set companion object's panelAttacks label and Content. */
			panelAttacks = Pair(
				label_attacks,
				content_attacks.findViewById(R.id.list_attacks) as ViewGroup)
		}

		attacks += character.attackUnarmed
		attacks += character.attackImprovised
		attacks += character.attackSpells
		attacks += character.attackDrawNew

		if (character.attackEquipped != null) {
			attacks += character.attackEquipped!!
		}

		// inventory weapons to attack
		/*
		attacks += character.bags.values.map { it.inside }.flatten().toSet()
			.filter { it is Weapon && it != character.hands.first && it != character.hands.second }
			.map { Attack(
				(it as Weapon).name,
				ranged = !it.weaponType.melee,
				damage = it.damage to it.damageType,
				)}

		val attackPanel = panelAttacks.second as ListView

		// TODO (2020-10-07) add attack roller
		// TODO (2020-10-07) add all attacks and a listener (auto level-up/equipment update)

		attackPanel.adapter = ArrayAdapter(
			this@MainActivity,
			android.R.layout.simple_list_item_1,
			attacks)

		/* Update preview. */
		label_attacks.text = formatLabel(
			"Attacks",
			"${attacks.maxByOrNull{ it.damage.first.average }}"
			)
	}

	/** Make a weapon to an attack for the selected character. */
	private fun weaponToAttack(wpn: Weapon, str: Int = 0, dex: Int = 0) : Attack
		= Attack(
			wpn.name,
			ranged = !wpn.weaponType.melee,
			damage = wpn.damage to wpn.damageType,
			note = wpn.note,
			finesse = wpn.isFinesse,
			proficientValue = character.getProficiencyFor(wpn).second,
			modifierStrDex = str to dex)

	/** Update the "content_spells" panel.
	 * Show left and available spell slots.
	 * Show data of each learned spell and click to cast them.
	 * Here new spells which may be learnt can be added, or known spells can be prepared.
	 */
	private fun updateSpells(setListener: Boolean = false) {
		if (!isInitializedSpells()) {
			panelSpells = Pair(
				label_spells,
				contents.findViewById(R.id.content_spells) as ViewGroup)
		}

		val spellPanel = panelSpells.second

		/* Spell slots. */
		val spellSlots = spellPanel.findViewById(R.id.spell_slots) as TextView

		/* Known spells. */
		val spellList = spellPanel.findViewById(R.id.list_spells) as ListView

		/* Show left spell slots. */
		// TODO (2020-10-10) show left spell slots.
		// TODO (2020-10-10) and show left other magics sources ...

		// TODO (2020-10-10) display (different) spell casting modifier, DCs (for each casting class)

		/* Set spell list adapter (display).
		 * On click, activate spell.
		 * On longclick, replace, forget, prepare, other options...
		 */
		if (spellList.adapter == null) {
			log.debug("Set up te spellList.adapter")
			// XXX (2020-10-06) REFACTOR, proper classes. (spells)

			spellSlots.text = (0 .. 9).joinToString("") {
				it.toString()
			}

			spellList.adapter = ArrayAdapter<Spell>(
				this@MainActivity,
				R.layout.list_item_spell,
				R.id.name,
				character.spellsKnown)
		}

		/* Update preview. */
		label_spells.text = formatLabel(
			"Spells", "*Concentration*, ${character.current.spellSlot(1)}")
	}

	/** Update the "content_inventory" panel.
	 * Show currently carried weight, money and equipped items.
	 * Also show the carried or owned bags.
	 * Click a bag item, to get more options, what to do with it [equip, drop, sell].
	 * Also new items or money can be added, and equipped items can be dropped or stored.
	 */
	private fun updateInventory(setListener: Boolean = false) {
		if (!isInitializedInventory()) {
			panelInventory = Pair(
				label_inventory,
				content_inventory as ViewGroup)
		}

		log.debug("Update inventory")

		/* Weight bar. */
		(panelInventory.second.findViewById(R.id.bar_carried_weight) as ProgressBar).run {
			max = character.carryingCapacity.toInt()
			progress  = character.carriedWeight.toInt()
		}

		// XXX (2020-10-07) refactor inventory (currenlty equipped)

		/* Equipped. */
		(panelInventory.second.findViewById(R.id.equipped) as TextView).run {
			text = "Equipped and in Hands!"
		}

		// XXX (2020-10-07) refactor inventory (nested bags)

		/* List of bags. And their content. */
		(panelInventory.second.findViewById(R.id.list_bags) as ListView).run {
			val bags = character.bags.values.toList()

			/* List outer bags. */
			adapter = ArrayAdapter<Item>(
				this@MainActivity,
				android.R.layout.simple_list_item_1,
				bags)

			/* In click: container: Unfold / item: ??? */
			setOnItemClickListener {_, _, pos, _ ->
				if (bags[pos] is Container) {
					Toast.makeText(this@MainActivity, "Unfold bag", Toast.LENGTH_LONG).show()
				}
			}

			/* In click: container: Unfold / item: ??? */
			setOnItemLongClickListener {_, _, pos, _ ->
				Toast.makeText(this@MainActivity, "Open Option menu $pos", Toast.LENGTH_LONG).show()
				true
			}
		}

		label_inventory.text = formatLabel(
			"Inventory",
			"Money ${character.purse}, Bag weight ${56.6} lb")
	}

	/** Update the "content_classes" panel.
	 * Clicking on a feast will show more information about the feast.
	 * Also limited feasts can be activated.
	 * Here a new klass level can also be added.
	 */
	private fun updateKlasses(setListener: Boolean = false) {
		if (!isInitializedKlasses()) {
			panelKlasses = Pair(
				label_classes,
				content_classes as ViewGroup)
		}

		// XXX (2020-10-07) implement (content_classes)
		(content_classes.findViewById(R.id.list_classes) as ListView)
			.adapter = ArrayAdapter(
				this@MainActivity,
				// TODO (2020-10-07) complex layout with 1-level nested listviews: klass -> feats.
				android.R.layout.simple_list_item_1,
				character.klasses.keys.toList())

		label_classes.text = formatLabel(
			"Classes",
			"(${character.klassFirst}, ${character.klasses[character.klassFirst]})")
	}

	/** Update the "content_race_background" panel.
	 * Here are information about the race, the background and also pure story focussed notes.
	 * New notes can be added and also race or background features reviewed or activated.
	 */
	private fun updateStory(stnetListener: Boolean = false) {
		if (!isInitializedStory()) {
			panelStory = Pair(
				label_race_background,
				content_race_background as ViewGroup)
		}

		// XXX (2020-10-07) implement story view.
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

		var string = "${character.race}:${character.subrace} ("

		if (character.race.darkvision > 29) {
			// unicode: dark shade
			string += "${character.race.darkvision} ft \u2593, "
		}

		string += "${character.size})"
		string += "${character.background}:${character.backgroundFlavour}"

		label_race_background.text = formatLabel("About Me", string)
	}

	/** Initiate the panel with extra dice.
	 * There are extra dice and an additional custom field,
	 * to roll outside of possible ability or attack rolls. */
	private fun initiateExtraRolls() {
		/* Assign this listener to each custom dice term. */
		val extraDice = content_rolls.findViewById(R.id.extra_dice) as LinearLayout

		log.debug("Set up extra dice to roll.")

		(0 until extraDice.getChildCount()).forEach {
			extraDice.getChildAt(it).apply {
				if (this is EditText) {
					// insert term and roll it. (keep text)
					setOnKeyListener(OnKeyEventRoller())
					setTextSize(5.toFloat())

				} else if (this is TextView) {
					// simply roll the die
					val term = this.text.toString()
					val faces: Int = term.substring(1).toInt()
					setTextSize(5.toFloat())
					setOnClickListener(
						OnClickRoller(DiceTerm(faces), term))
				}
			}
		}

		log.debug("Extra Rolls are initiated.")
	}

	// TODO (2020-10-06) separate single logics. (onClick(view))
	override fun onClick(view: View) {
		log.debug("Clicked on $view")

		var context = this@MainActivity
		var (long, short) = Toast.LENGTH_LONG to Toast.LENGTH_SHORT

		when (view.getId()) {
			R.id.healthbar -> {
				// open content panel.
				panelHealth.second.toggleVisibility()
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
				closeContentsBut(R.id.content_rolls)

				/* Update roll shower. */
				val listRolls = content_rolls.findViewById(R.id.list_rolls) as ListView
				(listRolls.adapter as ArrayAdapter<*>).notifyDataSetChanged()
				listRolls.invalidateViews()
				listRolls.refreshDrawableState()
			}

			R.id.speed -> {
			  // XXX (2020-09-29) implement walk steps. (gui)
			  if (character.current.walk(ft = 5)) {
				  Toast.makeText(context, "Went 5ft", short).show()
				  (panelHealth.second.findViewById(R.id.text) as TextView).text = "${character.current.feetLeft} ft"
			  }

			}
			R.id.armorclass -> {
			  Toast.makeText(context, "Open Dresser", long).show()
			  // XXX (2020-09-29) implement equip armor, hands, etc.
			}
			else -> {
			  Toast.makeText(context, "Clicked on ${view}", long).show()
			  // TODO (2020-09-29) rest case? any click without purpose?
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
				log.debug("VISIBLE    $this")
			}
			else -> false.also {
				visibility = View.GONE
				log.debug("GONE       $this")
			}
		}
}

/** Android specific log. */
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
