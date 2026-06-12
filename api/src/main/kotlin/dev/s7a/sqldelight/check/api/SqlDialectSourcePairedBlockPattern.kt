package dev.s7a.sqldelight.check.api

/**
 * A paired source block pattern such as `CASE ... END`.
 */
public class SqlDialectSourcePairedBlockPattern(
    public val startExpression: SqlDialectSourcePatternExpression,
    public val endExpression: SqlDialectSourcePatternExpression,
    public val kind: SqlSourceBlockKind,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlDialectSourcePairedBlockPattern &&
            startExpression == other.startExpression &&
            endExpression == other.endExpression &&
            kind == other.kind

    override fun hashCode(): Int {
        var result = startExpression.hashCode()
        result = 31 * result + endExpression.hashCode()
        result = 31 * result + kind.hashCode()
        return result
    }

    override fun toString(): String =
        "SqlDialectSourcePairedBlockPattern(startExpression=$startExpression, endExpression=$endExpression, kind=$kind)"

    public companion object {
        /**
         * Parses compact source pattern expressions for a paired block.
         */
        public fun parse(
            startExpression: String,
            endExpression: String,
            kind: SqlSourceBlockKind,
        ): SqlDialectSourcePairedBlockPattern =
            SqlDialectSourcePairedBlockPattern(
                startExpression = SqlDialectSourcePatternExpression.parse(startExpression),
                endExpression = SqlDialectSourcePatternExpression.parse(endExpression),
                kind = kind,
            )
    }
}
