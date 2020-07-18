package de.nox.dndassistant

private val logger = LoggerFactory.getLogger("TermGUI")

fun main() {
	println(
		"DnD Application, display stats and roll your dice!\n"
		+ "Happy Gaming! :D\n"
		+ "==================================================\n");

	val pc : PlayerCharacter
		= PlayerCharacter("Onyx Necklace", race = "Gnome", player = "Nox")

	// pc.rollAbilityScores()
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
		weightClass = WeightClass.LIGHT,
		weaponType = Weapon.Type.SIMPLE_MELEE,
		damageType = arrayOf(DamageType.PIERCING),
		damage = DiceTerm(D4),
		throwable = true,
		isFinesse = true,
		note = "Finesse, light, thrown (range 20/60)")

	pc.pickupItem(weapon)

	logger.info("Onyx' inventory: ${pc.inventoryWeight} lb, ${pc.inventory}, ${pc.purse}")

	pc.wield(pc.inventory[0] as Weapon)

	logger.info("Onyx' inventory: ${pc.inventoryWeight} lb, ${pc.inventory}, ${pc.purse}")
	logger.info("Onyx' wields: ${pc.handMain}, ${pc.handOff}")

	pc.unwield(both = true)

	logger.info("Onyx' inventory: ${pc.inventoryWeight} lb, ${pc.inventory}, ${pc.purse}")
	logger.info("Onyx' wields: ${pc.handMain}, ${pc.handOff}")

	logger.info("Sell the weapon (${weapon})")
	pc.sellItem(weapon)

	logger.info("Onyx' inventory: ${pc.inventoryWeight} lb, ${pc.inventory}, ${pc.purse}")
	logger.info("Onyx' wields: ${pc.handMain}, ${pc.handOff}")

	logger.info("Buy the weapon (${weapon})")
	pc.buyItem(weapon)

	logger.info("Onyx' inventory: ${pc.inventoryWeight} lb, ${pc.inventory}, ${pc.purse}")
	logger.info("Onyx' wields: ${pc.handMain}, ${pc.handOff}")
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
