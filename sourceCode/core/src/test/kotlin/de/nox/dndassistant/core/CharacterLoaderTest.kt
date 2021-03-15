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
		log.info("Loaded the Hero: Skills ${hero.proficiencies.toList().joinToString(", ", "{", "}")}")

		log.info("Test outsider mapping? Better not? (if it was applied, all booleans will be flipped.)")
		hero.proficiencies.mapValues { (_, expertReason) -> expertReason.let { (e, r) -> !e to r } }

		log.info("Loaded the Hero: Skills ${hero.proficiencies.toList().joinToString(", ", "{", "}")}")

		log.info("Loaded the Hero: Speed: ${hero.speed.toList().joinToString()}")

		log.info("Loaded the Hero: Klasses: ${hero.klasses}")

		hero.hitpointsNow -= hero.hitpointsMax*3 // instant death

		log.info("-".repeat(50))

		log.info("Hero overview")
		hero.run {
			log.info("""
			Name: ${name}

			Player: ${player}
			Inspiration: $inspiration

			Race: ${race}
			Level: ${level}
			Expierience: $experience

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
			${hero.skillValues.toList().joinToString("\n\t\t\t* ","* ") { (s,v) -> "%s: %+d".format(s.name, v) }}

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

		log.info("\n\nCount Before:\n${hero.specialities.last()}")
		hero.specialities.last().countUp() // 7 (days in campaign)
		log.info("\n\nCount Before:\n${hero.specialities.last()}")
		hero.specialities.last().countUp(3) // 10
		log.info("\n\nCount Up:\n${hero.specialities.last()}")

		assertTrue(false)

		log.info("Test: testLoading: OK!")
	}
}
