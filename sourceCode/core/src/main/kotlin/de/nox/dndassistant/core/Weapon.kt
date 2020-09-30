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
	val range: Range = Range(5), // range in feet. => disadvantage out of range.

	/* The damage, which is dealt, on hit. */
	val damageType: Set<DamageType>,

	val damage: DiceTerm, // damage on hit and type.

	/* A thrown melee weapon without thrown property deals 1D4
	 * (like ranged weapon on melee attack, out of range). */
	val throwable : Boolean = false,
	val thrownRange: Range = Range(20,60),
	val thrown: DiceTerm = DiceTerm(D4),

	/* Can also be used two-handed => has a second dice term. */
	val versatile: DiceTerm? = null,

	/* needs two hand to wield, no off-hand possible. */
	val isTwoHanded: Boolean = false,

	/* Ranged attack with AMMUNITION, only if available, expended on use.
	 * Draw from quiver needs free other-hand.
	 * Recover ammunition up to half (on battlefield). */
	val ammunition: Array<String> = arrayOf(),

	/* can use dexterity modifier => use highest modifier (on default),
	 * otherwise, use STR for melee and DEX for ranged. */
	val isFinesse: Boolean = false,

	// TODO (2020-07-08) silvered

	val note: String  // notes
) : Item, Skillable {

	private val LOG_TAG = "D&D Weapon"

	override fun toString() : String = name

	/* A range contains two numbers: Normal and long.
	 * Beyond normal range: Disadvantage.
	 * Beyond long range: Out-of-range.
	 * (Long can also be normal.) */
	data class Range(val normal_: Int, val long_: Int = 0) : Comparable<Range> {

		val normal = normal_
		val long = if (long_ < normal) normal else long_

		/* True, if x is in range of the range aka. not out-of-range.*/
		operator fun contains(x: Int) : Boolean
			= x <= long

		fun hasDisadventage(x: Int) : Boolean
			= normal != long // no disadvantage, on any range.
			|| normal < x // disadvantage, beyond normal range.

		/* Compare by length in-between. */
		override fun compareTo(other: Range) : Int = normal - other.normal
	}

	/* Basic: Weapon Types: Simple or Martial, Melee or Ranged.
	 * Also the "other" option is available.*/
	enum class Type(val simple: Boolean, val melee: Boolean) : Skillable {
		SIMPLE_MELEE(true, true),
		SIMPLE_RANGED(true, false),
		MARTIAL_MELEE(false, true),
		MARTIAL_RANGED(false, false),
		OTHER(false, false)
	}

	/* TWO HANDED ATTACK.
	 * Versatile Weapon as two-handed: Use versatile Damage Dice.
	 * One-Handed Weapon as two-handed (wo/ versatility): No change.
	 */
	val damageUsingTwoHanded : DiceTerm
		= versatile ?: damage
		// = if (versatile != null) versatile?? else damage
	// can be used two handed.

	/* One HANDED ATTACK.
	 * Two-Handed weapon use as one-handed: see suggestions.
	 * [last update: 2020-06-13](https://rpg.stackexchange.com/a/128940)
	 * - Less control (Disadvantage).
	 * - reduced power, like versatile weapons.
	 * - perquisites to wield the weapon at all (like STR > 15)
	 * - lose/alter proficiency
	 */
	val damageUsingOneHanded : DiceTerm = when {
		isTwoHanded -> damage // with disadvantage or lesser dice term.
		else -> damage // can have disadvantage.
	}

	private val improvised = DiceTerm(D4)

	/* RANGED ATTACK
	 * Additionally (like a two-handed ranged weapon: any bow)
	 * Ranged weapon as melee : "Improvised" 1D4 ...
	 * Melee Weapon thrown (20/60): "Improvised" 1d4
	 * Ranged weapon in long-range: Disadvantage
	 */
	val damageUsingMelee : DiceTerm = when {
		weaponType != Type.OTHER && !weaponType.melee -> improvised
		else -> damage
	}

	val damageUsingRanged : DiceTerm = when {
		weaponType != Type.OTHER && weaponType.melee -> improvised
		else -> damage // can have disadvantage.
	}
}
