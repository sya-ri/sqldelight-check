package dev.s7a.sqldelight.check.reporter.json

import kotlinx.serialization.Serializable

/**
 * Serialized text edit inside a diagnostic fix.
 */
@Serializable
internal data class JsonTextEdit(
    val range: JsonRange,
    val replacement: String,
)
