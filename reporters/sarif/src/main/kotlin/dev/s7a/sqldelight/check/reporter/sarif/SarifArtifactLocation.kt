package dev.s7a.sqldelight.check.reporter.sarif

import kotlinx.serialization.Serializable

/**
 * SARIF artifact URI for a source file.
 */
@Serializable
internal data class SarifArtifactLocation(
    val uri: String,
)
