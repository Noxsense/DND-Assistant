package de.nox.dndassistant.core

// 2021-04-12 refactored, very new logic
/** An Attack is defined by its wrapped attsck source (like a weapon or spell),
 *  how the damage will be rolled.
 *  a reach (in feat), (default: 5ft meelee)
 *  how many targets will be hit, (default: One target)
 *  and a description (default: empty)
 */
public data class Attack(val source: Any, val damage: List<Damage>, val reach: Int = 5 /*ft*/, val targets: String = "One Target", val description: String = "") {
	public companion object {
		var defaultCatalog: Map<Any, Map<DamageType, Int>> = mapOf(
			"Item: Dagger" to mapOf(DamageType.PIERCING to 4 /*+max(STR/DEX)*/),
			"Item: Magic Missile" to mapOf(DamageType.FORCE to 4+1),
			"Unarmed Attack" to mapOf(DamageType.BLUDGEONING to 0 /*max(STR)*/),
		)

		/** Unarmed attack. */
		val UNARMED = Attack("Unarmed Strike", DamageType.BLUDGEONING, "1 + STR")
		val UNARMED_ATTACK_ROLL = "1d20 + STR + proficiencyBonus"
		// always proficient.
	}

	public constructor(source: Any, damageType: DamageType, damageValue: String, reach: Int = 5, targets: String = "One Target", description: String = "")
		: this(source, listOf(Damage(damageType, damageValue)), reach, targets, description);

	/** Damage of an Attack. */
	// TODO (2021-04-12) replace INT with the variable TERM.
	public data class Damage(val type: DamageType, val value: String) {
		public fun equalsType(other: Damage) = other.type == this.type
		public fun equalsValue(other: Damage) = other.value == this.value
		override public fun equals(other: Any?) = other != null && other is Damage && equalsType(other) && equalsValue(other)
	}

	/** String representation of the Attack given by the source. */
	public val label: String = source.toString()

	/** String representation of the summands of the (base) damage. */
	public val damageSumString: String
		= damage.joinToString(" + ") { "${it.value} (${it.type.toString().toLowerCase()})" }

	/** String representation of an attack.
	 * e.g. Dagger (1d4 + STR/DEX)
	 * e.g. Spiritual Weapon (1d8 + STR/DEX/CON/CHA/WIS/INT/...)
	 */
	override public fun toString()
		= "$label ($damageSumString)"

	/** Pretty String representation of an attack and it's atttack roll.
	 * e.g. Bite. Meelee Weapon Attack: +4 to hit, reach 5ft, one target, Hit: 2d4 + 2 piercing damage, knocked prone (DC 11 STR).
	 *
	 * @param evaluateWith if not null, replace the variable names in attack roll and attack damage with respective values.
	 * @return String for Pair.
	 */
	public fun toAttackString(attackRoll: String, showDescription: Boolean = true, evaluateWith: Map<String, Int>? = null)
		= "%s. %d ft: %s, %s. Hit: %s.".format(
				label,
				reach,
				when {
					// attack roll is not given (hits always, or DC)
					attackRoll.startsWith("DC") -> attackRoll

					// attack roll is just d20 + variables: just write d20
					evaluateWith != null -> attackRoll

					else -> attackRoll
				},
				targets,

				// damage: e.g. 2d4 + STR (piercing damage)
				damage.joinToString(" + ") { (type, value) -> "$value (${type.toString().toLowerCase()} damage)" }
				+ if (showDescription) description else "",
			)
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
