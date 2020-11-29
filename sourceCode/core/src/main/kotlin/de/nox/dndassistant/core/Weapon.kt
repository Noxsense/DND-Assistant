package de.nox.dndassistant.core

/** Weapon <- Skillable.
 * "Your class grants proficiency in certain Weapons, reflecting both the
 * class’s focus and the tools you are most likely to use. Whether you favour a
 * Longsword or a Longbow, your weapon and your ability to wield it effectively
 * can mean the difference between life and death while Adventuring.
 *
 * The Weapons table shows the most Common Weapons used in the fantasy gaming
 * worlds, their price and weight, the damage they deal when they hit, and any
 * special properties they possess. Every weapon is classified as either melee or
 * ranged. A melee weapon is used to Attack a target within 5 feet of you,
 * whereas a ranged weapon is used to Attack a target at a distance."
 */
data class Weapon(
	/* Inherit from item.*/
	override val name: String, // inherit from Item
	override val weight: Double, // inherit from item
	override val cost: Money, // inherit from item

	/* LIGHT weapons can be dual-wielded.
	 * SMALL creatures have disadvantage with HEAVY weapons. */
	val weightClass: WeightClass = WeightClass.NONE,

	/* Weapon specific attributes.*/

	val weaponType: Type,
	val range: IntRange = 0..5, // range in feet. => disadvantage out of range.

	/* The damage, which is dealt, on hit. */
	val damage: List<Damage>, // damage on hit and type.

	/* A thrown melee weapon without thrown property deals 1D4
	 * (like ranged weapon on melee attack, out of range). */
	val thrown: Pair<IntRange, List<Damage>>? = null,

	/* Can also be used two-handed => has a second dice term. */
	val versatile: List<Damage>? = null,

	/* needs two hand to wield, no off-hand possible. */
	val isTwoHanded: Boolean = false,

	/* Ranged attack with AMMUNITION, only if available, expended on use.
	 * Draw from quiver needs free other-hand.
	 * Recover ammunition up to half (on battlefield). */
	val ammunition: Array<String> = arrayOf(),

	/* can use dexterity modifier => use highest modifier (on default),
	 * otherwise, use STR for melee and DEX for ranged. */
	val isFinesse: Boolean = false,

	// TODO (2020-07-08) silvered and other / harder materials?

	val note: String  // notes
) : Item, Skillable {

	private val LOG_TAG = "D&D Weapon"

	override fun toString() : String = name

	/** Check, if a given distance is still in weapons range. */
	fun inRange(distance: Int, isThrown: Boolean = false) : Boolean
		= distance in range

	/* Basic: Weapon Types: Simple or Martial, Melee or Ranged.
	 * Also the "other" option is available.*/
	enum class Type(val simple: Boolean, val melee: Boolean) : Skillable {
		SIMPLE_MELEE(true, true),
		SIMPLE_RANGED(true, false),
		MARTIAL_MELEE(false, true),
		MARTIAL_RANGED(false, false),
		OTHER(false, false)
	}
}
