package dev.s7a.sqldelight.check.api

internal fun Set<String>.normalizedSqlTerms(): Set<String> =
    mapTo(mutableSetOf()) { term -> term.lowercase() }

internal fun String.patternParts(): List<SqlDialectSourcePatternPart> =
    trim()
        .split(Regex("\\s+"))
        .filter { token -> token.isNotBlank() }
        .map { token -> token.patternPart() }

private fun String.patternPart(): SqlDialectSourcePatternPart {
    val optional = startsWith("[") && endsWith("]")
    val choice = startsWith("{") && endsWith("}")
    val body =
        when {
            optional || choice -> substring(1, length - 1)
            else -> this
        }
    val alternatives = body.split("|").filter { term -> term.isNotBlank() }.toSet()
    return SqlDialectSourcePatternPart(
        alternatives = alternatives,
        optional = optional,
    )
}
