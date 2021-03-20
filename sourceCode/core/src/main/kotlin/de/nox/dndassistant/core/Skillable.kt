package de.nox.dndassistant.core

import kotlin.math.floor

/** Something a character can learn.
 * For example a skill, to add proficiency.
 * A Weapon or armor to use it.
 * A Tool to use it.
 * A language to communicate. */
interface Skillable;

/* Proficiency level.*/
enum class Proficiency{
	NONE,
	PROFICIENT,
	EXPERT;

	val factor: Int get() = this.ordinal

	val symbol: Char get() = when (this) {
		NONE -> ' '
		PROFICIENT -> '*'
		EXPERT -> '#'
	}

	operator fun plus(other: Proficiency?) = when {
		other == null -> this // null as NONE.
		this == NONE -> other // NONE(0) + ANY(x) = x
		this == PROFICIENT && other == NONE -> this // PROF(1) + NONE(0) = PROF (1)
		else -> EXPERT // (X>=1) + (Y>=1) = Z(>1)
	}

	operator fun minus(other: Proficiency?) = when {
		other == null -> this // this(?) - null(0) = this
		other == NONE -> this // this(?) - NONE(0) = this
		this == EXPERT && other == PROFICIENT -> PROFICIENT // this(2) - other(1) = 1
		else -> NONE // this(>=1) - other(>=1) = 0
	}
}

/** A language a character can speak, read and write. */
data class Language(val name: String) : Skillable;

/* The base ability score.*/
enum class Ability(val fullname: String) {
	STR("STRENGTH"),
	DEX("DEXTERITY"),
	CON("CONSTITUTION"),
	INT("INTELLIGENCE"),
	WIS("WISDOM"),
	CHA("CHARISMA");

	companion object {
		/** Get the modifier for a certain score value. */
		public fun scoreToModifier(score: Int) : Int
			= floor((score - 10) / 2.0).toInt()

		public fun fromString(str: String) : Ability
			= when (str.toUpperCase()) {
				"STR", "STRENGTH"      -> Ability.STR
				"DEX", "DEXTERITY"     -> Ability.DEX
				"CON", "CONSTITUTION"  -> Ability.CON
				"INT", "INTELLIGENCE"  -> Ability.INT
				"WIS", "WISDOM"        -> Ability.WIS
				"CHA", "CHARISMA"      -> Ability.CHA
				else -> throw Exception("String \"$str\" cannot be parsed to an Ability.")
			}
	}
}

/* Body skill.*/
enum class Skill(val source: Ability) : Skillable {
	ATHLETICS(Ability.STR),

	ACROBATICS(Ability.DEX),
	SLEIGHT_OF_HAND(Ability.DEX),
	STEALTH(Ability.DEX),

	ARCANA(Ability.INT),
	HISTORY(Ability.INT),
	INVESTIGATION(Ability.INT),
	NATURE(Ability.INT),
	RELIGION(Ability.INT),

	ANIMAL(Ability.WIS),
	INSIGHT(Ability.WIS),
	MEDICINE(Ability.WIS),
	PERCEPTION(Ability.WIS),
	SURVIVAL(Ability.WIS),

	DECEPTION(Ability.CHA),
	INTIMIDATION(Ability.CHA),
	PERFORMANCE(Ability.CHA),
	PERSUASION(Ability.CHA);

	val fullname: String get()
		= name.toLowerCase().replace("_", " ")

	override fun toString() : String
		= fullname
}
