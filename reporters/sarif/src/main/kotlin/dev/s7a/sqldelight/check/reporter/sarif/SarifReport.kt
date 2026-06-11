package dev.s7a.sqldelight.check.reporter.sarif

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Root SARIF report document.
 */
@Serializable
internal data class SarifReport(
    val version: String,
    @SerialName("\$schema")
    val schema: String,
    val runs: List<SarifRun>,
)
