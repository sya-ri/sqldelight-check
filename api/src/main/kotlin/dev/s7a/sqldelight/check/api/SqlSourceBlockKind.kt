package dev.s7a.sqldelight.check.api

/**
 * The conservative syntactic meaning of a SQL source block.
 */
public interface SqlSourceBlockKind {
    /**
     * A top-level SQLDelight or SQL statement.
     */
    public data object Statement : SqlSourceBlockKind

    /**
     * A dialect source-pattern clause segment.
     */
    public data object Clause : SqlSourceBlockKind

    /**
     * A parenthesized SQL expression or item list.
     */
    public data object ParenthesizedExpression : SqlSourceBlockKind

    /**
     * A parenthesized subquery or nested statement.
     */
    public data object Subquery : SqlSourceBlockKind

    /**
     * A `CASE ... END` expression.
     */
    public data object CaseExpression : SqlSourceBlockKind
}
