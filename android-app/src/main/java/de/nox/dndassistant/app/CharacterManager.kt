package de.nox.dndassistant.app

import de.nox.dndassistant.core.Hero
import de.nox.dndassistant.core.Ability

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
