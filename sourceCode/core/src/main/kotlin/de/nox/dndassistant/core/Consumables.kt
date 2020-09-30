package de.nox.dndassistant.core

/** Consumables.
 * Food to eat. Potions, to swallow or other body contact. Scrolls to read.
 */
data class Consumables(
	override val name: String,
	override val weight: Double,
	override val cost: Money
) : Item {

	override fun toString() : String = name
}
