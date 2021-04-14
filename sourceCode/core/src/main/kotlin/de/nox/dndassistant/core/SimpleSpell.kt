package de.nox.dndassistant.core

import kotlin.math.max

data class SimpleSpell(
	val name: String, // key => equality
	val school: String,
	val castingTime: String,
	val ritual: Boolean,
	val components: Components,
	val reach: Int, // in feet
	val targets: String, // touch / self / one target / cubic area, circular, cone, etc...
	val duration: String,
	val concentration: Boolean,
	val description: String,
	val levels: Map<Int, Map<String, Any>>, // levels this spell can be cast with, and their optionally additional effects each level [<Effect Name, Effect Values>]
	val optAttackRoll: Boolean = false,
	val optSpellDC: Boolean = false, // needs spell DC to be used.
	val klasses: Set<Pair<String, String>> = setOf() // set of <klasses, sublasses> that can learn that spell.
	) {

	public companion object {
		public var Catalog: MutableSet<SimpleSpell> = mutableSetOf() // list of known spells.

		/** Get a SimpleSpell from the Catalog given by its name. */
		public fun getSpell(spellName: String) : SimpleSpell?
			= Catalog.find { spell -> spell.name == spellName }
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

	/** Representation:
	 * (level-school) Name / casting time (+ritual) / range / duration (+concentration)
	 */
	public override fun toString() = "(%d-%s) %s / %s / %s / %s".format(
		baseLevel, school,
		this.name,
		this.castingTime + if (ritual) " (ritual)" else "",
		"$reach ft ($targets)",
		this.duration + if (concentration) " (concentration)" else ""
		)

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



/** Data class wrapping a Spell with Hero specific meta data. */
data class LearnedSpell(val spell: SimpleSpell, val reasonKnown: String, var prepared: Boolean, var labels: Set<String>) {
	/** Equality by spell (as key). */
	override public fun equals(other: Any?) = other != null && when (other) {
		is LearnedSpell -> other.spell == this.spell // equal by underlying spell.
		is String -> other == this.spell.name // compare this learned spell with string.
		else -> false
	}
}


// XXX REMOVE ME: (Learnt(Spell + Source), minGoal + Caster(noFocus,counters,verbal,somatic) + metamagic?) => cast spell
/** Try to cast a spell.
 * If the spell can be cast, wrap it into @see CastSpell otherwise return a String (Either<CastSpell, String>).
 */
public fun checkSpellCastable(caster: Hero, learnedSpell: LearnedSpell, minLevel: Int = 0, metamagic: String? = null) : Either<CastSpell, String> {
	val spell = learnedSpell.spell
	val spellName = spell.name

	/* Spell unknown to caster. */
	if (!caster.hasSpellLearned(spell))
		return Either.Right("Spell ($spellName) is not learned by the caster.")

	/* Spell is not prepared for the caster. */
	if (!caster.hasSpellPrepared(spell))
		return Either.Right("Spell ($spellName) is not prepared by the caster.")

	/* Check if enough physical resoureces are available. */
	var (requiredItems, optionalItems)
		= spell.components.materialGP
		.partition { (_, valueGP) -> valueGP > 0 }

	/* Check if an arcane focus or component pouch can be used, otherwise other items are also needed.. */
	// if (caster.getArcaneFocus() == null) { }

	/* Try to lcast level in this minimally requested, minimally needed and minimally available. */
	// XXX
	var counters = when (learnedSpell.reasonKnown) {
		"Base Klass (etc)." -> caster.specialities.filter { it.name.startsWith("Spell Slot") }
		else -> listOf()
	}

	// Drop unavailable counters: If with counter, then still not zero.
	counters = counters.filterNot { it.count?.run { current < 1 } ?: false }

	/* Neither Spell Slots, item charges nor other counters have capacity to cast the spell left. */
	if (counters.size < 1)
		return Either.Right("Cannot Cast Spell ($spellName). No magic ressource / counter available.")

	val availableMinSpellLevel = counters
		.map { if (it.name.startsWith("Spell Slot")) { it.name.last().toInt() - 48 /*char num*/ } else { 0 } }
		.minOrNull() ?: 0

	var thenLevel = max(max(minLevel, spell.baseLevel), availableMinSpellLevel)

	/* Effects of the level the spell will then be cast with. */
	var thenEffects = spell.levels
		.toList()
		.dropLastWhile { (lvl, _) -> lvl > thenLevel } // drop too high
		.maxByOrNull { (lvl, _) -> lvl }?.second // effects of highest already fitting
		?: mapOf() // no effects.

	return Either.Left(CastSpell(
		learnedSpell = learnedSpell,

		// consumed to cast the spell then.
		counter = null,
		verbal = spell.components.verbal,
		somatic = spell.components.somatic,
		consumedItems = listOf(),

		// special effects the cast spell will have
		targetedLevel = thenLevel,
		concentration = thenEffects["concentration"] as? Boolean ?: spell.concentration,
		targets = thenEffects["targets"] as? String ?: spell.targets, // XXX by higher level or metamagic or other means.
		duration = thenEffects["duration"] as? String ?: spell.duration, // XXX by higher level or metamagic or other means.

		// final result message for successfully cast spell
		meta = "Spell ($spellName) can be successfully cast.",
	))
}


/** Simple Summary of a successfully cast spell:
 *  Its targeted level, the success and which counter and items would be used.
 */
public class CastSpell(
	val learnedSpell: LearnedSpell,

	// success and requirements and ressources to cast
	val counter: Speciality?,  // spell slot counters or special abilities, items, etc.
	val verbal: Boolean,  // typically defined by spell, but can be surpressed by meta magic
	val somatic: Boolean, // typically defined by spell, but can be surpressed by meta magic
	val consumedItems: List<String>,

	// resulting level, conncntration, etc.
	val targetedLevel: Int, // level intended to cast the spell with, or on success: the least possible with intended level (eg. intended lvl. 4, but next availble is only 6)
	val concentration: Boolean = learnedSpell.spell.concentration, // can be surpressed by higher level casts, etc.
	val targets: String, // can be influenced by higher level casts or meta magic
	val duration: String, // can be influenced by higher level casts or meta magic

	val meta: String,  // more information like success or fail message
	);
