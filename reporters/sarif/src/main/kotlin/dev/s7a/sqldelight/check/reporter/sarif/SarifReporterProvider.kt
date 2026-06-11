package dev.s7a.sqldelight.check.reporter.sarif

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import dev.s7a.sqldelight.check.reporter.api.ReporterId
import dev.s7a.sqldelight.check.reporter.api.ReportOutput
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Provider for the built-in SARIF reporter.
 */
public class SarifReporterProvider : ReporterProvider {
    override val id: ReporterId = ReporterId("sarif")

    override fun create(options: Map<String, String>): Reporter =
        SarifReporter(
            json =
                Json {
                    prettyPrint = options[PRETTY_PRINT_OPTION]?.toBooleanStrictOrNull() ?: false
                },
        )
}

private class SarifReporter(
    private val json: Json,
) : Reporter {
    override fun write(
        report: Report,
        output: ReportOutput,
    ) {
        output.file().use { file ->
            file.write(json.encodeToString(report.toSarifReport()).toByteArray())
        }
    }
}

private const val PRETTY_PRINT_OPTION = "prettyPrint"

@Serializable
private data class SarifReport(
    val version: String,
    @SerialName("\$schema")
    val schema: String,
    val runs: List<SarifRun>,
)

@Serializable
private data class SarifRun(
    val tool: SarifTool,
    val results: List<SarifResult>,
)

@Serializable
private data class SarifTool(
    val driver: SarifDriver,
)

@Serializable
private data class SarifDriver(
    val name: String,
    val semanticVersion: String,
    val rules: List<SarifRule>,
)

@Serializable
private data class SarifRule(
    val id: String,
)

@Serializable
private data class SarifResult(
    val ruleId: String,
    val level: String,
    val message: SarifMessage,
    val locations: List<SarifLocation>? = null,
)

@Serializable
private data class SarifMessage(
    val text: String,
)

@Serializable
private data class SarifLocation(
    val physicalLocation: SarifPhysicalLocation,
)

@Serializable
private data class SarifPhysicalLocation(
    val artifactLocation: SarifArtifactLocation,
    val region: SarifRegion,
)

@Serializable
private data class SarifArtifactLocation(
    val uri: String,
)

@Serializable
private data class SarifRegion(
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
)

private fun Report.toSarifReport(): SarifReport =
    SarifReport(
        version = "2.1.0",
        schema = "https://json.schemastore.org/sarif-2.1.0.json",
        runs =
            listOf(
                SarifRun(
                    tool =
                        SarifTool(
                            driver =
                                SarifDriver(
                                    name = "sqldelight-check",
                                    semanticVersion = "0.1.0",
                                    rules =
                                        diagnostics
                                            .map { diagnostic -> diagnostic.sarifRuleId() }
                                            .distinct()
                                            .map(::SarifRule),
                                ),
                        ),
                    results = diagnostics.map { diagnostic -> diagnostic.toSarifResult() },
                ),
            ),
    )

private fun Diagnostic.toSarifResult(): SarifResult =
    SarifResult(
        ruleId = sarifRuleId(),
        level = severity.toSarifLevel(),
        message = SarifMessage(message),
        locations = sarifLocations(),
    )

private fun Diagnostic.sarifRuleId(): String = ruleId.value

private fun Diagnostic.sarifLocations(): List<SarifLocation>? {
    val sourceFile = file ?: return null
    val sourceRange = range ?: return null
    return listOf(
        SarifLocation(
            physicalLocation =
                SarifPhysicalLocation(
                    artifactLocation = SarifArtifactLocation(sourceFile.path),
                    region = sourceRange.toSarifRegion(),
                ),
        ),
    )
}

private fun SourceRange.toSarifRegion(): SarifRegion =
    SarifRegion(
        startLine = start.line,
        startColumn = start.column,
        endLine = end.line,
        endColumn = end.column,
    )

private fun Severity.toSarifLevel(): String =
    when (this) {
        Severity.Error -> "error"
        Severity.Warning -> "warning"
        Severity.Info -> "note"
    }
