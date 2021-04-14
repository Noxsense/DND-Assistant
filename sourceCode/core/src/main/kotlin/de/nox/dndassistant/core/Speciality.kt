package de.nox.dndassistant.core

// optional feat [+ (optinal) Flavour]
// Actor; Performance, Charisma, etc.
// Spell sniper: No Coverage but full, doubled ranged attack range
// Ritualer: Ability to have rituals like bard or cleric or etc. (chosen)
abstract class Speciality(val name: String, val count: Count?, val description: String) {
	//
	// count: maximal uses, maybe rechargable, loaded items
	/**
	 * (Optional) Count of a Feature or Trait.
	 * It can have a counted maximal value or is just a counter if the max is 0.
	 */
	class Count(val recharge: String, val max: Int = 0, var current: Int = max, var loaded: List<Any>? = null)  {
		override fun toString() : String
			= ("$current"
			+ (if (max > 0) "/$max" else "")
			+ (if (loaded != null) " $loaded" else "")
			+ (if (recharge.length > 0) " (rechaging: $recharge)" else ""))
	}

	// class LoadedLimit(recharge: String, var items: List<Any>) : Count(recharge, max = items.size(), 0)

	override fun toString() : String
		// = name + if (count != null) " { counted: $count }" else ""
		 = "${name} ${count?.let {
				// Pretty
				// circle: \u26aa (white) \u26ab (black)
				// black dots: still available, white dots: already used.
				(0 until it.max).joinToString("") { x -> if (x < it.current) "\u26ab" else "\u26aa"}
			}}"

	public override fun equals(other: Any?) : Boolean
		= (other != null && other is Speciality && other.name == this.name && other.description == this.description)
		.also { "called Speciality($this).equals($other)"}

	/** Reset counter:Set Current to max. */
	public fun resetCounter() = count?.apply { current = max }

	/** Update the counter: Decrease its current count. */
	public fun countDown(steps: Int = 1) = countUp(-steps)

	/** Update the counter: Increase its current count. */
	public fun countUp(steps: Int = 1) = count?.apply {
		// update
		current += steps

		// don't cross bounds 0 .. max
		if (current < 0) current = 0
		if (max > 0 && current > max) current = max
	}

	public fun getCountCurrent() = count?.current ?: 0
}

// TODO (2021-03-11) Speciality of an Item (like +3 or additional spells) vs Buff of an Item (one time for period +3 or so)

// Effect like Buff, Debuf or Condition.
// handle count as countdown, which will be used up by time and not by the user. if no countdoen is given, the time alsomdon't uses up the countdown.
// Prone is an Effect("Prone", 0, "Lies on the floor, disadventage on attacks and being attacked by ranged attacks").
// Potion of Haste is an Effect("Haste", 60, "Haste stuff").
// Shield (Spell) is an Effect("Shield", 6, "+2 AC and blocked attack"). // until turn
// Dying?
/** Effect on the Hero, maybe by being pushed or bewitched or other reasons.
 * @param seconds, if given, the time will eat up the effect, otherwise the player or enemy must undo it.
 * @param removable if the hero themselves can remove the effect or if other methods (like remove curse or so) must be done.'*/
class Effect(name: String, seconds: Int = 0, val removable: Boolean = true, description: String) : Speciality(name, Speciality.Count(recharge = "", max = seconds, current = seconds, loaded = null), description) {
	public override fun equals(other: Any?)
		= other != null && other is Effect && other.name == this.name && other.description == this.description && other.removable == this.removable
}

// TODO should it link to it's klass or the klass to the trait or double linked?
/** KlassTrait given by a klass and subklass on a certain klass level.
 * A Klass Trait may give special abilities, improvements or resources to the Hero.*/
class KlassTrait(name: String, val klass: Triple<String, String, Int>, count: Count? = null, description: String = "") : Speciality(name, count, description) {
	public override fun equals(other: Any?)
		= other != null && other is KlassTrait && other.name == this.name && other.description == this.description
}

// TODO should it link to it's klass or the klass to the trait or double linked?
/** RaceFeature, given by the race and certain Hero Levels.
 * A race feature may give special abilities, improvements or resources. */
class RaceFeature(name: String, val race: Pair<String, String>, val level: Int = 1, count: Count? = null, description: String = "") : Speciality(name, count, "") {
	public override fun equals(other: Any?)
		= other != null && other is RaceFeature && other.name == this.name && other.description == this.description
}

// TODO should it link to it's klass or the klass to the trait or double linked?
/** A Feat which can be optionally gained by certain circumstances.
 * It may give certain sepcial abilities, improvements or resources. */
class Feat(name: String, count: Count? = null, description: String = "") : Speciality(name, count, description) {
	public override fun equals(other: Any?)
		= other != null && other is Feat && other.name == this.name && other.description == this.description
}

// TODO should it link to it's klass or the klass to the trait or double linked?
// eg. Mage Ring with 5 loaded spells or combinations of summing up a level 5 spell
// eg. each atack with it grants +3 attack bonus and +3 attack damage.
/** An ItemFeature can be hold as lomg as the corresponding Item is held and probably atuned by the Hero.
 * It may give certain improvements, abilities and resources. */
class ItemFeature(name: String, count: Count? = null, description: String = "") : Speciality(name, count, description) {
	public override fun equals(other: Any?)
		= other != null && other is ItemFeature && other.name == this.name && other.description == this.description
}

// custom counter like: defeated dragons or days in this campaign
/** A CustomCount is a counter or countdown made by the Hero's Player to get track of personal and other ideas.
 * For example encountered Dragons or days trained with a unproficient weapon. */
class CustomCount(name: String, count: Count? = null, description: String = "") : Speciality(name, count, description) {
	public override fun equals(other: Any?)
		= other != null && other is CustomCount && other.name == this.name && other.description == this.description
}


