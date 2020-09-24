package de.nox.dndassistant.core

import kotlin.math.floor

private val logger = LoggerFactory.getLogger("Background")


enum class Alignment {
	LAWFUL_GOOD,
	LAWFUL_NEUTRAL,
	LAWFUL_EVIL,
	NEUTRAL_GOOD,
	NEUTRAL_NEUTRAL,
	NEUTRAL_EVIL,
	CHAOTIC_GOOD,
	CHAOTIC_NEUTRAL,
	CHAOTIC_EVIL;

	val abbreviation : String
		= name.split("_").toList().joinToString(
			"","","", transform = { it[0].toString() } )

	override fun toString() : String
		= name.toLowerCase().replace("_", " ")
}

data class Background(
	val name: String,
	val proficiencies : List<Skillable>, // skill, tool proficiencies
	val equipment : List<Item>,
	val money: Money) {

	override fun toString() : String = name

	var description: String = ""
	var extraLanguages: Int = 0 // addionally learnt languages.

	var suggestedSpeciality: List<String> = listOf()

	var suggestedTraits: List<String> = listOf()
	var suggestedIdeals: List<String> = listOf()
	var suggestedBonds: List<String> = listOf()
	var suggestedFlaws: List<String> = listOf()
}
