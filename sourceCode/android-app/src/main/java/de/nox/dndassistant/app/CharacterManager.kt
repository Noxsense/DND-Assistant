package de.nox.dndassistant.app

import de.nox.dndassistant.core.PlayerCharacter
import de.nox.dndassistant.core.playgroundWithOnyx

/** CharacterManager.
 * - Provide options to load, update and save the player's character.
 * - Get the currently loaded character. */
class CharacterManager private constructor() {

	private val log = LoggerFactory.getLogger("D&D Characters")

	public companion object {
		/** Singleton. */
		val INSTANCE: CharacterManager = CharacterManager()
	}

	/** Get the currently loaded character.
	 * Load dummy in the beginning. */
	public var character: PlayerCharacter = playgroundWithOnyx()
		private set

	/** Load current character from a resource, given by an indicator. */
	public fun loadCharacter(res: String) {
		character = playgroundWithOnyx() // TODO (2020-10-28) Load the selected character.
	}

	/** Safe the current character with an indicator , to reload it later.
	 * The indicator can be a keyword or filename, defined only for this character. */
	public fun saveCharacter(res: String) : Boolean
		= true // save character to given relaod indicator.

	/** Initiate the creation of a new character. */
	public fun createCharacter() {
	}
}
