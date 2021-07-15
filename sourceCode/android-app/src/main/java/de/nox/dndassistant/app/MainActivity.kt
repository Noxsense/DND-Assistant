package de.nox.dndassistant.app

import android.app.AlertDialog
import android.app.AlertDialog.Builder
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.GridView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import kotlinx.android.synthetic.main.activity_main.*

// import de.nox.dndassistant.core.toAttackString
import de.nox.dndassistant.core.Ability
// import de.nox.dndassistant.core.Attack
// import de.nox.dndassistant.core.CastSpell
import de.nox.dndassistant.core.Hero
import de.nox.dndassistant.core.RollHistory
import de.nox.dndassistant.core.RollingTerm
import de.nox.dndassistant.core.Number
// import de.nox.dndassistant.core.SimpleSpell
import de.nox.dndassistant.core.Speciality

class MainActivity : AppCompatActivity(), View.OnClickListener {

	private val log = LoggerFactory.getLogger("D&D Main")

	/* The player hero. */
	private val hero : Hero get() = CharacterManager.INSTANCE.hero

	companion object {
		lateinit var instance: AppCompatActivity
			private set
	}

	private lateinit var mainLayout: ViewGroup

	private lateinit var panelAbilities: View
		private set

	/** Check if preview and context (abilities) are initiated. */
	private fun isInitializedPanelAbilities() : Boolean = ::panelAbilities.isInitialized

	private lateinit var panelRolls: Pair<TextView, ViewGroup>
		private set
		// initiated on setupRollPanel()

	/** Check if preview and context (rolls) are initiated. */
	private fun isInitializedRolls() : Boolean = ::panelRolls.isInitialized

	private lateinit var li: LayoutInflater

	// Use the 'by viewModels()' Kotlin property delegate
	// from the activity-ktx artifact
	// private lateinit val model: HeroViewModel
	private lateinit var model: HeroViewModel
	private fun modelIsInitialized() = ::model.isInitialized

	/** Dialog do to display the spells.
	 *  Here spells can be viewed and prepared, unprepared, or just marked with any label. */
	private val dialogSpells: AlertDialog by lazy {
		AlertDialog.Builder(this@MainActivity).apply{
			// adapter = content.findViewById<ListView>.adapter
			setView(ListView(this@MainActivity).apply {
				adapter = ArrayAdapter<String>(
					this@MainActivity,
					// TODO (2021-05-17) prettier custom list items for spells (mark with any, prepare, unprepare..., cast on custom spell slot)
					R.layout.list_item_spell, R.id.spell_name,
					hero.spells.toList().map { it.toString() }
				)
			})
		}
		.create() // make alaert dialog
		.also { dialog ->
			dialog.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent)
		}
	}

	/** Dialog to display attacks, moves and spells castable within a round.
	 * Clicking an item, will do the action and may use up the connected resources. */
	private val dialogAttacks: AlertDialog by lazy {
		AlertDialog.Builder(this@MainActivity).apply{
			hero.updateAttacks() // XXX always fetch the latest and observe changes on your own.
			// adapter = content.findViewById<ListView>.adapter

			setView(AttackListView(this@MainActivity).apply {
				addAttacks(hero.attacks) // add all currently known attacks
			})
		}
		.create()
		.also { dialog ->
			// finalaize to access the views without show.
			dialog.create()
			dialog.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent)
		}
	}

	/** Dialog with the skills.
	 * Clicking an item will roll a d20 and add the modifier. */
	private val dialogSkills: AlertDialog by lazy {
		AlertDialog.Builder(this@MainActivity).apply{
		// adapter = content.findViewById<ListView>.adapter
		setView(ListView(this@MainActivity).apply {
			adapter = ArrayAdapter<String>(
				this@MainActivity,
				// TODO (2021-05-17) prettier custom list items for skills.
				android.R.layout.simple_list_item_1,
				hero.skillValues.toList().map { it.toString() }
			)
		})
		}
		.create() // make alaert dialog
	}

	/** Dialog with other special actions (like sneak attack rolls, lay on hands, etc.
	 */
	val dialogSpecialActions : AlertDialog by lazy {
		// XXX implement
		AlertDialog.Builder(this@MainActivity).apply{
			// adapter = content.findViewById<ListView>.adapter
			setView(ListView(this@MainActivity).apply {
				adapter = ArrayAdapter<String>(
					this@MainActivity,
					R.layout.list_item_feat, R.id.name,
					arrayOf("Som' great action", "Som' more great action.", "Using this great action redcues a counter"))
			})
		}
		.create() // make alaert dialog
		.also { dialog ->
			// finalaize to access the views without show.
			dialog.create()
			dialog.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent)
		}
	}

	/** Dialog of the inventory.
	 * Each item has different options: To be dropped (opt. also to be sold (dropped with money back)),
	 * to put into another back (or to equip of not stored in any bag, but still hold. */
	private val dialogInventory: AlertDialog by lazy {
		AlertDialog.Builder(this@MainActivity).apply{
			// adapter = content.findViewById<ListView>.adapter
			// TODO list with items and multiple buttons or onfolding more buttons.
			setView(ListView(this@MainActivity).apply {
				adapter = ArrayAdapter<String>(
					this@MainActivity,
					R.layout.list_item_item, R.id.item_name,
					hero.inventory.map { it.toString() },
				)
				setBackgroundResource(android.R.color.transparent) // "floating" items
			})
		}
		.create() // make alaert dialog
		.also { dialog ->
			// finalaize to access the views without show.
			dialog.create()
			dialog.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent)
		}
	}

	/** Dialog to display the history of rolls. */
	val dialogRolls: android.app.Dialog by lazy {
		RollingTermDialog(this@MainActivity)
	}

	/** Dialog popup for the Notes: Custom list with multiple text items and
	 *  option to append or insert a new note. */
	private val dialogStory: AlertDialog by lazy {
		AlertDialog.Builder(this@MainActivity).apply {
			setView(R.layout.dialog_story)
			// setTitle(R.string.dialog_story_title)
		}
		.create()
		.also { dialog ->
			// finalize views without showing yet.
			dialog.create()
			dialog.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent)

			val storyList = dialog.findViewById<ListView>(R.id.dialog_story_list)

			if (storyList == null) {
				Toast.makeText(instance, "Story list still null? (Version on Click)", Toast.LENGTH_SHORT).show()
			} else if (storyList.adapter == null) {
				storyList.adapter = StoryListAdapter(this@MainActivity).apply {
					addAll(listOf("Day 1\nAwoken in Catacomb.", "Day 2\n Nothing Special.", "Day 3\n Received item from nice dragon."), true)
				}
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		/* default loading. */
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		// TODO use other options of layout for smaller resolutions.

		log.debug("Initiated MainActivity.")

		li = LayoutInflater.from(this)

		instance = this@MainActivity

		mainLayout = main_layout

		// lateinit of var model.
		model =  ViewModelProvider(this)[HeroViewModel::class.java]

		/* Show Player Character Name. */
		val observingName = Observer<String> { name -> character_name.text = getString(R.string.character_name, name) }
		model.currentName.observe(this, observingName)
		model.currentName.setValue(hero.name)

		getWindowManager().getDefaultDisplay().run {
			model.currentName.setValue("h: ${getHeight()},  w: ${getWidth()}, is hdr: ${isHdr()}")
		}

		/* Ability panel. */
		if (!isInitializedPanelAbilities()) panelAbilities = abilities_grid

		val observingAbilities = Observer<List<Pair<Int, Boolean>>> { ints ->
			// TODO (2021-05-10) make it like real mvvm, set currentSTR to hero.abilities[STR] and the it will update the view.
			(panelAbilities as AbilitiesView).setScores(Ability.values().associateWith { a -> hero.abilities[a]!! })
		}

		model.currentAbilities.observe(this, observingAbilities)

		model.currentAbilities.setValue(Ability.values().map { hero.abilities[it]!! })

		// TODO (2021-05-07) remove anonoymous inplace placeholders...

		/* Show Level and XP, formatted. */
		val observingXP = Observer<Pair<Int,Int>> { (lvl, xp) ->
			experience.text = getString(R.string.level_xp).format(lvl, xp)
		}
		// TODO (2021-05-14) ViewModel xp, level

		/* Show and load note panel. */

		/* Show rolls. */

		/* Setup dialogs and views (and listeners) for spells, items, attacks and other actions. */
		setupActionsPanel()

		/* Setup health bar and hidden death save views. */
		setupHealthPanel()
		val observingHPNow = Observer<Hero> { h ->
			healthbar_new.max = h.hitpointsMax
			healthbar_new.progress = h.hitpointsNow
			hp_stats.text = "%d / %d (%+d)".format(h.hitpointsNow, h.hitpointsMax, h.hitpointsTmp)
		}

		/* Click to open view all skills. And other proficiencies. */

		profificiency.text = "__Proficiency Bonus__: %+d".format(hero.proficiencyBonus)

		// TODO 2021-01-26 action panel

		/* Search in currently displayed content view. */
		// findViewById<EditText>(R.id.search_content)

		// TODO (2020-11-22) keep screen on?
		window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
	}

	// TODO (2020-10-12) handle on resume, to reload views after standby?
	// override fun onResume()

	/** Poke the roll history view to display potentially updated entries.
	 * Also display the most recent result in the preview. */
	public fun notifyRollsUpdated() {
		if (!isInitializedRolls()) return

		val (preview, content) = panelRolls
		// content.findViewById<ListView>(R.id.list_rolls).run {
		// 	@Suppress("UNCHECKED_CAST")
		// 	(adapter as ArrayAdapter<RollResult>).notifyDataSetChanged()

		// 	/* Show last rolls, if available. */
		// 	preview.text = getString(R.string.title_rolls).format(
		// 		try { "Latest: ${(adapter.getItem(0) as RollResult?)?.value}" }
		// 		catch (e: IndexOutOfBoundsException) { "No rolls yet." }
		// 	)
		// }
	}

	/** Setup healthbar, hidden death save views and the listeners. */
	// TODO what shoud the listener change? setValue of the Viewmodel or the model underlying
	private fun setupHealthPanel() {
		/* Hide or show the more health panel.
		 * With Death Saves and Fails, and resting options. */
		healthbar_new.setOnClickListener (this@MainActivity)

		healthbar_new.max = hero.hitpointsMax
		healthbar_new.progress = hero.hitpointsNow

		// parse custom hp change (also a complete rolling term) and add (substract) it.
		// with additional option to SET (replace) the value witht he rolled result.
		hp_custom_modify.setOnKeyListener { view, code, event ->
			// if confirmed

			log.debug("$view onKeyEvent(code = $code, event = $event)")

			if (event.action == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_ENTER) {
				// modify the hero's hitpoints.
				// roll the typed in term and if it starts with an '=' the hero's HP will be replaced, otherwise just added.
				modifyHerosHitpoints(rollHealModifyIn(), replacingHitpoints)
				true
			} else {
				false // not yet consumed
			}
		}

		updateHitpointsViews()

		take_damage.setOnClickListener (this@MainActivity)
		heal.setOnClickListener (this@MainActivity)
		deathsave_fail.setOnClickListener (this@MainActivity)
		deathsave_success.setOnClickListener (this@MainActivity)
		deathsave_roll.setOnClickListener (this@MainActivity)
		deathsave_overview.setOnClickListener (this@MainActivity)
	}

	/** Resulting text of the  @see hp_custom_modify TextView. */
	private val healModifyIn: String
		get() = hp_custom_modify.text.toString()

	/**
	If the healModifyIn term starts with a equal mark, the rolled result will replace the current hitpoints.
	 * This value checks, if the term is a replacing term or a modifying term.  */
	private val replacingHitpoints: Boolean
		get() = healModifyIn.trim().startsWith("=")

	/** The resulting term, which will be pasred from the text input on @see hp_custom_modify. */
	private val modifyInTerm: RollingTerm
		get() = try {
				// remove optional equal signs in the beginning.
				RollingTerm.parse(healModifyIn.dropWhile { c -> c == ' ' || c == '=' }) }
			catch (e: Exception) {
				Number(0)
			}

	/**
	 * Roll a term which was parsed from the @see hp_custom_modify TextView.
	 * @param method If it is (-1) the result is damage damage, on (+1) it will be healed and on (0) it will be replaced.
	 * @return returns the resulting number, only (-1) and (+1) may modify the output.
	 */
	private fun rollHealModifyIn(method: Int = 1) : Int
		= Utils.showRolledTerm(
			mainLayout,
			when (method) {
				-1 -> "Rolled Hitpoint Reduction"
				+1 -> "Rolled Hitpoint Restoration"
				0 -> "Rolled updated Hitpoints"
				else -> "Modify Hitpoints?"
			},
			if (method >= 0) (modifyInTerm) else (-modifyInTerm)
		).first

	/**
	 * Modify the connected hero's hitpoints.
	 * @param value value to add or reduce.
	 * @param replacing if replacing is true, the curren hitpoints will be replaced by the value, otherwise just updated.
	 */
	private fun modifyHerosHitpoints(value: Int, replacing: Boolean = false) {
		hero.hitpointsNow = when (replacing) {
			true -> value
			else -> hero.hitpointsNow + value
		}
		updateHitpointsViews()
	}

	/** Display the updated hero's hitpoints. */
	private fun updateHitpointsViews() {
			healthbar_new.progress = hero.hitpointsNow;
			hp_stats.text = "%d / %d (%+d)".format(hero.hitpointsNow, hero.hitpointsMax, hero.hitpointsTmp)
	}

	/** Setup panel and buttons and dialogs for actions, spells and items, and resource counters. */
	private fun setupActionsPanel() {

		// Display spells (school, levels, description, option to equip / unequip / add label )
		// click: DESCRIPTION (unfold description)
		// long click: LABEL (open options to equip, unequip, cast from this menu, mark or other labels.)

		// Display inventory
		// XXX Prohibit mutally storing items

		// Display Counter: [Name, Counter, Reset Time] - Add (if counting) / Reduce if countdown.
		grid_counters.adapter = object: ArrayAdapter<Speciality>(
			this@MainActivity,
			android.R.layout.simple_list_item_1,
			hero.specialities,
			) {}
		grid_counters.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
			// reduce resource manually.
			hero.specialities[position]?.let { c -> (c).countDown() }
			(grid_counters.adapter as ArrayAdapter<Any>?)?.notifyDataSetChanged()
			// Toast.makeText(this@MainActivity, hero.specialities[position]?.toString() ?: "No Counter!", Toast.LENGTH_SHORT).show()
		}
		grid_counters.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, position, _ ->
			// reset resource manually.
			hero.specialities[position]?.let { c ->
				(c as Speciality).resetCounter()
				(grid_counters.adapter as ArrayAdapter<Any>?)?.notifyDataSetChanged()
				true
			} ?: false
		}
	}

	// TODO (2020-10-06) separate single logics. (onClick(view))
	/** Managing all main views' clicks. */
	override fun onClick(view: View) {
		log.debug("Clicked on $view")

		when (view.getId()) {
			R.id.view_story -> {
				dialogStory.show() // show notes.
			}

			/* Unfold health and death panel view. */
			R.id.healthbar_new -> {
				more_health_panel.toggleVisibility()
			}

			/* Reduce health points. If more-healthpanel is open, use input field. */
			R.id.take_damage -> {
				// TODO ViewModel
				modifyHerosHitpoints(
					when {
						more_health_panel.visibility == View.VISIBLE && !replacingHitpoints -> {
							// maybe roll the value to reduce
							rollHealModifyIn(-1) // reduce
						}
						else -> -1
					})
			}

			/* Increase health points. If more-healthpanel is open, use input field. */
			R.id.heal -> {
				// TODO ViewModel
				modifyHerosHitpoints(
					when {
						more_health_panel.visibility == View.VISIBLE && !replacingHitpoints -> {
							// maybe roll the value to reduce
							rollHealModifyIn(+1) // heal
						}
						else -> 1
					})
			}

			/* Add death save success. */
			R.id.deathsave_fail -> {
				// TODO ViewModel
				deathsave_overview.text = deathsave_overview.text.toString() + "X"
			}

			/* Add death save fail. */
			R.id.deathsave_success -> {
				// TODO ViewModel
				deathsave_overview.text = deathsave_overview.text.toString() + "O"
			}

			/* Roll death save throw and automatically enter the resul. */
			R.id.deathsave_roll -> {
				// TODO ViewModel
				val result = Utils.showRolledTerm(mainLayout, "Death Saving Throw", RollingTerm.D20).first
				when (result) {
					in 0..10 -> deathsave_success.performClick() // success
					else -> deathsave_fail.performClick() // fail
				}
			}

			/* Reset the death saves. */
			R.id.deathsave_overview -> {
				// TODO ViewModel
				deathsave_overview.text = ""
			}

			/* Click to open a view all skills. And other proficiencies. And Edit them */
			R.id.profificiency_language_skills -> {
				Toast.makeText(this@MainActivity, "Edit proficiencies.", Toast.LENGTH_SHORT).show()
			}

			/* Dialog with attacks (list). */
			R.id.action_attack -> {
				dialogAttacks.show()
			}

			/* Open roll history in dialog. */
			R.id.dialog_dice_open -> {
				// rollHistoryAdapter.notifyDataSetChanged()
				dialogRolls.show() // show newly created dialog
			}

			/* Dialog with spells (read, prepare and mark). */
			R.id.action_spells -> {
				dialogSpells.show() // show newly created dialog
			}

			/* Dialog with other actions (like sneak attack rolls, lay on hands, etc.) */
			R.id.action_special -> {
				// XXX good code.
				dialogSpecialActions .show() // show newly created dialog
			}

			/* Dialog with roll history. */
			R.id.action_inventory -> {
				dialogInventory.show()
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

// TODO own file:
// TODO (2021-05-17) each property or whole hero?
class HeroViewModel: ViewModel() {
	// val
	val currentName: MutableLiveData<String> by lazy {
		MutableLiveData<String>()
	}

	val currentAbilities: MutableLiveData<List<Pair<Int, Boolean>>> by lazy {
		MutableLiveData<List<Pair<Int, Boolean>>>()
	}
}
