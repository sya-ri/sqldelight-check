package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports `OFFSET` pagination, which should usually be replaced with keyset pagination.
 */
public class NoOffsetPaginationRule : Rule {
    override val id: String = "no-offset-pagination"
    override val defaultSeverity: Severity = Severity.Info
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.sqlTokens()
            .filter { token ->
                token.isKeyword("offset") &&
                    content.sqlParenthesisDepthAt(token.startOffset) == 0 &&
                    content.previousSqlCharacterBefore(token.startOffset)?.value != ':'
            }
            .forEach { token ->
                reporter.report(
                    Diagnostic(
                        ruleId = RuleId(id),
                        severity = defaultSeverity,
                        message = "Prefer keyset pagination over OFFSET pagination for stable and scalable paging.",
                        file = context.file,
                        range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}
