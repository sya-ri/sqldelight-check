package dev.s7a.sqldelight.check.reporter.sarif

import kotlinx.serialization.Serializable

/**
 * SARIF run emitted by sqldelight-check.
 */
@Serializable
internal data class SarifRun(
    val tool: SarifTool,
    val results: List<SarifResult>,
)
