package de.nox.dndassistant;

import kotlin.math.floor;

private fun getModifier(value : Int) = floor((value - 10) / 2.0).toInt()

data class PlayerCharacter(
	val name : String,
	val race: String = "Human" /*TODO*/,
	val gender: String = "divers",
	val player: String = ""
	) {
	
	var expiriencePoints : Int = 0

	var abilityScore : Map<Ability, Int> = enumValues<Ability>().map { it }.associateWith { 10 }
		private set

	var abilityModifier : Map<Ability, Int> = abilityScore.mapValues { getModifier(it.value) }
		private set

	var savingThrows : List<Ability> = listOf()
		private set

	var proficientSkills : Map<Skill, Proficiency> = mapOf()
		private set

	var proficientTools : Map<String, Proficiency> = mapOf()
		private set

	var proficientValue : Int = 2

	val initiative : Int get()
		= this.abilityModifier.getOrDefault(Ability.DEX,
			getModifier(this.abilityScore.getOrDefault(Ability.DEX, 0)))

	fun initiativeRoll() : Int = initiative + Die(20).roll()
	
	val armorClass : Int get() {
		println("Look up, what the PC's wearing. Maybe add DEX modifier.")
		println("Not wearing anything: AC 10 + DEX")
		return 10 + abilityModifier.getOrDefault(Ability.DEX, 0)
	}

	val maxHitPoints : Int get()
		= -1 // TODO(2020-06-26)

	val currentHitPoints : Int get()
		= -1 // TODO(2020-06-26)

	val temporaryHitPoints : Int get()
		= -1 // TODO(2020-06-26)

	var deathSaves : Array<Int> = arrayOf(0, 0, 0, 0, 0) // maximal 5 chances.
		private set

	fun rollAbilityScores() {
		abilityScore = abilityScore.mapValues { Die(20).roll() }
		setAbilityModifiers()
	}

	private fun setAbilityModifiers() {
		abilityModifier = abilityScore.mapValues { getModifier(it.value) }
	}

	/* Set the attributes' values.
	 * If a param is set to (-1), it will be rolled with a D20.
	 */
	fun setAbilityScore(a : Ability, v: Int) {
		val d20 : SimpleDice = SimpleDice(20, 1);

		abilityScore += Pair(a, if (v > 0) v else d20.roll())

		abilityModifier = abilityScore.mapValues { getModifier(it.value) }
	}

	fun skillScore(skill: Skill) : Int
		= abilityModifier.getOrDefault(skill.source, 0) + 0

	fun getProficiencyFor(skill: Skill) : Proficiency
		= proficientSkills.getOrDefault(skill, Proficiency.NONE)

	/* Add proficiency to saving trhow.*/
	fun addProficiency(saving : Ability) {
		if (!(saving in savingThrows)) savingThrows += saving
	}

	/* Add proficiency to a skill. If twice, add expertise.*/
	fun addProficiency(skill : Skill) {
		val level
			= if (skill in proficientSkills.keys) Proficiency.EXPERT
			else Proficiency.PROFICIENT

		proficientSkills += Pair(skill, level)
	}
}

/* The base ability score.*/
enum class Ability(val fullname : String) {
	STR("STRENGTH"),
	DEX("DEXTERITY"),
	CON("CONSTITUTION"),
	INT("INTELLIGENCE"),
	WIS("WISDOM"),
	CHA("CHARISMA")
}

/* Body skill.*/
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

/* Proficiency level.*/
enum class Proficiency(val factor: Int){
	NONE(0),
	PROFICIENT(1),
	EXPERT(2)
}
