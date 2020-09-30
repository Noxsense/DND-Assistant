package de.nox.dndassistant.core

import kotlin.math.floor

private val logger = LoggerFactory.getLogger("Background")


/** An Alignment is a direction of good or evil, and lawful or chaotic
 * the character can take.
 */
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

/** A Background of a character provides two extra skills and
 * in sum two tool proficiencies or languages.

 * The background also defines the roleplay components a lot.
 */
data class Background(
	val name: String,
	val proficiencies : List<Skillable>, // skill, tool proficiencies
	val equipment : List<Item>,
	val money: Money) {

	override fun toString() : String = name

	var description: String = ""
	var extraLanguages: Int = 0 // additionally learnt languages.

	var suggestedSpeciality: List<String> = listOf()

	var suggestedTraits: List<String> = listOf()
	var suggestedIdeals: List<String> = listOf()
	var suggestedBonds: List<String> = listOf()
	var suggestedFlaws: List<String> = listOf()
}
