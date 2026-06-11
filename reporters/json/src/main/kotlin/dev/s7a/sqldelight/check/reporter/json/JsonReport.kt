package dev.s7a.sqldelight.check.reporter.json

import kotlinx.serialization.Serializable

/**
 * Root JSON report document.
 */
@Serializable
internal data class JsonReport(
    val summary: JsonSummary,
    val diagnostics: List<JsonDiagnostic>,
)
