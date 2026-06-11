package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports simple inclusive range predicates that can be written with BETWEEN.
 */
public class PreferBetweenForInclusiveRangeRule : Rule {
    override val id: RuleId = RuleId("prefer-between-for-inclusive-range")
    override val defaultSeverity: Severity = Severity.Info
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("where")) return@forEachIndexed
            val statementEnd = content.statementEndAfter(token.startOffset)
            val boundary = tokens.firstBoundaryOffsetAfter(index + 1, statementEnd, betweenBoundaryKeywords)
            val segment = content.substring(token.endOffset, boundary)
            inclusiveRangePattern.findAll(segment).forEach { match ->
                val column = match.groupValues[1]
                val repeated = match.groupValues[3]
                if (!column.equals(repeated, ignoreCase = true)) return@forEach
                val startOffset = token.endOffset + match.range.first
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Prefer BETWEEN for simple inclusive ranges on the same expression.",
                        file = context.file,
                        range = content.rangeAtOffsets(startOffset, startOffset + column.length),
                        database = context.database,
                    ),
                )
            }
        }
    }
}

private val inclusiveRangePattern =
    Regex("""\b([A-Za-z_][A-Za-z0-9_.]*)\s*>=\s*(:[A-Za-z_][A-Za-z0-9_]*|\?|[-]?\d+(?:\.\d+)?)\s+AND\s+([A-Za-z_][A-Za-z0-9_.]*)\s*<=""", RegexOption.IGNORE_CASE)

private val betweenBoundaryKeywords = setOf("except", "group", "having", "intersect", "limit", "offset", "order", "union", "window")
