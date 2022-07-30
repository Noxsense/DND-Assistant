package de.nox.dndassistant.core.terms.exceptions

/** Recreating the term is not possible. Too many left over terms. */
public class NotEnoughOperatorsException(msg: String = "Not enough operatores to combine all terms")
	: TermParsingException(msg);
