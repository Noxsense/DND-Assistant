package de.nox.dndassistant.app

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

import de.nox.dndassistant.core.Logger
import de.nox.dndassistant.core.PlayerCharacter
import de.nox.dndassistant.core.Spell
import de.nox.dndassistant.core.Ability

class SpellView : LinearLayout {

	companion object {
		protected val log : Logger = LoggerFactory.getLogger("D&D SpellView")
	}

	constructor(c: Context, attrs: AttributeSet?) : super(c, attrs);

	private val li = LayoutInflater.from(this.getContext())

	/** The loaded character. */
	private val ch: PlayerCharacter get() = CharacterManager.INSTANCE.character

	private val spellsLearnt: Map<Spell, String> get() = ch.spellsLearnt
	private val spellsKnown: List<Spell> get() = ch.spellsKnown
	private val spellsPrepared: Set<Spell> get() = ch.current.spellsPrepared
	private val spellsCast: Map<Spell, Int> get() = ch.current.spellsCast

	/* Casting string from resource. */
	private val cast: String get() = getContext().getString(R.string.cast)

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

	/** Adapter for Spell list. With custom spell list items.
	  * Use items from getItem(). */
	private val spellListAdapter: ArrayAdapter<Spell> = object: ArrayAdapter<Spell>(
		getContext(), R.layout.list_item_spell, R.id.name
	){
		/** getCount() as spellsKnown.size. */
		override public fun getCount() : Int = spellsKnown.size

		/** getItem(p0) as spellsKnown(p0). */
		override public fun getItem(p0: Int) : Spell? = spellsKnown.get(p0)

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

			/* All child views on the view. */
			val nameView = v0.findViewById<TextView>(R.id.name)
			val statsView = v0.findViewById<TextView>(R.id.stats)
			val noteView = v0.findViewById<TextView>(R.id.note)
			val sourceView = v0.findViewById<TextView>(R.id.magic_source)
			val castView = v0.findViewById<TextView>(R.id.spell_cast)

			/* If spell is null, reset view. */

			/* Show name. */
			nameView.text = spell?.name ?: "Spell"

			statsView.text = (
				"${spell?.school ?: "School"} ${when (spell?.level) {
					null -> "level"
					0 -> "cantrip"
					else -> "${spell!!.level}"
				}}\n" +
				"Duration: ${spell?.duration ?: "Duration"}" +
				"${if (spell?.concentration ?: false) " (C)" else ""} \n" +
				"Area: ${spell?.area ?: "Area"}\n" +
				"Effect: EFFECT")

			noteView.run {
				text = spell?.note ?: ""
				visibility = View.GONE // start hidden.
			}

			sourceView.text = if (spell == null) "" else spellsLearnt[spell]

			castView.text = "$cast\n(VMS)\n 1 action"

			/* Highlight spells learnt, "hide" unprepared/not active. */
			v0.alpha = when (spell == null || spell !in spellsPrepared) {
				true -> 0.33f // halfly hidden
				else -> 1.00f // fully displayed
			}

			/* Highlight spells which are currently cast. */
			v0.setBackgroundColor(when (spell == null || spell !in spellsCast) {
				true -> 0x00000000 // transparent
				else -> 0x33006666 // highlight
			})

			nameView.setOnClickListener(showClick) // hide | show source.
			castView.setOnClickListener(castClick) // cast the spell.
			// castView.setOnClickListener(cancelClick)
			// castView.setOnClickListener(prepareClick)

			return v0
		}
	}

	var previewView : TextView? = null

	/** Translate view to spell. */
	private fun viewToSpell(v: View) : Spell?
		= spellsKnown.get(
			spellListView.getFirstVisiblePosition()
			+ spellListView.indexOfChild(v.getParent() as View))

	private fun toast(str: String, long: Boolean = false)
		= Toast.makeText(getContext(), str, when (long) {
			true -> Toast.LENGTH_LONG
			else -> Toast.LENGTH_SHORT
		}).show()

	/** Show source on click. */
	private val showClick: View.OnClickListener = View.OnClickListener { v ->
		/* Show content of the connected title. */
		val parent = v.getParent() as View
		val source = parent.findViewById<TextView>(R.id.note)

		source.visibility = when (source.visibility) {
			View.VISIBLE -> View.GONE
			else -> View.VISIBLE.also {
				toast(source.text.toString(), long = true)
			}
		}

	}

	/** Cast the spell on click. */
	private val castClick: View.OnClickListener = View.OnClickListener { v ->
		/* Cast the spell of the connected spell view. */
		val spell = viewToSpell(v)

		// TODO (2020-11-04) is it possible to cast unprepared spells, eg. from scroll, magic ring, etc?

		if (spell != null && spell in spellsPrepared && ch.current.castSpell(spell)) {
			reload()
			toast("Cast $spell")
		}
	}

	/** Cancel the cast spell on click. */
	private val cancelClick: View.OnClickListener = View.OnClickListener { v ->
		/* Cancel the spell of the connected spell view. */
		val spell = viewToSpell(v)

		if (spell != null) {
			ch.current.cancelSpell(spell)
			reload()
			toast("Cancel $spell")
		}
	}

	/** Prepare or unprepare the spell on click (toggle preparetion state). */
	private val prepareClick: View.OnClickListener = View.OnClickListener { v ->
		/* Prepare the spell of the connected spell view. */
		val spell = viewToSpell(v)



		if (spell != null && ch.current.prepareSpell(spell)) {
			reload()
			toast("Prepared $spell")
		}
	}

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

	/** Generate a preview text for the spells. */
	public fun getPreview() : String {
		val spellSlotsAvailable = (1..9)
			.filter { slot(it) > 0 } // spell slots available.
			.joinToString("|", "[", "]") // [1|2|3|]

		val concentration = ch.current.spellConcentration?.first?.name ?: ""

		return "$spellSlotsAvailable${if (concentration != "") "*$concentration*" else ""}"
	}

	/** Fill the spell slot view with the current spell slot sitation. */
	public fun showSpellSlots() {

		// TODO (2020-11-04) refactor, more beautiful "Show All about this Spell Caster"

		// TODO (2020-11-04) spell casting ability (depend by the klasses, on multiclass list all of them.)
		// for all spellcassting classes

		val spellcastingAbility = Ability.CHA

		val spellcastingMod = ch.abilityModifier(spellcastingAbility)
		val spellAttackMod = ch.proficiencyBonus + spellcastingMod
		val spellDC = spellAttackMod + 9

		spellSlotView.text = (
			(1..9).joinToString(" ") { "$it".repeat(slot(it)) } + "\n\n" +
			"Prepared Cantrips: ${spellsPrepared.filter { it.level < 1 }.size}\n" +
			"Prepared Spells:   ${spellsPrepared.filter { it.level > 0 }.size}\n\n" +
			"Spellcasting Ability: $spellcastingAbility ($spellcastingMod)\n" +
			"Spellcasting Attack Mod: (+$spellAttackMod)\n" +
			"Spellcasting DC: ($spellDC)\n\n" +
			"Can cast rituals: false\n\n" + // TODO (2020-11-04) rituals
			"Sorcery Points (Metamagic): ${6}/${ch.level}\n" // TODO (2020-11-04) Metamagic and other
			)

		spellSlotView.textSize = 7.0f
	}

	/** Notify the spells changed. */
	public fun notifySpellChanged() {
		spellListView;
		spellListAdapter.notifyDataSetChanged()
	}

	/** Shortcut for current spell slots. */
	private fun slot(s: Int) : Int = ch.current.spellSlot(s)
}
