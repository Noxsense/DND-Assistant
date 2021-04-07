package de.nox.dndassistant.core

data class SimpleSpell(
	val name: String, // key => equality
	val school: String,
	val castingTime: String,
	val ritual: Boolean,
	val components: Components,
	val range: String,
	val duration: String,
	val concentration: Boolean,
	val description: String,
	val levels: Map<Int, Map<String, Any>>, // levels this spell can be cast with, and their optionally additional effects each level [<Effect Name, Effect Values>]
	val optAttackRoll: Boolean = false,
	val optSpellDC: Boolean = false, // needs spell DC to be used.
	val klasses: Set<Pair<String, String>> = setOf() // set of <klasses, sublasses> that can learn that spell.
	) {

	public companion object {
	}

	public data class Components(
		val verbal: Boolean,
		val somatic: Boolean,
		val materialGP: List<Pair<String, Int>>, // material and least worth (if necessary)
	) {

		/** Custom constructer with no attribute. */
		public constructor() : this(false, false, listOf());

		public companion object {
			/* Only verbal components. */
			val V = Components(true, false, listOf())

			/* Only somatic components. */
			val S = Components(false, true, listOf())

			/** Only Material Components. */
			fun M(materialGP: List<Pair<String, Int>>) = Components(false, false, materialGP)

			/* Verbal somatic components. */
			val VS = Components(true, true, listOf())

			/** Verbal, Material Components. */
			fun VM(materialGP: List<Pair<String, Int>>) = Components(true, false, materialGP)

			/** Somatic, Material Components. */
			fun SM(materialGP: List<Pair<String, Int>>) = Components(false, true, materialGP)

			/** Verbal, Somatic, Material Components. */
			fun VSM(materialGP: List<Pair<String, Int>>) = Components(true, true, materialGP)
		}

		val leastWorthGP: Int = materialGP.fold(0) { b, i -> b + i.second }
		val material: List<String> = materialGP.map { it.first }

		override public fun toString() : String
			= ( (if (verbal) "V" else "")
			+ (if (somatic) "S" else "")
			+ (if (material.size > 0) "M" + (if (leastWorthGP > 0) "gp" else "") else "")
			)
	};

	/** Representation. */
	public override fun toString() = this.name

	/** Equality by name. Name is the identificator. */
	public override fun equals(other: Any?) = other != null && other is SimpleSpell && other.name == this.name

	/** Minimum level this spell needs to be cast with.
	 * Interpreting negeativly given levels as klass levels for levelling cantrips.
	 */
	val baseLevel: Int get() = levels.keys.minOrNull()?.let { min ->
		if (min < 0) 0 else min // negative level => abs(level) as Klass' level; base level is 0
	} ?: 1


	/** Magic Schools of each Spell. */
	public enum class School(val description: String) {
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
}
