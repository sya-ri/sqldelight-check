package dev.s7a.sqldelight.check.dialects.mysql

import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ClauseBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ColumnChangeOperation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ColumnDropOperation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ColumnModifyOperation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.CommonFunctionName
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.CopyAlgorithmClause
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.DataTypeName
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ExclusiveLockClause
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.IntegerDisplayWidthType
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.KeywordCaseTarget
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.LegacyUtf8CharsetDeclaration
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ReplaceIntoStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SqlDelightExecutableStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SqlDelightStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementContinuation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TableReferenceBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.sourcePatterns
import dev.s7a.sqldelight.check.api.withoutExpressions

/**
 * Source scanner patterns for SQLDelight's MySQL dialect.
 */
public val MySqlDialectSourcePatterns: SqlDialectSourcePatterns =
    SqlDialectSourcePatterns(
        patterns =
            SqlDialectSourcePatterns.SourceScannerDefault.patterns
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
                sourcePatterns("REPLACE", roles = setOf(SqlDelightExecutableStatementStart)) +
                sourcePatterns("REPLACE INTO", roles = setOf(ReplaceIntoStatementStart)) +
                sourcePatterns("INSERT REPLACE", "WITH REPLACE", roles = setOf(StatementContinuation)) +
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
                sourcePatterns("ALGORITHM COPY", roles = setOf(CopyAlgorithmClause)) +
                sourcePatterns("CHANGE COLUMN", roles = setOf(ColumnChangeOperation)) +
                sourcePatterns("DROP COLUMN", roles = setOf(ColumnDropOperation)) +
                sourcePatterns("MODIFY COLUMN", roles = setOf(ColumnModifyOperation)) +
                sourcePatterns("LOCK EXCLUSIVE", roles = setOf(ExclusiveLockClause)) +
                sourcePatterns("CHARACTER SET UTF8", "CHARSET UTF8", roles = setOf(LegacyUtf8CharsetDeclaration)) +
                sourcePatterns(
                    "BIGINT",
                    "INT",
                    "INTEGER",
                    "MEDIUMINT",
                    "SMALLINT",
                    "TINYINT",
                    roles = setOf(IntegerDisplayWidthType),
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
