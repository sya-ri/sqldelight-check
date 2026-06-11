package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports anonymous SQLDelight parameters that should use named parameters.
 */
public class PreferNamedParametersRule : Rule {
    override val id: RuleId = RuleId("standard:prefer-named-parameters")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!context.file.path.endsWith(".sq")) return

        val content = context.file.content
        content
            .sqlCharacters()
            .filter { character -> character.value == '?' }
            .filterNot { character -> content.previousSqlTokenBefore(character.offset)?.isKeyword("in") == true }
            .forEach { character ->
                reporter.report(
                    Diagnostic(
                        ruleId = id,
                        severity = defaultSeverity,
                        message = "Use a named SQLDelight parameter instead of anonymous ?.",
                        file = context.file,
                        range = content.rangeAtOffsets(character.offset, character.offset + 1),
                        database = context.database,
                    ),
                )
            }
    }
}

private fun String.previousSqlTokenBefore(offset: Int): SqlToken? =
    sqlTokens()
        .takeWhile { token -> token.endOffset <= offset }
        .lastOrNull()
