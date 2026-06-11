package dev.s7a.sqldelight.check.reporter.json

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import dev.s7a.sqldelight.check.reporter.api.ReporterId
import dev.s7a.sqldelight.check.reporter.api.ReportOutput
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Provider for the built-in JSON reporter.
 */
public class JsonReporterProvider : ReporterProvider {
    override val id: ReporterId = ReporterId("json")

    override fun create(options: Map<String, String>): Reporter =
        JsonReporter(
            json =
                Json {
                    prettyPrint = options[PRETTY_PRINT_OPTION]?.toBooleanStrictOrNull() ?: false
                },
        )
}

private class JsonReporter(
    private val json: Json,
) : Reporter {
    override fun write(
        report: Report,
        output: ReportOutput,
    ) {
        output.file().use { file ->
            file.write(json.encodeToString(report.toJsonReport()).toByteArray())
        }
    }
}

private const val PRETTY_PRINT_OPTION = "prettyPrint"

@Serializable
private data class JsonReport(
    val formatVersion: String,
    val summary: JsonSummary,
    val diagnostics: List<JsonDiagnostic>,
)

@Serializable
private data class JsonSummary(
    val diagnostics: Int,
    val errors: Int,
    val warnings: Int,
    val infos: Int,
)

@Serializable
private data class JsonDiagnostic(
    val ruleId: String?,
    val severity: String,
    val message: String,
    val file: String?,
    val range: JsonRange?,
    val database: JsonDatabase?,
    val fixes: List<JsonFix>,
)

@Serializable
private data class JsonDatabase(
    val name: String,
    val dialect: JsonDialect,
)

@Serializable
private data class JsonDialect(
    val family: String,
    val displayName: String,
    val artifact: String?,
    val version: String?,
    val implementationClass: String?,
    val capabilities: List<String>,
)

@Serializable
private data class JsonFix(
    val title: String,
    val safety: String,
    val edits: List<JsonTextEdit>,
)

@Serializable
private data class JsonTextEdit(
    val range: JsonRange,
    val replacement: String,
)

@Serializable
private data class JsonRange(
    val start: JsonPosition,
    val end: JsonPosition,
)

@Serializable
private data class JsonPosition(
    val line: Int,
    val column: Int,
)

private fun Report.toJsonReport(): JsonReport =
    JsonReport(
        formatVersion = "0.1.0",
        summary =
            JsonSummary(
                diagnostics = diagnostics.size,
                errors = diagnostics.count { diagnostic -> diagnostic.severity == Severity.Error },
                warnings = diagnostics.count { diagnostic -> diagnostic.severity == Severity.Warning },
                infos = diagnostics.count { diagnostic -> diagnostic.severity == Severity.Info },
            ),
        diagnostics = diagnostics.map { diagnostic -> diagnostic.toJsonDiagnostic() },
    )

private fun Diagnostic.toJsonDiagnostic(): JsonDiagnostic =
    JsonDiagnostic(
        ruleId = ruleId.value,
        severity = severity.name.lowercase(),
        message = message,
        file = file?.path,
        range = range?.toJsonRange(),
        database = database?.toJsonDatabase(),
        fixes = fixes.map { fix -> fix.toJsonFix() },
    )

private fun DatabaseContext.toJsonDatabase(): JsonDatabase =
    JsonDatabase(
        name = name,
        dialect = dialect.toJsonDialect(),
    )

private fun SqlDialect.toJsonDialect(): JsonDialect =
    JsonDialect(
        family = family.name,
        displayName = displayName,
        artifact = artifact,
        version = version,
        implementationClass = implementationClass,
        capabilities = capabilities.map { capability -> capability.id }.sorted(),
    )

private fun Fix.toJsonFix(): JsonFix =
    JsonFix(
        title = title,
        safety = safety.name.lowercase(),
        edits = edits.map { edit -> edit.toJsonTextEdit() },
    )

private fun TextEdit.toJsonTextEdit(): JsonTextEdit =
    JsonTextEdit(
        range = range.toJsonRange(),
        replacement = replacement,
    )

private fun SourceRange.toJsonRange(): JsonRange =
    JsonRange(
        start = start.toJsonPosition(),
        end = end.toJsonPosition(),
    )

private fun SourcePosition.toJsonPosition(): JsonPosition =
    JsonPosition(line = line, column = column)
