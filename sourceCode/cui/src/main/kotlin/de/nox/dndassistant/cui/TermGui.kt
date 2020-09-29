package de.nox.dndassistant.cui

import de.nox.dndassistant.core.*

private val logger = LoggerFactory.getLogger("TermGUI")

// TODO own package.
// w: Some JAR files in the classpath have the Kotlin Runtime library bundled into them. This may cause difficult to debug problems if there's a different version of the Kotlin Runtime library in the classpath. Consider removing these libraries from the classpath

fun main(args: Array<String>) {
	println(
		"""
		DnD Application, display stats and roll your dice!
		Happy Gaming! :D
		==================================================
		${args}
		""".trimIndent())

	val pc = playgroundWithOnyx()

	val display = PCDisplay(pc, "Nox")
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
		unfoldKlasses: Boolean = false,
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
		println(showKlasses(unfoldKlasses))
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
				val maxRow = low.values.map { it.size }.maxOrNull() ?: 0

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
				char.hitdice.keys.joinToString(", ", "[", "]") {
					"d${it}"
				},
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

			val maxRows = listOf(languages.size, proficient.size, expert.size).maxOrNull()!!

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
			val cur = "|(%.1f lb) [".format(char.carriedWeight)
			val cap = "] (%.1f lb)".format(char.carryingCapacity)
			val fix = (cur + cap).length // length of the side chars, which are already used.

			val full = width - fix
			val part = (full * char.carriedWeight / char.carryingCapacity).toInt()

			val bar = "$".repeat(part) + "-".repeat(full - part)

			// add the bar.
			content += "\n" + cur + bar + cap
			content += "\n|"

			// TODO (2020-07-18) Implement 'fun showInventory(unfold: Boolean = false) : String'

			// show equipped items.
			content += "\n|# Equipped"
			content += " (%.1f lb)".format(
				char.carriedWeightHands
				+ char.carriedWeightWorn
			)
			content += "\n|| * Hold: ${char.hands}"
			content += "\n|| * " + char.worn.toList().joinToString("|| * ") {
				"%${-width/3}s %s (%.1flb)\n".format(
					"${it.first}:",
					it.second.toString(),
					it.second.weight)
			}

			if (!char.bags.isEmpty()) {
				content += "\n|# Bags:"
				content += " (%.1f lb)".format(char.carriedWeightBags)
				content += char.bags.toList().joinToString(
					separator = "",
					transform = {
						printBag(it.first, it.second)
					}
				)
			}

			content += "\n"
		}
		val preview = " (%.1f lb, %s)".format(char.carriedWeight, "${char.purse}")
		return "# Inventory" + preview + content
	}

	private fun printBag(bagKey: String, bag: Container) : String {
		val note : String

		val weight = bag.sumWeight(true)
		var items = ""

		if (bag.isEmpty()) {
			note = "empty"
		} else if (bag.size < 2) {
			note = "{${bag.inside[0]}}"
		} else {
			note = if (bag.isFull()) "full" else bag.capacity
			bag.insideGrouped.forEach {
				items += "\n|| | %4d %c %${-width + 23}s %c %5.1f lb".format(
					it.value.size, // items count
					0xd7, // times symbol
					it.key, // items name
					0x3a3, // sum symbol
					it.value.sumByDouble {
						// summed weight
						if (it is Container) it.sumWeight(true) else it.weight
					}
				)
			}
		}

		var firstLine = "\n|| * ${bag.name} ($weight lb, $note)"
		firstLine = "$firstLine %${width - firstLine.length}s".format(bagKey)

		return "${firstLine}${items}"
	}

	/** Show the available attacks, preview: most damage attack. */
	fun showAttacks(unfold: Boolean = false) : String {
		val str = char.abilityModifier(Ability.STR)
		val dex = char.abilityModifier(Ability.DEX)

		var attacks: List<Attack> = listOf()

		/* Currently equipped weapon, if something equipped. */
		char.hands.toList().forEach {
			if (it != null && it is Weapon) {
				val titleNote
					= ("(held"
					+ (if (it.weightClass == WeightClass.LIGHT) ", light" else "")
					+ ")")

				val proficient
					= (char.proficiencies.contains(it)
					|| char.proficiencies.contains(it.weaponType))

				attacks += Attack(
					"${it.name} $titleNote",
					ranged = !it.weaponType.melee,
					damage = it.damage to it.damageType,
					note = it.note,
					finesse = it.isFinesse,
					proficientValue = if (proficient) char.proficientValue else 0,
					modifierStrDex = str to dex)
			}
		}

		/* Unarmed Attack: Proficient, with STR, if not said otherwise. */
		attacks += Attack("Unarmed",
			ranged = false,
			damage = DiceTerm(0) to setOf(DamageType.BLUDGEONING),
			note = "slap, hit, kick, push...",
			proficientValue = char.proficientValue, // always proficient.
			modifierStrDex = str to dex)

		/* Carried weapons in inventory. */
		attacks += char.bags.values.map { it.inside }.flatten().toSet()
			.filter { it is Weapon && it != char.hands.first && it != char.hands.second }
			// set of unique weapons, which are also not carried.
			.map { Attack(
				(it as Weapon).name,
				ranged = !it.weaponType.melee,
				damage = it.damage to it.damageType,
				note = it.note,
				finesse = it.isFinesse,
				proficientValue = if (char.proficiencies.contains(it) ||
				char.proficiencies.contains(it.weaponType)) char.proficientValue else 0,
				modifierStrDex = str to dex)
			}.sorted().reversed()

		/* Failed improvised weapon attacks. */
		attacks += Attack("Improvised",
			ranged = false,
			damage = DiceTerm(D4) to setOf(),
			note = "Hit with somehting unfitting",
			proficientValue = 0, // TODO (2020-07-19) proficient with improvised? => depends on intended weapon?
			modifierStrDex = str to dex)

		attacks += Attack("Improvised (thrown)",
			ranged = true,
			damage = DiceTerm(D4) to setOf(),
			note = "Throw a non-throwable weapon / item",
			proficientValue = 0, // TODO (2020-07-19) proficient with improvised? => depends on intended weapon?
			modifierStrDex = str to dex)

		// TODO (2020-08-08) add attacks with damaging spells

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
		val maxAtk = attacks.maxByOrNull { it.damageRoll.average }!!

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
			if (it.second < 0) "\u221E" /* infinity */ else "$it"
		})

		preview += "${(0..9).filter{ char.spellSlots[it].second > 0 }
			.joinToString(":", "left slots: [", "]") }"

		content += "\n|# Learnt Spells"
		content += char.spellsKnown.sortedWith(comparingSpells)
			.joinToString("", "", "\n") {
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
			}

		return "# Spells (${preview})" + if (unfold) content else ""
	}

	fun showKlasses(unfold: Boolean = false) : String {
		var content = ""

		// TODO (2020-07-18) Implement PlayerCharacter's classes.

		val classes = char.klasses

		if (unfold) {
			content += "\n"

			content += classes.toList().joinToString(
				prefix = "| # ",
				separator = "\n| # ",
				postfix = "\n",
				transform = {
					val (l, s) = it.second

					val features = it.first.getFeaturesAtLevel(l, s)

					("${it.first}, level ${l}${if (s == "") s else " "}$s"
					+ if (features.size < 1) "" else features.joinToString(
						separator = "\n| | * ",
						prefix    = "\n| | * ",
						transform = {
							("${it.title}"
							+ if (it.hasDescription) {
								"\n" + it.description.wrap("| |   ") + "\n| |"
							} else {
								""
							})
						}
					))
				}
			)

		}

		// preview of classes and levels.
		val preview = classes.toList().joinToString(
			prefix = " (", postfix = ")", transform = {
			"${it.first}:${it.second}"
		})
		return "# Classes" + preview + content
	}

	fun showRaces(unfold: Boolean = false) : String {
		var content = ""

		// TODO (2020-07-19) Implement PlayerCharacter's Races.

		val raceName = "${char.race.name}"
		val subRace = "${char.subrace}"
		val darkvision = when (char.race.darkvision) {
			0 -> ""
			else -> ", darkvision ${char.race.darkvision}"
		}

		if (unfold) {
			content += char.race.allFeatures(char.subrace).joinToString(
				"\n| * ", "\n| * ", "\n", transform = {
					it.name + when (it.hasDescription) {
						false -> ""
						else -> ":\n" + it.description.wrap("| | | ")
					}
				})
			content += "|\n"
			content += char.race.description.wrap("| ")
			content += "\n"
		}

		return "# Races (${raceName} (${subRace})${darkvision})" + content
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
			content += "| * %${-len}s %s\n".format("Background:", char.background.first)
			content += "| * %${-len}s %s\n".format("Alignment:", char.alignment)

			content += "| * Motives:\n"
			content += "| | %${-len}s %s\n".format("* Speciality:", char.background.second)
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

		val size = char.race.size.name.toLowerCase()
		val form = char.form
		val etc = char.appearance

		if (unfold) {
			content += "\n"

			val len = width / 4 - 2

			content += "| * %${-len}s %s\n".format("Height:", "${"%.2f".format(char.height * 30.5)} cm, ${size}")
			content += "| * %${-len}s %s\n".format("Weight:", "${char.weight} lb, ${char.form} ")
			content += "| * %${-len}s %s\n".format("More:", "$etc, ???")

			// TODO (2020-07-18) Add (ASCII) picture?
		}

		val preview = " (${size}, $form, $etc)" // show size/height/weight
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

	fun String.wrap(indent: String = "", alsoFirstIndent: Boolean = true) : String {
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

		return lines.joinToString(
			"\n${indent}",
			if (alsoFirstIndent) "${indent}" else "",
			"")
	}
}
