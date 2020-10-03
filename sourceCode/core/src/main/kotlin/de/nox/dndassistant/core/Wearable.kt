package de.nox.dndassistant.core

enum class BodyType {
	HEAD, // hat, helmet...
	SHOULDERS, // like cloaks ...
	NECK, // like necklace ...
	BODY, // like main outfit, onsies or dresses ...
	HAND, // like for one glove (2x)
	RING, // for fingers (10x... species related)
	FOOT, // for one shoe or so (2x)
	ARMOR, // worn above clothes.
	SHIELD; // only one; can strapped on one arm, maximally one weapon can be wield, and other non-combat items can be hold.
}

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
