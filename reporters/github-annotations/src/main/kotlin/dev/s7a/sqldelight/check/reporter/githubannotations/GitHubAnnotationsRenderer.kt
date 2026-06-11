package dev.s7a.sqldelight.check.reporter.githubannotations

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.reporter.api.Report

internal fun Report.toGitHubAnnotations(): String =
    buildString {
        diagnostics.forEach { diagnostic ->
            appendLine(diagnostic.toWorkflowCommand())
        }
    }

private fun Diagnostic.toWorkflowCommand(): String {
    val properties =
        buildList {
            add("title=${ruleId.value.escapeWorkflowCommandProperty()}")
            file?.path?.let { path ->
                add("file=${path.escapeWorkflowCommandProperty()}")
            }
            range?.let { sourceRange ->
                addAll(sourceRange.toWorkflowCommandProperties())
            }
        }
    return "::${severity.toWorkflowCommand()} ${properties.joinToString(",")}::${message.escapeWorkflowCommandData()}"
}

private fun SourceRange.toWorkflowCommandProperties(): List<String> =
    listOf(
        "line=${start.line}",
        "col=${start.column}",
        "endLine=${end.line}",
        "endColumn=${end.column}",
    )

private fun Severity.toWorkflowCommand(): String =
    when (this) {
        Severity.Error -> "error"
        Severity.Warning -> "warning"
        Severity.Info -> "notice"
    }

private fun String.escapeWorkflowCommandData(): String =
    replace("%", "%25")
        .replace("\r", "%0D")
        .replace("\n", "%0A")

private fun String.escapeWorkflowCommandProperty(): String =
    escapeWorkflowCommandData()
        .replace(":", "%3A")
        .replace(",", "%2C")
