package dev.s7a.sqldelight.check.reporter.sarif

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.reporter.api.Report

internal fun Report.toSarifReport(): SarifReport =
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
