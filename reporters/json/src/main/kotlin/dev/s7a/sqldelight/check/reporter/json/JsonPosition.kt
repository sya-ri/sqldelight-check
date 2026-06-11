package dev.s7a.sqldelight.check.reporter.json

import kotlinx.serialization.Serializable

/**
 * Serialized one-based source position.
 */
@Serializable
internal data class JsonPosition(
    val line: Int,
    val column: Int,
)
