package dev.s7a.sqldelight.check.dialects.postgres

import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.AliasBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ClauseBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.CommonFunctionName
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.DataTypeName
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.GroupByBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.JoinConditionBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.KeywordCaseTarget
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.OrderByBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.PredicateBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SqlDelightStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TableReferenceBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.sourcePatterns

/**
 * Source scanner patterns for SQLDelight's PostgreSQL dialect.
 */
public val PostgresDialectSourcePatterns: SqlDialectSourcePatterns =
    SqlDialectSourcePatterns(
        patterns =
            SqlDialectSourcePatterns.SourceScannerDefault.patterns +
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
                sourcePatterns("FETCH", "FOR", "RETURNING", roles = setOf(OrderByBoundary)) +
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
                sourcePatterns("ILIKE", "LATERAL", "MATERIALIZED", "OVERLAPS", "SIMILAR", "UNNEST", roles = setOf(AliasBoundary)) +
                sourcePatterns(
                    "ILIKE",
                    "LATERAL",
                    "MATERIALIZED",
                    "OVERLAPS",
                    "SIMILAR",
                    "TEMPORARY",
                    "UNLOGGED",
                    roles = setOf(KeywordCaseTarget),
                ) +
                sourcePatterns("DROP COLUMN", roles = setOf(ColumnDropOperation)) +
                sourcePatterns("RENAME COLUMN", roles = setOf(ColumnRenameOperation)) +
                sourcePatterns("RENAME TO", roles = setOf(TableRenameOperation)) +
                sourcePatterns(
                    "CREATE INDEX CONCURRENTLY",
                    "CREATE UNIQUE INDEX CONCURRENTLY",
                    roles = setOf(CreateConcurrentIndexStatementStart),
                ) +
                sourcePatterns("CONCURRENTLY", roles = setOf(ConcurrentlyClause)) +
                sourcePatterns("REINDEX", roles = setOf(ReindexStatementStart)) +
                sourcePatterns("SYSTEM", roles = setOf(ReindexSystemTarget)) +
                sourcePatterns("TYPE", roles = setOf(ColumnTypeChangeOperation)) +
                sourcePatterns("NOT VALID", roles = setOf(NotValidConstraintClause)) +
                sourcePatterns("BIGSERIAL", "SERIAL", "SMALLSERIAL", roles = setOf(SerialDataTypeName)) +
                sourcePatterns(
                    "CURRENT_TIMESTAMP",
                    "GEN_RANDOM_UUID",
                    "NOW",
                    "RANDOM",
                    "UUID_GENERATE_V4",
                    roles = setOf(VolatileDefaultFunction),
                ),
    )
