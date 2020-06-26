package de.nox.dndassistant;

import kotlin.math.floor;

private fun getModifier(value : Int) = floor((value - 10) / 2.0).toInt()

data class PlayerCharacter(val name : String, val player: String) {
	var abilityScore : Map<Ability, Int> = enumValues<Ability>().map { it }.associateWith { 10 }
		private set

	var abilityModifier : Map<Ability, Int> = abilityScore.mapValues { getModifier(it.value) }
		private set

	var savingThrows : List<Ability> = listOf()
		private set

	var proficientSkills : Map<Skill, ProficiencyLevel> = mapOf()
		private set
	var proficientTools : Map<String, ProficiencyLevel> = mapOf()
		private set
	var proficientValue : Int = 2
		private set

	fun rollAbilityScores() {
		abilityScore = abilityScore . mapValues { Die(20).roll() }
		abilityModifier = abilityScore . mapValues { getModifier(it.value) }
	}

	/* Set the attributes' values.
	 * If a param is set to (-1), it will be rolled with a D20.
	 */
	fun setAbilityScore(a : Ability, v: Int) {

		val d20 : SimpleDice = SimpleDice(20, 1);

		abilityScore += Pair(a, if (v > 0) v else d20.roll())

		abilityModifier = abilityScore.mapValues { getModifier(it.value) }
	}

	fun printAbilityScores() {
		for (a in enumValues<Ability>()) {
			var score = abilityScore.getOrDefault(a, 10)
			var mod = abilityModifier.getOrDefault(a, 0)
			var prof = mod + proficientValue
			var expt = prof + proficientValue
			var save = a in savingThrows

			println("%13s: %2d (%+d) \u21d2  %s, %s".format(
				a.fullname,
				score, mod,
				if (save) "SAVING(%+d)".format(prof) else "saving(%+d)".format(mod),
				enumValues<Skill>().filter{it.source == a}
					.map{
						val level = proficientSkills.getOrDefault(it, ProficiencyLevel.NONE)
						val (proficientStr,bonus,show) = when (level) {
							ProficiencyLevel.EXPERT     -> Triple("**",expt,it.name)
							ProficiencyLevel.PROFICIENT -> Triple("*",prof,it.name[0].toTitleCase() + it.name.substring(1).toLowerCase())
							ProficiencyLevel.NONE       -> Triple("",mod,it.name.toLowerCase())
						}
						"%s(%+d%s)".format(show, bonus, proficientStr)
					}.joinToString()))
		}
	}

	/* Add proficiency to saving trhow.*/
	fun addProficiency(saving : Ability) {
		if (!(saving in savingThrows)) savingThrows += saving
	}

	/* Add proficiency to a skill. If twice, add expertise.*/
	fun addProficiency(skill : Skill) {
		val level
			= if (skill in proficientSkills.keys) ProficiencyLevel.EXPERT
			else ProficiencyLevel.PROFICIENT

		proficientSkills += Pair(skill, level)
	}
}

enum class Ability(val fullname : String) {
	STR("STRENGTH"),
	DEX("DEXTERITY"),
	CON("CONSTITUTION"),
	INT("INTELLIGENCE"),
	WIS("WISDOM"),
	CHA("CHARISMA")
}

enum class Skill(val source: Ability) {
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

enum class ProficiencyLevel(val factor: Int){ NONE(0), PROFICIENT(1), EXPERT(2) }
