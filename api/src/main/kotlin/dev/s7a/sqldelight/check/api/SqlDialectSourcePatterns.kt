package dev.s7a.sqldelight.check.api

import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.AliasBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ClauseBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ExpressionContinuation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.JoinModifier
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ParenthesizedExpressionContinuation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SelectListStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SqlDelightStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementContinuation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TableReferenceBoundary

/**
 * Dialect-specific source patterns used by conservative source-text scanners.
 *
 * These patterns are used by source-text rules; they are not a full parser
 * grammar. Each pattern declares the scanner role it fulfills, so custom
 * dialect integrations can register engine-specific syntax once and rules can
 * ask for the meaning they need.
 */
public class SqlDialectSourcePatterns(
    public val patterns: Set<SqlDialectSourcePattern> = SourceScannerDefault.patterns,
) {
    public fun matchPrefix(
        role: SqlDialectSourcePatternRole,
        terms: List<String>,
    ): Int? =
        patterns
            .asSequence()
            .filter { pattern -> role in pattern.roles }
            .mapNotNull { pattern -> pattern.expression.matchPrefix(terms) }
            .maxOrNull()

    public fun matches(
        role: SqlDialectSourcePatternRole,
        terms: List<String>,
    ): Boolean =
        matchPrefix(role, terms) != null

    public fun patternsFor(role: SqlDialectSourcePatternRole): Set<SqlDialectSourcePattern> =
        patterns.filterTo(mutableSetOf()) { pattern -> role in pattern.roles }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlDialectSourcePatterns &&
            patterns == other.patterns

    override fun hashCode(): Int = patterns.hashCode()

    override fun toString(): String =
        "SqlDialectSourcePatterns(patterns=$patterns)"

    public companion object {
        /**
         * Conservative fallback used by the source scanner when no known dialect preset applies.
         */
        public val SourceScannerDefault: SqlDialectSourcePatterns =
            SqlDialectSourcePatterns(
                patterns =
                    sourcePatterns(
                        "ALTER",
                        roles = arrayOf(StatementStart, SqlDelightStatementStart),
                    ) +
                        sourcePatterns(
                            "CREATE",
                            "DELETE",
                            "INSERT",
                            "SELECT",
                            "UPDATE",
                            "WITH",
                            roles = arrayOf(StatementStart, SqlDelightStatementStart),
                        ) +
                        sourcePatterns(
                            "DROP",
                            roles = arrayOf(StatementStart, SqlDelightStatementStart),
                        ) +
                        sourcePatterns(
                            "CREATE SELECT",
                            "CREATE WITH",
                            "INSERT SELECT",
                            "INSERT WITH",
                            "WITH DELETE",
                            "WITH INSERT",
                            "WITH SELECT",
                            "WITH UPDATE",
                            roles = arrayOf(StatementContinuation),
                        ) +
                        sourcePatterns(
                            "CASE",
                            "CAST",
                            "COALESCE",
                            "COUNT",
                            "ELSE",
                            "END",
                            "FALSE",
                            "FILTER",
                            "FROM",
                            "NULL",
                            "OVER",
                            "THEN",
                            "TRUE",
                            "WHEN",
                            roles = arrayOf(AliasBoundary),
                        ) +
                        sourcePatterns(
                            "CROSS",
                            "EXCEPT",
                            "FULL",
                            "GROUP",
                            "HAVING",
                            "INNER",
                            "INTERSECT",
                            "JOIN",
                            "LEFT",
                            "LIMIT",
                            "OFFSET",
                            "ON",
                            "ORDER",
                            "RIGHT",
                            "UNION",
                            "USING",
                            "WHERE",
                            roles = arrayOf(TableReferenceBoundary),
                        ) +
                        sourcePatterns(
                            "CROSS",
                            "FULL",
                            "INNER",
                            "JOIN",
                            "LEFT",
                            "OUTER",
                            "RIGHT",
                            roles = arrayOf(JoinModifier),
                        ) +
                        sourcePatterns(
                            "BY",
                            "CROSS JOIN",
                            "EXCEPT",
                            "FETCH",
                            "FOR UPDATE",
                            "FROM",
                            "FULL [OUTER] JOIN",
                            "GROUP",
                            "GROUP BY",
                            "HAVING",
                            "INNER JOIN",
                            "INTERSECT",
                            "INTO",
                            "JOIN",
                            "LEFT [OUTER] JOIN",
                            "LIMIT",
                            "OFFSET",
                            "ON",
                            "ON CONFLICT",
                            "ORDER",
                            "ORDER BY",
                            "RETURNING",
                            "RIGHT [OUTER] JOIN",
                            "SET",
                            "UNION",
                            "USING",
                            "VALUES",
                            "WHERE",
                            "WINDOW",
                            roles = arrayOf(ClauseBoundary),
                        ) +
                        sourcePatterns(
                            "SELECT",
                            roles = arrayOf(SelectListStart),
                        ) +
                        sourcePatterns(
                            "AND",
                            "ELSE",
                            "OR",
                            "OVER",
                            "PARTITION",
                            "RANGE",
                            "ROWS",
                            "THEN",
                            "WHEN",
                            roles = arrayOf(ExpressionContinuation),
                        ) +
                        sourcePatterns(
                            "OR",
                            "ORDER",
                            "ORDER BY",
                            "PARTITION",
                            "PARTITION BY",
                            "RANGE",
                            "ROWS",
                            roles = arrayOf(ParenthesizedExpressionContinuation),
                        ),
            )

        /**
         * SQLite source scanner patterns.
         */
        public val SQLite: SqlDialectSourcePatterns =
            SqlDialectSourcePatterns(
                patterns =
                    SourceScannerDefault.patterns
                        .withoutExpressions("FULL", "RIGHT")
                        .withoutExpressions("FULL [OUTER] JOIN", "RIGHT [OUTER] JOIN"),
            )

        /**
         * MySQL source scanner patterns.
         */
        public val MySql: SqlDialectSourcePatterns =
            SqlDialectSourcePatterns(
                patterns =
                    SourceScannerDefault.patterns +
                        sourcePatterns("FOR", roles = arrayOf(TableReferenceBoundary, ClauseBoundary)),
            )

        /**
         * PostgreSQL source scanner patterns.
         */
        public val PostgreSql: SqlDialectSourcePatterns =
            SqlDialectSourcePatterns(
                patterns =
                    SourceScannerDefault.patterns +
                        sourcePatterns("FETCH", "FOR", roles = arrayOf(TableReferenceBoundary, ClauseBoundary)) +
                        sourcePatterns("FETCH {FIRST|NEXT}", roles = arrayOf(ClauseBoundary)),
            )

        /**
         * HSQL source scanner patterns.
         */
        public val Hsql: SqlDialectSourcePatterns =
            SqlDialectSourcePatterns(
                patterns =
                    SourceScannerDefault.patterns +
                        sourcePatterns("FETCH", roles = arrayOf(TableReferenceBoundary, ClauseBoundary)) +
                        sourcePatterns("FETCH {FIRST|NEXT}", roles = arrayOf(ClauseBoundary)),
            )
    }
}

private fun sourcePatterns(
    vararg expressions: String,
    roles: Array<SqlDialectSourcePatternRole>,
): Set<SqlDialectSourcePattern> =
    expressions
        .mapTo(mutableSetOf()) { expression ->
            SqlDialectSourcePattern.parse(expression, *roles)
        }

private fun Set<SqlDialectSourcePattern>.withoutExpressions(vararg expressions: String): Set<SqlDialectSourcePattern> {
    val removals = expressions.mapTo(mutableSetOf()) { expression -> SqlDialectSourcePatternExpression.parse(expression) }
    return filterTo(mutableSetOf()) { pattern -> pattern.expression !in removals }
}
