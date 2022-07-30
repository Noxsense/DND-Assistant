package de.nox.dndassistant.core.terms.exceptions

/** Term represents not an basic term. */
public class NoBasicTermException(msg: String = "This Term does not represent a basic RollingTerm.")
	: TermParsingException(msg);

