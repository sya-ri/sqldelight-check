package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

/**
 * Reports query labels whose leading verb does not match the labeled statement.
 */
public class QueryLabelMatchesOperationRule : Rule {
    override val id: RuleId = RuleId("query-label-matches-operation")
    override val defaultSeverity: Severity = Severity.Info
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Query) return

        val content = context.file.content
        content.sharedSqlDelightLabels().forEach { label ->
            val operation = content.operationAfter(label.bodyStartOffset) ?: return@forEach
            if (label.name.startsWithAny(operation.expectedPrefixes)) return@forEach

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "SQLDelight label '${label.name}' should start with ${operation.expectedPrefixes.joinToString(" or ")}.",
                    file = context.file,
                    range = content.rangeAtOffsets(label.startOffset, label.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private data class SqlDelightOperation(
    val term: SqlDialectSourceTerm,
    val expectedPrefixes: Set<String>,
)

private fun String.operationAfter(offset: Int): SqlDelightOperation? =
    sqlTokens()
        .dropWhile { token -> token.startOffset < offset }
        .firstNotNullOfOrNull { token ->
            val term = operationPrefixTerms.keys.firstOrNull { term -> token.isTerm(term) }
            if (term == null || sqlParenthesisDepthAt(token.startOffset) != 0) {
                null
            } else {
                SqlDelightOperation(term = term, expectedPrefixes = operationPrefixTerms.getValue(term))
            }
        }

private fun String.startsWithAny(prefixes: Set<String>): Boolean = prefixes.any { prefix -> startsWith(prefix) }

private val operationPrefixTerms =
    mapOf(
        SqlDialectSourceTerm.Select to setOf("select", "find", "get", "list", "count", "exists"),
        SqlDialectSourceTerm.Insert to setOf("insert", "add", "create", "upsert"),
        SqlDialectSourceTerm.Update to setOf("update", "set", "upsert"),
        SqlDialectSourceTerm.Delete to setOf("delete", "remove"),
    )
