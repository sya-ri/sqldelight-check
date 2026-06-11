package dev.s7a.sqldelight.check.reporter.json

import kotlinx.serialization.Serializable

/**
 * Serialized SQL dialect metadata for a diagnostic database.
 */
@Serializable
internal data class JsonDialect(
    val family: String,
    val capabilities: List<String>,
)
