package de.nox.dndassistant.cui

import kotlin.math.ceil
import kotlin.math.log

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import de.nox.dndassistant.core.*

/** Extending method: Hero to pretty Markdown (text based human readable format).*/
public fun Hero.toMarkdown() : String
	= this.run {
		val indent = "\t\t" // to trim off and everything is equally long.

		"""
		# ${name}

		- *Generated with D&amp;D Assistant: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss"))}*
		- Player: ${player}
		- Inspiration: ${(1..inspiration).map { "!" }}
		<br/>

		- Level: ${level}
		- Experience: $experience
		- Race: ${race}
		- Classes: ${klasses.toList().joinToString("\n${indent}\t* ", "\n${indent}\t* "){(klass, subklass, level) -> "$klass ($subklass), $level" }}

		## Conditions:
		- `${hitpoints.prettyBar(filledChar = 'x', emptyChar = '.', leftLabel = "HP:", printNums = true)}`

		### Death Saves:
		- $deathsaves

		### Hit Dice:
		- ${hitdice.diceList().joinToString(" + ")} (of maximal ${hitdiceMax.diceList().joinToString(" + ")})

		### Buffs and Effects:
		- <span style='background:red'>TODO</span>
		- being near paladin: +2 saving throws
		- being bewitched with Bless: 1d4 on saving throws and co.
		- being bewitched with Bane: -1d4 on saving throws
		- being bewitched with Mage Armor and naked: 13 + DEX
		- being bewitched by Charm: Doing stuff as they like
		- being bewitched by Confusing: Doing random stuff
		- exhaustion points: 1

		${prettyTable(listOf(
			Ability.values().map { "%${80 / (Ability.values().size + 2) }s".format(it.name) },  // header => stretch.
			Ability.values().map { abilities[it]?.let { (score, save) -> "%c%2d".format(if (save) '!' else ' ', score)} },  // scores and saves
			Ability.values().map { "%+d".format(abilities[it].getModifier()) },  // modifiers
		))
		.split("\n").joinToString("\n${indent}") // WORKAROUND: fixing the hurt indentation
		}

		## Proficiencies:
		- ${"%+d".format(proficiencyBonus)}

		### Languages:
		${languages.joinToString("\n${indent}* ","* ")}

		### Skills:
		${skillValues.toList().joinToString("\n${indent}* ","* ") { (s,v) ->
			val proficient = skills[s]?.first

			val str = "`%+d` - %s".format(v, s.name)

			// print plain text or bold with proficiency
			if (proficient == null) str else "**${str} ($proficient)**"
		}}

		### Tools:
		${tools.toList().joinToString("\n${indent}* ","* "){ (tool, reason) ->
			val (toolName, toolCategory) = tool
			val (proficient, _) = reason
			(
			// what
			when {
				toolName.length < 1 -> "Any Tool of Type \"$toolCategory\""
				else -> toolName
			}
			// optional proficiency
			+ when (proficient) {
				SimpleProficiency.P -> ""
				else -> " (Expert)"
			})
		}}

		## Actions:

		### Specialities:
		* <span style='background:red'>TODO</span>
			* What is a not Speciality, what is a speciality ?
			* Darkvision (always, not used up)
			* Ki Points (rechargable)
				- Use 'Second Wind' or so
			* Metamagic, with Sorcery Points as Resource (rechargable every long rest)
				* Used by Equipped Speciality: 'Heightened Metamagic' or others etc
			* Spellcaster, with various Spell Slots as Resource (rechargable every long rest)
				* used by equipped spells like 'Make Armor'? But also Replacable.
			* Consumed Potion
				* mega strong?
			* Equipped Weapon / Item
				* Cast 'Sunburst' with Shield (eg. with Ring of Spell Storing)
		${specialities.joinToString("\n${indent}* ", "* ")}

		### Attacks:
		* <span style='background:red'>TODO</span>
		* physical attacks
		* spells
		* spell DCs, spell (attack) modifiers, spell level

		### Spells:
		* Spells sorted by level?
		* Also show preparable spells (for later planning?)
		* <span style='background:red'>TODO</span>
		* Arcane Focus: ${getArcaneFocus()?.name}
		* Spells Prepared: ${spellsPrepared.size} items
		* ${spells}

		## Inventory:
		`${(inventory.weight() to maxCarriageWeight()).prettyBar(filledChar = 'x', emptyChar = '.', leftLabel = "Capacity: ", printNums = true, rightLabel = "lb")}`
		- summed items: ${inventory.size}
		- summed value: ${inventory.copperValue() * 1.0 / SimpleItem.GP_TO_CP} gp
		- ${inventory.printNested().split("\n").joinToString("\n${indent}")}

		""".replaceIndent(" ") // new margin
		.replace("\t", "  ")
		.replace(Regex("\\s+\\n"), "\n\n").trimEnd() // remove trailing space each line.
	}

/** Pretty print / string a bar for a number tuple, read as {now, max}.
 * @param filledChar the Char that represents the filled bar.
 * @param emptyChar the Char that represents the empty bar.
 * @param length intended lenght of the whole bar, label(s) inclusive.
 * @param leftLabel if given, print this label to the left side (with space).
 * @param printNums if true, add the label "now / max" to the bar (on the right side)
 * @param leftLabel if given, print this label to the right side (with space and after now/max if there).
 */
private fun Pair<Number, Number>.prettyBar(filledChar: Char = '#', emptyChar: Char = '-', length: Int = 80, leftLabel: String? = null, printNums: Boolean = false, rightLabel: String? = null) : String
	= this.let { (now, max) ->
		val maxCharLen: Int = ceil(log(max.toDouble(), 10.0)).toInt() // lenght of the maximal value
		val numLabel: String = when (max) {
			is Int -> " %${maxCharLen}d / %d"  // as integer
			is Double -> " %${maxCharLen}.1f / %${maxCharLen}.1f"  // as floating point
			else -> " %${maxCharLen}s / %${maxCharLen}s"  // as strings
		}.format(now, max) // format

		// reduce number, and left and right labels, if given.
		// if the labels are multilined, use the last part of the left and the first part of the right label
		val maxLen: Int = (
			length
			- (leftLabel?.split("\n")?.let { it.get(it.size - 1).length.plus(1) } ?: 0) // reduce by last line of right label
			- (if (printNums) numLabel.length else 0)
			- (rightLabel?.split("\n")?.let { it.get(0).length.plus(1) } ?: 0) // reduce by first line of the right label
			)

		val part: Int = (now.toInt() * maxLen / max.toInt()).toInt()

		((leftLabel?.toString()?.plus(" ") ?: "")
		+ when {
			part < 0 -> "$emptyChar".repeat(maxLen)
			part < maxLen -> "$filledChar".repeat(part) + "$emptyChar".repeat(maxLen - part)
			else -> "$filledChar".repeat(maxLen)
		}
		+ (if (printNums) numLabel else "")
		+ (rightLabel?.toString()?.insert(0, " ") ?: "")
		)
	}

/** Pretty print a table, given by List (rows) of List (colums).
 * Each row has at least the max number of columns.
 * Each cell has the same width.
 * Nulll or not filled will be displayed as empty string.
 * @param cells List of List  =>  Rows, containing columns with Any Content.
 * @param extraLines row indices to insert a line BEFORE.
 * @param replaceStrongLines if there are two table lines under each other, replace it with a |==|==| line.
 */
private fun prettyTable(cells: List<List<Any?>>, extraLines: List<Int>? = null, replaceStrongLines: Boolean = true) : String {
	if (cells.size < 1) return ""

	// maxium of columns for each row.
	val maxCol = cells.map { it.size } .maxOrNull() ?: 1

	// maximum string lenght of a cell.
	val cellMaxLen = cells.flatMap { it }.map { it?.toString()?.length ?: 1 }. maxOrNull() ?: 1

	val printRow: ((List<Any?>, Char) -> String)
	printRow = { row, sides ->
		(row + (row.size until maxCol).map { "" }).joinToString("|", "|", "|") { col ->
			"${sides}%${cellMaxLen}s${sides}".format(col?.toString() ?: "")
		}
	}

	// a table line.
	val tableLine = printRow((1 .. maxCol).map { "-".repeat(cellMaxLen) }, '-')

	val headerString = printRow(cells[0], ' ')

	// generate pretty table rows (each as string)
	var tableRows: List<String> = when (cells.size) {
		1 -> listOf(headerString) // only header
		else -> listOf(
			headerString,
			tableLine,
		) + cells.subList(1, cells.size).map { row -> printRow(row, ' ') }
	}

	// insert additional rows.
	if (extraLines != null) {
		extraLines.sorted().filter { it != 2 }.reversed().forEach { index ->
			tableRows = tableRows.insert(index, listOf(tableLine))
		}
	}

	var table = tableRows.joinToString("\n")

	// replace double "|---|---|\n|---|---|" table lines with |===|===|
	if (replaceStrongLines) {
		table = table.replace(tableLine + "\n" + tableLine, tableLine.replace("-", "="))
	}

	return table
}

/** Print a pretty / nested string of a simple item collection. */
public fun Collection<Pair<SimpleItem, String>>.printNested() : String {
	// group items by storage place.
	val bagsWithContent = this.groupBy { it.second }

	if (bagsWithContent.size < 1 || !bagsWithContent.containsKey("")) {
		return "No Items held or worn, therefore no bags are carried as well."
	}

	val directlyCarried = bagsWithContent[""]!!

	/* Pretty print:
	 * - nested
	 * - sum up same objects (if they don't carry anything)
	 */
	lateinit var printNestedBags: ((List<Pair<SimpleItem, String>>, Int) -> String)
	printNestedBags = { items, level ->
		val (bagsPre, nobagsPre) = items.partition {
			(i,_) -> i.identifier in bagsWithContent.keys
		}

		// indentation of each nesting level
		val itemSep = "\n" + "\t".repeat(level)

		// sum up same objects, not containing anything.
		val nobags = nobagsPre.groupBy { (i, _) -> i.name }.toList()
			.joinToString(itemSep, itemSep) { (name, allSame) ->
				"- *(${allSame.size}x)* **$name**"
			}

		// print nested bags.
		val bags = bagsPre
			.joinToString(itemSep, itemSep) { (it, _) ->
				(bagsWithContent[it.identifier]!!.let { nestedBag ->
					("+ **${it.name}**"
					// inside number, carried weigh, own weight
					+ " *(carries: %d items, %.1f  (+ %.1f) lb)*".format(nestedBag.size, nestedBag.weight(), it.weight)
					+ printNestedBags(nestedBag, level + 1))
				})
			}

		((if (nobagsPre.size > 0) nobags else "") // show summed items
		+ (if (bagsPre.size > 0) bags else "") // show carrying items
		)
	}

	return ("**Worn / Held**"
		+ " *(%d items, %.1f lb)*".format(directlyCarried.size, directlyCarried.weight())
		+ printNestedBags(directlyCarried, 1)
		)
}

typealias Die = String

/** Pretty print a map of dice and their counts. */
private fun Map<Die, Int>.diceList() : List<String>
	= this.toList().map { (die, cnt) -> "${cnt}${die}" }

/** Insert a string into another String on a given position. */
private fun String.insert(index: Int, value: String) : String
	= StringBuilder(this).insert(index, value).toString()

/** Insert a Sublist into another List on a given position. */
private fun <T> List<T>.insert(index: Int, value: List<T>) : List<T>
	= this.subList(0, index) + value + this.subList(index, this.size)

