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
import de.nox.dndassistant.core.SpellcasterKlass
import de.nox.dndassistant.core.Ability

class SpellView : LinearLayout {

	companion object {
		protected val log : Logger = LoggerFactory.getLogger("D&D SpellView")
	}

	constructor(c: Context, attrs: AttributeSet?) : super(c, attrs);

	private val li = LayoutInflater.from(this.getContext())

	/** The loaded character. */
	private val ch: PlayerCharacter get() = CharacterManager.INSTANCE.character

	private val spellsLearntWith: Map<Spell, Pair<Ability, Boolean>> get() = ch.spellsLearntWith
	private val spellsLearnt: List<Spell> get() = ch.spellsLearnt
	private val spellsPrepared: Set<Spell> get() = ch.current.spellsPrepared
	private val spellsCast: Map<Spell, Pair<Int, Int>> get() = ch.current.spellsCast
	private val spellConcentration: Pair<Spell, Int>? get() = ch.current.spellConcentration

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
		/** getCount() as spellsLearnt.size. */
		override public fun getCount() : Int = spellsLearnt.size

		/** getItem(p0) as spellsLearnt(p0). */
		override public fun getItem(p0: Int) : Spell? = spellsLearnt.get(p0)

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
				"${spell?.school ?: "School"}, ${when (spell?.level) {
					null -> "Level ?"
					0 -> "Cantrip"
					else -> "Level ${spell!!.level}"
				}}\n" +
				"Duration: ${spell?.showEffectDuration() ?: "? seconds"}\n" +
				"${spell?.briefEffect()}")

			noteView.run {
				text = spell?.description ?: ""
				visibility = View.GONE // start hidden.
			}

			/* Show Spell casting ability and maybe hint other spell sources. */
			sourceView.text = spellsLearntWith[spell]?.let { (ability, slot) ->
				(ability.fullname + (if (slot) "" else " from another Source"))
			} ?: ""

			castView.text = "$cast\n${spell?.showCasting()}"

			/* Highlight spells learnt, "hide" unprepared/not active. */
			v0.alpha = when (spell == null || spell !in spellsPrepared) {
				true -> 0.33f // halfly hidden
				else -> 1.00f // fully displayed
			}

			/* Highlight spells which are currently cast. */
			v0.setBackgroundColor(when {
				spell == null || spell !in spellsCast -> 0x00000000 // transparent (uncast)
				spell == (spellConcentration?.first ?: null) -> 0x3300FF00 // highlight (cast and concentration)
				else -> 0x33FFFF00 // highlight (just cast)
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
		= spellsLearnt.get(
			spellListView.getFirstVisiblePosition()
			+ spellListView.indexOfChild(v.getParent() as View))

	private fun toast(str: String, long: Boolean = false)
		= Toast.makeText(getContext(), str, when (long) {
			true -> Toast.LENGTH_LONG
			else -> Toast.LENGTH_SHORT
		}).show()

	// TODO (2020-11-12) refactor to effects -> show next effect?
	private fun Spell.briefEffect() : String
		= ("""
		Area: area
		Save: attackSave
		Effect: effect
		""".trimIndent())

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
		val prepCantripsCnt = spellsPrepared.filter { it.level < 1 }.size

		spellSlotView.text = (
			(1..9).joinToString(" ") {
				/* Available spell slots (not yet spent) as circled numbers. */
				/* \u2776 (10102) ... \u277e (10110) */
				(10101 + it).toChar().toString().repeat(slot(it))
			} + "\n\n" +
			"Prepared Cantrips: ${prepCantripsCnt}\n" +
			"Prepared Spells:   ${prepSpellsCnt - prepCantripsCnt}\n\n") +
			spellcasterNote
			// "Sorcery Points (Metamagic): ${6}/${ch.level}\n" // TODO (2020-11-04) Metamagic and other

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
