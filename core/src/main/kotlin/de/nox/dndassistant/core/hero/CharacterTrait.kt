package de.nox.dndassistant.core.hero

import de.nox.dndassistant.core.hero.CharacterAttribute

/**
 * A Character Trait is part of the Character Stats or Inventory.
 *
 * They represent traits which are more written or single values,
 * such as known languages, single proficiencies or whole descriptions
 * of the character abilities, such as descriptions of KI points.
 */
public data class CharacterTrait(
	override val name: String,
	override val category: String,

	override var notes: String,

	override var dependsOn: CharacterAttribute?,

    /** If true, this trait can be mapped
     * as "is proficient" to another CharacterAttribute,
     * mostly a CharacterValue. */
    val indicatesProficiency: Boolean,

    /** A trait can be directly connected to another attribute.
     * Since a attribute can be depending on another attribute,
     * this can become circular. */
    val influencedValue: CharacterValue?,
): CharacterAttribute {
	override var displayName: String = this.name
}
