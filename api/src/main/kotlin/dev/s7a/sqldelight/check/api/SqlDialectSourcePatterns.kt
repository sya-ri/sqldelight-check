package dev.s7a.sqldelight.check.api

import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.AliasBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.BooleanOperator
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ClauseBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.CoalesceAlternativeFunction
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ColumnConstraintStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.CommonFunctionName
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.DataTypeName
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ExpressionContinuation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.GroupByBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.IndexUnfriendlyFunction
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.JoinConditionBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.JoinModifier
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.KeywordCaseTarget
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.MajorClauseStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.OrderByBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ParenthesizedExpressionContinuation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.PredicateBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.PredicateStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SelectListStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SetOperator
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SqlDelightExecutableStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SqlDelightMappableStorageTypeName
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
 *
 * Dialect presets intentionally describe broad dialect-family syntax rather
 * than exact engine-version validity. SQLDelight's parser is responsible for
 * accepting or rejecting concrete SQL. These patterns only help source scanners
 * avoid misreading supported-looking syntax while linting.
 */
public class SqlDialectSourcePatterns(
    public val patterns: Set<SqlDialectSourcePattern> = SourceScannerDefault.patterns,
    public val blockPatterns: SqlDialectSourceBlockPatterns = SqlDialectSourceBlockPatterns.Default,
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
            patterns == other.patterns &&
            blockPatterns == other.blockPatterns

    override fun hashCode(): Int = 31 * patterns.hashCode() + blockPatterns.hashCode()

    override fun toString(): String =
        "SqlDialectSourcePatterns(patterns=$patterns, blockPatterns=$blockPatterns)"

    public companion object {
        /**
         * Conservative fallback used by the source scanner when no known dialect preset applies.
         *
         * This is a broad SQL baseline, not a validation grammar.
         */
        public val SourceScannerDefault: SqlDialectSourcePatterns =
            SqlDialectSourcePatterns(
                patterns =
                    sourcePatterns(
                        "ALTER",
                        roles = setOf(StatementStart, SqlDelightStatementStart),
                    ) +
                        sourcePatterns(
                            "CREATE",
                            "DELETE",
                            "INSERT",
                            "SELECT",
                            "UPDATE",
                            "WITH",
                            roles = setOf(StatementStart, SqlDelightStatementStart),
                        ) +
                        sourcePatterns(
                            "DELETE",
                            "INSERT",
                            "SELECT",
                            "UPDATE",
                            roles = setOf(SqlDelightExecutableStatementStart),
                        ) +
                        sourcePatterns(
                            "DROP",
                            roles = setOf(StatementStart, SqlDelightStatementStart),
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
                            roles = setOf(StatementContinuation),
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
                            roles = setOf(AliasBoundary),
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
                            roles = setOf(TableReferenceBoundary),
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
                            roles = setOf(JoinModifier),
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
                            roles = setOf(ClauseBoundary),
                        ) +
                        sourcePatterns(
                            "WHERE",
                            "HAVING",
                            "ON",
                            roles = setOf(PredicateStart),
                        ) +
                        sourcePatterns(
                            "FROM",
                            "GROUP BY",
                            "HAVING",
                            "LIMIT",
                            "OFFSET",
                            "ORDER BY",
                            "WHERE",
                            roles = setOf(MajorClauseStart),
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
                            roles = setOf(PredicateBoundary),
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
                            roles = setOf(JoinConditionBoundary),
                        ) +
                        sourcePatterns(
                            "AND",
                            "OR",
                            roles = setOf(BooleanOperator),
                        ) +
                        sourcePatterns(
                            "EXCEPT",
                            "INTERSECT",
                            "UNION",
                            roles = setOf(SetOperator),
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
                            roles = setOf(ColumnConstraintStart),
                        ) +
                        sourcePatterns(
                            "CHECK",
                            "CONSTRAINT",
                            "FOREIGN KEY",
                            "PRIMARY KEY",
                            "UNIQUE",
                            roles = setOf(TableConstraintStart),
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
                            roles = setOf(GroupByBoundary),
                        ) +
                        sourcePatterns(
                            "EXCEPT",
                            "FETCH",
                            "INTERSECT",
                            "LIMIT",
                            "OFFSET",
                            "UNION",
                            "WHERE",
                            roles = setOf(OrderByBoundary),
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
                            roles = setOf(KeywordCaseTarget),
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
                            roles = setOf(CommonFunctionName),
                        ) +
                        sourcePatterns(
                            "IFNULL",
                            "NVL",
                            roles = setOf(CoalesceAlternativeFunction),
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
                            roles = setOf(IndexUnfriendlyFunction),
                        ) +
                        sourcePatterns(
                            "BIGINT",
                            "BLOB",
                            "BOOL",
                            "BOOLEAN",
                            "CHAR",
                            "CLOB",
                            "DECIMAL",
                            "DOUBLE",
                            "FLOAT",
                            "INT",
                            "INTEGER",
                            "NUMERIC",
                            "REAL",
                            "SMALLINT",
                            "TEXT",
                            "TIMESTAMP",
                            "VARCHAR",
                            roles = setOf(DataTypeName),
                        ) +
                        sourcePatterns(
                            "ANY",
                            "BLOB",
                            "BOOLEAN",
                            "CHAR",
                            "CLOB",
                            "DATE",
                            "DATETIME",
                            "DECIMAL",
                            "DOUBLE",
                            "FLOAT",
                            "INT",
                            "INTEGER",
                            "NUMERIC",
                            "REAL",
                            "TEXT",
                            "TIME",
                            "TIMESTAMP",
                            "VARCHAR",
                            roles = setOf(SqlDelightMappableStorageTypeName),
                        ) +
                        sourcePatterns(
                            "SELECT",
                            roles = setOf(SelectListStart),
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
                            roles = setOf(ExpressionContinuation),
                        ) +
                        sourcePatterns(
                            "OR",
                            "ORDER",
                            "ORDER BY",
                            "PARTITION",
                            "PARTITION BY",
                            "RANGE",
                            "ROWS",
                            roles = setOf(ParenthesizedExpressionContinuation),
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
                        .withoutExpressions("FULL [OUTER] JOIN", "RIGHT [OUTER] JOIN") +
                        sourcePatterns(
                            "ANALYZE",
                            "ATTACH",
                            "BEGIN",
                            "COMMIT",
                            "DETACH",
                            "END",
                            "EXPLAIN",
                            "PRAGMA",
                            "REINDEX",
                            "RELEASE",
                            "REPLACE",
                            "ROLLBACK",
                            "SAVEPOINT",
                            "VACUUM",
                            roles = setOf(StatementStart, SqlDelightStatementStart),
                        ) +
                        sourcePatterns(
                            "REPLACE",
                            roles = setOf(SqlDelightExecutableStatementStart),
                        ) +
                        sourcePatterns(
                            "INSERT REPLACE",
                            "WITH REPLACE",
                            roles = setOf(StatementContinuation),
                        ) +
                        sourcePatterns(
                            "CONFLICT",
                            "DO",
                            "EXCLUDED",
                            "FAIL",
                            "IGNORE",
                            "REPLACE",
                            "ROLLBACK",
                            roles = setOf(KeywordCaseTarget),
                        ) +
                        sourcePatterns(
                            "FTS5",
                            "JSON",
                            roles = setOf(DataTypeName),
                        ),
            )

        /**
         * MySQL source scanner patterns.
         */
        public val MySql: SqlDialectSourcePatterns =
            SqlDialectSourcePatterns(
                patterns =
                    SourceScannerDefault.patterns
                        .withoutExpressions("FULL", "FULL [OUTER] JOIN", "ON CONFLICT") +
                        sourcePatterns(
                            "ANALYZE",
                            "CALL",
                            "DESCRIBE",
                            "EXPLAIN",
                            "OPTIMIZE",
                            "REPLACE",
                            "SHOW",
                            "TRUNCATE",
                            "USE",
                            roles = setOf(StatementStart, SqlDelightStatementStart),
                        ) +
                        sourcePatterns(
                            "REPLACE",
                            roles = setOf(SqlDelightExecutableStatementStart),
                        ) +
                        sourcePatterns(
                            "INSERT REPLACE",
                            "WITH REPLACE",
                            roles = setOf(StatementContinuation),
                        ) +
                        sourcePatterns("FOR", roles = setOf(TableReferenceBoundary, ClauseBoundary)) +
                        sourcePatterns(
                            "FOR SHARE",
                            "FOR UPDATE",
                            "LOCK IN SHARE MODE",
                            "ON DUPLICATE KEY UPDATE",
                            roles = setOf(ClauseBoundary),
                        ) +
                        sourcePatterns(
                            "DUPLICATE",
                            "KEY",
                            "LOCK",
                            "REPLACE",
                            "SHARE",
                            "UNSIGNED",
                            "ZEROFILL",
                            roles = setOf(KeywordCaseTarget),
                        ) +
                        sourcePatterns(
                            "BIT",
                            "DATE",
                            "DATETIME",
                            "ENUM",
                            "JSON",
                            "LONGBLOB",
                            "LONGTEXT",
                            "MEDIUMBLOB",
                            "MEDIUMINT",
                            "MEDIUMTEXT",
                            "TINYBLOB",
                            "TINYINT",
                            "TINYTEXT",
                            "TIME",
                            "VARBINARY",
                            "YEAR",
                            roles = setOf(DataTypeName),
                        ) +
                        sourcePatterns(
                            "CONCAT",
                            "CURDATE",
                            "CURRENT_DATE",
                            "CURRENT_TIME",
                            "CURRENT_TIMESTAMP",
                            "DATE_FORMAT",
                            "IF",
                            "NOW",
                            "RAND",
                            roles = setOf(CommonFunctionName),
                        ),
            )

        /**
         * PostgreSQL source scanner patterns.
         */
        public val PostgreSql: SqlDialectSourcePatterns =
            SqlDialectSourcePatterns(
                patterns =
                    SourceScannerDefault.patterns +
                        sourcePatterns(
                            "ANALYZE",
                            "CALL",
                            "COMMENT",
                            "COPY",
                            "EXPLAIN",
                            "GRANT",
                            "LISTEN",
                            "NOTIFY",
                            "RESET",
                            "REVOKE",
                            "SET",
                            "SHOW",
                            "TRUNCATE",
                            "VACUUM",
                            roles = setOf(StatementStart, SqlDelightStatementStart),
                        ) +
                        sourcePatterns("FETCH", "FOR", roles = setOf(TableReferenceBoundary, ClauseBoundary)) +
                        sourcePatterns(
                            "DISTINCT ON",
                            "FETCH {FIRST|NEXT} [ROW|ROWS]",
                            "FOR KEY SHARE",
                            "FOR NO KEY UPDATE",
                            "FOR SHARE",
                            "FOR UPDATE",
                            "ON CONFLICT",
                            roles = setOf(ClauseBoundary),
                        ) +
                        sourcePatterns(
                            "FETCH",
                            "FOR",
                            "RETURNING",
                            roles = setOf(OrderByBoundary),
                        ) +
                        sourcePatterns(
                            "FETCH",
                            "FOR",
                            "RETURNING",
                            roles = setOf(GroupByBoundary, PredicateBoundary, JoinConditionBoundary),
                        ) +
                        sourcePatterns(
                            "BIGSERIAL",
                            "BYTEA",
                            "CIDR",
                            "INET",
                            "INTERVAL",
                            "JSON",
                            "JSONB",
                            "MONEY",
                            "SERIAL",
                            "SMALLSERIAL",
                            "TIME",
                            "TIMESTAMPTZ",
                            "UUID",
                            "XML",
                            roles = setOf(DataTypeName),
                        ) +
                        sourcePatterns(
                            "ARRAY_AGG",
                            "CURRENT_DATE",
                            "CURRENT_TIME",
                            "CURRENT_TIMESTAMP",
                            "EXTRACT",
                            "JSON_AGG",
                            "JSON_BUILD_OBJECT",
                            "JSON_OBJECT_AGG",
                            "NOW",
                            "STRING_AGG",
                            "UNNEST",
                            roles = setOf(CommonFunctionName),
                        ) +
                        sourcePatterns(
                            "ILIKE",
                            "LATERAL",
                            "MATERIALIZED",
                            "OVERLAPS",
                            "SIMILAR",
                            "UNNEST",
                            roles = setOf(AliasBoundary),
                        ) +
                        sourcePatterns(
                            "ILIKE",
                            "LATERAL",
                            "MATERIALIZED",
                            "OVERLAPS",
                            "SIMILAR",
                            "TEMPORARY",
                            "UNLOGGED",
                            roles = setOf(KeywordCaseTarget),
                        ),
            )

        /**
         * HSQL source scanner patterns.
         */
        public val Hsql: SqlDialectSourcePatterns =
            SqlDialectSourcePatterns(
                patterns =
                    SourceScannerDefault.patterns
                        .withoutExpressions("ON CONFLICT") +
                        sourcePatterns(
                            "CALL",
                            "MERGE",
                            "SCRIPT",
                            "SHUTDOWN",
                            "TRUNCATE",
                            roles = setOf(StatementStart, SqlDelightStatementStart),
                        ) +
                        sourcePatterns(
                            "MERGE",
                            roles = setOf(SqlDelightExecutableStatementStart),
                        ) +
                        sourcePatterns(
                            "WITH MERGE",
                            roles = setOf(StatementContinuation),
                        ) +
                        sourcePatterns("FETCH", roles = setOf(TableReferenceBoundary, ClauseBoundary)) +
                        sourcePatterns(
                            "FETCH {FIRST|NEXT} [ROW|ROWS]",
                            "MERGE INTO",
                            "WHEN MATCHED",
                            "WHEN NOT MATCHED",
                            roles = setOf(ClauseBoundary),
                        ) +
                        sourcePatterns(
                            "FETCH",
                            roles = setOf(OrderByBoundary, GroupByBoundary, PredicateBoundary, JoinConditionBoundary),
                        ) +
                        sourcePatterns(
                            "IDENTITY",
                            "INTERVAL",
                            "LONGVARBINARY",
                            "LONGVARCHAR",
                            "TIME",
                            "TINYINT",
                            "UUID",
                            roles = setOf(DataTypeName),
                        ) +
                        sourcePatterns(
                            "MERGE",
                            "MATCHED",
                            "NEXT",
                            "ROW",
                            "ROWS",
                            roles = setOf(KeywordCaseTarget),
                        ),
            )
    }
}

/**
 * Parses source pattern [expressions] and attaches [roles] to each pattern.
 *
 * This is intended for dialect presets and custom dialect definitions that
 * extend or replace [SqlDialectSourcePatterns].
 */
public fun sourcePatterns(
    vararg expressions: String,
    roles: Set<SqlDialectSourcePatternRole>,
): Set<SqlDialectSourcePattern> =
    expressions
        .mapTo(mutableSetOf()) { expression ->
            SqlDialectSourcePattern(
                expression = SqlDialectSourcePatternExpression.parse(expression),
                roles = roles,
            )
        }

/**
 * Removes patterns whose expression exactly matches one of [expressions].
 *
 * This is useful for dialects that start from an existing preset and subtract
 * syntax that the target engine does not support.
 */
public fun Set<SqlDialectSourcePattern>.withoutExpressions(vararg expressions: String): Set<SqlDialectSourcePattern> {
    val removals = expressions.mapTo(mutableSetOf()) { expression -> SqlDialectSourcePatternExpression.parse(expression) }
    return filterTo(mutableSetOf()) { pattern -> pattern.expression !in removals }
}
