package dev.s7a.sqldelight.check.reporter.sarif

import kotlinx.serialization.Serializable

/**
 * SARIF tool metadata wrapper.
 */
@Serializable
internal data class SarifTool(
    val driver: SarifDriver,
)
