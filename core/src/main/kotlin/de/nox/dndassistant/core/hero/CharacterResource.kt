package de.nox.dndassistant.core.hero

import de.nox.dndassistant.core.hero.CharacterAttribute

/**
 * A Character Resource is part of the Character Stats or Inventory.
 *
 * Any counter which can be used up.
 */
public data class CharacterResource(
	override val name: String,
	override val category: String,

	override var notes: String,

	override var dependsOn: CharacterAttribute?,

	/** The maximal amount for the Resource.
	 * Can also be Int.MAX_VALUE to indicate an infinity. */
	var max: Int,
	//
): CharacterAttribute {
	override var displayName: String = this.name

	/** Current amount availaboe of the resource. */
	var value: Int = 0
		set(v) {
			// do not set below 0.
			this.value = if (v < 0) { 0 } else { v }
		}

	/** Set the current value to the given max value. */
	public fun refill() {
		this.value = this.max
	}

	/** Set the current value to 0. */
	public fun setEmpty() {
		this.value = 0
	}

	/** Increase current value. */
	public fun increase(n: Int = 1) : Int {
		this.value += n
		return this.value
	}

	/** Decrease the current value. */
	public fun decrease(n: Int = 1) : Int {
		this.value -= n
		return this.value
	}
}
