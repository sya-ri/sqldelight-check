package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports SQLDelight query labels that are not lower camel case.
 */
public class QueryNameCaseRule : Rule {
    override val id: RuleId = RuleId("query-name-case")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Query) return

        val content = context.file.content
        content.sqlDelightLabels().forEach { label ->
            if (label.name.isLowerCamelIdentifier()) return@forEach

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "SQLDelight query label '${label.name}' should be lower camel case.",
                    file = context.file,
                    range = content.rangeAtOffsets(label.startOffset, label.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private data class SqlDelightLabel(
    val name: String,
    val startOffset: Int,
    val endOffset: Int,
)

private fun String.sqlDelightLabels(): Sequence<SqlDelightLabel> =
    sequence {
        linesWithRanges().forEach { line ->
            val first = line.firstNonWhitespaceOffset ?: return@forEach
            if (startsWith("--", first) || startsWith("/*", first)) return@forEach

            val name = identifierTokenAt(first) ?: return@forEach
            if (getOrNull(name.endOffset) != ':') return@forEach
            if (getOrNull(name.endOffset + 1) == ':') return@forEach

            yield(SqlDelightLabel(name = name.text, startOffset = name.startOffset, endOffset = name.endOffset))
        }
    }
