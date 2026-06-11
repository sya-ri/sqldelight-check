package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports SQLDelight model-bound INSERT statements.
 */
public class AvoidModelBoundInsertForPublicApiRule : Rule {
    override val id: RuleId = RuleId("standard:avoid-model-bound-insert-for-public-api")
    override val defaultSeverity: Severity = Severity.Info
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!context.file.path.endsWith(".sq")) return

        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("insert")) return@forEachIndexed
            val into = tokens.getOrNull(index + 1)?.takeIf { it.isKeyword("into") } ?: return@forEachIndexed
            tokens.getOrNull(index + 2) ?: return@forEachIndexed
            val values = tokens.firstKeywordAfter(index + 3, content.statementEndAfter(token.startOffset), "values") ?: return@forEachIndexed
            val parameter = content.nextSqlCharacterAfter(values.endOffset) ?: return@forEachIndexed
            if (parameter.value != '?') return@forEachIndexed

            reporter.report(
                Diagnostic(
                    ruleId = id,
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
