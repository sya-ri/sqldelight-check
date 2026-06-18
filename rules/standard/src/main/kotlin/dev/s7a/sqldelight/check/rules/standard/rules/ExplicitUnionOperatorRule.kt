package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

/**
 * Reports `UNION` operators that do not explicitly specify `ALL` or `DISTINCT`.
 */
public class ExplicitUnionOperatorRule : Rule {
    override val id: RuleId = RuleId("explicit-union-operator")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.Union)) return@forEachIndexed
            val next = tokens.getOrNull(index + 1)
            if (next != null && unionModifierTerms.any { term -> next.isTerm(term) }) return@forEachIndexed

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "UNION should explicitly specify ALL or DISTINCT.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                    database = context.database,
                    fixes = listOf(content.insertUnionDistinctFix(token.endOffset)),
                ),
            )
        }
    }
}

private fun String.insertUnionDistinctFix(offset: Int): Fix =
    Fix(
        title = "Use UNION DISTINCT",
        safety = FixSafety.Safe,
        edits = listOf(TextEdit(range = rangeAtOffsets(offset, offset), replacement = " DISTINCT")),
    )

private val unionModifierTerms =
    setOf(
        SqlDialectSourceTerm.All,
        SqlDialectSourceTerm.Distinct,
    )
