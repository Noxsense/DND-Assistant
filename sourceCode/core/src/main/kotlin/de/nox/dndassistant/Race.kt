package de.nox.dndassistant

import kotlin.math.floor

private val logger = LoggerFactory.getLogger("Race")

data class Race(
	val name: String,
	val abilityScoreIncrease: Map<Ability, Int> = mapOf(),
	val age: Map<String, Int> = mapOf("adult" to 18),
	val alignment: String = "neutral",
	val size: Size = Size.MEDIUM,
	val sizeDescription: String = "ca. 180m high, ca. 75kg",
	val speed: Map<String, Int> = mapOf("walking" to 30),
	val languages: List<String> = listOf("Common"),
	val darkvision: Int = 0,
	val features: List<Feature> = listOf(),
	val subrace: Map<String, List<Feature>> = mapOf()
) {
	var description: String = "${name} description"

	fun allFeatures(forSubrace: String = "") : List<Feature>
		= features + subrace.getOrDefault(forSubrace, listOf())

	fun getLifeStage(ageInYears: Int) : String
		= age.filterValues { it <= ageInYears }.maxBy { it.value }?.key ?: "adult"

	override fun equals(other: Any?) : Boolean
		= other != null && other is Race && name == other.name

	override fun toString() : String
		= name

	/** A race feature: A title and it's description.*/
	class Feature(val name: String, val description: String = "") {
		val hasDescription: Boolean get()
			= description.trim().length > 0

		override fun equals(other: Any?)
			= other != null && other is Feature && name == other.name

		override fun toString() : String
			= name
	}

}
