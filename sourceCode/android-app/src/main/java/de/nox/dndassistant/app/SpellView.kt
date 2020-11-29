package de.nox.dndassistant.app

import android.app.AlertDialog
import android.app.AlertDialog.Builder
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Filter
import android.widget.Filter.FilterResults
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

import de.nox.dndassistant.core.Ability
import de.nox.dndassistant.core.D20
import de.nox.dndassistant.core.DiceTerm
import de.nox.dndassistant.core.Logger
import de.nox.dndassistant.core.PlayerCharacter
import de.nox.dndassistant.core.d
import de.nox.dndassistant.core.Spell
import de.nox.dndassistant.core.SpellcasterKlass

class SpellView : LinearLayout {

	companion object {
		protected val log : Logger = LoggerFactory.getLogger("D&D SpellView")
	}

	constructor(c: Context, attrs: AttributeSet?) : super(c, attrs);

	private val li = LayoutInflater.from(getContext())

	/** The loaded character. */
	private val ch: PlayerCharacter get() = CharacterManager.INSTANCE.character

	private val spellsLearntWith: Map<Spell, Pair<Ability, Boolean>> get() = ch.spellsLearntWith
	private val spellsLearnt: List<Spell> get() = ch.spellsLearnt
	private val spellsPrepared: Set<Spell> get() = ch.current.spellsPrepared
	private val spellsCast: Map<Spell, Pair<Int, Int>> get() = ch.current.spellsCast
	private val spellConcentration: Pair<Spell, Int>? get() = ch.current.spellConcentration

	/* Casting string from resource. */
	private val castSym: String get() = getContext().getString(R.string.cast)

	/** View to show spell slots. */
	private val spellSlotView: TextView by lazy {
		findViewById<TextView>(R.id.spell_slots)
	}

	/** View to show spell list. */
	private val spellListView: ListView by lazy {
		findViewById<ListView>(R.id.list_spells).also {
			it.adapter = spellListAdapter
		}
	}

	/** A dialog to show more information and options for a selected spell. */
	private val spellShowDialog: AlertDialog by lazy {
		Builder(getContext())
			.setView(li.inflate(R.layout.dialog_spell_cast, null))
			.create()
	}

	/** In Spell Adapter currently displayed spells. */
	private var spellsList: List<Spell> = spellsLearnt // start with all.

	/** Adapter for Spell list. With custom spell list items.
	  * Use items from getItem(). */
	private val spellListAdapter: ArrayAdapter<Spell> = object: ArrayAdapter<Spell>(
		getContext(), R.layout.list_item_spell, R.id.name
	){
		private var filter: Filter = object: Filter() {
			override fun publishResults(constraint: CharSequence, results: Filter.FilterResults): Unit {
				spellsList = results.values as List<Spell>
				notifyDataSetChanged()
			}
			override protected fun performFiltering(constraint: CharSequence): Filter.FilterResults {
				var results: FilterResults = FilterResults()

				var filteredSpells: List<Spell> = when {
					constraint.length < 1 -> spellsLearnt
					else -> {
						// TODO (2020-11-22) a more complex filter with keyword supported search.
						// example: L<8, ATK=melee|ranged SAVE=STR|DEX|CON|WIS|INT|CHA RANGE<120 DURATION<1h ...
						spellsLearnt.filter { it.name.contains(constraint, true) }
					}
				}

				results.count = filteredSpells.size
				results.values = filteredSpells

				return results
			}
		}

		override public fun getFilter() : Filter
			= filter

		/** getCount() as spellsLearnt.size. */
		override public fun getCount() : Int = spellsList.size

		/** getItem(p0) as spellsLearnt(p0). */
		override public fun getItem(p0: Int) : Spell? = spellsList.get(p0)

		/** True if the item is not a separator. */
		override public fun isEnabled(p0: Int) : Boolean = false

		/** getView(p0,v0?,p1) as inflate(list_item_spell). */
		override public fun getView(p0: Int, v0: View?, p1: ViewGroup) : View {
			if (v0 == null) {
				log.debug("Creating a new view for Spell Nr. $p0.")
				/* new view. */
				val v1 = li.inflate(R.layout.list_item_spell, p1, false)
				return getView(p0, v1, p1) // return newly created/inflated v1
			}

			// v0 is not empty, otherwise returned to be not null in previous check.

			/* Fill view with current spell. */
			val spell: Spell? = getItem(p0) // get listed spell.
			return fillSpellListItem(spell, v0)
		}
	}

	/** Fill the given view with a spell. */
	fun fillSpellListItem(spell: Spell?, v0: View) : View {
		/* Return the spell, before filling, if it doesn't fit the id */
		if (v0.id != R.id.spell_item_layout) {
			return v0
		}

		/* If spell is null, reset view. */
		val firstOptionalRoll: DiceTerm? = spell?.effects
			?.map { it.optionalRolls }
			?.filter { it != null }
			?.firstOrNull()

		v0.setOnClickListener(showMoreClick)

		/* Show name. */
		v0.findViewById<TextView>(R.id.spell_name).run {
			text = spell?.name ?: "Spell"
		}

		/* Show (normal) effect (on success).
		 * Open dialog to cast with more options, or attacking and optinal rolls. */
		v0.findViewById<TextView>(R.id.spell_on_success).run {
			text = (spell?.effects?.first()?.onSuccess ?: "") +
			(firstOptionalRoll?.let { " ($it)" } ?: "")
		}

		/* Show (normal) duration. */
		v0.findViewById<TextView>(R.id.spell_duration)
			.text = spell?.showEffectDuration() ?: "?"

		/* Show (normal) Area. */
		v0.findViewById<TextView>(R.id.spell_area)
			.text = spell?.showEffectArea()?.toString() ?: "?"

		/* School, Level */
		v0.findViewById<TextView>(R.id.brief_spell_school_minlvl)
			.text = ("${spell?.school?.name?.substring(0, 3)?.toLowerCase() ?: "xxx"}:${spell?.minlevel ?: -1}")

		/* Cast the spell with the minmum level (or higher, if minimum is not available. */
		v0.findViewById<View>(R.id.spell_quick_cast).run {
			setOnClickListener(quickCastClick)
		}

		/* Cancel the spell, hide if the spell is not cast. */
		v0.findViewById<View>(R.id.spell_cancel).run {
			setOnClickListener(cancelClick)
			setVisible(spell != null && spellsCast.containsKey(spell))
		}

		/* Highlight spells learnt, "hide" unprepared/not active. */
		v0.alpha = when (spell == null || spell !in spellsPrepared) {
			true -> 0.33f // halfly hidden
			else -> 1.00f // fully displayed
		}

		/* Highlight spells which are currently cast. */
		v0.setBackgroundColor(when {
			spell == null || spell !in spellsCast -> 0x00000000 // transparent (uncast)
			spell == (spellConcentration?.first) -> 0x3300FF00 // highlight (cast and concentration)
			else -> 0x33FFFF00 // highlight (just cast)
		})

		return v0
	}

	/** Fill the dialog with the current spell.
	 * @return the filled dialog. */
	fun fillSpellDialog(spell: Spell?) : AlertDialog {
		val v0 = spellShowDialog

		v0.create()

		/* End early, make sure, below is not null. */
		if (spell == null) return v0

		/* Show the full data and clickable options for the spell in a diaolog.
		 * Options:
		 * - if Attack Spell: An Spell Attack Roller.
		 * - if something else needs to be rolled: other rolls.
		 * - casting the spell on higher spell slots (if it's not a cantrip.)
		 * - Show hints about the school and the counter and anything.
		 */

		val minlevel = spell.minlevel
		val needsAttack = spell.effects.any { it.needsAttack }
		val source = spellsLearntWith[spell]!! // shouldn't be null, since only learnt spells are displayed.
		val spellAbility = source.first

		/* If spell is null, reset view. */
		val firstOptionalRoll: DiceTerm? = spell.effects
			.map { it.optionalRolls }
			.filter { it != null }
			.firstOrNull()

		/* Show School, Level and Spell casting ability (maybe also if learnt by another source). */
		v0.findViewById<TextView>(R.id.spell_school_minlvl)
			.text = spell.let {
				val source = spellAbility.fullname + (if (source.second) "" else " ?/?")

				val school = spell.school
				val lvl = if (minlevel < 1) "Cantrip"  else "Lvl $minlevel"

				"$school $lvl, $source"
			}

		/* Show name. */
		v0.findViewById<TextView>(R.id.spell_dailog_title)
			.text = spell.name

		/* Toggle Prepare the spell with the minmum level. */
		v0.findViewById<CheckBox>(R.id.spell_try_prepare).run {
			setOnClickListener { _ -> when (ch.current.toggleSpellPreparation(spell)) {
					true -> toast("Prepared $spell")
					else -> toast("Unprepared $spell")
				}
				reload()
			}
			setChecked(spell in spellsPrepared)
		}

		/* Show the full description. */
		v0.findViewById<TextView>(R.id.spell_description)
			.text = spell.description

		val slotView = v0.findViewById<SeekBar>(R.id.spell_on_higher_level_slots)

		/* Add a handy option to as more powerfull spells (use higher spell slots). */
		if (minlevel > 0) {
			// slotView.min = minlevel
			slotView.progress = minlevel
		}
		slotView.setVisible(minlevel > 0)

		/* Show selected level's effects. */
		v0.findViewById<TextView>(R.id.spell_on_higher_level_text)
			.text = spell.getEffect(slotView.progress).show()

		/* Cast the Spell. */
		v0.findViewById<TextView>(R.id.spell_dialog_cast).run {
			// XXX (2020-11-26) avoid anonymous OnClickListener
			setOnClickListener { _ ->
				val lvl = when {
					minlevel == 0 -> 0
					else ->  v0.findViewById<SeekBar>(R.id.spell_on_higher_level_slots).progress
				}
				castSpell(spell, lvl)
			}

			val c = spell.casting

			text = "Cast now (${c.duration}, ${c.VSM})"

			/* Show Spell casting components, and maybe if ritual or not etc. */
			v0.findViewById<TextView>(R.id.spell_casting_components).run {
				if (c.needsMaterials) {
					text = c.materials.toList().toString()
					visibility = View.VISIBLE
				} else {
					visibility = View.GONE
				}
			}
		}

		/* Hide or show optional rolls. */
		if (firstOptionalRoll != null || needsAttack) {
			v0.findViewById<View>(R.id.spell_more_options).setVisible(true)

			/* Roll a spell attack. */
			// XXX (2020-11-26) tidy up anonymous OnClickListener.
			// TODO (2020-11-26) feature: On long click, try to add a attack shortcut.
			v0.findViewById<View>(R.id.spell_attack).run {
				setOnClickListener(OnEventRoller.Builder(D20)
					.addDiceTerm(d(1, ch.proficiencyBonus + ch.abilityModifier(spellAbility))) // spell attack bonus
					.setReasonString("Spell Attack") // cast the spell.
					.create())
				// deactivate if not a spell attack.
				setVisible(needsAttack)
			}

			/* Roll optional dice term. */
			v0.findViewById<TextView>(R.id.spell_optional_rolls).run {
				if (firstOptionalRoll != null) {
					setOnClickListener(
						OnEventRoller.Builder(firstOptionalRoll)
							.setReasonString("Rolled for ${spell.name}") // cast the spell.
							.create())
					text = "Roll '$firstOptionalRoll'"
					visibility = View.VISIBLE
				} else {
					// deactivate if not needed.
					visibility = View.GONE
				}
			}
		} else {
			/* Hide optional buttons. */
			v0.findViewById<View>(R.id.spell_more_options).setVisible(false)
		}

		return v0
	}

	fun View.setVisible(show: Boolean)
		= run { visibility = if (show) View.VISIBLE else View.GONE }

	var previewView : TextView? = null

	/** Translate view to spell. */
	private fun viewToSpell(v: View) : Spell?
		= (spellListView.getFirstVisiblePosition() // which spells are displayed
		+ spellListView.indexOfChild(v.getParent() as View) // spell list item's position.
		).let { index -> when {
				index >= spellsList.size || index < 0 -> null
				else -> spellsList.get(index)
			}.also {
				log.debug("$it = spellsList[$index] = viewToSpell($v)")
			}
		}

	private fun toast(str: String, long: Boolean = false)
		= Toast.makeText(getContext(), str, when (long) {
			true -> Toast.LENGTH_LONG
			else -> Toast.LENGTH_SHORT
		}).show()

	/** Show source on click. */
	private val showMoreClick: View.OnClickListener = View.OnClickListener { v ->
		/* Open the long description and casting options on click. */
		// val item = (v.getChildAt(0)) as View
		// val spell = viewToSpell(item) // more nested

		val spell = (spellListView.getFirstVisiblePosition() // which spells are displayed
		+ spellListView.indexOfChild(v as View) // spell list item's position.
		).let { index -> when {
				index >= spellsList.size || index < 0 -> null
				else -> spellsList.get(index)
			}.also {
				log.debug("$it = spellsList[$index] = viewToSpell($v)")
			}
		}

		if (/*item != null &&*/ spell != null) {
			fillSpellDialog(spell).show()
		}
	}

	/** Just Cast the spell with the lowest possible spell level, or next available one. */
	private val quickCastClick: View.OnClickListener = View.OnClickListener { v ->
		val spell = viewToSpell((v.getParent() as View).getParent() as View) // more nested

		if (spell != null && spell in spellsPrepared) {
			var bestLvl = spell?.minlevel ?: 0

			/* Find the next higher spell slot, to cast the spell,
			 * when the needed spell slot is not longer available. */
			while (ch.current.spellSlot(bestLvl) < 1 && bestLvl in 0..9) {
				bestLvl += 1
			}

			log.debug("Quickly cast '$spell', using slot $bestLvl (try to)")

			castSpell(spell, bestLvl)
		}
	}

	/** Cast the spell on given level, or minimum level. */
	private fun castSpell(spell: Spell?, level: Int) : Boolean {
		if (!isPrepared(spell)) {
			log.debug("Spell cannot be cast, it's not prepared. ($spell)")
			return false
		}

		val minlevel = spell!!.minlevel
		val higher = (level - minlevel).let { when { it < 0 -> 0; else -> it } }

		log.debug("Cast '$spell', using slot $level (try to)")

		return ch.current.castSpell(spell, false, higher).let { success ->
			if (success) reload()
			toast("Spell '$spell' is ${when {success -> "Cast"; else -> "Not Cast"}}")
			success
		}
	}

	/** Cancel the cast spell on click. */
	private val cancelClick: View.OnClickListener = View.OnClickListener { v ->
		/* Cancel the spell of the connected spell view. */
		val spell = viewToSpell((v.getParent() as View).getParent() as View) // more nested

		if (spell != null) {
			ch.current.cancelSpell(spell)
			reload()
			toast("Cancel $spell")
		}
	}

	/** Prepare or unprepare the spell on click (toggle preparetion state). */
	private val togglePreparedClick: View.OnClickListener = View.OnClickListener { v ->
		/* Prepare the spell of the connected spell view. */
		val spell = viewToSpell((v.getParent() as View).getParent() as View) // more nested

		if (spell != null) {
			when (ch.current.toggleSpellPreparation(spell)) {
				true -> toast("Prepared $spell")
				else -> toast("Unprepared $spell")
			}
			reload()
		}
	}

	/** Show current spell slots and reload the spell view. */
	public fun reload() {
		/* Show preview, if given. */
		previewView?.text = "%s (%s)".format(
			context.getString(R.string.title_spells),
			getPreview())

		/* Show left spell slots. */
		showSpellSlots()

		/* Show cast and known and preparable spells. */
		notifySpellChanged()
	}

	/** Sort spells again. */
	public fun sortSpells() {
		/* Sort the spells. */
		spellsList = spellsList.sortedWith(ch.current.spellComperator)
		notifySpellChanged()
	}

	/** Generate a preview text for the spells. */
	public fun getPreview() : String {
		val spellSlotsAvailable = (1..9)
			.filter { slot(it) > 0 } // spell slots available.
			.joinToString("|", "[", "]") // [1|2|3|]

		val concentration = spellConcentration?.first.let { c ->
			when { c == null -> ""; else -> " ${187.toChar()}${c!!.name}${171.toChar()}" }
		}

		return "$spellSlotsAvailable${concentration}"
	}

	/** Fill the spell slot view with the current spell slot sitation. */
	public fun showSpellSlots() {
		// TODO (2020-11-04) refactor, more beautiful "Show All about this Spell Caster"

		// TODO (2020-11-04) spell casting ability (depend by the klasses, on multiclass list all of them.)
		// for all spellcassting classes

		val profBonus = ch.proficiencyBonus

		val spellcasterNote = ch.klasses
			.filterKeys { it is SpellcasterKlass }
			.toList()
			.joinToString("\n") { (k, more) ->
				val ability = (k as SpellcasterKlass).spellcastingAbility
				val mod = ch.abilityModifier(ability)
				val atk = profBonus + mod
				val dc = mod + 9

				/* long version.
				( "Spellcasting Ability: $spellcastingAbility ($spellcastingMod)\n"
				+ "Spellcasting Attack Mod: (+$spellAttackMod)\n"
				+ "Spellcasting DC: ($spellDC)\n\n" +
				+ "Can cast rituals: false\n\n"
				)
				*/

				val (lvl, special) = more

				/* Brief version. */
				("Caster $k: (${ability}) "
				+ "=> %+d / Atk %+d / DC %d ".format(mod, atk, dc)
				+ "/ Has: ${k.spellsKnownAt(lvl)}"
				+ "/ Prepares: ${k.spellSwap} per LR"
				+ (if (k.spellRitual) "/ Rituals" else ""))
			}

		val prepSpellsCnt = spellsPrepared.size
		val prepCantripsCnt = spellsPrepared.filter { it.minlevel < 1 }.size

		spellSlotView.text = (
			(1..9).joinToString(" ") {
				/* Available spell slots (not yet spent) as circled numbers. */
				/* \u2776 (10102) ... \u277e (10110) */
				(10101 + it).toChar().toString().repeat(slot(it))
			} + "\n\n" +
			"Prepared Cantrips: ${prepCantripsCnt}\n" +
			"Prepared Spells:   ${prepSpellsCnt - prepCantripsCnt}\n\n")
			spellcasterNote
			// "Sorcery Points (Metamagic): ${6}/${ch.level}\n" // TODO (2020-11-04) Metamagic and other

		// spellSlotView.textSize = 7.0f
	}

	/** Notify the spells changed. */
	public fun notifySpellChanged() {
		spellListView;
		spellListAdapter.notifyDataSetChanged()
	}

	/** Filter the spell list view by a given char sequence. */
	public fun filterSpells(cs: CharSequence) {
		spellListAdapter.getFilter().filter(cs)
	}

	/** Shortcut for current spell slots. */
	private fun slot(s: Int) : Int = ch.current.spellSlot(s)

	/** Check, if a spell is prepared. */
	private fun isPrepared(spell: Spell?) = spell != null && spell in spellsPrepared

	/** Get the maximum of two numbers. */
	private fun max(a: Int, b: Int) = when { a >= b -> a; else -> a; } }
