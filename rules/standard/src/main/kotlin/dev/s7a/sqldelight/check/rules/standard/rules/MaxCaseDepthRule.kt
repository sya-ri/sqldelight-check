package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlSourceBlockKind
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.positiveIntOption

private const val DEFAULT_MAX_CASE_DEPTH = 2

/**
 * Reports CASE expressions nested deeper than the configured limit.
 */
public class MaxCaseDepthRule : Rule {
    private val maxCaseDepthOption by positiveIntOption("maxDepth", DEFAULT_MAX_CASE_DEPTH)

    override val id: RuleId = RuleId("max-case-depth")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val maxDepth = context.options[maxCaseDepthOption]
        val content = context.file.content
        val structure = context.sourceStructure
        structure.blocks
            .filter { block -> block.kind == SqlSourceBlockKind.CaseExpression }
            .forEach { block ->
                val depth = structure.tokens[block.startTokenIndex].caseDepth + 1
                if (depth <= maxDepth) return@forEach
                val token = structure.tokens[block.startTokenIndex].token
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "CASE nesting depth is greater than $maxDepth.",
                        file = context.file,
                        range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}
