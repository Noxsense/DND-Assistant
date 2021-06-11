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
		val UNARMED = Attack("Unarmed Strike", DamageType.BLUDGEONING, Number(1) + Reference("STR"), description = "A punch, kick, head-butt, or similar forceful blow (none of which count as weapons). You are proficient with your unarmed strikes.")
		val UNARMED_ATTACK_ROLL = "1d20 + STR + proficiencyBonus"
		// always proficient.
	}

	public constructor(source: Any, damageType: DamageType, damageValue: RollingTerm, reach: Int = 5, targets: String = "One Target", description: String = "")
		: this(source, listOf(Damage(damageType, damageValue)), reach, targets, description);

	/** Damage of an Attack. */
	// TODO (2021-04-12) replace INT with the variable TERM.
	public data class Damage(val type: DamageType, val value: RollingTerm) {
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
