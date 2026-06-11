package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.rule.api.isKeyword
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.rule.api.sqlTokens

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports PostgreSQL DDL patterns that can take strong locks.
 *
 * The rule highlights migration statements that commonly need an online
 * migration strategy.
 */
public class ExcessiveLocksRule : Rule {
    override val id: RuleId = RuleId("excessive-locks")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = DialectCapability.PostgreSql

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return

        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("create")) return@forEachIndexed

            val indexToken = tokens.createIndexToken(index) ?: return@forEachIndexed
            val nextToken = tokens.getOrNull(tokens.indexOf(indexToken) + 1)
            if (nextToken?.isKeyword("concurrently") == true) return@forEachIndexed

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Use CREATE INDEX CONCURRENTLY for PostgreSQL indexes that may be built on live tables.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, indexToken.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private fun List<SqlToken>.createIndexToken(createIndex: Int): SqlToken? {
    val first = getOrNull(createIndex + 1) ?: return null
    if (first.isKeyword("index")) return first
    if (!first.isKeyword("unique")) return null
    val second = getOrNull(createIndex + 2) ?: return null
    return if (second.isKeyword("index")) second else null
}
