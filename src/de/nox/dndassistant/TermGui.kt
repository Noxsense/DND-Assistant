package de.nox.dndassistant

private val logger = LoggerFactory.getLogger("TermGUI")

fun main() {
	println(
		"DnD Application, display stats and roll your dice!\n"
		+ "Happy Gaming! :D\n"
		+ "==================================================\n");

	testDice()
	logger.info("=============================")

	testMoney()
	logger.info("=============================")

	val pc : PlayerCharacter = PlayerCharacter("Onyx Necklace", race = "Gnome", player = "Nox")

	pc.rollAbilityScores()
	printPlayer(pc)

	logger.debug("Proficent Skills: " + pc.proficientSkills)
	pc.addProficiency(Skill.SLEIGHT_OF_HAND)
	logger.debug("Proficent Skills: " + pc.proficientSkills)
	pc.addProficiency(Skill.STEALTH)
	logger.debug("Proficent Skills: " + pc.proficientSkills)
	pc.addProficiency(Skill.SLEIGHT_OF_HAND)
	logger.debug("Proficent Skills: " + pc.proficientSkills)
	pc.addProficiency(Ability.DEX)
	pc.addProficiency(Ability.INT)

	logger.info("=============================\n\n\n")
	printPlayer(pc)

	println("\n\n\n")

	println(playerAbilitiesHorizontally(pc, false, true))

	val weapon =  Weapon("Dagger", 1.0, Money(gp=2),
		isMartial = false, isRanged = false, range = (1..5),
		dmgDice = DiceTerm(Die(4)),
		dmgType = "piercing",
		weightClass = 1 /*Weapon.WEIGHT_CLASS_LIGHT*/,
		isFinesse = true,
		note = "Finesse, light, thrown (range 20/60)")

	pc.inventory += weapon

	logger.info("Onyx' inventory: ${pc.inventoryWeight()} lb, ${pc.inventory}, ${pc.purse}")

	// pc.wield(pc.inventory[0] as Weapon)

	logger.info("Onyx' inventory: ${pc.inventoryWeight()} lb, ${pc.inventory}, ${pc.purse}")
	// logger.info("Onyx' wields: ${pc.equippedHandMain}, ${pc.equippedHandOff}")
	// logger.info("Onyx' hits: ${pc.attackRoll()}")

	// pc.unwield(both = true)

	logger.info("Onyx' inventory: ${pc.inventoryWeight()} lb, ${pc.inventory}, ${pc.purse}")
	// logger.info("Onyx' wields: ${pc.equippedHandMain}, ${pc.equippedHandOff}")
	// logger.info("Onyx' hits: ${pc.attackRoll()}")

	logger.info("Sell the weapon (${weapon})")
	pc.sellItem(weapon)

	logger.info("Onyx' inventory: ${pc.inventoryWeight()} lb, ${pc.inventory}, ${pc.purse}")
	// logger.info("Onyx' wields: ${pc.equippedHandMain}, ${pc.equippedHandOff}")
	// logger.info("Onyx' hits: ${pc.attackRoll()}")

	logger.info("Buy the weapon (${weapon})")
	pc.buyItem(weapon)

	logger.info("Onyx' inventory: ${pc.inventoryWeight()} lb, ${pc.inventory}, ${pc.purse}")
	// logger.info("Onyx' wields: ${pc.equippedHandMain}, ${pc.equippedHandOff}")
	// logger.info("Onyx' hits: ${pc.attackRoll()}")
}

fun testDice() : Boolean {
	var passed = true

	val die: SimpleDice = SimpleDice(20);

	for (i in 1..10) {
		logger.verbose(die.roll());
	}

	for (i in 1..10) {
		logger.verbose((1..3).map { SimpleDice(1, -3).roll() }.joinToString() )
	}

	val diceRegex = "3d8 + d12 - D21 + 3 + 3 - 3"
	val diceTerm = DiceTerm.parse(diceRegex)

	logger.info(diceTerm)

	logger.info(D6.rollTake(3, 4, true))

	return passed;
}

fun testMoney(): Boolean {

	var passedAll = true
	var current: Boolean
	var (passedCnt,allCnt) = Pair(0,0)
	var testStr : String

	var purse = Money()

	current = testEquals("normalized", 0, purse.asCopper)
	passedAll = passedAll && testEquals("Empty Purse", true, current)
	passedCnt += if (current) 1 else 0; allCnt += 1

	purse += Money(sp = 5, gp = 3 * Money.PP_GP /*30*/)

	current = testEquals("normalized", 3050, purse.asCopper)
	current = testEquals("Gold", 30, purse.gp) && current
	current = testEquals("Silver", 5, purse.sp) && current
	current = testEquals("result", Money(0, 30, 0, 5, 0), purse) && current
	passedAll = passedAll && testEquals("Paid 30gp and 5sp", true, current)
	passedCnt += if (current) 1 else 0; allCnt += 1

	logger.debug("-----")

	purse = purse.changeUp(Money.GP)

	// still the same amount.
	current = testEquals("normalized", 3050, purse.asCopper)
	current = testEquals("Platinum", 1, purse.pp) && current
	current = testEquals("Gold", 20, purse.gp) && current
	current = testEquals("Silver", 5, purse.sp) && current
	current = testEquals("result", Money(1, 20, 0, 5, 0), purse) && current
	passedAll = passedAll && testEquals("GP \u2191 PP, same sum", true, current)
	passedCnt += if (current) 1 else 0; allCnt += 1

	purse = purse.changeDown(Money.SP)

	// still the same amount.
	current = testEquals("normalized", 3050, purse.asCopper)
	current = testEquals("Platinum", 1, purse.pp) && current
	current = testEquals("Gold", 20, purse.gp) && current
	current = testEquals("Silver", 4, purse.sp) && current
	current = testEquals("Copper", 10, purse.cp) && current
	current = testEquals("result", Money(1, 20, 0, 4, 10), purse) && current
	passedAll = testEquals("SP \u2193 CP, same sum", true, current) && passedAll
	passedCnt += if (current) 1 else 0; allCnt += 1

	logger.debug("-----")

	purse = Money(cp = 1) - Money(cp = 2)
	current = testEquals("normalized", 1, purse.asCopper)
	passedAll = testEquals("Not reduced: 1cp", true, current) && passedAll
	passedCnt += if (current) 1 else 0; allCnt += 1

	purse = Money(ep = 10, ignoreElectrum = true)
	passedAll = (testEquals("init: ep:10", 500, purse.asCopper)) && passedAll
	passedCnt += if (current) 1 else 0; allCnt += 1
	// TODO (2020-07-03)

	testStr = "Change up: EP(10) \u2191 GP, all (2)"
	purse = purse.changeUp(Money.EP)
	current = testEquals(testStr, 500, purse.asCopper) && current
	current = testEquals(testStr, 5, purse.gp) && current
	current = testEquals(testStr, 0, purse.ep) && current
	passedAll = testEquals(testStr, true, current) && passedAll
	passedCnt += if (current) 1 else 0; allCnt += 1
	logger.debug("Purse: ${purse}, change up, ignore electrum => change all.")

	purse = Money(ep = 10, ignoreElectrum = true)
	passedAll = testEquals("init: ep:10", 500, purse.asCopper) && passedAll
	passedCnt += if (current) 1 else 0; allCnt += 1

	testStr = "change down: EP \u2192 CP, all (50)"
	purse = purse.changeDown(Money.EP)
	current = testEquals(testStr, 500, purse.asCopper) && current
	current = testEquals(testStr, 0, purse.ep) && current
	current = testEquals(testStr, 50, purse.sp) && current
	passedAll = passedAll && current
	passedCnt += if (current) 1 else 0; allCnt += 1

	/* Thoughts
	 * x:02pp 01gp 0ep 99sp 20cp = 3110cp
	 * -: 0pp 25gp 0ep  0sp 25cp ⇒ 2523cp possible
	 * ------------------------------------------------
	 * =:0pp -24gp 0ep  0sp -2cp ⇒ but it is negative
	 *
	 * vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
	 * x: 2pp  1gp 0ep 99sp 20cp
	 * -: 1pp 15gp 0ep  0sp 25cp ⇒ other.changeUp(G)
	 * -: 2pp  5gp 0ep  0sp 25cp ⇒ other.changeUp(G)
	 * -: 2pp  1gp 0ep 40sp 25cp ⇒ other.changeDown(S)
	 * -: 2pp  1gp 0ep 41sp 15cp ⇒ other.changeUp(C)
	 * ================================================
	 * =: 0pp  0gp 0ep 58sp  2cp ⇒ result option
	 */

	logger.debug("=======")

	testStr = "[10pp (100gp)] - [Bow 25gp] = [(75gp) 7pp 5gp]"

	purse = Money(pp = 10) // 100g
	current = testEquals( "normalized", 10000, purse.asCopper)
	passedAll = passedAll && testEquals( "Money set 10pp", true, current)
	passedCnt += if (current) 1 else 0; allCnt += 1

	purse -= Money(gp = 25) // sh
	logger.debug("Buy a new Bow (25gp), purse had enough.")

	current = testEquals("normalized", 10000 - 2500, purse.asCopper)
	current = testEquals(testStr, Money(pp=7, gp=5), purse) && current
	passedAll = passedAll && testEquals("Change money invalid (bow)", true, current)
	passedCnt += if (current) 1 else 0; allCnt += 1

	// --------

	purse = Money(2, 1, 0, 99, 20)
	testStr = "Bow (25gp) and 5 arrows (25cp) could be paid, rest: 58sp 2cp"

	current = testEquals("normalized", 3110, purse.asCopper)
	current = testEquals("Initiated: 2pp 99sp 20cp", true, current) && current
	passedAll = passedAll && current
	passedCnt += if (current) 1 else 0; allCnt += 1

	// by a bow and some arrows (5 arrpws = 100/20cp * 5 = 25cp)
	purse -= Money(0, 25, 0, 0, 25)

	current = testEquals("normalized", 3110 - 2500 - 25, purse.asCopper)
	current = testEquals("paid bow", Money(sp = 58, cp = 5), purse) && current
	passedAll = passedAll && testEquals(testStr, true, current)

	passedCnt += if (current) 1 else 0; allCnt += 1

	logger.debug("=======")

	logger.debug("Money - Test: ${passedCnt} / ${allCnt}")
	logger.debug("Money - Test: " + (if (passedAll) "PASSED" else "FAILED"))

	return passedAll;
}

fun testEquals(s: String = "", should: Any, was: Any) : Boolean {
	val eq = should == was

	var str = "TEST  " +
		(if (eq) "PASSED" else "FAILED, should be: ${should}, was: ${was}") +
		(if (s == "") "" else "  (${s})")

	logger.verbose(str)

	return eq
}

fun playerAbilitiesHorizontally(
	pc: PlayerCharacter,
	withSkill: Boolean = false,
	hline: Boolean = false)
	: String {

	/* spacey
	 * " #(+5) SLEIGHT_OF_HAND |"
	 * " ..20 (+5) ........... |"
	 *
	 * dense
	 * " FULLNAME.... |"
	 * " ..20 (+5) .. |" */
	val width = if (withSkill) 21 else 12
	val valuesFormat = "  (%+d) %2d"
	val headerFormat = " %${-width}s "

	val header = enumValues<Ability>().joinToString(
		prefix = "|", postfix = "|", separator = "|",
		transform = { a -> headerFormat.format(a.fullname) })

	val values = enumValues<Ability>().joinToString(
		prefix = "|", postfix = "|", separator = "|",
		transform = { a -> valuesFormat.format(
			pc.abilityModifier(a),
			pc.abilityScore(a)) + " ".repeat(width - 7)})

	val hlineStr = enumValues<Ability>().joinToString(
		prefix = "+", postfix = "+", separator = "+",
		transform = { _ -> ("-".repeat(width + 2)) })

	var more = ""
	if (withSkill) {
		/* 0: saving   , ~ ,     ~       ,   ~        ,    ~       ,    ~
		 * 1: athletics, - , acrobatics  , arcana     , animal     , deception
		 * 2:    -     , - , sleight     , history    , insight    , intimidation
		 * 3:    -     , - , stealth     , invest     , medicine   , performance
		 * 4:    -     , - , -           , nature     , perception , persuation
		 * 5:    -     , - , -           , religion   , survival   , -
		 * BUT: Direction is not always given like that. */

		val maxRow = 5
		var moreArray = Array (maxRow) { Array (enumValues<Ability>().size) {""} }
		var r : Int

		for (s in enumValues<Skill>()) {
			val name = s.name
			val lower = name.toLowerCase()
			val level = pc.getProficiencyFor(s)
			var src = s.source

			val (indicator, show) = when (level) {
				Proficiency.EXPERT     -> Pair("#", name)
				Proficiency.PROFICIENT -> Pair("*", lower.capitalize())
				Proficiency.NONE       -> Pair(" ", lower)
			}

			more = "%s(%+d) %s".format(indicator, pc.skillScore(s), show)

			r = 0 // find row (number) which is still empty.
			while (moreArray[r][src.ordinal] != "" && r < maxRow - 1) {
				r += 1
			}

			moreArray[r][src.ordinal] = more
		}

		more = enumValues<Ability>().joinToString(
				prefix = "|",
				postfix = " |\n" + hlineStr + "\n",
				separator = " |",
				transform = {
					val save = it in pc.savingThrows
					val (p, s) = if (save) Pair("*", "S") else Pair(" ", "s")
					" ${p}(%+d) ${s}aving".format(pc.savingScore(it)) +
					" ".repeat(width - 12)
				})

		more += moreArray.joinToString(
			separator = "\n",
			transform = {
				row -> row.joinToString(
					prefix = "|", postfix = " |", separator = " |",
					transform = { " %${-width}s".format(it) })
			})

	}

	return "" +
		(if (hline || withSkill) hlineStr + "\n" else "") +
		header +
		(if (hline || withSkill) "\n" + hlineStr else "") + "\n" +
		values +
		(if (withSkill) "\n" + hlineStr + "\n" + more else "") +
		(if (hline || withSkill) "\n" + hlineStr else "")
}

fun playerAbilitiesVertically( pc: PlayerCharacter, withSkill: Boolean = false) : String {
	var str = ""
	for (a in enumValues<Ability>()) {
		var score = pc.abilityScore.getOrDefault(a, 10)
		var mod = pc.abilityModifier.getOrDefault(a, 0)
		var prof = mod + pc.proficientValue

		if (!withSkill) {
			str += "%2d (%+d) %s\n".format(score, mod, a.fullname)
			continue
		}

		var save = if (a in pc.savingThrows) {
			"*(%+d) SAVING".format(prof)
		} else {
			" (%+d) saving".format(mod)
		}

		str += "%2d (%+d) %s\n  %s".format(score, mod, a.fullname, save)

		if (a != Ability.CON) {
			str += enumValues<Skill>()
				.filter{it.source == a}
				.map {
					val name = it.name
					val lower = name.toLowerCase()
					val level = pc.getProficiencyFor(it)

					val (indicator, show) = when (level) {
						Proficiency.EXPERT     -> Pair("#", name)
						Proficiency.PROFICIENT -> Pair("*", lower.capitalize())
						Proficiency.NONE       -> Pair(" ", lower)
					}

					var bonus = mod + level.factor * pc.proficientValue
					"%s(%+d) %s".format(indicator, bonus, show)
				}.joinToString(prefix = "\n  ", separator = "\n  ")
		}
		str += "\n" + (".".repeat(25)) + "\n"
	}
	return str;
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

	println()

	println(playerAbilitiesHorizontally(pc, true))

	println()

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
}
