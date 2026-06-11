package dev.s7a.sqldelight.check.reporter.json

import kotlinx.serialization.Serializable

/**
 * Serialized diagnostic entry in a JSON report.
 */
@Serializable
internal data class JsonDiagnostic(
    val ruleId: String?,
    val severity: String,
    val message: String,
    val file: String?,
    val range: JsonRange?,
    val database: JsonDatabase?,
    val fixes: List<JsonFix>,
)
