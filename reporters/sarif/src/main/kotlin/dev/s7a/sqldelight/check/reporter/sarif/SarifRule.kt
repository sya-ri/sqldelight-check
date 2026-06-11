package dev.s7a.sqldelight.check.reporter.sarif

import kotlinx.serialization.Serializable

/**
 * SARIF rule descriptor.
 */
@Serializable
internal data class SarifRule(
    val id: String,
)
