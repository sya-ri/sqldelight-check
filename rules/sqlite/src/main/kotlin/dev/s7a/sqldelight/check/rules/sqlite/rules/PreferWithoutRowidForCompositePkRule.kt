package dev.s7a.sqldelight.check.rules.sqlite.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.CreateTableStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.PrimaryKeyConstraint
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.WithoutRowidClause
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.rule.api.SqlTokenMatch
import dev.s7a.sqldelight.check.rule.api.containsSourcePattern
import dev.s7a.sqldelight.check.rule.api.findSourcePattern
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports SQLite composite primary key tables that can consider WITHOUT ROWID.
 *
 * WITHOUT ROWID can reduce duplicate primary-key storage for some composite-key
 * SQLite tables.
 */
public class PreferWithoutRowidForCompositePkRule : Rule {
    override val id: RuleId = RuleId("prefer-without-rowid-for-composite-pk")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = DialectCapability.SQLite

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        reportSqlStatementMatches(
            context = context,
            reporter = reporter,
            message = "Consider WITHOUT ROWID for SQLite tables with composite primary keys.",
        ) { statement ->
            statement.findCompositePrimaryKeyCreateTable(content, context.database.dialect.sourcePatterns)
        }
    }
}

private fun List<SqlToken>.findCompositePrimaryKeyCreateTable(
    content: String,
    sourcePatterns: SqlDialectSourcePatterns,
): SqlTokenMatch? {
    val statementStart = findSourcePattern(CreateTableStatementStart, sourcePatterns) ?: return null
    if (containsSourcePattern(WithoutRowidClause, sourcePatterns)) return null
    val primaryKey = findSourcePattern(PrimaryKeyConstraint, sourcePatterns) ?: return null
    if (!content.hasCommaInParenthesesAfter(primaryKey.endToken.endOffset)) return null
    return SqlTokenMatch(statementStart.startToken, primaryKey.endToken)
}

private fun String.hasCommaInParenthesesAfter(offset: Int): Boolean {
    val openOffset = indexOf('(', startIndex = offset)
    if (openOffset == -1) return false
    var depth = 0
    var index = openOffset
    while (index < length) {
        when (this[index]) {
            '(' -> depth++
            ')' -> {
                depth--
                if (depth == 0) return false
            }
            ',' -> if (depth == 1) return true
            ';' -> if (depth == 0) return false
        }
        index++
    }
    return false
}
