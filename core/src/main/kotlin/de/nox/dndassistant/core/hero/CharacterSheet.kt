package de.nox.dndassistant.core.hero

import de.nox.dndassistant.core.terms.RollingTerm
import de.nox.dndassistant.core.terms.TermVaribales

/**
 * A Character Sheet is a collection of character values.
 *
 * Example:
 * - Player Character "Hero Adventurer"
 *   <pre>{@code
 *   CharacterSheet(
 *    name = "Hero Adventurer",
 *    level = 1,
 *    notes = "Hero played by Kotlin Examples",
 *    lore = mutableListOf((
 *      "(2022-07-29) Born in Examples",
 *      "(2022-07-30) Currently Lawful Neutral",
 *      "Looks a bit shaggy",
 *      "Wants to become real",
 *      "Currently imprisoned in tests",
 *    ),
 *    characterValues = mutableListOf((
 *      CharacterValue("STRENGTH", "ATTRIBUTE", Number(10), "+0", note),
 *      CharacterValue("DEXTERITY", "ATTRIBUTE", Number(18), "+4", note),
 *      ...
 *      CharacterValue("PROFICIENCY", "PROFICIENCY", Number(2), "+2", note),
 *      ...
 *      CharacterValue("PERCEPTION", "ATTRIBUTE", Reference("WISDOM"), "+2", note),
 *      CharacterValue("STEALTH", "ATTRIBUTE", Reference("DEXTERITY") + Reference("PROFICIENCY"), "+2", note),
 *      ...
 *      CharacterValue("KNITTING", "ATTRIBUTE", Reference("CONSTITUTION") + Reference("PROFICIENCY"), "+2", "custom"),
 *      ...
 *      CharacterValue("SPELL_ATTACK", "SPECIAL", Reference("CHARISMA") + Reference("PROFICIENCY"), "+2", note),
 *      ...
 *      CharacterValue("DAGGER_ATTACK", "SPECIAL", Reference("DEXTERITY") + Reference("PROFICIENCY"), "+6", note),
 *      CharacterValue("MACE_ATTACK", "SPECIAL", Reference("STRENGTH"), "+0", note),
 *      ...
 *    ),
 *    traits = mutableListOf((
 *      CharacterTrait("KI_POINTS", "description of ki points"),
 *      CharacterTrait("Lucky", "description of lucky trait"),
 *      CharacterTrait("BEASTIC_PERCEPTION", "description beastic perception"),
 *      CharacterTrait("PROFICIENCY_DAGGER", "is proficient with dagger as thief"),
 *      CharacterTrait("THIEFS_CANT", "understands thiefs cant"),
 *      CharacterTrait("GNOMIC", "understands gnomic language (written and spoken)"),
 *      ...
 *    )
 *    resources = mutableListOf((
 *      CharacterResource("KI_POINTS", category = "TRAIT", max = 10, available = 9, restoreable = LONG_REST, note),
 *      CharacterResource("SPELL_SLOT_1", category = "TRAIT", max = 4, available = 2, restoreable = LONG_REST, note),
 *      ...
 *      CharacterResource("MAGIC ITEM SPELL", category = "MAGIC_ITEM", max = 10, available = 9, restoreable = SHORTREST, note),
 *      CharacterResource("MAGIC ITEM", category = "MAGIC_ITEM", max = 10, available = 9, restoreable = CUSTOM, note),
 *      ...
 *      CharacterResource("DAGGER", category = "ITEM", max = Int.MAX_VALUE, available = 9, restoreable = NOT_RESTORABLE, note),
 *      CharacterResource("GOLD_POINTS", category = "CURRENCY", max = Int.MAX_VALUE, available = 9, restoreable = NOT_RESTORABLE, note),
 *      ...
 *      CharacterResource("NOSE FLICKS", category = "CUSTOM", max = Int.MAX_VALUE, available = 9, restoreable = NOT_RESTORABLE, note),
 *      CharacterResource("NIGHTS UNTIL EXECUTION", category = "CUSTOM", max = Int.MAX_VALUE, available = 2, restoreable = NOT_RESTORABLE, note),
 *    )
 *   )}</pre>
 */
public data class CharacterSheet(
	val name: String,

    /** Level or challenge rating. */
	var level: Int,

	/** Maximal hitpoints. */
	var hitpointsMax: Int,

    /** Current hitpoints. */
	var hitpoints: Int,

	/** Main notes about the character,
     * such as player or as enemy a quick lore overview. */
	var notes: String,

	/** Story and notes about the character. */
	val lore: MutableList<String>,

	/** Attributes with numbers, such as the ATTRIBUTES, SKILLS or ATTACKS. */
	val stats: MutableList<CharacterValue>,

	/** Attributes which */
	val traits: MutableList<CharacterTrait>,

	/** Attributes which can be counted, such as SPELL_SLOTS or INVENTORY_ITEMS.
     * Also custom counter, such as */
	val resources: MutableList<CharacterResource>,
)

