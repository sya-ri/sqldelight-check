package dev.s7a.sqldelight.check.api

/**
 * Terms that adjust source parenthesis nesting depth.
 */
public class SqlDialectSourceParenthesisDepthTerms(
    public val openTerm: String,
    public val closeTerm: String,
) {
    public val normalizedOpenTerm: String = openTerm.lowercase()

    public val normalizedCloseTerm: String = closeTerm.lowercase()

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlDialectSourceParenthesisDepthTerms &&
            openTerm == other.openTerm &&
            closeTerm == other.closeTerm

    override fun hashCode(): Int =
        31 * openTerm.hashCode() + closeTerm.hashCode()

    override fun toString(): String =
        "SqlDialectSourceParenthesisDepthTerms(openTerm=$openTerm, closeTerm=$closeTerm)"
}
