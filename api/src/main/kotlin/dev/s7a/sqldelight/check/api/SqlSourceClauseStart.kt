package dev.s7a.sqldelight.check.api

/**
 * A dialect pattern match that starts a source clause block.
 */
internal class SqlSourceClauseStart(
    val context: SqlSourceTokenContext,
    val match: SqlSourcePatternMatch,
)
