package de.nox.dndassistant.core

/** Attack.
 * Create an attack a Weapon or a Spell can make depending on the strength or
 * dexterity and maybe proficiency value.
 * @url (https://roll20.net/compendium/dnd5e/Combat#h-Making%20an%20Attack)
 */
data class Attack(
	val name: String,
	val ranged: Boolean,
	val damage: Pair<DiceTerm, Set<DamageType>>,
	val finesse: Boolean = false,
	val proficientValue: Int = 0,
	val modifierStrDex: Pair<Int, Int> = Pair(0, 0),
	val note: String = ""
): Comparable<Attack> {

	/** Decide which bonus is added. */
	val bonus: Int = when {
		finesse -> modifierStrDex.run { Math.max(first, second) }
		ranged -> modifierStrDex.second // dex
		else -> modifierStrDex.first // str
	}

	/** Attack Bonus: (STR or DEX) + (proficiency if proficient). */
	val attackBonus: Int
		= bonus +  (proficientValue)

	val damageRollPure : DiceTerm
		= damage.first

	val damageType : Set<DamageType>
		= damage.second

	/** Damage Modifier: Ability modifier, used for attack roll. */
	val damageRoll: DiceTerm
		= (damageRollPure + Bonus(bonus)).contracted().let {
			if (it.dice.size == 1
				&& (it.dice[0].faces < 2 && it.dice[0].factor < 0)) {
				// logger.warn("Never Negative Damage")
				DiceTerm(0)
			} else {
				it
			}
		}

	/** The damage roll for a critical hit are twice the base attack damage dice. */
	val damageRollCritical: DiceTerm
		= (damageRoll + damageRollPure).contracted()

	/** Compare an attack by it's average damage */
	override fun compareTo(other: Attack) : Int
		= (damageRoll.average - other.damageRoll.average).toInt()
}

/** DamageType.
 * Different attacks, damaging Spells, and other harmful Effects deal different
 * types of damage. Damage Types have no rules of their own, but other rules, such
 * as damage Resistance, rely on the types.
 * @url (https://roll20.net/compendium/dnd5e/Combat#h-Damage%20Types)
 */
enum class DamageType(val note: String) {

	ACID(
		"The corrosive spray of a black dragon’s breath and " +
		"the dissolving enzymes secreted by a Black Pudding deal acid damage."),

	BLUDGEONING(
		"Blunt force attacks—hammers, Falling, constriction, " +
		"and the like—deal bludgeoning damage."),

	COLD(
		"The Infernal chill radiating from an Ice Devil’s spear and " +
		"the frigid blast of a white dragon’s breath deal cold damage."),

	FIRE(
		"Red Dragons breathe fire, and " +
		"many Spells conjure flames to deal fire damage."),

	FORCE(
		"Force is pure magical energy focused into a damaging form. " +
		"Most Effects that deal force damage are Spells, " +
		"including Magic Missile and Spiritual Weapon."),

	LIGHTNING(
		"A Lightning Bolt spell and " +
		"a blue dragon’s breath deal lightning damage."),

	NECROTIC(
		"Necrotic damage, " +
		"dealt by certain Undead and a spell such as Chill Touch, " +
		"withers matter and even the soul."),

	PIERCING(
		"Puncturing and impaling attacks, including spears and monsters’ bites, " +
		"deal piercing damage."),

	POISON(
		"Venomous stings and " +
		"the toxic gas of a green dragon’s breath deal poison damage."),

	PSYCHIC(
		"Mental Abilities " +
		"such as a mind flayer’s psionic blast deal psychic damage."),

	RADIANT(
		"Radiant damage, dealt by a cleric’s Flame Strike spell or " +
		"an angel’s smiting weapon, " +
		"sears the flesh like fire and overloads the spirit with power."),

	SLASHING(
		"Swords, axes, and monsters’ claws deal slashing damage."),

	THUNDER(
		"A concussive burst of sound, " +
		"such as the effect of the Thunderwave spell, " +
		"deals thunder damage.")
}

/** Page 290 */
enum class Condition(val note: String) {

	BLINDED(
		"Auto-fail sight-dependant checks, " +
		"disadvantage to your attacks, " +
		"hostile has advantage"),

	CHARMED(
		"Cannot hurt / attack charmer, " +
		"charmer has advantage to social ability checks"),

	DEAFENED(
		"Auto-fail hearing-dependant checks"),

	FRIGHTENED(
		"Disadvantage to checks/attacks " +
		"while source of fear is in line of sight. " +
		"Can’t move closer to source of fear"),

	GRAPPLED(
		"Speed 0, no bonus. " +
		"Ends when grappler incapacitated or " +
		"when moved out of reach of grappler from an effect"),

	INCAPACITATED(
		"No actions / reactions"),

	INVISIBLE(
		"Hiding = Heavily Obscured, " +
		"still makes noise and tracks. " +
		"You attack with advantage, " +
		"hostile has disadvantage"),

	PARALYZED(
		"Incapacitated. " +
		"Auto-fail DEX & STR saves. " +
		"Hostile has advantage. " +
		"All damage from within 5 ft. critical"),

	PETRIFIED(
		"Your weight increases x10, " +
		"incapacitated, " +
		"unaware of surroundings. " +
		"Hostile has advantage. " +
		"Auto-fail DEX and STR saves, " +
		"resist all damage / poison / disease"),

	POISONED(
		"Attacks & ability checks have disadvantage"),

	PRONE(
		"Can only crawl (1/2 speed), unless stands. " +
		"Standing costs half of movement speed for round. " +
		"You attack with disadvantage. " +
		"Hostile has advantage within 5 ft.; " +
		"over 5 ft., has disadvantage"),

	RESTRAINED(
		"Speed 0, no bonus. " +
		"Your attacks & DEX saves have disadvantage. " +
		"Hostile has advantage"),

	STUNNED(
		"Incapacitated. " +
		"Hostile has advantage. " +
		"Auto-fail DEX / STR saves"),

	UNCONSCIOUS(
		"Incapacitated & prone. " +
		"Auto-fail DEX & STR saves. " +
		"Hostile has advantage. " +
		"All damage from within 5 ft. critical")
}
