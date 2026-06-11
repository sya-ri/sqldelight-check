package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.rule.api.isKeyword
import dev.s7a.sqldelight.check.rule.api.maskSqlCommentsAndQuotedText
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.rule.api.sqlStatements
import dev.s7a.sqldelight.check.rule.api.sqlTokens

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports PostgreSQL REINDEX statements that omit CONCURRENTLY.
 *
 * Concurrent reindexing avoids blocking writes for indexes that can be rebuilt
 * online.
 */
public class ReindexConcurrentlyRule : Rule {
    override val id: String = "reindex-concurrently"
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = DialectCapabilities.PostgreSql

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return

        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("reindex")) return@forEachIndexed
            if (tokens.reindexTargetKeywordAfter(index)?.isKeyword("system") == true) return@forEachIndexed
            if (tokens.statementTokensAfter(index).any { candidate -> candidate.isKeyword("concurrently") }) {
                return@forEachIndexed
            }

            reporter.report(
                Diagnostic(
                    ruleId = RuleId(id),
                    severity = defaultSeverity,
                    message = "Use REINDEX CONCURRENTLY for PostgreSQL reindex operations on live objects.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private fun List<SqlToken>.reindexTargetKeywordAfter(startIndex: Int): SqlToken? =
    statementTokensAfter(startIndex)
        .firstOrNull { token ->
            token.isKeyword("index") ||
                token.isKeyword("table") ||
                token.isKeyword("schema") ||
                token.isKeyword("database") ||
                token.isKeyword("system")
        }

private fun List<SqlToken>.statementTokensAfter(startIndex: Int): List<SqlToken> {
    val result = mutableListOf<SqlToken>()
    var index = startIndex + 1
    while (index < size && this[index].text != ";") {
        result += this[index]
        index++
    }
    return result
}
