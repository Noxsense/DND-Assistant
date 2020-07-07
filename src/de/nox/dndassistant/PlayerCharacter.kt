package de.nox.dndassistant

import kotlin.math.floor;

private fun getModifier(value : Int) = floor((value - 10) / 2.0).toInt()

data class PlayerCharacter(
	val name : String,
	val race: String = "Human" /*TODO*/,
	val gender: String = "divers",
	val player: String = ""
	) {

	var expiriencePoints : Int
		= 0

	var abilityScore : Map<Ability, Int>
		= enumValues<Ability>().map { it }.associateWith { 10 }
		private set

	var abilityModifier : Map<Ability, Int>
		= abilityScore.mapValues { getModifier(it.value) }
		private set

	var savingThrows : List<Ability>
		= listOf()
		private set

	var proficientSkills : Map<Skill, Proficiency>
		= mapOf()
		private set

	var proficientTools : Map<String, Proficiency>
		= mapOf()
		private set

	var proficiencies : Map<AbstractSkill, Proficiency>
		= mapOf()
		private set

	var proficientValue : Int
		= 2

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

	fun savingScore(a: Ability) : Int
		= abilityModifier.getOrDefault(a, 0) +
			if (a in savingThrows) proficientValue else 0

	fun skillScore(s: Skill) : Int
		= abilityModifier.getOrDefault(s.source, 0) +
			getProficiencyFor(s).factor * proficientValue

	fun abilityScore(a: Ability) : Int
		= abilityScore.getOrDefault(a, 10)

	fun abilityModifier(a: Ability) : Int
		= abilityModifier.getOrDefault(a, 10)

	/* Get a random roll (D20) for a requesting ability, adding its bonus.*/
	fun rollCheck(a: Ability) : Int = Die(20).roll() + abilityModifier(a)

	/* Get a random roll (D20) for a requesting skill, adding its bonus.*/
	fun rollCheck(s: Skill) : Int = Die(20).roll() + skillScore(s)

	/* Get a random roll (D20) for a requesting saving throw, adding its bonus.*/
	fun rollSave(a: Ability) : Int = Die(20).roll() + savingScore(a)

	fun getProficiencyFor(skill: Skill) : Proficiency
		= proficientSkills.getOrDefault(skill, Proficiency.NONE)

	/* Add proficiency to saving trhow.*/
	fun addProficiency(saving : Ability) {
		if (!(saving in savingThrows)) savingThrows += saving
	}

	/* Add proficiency to a skill. If twice, add expertise.*/
	fun addProficiency(skill : Skill) {
		val level = if (skill in proficientSkills.keys) {
				Proficiency.EXPERT
			} else {
				Proficiency.PROFICIENT
			}

		proficientSkills += Pair(skill, level)
	}

	val initiative : Int get()
		= this.abilityModifier.getOrDefault(Ability.DEX,
			getModifier(this.abilityScore.getOrDefault(Ability.DEX, 0)))

	fun initiativeRoll() : Int = initiative + Die(20).roll()

	val armorClass : Int get() {
		// TODO (2020-06-26)
		// Look up, what the PC's wearing. Maybe add DEX modifier.
		// Not wearing anything: AC 10 + DEX
		return 10 + abilityModifier.getOrDefault(Ability.DEX, 0)
	}

	val maxHitPoints : Int get()
		= -1 // TODO(2020-06-26)

	val curHitPoints : Int get()
		= -1 // TODO(2020-06-26)

	val tmpHitPoints : Int get()
		= -1 // TODO(2020-06-26)

	val hasTmpHitpoints : Boolean get()
		= tmpHitPoints > 0 && tmpHitPoints != maxHitPoints

	var deathSaves : Array<Int> = arrayOf(0, 0, 0, 0, 0) // maximal 5 chances.
		private set

	fun resetDeathSaves() {
		deathSaves.map { _ -> 0 }
	}

	/* Count fails and successes and returns the more significcant value.*/
	fun checkDeathFight() : Int {
		val success = deathSaves.count { it > 0 }
		val failed = deathSaves.count { it < 0 }

		return when {
			success > 2 -> 3
			failed > 2 -> -3
			success > failed -> 1
			success < failed -> -1
			else -> 0
		}
	}

	/* Take a short rest. Recover hitpoints, maybe magic points, etc. */
	fun rest(shortRest: Boolean = true) {
		println("Rest " + (if (shortRest) "short" else "long"))
		// TODO (2020-06-26)
	}

	var knownLanguages : List<String>
		= listOf("Common")
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
