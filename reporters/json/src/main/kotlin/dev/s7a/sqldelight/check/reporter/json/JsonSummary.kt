package dev.s7a.sqldelight.check.reporter.json

import kotlinx.serialization.Serializable

/**
 * Aggregate diagnostic counts in a JSON report.
 */
@Serializable
internal data class JsonSummary(
    val diagnostics: Int,
    val errors: Int,
    val warnings: Int,
    val infos: Int,
)
