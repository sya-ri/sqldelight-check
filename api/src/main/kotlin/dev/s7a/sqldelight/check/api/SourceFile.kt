package dev.s7a.sqldelight.check.api

/**
 * Source file known to sqldelight-check.
 */
public data class SourceFile(
    /**
     * Project-relative path used in diagnostics and reports.
     */
    public val path: String,
    /**
     * Full file content at the time analysis starts.
     */
    public val content: String,
)
