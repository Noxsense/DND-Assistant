package de.nox.dndassistant.app

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.util.Log
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

import kotlinx.android.synthetic.main.activity_main.*

import de.nox.dndassistant.core.PlayerCharacter
import de.nox.dndassistant.core.Item
import de.nox.dndassistant.core.Ability
import de.nox.dndassistant.core.Proficiency
import de.nox.dndassistant.core.Spell
import de.nox.dndassistant.core.Container
import de.nox.dndassistant.core.Skill
import de.nox.dndassistant.core.Skillable
import de.nox.dndassistant.core.Weapon
import de.nox.dndassistant.core.Attack
import de.nox.dndassistant.core.playgroundWithOnyx
import de.nox.dndassistant.core.DiceTerm
import de.nox.dndassistant.core.SimpleDice
import de.nox.dndassistant.core.D20

class MainActivity : AppCompatActivity(), View.OnClickListener {

	private val log = LoggerFactory.getLogger("D&D Main")

	/* The player character. */
	private lateinit var character : PlayerCharacter

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

	private lateinit var panelSpells: Pair<TextView, ViewGroup>
		private set
		// initiated on updateSpells()

	/** Check if preview and context (spells) are initiated. */
	private fun isInitializedSpells() : Boolean = ::panelSpells.isInitialized

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

	private var attacks: List<Attack> = listOf()

	private lateinit var li: LayoutInflater

	override fun onCreate(savedInstanceState: Bundle?) {
		/* default loading. */
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		li = LayoutInflater.from(this)

		instance = this

		log.debug("Initiated Activity.")

		// XXX (2020-09-27) load character, create character.
		character = playgroundWithOnyx()

		log.debug("Player Character is loaded.")

		/* Update the character specific panels:
		 * Fill them with current character's data. */
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
		label_skills.setOnClickListener(this)
		label_attacks.setOnClickListener(this@MainActivity)
		label_spells.setOnClickListener(this)
		label_inventory.setOnClickListener(this)
		label_classes.setOnClickListener(this)
		label_race_background.setOnClickListener(this)
	}

	/** Poke the roll history view to display potentially updated entries.
	 * Also display the most recent result in the preview. */
	public fun notifyRollsUpdated() {
		if (!isInitializedRolls()) return
		val (preview, content) = panelRolls
		content.findViewById<ListView>(R.id.list_rolls).run {
			(adapter as ArrayAdapter<RollResult>).notifyDataSetChanged()

			preview.text = formatLabel(
				"Rolls & Counters",
				"Latest: ${(adapter.getItem(0) as RollResult?)?.value}")
		}
	}

	// TODO (2020-10-12) handle on resume, to reload views after standby?
	// override fun onResume()

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

		(panelAbilities as AbilitiesView).run {
			this.setScores(character)
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
		(panelHealth.second.findViewById<TextView>(R.id.speed)).apply {
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
		(panelHealth.second.findViewById<TextView>(R.id.armorclass)).apply {
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
		val resting = panelHealth.second.findViewById<LinearLayout>(R.id.resting)
		var restCount = resting.getChildCount()

		val longrest = resting.findViewById<TextView>(R.id.longrest)

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
				contents.findViewById<ViewGroup>(R.id.content_skills))
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

		val profValue = skillPanel.findViewById<TextView>(R.id.proficiency_value)
		val skillList = skillPanel.findViewById<ListView>(R.id.list_skills)
		val profList = skillPanel.findViewById<ListView>(R.id.list_proficiencies_and_languages)

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
						val newView = li.inflate(R.layout.list_item_skill, parent, false)
						return getView(i, newView, parent)
					}

					// view now not null for sure.

					val skill = getItem(i)!!
					val smod = character.skillModifier(skill)
					val proficiency = character.getProficiencyFor(skill).first

					log.debug("Update skill value ($skill: $smod).")

					(view.findViewById<TextView>(R.id.name)).run {
						text = skill.name.toLowerCase().capitalize()
					}

					(view.findViewById<TextView>(R.id.value)).run {
						/* Show skill modifier. */
						text = "%+d".format(smod)

						/* Add skill check roll. */
						setOnClickListener(OnEventRoller(
							D20.toTerm() + smod,
							"Skill Check ($skill)"))
					}

					/* Highlight the item according to it's proficiency level. */
					if (proficiency != Proficiency.NONE) {
						view.setBackground(getDrawable(when (proficiency) {
							Proficiency.EXPERT -> R.drawable.bg_expert
							else -> R.drawable.bg_proficient
						}))
					}

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
				content_attacks.findViewById<ViewGroup>(R.id.list_attacks))
		}

		attacks += character.attackUnarmed // unarmed strike
		attacks += character.attackImprovised // melee improvised (1d4)
		attacks += character.attackSpells // spells to attacks
		attacks += character.attackDrawNew // "inventory" to attacks

		if (character.attackEquipped != null) {
			attacks += character.attackEquipped!!
		}

		val attackPanel = panelAttacks.second as ListView

		// TODO (2020-10-07) add attack roller
		// TODO (2020-10-07) add all attacks and a listener (auto level-up/equipment update)

		attackPanel.adapter = object: ArrayAdapter<Attack>(
			this@MainActivity,
			R.layout.list_item_attack,
			R.id.name_attack,
			attacks){
				override fun getView(i: Int, v: View?, p: ViewGroup) : View {
					if (v == null) {
						val newView = li.inflate(R.layout.list_item_attack, p, false)
						return getView(i, newView, p)
					}

					if (i > getCount()) return v

					// view now not null for sure.

					val attack = getItem(i)!!

					(v.findViewById<TextView>(R.id.name_attack)).run {
						text = attack.name
						// setOnClickListener() // TODO (2020-10-11) // show notes.
					}

					(v.findViewById<TextView>(R.id.attack_roll)).run {
						text = "%+d".format(attack.attackBonus)
						setOnClickListener(OnEventRoller(
							D20.toTerm() + attack.attackBonus,
							"Attack with ${attack.name}"))
					}

					(v.findViewById<TextView>(R.id.attack_range))
						.text = "TODO"

					// TODO (2020-10-11) refactor: attack.ranged, add actual range for disadvantage rolls, etc?

					(v.findViewById<TextView>(R.id.damage_dice)).run {
						val dmgRoll = attack.damageRoll
						text = dmgRoll.toString()
						// TODO (2020-10-11) make depended on attack roll ? => 20 -> Crit?
						setOnClickListener(OnEventRoller(
							dmgRoll, "Damage with ${attack.name}"))
					}

					(v.findViewById<TextView>(R.id.damage_type)).run {
						val dmgTypes = attack.damageType
						text = when (dmgTypes.size) {
							0 -> "???"
							else -> dmgTypes.joinToString()
						}
					}

					(v.findViewById<TextView>(R.id.note)).run {
							/* Hide, if there is no note to show. */
							if (attack.note.trim().length < 1) {
								visibility = View.GONE
							} else {
								text = attack.note
							}
						}

					return v
				}
			}

		/* Update preview. */
		label_attacks.text = formatLabel(
			"Attacks",
			"${attacks.maxByOrNull{ it.damage.first.average }}")
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
				contents.findViewById<ViewGroup>(R.id.content_spells))
		}

		val spellPanel = panelSpells.second

		/* Spell slots. */
		val spellSlots = spellPanel.findViewById<TextView>(R.id.spell_slots)

		/* Known spells. */
		val spellList = spellPanel.findViewById<ListView>(R.id.list_spells)

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

			// TODO (2020-10-11) replace with real code instead of placeholder
			spellList.adapter = object: ArrayAdapter<Spell>(
				this@MainActivity,
				R.layout.list_item_spell,
				R.id.name,
				character.spellsKnown
			){
				override fun getView(i: Int, v: View?, p: ViewGroup): View {
					if (v == null) {
						val newView = li.inflate(R.layout.list_item_spell, p, false)
						return getView(i, newView, p)
					}

					if (i >= getCount()) return v // make sure, spell is not null

					val spell = getItem(i)!!

					/* Show spell and make clickable / interactive. */

					(v.findViewById<TextView>(R.id.name)).run {
						text = spell.name
						// setOnClickListener() // TODO (unfold info and options /replace/(un)prepare)
					}

					// TODO (2020-10-11) pretty stats.

					(v.findViewById<TextView>(R.id.stats)).run {
						var vsm = ""

						if (spell.invocationVerbal) vsm += "V"
						if (spell.invocationSomatic) vsm += "S"
						if (spell.invocationMatierial) vsm += "M (the material?)"

						text = """
							${spell.school}, ${spell.level}
							${spell.castingTime} / ${spell.ritual}, $vsm
							${spell.distance} ft (${spell.area})
							${spell.duration} sec (${spell.concentration})

							${spell.attackSave} \u21d2 ${spell.damageEffect}
							""".trimIndent()
					}

					(v.findViewById<TextView>(R.id.note)).run {
						text = spell.note
					}

					(v.findViewById<TextView>(R.id.spell_cast)).run {
						setOnClickListener {
							// XXX (2020-10-11) needs to be implemented. (normal cast on level)
							// TODO (2020-10-11) consider accidentally casted
							Toast.makeText(
								this@MainActivity,
								"Cast spell ${spell}",
								Toast.LENGTH_SHORT)
								.show()
						}
						setOnLongClickListener {
							// XXX (2020-10-11) needs to be implemented. (cast with higher level)
							Toast.makeText(
								this@MainActivity,
								"Attempt to cast spell ${spell} with higher spell slot.",
								Toast.LENGTH_SHORT)
								.show()
							true
						}
					}

					return v
				}
			}
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

		val content = panelInventory.second
		val preview = panelInventory.first

		val weightBar = content.findViewById<ProgressBar>(R.id.bar_carried_weight)
		val moneyNote = content.findViewById<TextView>(R.id.money)
		val equipment = content.findViewById<TextView>(R.id.equipped)

		val bags = panelInventory.second.findViewById<LinearLayout>(R.id.list_bags)

		/* Weight bar. */
		weightBar.run {
			max = character.carryingCapacity.toInt()
			progress  = character.carriedWeight.toInt()
		}

		/* Money. */
		moneyNote.run {
			text = "Purse: ${character.purse}"
		}

		// XXX (2020-10-07) refactor inventory (currenlty equipped)

		/* Equipped. */
		equipment.run {
			text = Html.fromHtml("""
				<b>Equipped and in Hands!</b>
				<p>(WIP)

				${character.worn}
				</p>""".trimIndent())

			setOnClickListener {
				Toast.makeText(
					this@MainActivity,
					"Open dresser", // see also armorclass
					Toast.LENGTH_SHORT
				).show()
			}
		}

		// XXX (2020-10-07) refactor inventory (nested bags)

		/* Sorted map keys. */
		val keys: List<String>
			= character.bags.keys.toList().sorted()

		/* Top level of bags. */
		val topLevel: List<String>
			= keys.filter { it.count { it == ':' }  < 2 }

		/* List of bags. And their content. */
		bags.run {
			// XXX placeholder.

			/* Check for changes. Update. */
			// TODO (2020-10-11) implement: inventory Check for changes. Update.

			removeAllViews() // clean children.

			topLevel.forEach { bagkey ->
				val bag = character.bags[bagkey]!!

				log.debug("Setup view for '$bag'")

				val bagView = createNewItemView(bags)
				bags.addView(fillItemView(bag, bagView))
			}
		}

		preview.text = formatLabel(
			"Inventory",
			"Money ${character.purse}, Carrying ${character.carriedWeight} lb")
	}

	// create a new view for an item / bag.
	private fun createNewItemView(parent: ViewGroup) : View
		= li.inflate(R.layout.list_item_item, parent, false)

	/** Fill View with Container information. */
	private fun fillItemView(item: Item, itemView: View) = itemView.run {
		log.debug("Fill data of just an item ($item).")
		(this.findViewById<TextView>(R.id.item_name))
			?.text = item.name

		/* Open dialog for more options. */
		setOnLongClickListener(onInventoryLongkListener)

		if (item is Container) {
			// go to fillBagView
			// differs in container_inside and info views, also other click.
			fillBagView(item, itemView)

		} else {
			(this.findViewById<TextView>(R.id.item_info_0))
				?.text = "${item.weight} -- ${item.cost}"

			(this.findViewById<TextView>(R.id.item_info_1))
				?.text = ""

			// remove for normal object.
			// (this as ViewGroup).removeView(itemView.findViewById(R.id.container_inside))

			this
		}
	}

	/** Fill View with normal Item information. */
	private fun fillBagView(bag: Container, bagView: View) : View = bagView.run {
		log.debug("Fill Data with Bag/Container Content ($bag).")

		this.findViewById<TextView>(R.id.item_info_0)
			?.text = "(${bag.sumWeight()} + ${bag.weight})" +
			" -- (${bag.sumCost()} + ${bag.cost})"

		this.findViewById<TextView>(R.id.item_info_1)
			?.text = "contains: ${bag.countItems} items."

		log.debug("Add panel for own content.")
		val bagViewStore = this.findViewById<LinearLayout>(R.id.container_inside)

		bagViewStore.visibility = View.VISIBLE // View.GONE

		/* add nested list for stored items. */
		bag.inside.map { stored ->
			bagViewStore.addView(
				/* inflate a new layout and add new view into container_inside. */
				fillItemView(stored, createNewItemView(bagViewStore)))
		}

		/* On click: Open / Collapse bag content. */
		setOnClickListener(onBagClickListener)

		this
	}

	/** Find own container_inside child: toggle it's visibility. */
	private val onBagClickListener: OnClickListener
		= OnClickListener { v ->
			val insideView = v.findViewById<LinearLayout>(R.id.container_inside)
			insideView?.toggleVisibility()

			log.debug("Toggle bag.container_inside, direct kids: "
				+ "${insideView?.getChildCount() ?: 0}")
		}

	/** Open dialog with more information and options. */
	private val onInventoryLongkListener: OnLongClickListener
		= OnLongClickListener { v ->
			// TODO (2020-10-12) find the concrete item / reference of item below view.

			val item: Item? = null

			log.debug("Open menu for item: $item (on $v)")

			Toast.makeText(
				this@MainActivity,
				"Open menu for item: $item (on $v)",
				Toast.LENGTH_SHORT
			).show()

			true
		}

	/** Update the "content_classes" panel.
	 * Clicking on a feast will show more information about the feast.
	 * Also limited feasts can be activated.
	 * Here a new klass level can also be added. */
	private fun updateKlasses(setListener: Boolean = false) {
		if (!isInitializedKlasses()) {
			panelKlasses = Pair(
				label_classes,
				content_classes as ViewGroup)
		}

		// XXX (2020-10-07) implement (content_classes)
		val klassList = (panelKlasses.second.findViewById<LinearLayout>(R.id.list_classes))

		// TODO (2020-10-12) update only needed.
		klassList.removeAllViews() // remove all views.

		var previewText = ""

		/* Add for each class a new List of each klass and their feats. */
		character.klasses.toList().forEach { (klass, lvlSpecial) ->
			val (lvl, special) = lvlSpecial

			previewText += ", $klass"
			if (special.trim().length > 0) previewText += " $special"
			previewText += " (lvl. $lvl)"

			val kv = li.inflate(R.layout.list_item_klass, klassList, false)

			klassList.addView(kv) // add to overview.

			kv.findViewById<TextView>(R.id.name_klass)?.run {
				text = "${klass.name} ($special $lvl)"
			}

			kv.findViewById<TextView>(R.id.description)?.run {
				text = """
					Hitdie: ${klass.hitdie}
					${klass.description}
					""".trimIndent()
			}

			val klassFeatList = kv.findViewById<LinearLayout>(R.id.list)

			klass.getFeaturesAtLevel(lvl, special).forEach { feat ->
				val view = li.inflate(R.layout.list_item_feat, klassFeatList, false)

				view.apply {
					findViewById<TextView>(R.id.name).text = feat.title
					findViewById<TextView>(R.id.level).text = "${feat.level}"
					findViewById<TextView>(R.id.description).text = "${feat.description}"

					// TODO (2020-10-12) refactor klasses and feats.
					// findViewById<TextView>(R.layout.limits).text = "LIMITS"
				}

				klassFeatList.addView(view)
			}
		}

		panelKlasses.first.text = formatLabel(
			"Classes",
			"${previewText.substring(2)}")
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
			content_race_background.findViewById<View>(id)
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
			// unicode: first quarter moon symbol
			string += "\uD83C\uDF13 ${character.race.darkvision} ft, "
		}

		string += "${character.size}) - "
		string += "${character.background}:${character.backgroundFlavour}"

		label_race_background.text = formatLabel("About Me", string)
	}

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
					// insert term and roll it. (keep text)
					setOnKeyListener(OnEventRoller(DiceTerm(0)))

				} else if (this is TextView) {
					// simply roll the die
					val term = this.text.toString()
					val faces: Int = term.substring(1).toInt()
					setOnClickListener(
						OnEventRoller(DiceTerm(faces), term))
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
				val listRolls = content_rolls.findViewById<ListView>(R.id.list_rolls)
				(listRolls.adapter as ArrayAdapter<RollResult>).notifyDataSetChanged()
				listRolls.invalidateViews()
				listRolls.refreshDrawableState()
			}

			R.id.speed -> {
			  // XXX (2020-09-29) implement walk steps. (gui)
			  if (character.current.walk(ft = 5)) {
				  Toast.makeText(context, "Went 5ft", short).show()
				  (panelHealth.second.findViewById<TextView>(R.id.text))
					.text = "${character.current.feetLeft} ft"
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
