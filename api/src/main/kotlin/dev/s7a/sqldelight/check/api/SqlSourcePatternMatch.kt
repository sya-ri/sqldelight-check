package dev.s7a.sqldelight.check.api

/**
 * A dialect source pattern match that starts at a source token.
 *
 * The [length] value is the number of source tokens consumed by the matched
 * pattern expression.
 */
public class SqlSourcePatternMatch(
    public val pattern: SqlDialectSourcePattern,
    public val length: Int,
) {
    /**
     * The source-scanner roles attached to the matched pattern.
     */
    public val roles: Set<SqlDialectSourcePatternRole>
        get() = pattern.roles

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlSourcePatternMatch &&
            pattern == other.pattern &&
            length == other.length

    override fun hashCode(): Int = 31 * pattern.hashCode() + length

    override fun toString(): String =
        "SqlSourcePatternMatch(pattern=$pattern, length=$length)"
}
