package dev.s7a.sqldelight.check.api

import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.AliasBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.BooleanOperator
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ClauseBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.CoalesceAlternativeFunction
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ColumnConstraintStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.CommonFunctionName
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ExpressionContinuation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.GroupByBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.JoinConditionBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.JoinModifier
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.KeywordCaseTarget
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.MajorClauseStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.OrderByBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ParenthesizedExpressionContinuation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.PredicateBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.PredicateStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.IndexUnfriendlyFunction
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SelectListStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SetOperator
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SqlDelightExecutableStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SqlDelightStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementContinuation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TableReferenceBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TableConstraintStart

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
                            "DELETE",
                            "INSERT",
                            "SELECT",
                            "UPDATE",
                            roles = arrayOf(SqlDelightExecutableStatementStart),
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
                            "NATURAL",
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
                            "NATURAL",
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
                            "WHERE",
                            "HAVING",
                            "ON",
                            roles = arrayOf(PredicateStart),
                        ) +
                        sourcePatterns(
                            "FROM",
                            "GROUP BY",
                            "HAVING",
                            "LIMIT",
                            "OFFSET",
                            "ORDER BY",
                            "WHERE",
                            roles = arrayOf(MajorClauseStart),
                        ) +
                        sourcePatterns(
                            "EXCEPT",
                            "FETCH",
                            "GROUP",
                            "GROUP BY",
                            "HAVING",
                            "INTERSECT",
                            "LIMIT",
                            "OFFSET",
                            "ORDER",
                            "ORDER BY",
                            "UNION",
                            "WHERE",
                            "WINDOW",
                            roles = arrayOf(PredicateBoundary),
                        ) +
                        sourcePatterns(
                            "CROSS JOIN",
                            "EXCEPT",
                            "FETCH",
                            "FULL [OUTER] JOIN",
                            "GROUP",
                            "GROUP BY",
                            "HAVING",
                            "INNER JOIN",
                            "INTERSECT",
                            "JOIN",
                            "LEFT [OUTER] JOIN",
                            "LIMIT",
                            "NATURAL JOIN",
                            "NATURAL LEFT [OUTER] JOIN",
                            "OFFSET",
                            "ORDER",
                            "ORDER BY",
                            "RIGHT [OUTER] JOIN",
                            "UNION",
                            "WHERE",
                            "WINDOW",
                            roles = arrayOf(JoinConditionBoundary),
                        ) +
                        sourcePatterns(
                            "AND",
                            "OR",
                            roles = arrayOf(BooleanOperator),
                        ) +
                        sourcePatterns(
                            "EXCEPT",
                            "INTERSECT",
                            "UNION",
                            roles = arrayOf(SetOperator),
                        ) +
                        sourcePatterns(
                            "CHECK",
                            "COLLATE",
                            "CONSTRAINT",
                            "DEFAULT",
                            "GENERATED",
                            "NOT NULL",
                            "NULL",
                            "PRIMARY KEY",
                            "REFERENCES",
                            "UNIQUE",
                            roles = arrayOf(ColumnConstraintStart),
                        ) +
                        sourcePatterns(
                            "CHECK",
                            "CONSTRAINT",
                            "FOREIGN KEY",
                            "PRIMARY KEY",
                            "UNIQUE",
                            roles = arrayOf(TableConstraintStart),
                        ) +
                        sourcePatterns(
                            "EXCEPT",
                            "HAVING",
                            "INTERSECT",
                            "LIMIT",
                            "OFFSET",
                            "ORDER",
                            "ORDER BY",
                            "UNION",
                            "WHERE",
                            "WINDOW",
                            roles = arrayOf(GroupByBoundary),
                        ) +
                        sourcePatterns(
                            "EXCEPT",
                            "FETCH",
                            "INTERSECT",
                            "LIMIT",
                            "OFFSET",
                            "UNION",
                            "WHERE",
                            roles = arrayOf(OrderByBoundary),
                        ) +
                        sourcePatterns(
                            "ADD",
                            "ALTER",
                            "AND",
                            "AS",
                            "ASC",
                            "BETWEEN",
                            "BY",
                            "CASE",
                            "CHECK",
                            "COLUMN",
                            "CONSTRAINT",
                            "CREATE",
                            "DEFAULT",
                            "DELETE",
                            "DESC",
                            "DISTINCT",
                            "DROP",
                            "ELSE",
                            "END",
                            "EXISTS",
                            "FOREIGN",
                            "FROM",
                            "GROUP",
                            "HAVING",
                            "IN",
                            "INDEX",
                            "INNER",
                            "INSERT",
                            "INTO",
                            "IS",
                            "JOIN",
                            "KEY",
                            "LEFT",
                            "LIKE",
                            "LIMIT",
                            "NOT",
                            "ON",
                            "OR",
                            "ORDER",
                            "OUTER",
                            "PRIMARY",
                            "REFERENCES",
                            "RIGHT",
                            "SELECT",
                            "SET",
                            "TABLE",
                            "THEN",
                            "UNION",
                            "UNIQUE",
                            "UPDATE",
                            "VALUES",
                            "WHEN",
                            "WHERE",
                            roles = arrayOf(KeywordCaseTarget),
                        ) +
                        sourcePatterns(
                            "ABS",
                            "AVG",
                            "COALESCE",
                            "COUNT",
                            "DATE",
                            "DATETIME",
                            "GROUP_CONCAT",
                            "HEX",
                            "IFNULL",
                            "INSTR",
                            "JSON_EXTRACT",
                            "LENGTH",
                            "LOWER",
                            "LTRIM",
                            "MAX",
                            "MIN",
                            "NULLIF",
                            "RANDOM",
                            "REPLACE",
                            "ROUND",
                            "RTRIM",
                            "STRFTIME",
                            "SUBSTR",
                            "SUBSTRING",
                            "SUM",
                            "TIME",
                            "TRIM",
                            "TYPEOF",
                            "UPPER",
                            roles = arrayOf(CommonFunctionName),
                        ) +
                        sourcePatterns(
                            "IFNULL",
                            "NVL",
                            roles = arrayOf(CoalesceAlternativeFunction),
                        ) +
                        sourcePatterns(
                            "COALESCE",
                            "DATE",
                            "IFNULL",
                            "LOWER",
                            "SUBSTR",
                            "SUBSTRING",
                            "TRIM",
                            "UPPER",
                            roles = arrayOf(IndexUnfriendlyFunction),
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
