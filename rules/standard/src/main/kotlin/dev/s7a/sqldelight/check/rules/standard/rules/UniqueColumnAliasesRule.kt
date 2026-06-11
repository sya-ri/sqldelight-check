package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports duplicate SELECT result column aliases within the same SELECT list.
 *
 * Duplicate result aliases make generated row types harder to reason about and
 * can hide which expression a downstream reference reads.
 */
public class UniqueColumnAliasesRule : Rule {
    override val id: RuleId = RuleId("standard:unique-column-aliases")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.resultColumnAliases()
            .groupBy { alias -> alias.selectStartOffset }
            .values
            .forEach { aliases ->
                val seen = mutableSetOf<String>()
                aliases.forEach { alias ->
                    if (seen.add(alias.token.text.lowercase())) return@forEach
                    reporter.report(
                        Diagnostic(
                            ruleId = id,
                            severity = defaultSeverity,
                            message = "Column aliases should be unique within a SELECT list.",
                            file = context.file,
                            range = content.rangeAtOffsets(alias.token.startOffset, alias.token.endOffset),
                            database = context.database,
                        ),
                    )
                }
            }
    }
}
