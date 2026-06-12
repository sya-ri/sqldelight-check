package dev.s7a.sqldelight.check.rules.sqlite.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.dialects.sqlite.SQLiteDialectId
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.dialects.sqlite.NonIntegerRowidPrimaryKeyType
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.PrimaryKeyConstraint
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.rule.api.SqlTokenMatch
import dev.s7a.sqldelight.check.rule.api.findSourcePattern
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports SQLite rowid primary keys that do not use the exact INTEGER PRIMARY KEY spelling.
 *
 * SQLite only gives rowid alias behavior to the exact `INTEGER PRIMARY KEY`
 * declaration.
 */
public class PreferIntegerPrimaryKeyRule : Rule {
    override val id: RuleId = RuleId("prefer-integer-primary-key")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetDialect: DialectId = SQLiteDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        reportSqlStatementMatches(
            context = context,
            reporter = reporter,
            message = "Use INTEGER PRIMARY KEY for SQLite rowid primary keys.",
        ) { statement ->
            statement.findNonIntegerPrimaryKey(context.database.dialect.sourcePatterns)
        }
    }
}

private fun List<SqlToken>.findNonIntegerPrimaryKey(sourcePatterns: SqlDialectSourcePatterns): SqlTokenMatch? =
    indices.firstNotNullOfOrNull { index ->
        val type = get(index)
        if (!sourcePatterns.matches(NonIntegerRowidPrimaryKeyType, listOf(type.normalizedText))) {
            return@firstNotNullOfOrNull null
        }
        val primaryKey = drop(index + 1).findSourcePattern(PrimaryKeyConstraint, sourcePatterns)
            ?: return@firstNotNullOfOrNull null
        if (primaryKey.startToken != getOrNull(index + 1)) return@firstNotNullOfOrNull null
        SqlTokenMatch(type, primaryKey.endToken)
    }
