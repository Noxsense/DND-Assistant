package de.nox.dndassistant.core

private val log = LoggerFactory.getLogger("Loader")

// XXX (2020-10-26) implement character saver.
/** Save a player character to source. */
public fun saveCharater(char: PlayerCharacter, src: String) {
}

/** Load the state from a given resource. */
public fun loadState(char: PlayerCharacter, stateRes: String) {
}

// XXX (2020-10-26) implement character loader.
/** Load a player character from source. */
public fun loadCharater(src: String) : PlayerCharacter
	= playgroundWithOnyx().also {
		log.info("Loader charachter $it from source: $src")
	}

