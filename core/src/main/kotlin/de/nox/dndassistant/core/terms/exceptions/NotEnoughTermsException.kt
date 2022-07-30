package de.nox.dndassistant.core.terms.exceptions

/** Recreating the term is not possible. Not enough terms. */
public class NotEnoughTermsException(msg: String = "Not enough terms, to operate with.")
	: TermParsingException(msg);
