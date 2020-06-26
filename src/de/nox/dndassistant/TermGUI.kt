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

	val pc : PlayerCharacter = PlayerCharacter("Onyx Necklace", race = "Gnome", player = "Nox")

	pc.rollAbilityScores()
	printPlayer(pc)

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
	printPlayer(pc)
}

/* Print PlayerCharacter.
 * Display all statistics like ability scores and skill scores, languages.
 * Show treats and features.
 *
 * Print to normal terminal interface.
 */
fun printPlayer(pc: PlayerCharacter) {

	if (pc.player != "")
		println("Player: ${pc.name}, ${pc.race}");

	println("Name:   ${pc.name}, ${pc.race}");
	println("Name:   ${pc.name}, ${pc.race}");
	println("XP:     ${pc.expiriencePoints}");

	for (a in enumValues<Ability>()) {
		var score = pc.abilityScore.getOrDefault(a, 10)
		var mod = pc.abilityModifier.getOrDefault(a, 0)
		var prof = mod + pc.proficientValue
		
		var save = if (a in pc.savingThrows) {
			"SAVING(%+d)".format(prof)
		} else {
			"saving(%+d)".format(mod)
		}

		println("%13s: %2d (%+d) \u21d2  %s, %s".format(
			a.fullname,
			score, mod,
			save,
			enumValues<Skill>()
				.filter{it.source == a}
				.map {
					val name = it.name
					val lower = name.toLowerCase()
					val level = pc.getProficiencyFor(it)
					val (indicator, show) = when (level) {
						Proficiency.EXPERT     -> Pair("**", name)
						Proficiency.PROFICIENT -> Pair("*", name[0] + lower.substring(1))
						Proficiency.NONE       -> Pair("", lower)
					}

					var bonus = mod + level.factor * pc.proficientValue
					"%s(%+d%s)".format(show, bonus, indicator)
				}
				.joinToString()))
	}
}
