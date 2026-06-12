package dev.s7a.sqldelight.check.api

/**
 * One part in a dialect source pattern expression.
 *
 * A part can match any value in [alternatives]. Optional parts can be omitted
 * when matching the whole expression.
 */
public class SqlDialectSourcePatternPart(
    alternatives: Set<String>,
    public val optional: Boolean = false,
) {
    public val alternatives: Set<String> = alternatives.normalizedSqlTerms()

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlDialectSourcePatternPart &&
            alternatives == other.alternatives &&
            optional == other.optional

    override fun hashCode(): Int = 31 * alternatives.hashCode() + optional.hashCode()

    override fun toString(): String =
        "SqlDialectSourcePatternPart(alternatives=$alternatives, optional=$optional)"
}
