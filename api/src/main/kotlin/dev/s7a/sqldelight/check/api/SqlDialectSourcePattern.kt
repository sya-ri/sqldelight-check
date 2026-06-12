package dev.s7a.sqldelight.check.api

/**
 * A dialect source pattern and the scanner roles it fulfills.
 */
public class SqlDialectSourcePattern(
    public val expression: SqlDialectSourcePatternExpression,
    public val roles: Set<SqlDialectSourcePatternRole>,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlDialectSourcePattern &&
            expression == other.expression &&
            roles == other.roles

    override fun hashCode(): Int = 31 * expression.hashCode() + roles.hashCode()

    override fun toString(): String =
        "SqlDialectSourcePattern(expression=$expression, roles=$roles)"

    public companion object {
        public fun parse(
            expression: String,
            vararg roles: SqlDialectSourcePatternRole,
        ): SqlDialectSourcePattern =
            SqlDialectSourcePattern(
                expression = SqlDialectSourcePatternExpression.parse(expression),
                roles = roles.toSet(),
            )
    }
}
