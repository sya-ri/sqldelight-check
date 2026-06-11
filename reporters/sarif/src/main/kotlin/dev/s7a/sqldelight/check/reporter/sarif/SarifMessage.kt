package dev.s7a.sqldelight.check.reporter.sarif

import kotlinx.serialization.Serializable

/**
 * SARIF message text.
 */
@Serializable
internal data class SarifMessage(
    val text: String,
)
