package de.nox.dndassistant.app

import de.nox.dndassistant.core.Ability
import de.nox.dndassistant.core.Hero
import de.nox.dndassistant.core.SimpleSpell
import de.nox.dndassistant.core.Speciality
import de.nox.dndassistant.core.CustomCount
import de.nox.dndassistant.core.SimpleItem

/** CharacterManager.
 * - Provide options to load, update and save the player's hero.
 * - Get the currently loaded hero. */
class CharacterManager private constructor() {

	private val log = LoggerFactory.getLogger("D&D Characters")

	public companion object {
		/** Singleton. */
		val INSTANCE: CharacterManager = CharacterManager()
	}

	/** Get the currently loaded hero.
	 * Load dummy in the beginning. */
	public var hero: Hero = Hero(name = "Full Hero Name", race = "Human" to "Sub-Human", player = null)
		.apply {
			// give some random ability scores.
			Ability.values().forEach { a ->
				this.abilities[a] = (1..20).random() to ((0..1).random() == 0)
			}
			hitpointsMax = (6..200).random()
			hitpointsNow = (10..hitpointsMax).random()

			// add up to 10 random spells
			(0..(0..3).random()).forEach {
				// learn spell
				learnSpell(SimpleSpell(
					name = "Random Spell $it",
					school = "EVOCATION",
					castingTime = "1 act", ritual = false,
					components = SimpleSpell.Components(),
					reach = 60, // feet
					targets = "1 target",
					duration = "Instantious",
					concentration = false,
					description = "A random cantrip genereated by CharacterManager.kt",
					levels = mapOf(it to mapOf("Spell Attack" to ("5d8" to "Fire damage"))),
					optAttackRoll = true,
					optSpellDC = false,
					klasses = setOf(),
				), "Race")

				// prepare spell
				prepareSpell("Random Spell $it")

				// add spell slots
				if (it > 0) specialities += CustomCount("Spell Slot $it", Speciality.Count("long rest", 1), "Magic Resource for Spells up to level $it")
			}

		}
		private set

	/** Load current hero from a resource, given by an indicator. */
	public fun loadCharacter(res: String) {
		// TODO (2020-10-28) Load the selected hero.
	}

	/** Safe the current hero with an indicator , to reload it later.
	 * The indicator can be a keyword or filename, defined only for this hero. */
	public fun saveCharacter(res: String) : Boolean
		= true // save hero to given relaod indicator.

	/** Initiate the creation of a new hero. */
	public fun createCharacter() {
	}
}
