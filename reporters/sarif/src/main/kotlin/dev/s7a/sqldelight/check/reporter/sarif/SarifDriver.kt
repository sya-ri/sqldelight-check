package dev.s7a.sqldelight.check.reporter.sarif

import kotlinx.serialization.Serializable

/**
 * SARIF driver metadata for sqldelight-check.
 */
@Serializable
internal data class SarifDriver(
    val name: String,
    val semanticVersion: String,
    val rules: List<SarifRule>,
)
