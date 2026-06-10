package dev.s7a.sqldelight.check.api

/**
 * Severity assigned to a diagnostic after rule defaults and user configuration are resolved.
 */
public enum class Severity {
    /** Informational diagnostic that does not normally fail builds. */
    Info,

    /** Warning diagnostic that should be visible but does not fail builds by default. */
    Warning,

    /** Error diagnostic that should fail check tasks. */
    Error,
}
