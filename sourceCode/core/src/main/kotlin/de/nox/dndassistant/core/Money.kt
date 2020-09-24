package de.nox.dndassistant.core

data class Money(
	val pp: Int = 0, /* platiunum. */
	val gp: Int = 0, /* gold. */
	val ep: Int = 0, /* electrum. */
	val sp: Int = 0, /* silver. */
	val cp: Int = 0, /* copper. */
	val ignoreElectrum : Boolean = true
) : Comparable<Money> {

	private val logger = LoggerFactory.getLogger("Money")

	/* Return the whole value as smallest currency,*/
	val asCopper: Int
		= cp + (SP_CP * (sp + (EP_SP * ep + (GP_SP * (gp + (PP_GP * pp))))))

	/** Weight of that money pile, where 50 coins have 50 lb.*/
	val weight: Double = (cp + sp + ep + gp + pp) / 50.0

	/* Constants.*/
	companion object {
		const val PP = 0; const val PP_GP = 10 /*10gp.*/
		const val GP = 1; const val GP_EP = 2; const val GP_SP = 10 /*2ep, 10sp.*/
		const val EP = 2; const val EP_SP = 5 /*5sp*/
		const val SP = 3; const val SP_CP = 10 /*10cp.*/
		const val CP = 4; const val CP_GP = 1
	}

	override fun compareTo(other: Money) = asCopper - other.asCopper

	override fun toString() : String
		= Regex("\\s*\\b0[pgesc]p\\s*")
			.replace("${pp}pp ${gp}gp ${ep}ep ${sp}sp ${cp}cp", "")
			.let { str -> if (str == "") "0cp" else str }

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
			logger.verbose("Cannot abstract the other amount, it is too big.")
			return this;
		}

		var negative = false

		var pP = pp - other.pp; negative = negative || pP < 0
		var gP = gp - other.gp; negative = negative || gP < 0
		var eP = ep - other.ep; negative = negative || eP < 0
		var sP = sp - other.sp; negative = negative || sP < 0
		var cP = cp - other.cp; negative = negative || cP < 0

		if (!negative) {
			logger.debug("Easily reduced.")
			return Money(pP, gP, eP, sP, cP, ignoreElectrum)
		}

		logger.debug("Needs some fixes, can be done!")

		// figure out, which values are negative, borrow up and down!
		// it must be possible, otherwise, it would have said no before.

		/* Borrow from silver.*/
		if (cP < 0) {
			// borrow from silver.
			sP += cP / SP_CP - 1 // at least one.
			cP += SP_CP
		}

		/* Borrow from higher (electrum, gold, platinum) or lower (copper). */
		while (sP < 0) {
			if (eP > 0 || gP > 0 || pp > 0) {
				// Borrow from the higher coin, if possible.
				if (ignoreElectrum) {
					gP -= 1
					sP += EP_SP
				} else  {
					eP -= 1
					sP += EP_SP
				}
			} else {
				// Borrow from the lower coin.
				cP -= SP_CP
				sP += 1
			}
		}

		/* Borrow from higher (gold, platinum) or lower (silver, copper). */
		while (eP < 0) {
			if (gP > 0 || pp > 0) {
				// Borrow from the higher coin, if possible.
				gP -= 1
				eP += GP_EP
			} else if (sP >= EP_SP) {
				// Borrow from the lower coin.
				sP -= EP_SP
				eP += 1
			} else {
				// Borrow from the lower coin.
				cP -= EP_SP * SP_CP
				eP += 1
			}
		}
		/* Borrow from higher (platinum) or lower (electrum, silver, copper). */
		while (gP < 0) {
			if (pP > 0) {
				// Borrow from the higher coin, if possible.
				pP -= 1
				gP += PP_GP
			} else if (eP >= GP_EP) {
				// Borrow from the lower coin.
				eP -= GP_EP
				gP += 1
			} else if (sP >= GP_EP * EP_SP) {
				// Borrow from the lower coin.
				sP -= GP_SP
				gP += 1
			} else {
				// Borrow from the lower coin.
				cP -= GP_SP * SP_CP
				gP += 1
			}
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
			from == GP && gp > PP_GP -> {
				pP += 1;
				gP -= PP_GP;
			}
			/* change copper to silver.*/
			from == CP && cp > SP_CP -> {
				sP += 1;
				cP -= SP_CP;
			}
			/* Ignore electrum: Skip silver to electrum, change silver to gold.*/
			ignoreElectrum && from == SP && sp > GP_SP -> {
				gP += 1;
				sP -= GP_SP
			}
			/* Normal Silver change.*/
			from == SP && sp > GP_EP -> {
				eP += 1;
				sP -= GP_EP
			}
			/* Ignore electrum, try to get rid of all electrums.*/
			ignoreElectrum && from == EP && ep > GP_EP -> {
				gP += ep / GP_EP;
				eP %= GP_EP
			}
			/* Normal electrum change.*/
			from == EP && ep > GP_EP -> {
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
			from == PP && pp > 0 -> {
				pP -= 1;
				gP += PP_GP;
			}
			/* change  silver to copper.*/
			from == SP && sp > 0 -> {
				sP -= 1;
				cP += SP_CP;
			}
			/* Ignore electrum: Skip gold to electrum, change gold to silver.*/
			ignoreElectrum && from == GP && gp > 0 -> {
				gP -= 1;
				sP += GP_SP
			}
			/* Normal Silver change.*/
			from == GP && gp > 0 -> {
				gP -= 1;
				eP += GP_EP
			}
			/* Ignore electrum, try to get rid of all electrums.*/
			ignoreElectrum && from == EP && ep > 0 -> {
				eP = 0;
				sP += ep * EP_SP
			}
			/* Normal electrum change.*/
			from == EP && ep > 0 -> {
				eP += 1;
				sP -= EP_SP
			}
		}

		return Money(pP, gP, eP, sP, cP) // apply changes.
	}
}
