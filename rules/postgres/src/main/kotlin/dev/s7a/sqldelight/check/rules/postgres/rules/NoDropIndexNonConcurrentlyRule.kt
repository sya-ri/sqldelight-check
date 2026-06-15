package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.dialects.postgres.PostgresDialectId
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.rule.api.isKeyword
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.sqlStatements
import dev.s7a.sqldelight.check.rule.api.sqlTokens

/**
 * Reports PostgreSQL DROP INDEX statements that omit CONCURRENTLY.
 */
public class NoDropIndexNonConcurrentlyRule : Rule {
    override val id: RuleId = RuleId("no-drop-index-non-concurrently")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetDialect: DialectId = PostgresDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Migration) return

        val content = context.file.content
        content.sqlTokens()
            .toList()
            .sqlStatements()
            .filter { statement -> statement.startsWithKeywords("drop", "index") }
            .filterNot { statement -> statement.any { token -> token.isKeyword("concurrently") } }
            .forEach { statement ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Use DROP INDEX CONCURRENTLY for PostgreSQL indexes that may be dropped from live tables.",
                        file = context.file,
                        range = content.rangeAtOffsets(statement[0].startOffset, statement[1].endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private fun List<SqlToken>.startsWithKeywords(vararg keywords: String): Boolean =
    size >= keywords.size && keywords.indices.all { index -> get(index).isKeyword(keywords[index]) }
