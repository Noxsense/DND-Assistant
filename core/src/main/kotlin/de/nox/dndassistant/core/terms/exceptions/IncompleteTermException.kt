package de.nox.dndassistant.core.terms.exceptions

/** Incompplete term, for example if a opening bracket was not closed. */
public class IncompleteTermException(msg: String = "Not enough operatores to combine all terms")
	: TermParsingException(msg);
