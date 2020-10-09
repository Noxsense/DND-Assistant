package de.nox.dndassistant.core

data class Spell(
	val name: String,
	val school: School,
	val level: Int,
	val invocationVerbal: Boolean,
	val invocationSomatic: Boolean,
	val invocationMatierial: Boolean,
	val castingTime: String,
	val distance: Int, // feet
	val area: Area = Area.CUBE,
	val duration: Int = 0, // seconds.
	val concentration: Boolean = false,
	val ritual: Boolean = false,
	val attackSave: Ability? = null,
	val damageEffect: String = "",
	// spellTag: String = "", // heal, damage, social, buff, debug, environmental, poison, ...
	val note: String = ""
	)
	: Comparable<Spell> {

	enum class Area {
		SELF,
		TOUCH,
		CUBE,
		CYLINDER,
		CONE,
		SPHERE; };

	enum class School {
		ABJURATION,
		CONJURATION,
		DIVINATION,
		ENCHANTMENT,
		EVOCATION,
		ILLUSION,
		NECROMANCY,
		TRANSMUTATION; };

	private val LOG_TAG = "D&D Spell"

	override fun compareTo(other: Spell) : Int
		= level - other.level

	override fun toString()
		= "$name (" + when (level) {
			0 -> "Cantrip"
			1 -> "1st-level"
			2 -> "2nd-level"
			3 -> "3rd-level"
			else -> "${level}th-level"
		} + " $school)"
}

/** A school of magic, which categories a certain spell and may apply own rules
 * and believes.
 * There are eight schools (5e SRD):
 * - Abjuration:    Protect.
 * - Conjuration:   Transport.
 * - Divination     Reveal.
 * - Enchantment:   Influence.
 * - Evocation:     Energy.
 * - Illusion:      Deceive.
 * - Necromancy:    Revive.
 * - Transmutation: Change.
 */
enum class  SchoolOfMagic(val description: String) {
	ABJURATION(
		"""
		spells are protective in nature, though some of them have aggressive uses.
		They create magical barriers, negate harmful effects, harm trespassers,
		or banish creatures to other planes of existence.
		""".trimIndent()),
	CONJURATION(
		"""
		spells involve the transportation of objects and creatures from one
		location to another. Some spells summon creatures or objects to the
		caster's side, whereas others allow the caster to teleport to another
		location. Some conjurations create objects or effects out of nothing.
		""".trimIndent()),
	DIVINATION(
		"""
		spells reveal information, whether in the form of secrets long
		forgotten, glimpses of the future, the locations of hidden things, the
		truth behind illusions, or visions of distant people or places.
		""".trimIndent()),
	ENCHANTMENT(
		"""
		spells affect the minds of others, influencing or controlling their
		behavior. Such spells can make enemies see the caster as a friend,
		force creatures to take a course of action, or even control another
		creature like a puppet.
		""".trimIndent()),
	EVOCATION(
		"""
		spells manipulate magical energy to produce a desired effect. Some
		call up blasts of fire or lightning. Others channel positive energy to
		heal wounds.
		""".trimIndent()),
	ILLUSION(
		"""
		spells deceive the senses or minds of others. They cause people to see
		things that are not there, to miss things that are there, to hear
		phantom noises, or to remember things that never happened. Some
		illusions create phantom images that any creature can see, but the most
		insidious illusions plant an image directly in the mind of a creature.
		""".trimIndent()),
	NECROMANCY(
		"""
		spells manipulate the  energies of life and death. Such spells can
		grant an extra reserve of life force, drain the life energy from
		another creature, create the undead, or even bring the dead back to
		life.
		Creating the undead through the use of necromancy spells such
		as animate dead is not a good act,
		and only evil casters use such spells frequently.
		""".trimIndent()),
	TRANSMUTATION(
		"""
		spells change the properties of a creature, object, or environment.
		They might turn an enemy into a harmless creature, bolster the strength
		of an ally, make an object move at the caster's command, or enhance a
		creature's innate healing abilities to rapidly recover from injury.
		""".trimIndent());

	override fun toString()
		= name.capitalize()

	val descriptionLine: String get()
		= description.replace("\n", " ")
}
