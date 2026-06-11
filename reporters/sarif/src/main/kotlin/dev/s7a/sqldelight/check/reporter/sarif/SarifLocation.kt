package dev.s7a.sqldelight.check.reporter.sarif

import kotlinx.serialization.Serializable

/**
 * SARIF location for a diagnostic.
 */
@Serializable
internal data class SarifLocation(
    val physicalLocation: SarifPhysicalLocation,
)
