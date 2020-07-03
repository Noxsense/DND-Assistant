package de.nox.dndassistant;

interface Item {
	val name: String
	val weight: Float
	val cost: Money
}

data class Money(
	val pp: Int = 0, /* platiunum. */
	val gp: Int = 0, /* gold. */
	val ep: Int = 0, /* electrum. */
	val sp: Int = 0, /* silver. */
	val cp: Int = 0, /* copper. */
	val ignoreElectrum : Boolean = true
) : Comparable<Money> {

	/* Return the whole value as smallest currency,*/
	val asCopper: Int
		= cp + (SP_CP * (sp + (EP_SP * ep + (GP_EP * gp * (PP_GP * pp)))))

	/* Constants.*/
	companion object {
		const val PLATINUM = 0; const val PP_GP = 10 /*10gp.*/
		const val GOLD     = 1; const val GP_EP = 2; const val GP_SP = 10 /*2ep, 10sp.*/
		const val ELECTRUM = 2; const val EP_SP = 5 /*5sp*/
		const val SILVER   = 3; const val SP_CP = 10 /*10cp.*/
		const val COPPER   = 4; const val CP_GP = 1
	}

	override fun compareTo(other: Money) = asCopper - other.asCopper

	override fun toString() : String = "${pp}pp ${gp}gp ${ep}ep ${sp}sp ${cp}cp"

	/* Add two Money piles to each other. */
	operator fun plus(other: Money) : Money
		= Money(
			pp + other.pp,
			gp + other.gp,
			ep + other.ep,
			sp + other.sp,
			cp + other.cp,
			ignoreElectrum)

	/* Remove a given Money piles from this.
	 * If the other amount cannot be abstracted, return "this" again.
	 */
	operator fun minus(other: Money) : Money {

		/* Abort: Too small to be reduced by "other".*/
		if (this < other) {
			println("Cannot abstract the other amount, it is too big.")
			return this;
		}

		var cP : Int; var sP : Int; var eP : Int; var gP : Int; var pP : Int
		var borrow : Int = 0

		/* TODO (2020-07-01) refactor!
		 * - Questions: purse: 0gp 500sp, bow costs: 25gp = 250sp => now:
		 * cannot be payed. :< but I can be paid.
		 *
		 * BUT: we want to keep the single coins, if possible. We don't not
		 * always normalize the purse, by just buying something little.
		 */

		cP = cp - other.cp

		if (cP < 0) {
			borrow = cP / SP_CP - 1
			cP = SP_CP + (cP % SP_CP)
		}

		sP = sp - other.sp + borrow

		if (!ignoreElectrum) {
			if (sP < 0) {
				borrow = sP / EP_SP - 1
				sP = EP_SP + (sP % EP_SP)
			} else {
				borrow = 0
			}

			eP = ep - other.ep + borrow

			if (eP < 0) {
				borrow = eP / GP_EP - 1
				eP = GP_EP + (eP % GP_EP)
			} else {
				borrow = 0
			}
		} else {
			/* Borrow silver from gold.*/
			if (sP < 0) {
				borrow = sP / GP_SP - 1
				sP = GP_SP + (sP % GP_SP)
			} else {
				borrow = 0
			}

			eP = ep - other.ep

			// if too less add to borrowed.
			if (ep < 0) {
				borrow += ep / GP_EP - 1
				ep
			}

			// What if there is EP to remove, but it should be ignored?
		}

		gP = gp - other.gp + borrow

		if (gP < 0) {
			borrow = gP / PP_GP - 1
			gP = EP_SP + (gP % EP_SP)
		} else {
			borrow = 0
		}

		pP = pp - other.pp + borrow

		if (pP < 0) {
			println("Substraction to expensive. Abort.")
			return this
		}

		return Money(pP, gP, eP, sP, cP, ignoreElectrum)
	}

	/** Change one {needed} piece set to a higher value.
	 * @param from the lower piece type to the next higher.
	 * If from is Platinum, nothing will change,
	 * otherwise, if 100pc can be reduced, one new higher coin appear,
	 * and 100p are removed.
	 * @return a new Money pile with changed values.
	 **/
	fun changeUp(from: Int) : Money {
		var pP = pp; var gP = gp; var eP = ep; var sP = sp; var cP = cp

		when {
			/* change gold to platinum.*/
			from == GOLD && gp > PP_GP -> {
				pP += 1;
				gP -= PP_GP;
			}
			/* change copper to silver.*/
			from == COPPER && cp > SP_CP -> {
				sP += 1;
				cP -= SP_CP;
			}
			/* Ignore electrum: Skip silver to electrum, change silver to gold.*/
			ignoreElectrum && from == SILVER && sp > GP_SP -> {
				gP += 1;
				sP -= GP_SP
			}
			/* Normal Silver change.*/
			from == SILVER && sp > GP_EP -> {
				eP += 1;
				sP -= GP_EP
			}
			/* Ignore electrum, try to get rid of all electrums.*/
			ignoreElectrum && from == ELECTRUM && ep > GP_EP -> {
				gP += ep / GP_EP;
				eP %= GP_EP
			}
			/* Normal electrum change.*/
			from == ELECTRUM && ep > GP_EP -> {
				gP += 1;
				eP -= GP_EP
			}
		}

		return Money(pP, gP, eP, sP, cP) // apply changes.
	}

	/** Change a coin of the money pile to value coins of a lesser type
	.* All but copper.*/
	fun changeDown(from: Int) : Money {
		var pP = pp; var gP = gp; var eP = ep; var sP = sp; var cP = cp

		when {
			/* change  platinum to gold.*/
			from == PLATINUM && pp > 0 -> {
				pP -= 1;
				gP += PP_GP;
			}
			/* change  silver to copper.*/
			from == SILVER && sp > 0 -> {
				sP -= 1;
				cP += SP_CP;
			}
			/* Ignore electrum: Skip gold to electrum, change gold to silver.*/
			ignoreElectrum && from == GOLD && gp > 0 -> {
				gP -= 1;
				sP += GP_SP
			}
			/* Normal Silver change.*/
			from == GOLD && gp > 0 -> {
				gP -= 1;
				eP += GP_EP
			}
			/* Ignore electrum, try to get rid of all electrums.*/
			ignoreElectrum && from == ELECTRUM && ep > 0 -> {
				eP = 0;
				sP += ep * EP_SP
			}
			/* Normal electrum change.*/
			from == ELECTRUM && ep > 0 -> {
				eP += 1;
				sP -= EP_SP
			}
		}

		return Money(pP, gP, eP, sP, cP) // apply changes.
	}
}

/** Weapon.
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
	override val weight: Float, // inherit from item
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
) : Item, AbstractSkill {

	final val WEIGHT_CLASS_NONE = 0
	final val WEIGHT_CLASS_LIGHT = 1
	final val WEIGHT_CLASS_HEAVY = 2

	// load wepon.

	override fun toString() : String = name

}

/** ToolSkill.
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
	override val weight: Float,
	override val cost: Money,

	val toolType: Int

) : Item, AbstractSkill {

	final val TYPE_ARTISIAN_TOOL      = 0
	final val TYPE_GAMING_SET         = 1
	final val TYPE_MUSICAL_INSTRUMENT = 2
	final val TYPE_VEHICLE            = 3
}
