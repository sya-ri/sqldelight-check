package dev.s7a.sqldelight.check.api

/**
 * SQLDelight source file kind inferred from the file extension.
 */
public enum class SourceFileKind {
    /**
     * A `.sq` query/schema source file.
     */
    Query,

    /**
     * A `.sqm` migration source file.
     */
    Migration,

    /**
     * Any other source file kind.
     */
    Other,
}
