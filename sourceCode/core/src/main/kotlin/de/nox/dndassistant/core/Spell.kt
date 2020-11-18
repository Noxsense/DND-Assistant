package de.nox.dndassistant.core

data class Spell(
	val name: String,
	val school: School,
	val casting: Casting,
	val effects: List<Effect>,
	val description: String,
	val casterKlasses: List<String>
) : Comparable<Spell> {

	enum class Area {
		SELF,
		TOUCH,
		TARGET,
		CUBE,
		CYLINDER,
		CONE,
		SPHERE; };

	/** A school of magic, which categories a certain spell and may apply own rules
	 * and believes.
	 * There are eight schools (5e SRD):
	 * - Abjuration:    Protect.
	 * - Conjuration:   Transport.
	 * - Divination     Reveal.
	 * - Enchantment:   Influence.
	 * - Evocation:     Energy.
	 * - Illusion:      Deceive.
	 * - Necromancy:    Alter the Life and Death.
	 * - Transmutation: Change.
	 */
	enum class  School(val description: String) {
		ABJURATION(
			"""
			Spells are protective in nature, though some of them have aggressive uses.
			They create magical barriers, negate harmful effects, harm trespassers,
			or banish creatures to other planes of existence.
			""".trimIndent()),
		CONJURATION(
			"""
			Spells involve the transportation of objects and creatures from one
			location to another. Some spells summon creatures or objects to the
			caster's side, whereas others allow the caster to teleport to another
			location. Some conjurations create objects or effects out of nothing.
			""".trimIndent()),
		DIVINATION(
			"""
			Spells reveal information, whether in the form of secrets long
			forgotten, glimpses of the future, the locations of hidden things, the
			truth behind illusions, or visions of distant people or places.
			""".trimIndent()),
		ENCHANTMENT(
			"""
			Spells affect the minds of others, influencing or controlling their
			behavior. Such spells can make enemies see the caster as a friend,
			force creatures to take a course of action, or even control another
			creature like a puppet.
			""".trimIndent()),
		EVOCATION(
			"""
			Spells manipulate magical energy to produce a desired effect. Some
			call up blasts of fire or lightning. Others channel positive energy to
			heal wounds.
			""".trimIndent()),
		ILLUSION(
			"""
			Spells deceive the senses or minds of others. They cause people to see
			things that are not there, to miss things that are there, to hear
			phantom noises, or to remember things that never happened. Some
			illusions create phantom images that any creature can see, but the most
			insidious illusions plant an image directly in the mind of a creature.
			""".trimIndent()),
		NECROMANCY(
			"""
			Spells manipulate the  energies of life and death. Such spells can
			grant an extra reserve of life force, drain the life energy from
			another creature, create the undead, or even bring the dead back to
			life.
			Creating the undead through the use of necromancy spells such
			as animate dead is not a good act,
			and only evil casters use such spells frequently.
			""".trimIndent()),
		TRANSMUTATION(
			"""
			Spells change the properties of a creature, object, or environment.
			They might turn an enemy into a harmless creature, bolster the strength
			of an ally, make an object move at the caster's command, or enhance a
			creature's innate healing abilities to rapidly recover from injury.
			""".trimIndent());

		override fun toString()
			= name.capitalize()

		val descriptionLine: String get()
			= description.replace("\n", " ")
	}

	private val LOG_TAG = "D&D Spell"

	override fun compareTo(other: Spell) : Int
		= when {
			level != other.level -> level - other.level // first level sort
			else -> name.compareTo(other.name) // second level sort
		}

	/* Information bundle about casting and co. */
	public class Casting(
		val duration: String = "1 action", // reaction / bonus / 1 minutes

		val ritual: Boolean = false,

		val verbal: Boolean = true,
		val somatic: Boolean = true,
		val materials: Map<String, Int> = mapOf<String, Int>() // item to min value.

		// override fun toString(): String = duration
	);

	/** Information bundle about one effect of the spell. */
	public class Effect(
		val level: Int = 1,

		val area: String = "Touch", // self (30ft radius) // 8 willing // attack target // area by beam
		val distance: Int = 0, // ft
		// TODO (2020-11-12) what is about multiple targets?
		// [ ] 3 darts (targets) + 1 each higher slot level
		// [ ] higher slots: copy with higher value or add (cumulative) ?

		val concentration: Boolean = false,
		val duration: String = "1 round",
		val result: String = "",

		val savingThrow: Ability? = Ability.DEX,
		val onSuccess: String = "No harm", // half harm
		val onFail: String = "12d6 (fire)" // paralized

		// enum class Tag { Heal, Attack+Dmg, Savable, Charm; };

		// override fun toString(): String = result
	);

	init {
		if (name.trim().length < 1) throw Exception("Spell has empty name")

		if (effects.size < 1) throw Exception("Spell has no effect.")
	}

	/** String representation of a spell: Show name. */
	override fun toString(): String
		= name

	/** Get the minimum level the spell can be cast in. */
	val level: Int get()
		= effects.minByOrNull { it.level }!!.level // must not be without effects

	/** String representation for a certain level. */
	fun showLevel(lvl: Int = level) : String = when (lvl) {
		0 -> "Cantrip"
		else -> "Level $lvl"
	}

	/** Show casting invocation components. */
	fun showInvocationComponents(): String
		= casting.let { c ->
			/* Show invocation components. */
			val V = when { c.verbal -> "V"; else -> "" }
			val S = when { c.somatic -> "S"; else -> "" }
			val M = when { c.materials.size > 0 -> "M, ${c.materials}"; else -> "" }

			/* Final String. */
			"$V$S$M"
		}

	/** Show casting time of the spell in readable English. */
	fun showCasting(): String
		= casting.let { c ->
			/* Maybe tag ritual spell. */
			val R = when { c.ritual -> "<R> "; else -> "" }

			/* Show invocation components. */
			val vsm = showInvocationComponents()

			/* Final String. */
			"${R}${c.duration} ($vsm)"
		}

	fun needsConcentration(lvl: Int = level) : Boolean
		= effects.let {
			val ofLevel = it.filter { it.level == lvl }

			if (ofLevel.size < 1) {
				needsConcentration(level) // minimum effect's concentration.
			} else {
				/* Return concentration. */
				ofLevel[0].concentration
			}
		}

	fun getDuration(lvl: Int = level) : String
		= effects.let {
			val ofLevel = it.filter { it.level == lvl }

			if (ofLevel.size < 1) {
				/* No effect on given level, use smallest known:
				 * eg. If cast on level 5, without special effects: It's just like 3, or etc. */
				getDuration(level)
			} else {
				/* Return duration. */
				ofLevel[0].duration
			}
		}

	fun showDuration(lvl: Int = level) : String
		= getDuration(lvl) // TODO (2020-11-12) currently the same.
}
