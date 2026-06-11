package dev.s7a.sqldelight.check.api

/**
 * Source file known to sqldelight-check.
 */
public class SourceFile(
    /**
     * Root-project-relative path used in diagnostics and reports.
     */
    public val path: String,
    /**
     * Full file content at the time analysis starts.
     */
    public val content: String,
) {
    /**
     * SQLDelight source kind inferred from [path].
     */
    public val kind: SourceFileKind =
        when {
            path.endsWith(".sq") -> SourceFileKind.Query
            path.endsWith(".sqm") -> SourceFileKind.Migration
            else -> SourceFileKind.Other
        }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SourceFile &&
            path == other.path &&
            content == other.content

    override fun hashCode(): Int = 31 * path.hashCode() + content.hashCode()

    override fun toString(): String = "SourceFile(path=$path, content=$content)"
}
