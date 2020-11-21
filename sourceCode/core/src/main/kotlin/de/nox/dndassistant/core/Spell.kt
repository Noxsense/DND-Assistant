package de.nox.dndassistant.core

data class Spell(
	val name: String,
	val school: School,
	val casting: Casting,
	val effects: List<Effect>,
	val description: String,
	var casterKlasses: List<SpellcasterKlass>
) : Comparable<Spell> {

	companion object {
		val INDICATOR_CONCENTRATION: String = 9400.toChar().toString() // circled C (C)
		val INDICATOR_RITUAL: String = 9415.toChar().toString() // squared R (R)
	}

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

	/* Information bundle about casting and co.
	 * The Default Casting is [1 action, (VS). */
	public class Casting(
		val duration: String = "1 action", // reaction / bonus / 1 minutes

		val ritual: Boolean = false,

		val verbal: Boolean = true,
		val somatic: Boolean = true,
		val materials: Map<String, Int> = mapOf<String, Int>() // item to min value.

		// override fun toString(): String = duration
	);

	/** Information bundle about one effect of the spell.
	 * The default Effect is [LVL?, Touch/0ft, <>Instantaneous, No Atk, no Saving Throw]. */
	public class Effect(
		/** The level this spell is cast with.
		 * On higher level, more powerful effects cold happen. */
		val level: Int,

		/** Area the spell effects.
		 * This area is described by distance and one/multiple target, or even a space.
		 * Such targets can be: willing or unwilling targets, touched or attacked,
		 * just yourself and maybe an area around you,
		 * or a whole area inside a line, sphere, cube, cylinder or cone
		 * (with radius or reach). */
		val area: String = "Touch",
		val distance: Int = 0, // ft

		// TODO (2020-11-12) what is about multiple targets?
		// [ ] 3 darts (targets) + 1 each higher slot level
		// [ ] higher slots: copy with higher value or add (cumulative) ?

		/** The duration time of the spell effect.
		 * By default: The duration is instantaneous (maximally 1 second or less). */
		val duration: String = "Instantaneous",

		/** If the spell needs concentration, to hold on, this is indicated here.
		 * By default: No concentration is needed (false). */
		val concentration: Boolean = false,

		/** Describe the successful spell result (caster has full success). */
		val onSuccess: String = "",

		/** The spell may be targeting and may need an attack roll to hit.
		 * By default: No targeting and attacking is needed (false). */
		val needsAttack: Boolean = false,

		/** Optional rolls, for like damage, duration, or heal, etc.
		 * By default: No extra roll needed (null).
		 * What can embody the outcome of a Spell Effect, what can a spell?
		 * - can lead do a condition. (Blinded|charmed|...|Unconscious) | being controlled | Alive again?
		 * - can lead to damage, maybe multiple types (like bludgeoning and ice damage on ice storm)
		 * - can lead to healing (like heal)
		 * - can lead to creating something (create a wall, create a cage) | summoning something up
		 * - can influence the area or targets => higher armor class, molded around, make some wings and fly around
		 */
		val optionalRolls: DiceTerm? = null,
		// ALT: val resulsWithNote: Map<String, Any> = mapOf("ATTACK" to Damage(DamageType.FIRE to DiceTerm(6)), "Knock out" to Condition.PRONE),

		/** The target can make optional saving throws to avoid the spell's full power.
		 * By default: The spell effect cannot be avoided (null). */
		val savingThrow: Ability? = null,

		/** Describe what happens, if the target could be successfully saved.
		 * By default: There is no special mention for a saved target. */
		val forSaved: String? = null,

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
			val R = when { c.ritual -> INDICATOR_RITUAL; else -> "" }

			/* Show invocation components. */
			val vsm = showInvocationComponents()

			/* Final String. */
			"${R}${c.duration} ($vsm)"
		}

	/** Get the effects, which match the effect.
	 * If no effect has the certain level, get the next lesser level. */
	private fun getEffect(lvl:Int = level) : Effect
		= effects.filter { it.level == lvl }.let { ofLevel ->
			if (ofLevel.size < 1) getEffect(level) else ofLevel[0]
		}

	/** Get duration for spell cast on a certain level. */
	fun getEffectDuration(lvl: Int = level) : Pair<String, Boolean>
		= getEffect(lvl).let { e -> e.duration to e.concentration }

	/** Show duration for spell cast on a certain level. */
	fun showEffectDuration(lvl: Int = level) : String
		= getEffectDuration(lvl).let { (dur, conc) ->
			"${when (conc) { true -> INDICATOR_CONCENTRATION; else -> ""}} ${dur}"
		}

	/** Show area of the spell effect, when cast with a certain level. */
	fun showEffectArea(lvl: Int = level) : Pair<String, Int>
		= getEffect(lvl).let { e -> e.area to e.distance }

	/** Show all effects as Map of Level to String. */
	fun showEffects() : List<Pair<Int, String>> = effects.map { e ->
		val duration = "${if (e.concentration) "${INDICATOR_CONCENTRATION} " else ""}${e.duration}"
		val distance = "${e.area} (${e.distance}ft)"

		val save = when (e.savingThrow) {
			null -> ""
			else -> " (Saving Throw: ${e.savingThrow})"
		}

		val term = when (e.optionalRolls) {
			null -> ""
			else -> " (${e.optionalRolls})"
		}

		e.level to "[$duration | $distance] -- ${e.onSuccess}${save}${term}"
	}
}
