package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports SQLDelight model-bound INSERT statements.
 */
public class AvoidModelBoundInsertForPublicApiRule : Rule {
    override val id: RuleId = RuleId("avoid-model-bound-insert-for-public-api")
    override val defaultSeverity: Severity = Severity.Info
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Query) return

        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.Insert)) return@forEachIndexed
            val into = tokens.getOrNull(index + 1)?.takeIf { it.isTerm(SqlDialectSourceTerm.Into) } ?: return@forEachIndexed
            tokens.getOrNull(index + 2) ?: return@forEachIndexed
            val values =
                tokens.firstTermAfter(index + 3, content.statementEndAfter(token.startOffset), SqlDialectSourceTerm.Values)
                    ?: return@forEachIndexed
            val parameter = content.nextSqlCharacterAfter(values.endOffset) ?: return@forEachIndexed
            if (parameter.value != '?') return@forEachIndexed

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Prefer an explicit INSERT column list and named values over model-bound VALUES ?.",
                    file = context.file,
                    range = content.rangeAtOffsets(into.startOffset, parameter.offset + 1),
                    database = context.database,
                ),
            )
        }
    }
}
