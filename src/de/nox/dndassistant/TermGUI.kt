package de.nox.dndassistant;

fun main() {
	println(
		"DnD Application, display stats and roll your dice!\n"
		+ "Happy Gaming! :D\n"
		+ "==================================================\n");

	val die: SimpleDice = SimpleDice(20);

	for (i in 1..10) {
		println(die.roll());
	}

	for (i in 1..10) {
		print(SimpleDice(1, -1).roll())
		print("; ")
		print(SimpleDice(3, -1).roll())
		print("; ")
		println(SimpleDice(1, -3).roll())
	}

	val diceRegex = "3d8 + d12 - D21 + 3 + 3 - 3"
	val diceTerm = parseDiceTerm(diceRegex)

	println(diceTerm)

	println(rollTake(Die(6), 3, 4, true))

	// val diceRegexInvalid = "3d8 + d12 + 3 + 3 - 3 + 12d"
	// parseDiceTerm(diceRegexInvalid)

	val pc : PlayerCharacter = PlayerCharacter("Onyx Necklace", "Nox")

	pc.rollAbilityScores()
	pc.printAbilityScores()

	println("Proficent Skills: " + pc.proficientSkills)
	pc.addProficiency(Skill.SLEIGHT_OF_HAND)
	println("Proficent Skills: " + pc.proficientSkills)
	pc.addProficiency(Skill.STEALTH)
	println("Proficent Skills: " + pc.proficientSkills)
	pc.addProficiency(Skill.SLEIGHT_OF_HAND)
	println("Proficent Skills: " + pc.proficientSkills)
	pc.addProficiency(Ability.DEX)
	pc.addProficiency(Ability.INT)

	println("---------")
	println(pc.name + " from " + pc.player)
	pc.printAbilityScores()
}
