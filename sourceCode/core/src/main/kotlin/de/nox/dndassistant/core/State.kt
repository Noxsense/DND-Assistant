package de.nox.dndassistant.core

import kotlin.math.min

/** A state the attributes of the character can have.
 * This is a PlayerCharacter depended class and a state my not exists without
 * it's player character.
 */
data class State(val pc: PlayerCharacter) {
	var hitpoints: Int = pc.hitpoints // current hitpoints
		private set

	// TODO (2020-10-02) remove HP-tmp on spell end

	// Last until the buffer is destroyed, long rest or with given duration.
	var hitpointsTMP: Int = 0 // extra buffer for the hit points
		private set

	/** Current maximal hitpoints: Maximal hitpoints plus maybe temporary hitpoints. */
	val hitpointsMax: Int
		get() = pc.hitpoints + hitpointsTMP

	var hitdice: List<DiceTerm.Die> = pc.hitdice.toMutableList() // to new list.
		private set

	var initiativeRolled: Int = 0 // latest rolled initiative, 0 if none
		private set

	// TODO (2020-10-02) armor class with spells?
	var armorClass: Int = pc.armorClass // current armor class by possible spells, etc.
		private set

	var stabilized: Boolean = true
		private set

	var deathsaveFail: Int = 0
		private set

	var deathsaveSuccess: Int = 0
		private set

	/** The feet which may be left on a turn. */
	var feetLeft: Int = 0
		private set

	/** Left Spell slots. */
	public var spellSlots: IntArray
		= pc.spellSlots.toMutableList().toIntArray() // copied to a new list ?
		private set

	public val spellComperator: Comparator<Spell> = Comparator<Spell> { a, b ->
		/* Check, if both spells are prepared. */
		val aPrep = a in this.spellsPrepared
		val bPrep = b in this.spellsPrepared

		/* Check if the spell is cast or not. */
		val aCast = this.spellsCast.containsKey(a)
		val bCast = this.spellsCast.containsKey(b)

		when {
			a == b -> {
				0 /* spells are the same. */
			}
			aPrep != bPrep -> {
				/* One spell is prepared, the other is not. */
				-aPrep.compareTo(bPrep) // descending: true smaller false
			}
			!aPrep || (aPrep && !aCast && !bCast) -> {
				/* Not different and one is not prepared
				 * => Both spells are not prepared/equipped.
				 * OR:
				 * NOt different and one is prepared
				 * => Both spells are prepared, both are not cast. */
				a.compareTo(b) // compares by level and name.
			}
			aCast != bCast -> {
				/* Both spells are prepared/equipped. (Else caught before.)
				 * Only one is cast. */
				-aCast.compareTo(bCast) // descending: true smaller false
			}
			else -> {
				/* Both spells are prepared, both are cast.
				 * Get each rest time; sort concentration to head. */
				val spellContr = this.spellConcentration?.first

				val aRestTime = if (a == spellContr) 0 else this.spellsCast[a]!!.second
				val bRestTime = if (b == spellContr) 0 else this.spellsCast[b]!!.second

				aRestTime.compareTo(bRestTime)
			}
		}
	}

	private fun slotIndex(slot: Int) : Int
		= Math.min(Math.max(slot - 1, 0), 8)

	/** Getter for a available, left spell slot. */
	fun spellSlot(slot: Int) : Int
		= when {
			slot == 0 -> 1 // cantrip can be always cast.
			else -> spellSlots[slotIndex(slot)]
		}

	/** Get the highest available spell slot. */
	fun highestSpellSlot() : Int
		= spellSlots.dropLastWhile { it < 1 }.size // count spell slots that are still available.

	/** Try to use a spell slot and return reduction was successful. */
	fun spellSlotUse(slot: Int) : Boolean
		= when {
			/* No spell slot available. */
			spellSlot(slot) < 1 -> false

			/* No spell slot needed. */
			slot < 1 -> true . also {
				pc.log.debug("Cantrip hasn't used up a spell slot.")
			}

			/* Reduce spell slot. */
			else -> true.also{
				// the actual reduction.
				spellSlots[slotIndex(slot)] -= 1
				pc.log.debug("Redcued spell slots of $slot to ${spellSlot(slot)}")
			}
		}

	/** Prepared spells (or equipped/learnt).
	 * For prepares, they can change each long rest.
	 * For non-prepares, it depends on the rules,  but they may be able to change
	 * one per long rest or on each level up. */
	var spellsPrepared: Set<Spell>
		= setOf()
		private set

	/** Spells cast by the PlayerCharacter,
	  * mapped with spell slot used and countdown in seconds. */
	var spellsCast: Map<Spell, Pair<Int,Int>>
		= mapOf()
		private set

	/** Cast spell that needs concentration, mapped with cast spell. */
	var spellConcentration: Pair<Spell, Int>? = null
		private set

	/** Spells influencing this Character, mapped with countdown in seconds. */
	var spellbounded: Map<Spell, Int>
		= mapOf()
		private set

	/** Conditions influencing this character, mapped with countdown in seconds.
	 * An negative number will be handled as infinitive. */
	var conditions: Map<Condition, Int>
		= mapOf()
		private set

	/** String representation to of the current state. */
	override public fun toString()
		= ("Connected to ${pc}"
		+ ": HP: %d/%d (%+d)".format(hitpoints, pc.hitpoints, hitpointsTMP)
		+ ", Condition: [%s]".format(conditions.toList().joinToString(", "))
		+ ", Spells: [%s] ".format(spellSlots.joinToString(","))
		+ (spellConcentration?.first?.name ?: "")
		)

	/** Add damage to the character.
	 * If the hit causes damage, which causes more than the negative max hp,
	 * the character dies immediately.
	 * If a character with HP 0 is hit, it causes an immediate death saving fail.
	 * Otherwise just reduce the HP and maybe fall unconscious, if 0 hp is reached.
	 *
	 * @param hp the final hitpoints, which will be reduced from the current hp.
	 */
	fun takeHit(hp: Int, critical: Boolean = false) {
		/* buffer the damage taken. */
		pc.log.info("Take hit of $hp damage, current HP: $hitpoints (${"%+d".format(hitpointsTMP)}).")

		val hpRest = hp - hitpointsTMP
		hitpointsTMP -= hp

		if (hitpointsTMP < 0) {
			// hitpoints += hitpointsTMP // removed buffered rest damage.
			hitpointsTMP = 0 // reset to null.
			pc.log.info(
				"Buffer/Temporary HP used up, Remove from normal HP."
				+ "Rest damage left: $hpRest.")
		} else if (hitpointsTMP > 0) {
			pc.log.info(
				"Buffer/Temporary HP reduced."
				+ "Rest damage left: $hpRest.")
		}

		if (hitpoints - hpRest <= -pc.hitpoints) {
			/* Immediate death. */
			pc.log.info(
				"The taken hit was to big," +
				"the character dies immidiatly.")

			deathsaveSuccess = 0
			deathsaveFail = 3
			stabilized = false

		} else if (hpRest > 0 && hitpoints == 0) {
			/* Immediate death saving fail. */
			pc.log.info(
				"The unconscious character takes damage, " +
				"a death save fail is added.")

			deathsaveFail += if (critical) 2 else 1
			stabilized = false

		} else {
			pc.log.info("The character just takes damage")
			hitpoints = Math.max(hitpoints - hpRest, 0)

			/* Check if now unconscious. */
			if (hitpoints < 1) {
				/* drop unconscious, and not stabilised. */
				stabilized = false
				deathsaveSuccess = 0
				deathsaveFail = 0
			}
		}

		/* Finally, if below 0, reset to zero */
		hitpoints = Math.max(hitpoints, 0)
		hitpointsTMP = Math.max(hitpointsTMP, 0)

		pc.log.info("Hit taken, rest HP: $hitpoints (${"%+d".format(hitpointsTMP)})")
	}

	/** Add another death save result (fail, true)
	 * @param success if true, a success is added otherwise a fail.
	 * @param critical if true, two successes or fails will be added.
	 * @return sum (Int) of the final successes or fails (accordingly to the input)
	 */
	fun deathSaved(success: Boolean, critical: Boolean = false): Int
		= when (success) {
			true -> deathsaveSuccess.run {
				plus(if (critical) 2 else 1)
				this
			}
			else -> deathsaveFail.run {
				plus(if (critical) 2 else 1)
				this
			}
		}

	/** End the death saving fight and be stabilized, but not healed. */
	fun stabilize() {
		stabilized = true
		deathsaveFail = 0
		deathsaveSuccess = 0
		pc.log.info("Stabilized")
	}

	/** Heal the character, this also stabilizes the character.
	 * @param hp the amount to heal; if negative, only stabilisation is applied.
	 */
	fun heal(hp: Int) {
		/* Apply stabilisation, reset the death fight. */
		stabilize()

		/* heal if heal is bigger than 0. */
		if (hp > 0) {
			hitpoints = Math.min(hitpoints + hp, pc.hitpoints)
			pc.log.info("Healed (+$hp HP) => $hitpoints HP")
		}
	}

	/** Make a short rest, ca. 1h.
	 * @param spentDice (optionally) spent hitdie to heal. */
	fun restShort(spentDice: List<DiceTerm.Die> = listOf(), heal: Int) {
		if (hitpoints < 1) {
			pc.log.info("Character cannot take effect of a rest, they are not yet healed.")
			return
		}

		/* Remove the spent dice from available hit die. */
		spentDice.forEach { d -> hitdice = hitdice.minusElement(d) }

		pc.log.info("Short Rest, heal $heal HP, spent dice: $spentDice => left dice $hitdice")

		tick(1800)

		/* Apply the heal. */
		heal(heal)
	}

	/** Make a long rest, ca. 8h with max. one 2h-guard-shift. */
	fun restLong() {
		// character must have at least 1hp
		if (hitpoints < 1) {
			pc.log.info("Long Rest started, but health ($hitpoints) too low, to gain profit from it.")
			return
		}

		pc.log.info("Long Rest, refilled health, restored half of the hitdice")

		/* HP to max HP. */
		heal(pc.hitpoints)

		/* Free temporary hitpoints. */
		hitpointsTMP = 0
		pc.log.info("Temporary hitpoints resetted.")

		/* Restore half of the spent hitdice, rounded down. */
		var missing: List<DiceTerm.Die> = pc.hitdice.toMutableList()
		val max: Int = missing.size
		hitdice.forEach { missing = missing.minusElement(it) }

		var toRestore = missing.take(Math.max(1, max / 2))

		hitdice += toRestore

		pc.log.info("Added hit dice: +${toRestore.size} => $hitdice.")

		/* Restore all spell slots. */
		for (i in spellSlots.indices) spellSlots[i] = pc.spellSlots[i]
		tick(28800) // spent 8 hours of time, at least.

		pc.log.info("Restored spell slots: ${spellSlots.joinToString()}.")

		/* Reset number of prepared spells during this long rest. */
		preparedSpellsCount = 0
	}

	/** Rest depending on the hours: shorter than 1 => no rest at all, at least 8h => long rest. */
	public fun restHours(hour: Int = 1) {
		when {
			hour < 1 -> pc.log.info("Could not get rest for $hour hour.")
			hour < 8 -> restShort(heal = 0) // short rest (not using any hit die).
			hour < 12 -> restLong() // normal long rest.
			else -> { hitpoints = 1; restLong() } // long rest with restoration of the missing hp, if needed.
		}
	}

	/** Walk {ft} feet.
	 * Return true, if the steps are consumed. */
	fun walk(ft: Int = 5): Boolean
		= (feetLeft > ft).apply { if (this) {
			feetLeft -= ft
			pc.log.info("Walked $ft ft.")
		}}

	/** Reset feetLeft to the feet available on normal speed.
	 * @return the new feetleft. */
	fun resetSpeed(): Int
		= (pc.speedMap.get("normal") ?: 0).also {
			feetLeft = it // apply normal feet speed to left.
		}

	// TODO (2020-10-03) learn a new spell? => not a condition, to PC!

	// TODO (2020-10-03) prepare a learnt spell set.
	// TODO (2020-10-03) cast a spell, maybe spend a spell slot, set to active.
	// TODO (2020-10-03) other spell sources like Charisma modifier or once per longrest/shortrest.

	/** Cast a spell.
	 * If the spell is cast from a scroll or item, it don't need to be learnt
	 * and also don't uses a spell slot.
	 * @param spell the spell which is then active.
	 * @param unlearned a spell which is not learnt, but cast with help of a magic item.
	 * @param higherSpellSlots extra spent spell slot to cast this spell (default: 0, positive number starting at 0)
	 * @return true, if the spell is cast, otherwise false.
	 */
	fun castSpell(spell: Spell, unlearned: Boolean = false, higherSpellSlots: Int = 0) : Boolean
		= (spell.minlevel + higherSpellSlots).let { lvl -> when {
			/* Not learnt, but cast with an item (like scroll or item. */
			unlearned -> {
				spellsCast += spell to (-1 to -1) // TODO (2020-10-30) duration of item magic.
				pc.log.debug("Spell '$spell' is not learned, but cast from forgein knowledge.")
				true
			}
			/* Not enough spell slots left, to cast this spell. */
			spellSlot(lvl) < 1 -> {
				pc.log.info("Not enough spell slots left to cast $spell")
				false
			}
			/* Spell can be cast. */
			else -> {
				val (duration, concentration) = spell.getEffectDuration(lvl)

				/* New spell is spell with concentration. */
				if (concentration) {
					/* If new spell needs concentration, replace other
					 * concentration holding spell. */
					if (spellConcentration != null) {
						spellsCast = spellsCast - spellConcentration!!.first

						pc.log.info("Lost Concentration for other spells, in order to cast a new concentration spell!")
					}
					spellConcentration = spell to lvl
					pc.log.debug("Casting '$spell': New Concentration.")
				}

				// TODO (2020-10-30) focuS | resource used on spell cast.

				var seconds = 1

				// XXX (2020-11-12) duration => spell.durationSeconds

				/* Check spell source. */
				val (_, usesSpellSlots) = pc.spellsLearntWith.getOrElse(spell) { Ability.WIS to true }

				pc.log.debug("Casting '$spell': uses spell slots (learnt): $usesSpellSlots.")

				if (usesSpellSlots) {
					pc.log.info("Cast '$spell', spell slot ${lvl} for ${seconds} secs (${duration})")
					spellSlotUse(lvl) // use spell slot
					spellsCast += spell to (lvl to Math.max(1, seconds)) // add with duration (at least 1).
					pc.log.debug("Cast spells: $spellsCast")
				}
				true
			}
		}}

	/** Stop, dispell or cancel a cast spell. */
	fun cancelSpell(spell: Spell) {
		spellsCast -= spell

		/* If cancelled spell holds concentration, then cancel concetration. */
		if (spellConcentration != null && spell == spellConcentration!!.first) {
			spellConcentration = null
		}

		pc.log.info("Dispelled '${spell}' => spells cast: $spellsCast")
	}

	/* Number of prepared spells since last longrest. */
	public var preparedSpellsCount: Int = 0
		private set

	/** Try to prepare a spell after a long rest.
	 * A preparation can fail, if the spell is unknown, or prepartion limits were reached.
	 * @return true, if the preparation was successful. */
	fun prepareSpell(spell: Spell) : Boolean {
		return when {
			spell in spellsPrepared -> true.also {
				pc.log.info("Spell '$spell' is already prepared, so .. the spell is prepared.")
			}
			/* Spell is unknown. */
			spell !in pc.spellsLearntWith.keys -> false.also {
				pc.log.info("Spell '$spell' cannot be prepared, it is unknown.")
			}
			/* Number of preparable spells per long rest ist met. */
			false -> false.also {
				pc.log.info("Spell '$spell' cannot be prepared, it is unknown.")
			}
			/* Spell cannot be prepared due to level limitations. */
			// TODO (2020-10-30) implement.
			spell.minlevel > 9 -> false. also {
				pc.log.info("Spell '$spell' cannot be prepared due to level limitations.")
			}
			/* PREPARE THE SPELL. */
			else -> true. also {
				pc.log.info("Spell '$spell' is prepared.")
				preparedSpellsCount += 1
				spellsPrepared += spell
			}
		}
	}

	/** Unprepare a spell (after a long rest). */
	fun unprepareSpell(spell: Spell) {
		spellsPrepared -= spell
	}

	/** Toggle the spell preparation.
	 * @return true, if spell is now prepared, otherwise false. */
	fun toggleSpellPreparation(spell: Spell): Boolean
		= when (spell in spellsPrepared) {
			true -> false. also {
				spellsPrepared -= spell
				pc.log.info("Toggle Spell prepation: Unprepare. ($spell)")
			}
			else -> prepareSpell(spell).also {
				pc.log.info("Toggle Spell prepation: Try to prepare. ($spell)")
			}
		}

	/** Decrease left duration of all activated spells.
	 * Remove spells, with left duration below 1 seconds.
	 * @param sec seconds to reduce from active spells. */
	fun tick(sec: Int = 6) {
		pc.log.info("Tick all status effects for $sec seconds.")
		// reduce condition rest time

		/* reduce spells own rest time */
		val (running, expired) = spellsCast // Map<Spell, Pair<Int,Int>>
			.toList()
			.map { (spell, cast) ->
				val (slot, seconds) = cast
				spell to (slot to (seconds - sec)) // reduce left duration.
			}.partition { it.second.second > 0 } // split to ongoing | done

		/** If spell with concentration is expired, remove it from focus.
		  * Also check,, if the spells that holds concentration is still active. */
		val noConcentration: Boolean = (spellConcentration != null
			&& ((spellConcentration?.first in expired.map { (s,_) -> s }) // expired
			|| (spellConcentration?.first !in running.map { (s,_) -> s }))) // not active

		if (noConcentration) {
			spellConcentration = null
		}

		pc.log.info( "Tick: Expired Spells: ${expired}")
		pc.log.debug("Tick: Running Spells: ${running}")
		pc.log.debug("Tick: Focused Spell:  ${spellConcentration}")

		/* Take spells with removed left, but still active time. */
		spellsCast = running.toMap()
	}
}
