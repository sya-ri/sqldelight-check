package dev.s7a.sqldelight.check.reporter.markdown

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import dev.s7a.sqldelight.check.reporter.api.ReporterId
import dev.s7a.sqldelight.check.reporter.api.ReportOutput

/**
 * Provider for the built-in Markdown reporter.
 */
public class MarkdownReporterProvider : ReporterProvider {
    override val id: ReporterId = ReporterId("markdown")

    override fun create(options: Map<String, String>): Reporter = MarkdownReporter
}

private object MarkdownReporter : Reporter {
    override fun write(
        report: Report,
        output: ReportOutput,
    ) {
        output.file().use { file ->
            file.write(report.toMarkdown().toByteArray())
        }
    }
}

private fun Report.toMarkdown(): String =
    buildString {
        appendLine("# sqldelight-check")
        appendLine()
        appendLine("| Total | Errors | Warnings | Infos |")
        appendLine("| ---: | ---: | ---: | ---: |")
        appendLine(
            "| ${diagnostics.size} | " +
                "${diagnostics.count(Severity.Error)} | " +
                "${diagnostics.count(Severity.Warning)} | " +
                "${diagnostics.count(Severity.Info)} |",
        )
        appendLine()
        appendLine("## Diagnostics")
        appendLine()
        if (diagnostics.isEmpty()) {
            appendLine("No diagnostics.")
            return@buildString
        }
        appendLine("| Severity | Rule | Location | Message | Fixes |")
        appendLine("| --- | --- | --- | --- | ---: |")
        diagnostics.forEach { diagnostic ->
            appendDiagnosticRow(diagnostic)
        }
    }

private fun List<Diagnostic>.count(severity: Severity): Int =
    count { diagnostic -> diagnostic.severity == severity }

private fun StringBuilder.appendDiagnosticRow(diagnostic: Diagnostic) {
    append("| ")
    append(diagnostic.severity.name.escapeMarkdownTableCell())
    append(" | `")
    append((diagnostic.qualifiedRuleId?.value ?: diagnostic.ruleId?.value ?: "-").escapeMarkdownCode())
    append("` | ")
    append(diagnostic.locationLabel().escapeMarkdownTableCell())
    append(" | ")
    append(diagnostic.message.escapeMarkdownTableCell())
    append(" | ")
    append(diagnostic.fixes.size)
    appendLine(" |")
}

private fun Diagnostic.locationLabel(): String {
    val path = file?.path ?: "-"
    val rangeLabel = range?.toLocationLabel()
    return if (rangeLabel == null) path else "$path:$rangeLabel"
}

private fun SourceRange.toLocationLabel(): String = "${start.line}:${start.column}"

private fun String.escapeMarkdownTableCell(): String =
    replace("\\", "\\\\")
        .replace("|", "\\|")
        .replace("\n", "<br>")
        .replace("\r", "")

private fun String.escapeMarkdownCode(): String =
    replace("\\", "\\\\")
        .replace("`", "\\`")
