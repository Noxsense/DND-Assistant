package de.nox.dndassistant.cui

import java.io.File
import de.nox.dndassistant.core.*

private val logger = LoggerFactory.getLogger("HtmlGui")

// TODO own package.
// w: Some JAR files in the classpath have the Kotlin Runtime library bundled into them. This may cause difficult to debug problems if there's a different version of the Kotlin Runtime library in the classpath. Consider removing these libraries from the classpath

fun main() {
	println(
		"""
		DnD Application, display stats and roll your dice!
		Happy Gaming! :D
		HTML!
		==================================================
		""".trimIndent())

	val pc = playgroundWithOnyx()

	val display = HtmlPlayerDisplay(pc, "Nox")
	display.display()
}

fun feetToMeter(feet: Double) : Double
	= feet * 0.3048

class HtmlPlayerDisplay(val char: PlayerCharacter, val player: String) {
	/* Print the Display for the player character to the standard output.*/
	fun display() {
		val style = """
		<style>
		html, body {
		background: white;
		color: black;
		height: 90%;
		font-size: 1.5vw;
		}
		.smaller {
		font-size: smaller;
		}
		.larger {
		font-size: larger;
		}
		.center {
		text-align: center;
		}
		.right {
		text-align: right;
		}
		.flex-container {
		display: flex;
		flex-wrap: wrap;
		margin: 0px;
		padding: 0px;
		justify-content: space-between;
		}
		.flex-container > div {
		margin: 0px;
		padding: 0px;
		}
		.overview-panel {
		margin: 0px;
		overflow-x: hidden;
		width: 49%;
		overflow-y: auto;
		height: 40vw;
		}
		#second-view {
		font-size: 120%;
		}
		/*@media screen and (max-device-width: 600px) { */
		@media screen and (max-aspect-ratio: 3/2) {
		body {
		font-size: 3vw;
		}
		.overview-panel {
		width: 100%;
		height: auto;
		}
		}
		table {
		width: 100%;
		table-layout: fixed;
		}
		tr,td {
		vertical-align:top;
		text-align: left;
		border:1px solid #ddd;
		}
		ol {
		padding: 0% 0% 0% 6%;
		margin: 0%;
		}
		ul {
		padding: 0% 0% 0% 6%;
		margin: 0%;
		}
		.no-bullets {
		list-style-type: none;
		}
		.value {
		text-align:center;
		padding:5px;
		}
		.progress {
		width:100%;
		background:#dddddd55;
		height: 3vw;
		}
		.progress-value {
		width:60%;
		background: lime;
		border:1px solid #366600;
		height: 90%;
		padding: 1px;
		margin:0%;
		}
		.progress-bar td {
		text-align: center;
		vertical-align: middle;
		border: none;
		}
		.progress-bar td:nth-child(2) {
		width:90%;
		}
		.preview {
		opacity: 0.5;
		font-style: italic;
		font-weight: normal;
		font-size: 70%;
		}
		.bordered {
		border:1px solid #44444455;
		padding: 2px;
		}
		/* https://www.w3schools.com/howto/howto_js_collapsible.asp */
		/* Style the button that is used to open and close the collapsible content */
		.collapsible {
		cursor: pointer;
		text-align: left;
		width: 100%;
		border: none;
		}
		.bold {
		font-weight:bold;
		}
		/* Add a background color to the button if it is clicked on (add the .active class with JS), and when you move the mouse over it (hover) */
		.active, .collapsible:hover {
		background-color: #aaaaaa55;
		}
		/* Style the collapsible content. Note: hidden by default */
		.content {
		padding-left: 2%;
		padding-bottom: 5%;
		display: none;
		font-size: 80%;
		width: 90%;
		overflow: hidden;
		}
		</style>
		<style title="pretty-specific">
		#dice td {
		vertical-align: middle;
		text-align: center;
		color: gray;
		}
		#inventory-bar td:nth-child(2) {
		width: 80%;
		}
		#inventory-bar .progress-value {
		background: gold;
		border-color: #cc9900;
		}
		#inventory .bag .content {
		style: border:1px solid #55555533;
		padding: 2px;
		}
		#attributes td {
		font-size: smaller;
		white-space: nowrap;
		}
		#attributes ul {
		font-size: larger;
		padding-top:1vw;
		margin:0px;
		}
		#attributes li {
		font-size: 67%;
		white-space: nowrap;
		}
		#attributes .proficient {
		background: #77777755;
		font-weight: bold;
		}
		#attributes .expert {
		background: #00000055;
		font-weight: bold;
		}
		#attacks table {
		table-layout: auto;
		}
		#attacks .name {
		width: 40%;
		}
		#spells #spell-slots td {
		text-align:center;
		}
		#spells .spells-updater td {
		text-align:center;
		font-size: smaller;
		}
		#spells .spell-prepared-slot {
		width: 8%;
		text-align:center;
		font-size:xx-small;
		color: gray;
		}
		#spells .spell-active-duration {
		width: 10%;
		text-align:rightt;
		font-weight:100;
		font-size:xx-small;
		color: gray;
		}
		#spells .spell .activated { font-weight: bold; }
		#spells .spell .concentration { text-decoration: underline; }
		</style>
		"""

		val htmlHorror = """
		<!DOCTYPE html>
		<html lang="en">
		<head>
		<meta charset="utf-8">
		<title>D&amp;D Assitant</title>
		<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
		${style}
		</head>
		<!-- //////////////////////////////////////////////////////////////////// -->
		<body>
		<div id="player-character">
		<div id="playercharacter-name" class="bold" style="font-size:200%;">
		${char.name}
		</div>
		<div class="flex-container">
		<div id="statistics" class="overview-panel">
		<div id="level-expirience">
		<div class="preview">${char.level} level, ${char.expiriencePoints} XP</div>
		</div>
		<div id="attributes" class="larger">${showAttributes()}</div>
		<div id="hitpoints-bar">
		<div>${showHealthBar()}</div>
		</div>
		${showCombatStats()}
		<div id="rolls">
		<br/>
		<b>Extra Dice</b>
		<table id="dice" class="bold">
		<tr>
		<td class="roller" id="d2"  rollMax="2">D2</td>
		<td class="roller" id="d3"  rollMax="3">D3</td>
		<td class="roller" id="d4"  rollMax="4">D4</td>
		<td class="roller" id="d6"  rollMax="6">D6</td>
		<td class="roller" id="d8"  rollMax="8">D8</td>
		<td class="roller" id="d10" rollMax="10">D10</td>
		<td class="roller" id="d12" rollMax="12">D12</td>
		<td class="roller" id="d20" rollMax="20">D20</td>
		<td><input id="bonus" type="number" size="2"/></td>
		</tr>
		</table>
		</div>
		</div>
		<div id="second-view" class="overview-panel" style="padding-top:1vw;">
		<ol>
		<li id="proficiency">${showProficiencies()}</li>
		<li id="attacks">${showAttacks()}</li>
		<li id="spells">${showSpells()}</li>
		<li id="inventory">${showInventory()}</li>
		<li id="classes">${showKlasses()}</li>
		<li id="races">${showRaces()}</li>
		<li id="background-motives-alignment">${showBackground()}</li>
		<li id="appearance">${showAppearance()} </li>
		<li id="late rolls">
		<div class="collapsible"><b>Roll History</b> <span id="roll-preview" class="preview">(latest roll)</span> </div>
		<div class="content"><ul id="roll-history"><li>No entries</li></ul></div>
		</li>
		</ol>
		</div>
		</div>
		</div>
		<!-- //////////////////////////////////////////////////////////////////// -->
		<!-- Scripts. -->
		<script type="text/javascript">
			var i;
			/* Add EventListener to "collapsible". */
			var coll = document.getElementsByClassName("collapsible");
			for (i = 0; i < coll.length; i++) {
				coll[i].addEventListener("click", function() {
					this.classList.toggle("active");
					var content = this.nextElementSibling;
					if (!content.classList.contains("content")) {
						return
					} else if (content.style.display === "block") {
						content.style.display = "none";
					} else {
						content.style.display = "block";
					}
				});
			}
			/* ------------------------------------------------------------- */
			/* reopen the attributes. */
			var as = document.getElementById('attributes')
				.getElementsByClassName('collapsible')
			for (i = 0; i < as.length; i++) {
				as[i].classList.toggle('active')
				as[i].nextElementSibling.style.display = 'block'
			}
			/* Add EventListener to "roller". */
		</script>
		<script>
			fun die(face) {
				return (Math.random() % face) + 1 ;
			}
			fun roller(face, count) {
				var i = 0;
				var sum = 0;
				for (i = 0; i < count; i++) {
					sum += die(face);
				}
				return sum;
			}
		</script>
		</body>
		</html>
		""".trimIndent().trimIndent().trimIndent()

		val filename = "/tmp/DND-Assistant.htm"
		File(filename).writeText(htmlHorror.trimIndent())

		// File("/home/nox/Test.txt", "Hello world")
		// File("Test2.txt", "Hello world")

		println("Written to file ${filename}")
	}

	/** Show the big table of the attributes. */
	fun showAttributes() : String {
		var content = ""
		val groupSkills = enumValues<Skill>().groupBy { it.source }
		enumValues<Ability>().forEach { a ->
			val fullname = a.fullname.toLowerCase()
			val name = a.name.toLowerCase()
			content += "\n" + td(
				attributes = "title='${fullname.capitalize()}' id='${fullname}'",
				content
					= ("\n"
					+ div(
						attributes = "class='collapsible'",
						content = b(a.toString())
						+ "&nbsp;"
						+ span(
							"%d (%+d)".format(
								char.abilityScore(a),
								char.abilityModifier(a)),
							"class='preview roller' id='val-${name}'")
					)
					+ "\n"
					+ div(
						attributes = "class='content'",
						content = ul(
							attributes = "class='no-bullets larger'",
							content = (saveToListItem(a)
							+ (groupSkills[a] ?: listOf()).joinToString("") {
								skillToListItem(it)
							})
						)
					)
				) + "\n"
			)
		}

		return table(tr(content))
	}

	private fun saveToListItem(save: Ability) : String
		= li(
			attributes = "class='roller'",
			content = "<span class='preview %s'>%+d</span> %s".format(
				char.getProficiencyFor(save).run { when (this) {
					Proficiency.NONE -> ""
					else -> this.name.toLowerCase()
				}},
				char.abilityModifier(save),
				save.name + " Save")
		)

	private fun skillToListItem(skill: Skill) : String
		= li(
			attributes = "class='roller'",
			content = "<span class='preview %s'>%+d</span> %s".format(
				char.getProficiencyFor(skill).run { when (this) {
					Proficiency.NONE -> ""
					else -> this.name.toLowerCase()
				}},
				char.skillScore(skill),
				skill.fullname)
		)

	fun showCombatStats() : String {
		// show initiative, armor class, death saves, etc.

		val deathsavePass = "&#x2661;"
		val deathsaveFail = "&#x2620;"

		val styleLongRest = "class='bordered smaller' style='width:30%;border-width:2px;' "
		val styleShortRest =  "class='bordered smaller' style='width:20%;padding:5% 0%;' "

		val styleLightGray = "class='value' style='width:10%;color:lightgray;' "

		val styleGrayDashed = "class='value' style='width:75%;border:1px dashed #aaaaaa22;' "

		return div(
			table(
				attributes = "class='smaller'",
				content = tr(
					attributes = "id='hitpoints'",
					content =
					td(
						b("Hit Points")
						+ div("${char.curHitPoints}", "class='value' id='hitpoints-current'")
					)

					+ td(
						b("Hit Dice")
						+ div(
							// long rest: fill dice: ceil(dice / 2), full hp
							div("zzZ", styleLongRest + " title='Long Rest.'")

							// sort rest: roll some or all hit dice, get some hp
							+ char.hitdice.keys.joinToString(""){
								it.asFaceList().joinToString("") {
									div("d${it}",
									styleShortRest + " title='Short Rest.'")
								}
							},
						"class='roller value flex-container' " +
						"id='hitdice' " +
						"style='justify-content: center;'")

					)

					// fail &#x2620; passed: &#x2661;
					+ td(
						b("Death Saves")
						+ div(
							div(deathsaveFail, styleLightGray + "id='deathsaves-fail' title='Failed.'")
							+ div(char.deathSaves.toList().joinToString(""){ when(it) {
									-1 -> deathsaveFail
									1 -> deathsavePass
									else -> ""
							}}, styleGrayDashed + " id='deathsaves'")
							+ div(deathsavePass, styleLightGray + "id='deathsaves-passed' title='Passed.'"),
							"class='flex-container'")
					)
				)
				+ tr(
					attributes = "id='ac-init-speed'",
					content =

					td(
					b("Armor Class")
					+ div("${char.armorClass}", "id='ac' class='value' title= '${char.worn}'")
					)


					+ td(
					b("Initiative")
					+ div("%+d".format(char.initiative),
					"class='roller value' id='initiative' title='Initiative! Roll for Initiative!'")
					)

					/* Difficult terrain: +1 feet spent for the normal feet
					 * Climbing, Swimming, Crawling: +1 feet spent for the normal feet
					 * Long Jump: ("Run 10ft: distance up to strength score | stading: 1/2 Strength score")
					 * High Jump: ("Run 10ft: distance 3 + up to strength score | Stading: 1/2 that (extend arms to add 1.5x height"))
					 */

					+ td(
					b("Speed")
					+ div(char.speed.toList().joinToString(""){
							div("%dft (%s)".format(it.second, it.first))
						}, "id='speed' class='value' title=" + """
						'Options:
						- Dash (run 2x full speed (movement and main))
						- Stand Up (0.5x speed)
						- Climb/Crawl/Swim (spend extra +1)
						- Difficult Terrain (spend extra +1)
						- Long Jump (stand: 1/2 STR mod, run 10ft: STR mod),
						- High Jump (stand: 1/2 STR mod + 1.5), run 10ft: STR mod + 3).'
						""".trimIndent())
					)
				)
				+ tr(
					td(
					div(char.conditions.joinToString("") {
						div(it.name, "class='bordered' title='${it.note}'")
					}, "class='flex-container' style='padding:2%;'"),
					"colspan='3'"))
			))
	}

	/** Show health bar.*/
	fun showHealthBar() : String {
		val hp = char.curHitPoints to char.maxHitPoints
		return bar("hitpoint-bar",
			hp.first * 100.0 / hp.second,
			"-", "+", "${hp.first}/${hp.second} hp",
			"green")
	}

	/** Show the proficiencies and languages, preview: proficiency bonus.*/
	fun showProficiencies() : String {
		return div(
			div(
				attributes = "class='collapsible'",
				content = b("Proficiencies &amp; Languages")
				+ " " + previewSpan("(%+d)".format(char.proficientValue)))
			+ div(
				attributes = "class='content'",
				content = "\n" + div(
					attributes = "class='flex-container'",
					content
						= ("\n"
						+ div(b("Language")
							+ ul(listOf("Common", "Gnomish", "TODO")
							.joinToString("") { li(it) }))
						+ "\n"
						+ div(b("Proficient")
							+ ul(char.proficiencies
							.filterValues { it == Proficiency.PROFICIENT }.keys
							.joinToString("") { li(it.toString()) } ))
						+ "\n"
						+ div(b("Expert")
							+ ul(char.proficiencies
							.filterValues { it == Proficiency.EXPERT }.keys
							.joinToString("") { li(it.toString()) } ))
						)
				)
			)
		)
	}

	/** Show the inventory, preview: weight and money.*/
	fun showInventory() : String {
			// this bar displays, how much of the carrying weight capacity is already carried.
		var content = div(bar("inventory-bar",
				char.carriedWeight / char.carryingCapacity * 100.0,
				span("%.1f lb".format(char.carriedWeight), "class='smaller'"),
				span("%.1f lb".format(char.carryingCapacity), "class='smaller'"),
				"Inventory Capacity",
				"gold"
				), "style='padding:1vw'")

		// show equipped items.
		content += ("\n" + div("${b("Equipped")} <span class='preview'>(%.1f lb)</span>".format(
				char.carriedWeightHands + char.carriedWeightWorn
			))
			+ "\n"
			+ table(
				tr(td(b("Hold")) + td("${char.hands}")) + "\n"
				+ char.worn.toList().joinToString("\n") {
					tr(
						td(b(it.first.toLowerCase().capitalize()))
						+ td(it.second.toString()))
				} + "\n",
				"class='smaller'"
			)
		)

		if (!char.bags.isEmpty()) {
			content += "\n<br/>\n"
			content += div(b("Bags") + span(" (%.1f lb)".format(
				char.carriedWeightBags
			), "class='preview'"))

			// only top nesting.
			content += ul(char.bags
				.filterKeys { it.toCharArray().filter { c -> c == ':' }.size < 2}
				.toList().joinToString("") {
					"\n" + bagToListItem(it.second)
				})
		}

		val preview = span("(%.1f lb / %s)".format(
			char.carriedWeight,
			"${char.purse}"), "class='preview'")

		return div(
			div(b("Inventory") + " $preview", "class='collapsible'")
			+ div(content + "\n", "class='content'"))
	}

	private fun bagToListItem(bag: Container) : String {
		val note : String

		val weight = bag.sumWeight(true)
		var items = ""

		if (bag.isEmpty()) {
			note = "empty"
		} else {
			note = if (bag.isFull()) "full" else bag.capacity
			// sum items by category.
			bag.insideGrouped.forEach {
				if (it.value[0] is Container) {
					// display the sub containers.
					it.value.forEach { subBag ->
						items += "\n" + bagToListItem(subBag as Container)
					}
				} else if (it.value[0] is LooseItem) {
					items += "\n" + li("%s <span class='preview'>(%.2f %s / %.1f lb)</span>".format(
						it.key, // items name
						999.9, // it.value.sumByDouble { (it as LooseItem).count }, // items count // TODO cannot be displayed.
						"liter", // (it.value[0] as LooseItem).measure, // measure unit // TODO difficulties to display.
						it.value.sumByDouble {
							// summed weight
							if (it is Container) it.sumWeight(true) else it.weight
						}
					))
				} else {
					items += "\n" + li("%4d %c %s <span class='preview'>(%c%5.1f lb)</span>".format(
						it.value.size, // items count
						0xd7, // times symbol
						it.key, // items name
						0x3a3, // sum symbol
						it.value.sumByDouble {
							// summed weight
							if (it is Container) it.sumWeight(true) else it.weight
						}
					))
				}
			}
			items = ul(items)
		}

		var firstLine = "${bag.name} " + span("($weight lb, $note)", "class='preview'")

		firstLine = div(firstLine, "class='collapsible'")
		items = div(items, "class='content'")

		return li("${firstLine}${items}", "class='bag'")
	}

	/** Show the available attacks, preview: most damage attack. */
	fun showAttacks() : String {
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

		val content = div(
			attributes = "class='content'",
			content = table(attacks.joinToString("") {
				tr(
					td(it.name.replace("(", "<div class='preview'>").replace(")", "</div>"), "style='width:33%;'")
					+ td("%+d".format(it.attackBonus), "class='right' style='padding:0.5vw;'")
					+ td(it.damageRoll.toString()
						+ previewSpan(" (%.1f)".format(it.damageRoll.average))
						+ div(if (it.damageType.size < 1) "???" else it.damageType.joinToString(), "style='font-size:small;'"),
						"style='width:33%;padding:0.5vw;'")
					+ td(it.note, "class='preview'")
				)
			})
		)

		// Preview: Maximal Attack.
		// it must be at least size:3, since they are put manually.
		val maxAtk = attacks.maxByOrNull { it.damageRoll.average }!!

		val title = div(
			attributes = "class='collapsible'",
			content
				= (b("Attacks")
				+ " "
				+ span("(max: ${maxAtk.name}:${maxAtk.damageRoll})",
				"class='preview'"))
		)

		return div(title + content)
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
	fun showSpells() : String {
		var content: String = ""
		var preview: String = ""

		preview += "${(0..9).filter{ char.spellSlots[it].second > 0 }
			.joinToString(":", "left slots: [", "]") }"

		// TODO (2020-09-03) other magic sources, like the dark elf: once per long rest

		/* Content. */
		content += div(
			attributes = "class='content'",
			content = ("\n"
			+ b("Spell Slots")
			+ table(
				"\n"
				+ tr((0 until char.spellSlots.size).toList().joinToString("") {
					td(b("$it"), "class='spellslot'")
				})
				+ "\n"
				+ tr(char.spellSlots.toList().joinToString("") {
					td(if (it.second < 0) "\u221E" /* infinity */ else "${it.second}",
					"class='spellslot'")
				})
				+ "\n",
				"id='spell-slots' title='Spell Slots'"
			  )
			+ "\n<br/>\n"
			+ table(
				attributes = "class='spells-updater'",
				content = tr(td("[+] Learn new a spell.") + td("[~] Prepare other spells.")))
			+ "\n<br/>\n"
			+ b("Known Spells")
			+ "\n"
			+ table(char.spellsKnown
				.sortedWith(comparingSpells)
				.joinToString("") {
					var spellName = "${it.name}"
					var spellSchool = "${it.school}, "
					spellSchool += if (it.level > 0) "Level ${it.level}" else "Cantrip"
					var spellAttribute = ""

					var spellLearnt = "Wizard" // TODO

					val prepSlot = char.spellsPrepared.getOrDefault(it, -1)
					val activeLeft = char.spellsActive.getOrDefault(it, -1)

					val prep : String
					if (prepSlot < it.level && true /* TODO */) {
						prep = i("not prepared")
						spellAttribute = "style=opacity:40%;"
					} else {
						prep = "${prepSlot}"
					}

					val duration = when {
						activeLeft < 1 -> ""
						it.concentration ->
							u("${activeLeft}s left", "title=Concentration").also { _ ->
								preview += ", *${spellName}" // also add spell to preview.
								spellAttribute = "class='concentration activated'"
						}
						else ->
							"${activeLeft}s left".also {
								preview += ", ${spellName}" // also add spell to preview
								spellAttribute = "class='activated'"
						}
					}

					spellName = span(spellName, spellAttribute)
					spellSchool = div(
						"${spellSchool}, learnt as ${spellLearnt}",
						"style='color:gray;font-size:xx-small;'")

					val td0 = td(prep, "class='spell-prepared-slot'")
					val td1 = td("${spellName}${spellSchool}")
					val td2 = td(duration, "class='spell-active-duration'")

					"\n" + tr(
						attributes = "class='spell'",
						content = td0 + td1 + td2
					)
				}
				+ "\n")
			)
		)

		/* Title */
		val title = div(
			attributes = "class='collapsible'",
			content = (b("Spells") + " " + span("(${preview})", "class='preview'"))
			)

		return div(title + "\n" + content)
	}

	fun showKlasses() : String {
		val classes = char.klasses

		// preview of classes and levels.
		val preview = classes.toList().joinToString(
			prefix = " (", postfix = ")", transform = {
			"${it.first}:${it.second.second} (lvl.${it.second.first})"
		})

		return div(
			div(
				attributes = "class='collapsible'",
				content = b("Classes") + " " + previewSpan(preview))
			+ "\n"
			+ div(
				attributes = "class='content'",
				content = ul(classes.toList().joinToString("") {
					val (l, s) = it.second

					val features = it.first.getFeaturesAtLevel(l, s)

					// occupation, (level, specialisiation)
					// add features from the occupation.

					var featuresStr = ""

					if (features.size > 0) {
						featuresStr = ul(features.joinToString("") {
							li("${it.title}\n"
								+ div(it.description,
								"class='preview' style='padding-bottom:1%;'")
								+ "\n"
							)}
						)
					}

					// Occupation, level, Specialisation
					// - features ...
					li("${it.first}, level ${l}"
						+ (if (s == "") s else " $s")
						+ featuresStr
					)
				})
			)
		)
	}

	fun showRaces() : String {
		val raceName = "${char.race.name}"
		val subRace = "${char.subrace}"
		val darkvision = when (char.race.darkvision) {
			0 -> ""
			else -> ", darkvision ${char.race.darkvision}"
		}

		return div(
			div(
				attributes = "class='collapsible'",
				content = (b("Race")
				+ previewSpan(" (${raceName} (${subRace})${darkvision})")))
			+ "\n"
			+ div(
				attributes = "class='content'",
				content = (""
					+ char.race.allFeatures(char.subrace)
						.joinToString("", "<ul>", "</ul>") {
							li(it.name + when (it.hasDescription) {
								false -> ""
								else -> (":\n" + div(it.description,
									"class='preview smaller'"))
							})
						}
					+ "\n<br/>\n"
					+ div(char.race.description, "class='bordered smaller'")
					+ "\n"
				)
			)
		)
	}

	fun showBackground() : String {
		val age = when {
			char.age < 0 -> "${-char.age} days"
			else -> "${char.age} yrs"
		}

		// show background name and alignment.
		val preview = "(${age} (${char.alignment.abbreviation}), ${char.background})"

		return div(
			div(b("Background") + " " + previewSpan(preview), "class='collapsible'")
			+ "\n"
			+ div(
				attributes = "class='content'",
				content = table(
					tr(td("Age:", "style='width:30%'") + td(age))
					+ tr(td("Alignment:") + td(char.alignment.toString()))
					+ tr(td("Background:") + td("${char.background}, ${char.speciality}"))
				)
				+ "\n<br/>\n"
				+ b("Motives:") + "\n"
				+ table(
					tr(td("Trait:", "style='width:30%'") + td(char.trait))
					+ tr(td("Ideal:") + td(char.ideal))
					+ tr(td("Bonds:") + td(char.bonds))
					+ tr(td("Flaws:") + td(char.flaws))
				)
				+ "\n<br/>\n"
				+ b("Story:")
				+ char.history.joinToString("","<ul>", "</ul>") { li(it) }
			)
		)
	}

	fun showAppearance() : String {
		// TODO (2020-07-18) Implement PlayerCharacter's appearance.

		val size = char.race.size.name.toLowerCase()
		val form = char.form
		val etc = char.appearance

		// Preview: show size/height/weight
		val preview
			= listOf(size, form, etc)
			.filter { it.trim() != "" }

		return div(
			div(
				b("Appearance") + " "
				+ span(preview.joinToString(", ", "(", ")"), "class='preview'"),
				"class='collapsible'")
			+ div(
				attributes = "class='content'",
				content = table(
					"\n" + tr(td("Height:", "width=30%") + td("%.1f cm, %s".format(feetToMeter(char.height) * 100.0, size)))
					+ "\n" + tr(td("Weight:") + td("%.1f lb, %s".format(char.weight, form)))
					+ "\n" + tr(td("More:") + td(etc))
				)
			)
		)
	}

	fun showRollHistory(unfold: Boolean = false) : String {
		return "# RollHistory" + if (unfold) {
			"\n|"
			// TODO (2020-07-18) Roll history .... ?
		} else {
			""
		}
	}

	fun htmlTag(tag: String, content: String, attributes: String = "") : String
		= "<${tag}${if (attributes.length > 0) " $attributes" else ""}>${content}</${tag}>"

	fun span(content: String, attributes: String = "") : String
		= htmlTag("span", content, attributes)

	fun previewSpan(content: String, attributes: String = "") : String
		= span(content, ("class='preview' " + attributes).trim())

	fun div(content: String, attributes: String = "") : String
		= htmlTag("div", content, attributes)

	fun collapsibleDiv(content: String, attributes: String = "") : String
		= div(content, ("class='collapsible' " + attributes).trim())

	fun b(content: String, attributes: String = "") : String
		= htmlTag("b", content, attributes)

	fun i(content: String, attributes: String = "") : String
		= htmlTag("i", content, attributes)

	fun u(content: String, attributes: String = "") : String
		= htmlTag("u", content, attributes)

	fun li(content: String, attributes: String = "") : String
		= htmlTag("li", content, attributes)

	fun ul(content: String, attributes: String = "") : String
		= htmlTag("ul", content, attributes)

	fun ol(content: String, attributes: String = "") : String
		= htmlTag("ol", content, attributes)

	fun th(content: String, attributes: String = "") : String
		= htmlTag("th", content, attributes)

	fun td(content: String, attributes: String = "") : String
		= htmlTag("td", content, attributes)

	fun tr(content: String, attributes: String = "") : String
		= htmlTag("tr", content, attributes)

	fun table(content: String, attributes: String = "") : String
		= htmlTag("table", content, attributes)

	private fun bar(
		id: String,
		value: Double,
		left: String = "",
		right: String = "",
		title: String = "",
		colour: String = "#444"
	) : String {
		return table(
			attributes = ("id='$id'"
				+ " class='progress-bar'"
				+ " title='$title (${"%.1f".format(value)}%)'"),
			content = tr(
				td(left, "id='$id-left'")
				+ td(
					div(
						attributes = "id='$id-bar' class='progress'",
						content = div("",
						"id='$id-bar-value'"
						+ " class='progress-value'"
						+ " style='width:${value}%;background:${colour}'")
					)
				)
				+ td(right, "id='$id-right'")
			 )
		)
	}
}
