package dev.s7a.sqldelight.check.reporter.json

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import java.io.OutputStream

/**
 * Provider for the built-in JSON reporter.
 */
public class JsonReporterProvider : ReporterProvider {
    override val id: String = "json"

    override fun create(options: Map<String, String>): Reporter = JsonReporter
}

private object JsonReporter : Reporter {
    override fun write(
        report: Report,
        output: OutputStream,
    ) {
        output.write(report.toJson().toByteArray())
    }
}

private fun Report.toJson(): String =
    buildString {
        append("{")
        appendJsonField("formatVersion", "0.1.0")
        append(",")
        append("\"summary\":")
        appendSummary(this@toJson)
        append(",")
        append("\"diagnostics\":[")
        diagnostics.forEachIndexed { index, diagnostic ->
            if (index > 0) append(",")
            appendDiagnostic(diagnostic)
        }
        append("]}")
    }

private fun StringBuilder.appendSummary(report: Report) {
    append("{")
    appendJsonField("diagnostics", report.diagnostics.size)
    append(",")
    appendJsonField("errors", report.diagnostics.count { diagnostic -> diagnostic.severity == Severity.Error })
    append(",")
    appendJsonField("warnings", report.diagnostics.count { diagnostic -> diagnostic.severity == Severity.Warning })
    append(",")
    appendJsonField("infos", report.diagnostics.count { diagnostic -> diagnostic.severity == Severity.Info })
    append("}")
}

private fun StringBuilder.appendDiagnostic(diagnostic: Diagnostic) {
    append("{")
    appendNullableJsonField("ruleId", diagnostic.ruleId?.value)
    append(",")
    appendJsonField("severity", diagnostic.severity.name.lowercase())
    append(",")
    appendJsonField("message", diagnostic.message)
    append(",")
    appendNullableJsonField("file", diagnostic.file?.path)
    append(",")
    append("\"range\":")
    appendRange(diagnostic.range)
    append(",")
    append("\"database\":")
    appendDatabase(diagnostic)
    append(",")
    append("\"fixes\":[")
    diagnostic.fixes.forEachIndexed { index, fix ->
        if (index > 0) append(",")
        appendFix(fix)
    }
    append("]}")
}

private fun StringBuilder.appendDatabase(diagnostic: Diagnostic) {
    val database = diagnostic.database
    if (database == null) {
        append("null")
        return
    }
    append("{")
    appendJsonField("name", database.name)
    append(",")
    append("\"dialect\":{")
    appendJsonField("family", database.dialect.family.name)
    append(",")
    appendJsonField("displayName", database.dialect.displayName)
    append(",")
    appendNullableJsonField("artifact", database.dialect.artifact)
    append(",")
    appendNullableJsonField("version", database.dialect.version)
    append(",")
    appendNullableJsonField("implementationClass", database.dialect.implementationClass)
    append(",")
    append("\"capabilities\":[")
    database.dialect.capabilities.sorted().forEachIndexed { index, capability ->
        if (index > 0) append(",")
        appendJsonString(capability)
    }
    append("]}}")
}

private fun StringBuilder.appendFix(fix: Fix) {
    append("{")
    appendJsonField("title", fix.title)
    append(",")
    appendJsonField("safety", fix.safety.name.lowercase())
    append(",")
    append("\"edits\":[")
    fix.edits.forEachIndexed { index, edit ->
        if (index > 0) append(",")
        appendEdit(edit)
    }
    append("]}")
}

private fun StringBuilder.appendEdit(edit: TextEdit) {
    append("{")
    append("\"range\":")
    appendRange(edit.range)
    append(",")
    appendJsonField("replacement", edit.replacement)
    append("}")
}

private fun StringBuilder.appendRange(range: SourceRange?) {
    if (range == null) {
        append("null")
        return
    }
    append("{")
    append("\"start\":")
    appendPosition(range.start)
    append(",")
    append("\"end\":")
    appendPosition(range.end)
    append("}")
}

private fun StringBuilder.appendPosition(position: SourcePosition) {
    append("{")
    appendJsonField("line", position.line)
    append(",")
    appendJsonField("column", position.column)
    append("}")
}

private fun StringBuilder.appendJsonField(
    name: String,
    value: String,
) {
    appendJsonString(name)
    append(":")
    appendJsonString(value)
}

private fun StringBuilder.appendJsonField(
    name: String,
    value: Int,
) {
    appendJsonString(name)
    append(":")
    append(value)
}

private fun StringBuilder.appendNullableJsonField(
    name: String,
    value: String?,
) {
    appendJsonString(name)
    append(":")
    if (value == null) {
        append("null")
    } else {
        appendJsonString(value)
    }
}

private fun StringBuilder.appendJsonString(value: String) {
    append("\"")
    value.forEach { character ->
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(character)
        }
    }
    append("\"")
}
