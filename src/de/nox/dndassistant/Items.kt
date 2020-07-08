package de.nox.dndassistant

interface Item {
	val name: String
	val weight: Double
	val cost: Money
}

///////////////////////////////////////////////////////////////////////////////

/** Weapon <- Skillable.
 * "Your class grants proficiency in certain Weapons, reflecting both the
 * class’s focus and the tools you are most likely to use. Whether you favor a
 * Longsword or a Longbow, your weapon and your ability to wield it effectively
 * can mean the difference between life and death while Adventuring.
 *
 * The Weapons table shows the most Common Weapons used in the fantasy gaming
 * worlds, their price and weight, the damage they deal when they hit, and any
 * special properties they possess. Every weapon is classified as either melee or
 * ranged. A melee weapon is used to Attack a target within 5 feet of you,
 * whereas a ranged weapon is used to Attack a target at a distance."
 *
 */
data class Weapon(
	/* Inherit from item.*/
	override val name: String, // inherit from Item
	override val weight: Double, // inherit from item
	override val cost: Money, // inherit from item

	/* Weapon specific attributs.*/
	val isMartial: Boolean, // false: Simple, true: Martial
	val isRanged: Boolean, // false: Melee, true: Ranged
	val range: Iterable<Int> = (1..5), // range in feet. => no disadventage within range.
	val dmgDice: DiceTerm, // damage on hit.
	val dmgType: String, // one or more damge types.
	val note: String, // notes

	val weightClass: Int = 0, // weightClass: none | light | heavy
	val isFinesse: Boolean = false, // can use dextrity modififier => use highest modifier (on default)
	val isVersatile: Boolean = false, // can also be used two-handed => creates a "new" weapon.
	val isTwoHanded: Boolean = false, // needs two hand to wield, no off-hand possible.
	val isThrowable: Boolean = false  // can be thrown => creates a "new" weapon.
) : Item, Skillable {

	final val WEIGHT_CLASS_NONE = 0
	final val WEIGHT_CLASS_LIGHT = 1
	final val WEIGHT_CLASS_HEAVY = 2

	// load wepon.

	override fun toString() : String = name
}

///////////////////////////////////////////////////////////////////////////////

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

	val toolType: Int

) : Item, Skillable {

	final val TYPE_ARTISIAN_TOOL      = 0
	final val TYPE_GAMING_SET         = 1
	final val TYPE_MUSICAL_INSTRUMENT = 2
	final val TYPE_VEHICLE            = 3
	override fun toString() : String = name
}
