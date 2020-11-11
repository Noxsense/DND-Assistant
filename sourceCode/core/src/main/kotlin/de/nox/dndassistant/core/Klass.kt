package de.nox.dndassistant.core

import kotlin.math.floor

// TODO (2020-07-31) connect spellcasting with klass.

/** A class of the characters. */
public class Klass(
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
