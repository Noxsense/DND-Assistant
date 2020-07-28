package de.nox.dndassistant

private val logger = LoggerFactory.getLogger("TermGUI")

fun main() {
	println(
		"""
		DnD Application, display stats and roll your dice!
		Happy Gaming! :D
		==================================================
		""".trimIndent())

	playgroundWithOnyx()
}

fun playgroundWithOnyx() {
	val pc : PlayerCharacter
		= PlayerCharacter("Onyx Necklace", race = "Gnome", player = "Nox")

	// pc.rollAbilityScores()

	pc.setAbilityScores(mapOf(
		Ability.STR to 6,
		Ability.DEX to 17,
		Ability.CON to 11,
		Ability.INT to 16,
		Ability.WIS to 15,
		Ability.CHA to 10
	))

	pc.addProficiency(Skill.SLEIGHT_OF_HAND) // proficient
	pc.addProficiency(Skill.STEALTH) // proficient
	pc.addProficiency(Skill.SLEIGHT_OF_HAND) // expert
	pc.addProficiency(Ability.DEX) // saving throw
	pc.addProficiency(Ability.INT) // saving throw

	logger.debug("Proficent Skills: " + pc.proficiencies)

	val dagger =  Weapon("Dagger", 1.0, Money(gp=2),
		weightClass = WeightClass.LIGHT,
		weaponType = Weapon.Type.SIMPLE_MELEE,
		damage = DiceTerm(D4),
		damageType = setOf(DamageType.PIERCING),
		throwable = true,
		isFinesse = true,
		note = "Finesse, light, thrown (range 20/60)")

	val spear =  Weapon("Spear", 3.0, Money(gp=1),
		weightClass = WeightClass.LIGHT,
		weaponType = Weapon.Type.SIMPLE_MELEE,
		damage = DiceTerm(D6),
		damageType = setOf(DamageType.PIERCING),
		throwable = true,
		// versatile: 1d8
		note = "Thrown (range 20/60) | versatile (1d8)")

	pc.pickupItem(spear)
	pc.pickupItem(dagger)

	logger.info("Onyx' inventory: ${pc.inventoryWeight} lb, ${pc.inventory}, ${pc.purse}")

	pc.wield(pc.inventory[0] as Weapon)

	logger.info("Onyx' inventory: ${pc.inventoryWeight} lb, ${pc.inventory}, ${pc.purse}")
	logger.info("Onyx' wields: ${pc.handMain}, ${pc.handOff}")

	pc.unwield(both = true)

	logger.info("Onyx' inventory: ${pc.inventoryWeight} lb, ${pc.inventory}, ${pc.purse}")
	logger.info("Onyx' wields: ${pc.handMain}, ${pc.handOff}")

	logger.info("Sell the dagger (${dagger})")
	pc.sellItem(dagger)

	logger.info("Onyx' inventory: ${pc.inventoryWeight} lb, ${pc.inventory}, ${pc.purse}")
	logger.info("Onyx' wields: ${pc.handMain}, ${pc.handOff}")

	logger.info("Buy the dagger (${dagger})")
	pc.buyItem(dagger)

	logger.info("Onyx' inventory: ${pc.inventoryWeight} lb, ${pc.inventory}, ${pc.purse}")
	logger.info("Onyx' wields: ${pc.handMain}, ${pc.handOff}")

	(1..60).forEach { pc.pickupItem(dagger) } // get +60 daggers => inventory +60lb
	// pc.pickupItem(Item("Crowbar", 5.0, Money()))

	val mageHand = Spell(
		"Mage Hand", // name
		"Conjuration", 0, // school, level
		"1 action", "30 feet", "V,S", 60, false, // casting time, range, components, duration, concentration
		ritual = false,
		note = """
		Vansishes over 30ft range, or re-cast;
		manipulate objects, open / unlock container, stow / retrive item, pour contents;
		cannot attack, cannot activate magic items, cannot carry more than 10 pounds
		""")

	val guidance = Spell(
		"Guidance", // name
		"Divination", // school
		0, // spell level
		"1 action", // casting time
		"Touch", // range
		"V,S", // components
		60, true, // duration, concentration
		ritual = false, // is ritual
		note = """
		1. Touch a willing creature.
		2. Roll d4, add to one ability check of choice (pre/post). End.
		""".trimIndent() // description
		)

	val spells: List<Spell> = listOf(
		  Spell("Spell 5", "Illusion",      5, "1 action", "5 feet", "V", 1,     false, true , "...")
		, Spell("Spell 1", "Conjuration",   1, "1 action", "Touch",  "V", 60,    false, false, "...")
		, mageHand
		, Spell("Spell 0", "Abjuration",    0, "1 action", "Touch",  "V", 60,    false, false, "...")
		, Spell("Spell 6", "Necromancy",    6, "1 action", "6 feet", "V", 1,     false, false, "...")
		, guidance
		, Spell("Spell 2", "Divination",    2, "1 action", "1 feet", "V", 1,     false, false, "...")
		, Spell("Spell 4", "Evocation",     4, "1 action", "4 feet", "V", 86400, false, true , "...")
		, Spell("Spell 7", "Transmutation", 7, "1 action", "7 feet", "V", 60,    false, false, "...")
		, Spell("Spell 3", "Enchantment",   3, "1 action", "3 feet", "V", 1,     false, false, "...")
		)

	spells.forEach { pc.learnSpell(it, "Wizard") }

	pc.castSpell(spells.find{ it == guidance }!!)
	pc.prepareSpell(spells.find{ it.name == "Spell 4" }!!)
	pc.castSpell(spells.find{ it.name == "Spell 4" }!!)
	pc.prepareSpell(spells.find{ it.name == "Spell 7" }!!)

	val display = PCDisplay(pc, "Nox")
	pc.maxHitPoints = 12
	pc.curHitPoints = 7

	pc.setBackground(Background(
		name = "Sage",
		proficiencies = listOf(Skill.ARCANA, Skill.HISTORY),
		equipment = listOf(/*bottle,ink,pen,small knife, letter,clothes*/),
		money = Money(gp=10)).apply {
		extraLanguages = 2
		},
		true)

	pc.speciality = "Libarian"
	pc.trait = "Watch and Learn."
	pc.ideal = "Knowledge."
	pc.bonds = "Protect the weak."
	pc.flaws = "Stupid, hurtful men."

	pc.history += "Born in a gnome village"
	pc.history += "Childhood with loving and caring gnome parents, both libarians"
	pc.history += "Still childhood, Parents left for own small adventure, but disappeared unplanned"
	pc.history += "Raised by village and friend's parent"
	pc.history += "Friend left for better lifestyle without being judged"
	pc.history += "Left to search friend and parents"
	pc.history += "Got abducted, sold into a brothel "
	pc.history += "Run away after learning how to fight back by mysterious elf woman"
	pc.history += "Became Assassin (Rogue)"

	println("\n".repeat(5))
	display.display()

	pc.conditions += Condition.UNCONSCIOUS
	pc.deathSavesSuccess()
	pc.deathSavesFail()
	pc.deathSavesFail()
	pc.deathSavesSuccess()
	pc.deathSavesSuccess()

	println("\n".repeat(5))
	display.display(true, true, true, true, true, true, true, true, true, true)
}

class PCDisplay(val char: PlayerCharacter, val player: String) {
	val width = 79

	/* Print the Display for the player character to the standard output.*/
	fun display(
		unfoldAttributes: Boolean = false,
		unfoldProficiencies: Boolean = false,
		unfoldInventory: Boolean = false,
		unfoldAttacks: Boolean = false,
		unfoldSpells: Boolean = false,
		unfoldClasses: Boolean = false,
		unfoldRaces: Boolean = false,
		unfoldBackground: Boolean = false,
		unfoldAppearance: Boolean = false,
		unfoldRollHistory: Boolean = false
		) {

		val level = 1 // depends on xp

		println(char.name)
		println("Lvl: $level, XP: ${char.expiriencePoints}, player: $player")

		println(showAttributes(unfoldAttributes))

		println()
		println(showHealthBar())
		println()

		println(showCombatStats())

		println(showProficiencies(unfoldProficiencies))
		println(showInventory(unfoldInventory))
		println(showAttacks(unfoldAttacks))
		println(showSpells(unfoldSpells))
		println(showClasses(unfoldClasses))
		println(showRaces(unfoldRaces))
		println(showBackground(unfoldBackground))
		println(showAppearance(unfoldAppearance))
		println(showRollHistory(unfoldRollHistory))
	}

	/** Show the big table of the attributes. */
	fun showAttributes(unfold: Boolean = false) : String {
		// for 3 columns.
		var col = 3
		var cwidth = (width - 1) / col
		var thline1 = ("+" + "-".repeat(cwidth - 1)).repeat(col) + "+"
		var thline2 = ("+" + "=".repeat(cwidth - 1)).repeat(col) + "+"

		val groupSkills = enumValues<Skill>().groupBy { it.source }

		// TODO (2020-07-18) less nesting!

		var str = thline2 // line =====
		val abs = enumValues<Ability>().toList()
		for (i in (0 until abs.size step col)) {
			val curabs = abs.subList(i, i + col)
			str += curabs.joinToString(
				separator = "",
				prefix = "\n|",
				postfix = "",
				transform = {
					" %-15s %+2d (%02d) |".format(
						it.fullname,
						char.abilityModifier(it),
						char.abilityScore(it))
				})

			if (unfold) {
				str += "\n" + thline1 // line ------
				val low = groupSkills.filterKeys { it in curabs }
				val maxRow = low.values.map { it.size }.max() ?: 0

				val cellForm = "| %+2d %c %-18s "
				// ability saving throws
				str += curabs.joinToString(
					separator = "",
					prefix = "\n",
					postfix = "|",
					transform = { cellForm.format(
						char.savingScore(it),
						char.getProficiencyFor(it).symbol,
						"Saving Throw"
					) })

				// ability skills
				for (r in (0 until maxRow)) {
					str += curabs.joinToString(
						separator = "",
						prefix = "\n",
						postfix = "|",
						transform = {
							var below = low.get(it) ?: listOf()
							if (r >= below.size) {
								"|" + " ".repeat(cwidth - 1)
							} else {
								var skill = below.get(r)
								cellForm.format(
									char.skillScore(skill),
									char.getProficiencyFor(skill).symbol,
									skill.fullname)
							}
					})
				}
			}
			str += "\n" + thline2 // line ======
		}

		return str
	}

	fun showCombatStats() : String {
		// TODO (2020-07-27) Combat stats.
		val len = width / 2 - 1 // len of a column.

		val colLine1 = ("+${"-".repeat(len)}").repeat(2) + "+\n"
		val colLine2 = ("+${"=".repeat(len)}").repeat(2) + "+\n"
		val outLine1 = ("+${"-".repeat(width - 2)}+\n")
		val outLine2 = ("+${"=".repeat(width - 2)}+\n")

		val format = (colLine2
			+ "| Armor Class: %${len - 15}s | Initiative:  %+${len - 15}d |\n" + colLine1
			+ "| Hit Dice:    %${len - 15}s | Death Saves:  %${len - 16}s |\n" + colLine1
			)

		return (format.format(
				char.armorClass,
				char.initiative,
				listOf(D8, D8, D8).joinToString(",", "[", "]", transform = {
					"d${it.faces}"
				}),
				char.deathSaves.toList().joinToString("", "", "",
					transform = {when (it) {
						-1 -> "\u2620" // failed (death head)
						0 -> ""
						else -> "\u2661" // success (white heart)
				}}))

			+ "| %${-width + 4}s |\n".format("Conditions:")
			+ char.conditions.joinToString("", "", "", transform = {
				"| %${-width + 4}s |\n".format("* $it")
			})
			+ outLine1

			+ "| %${-width + 4}s |\n".format("Speed:")
			+ char.speed.toList().joinToString("", "", "", transform = {
				"| %${-width + 4}s |\n".format("* ${it.second}ft (${it.first})")
			})
			+ outLine2)
	}

	/** Show health bar.*/
	fun showHealthBar() : String {
		// this bar displays, how much of the carrying weight capacity is already carried.
		val fix = 10 // length of the side chars, which are already used.
		val full = width - fix
		val part = full * char.curHitPoints / char.maxHitPoints
		val bar = "\u2665".repeat(part) + ".".repeat(full - part) // print hearts

		val label = " ${char.curHitPoints}/${char.maxHitPoints} hp" // eg. 7/12 hp

		val labelledBar = bar.substring(0, bar.length - label.length) + label // right aligned

		return "(-) [" + labelledBar + "] (+)"
	}

	/** Show the proficiencies and languages, preview: proficiency bonus.*/
	fun showProficiencies(unfold: Boolean = false) : String {
		var content = ""
		if (unfold) {
			val languages : List<String> = listOf()
			val (proficient, expert)
				= char.proficiencies.keys.partition {
					char.proficiencies[it] == Proficiency.PROFICIENT
				}

			val maxRows = listOf(languages.size, proficient.size, expert.size).max()!!

			val len = -width / 3 + 1 + 2
			val trFormat = "|%${len}s |%${len}s |%${len}s"

			content += "\n" + trFormat.format("LANGUAGE", "PROFICIENT", "EXPERT").trim()

			(0 until maxRows).forEach{
				content += "\n" + trFormat.format(
					if (it < languages.size) " * ${languages[it]}" else "",
					if (it < proficient.size) " * ${proficient[it]}" else "",
					if (it < expert.size) " * ${expert[it]}" else "").trim()
			}

			content += "\n"
		}
		val preview = "(%+d)".format(char.proficientValue)
		return "# Proficiencies & Language " + preview + content
	}

	/** Show the inventory, preview: weight and money.*/
	fun showInventory(unfold: Boolean = false) : String {
		var content = ""
		if (unfold) {
			// this bar displays, how much of the carrying weight capacity is already carried.
			val cur = "|(%.1f lb) [".format(char.inventoryWeight)
			val cap = "] (%.1f lb)".format(char.carryingCapacity)
			val fix = (cur + cap).length // length of the side chars, which are already used.

			val full = width - fix
			val part = (full * char.inventoryWeight / char.carryingCapacity).toInt()

			val bar = "$".repeat(part) + "-".repeat(full - part)

			// add the bar.
			content += "\n" + cur + bar + cap
			content += "\n|"

			// TODO (2020-07-18) Implement 'fun showInventory(unfold: Boolean = false) : String'

			// show equipped items.
			content += "\n|# Equipped"
			content += "\n||"

			// show other items.
			content += "\n|# Bag"
			char.inventory.groupBy { it.name }.forEach {
				// add item, count and weight to list.
				content += "\n|| * %s (%dx, %.1f lb)".format(
					it.key,
					it.value.size,
					it.value.sumByDouble { it.weight }
				)
			}

			content += "\n"
		}
		val preview = " (%.1f lb, %s)".format(char.inventoryWeight, "${char.purse}")
		return "# Inventory" + preview + content
	}

	/** Show the available attacks, preview: most damage attack. */
	fun showAttacks(unfold: Boolean = false) : String {
		val str = char.abilityModifier(Ability.STR)
		val dex = char.abilityModifier(Ability.DEX)

		data class Attack(
			val name: String,
			val proficient: Boolean,
			val ranged: Boolean,
			val damageRollPure: DiceTerm,
			val damageType: Set<DamageType>,
			val note: String = "",
			val finesse: Boolean = false)
			: Comparable<Attack> {

			val bonus: Int = when {
					finesse -> Math.max(str, dex)
					ranged -> dex
					else -> str
				}

			val attackBonus: Int
				= bonus +  (if (proficient) char.proficientValue else 0)

			//  TODO (2020-07-19) How to set the damage roll.
			val damageRoll: DiceTerm
				= (damageRollPure + Bonus(bonus)).contracted()

			override fun compareTo(other: Attack) : Int
				= (damageRoll.average - other.damageRoll.average).toInt()
		}

		var attacks: List<Attack> = listOf()

		/* Unarmed Attack: Proficient, with STR, if not said otherwise. */
		attacks += Attack("Unarmed", true, false,
			DiceTerm(0), setOf(DamageType.BLUDGEONING),
			"slap, hit, kick, push...")

		/* Currently equipped weapon, if something equipped. */
		attacks += Attack("Equipped / TODO (20200719)",
			true,
			true,
			DiceTerm(2), setOf())

		/* Carried weapons in inventory. */
		attacks += char.inventory.toSet()
			.filter { it is Weapon }
			.map { Attack(
				it.name,
				char.proficiencies.contains(it as Weapon) ||
				char.proficiencies.contains(it.weaponType),
				!it.weaponType.melee,
				it.damage, it.damageType,
				it.note,
				it.isFinesse)
			}.sorted().reversed()

		/* Failed improvised weapon attacks. */
		// TODO (2020-07-19) proficient with improvised?
		attacks += Attack("Improvised", false, false, DiceTerm(D4), setOf(), "Hit with somehting unfitting")
		attacks += Attack("Improvised (thrown)", false, true, DiceTerm(D4), setOf(), "Throw a non-throwable weapon / item")

		val trFormat
			= ("| %${-width / 3}s |"
			+ " %+3d | \u00f8%4.1f:  %-12s %${width * 2 / 3 -4 -5 -13 -11}s |"
			+ " %s")

		val trHline
			= trFormat.format("", 0, 0.0, "", "", "").trim()
			.map { if (it == '|') '+' else '-' }.joinToString("")

		val content = attacks.joinToString(
			"\n", "\n" + trHline + "\n", "\n" + trHline + "\n",
			transform = { trFormat.format(
				it.name,
				it.attackBonus,
				it.damageRoll.average,
				it.damageRoll,
				if (it.damageType.size < 1) "???" else it.damageType.joinToString(),
				it.note
			).trim()
		})

		// Preview: Maximal Attack.
		// it must be at least size:3, since they are put manually.
		val maxAtk = attacks.maxBy { it.damageRoll.average }!!

		return ("# Attacks (max: ${maxAtk.name}:${maxAtk.damageRoll})"
			+ if (unfold) content else "" )
	}

	/** Compare a spell depending on the player character.
	 * If they are activated, put spells with concentration first.
	 * Then by left duration (short to long).
	 * If not activated, put prepared first.
	 * If also no spell is prepared, sort by spell level.
	 */
	private inner class ComparatorSpell : Comparator<Spell> {
		override fun compare(a: Spell, b: Spell) : Int {
			if (a == b) return 0 // same spell.

			/* If any spell is active, check if a or b can be top. */
			if (char.spellsActive.size > 0) {
				val activeA = char.spellsActive.getOrDefault(a, 0)
				val activeB = char.spellsActive.getOrDefault(b, 0)

				/* If a or be hold concentration, put that spell before the other. */
				when {
					a.concentration -> return -1 // sort reversed.
					b.concentration -> return 1 // sort reversed.
				}

				/* If a spell is active, put it on top. */
				if (activeA > 0 || activeB > 0) {
					return -activeA.compareTo(activeB) // sort reversed.
				}
			}

			/* If any spell is prepared, put also top of rest. */
			if (char.spellsPrepared.size > 0) {
				val preparedA = char.spellsPrepared.getOrDefault(a, -1)
				val preparedB = char.spellsPrepared.getOrDefault(b, -1)

				/* If a or b are prepared, return their comparisson. */
				when {
					preparedA > -1 && preparedB > -1 -> {
						return preparedA.compareTo(preparedB)
					}
					preparedA > -1 -> return -1
					preparedB > -1 -> return 1
				}
			}

			return a.compareTo(b) // default comparisson
		}
	}

	private val comparingSpells = ComparatorSpell()

	/** Show the available spells. */
	fun showSpells(unfold: Boolean = false) : String {
		var content: String = ""
		var preview: String = ""

		/* Spell slots. */
		content += "\n|# Spell slots"
		content += "\n|| " + char.spellSlots.joinToString(" | ", "[ ", " ]", transform = {
			if (it < 0) "\u221E" /* infinity */ else "$it"
		})

		preview += "${(0..9).filter{ char.spellSlots[it] > 0 }
			.joinToString(":", "left slots: [", "]") }"

		content += "\n|# Learnt Spells"
		content += char.spellsKnown.sortedWith(comparingSpells)
			.joinToString("", "", "\n", transform = {
				val prepSlot = char.spellsPrepared.getOrDefault(it, -1)
				val activeLeft = char.spellsActive.getOrDefault(it, -1)

				val prep = if (prepSlot < it.level) "" else "${prepSlot} \u21d0 "

				val duration = when {
					activeLeft < 1 -> "."
					it.concentration ->
						"Concentration for ${activeLeft} second!".also { _ ->
							preview += ", *${it.name}" // also add spell to preview.
					}
					else ->
						"Active for ${activeLeft} second.".also { _ ->
							preview += ", ${it.name}" // also add spell to preview
					}
				}

				val leftSide = "${prep}${it}"

				("\n| * $leftSide %${width - leftSide.length - 5}s".format(
					duration
				))
			})

		return "# Spells (${preview})" + if (unfold) content else ""
	}

	fun showClasses(unfold: Boolean = false) : String {
		var content = ""

		// TODO (2020-07-18) Implement PlayerCharacter's classes.

		val classes = listOf(
			Triple("Class1: Archetype", 1, listOf("can-do1", "can-do2", "can-do3")),
			Triple("Class2: Archetype", 1, listOf("can-do1", "can-do2"))
			)

		if (unfold) {
			content += "\n"

			content += classes.joinToString(
				prefix = "| # ",
				separator = "\n| # ",
				postfix = "\n",
				transform = {
					("${it.first}, level ${it.second}"
					+ it.third.joinToString(
						prefix    = "\n| | * ",
						separator = "\n| | * "
					))
				}
			)
		}

		// preview of classes and levels.
		val preview = classes.joinToString(
			prefix = " (", postfix = ")", transform = {
			"${it.first}:${it.second}"
		})
		return "# Classes" + preview + content
	}

	fun showRaces(unfold: Boolean = false) : String {
		var content = ""

		// TODO (2020-07-19) Implement PlayerCharacter's Races.

		val raceName = "???"
		val subRace = "???"

		if (unfold) {
			content += "\n| " + "{??? Race notes ???}" + "\n"
		}

		return "# Races ($raceName, $subRace)" + content
	}

	fun showBackground(unfold: Boolean = false) : String {
		var content = ""

		// TODO (2020-07-18) Implement PlayerCharacter's Background and Aligmnent.
		val age = when {
			char.age < 0 -> "${-char.age} days"
			else -> "${char.age} yrs"
		}

		if (unfold) {
			content += "\n"

			val len = width / 4 - 2

			content += "| * %${-len}s %s\n".format("Age:", age)
			content += "| * %${-len}s %s\n".format("Background:", char.background)
			content += "| * %${-len}s %s\n".format("Alignment:", char.alignment)

			content += "| * Motives:\n"
			content += "| | %${-len}s %s\n".format("* Speciality:", char.speciality)
			content += "| | %${-len}s %s\n".format("* Trait:", char.trait)
			content += "| | %${-len}s %s\n".format("* Ideal:", char.ideal)
			content += "| | %${-len}s %s\n".format("* Bonds:", char.bonds)
            content += "| | %${-len}s %s\n".format("* Flaws:", char.flaws)

			content += ("| * Story:"
				+ char.history.joinToString("\n| | * ","\n| | * ", "\n"))
		}

		// show background name and alignment.
		val preview = " (${age} (${char.alignment.abbreviation}), ${char.background})"
		return "# Background" + preview + content
	}

	fun showAppearance(unfold: Boolean = false) : String {
		var content = ""

		// TODO (2020-07-18) Implement PlayerCharacter's appearance.

		val size = "tiny|small|medium|large|giant"
		val form = "slim|bulky|defined ..."
		val etc = "pretty"

		if (unfold) {
			content += "\n"

			val len = width / 4 - 2

			content += "| * %${-len}s %s\n".format("Height:", "?\" / ? cm, ${size}")
			content += "| * %${-len}s %s\n".format("Weight:", "90lb / ?kg, ${form}")
			content += "| * %${-len}s %s\n".format("More:", "$etc, ???")

			// TODO (2020-07-18) Add (ASCII) picture?
		}

		val preview = " ($size, $form, $etc)" // show size/height/weight
		return "# Appearance" + preview + content
	}

	fun showRollHistory(unfold: Boolean = false) : String {
		return "# RollHistory" + if (unfold) {
			"\n|"
			// TODO (2020-07-18) Roll history .... ?
		} else {
			""
		}
	}

	fun String.wrap(indent: String = "") : String {
		var lines : List<String> = listOf() // this.chunked(len), on char.
		var line = ""

		val len = width - indent.length

		this.forEach {
			if (line.length >= len && it == ' ' ) {
				lines += line
				line = ""
			} else {
				line += it
			}
		}

		if (line != "") {
			lines += line
		}

		return lines.joinToString("\n${indent}", "${indent}", "")
	}
}
