package dev.s7a.sqldelight.check.dialects.hsql

import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ClauseBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.DataTypeName
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.GroupByBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.JoinConditionBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.KeywordCaseTarget
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.OrderByBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.PredicateBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SqlDelightExecutableStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SqlDelightStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementContinuation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TableReferenceBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.sourcePatterns
import dev.s7a.sqldelight.check.api.withoutExpressions

/**
 * Source scanner patterns for SQLDelight's HSQL dialect.
 */
public val HsqlDialectSourcePatterns: SqlDialectSourcePatterns =
    SqlDialectSourcePatterns(
        patterns =
            SqlDialectSourcePatterns.SourceScannerDefault.patterns
                .withoutExpressions("ON CONFLICT") +
                sourcePatterns(
                    "CALL",
                    "MERGE",
                    "SCRIPT",
                    "SHUTDOWN",
                    "TRUNCATE",
                    roles = setOf(StatementStart, SqlDelightStatementStart),
                ) +
                sourcePatterns("MERGE", roles = setOf(SqlDelightExecutableStatementStart)) +
                sourcePatterns("WITH MERGE", roles = setOf(StatementContinuation)) +
                sourcePatterns("FETCH", roles = setOf(TableReferenceBoundary, ClauseBoundary)) +
                sourcePatterns(
                    "FETCH {FIRST|NEXT} [ROW|ROWS]",
                    "MERGE INTO",
                    "WHEN MATCHED",
                    "WHEN NOT MATCHED",
                    roles = setOf(ClauseBoundary),
                ) +
                sourcePatterns(
                    "BACKUP DATABASE",
                    "CHECKPOINT",
                    "PERFORM EXPORT",
                    "PERFORM IMPORT",
                    "SCRIPT",
                    "SHUTDOWN",
                    roles = setOf(SystemOperationStatement),
                ) +
                sourcePatterns("SET DATABASE", "SET FILES", roles = setOf(DatabaseFileSettingStatement)) +
                sourcePatterns("CREATE TEXT TABLE", roles = setOf(TextTableSourceStatement)) +
                sourcePatterns("SET TABLE", roles = setOf(TextTableSourceBindingStart)) +
                sourcePatterns("SOURCE", roles = setOf(TextTableSourceClause)) +
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
                sourcePatterns("MERGE", "MATCHED", "NEXT", "ROW", "ROWS", roles = setOf(KeywordCaseTarget)),
    )
