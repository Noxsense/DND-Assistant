package de.nox.dndassistant.core.terms

public typealias TermVaribales = (Reference) -> Int

/** A simple / basic RollingTerm, that will be replaced with a function that returns an Int. */
class Reference(val name: String): BasicRollingTerm();
