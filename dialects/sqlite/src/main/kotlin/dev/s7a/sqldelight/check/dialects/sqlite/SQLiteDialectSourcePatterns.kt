package dev.s7a.sqldelight.check.dialects.sqlite

import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.AutoincrementKeyword
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ComplexAlterTableOperation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.DataTypeName
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.DoUpdateClause
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ForeignKeysOffValue
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ForeignKeysOnValue
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ForeignKeysPragmaStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.InsertOrReplaceStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.KeywordCaseTarget
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.NonIntegerRowidPrimaryKeyType
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.OnConflictClause
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ReplaceIntoStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SqlDelightExecutableStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SqlDelightStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementContinuation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.WithoutRowidClause
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.sourcePatterns
import dev.s7a.sqldelight.check.api.withoutExpressions

/**
 * Source scanner patterns for SQLDelight's SQLite dialects.
 */
public val SQLiteDialectSourcePatterns: SqlDialectSourcePatterns =
    SqlDialectSourcePatterns(
        patterns =
            SqlDialectSourcePatterns.SourceScannerDefault.patterns
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
                sourcePatterns("REPLACE", roles = setOf(SqlDelightExecutableStatementStart)) +
                sourcePatterns("REPLACE INTO", roles = setOf(ReplaceIntoStatementStart)) +
                sourcePatterns("INSERT REPLACE", "WITH REPLACE", roles = setOf(StatementContinuation)) +
                sourcePatterns("INSERT OR REPLACE", roles = setOf(InsertOrReplaceStatementStart)) +
                sourcePatterns("ON CONFLICT", roles = setOf(OnConflictClause)) +
                sourcePatterns("DO UPDATE", roles = setOf(DoUpdateClause)) +
                sourcePatterns("PRAGMA FOREIGN_KEYS", roles = setOf(ForeignKeysPragmaStatementStart)) +
                sourcePatterns("OFF", roles = setOf(ForeignKeysOffValue)) +
                sourcePatterns("ON", roles = setOf(ForeignKeysOnValue)) +
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
                    "ALTER COLUMN",
                    "ADD CONSTRAINT",
                    "DROP CONSTRAINT",
                    roles = setOf(ComplexAlterTableOperation),
                ) +
                sourcePatterns("AUTOINCREMENT", roles = setOf(AutoincrementKeyword)) +
                sourcePatterns("BIGINT", "INT", "LONG", roles = setOf(NonIntegerRowidPrimaryKeyType)) +
                sourcePatterns("WITHOUT ROWID", roles = setOf(WithoutRowidClause)) +
                sourcePatterns("FTS5", "JSON", roles = setOf(DataTypeName)),
    )
