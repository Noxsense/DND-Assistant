package de.nox.dndassistant.core

/**
 * A Condition a hero may have.
 * See Dnd Player's Handbook Page 290. */
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
