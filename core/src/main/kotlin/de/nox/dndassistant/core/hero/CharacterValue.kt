package de.nox.dndassistant.core.hero

import de.nox.dndassistant.core.terms.RollingTerm
import de.nox.dndassistant.core.terms.TermVaribales
import de.nox.dndassistant.core.hero.CharacterAttribute

/**
 * A Character Value is part of the Character Stats.
 *
 * A character value can be an attribute or a skill or anything.
 *
 * A character value can generate a new reference.
 *
 * For example,
 * <pre>{@code
 * CharacterValue = {
 *  "name": "ATTRIBUTE_STRENGTH",
 *  "displayName": "Strength",
 *  "category": "ATTRIBUTE",
 *  "notes": "Attribute Strength"
 *  "dependsOn": null,
 *  "displayValue": "20",
 *  "proficient": false,
 *  "value": "Number(20)",
 *  "rollWith": "D20",
 *  "baseValue": "Number(20)",
 * }
 *
 * CharacterValue = {
 *  "name": "ATTRIBUTE_MODIFIER_DEXTERITY",
 *  "displayName": "DEX",
 *  "category": "ATTRIBUTE_MODIFIER",
 *  "notes": "Attribute Modifier"
 *  "dependsOn": "Attribute('DEXTERITY'),
 *  "displayValue": "+3",
 *  "proficient": false,
 *  "value": "(Reference(ATTRIBUTE_DEXTERITY) - 10) / 2",
 *  "rollWith": null,
 *  "baseValue": "(Reference(ATTRIBUTE_DEXTERITY) - 10) / 2",
 * }
 *
 * CharacterValue = {
 *  "name": "SKILL_STEALTH",
 *  "displayName": "Stealth",
 *  "category": "SKILL",
 *  "notes": "Skill Stealth"
 *  "dependsOn": null,
 *  "displayValue": "+5",
 *  "proficient": true,
 *  "value": "D20 + ATTRIBUTE_MODIFIER_DEXTERITY + PROFICIENCY",
 *  "rollWith": "D20",
 *  "baseValue": "Reference('ATTRIBUTE_MODIFIER_DEXTERITY')",
 * }
 *
 * CharacterValue = {
 *  "name": "ATTACK_DAGGER",
 *  "displayName": "Dagger",
 *  "category": "WEAPON_ATTACK",
 *  "notes": "Dagger Attack, favourite bla. (Finesse, throwable)"
 *  "dependsOn": Attribute(INVENTORY_DAGGER),
 *  "displayValue": "+5",
 *  "proficient": true,
 *  "value": "D20 + ATTRIBUTE_MODIFIER_DEXTERITY + PROFICIENCY",
 *  "rollWith": "D20",
 *  "baseValue": "Reference('ATTRIBUTE_MODIFIER_DEXTERITY')",
 * }
 *
 * CharacterValue = {
 *  "name": "ATTACK_DAMAGE_DAGGER",
 *  "displayName": "Dagger (Damage)",
 *  "category": "WEAPON_ATTACK_DAMAGE",
 *  "notes": "Two hand fighting."
 *  "dependsOn": Attribute(INVENTORY_DAGGER),
 *  "displayValue": "+5",
 *  "proficient": true,
 *  "value": "Die(4) + Reference('ATTRIBUTE_MODIFIER_DEXTERITY') + Number('PROFICIENCY')",
 *  "baseValue": "Reference('ATTRIBUTE_MODIFIER_DEXTERITY')",
*   "rollWith": "D4",
 * }
 * }</pre>
 */
public data class CharacterValue(
	override val name: String,
	override val category: String,

	override var notes: String,

	override var dependsOn: CharacterAttribute?,

	/** Display Value, such as "+2" instead of 10. */
	var displayValue: String,

    /** An optionally set field if the value can get proficiency rolls.
     * This may be overwritten by an automatic CharacterSheet check,
     * if no such proficiency can be found. */
	var profcient: Boolean,

	/** The optionally modified term such as STEALTH := DEX_MODIFIER + (2 * PROFICIENCY). */
	var value: RollingTerm,

    /** Additional Rolling Term to add.
	 * This term is also part of the end {@see value}.
	 * If the {@see value} is automatically generated,
	 * this value should also be used (with the proficiency and baseValue) . */
	var rollWith: RollingTerm?,

	/** The original term such as STEALTH := DEX_MODIFIER.
     * Just without any proficiency.
	 * This is part of {@see value}.
	 * If this value is automatically generated,
	 * the baseValue used with the {@see rollWith} and proficiency. */
	var baseValue: RollingTerm,

) : CharacterAttribute {
	override var displayName: String = this.name
}
