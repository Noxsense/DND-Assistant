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

	println("=============================")
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
		println("Player:       ${pc.player}");

	println("Name:         ${pc.name}, ${pc.race}");
	println("XP:           ${pc.expiriencePoints}");

	/* single line and double line.*/
	val lineLen = 30
	val sLine = (1..lineLen).joinToString(separator = "", transform = { _ -> "-"})
	val dLine = (1..lineLen).joinToString(separator = "", transform = { _ -> "="})

	println(sLine)

	for (a in enumValues<Ability>()) {
		var score = pc.abilityScore.getOrDefault(a, 10)
		var mod = pc.abilityModifier.getOrDefault(a, 0)
		var prof = mod + pc.proficientValue

		var save = if (a in pc.savingThrows) {
			"*(%+d) SAVING".format(prof)
		} else {
			" (%+d) saving".format(mod)
		}

		print("%-15s %2d (%+d) \u21d2 %s".format(
			a.fullname + ":", score, mod, save))

		if (a != Ability.CON) {
			print("\t" + enumValues<Skill>()
				.filter{it.source == a}
				.map {
					val name = it.name
					val level = pc.getProficiencyFor(it)

					val (indicator, show) = when (level) {
						Proficiency.EXPERT     -> Pair(":", name)
						Proficiency.PROFICIENT -> Pair(".", name.capitalize())
						Proficiency.NONE       -> Pair(" ", name.toLowerCase())
					}

					var bonus = mod + level.factor * pc.proficientValue
					"%s(%+d) %s".format(indicator, bonus, show)
				}.joinToString(separator = "\t"))
		}

		println()
	}

	println(sLine)

	println("%-15s %d".format("AC:", pc.armorClass))
	println("%-15s %+d".format("Init:", pc.initiative))
	println("%-15s %d/%d".format("HP:", pc.curHitPoints, pc.maxHitPoints)
		+ (if (pc.hasTmpHitpoints) "(%{pc.tmpHitPoints})" else ""))

	println("%-15s %s".format("Death Saves:", pc.deathSaves.joinToString(
		prefix = "[", separator = " ", postfix = "]",
		transform = { when (it) {
			-1 -> "x" // failure
			1 -> "\u2713" // check mark
			else -> "_"
	}}) + (when (pc.checkDeathFight()) {
		-3 -> " Dead"
		+3 -> " Stable"
		0 -> ""
		else -> " Fighting (Roll)!"
	})))

	println(sLine)

	println("%-15s %s".format("Languages:", pc.knownLanguages.joinToString()))
	println("%-15s %s".format("Proficiencies:",
		pc.proficientSkills.toString() +
		pc.proficientTools.toString()))

	println(dLine)

	var purse = Money()
	println("Money: ${purse}")

	purse += Money(gp = 100, sp = 50)
	println("Money: ${purse} (after payment)")

	purse = purse.change(Money.GOLD)
	println("Money: ${purse} (after change gp \u2192 pp)")

	purse = Money(gp = purse.gp, pp = purse.pp + 2, cp = purse.cp, sp = purse.sp)
	println("Money: ${purse} (after manual change gp \u2192 pp)")

}
