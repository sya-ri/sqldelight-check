package dev.s7a.sqldelight.check.reporter.json

import kotlinx.serialization.Serializable

/**
 * Serialized SQL dialect metadata for a diagnostic database.
 */
@Serializable
internal data class JsonDialect(
    val family: String,
    val displayName: String,
    val artifact: String?,
    val version: String?,
    val implementationClass: String?,
    val capabilities: List<String>,
)
