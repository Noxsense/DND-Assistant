package de.nox.dndassistant.core

import kotlin.test.Test
import kotlin.test.assertTrue

class ItemTest {

	private val logger = LoggerFactory.getLogger("ItemTest")

	@Test
	fun testConsumables() {
		// TODO (2020-09-04)
		assertTrue(true, "Test consumables.")
		// create consumble.
		// say it is consumed or so.
	}

	@Test
	fun testClothes() {
		// TODO (2020-09-04)
		assertTrue(true, "Test clothes")
		// create clothings.
		// each clothing for different body positions.
		// modular clothes (like gloves may need the partner)
	}

	@Test
	fun testWeapons() {
		// TODO (2020-09-04)
		assertTrue(true, "Test Weapons")
		// create a weapon of each type and combination
		// two handed, versatile, etc...
		// get damage and other notes.
	}

	@Test
	fun testArmor() {
		// TODO (2020-09-04)
		assertTrue(true, "Test Armor")
		// create armor of each kind
		// get AC and other notes.
	}

	@Test
	fun testStorage() {
		// TODO (2020-09-04)
		assertTrue(true, "Test Storage")
		// store items into a bag.
		// check the bag
		// check the nested bag
	}

	@Test
	fun testLooseItems() {
		// TODO (2020-09-04)
		assertTrue(true, "Test loose items")
		// put loose item into a container.
		// small pieces might not mix, and add to each other.
		// liquid items may mix and loose their attributes.
		// TODO (2020-09-04) Q: A liquid is also consumable?
	}
}
