package de.nox.dndassistant.core

/** Tool <- Skillable.
 * "A tool helps you to do something you couldn't otherwise do, such as craft
 * or repair an item, forge a document, or pick a lock. Your race, class,
 * Background, or feats give you proficiency with certain tools. Proficiency with
 * a tool allows you to add your Proficiency Bonus to any ability check you make
 * using that tool. Tool use is not tied to a single ability, since proficiency
 * with a tool represents broader knowledge of its use. For example, the GM might
 * ask you to make a Dexterity check to carve a fine detail with your woodcarver's
 * tools, or a Strength check to make something out of particularly hard wood."
 */
data class Tool(
	override val name: String,
	override val weight: Double,
	override val cost: Money,

	val type: Type

) : Item, Skillable {

	enum class Type {
		ARTISIAN_TOOL,
		GAMING_SET,
		MUSICAL_INSTRUMENT,
		VEHICLE
	}

	override fun toString() : String = name
}
