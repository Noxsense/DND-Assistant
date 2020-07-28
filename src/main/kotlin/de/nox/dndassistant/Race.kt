package de.nox.dndassistant

import kotlin.math.floor

private val logger = LoggerFactory.getLogger("Race")

data class SubRace(
	val superRace: String,
	val name: String,
	val speed: Map<String, Int>,
	val abilityChanges: Map<Ability, Int> = mapOf(),
	val darkvision: Int = 0,
	val languages: List<String> = listOf()
) {
	var description: String = "${name} description"

	var racialTraits: Map<String, String> = if(darkvision > 0) {
			mapOf("Darkvision ${darkvision} ft" to "")
		} else {
			mapOf()
		}
		private set

	fun addTrait(title: String, description: String = "") {
		racialTraits += title to description
	}

	override fun equals(other: Any?) : Boolean = when {
		other == null -> false
		other !is SubRace -> false
		else -> (superRace == other.superRace) && (name == other.name)
	}

	override fun toString() : String = when {
		name == superRace || name == "-" -> superRace
		else -> "${superRace} ({$name})"
	}
}
