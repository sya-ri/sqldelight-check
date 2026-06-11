package dev.s7a.sqldelight.check.reporter.json

import kotlinx.serialization.Serializable

/**
 * Serialized source range with start and end positions.
 */
@Serializable
internal data class JsonRange(
    val start: JsonPosition,
    val end: JsonPosition,
)
