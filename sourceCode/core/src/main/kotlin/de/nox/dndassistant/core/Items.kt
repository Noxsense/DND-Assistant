package de.nox.dndassistant.core

/* TODO (2020-07-07) https://roll20.net/compendium/dnd5e/Rules:Objects#content
 * - Armor Class of an Item
 * - Hit Points of an Item
 */
interface Item {
	val name: String
	val weight: Double
	val cost: Money
}

enum class WeightClass {
	NONE,
	LIGHT,
	HEAVY
}

enum class Size {
	TINY,
	SMALL,
	MEDIUM,
	LARGE,
	HUGE,
	GIANT; // GARGANTUM
}
