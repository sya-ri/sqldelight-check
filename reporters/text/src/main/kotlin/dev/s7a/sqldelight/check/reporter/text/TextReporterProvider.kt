package dev.s7a.sqldelight.check.reporter.text

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import dev.s7a.sqldelight.check.reporter.api.ReportOutput

/**
 * Provider for the built-in plain text reporter.
 */
public class TextReporterProvider : ReporterProvider {
    override val id: String = "text"

    override fun create(options: Map<String, String>): Reporter = TextReporter
}

private object TextReporter : Reporter {
    override fun write(
        report: Report,
        output: ReportOutput,
    ) {
        output.file().use { file ->
            file.write(report.render().toByteArray())
        }
    }
}

private fun Report.render(): String =
    buildString {
        appendLine("sqldelight-check diagnostics: ${diagnostics.size}")
        if (diagnostics.isEmpty()) {
            return@buildString
        }

        appendLine("Diagnostics:")
        diagnostics
            .sorted()
            .forEach { diagnostic ->
                appendLine("- ${diagnostic.severity.label} ${diagnostic.ruleLabel} at ${diagnostic.locationLabel}")
                appendIndented(diagnostic.message)
                if (diagnostic.fixes.isNotEmpty()) {
                    appendLine("  fixes:")
                    diagnostic.fixes.forEach { fix -> appendFix(diagnostic, fix) }
                }
            }
    }

private fun List<Diagnostic>.sorted(): List<Diagnostic> =
    withIndex()
        .sortedWith(
            compareBy<IndexedValue<Diagnostic>>(
                { it.value.file?.path ?: "" },
                { it.value.range?.start?.line ?: 0 },
                { it.value.range?.start?.column ?: 0 },
                { it.value.range?.end?.line ?: 0 },
                { it.value.range?.end?.column ?: 0 },
                { it.value.severity.sortOrder },
                { it.value.ruleId?.value ?: "" },
                { it.value.message },
                { it.index },
            ),
        )
        .map { it.value }

private fun StringBuilder.appendIndented(text: String) {
    text
        .lines()
        .forEach { line -> appendLine("  $line") }
}

private fun StringBuilder.appendFix(
    diagnostic: Diagnostic,
    fix: Fix,
) {
    appendLine("  - ${fix.title} [${fix.safety.name.lowercase()}]")
    fix.edits.forEachIndexed { index, edit ->
        appendLine("    edit ${index + 1}: ${edit.locationLabel(diagnostic)} -> ${edit.replacement.quoted()}")
    }
}

private val Diagnostic.ruleLabel: String
    get() = ruleId?.value ?: "sqldelight-check"

private val Diagnostic.locationLabel: String
    get() = locationLabel(file?.path, range)

private fun TextEdit.locationLabel(diagnostic: Diagnostic): String = locationLabel(diagnostic.file?.path, range)

private fun locationLabel(
    path: String?,
    range: SourceRange?,
): String {
    val base = path ?: "<project>"
    return if (range == null) {
        base
    } else {
        "$base:${range.start.line}:${range.start.column}-${range.end.line}:${range.end.column}"
    }
}

private val Severity.label: String
    get() =
        when (this) {
            Severity.Error -> "Error"
            Severity.Warning -> "Warning"
            Severity.Info -> "Info"
        }

private val Severity.sortOrder: Int
    get() =
        when (this) {
            Severity.Error -> 0
            Severity.Warning -> 1
            Severity.Info -> 2
        }

private fun String.quoted(): String = "\"" + escaped() + "\""

private fun String.escaped(): String =
    buildString {
        this@escaped.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
