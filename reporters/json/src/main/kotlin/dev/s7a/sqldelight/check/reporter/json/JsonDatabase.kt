package dev.s7a.sqldelight.check.reporter.json

import kotlinx.serialization.Serializable

/**
 * Serialized database context for a diagnostic.
 */
@Serializable
internal data class JsonDatabase(
    val name: String,
    val dialect: JsonDialect,
)
