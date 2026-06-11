package dev.s7a.sqldelight.check.reporter.json

import kotlinx.serialization.Serializable

/**
 * Serialized fix attached to a diagnostic.
 */
@Serializable
internal data class JsonFix(
    val title: String,
    val safety: String,
    val edits: List<JsonTextEdit>,
)
