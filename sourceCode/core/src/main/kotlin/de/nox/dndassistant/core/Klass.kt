package de.nox.dndassistant.core

import kotlin.math.floor

// TODO (2020-07-31) connect spellcasting with klass.

/** A Class (derived from Klass) that can cast spells. */
public class SpellcasterKlass(
	name: String,
	hitdie: SimpleDice,
	savingThrows: List<Ability>,
	klassLevelTable: Set<Feature>,
	specialisations: Map<String, Set<Feature>>,
	description: String,

	val spellcastingAbility: Ability,
	val formulaSpellDC: String,
	val formulaSpellAttack: String,
	val spellRitual: Boolean = false,
	val spellPreparationPerLevel: List<Pair<Int, Int>> = listOf(-1 to -1), // known at all, -1 as unlimited
	val spellSwap: Int = 1, // per long rest
	val castingLevel: Power = Power.FULL_CASTER,
) : Klass(
	name, hitdie, savingThrows, klassLevelTable, specialisations, description) {

	/** Return the number of cantrips and spells, which can be learnt at the given level. */
	fun spellsKnownAt(lvl: Int) : Pair<Int, Int>
		= when {
			lvl < 0 -> (0 to 0)
			lvl >= spellPreparationPerLevel.size -> spellsKnownAt(spellPreparationPerLevel.size - 1)
			else -> spellPreparationPerLevel[lvl]
		}

	enum class Power { FULL_CASTER, HALF_CASTER, THIRD_CASTER, };
}

/** A class of the characters. */
open public class Klass(
	val name: String,
	val hitdie: SimpleDice = SimpleDice(0),
	val savingThrows: List<Ability> = listOf(),
	val klassLevelTable: Set<Feature> = setOf(),
	val specialisations: Map<String, Set<Feature>> = mapOf(),
	val description: String = "A ${name} is like that."
) {

	/** Get all the received traits for a certain level and maybe specialisation.
	 * If the level is out of range, return all traits of levels and all classes.
	 * @param level the level, traits should be collected for.
	 * @param specialisation additional attribute to maybe add filtered traits by specialisation.
	 * @return map of trait title and brief description.
	 */
	fun getFeaturesAtLevel(level: Int, specialisation: String = "") : List<Feature> {
		/* Return all possible traits. */
		if (level < 1 || level > 20) {
			return listOf()
		}

		return (specialisationFeaturs(specialisation) + klassLevelTable)
			.filter { feature -> feature.level <= level }
			.sorted()
	}

	fun specialisationFeaturs(name: String) : Set<Feature>
		= specialisations.getOrDefault(name, setOf())

	override fun equals(other: Any?) : Boolean
		= other != null && other is Klass && (name == other.name)

	override fun toString() : String
		= "${name}"

	/** A klass feature is an ability or thing with description, the klass member will earn.  */
	data class Feature(val level: Int, val title: String, val description: String = "")
	: Comparable<Feature> {
		val hasDescription : Boolean get()
			= description.trim() != ""

		override fun compareTo(other: Feature) : Int
			= when {
				level != other.level -> level.compareTo(other.level)
				else -> title.compareTo(other.title)
			}

		override fun equals(other: Any?) : Boolean
			= (other != null && other is Feature
				&& (level == other.level && title == other.title))

		override fun toString() : String
			= "$title (at level ${level})"
	}
}
