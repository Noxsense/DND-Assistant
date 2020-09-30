package de.nox.dndassistant.core

/** Something a player can wear, like shoes, accessoires or also armor. */
interface Wearable : Item {
	override val name: String
	override val weight: Double
	override val cost: Money
	val position: BodyType
}

/** A basic class for wearable stuff, capes and necklaces included. */
data class Clothes(
	/* Inherit from item.*/
	override val name: String, // inherit from Item
	override val weight: Double, // inherit from item
	override val cost: Money, // inherit from item

	override val position: BodyType = BodyType.BODY,
	val description : String = ""
) : Wearable {

	override fun toString(): String
		= name

}
