package de.nox.dndassistant.core.hero

public interface CharacterAttribute {
    val name: String
    val category: String

	/** Display name such as "Strength" for "ATTRIBUTE_STRENGTH".
     * If the displayName is blank, this Value is hidden. */
    var displayName: String

	/** Notes about the attribute which can be modified with optional notes. */
    var notes: String

    /** A character attribute can be directly depend on another character attribute.
     *
     * For example, an DAGGER_ATTACK depends on carried Daggers (INVENTORY_DAGGER),
     * SKILL_STEALTH on ATTRIBUTE_DEXTERITY,
     * or CLASS_ABILITY_MONK_KI_POINTS on being a MONK.
     *
     * Also an INVENTORY_DAGGER can be depend on an INVENTORY_BAGPACK. */
    var dependsOn: CharacterAttribute?
}
