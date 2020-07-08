package de.nox.dndassistant

/* Proficiency level.*/
enum class Proficiency(val factor: Int){
	NONE(0),
	PROFICIENT(1),
	EXPERT(2)
}

interface Skillable

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
	PERSUASION(Ability.CHA)
}
