package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports `SELECT` modifiers that are split onto another line.
 */
public class SelectModifierLinePositionRule : Rule {
    override val id: String = "select-modifier-line-position"
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val lines = content.linesWithRanges()
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("select")) return@forEachIndexed

            val modifier = tokens.getOrNull(index + 1) ?: return@forEachIndexed
            if (modifier.normalizedText !in selectModifiers) return@forEachIndexed

            val selectLine = lines.lineContaining(token.startOffset) ?: return@forEachIndexed
            val modifierLine = lines.lineContaining(modifier.startOffset) ?: return@forEachIndexed
            if (selectLine.number == modifierLine.number) return@forEachIndexed

            reporter.report(
                Diagnostic(
                    ruleId = RuleId(id),
                    severity = defaultSeverity,
                    message = "${modifier.text.uppercase()} should be on the same line as SELECT.",
                    file = context.file,
                    range = content.rangeAtOffsets(modifier.startOffset, modifier.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private val selectModifiers = setOf("all", "distinct")
