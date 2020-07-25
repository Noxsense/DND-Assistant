package de.nox.dndassistant

private val logger = LoggerFactory.getLogger("Spell")

data class Spell(
	val name: String,
	val school: String,
	val level: Int,
	val castingTime: String,
	val range: String,
	val components: String,
	val duration: Int, // seconds.
	val concentration: Boolean = false,
	val note: String = "",
	val ritual: Boolean = false
	)
	: Comparable<Spell> {

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

/* Wrapper for a spell, which is learned with a certain class.
 * This also holds the current states, the spell caster initiated.
 */
data class LearntSpell (
	val spell: Spell,
	val learnedAs: String // Class name: eg. learned as Bard or Druid => important for ability score.
	) : Comparable<LearntSpell> {

	var preparedSlot: Int = -1
		private set

	/** Check, if the spell is readied for a spell slot (or a cantrip). */
	val prepared: Boolean get()
		= spell.level == 0 || preparedSlot >= spell.level

	var usedSpellSlot: Int = 0
		private set

	var holdsConcentration: Boolean = false
		private set

	var leftDuration: Int = 0 /* as seconds */
		private set

	/** Return the DC, to resist the spell.
	 * @return 8 + (Spellcasting Ability Modifier) + (Proficiency Bonus) + (Any Special Modifier). */
	val savingThrow: Int get()
		= 0 // TODO (2020-07-24)

	/** Return the possible dealt damage of the spell.
	 * @return (spellcasting ability modifier) + (proficiency bonus). */
	val attackDamage: Int get()
		= 0 // TODO (2020-07-24)

	/** Prepare the wrapped spell, to cast. */
	fun prepare(spellSlot: Int = 0) {
		preparedSlot = when {
			spellSlot == 0 -> spell.level // default: prepare on matching level.
			spellSlot < spell.level -> -1 // unprepare.
			else -> spellSlot
		}
	}

	/** Cast a prepared spell with the default or chosen slot level.
	 * If the spell is not prepared, nothing will change.
	 * The spell will not prepared anymore.
	 * Concentration, may be triggered, duration will start.
	 * @param level the slot level, the spell should be activated with, used at least the spell level.
	 * @return the used spell level, if not casted at all, 0 is returned.
	 */
	fun cast(level: Int = -1) : Int {
		// TODO (2020-07-25) needs to be prepared, needs not to be prepared?
		val mustBePrepared = false
		if (mustBePrepared && (spell.level != 0 || preparedSlot < 0)) {
			return -1 // abort, not casting at all.
		}

		leftDuration = spell.duration
		holdsConcentration = spell.concentration
		usedSpellSlot = Math.min(9, Math.max(spell.level, level))
		return usedSpellSlot
	}

	/** This decrease the left duration.
	 * If the left duration ran out and concentration was held, release concentration.
	 */
	fun spentTime(seconds: Int = 6) {
		leftDuration -= seconds

		if (leftDuration < 1) {
			endSpell()
			holdsConcentration = false
		}
	}

	/** This resets the left duration and maybe held concentration.*/
	fun endSpell() {
		leftDuration = 0
		holdsConcentration = false
	}

	/** Compare two learnt spells.
	 * If they are activated, put spells with concentration first.
	 * Then by left duration (short to long).
	 * If not activated, put prepared first.
	 * If also no spell is prepared, sort by spell level.
	 */
	override fun compareTo(other: LearntSpell) : Int
		= when {
			holdsConcentration != other.holdsConcentration ->
				-holdsConcentration.compareTo(other.holdsConcentration)
			leftDuration != other.leftDuration ->
				-leftDuration + other.leftDuration
			prepared != other.prepared ->
				-prepared.compareTo(other.prepared)
			prepared ->
				-preparedSlot + other.preparedSlot
			else ->
				spell.compareTo(other.spell)
		}

	override fun toString() : String {
		val preparedState = when {
			spell.level < 1 -> "{C} "
			prepared -> "{${preparedSlot}} "
			else -> ""
		}

		/* Show prepared slot and spell information. */
		val left = "${preparedState}${spell}"

		/* Not casted => show only left side. */
		if (leftDuration < 1)
			return left

		val activationState = when {
			holdsConcentration -> "Concentration for %6ds!"
			else -> "active for %6ds."
		}.format(leftDuration)

		return "%s %${70 - left.length}s".format(left, activationState)
	}
}

/** A school of magic, which categories a certain spell and may apply own rules
 * and believings.
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
