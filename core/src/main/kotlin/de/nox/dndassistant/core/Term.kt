package de.nox.dndassistant.core

/**
 * Term, that defines a next value.
 */
public interface Term {

    /**
     * Get the current value of the Term.
     * <br/>
     * If it is a rolling term, it gets the latest rolled term.
     * If it is a variable / reference, it gets the current snapshot of that value.
     *
     * @return get current value of the term.
     */
    fun get() : Int
}

public interface CombinedTerm : Term {
}

// TODO (2024-07-31) own files.

public class NumTerm(
    value: Int
) : Term {

    override public fun get() : Int
        = 0
}

public fun interface ReferenceLookup {
    fun lookup() : Int
}

public class RefTerm(
    name : String,
    lookup : ReferenceLookup
) : Term {
    override public fun get() : Int
        = 0
}

public class RollTerm(
    face : Int
) : Term {
    override public fun get() : Int
        = 0
}

public class SumTerm(
    left: Term,
    right: Term
) : CombinedTerm {
    override public fun get() : Int
        = 0
}
