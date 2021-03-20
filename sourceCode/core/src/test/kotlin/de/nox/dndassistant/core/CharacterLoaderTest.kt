package de.nox.dndassistant.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.math.abs

import java.io.File

class CharacterLoaderTest {

	private val log = LoggerFactory.getLogger("Hero-Loader-Test")

	@Test
	fun testLoading() {
		log.info("Test: testLoading()")
		log.displayLevel(LoggingLevel.DEBUG)
		var hero = loadHero(File("src/test/resources/Hero.json").getAbsolutePath())

		log.info("Loaded the Hero: $hero")

		log.info("Test outsider mapping? Better not? (if it was applied, all booleans will be flipped.)")


		log.info("Loaded the Hero: Speed: ${hero.speed.toList().joinToString()}")

		log.info("Loaded the Hero: Klasses: ${hero.klasses}")

		log.info("-".repeat(50))

		log.info("Hero overview")
		hero.run {
			log.info("""
			Name: ${name}

			Player: ${player}
			Inspiration: $inspiration

			Race: ${race}
			Level: ${level}
			Experience: $experience

			HP: $hitpoints
			Death Saves: $deathsaves
			Hit Dice: $hitdice ($hitdiceMax)

			Attributes:
			| STR | DEX | CON | WIS | INT | CHA |
			${"| %s | %s | %s | %s | %s | %s |".format(abilities[Ability.STR], abilities[Ability.DEX], abilities[Ability.CON], abilities[Ability.WIS], abilities[Ability.INT], abilities[Ability.CHA])}
			${"| %+3d | %+3d | %+3d | %+3d | %+3d | %+3d |".format(abilities[Ability.STR].getModifier(), abilities[Ability.DEX].getModifier(), abilities[Ability.CON].getModifier(), abilities[Ability.WIS].getModifier(), abilities[Ability.INT].getModifier(), abilities[Ability.CHA].getModifier())}

			Proficiency: $proficiencyBonus

			Languages:
			$languages

			Skills:
			${skillValues.toList().joinToString("\n\t\t\t* ","* ") { (s,v) -> "%s: %+d (%s)".format(s.name, v, skills[s]?.first?.toString() ?: "") }}

			Tools:
			${toolProficiencies}

			Classes:
			${hero.klasses.toList().joinToString("\n\t\t\t* ", "* "){(klass, subklass, level) -> "$klass ($subklass), $level" }}

			Specialities:
			${specialities.joinToString("\n\t\t\t* - ", "* - ")}

			Attacks:
			? - physical attacks
			? - spells
			? - spell DCs, spell (attack) modifiers, spell level

			Spells:
			? - Spells sorted by level?
			? - Also show preparable spells (for later planning?)
			* - Arcane Focus: ${getArcaneFocus()?.name}
			* - Spells Prepared: ${spellsPrepared.size} items
			* - ${spells}

			Buffs and Effects:
			? - being near paladin: +2 saving throws
			? - being bewitched with Bless: 1d4 on saving throws and co.
			? - being bewitched with Bane: -1d4 on saving throws
			? - being bewitched with Mage Armor and naked: 13 + DEX
			? - being bewitched by Charm: Doing stuff as they like
			? - being bewitched by Confusing: Doing random stuff
			? - exhaustion points: 1

			Inventory:
			- summed items: ${inventory.size}
			- summed weight: ${inventory.weight()} / ${maxCarriageWeight()} lb
			- summed value: ${inventory.copperValue()} cp
			> DEBUG INVENTORY: $inventory
			- ${inventory.printNested().split("\n").joinToString("\n\t\t\t")}

			""".trimIndent()
			)
		}

		"Hero's Name"
			.assertEquals("Example Hero", hero.name)

		"Hero's Player"
			.assertEquals("Nox", hero.player)

		"Hero's Inspiration"
			.assertEquals(2, hero.inspiration) // accumulated inspiration "points" / tokens

		"Hero's Race"
			.assertEquals("Dog" to "Bantam Dog", hero.race)

		"Hero's Level"
			.assertEquals(14, hero.level)

		"Hero's Experience"
			.assertEquals(Hero.Experience(0, "milestone"), hero.experience)


		"Hero's HP"
			.assertEquals(25 to  109, hero.hitpoints)

		"Hero's Death Saves"
			.assertEquals(Hero.DeathSaveFight(), hero.deathsaves) // empty

		"Hero's Death Saves: Is not dead, is not saved (undecided)."
			.assertEquals(null, hero.deathsaves.evalSaved())


		// "Hero's Hit Dice".assertEquals(null, hero.hitdice) // 6d6
		// "Hero's Hit Dice (Max)".assertEquals(14, hero.hitdiceMax.size) // 14 dice: 12 sorcerer + 2 monk

		hero.hitpointsNow -= hero.hitpointsMax*3 // instant death

		"Hero's HP (after deadly blow)"
			.assertEquals(0 to 109, hero.hitpoints)

		"Hero's Death Saves (after deadly blow): Is finally dead."
			.assertEquals(false, hero.deathsaves.evalSaved())

		 // 7 (days in campaign)
		"Hero's Custom Counter Count Before: ${hero.specialities.last()}"
			.assertEquals(6, hero.specialities.last().count?.current)

		hero.specialities.last().countUp()
		"Hero's Custom Counter Count (+1): ${hero.specialities.last()}"
			.assertEquals(7, hero.specialities.last().count?.current)

		hero.specialities.last().countUp(3)
		"Hero's Custom Counter Count (+3): ${hero.specialities.last()}"
			.assertEquals(10, hero.specialities.last().count?.current)

		"Hero's Languages: ${hero.languages}"
			.assertEquals(0, 0)

		"Hero's Skills: ${hero.skillValues.toList()}}"
			.assertEquals(0, 0)

		"Hero's Classes: ${hero.klasses.toList()}}"
			.assertEquals(0, 0)

		"Hero's Specialities: ${hero.specialities}"
			.assertEquals(0, 0)

		val todo = """
		Attacks:
		? - physical attacks
		? - spells
		? - spell DCs, spell (attack) modifiers, spell level

		Spells:
		? - Spells sorted by level?
		? - Also show preparable spells (for later planning?)
		* - Arcane Focus: ${hero.getArcaneFocus()?.name}
		* - Spells Prepared: ${hero.spellsPrepared.size} items
		* - ${hero.spells}

		Buffs and Effects:
		? - being near paladin: +2 saving throws
		? - being bewitched with Bless: 1d4 on saving throws and co.
		? - being bewitched with Bane: -1d4 on saving throws
		? - being bewitched with Mage Armor and naked: 13 + DEX
		? - being bewitched by Charm: Doing stuff as they like
		? - being bewitched by Confusing: Doing random stuff
		? - exhaustion points: 1
		"""

		"Hero's Max Carrying Capacity: STR * 15 = 7 * 15"
			.assertEquals(hero.ability(Ability.STR) * 15.0, hero.maxCarriageWeight())

		"Hero's Inventory: Count of items"
			.assertEquals(13 + 1 /*oil*/ + 2500 /*gp*/, hero.inventory.size)

		"Hero's Inventory: Summed weight (more than 2.5k GP (0.01 lb)"
			.assertEquals(true, 25.0 <= hero.inventory.weight())

		"Hero's Inventory: Summed value (more than 2.5k GP (50 cp)"
			.assertEquals(true, 2500 * SimpleItem.GP_TO_CP <= hero.inventory.copperValue())

		// Inventory:
		// - summed items: ${hero.inventory.size}
		// - summed weight: ${hero.inventory.weight()} / ${maxCarriageWeight()} lb
		// - summed value: ${hero.inventory.copperValue()} cp
		// > DEBUG INVENTORY: $inventory
		// - ${hero.inventory.printNested().split("\n").joinToString("\n\t\t\t")}

		log.info("Test: testLoading: OK!")
	}

	private fun <T> String.assertEquals(expected: T, actual: T)
		= let {
			assertEquals(expected, actual, this)
			log.debug("Assert Equals: '$this' as expected ($expected).")
		}
}
