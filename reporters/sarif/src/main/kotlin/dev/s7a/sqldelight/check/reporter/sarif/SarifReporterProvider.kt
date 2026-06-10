package dev.s7a.sqldelight.check.reporter.sarif

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import java.io.OutputStream

/**
 * Provider for the built-in SARIF reporter.
 */
public class SarifReporterProvider : ReporterProvider {
    override val id: String = "sarif"

    override fun create(options: Map<String, String>): Reporter = SarifReporter
}

private object SarifReporter : Reporter {
    override fun write(
        report: Report,
        output: OutputStream,
    ) {
        output.write(report.toSarif().toByteArray())
    }
}

private fun Report.toSarif(): String =
    buildString {
        append("{")
        appendJsonField("version", "2.1.0")
        append(",")
        appendJsonField("\$schema", "https://json.schemastore.org/sarif-2.1.0.json")
        append(",")
        append("\"runs\":[{")
        append("\"tool\":{\"driver\":{")
        appendJsonField("name", "sqldelight-check")
        append(",")
        appendJsonField("semanticVersion", "0.1.0")
        append(",")
        append("\"rules\":[")
        diagnostics
            .map { diagnostic -> diagnostic.ruleId?.value ?: "sqldelight-check:diagnostic" }
            .distinct()
            .forEachIndexed { index, ruleId ->
                if (index > 0) append(",")
                append("{")
                appendJsonField("id", ruleId)
                append("}")
            }
        append("]}}},")
        append("\"results\":[")
        diagnostics.forEachIndexed { index, diagnostic ->
            if (index > 0) append(",")
            appendResult(diagnostic)
        }
        append("]}]}")
    }

private fun StringBuilder.appendResult(diagnostic: Diagnostic) {
    append("{")
    appendJsonField("ruleId", diagnostic.ruleId?.value ?: "sqldelight-check:diagnostic")
    append(",")
    appendJsonField("level", diagnostic.severity.toSarifLevel())
    append(",")
    append("\"message\":{")
    appendJsonField("text", diagnostic.message)
    append("}")
    val file = diagnostic.file
    val range = diagnostic.range
    if (file != null && range != null) {
        append(",")
        append("\"locations\":[{\"physicalLocation\":{")
        append("\"artifactLocation\":{")
        appendJsonField("uri", file.path)
        append("},")
        append("\"region\":{")
        appendJsonField("startLine", range.start.line)
        append(",")
        appendJsonField("startColumn", range.start.column)
        append(",")
        appendJsonField("endLine", range.end.line)
        append(",")
        appendJsonField("endColumn", range.end.column)
        append("}}}]")
    }
    append("}")
}

private fun Severity.toSarifLevel(): String =
    when (this) {
        Severity.Error -> "error"
        Severity.Warning -> "warning"
        Severity.Info -> "note"
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
